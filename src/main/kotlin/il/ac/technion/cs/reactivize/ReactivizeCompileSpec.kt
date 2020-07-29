package il.ac.technion.cs.reactivize

import java.io.File

data class ReactivizeCompileSpec(
    val destinationDir: File,
    val workingDir: File,
    val tempDir: File,
    val compileClasspath: Iterable<File>,
    val inpath: Iterable<File>,
    val classNames: Iterable<String>
)