package il.ac.technion.cs.reactivize.pta

import il.ac.technion.cs.reactivize.helpers.SootUtil.init
import il.ac.technion.cs.reactivize.pta.PtgForwardAnalysis.AnalysisType
import org.junit.Assert
import org.junit.jupiter.api.Test

class TestE5 {
    var className = "il.ac.technion.cs.reactivize.pta.Tests"
    @Test
    fun test23() {
        // Test: Test b�sico de CallGraph
        val ptg = PointsToGraph()
        val analysis = PtgForwardAnalysis("void test23()", className, ptg, AnalysisType.E4)
        val callgraph: Set<Edge> = analysis.pointsToGraph.callgraph

        // Llamado al new de ClassA.
        var e = Edge("il.ac.technion.cs.reactivize.pta.Tests_void test23()", "l_174", "il.ac.technion.cs.reactivize.pta.Tests\$ClassA_void <init>(il.ac.technion.cs.reactivize.pta.Tests)")
        Assert.assertTrue(callgraph.contains(e))

        // Llamado a la funcion initClassBAAttribute()
        e = Edge("il.ac.technion.cs.reactivize.pta.Tests_void test23()", "l_175", "il.ac.technion.cs.reactivize.pta.Tests\$ClassA_void initClassBAttribute()")
        Assert.assertTrue(callgraph.contains(e))

        // Dentro del constructor de ClassA se llama a un constructor <init> sin parametros.
        e = Edge("il.ac.technion.cs.reactivize.pta.Tests\$ClassA_void <init>(il.ac.technion.cs.reactivize.pta.Tests)", "l_132", "il.ac.technion.cs.reactivize.pta.Tests\$ClassA_void <init>()")
        Assert.assertTrue(callgraph.contains(e))

        // Lo mismo que el anterior.
        e = Edge("il.ac.technion.cs.reactivize.pta.Tests\$ClassB_void <init>(il.ac.technion.cs.reactivize.pta.Tests)", "l_148", "il.ac.technion.cs.reactivize.pta.Tests\$ClassB_void <init>()")
        Assert.assertTrue(callgraph.contains(e))

        // El llamado al new de ClassB en initClassBAttribute().
        e = Edge(
            "il.ac.technion.cs.reactivize.pta.Tests\$ClassA_void initClassBAttribute()",
            "l_137",
            "il.ac.technion.cs.reactivize.pta.Tests\$ClassB_void <init>(il.ac.technion.cs.reactivize.pta.Tests)"
        )
        Assert.assertTrue(callgraph.contains(e))
    }

    @Test
    fun test24() {
        // Test: Doble llamado desde el mismo metodo, para identificarlos.
        val ptg = PointsToGraph()
        val analysis = PtgForwardAnalysis("void test24()", className, ptg, AnalysisType.E4)
        val callgraph: Set<Edge> = analysis.pointsToGraph.callgraph
        var e = Edge("il.ac.technion.cs.reactivize.pta.Tests_void test24()", "l_180", "il.ac.technion.cs.reactivize.pta.Tests\$ClassA_void initClassBAttribute()")
        Assert.assertTrue(callgraph.contains(e))
        e = Edge("il.ac.technion.cs.reactivize.pta.Tests_void test24()", "l_181", "il.ac.technion.cs.reactivize.pta.Tests\$ClassA_void initClassBAttribute()")
        Assert.assertTrue(callgraph.contains(e))
    }

    @Test
    fun test25() {
        // Test: El llamado a a.lives() debe hacer que el callgraph apunte a perro y a gato.
        val ptg = PointsToGraph()
        val analysis = PtgForwardAnalysis("void test25()", className, ptg, AnalysisType.E4)
        val callgraph: Set<Edge> = analysis.pointsToGraph.callgraph

        // La misma linea, tiene que dar el llamado a lives() de Dog y de Cat
        var e = Edge("il.ac.technion.cs.reactivize.pta.Tests_void test25()", "l_192", "tests.Dog_int lives()")
        Assert.assertTrue(callgraph.contains(e))
        e = Edge("il.ac.technion.cs.reactivize.pta.Tests_void test25()", "l_192", "tests.Cat_int lives()")
        Assert.assertTrue(callgraph.contains(e))

        // De yapa, los llamados a los new de Cat y Dog.
        e = Edge("il.ac.technion.cs.reactivize.pta.Tests_void test25()", "l_188", "tests.Dog_void <init>()")
        Assert.assertTrue(callgraph.contains(e))
        e = Edge("il.ac.technion.cs.reactivize.pta.Tests_void test25()", "l_190", "tests.Cat_void <init>()")
        Assert.assertTrue(callgraph.contains(e))
    }

    @Test
    fun test26() {
        // Test: Varias llamadas anidadas (El ptg se testea en la suite para E4).
        val ptg = PointsToGraph()
        val analysis = PtgForwardAnalysis("void test26()", className, ptg, AnalysisType.E4)
        val callgraph: Set<Edge> = analysis.pointsToGraph.callgraph
        var e: Edge? = null
        // De test26 se llama a classBFromConstructor() L:196
        e = Edge("il.ac.technion.cs.reactivize.pta.Tests_void test26()", "l_196", "il.ac.technion.cs.reactivize.pta.Tests_il.ac.technion.cs.reactivize.pta.Tests\$ClassB classBFromConstructor()")
        Assert.assertTrue(callgraph.contains(e))

        // Desde classBFromConstructor() se llama a func() L:196
        e = Edge("il.ac.technion.cs.reactivize.pta.Tests_il.ac.technion.cs.reactivize.pta.Tests\$ClassB classBFromConstructor()", "l_163", "il.ac.technion.cs.reactivize.pta.Tests\$ClassB_void func()")
        Assert.assertTrue(callgraph.contains(e))

        // Desde func() se llama a initClassBAttribute() L:152
        e = Edge("il.ac.technion.cs.reactivize.pta.Tests\$ClassB_void func()", "l_152", "il.ac.technion.cs.reactivize.pta.Tests\$ClassA_void initClassBAttribute()")
        Assert.assertTrue(callgraph.contains(e))

        // Desde initClassBAttribute se llama al constructor de ClassB L:137
        e = Edge(
            "il.ac.technion.cs.reactivize.pta.Tests\$ClassA_void initClassBAttribute()",
            "l_137",
            "il.ac.technion.cs.reactivize.pta.Tests\$ClassB_void <init>(il.ac.technion.cs.reactivize.pta.Tests)"
        )
        Assert.assertTrue(callgraph.contains(e))
    }

    init {
        init()
    }
}
