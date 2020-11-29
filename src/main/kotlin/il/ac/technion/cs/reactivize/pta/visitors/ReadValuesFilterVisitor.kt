package il.ac.technion.cs.reactivize.pta.visitors

import il.ac.technion.cs.reactivize.helpers.RCTUtil
import soot.SootMethod
import soot.Value
import soot.jimple.*

class ReadValuesFilterVisitor : AbstractJimpleValueSwitch() {
    private val resultingValues: MutableList<Value> = ArrayList()
    var methodCalls: Map<SootMethod, List<Value?>>? = null

    fun applyTo(value: Value): List<Value> {
        if (resultingValues.isNotEmpty()) {
            throw Exception("Cannot reuse a visitor!")
        }

        value.apply(this)
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
        // TODO: maybe static fields are interesting?
        if (v is Constant || v is StaticFieldRef) {
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

        // For anything built-in, we'd rather just process the read and ignore the method call
        if (RCTUtil.isBuiltinMethod(expr.method)) {
            expr.args.forEach { it.apply(this) }
            return
        }

        val resolvedArgs = expr.args.map(this::applyAndGet)

        // No values are passing, don't care about this sub-method
        // TODO: what about side effects?
        if (!resolvedArgs.any {it != null}) {
            return
        }

        // TODO: instance context etc.
        // Values are passing, gotta process that method as well
        methodCalls = mapOf(expr.method to resolvedArgs)
    }

    private fun applyAndGet(value: Value): Value? {
        val oldResultCount = resultingValues.size

        value.apply(this)

        return if (oldResultCount == resultingValues.size) {
            null
        } else {
            resultingValues[resultingValues.size - 1]
        }
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
