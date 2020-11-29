package il.ac.technion.cs.reactivize.pta

import il.ac.technion.cs.reactivize.helpers.MultiMethodFlowAnalysis
import il.ac.technion.cs.reactivize.pta.visitors.PTAStatementVisitor
import soot.SootMethod
import soot.Unit
import soot.toolkits.graph.DirectedGraph
import soot.toolkits.scalar.ForwardFlowAnalysis

class PTAForwardFlowAnalysis(
    val method: SootMethod,
    graph: DirectedGraph<Unit>,
    val options: PTAOptions,
    val analysisContainer: MultiMethodFlowAnalysis<Unit, PTAGraph>
) :
    ForwardFlowAnalysis<Unit, PTAGraph>(graph) {

    init {
        doAnalysis()
    }

    override fun newInitialFlow(): PTAGraph {
        return PTAGraph(method, options, analysisContainer)
    }

    override fun merge(in1: PTAGraph, in2: PTAGraph, out: PTAGraph) {
        out.merge(in1)
        out.merge(in2)
    }

    override fun copy(source: PTAGraph, dest: PTAGraph) {
        dest.copy(source)
    }

    override fun flowThrough(`in`: PTAGraph, d: Unit, out: PTAGraph) {
        copy(`in`, out)

        d.apply(PTAStatementVisitor(out))
    }
}
