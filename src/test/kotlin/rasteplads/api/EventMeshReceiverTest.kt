package rasteplads.api

import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking

class EventMeshReceiverTest {

    @BeforeTest
    @AfterTest
    fun clean() {
        device.stopReceiving()
        device.stopTransmitting()
        device.transmittedMessages.removeAll { true }
        device.receivedPool.get().removeAll { true }
    }

    @Test
    fun receivesCorrectMessages(): Unit = runBlocking {
        val b = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
        val rx = EventMeshReceiver(device)
        val l = mutableListOf<ByteArray>()
        rx.duration = RX_DURATION // 5 sec
        rx.setReceivedMessageCallback { l.add(it) }
        launch { rx.scanForMessages() }
        delay(100)
        assertEquals(0, l.size)
        assert(l.all { it.contentEquals(b) })
        device.receiveMessage(b)
        delay(300)
        assertEquals(1, l.size)
        assert(l.all { it.contentEquals(b) })
        delay(300)
        assertEquals(1, l.size)
        assert(l.all { it.contentEquals(b) })
        device.receiveMessage(b)
        device.receiveMessage(b)
        delay(300)
        assertEquals(3, l.size)
        assert(l.all { it.contentEquals(b) })
        delay(300)
        assertEquals(3, l.size)
        assert(l.all { it.contentEquals(b) })
    }

    @Test
    fun receivesCorrectMessagesWhileScanningForId(): Unit = runBlocking {
        val b = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
        val rx = EventMeshReceiver(device)
        val l = mutableListOf<ByteArray>()
        rx.duration = RX_DURATION // 5 sec
        rx.setReceivedMessageCallback { l.add(it) }
        assertFalse(device.receiving.get())
        launch { rx.scanForMessages() }
        delay(100)
        assert(device.receiving.get())
        assertEquals(0, l.size)
        assert(l.all { it.contentEquals(b) })
        device.receiveMessage(b)
        delay(300)
        assertEquals(1, l.size)
        assert(l.all { it.contentEquals(b) })
        delay(300)
        assertEquals(1, l.size)
        assert(l.all { it.contentEquals(b) })
        device.receiveMessage(b)
        device.receiveMessage(b)
        delay(300)
        assertEquals(3, l.size)
        assert(l.all { it.contentEquals(b) })
        var id = false
        launch { rx.scanForID(byteArrayOf(0, 1, 2, 4)) { id = true } }
        assertFalse(id)
        delay(100)
        device.receiveMessage(b)
        delay(100)
        assertEquals(4, l.size)
        assert(l.all { it.contentEquals(b) })
        assertFalse(id)
        delay(100)
        device.receiveMessage(byteArrayOf(0, 1, 2, 4, 5, 6, 7, 8, 9))
        delay(100)
        assertEquals(5, l.size)
        assert(l.count { it.contentEquals(b) } == l.size - 1)
        assert(id)
        delay(100)
        device.receiveMessage(b)
        delay(100)
        assertEquals(6, l.size)
        assert(l.count { it.contentEquals(b) } == l.size - 1)
    }

    @Test
    fun receivesCorrectMessagesWhileScanningForIdAndStopping(): Unit = runBlocking {
        val b = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
        val rx = EventMeshReceiver(device)
        val l = mutableListOf<ByteArray>()
        rx.duration = RX_DURATION // 5 sec
        rx.setReceivedMessageCallback { l.add(it) }
        assertFalse(device.receiving.get())
        launch { rx.scanForMessages() }
        delay(300)
        assert(device.receiving.get())
        assertEquals(0, l.size)
        assert(l.all { it.contentEquals(b) })
        device.receiveMessage(b)
        delay(300)
        assertEquals(1, l.size)
        assert(l.all { it.contentEquals(b) })
        delay(1300)
        assertEquals(1, l.size)
        assert(l.all { it.contentEquals(b) })
        device.receiveMessage(b)
        device.receiveMessage(b)
        delay(1000)
        assertEquals(3, l.size)
        assert(l.all { it.contentEquals(b) })
        var id = false
        val j = launch { rx.scanForID(byteArrayOf(0, 1, 2, 4)) { id = true } }
        assert(device.receiving.get())
        assertFalse(id)
        delay(100)
        device.receiveMessage(b)
        delay(100)
        assertEquals(4, l.size)
        assert(l.all { it.contentEquals(b) })
        assertFalse(id)
        // 5100 ms passed, initial launch should be done, while the other should continue
        delay(2000)
        assert(device.receiving.get())
        device.receiveMessage(b)
        delay(100)
        assertEquals(4, l.size)
        assert(l.all { it.contentEquals(b) })
        assertFalse(id)
        device.receiveMessage(byteArrayOf(0, 1, 2, 4, 5, 6, 7, 8, 9))
        delay(500)
        assertEquals(4, l.size)
        assert(l.all { it.contentEquals(b) })
        assertFalse(device.receiving.get())
        assert(id)
        delay(100)
        device.receiveMessage(b)
        delay(100)
        assertEquals(4, l.size)
        assert(l.all { it.contentEquals(b) })
        j.join()
    }

    @Test
    fun scanningForId(): Unit = runBlocking {
        val b = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
        val rx = EventMeshReceiver(device)
        var id = false
        rx.duration = RX_DURATION // 5 sec
        assertFalse(device.receiving.get())
        launch { rx.scanForID(byteArrayOf(0, 1, 2, 4)) { id = true } }
        delay(100)
        assert(device.receiving.get())
        assertFalse(id)
        device.receiveMessage(b)
        assert(device.receiving.get())
        assertFalse(id)
        delay(300)
        device.receiveMessage(b)
        assert(device.receiving.get())
        assertFalse(id)
        delay(300)
        device.receiveMessage(byteArrayOf(0, 1, 2, 4, 5, 6, 7, 8, 9, 9, 9, 9))
        delay(100)
        assertFalse(device.receiving.get())
        assert(id)
    }

    @Test
    fun scanningForIdThenReceivingMessagesAndStopping(): Unit = runBlocking {
        val b = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
        val rx = EventMeshReceiver(device)
        val l = mutableListOf<ByteArray>()
        rx.duration = RX_DURATION // 5 sec
        rx.setReceivedMessageCallback { l.add(it) }
        assertFalse(device.receiving.get())
        var id = false
        launch { rx.scanForID(byteArrayOf(0, 1, 2, 4)) { id = true } }
        delay(100)
        assert(device.receiving.get())
        assertFalse(id)
        device.receiveMessage(b)
        assert(device.receiving.get())
        assertFalse(id)
        delay(300)
        device.receiveMessage(b)
        assert(device.receiving.get())
        assertFalse(id)
        delay(200)
        launch { rx.scanForMessages() }
        delay(300)
        assert(device.receiving.get())
        assertEquals(0, l.size)
        assert(l.all { it.contentEquals(b) })
        device.receiveMessage(b)
        delay(300)
        assertEquals(1, l.size)
        assert(l.all { it.contentEquals(b) })
        device.receiveMessage(b)
        device.receiveMessage(byteArrayOf(0, 1, 2, 4, 5, 6, 7, 8, 98, 7, 6, 6))
        delay(1000)
        assertEquals(3, l.size)
        assertEquals(l.size - 1, l.count { it.contentEquals(b) })
        assert(device.receiving.get())
        assert(id)
        delay(100)
        device.receiveMessage(b)
        delay(100)
        assertEquals(4, l.size)
        assertEquals(l.size - 1, l.count { it.contentEquals(b) })
    }

    @Test
    fun startsAndStops() = runBlocking {
        assertFalse(device.receiving.get())
        val rx = EventMeshReceiver(device)
        rx.duration = 1_000
        assertFalse(device.receiving.get())
        launch { rx.scanForMessages() }
        delay(100)
        assert(device.receiving.get())
        delay(1_000)
        assertFalse(device.receiving.get())
    }

    @Test
    fun stopsInBetween() = runBlocking {
        val b = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
        val l = mutableListOf<ByteArray>()
        val rx = EventMeshReceiver(device)
        rx.duration = 1_000
        rx.setReceivedMessageCallback { l.add(it) }

        assertFalse(device.receiving.get())
        launch { rx.scanForMessages() }
        delay(100)
        assert(device.receiving.get())

        device.receiveMessage(b)
        delay(100)
        assertEquals(1, l.size)
        assert(l.all { it.contentEquals(b) })
        device.receiveMessage(b)
        delay(100)
        assertEquals(2, l.size)
        assert(l.all { it.contentEquals(b) })

        delay(1000)
        assertFalse(device.receiving.get())
        delay(100)
        launch { rx.scanForMessages() }
        delay(100)
        assert(device.receiving.get())

        device.receiveMessage(b)
        delay(100)
        assertEquals(3, l.size)
        assert(l.all { it.contentEquals(b) })
        device.receiveMessage(b)
        delay(100)
        assertEquals(4, l.size)
        assert(l.all { it.contentEquals(b) })
    }

    companion object {
        const val RX_DURATION: Long = 5_000
        val device = MockDevice(Channel(0), Channel(0), 0)
    }
}
