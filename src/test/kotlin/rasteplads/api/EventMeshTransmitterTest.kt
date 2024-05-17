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
        launchPool.forEach { it.cancelAndJoin() }
        launchPool.removeAll { true }
    }

    @Test
    fun `transmitting correct through EventMeshTransmitter`() {
        val device = newDevice()
        val tx = EventMeshTransmitter(device)
        val b = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        var t: Long = 250
        launchPool.add(GlobalScope.launch { tx.transmit(b, t) })
        delay(100)
        assert(device.transmitting.get())
        // Sleep here to ensure that additional messages are not sent, and it stops transmitting
        delay(1000)
        assertFalse(device.transmitting.get())
        assertEquals((t / TX_INTERVAL), device.transmittedMessages.size.toLong())
        assert(device.transmittedMessages.all { it.contentEquals(b) })

        device.transmittedMessages.removeAll { true }
        t = 1000
        launchPool.add(GlobalScope.launch { tx.transmit(b, t) })
        delay(150)
        assert(device.transmitting.get())
        // Sleep here to ensure that additional messages are not sent, and it stops transmitting
        delay(1200)
        assertFalse(device.transmitting.get())

        assertEquals((t / TX_INTERVAL), device.transmittedMessages.size.toLong())
        assert(device.transmittedMessages.all { it.contentEquals(b) })
    }

    companion object {
        const val TX_INTERVAL: Long = 50
        fun newDevice() = MockDevice(TX_INTERVAL)
        fun delay(ms: Long) = Thread.sleep(ms)
    }
}
