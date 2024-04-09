package util.ByteArrayExtension

import org.example.util.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ByteTest {
    fun testEq(v: Byte) = assertEquals(v, v.toByteArray().toByte())
    // fun testEq(v: UByte) = assertEquals(v, v.toByteArray().toUByte())

    @Test
    fun signedReflexive() {
        testEq(-1)
        testEq(0)
        testEq(1)

        testEq(10)
        testEq(-10)

        testEq(99)
        testEq(-99)

        testEq(Byte.MAX_VALUE)
        testEq(Byte.MIN_VALUE)
    }
    /*
       @Test
       fun usignedReflexive() {

           testEq(1u)
           testEq(100u)
           testEq(99u)
           testEq(UByte.MAX_VALUE)
           testEq(UByte.MIN_VALUE)
       }
    */
}
