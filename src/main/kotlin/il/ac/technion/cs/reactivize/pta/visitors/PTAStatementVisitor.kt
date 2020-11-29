package il.ac.technion.cs.reactivize.pta.visitors

import il.ac.technion.cs.reactivize.pta.PTAGraph
import soot.*
import soot.jimple.*
import soot.jimple.internal.*

class PTAStatementVisitor(val graph: PTAGraph) : AbstractStmtSwitch() {
    override fun caseIdentityStmt(stmt: IdentityStmt) {
        println(stmt)
        throw Exception("Haven't managed to make this happen yet")
    }

    override fun caseAssignStmt(stmt: AssignStmt) {
        val left = stmt.leftOp
        val right = stmt.rightOp

        // NewExpr will be immediately followed by <init> invocation with args
        if (right is AbstractNewExpr) {
            return
        }

        // TODO: Type visitor?
        var type: SootClass? = null
        if (right is AbstractInvokeExpr) {
            val typeObj = right.method.returnType

            if (typeObj is RefType) {
                type = typeObj.sootClass
            } else if (typeObj is PrimType) {
                type = typeObj.boxedType().sootClass
            }
        }

        // TODO: deal with all these writes, if we care...
        if (right is InstanceFieldRef) {
            graph.link(right.base, stmt)
        }

        // Reads for all RHS values
        val hasDependencies = if (right is AbstractInvokeExpr) {
            processInvokeExpr(right, stmt)
        } else {
            processReadValues(stmt, right) || right is InstanceFieldRef
        }

        // New write
        graph.introduce(left, right, type = type, isRoot = !hasDependencies)
    }

    override fun caseIfStmt(stmt: IfStmt) {
        println(stmt)
    }

    override fun caseInvokeStmt(stmt: InvokeStmt) {
        processInvokeExpr(stmt.invokeExpr, stmt)
    }

    private fun processInvokeExpr(expr: InvokeExpr, stmt: Stmt): Boolean {
        // Post NewExpr init
        if (expr is AbstractSpecialInvokeExpr && expr.method.name == "<init>") {
            val newType = expr.method.declaringClass
            val hasDependencies = expr.args.map { processReadValues(stmt, it) }.any { it }

            graph.introduce(expr.base, expr, type = newType, isRoot = !hasDependencies)

            // TODO: handle lambda-generated classes

            // ASSUMPTION: don't care about ctor analysis
            return hasDependencies
        }

        // TODO: Connect to graph of call if this is our code
        return processReadValues(stmt, expr)
    }

    override fun caseReturnStmt(stmt: ReturnStmt) {
        println(stmt)
    }

    override fun caseReturnVoidStmt(stmt: ReturnVoidStmt) {
        println(stmt)
    }

    private fun processReadValues(stmt: Stmt, value: Value): Boolean {
        val values = ReadValuesFilterVisitor().applyTo(value)
        values.forEach { graph.link(it, stmt) }

        return values.isNotEmpty()
    }

    override fun defaultCase(obj: Any) {
        println("Switch default on: $obj")
    }
}
