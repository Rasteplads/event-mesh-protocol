package rasteplads.api

import java.time.Duration
import java.util.concurrent.atomic.*
import kotlin.reflect.jvm.isAccessible
import kotlin.test.*
import kotlin.test.Test
import kotlinx.coroutines.*
import org.junit.jupiter.api.Nested
import rasteplads.api.EventMesh.Companion.ID_MAX_SIZE
import rasteplads.util.byte_array_extension.generateRands
import rasteplads.util.plus

class MockDevice(override val transmissionInterval: Long) : TransportDevice {
    val transmittedMessages: AtomicReference<MutableList<ByteArray>> =
        AtomicReference(mutableListOf())
    val receivedPool: AtomicReference<MutableList<ByteArray>> = AtomicReference(mutableListOf())
    val receivedMsg: AtomicReference<ByteArray?> = AtomicReference(null)

    val transmitting: AtomicBoolean = AtomicBoolean(false)
    val receiving: AtomicBoolean = AtomicBoolean(false)

    var tx: Job? = null

    override fun beginTransmitting(message: ByteArray): Unit = runBlocking {
        transmittedMessages.get().removeAll { true }
        transmitting.set(true)

        tx =
            GlobalScope.launch {
                try {
                    while (isActive) {
                        yield()
                        transmittedMessages.get().add(message)
                        yield()
                        delay(transmissionInterval)
                        yield()
                    }
                } catch (_: Exception) {}
            }
    }

    override fun stopTransmitting() {
        tx?.cancel()
        tx = null
        transmitting.set(false)
    }

    override fun beginReceiving(callback: suspend (ByteArray) -> Unit) = runBlocking {
        receiving.set(true)

        while (receiving.get()) {
            receivedMsg.getAndSet(null)?.let { callback(it) }
            yield()
            /*
            val iter = receivedPool.get().iterator()
            for (i in iter) {
                callback(i)
                iter.remove()
            }
             */
            delay(50) // 1 sec
            yield()
        }
    }

    override fun stopReceiving(): Unit = receiving.set(false)

    fun receiveMessage(b: ByteArray) = runBlocking {
        if (receiving.get()) {
            receivedMsg.set(b)
            while (receiving.get() && receivedMsg.get() != null) {
                delay(50)
                yield()
            }
        }
        // receivedPool.get().add(b)
    }
}

class EventMeshDeviceTest {

    private val launchPool = mutableListOf<Job>()
    // @BeforeTest
    @AfterTest
    fun clean(): Unit = runBlocking {
        device.stopReceiving()
        device.stopTransmitting()
        //device.transmittedMessages.get().removeAll { true }
        device.receivedMsg.set(null)
        device.receivedPool.get().removeAll { true }
        launchPool.forEach { it.cancelAndJoin() }
        launchPool.removeAll { true }
    }

    companion object {
        const val T_INTERVAL: Long = 100
        val device = MockDevice(T_INTERVAL)

        inline fun <reified C, reified R> getValueFromClass(target: C, field: String): R =
            C::class
                .members
                .find { m -> m.name == field }!!
                .apply { isAccessible = true }
                .call(target) as R
    }

    @Test
    fun `throws with small id`(): Unit = runBlocking {
        val e =
            EventMeshDevice(
                EventMeshReceiver(device), EventMeshTransmitter(device), Duration.ofMillis(100))

        for (i in ID_MAX_SIZE + 1..Short.MAX_VALUE) { // Arbitrary big value
            assertFails { e.startTransmitting(0, generateRands(i).toByteArray(), byteArrayOf()) }
        }
    }

    @Test
    fun `receives correctly`(): Unit = runBlocking {
        val b = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        val rx = EventMeshReceiver(device)
        val l = mutableListOf<ByteArray>()
        rx.setReceivedMessageCallback(l::add)
        val e =
            EventMeshDevice(rx, EventMeshTransmitter(device), rxDuration = Duration.ofMillis(1000))

        launchPool.add(GlobalScope.launch { e.startReceiving() })
        delay(50)
        assert(device.receiving.get())
        device.receiveMessage(b)
        delay(100)
        device.receiveMessage(b)
        delay(100)
        device.receiveMessage(b)
        delay(1000)
        assertFalse(device.receiving.get())

        assertEquals(3, l.size)
        assert(l.all { it.contentEquals(b) })
    }

    @Test
    fun `transmits correctly`(): Unit = runBlocking {
        val b = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        val rx = EventMeshReceiver(device)
        val tx = EventMeshTransmitter(device)
        val l = mutableListOf<ByteArray>()
        rx.setReceivedMessageCallback(l::add)
        val e = EventMeshDevice(rx, tx, txTimeout = Duration.ofMillis(1045))

        val ttl: Byte = 2
        launchPool.add(GlobalScope.launch { e.startTransmitting(ttl, byteArrayOf(0, 1, 2, 3), b) })
        delay(200)
        assert(device.transmitting.get())
        assert(device.receiving.get())
        delay(1200)
        assertFalse(device.transmitting.get())
        assertFalse(device.receiving.get())
        // TODO: This fails on macos-latest on github actions - why? idk
        // 1000 / 100 = 10 (+1 cuz it does it on time 0)
        assertEquals(
            (tx.transmitTimeout.floorDiv(T_INTERVAL) + 1).toInt(),
            device.transmittedMessages.get().size)

        val combined = ttl + byteArrayOf(0, 1, 2, 3) + b
        assert(device.transmittedMessages.get().all { it.contentEquals(combined) })
    }

    @Test
    fun `calls echo correctly`(): Unit = runBlocking {
        val rx = EventMeshReceiver(device)
        var echo = false
        val tx = EventMeshTransmitter(device)
        val e = EventMeshDevice(rx, tx, txTimeout = Duration.ofMillis(1000), echo = { echo = true })

        launchPool.add(
            GlobalScope.launch { e.startTransmitting(2, byteArrayOf(0, 1, 2, 3), byteArrayOf()) })
        delay(100)
        assert(device.transmitting.get())
        assert(device.receiving.get())
        assertFalse(echo)

        delay(500)
        assertFalse(echo)
        assert(device.transmitting.get())
        assert(device.receiving.get())

        delay(1000)
        assert(echo)
        assertFalse(device.transmitting.get())
        assertFalse(device.receiving.get())
    }

    @Test
    fun `does not call echo`(): Unit = runBlocking {
        val rx = EventMeshReceiver(device)
        var echo = false
        val tx = EventMeshTransmitter(device)
        val e = EventMeshDevice(rx, tx, txTimeout = Duration.ofMillis(1001), echo = { echo = true })

        launchPool.add(
            GlobalScope.launch { e.startTransmitting(2, byteArrayOf(0, 1, 2, 3), byteArrayOf()) })
        delay(100)
        assertFalse(echo)
        assert(device.transmitting.get())
        assert(device.receiving.get())
        delay(500)
        assertFalse(echo)
        device.receiveMessage(byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8))
        delay(1000)
        assertFalse(echo)
        assertFalse(device.transmitting.get())
        assertFalse(device.receiving.get())
    }

    @Nested
    inner class BuilderTest {
        @Test
        fun `Missing transmitter`() {
            val rx = EventMeshReceiver(device)
            assertFails { EventMeshDevice.Builder().withReceiver(rx).build() }
        }

        @Test
        fun `Missing receiver`() {
            val tx = EventMeshTransmitter(device)
            assertFails { EventMeshDevice.Builder().withTransmitter(tx).build() }
        }

        @Test
        fun `with optional`() {
            val tx = EventMeshTransmitter(device)
            val rx = EventMeshReceiver(device)
            val e = EventMeshDevice.Builder().withTransmitter(tx).withReceiver(rx)
            var eb: EventMeshDevice
            run {
                eb = e.withEchoCallback {}.build()
                val echo = getValueFromClass<EventMeshDevice, (() -> Unit)?>(eb, "echo")
                assertNotNull(echo)
            }
            run {
                eb = e.withReceiveDuration(Duration.ofSeconds(10)).build()
                val r = getValueFromClass<EventMeshDevice, EventMeshReceiver>(eb, "receiver")
                assertEquals(10_000, r.duration)
            }
            run {
                eb = e.withReceiveDuration(10).build()
                val r = getValueFromClass<EventMeshDevice, EventMeshReceiver>(eb, "receiver")
                assertEquals(10, r.duration)
            }
            run {
                eb = e.withTransmitTimeout(Duration.ofSeconds(10)).build()
                val t = getValueFromClass<EventMeshDevice, EventMeshTransmitter>(eb, "transmitter")
                assertEquals(10_000, t.transmitTimeout)
            }
            run {
                eb = e.withTransmitTimeout(10).build()
                val t = getValueFromClass<EventMeshDevice, EventMeshTransmitter>(eb, "transmitter")
                assertEquals(10, t.transmitTimeout)
            }
        }
    }
}
