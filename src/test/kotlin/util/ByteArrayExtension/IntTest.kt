package util.ByteArrayExtension

import kotlin.test.assertFails
import org.example.util.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class IntTest {

    fun testEq(v: Int): Unit = assertEquals(v, v.toByteArray().toInt())

    fun testEq(v: ByteArray): Unit =
        assertEquals(v.map { it.toByte() }, v.toInt().toByteArray().map { it.toByte() })

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

    @Test
    fun fromByteArrayReflexive() {
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
    fun generatedTestsReflexive() {
        val num = 10_000
        (0..num).forEach { _ ->
            generateRands(Int.SIZE_BYTES).permutations().forEach { testEq(it.toByteArray()) }
        }
        (Int.MIN_VALUE..Int.MAX_VALUE).forEach { n -> testEq(n) }
    }

    @Test
    fun throwsOnSmallArray() {
        val i = Int.SIZE_BYTES
        (0..i).forEach { num ->
            if (num != Int.SIZE_BYTES) assertFails { generateRands(num).toByteArray().toInt() }
        }
    }
}
