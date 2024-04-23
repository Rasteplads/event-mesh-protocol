package rasteplads.util.byte_array_extension

import kotlin.test.assertFails
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import rasteplads.util.*

class LongTest {
    private fun testEq(v: Long): Unit = assertEquals(v, v.toByteArray().toLong())

    private fun testEq(v: ULong): Unit = assertEquals(v, v.toByteArray().toULong())

    private fun testEq(v: ByteArray): Unit =
        assertEquals(v.toList(), v.toLong().toByteArray().toList())

    @Test
    fun `from long reflexive`() {
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

    @Test
    fun `from unsigned long reflexive`() {
        testEq(0u)
        testEq(1u)

        testEq(1000u)

        testEq(99999u)

        testEq(ULong.MAX_VALUE)
        testEq(ULong.MIN_VALUE)
    }

    @Test
    fun `from byte array reflexive`() {
        for (a in cartesianProduct(Long.SIZE_BYTES)) {
            assertEquals(a.size, Long.SIZE_BYTES)
            testEq(a.toByteArray())
        }
    }

    @Test
    fun `generated tests reflexive`() {
        val num = 100
        (0..num).forEach { _ ->
            generateRands(Long.SIZE_BYTES).permutations().forEach { testEq(it.toByteArray()) }
        }
        // (`redundancy in arrays is ignored`Long.MIN_VALUE..Long.MAX_VALUE).step(1000000).forEach {
        // n -> testEq(n) }
        // (ULong.MIN_VALUE..ULong.MAX_VALUE).step(1000000).forEach { n -> testEq(n) }
    }

    @Test
    fun `throws on small array`() {
        val i = Long.SIZE_BYTES
        (0..i).forEach { num ->
            if (num != Long.SIZE_BYTES) {
                assertFails { generateRands(num).toByteArray().toLong() }
                assertFails { generateRands(num).toByteArray().toULong() }
            }
        }
    }

    @Test
    fun `redundancy in arrays is ignored`() {
        val i = 1000
        (0..i).forEach { _ ->
            val arr = generateRands(Long.SIZE_BYTES * 2).toByteArray()
            val arr2 = arr.copyOf(Long.SIZE_BYTES)

            val res1 = arr.toLong()
            val res2 = arr2.toLong()

            assertEquals(res1, res2)
            assertEquals(res1.toByteArray().toList(), res2.toByteArray().toList())
        }
    }
}
