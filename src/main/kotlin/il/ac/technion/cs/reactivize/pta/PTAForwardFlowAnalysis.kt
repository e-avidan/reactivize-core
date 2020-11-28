package il.ac.technion.cs.reactivize.pta

import soot.Unit
import soot.toolkits.graph.DirectedGraph
import soot.toolkits.scalar.ForwardFlowAnalysis

class PTAForwardFlowAnalysis(graph: DirectedGraph<Unit>) :
    ForwardFlowAnalysis<Unit, PTAGraph>(graph) {

    init {
        doAnalysis()
    }

    override fun newInitialFlow(): PTAGraph {
        return PTAGraph()
    }

    override fun merge(in1: PTAGraph, in2: PTAGraph, out: PTAGraph) {
        out.merge(in1)
        out.merge(in2)
    }

    override fun copy(source: PTAGraph, dest: PTAGraph) {
        dest.copy(source)
    }

    override fun flowThrough(`in`: PTAGraph, d: Unit, out: PTAGraph) {
        out.copy(`in`)

        d.apply { StatementVisitor(out) }
    }
}
