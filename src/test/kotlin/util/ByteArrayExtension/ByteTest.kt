package util.ByteArrayExtension

import kotlin.test.assertFails
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rasteplads.util.*

class ByteTest {
    private fun testEq(v: Byte): Unit = assertEquals(v, v.toByteArray().toByte())

    private fun testEq(v: ByteArray): Unit =
        assertEquals(v.toList(), v.toByte().toByteArray().toList())

    @Test
    fun fromByteReflexive() {
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

    @Test
    fun fromByteArrayReflexive() {
        testEq(byteArrayOf(-1))
        testEq(byteArrayOf(0))
        testEq(byteArrayOf(1))

        testEq(byteArrayOf(10))
        testEq(byteArrayOf(-10))

        testEq(byteArrayOf(99))
        testEq(byteArrayOf(-99))

        for (a in cartesianProduct(Byte.SIZE_BYTES)) {
            assertEquals(a.size, Byte.SIZE_BYTES)
            testEq(a.toByteArray())
        }
    }

    @Test
    fun generatedTestsReflexive() {
        val num = 100_000
        (0..num).forEach { _ ->
            generateRands(Byte.SIZE_BYTES).permutations().forEach { testEq(it.toByteArray()) }
        }
        (Byte.MIN_VALUE..Byte.MAX_VALUE).forEach { n -> testEq(n.toByte()) }
    }

    @Test
    fun throwsOnSmallArray() {
        val i = Byte.SIZE_BYTES
        (0..i).forEach { num ->
            if (num != Byte.SIZE_BYTES) assertFails { generateRands(num).toByteArray().toByte() }
        }
    }

    @Test
    fun redundancyInArraysIsIgnored() {
        val i = 1000
        (0..i).forEach { _ ->
            val arr = generateRands(Byte.SIZE_BYTES * 2).toByteArray()
            val arr2 = arr.copyOf(Byte.SIZE_BYTES)

            val res1 = arr.toByte()
            val res2 = arr2.toByte()

            assertEquals(res1, res2)
            assertEquals(res1.toByteArray().toList(), res2.toByteArray().toList())
        }
    }
}
