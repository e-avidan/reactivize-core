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
            val ret = transform(it.subunits)
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
        // Do I need this? It's supposedly public...
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
        acceptBody.locals.addAll(listOf(acceptOuterThis, acceptObservable))
        val onNextCall =
            Scene.v().getMethod("<io.reactivex.rxjava3.subjects.BehaviorSubject: void onNext(java.lang.Object)>")


        Jimple.v().apply {
            acceptBody.units.addAll(
                listOf(
                    newAssignStmt(acceptOuterThis, newInstanceFieldRef(acceptBody.thisLocal, outerThisField.makeRef())),
                    newAssignStmt(acceptObservable, newInstanceFieldRef(acceptOuterThis, observerField.makeRef())),
                    newInvokeStmt(
                        newVirtualInvokeExpr(
                            acceptObservable,
                            onNextCall.makeRef(),
                            acceptBody.parameterLocals[0]
                        )
                    ),
                    newReturnVoidStmt()
                )
            )
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
            if (field.unitInCallingMethod == null) {
                println("Not handling $field")
                return@forEach
            }
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
        sootClass: SootClass,
        observableFieldName: String,
        namePrefix: String,
        subscriberBody: JimpleBody,
        lambdaClass: SootClass,
        acceptInitMethod: SootMethod,
        instance: Local,
        subscriberMethodName: String? = null
    ): List<Unit> {
        val observable = Jimple.v().newLocal(
            "${namePrefix}observable",
            Scene.v().getSootClass("io.reactivex.rxjava3.subjects.BehaviorSubject").type
        )
        val lambda = Jimple.v().newLocal("${namePrefix}lambda", lambdaClass.type)
        val consumer = Jimple.v()
            .newLocal(
                "${namePrefix}consumer",
                Scene.v().getSootClass("io.reactivex.rxjava3.functions.Consumer").type
            )
        subscriberBody.locals.addAll(listOf(observable, lambda, consumer))
        val memberObservableField = sootClass.getField(
            observableFieldName,
            Scene.v().getSootClass("io.reactivex.rxjava3.subjects.BehaviorSubject").type
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
                newInvokeStmt(
                    newVirtualInvokeExpr(
                        observable, Scene.v().getSootClass("io.reactivex.rxjava3.core.Observable")
                            .getMethod("io.reactivex.rxjava3.disposables.Disposable subscribe(io.reactivex.rxjava3.functions.Consumer)")
                            .makeRef(), listOf(consumer)
                    )
                )
            ) + (if (subscriberMethodName != null) listOf(
                newInvokeStmt(
                    newVirtualInvokeExpr(
                        instance,
                        sootClass.getMethodByName(subscriberMethodName).makeRef()
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
        c.addField(f)
        modifiedClasses.add(c)

        // Initialize the observable
        val namePrefix = "${v.observableName}_"
        val initBody = c.getMethodByName("<init>").activeBody as JimpleBody
        val observableLocal = Jimple.v()
            .newLocal("${namePrefix}observableTemp", RefType.v("io.reactivex.rxjava3.subjects.BehaviorSubject"))
        val observableCall = Scene.v()
            .getMethod("<io.reactivex.rxjava3.subjects.BehaviorSubject: io.reactivex.rxjava3.subjects.BehaviorSubject create()>")
        initBody.locals.add(observableLocal)

        // Update the ctor
        Jimple.v().apply {
            initBody.units.insertBefore(
                listOf(
                    newAssignStmt(observableLocal, newStaticInvokeExpr(observableCall.makeRef())),
                    newAssignStmt(newInstanceFieldRef(initBody.thisLocal, f.makeRef()), observableLocal)
                ), initBody.firstNonIdentityStmt
            )
        }
        initBody.fixLateIdentityStatements()
        initBody.validate() // Kotlin generates code in ctors it doesn't like.

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
            assert(v.subunits.size == 1)
            val valueMethod = v.subunits[0] as ReactivizableMethod
            val method = valueMethod.sootMethod
            val body = Jimple.v().newBody(method)
            method.activeBody = body
            body.insertIdentityStmts()
            Jimple.v().apply {
                if (method.returnType != VoidType.v()) {
                    val retLocal = newLocal("ret", method.returnType)
                    body.locals.add(retLocal)
                    body.units.addAll(
                        listOf(
                            newAssignStmt(
                                retLocal,
                                newVirtualInvokeExpr(
                                    body.thisLocal,
                                    v.sootClass.getMethodByName(v.subscriberMethodName).makeRef()
                                )
                            ),
                            newReturnStmt(retLocal)
                        )
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
                            newReturnVoidStmt()
                        ), body.firstNonIdentityStmt
                    )
                }
            }
            body.validate()
        }
    }

    override fun visit(v: ReactivizableFieldInClass) {
        val observerField =
            SootField(v.observerName, RefType.v("io.reactivex.rxjava3.subjects.BehaviorSubject"))
        v.sootClass.addField(observerField)
        modifiedClasses.add(v.sootClass)

        /* Initialize the observable */
        val initBody = v.sootClass.getMethodByName("<init>").activeBody as JimpleBody
        val observableLocal =
            Jimple.v().newLocal("observableTemp", RefType.v("io.reactivex.rxjava3.subjects.BehaviorSubject"))
        initBody.locals.add(observableLocal)
        val observableCall = Scene.v()
            .getMethod("<io.reactivex.rxjava3.subjects.BehaviorSubject: io.reactivex.rxjava3.subjects.BehaviorSubject create()>")
        initBody.units.insertBefore(
            listOf(
                Jimple.v()
                    .newAssignStmt(
                        observableLocal,
                        Jimple.v().newStaticInvokeExpr(observableCall.makeRef())
                    ),
                Jimple.v()
                    .newAssignStmt(
                        Jimple.v().newInstanceFieldRef(initBody.thisLocal, observerField.makeRef()),
                        observableLocal
                    )
            ), initBody.firstNonIdentityStmt // insert first to avoid understanding control flow
        )
        initBody.fixLateIdentityStatements()
        initBody.validate() // Kotlin generates code this doesn't like.

        /* Call onNext on the observable in the setter (which we assume exists and is used) */
        val oldSetterBody =
            v.sootClass.getMethod("set" + v.sootField.name.capitalize(), listOf(v.sootField.type)).activeBody
        val newSetterBody = Jimple.v().newBody(oldSetterBody.method)
        val setterObservableLocal =
            Jimple.v().newLocal("observable", RefType.v("io.reactivex.rxjava3.subjects.BehaviorSubject"))
        newSetterBody.locals.addAll(oldSetterBody.locals)
        newSetterBody.locals.add(setterObservableLocal)
        val onNextCall =
            Scene.v().getMethod("<io.reactivex.rxjava3.subjects.BehaviorSubject: void onNext(java.lang.Object)>")

        val firstNonAssign = oldSetterBody.units.indexOfFirst { it !is AssignStmt && it !is IdentityStmt }
        println("firstNonAssign: $firstNonAssign")
        println(oldSetterBody.units.map { "$it|${it.javaClass}" })
        newSetterBody.units.addAll(oldSetterBody.units.toList().slice(0 until firstNonAssign))

        newSetterBody.units.add(
            Jimple.v().newAssignStmt(
                setterObservableLocal,
                Jimple.v().newInstanceFieldRef(newSetterBody.thisLocal, observerField.makeRef())
            )
        )
        newSetterBody.units.add(
            Jimple.v().newInvokeStmt(
                Jimple.v()
                    .newVirtualInvokeExpr(setterObservableLocal, onNextCall.makeRef(), newSetterBody.locals.first)
            )
        )
        newSetterBody.units.addAll(
            oldSetterBody.units.toList().slice(firstNonAssign until oldSetterBody.units.size)
        )
        oldSetterBody.method.activeBody = newSetterBody
        newSetterBody.validate()
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