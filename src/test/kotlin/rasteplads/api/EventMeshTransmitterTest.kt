package rasteplads.api

// import org.junit.jupiter.api.Test
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
        // device.stopReceiving()
        // device.stopTransmitting()
        // device.transmittedMessages.get().removeAll { true }
        // device.receivedMsg.set(null)
        launchPool.forEach { it.cancelAndJoin() }
        launchPool.removeAll { true }
    }

    @Test
    fun `transmitting correct through EventMeshTransmitter`() = runBlocking {
        val device = EventMeshReceiverTest.newDevice()
        val tx = EventMeshTransmitter(device)
        val b = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        tx.transmitTimeout = 250
        launchPool.add(GlobalScope.launch { tx.transmit(b) })
        delay(100)
        assert(device.transmitting.get())
        // Sleep here to ensure that additional messages are not sent, and it stops transmitting
        delay(1000)
        assertFalse(device.transmitting.get())
        assertEquals(
            (tx.transmitTimeout / TX_INTERVAL),
            device.transmittedMessages.get().size.toLong()
        )
        assert(device.transmittedMessages.get().all { it.contentEquals(b) })

        device.transmittedMessages.get().removeAll { true }
        tx.transmitTimeout = 1000
        launchPool.add(GlobalScope.launch { tx.transmit(b) })
        delay(150)
        assert(device.transmitting.get())
        // Sleep here to ensure that additional messages are not sent, and it stops transmitting
        delay(1200)
        assertFalse(device.transmitting.get())

        assertEquals(
            (tx.transmitTimeout / TX_INTERVAL),
            device.transmittedMessages.get().size.toLong()
        )
        assert(device.transmittedMessages.get().all { it.contentEquals(b) })
    }

    companion object {
        const val TX_INTERVAL: Long = 50
        fun newDevice() = MockDevice(TX_INTERVAL)
    }
}
