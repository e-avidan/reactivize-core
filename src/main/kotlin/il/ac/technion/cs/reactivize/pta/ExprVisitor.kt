package il.ac.technion.cs.reactivize.pta

import il.ac.technion.cs.reactivize.pta.PtgForwardAnalysis.AnalysisType
import soot.Local
import soot.jimple.*
import java.util.*

class ExprVisitor(var analysis: AnalysisType) : AbstractJimpleValueSwitch() {
    var exprInfo: ExprInfo? = null
        protected set

    override fun caseInstanceFieldRef(arg0: InstanceFieldRef) {
        exprInfo = ExprInfo()
        if (arg0.field.name == "this$0") {
            // XXX: Hack, es para que this.this$0 = il.ac.technion.cs.reactivize.pta.Tests, es decir, la clase.
            // Como no se como funciona, lo voy a evitar
            exprInfo!!.isConstructorAccess = true
        }
        exprInfo!!.field = arg0.field.name
        exprInfo!!.`var` = arg0.base.toString()
        exprInfo!!.isFieldAccess = true
    }

    override fun caseStaticFieldRef(arg0: StaticFieldRef) {
        exprInfo = ExprInfo()
        exprInfo!!.field = arg0.field.name
        exprInfo!!.`var` = arg0.type.toString() + "_STATIC"
        exprInfo!!.isFieldAccess = true
        exprInfo!!.isStatic = true
    }

    override fun caseLocal(arg0: Local) {
        exprInfo = ExprInfo()
        exprInfo!!.`var` = arg0.name
    }

    override fun caseNewExpr(arg0: NewExpr) {
        exprInfo = ExprInfo()
        exprInfo!!.`var` = arg0.type.toString()
        exprInfo!!.isNewObj = true
    }

    /*** Analisis E4  */
    override fun caseSpecialInvokeExpr(arg0: SpecialInvokeExpr) {
        if (analysis == AnalysisType.E4) {
            generalInvokeInfo(arg0)
            exprInfo!!.receiver = arg0.base.toString()
        } else {
            super.caseSpecialInvokeExpr(arg0)
        }
    }

    override fun caseStaticInvokeExpr(arg0: StaticInvokeExpr) {
        if (analysis == AnalysisType.E4) {
            generalInvokeInfo(arg0)
            exprInfo!!.isStatic = true
            exprInfo!!.receiver = arg0.method.declaringClass.toString()
        } else {
            super.caseStaticInvokeExpr(arg0)
        }
    }

    override fun caseVirtualInvokeExpr(arg0: VirtualInvokeExpr) {
        if (analysis == AnalysisType.E4) {
            generalInvokeInfo(arg0)
            exprInfo!!.receiver = arg0.base.toString()
        } else {
            super.caseVirtualInvokeExpr(arg0)
        }
    }

    override fun caseParameterRef(arg0: ParameterRef) {
        if (analysis == AnalysisType.E4) {
            exprInfo = ExprInfo()
            exprInfo!!.isParameter = true
            exprInfo!!.paramIndex = arg0.index
        } else {
            super.caseParameterRef(arg0)
        }
    }

    override fun caseThisRef(arg0: ThisRef) {
        if (analysis == AnalysisType.E4) {
            exprInfo = ExprInfo()
            exprInfo!!.isThis = true
        } else {
            super.caseThisRef(arg0)
        }
    }

    fun generalInvokeInfo(arg0: InvokeExpr) {
        exprInfo = ExprInfo()
        exprInfo!!.isInvocation = true
        exprInfo!!.methodSignature = arg0.method.subSignature
        exprInfo!!.args = ArrayList()
        for (v in arg0.args) exprInfo!!.args?.add(v.toString())
    }
}
