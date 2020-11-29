package il.ac.technion.cs.reactivize.pta

import soot.SootClass
import soot.Unit
import soot.Value

data class PTANode(
    val target: Value,
    val type: SootClass?,
    val value: Value? = null,
    val unit: Unit? = null
) {
    // Anything mutable should not be Data, since it'll ruin hashing
    var children: MutableSet<PTANode> = HashSet()

    fun isImportant(options: PTAOptions): Boolean {
        return isSelfImportant(options) || children.any { it.isImportant(options) }
    }

    fun isSelfImportant(options: PTAOptions): Boolean {
        // TODO: can also trim based on method
        return (type != null && options.analysisClasses.contains(type))
    }
}
