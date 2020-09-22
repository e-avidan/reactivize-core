package il.ac.technion.cs.reactivize.graph

import soot.SootMethod

class FunctionCallingMethod(
    val sootMethod: SootMethod,
    val subscriberMethodName: String,
    override val subunits: List<WorkUnit>
) : ReactivizableMethod {
    override fun accept(visitor: WorkUnitVisitor) {
        visitor.visit(this)
    }
}