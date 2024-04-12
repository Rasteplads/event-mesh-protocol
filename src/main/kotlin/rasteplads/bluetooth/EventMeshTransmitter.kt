package rasteplads.bluetooth

class EventMeshTransmitter(private val device: TransportDevice) {
    /**
     * Adds the message to the outgoing message queue. The message will be transmitted on the next
     * possible opportunity.
     */
    fun transmit(message: ByteArray, callback: (ByteArray) -> Unit) {
        TODO()
    }

    fun transmit(message: ByteArray) {
        device.beginTransmitting(message)
    }
}
