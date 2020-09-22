package il.ac.technion.cs.reactivize

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class ReactivizePostCompilerTest {
    @Test
    fun sanity() {
        ReactivizePostCompiler()
    }

    @BeforeEach
    fun resetSoot() {
        soot.G.reset()
    }

    @Test
    fun `empty execute`() {
        val destDir = Files.createTempDirectory("dest").toFile()
        val classpath = System.getProperty("java.class.path").split(":").map(::File).filter(File::exists)
        val pc = ReactivizePostCompiler()
        val spec = ReactivizeCompileSpec(
            destDir,
            Files.createTempDirectory("work").toFile(),
            Files.createTempDirectory("temp").toFile(),
            classpath,
            listOf(),
            listOf(),
            listOf()
        )
        pc.execute(spec)
    }

    @Test
    fun `load finance`() {
        val destDir = Files.createTempDirectory("dest").toFile()
        val classpath: Iterable<File> =
            System.getProperty("java.class.path").split(":").map(::File).filter(File::exists) + listOf(
                Paths.get(
                    ReactivizePostCompilerTest::class.java.getResource("/classes").toURI()
                ).toFile()
            )
        println(classpath)
        println("Destination: $destDir")
        val pc = ReactivizePostCompiler()
        val spec = ReactivizeCompileSpec(
            destDir,
            Files.createTempDirectory("work").toFile(),
            Files.createTempDirectory("temp").toFile(),
            classpath,
            listOf(),
            listOf(
                "il.ac.technion.cs.reactivize.sample.finance.QuoteGetter",
                "il.ac.technion.cs.reactivize.sample.finance.QuoteGetterKt"
            ),
            listOf("il.ac.technion.cs.reactivize.sample", "yahoofinance")
        )
        pc.execute(spec)
        println(destDir.listFiles().map(File::getAbsolutePath))
        val c = soot.Scene.v().getSootClass("il.ac.technion.cs.reactivize.sample.finance.QuoteGetter")
        println(c.methods[0].activeBody)
    }
}