package il.ac.technion.cs.reactivize.pta

import il.ac.technion.cs.reactivize.helpers.SootUtil.init
import il.ac.technion.cs.reactivize.pta.PtgForwardAnalysis.AnalysisType
import org.junit.Assert
import org.junit.jupiter.api.Test

class TestE2 {
    var className = "il.ac.technion.cs.reactivize.pta.Tests"
    @Test
    fun test10() {
        // Test: Carga de parametros L(p) = {PN} para todo p.
        val analysis = PtgForwardAnalysis(
            "void test10(java.lang.Object,java.lang.Object)",
            className,
            PointsToGraph(),
            AnalysisType.E2
        )
        val locals: Map<String, Set<String>?> = analysis.pointsToGraph.locals
        Assert.assertTrue(locals.containsKey("this"))
        Assert.assertTrue(locals["this"]!!.contains("PN_this"))
        Assert.assertTrue(locals.containsKey("x"))
        Assert.assertTrue(locals["x"]!!.contains("PN_x"))
        Assert.assertTrue(locals.containsKey("y"))
        Assert.assertTrue(locals["y"]!!.contains("PN_y"))
    }

    @Test
    fun test11() {
        // Test: Comprobar el efecto de ln.
        val analysis = PtgForwardAnalysis("void test11()", className, PointsToGraph(), AnalysisType.E2)
        val locals: Map<String, Set<String>?> = analysis.pointsToGraph.locals
        val edges: Set<Edge> = analysis.pointsToGraph.edges
        val redges: Set<Edge> = analysis.pointsToGraph.redges
        Assert.assertTrue(locals.containsKey("a"))
        Assert.assertTrue(locals.containsKey("b"))
        Assert.assertTrue(locals.containsKey("b2"))

        // x = y.f carga un ln fresco (se distinguen con numero de linea).
        Assert.assertTrue(locals["b"]!!.contains("ln_72"))
        Assert.assertTrue(locals["b2"]!!.contains("ln_73"))
        val aObj = "il.ac.technion.cs.reactivize.pta.Tests\$ClassA_1_68"
        Assert.assertTrue(locals["a"]!!.contains(aObj))

        // Los nodos que apunta y, van por classBAttribute a los nuevos nodos ln
        var r = Edge(aObj, "classBAttribute", "ln_72")
        Assert.assertTrue(redges.contains(r))
        r = Edge(aObj, "classBAttribute", "ln_73")
        Assert.assertTrue(redges.contains(r))

        // Los ejes quedan igual
        val e = Edge(aObj, "classBAttribute", "il.ac.technion.cs.reactivize.pta.Tests\$ClassB_1_69")
        Assert.assertTrue(edges.contains(e))
    }

    init {
        init()
    }
}
