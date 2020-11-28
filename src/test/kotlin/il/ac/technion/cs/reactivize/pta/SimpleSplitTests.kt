package il.ac.technion.cs.reactivize.pta

import il.ac.technion.cs.reactivize.REQUIRED_CLASS_NAMES
import il.ac.technion.cs.reactivize.ReactivizePostCompilerTest
import org.junit.Assert
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import soot.G
import soot.Scene
import soot.SootClass
import soot.options.Options
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class SimpleSplitTests {
    @BeforeEach
    fun configureClassPath() {
        val classpath: Iterable<File> =
            System.getProperty("java.class.path").split(":").map(::File).filter(File::exists) + listOf(
                Paths.get(
                    SimpleSplitTests::class.java.getResource("/classes").toURI()
                ).toFile()
            )

        G.v().resetSpark()
        Options.v().set_whole_program(true)
        Options.v().setPhaseOption("jb", "use-original-names:true")
        Options.v().setPhaseOption("cg.spark", "on")

        val javaHome = System.getProperty("java.home")
        val sootCp =
            "VIRTUAL_FS_FOR_JDK:${classpath.joinToString(separator = ":") { it.path }}:${Scene.defaultJavaClassPath()}:${javaHome}"
        println(sootCp)
        Options.v().set_soot_classpath(sootCp)

        Options.v().set_prepend_classpath(true)
        Options.v().set_no_bodies_for_excluded(true)
        Options.v().set_allow_phantom_refs(true)
        Options.v().set_whole_program(true)

        Options.v().setPhaseOption("cg.spark", "on")
        Options.v().setPhaseOption("jb", "use-original-names:true")
        Options.v().set_output_format(Options.output_format_jimple)

//        Scene.v().loadNecessaryClasses()
    }

    @BeforeEach
    fun resetSoot() {
//        soot.G.reset()
    }

    @Test
    fun test10() {
        val analysis = PtgForwardAnalysis(
            "void main()",
            "il.ac.technion.cs.reactivize.sample.splitting.simple.SimpleNoSplitExampleKt",
            PointsToGraph(),
            PtgForwardAnalysis.AnalysisType.E2
        )

        val locals: Map<String, Set<String>?> = analysis.pointsToGraph.locals
    }
}
