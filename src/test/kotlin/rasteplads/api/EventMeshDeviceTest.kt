package rasteplads.api

import java.time.Duration
import java.util.concurrent.atomic.*
import kotlin.math.*
import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.junit.jupiter.api.Test
import rasteplads.api.EventMesh.Companion.ID_MAX_SIZE
import rasteplads.util.byte_array_extension.generateRands

class MockDevice(
    override val receiveChannel: Channel<ByteArray>,
    override val transmitChannel: Channel<ByteArray>,
    override val transmissionInterval: Long
) : TransportDevice {

    val transmittedMessages: AtomicReference<MutableList<ByteArray>> =
        AtomicReference(mutableListOf())
    val receivedPool: AtomicReference<MutableList<ByteArray>> = AtomicReference(mutableListOf())

    val transmitting: AtomicBoolean = AtomicBoolean(false)
    val receiving: AtomicBoolean = AtomicBoolean(false)

    override fun beginTransmitting(message: ByteArray): Unit = runBlocking {
        transmittedMessages.get().removeAll { true }
        transmitting.set(true)

        GlobalScope.launch {
            try {
                while (transmitting.get()) {
                    transmittedMessages.get().add(message)
                    delay(transmissionInterval)
                }
            } catch (_: Exception) {}
        }
    }

    override fun stopTransmitting(): Unit = transmitting.set(false)

    override fun beginReceiving(callback: suspend (ByteArray) -> Unit) = runBlocking {
        receiving.set(true)

        while (receiving.get()) {
            val iter = receivedPool.get().iterator()
            for (i in iter) {
                callback(i)
                iter.remove()
            }
            delay(50) // 1 sec
            yield()
        }
    }

    override fun stopReceiving(): Unit = receiving.set(false)

    fun receiveMessage(b: ByteArray) {
        receivedPool.get().add(b)
    }
}

class EventMeshDeviceTest {

    companion object {
        const val T_INTERVAL: Long = 100
        val device = MockDevice(Channel(0), Channel(0), T_INTERVAL)
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

        launch { e.startReceiving() }
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
        val e = EventMeshDevice(rx, tx, txTimeout = Duration.ofMillis(1050))

        val ttl: Byte = 2
        launch { e.startTransmitting(ttl, byteArrayOf(0, 1, 2, 3), b) }
        delay(50)
        assert(device.transmitting.get())
        assert(device.receiving.get())
        delay(1100)
        assertFalse(device.transmitting.get())
        assertFalse(device.receiving.get())
        assertEquals(
            ceil(tx.transmitTimeout / T_INTERVAL.toDouble()).toInt(),
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

        launch { e.startTransmitting(2, byteArrayOf(0, 1, 2, 3), byteArrayOf()) }
        delay(50)
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

        launch { e.startTransmitting(2, byteArrayOf(0, 1, 2, 3), byteArrayOf()) }
        delay(50)
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

    // TODO: Test Builder
    class BuilderTest {}
}
