package util.ByteArrayExtension

import kotlin.test.assertFails
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rasteplads.util.*

class LongTest {
    fun testEq(v: Long) = assertEquals(v, v.toByteArray().toLong())

    fun testEq(v: ByteArray): Unit =
        assertEquals(v.map { it.toByte() }, v.toLong().toByteArray().map { it.toByte() })

    @Test
    fun fromLongReflexive() {
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
    fun fromByteArrayReflexive() {
        for (a in cartesianProduct(Long.SIZE_BYTES)) {
            assertEquals(a.size, Long.SIZE_BYTES)
            testEq(a.toByteArray())
        }
    }

    @Test
    fun generatedTestsReflexive() {
        val num = 100
        (0..num).forEach { _ ->
            generateRands(Long.SIZE_BYTES).permutations().forEach { testEq(it.toByteArray()) }
        }
        // (Long.MIN_VALUE..Long.MAX_VALUE).step(1000000).forEach { n -> testEq(n) }
    }

    @Test
    fun throwsOnSmallArray() {
        val i = Long.SIZE_BYTES
        (0..i).forEach { num ->
            if (num != Long.SIZE_BYTES) assertFails { generateRands(num).toByteArray().toLong() }
        }
    }
}
