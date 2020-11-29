package il.ac.technion.cs.reactivize.pta

import il.ac.technion.cs.reactivize.helpers.SootUtil

object PTA {
    fun run(options: PTAOptions): PTAGraph {
        val unitGraph = SootUtil.loadMethod(options.methodSignature, options.className)
        val analysis = PTAForwardFlowAnalysis(unitGraph, options)

        if (unitGraph.tails.size != 1) {
            throw Exception("Expected a single result flow")
        }

        val resultingFlows = unitGraph.tails.map { analysis.getFlowAfter(it).postProcess() }
        return resultingFlows[0]
    }
}
