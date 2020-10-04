package il.ac.technion.cs.reactivize.graph

import soot.SootClass
import soot.SootMethod
import soot.Unit

class ClassWithObservable(
    val sootClass: SootClass,
    val callingMethod: SootMethod?,
    val unitInCallingMethod: Unit?,
    val observableFieldName: String,
    override val subunits: List<WorkUnit>
) : WorkUnit {
    override fun accept(visitor: WorkUnitVisitor) {
        visitor.visit(this)
    }

    override fun describeThis(): String =
        "${this::class.simpleName}(${sootClass.name}, ${callingMethod?.name} ($unitInCallingMethod) ${observableFieldName})"
}