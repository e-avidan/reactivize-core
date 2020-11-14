package il.ac.technion.cs.reactivize.pta

import org.apache.commons.lang3.builder.HashCodeBuilder

class Edge(var source: String?, var field: String?, var target: String?) {
    override fun hashCode(): Int {
        return HashCodeBuilder(17, 31).append(source).append(target).append(field).toHashCode()
    }

    override fun equals(obj: Any?): Boolean {
        if (obj !is Edge) return false
        if (obj === this) return true
        val edge = obj
        return target == edge.target && source == edge.source && field == edge.field
    }

    override fun toString(): String {
        return "($source,$field,$target)"
    }
}
