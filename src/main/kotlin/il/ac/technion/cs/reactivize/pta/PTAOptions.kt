package il.ac.technion.cs.reactivize.pta

import soot.SootClass
import soot.SootMethod

data class PTAOptions(val className: String, val methodSignature: String, val analysisMethods: Set<SootMethod>) {
    val analysisClasses: Set<SootClass> by lazy {
        analysisMethods.map { it.declaringClass }.toSet()
    }
}
