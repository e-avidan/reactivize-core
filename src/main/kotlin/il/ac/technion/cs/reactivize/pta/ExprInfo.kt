package il.ac.technion.cs.reactivize.pta

class ExprInfo {
    var isFieldAccess = false
    var isNewObj = false
    var field = "_NO_FIELD_"
    var `var` = "_NO_VAR_"
    var isStatic = false

    /* Invocations */
    var isInvocation = false
    var methodSignature: String? = null
    var args: MutableList<String?>? = null
    var receiver: String? = null

    /* Parameters */
    var isParameter = false
    var paramIndex = 0
    var isThis = false
    var isConstructorAccess = false
}
