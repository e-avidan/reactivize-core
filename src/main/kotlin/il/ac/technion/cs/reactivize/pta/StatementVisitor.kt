package il.ac.technion.cs.reactivize.pta

import soot.Local
import soot.jimple.*

class StatementVisitor(val graph: PTAGraph) : AbstractStmtSwitch() {
    override fun caseInvokeStmt(stmt: InvokeStmt) {
        defaultCase(stmt)
    }

    override fun caseAssignStmt(stmt: AssignStmt) {
        defaultCase(stmt)
    }

    override fun caseIdentityStmt(stmt: IdentityStmt) {
        graph.addLocal(stmt.leftOp as Local, stmt.rightOp)
    }

    override fun caseIfStmt(stmt: IfStmt) {
        defaultCase(stmt)
    }

    override fun caseRetStmt(stmt: RetStmt) {
        defaultCase(stmt)
    }

    override fun caseReturnStmt(stmt: ReturnStmt) {
        defaultCase(stmt)
    }

    override fun caseReturnVoidStmt(stmt: ReturnVoidStmt) {
        defaultCase(stmt)
    }

    override fun caseThrowStmt(stmt: ThrowStmt) {
        defaultCase(stmt)
    }

    override fun defaultCase(obj: Any) {
        println("Switch default on: $obj")
    }
}
