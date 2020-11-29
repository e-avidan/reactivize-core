package il.ac.technion.cs.reactivize.helpers

object RCTUtil {
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
}
