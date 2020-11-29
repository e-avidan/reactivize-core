package il.ac.technion.cs.reactivize.pta

import soot.SootClass
import soot.Unit
import soot.Value
import soot.jimple.internal.AbstractInvokeExpr

data class PTANode(
    val target: Value,
    val type: SootClass?,
    val value: Value? = null,
    val unit: Unit? = null
) {
    // TODO: maybe encapsulate these, to prevent problems down the line
    // Anything mutable should not be Data, since it'll ruin hashing
    var children: MutableSet<PTANode> = HashSet()
    var methodCall: PTAGraph? = null
    var methodArgIndex: Int = -1

    fun cloneWithSubgraph(copyNode: (PTANode) -> PTANode): PTANode {
        val node = copy()
        node.children = HashSet()
        node.methodCall = methodCall
        node.methodArgIndex = methodArgIndex

        children.map { copyNode(it) }.forEach { node.children.add(it) }

        return node
    }

    fun isImportant(options: PTAOptions): Boolean {
        return isSelfImportant(options) || children.any { it.isImportant(options) }
    }

    // TODO: cache with options? i.e. lazy
    fun isSelfImportant(options: PTAOptions): Boolean {
//        return (type != null && options.analysisClasses.contains(type))
        // TODO: uglyyyy
        return unit != null && unit.useBoxes.map { it.value }
            .any { it is AbstractInvokeExpr && options.analysisMethods.contains(it.method) }
    }
}
