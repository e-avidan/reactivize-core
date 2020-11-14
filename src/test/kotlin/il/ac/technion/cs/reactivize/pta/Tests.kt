package il.ac.technion.cs.reactivize.pta

class Tests {
    // XXX: test1 DEBE quedar fijo, porque se mira el numero de linea.
    fun test1() {
        val a = ClassA()
    }

    fun test2() {
        val a = ClassA()
        val a2 = a
    }

    fun test3() {
        val a = ClassA()
        a.classBAttribute = ClassB()
        var b: ClassB? = ClassB()
        b = a.classBAttribute
    }

    fun test4() {
        val a = ClassA()
        val b = ClassB()
        a.classBAttribute = b
    }

    fun test5() {
        val a = ClassA()
        val a2 = ClassA()
        a2.classBAttribute = ClassB()
        a.classBAttribute = a2.classBAttribute
    }

    fun test6() {
        val a = ClassA()
        val a2 = ClassA()
    }

    fun test7() {
        var a: ClassA? = ClassA()
        for (i in 0..4) {
            val a2 = ClassA()
            a = a2
        }
    }

    fun test8() {
        var a = ClassA()
        val x = 1
        a = if (x > 2) {
            ClassA()
        } else {
            ClassA()
        }
    }

    fun test9() {
        val a = ClassA()
        a.classAAttribute = ClassA()
        a.classAAttribute = ClassA()
        var a2: ClassA? = ClassA()
        a2 = a
        a2 = a.classAAttribute
    }

    fun test10(x: Any?, y: Any?) {}
    fun test11() {
        val a = ClassA()
        a.classBAttribute = ClassB()
        var b: ClassB? = ClassB()
        var b2: ClassB? = ClassB()
        b = a.classBAttribute
        b2 = a.classBAttribute
    }

    fun test12() {
        val c: TestClassC = TestClassC.instance!!
    }

    fun test13() {
        val o = Any()
        val a = modify(o)
    }

    fun test14() {
        var a = ClassA()
        val x = 1
        if (x > 2) {
            a = ClassA()
            return
        } else {
            a = ClassA()
            return
        }
    }

    /* XXX: Por el new esto tiene que quedar siempre en linea 99!!*/
    private fun modify(x: Any): Any {
        var x: Any? = x
        x = Any()
        return x
    }

    fun test15() {
        val a = ClassA()
        a.ObjectAttribute = Any()
        a.ObjectAttribute = modify(a)
    }

    var classAAttribute // Si lo instancio aca no aparece en el analisis.
            : ClassA? = null

    fun test16() {
        classAAttribute = ClassA()
    }

    fun test17() {
        val c: TestClassC = TestClassC.instance!!
    }

    fun test18() {
        staticAAttribute = ClassA()
        val a = staticAAttribute
    }

    fun test19() {
        instanceAAttribute = ClassA()
        val a = instanceAAttribute
    }

    inner class ClassA {
        var classBAttribute: ClassB? = null
        var classAAttribute: ClassA? = null
        var ObjectAttribute: Any? = null
        fun initClassBAttribute() {
            classBAttribute = ClassB()
        }
    }

    fun test20() {
        val a = ClassA()
        a.initClassBAttribute()
        val b = a.classBAttribute
    }

    inner class ClassB {
        var classAAttribute: ClassA? = null
        fun func() {
            val a = ClassA()
            a.initClassBAttribute()
            classAAttribute = a
        }
    }

    fun test21() {
        val b = classBFromConstructor()
    }

    private fun classBFromConstructor(): ClassB? {
        val b = ClassB()
        b.func()
        return b.classAAttribute!!.classBAttribute
    }

    fun test22() {
        val c = TestClassC()
        val y = c.recursive(2)
    }

    fun test23() {
        val a = ClassA()
        a.initClassBAttribute()
    }

    fun test24() {
        val a = ClassA()
        a.initClassBAttribute()
        a.initClassBAttribute()
    }

    fun test25() {
        val x = 1
        var a: Animal? = null
        a = if (x > 2) {
            Dog()
        } else {
            Cat()
        }
        val z = a.lives()
    }

    fun test26() {
        val b = classBFromConstructor()
    }

    fun modifySelf() {
        classAAttribute = ClassA()
    }

    fun test27() {
        modifySelf()
        val a = classAAttribute
    }

    fun test28() {
        val a = ClassA()
        val c = TestClassC(a)
        val a2: ClassA? = c.classAAttribute
    }

    internal inner class ClassD {
        fun returnClassA(): ClassA {
            return ClassA()
        }
    }

    fun test29() {
        val d = ClassD()
        var a: ClassA? = null
        for (i in 0..3) {
            a = d.returnClassA()
        }
        val a2 = a
    }

    fun test30() {
        val a = ClassA()
        func1(a)
        val b = a.classBAttribute
    }

    fun func1(a: ClassA) {
        func2(a)
    }

    fun func2(a: ClassA) {
        a.classBAttribute = ClassB()
    }

    companion object {
        var instanceAAttribute: ClassA? = null
        var staticAAttribute: ClassA? = null
    }
}
