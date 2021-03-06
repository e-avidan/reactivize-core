package il.ac.technion.cs.reactivize

import il.ac.technion.cs.reactivize.graph.*
import soot.*
import soot.Unit
import soot.jimple.*
import soot.toolkits.graph.UnitGraph
import soot.toolkits.scalar.BackwardFlowAnalysis

class ReactivizeTransformer(val spec: ReactivizeCompileSpec) {
    private val visitor = TransformVisitor()

    fun transform(graph: List<WorkUnit>): Iterable<SootClass> {
        graph.forEach {
            transform(it.subunits)
            it.accept(visitor)
        }
        return visitor.modifiedClasses
    }
}

class TransformVisitor : WorkUnitVisitor {
    val modifiedClasses: MutableSet<SootClass> = mutableSetOf()

    override fun visit(v: ValueMethod) {
        val m = v.sootMethod
        val c = m.declaringClass

        val observerField = c.getFieldUnsafe(
            v.observableName,
            Scene.v().getSootClass("io.reactivex.rxjava3.subjects.BehaviorSubject").type
        ) ?: createValueMethodObserverField(v)
        // Do I need this? It's supposedly public... Maybe use the getter instead.
        observerField.modifiers = Modifier.PUBLIC

        // Need to create this here because we reference it.
        println("creating method subscriberMethodName = '${v.subscriberMethodName}'")
        // FIXME: Parameters should maybe be `m.parameterTypes`. But that would make calling the method harder.
        val subscriberMethod = SootMethod(v.subscriberMethodName, listOf(), m.returnType, Modifier.PUBLIC)
        c.addMethod(subscriberMethod)
        modifiedClasses.add(c)

        // TODO: Move lambda class naming to analysis stage
        val lambdaClass =
            SootClass(
                c.javaPackageName + "." + c.javaStyleName + "$" + m.name + "$" + "reactivize" + "$" + "1",
                Modifier.PUBLIC
            )
        modifiedClasses.add(lambdaClass)
        lambdaClass.superclass = Scene.v().getSootClass("java.lang.Object")
        lambdaClass.addInterface(Scene.v().getSootClass("io.reactivex.rxjava3.functions.Consumer"))
        lambdaClass.outerClass = c
        Scene.v().addClass(lambdaClass)

        val outerThisField = SootField("this\$0", c.type)
        lambdaClass.addField(outerThisField)

        val acceptMethod =
            SootMethod("accept", listOf(RefType.v("java.lang.Object")), VoidType.v(), Modifier.PUBLIC or Modifier.FINAL)
        lambdaClass.addMethod(acceptMethod)
        val acceptBody = Jimple.v().newBody(acceptMethod)
        acceptMethod.activeBody = acceptBody
        acceptBody.insertIdentityStmts()

        val acceptOuterThis = Jimple.v().newLocal("\$stack1", c.type)
        val acceptObservable = Jimple.v().newLocal("\$stack2", observerField.type)
        val acceptGetterResult = Jimple.v().newLocal("\$stack3", m.returnType)
        acceptBody.locals.addAll(listOf(acceptOuterThis, acceptObservable, acceptGetterResult))
        val onNextCall =
            Scene.v().getMethod("<io.reactivex.rxjava3.subjects.BehaviorSubject: void onNext(java.lang.Object)>")

        // TODO/FIXME: Box all primitive types, not just Double.
        val onNextInvocation = if (m.returnType == DoubleType.v()) {
            val boxLocal = Jimple.v().newLocal("box", Scene.v().getType("java.lang.Double"))
            acceptBody.locals.add(boxLocal)
            listOf(
                Jimple.v().newAssignStmt(boxLocal, Jimple.v().newNewExpr(Scene.v().getRefType("java.lang.Double"))),
                Jimple.v().newInvokeStmt(
                    Jimple.v().newSpecialInvokeExpr(
                        boxLocal,
                        Scene.v().getSootClass("java.lang.Double").getMethod("<init>", listOf(DoubleType.v()))
                            .makeRef(),
                        acceptGetterResult
                    )
                ),
                Jimple.v().newInvokeStmt(
                    Jimple.v().newVirtualInvokeExpr(
                        acceptObservable,
                        onNextCall.makeRef(),
                        boxLocal
                    )
                )
            )
        } else {
            listOf(
                Jimple.v().newInvokeStmt(
                    Jimple.v().newVirtualInvokeExpr(
                        acceptObservable,
                        onNextCall.makeRef(),
                        acceptGetterResult
                    )
                )
            )
        }

        Jimple.v().apply {
            acceptBody.units.addAll(
                listOf(
                    newAssignStmt(acceptOuterThis, newInstanceFieldRef(acceptBody.thisLocal, outerThisField.makeRef())),
                    newAssignStmt(acceptObservable, newInstanceFieldRef(acceptOuterThis, observerField.makeRef())),
                    /* FIXME: This is a cop-out, calling the getter to get a value isn't reactive. We need to create a
                     *   different accept lambda for each observer, and use code from the getter to convert the value
                     *   that was obtained in the reactive path (i.e., the parameter) to the final type. */
                    newAssignStmt(acceptGetterResult, newVirtualInvokeExpr(acceptOuterThis, m.makeRef()))
                )
            )
            acceptBody.units.addAll(onNextInvocation)
            acceptBody.units.add(Jimple.v().newReturnVoidStmt())
        }
        acceptBody.validate()

        val acceptInitMethod = SootMethod("<init>", listOf(c.type), VoidType.v())
        val acceptInitBody = Jimple.v().newBody(acceptInitMethod)
        acceptInitMethod.activeBody = acceptInitBody
        lambdaClass.addMethod(acceptInitMethod)

        acceptInitBody.insertIdentityStmts()
        Jimple.v().apply {
            acceptInitBody.units.addAll(
                listOf(
                    newAssignStmt(
                        newInstanceFieldRef(acceptInitBody.thisLocal, outerThisField.makeRef()),
                        acceptInitBody.parameterLocals[0]
                    ),
                    newInvokeStmt(
                        newSpecialInvokeExpr(
                            acceptInitBody.thisLocal,
                            Scene.v().getSootClass("java.lang.Object").getMethodByName("<init>").makeRef()
                        )
                    ),
                    newReturnVoidStmt()
                )
            )
        }

        acceptInitBody.validate()

        val subscriberBody = Jimple.v().newBody(subscriberMethod)
        subscriberMethod.activeBody = subscriberBody

        subscriberBody.locals.addAll(m.activeBody.locals)
        subscriberBody.units.addAll(m.activeBody.units)

        val unitToInstructionsMap: MutableMap<Unit, List<Unit>> = mutableMapOf()

        v.subunits.filterIsInstance<ReactivizableFieldInClass>().forEach { field ->
            val stmt = field.unitInCallingMethod as Stmt
            assert(stmt.containsFieldRef())

            val fieldRef = stmt.fieldRef as InstanceFieldRef
            val baseLocal = fieldRef.base as Local
            val namePrefix = "${baseLocal.name}_${fieldRef.field.name}_"

            unitToInstructionsMap[stmt] = createSubscribeMethodInstructions(
                field.sootClass,
                field.observerName,
                namePrefix,
                subscriberBody,
                lambdaClass,
                acceptInitMethod,
                baseLocal
            )
        }
        v.subunits.filterIsInstance<ClassWithObservable>().forEach { subunit ->
            if (subunit.unitInCallingMethod == null) {
                println("Not handling $subunit")
                return@forEach
            }

            val stmt = subunit.unitInCallingMethod as Stmt
            assert(stmt.containsInvokeExpr())

            val invokeExpr = stmt.invokeExpr as InstanceInvokeExpr
            val instance = invokeExpr.base as Local
            val namePrefix = "${instance.name}_"

            unitToInstructionsMap[stmt] = createSubscribeMethodInstructions(
                subunit.sootClass,
                subunit.observableFieldName,
                namePrefix,
                subscriberBody,
                lambdaClass,
                acceptInitMethod,
                instance,
                subunit.subscriberMethodName
            )
        }

        for (entry in unitToInstructionsMap) {
            subscriberBody.units.insertAfter(entry.value, entry.key)
        }

        println("!!!")
        println("Handled: $m")
        println("New method: $subscriberMethod")
        println(subscriberBody)
        println("???")
        subscriberBody.validate()
    }

    private fun createSubscribeMethodInstructions(
        dependencyClass: SootClass,
        observableFieldName: String,
        namePrefix: String,
        subscriberBody: JimpleBody,
        lambdaClass: SootClass,
        acceptInitMethod: SootMethod,
        instance: Local,
        subscriberMethodName: String? = null
    ): List<Unit> {
        val disposableHelper = Scene.v().getSootClass("il.ac.technion.cs.reactivize.helpers.DisposableStore")
        val disposableField = SootField("$observableFieldName\$reactivize\$disposable", disposableHelper.type)
        addFieldToClass(
            subscriberBody.method.declaringClass,
            disposableHelper.getMethod("create", listOf()),
            disposableField
        )

        val observable = Jimple.v().newLocal(
            "${namePrefix}observable",
            Scene.v().getSootClass("io.reactivex.rxjava3.core.Observable").type
        )
        val lambda = Jimple.v().newLocal("${namePrefix}lambda", lambdaClass.type)
        val consumer = Jimple.v()
            .newLocal(
                "${namePrefix}consumer",
                Scene.v().getType("io.reactivex.rxjava3.functions.Consumer")
            )
        val disposable = Jimple.v()
            .newLocal("${namePrefix}disposable", Scene.v().getType("io.reactivex.rxjava3.disposables.Disposable"))
        val disposableHelperLocal = Jimple.v().newLocal("${namePrefix}disposableHelper", disposableHelper.type)
        subscriberBody.locals.addAll(listOf(observable, lambda, consumer, disposable, disposableHelperLocal))
        val memberObservableField = dependencyClass.getField(
            observableFieldName,
            Scene.v().getType("io.reactivex.rxjava3.subjects.BehaviorSubject")
        )

        Jimple.v().apply {
            return listOf(
                newAssignStmt(observable, newInstanceFieldRef(instance, memberObservableField.makeRef())),
                newAssignStmt(lambda, newNewExpr(lambdaClass.type)),
                newInvokeStmt(
                    newSpecialInvokeExpr(lambda, acceptInitMethod.makeRef(), listOf(subscriberBody.thisLocal))
                ),
                newAssignStmt(
                    consumer,
                    newCastExpr(lambda, Scene.v().getSootClass("io.reactivex.rxjava3.functions.Consumer").type)
                ),
                newAssignStmt(
                    disposable,
                    newVirtualInvokeExpr(
                        observable, Scene.v().getSootClass("io.reactivex.rxjava3.core.Observable")
                            .getMethod("io.reactivex.rxjava3.disposables.Disposable subscribe(io.reactivex.rxjava3.functions.Consumer)")
                            .makeRef(), listOf(consumer)
                    )
                ),
                newAssignStmt(
                    disposableHelperLocal,
                    newInstanceFieldRef(subscriberBody.thisLocal, disposableField.makeRef())
                ),
                newInvokeStmt(
                    newVirtualInvokeExpr(
                        disposableHelperLocal,
                        disposableHelper.getMethodByName("setDisposable").makeRef(),
                        disposable
                    )
                )
            ) + (if (subscriberMethodName != null) listOf(
                newInvokeStmt(
                    newVirtualInvokeExpr(
                        instance,
                        dependencyClass.getMethodByName(subscriberMethodName).makeRef()
                    )
                )
            ) else listOf<Unit>())
        }
    }

    private fun createValueMethodObserverField(v: ValueMethod): SootField {
        val m = v.sootMethod
        val c = m.declaringClass

        // FIXME: Add a getter for the field instead of making it public
        val f = SootField(v.observableName, RefType.v("io.reactivex.rxjava3.subjects.BehaviorSubject"), Modifier.PUBLIC)
        addFieldToClass(
            c,
            Scene.v()
                .getMethod("<io.reactivex.rxjava3.subjects.BehaviorSubject: io.reactivex.rxjava3.subjects.BehaviorSubject create()>"),
            f
        )

        return f
    }

    override fun visit(v: FunctionCallingMethod) {
        // FIXME: Do something. But don't throw.
        val m = v.sootMethod
        val c = m.declaringClass
        modifiedClasses.add(c)

        // Need to create this here because we reference it.
        println("creating method subscriberMethodName = '${v.subscriberMethodName}'")
        // FIXME: Parameters should maybe be `m.parameterTypes`. But that would make calling the method harder.
        val subscriberMethod = SootMethod(v.subscriberMethodName, listOf(), m.returnType)
        c.addMethod(subscriberMethod)

        subscriberMethod.activeBody = m.activeBody // Make it do the same as the regular method, FIXME.
    }

    override fun visit(v: ClassWithObservable) {
        // FIXME: ClassWithObservable is useless, merge with ReactivizableMethod
        if (v.unitInCallingMethod == null) {
            /* If this is a top-level, i.e. annotated method, replace the method with the subscribe version. */
            val stopRecursionField =
                SootField("stopRecursionField\$reactivize", BooleanType.v(), Modifier.PUBLIC or Modifier.STATIC)
            v.sootClass.addField(stopRecursionField)

            assert(v.subunits.size == 1)
            val valueMethod = v.subunits[0] as ReactivizableMethod
            val method = valueMethod.sootMethod
            val body = method.activeBody as JimpleBody
            val firstUnit = body.firstNonIdentityStmt
            val stopRecursionLocal = Jimple.v().newLocal("stopRecursion", stopRecursionField.type)
            body.locals.add(stopRecursionLocal)
            val firstUnitBox = Jimple.v().newStmtBox(Jimple.v().newNopStmt())
            Jimple.v().apply {
                body.units.insertBefore(
                    listOf(
                        newAssignStmt(stopRecursionLocal, newStaticFieldRef(stopRecursionField.makeRef())),
                        newIfStmt(newEqExpr(stopRecursionLocal, IntConstant.v(1)), firstUnitBox),
                        newAssignStmt(newStaticFieldRef(stopRecursionField.makeRef()), IntConstant.v(1))
                    ), firstUnit
                )
                if (method.returnType != VoidType.v()) {
                    val retLocal = newLocal("ret", method.returnType)
                    body.locals.add(retLocal)
                    body.units.insertBefore(
                        listOf(
                            newAssignStmt(
                                retLocal,
                                newVirtualInvokeExpr(
                                    body.thisLocal,
                                    v.sootClass.getMethodByName(v.subscriberMethodName).makeRef()
                                )
                            ),
                            newAssignStmt(newStaticFieldRef(stopRecursionField.makeRef()), IntConstant.v(0)),
                            newReturnStmt(retLocal)
                        ), firstUnit
                    )
                } else {
                    body.units.insertBefore(
                        listOf(
                            newInvokeStmt(
                                newVirtualInvokeExpr(
                                    body.thisLocal,
                                    v.sootClass.getMethodByName(v.subscriberMethodName).makeRef()
                                )
                            ),
                            newAssignStmt(newStaticFieldRef(stopRecursionField.makeRef()), IntConstant.v(0)),
                            newReturnVoidStmt()
                        ), firstUnit
                    )
                }
            }
            firstUnitBox.unit = firstUnit // Doing it *here* triggers Jimple to back-patch.
            body.validate()
        }
    }

    override fun visit(v: ReactivizableFieldInClass) {
        /* Create and initialize a field for the observable. */
        val observerField =
            SootField(v.observerName, RefType.v("io.reactivex.rxjava3.subjects.BehaviorSubject"))
        addFieldToClass(
            v.sootClass,
            Scene.v()
                .getMethod("<io.reactivex.rxjava3.subjects.BehaviorSubject: io.reactivex.rxjava3.subjects.BehaviorSubject create()>"),
            observerField
        )

        /* Call onNext on the observable in the setter (which we assume exists and is used) */
        val setterMethod = v.sootClass.getMethod("set" + v.sootField.name.capitalize(), listOf(v.sootField.type))
        val oldSetterBody = setterMethod.activeBody as JimpleBody
        val newSetterBody = Jimple.v().newBody(oldSetterBody.method)
        val setterObservableLocal =
            Jimple.v().newLocal("observable", RefType.v("io.reactivex.rxjava3.subjects.BehaviorSubject"))
        newSetterBody.locals.addAll(oldSetterBody.locals)
        newSetterBody.locals.add(setterObservableLocal)
        val onNextCall =
            Scene.v().getMethod("<io.reactivex.rxjava3.subjects.BehaviorSubject: void onNext(java.lang.Object)>")

        println("Calling onNext; method is $setterMethod, observer is $observerField")


        newSetterBody.units.addAll(oldSetterBody.units)
        Jimple.v().apply {
            newSetterBody.units.insertAfter(
                listOf(
                    newAssignStmt(
                        setterObservableLocal, newInstanceFieldRef(newSetterBody.thisLocal, observerField.makeRef())
                    ),
                    newInvokeStmt(
                        newVirtualInvokeExpr(
                            setterObservableLocal,
                            onNextCall.makeRef(),
                            newSetterBody.parameterLocals[0]
                        )
                    )
                ), newSetterBody.firstNonIdentityStmt
            )
        }
        oldSetterBody.method.activeBody = newSetterBody
        newSetterBody.validate()
    }

    private fun addFieldToClass(sootClass: SootClass, parameterlessBuilderMethod: SootMethod, field: SootField) {
        sootClass.addField(field)
        modifiedClasses.add(sootClass)

        // Grab parameters
        val namePrefix = "${field.name}_"
        val initBody = sootClass.getMethodByName("<init>").activeBody as JimpleBody
        val tempLocal = Jimple.v()
            .newLocal("${namePrefix}temp", parameterlessBuilderMethod.returnType)
        initBody.locals.add(tempLocal)

        // Update the ctor
        Jimple.v().apply {
            initBody.units.insertBefore(
                listOf(
                    newAssignStmt(tempLocal, newStaticInvokeExpr(parameterlessBuilderMethod.makeRef())),
                    newAssignStmt(newInstanceFieldRef(initBody.thisLocal, field.makeRef()), tempLocal)
                ), initBody.firstNonIdentityStmt
            )
        }
        initBody.fixLateIdentityStatements()
        initBody.validate()
    }
}

open class MyBackwardFlowAnalysis(cfg: UnitGraph) : BackwardFlowAnalysis<Unit, MutableSet<Local>>(cfg) {
    override fun newInitialFlow(): MutableSet<Local> = mutableSetOf()
    override fun copy(source: MutableSet<Local>, dest: MutableSet<Local>) {
        dest.clear()
        dest.addAll(source)
    }

    override fun merge(in1: MutableSet<Local>, in2: MutableSet<Local>, out: MutableSet<Local>) {
        out.clear()
        out.addAll(in1.union(in2)) // may analysis
    }

    override fun flowThrough(input: MutableSet<Local>, d: Unit, out: MutableSet<Local>) {
        val stmt = d as Stmt
        out.clear()
        out.addAll(input)
        if (stmt.containsInvokeExpr()) {
            if (stmt.invokeExpr.method.name == "println" && stmt.invokeExpr.method.declaringClass.name == "java.io.PrintStream") {
                for (v in stmt.invokeExpr.args) {
                    if (v is Local) {
                        out.add(v)
                    }
                }
            }
        }
        if (stmt is AssignStmt) {
            val left = stmt.leftOp as Local
            val right = stmt.rightOp


            out.remove(left)
            if (left in input) {
                when (right) {
                    is InvokeExpr -> {
                        if (right is InstanceInvokeExpr && right.base is Local) {
                            out.add(right.base as Local)

                        }
                    }
                    is FieldRef -> {
                        if (right is InstanceFieldRef && right.base is Local) {
                            out.add(right.base as Local)
                        }
                        /* ??? */
                    }
                    else -> throw IllegalStateException("Unhandled ${right.javaClass.name}")
                }
            }
        }
        if (stmt is ReturnStmt) {
            out.add(stmt.op as Local)
        }

        println("d: $d in: $input out: $out")
    }
}

fun JimpleBody.fixLateIdentityStatements() {
    units.dropWhile { it != firstNonIdentityStmt }.filterIsInstance<IdentityStmt>().forEach {
        units.remove(it)
        units.insertBefore(it, firstNonIdentityStmt)
    }
}