package il.ac.technion.cs.reactivize.pta

import soot.Local
import soot.toolkits.graph.UnitGraph

object PTATestUtil {
    val QUOTE_GETTER_CTOR = "new il.ac.technion.cs.reactivize.sample.finance.QuoteGetter"
    val STRING_BUILDER_CTOR = "new java.lang.StringBuilder"
    val VIRTUAL_INVOKE = "virtualinvoke"
    val ZERO_INITIALIZER = "LinkedRValueBox(0)"

    /*
    Mostly used to verify the type of variables so that our tests make sense
    Otherwise they could change when we change the samples, and we'd have a bad time :-(
     */
    fun assertLocalAssignment(graph: UnitGraph?, local: Local, expectedAssigment: String) {
        val definitionUnits = graph!!.body.units.filter { it.defBoxes.size == 1 && it.defBoxes[0].value == local }

        assert(definitionUnits.size == 1) { "expected $local to be defined once, but instead found ${definitionUnits.size} definitions" }

        val actualAssignment = definitionUnits.flatMap { it.useBoxes }.map { it.value }.joinToString(" ")

        if (expectedAssigment == VIRTUAL_INVOKE) {
            assert(actualAssignment.startsWith(VIRTUAL_INVOKE)) { "expected $local to be assigned by a virtual invocation, instead got $actualAssignment" }
            return
        }

        assert(expectedAssigment == actualAssignment) { "expected $local to be assigned by $expectedAssigment, but instead got $actualAssignment" }
    }

    fun assertGeneratedVar(graph: UnitGraph?, local: Local) {
        assertLocalAssignment(graph, local, VIRTUAL_INVOKE)
    }
}
