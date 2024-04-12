package rasteplads.bluetooth

class EventMeshTransmitter<T>(private val device: TransportDevice<T>){
    /**
     * Adds the message to the outgoing message queue. The message will be transmitted on the next
     * possible opportunity.
     */
    fun transmit(message: Message<T>, callback: (Message<T>) -> Unit){ TODO() }
    fun transmit(message: Message<T>){ device.beginTransmitting(message) }
}