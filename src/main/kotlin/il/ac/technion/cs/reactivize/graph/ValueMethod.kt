package il.ac.technion.cs.reactivize.graph

import soot.SootMethod

class ValueMethod(
    override val sootMethod: SootMethod,
    override val subscriberMethodName: String,
    val observableName: String,
    override val subunits: List<WorkUnit>
) : ReactivizableMethod {
    override fun accept(visitor: WorkUnitVisitor) {
        visitor.visit(this)
    }

    override fun describeThis(): String =
        "${this::class.simpleName}(${sootMethod}, ${subscriberMethodName}, ${observableName})"
}