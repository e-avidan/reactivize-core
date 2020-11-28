package il.ac.technion.cs.reactivize.helpers

import soot.G
import soot.PackManager
import soot.Scene
import soot.SootClass
import soot.jimple.spark.geom.geomPA.GeomPointsTo
import soot.jimple.spark.geom.geomPA.GeomQueries
import soot.options.Options
import soot.toolkits.graph.BriefUnitGraph
import soot.toolkits.graph.UnitGraph
import java.io.File
import java.nio.file.Paths
import kotlin.reflect.KClass

object SootUtil {
    val EXPECTED_PTA_CLASS = "soot.jimple.spark.geom.geomPA.GeomPointsTo"

    fun <T: Any> initSootWithKlassAndPTA(klass: KClass<T>, sampleName: String? = null): UnitGraph? {
        var verifyPTA = true

        try {
            reset()

            setClassPath(klass)
            configure(enablePTA = true)

            if (sampleName != null) {
                return loadSample(sampleName)
            }

            load()
            return null
        } catch (ex: Exception) {
            verifyPTA = false
            throw ex
        } finally {
            if (verifyPTA) {
                verifyPTAConfigured()
            }
        }
    }

    fun getPTAQuery(): GeomQueries {
        verifyPTAConfigured()

        val pta = Scene.v().pointsToAnalysis
        val geomPTA = pta as GeomPointsTo
        return GeomQueries(geomPTA)
    }

    private fun reset() {
        G.reset()
        G.v().resetSpark()
    }

    private fun configure(enablePTA: Boolean = true) {
        Options.v().set_whole_program(true)
        Options.v().set_prepend_classpath(true)
        Options.v().set_no_bodies_for_excluded(true)
        Options.v().set_allow_phantom_refs(true)
        Options.v().set_output_format(Options.output_format_jimple)

        Options.v().setPhaseOption("jb", "use-original-names:true")

        if (enablePTA) {
            enablePTA()
        }
    }

    private fun <T: Any> getKlassPath(klass: KClass<T>): Iterable<File> {
        val systemPath = System.getProperty("java.class.path").split(":").map(::File).filter(File::exists)
        val klassPath = listOf(Paths.get(klass.java.getResource("/classes").toURI()).toFile())

        return systemPath + klassPath
    }

    private fun <T: Any> setClassPath(klass: KClass<T>) {
        val javaHome = System.getProperty("java.home")
        val classPath = getKlassPath(klass)

        val sootClassPath =
            "VIRTUAL_FS_FOR_JDK:${classPath.joinToString(separator = ":") { it.path }}:${Scene.defaultJavaClassPath()}:${javaHome}"

        Options.v().set_soot_classpath(sootClassPath)
    }

    private fun enablePTA() {
        Options.v().setPhaseOption("cg","enabled:true");
        Options.v().setPhaseOption("cg.spark","enabled:true");
        Options.v().setPhaseOption("cg.spark","geom-pta:true");
    }

    private fun verifyPTAConfigured() {
        val ptaClass = Scene.v().pointsToAnalysis.javaClass.name
        if (ptaClass != EXPECTED_PTA_CLASS) {
            throw Exception("PTA loaded incorrectly - got $ptaClass")
        }
    }

    private fun load() {
        Scene.v().loadNecessaryClasses()
        PackManager.v().runPacks()
    }

    private fun loadMethod(methodSignature: String, className: String): UnitGraph {
        if (!Scene.v().containsClass(className)) {
            throw Exception("Class $className should have been loaded (see addBasicClass)")
        }

        val klass = Scene.v().getSootClass(className)
        val method = klass.getMethod(methodSignature)
        val body = method.retrieveActiveBody()

        return BriefUnitGraph(body)
    }

    private fun addBasicClass(className: String) {
        Scene.v().addBasicClass(className, SootClass.SIGNATURES);
        val c = Scene.v().forceResolve(className, SootClass.BODIES)
        c.setApplicationClass()
    }

    private fun loadSample(name: String): UnitGraph {
        val sampleClassName = "il.ac.technion.cs.reactivize.sample.$name"

        addBasicClass(sampleClassName)
        load()

        return loadMethod("void main()", sampleClassName)
    }
}
