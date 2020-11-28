package il.ac.technion.cs.reactivize.pta

import il.ac.technion.cs.reactivize.helpers.SootUtil

object PTA {
    fun run(options: PTAOptions) {
        val graph = SootUtil.loadMethod(options.methodSignature, options.className)
        val analysis = PTAForwardFlowAnalysis(graph)
        println (analysis)
    }
}
