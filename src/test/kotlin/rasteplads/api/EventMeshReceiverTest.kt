package rasteplads.api

import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.jvm.isAccessible
import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.runBlocking
import rasteplads.api.EventMeshDeviceTest.Companion.getValueFromClass

class EventMeshReceiverTest {

    private val launchPool = mutableListOf<Job>()
    // @BeforeTest
    @AfterTest
    fun clean(): Unit = runBlocking {
        device.stopReceiving()
        device.stopTransmitting()
        device.transmittedMessages.get().removeAll { true }
        device.receivedPool.get().removeAll { true }
        launchPool.forEach { it.cancelAndJoin() }
        launchPool.removeAll { true }
    }

    @Test
    fun `receives correct messages`(): Unit = runBlocking {
        val b = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
        val rx = EventMeshReceiver(device)
        val l = mutableListOf<ByteArray>()
        rx.duration = RX_DURATION // 5 sec
        rx.setReceivedMessageCallback { l.add(it) }
        launchPool.add(GlobalScope.launch { rx.scanForMessages() })
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
    fun `receives correct messages while scanning for ID`(): Unit = runBlocking {
        val b = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
        val rx = EventMeshReceiver(device)
        val l = mutableListOf<ByteArray>()
        rx.duration = RX_DURATION // 5 sec
        rx.setReceivedMessageCallback { l.add(it) }
        assertFalse(device.receiving.get())
        launchPool.add(GlobalScope.launch { rx.scanForMessages() })
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
        launchPool.add(
            GlobalScope.launch { rx.scanForID(byteArrayOf(0, 1, 2, 4), RX_DURATION) { id = true } })
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
        assertEquals(4, l.size)
        assert(l.all { it.contentEquals(b) })
        assert(id)
        delay(100)
        device.receiveMessage(b)
        delay(100)
        assertEquals(5, l.size)
        assert(l.all { it.contentEquals(b) })
    }

    @Test
    fun `receives correct messages while scanning for ID and stopping`(): Unit = runBlocking {
        val b = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
        val rx = EventMeshReceiver(device)
        val l = mutableListOf<ByteArray>()
        rx.duration = RX_DURATION // 5 sec
        rx.setReceivedMessageCallback { l.add(it) }
        assertFalse(device.receiving.get())
        launchPool.add(GlobalScope.launch { rx.scanForMessages() })
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
        launchPool.add(
            GlobalScope.launch { rx.scanForID(byteArrayOf(0, 1, 2, 4), RX_DURATION) { id = true } })
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
    }

    @Test
    fun `scanning for ID`(): Unit = runBlocking {
        val b = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
        val rx = EventMeshReceiver(device)
        var id = false
        rx.duration = RX_DURATION // 5 sec
        assertFalse(device.receiving.get())
        launchPool.add(
            GlobalScope.launch { rx.scanForID(byteArrayOf(0, 1, 2, 4), RX_DURATION) { id = true } })
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
        delay(300)
        assertFalse(device.receiving.get())
        assert(id)
    }

    @Test
    fun `scanning for ID then receiving messages and stopping`(): Unit = runBlocking {
        val b = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
        val rx = EventMeshReceiver(device)
        val l = mutableListOf<ByteArray>()
        rx.duration = RX_DURATION // 5 sec
        rx.setReceivedMessageCallback { l.add(it) }
        assertFalse(device.receiving.get())
        var id = false
        launchPool.add(
            GlobalScope.launch { rx.scanForID(byteArrayOf(0, 1, 2, 4), RX_DURATION) { id = true } })
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
        launchPool.add(GlobalScope.launch { rx.scanForMessages() })
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
        assertEquals(2, l.size)
        assert(l.all { it.contentEquals(b) })
        assert(device.receiving.get())
        assert(id)
        delay(100)
        device.receiveMessage(byteArrayOf(0, 1, 2, 4, 5, 6, 7, 8))
        delay(100)
        assertEquals(3, l.size)
        assertEquals(l.size - 1, l.count { it.contentEquals(b) })
    }

    @Test
    fun `starts and stops`() = runBlocking {
        assertFalse(device.receiving.get())
        val rx = EventMeshReceiver(device)
        rx.duration = 1_000
        assertFalse(device.receiving.get())
        launchPool.add(GlobalScope.launch { rx.scanForMessages() })
        delay(100)
        assert(device.receiving.get())
        delay(1_100)
        assertFalse(device.receiving.get())
    }

    @Test
    fun `stops in between`() = runBlocking {
        val b = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
        val l = mutableListOf<ByteArray>()
        val rx = EventMeshReceiver(device)
        rx.duration = 1_000
        rx.setReceivedMessageCallback { l.add(it) }

        assertFalse(device.receiving.get())
        launchPool.add(GlobalScope.launch(Dispatchers.IO) { rx.scanForMessages() })
        delay(200)
        assert(device.receiving.get())

        device.receiveMessage(b)
        delay(200)
        assertEquals(1, l.size)
        assert(l.all { it.contentEquals(b) })
        device.receiveMessage(b)
        delay(200)
        assertEquals(2, l.size)
        assert(l.all { it.contentEquals(b) })

        delay(1500)
        assertFalse(device.receiving.get()) // Mayhaps this
        delay(200)
        launchPool.add(GlobalScope.launch(Dispatchers.IO) { rx.scanForMessages() })
        delay(500)
        assert(device.receiving.get())
        /*
        device.receiveMessage(b)
        delay(200)
        assertEquals(3, l.size)
        assert(l.all { it.contentEquals(b) })
        device.receiveMessage(b)
        delay(200)
        assertEquals(4, l.size)
        assert(l.all { it.contentEquals(b) })
         */
    }

    @Test
    fun `throws exception`(): Unit = runBlocking {
        assertFails {
            EventMeshReceiver(device).scanForID(
                byteArrayOf(
                    0,
                    1,
                    2,
                    4,
                    5,
                    6,
                ),
                1_000) {}
        }
    }

    // TODO: Test starting and stopping (and counter)

    @Test
    fun `multiple starters`() {
        val rx = EventMeshReceiver(EventMeshDeviceTest.device)
        val count = getValueFromClass<EventMeshReceiver, AtomicInteger>(rx, "scannerCount")
        val e = 10

        assertEquals(0, count.get())

        for (i in 1..e) callFuncFromClass<EventMeshReceiver>(rx, "startDevice")
        assertEquals(e, count.get())

        for (i in 1..e / 2) callFuncFromClass<EventMeshReceiver>(rx, "stopDevice")
        assertEquals(e / 2, count.get())

        for (i in 1..e / 2) callFuncFromClass<EventMeshReceiver>(rx, "stopDevice")

        assertEquals(0, count.get())
    }

    @Test
    fun `scanners can't go below zero`() {
        val rx = EventMeshReceiver(EventMeshDeviceTest.device)
        val count = getValueFromClass<EventMeshReceiver, AtomicInteger>(rx, "scannerCount")
        val e = 10

        assertEquals(0, count.get())

        for (i in 1..e) callFuncFromClass<EventMeshReceiver>(rx, "stopDevice")
        assertEquals(0, count.get())
    }

    companion object {
        const val RX_DURATION: Long = 5_000
        val device = MockDevice(100)

        inline fun <reified C> callFuncFromClass(target: C, field: String) {
            C::class
                .members
                .find { m -> m.name == field }!!
                .apply { isAccessible = true }
                .call(target)
        }
    }
}
