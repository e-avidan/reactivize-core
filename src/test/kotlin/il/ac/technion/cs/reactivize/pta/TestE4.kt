package il.ac.technion.cs.reactivize.pta

import il.ac.technion.cs.reactivize.helpers.SootUtil.init
import il.ac.technion.cs.reactivize.pta.PtgForwardAnalysis.AnalysisType
import org.junit.Assert
import org.junit.jupiter.api.Test
import java.util.*

class TestE4 {
    var className = "il.ac.technion.cs.reactivize.pta.Tests"
    @Test
    fun test13() {
        // Test: x = o.invoke();
        val ptg = PointsToGraph()
        val analysis = PtgForwardAnalysis("void test13()", className, ptg, AnalysisType.E4)
        val locals: Map<String, Set<String>?> = analysis.pointsToGraph.locals
        Assert.assertTrue(locals.containsKey("o"))
        Assert.assertTrue(locals.containsKey("a"))
        Assert.assertTrue(!locals.containsKey("x")) // Se borra al salir de la funcion.
        Assert.assertTrue(locals["o"]!!.contains("java.lang.Object_1_81"))
        Assert.assertTrue(locals["a"]!!.contains("java.lang.Object_1_99"))
    }

    @Test
    fun test15() {
        // Test: x.f = o.invoke();
        val ptg = PointsToGraph()
        val analysis = PtgForwardAnalysis("void test15()", className, ptg, AnalysisType.E4)
        val locals: Map<String, Set<String>?> = analysis.pointsToGraph.locals
        val edges: Set<Edge> = analysis.pointsToGraph.edges
        Assert.assertTrue(locals.containsKey("a"))
        val obj: String = "il.ac.technion.cs.reactivize.pta.Tests\$ClassA_1_104"
        Assert.assertTrue(locals["a"]!!.contains(obj))
        val targets: MutableSet<String?> = HashSet()
        for (e in edges) {
            if (e.source == obj) {
                Assert.assertTrue(e.field == "ObjectAttribute")
                targets.add(e.target)
            }
        }
        Assert.assertTrue(targets.contains("java.lang.Object_1_105"))
        Assert.assertTrue(targets.contains("java.lang.Object_1_99"))
    }

    @Test
    fun test16() {
        // Test: field = new A(); = this.field = new A(); | El this es impl�cito.
        val ptg = PointsToGraph()
        val analysis = PtgForwardAnalysis("void test16()", className, ptg, AnalysisType.E4)
        val locals: Map<String, Set<String>?> = analysis.pointsToGraph.locals
        val edges: Set<Edge> = analysis.pointsToGraph.edges
        Assert.assertTrue(locals.containsKey("this"))
        val source: String = "il.ac.technion.cs.reactivize.pta.Tests_THIS"
        val target: String = "il.ac.technion.cs.reactivize.pta.Tests\$ClassA_1_111"
        for (e in edges) {
            if (e.source == source) {
                Assert.assertTrue(e.field == "classAAttribute")
                Assert.assertTrue(e.target == target)
            }
        }
    }

    @Test
    fun test17() {
        // Test: Llamado a metodo estatico
        val ptg = PointsToGraph()
        val analysis = PtgForwardAnalysis("void test17()", className, ptg, AnalysisType.E4)
        val locals: Map<String, Set<String>?> = analysis.pointsToGraph.locals
        Assert.assertTrue(locals.containsKey("c"))
        Assert.assertTrue(locals["c"]!!.contains("tests.TestClassC_1_11"))
    }

    @Test
    fun test20() {
        // Test: x = o.invoke();
        val ptg = PointsToGraph()
        val analysis = PtgForwardAnalysis("void test20()", className, ptg, AnalysisType.E4)
        val locals: Map<String, Set<String>?> = analysis.pointsToGraph.locals
        Assert.assertTrue(locals.containsKey("a"))
        Assert.assertTrue(locals.containsKey("b"))
        Assert.assertTrue(locals["a"]!!.contains("il.ac.technion.cs.reactivize.pta.Tests\$ClassA_1_142"))
        Assert.assertTrue(locals["b"]!!.contains("il.ac.technion.cs.reactivize.pta.Tests\$ClassB_1_137"))
    }

    @Test
    fun test21() {
        // Test: method inside static.
        val ptg = PointsToGraph()
        val analysis = PtgForwardAnalysis("void test21()", className, ptg, AnalysisType.E4)
        val locals: Map<String, Set<String>?> = analysis.pointsToGraph.locals
        Assert.assertTrue(locals.containsKey("b"))
        Assert.assertTrue(locals["b"]!!.contains("il.ac.technion.cs.reactivize.pta.Tests\$ClassB_1_137"))
    }

    @Test
    fun test22() {
        // Test: Se prueba recursion y wrongs(que hasta ahora no se probo).
        val ptg = PointsToGraph()
        val analysis = PtgForwardAnalysis("void test22()", className, ptg, AnalysisType.E4)
        val locals: Map<String, Set<String>?> = analysis.pointsToGraph.locals
        val wrongs: Set<String> = analysis.pointsToGraph.wrongs
        Assert.assertTrue(locals.containsKey("c"))
        Assert.assertTrue(locals["c"]!!.contains("tests.TestClassC_1_169"))

        // El jimple del constructor llama a otro <init>, eso me hacia colgar.
        Assert.assertTrue(wrongs.contains("rec_tests.TestClassC_void <init>()"))

        // El llamado recursivo de recursive se mete en wrongs.
        Assert.assertTrue(wrongs.contains("rec_tests.TestClassC_int recursive(int)"))
    }

    @Test
    fun test27() {
        // Test: Modificacion de atributo del this mediante metodo (this implicito)
        val ptg = PointsToGraph()
        val analysis = PtgForwardAnalysis("void test27()", className, ptg, AnalysisType.E4)
        val locals: Map<String, Set<String>?> = analysis.pointsToGraph.locals
        Assert.assertTrue(locals.containsKey("a"))
        Assert.assertTrue(locals["a"]!!.contains("il.ac.technion.cs.reactivize.pta.Tests\$ClassA_1_200"))
    }

    @Test
    fun test28() {
        // Test: Analisis del constructor (con parametros).
        val ptg = PointsToGraph()
        val analysis = PtgForwardAnalysis("void test28()", className, ptg, AnalysisType.E4)
        val locals: Map<String, Set<String>?> = analysis.pointsToGraph.locals
        Assert.assertTrue(locals.containsKey("a2"))
        Assert.assertTrue(locals["a2"]!!.contains("il.ac.technion.cs.reactivize.pta.Tests\$ClassA_1_209"))
    }

    @Test
    fun test29() {
        // Test: Llamados a funcion dentro de un FOR.
        val ptg = PointsToGraph()
        val analysis = PtgForwardAnalysis("void test29()", className, ptg, AnalysisType.E4)
        val locals: Map<String, Set<String>?> = analysis.pointsToGraph.locals
        Assert.assertTrue(locals.containsKey("a"))
        Assert.assertTrue(locals["a"]!!.contains("il.ac.technion.cs.reactivize.pta.Tests\$ClassA_1_216"))

        // XXX: El JIMPLE no respeta el nombre de la variable, en lugar de "a2" le pone "i".
        Assert.assertTrue(locals.containsKey("i"))
        Assert.assertTrue(locals["i"]!!.contains("il.ac.technion.cs.reactivize.pta.Tests\$ClassA_1_216"))
    }

    @Test
    fun test30() {
        // Test: Llamados a funcion dentro de un FOR.
        val ptg = PointsToGraph()
        val analysis = PtgForwardAnalysis("void test30()", className, ptg, AnalysisType.E4)
        val locals: Map<String, Set<String>?> = analysis.pointsToGraph.locals
        Assert.assertTrue(locals.containsKey("b"))
        Assert.assertTrue(locals["b"]!!.contains("il.ac.technion.cs.reactivize.pta.Tests\$ClassB_1_241"))
    }

    init {
        init()
    }
}
