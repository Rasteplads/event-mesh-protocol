package rasteplads.api

import kotlin.math.ceil
import kotlin.test.assertEquals
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.junit.jupiter.api.Test

class EventMeshTransmitterTest {

    /*
    @Test
    fun transmittingCorrectNumber() {
        val b = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        var sleep: Long = 120
        device.beginTransmitting(b)
        Thread.sleep(sleep)
        device.stopTransmitting()
        assertEquals(ceil((sleep / TX_INTERVAL.toDouble())).toInt(), device.transmittedMessages.size)

        device.beginTransmitting(b)
        Thread.sleep(sleep)
        device.stopTransmitting()
        assertEquals(ceil((sleep / TX_INTERVAL.toDouble())).toInt(), device.transmittedMessages.size)

        sleep = 1020
        device.beginTransmitting(b)
        Thread.sleep(sleep)
        device.stopTransmitting()
        assertEquals(ceil((sleep / TX_INTERVAL.toDouble())).toInt(), device.transmittedMessages.size)
    }
     */

    @Test
    fun transmittingCorrectThroughEventMeshTransmitter() {
        val tx = EventMeshTransmitter(device)
        val b = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        tx.transmitTimeout = 140
        tx.transmit(b)
        Thread.sleep(
            1000) // Sleep here to ensure that additional messages are not sent, and it stopos
        // transmitting

        assertEquals(
            ceil((tx.transmitTimeout / TX_INTERVAL.toDouble())).toInt(),
            device.transmittedMessages.size)
        assert(device.transmittedMessages.all { it.contentEquals(b) })

        tx.transmitTimeout = 1000
        tx.transmit(b)
        Thread.sleep(
            1000) // Sleep here to ensure that additional messages are not sent, and it stopos
        // transmitting

        assertEquals(
            ceil((tx.transmitTimeout / TX_INTERVAL.toDouble())).toInt(),
            device.transmittedMessages.size)
        assert(device.transmittedMessages.all { it.contentEquals(b) })
    }

    companion object {
        const val TX_INTERVAL: Long = 50
        val device = MockDevice(Channel(0), Channel(0), TX_INTERVAL)
    }
}
