package il.ac.technion.cs.reactivize.pta

import il.ac.technion.cs.reactivize.helpers.SootUtil
import soot.Unit
import soot.toolkits.graph.DirectedGraph
import soot.toolkits.graph.UnitGraph
import soot.toolkits.scalar.ForwardFlowAnalysis
import soot.util.Switch

class PtgForwardAnalysis(
    methodSignature: String?,
    className: String?,
    var ptg: PointsToGraph,
    var analysisType: AnalysisType
) : ForwardFlowAnalysis<Unit, PointsToGraph>(Init(methodSignature, className)) {
    enum class AnalysisType {
        E1, E2, E4
    }

    companion object {
        private fun Init(methodSignature: String?, className: String?): UnitGraph {
            return SootUtil.loadMethod(methodSignature, className)
        }
    }

    // Merge final de todos los analisis.
    val pointsToGraph: PointsToGraph
        get() {
            // Merge final de todos los analisis.
            val ptg = PointsToGraph()
            for (unit in (graph as UnitGraph).tails) {
                (getFlowAfter(unit) as PointsToGraph?)!!.merge(ptg)
            }
            return ptg
        }

    init {
        val beforeAnalysisMethod = ptg.methodSignature
        val beforeAnalysisClass = ptg.className
        ptg.methodSignature = methodSignature
        ptg.className = className
        ptg.addThis()
        doAnalysis()
        ptg.methodSignature = beforeAnalysisMethod
        ptg.className = beforeAnalysisClass
    }

    override fun newInitialFlow(): PointsToGraph {
        val initPtg = PointsToGraph()
        ptg.copy(initPtg)
        return initPtg
    }

    override fun copy(source: PointsToGraph, dest: PointsToGraph) {
        source.copy(dest)
    }

    override fun merge(in1: PointsToGraph, in2: PointsToGraph, out: PointsToGraph) {
        in1.merge(out)
        in2.merge(out)
    }

    override fun flowThrough(input: PointsToGraph, node: Unit, out: PointsToGraph) {
        input.copy(out)
        node.apply(StmtVisitor(out, analysisType) as Switch)
    }
}
