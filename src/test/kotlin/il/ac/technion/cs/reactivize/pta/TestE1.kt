package il.ac.technion.cs.reactivize.pta

import il.ac.technion.cs.reactivize.helpers.SootUtil.init
import il.ac.technion.cs.reactivize.pta.PtgForwardAnalysis.AnalysisType
import org.junit.Assert
import org.junit.jupiter.api.Test
import java.util.*

class TestE1 {
    var className = "il.ac.technion.cs.reactivize.pta.Tests"
    @Test
    fun test1() {
        // Test: Creacion de objeto.
        val analysis = PtgForwardAnalysis("void test1()", className, PointsToGraph(), AnalysisType.E1)
        val locals: Map<String, Set<String>?> = analysis.pointsToGraph.locals
        Assert.assertTrue(locals.containsKey("a"))
        Assert.assertTrue(locals["a"]!!.contains("il.ac.technion.cs.reactivize.pta.Tests\$ClassA_1_6"))
    }

    @Test
    fun test2() {
        // Test: Asignaci�n simple.
        val analysis = PtgForwardAnalysis("void test2()", className, PointsToGraph(), AnalysisType.E1)
        val locals: Map<String, Set<String>?> = analysis.pointsToGraph.locals
        Assert.assertTrue(locals.containsKey("a"))
        Assert.assertTrue(locals.containsKey("a2"))
        Assert.assertTrue(locals["a"] == locals["a2"])
    }

    @Test
    fun test3() {
        // Test: Strong update.
        val analysis = PtgForwardAnalysis("void test3()", className, PointsToGraph(), AnalysisType.E1)
        val locals: Map<String, Set<String>> = analysis.pointsToGraph.locals
        val edges: Set<Edge> = analysis.pointsToGraph.edges
        Assert.assertTrue(locals.containsKey("a"))
        Assert.assertTrue(locals.containsKey("b"))

        // TODO: Had to add this, otherwise it wouldn't compile
        val bObj = locals["b"]!!.iterator().next()

        for (e in edges) {
            if (e.field == "classBAttributte") {
                Assert.assertTrue(bObj == e.target)
            }
        }
    }

    @Test
    fun test4() {
        // Test: Existencia de eje entre objeto A y objeto B (a = new(), b= new(), a.f = b)
        val analysis = PtgForwardAnalysis("void test4()", className, PointsToGraph(), AnalysisType.E1)
        val locals: Map<String, Set<String>?> = analysis.pointsToGraph.locals
        val edges: Set<Edge> = analysis.pointsToGraph.edges
        Assert.assertTrue(locals.containsKey("a"))
        Assert.assertTrue(locals.containsKey("b"))
        val aObj = locals["a"]!!.iterator().next()
        val bObj = locals["b"]!!.iterator().next()
        for (e in edges) {
            if (e.source == aObj) {
                Assert.assertTrue(e.target == bObj)
            }
        }
    }

    @Test
    fun test5() {
        // Test: Strong update de fields.
        val analysis = PtgForwardAnalysis("void test5()", className, PointsToGraph(), AnalysisType.E1)
        val locals: Map<String, Set<String>?> = analysis.pointsToGraph.locals
        val edges: Set<Edge> = analysis.pointsToGraph.edges
        Assert.assertTrue(locals.containsKey("a"))
        Assert.assertTrue(locals.containsKey("a2"))
        val aObj = locals["a"]!!.iterator().next()
        val a2Obj = locals["a2"]!!.iterator().next()
        var aObjTarget: String? = null
        var a2ObjTarget: String? = null
        for (e in edges) {
            if (e.source == a2Obj) {
                Assert.assertTrue(e.field == "classBAttribute")
                a2ObjTarget = e.target
            } else if (e.source == aObj) {
                Assert.assertTrue(e.field == "classBAttribute")
                aObjTarget = e.target
            }
        }
        Assert.assertTrue(aObjTarget == a2ObjTarget)
    }

    @Test
    fun test6() {
        // Test: Dos allocs en la misma linea
        val analysis = PtgForwardAnalysis("void test6()", className, PointsToGraph(), AnalysisType.E1)
        val locals: Map<String, Set<String>?> = analysis.pointsToGraph.locals
        Assert.assertTrue(locals.containsKey("a"))
        Assert.assertTrue(locals.containsKey("a2"))
        val aObj = locals["a"]!!.iterator().next()
        val a2Obj = locals["a2"]!!.iterator().next()
        Assert.assertTrue(aObj != a2Obj)
    }

    @Test
    fun test7() {
        // Test: Allocs en FOR
        val analysis = PtgForwardAnalysis("void test7()", className, PointsToGraph(), AnalysisType.E1)
        val locals: Map<String, Set<String>?> = analysis.pointsToGraph.locals
        Assert.assertTrue(locals.containsKey("a"))
        Assert.assertTrue(locals.containsKey("a2"))
        Assert.assertTrue(locals["a2"]!!.contains("il.ac.technion.cs.reactivize.pta.Tests\$ClassA_1_41"))
        Assert.assertTrue(locals["a"]!!.contains("il.ac.technion.cs.reactivize.pta.Tests\$ClassA_1_41"))
    }

    @Test
    fun test8() {
        // Test: Join de Allocs en If-Then-Else
        val analysis = PtgForwardAnalysis("void test8()", className, PointsToGraph(), AnalysisType.E1)
        val locals: Map<String, Set<String>?> = analysis.pointsToGraph.locals
        Assert.assertTrue(locals.containsKey("a"))
        Assert.assertTrue(locals["a"]!!.contains("il.ac.technion.cs.reactivize.pta.Tests\$ClassA_1_50"))
        Assert.assertTrue(locals["a"]!!.contains("il.ac.technion.cs.reactivize.pta.Tests\$ClassA_1_52"))
    }

    @Test
    fun test9() {
        // Test: Error en "a = new (), a = b,  a = b.f" (agrega nueva ref a� a L (?))
        val analysis = PtgForwardAnalysis("void test9()", className, PointsToGraph(), AnalysisType.E1)
        val locals: Map<String, Set<String>?> = analysis.pointsToGraph.locals
        val edges: Set<Edge> = analysis.pointsToGraph.edges
        Assert.assertTrue(locals.containsKey("a"))
        Assert.assertTrue(locals.containsKey("a2"))
        Assert.assertTrue(locals["a"]!!.contains("il.ac.technion.cs.reactivize.pta.Tests\$ClassA_1_57"))
        val aObj = locals["a"]!!.iterator().next()

        // Por el soft update tienen que existir los dos objetos.
        var aObjTarget1: String? = null
        var aObjTarget2: String? = null
        for (e in edges) {
            if (e.source == aObj) {
                Assert.assertTrue(e.field == "classAAttribute")
                if (aObjTarget1 == null) aObjTarget1 = e.target else aObjTarget2 = e.target
            }
        }
        Assert.assertTrue(aObjTarget1 != null && aObjTarget2 != null)
        Assert.assertTrue(aObjTarget1 != aObjTarget2)

        // Por el stronge update a2 no es ni el objeto de la linea 60 ni el de la linea 57
        Assert.assertTrue(!locals["a2"]!!.contains("il.ac.technion.cs.reactivize.pta.Tests\$ClassA_1_60"))
        Assert.assertTrue(!locals["a2"]!!.contains("il.ac.technion.cs.reactivize.pta.Tests\$ClassA_1_57"))

        // Por la tercer regla, "a2" debe contener los objetos asignados a "a"
        Assert.assertTrue(locals["a2"]!!.contains(aObjTarget1))
        Assert.assertTrue(locals["a2"]!!.contains(aObjTarget2))

        // Se chequea que no agrege nueva ref a� a L
        val map: MutableSet<String> = HashSet()
        for (local in locals.keys) {
            if (!local.startsWith("\$r")) {
                map.add(local)
            }
        }
        map.remove("a")
        map.remove("a2")
        map.remove("this")
        map.remove("_STATIC_")
        Assert.assertTrue(map.isEmpty())
    }

    @Test
    fun test12() {
        // Test: Comportamiento con funciones estaticas
        val analysis = PtgForwardAnalysis("void test12()", className, PointsToGraph(), AnalysisType.E1)

        // Para el analisis del Ejercicio 1, a los stmts con funciones no los
        // analizamos. El objetivo del test es ver que no se cuelgue y que
        // efectivamente no haga nada.
        val locals: Map<String, Set<String>?> = analysis.pointsToGraph.locals
        Assert.assertTrue(!locals.containsKey("c"))
    }

    @Test
    fun test14() {
        // Test: Este test es parecido al test 8, pero si hay return no se hace
        // merge en el analisis, por lo que hay que mergearlo al final... creo.
        val analysis = PtgForwardAnalysis("void test14()", className, PointsToGraph(), AnalysisType.E1)
        val locals: Map<String, Set<String>?> = analysis.pointsToGraph.locals
        Assert.assertTrue(locals.containsKey("a"))
        Assert.assertTrue(locals["a"]!!.contains("il.ac.technion.cs.reactivize.pta.Tests\$ClassA_1_89"))
        Assert.assertTrue(locals["a"]!!.contains("il.ac.technion.cs.reactivize.pta.Tests\$ClassA_1_92"))
    }

    @Test
    fun test18() {
        // Test: Se testea la lectura y escritura de atributos estaticos.
        val analysis = PtgForwardAnalysis("void test18()", className, PointsToGraph(), AnalysisType.E1)
        val locals: Map<String, Set<String>?> = analysis.pointsToGraph.locals
        val statics: Map<String, Set<String>?> = analysis.pointsToGraph.statics
        val edges: Set<Edge> = analysis.pointsToGraph.edges
        Assert.assertTrue(statics.containsKey("il.ac.technion.cs.reactivize.pta.Tests\$ClassA_STATIC"))
        Assert.assertTrue(locals.containsKey("a"))
        val source: String = "il.ac.technion.cs.reactivize.pta.Tests\$ClassA_STATIC"
        val target: String = "il.ac.technion.cs.reactivize.pta.Tests\$ClassA_1_119"
        Assert.assertTrue(statics["il.ac.technion.cs.reactivize.pta.Tests\$ClassA_STATIC"]!!.contains(source))
        Assert.assertTrue(locals["a"]!!.contains(target))
        for (e in edges) {
            if (e.source == source) {
                Assert.assertTrue(e.field == "staticAAttribute")
                Assert.assertTrue(e.target == target)
            }
        }
    }

    @Test
    fun test19() {
        // Test: Se testea la lectura y escritura de atributos de this (implicito).
        val analysis = PtgForwardAnalysis("void test19()", className, PointsToGraph(), AnalysisType.E1)
        val locals: Map<String, Set<String>?> = analysis.pointsToGraph.locals
        val edges: Set<Edge> = analysis.pointsToGraph.edges
        Assert.assertTrue(locals.containsKey("this"))
        Assert.assertTrue(locals.containsKey("a"))
        val source: String = "il.ac.technion.cs.reactivize.pta.Tests_THIS"
        val target: String = "il.ac.technion.cs.reactivize.pta.Tests\$ClassA_1_124"
        Assert.assertTrue(locals["this"]!!.contains(source))
        Assert.assertTrue(locals["a"]!!.contains(target))
        for (e in edges) {
            if (e.source == source) {
                Assert.assertTrue(e.field == "instanceAAttribute")
                Assert.assertTrue(e.target == target)
            }
        }
    }

    init {
        init()
    }
}
