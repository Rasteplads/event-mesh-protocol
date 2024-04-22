package rasteplads.api

import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.jvm.isAccessible
import kotlin.test.*
import kotlin.test.Test
import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import rasteplads.api.EventMesh.Companion.MESSAGE_CACHE_TIME
import rasteplads.messageCache.MessageCache
import rasteplads.util.*

class EventMeshTest {

    @BeforeTest
    @AfterTest
    fun clean() {
        testDevice.stopReceiving()
        testDevice.stopTransmitting()
        testDevice.transmittedMessages.get().removeAll { true }
        testDevice.receivedPool.get().removeAll { true }
    }

    companion object {
        val testDevice = MockDevice(100)

        fun correct() =
            EventMesh.builder<Int, Byte>(testDevice)
                .setDataConstant(0)
                .setIDConstant(10)
                .setMessageCallback { _, _ -> }
                .setIDDecodeFunction { b -> b.toInt() }
                .setDataDecodeFunction { b -> b.toByte() }
                .setIDEncodeFunction { i -> i.toByteArray() }
                .setDataEncodeFunction { b -> byteArrayOf(b) }

        inline fun <reified C, reified R> getValueFromClass(target: C, field: String): R =
            C::class
                .members
                .find { m -> m.name == field }!!
                .apply { isAccessible = true }
                .call(target) as R
    }

    @Nested
    inner class Receiving {
        @Test
        fun `messages get passed through`(): Unit = runBlocking { // TODO: refactor
            val l = mutableListOf<Int>()
            val f =
                correct()
                    .withMsgScanInterval(Duration.ofMillis(100))
                    .setMessageCallback { i, byte -> l.add(i) }
                    .build()
            var b = byteArrayOf(0, 2, 3, 4, 5, 6, 7)
            val id = b.slice(1..4).toByteArray().toInt()

            f.start()
            delay(100)
            testDevice.receiveMessage(b)
            delay(100)
            assertEquals(1, l.size)
            assert(l.all { it == id })

            testDevice.receiveMessage(b)
            delay(100)
            assertEquals(1, l.size)
            assert(l.all { it == id })

            b = byteArrayOf(0, 3, 2, 4, 5, 6, 7, 8)
            testDevice.receiveMessage(b)
            delay(100)
            assertEquals(2, l.size)
            // assert(l.all { it == id })
            // TODO: more
        }

        @Test
        fun `works with message cache`(): Unit = runBlocking { // TODO: refactor
            val l = mutableListOf<Int>()
            val f =
                correct()
                    .withMsgScanInterval(Duration.ofMillis(100))
                    .setMessageCallback { i, byte -> l.add(i) }
                    .withMsgCacheDelete(Duration.ofSeconds(1))
                    .build()
            val b = byteArrayOf(0, 2, 3, 4, 5, 6, 7)
            val id = b.slice(1..4).toByteArray().toInt()

            f.start()
            delay(100)
            testDevice.receiveMessage(b)
            delay(100)
            assertEquals(1, l.size)
            assert(l.all { it == id })

            testDevice.receiveMessage(b)
            delay(100)
            println(l)
            assertEquals(1, l.size)
            assert(l.all { it == id })
            delay(1500)

            // b = byteArrayOf(0, 3, 2, 4, 5, 6, 7, 8)
            testDevice.receiveMessage(b)
            delay(100)
            println(l)
            assertEquals(2, l.size)
            assert(l.all { it == id })
            // TODO: more
        }

        @Test // TODO
        fun `relays correctly`(): Unit = runBlocking { // TODO: refactor
        }

        @Test // TODO
        fun `filters correctly`(): Unit = runBlocking { // TODO: refactor
        }

        @Test
        fun `return arrays too large`(): Unit = runBlocking { // TODO: refactor
            val f = correct().withMsgScanInterval(Duration.ofMillis(100)).build()
            /*
                    val runFunc: (String, ByteArray) -> Unit = { name, b ->
                        EventMesh::class
                            .members
                            .find { m -> m.name == name }!!
                            .apply { isAccessible = true }
                            .call(f, b)
                    }
                    val scan = "scanningCallback"
                    assertFails { runFunc(scan, byteArrayOf(1)) }
                    assertFails { runFunc(scan, byteArrayOf(1, 2)) }
                    assertFails { runFunc(scan, byteArrayOf(1, 2, 3)) }
                    assertFails { runFunc(scan, byteArrayOf(1, 2, 3, 4)) }
            */
            val scanner = getValueFromClass<EventMesh<Int, Byte>, Job?>(f, "btScanner")
            f.start()
            delay(100)
            testDevice.receiveMessage(byteArrayOf())
            delay(100)
            scanner!!.children.forEach { println(it) }
            println()
            assert(scanner.children.any { it.isCancelled })
            // TODO: more
        }
    }

    @Nested
    inner class Transmitting {
        @Test
        fun `testing data generator`(): Unit = runBlocking { // TODO: refactor
            val l = mutableListOf<Byte>()
            val d = AtomicInteger(0)
            val f =
                correct()
                    .withMsgSendInterval(Duration.ofMillis(100))
                    .withMsgSendTimeout(Duration.ofMillis(10))
                    .setMessageCallback { i, byte -> l.add(byte) }
                    .withMsgCacheDelete(Duration.ofSeconds(1))
                    .setDataGenerator { d.getAndIncrement().toByte() }
                    .build()

            f.start()
            delay(1000)
            f.stop()

            assertEquals(1, testDevice.transmittedMessages.get().size)
            assertEquals(
                (d.get() - 1).toByte(), testDevice.transmittedMessages.get().first().last())
        }

        @Test
        fun `testing id generator`(): Unit = runBlocking { // TODO: refactor
            val l = mutableListOf<Int>()
            val d = AtomicInteger(0)
            val f =
                correct()
                    .withMsgSendInterval(Duration.ofMillis(100))
                    .withMsgSendTimeout(Duration.ofMillis(10))
                    .setMessageCallback { i, byte -> l.add(i) }
                    .withMsgCacheDelete(Duration.ofSeconds(1))
                    .setIDGenerator { d.getAndIncrement() }
                    .build()

            f.start()
            delay(1000)
            f.stop()

            assertEquals(1, testDevice.transmittedMessages.get().size)
            assertEquals(
                (d.get() - 1),
                testDevice.transmittedMessages.get().first().slice(1..5).toByteArray().toInt())
        }

        @Test
        fun `testing data const`(): Unit = runBlocking { // TODO: refactor
            val l = mutableListOf<Byte>()
            val d: Byte = 0
            val f =
                correct()
                    .withMsgSendInterval(Duration.ofMillis(100))
                    .withMsgSendTimeout(Duration.ofMillis(10))
                    .setMessageCallback { i, byte -> l.add(byte) }
                    .withMsgCacheDelete(Duration.ofSeconds(1))
                    .setDataConstant(d)
                    .build()

            f.start()
            delay(1000)
            f.stop()

            assertEquals(1, testDevice.transmittedMessages.get().size)
            assertEquals(d, testDevice.transmittedMessages.get().first().last())
        }

        @Test
        fun `testing id const`(): Unit = runBlocking { // TODO: refactor
            val l = mutableListOf<Int>()
            val d = 10
            val f =
                correct()
                    .withMsgSendInterval(Duration.ofMillis(100))
                    .withMsgSendTimeout(Duration.ofMillis(10))
                    .setMessageCallback { i, byte -> l.add(i) }
                    .withMsgCacheDelete(Duration.ofSeconds(1))
                    .setIDConstant(d)
                    .build()

            f.start()
            delay(1000)
            f.stop()

            assertEquals(1, testDevice.transmittedMessages.get().size)
            assertEquals(
                d, testDevice.transmittedMessages.get().first().slice(1..5).toByteArray().toInt())
        }

        @Test
        fun `testing correct ttl`(): Unit = runBlocking { // TODO: refactor
            val l = mutableListOf<Int>()
            val d = 100u
            val f =
                correct()
                    .withMsgSendInterval(Duration.ofMillis(100))
                    .withMsgSendTimeout(Duration.ofMillis(10))
                    .setMessageCallback { i, byte -> l.add(i) }
                    .withMsgCacheDelete(Duration.ofSeconds(1))
                    .withMsgTTL(d)
                    .build()

            f.start()
            delay(1000)
            f.stop()

            assertEquals(1, testDevice.transmittedMessages.get().size)
            assertEquals(d, testDevice.transmittedMessages.get().first().first().toUInt())
        }
    }

    @Nested
    inner class Builder {

        @Test
        fun `missing all functions`() {
            assertFails { EventMesh.builder<Int, Byte>().build() }
        }

        @Test
        fun `missing one function`() {
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

        /*TODO: can't really be tested, since it has been moved to variables in the builder (very hidden), maybe refactor?
               @Test
               fun `correct number of filters`() {
                   val f = correct()
                   f.addFilterFunction { _ -> true }
                   var l =
                       getValueFromClass<EventMesh<Int, Byte>, List<(Int) -> Boolean>>(
                           f.build(), "filterID")
                   Assertions.assertEquals(l.size, 1)
                   f.addFilterFunction { i -> i <= 100 }
                   l =
                       getValueFromClass<EventMesh<Int, Byte>, List<(Int) -> Boolean>>(
                           f.build(), "filterID")
                   Assertions.assertEquals(l.size, 2)
                   f.addFilterFunction({ i -> i >= 10 }, { i -> i and 1 == 1 })
                   l =
                       getValueFromClass<EventMesh<Int, Byte>, List<(Int) -> Boolean>>(
                           f.build(), "filterID")
                   assert(l.all { fn -> fn(21) })
                   Assertions.assertFalse(l.all { fn -> fn(9) })
                   Assertions.assertFalse(l.all { fn -> fn(101) })
                   Assertions.assertFalse(l.all { fn -> fn(40) })
               }
        */

        /* TODO: can't really be tested, since it has been moved to variables in the builder (very hidden), maybe refactor?
        @Test
        fun `overriding data`() {
            val clazz = EventMesh.Companion::class.members
                .find { m -> m.name == "BuilderImpl" }!!
                .apply { isAccessible = true }
                //.call(target) as R

            val f = correct()
            f.setDataConstant(9)
            var l =
                getValueFromClass<clazz<Int, Byte>, Either<Byte, () -> Byte>>(
                    f, "msgData")
            assert(l.isLeft())
            Assertions.assertEquals(l.getLeft()!!, 9)

            f.setDataGenerator { 90 }
            l =
                getValueFromClass<EventMesh.Companion.Builder<Int, Byte>, Either<Byte, () -> Byte>>(
                    f, "msgData")
            assert(l.isRight())
            Assertions.assertEquals(l.getRight()!!(), 90)

            f.setIDConstant(9)
            var k =
                getValueFromClass<EventMesh.Companion.Builder<Int, Byte>, Either<Int, () -> Int>>(f, "msgId")
            assert(k.isLeft())
            Assertions.assertEquals(k.getLeft()!!, 9)

            f.setIDGenerator { 90 }
            k = getValueFromClass<EventMesh.Companion.Builder<Int, Byte>, Either<Int, () -> Int>>(f, "msgId")
            assert(k.isRight())
            Assertions.assertEquals(k.getRight()!!(), 90)
        }
         */

        @Test
        fun `testing optional fields`() {
            val f = correct()
            var name = "msgSendInterval"
            var default = getValueFromClass<EventMesh<Int, Byte>, Duration>(f.build(), name)
            var modded =
                getValueFromClass<EventMesh<Int, Byte>, Duration>(
                    f.withMsgSendInterval(default.plusMinutes(10)).build(), name)
            Assertions.assertNotEquals(modded, default)

            name = "msgScanInterval"
            default = getValueFromClass<EventMesh<Int, Byte>, Duration>(f.build(), name)
            modded =
                getValueFromClass<EventMesh<Int, Byte>, Duration>(
                    f.withMsgScanInterval(default.plusMinutes(10)).build(), name)
            Assertions.assertNotEquals(modded, default)

            name = "msgCacheLimit"
            val def = getValueFromClass<EventMesh<Int, Byte>, Long>(f.build(), name)
            val mod =
                getValueFromClass<EventMesh<Int, Byte>, Long>(
                    f.withMsgCacheLimit(def + 10).build(), name)
            Assertions.assertNotEquals(mod, def)

            name = "msgTTL"
            val deff = getValueFromClass<EventMesh<Int, Byte>, UInt>(f.build(), name)
            val modd =
                getValueFromClass<EventMesh<Int, Byte>, UInt>(
                    f.withMsgTTL(deff + 10u).build(), name)
            Assertions.assertNotEquals(modd, deff)

            name = "device"
            var device = getValueFromClass<EventMesh<Int, Byte>, EventMeshDevice>(f.build(), name)
            var echo = getValueFromClass<EventMeshDevice, (() -> Unit)?>(device, "echo")
            assertNull(echo)
            device =
                getValueFromClass<EventMesh<Int, Byte>, EventMeshDevice>(
                    f.withEchoCallback {}.build(), name)
            echo = getValueFromClass<EventMeshDevice, (() -> Unit)?>(device, "echo")
            assertNotNull(echo)

            val rxDuration: Long = 50
            device =
                getValueFromClass<EventMesh<Int, Byte>, EventMeshDevice>(
                    f.withMsgScanDuration(Duration.ofMillis(rxDuration)).build(), name)
            val rx = getValueFromClass<EventMeshDevice, EventMeshReceiver>(device, "receiver")
            assertEquals(rxDuration, getValueFromClass<EventMeshReceiver, Long>(rx, "duration"))
        }

        @Test
        fun `no message cache`() {
            val name = "messageCache"

            val f =
                EventMesh.builder<Int, Byte>(testDevice)
                    .setDataConstant(0)
                    .setIDGenerator { 10 }
                    .setMessageCallback { _, _ -> }
                    .setIDDecodeFunction { _ -> 9 }
                    .setDataDecodeFunction { _ -> 0 }
                    .setIDEncodeFunction { _ -> byteArrayOf(0, 1, 2, 3) }
                    .setDataEncodeFunction { b -> byteArrayOf(b) }
            // .build()
            Assertions.assertNotNull(
                getValueFromClass<EventMesh<Int, Byte>, MessageCache<Int>?>(f.build(), name))
            Assertions.assertNull(
                getValueFromClass<EventMesh<Int, Byte>, MessageCache<Int>?>(
                    f.withMsgCache(null).build(), name))

            val g =
                EventMesh.builder<Int, Byte>(testDevice)
                    .setDataConstant(0)
                    .setIDGenerator { 10 }
                    .setMessageCallback { _, _ -> }
                    .setIDDecodeFunction { _ -> 9 }
                    .setDataDecodeFunction { _ -> 0 }
                    .setIDEncodeFunction { _ -> byteArrayOf(0, 1, 2, 3) }
                    .setDataEncodeFunction { b -> byteArrayOf(b) }
            // .build()
            Assertions.assertNull(
                getValueFromClass<EventMesh<Int, Byte>, MessageCache<*>?>(
                    g.withMsgCache(null).build(), name))
        }

        @Test
        fun `overriding message cache`() {
            val name = "messageCache"

            val f = correct()
            var mc = getValueFromClass<EventMesh<Int, Byte>, MessageCache<Int>?>(f.build(), name)
            Assertions.assertNotNull(mc)
            var time = getValueFromClass<MessageCache<Int>, Long>(mc!!, "cacheTimeInSeconds")
            assertEquals(MESSAGE_CACHE_TIME, time)

            var new_time: Long = 30
            mc =
                getValueFromClass<EventMesh<Int, Byte>, MessageCache<Int>?>(
                    f.withMsgCacheDelete(Duration.ofSeconds(new_time)).build(), name)
            Assertions.assertNotNull(mc)
            time = getValueFromClass<MessageCache<Int>, Long>(mc!!, "cacheTimeInSeconds")
            assertEquals(new_time, time)

            new_time = 15
            mc =
                getValueFromClass<EventMesh<Int, Byte>, MessageCache<Int>?>(
                    f.setMessageCache(MessageCache(new_time)).build(), name)
            Assertions.assertNotNull(mc)
            time = getValueFromClass<MessageCache<Int>, Long>(mc!!, "cacheTimeInSeconds")
            assertEquals(new_time, time)
        }

        @Test
        fun `different builders`() {
            var b = EventMesh.builder<Int, Byte>()
            assertFails { b.build() }
            b.setDataConstant(0)
                .setReceiver(EventMeshReceiver(testDevice))
                .setTransmitter(EventMeshTransmitter(testDevice))
                .setIDGenerator { 10 }
                .setMessageCallback { _, _ -> }
                .setIDDecodeFunction { _ -> 9 }
                .setDataDecodeFunction { _ -> 0 }
                .setIDEncodeFunction { _ -> byteArrayOf(0, 1, 2, 3) }
                .setDataEncodeFunction { b -> byteArrayOf(b) }
                .build()

            b = EventMesh.builder(null)
            assertFails { b.build() }
            b.setDataConstant(0)
                .setReceiver(EventMeshReceiver(testDevice))
                .setTransmitter(EventMeshTransmitter(testDevice))
                .setIDGenerator { 10 }
                .setMessageCallback { _, _ -> }
                .setIDDecodeFunction { _ -> 9 }
                .setDataDecodeFunction { _ -> 0 }
                .setIDEncodeFunction { _ -> byteArrayOf(0, 1, 2, 3) }
                .setDataEncodeFunction { b -> byteArrayOf(b) }
                .build()

            b = EventMesh.builder(MessageCache(MESSAGE_CACHE_TIME))
            assertFails { b.build() }
            b.setDataConstant(0)
                .setReceiver(EventMeshReceiver(testDevice))
                .setTransmitter(EventMeshTransmitter(testDevice))
                .setIDGenerator { 10 }
                .setMessageCallback { _, _ -> }
                .setIDDecodeFunction { _ -> 9 }
                .setDataDecodeFunction { _ -> 0 }
                .setIDEncodeFunction { _ -> byteArrayOf(0, 1, 2, 3) }
                .setDataEncodeFunction { b -> byteArrayOf(b) }
                .build()

            b = EventMesh.builder(testDevice)
            assertFails { b.build() }
            b.setDataConstant(0)
                .setIDGenerator { 10 }
                .setMessageCallback { _, _ -> }
                .setIDDecodeFunction { _ -> 9 }
                .setDataDecodeFunction { _ -> 0 }
                .setIDEncodeFunction { _ -> byteArrayOf(0, 1, 2, 3) }
                .setDataEncodeFunction { b -> byteArrayOf(b) }
                .build()

            b = EventMesh.builder(testDevice, null)
            assertFails { b.build() }
            b.setDataConstant(0)
                .setIDGenerator { 10 }
                .setMessageCallback { _, _ -> }
                .setIDDecodeFunction { _ -> 9 }
                .setDataDecodeFunction { _ -> 0 }
                .setIDEncodeFunction { _ -> byteArrayOf(0, 1, 2, 3) }
                .setDataEncodeFunction { b -> byteArrayOf(b) }
                .build()

            b = EventMesh.builder(testDevice, MessageCache(MESSAGE_CACHE_TIME))
            assertFails { b.build() }
            b.setDataConstant(0)
                .setIDGenerator { 10 }
                .setMessageCallback { _, _ -> }
                .setIDDecodeFunction { _ -> 9 }
                .setDataDecodeFunction { _ -> 0 }
                .setIDEncodeFunction { _ -> byteArrayOf(0, 1, 2, 3) }
                .setDataEncodeFunction { b -> byteArrayOf(b) }
                .build()

            b = EventMesh.builder(EventMeshReceiver(testDevice), EventMeshTransmitter(testDevice))
            assertFails { b.build() }
            b.setDataConstant(0)
                .setIDGenerator { 10 }
                .setMessageCallback { _, _ -> }
                .setIDDecodeFunction { _ -> 9 }
                .setDataDecodeFunction { _ -> 0 }
                .setIDEncodeFunction { _ -> byteArrayOf(0, 1, 2, 3) }
                .setDataEncodeFunction { b -> byteArrayOf(b) }
                .build()

            b =
                EventMesh.builder(
                    EventMeshReceiver(testDevice), EventMeshTransmitter(testDevice), null)
            assertFails { b.build() }
            b.setDataConstant(0)
                .setIDGenerator { 10 }
                .setMessageCallback { _, _ -> }
                .setIDDecodeFunction { _ -> 9 }
                .setDataDecodeFunction { _ -> 0 }
                .setIDEncodeFunction { _ -> byteArrayOf(0, 1, 2, 3) }
                .setDataEncodeFunction { b -> byteArrayOf(b) }
                .build()

            b =
                EventMesh.builder(
                    EventMeshReceiver(testDevice),
                    EventMeshTransmitter(testDevice),
                    MessageCache(MESSAGE_CACHE_TIME))
            assertFails { b.build() }
            b.setDataConstant(0)
                .setIDGenerator { 10 }
                .setMessageCallback { _, _ -> }
                .setIDDecodeFunction { _ -> 9 }
                .setDataDecodeFunction { _ -> 0 }
                .setIDEncodeFunction { _ -> byteArrayOf(0, 1, 2, 3) }
                .setDataEncodeFunction { b -> byteArrayOf(b) }
                .build()
        }
    }
}
