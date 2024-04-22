package rasteplads.util.byte_array_extension

import kotlin.test.assertFails
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import rasteplads.util.*

class ByteTest {
    private fun testEq(v: Byte): Unit = assertEquals(v, v.toByteArray().toByte())

    private fun testEq(v: UByte): Unit = assertEquals(v, v.toByteArray().toUByte())

    private fun testEq(v: ByteArray): Unit =
        assertEquals(v.toList(), v.toByte().toByteArray().toList())

    @Test
    fun `from byte reflexive`() {
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
    fun `from unsigned byte reflexive`() {
        testEq(0u)
        testEq(1u)

        testEq(10u)

        testEq(99u)

        testEq(UByte.MAX_VALUE)
        testEq(UByte.MIN_VALUE)
    }

    @Test
    fun `from byte array reflexive`() {
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
    fun `generated tests reflexive`() {
        val num = 100_000
        (0..num).forEach { _ ->
            generateRands(Byte.SIZE_BYTES).permutations().forEach { testEq(it.toByteArray()) }
        }
        (Byte.MIN_VALUE..Byte.MAX_VALUE).forEach { n -> testEq(n.toByte()) }
        (UByte.MIN_VALUE..UByte.MAX_VALUE).forEach { n -> testEq(n.toUByte()) }
    }

    @Test
    fun `throws on small array`() {
        val i = Byte.SIZE_BYTES
        (0..i).forEach { num ->
            if (num != Byte.SIZE_BYTES) {
                assertFails { generateRands(num).toByteArray().toByte() }
                assertFails { generateRands(num).toByteArray().toUByte() }
            }
        }
    }

    @Test
    fun `redundancy in arrays is ignored`() {
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
