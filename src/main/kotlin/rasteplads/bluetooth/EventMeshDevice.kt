package rasteplads.bluetooth

import java.time.Duration
import kotlinx.coroutines.*

class EventMeshDevice(
    private val receiver: EventMeshReceiver,
    private val transmitter: EventMeshTransmitter,
    tTimeout: Duration? = null,
    rDuration: Duration? = null,
) {
    private val transmitTimeout: Duration
    private val receiveDuration: Duration

    init {
        receiver.handlers.add { message -> onMessageReceived(message) }
        transmitTimeout = tTimeout ?: Duration.ofSeconds(60)
        receiveDuration = rDuration ?: Duration.ofSeconds(600)
    }

    suspend fun startTransmitting(message: ByteArray) = runBlocking {
        // Begin transmitting.
        // If null, no echo
        withTimeoutOrNull(transmitTimeout.toMillis()) {
            // TODO: Transmit with a given interval, cancel on echo
            while (isActive) {
                transmitter.transmit(message)
                delay(100000000000000000) // TODO: interval
            }
        }
            ?: Unit
    }

    fun addReceivedMessageCallback(f: (ByteArray) -> Unit) = receiver.handlers.add(f)

    fun addReceivedMessageCallback(vararg f: (ByteArray) -> Unit) = receiver.handlers.addAll(f)

    private fun onMessageReceived(message: ByteArray) {}

    class Builder {
        private var receiver: EventMeshReceiver? = null
        private var transmitter: EventMeshTransmitter? = null
        private var tTimeout: Duration? = null
        private var rDuration: Duration? = null

        fun withReceiver(receiver: EventMeshReceiver): Builder {
            this.receiver = receiver
            return this
        }

        fun withTransmitter(transmitter: EventMeshTransmitter): Builder {
            this.transmitter = transmitter
            return this
        }

        fun withTransmitTimeout(d: Duration): Builder {
            tTimeout = d
            return this
        }

        fun withReceiveDuration(d: Duration): Builder {
            rDuration = d
            return this
        }

        fun build(): EventMeshDevice {
            check(transmitter != null) {
                "A transmitter must be specified when using the EventMeshDevice.Builder."
            }
            check(receiver != null) {
                "A receiver must be specified when using the EventMeshDevice.Builder."
            }
            // TODO: Construct tx and rx if none are provided.
            // val transmitter = this.transmitter ?: EventMeshTransmitter<T>()

            return EventMeshDevice(receiver!!, transmitter!!)
        }
    }
}
