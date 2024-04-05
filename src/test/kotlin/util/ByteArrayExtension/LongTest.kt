package util.ByteArrayExtension;

import org.example.util.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LongTest {
    fun testEq(v: Long) = assertEquals(v, v.toByteArray().toLong())
    //fun testEq(v: ULong) = assertEquals(v, v.toUByteArray().toULong())


    @Test
    fun signedReflexive() {
        testEq(-1)
        testEq(0)
        testEq(1)

        testEq(1000)
        testEq(-1000)

        testEq(99999)
        testEq(-99999)

        testEq(Long.MAX_VALUE)
        testEq(Long.MIN_VALUE)
    }
/*
    @Test
    fun usignedReflexive() {

        testEq(1u)
        testEq(1000u)
        testEq(99999u)
        testEq(ULong.MAX_VALUE)
        testEq(ULong.MIN_VALUE)
    }
 */
}
