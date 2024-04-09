package api.FatClassTest

import kotlin.reflect.jvm.isAccessible
import org.example.api.FatClass
import org.example.util.Either
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BuilderTest {
    private fun correct(): FatClass.Companion.Builder<Int, Byte> =
        FatClass.builder<Int, Byte>()
            .withDataConstant(0)
            .withIDGenerator { -> 10 }
            .withHandleMessage { _, _ -> }
            .withIntoIDFunction { _ -> 9 }
            .withIntoDataFunction { _ -> 0 }
            .withFromIDFunction { _ -> byteArrayOf(0, 1, 2, 3) }
            .withFromDataFunction { b -> byteArrayOf(b) }

    private inline fun <reified C, reified R> getValueFromClass(target: C, field: String): R =
        C::class.members.find { m -> m.name == field }!!.apply { isAccessible = true }.call(target)
            as R

    @Test
    fun missingAllFunc() {
        assertThrows(Exception::class.java) { FatClass.builder<Int, Byte>().build() }
    }

    @Test
    fun missingOneFunc() {
        assertThrows(Exception::class.java) {
            FatClass.builder<Int, Byte>()
                .withIDGenerator { -> 10 }
                .withHandleMessage { i, b -> }
                .withIntoIDFunction { _ -> 9 }
                .withIntoDataFunction { _ -> 0 }
                .withFromIDFunction { i -> byteArrayOf(0, 1, 2, 3) }
                .withFromDataFunction { b -> byteArrayOf(b) }
                .build()
        }

        assertThrows(Exception::class.java) {
            FatClass.builder<Int, Byte>()
                .withDataConstant(0)
                .withHandleMessage { i, b -> }
                .withIntoIDFunction { _ -> 9 }
                .withIntoDataFunction { _ -> 0 }
                .withFromIDFunction { i -> byteArrayOf(0, 1, 2, 3) }
                .withFromDataFunction { b -> byteArrayOf(b) }
                .build()
        }
        assertThrows(Exception::class.java) {
            FatClass.builder<Int, Byte>()
                .withDataConstant(0)
                .withIDGenerator { -> 10 }
                .withIntoIDFunction { _ -> 9 }
                .withIntoDataFunction { _ -> 0 }
                .withFromIDFunction { i -> byteArrayOf(0, 1, 2, 3) }
                .withFromDataFunction { b -> byteArrayOf(b) }
                .build()
        }
        assertThrows(Exception::class.java) {
            FatClass.builder<Int, Byte>()
                .withDataConstant(0)
                .withIDGenerator { -> 10 }
                .withIntoIDFunction { _ -> 9 }
                .withIntoDataFunction { _ -> 0 }
                .withFromIDFunction { i -> byteArrayOf(0, 1, 2, 3) }
                .withFromDataFunction { b -> byteArrayOf(b) }
                .build()
        }
        assertThrows(Exception::class.java) {
            FatClass.builder<Int, Byte>()
                .withDataConstant(0)
                .withIDGenerator { -> 10 }
                .withHandleMessage { i, b -> }
                .withIntoDataFunction { _ -> 0 }
                .withFromIDFunction { i -> byteArrayOf(0, 1, 2, 3) }
                .withFromDataFunction { b -> byteArrayOf(b) }
                .build()
        }
        assertThrows(Exception::class.java) {
            FatClass.builder<Int, Byte>()
                .withDataConstant(0)
                .withIDGenerator { -> 10 }
                .withHandleMessage { i, b -> }
                .withIntoIDFunction { _ -> 9 }
                .withFromIDFunction { i -> byteArrayOf(0, 1, 2, 3) }
                .withFromDataFunction { b -> byteArrayOf(b) }
                .build()
        }
        assertThrows(Exception::class.java) {
            FatClass.builder<Int, Byte>()
                .withDataConstant(0)
                .withIDGenerator { -> 10 }
                .withHandleMessage { i, b -> }
                .withIntoIDFunction { _ -> 9 }
                .withIntoDataFunction { _ -> 0 }
                .withFromDataFunction { b -> byteArrayOf(b) }
                .build()
        }
        assertThrows(Exception::class.java) {
            FatClass.builder<Int, Byte>()
                .withDataConstant(0)
                .withIDGenerator { -> 10 }
                .withHandleMessage { i, b -> }
                .withIntoIDFunction { _ -> 9 }
                .withIntoDataFunction { _ -> 0 }
                .withFromIDFunction { i -> byteArrayOf(0, 1, 2, 3) }
                .build()
        }
    }

    @Test
    fun correctNumberOfFilters() {
        val f = correct()
        f.addFilterFunction { _ -> true }
        var l =
            getValueFromClass<FatClass<Int, Byte>, List<(Int) -> Boolean>>(f.build(), "filterID")
        assertEquals(l.size, 1)
        f.addFilterFunction { i -> i <= 100 }
        l = getValueFromClass<FatClass<Int, Byte>, List<(Int) -> Boolean>>(f.build(), "filterID")
        assertEquals(l.size, 2)
        f.addFilterFunction({ i -> i >= 10 }, { i -> i and 1 == 1 })
        l = getValueFromClass<FatClass<Int, Byte>, List<(Int) -> Boolean>>(f.build(), "filterID")
        assert(l.all { fn -> fn(21) })
        assertFalse(l.all { fn -> fn(9) })
        assertFalse(l.all { fn -> fn(101) })
        assertFalse(l.all { fn -> fn(40) })
    }

    @Test
    fun switchingData() {
        val f = correct()
        f.withDataConstant(9)
        var l = getValueFromClass<FatClass<Int, Byte>, Either<Int, () -> Int>>(f.build(), "msgData")
        assert(l.isLeft())
        assertEquals(l.getLeft()!!, 9)

        f.withDataGenerator { 90 }
        l = getValueFromClass<FatClass<Int, Byte>, Either<Int, () -> Int>>(f.build(), "msgData")
        assert(l.isRight())
        assertEquals(l.getRight()!!(), 90)
    }
}
