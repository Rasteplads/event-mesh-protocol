package util.ByteArrayExtension

import org.example.util.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ShortTest {
    fun testEq(v: Short) = assertEquals(v, v.toByteArray().toShort())
    // fun testEq(v: UShort) = assertEquals(v, v.toUByteArray().toUShort())

    @Test
    fun signedReflexive() {
        testEq(0)
        testEq(1)
        testEq(-1)

        testEq(1000)
        testEq(-1000)

        testEq(9999)
        testEq(-9999)

        testEq(Short.MAX_VALUE)
        testEq(Short.MIN_VALUE)
    }
    /*
    @Test
    fun usignedReflexive() {

        testEq(1u)
        testEq(1000u)
        testEq(9999u)
        testEq(UShort.MAX_VALUE)
        testEq(UShort.MIN_VALUE)

    }
     */
}
