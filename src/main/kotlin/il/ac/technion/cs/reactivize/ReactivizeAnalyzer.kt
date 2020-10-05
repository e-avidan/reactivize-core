package il.ac.technion.cs.reactivize

import il.ac.technion.cs.reactivize.api.TransformedMarker
import il.ac.technion.cs.reactivize.graph.*
import soot.*
import soot.Unit
import soot.jimple.*
import soot.tagkit.AnnotationStringElem
import soot.tagkit.AnnotationTag
import soot.tagkit.VisibilityAnnotationTag
import soot.toolkits.graph.ExceptionalUnitGraph

class ReactivizeAnalyzer(val spec: ReactivizeCompileSpec) {
    fun createGraph(): List<WorkUnit> = findAnnotatedMethods().flatMap(this::analyzeAnnotatedMethod)

    fun analyzeMethod(
        m: SootMethod,
        observableCandidateName: String?,
        callingMethod: SootMethod?,
        unitInCallingMethod: Unit?
    ): List<WorkUnit> {
        val body = m.activeBody as JimpleBody
        val analysis = object : MyBackwardFlowAnalysis(ExceptionalUnitGraph(body)) {
            init {
                doAnalysis()
            }
        }

        val observableName = observableCandidateName ?: createObservableName(m)
        // FIXME: Handle case where this name is already taken.
        val subscriberMethodName = m.name + "\$subscriber"

        return listOf(
            ClassWithObservable(
                m.declaringClass, unitInCallingMethod, observableName, subscriberMethodName, listOf(
                    ValueMethod(
                        m,
                        subscriberMethodName,
                        observableName,
                        analyzeMethodInstructions(body, analysis)
                    )
                )
            )
        )
    }

    fun analyzeAnnotatedMethod(m: SootMethod): List<WorkUnit> {
        val observableCandidateName = findRequestedObservableName(m)
        val body = m.activeBody as JimpleBody
        val analysis = object : MyBackwardFlowAnalysis(ExceptionalUnitGraph(body)) {
            init {
                doAnalysis()
            }
        }
        // FIXME: Handle case where this name is already taken.
        val subscriberMethodName = m.name + "\$subscriber"

        return listOf(
            if (observableCandidateName != null) {
                ClassWithObservable(
                    m.declaringClass,
                    null,
                    observableCandidateName,
                    subscriberMethodName,
                    listOf(
                        ValueMethod(
                            m,
                            subscriberMethodName,
                            observableCandidateName,
                            analyzeMethodInstructions(body, analysis)
                        )
                    )
                )
            } else {

                ClassWithObservable(
                    m.declaringClass,
                    null,
                    "???",
                    subscriberMethodName,
                    listOf(FunctionCallingMethod(m, subscriberMethodName, analyzeMethodInstructions(body, analysis)))
                )
            }
        )
    }

    private fun analyzeMethodInstructions(
        body: JimpleBody,
        analysis: MyBackwardFlowAnalysis
    ): List<WorkUnit> {
        /*val pathAnalysis = object : FindPathDataFlowAnalysis(ExceptionalUnitGraph(body)) {
            init {
                doAnalysis()
            }
        }*/
        return sequence {
            yieldAll(body.units.asSequence().filterIsInstance<Stmt>()
                .filterIsInstance<AssignStmt>()
                .filter { it.containsFieldRef() && it.leftOp in analysis.getFlowAfter(it) }
                .map { Pair(it, it.fieldRef) }
                .filterIsInstance<Pair<AssignStmt, InstanceFieldRef>>()
                .filter { (it.second.base as Local).name == "this" }
                .filter { // Make sure it has a setter (i.e., it's mutable)
                    try {
                        body.method.declaringClass.getMethod(
                            "set" + it.second.field.name.capitalize(),
                            listOf(it.second.type)
                        ) != null
                    } catch (e: Exception) {
                        false
                    }
                }
                .map {
                    ReactivizableFieldInClass(
                        it.second.field,
                        body.method.declaringClass,
                        it.first,
                        createObservableName(it.second.field),
                        listOf()
                    )
                }
            )
            yieldAll(body.units.asSequence().filterIsInstance<Stmt>()
                .filter { it.containsInvokeExpr() && it.invokeExpr is InstanceInvokeExpr }
                .map { Pair(it, it.invokeExpr as InstanceInvokeExpr) }
                .filter { it.second.base in analysis.getFlowBefore(it.first) }
                .flatMap {
                    val method = it.second.method
                    val sootClass = method.declaringClass

                    if (null != spec.applicationClassPackagePrefixes.find { prefix ->
                            sootClass.packageName.startsWith(prefix)
                        }) {
                        // FIXME: fieldInParent shouldn't be null, it should be a path to the the instance.
                        println("Analyze method: $method")
                        println("Path analysis: ${analysis.getFlowAfter(it.first)}")
                        analyzeMethod(
                            method,
                            findRequestedObservableName(method),
                            body.method,
                            it.first
                        ).asSequence()
                    } else {
                        listOf<ReactivizableFieldInClass>().asSequence()
                    }
                })
        }.toList()
    }

    private fun findAnnotatedMethods(): Collection<SootMethod> =
        Scene.v().applicationClasses.flatMap { it.methods }.filter { m ->
            m.tags.filterIsInstance<VisibilityAnnotationTag>().flatMap { it.annotations }.map { it.type }
                .any { AnnotationKind.parse(it) != null }
        }.filterNot { m ->
            m.tags.filterIsInstance<VisibilityAnnotationTag>().flatMap { it.annotations }.map { it.type }
                .any { it == TransformedMarker::class.bytecodeName }
        }.map { println(it); it }

    private fun findAnnotations(m: SootMethod): List<Pair<AnnotationTag, AnnotationKind?>> =
        m.tags.filterIsInstance<VisibilityAnnotationTag>()
            .flatMap(VisibilityAnnotationTag::getAnnotations)
            .map { it to AnnotationKind.parse(it.type) }
            .filter { it.second != null }

    private fun findRequestedObservableName(m: SootMethod): String? =
        findAnnotations(m).findLast { it.second == AnnotationKind.REACTIVIZE_VALUE }?.first?.elems?.filterIsInstance<AnnotationStringElem>()
            ?.find { it.kind == 's' && it.name == "observableField" }?.value

    private fun createObservableName(m: SootMethod): String =
        createObservableName("${m.name}\$m\$observable", m.declaringClass)

    private fun createObservableName(f: SootField): String =
        createObservableName("${f.name}\$f\$observable", f.declaringClass)

    private fun createObservableName(c: String, declaringClass: SootClass): String {
        if (c in declaringClass.fields.map(SootField::getName)) {
            for (i in 0..50) {
                val alternate = "$c\$$i"
                if (alternate !in declaringClass.fields.map(SootField::getName))
                    return alternate
            }
        } else {
            return c
        }
        throw RuntimeException("Couldn't create observable name ($c in $declaringClass)")
    }
}