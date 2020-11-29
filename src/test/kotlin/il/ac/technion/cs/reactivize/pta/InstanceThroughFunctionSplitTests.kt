package il.ac.technion.cs.reactivize.pta

import il.ac.technion.cs.reactivize.helpers.SootUtil
import org.junit.jupiter.api.BeforeAll

import org.junit.jupiter.api.Test
import soot.toolkits.graph.UnitGraph

// TODO: just copied this for now to debug
class InstanceThroughFunctionSplitTests {
    var LAMBDA1_CLASSNAME = "il.ac.technion.cs.reactivize.sample.splitting.instance.InstanceSplitExampleKt\$main\$1"

    companion object {
        private lateinit var graph: PTAGraph

        @BeforeAll
        @JvmStatic
        fun runAnalysis() {
            graph = SootUtil.initSootWithKlassAndPTA(
                InstanceThroughFunctionSplitTests::class,
                sampleName = "splitting.instance_through_function.InstanceThroughFunctionSplitExampleKt"
            )!!
        }
    }

    @Test
    fun verifyPTAForSamePtr() {
        assert(2 == graph.roots.size)
    }

//
//    @Test
//    fun verifyPTAForCounter1() {
//        val query = SootUtil.getPTAQuery()
//        val locals = graph!!.body.locals.toList()
//
//        val ctr1 = locals[3]
//        PTATestUtil.assertLocalAssignment(graph, ctr1, PTATestUtil.ZERO_INITIALIZER)
//
//        val lambda1 = locals[5]
//        PTATestUtil.assertLocalAssignment(graph, lambda1, "new $LAMBDA1_CLASSNAME")
////
////
////        assert(query.isAlias(qgVar, qgVar)) { "Should at the very least work for ANY context" }
////        assert(query.isAliasCI(qgVar, qgVar)) { "Should be true for ALL contexts" }
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
