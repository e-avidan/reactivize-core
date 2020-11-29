package il.ac.technion.cs.reactivize.pta

import il.ac.technion.cs.reactivize.helpers.MultiMethodFlowAnalysis
import il.ac.technion.cs.reactivize.helpers.SootUtil
import soot.SootMethod
import soot.toolkits.graph.DirectedGraph
import soot.Unit

object PTA {
    fun run(options: PTAOptions): PTAGraph {
        val initialMethod = SootUtil.resolveMethod(options.methodSignature, options.className)
        val analysisContainer = MultiMethodFlowAnalysis(
            loadMethod = SootUtil::loadMethod,
            runAnalysis = { method: SootMethod, graph: DirectedGraph<Unit>, container: MultiMethodFlowAnalysis<Unit, PTAGraph> ->
                PTAForwardFlowAnalysis(
                    method,
                    graph,
                    options,
                    container
                )
            }
        )

        val result = analysisContainer.analyze(initialMethod)
        val finalResult = result.postProcess()  // TODO: maybe this shouldn't be here

        return finalResult
    }
}
