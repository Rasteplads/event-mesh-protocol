package rasteplads.api

interface TransportDevice<TRx,TTx> {
    val transmissionInterval: Long

    fun beginTransmitting(message: ByteArray): TTx

    fun stopTransmitting(callback: TTx)

    fun beginReceiving(callback: suspend (ByteArray) -> Unit): TRx

    fun stopReceiving(callback: TRx)
}
