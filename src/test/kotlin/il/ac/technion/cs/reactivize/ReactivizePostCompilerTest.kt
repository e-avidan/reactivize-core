package il.ac.technion.cs.reactivize

import org.junit.jupiter.api.Test
import java.io.File

class ReactivizePostCompilerTest {
    @Test
    fun sanity() {
        ReactivizePostCompiler()
    }

    @Test
    fun `empty execute`() {
        val pc = ReactivizePostCompiler()
        val spec = ReactivizeCompileSpec(
            File.createTempFile("dest", null),
            File.createTempFile("work", null),
            File.createTempFile("temp", null),
            listOf(),
            listOf(),
            listOf(),
            listOf()
        )
        pc.execute(spec)
    }
}