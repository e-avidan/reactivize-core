package il.ac.technion.cs.reactivize.graph

import soot.SootMethod

class FunctionCallingMethod(
    override val sootMethod: SootMethod,
    override val subscriberMethodName: String,
    override val subunits: List<WorkUnit>
) : ReactivizableMethod {
    override fun accept(visitor: WorkUnitVisitor) {
        visitor.visit(this)
    }
}