package rasteplads.util.byte_array_extension

import kotlin.test.assertFails
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import rasteplads.util.*

class ShortTest {
    private fun testEq(v: Short): Unit = assertEquals(v, v.toByteArray().toShort())

    private fun testEq(v: UShort): Unit = assertEquals(v, v.toByteArray().toUShort())

    private fun testEq(v: ByteArray): Unit =
        assertEquals(v.toList(), v.toShort().toByteArray().toList())

    @Test
    fun `from short reflexive`() {
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

    @Test
    fun `from unsigned short reflexive`() {
        testEq(0u)
        testEq(1u)

        testEq(1000u)

        testEq(9999u)

        testEq(UShort.MAX_VALUE)
        testEq(UShort.MIN_VALUE)
    }

    @Test
    fun `from byte array reflexive`() {
        testEq(byteArrayOf(-1, 2))
        testEq(byteArrayOf(0, 1))
        testEq(byteArrayOf(1, 1))

        testEq(byteArrayOf(10, 0))
        testEq(byteArrayOf(-10, 0))

        testEq(byteArrayOf(99, 3))
        testEq(byteArrayOf(-99, 5))

        for (a in cartesianProduct(Short.SIZE_BYTES)) {
            assertEquals(a.size, Short.SIZE_BYTES)
            testEq(a.toByteArray())
        }
    }

    @Test
    fun `generated tests reflexive`() {
        val num = 10_000
        (0..num).forEach { _ ->
            generateRands(Short.SIZE_BYTES).permutations().forEach { testEq(it.toByteArray()) }
        }
        (Short.MIN_VALUE..Short.MAX_VALUE).forEach { n -> testEq(n.toShort()) }
        (UShort.MIN_VALUE..UShort.MAX_VALUE).forEach { n -> testEq(n.toShort()) }
    }

    @Test
    fun `throws on small array`() {
        val i = Short.SIZE_BYTES
        (0..i).forEach { num ->
            if (num != Short.SIZE_BYTES) {
                assertFails { generateRands(num).toByteArray().toShort() }
                assertFails { generateRands(num).toByteArray().toUShort() }
            }
        }
    }

    @Test
    fun `redundancy in arrays is ignored`() {
        val i = 1000
        (0..i).forEach { _ ->
            val arr = generateRands(Short.SIZE_BYTES * 2).toByteArray()
            val arr2 = arr.copyOf(Short.SIZE_BYTES)

            val res1 = arr.toShort()
            val res2 = arr2.toShort()

            assertEquals(res1, res2)
            assertEquals(res1.toByteArray().toList(), res2.toByteArray().toList())
        }
    }
}
