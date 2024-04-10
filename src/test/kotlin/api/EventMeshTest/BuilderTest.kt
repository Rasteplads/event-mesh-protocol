package api.EventMeshTest

import api.EventMesh
import kotlin.reflect.jvm.isAccessible
import kotlin.test.assertFails
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rasteplads.util.Either

class BuilderTest {
    private fun correct(): EventMesh.Companion.Builder<Int, Byte> =
        EventMesh.builder<Int, Byte>()
            .setDataConstant(0)
            .setIDGenerator { -> 10 }
            .setHandleMessage { _, _ -> }
            .setIntoIDFunction { _ -> 9 }
            .setIntoDataFunction { _ -> 0 }
            .setFromIDFunction { _ -> byteArrayOf(0, 1, 2, 3) }
            .setFromDataFunction { b -> byteArrayOf(b) }

    private inline fun <reified C, reified R> getValueFromClass(target: C, field: String): R =
        C::class.members.find { m -> m.name == field }!!.apply { isAccessible = true }.call(target)
            as R

    @Test
    fun missingAllFunc() {
        assertFails { EventMesh.builder<Int, Byte>().build() }
    }

    @Test
    fun missingOneFunc() {
        assertFails {
            EventMesh.builder<Int, Byte>()
                .setIDGenerator { -> 10 }
                .setHandleMessage { i, b -> }
                .setIntoIDFunction { _ -> 9 }
                .setIntoDataFunction { _ -> 0 }
                .setFromIDFunction { i -> byteArrayOf(0, 1, 2, 3) }
                .setFromDataFunction { b -> byteArrayOf(b) }
                .build()
        }

        assertFails {
            EventMesh.builder<Int, Byte>()
                .setDataConstant(0)
                .setHandleMessage { i, b -> }
                .setIntoIDFunction { _ -> 9 }
                .setIntoDataFunction { _ -> 0 }
                .setFromIDFunction { i -> byteArrayOf(0, 1, 2, 3) }
                .setFromDataFunction { b -> byteArrayOf(b) }
                .build()
        }
        assertFails {
            EventMesh.builder<Int, Byte>()
                .setDataConstant(0)
                .setIDGenerator { -> 10 }
                .setIntoIDFunction { _ -> 9 }
                .setIntoDataFunction { _ -> 0 }
                .setFromIDFunction { i -> byteArrayOf(0, 1, 2, 3) }
                .setFromDataFunction { b -> byteArrayOf(b) }
                .build()
        }
        assertFails {
            EventMesh.builder<Int, Byte>()
                .setDataConstant(0)
                .setIDGenerator { -> 10 }
                .setIntoIDFunction { _ -> 9 }
                .setIntoDataFunction { _ -> 0 }
                .setFromIDFunction { i -> byteArrayOf(0, 1, 2, 3) }
                .setFromDataFunction { b -> byteArrayOf(b) }
                .build()
        }
        assertFails {
            EventMesh.builder<Int, Byte>()
                .setDataConstant(0)
                .setIDGenerator { -> 10 }
                .setHandleMessage { i, b -> }
                .setIntoDataFunction { _ -> 0 }
                .setFromIDFunction { i -> byteArrayOf(0, 1, 2, 3) }
                .setFromDataFunction { b -> byteArrayOf(b) }
                .build()
        }
        assertFails {
            EventMesh.builder<Int, Byte>()
                .setDataConstant(0)
                .setIDGenerator { -> 10 }
                .setHandleMessage { i, b -> }
                .setIntoIDFunction { _ -> 9 }
                .setFromIDFunction { i -> byteArrayOf(0, 1, 2, 3) }
                .setFromDataFunction { b -> byteArrayOf(b) }
                .build()
        }
        assertFails {
            EventMesh.builder<Int, Byte>()
                .setDataConstant(0)
                .setIDGenerator { -> 10 }
                .setHandleMessage { i, b -> }
                .setIntoIDFunction { _ -> 9 }
                .setIntoDataFunction { _ -> 0 }
                .setFromDataFunction { b -> byteArrayOf(b) }
                .build()
        }
        assertFails {
            EventMesh.builder<Int, Byte>()
                .setDataConstant(0)
                .setIDGenerator { -> 10 }
                .setHandleMessage { i, b -> }
                .setIntoIDFunction { _ -> 9 }
                .setIntoDataFunction { _ -> 0 }
                .setFromIDFunction { i -> byteArrayOf(0, 1, 2, 3) }
                .build()
        }
    }

    @Test
    fun correctNumberOfFilters() {
        val f = correct()
        f.addFilterFunction { _ -> true }
        var l =
            getValueFromClass<EventMesh<Int, Byte>, List<(Int) -> Boolean>>(f.build(), "filterID")
        assertEquals(l.size, 1)
        f.addFilterFunction { i -> i <= 100 }
        l = getValueFromClass<EventMesh<Int, Byte>, List<(Int) -> Boolean>>(f.build(), "filterID")
        assertEquals(l.size, 2)
        f.addFilterFunction({ i -> i >= 10 }, { i -> i and 1 == 1 })
        l = getValueFromClass<EventMesh<Int, Byte>, List<(Int) -> Boolean>>(f.build(), "filterID")
        assert(l.all { fn -> fn(21) })
        assertFalse(l.all { fn -> fn(9) })
        assertFalse(l.all { fn -> fn(101) })
        assertFalse(l.all { fn -> fn(40) })
    }

    @Test
    fun overridingData() {
        val f = correct()
        f.setDataConstant(9)
        var l =
            getValueFromClass<EventMesh<Int, Byte>, Either<Int, () -> Int>>(f.build(), "msgData")
        assert(l.isLeft())
        assertEquals(l.getLeft()!!, 9)

        f.setDataGenerator { 90 }
        l = getValueFromClass<EventMesh<Int, Byte>, Either<Int, () -> Int>>(f.build(), "msgData")
        assert(l.isRight())
        assertEquals(l.getRight()!!(), 90)
    }

    @Test
    fun testingOptionalFields() {
        val f = correct()
        var name = "msgDelete"
        var default = getValueFromClass<EventMesh<Int, Byte>, UInt>(f.build(), name)
        var modded =
            getValueFromClass<EventMesh<Int, Byte>, UInt>(
                f.withMsgCacheDelete(default + 10u).build(), name)
        assertNotEquals(modded, default)

        name = "msgTTL"
        default = getValueFromClass<EventMesh<Int, Byte>, UInt>(f.build(), name)
        modded =
            getValueFromClass<EventMesh<Int, Byte>, UInt>(f.withMsgTTL(default + 10u).build(), name)
        assertNotEquals(modded, default)

        name = "msgSendInterval"
        default = getValueFromClass<EventMesh<Int, Byte>, UInt>(f.build(), name)
        modded =
            getValueFromClass<EventMesh<Int, Byte>, UInt>(
                f.withMsgSendInterval(default + 10u).build(), name)
        assertNotEquals(modded, default)

        name = "msgSendDuration"
        default = getValueFromClass<EventMesh<Int, Byte>, UInt>(f.build(), name)
        modded =
            getValueFromClass<EventMesh<Int, Byte>, UInt>(
                f.withMsgSendDuration(default + 10u).build(), name)
        assertNotEquals(modded, default)

        name = "msgScanInterval"
        default = getValueFromClass<EventMesh<Int, Byte>, UInt>(f.build(), name)
        modded =
            getValueFromClass<EventMesh<Int, Byte>, UInt>(
                f.withMsgScanInterval(default + 10u).build(), name)
        assertNotEquals(modded, default)

        name = "msgScanDuration"
        default = getValueFromClass<EventMesh<Int, Byte>, UInt>(f.build(), name)
        modded =
            getValueFromClass<EventMesh<Int, Byte>, UInt>(
                f.withMsgScanDuration(default + 10u).build(), name)
        assertNotEquals(modded, default)

        name = "msgCacheLimit"
        default = getValueFromClass<EventMesh<Int, Byte>, UInt>(f.build(), name)
        modded =
            getValueFromClass<EventMesh<Int, Byte>, UInt>(
                f.withMsgCacheLimit(default + 10u).build(), name)
        assertNotEquals(modded, default)
    }
}
