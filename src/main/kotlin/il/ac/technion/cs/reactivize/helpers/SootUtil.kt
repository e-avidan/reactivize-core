package il.ac.technion.cs.reactivize.helpers

import soot.Scene
import soot.toolkits.graph.BriefUnitGraph
import soot.toolkits.graph.UnitGraph
import java.io.File

object SootUtil {
    fun loadMethod(methodSignature: String?, className: String?): UnitGraph {
        if (!Scene.v().containsClass(className)) {
            val klass = Scene.v().tryLoadClass(className, 0)
            klass.setApplicationClass()
            Scene.v().loadNecessaryClasses()
        }

        val method = Scene.v().getSootClass(className).getMethod(methodSignature)
        val body = method.retrieveActiveBody()
        return BriefUnitGraph(body)
    }
}
