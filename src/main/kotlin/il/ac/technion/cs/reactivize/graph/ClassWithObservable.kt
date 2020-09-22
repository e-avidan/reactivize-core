package il.ac.technion.cs.reactivize.graph

import soot.SootClass
import soot.SootField

class ClassWithObservable(
    val sootClass: SootClass,
    val fieldInParent: SootField?,
    val observableFieldName: String,
    override val subunits: List<WorkUnit>
) : WorkUnit {
    override fun accept(visitor: WorkUnitVisitor) {
        visitor.visit(this)
    }

    override fun describeThis(): String =
        "${this::class.simpleName}(${sootClass.name}, ${fieldInParent?.declaringClass?.name}::${fieldInParent?.name} ${observableFieldName})"
}