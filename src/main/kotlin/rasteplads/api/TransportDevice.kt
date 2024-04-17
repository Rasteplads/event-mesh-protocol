package rasteplads.bluetooth

interface TransportDevice {
    val transmissionInterval: Long

    fun beginTransmitting(message: ByteArray)

    fun stopTransmitting()

    fun beginReceiving(callback: suspend (ByteArray) -> Unit)

    fun stopReceiving()
}
