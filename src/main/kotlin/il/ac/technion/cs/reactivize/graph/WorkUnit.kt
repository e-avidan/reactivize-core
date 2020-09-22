package il.ac.technion.cs.reactivize.graph

interface WorkUnit {
    val subunits: List<WorkUnit>

    fun accept(visitor: WorkUnitVisitor)

    fun describe(): String {
        return "${describeThis()}\n" + subunits.joinToString("\n", postfix = "\n", transform = WorkUnit::describe)
            .prependIndent("    ")
    }

    fun describeThis(): String = "${this::class.simpleName}(...)"
}

interface WorkUnitVisitor {
    fun visit(v: ValueMethod)
    fun visit(v: FunctionCallingMethod)
    fun visit(v: ClassWithObservable)
    fun visit(v: ReactivizableFieldInClass)
}
