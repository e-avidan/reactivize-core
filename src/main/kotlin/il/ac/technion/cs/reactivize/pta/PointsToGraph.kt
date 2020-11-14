package il.ac.technion.cs.reactivize.pta

import java.util.*
import kotlin.collections.ArrayList

class PointsToGraph {
    var edges = HashSet<Edge>()
    var redges = HashSet<Edge>()
    var locals = HashMap<String, MutableSet<String>>()
    var wrongs = HashSet<String>()
    var lines = HashMap<Int, MutableSet<String>>()
    var statics = HashMap<String, MutableSet<String>>()

    /* Interprocedural */
    var args: MutableList<MutableSet<String>> = ArrayList()
    var thisnodes: MutableSet<String> = HashSet()
    var methodSignature: String? = null
    var className: String? = null
    var retnodes = HashSet<String>()

    /* CallGraph */
    var callgraph = HashSet<Edge>()
    fun addThis() {
        val p = "this"
        val TN = className + "_THIS"
        val set: MutableSet<String> = HashSet()
        set.add(TN)
        locals[p] = set
        thisnodes = HashSet()
        thisnodes.addAll(set)
    }

    fun merge(dst: PointsToGraph) {
        dst.edges.addAll(edges)
        dst.redges.addAll(redges)

        for (local in locals.keys) {
            if (dst.locals.containsKey(local)) {
                val l = dst.locals[local]
                l!!.addAll(locals[local]!!)
                dst.locals[local] = l
            } else {
                dst.locals[local] = locals[local]!!
            }
        }

        dst.wrongs.addAll(wrongs)
        for (line in lines.keys) {
            if (dst.lines.containsKey(line)) {
                val l = dst.lines[line]
                l!!.addAll(lines[line]!!)
                dst.lines[line] = l
            } else {
                dst.lines[line] = lines[line]!!
            }
        }
        dst.retnodes.addAll(retnodes)
        for (s in statics.keys) {
            if (dst.statics.containsKey(s)) {
                val l = dst.statics[s]!!
                l.addAll(statics[s]!!)
                dst.statics[s] = l
            } else {
                dst.statics[s] = statics[s]!!
            }
        }
        dst.callgraph.addAll(callgraph)
    }

    fun copy(dest: PointsToGraph) {
        dest.edges = HashSet()
        dest.redges = HashSet()
        dest.locals = HashMap()
        dest.wrongs = HashSet()
        dest.args = ArrayList()
        dest.thisnodes = HashSet()
        dest.retnodes = HashSet()
        dest.statics = HashMap()
        dest.callgraph = HashSet()
        for (e in edges) {
            dest.edges.add(Edge(e!!.source, e.field, e.target))
        }
        for (e in redges) {
            dest.redges.add(Edge(e!!.source, e.field, e.target))
        }
        for (local in locals.keys) {
            val l: Set<String>? = locals[local]
            val dstl: MutableSet<String> = HashSet()
            dstl.addAll(l!!)
            dest.locals[local] = dstl
        }
        dest.wrongs.addAll(wrongs)
        for (line in lines.keys) {
            val l: MutableSet<String> = lines[line!!]!!
            val dstl: MutableSet<String> = HashSet()
            dstl.addAll(l!!)
            dest.lines[line] = dstl
        }
        for (s in args) {
            val ds: MutableSet<String> = HashSet()
            ds.addAll(s)
            dest.args.add(ds)
        }
        dest.methodSignature = methodSignature
        dest.className = className
        dest.thisnodes.addAll(thisnodes)
        dest.retnodes.addAll(retnodes)
        for (s in statics.keys) {
            val l = statics[s]
            val dstl: MutableSet<String> = HashSet()
            dstl.addAll(l!!)
            dest.statics[s] = dstl
        }
        for (e in callgraph) {
            dest.callgraph.add(Edge(e!!.source, e.field, e.target))
        }
    }

    override fun equals(obj: Any?): Boolean {
        if (obj !is PointsToGraph) return false
        if (obj === this) return true
        val graph = obj
        return edges == graph.edges && redges == graph.edges && locals == graph.locals && wrongs == graph.wrongs && lines == graph.lines && args == graph.args && statics == graph.statics && callgraph == graph.callgraph
    }

    fun lineCount(line: Int, variable: String?): Int {
        var count = 1
        if (lines.containsKey(line)) {
            if (!lines[line]!!.contains(variable)) {
                lines[line]!!.add(variable!!)
            }
            count = lines[line]!!.size
        } else {
            val set = HashSet<String>()
            set.add(variable!!)
            lines[line] = set
        }
        return count
    }
}
