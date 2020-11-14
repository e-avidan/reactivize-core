package il.ac.technion.cs.reactivize.helpers

import soot.Scene
import soot.toolkits.graph.BriefUnitGraph
import soot.toolkits.graph.UnitGraph
import java.io.File
import java.nio.file.Paths

object SootUtil {
// TODO: verify if some flags here might still be relevant
    fun init() {
        val someClassFile = File("./build/classes/kotlin/test").absoluteFile
        soot.options.Options.v().set_keep_line_number(true)
        soot.options.Options.v().setPhaseOption("jb", "use-original-names:true")
        soot.options.Options.v().set_whole_program(true)
        Scene.v().sootClassPath = Scene.v().sootClassPath + File.pathSeparator + someClassFile + File.pathSeparator + File("./build/classes/kotlin/main").absoluteFile + File.pathSeparator + System.getProperty("java.class.path").split(":").map(::File).filter(File::exists).map(File::getPath).joinToString(":")
    }

    fun loadMethod(methodSignature: String?, className: String?): UnitGraph {
        if (!Scene.v().containsClass(className)) {
            val c = Scene.v().tryLoadClass(className, 0)
            c.setApplicationClass()
            Scene.v().loadNecessaryClasses()
        }
        val m = Scene.v().getSootClass(className).getMethod(methodSignature)
        val b = m.retrieveActiveBody()
        return BriefUnitGraph(b)
    }
}
