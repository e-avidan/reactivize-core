package il.ac.technion.cs.reactivize.graph

import soot.SootClass
import soot.SootField
import soot.Unit

class ReactivizableFieldInClass(
    val sootField: SootField, val sootClass: SootClass,
    val unitInCallingMethod: Unit,
    val observerName: String,
    override val subunits: List<WorkUnit>
) : WorkUnit {
    override fun accept(visitor: WorkUnitVisitor) {
        visitor.visit(this)
    }

    override fun describeThis(): String =
        "${this::class.simpleName}($sootField, $sootClass, $unitInCallingMethod, $observerName)"
}