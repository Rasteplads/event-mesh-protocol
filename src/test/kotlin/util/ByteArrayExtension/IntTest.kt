package util.ByteArrayExtension

import org.example.util.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class IntTest {

    fun testEq(v: Int) = assertEquals(v, v.toByteArray().toInt())
    // fun testEq(v: UInt) = assertEquals(v, v.toUByteArray().toUInt())

    @Test
    fun signedReflexive() {
        testEq(-1)
        testEq(0)
        testEq(1)

        testEq(1000)
        testEq(-1000)

        testEq(99999)
        testEq(-99999)

        testEq(Int.MAX_VALUE)
        testEq(Int.MIN_VALUE)
    }
    /*
       @Test
       fun usignedReflexive() {

           testEq(1u)
           testEq(1000u)
           testEq(99999u)
           testEq(UInt.MAX_VALUE)
           testEq(UInt.MIN_VALUE)
       }
    */
}
