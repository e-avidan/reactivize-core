package il.ac.technion.cs.reactivize.graph

import soot.SootMethod

interface ReactivizableMethod : WorkUnit {
    val sootMethod: SootMethod
    val subscriberMethodName: String
}