package il.ac.technion.cs.reactivize.pta

import il.ac.technion.cs.reactivize.helpers.RCTUtil
import soot.Local
import soot.Value

// TODO: probably DELETE, unless graph doesn't work out
class PTAGraphWithSets {
    var locals: MutableMap<Local, MutableSet<Value>> = HashMap()
    var edges: MutableMap<Local, MutableSet<Value>> = HashMap()

    fun merge(src: PTAGraphWithSets){
        RCTUtil.mergeMapsOfSets(locals, src.locals)
    }

    fun copy(src: PTAGraphWithSets) {
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
        RCTUtil.getNestedMapSet(locals, local).add(value)
    }
}
