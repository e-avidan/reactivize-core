package il.ac.technion.cs.reactivize.helpers

import soot.SootMethod
import soot.toolkits.graph.DirectedGraph
import soot.toolkits.scalar.FlowAnalysis

class MultiMethodFlowAnalysis<N, A>(
    val loadMethod: (SootMethod) -> DirectedGraph<N>,
    val runAnalysis: (SootMethod, DirectedGraph<N>, MultiMethodFlowAnalysis<N, A>) -> FlowAnalysis<N, A>
) {
    val methods: MutableMap<SootMethod, DirectedGraph<N>> = HashMap()
    val analysisResults: MutableMap<DirectedGraph<N>, FlowAnalysis<N, A>> = HashMap()

    fun analyze(method: SootMethod): A {
        var graph = methods[method]

        // Haven't seen this method before
        if (graph == null) {
            println("Analyzing $method")

            graph = loadMethod(method)

            // TODO: recursion
            analysisResults[graph] = runAnalysis(method, graph, this)
        }

        return getAnalysisResult(graph)
    }

    private fun getAnalysisResult(graph: DirectedGraph<N>): A {
        if (graph.tails.size != 1) {
            throw Exception("Expected a single result flow")
        }

        val analysis = analysisResults[graph]!!
        val resultingFlows = graph.tails.map { analysis.getFlowAfter(it) }
        return resultingFlows[0]
    }
}
