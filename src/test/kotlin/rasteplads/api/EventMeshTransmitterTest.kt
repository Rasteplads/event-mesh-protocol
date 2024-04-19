package rasteplads.api

// import org.junit.jupiter.api.Test
import kotlin.math.ceil
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.*

class EventMeshTransmitterTest {

    private val launchPool = mutableListOf<Job>()
    // @BeforeTest
    @AfterTest
    fun clean(): Unit = runBlocking {
        EventMeshDeviceTest.device.stopReceiving()
        EventMeshDeviceTest.device.stopTransmitting()
        EventMeshDeviceTest.device.transmittedMessages.get().removeAll { true }
        EventMeshDeviceTest.device.receivedPool.get().removeAll { true }
        launchPool.forEach { it.cancelAndJoin() }
        launchPool.removeAll { true }
    }

    @Test
    fun `transmitting correct through EventMeshTransmitter`() = runBlocking {
        val tx = EventMeshTransmitter(device)
        val b = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        tx.transmitTimeout = 140
        launchPool.add(GlobalScope.launch { tx.transmit(b) })
        delay(50)
        assert(device.transmitting.get())
        // Sleep here to ensure that additional messages are not sent, and it stops transmitting
        delay(1000)
        assertFalse(device.transmitting.get())

        assertEquals(
            ceil((tx.transmitTimeout / TX_INTERVAL.toDouble())).toInt(),
            device.transmittedMessages.get().size)
        assert(device.transmittedMessages.get().all { it.contentEquals(b) })

        tx.transmitTimeout = 1000
        launchPool.add(GlobalScope.launch { tx.transmit(b) })
        delay(50)
        assert(device.transmitting.get())
        // Sleep here to ensure that additional messages are not sent, and it stops transmitting
        delay(1000)
        assertFalse(device.transmitting.get())

        assertEquals(
            ceil((tx.transmitTimeout / TX_INTERVAL.toDouble())).toInt(),
            device.transmittedMessages.get().size)
        assert(device.transmittedMessages.get().all { it.contentEquals(b) })
    }

    companion object {
        const val TX_INTERVAL: Long = 50
        val device = MockDevice(TX_INTERVAL)
    }
}
