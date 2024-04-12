package rasteplads.api.eventmesh_test

import java.time.Duration
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
            .setMessageCallback { _, _ -> }
            .setIDDecodeFunction { _ -> 9 }
            .setDataDecodeFunction { _ -> 0 }
            .setIDEncodeFunction { _ -> byteArrayOf(0, 1, 2, 3) }
            .setDataEncodeFunction { b -> byteArrayOf(b) }

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
                .setMessageCallback { _, _ -> }
                .setIDDecodeFunction { _ -> 9 }
                .setDataDecodeFunction { _ -> 0 }
                .setIDEncodeFunction { byteArrayOf(0, 1, 2, 3) }
                .setDataEncodeFunction { b -> byteArrayOf(b) }
                .build()
        }

        assertFails {
            EventMesh.builder<Int, Byte>()
                .setDataConstant(0)
                .setMessageCallback { _, _ -> }
                .setIDDecodeFunction { _ -> 9 }
                .setDataDecodeFunction { _ -> 0 }
                .setIDEncodeFunction { byteArrayOf(0, 1, 2, 3) }
                .setDataEncodeFunction { b -> byteArrayOf(b) }
                .build()
        }
        assertFails {
            EventMesh.builder<Int, Byte>()
                .setDataConstant(0)
                .setIDGenerator { 10 }
                .setIDDecodeFunction { _ -> 9 }
                .setDataDecodeFunction { _ -> 0 }
                .setIDEncodeFunction { byteArrayOf(0, 1, 2, 3) }
                .setDataEncodeFunction { b -> byteArrayOf(b) }
                .build()
        }
        assertFails {
            EventMesh.builder<Int, Byte>()
                .setDataConstant(0)
                .setIDGenerator { 10 }
                .setIDDecodeFunction { _ -> 9 }
                .setDataDecodeFunction { _ -> 0 }
                .setIDEncodeFunction { byteArrayOf(0, 1, 2, 3) }
                .setDataEncodeFunction { b -> byteArrayOf(b) }
                .build()
        }
        assertFails {
            EventMesh.builder<Int, Byte>()
                .setDataConstant(0)
                .setIDGenerator { 10 }
                .setMessageCallback { _, _ -> }
                .setDataDecodeFunction { _ -> 0 }
                .setIDEncodeFunction { byteArrayOf(0, 1, 2, 3) }
                .setDataEncodeFunction { b -> byteArrayOf(b) }
                .build()
        }
        assertFails {
            EventMesh.builder<Int, Byte>()
                .setDataConstant(0)
                .setIDGenerator { 10 }
                .setMessageCallback { _, _ -> }
                .setIDDecodeFunction { _ -> 9 }
                .setIDEncodeFunction { byteArrayOf(0, 1, 2, 3) }
                .setDataEncodeFunction { b -> byteArrayOf(b) }
                .build()
        }
        assertFails {
            EventMesh.builder<Int, Byte>()
                .setDataConstant(0)
                .setIDGenerator { 10 }
                .setMessageCallback { _, _ -> }
                .setIDDecodeFunction { _ -> 9 }
                .setDataDecodeFunction { _ -> 0 }
                .setDataEncodeFunction { b -> byteArrayOf(b) }
                .build()
        }
        assertFails {
            EventMesh.builder<Int, Byte>()
                .setDataConstant(0)
                .setIDGenerator { 10 }
                .setMessageCallback { _, _ -> }
                .setIDDecodeFunction { _ -> 9 }
                .setDataDecodeFunction { _ -> 0 }
                .setIDEncodeFunction { byteArrayOf(0, 1, 2, 3) }
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
            getValueFromClass<EventMesh<Int, Byte>, Either<Byte, () -> Byte>>(f.build(), "msgData")
        assert(l.isLeft())
        assertEquals(l.getLeft()!!, 9)

        f.setDataGenerator { 90 }
        l = getValueFromClass<EventMesh<Int, Byte>, Either<Byte, () -> Byte>>(f.build(), "msgData")
        assert(l.isRight())
        assertEquals(l.getRight()!!(), 90)

        f.setIDConstant(9)
        var k = getValueFromClass<EventMesh<Int, Byte>, Either<Int, () -> Int>>(f.build(), "msgId")
        assert(k.isLeft())
        assertEquals(k.getLeft()!!, 9)

        f.setIDGenerator { 90 }
        k = getValueFromClass<EventMesh<Int, Byte>, Either<Int, () -> Int>>(f.build(), "msgId")
        assert(k.isRight())
        assertEquals(k.getRight()!!(), 90)
    }

    @Test
    fun testingOptionalFields() {
        val f = correct()
        var name = "msgDelete"
        var default = getValueFromClass<EventMesh<Int, Byte>, Duration>(f.build(), name)
        var modded =
            getValueFromClass<EventMesh<Int, Byte>, Duration>(
                f.withMsgCacheDelete(default.plusMinutes(10)).build(), name)
        assertNotEquals(modded, default)

        name = "msgSendSessionInterval"
        default = getValueFromClass<EventMesh<Int, Byte>, Duration>(f.build(), name)
        modded =
            getValueFromClass<EventMesh<Int, Byte>, Duration>(
                f.withMsgSendSessionInterval(default.plusMinutes(10)).build(), name)
        assertNotEquals(modded, default)

        name = "msgSendInterval"
        default = getValueFromClass<EventMesh<Int, Byte>, Duration>(f.build(), name)
        modded =
            getValueFromClass<EventMesh<Int, Byte>, Duration>(
                f.withMsgSendInterval(default.plusMillis(10)).build(), name)
        assertNotEquals(modded, default)

        name = "msgSendTimeout"
        default = getValueFromClass<EventMesh<Int, Byte>, Duration>(f.build(), name)
        modded =
            getValueFromClass<EventMesh<Int, Byte>, Duration>(
                f.withMsgSendTimeout(default.plusMinutes(10)).build(), name)
        assertNotEquals(modded, default)

        name = "msgScanInterval"
        default = getValueFromClass<EventMesh<Int, Byte>, Duration>(f.build(), name)
        modded =
            getValueFromClass<EventMesh<Int, Byte>, Duration>(
                f.withMsgScanInterval(default.plusMinutes(10)).build(), name)
        assertNotEquals(modded, default)

        name = "msgScanDuration"
        default = getValueFromClass<EventMesh<Int, Byte>, Duration>(f.build(), name)
        modded =
            getValueFromClass<EventMesh<Int, Byte>, Duration>(
                f.withMsgScanDuration(default.plusMinutes(10)).build(), name)
        assertNotEquals(modded, default)

        name = "msgCacheLimit"
        var def = getValueFromClass<EventMesh<Int, Byte>, Long>(f.build(), name)
        var mod =
            getValueFromClass<EventMesh<Int, Byte>, Long>(
                f.withMsgCacheLimit(def + 10).build(), name)
        assertNotEquals(mod, def)

        name = "msgTTL"
        def = getValueFromClass<EventMesh<Int, Byte>, Long>(f.build(), name)
        mod = getValueFromClass<EventMesh<Int, Byte>, Long>(f.withMsgTTL(def + 10).build(), name)
        assertNotEquals(mod, def)
    }

    @Test
    fun noCache() {
        val name = "messageCache"

        val f =
            EventMesh.builder<Int, Byte>()
                .setDataConstant(0)
                .setIDGenerator { 10 }
                .setMessageCallback { _, _ -> }
                .setIDDecodeFunction { _ -> 9 }
                .setDataDecodeFunction { _ -> 0 }
                .setIDEncodeFunction { _ -> byteArrayOf(0, 1, 2, 3) }
                .setDataEncodeFunction { b -> byteArrayOf(b) }
        // .build()
        assertNotNull(getValueFromClass<EventMesh<Int, Byte>, MessageCache<Int>?>(f.build(), name))
        assertNull(
            getValueFromClass<EventMesh<Int, Byte>, MessageCache<Int>?>(
                f.withMsgCache(null).build(), name))

        val g =
            EventMesh.builder<Int, Byte>()
                .setDataConstant(0)
                .setIDGenerator { 10 }
                .setMessageCallback { _, _ -> }
                .setIDDecodeFunction { _ -> 9 }
                .setDataDecodeFunction { _ -> 0 }
                .setIDEncodeFunction { _ -> byteArrayOf(0, 1, 2, 3) }
                .setDataEncodeFunction { b -> byteArrayOf(b) }
        // .build()
        assertNull(
            getValueFromClass<EventMesh<Int, Byte>, MessageCache<*>?>(
                g.withMsgCache(null).build(), name))
    }

    @Test
    fun differentBuilders() {
        val b = EventMesh.builder<Int, Byte>()
        assertFails { b.build() }
        b.setDataConstant(0)
            .setIDGenerator { 10 }
            .setMessageCallback { _, _ -> }
            .setIDDecodeFunction { _ -> 9 }
            .setDataDecodeFunction { _ -> 0 }
            .setIDEncodeFunction { _ -> byteArrayOf(0, 1, 2, 3) }
            .setDataEncodeFunction { b -> byteArrayOf(b) }
            .build()

        var m = EventMesh.builder<Int, Byte>(null)
        assertFails { m.build() }
        m.setDataConstant(0)
            .setIDGenerator { 10 }
            .setMessageCallback { _, _ -> }
            .setIDDecodeFunction { _ -> 9 }
            .setDataDecodeFunction { _ -> 0 }
            .setIDEncodeFunction { _ -> byteArrayOf(0, 1, 2, 3) }
            .setDataEncodeFunction { b -> byteArrayOf(b) }
            .build()
        m = EventMesh.builder(MessageCache())
        assertFails { m.build() }
        m.setDataConstant(0)
            .setIDGenerator { 10 }
            .setMessageCallback { _, _ -> }
            .setIDDecodeFunction { _ -> 9 }
            .setDataDecodeFunction { _ -> 0 }
            .setIDEncodeFunction { _ -> byteArrayOf(0, 1, 2, 3) }
            .setDataEncodeFunction { b -> byteArrayOf(b) }
            .build()

        // TODO: Check with Device when type is ready
    }
}
