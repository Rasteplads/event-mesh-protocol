package rasteplads.bluetooth

import kotlinx.coroutines.channels.Channel


interface TransportDevice<T>{
    var messageBuffer: MessageBuffer<Message<T>>
    val receiveChannel: Channel<Message<T>>
    val transmitChannel: Channel<Message<T>>
    fun <T> beginTransmitting(message: Message<T>)
    fun <T> stopTransmitting(message: Message<T>)
    fun beginReceiving()
    fun stopReceiving()
}