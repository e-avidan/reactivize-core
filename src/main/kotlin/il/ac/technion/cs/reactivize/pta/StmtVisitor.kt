package il.ac.technion.cs.reactivize.pta

import il.ac.technion.cs.reactivize.pta.PtgForwardAnalysis.AnalysisType
import soot.jimple.*
import java.util.*

class StmtVisitor(private val ptg: PointsToGraph, var analysis: AnalysisType) : AbstractStmtSwitch() {
    override fun caseAssignStmt(arg0: AssignStmt) {
        val leftOpVisitor = ExprVisitor(analysis)
        val rightOpVisitor = ExprVisitor(analysis)
        arg0.leftOp.apply(leftOpVisitor)
        arg0.rightOp.apply(rightOpVisitor)

        /* Solo importan los objetos para el PTG */
        val leftExprInfo = leftOpVisitor.exprInfo
        val rightExprInfo = rightOpVisitor.exprInfo
        if (leftExprInfo == null || rightExprInfo == null || leftExprInfo.isConstructorAccess) return

        // p: x = new A()
        if (rightExprInfo.isNewObj) {
            // G� = G con L�(x) = {A_p}
            val line = arg0.javaSourceStartLineNumber
            val x = leftExprInfo.`var`
            val count = ptg.lineCount(line, leftExprInfo.`var`)
            val A_p = rightExprInfo.`var` + "_" + count + "_" + line
            val set: MutableSet<String> = HashSet()
            set.add(A_p)
            ptg.locals[x] = set
        } else if (!leftExprInfo.isFieldAccess && !rightExprInfo.isFieldAccess && !rightExprInfo.isInvocation) {
            // G� = G con L�(x) = L(y)
            val x = leftExprInfo.`var`
            val y = rightExprInfo.`var`
            if (ptg.locals[y] != null) {
                /* Si locals no tiene a "y" entonces es porque no es un obj */
                val set: MutableSet<String> = HashSet()
                set.addAll(ptg.locals[y]!!)
                ptg.locals[x] = set
            }
        } else if (!leftExprInfo.isFieldAccess && rightExprInfo.isFieldAccess) {
            if (analysis == AnalysisType.E2) {
                // G� = G con R� = R U { (n,f,ln) | n in L(y) } y L�(x) = { ln} con ln fresco
                val x = leftExprInfo.`var`
                val y = rightExprInfo.`var`
                var set: Set<String?>? = HashSet()
                set = if (leftExprInfo.isStatic) {
                    makeSureStaticExists(y, ptg)
                    ptg.statics[y]
                } else {
                    ptg.locals[y]
                }

                // L�(x) = { ln} con ln fresco
                val line = arg0.javaSourceStartLineNumber
                val ln = "ln_$line"
                val lset: MutableSet<String> = HashSet()
                lset.add(ln)
                ptg.locals[x] = lset

                // R� = R U { (n,f,ln) | n in L(y) }
                for (local in set!!) {
                    val e = Edge(local, rightExprInfo.field, ln)
                    ptg.redges.add(e)
                }
            } else {
                // G� = G con L�(x) = { n | (a,f,n) in E forall a in L(y)}
                val x = leftExprInfo.`var`
                val y = rightExprInfo.`var`
                var set: Set<String>? = HashSet()
                set = if (rightExprInfo.isStatic) {
                    makeSureStaticExists(y, ptg)
                    ptg.statics[y]
                } else {
                    ptg.locals[y]
                }
                ptg.locals[x] = HashSet()
                for (s in set!!) {
                    for (edge in ptg.edges) {
                        if (edge!!.source == s) {
                            ptg.locals[x]!!.add(edge.target!!)
                        }
                    }
                }
            }
        } else if (leftExprInfo.isFieldAccess && !rightExprInfo.isFieldAccess) {
            // G� = G con E� = E U { (a,f,n) | n in L(y) && a in L(x) }
            val x = leftExprInfo.`var`
            val f = leftExprInfo.field
            val y = rightExprInfo.`var`
            var set: Set<String?>? = HashSet()
            set = if (leftExprInfo.isStatic) {
                makeSureStaticExists(x, ptg)
                ptg.statics[x]
            } else {
                ptg.locals[x]
            }
            for (n in ptg.locals[y]!!) {
                for (a in set!!) {
                    val e = Edge(a, f, n)
                    ptg.edges.add(e)
                }
            }
        } else if (rightExprInfo.isInvocation) {
            if (analysis == AnalysisType.E4) {
                val ret = analyseInvocationExpr(rightExprInfo, arg0.javaSourceStartLineNumber)
                val x = leftExprInfo.`var`
                if (leftExprInfo.isFieldAccess) {
                    val f = leftExprInfo.field
                    for (n in ret) {
                        for (a in ptg.locals[x]!!) {
                            val e = Edge(a, f, n)
                            ptg.edges.add(e)
                        }
                    }
                } else {
                    val set: MutableSet<String> = HashSet()
                    set.addAll(ret)
                    ptg.locals[x] = set
                }
            }
        } else {
            println("Error: Assign no contemplado!")
        }
    }

    override fun caseIdentityStmt(arg0: IdentityStmt) {
        when (analysis) {
            AnalysisType.E1 -> {
                super.caseIdentityStmt(arg0)
            }
            AnalysisType.E2 -> {
                val leftOpVisitor = ExprVisitor(analysis)
                val rightOpVisitor = ExprVisitor(analysis)
                arg0.leftOp.apply(leftOpVisitor)
                arg0.rightOp.apply(rightOpVisitor)

                /* Solo importan los objetos para el PTG */
                val leftExprInfo = leftOpVisitor.exprInfo ?: return

                // El this lo tomo como parametro.
                val p = leftExprInfo.`var`
                val PN = "PN_$p"
                val set: MutableSet<String> = HashSet()
                set.add(PN)
                ptg.locals[p] = set
            }
            AnalysisType.E4 -> {
                val leftOpVisitor = ExprVisitor(analysis)
                val rightOpVisitor = ExprVisitor(analysis)
                arg0.leftOp.apply(leftOpVisitor)
                arg0.rightOp.apply(rightOpVisitor)
                val leftExprInfo = leftOpVisitor.exprInfo
                val rightExprInfo = rightOpVisitor.exprInfo
                var arg: MutableSet<String> = HashSet()
                if (rightExprInfo!!.isParameter) {
                    arg = ptg.args[rightExprInfo!!.paramIndex]!!
                } else if (rightExprInfo!!.isThis) {
                    arg = ptg.thisnodes
                }

                /* Si no esta vacio, entonces son argumentos que apuntan a algo en el PTG
				 * por lo que lo agrego a las locales. */if (!arg!!.isEmpty()) {
                    val p = leftExprInfo!!.`var`
                    ptg.locals[p] = arg
                }
            }
        }
    }

    override fun caseInvokeStmt(stmt: InvokeStmt) {
        if (analysis == AnalysisType.E4) {
            val visitor = ExprVisitor(analysis)
            stmt.invokeExpr.apply(visitor)
            val exprInfo = visitor.exprInfo
            analyseInvocationExpr(exprInfo, stmt.javaSourceStartLineNumber)
        }
    }

    override fun caseReturnStmt(stmt: ReturnStmt) {
        if (analysis == AnalysisType.E4) {
            if (ptg.locals.containsKey(stmt.op.toString())) ptg.retnodes.addAll(ptg.locals[stmt.op.toString()]!!)
        }
    }

    private fun analyseInvocationExpr(exprInfo: ExprInfo?, line: Int): Set<String> {
        val ret: MutableSet<String> = HashSet()

        /* Se evita la recursion infinita */if (exprInfo!!.methodSignature == ptg.methodSignature) {
            ptg.wrongs.add("rec_" + ptg.className + "_" + ptg.methodSignature)
            return ret
        }
        val effectiveTypes: MutableSet<String> = HashSet()
        var receiverNodes: MutableSet<String> = HashSet()
        if (!exprInfo.isStatic) {
            /* El receiver debe pertenecer al PTG */
            if (!ptg.locals.containsKey(exprInfo.receiver)) {
                println("Error fatal! No existe el receiver en el PTG!")
                System.exit(0)
            }

            /* Se extraen los tipos efectivos. */receiverNodes = ptg.locals[exprInfo.receiver]!!
            for (node in receiverNodes!!) {
                effectiveTypes.add(node!!.substring(0, node.indexOf("_")))
            }
        } else {
            effectiveTypes.add(exprInfo.receiver!!)
        }

        /* Por cada argumento, se extraen los nodos a los que apuntan. */
        /* TODO: Esto vale solo para x, no para x.f! OJO! */
        val argsNodes: MutableList<MutableSet<String>> = ArrayList()
        for (arg in exprInfo.args!!) {
            val set: MutableSet<String> = HashSet()
            if (ptg.locals.containsKey(arg)) {
                set.addAll(ptg.locals[arg]!!)
            }
            argsNodes.add(set)
        }

        /* Se guardan los argumentos y locales de este scope */
        val currentArgs = ptg.args
        val currentLocals = ptg.locals
        val currentMethodSignature = ptg.methodSignature
        val currentClassName = ptg.className
        val currentThis = ptg.thisnodes

        /* Se genera el analisis por cada metodo que podr�a haberse llamado. */for (effectiveType in effectiveTypes) {
            ptg.args = argsNodes
            ptg.thisnodes = receiverNodes
            ptg.locals = HashMap()
            val edge = Edge(
                currentClassName + "_" + currentMethodSignature,
                "l_$line",
                effectiveType + "_" + exprInfo.methodSignature
            )
            ptg.callgraph.add(edge)
            var p: PointsToGraph? = null
            try {
                val analysis = PtgForwardAnalysis(exprInfo.methodSignature, effectiveType, ptg, AnalysisType.E4)
                p = analysis.pointsToGraph
                p!!.merge(ptg)
            } catch (e: Exception) {
                /* Si no puede cargar la clase, no la analizo */
                if (!e.message!!.contains("No method")) {
                    println("Imposible analizar metodo: " + exprInfo.methodSignature + ", clase: " + effectiveType)
                    e.printStackTrace()
                }
            }
            ptg.thisnodes = currentThis
            ptg.locals = currentLocals
            ptg.methodSignature = currentMethodSignature
            ptg.className = currentClassName
            ptg.args = currentArgs
            ret.addAll(ptg.retnodes)

            // Si no es static, a los que apunta "this", hay que ligarlos al receiver.
            if (!exprInfo.isStatic && p != null) {
                for (thisl in p.locals["this"]!!) {
                    for (e in p.edges) {
                        if (e!!.source == thisl) {
                            for (rel in ptg.locals[exprInfo.receiver]!!) {
                                val ree = Edge(rel, e.field, e.target)
                                ptg.edges.add(ree)
                            }
                        }
                    }
                }
            }
        }
        return ret
    }

    companion object {
        fun makeSureStaticExists(staticName: String?, ptg: PointsToGraph) {
            if (!ptg.statics.containsKey(staticName)) {
                val set: MutableSet<String> = HashSet()
                set.add(staticName!!)
                ptg.statics[staticName] = set
            }
        }
    }
}
