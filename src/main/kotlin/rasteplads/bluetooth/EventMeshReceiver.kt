package rasteplads.bluetooth


class EventMeshReceiver<T>(
    private val device: TransportDevice<T>
) {
    val handlers = mutableListOf<(Message<T>) -> Unit>()

}