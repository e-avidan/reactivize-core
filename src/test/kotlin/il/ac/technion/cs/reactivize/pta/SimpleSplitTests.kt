package il.ac.technion.cs.reactivize.pta

import il.ac.technion.cs.reactivize.helpers.SootUtil
import org.junit.jupiter.api.BeforeAll

import org.junit.jupiter.api.Test
import soot.toolkits.graph.UnitGraph

class SimpleSplitTests {
    companion object {
        private lateinit var graph: PTAGraph

        @BeforeAll
        @JvmStatic
        fun runAnalysis() {
            graph = SootUtil.initSootWithKlassAndPTA(
                SimpleSplitTests::class,
                sampleName = "splitting.simple.SimpleNoSplitExampleKt"
            )!!
        }
    }

    @Test
    fun verifyPTAForSamePtr() {
        assert(0 == graph.roots.size)
    }

//    @Test
//    fun verifyPTAForSamePtr() {
//        val query = SootUtil.getPTAQuery()
//        val locals = graph!!.body.locals.toList()
//
//        val qgVar = locals[0]
//        PTATestUtil.assertLocalAssignment(graph, qgVar, PTATestUtil.QUOTE_GETTER_CTOR)
//
//        assert(query.isAlias(qgVar, qgVar)) { "Should at the very least work for ANY context" }
//        assert(query.isAliasCI(qgVar, qgVar)) { "Should be true for ALL contexts" }
//    }
//
//    @Test
//    fun verifyPTAForSamePtrDifferentLocal() {
//        val query = SootUtil.getPTAQuery()
//        val locals = graph!!.body.locals.toList()
//
//        val qgVar = locals[0]
//        val stringBuilderVar = locals[1]
//
//        PTATestUtil.assertLocalAssignment(graph, qgVar, PTATestUtil.QUOTE_GETTER_CTOR)
//        PTATestUtil.assertLocalAssignment(graph, stringBuilderVar, PTATestUtil.STRING_BUILDER_CTOR)
//
//        assert(!query.isAlias(qgVar, stringBuilderVar)) { "Different vars" }
//        assert(!query.isAliasCI(qgVar, stringBuilderVar)) { "Different vars" }
//    }
//
//    @Test
//    fun verifyPTAForJimpleVar() {
//        val query = SootUtil.getPTAQuery()
//        val locals = graph!!.body.locals.toList()
//
//        val jimpleGeneratedVar = locals[2]
//        PTATestUtil.assertGeneratedVar(graph, jimpleGeneratedVar)
//
//        assert(!query.isAlias(jimpleGeneratedVar, jimpleGeneratedVar)) { "Should be false" }
//        assert(!query.isAliasCI(jimpleGeneratedVar, jimpleGeneratedVar)) { "Should be false" }
//    }
}
