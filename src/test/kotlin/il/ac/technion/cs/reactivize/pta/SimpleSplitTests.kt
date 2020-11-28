package il.ac.technion.cs.reactivize.pta

import il.ac.technion.cs.reactivize.helpers.SootUtil
import org.junit.jupiter.api.BeforeAll

import org.junit.jupiter.api.Test
import soot.toolkits.graph.UnitGraph

class SimpleSplitTests {
    companion object {
        var graph: UnitGraph? = null

        @BeforeAll
        @JvmStatic
        fun loadSoot() {
            graph = SootUtil.initSootWithKlassAndPTA(
                SimpleSplitTests::class,
                sampleName = "splitting.simple.SimpleNoSplitExampleKt"
            )

            println(graph!!)
        }
    }

    @Test
    fun verifyPTAForSamePtr() {
        val query = SootUtil.getPTAQuery()
        val locals = InstanceSplitTests.graph!!.body.locals.toList()

        val qgVar = locals[0]
        PTATestUtil.assertLocalAssignment(InstanceSplitTests.graph, qgVar, PTATestUtil.QUOTE_GETTER_CTOR)

        assert(query.isAlias(qgVar, qgVar)) { "Should at the very least work for ANY context" }
        assert(query.isAliasCI(qgVar, qgVar)) { "Should be true for ALL contexts" }
    }

    @Test
    fun verifyPTAForSamePtrDifferentLocal() {
        val query = SootUtil.getPTAQuery()
        val locals = InstanceSplitTests.graph!!.body.locals.toList()

        val qgVar = locals[0]
        val stringBuilderVar = locals[1]

        PTATestUtil.assertLocalAssignment(InstanceSplitTests.graph, qgVar, PTATestUtil.QUOTE_GETTER_CTOR)
        PTATestUtil.assertLocalAssignment(InstanceSplitTests.graph, stringBuilderVar, PTATestUtil.STRING_BUILDER_CTOR)

        assert(!query.isAlias(qgVar, stringBuilderVar)) { "Different vars" }
        assert(!query.isAliasCI(qgVar, stringBuilderVar)) { "Different vars" }
    }

    @Test
    fun verifyPTAForJimpleVar() {
        val query = SootUtil.getPTAQuery()
        val locals = InstanceSplitTests.graph!!.body.locals.toList()

        val jimpleGeneratedVar = locals[2]
        PTATestUtil.assertGeneratedVar(InstanceSplitTests.graph, jimpleGeneratedVar)

        assert(!query.isAlias(jimpleGeneratedVar, jimpleGeneratedVar)) { "Should be false" }
        assert(!query.isAliasCI(jimpleGeneratedVar, jimpleGeneratedVar)) { "Should be false" }
    }
}
