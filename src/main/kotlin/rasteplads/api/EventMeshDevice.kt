package rasteplads.api

import java.time.Duration
import kotlinx.coroutines.*
import rasteplads.api.EventMesh.Companion.ID_MAX_SIZE
import rasteplads.util.plus

class EventMeshDevice(
    private val receiver: EventMeshReceiver,
    private val transmitter: EventMeshTransmitter,
    txTimeout: Duration? = null,
    rxDuration: Duration? = null,
    private val echo: (() -> Unit)? = null
) {

    init {
        txTimeout?.let { transmitter.transmitTimeout = it.toMillis() }
        rxDuration?.let { receiver.duration = it.toMillis() }
    }

    fun startTransmitting(ttl: Byte, id: ByteArray, message: ByteArray) = runBlocking {
        check(id.size <= ID_MAX_SIZE) { "ID too big" }
        val combinedMsg = ttl + id + message
        val tx = launch { transmitter.transmit(combinedMsg) }

        try {
            receiver.scanForID(id, transmitter.transmitTimeout) { tx.cancel() }
        } catch (e: TimeoutCancellationException) {
            echo?.invoke()
        }
    }

    fun startReceiving() = receiver.scanForMessages()

    class Builder {
        private lateinit var receiver: EventMeshReceiver
        private lateinit var transmitter: EventMeshTransmitter
        private var txTimeout: Duration? = null
        private var rxDuration: Duration? = null
        private var echo: (() -> Unit)? = null

        fun withReceiver(receiver: EventMeshReceiver): Builder {
            this.receiver = receiver
            return this
        }

        fun withTransmitter(transmitter: EventMeshTransmitter): Builder {
            this.transmitter = transmitter
            return this
        }

        fun withTransmitTimeout(d: Duration): Builder {
            txTimeout = d
            return this
        }

        fun withTransmitTimeout(milliseconds: Long): Builder {
            txTimeout = Duration.ofMillis(milliseconds)
            return this
        }

        fun withReceiveDuration(d: Duration): Builder {
            rxDuration = d
            return this
        }

        fun withReceiveDuration(milliseconds: Long): Builder {
            rxDuration = Duration.ofMillis(milliseconds)
            return this
        }

        fun withEchoCallback(e: (() -> Unit)?): Builder {
            echo = e
            return this
        }

        fun withReceiveMsgCallback(f: suspend (ByteArray) -> Unit): Builder {
            check(::receiver.isInitialized) {
                "A receiver must be specified when using the EventMeshDevice.Builder."
            }
            receiver.setReceivedMessageCallback(f)
            return this
        }

        fun build(): EventMeshDevice {
            check(::transmitter.isInitialized) {
                "A transmitter must be specified when using the EventMeshDevice.Builder."
            }
            check(::receiver.isInitialized) {
                "A receiver must be specified when using the EventMeshDevice.Builder."
            }
            // TODO: Construct tx and rx if none are provided.
            // val transmitter = this.transmitter ?: EventMeshTransmitter<T>()

            return EventMeshDevice(
                receiver, transmitter, rxDuration = rxDuration, txTimeout = txTimeout, echo = echo)
        }

        fun withDevice(device: TransportDevice): Builder {
            receiver = EventMeshReceiver(device)
            transmitter = EventMeshTransmitter(device)
            return this
        }
    }
}
