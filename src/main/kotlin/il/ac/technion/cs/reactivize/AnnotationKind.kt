package il.ac.technion.cs.reactivize

import il.ac.technion.cs.reactivize.api.Reactivize
import il.ac.technion.cs.reactivize.api.ReactivizeValue
import kotlin.reflect.KClass

enum class AnnotationKind(private val annotation: KClass<out Annotation>) {
    REACTIVIZE(Reactivize::class),
    REACTIVIZE_VALUE(ReactivizeValue::class);

    companion object {
        fun parse(bytecodeName: String) = when (bytecodeName) {
            REACTIVIZE.bytecodeName -> REACTIVIZE
            REACTIVIZE_VALUE.bytecodeName -> REACTIVIZE_VALUE
            else -> null
        }
    }

    val bytecodeName: String
        get() = annotation.bytecodeName
}

val <T : Annotation> KClass<T>.bytecodeName: String
    get() = "L${this.qualifiedName!!.replace(".", "/")};"