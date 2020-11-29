package il.ac.technion.cs.reactivize.pta

import soot.SootClass
import soot.Unit
import soot.Value
import java.lang.Exception

/*
 */
class PTAGraph(val options: PTAOptions) {
    // TODO: SSA much? what about branching... not even shimple will save us from that...
    var values: MutableMap<Value, PTANode> = HashMap()
    var roots: MutableSet<PTANode> = HashSet()

    /*
    Harder op since we may have to change the existing structures here
     */
    fun merge(src: PTAGraph) {
        // TODO
    }

    fun copy(src: PTAGraph) {
        src.values.forEach { (value, originalNode) ->
            val node = copyNode(originalNode)
            values[value] = node

            if (src.roots.contains(originalNode)) {
                roots.add(node)
            }
        }
    }

    private fun copyNode(original: PTANode): PTANode {
        // Check if we already copied it
        val existingNode = if(original.target != null) values[original.target] else  null
        if (existingNode != null) {
            return existingNode
        }

        val node = original.copy()
        node.children = HashSet()

        original.children.map { copyNode(it) }.forEach { node.children.add(it) }
        // TODO

        return node
    }

    /*
    Create final graph
     */
    fun postProcess(): PTAGraph {
        val result = PTAGraph(options)
        result.copy(this)

        result.trim()
        return result
    }

    private fun trim() {
        val obsoleteRoots = roots.filter { !it.isImportant(options) }
        obsoleteRoots.forEach { roots.remove(it) }
    }

    override fun equals(other: Any?): Boolean {
        // TODO
        return super.equals(other)
    }

    override fun hashCode(): Int {
        // TODO
        return super.hashCode()
    }

    /*
    For writes
     */
    fun introduce(target: Value, value: Value, type: SootClass?, isRoot: Boolean): PTANode {
        if (values.containsKey(target)) {
            // TODO: for now this doesn't matter, but let's be aware of this
            println("WARNING: $target already exists")
            return values[target]!!
        }

        val node = PTANode(target, value = value, type = type)
        values[target] = node

        if (isRoot) {
            roots.add(node)
        }

        return node
    }

    /*
    For reads
    TODO: add context depth
     */
    fun link(value: Value, unit: Unit): PTANode {
        val existingNode = values[value] ?: throw Exception("$value does not exist")

        val node = PTANode(value, unit = unit, type=null)
        existingNode.children.add(node)

        return node
    }
}
