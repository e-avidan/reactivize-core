package il.ac.technion.cs.reactivize.pta

import soot.SootClass

data class PTAOptions(val className: String, val methodSignature: String, val analysisClasses: Set<SootClass>) {
}
