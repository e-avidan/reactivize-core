package il.ac.technion.cs.reactivize.pta

import soot.Local
import soot.Value

class PTAGraph {
    var locals: MutableMap<Local, MutableSet<Value>> = HashMap()

    companion object {
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

    fun merge(src: PTAGraph){
        mergeMapsOfSets(locals, src.locals)
    }

    fun copy(src: PTAGraph) {
        reset()
        merge(src)
    }

    private fun reset() {
        locals = HashMap()
    }

    override fun equals(other: Any?): Boolean {
        // TODO
        return super.equals(other)
    }

    override fun hashCode(): Int {
        // TODO
        return super.hashCode()
    }

    fun addLocal(local: Local, value: Value) {
        getNestedMapSet(locals, local).add(value)
    }
}
