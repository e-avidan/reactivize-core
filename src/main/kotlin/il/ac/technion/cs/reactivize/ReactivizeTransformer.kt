package il.ac.technion.cs.reactivize

import soot.*
import soot.Unit
import soot.jimple.*
import soot.tagkit.VisibilityAnnotationTag
import soot.toolkits.graph.ExceptionalUnitGraph
import soot.toolkits.graph.UnitGraph
import soot.toolkits.scalar.BackwardFlowAnalysis

class ReactivizeTransformer {
    fun transform(): Iterable<SootClass> {
        return findAnnotatedMethods().flatMap(::handleAnnotatedMethod)
    }

    private fun findAnnotatedMethods() =
        Scene.v().applicationClasses.flatMap { it.methods }.filter { m ->
            m.tags.filterIsInstance<VisibilityAnnotationTag>().flatMap { it.annotations }.map { it.type }
                .any { it.contains("Reactivize") }
        }


    fun handleAnnotatedMethod(m: SootMethod): List<SootClass> {
        val body = m.activeBody as JimpleBody
        val cfg = ExceptionalUnitGraph(body)
        val analysis = object : MyBackwardFlowAnalysis(cfg) {
            init {
                doAnalysis()
            }
        }

        val map = mutableMapOf<SootClass, List<SootField>>()

        body.units.forEach { unit ->
            val stmt = unit as Stmt
            if (stmt.containsInvokeExpr() && stmt.invokeExpr is InstanceInvokeExpr) {
                val invokeExpr = stmt.invokeExpr as InstanceInvokeExpr
                if (invokeExpr.base in analysis.getFlowBefore(unit)) {
                    map.putAll(handleSourceMethod(invokeExpr.method))
                }
            }
        }

        val subscriberMethod = SootMethod(m.name + "\$subscriber", listOf(), VoidType.v())
        val subscriberBody = Jimple.v().newBody(subscriberMethod)
        subscriberMethod.activeBody = subscriberBody
        m.declaringClass.addMethod(subscriberMethod)
        subscriberBody.units.addAll(m.activeBody.units)
        subscriberBody.locals.addAll(m.activeBody.locals)

        val lambdaClass =
            SootClass(
                m.declaringClass.javaPackageName + "." + m.declaringClass.javaStyleName + "$" + m.name + "$" + "reactivize" + "$" + "1",
                Modifier.PUBLIC
            )
        lambdaClass.superclass = Scene.v().getSootClass("java.lang.Object")
        lambdaClass.addInterface(Scene.v().getSootClass("io.reactivex.rxjava3.functions.Consumer"))
        lambdaClass.outerClass = m.declaringClass
        Scene.v().addClass(lambdaClass)

        val outerThisField = SootField("this\$0", m.declaringClass.type)
        lambdaClass.addField(outerThisField)

        val acceptMethod =
            SootMethod("accept", listOf(RefType.v("java.lang.Object")), VoidType.v(), Modifier.PUBLIC or Modifier.FINAL)
        lambdaClass.addMethod(acceptMethod)
        val acceptBody = Jimple.v().newBody(acceptMethod)
        acceptMethod.activeBody = acceptBody

        val acceptThis = Jimple.v().newLocal("this", lambdaClass.type)
        acceptBody.locals.add(acceptThis)
        val acceptOuterThis = Jimple.v().newLocal("\$stack1", m.declaringClass.type)
        acceptBody.locals.add(acceptOuterThis)

        acceptBody.units.add(Jimple.v().newIdentityStmt(acceptThis, Jimple.v().newThisRef(lambdaClass.type)))
        acceptBody.units.add(
            Jimple.v().newAssignStmt(
                acceptOuterThis,
                Jimple.v().newInstanceFieldRef(acceptThis, outerThisField.makeRef())
            )
        )
        acceptBody.units.add(
            Jimple.v().newInvokeStmt(
                Jimple.v()
                    .newVirtualInvokeExpr(
                        acceptOuterThis,
                        m.declaringClass.getMethodByName(m.name + "\$subscriber").makeRef()
                    )
            )
        )
        acceptBody.units.add(Jimple.v().newReturnVoidStmt())

        val acceptInitMethod = SootMethod("<init>", listOf(m.declaringClass.type), VoidType.v())
        val acceptInitBody = Jimple.v().newBody(acceptInitMethod)
        acceptInitMethod.activeBody = acceptInitBody
        acceptInitBody.apply {
            val l0 = Jimple.v().newLocal("l0", lambdaClass.type)
            val l1 = Jimple.v().newLocal("l1", m.declaringClass.type)
            locals.add(l0)
            locals.add(l1)

            units.add(Jimple.v().newIdentityStmt(l0, Jimple.v().newThisRef(lambdaClass.type)))
            units.add(Jimple.v().newIdentityStmt(l1, Jimple.v().newParameterRef(m.declaringClass.type, 0)))
            units.add(Jimple.v().newAssignStmt(Jimple.v().newInstanceFieldRef(l0, outerThisField.makeRef()), l1))
            units.add(
                Jimple.v().newInvokeStmt(
                    Jimple.v()
                        .newSpecialInvokeExpr(
                            l0,
                            Scene.v().getSootClass("java.lang.Object").getMethodByName("<init>").makeRef()
                        )
                )
            )
            units.add(Jimple.v().newReturnVoidStmt())
        }
        lambdaClass.addMethod(acceptInitMethod)

        val newBody = Jimple.v().newBody(m)
        m.activeBody = newBody
        newBody.apply {
            locals.add(Jimple.v().newLocal("this", m.declaringClass.type))
            units.add(Jimple.v().newIdentityStmt(locals.first, Jimple.v().newThisRef(m.declaringClass.type)))
        }

        for ((cls, fields) in map) {
            for (field in fields) {
                val localField = m.declaringClass.fields.find { it.type == cls.type }!!
                val thing = Jimple.v().newLocal("thing", cls.type)
                newBody.locals.add(thing)
                val observable = Jimple.v().newLocal(
                    "observable",
                    Scene.v().getSootClass("io.reactivex.rxjava3.subjects.BehaviorSubject").type
                )
                newBody.locals.add(observable)
                val lambda = Jimple.v().newLocal("lambda", lambdaClass.type)
                newBody.locals.add(lambda)
                val consumer = Jimple.v()
                    .newLocal("consumer", Scene.v().getSootClass("io.reactivex.rxjava3.functions.Consumer").type)
                newBody.locals.add(consumer)

                newBody.units.apply {
                    add(
                        Jimple.v().newAssignStmt(
                            thing,
                            Jimple.v().newInstanceFieldRef(newBody.thisLocal, localField.makeRef())
                        )
                    )
                    add(Jimple.v().newAssignStmt(observable, Jimple.v().newInstanceFieldRef(thing, field.makeRef())))
                    add(Jimple.v().newAssignStmt(lambda, Jimple.v().newNewExpr(lambdaClass.type)))
                    add(
                        Jimple.v().newInvokeStmt(
                            Jimple.v()
                                .newSpecialInvokeExpr(lambda, acceptInitMethod.makeRef(), listOf(newBody.thisLocal))
                        )
                    )
                    add(
                        Jimple.v().newAssignStmt(
                            consumer,
                            Jimple.v().newCastExpr(
                                lambda,
                                Scene.v().getSootClass("io.reactivex.rxjava3.functions.Consumer").type
                            )
                        )
                    )
                    add(
                        Jimple.v().newInvokeStmt(
                            Jimple.v().newVirtualInvokeExpr(
                                consumer,
                                Scene.v().getSootClass("io.reactivex.rxjava3.core.Observable")
                                    .getMethod("io.reactivex.rxjava3.disposables.Disposable subscribe(io.reactivex.rxjava3.functions.Consumer)")
                                    .makeRef(),
                                listOf(consumer)
                            )
                        )
                    )
                }
            }
        }

        newBody.units.add(Jimple.v().newReturnVoidStmt())

        println(newBody)

        return listOf(lambdaClass, m.declaringClass)
    }

    fun handleSourceMethod(m: SootMethod): Map<SootClass, List<SootField>> {
        println(m)

        val body = m.activeBody as JimpleBody
        val cfg = ExceptionalUnitGraph(body)
        val analysis = object : MyBackwardFlowAnalysis(cfg) {
            init {
                doAnalysis()
            }
        }

        val fields = mutableListOf<SootField>()
        // TODO(mip): Move to ReactivizeAnalyzer or something like that?
        body.units.forEach { unit ->
            val stmt = unit as Stmt
            if (stmt is AssignStmt) {
                if (stmt.containsFieldRef() and (stmt.leftOp in analysis.getFlowAfter(stmt))) {
                    val fieldRef = stmt.fieldRef
                    if (fieldRef is InstanceFieldRef) {
                        val base = fieldRef.base as Local
                        if (base.name == "this") {
                            fields.add(fieldRef.field)
                        }
                    }
                }
            }
        }

        if (fields.map { it.declaringClass }.toSet().size != 1) {
            throw IllegalStateException("There was supposed to be only one class here $fields")
        }
        val cls = fields.map { it.declaringClass }.toSet().first()

        return mapOf(Pair(cls, handleClass(cls, fields)))
    }

    fun handleClass(cls: SootClass, fields: MutableList<SootField>): List<SootField> {
        println(cls.methods)
        println(cls.fields)

        val ret = mutableListOf<SootField>()

        for (field in fields) {
            /* Create a new observable */
            val newField =
                SootField(field.name + "\$Observable", RefType.v("io.reactivex.rxjava3.subjects.BehaviorSubject"))
            cls.addField(newField)
            ret.add(newField)

            /* Initialize the observable */
            val initBody = cls.getMethodByName("<init>").activeBody
            val fieldLocal = Jimple.v().newLocal("newField", field.type)
            val observableLocal =
                Jimple.v().newLocal("observable", RefType.v("io.reactivex.rxjava3.subjects.BehaviorSubject"))
            initBody.locals.add(fieldLocal)
            initBody.locals.add(observableLocal)
            val observableCall = Scene.v()
                .getMethod("<io.reactivex.rxjava3.subjects.BehaviorSubject: io.reactivex.rxjava3.subjects.BehaviorSubject createDefault(java.lang.Object)>")
            initBody.units.add(
                Jimple.v()
                    .newAssignStmt(fieldLocal, Jimple.v().newInstanceFieldRef(initBody.thisLocal, field.makeRef()))
            )
            initBody.units.add(
                Jimple.v()
                    .newAssignStmt(observableLocal, Jimple.v().newStaticInvokeExpr(observableCall.makeRef()))
            )
            initBody.units.add(
                Jimple.v()
                    .newAssignStmt(
                        Jimple.v().newInstanceFieldRef(initBody.thisLocal, newField.makeRef()),
                        observableLocal
                    )
            )

            /* Call onNext on the observable in the setter */
            val oldSetterBody = cls.getMethod("set" + field.name.capitalize(), listOf(field.type)).activeBody
            val newSetterBody = Jimple.v().newBody(oldSetterBody.method)
            val setterObservableLocal =
                Jimple.v().newLocal("observable", RefType.v("io.reactivex.rxjava3.subjects.BehaviorSubject"))
            newSetterBody.locals.addAll(oldSetterBody.locals)
            newSetterBody.locals.add(setterObservableLocal)
            val onNextCall =
                Scene.v().getMethod("<io.reactivex.rxjava3.subjects.BehaviorSubject: void onNext(java.lang.Object)>")
            newSetterBody.units.addAll(
                oldSetterBody.units.reversed().slice(1 until oldSetterBody.units.size).reversed()
            )
            newSetterBody.units.add(
                Jimple.v().newAssignStmt(
                    setterObservableLocal,
                    Jimple.v().newInstanceFieldRef(newSetterBody.thisLocal, newField.makeRef())
                )
            )
            newSetterBody.units.add(
                Jimple.v().newInvokeStmt(
                    Jimple.v()
                        .newVirtualInvokeExpr(setterObservableLocal, onNextCall.makeRef(), newSetterBody.locals.first)
                )
            )
            newSetterBody.units.add(oldSetterBody.units.last)
            assert(oldSetterBody.units.last is ReturnStmt)
            oldSetterBody.method.activeBody = newSetterBody

            println(initBody)
            println(newSetterBody)
        }

        return ret
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