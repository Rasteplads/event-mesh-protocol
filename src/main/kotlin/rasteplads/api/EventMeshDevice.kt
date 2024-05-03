package rasteplads.api

import java.time.Duration
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.*
import rasteplads.api.EventMesh.Companion.ID_MAX_SIZE
import rasteplads.util.plus

class EventMeshDevice<Rx, Tx>(
    private val receiver: EventMeshReceiver<Rx>,
    private val transmitter: EventMeshTransmitter<Tx>,
    txTimeout: Duration? = null,
    rxDuration: Duration? = null,
    private val echo: (() -> Unit)? = null
) {
    private val coroutineScope = CoroutineScope(CoroutineName("EventMeshDeviceScope"))

    init {
        txTimeout?.let { transmitter.transmitTimeout = it.toMillis() }
        rxDuration?.let { receiver.duration = it.toMillis() }
    }

    fun startTransmitting(ttl: Byte, id: ByteArray, message: ByteArray) {
        check(id.size <= ID_MAX_SIZE) { "ID too big" }
        val combinedMsg = ttl + id + message
        val tx = coroutineScope.launch { transmitter.transmit(combinedMsg) }

        try {
            receiver.scanForID(id, transmitter.transmitTimeout) { tx.cancel() }
        } catch (e: TimeoutException) {
            tx.cancel()
            echo?.invoke()
        }
    }

    fun startReceiving() = receiver.scanForMessages()

    class Builder<Rx, Tx>() {
        private lateinit var receiver: EventMeshReceiver<Rx>
        private lateinit var transmitter: EventMeshTransmitter<Tx>
        private var txTimeout: Duration? = null
        private var rxDuration: Duration? = null
        private var echo: (() -> Unit)? = null

        constructor(device: TransportDevice<Rx, Tx>) : this() {
            this.withDevice(device)
        }

        constructor(rx: EventMeshReceiver<Rx>, tx: EventMeshTransmitter<Tx>) : this() {
            receiver = rx
            transmitter = tx
        }

        fun withReceiver(receiver: EventMeshReceiver<Rx>): Builder<Rx, Tx> {
            this.receiver = receiver
            return this
        }

        fun withTransmitter(transmitter: EventMeshTransmitter<Tx>): Builder<Rx, Tx> {
            this.transmitter = transmitter
            return this
        }

        fun withTransmitTimeout(d: Duration): Builder<Rx, Tx> {
            txTimeout = d
            return this
        }

        fun withTransmitTimeout(milliseconds: Long): Builder<Rx, Tx> {
            txTimeout = Duration.ofMillis(milliseconds)
            return this
        }

        fun withReceiveDuration(d: Duration): Builder<Rx, Tx> {
            rxDuration = d
            return this
        }

        fun withReceiveDuration(milliseconds: Long): Builder<Rx, Tx> {
            rxDuration = Duration.ofMillis(milliseconds)
            return this
        }

        fun withEchoCallback(e: (() -> Unit)?): Builder<Rx, Tx> {
            echo = e
            return this
        }

        fun withReceiveMsgCallback(f: suspend (ByteArray) -> Unit): Builder<Rx, Tx> {
            check(::receiver.isInitialized) {
                "A receiver must be specified when using the EventMeshDevice.Builder."
            }
            receiver.setReceivedMessageCallback(f)
            return this
        }

        fun build(): EventMeshDevice<Rx, Tx> {
            check(::transmitter.isInitialized) {
                "A transmitter must be specified when using the EventMeshDevice.Builder."
            }
            check(::receiver.isInitialized) {
                "A receiver must be specified when using the EventMeshDevice.Builder."
            }
            // val transmitter = this.transmitter ?: EventMeshTransmitter<T>()

            return EventMeshDevice(
                receiver,
                transmitter,
                rxDuration = rxDuration,
                txTimeout = txTimeout,
                echo = echo
            )
        }

        fun withDevice(device: TransportDevice<Rx, Tx>): Builder<Rx, Tx> {
            receiver = EventMeshReceiver(device)
            transmitter = EventMeshTransmitter(device)
            return this
        }
    }
}
