package rasteplads.api

import java.time.Duration
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.jvm.isAccessible
import kotlin.test.*
import kotlin.test.Test
import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import rasteplads.api.EventMesh.Companion.MESSAGE_CACHE_TIME
import rasteplads.messageCache.MessageCache
import rasteplads.util.*

class EventMeshTest {

    companion object {
        val testDevice = MockDevice(100)

        fun correct() =
            EventMesh.builder<Int, Byte>(testDevice)
                .setDataConstant(0)
                .setDataSize(1)
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

        // fun delay(ms: Long) {
        //    Thread.sleep(ms)
        // }
    }

    @Test
    fun `multiple start`(): Unit = runBlocking {
        val d: Byte = 10
        val f =
            correct()
                .withMsgSendInterval(Duration.ofMillis(100))
                .withMsgSendTimeout(Duration.ofMillis(10))
                .setMessageCallback { _, _ -> }
                .withMsgCacheDelete(Duration.ofSeconds(1))
                .withMsgTTL(d)
                .build()

        try {
            f.start()
            f.start()
            f.start()
            f.start()
            f.start()
            f.start()
            f.start()
            delay(1000)
            assertNotNull(
                getValueFromClass<EventMesh<Int, Byte>, AtomicReference<Job?>>(f, "btSender").get()
            )
            assertNotNull(
                getValueFromClass<EventMesh<Int, Byte>, AtomicReference<Job?>>(f, "btScanner").get()
            )
        } finally {
            f.stop()
        }
        assertNull(
            getValueFromClass<EventMesh<Int, Byte>, AtomicReference<Job?>>(f, "btSender").get()
        )
        assertNull(
            getValueFromClass<EventMesh<Int, Byte>, AtomicReference<Job?>>(f, "btScanner").get()
        )
    }

    @Test
    fun `multiple stop`(): Unit = runBlocking {
        val d: Byte = 10
        val f =
            correct()
                .withMsgSendInterval(Duration.ofMillis(100))
                .withMsgSendTimeout(Duration.ofMillis(10))
                .setMessageCallback { _, _ -> }
                .withMsgCacheDelete(Duration.ofSeconds(1))
                .withMsgTTL(d)
                .build()

        try {
            f.start()
            delay(1000)
            assertNotNull(
                getValueFromClass<EventMesh<Int, Byte>, AtomicReference<Job?>>(f, "btSender").get()
            )
            assertNotNull(
                getValueFromClass<EventMesh<Int, Byte>, AtomicReference<Job?>>(f, "btScanner").get()
            )
        } finally {
            f.stop()
            f.stop()
            f.stop()
            f.stop()
            f.stop()
            f.stop()
            f.stop()
        }
        assertNull(
            getValueFromClass<EventMesh<Int, Byte>, AtomicReference<Job?>>(f, "btSender").get()
        )
        assertNull(
            getValueFromClass<EventMesh<Int, Byte>, AtomicReference<Job?>>(f, "btScanner").get()
        )
    }

    @Nested
    inner class Receiving {
        @BeforeTest
        @AfterTest
        fun clean(): Unit = runBlocking {
            testDevice.stopReceiving()
            testDevice.stopTransmitting()
            testDevice.receivedMsg.set(null)
            testDevice.transmitting.set(false)
            testDevice.receiving.set(false)
            testDevice.transmittedMessages.get().removeAll { true }
        }

        @Test
        fun `messages get passed through`() = runBlocking {
            val l = mutableListOf<Int>()
            val f =
                correct()
                    .withMsgScanInterval(Duration.ofMillis(50))
                    .withMsgSendInterval(Duration.ofMillis(50))
                    .withMsgSendTimeout(Duration.ofMillis(10))
                    .setMessageCallback { i, _ -> l.add(i) }
                    .build()
            var b = byteArrayOf(Byte.MIN_VALUE, 2, 3, 4, 5, 6, 7)
            val id = b.slice(1..4).toByteArray().toInt()

            try {
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

                b = byteArrayOf(Byte.MIN_VALUE, 3, 2, 4, 5, 6, 7, 8)
                testDevice.receiveMessage(b)
                delay(100)
                assertEquals(2, l.size)
            } finally {
                f.stop()
            }
        }

        @Test
        fun `works with message cache`() = runBlocking {
            val l = mutableListOf<Int>()
            val f =
                correct()
                    .withMsgScanInterval(Duration.ofMillis(50))
                    .setMessageCallback { i, _ -> l.add(i) }
                    .withMsgCacheDelete(Duration.ofSeconds(1))
                    .build()
            val b = byteArrayOf(Byte.MIN_VALUE, 2, 3, 4, 5, 6, 7)
            val id = b.slice(1..4).toByteArray().toInt()

            try {
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
                delay(1500)

                // b = byteArrayOf(0, 3, 2, 4, 5, 6, 7, 8)
                testDevice.receiveMessage(b)
                delay(100)
                assertEquals(2, l.size)
                assert(l.all { it == id })
            } finally {

                f.stop()
            }
        }

        @Test
        fun `relays correctly`(): Unit = runBlocking {
            val l = mutableListOf<Int>()
            val f =
                correct()
                    .setDataConstant(0)
                    .setIDConstant(0)
                    .withMsgTTL(0)
                    .withMsgScanInterval(Duration.ofMillis(50))
                    .withMsgSendInterval(Duration.ofMillis(50))
                    .withMsgSendTimeout(Duration.ofMillis(10))
                    .setMessageCallback { i, _ -> l.add(i) }
                    .withMsgCacheDelete(Duration.ofSeconds(1))
                    .build()

            try {
                f.start()
                delay(500)
                assertEquals(
                    0,
                    testDevice.transmittedMessages
                        .get()
                        .distinct()
                        .filter { b -> !b.all { i -> i == (0).toByte() } }
                        .size
                )

                testDevice.receiveMessage(byteArrayOf(Byte.MIN_VALUE, 0, 0, 0, 3, 6, 7))
                delay(500)
                assertEquals(
                    0,
                    testDevice.transmittedMessages
                        .get()
                        .distinct()
                        .filter { b -> !b.all { i -> i == (0).toByte() } }
                        .size
                )

                testDevice.receiveMessage(byteArrayOf(Byte.MIN_VALUE.inc(), 0, 0, 0, 4, 6, 7))
                delay(500)
                assertEquals(
                    1,
                    testDevice.transmittedMessages
                        .get()
                        .map { it.toList() }
                        .distinct()
                        .filterNot { b -> b.slice(1 ..< b.size).all { i -> i == (0).toByte() } }
                        .size
                )

                // NO DOUBLE RELAY
                testDevice.receiveMessage(byteArrayOf(Byte.MIN_VALUE.inc(), 0, 0, 0, 4, 6, 7))
                delay(500)
                assertEquals(
                    1,
                    testDevice.transmittedMessages
                        .get()
                        .distinct()
                        .filter { b -> !b.all { i -> i == (0).toByte() } }
                        .size
                )
            } finally {
                f.stop()
            }
        }

        @Test
        fun `relays correctly without cache`(): Unit = runBlocking {
            val f =
                correct()
                    .setDataConstant(0)
                    .setIDConstant(0)
                    .withMsgTTL(Byte.MIN_VALUE)
                    .withMsgScanInterval(Duration.ofMillis(50))
                    .withMsgSendInterval(Duration.ofMillis(50))
                    .withMsgSendTimeout(Duration.ofMillis(10))
                    .setMessageCallback { _, _ -> }
                    .withMsgCache(null)
                    .build()

            try {
                f.start()
                delay(500)
                assertEquals(
                    0,
                    testDevice.transmittedMessages
                        .get()
                        .distinct()
                        .filterNot { b -> b.slice(1 ..< b.size).all { i -> i == (0).toByte() } }
                        .size
                )

                testDevice.receiveMessage(byteArrayOf(Byte.MIN_VALUE, 0, 0, 0, 3, 6, 7))
                delay(500)
                assertEquals(
                    0,
                    testDevice.transmittedMessages
                        .get()
                        .map(ByteArray::toList)
                        // .distinct()
                        .filterNot { b -> b.slice(1 ..< b.size).all { i -> i == (0).toByte() } }
                        .size
                )

                val b = byteArrayOf(Byte.MIN_VALUE.inc(), 0, 0, 0, 4, 6, 7)
                testDevice.receiveMessage(b)
                delay(500)
                assertEquals(
                    1,
                    testDevice.transmittedMessages
                        .get()
                        .map(ByteArray::toList)
                        .distinct()
                        .filterNot { bp -> bp.slice(1 ..< bp.size).all { i -> i == (0).toByte() } }
                        .size
                )

                val bEx = byteArrayOf(Byte.MIN_VALUE, 0, 0, 0, 4, 6, 7)

                // DOUBLE RELAY
                testDevice.receiveMessage(b)
                delay(500)
                assert(
                    testDevice.transmittedMessages
                        .get()
                        .filterNot { bp -> bp.slice(1 ..< bp.size).all { i -> i == (0).toByte() } }
                        .all { l -> bEx.contentEquals(l) }
                )
            } finally {

                f.stop()
            }
        }

        @Test
        fun `filters correctly with one`() {
            fun delay(ms: Long) {
                Thread.sleep(ms)
            }
            val l = mutableListOf<Int>()
            val f =
                correct()
                    .setMessageCallback { i, _ -> l.add(i) }
                    .withMsgScanInterval(Duration.ofMillis(50))
                    .withMsgSendInterval(Duration.ofMillis(50))
                    .withMsgSendTimeout(Duration.ofMillis(10))
                    .addFilterFunction { i -> i % 2 == 0 } // Only even numbers
                    .withMsgCacheDelete(Duration.ofSeconds(1))
                    .build()

            try {
                f.start()
                delay(100)
                testDevice.receiveMessage(byteArrayOf(Byte.MIN_VALUE, 0, 0, 0, 2, 6, 7))
                Thread.sleep(500)
                assertEquals(1, l.size)

                testDevice.receiveMessage(byteArrayOf(Byte.MIN_VALUE, 0, 0, 0, 3, 6, 7))
                delay(200)
                assertEquals(1, l.size)
                delay(150)

                // b = byteArrayOf(0, 3, 2, 4, 5, 6, 7, 8)
                testDevice.receiveMessage(byteArrayOf(Byte.MIN_VALUE, 0, 0, 0, 4, 6, 7))
                delay(200)
                assertEquals(2, l.size)
                delay(100)

                testDevice.receiveMessage(byteArrayOf(Byte.MIN_VALUE, 0, 0, 0, 4, 6, 7))
                delay(200)
                assertEquals(2, l.size)
            } finally {
                f.stop()
            }
        }

        @Test
        fun `filters correctly with multiple`(): Unit = runBlocking {
            val l = mutableListOf<Int>()
            val f =
                correct()
                    .setMessageCallback { i, _ -> l.add(i) }
                    .withMsgScanInterval(Duration.ofMillis(50))
                    .withMsgSendInterval(Duration.ofMillis(50))
                    .withMsgSendTimeout(Duration.ofMillis(10))
                    .addFilterFunction(
                        { i -> i % 2 == 0 }, // even
                        { i -> i > 5 }, // greater than 5
                        { i -> i < 15 } // less than 15
                    )
                    .withMsgCacheDelete(Duration.ofSeconds(1))
                    .setIDConstant(0)
                    .setDataConstant(0)
                    .withMsgTTL(Byte.MIN_VALUE)
                    .build()

            try {
                f.start()
                delay(100)
                testDevice.receiveMessage(byteArrayOf(Byte.MIN_VALUE, 0, 0, 0, 2, 6, 7))
                delay(200)
                assertEquals(
                    0,
                    testDevice.transmittedMessages
                        .get()
                        .filterNot { i -> i.slice(1 ..< i.size).all { it == (0).toByte() } }
                        .size
                )

                testDevice.receiveMessage(byteArrayOf(Byte.MIN_VALUE, 0, 0, 0, 6, 6, 7))
                delay(200)
                assertEquals(1, l.size)
                delay(150)

                // b = byteArrayOf(0, 3, 2, 4, 5, 6, 7, 8)
                testDevice.receiveMessage(byteArrayOf(Byte.MIN_VALUE, 0, 0, 0, 16, 6, 7))
                delay(100)
                assertEquals(1, l.size)
                delay(200)

                testDevice.receiveMessage(byteArrayOf(Byte.MIN_VALUE, 0, 0, 0, 8, 6, 7))
                delay(200)
                assertEquals(2, l.size)
                delay(100)

                testDevice.receiveMessage(byteArrayOf(Byte.MIN_VALUE, 0, 0, 0, 8, 6, 7))
                delay(200)
                assertEquals(2, l.size)
            } finally {

                f.stop()
            }
        }

        /* TODO: Not detecting exceptions
        @Test
        fun `return arrays too large`(): Unit = runBlocking {
            val f =
                correct()
                    // .withMsgScanInterval(Duration.ofMillis(100))
                    .withMsgScanInterval(Duration.ofMillis(50))
                    .withMsgSendInterval(Duration.ofMillis(50))
                    .withMsgSendTimeout(Duration.ofMillis(10))
                    .build()
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
            val scanner =
                getValueFromClass<EventMesh<Int, Byte>, AtomicReference<Job?>>(f, "btScanner")
            assertFails {
                f.start()
                delay(1000)
                testDevice.receiveMessage(byteArrayOf())
                delay(1000)
                f.stop()
                // j.join()
            }
            scanner.get()!!.children.forEach { println(it) }
        }
         */
    }

    @Nested
    inner class Transmitting {
        @BeforeTest
        @AfterTest
        fun clean() {
            testDevice.stopReceiving()
            testDevice.stopTransmitting()
            testDevice.receivedMsg.set(null)
            testDevice.transmitting.set(false)
            testDevice.receiving.set(false)
            testDevice.transmittedMessages.get().removeAll { true }
        }

        @Test
        fun `testing data generator`(): Unit = runBlocking {
            var d: Byte = 0
            val f =
                correct()
                    .withMsgSendInterval(Duration.ofMillis(100))
                    .withMsgSendTimeout(Duration.ofMillis(10))
                    .setMessageCallback { _, _ -> }
                    .withMsgCacheDelete(Duration.ofSeconds(1))
                    .setDataGenerator { d++ }
                    .setIDConstant(0)
                    .withMsgTTL(Byte.MIN_VALUE)
                    .build()

            try {
                f.start()
                delay(1000)
            } finally {
                f.stop()
            }

            assertEquals(
                d.toInt(),
                testDevice.transmittedMessages.get().map(ByteArray::toList).distinct().size
            )
            assertEquals(
                0,
                testDevice.transmittedMessages
                    .get()
                    .map(ByteArray::toList)
                    .distinct()
                    .first()
                    .last()
            )
            assertEquals(
                d.dec(),
                testDevice.transmittedMessages.get().map(ByteArray::toList).distinct().last().last()
            )
        }

        @Test
        fun `testing id generator`(): Unit = runBlocking {
            var d = 0
            val f =
                correct()
                    .withMsgSendInterval(Duration.ofMillis(100))
                    .withMsgSendTimeout(Duration.ofMillis(10))
                    .setMessageCallback { _, _ -> }
                    .withMsgCacheDelete(Duration.ofSeconds(1))
                    .setIDGenerator { d++ }
                    .setDataConstant(0)
                    .withMsgTTL(Byte.MIN_VALUE)
                    .build()

            try {
                f.start()
                delay(1000)
            } finally {
                f.stop()
            }

            assertEquals(
                d,
                testDevice.transmittedMessages.get().map(ByteArray::toList).distinct().size
            )
            assertEquals(
                d - 1,
                testDevice.transmittedMessages
                    .get()
                    .map(ByteArray::toList)
                    .distinct()
                    .last()
                    .slice(1..5)
                    .toByteArray()
                    .toInt()
            )
            assertEquals(
                0,
                testDevice.transmittedMessages
                    .get()
                    .map(ByteArray::toList)
                    .distinct()
                    .first()
                    .slice(1..5)
                    .toByteArray()
                    .toInt()
            )
        }

        @Test
        fun `testing data const`(): Unit = runBlocking {
            val d: Byte = 10
            val f =
                correct()
                    .withMsgSendInterval(Duration.ofMillis(100))
                    .withMsgSendTimeout(Duration.ofMillis(10))
                    .setMessageCallback { _, _ -> }
                    .withMsgCacheDelete(Duration.ofSeconds(1))
                    .setDataConstant(d)
                    .setIDConstant(0)
                    .withMsgTTL(Byte.MIN_VALUE)
                    .build()

            try {
                f.start()
                delay(1000)
            } finally {

                f.stop()
            }

            // assertEquals(1, testDevice.transmittedMessages.get().size)
            assert(
                testDevice.transmittedMessages.get().map(ByteArray::toList).all { b ->
                    b.last() == d
                }
            )
        }

        @Test
        fun `testing id const`(): Unit = runBlocking {
            val d = 10
            val f =
                correct()
                    .withMsgSendInterval(Duration.ofMillis(100))
                    .withMsgSendTimeout(Duration.ofMillis(10))
                    .setMessageCallback { _, _ -> }
                    .withMsgCacheDelete(Duration.ofSeconds(1))
                    .setIDConstant(d)
                    .setDataConstant(0)
                    .withMsgTTL(Byte.MIN_VALUE)
                    .build()

            try {
                f.start()
                delay(1000)
            } finally {

                f.stop()
            }

            // assertEquals(d, testDevice.transmittedMessages.get().size)
            assert(
                testDevice.transmittedMessages.get().map(ByteArray::toList).all { b ->
                    b.slice(1..5).toByteArray().toInt() == d
                }
            )
        }

        @Test
        fun `testing correct ttl`(): Unit = runBlocking {
            val d: Byte = 10
            val f =
                correct()
                    .withMsgSendInterval(Duration.ofMillis(100))
                    .withMsgSendTimeout(Duration.ofMillis(10))
                    .setMessageCallback { _, _ -> }
                    .withMsgCacheDelete(Duration.ofSeconds(1))
                    .withMsgTTL(d)
                    .build()

            try {
                f.start()
                delay(1000)
            } finally {

                f.stop()
            }

            assert(testDevice.transmittedMessages.get().distinct().all { i -> i.first() == d })
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
                    .setDataSize(10)
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
                    .setDataSize(10)
                    .setIDEncodeFunction { byteArrayOf(0, 1, 2, 3) }
                    .setDataEncodeFunction { b -> byteArrayOf(b) }
                    .build()
            }
            assertFails {
                EventMesh.builder<Int, Byte>()
                    .setDataConstant(0)
                    .setIDGenerator { 10 }
                    .setIDDecodeFunction { _ -> 9 }
                    .setDataSize(10)
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
                    .setDataSize(10)
                    .setIDEncodeFunction { byteArrayOf(0, 1, 2, 3) }
                    .setDataEncodeFunction { b -> byteArrayOf(b) }
                    .build()
            }
            assertFails {
                EventMesh.builder<Int, Byte>()
                    .setDataConstant(0)
                    .setIDGenerator { 10 }
                    .setDataSize(10)
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
                    .setDataSize(10)
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
                    .setDataSize(10)
                    .setDataEncodeFunction { b -> byteArrayOf(b) }
                    .build()
            }
            assertFails {
                EventMesh.builder<Int, Byte>()
                    .setDataConstant(0)
                    .setDataSize(10)
                    .setIDGenerator { 10 }
                    .setMessageCallback { _, _ -> }
                    .setIDDecodeFunction { _ -> 9 }
                    .setDataDecodeFunction { _ -> 0 }
                    .setIDEncodeFunction { byteArrayOf(0, 1, 2, 3) }
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
                    .setDataEncodeFunction { b -> byteArrayOf(b) }
                    .build()
            }
        }

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
                    f.withMsgSendInterval(default.plusMinutes(10)).build(),
                    name
                )
            Assertions.assertNotEquals(modded, default)

            name = "msgScanInterval"
            default = getValueFromClass<EventMesh<Int, Byte>, Duration>(f.build(), name)
            modded =
                getValueFromClass<EventMesh<Int, Byte>, Duration>(
                    f.withMsgScanInterval(default.plusMinutes(10)).build(),
                    name
                )
            Assertions.assertNotEquals(modded, default)

            /*
            name = "msgCacheLimit"
            val def = getValueFromClass<EventMesh<Int, Byte>, Long>(f.build(), name)
            val mod =
                getValueFromClass<EventMesh<Int, Byte>, Long>(
                    f.withMsgCacheLimit(def + 10).build(), name)
            Assertions.assertNotEquals(mod, def)
             */

            name = "msgTTL"
            val deff: Byte = getValueFromClass<EventMesh<Int, Byte>, Byte>(f.build(), name)
            val modd =
                getValueFromClass<EventMesh<Int, Byte>, Byte>(
                    f.withMsgTTL((deff + 10).toByte()).build(),
                    name
                )
            Assertions.assertNotEquals(modd, deff)

            name = "device"
            var device = getValueFromClass<EventMesh<Int, Byte>, EventMeshDevice>(f.build(), name)
            var echo = getValueFromClass<EventMeshDevice, (() -> Unit)?>(device, "echo")
            assertNull(echo)
            device =
                getValueFromClass<EventMesh<Int, Byte>, EventMeshDevice>(
                    f.withEchoCallback {}.build(),
                    name
                )
            echo = getValueFromClass<EventMeshDevice, (() -> Unit)?>(device, "echo")
            assertNotNull(echo)

            val rxDuration: Long = 50
            device =
                getValueFromClass<EventMesh<Int, Byte>, EventMeshDevice>(
                    f.withMsgScanDuration(Duration.ofMillis(rxDuration)).build(),
                    name
                )
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
                    .setDataSize(10)
                    .setMessageCallback { _, _ -> }
                    .setIDDecodeFunction { _ -> 9 }
                    .setDataDecodeFunction { _ -> 0 }
                    .setIDEncodeFunction { _ -> byteArrayOf(0, 1, 2, 3) }
                    .setDataEncodeFunction { b -> byteArrayOf(b) }
            // .build()
            Assertions.assertNotNull(
                getValueFromClass<EventMesh<Int, Byte>, MessageCache<Int>?>(f.build(), name)
            )
            Assertions.assertNull(
                getValueFromClass<EventMesh<Int, Byte>, MessageCache<Int>?>(
                    f.withMsgCache(null).build(),
                    name
                )
            )

            val g =
                EventMesh.builder<Int, Byte>(testDevice)
                    .setDataConstant(0)
                    .setIDGenerator { 10 }
                    .setDataSize(10)
                    .setMessageCallback { _, _ -> }
                    .setIDDecodeFunction { _ -> 9 }
                    .setDataDecodeFunction { _ -> 0 }
                    .setIDEncodeFunction { _ -> byteArrayOf(0, 1, 2, 3) }
                    .setDataEncodeFunction { b -> byteArrayOf(b) }
            // .build()
            Assertions.assertNull(
                getValueFromClass<EventMesh<Int, Byte>, MessageCache<*>?>(
                    g.withMsgCache(null).build(),
                    name
                )
            )
        }

        @Test
        fun `overriding message cache`() {
            val name = "messageCache"

            val ms = "cacheTimeInMilliseconds"
            val f = correct()
            var mc = getValueFromClass<EventMesh<Int, Byte>, MessageCache<Int>?>(f.build(), name)
            Assertions.assertNotNull(mc)
            var time = getValueFromClass<MessageCache<Int>, Long>(mc!!, ms)
            assertEquals(MESSAGE_CACHE_TIME, time)

            var newTime: Long = 30_000
            mc =
                getValueFromClass<EventMesh<Int, Byte>, MessageCache<Int>?>(
                    f.withMsgCacheDelete(Duration.ofMillis(newTime)).build(),
                    name
                )
            Assertions.assertNotNull(mc)
            time = getValueFromClass<MessageCache<Int>, Long>(mc!!, ms)
            assertEquals(newTime, time)

            newTime = 15_000
            mc =
                getValueFromClass<EventMesh<Int, Byte>, MessageCache<Int>?>(
                    f.setMessageCache(MessageCache(newTime)).build(),
                    name
                )
            Assertions.assertNotNull(mc)
            time = getValueFromClass<MessageCache<Int>, Long>(mc!!, ms)
            assertEquals(newTime, time)
        }

        @Test
        fun `different builders`() {
            var b = EventMesh.builder<Int, Byte>()
            assertFails { b.build() }
            b.setDataConstant(0)
                .setReceiver(EventMeshReceiver(testDevice))
                .setTransmitter(EventMeshTransmitter(testDevice))
                .setDataSize(10)
                .setIDGenerator { 10 }
                .setMessageCallback { _, _ -> }
                .setIDDecodeFunction { _ -> 9 }
                .setDataDecodeFunction { _ -> 0 }
                .setIDEncodeFunction { _ -> byteArrayOf(0, 1, 2, 3) }
                .setDataEncodeFunction { bp -> byteArrayOf(bp) }
                .build()

            b = EventMesh.builder(null)
            assertFails { b.build() }
            b.setDataConstant(0)
                .setReceiver(EventMeshReceiver(testDevice))
                .setTransmitter(EventMeshTransmitter(testDevice))
                .setIDGenerator { 10 }
                .setDataSize(10)
                .setMessageCallback { _, _ -> }
                .setIDDecodeFunction { _ -> 9 }
                .setDataDecodeFunction { _ -> 0 }
                .setIDEncodeFunction { _ -> byteArrayOf(0, 1, 2, 3) }
                .setDataEncodeFunction { bp -> byteArrayOf(bp) }
                .build()

            b = EventMesh.builder(MessageCache(MESSAGE_CACHE_TIME))
            assertFails { b.build() }
            b.setDataConstant(0)
                .setReceiver(EventMeshReceiver(testDevice))
                .setTransmitter(EventMeshTransmitter(testDevice))
                .setIDGenerator { 10 }
                .setMessageCallback { _, _ -> }
                .setDataSize(10)
                .setIDDecodeFunction { _ -> 9 }
                .setDataDecodeFunction { _ -> 0 }
                .setIDEncodeFunction { _ -> byteArrayOf(0, 1, 2, 3) }
                .setDataEncodeFunction { bp -> byteArrayOf(bp) }
                .build()

            b = EventMesh.builder(testDevice)
            assertFails { b.build() }
            b.setDataConstant(0)
                .setIDGenerator { 10 }
                .setMessageCallback { _, _ -> }
                .setIDDecodeFunction { _ -> 9 }
                .setDataDecodeFunction { _ -> 0 }
                .setDataSize(10)
                .setIDEncodeFunction { _ -> byteArrayOf(0, 1, 2, 3) }
                .setDataEncodeFunction { bp -> byteArrayOf(bp) }
                .build()

            b = EventMesh.builder(testDevice, null)
            assertFails { b.build() }
            b.setDataConstant(0)
                .setIDGenerator { 10 }
                .setMessageCallback { _, _ -> }
                .setIDDecodeFunction { _ -> 9 }
                .setDataSize(10)
                .setDataDecodeFunction { _ -> 0 }
                .setIDEncodeFunction { _ -> byteArrayOf(0, 1, 2, 3) }
                .setDataEncodeFunction { bp -> byteArrayOf(bp) }
                .build()

            b = EventMesh.builder(testDevice, MessageCache(MESSAGE_CACHE_TIME))
            assertFails { b.build() }
            b.setDataConstant(0)
                .setIDGenerator { 10 }
                .setMessageCallback { _, _ -> }
                .setIDDecodeFunction { _ -> 9 }
                .setDataDecodeFunction { _ -> 0 }
                .setIDEncodeFunction { _ -> byteArrayOf(0, 1, 2, 3) }
                .setDataSize(10)
                .setDataEncodeFunction { bp -> byteArrayOf(bp) }
                .build()

            b = EventMesh.builder(EventMeshReceiver(testDevice), EventMeshTransmitter(testDevice))
            assertFails { b.build() }
            b.setDataConstant(0)
                .setDataSize(10)
                .setIDGenerator { 10 }
                .setMessageCallback { _, _ -> }
                .setIDDecodeFunction { _ -> 9 }
                .setDataDecodeFunction { _ -> 0 }
                .setIDEncodeFunction { _ -> byteArrayOf(0, 1, 2, 3) }
                .setDataEncodeFunction { bp -> byteArrayOf(bp) }
                .build()

            b =
                EventMesh.builder(
                    EventMeshReceiver(testDevice),
                    EventMeshTransmitter(testDevice),
                    null
                )
            assertFails { b.build() }
            b.setDataConstant(0)
                .setIDGenerator { 10 }
                .setMessageCallback { _, _ -> }
                .setDataSize(10)
                .setIDDecodeFunction { _ -> 9 }
                .setDataDecodeFunction { _ -> 0 }
                .setIDEncodeFunction { _ -> byteArrayOf(0, 1, 2, 3) }
                .setDataEncodeFunction { bp -> byteArrayOf(bp) }
                .build()

            b =
                EventMesh.builder(
                    EventMeshReceiver(testDevice),
                    EventMeshTransmitter(testDevice),
                    MessageCache(MESSAGE_CACHE_TIME)
                )
            assertFails { b.build() }
            b.setDataConstant(0)
                .setIDGenerator { 10 }
                .setMessageCallback { _, _ -> }
                .setIDDecodeFunction { _ -> 9 }
                .setDataSize(10)
                .setDataDecodeFunction { _ -> 0 }
                .setIDEncodeFunction { _ -> byteArrayOf(0, 1, 2, 3) }
                .setDataEncodeFunction { bp -> byteArrayOf(bp) }
                .build()
        }
    }
}
