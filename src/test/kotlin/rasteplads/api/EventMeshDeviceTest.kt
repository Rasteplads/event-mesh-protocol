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

class MockDevice(override val transmissionInterval: Long) : TransportDevice<Int, Int> {
    val transmittedMessages: AtomicReference<MutableList<ByteArray>> =
        AtomicReference(mutableListOf())
    private val receivedMsg: AtomicReference<ByteArray?> = AtomicReference(null)

    val transmitting: AtomicBoolean = AtomicBoolean(false)
    val receiving: AtomicBoolean = AtomicBoolean(false)

    private var tx: Job? = null
    private var rx: Job? = null
    private val txPool: HashMap<Int, Job> = hashMapOf()
    private var txC = 0
    private val rxPool: HashMap<Int, Job> = hashMapOf()
    private var rxC = 0

    override fun beginTransmitting(message: ByteArray): Int {
        transmitting.set(true)

        txPool[txC++] =
            GlobalScope.launch {
                try {
                    while (transmitting.get()) {
                        yield()
                        transmittedMessages.get().add(message.clone())
                        yield()
                        delay(transmissionInterval)
                        yield()
                    }
                } catch (_: Exception) {}
            }

        return txC - 1
    }

    override fun stopTransmitting(callback: Int) {
        txPool[callback]?.cancel()
        txPool.remove(callback)
        transmitting.set(txPool.isNotEmpty())
    }

    override fun beginReceiving(callback: suspend (ByteArray) -> Unit): Int {
        receiving.set(true)

        rxPool[rxC++] =
            GlobalScope.launch {
                while (isActive) {
                    receivedMsg.getAndSet(null)?.let { callback(it) }
                    yield()
                    delay(50) // 1 sec
                    yield()
                }
            }

        return rxC - 1
    }

    override fun stopReceiving(callback: Int): Unit {
        rxPool[callback]?.cancel()
        rxPool.remove(callback)
        receiving.set(rxPool.isNotEmpty())
    }

    fun receiveMessage(b: ByteArray) {
        if (!receiving.get()) return
        receivedMsg.set(b)
        while (receiving.get() && receivedMsg.get() != null) {
            Thread.sleep(50)
            // yield()
        }

        // receivedPool.get().add(b)
    }
}

class EventMeshDeviceTest {
    private val launchPool = mutableListOf<Job>()

    @BeforeTest
    @AfterTest
    fun clean() {
        // device.stopReceiving()
        // device.stopTransmitting()
        // device.transmittedMessages.get().removeAll { true }
        // device.receivedMsg.set(null)
        launchPool.forEach { it.cancel() }
        launchPool.removeAll { true }
    }

    companion object {
        const val T_INTERVAL: Long = 100
        //   val device = MockDevice(T_INTERVAL)
        fun newDevice() = MockDevice(T_INTERVAL)
        fun delay(ms: Long) = Thread.sleep(ms)

        inline fun <reified C, reified R> getValueFromClass(target: C, field: String): R =
            C::class
                .members
                .find { m -> m.name == field }!!
                .apply { isAccessible = true }
                .call(target) as R
    }

    @Test
    fun `throws with small id`() {
        val device = newDevice()
        val e =
            EventMeshDevice(
                EventMeshReceiver(device),
                EventMeshTransmitter(device),
                Duration.ofMillis(100)
            )

        for (i in ID_MAX_SIZE + 1..Short.MAX_VALUE) { // Arbitrary big value
            assertFails { e.startTransmitting(0, generateRands(i).toByteArray(), byteArrayOf()) }
        }
    }

    @Test
    fun `receives correctly`() {
        val device = newDevice()
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
        device.receiveMessage(b)
        device.receiveMessage(b)
        delay(1000)
        assertFalse(device.receiving.get())

        assertEquals(3, l.size)
        assert(l.all { it.contentEquals(b) })
    }

    @Test
    fun `transmits correctly`() {
        val device = newDevice()
        val b = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        val rx = EventMeshReceiver(device)
        val tx = EventMeshTransmitter(device)
        val l = mutableListOf<ByteArray>()
        rx.setReceivedMessageCallback(l::add)
        val e = EventMeshDevice(rx, tx, txTimeout = Duration.ofMillis(1030))

        val ttl: Byte = 2
        launchPool.add(GlobalScope.launch { e.startTransmitting(ttl, byteArrayOf(0, 1, 2, 3), b) })
        delay(200)
        assert(device.transmitting.get())
        assert(device.receiving.get())
        delay(1200)
        assertFalse(device.transmitting.get())
        assertFalse(device.receiving.get())
        // 1000 / 100 = 10 (+1 cuz it does it on time 0)
        assertEquals(
            (tx.transmitTimeout.floorDiv(T_INTERVAL) + 1).toInt(),
            device.transmittedMessages.get().size
        )

        val combined = ttl + byteArrayOf(0, 1, 2, 3) + b
        assert(device.transmittedMessages.get().all { it.contentEquals(combined) })
    }

    @Test
    fun `calls echo correctly`() {
        val device = newDevice()
        val rx = EventMeshReceiver(device)
        var echo = false
        val tx = EventMeshTransmitter(device)
        val e = EventMeshDevice(rx, tx, txTimeout = Duration.ofMillis(1000), echo = { echo = true })

        launchPool.add(
            GlobalScope.launch { e.startTransmitting(2, byteArrayOf(0, 1, 2, 3), byteArrayOf()) }
        )
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
    fun `does not call echo`() {
        val device = newDevice()
        val rx = EventMeshReceiver(device)
        var echo = false
        val tx = EventMeshTransmitter(device)
        val e = EventMeshDevice(rx, tx, txTimeout = Duration.ofMillis(1001), echo = { echo = true })

        launchPool.add(
            GlobalScope.launch { e.startTransmitting(2, byteArrayOf(0, 1, 2, 3), byteArrayOf()) }
        )
        delay(100)
        assertFalse(echo)
        assert(device.transmitting.get())
        assert(device.receiving.get())
        delay(500)
        assertFalse(echo)
        device.receiveMessage(byteArrayOf(1, 0, 1, 2, 3, 4, 5, 6, 7, 8))
        delay(1000)
        assertFalse(echo)
        assertFalse(device.transmitting.get())
        assertFalse(device.receiving.get())
    }

    @Nested
    inner class BuilderTest {
        @Test
        fun `Missing transmitter`() {
            val device = newDevice()
            val rx = EventMeshReceiver(device)
            assertFails { EventMeshDevice.Builder<Int, Int>().withReceiver(rx).build() }
        }

        @Test
        fun `Missing receiver`() {
            val device = newDevice()
            val tx = EventMeshTransmitter(device)
            assertFails { EventMeshDevice.Builder<Int, Int>().withTransmitter(tx).build() }
            assertFails {
                EventMeshDevice.Builder<Int, Int>().withTransmitter(tx).withReceiveMsgCallback {}
            }
        }

        @Test
        fun `with optional`() {
            val device = newDevice()
            val tx = EventMeshTransmitter(device)
            val rx = EventMeshReceiver(device)
            val e = EventMeshDevice.Builder<Int, Int>().withTransmitter(tx).withReceiver(rx)
            var eb: EventMeshDevice<Int, Int>
            run {
                eb = e.withEchoCallback {}.build()
                val echo = getValueFromClass<EventMeshDevice<Int, Int>, (() -> Unit)?>(eb, "echo")
                assertNotNull(echo)
            }
            run {
                eb = e.withReceiveDuration(Duration.ofSeconds(10)).build()
                val r =
                    getValueFromClass<EventMeshDevice<Int, Int>, EventMeshReceiver<Int>>(
                        eb,
                        "receiver"
                    )
                assertEquals(10_000, r.duration)
            }
            run {
                eb = e.withReceiveDuration(10).build()
                val r =
                    getValueFromClass<EventMeshDevice<Int, Int>, EventMeshReceiver<Int>>(
                        eb,
                        "receiver"
                    )
                assertEquals(10, r.duration)
            }
            run {
                eb = e.withTransmitTimeout(Duration.ofSeconds(10)).build()
                val t =
                    getValueFromClass<EventMeshDevice<Int, Int>, EventMeshTransmitter<Int>>(
                        eb,
                        "transmitter"
                    )
                assertEquals(10_000, t.transmitTimeout)
            }
            run {
                eb = e.withTransmitTimeout(10).build()
                val t =
                    getValueFromClass<EventMeshDevice<Int, Int>, EventMeshTransmitter<Int>>(
                        eb,
                        "transmitter"
                    )
                assertEquals(10, t.transmitTimeout)
            }
        }
    }
}
