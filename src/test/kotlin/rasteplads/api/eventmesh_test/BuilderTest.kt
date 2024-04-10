package rasteplads.api.eventmesh_test

import kotlin.reflect.jvm.isAccessible
import kotlin.test.assertFails
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import rasteplads.api.EventMesh
import rasteplads.messageCache.MessageCache
import rasteplads.util.Either

class BuilderTest {
    private fun correct() =
        EventMesh.builder<Int, Byte>()
            .setDataConstant(0)
            .setIDGenerator { 10 }
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
                .setIDGenerator { 10 }
                .setHandleMessage { _, _ -> }
                .setIntoIDFunction { _ -> 9 }
                .setIntoDataFunction { _ -> 0 }
                .setFromIDFunction { byteArrayOf(0, 1, 2, 3) }
                .setFromDataFunction { b -> byteArrayOf(b) }
                .build()
        }

        assertFails {
            EventMesh.builder<Int, Byte>()
                .setDataConstant(0)
                .setHandleMessage { _, _ -> }
                .setIntoIDFunction { _ -> 9 }
                .setIntoDataFunction { _ -> 0 }
                .setFromIDFunction { byteArrayOf(0, 1, 2, 3) }
                .setFromDataFunction { b -> byteArrayOf(b) }
                .build()
        }
        assertFails {
            EventMesh.builder<Int, Byte>()
                .setDataConstant(0)
                .setIDGenerator { 10 }
                .setIntoIDFunction { _ -> 9 }
                .setIntoDataFunction { _ -> 0 }
                .setFromIDFunction { byteArrayOf(0, 1, 2, 3) }
                .setFromDataFunction { b -> byteArrayOf(b) }
                .build()
        }
        assertFails {
            EventMesh.builder<Int, Byte>()
                .setDataConstant(0)
                .setIDGenerator { 10 }
                .setIntoIDFunction { _ -> 9 }
                .setIntoDataFunction { _ -> 0 }
                .setFromIDFunction { byteArrayOf(0, 1, 2, 3) }
                .setFromDataFunction { b -> byteArrayOf(b) }
                .build()
        }
        assertFails {
            EventMesh.builder<Int, Byte>()
                .setDataConstant(0)
                .setIDGenerator { 10 }
                .setHandleMessage { _, _ -> }
                .setIntoDataFunction { _ -> 0 }
                .setFromIDFunction { byteArrayOf(0, 1, 2, 3) }
                .setFromDataFunction { b -> byteArrayOf(b) }
                .build()
        }
        assertFails {
            EventMesh.builder<Int, Byte>()
                .setDataConstant(0)
                .setIDGenerator { 10 }
                .setHandleMessage { _, _ -> }
                .setIntoIDFunction { _ -> 9 }
                .setFromIDFunction { byteArrayOf(0, 1, 2, 3) }
                .setFromDataFunction { b -> byteArrayOf(b) }
                .build()
        }
        assertFails {
            EventMesh.builder<Int, Byte>()
                .setDataConstant(0)
                .setIDGenerator { 10 }
                .setHandleMessage { _, _ -> }
                .setIntoIDFunction { _ -> 9 }
                .setIntoDataFunction { _ -> 0 }
                .setFromDataFunction { b -> byteArrayOf(b) }
                .build()
        }
        assertFails {
            EventMesh.builder<Int, Byte>()
                .setDataConstant(0)
                .setIDGenerator { 10 }
                .setHandleMessage { _, _ -> }
                .setIntoIDFunction { _ -> 9 }
                .setIntoDataFunction { _ -> 0 }
                .setFromIDFunction { byteArrayOf(0, 1, 2, 3) }
                .build()
        }
    }

    @Test
    fun correctNumberOfFilters() {
        val f = correct()
        f.addFilterFunction { _ -> true }
        var l =
            getValueFromClass<EventMesh<Int, Byte, MessageCache<Int>>, List<(Int) -> Boolean>>(
                f.build(), "filterID")
        assertEquals(l.size, 1)
        f.addFilterFunction { i -> i <= 100 }
        l =
            getValueFromClass<EventMesh<Int, Byte, MessageCache<Int>>, List<(Int) -> Boolean>>(
                f.build(), "filterID")
        assertEquals(l.size, 2)
        f.addFilterFunction({ i -> i >= 10 }, { i -> i and 1 == 1 })
        l =
            getValueFromClass<EventMesh<Int, Byte, MessageCache<Int>>, List<(Int) -> Boolean>>(
                f.build(), "filterID")
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
            getValueFromClass<EventMesh<Int, Byte, MessageCache<Int>>, Either<Byte, () -> Byte>>(
                f.build(), "msgData")
        assert(l.isLeft())
        assertEquals(l.getLeft()!!, 9)

        f.setDataGenerator { 90 }
        l =
            getValueFromClass<EventMesh<Int, Byte, MessageCache<Int>>, Either<Byte, () -> Byte>>(
                f.build(), "msgData")
        assert(l.isRight())
        assertEquals(l.getRight()!!(), 90)

        f.setIDConstant(9)
        var k =
            getValueFromClass<EventMesh<Int, Byte, MessageCache<Int>>, Either<Int, () -> Int>>(
                f.build(), "msgId")
        assert(k.isLeft())
        assertEquals(k.getLeft()!!, 9)

        f.setIDGenerator { 90 }
        k =
            getValueFromClass<EventMesh<Int, Byte, MessageCache<Int>>, Either<Int, () -> Int>>(
                f.build(), "msgId")
        assert(k.isRight())
        assertEquals(k.getRight()!!(), 90)
    }

    @Test
    fun testingOptionalFields() {
        val f = correct()
        var name = "msgDelete"
        var default =
            getValueFromClass<EventMesh<Int, Byte, MessageCache<Int>>, UInt>(f.build(), name)
        var modded =
            getValueFromClass<EventMesh<Int, Byte, MessageCache<Int>>, UInt>(
                f.withMsgCacheDelete(default + 10u).build(), name)
        assertNotEquals(modded, default)

        name = "msgTTL"
        default = getValueFromClass<EventMesh<Int, Byte, MessageCache<Int>>, UInt>(f.build(), name)
        modded =
            getValueFromClass<EventMesh<Int, Byte, MessageCache<Int>>, UInt>(
                f.withMsgTTL(default + 10u).build(), name)
        assertNotEquals(modded, default)

        name = "msgSendInterval"
        default = getValueFromClass<EventMesh<Int, Byte, MessageCache<Int>>, UInt>(f.build(), name)
        modded =
            getValueFromClass<EventMesh<Int, Byte, MessageCache<Int>>, UInt>(
                f.withMsgSendInterval(default + 10u).build(), name)
        assertNotEquals(modded, default)

        name = "msgSendDuration"
        default = getValueFromClass<EventMesh<Int, Byte, MessageCache<Int>>, UInt>(f.build(), name)
        modded =
            getValueFromClass<EventMesh<Int, Byte, MessageCache<Int>>, UInt>(
                f.withMsgSendDuration(default + 10u).build(), name)
        assertNotEquals(modded, default)

        name = "msgScanInterval"
        default = getValueFromClass<EventMesh<Int, Byte, MessageCache<Int>>, UInt>(f.build(), name)
        modded =
            getValueFromClass<EventMesh<Int, Byte, MessageCache<Int>>, UInt>(
                f.withMsgScanInterval(default + 10u).build(), name)
        assertNotEquals(modded, default)

        name = "msgScanDuration"
        default = getValueFromClass<EventMesh<Int, Byte, MessageCache<Int>>, UInt>(f.build(), name)
        modded =
            getValueFromClass<EventMesh<Int, Byte, MessageCache<Int>>, UInt>(
                f.withMsgScanDuration(default + 10u).build(), name)
        assertNotEquals(modded, default)

        name = "msgCacheLimit"
        default = getValueFromClass<EventMesh<Int, Byte, MessageCache<Int>>, UInt>(f.build(), name)
        modded =
            getValueFromClass<EventMesh<Int, Byte, MessageCache<Int>>, UInt>(
                f.withMsgCacheLimit(default + 10u).build(), name)
        assertNotEquals(modded, default)
    }

    @Test
    fun noCache() {
        val name = "messageCache"

        val f =
            EventMesh.builder<Int, Byte>()
                .setDataConstant(0)
                .setIDGenerator { 10 }
                .setHandleMessage { _, _ -> }
                .setIntoIDFunction { _ -> 9 }
                .setIntoDataFunction { _ -> 0 }
                .setFromIDFunction { _ -> byteArrayOf(0, 1, 2, 3) }
                .setFromDataFunction { b -> byteArrayOf(b) }
        // .build()
        assertNotNull(
            getValueFromClass<EventMesh<Int, Byte, MessageCache<Int>>, MessageCache<Int>?>(
                f.build(), name))
        assertNull(
            getValueFromClass<EventMesh<Int, Byte, MessageCache<Int>>, MessageCache<Int>?>(
                f.withMsgCache(null).build(), name))

        val g =
            EventMesh.builderWithoutMC<Int, Byte>()
                .setDataConstant(0)
                .setIDGenerator { 10 }
                .setHandleMessage { _, _ -> }
                .setIntoIDFunction { _ -> 9 }
                .setIntoDataFunction { _ -> 0 }
                .setFromIDFunction { _ -> byteArrayOf(0, 1, 2, 3) }
                .setFromDataFunction { b -> byteArrayOf(b) }
        // .build()
        assertNull(
            getValueFromClass<EventMesh<Int, Byte, *>, MessageCache<*>?>(
                g.withMsgCache(null).build(), name))
    }
}
