package il.ac.technion.cs.reactivize.pta.visitors

import soot.Value
import soot.jimple.*

class ReadValuesFilterVisitor : AbstractJimpleValueSwitch() {
    private val resultingValues: MutableList<Value> = ArrayList()

    fun applyTo(value: Value): List<Value> {
        // TODO: decide if use boxes or not
        value.useBoxes.forEach { it.value.apply(this) }
        return resultingValues
    }

    override fun caseArrayRef(v: ArrayRef) {
        v.base.apply(this)
        v.index.apply(this)
    }

    override fun caseDynamicInvokeExpr(v: DynamicInvokeExpr) {
        throw Exception("Please god no...")
    }

    override fun caseCastExpr(v: CastExpr) {
        v.op.apply(this)
    }

    override fun caseInstanceOfExpr(v: InstanceOfExpr) {
        v.op.apply(this)
    }

    override fun caseNewArrayExpr(v: NewArrayExpr) {
        v.size.apply(this)
    }

    override fun caseNewMultiArrayExpr(v: NewMultiArrayExpr) {
        throw Exception("Please god no...")
    }

    override fun caseNewExpr(v: NewExpr) {
        // New expr are not interesting - they are followed by an init invocation
        return
    }

    override fun caseInstanceFieldRef(v: InstanceFieldRef) {
        // We want both the field in context, and the base
        v.base.apply(this)
        defaultCase(v)
    }

    private fun tryAddValueToResult(v: Value) {
        // Ignoring these
        if (v is Constant) {
            return
        }

        if (!true) {
            println("${ReadValuesFilterVisitor::class} default on: $v")
            return
        }

        resultingValues.add(v)
    }

    private fun handleUnaryOp(expr: UnopExpr) {
        expr.op.apply(this)
    }

    private fun handleBinaryOp(expr: BinopExpr) {
        expr.op1.apply(this)
        expr.op2.apply(this)
    }

    private fun handleInvokeOp(expr: InvokeExpr) {
        if (expr is InstanceInvokeExpr) {
            expr.base.apply(this)
        }

        expr.args.forEach { it.apply(this) }

        // TODO: look at method - we will want to link the related graphs
    }

    override fun defaultCase(obj: Any) {
        // Handle all similar wrappers the same
        if (obj is UnopExpr) {
            handleUnaryOp(obj)
            return
        }

        if (obj is BinopExpr) {
            handleBinaryOp(obj)
            return
        }

        if (obj is InvokeExpr) {
            handleInvokeOp(obj)
            return
        }

        // The rest can reach the adder :-)
        tryAddValueToResult(obj as Value)
    }
}
