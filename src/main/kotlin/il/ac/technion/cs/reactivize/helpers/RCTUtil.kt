package il.ac.technion.cs.reactivize.helpers

import soot.SootMethod

object RCTUtil {
    val BUILTIN_METHOD_PREFIXES = listOf("sun", "java", "kotlin", "jvm", "io.reactivex")

    fun <K, V> mergeMapsOfSets(dest: MutableMap<K, MutableSet<V>>, src: Map<K, MutableSet<V>>) {
        for ((key, values) in src.entries) {
            getNestedMapSet(dest, key).addAll(values)
        }
    }

    fun <K, V> getNestedMapSet(map: MutableMap<K, MutableSet<V>>, key: K): MutableSet<V> {
        var set = map.get(key)

        if (set == null) {
            set = HashSet()
            map.set(key, set)
        }

        return set
    }

    fun isBuiltinMethod(method: SootMethod): Boolean {
        val packageName = method.declaringClass.packageName
        return BUILTIN_METHOD_PREFIXES.any { packageName.startsWith(it) }
    }
}
