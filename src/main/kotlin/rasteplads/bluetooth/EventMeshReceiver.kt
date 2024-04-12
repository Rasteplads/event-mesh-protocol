package rasteplads.bluetooth

class EventMeshReceiver(private val device: TransportDevice) {
    val handlers = mutableListOf<(ByteArray) -> Unit>()
}
