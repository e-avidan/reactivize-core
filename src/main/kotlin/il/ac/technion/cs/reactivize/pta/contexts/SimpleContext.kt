package il.ac.technion.cs.reactivize.pta.contexts

import org.apache.commons.lang3.builder.HashCodeBuilder

class SimpleContext(val line: Int) {
    override fun hashCode(): Int {
        return HashCodeBuilder(17, 31).append(line).toHashCode()
    }

    override fun equals(obj: Any?): Boolean {
        if (obj !is SimpleContext) return false
        if (obj === this) return true
        return line == obj.line
    }

    override fun toString(): String {
        return "($line)"
    }
}
