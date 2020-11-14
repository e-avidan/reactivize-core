package il.ac.technion.cs.reactivize.pta

import il.ac.technion.cs.reactivize.pta.Tests.ClassA

class TestClassC {
    constructor() {}

    var classAAttribute: ClassA? = null

    constructor(a: ClassA?) {
        classAAttribute = a
    }

    fun recursive(i: Int): Int {
        var i = i
        var ret = 2
        if (i > 0) {
            i--
            ret = recursive(i) * 2
        }
        return ret
    }

    companion object {
        var instance: TestClassC? = null
            get() {
                if (field == null) {
                    field = TestClassC()
                }
                return field
            }
            private set
    }
}
