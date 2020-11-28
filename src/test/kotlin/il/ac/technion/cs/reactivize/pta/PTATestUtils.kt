package il.ac.technion.cs.reactivize.pta

import soot.Local
import soot.toolkits.graph.UnitGraph

object PTATestUtils {
    val VIRTUAL_INVOKE = "virtualinvoke"

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
