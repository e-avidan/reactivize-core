package il.ac.technion.cs.reactivize.pta

import il.ac.technion.cs.reactivize.helpers.MultiMethodFlowAnalysis
import soot.SootClass
import soot.SootMethod
import soot.Unit
import soot.Value
import soot.jimple.ParameterRef
import java.lang.Exception

/*
 */
class PTAGraph(
    val method: SootMethod,
    val options: PTAOptions,
    val analysisContainer: MultiMethodFlowAnalysis<Unit, PTAGraph>
) {
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
        val existingNode = if (original.target != null) values[original.target] else null
        if (existingNode != null) {
            return existingNode
        }

        return original.cloneWithSubgraph(this::copyNode)
    }

    /*
    Create final graph
     */
    fun postProcess(): PTAGraph {
        val result = PTAGraph(method, options, analysisContainer)
        result.copy(this)

        result.roots.forEach(result::bridgeMethodCalls)
        result.trim()

        return result
    }

    /*
    Links between parameter and argument for method calls
     */
    private fun bridgeMethodCalls(node: PTANode) {
        if (node.methodCall == null) {
            node.children.forEach(this::bridgeMethodCalls)
            return
        }

        if (node.methodArgIndex < 0) {
            throw Exception("Negative arg index ${node.methodArgIndex}")
        }

        if (node.children.isNotEmpty()) {
            throw Exception("Method call with children is unheard of *gasp*")
        }

        // TODO: this is an evil hack
        val parameterNodeContenders = node.methodCall!!.roots.filter { it.value is ParameterRef && it.value.index == node.methodArgIndex }

        if (parameterNodeContenders.size != 1) {
            throw Exception("Something went wrong..")
        }

        // TODO: this is not quite right, specifically the naming will be off
        node.children = parameterNodeContenders[0].children

        // Cleanup so we don't do this again
        node.methodCall = null
        node.methodArgIndex = -1

        // And continue down the rabbit hole :-)
        node.children.forEach(this::bridgeMethodCalls)
    }

    /*
    Prunes out everything that doesn't include the relevant data along it's control path
     */
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

        // TODO: unit from stmt
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
    fun link(value: Value, unit: Unit, unitMethodCalls: Map<SootMethod, List<Value?>>?): PTANode {
        val parent = values[value] ?: throw Exception("$value does not exist")
        val node = PTANode(value, unit = unit, type = null)

        if (parent.children.contains(node)) {
            throw Exception("Trying to add the same child $node multiple times (QQ - will this happen?)")
        }

        linkToMethod(node, unitMethodCalls)

        parent.children.add(node)
        return node
    }

    /*
    For linking to sub-methods
     */
    private fun linkToMethod(node: PTANode, unitMethodCalls: Map<SootMethod, List<Value?>>?) {
        if (unitMethodCalls == null) {
            return
        }

        if (unitMethodCalls.size != 1) {
            throw Exception("Cannot link to multiple methods at once")
        }

        unitMethodCalls.forEach { (method, args) ->
            val methodGraph = analysisContainer.analyze(method)

            // TODO: I don't like this solution..
            node.methodCall = methodGraph
            node.methodArgIndex = args.indexOf(node.target)

            if (node.methodArgIndex == -1) {
                throw Exception("Something went wrong with current inter procedural implementation")
            }

//            val existingCalls = RCTUtil.getNestedMapSet(methodCalls, method)
//            existingCalls.add(args)
        }
    }
}
