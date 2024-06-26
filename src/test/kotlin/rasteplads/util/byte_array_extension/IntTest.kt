package rasteplads.util.byte_array_extension

import kotlin.test.assertFails
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import rasteplads.util.*

class IntTest {

    private fun testEq(v: Int): Unit = assertEquals(v, v.toByteArray().toInt())

    private fun testEq(v: UInt): Unit = assertEquals(v, v.toByteArray().toUInt())

    private fun testEq(v: ByteArray): Unit =
        assertEquals(v.toList(), v.toInt().toByteArray().toList())

    @Test
    fun `from int reflexive`() {
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

    @Test
    fun `from unsigned int reflexive`() {
        testEq(0u)
        testEq(1u)

        testEq(1000u)

        testEq(99999u)

        testEq(UInt.MAX_VALUE)
        testEq(UInt.MIN_VALUE)
    }

    @Test
    fun `from byte array reflexive`() {
        testEq(byteArrayOf(-1, -2, -3, -4))
        testEq(byteArrayOf(0, 0, 0, 0))
        testEq(byteArrayOf(1, 2, 3, 4))

        testEq(byteArrayOf(10, 0, 0, 10))
        testEq(byteArrayOf(-10, 0, 0, -10))

        testEq(byteArrayOf(99, -100, 99, 100))
        testEq(byteArrayOf(-99, 5, 19, 120))

        for (a in cartesianProduct(Int.SIZE_BYTES)) {
            assertEquals(a.size, Int.SIZE_BYTES)
            testEq(a.toByteArray())
        }
    }

    @Test
    fun `generated tests reflexive`() {
        val num = 10_000
        (0..num).forEach { _ ->
            generateRands(Int.SIZE_BYTES).permutations().forEach { testEq(it.toByteArray()) }
        }
        (Int.MIN_VALUE..Int.MAX_VALUE).forEach { n -> testEq(n) }
        (UInt.MIN_VALUE..UInt.MAX_VALUE).forEach { n -> testEq(n) }
    }

    @Test
    fun `throws on small array`() {
        val i = Int.SIZE_BYTES
        (0..i).forEach { num ->
            if (num != Int.SIZE_BYTES) {
                assertFails { generateRands(num).toByteArray().toInt() }
                assertFails { generateRands(num).toByteArray().toUInt() }
            }
        }
    }

    @Test
    fun `redundancy in arrays is ignored`() {
        val i = 1000
        (0..i).forEach { _ ->
            val arr = generateRands(Int.SIZE_BYTES * 2).toByteArray()
            val arr2 = arr.copyOf(Int.SIZE_BYTES)

            val res1 = arr.toInt()
            val res2 = arr2.toInt()

            assertEquals(res1, res2)
            assertEquals(res1.toByteArray().toList(), res2.toByteArray().toList())
        }
    }
}
