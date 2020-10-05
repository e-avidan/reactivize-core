package il.ac.technion.cs.reactivize.graph

import soot.SootClass
import soot.Unit

class ClassWithObservable(
    val sootClass: SootClass,
    val unitInCallingMethod: Unit?,
    val observableFieldName: String,
    val subscriberMethodName: String,
    override val subunits: List<WorkUnit>
) : WorkUnit {
    override fun accept(visitor: WorkUnitVisitor) {
        visitor.visit(this)
    }

    override fun describeThis(): String =
        "${this::class.simpleName}(${sootClass.name}, $unitInCallingMethod, $observableFieldName, $subscriberMethodName)"
}