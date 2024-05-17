package rasteplads.api

import java.time.Duration
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import rasteplads.messageCache.MessageCache
import rasteplads.util.Either
import rasteplads.util.split

const val MSG_Q_TAG = "Message-Queue"

/**
 * This class handles communicating messages to the dynamic mesh network.
 *
 * `ID` represents the IDs for each message, `Data` represents the message itself, and `MC` is the
 * message cache for storing already known messages.
 *
 * If the default message cache is used, `ID` must af [equals] overridden.
 *
 * To use, use the one of the [builder] functions, that returns a [Builder], which needs some values
 * to work (more or less all methods following the 'set*' naming scheme). After this, you can simply
 * run [start], that runs everything in the background. When you want to stop, simply run [stop].
 *
 * @author t-lohse
 */
@OptIn(ExperimentalStdlibApi::class)
final class EventMesh<ID, Data>
private constructor(
    deviceBuilder: EventMeshDevice.Builder<*, *>,
    private val messageCache: MessageCache<ID>?,
    callback: (ID, Data) -> Unit,
    decodeID: (ByteArray) -> ID,
    decodeData: (ByteArray) -> Data,
    private val encodeID: (ID) -> ByteArray,
    private val encodeData: (Data) -> ByteArray,
    msgData: Either<Data, () -> Data>,
    msgId: Either<ID, () -> ID>,
    filterID: List<(ID) -> Boolean>,
    dataSize: Int,
    private val logger: (String, String) -> Unit
) {
    private val device: EventMeshDevice<*, *>
    private val msgData: () -> Data =
        if (msgData.isLeft()) {
            { msgData.getLeft()!! }
        } else msgData.getRight()!!
    private val msgId: () -> ID =
        if (msgId.isLeft()) {
            { msgId.getLeft()!! }
        } else msgId.getRight()!!

    private val relayQueue: LinkedBlockingQueue<Triple<Byte, ByteArray, ByteArray>> =
        LinkedBlockingQueue()
    init {
        fun scanningCallback(msg: ByteArray) {
            require(msg.size >= 1 + ID_MAX_SIZE + dataSize) {
                "Message does not conform with the minimum requirement (TTL + ID + provided size)"
            }

            val (idB, dataB) = msg.sliceArray(1 until msg.size).split(ID_MAX_SIZE)
            val id = decodeID(idB)

            if (messageCache == null || !messageCache.containsMessage(id)) {
                messageCache?.cacheMessage(id)
                if (filterID.all { f -> f(id) })
                    callback(id, decodeData(dataB.take(dataSize).toByteArray()))

                if (msg[0] > Byte.MIN_VALUE) {
                    relayQueue.add(Triple(msg[0].dec(), idB, dataB))
                    logger(
                        MSG_Q_TAG,
                        "Added message ${idB.toHexString()} to Relay Queue. Size: ${relayQueue.size}"
                    )
                    // relay(msg[0].dec(), idB, dataB)
                }
            }
        }
        device = deviceBuilder.withReceiveMsgCallback(::scanningCallback).build()
    }

    // TODO: set correct default values
    /** Time a message is stored in the cache */
    // private var msgDelete: Duration = Duration.ofSeconds(30)

    /** Time TO Live (TTL) for the messages. */
    private var msgTTL: Byte = 10

    /**
     * The interval the messages from [msgData] will be sent. This corresponds to 'Advertising
     * Interval' in Bluetooth
     */
    // private var msgSendInterval: Duration = Duration.ofMillis(100)

    /** The interval the between a message session. */
    private var msgSendInterval: Duration = Duration.ofSeconds(10)

    /**
     * The duration the messages from [msgData] will be sent. A message session will only be this
     * long, or until an echo. If `Duration.ZERO`, then there is no cap. This corresponds to
     * 'Advertising Timeout' in Bluetooth
     */
    // private var msgSendTimeout: Duration = Duration.ofSeconds(1)

    /** The interval incoming messages will be scanned for. */
    private var msgScanInterval: Duration = Duration.ofSeconds(5)

    /** The duration incoming messages will be scanned for. */
    // private var msgScanDuration: Duration = Duration.ofSeconds(1)

    /** The maximum number of elements stored in the cache */
    // private var msgCacheLimit: Long = 32

    private val scanner: AtomicReference<Job?> = AtomicReference(null)
    private val sender: AtomicReference<Job?> = AtomicReference(null)
    private val relayJob: AtomicReference<Job?> = AtomicReference(null)
    private val sending: Mutex = Mutex()
    private constructor(
        builder: BuilderImpl<ID, Data, *, *>
    ) : this(
        builder.device,
        builder.msgCache,
        builder.callback,
        builder.decodeID,
        builder.decodeData,
        builder.encodeID,
        builder.encodeData,
        builder.msgData,
        builder.msgID,
        builder.filterID,
        builder.dataSize,
        builder.logger
    ) {
        builder.msgTTL?.let { msgTTL = it }

        builder.msgSendInterval?.let { msgSendInterval = it }

        builder.msgScanInterval?.let { msgScanInterval = it }

        // builder.msgCacheLimit?.let { msgCacheLimit = it }
    }
    private val coroutineScope = CoroutineScope(CoroutineName("EventMeshScope"))

    /**
     * This function starts the device, and will launch to sub-processes; one for transmitting and
     * one for receiving. If it has already been started, it will not overwrite the already started
     * process.
     *
     * @see stop
     */
    fun start() {
        scanner.updateAndGet {
            when (it) {
                null -> coroutineScope.launch(Dispatchers.Unconfined) { device.startReceiving() }
                else -> it
            }
        }

        sender.updateAndGet {
            when (it) {
                null ->
                    coroutineScope.launch(Dispatchers.Unconfined) {
                        while (isActive) {
                            delay(msgSendInterval.toMillis())
                            sending.withLock {
                                val id = msgId()
                                val data = msgData()
                                messageCache?.cacheMessage(id)
                                device.startTransmitting(msgTTL, encodeID(id), encodeData(data))
                                yield()
                            }
                            yield()
                        }
                    }
                else -> it
            }
        }

        relayJob.updateAndGet {
            when (it) {
                null ->
                    coroutineScope.launch(Dispatchers.Unconfined) {
                        while (isActive) {
                            delay(250)

                            while (relayQueue.isNotEmpty()) {
                                sending.withLock {
                                    val (ttl, id, body) = relayQueue.poll()
                                    logger(
                                        MSG_Q_TAG,
                                        "Relaying message with id: ${id.toHexString()}"
                                    )
                                    device.startTransmitting(ttl, id, body, 1000)
                                    yield()
                                }
                                delay(100)
                                yield()
                            }
                            yield()
                        }
                    }
                else -> it
            }
        }

        /*
        scanner.updateAndGet {
            when (it) {
                null ->
                    coroutineScope.launch(Dispatchers.Unconfined) {
                        while (isActive) {
                            delay(msgScanInterval.toMillis())
                            device.startReceiving()
                            yield()
                        }
                    }
                else -> it
            }
        }
         */
    }

    /**
     * This function stops the device, waiting for it to finish its iteration.
     *
     * @see start
     */
    fun stop() = runBlocking {
        relayQueue.clear()
        sender.getAndSet(null)?.cancelAndJoin()
        scanner.getAndSet(null)?.cancelAndJoin()
        relayJob.getAndSet(null)?.cancelAndJoin()
        messageCache?.clearCache()
    }

    companion object {
        /**
         * Default value for the size of a message's data/content (when converted to buffer).
         *
         * The default value is 29.
         */
        const val DATA_MAX_SIZE = 29

        /**
         * Default value for the size of a message's ID (when converted to buffer).
         *
         * The default value is 4.
         */
        const val ID_MAX_SIZE = 4

        /**
         * Default value for the duration a message is saved in teh cache.
         *
         * The default value is 60 seconds.
         */
        const val MESSAGE_CACHE_TIME: Long = 60_000

        /**
         * Creates a [Builder] for [EventMesh] with the default message cache ([MessageCache]) and
         * [EventMeshDevice].
         *
         * @param ID The messages' ID
         * @param Data The messages' content
         */
        fun <ID, Data, Rx, Tx> builder(): Builder<ID, Data, Rx, Tx> =
            BuilderImpl(EventMeshDevice.Builder(), MessageCache(MESSAGE_CACHE_TIME))

        /**
         * Creates a [Builder] for [EventMesh] with a provided message cache (set to `null` to
         * disable). Uses the default [EventMeshDevice].
         *
         * @param ID The messages' ID
         * @param Data The messages' content
         * @param mc The instance of the [MessageCache] (set to `null` to disable)
         */
        fun <ID, Data, Rx, Tx> builder(mc: MessageCache<ID>?): Builder<ID, Data, Rx, Tx> =
            BuilderImpl(EventMeshDevice.Builder(), mc)

        /**
         * Creates a [Builder] for [EventMesh] with a provided [TransportDevice]. This device is
         * used for both the [EventMeshReceiver] and [EventMeshTransmitter]. The default message
         * cache ( [MessageCache]) is used.
         *
         * @param ID The messages' ID
         * @param Data The messages' content
         * @param device The instance of the [EventMeshDevice] (Or derivative)
         */
        fun <ID, Data, Rx, Tx> builder(device: TransportDevice<Rx, Tx>): Builder<ID, Data, Rx, Tx> =
            BuilderImpl(EventMeshDevice.Builder(device), MessageCache(MESSAGE_CACHE_TIME))

        /**
         * Creates a [Builder] for [EventMesh] with a provided [TransportDevice]. This device is
         * used for both the [EventMeshReceiver] and [EventMeshTransmitter]. The provided message
         * cache ( [MessageCache]) is used (set to `null` to disable).
         *
         * @param ID The messages' ID
         * @param Data The messages' content
         * @param device The instance of the [EventMeshDevice] (Or derivative)
         * @param mc The instance of the [MessageCache] (Or derivative) (set to `null` to disable)
         */
        fun <ID, Data, Rx, Tx> builder(
            device: TransportDevice<Rx, Tx>,
            mc: MessageCache<ID>?
        ): Builder<ID, Data, Rx, Tx> = BuilderImpl(EventMeshDevice.Builder(device), mc)

        /**
         * Creates a [Builder] for [EventMesh] with a provided [EventMeshReceiver] and
         * [EventMeshTransmitter]. The default message cache ([MessageCache]) is used.
         *
         * @param ID The messages' ID
         * @param Data The messages' content
         * @param rx The [EventMeshReceiver] used
         * @param tx The [EventMeshTransmitter] used
         */
        fun <ID, Data, Rx, Tx> builder(
            rx: EventMeshReceiver<Rx>,
            tx: EventMeshTransmitter<Tx>
        ): Builder<ID, Data, Rx, Tx> =
            BuilderImpl(EventMeshDevice.Builder(rx, tx), MessageCache(MESSAGE_CACHE_TIME))

        /**
         * Creates a [Builder] for [EventMesh] with a provided [EventMeshReceiver] and
         * [EventMeshTransmitter]. The provided message cache ([MessageCache]) is used (set to
         * `null` to disable).
         *
         * @param ID The messages' ID
         * @param Data The messages' content
         * @param rx The [EventMeshReceiver] used
         * @param tx The [EventMeshTransmitter] used
         * @param mc The instance of the [MessageCache] (Or derivative) (set to `null` to disable)
         */
        fun <ID, Data, Rx, Tx> builder(
            rx: EventMeshReceiver<Rx>,
            tx: EventMeshTransmitter<Tx>,
            mc: MessageCache<ID>?
        ): Builder<ID, Data, Rx, Tx> = BuilderImpl(EventMeshDevice.Builder(rx, tx), mc)

        interface Builder<ID, Data, Rx, Tx> {
            /**
             * Sets the [EventMeshReceiver] in [EventMeshDevice].
             *
             * @param rx The [EventMeshReceiver]
             * @return the modified [Builder]
             */
            fun setReceiver(rx: EventMeshReceiver<Rx>): Builder<ID, Data, Rx, Tx>

            /**
             * Sets the [EventMeshTransmitter] in [EventMeshDevice].
             *
             * @param tx The [EventMeshTransmitter]
             * @return the modified [Builder]
             */
            fun setTransmitter(tx: EventMeshTransmitter<Tx>): Builder<ID, Data, Rx, Tx>

            /**
             * Sets the [MessageCache] (`null` to disable).
             *
             * @param mc The [MessageCache]
             * @return the modified [Builder]
             */
            fun setMessageCache(mc: MessageCache<ID>?): Builder<ID, Data, Rx, Tx>

            /**
             * Sets a function that will be called on every message that is not filtered.
             *
             * @param f The function
             * @return the modified [Builder]
             * @see addFilterFunction
             */
            fun setMessageCallback(f: (ID, Data) -> Unit): Builder<ID, Data, Rx, Tx>

            /**
             * Sets a function that converts a binary representation into an `ID`. This is what is
             * read from the other nodes. The array will always have the length [ID_MAX_SIZE],
             * otherwise an exception will be thrown. This function should uphold have the same
             * identity when using the function set in [setIDEncodeFunction], meaning:
             * ```
             * x = g(f(x))
             * ```
             *
             * where `f` is the function set here, and `g` is the function set in
             * [setIDEncodeFunction] Some functions have been made that may be of use, see
             * [rasteplads.util]
             *
             * @param f The function
             * @return The modified [Builder]
             */
            fun setIDDecodeFunction(f: (ByteArray) -> ID): Builder<ID, Data, Rx, Tx>

            /**
             * Sets a function that converts a binary representation into an `ID`. This is what is
             * read from the other nodes. The array will always have the length [DATA_MAX_SIZE],
             * otherwise an exception will be thrown. This function should uphold have the same
             * identity when using the function set in [setDataEncodeFunction], meaning:
             * ```
             * x = g(f(x))
             * ```
             *
             * where `f` is the function set here, and `g` is the function set in
             * [setDataEncodeFunction] Some functions have been made that may be of use, see
             * [rasteplads.util]
             *
             * @param f The function
             * @return The modified [Builder]
             */
            fun setDataDecodeFunction(f: (ByteArray) -> Data): Builder<ID, Data, Rx, Tx>

            /**
             * Sets a function that converts the `ID` into a binary representation. This is needed
             * for transmitting the data. The array returned should have length [ID_MAX_SIZE],
             * otherwise an exception will be thrown. This function should uphold have the same
             * identity when using the function set in [setIDDecodeFunction], meaning:
             * ```
             * x = g(f(x))
             * ```
             *
             * where `f` is the function set here, and `g` is the function set in
             * [setIDDecodeFunction] Some functions have been made that may be of use, see
             * [rasteplads.util]
             *
             * @param f The function
             * @return The modified [Builder]
             */
            fun setIDEncodeFunction(f: (ID) -> ByteArray): Builder<ID, Data, Rx, Tx>

            /**
             * Sets a function that converts the `Data` into a binary representation. This is needed
             * for transmitting the data. The array returned should have length [DATA_MAX_SIZE],
             * otherwise an exception will be thrown. This function should uphold have the same
             * identity when using the function set in [setDataDecodeFunction], meaning:
             * ```
             * x = g(f(x))
             * ```
             *
             * where `f` is the function set here, and `g` is the function set in
             * [setDataDecodeFunction] Some functions have been made that may be of use, see
             * [rasteplads.util]
             *
             * @param f The function
             * @return The modified [Builder]
             */
            fun setDataEncodeFunction(f: (Data) -> ByteArray): Builder<ID, Data, Rx, Tx>

            /**
             * Sets a constant message that will be sent out from the device at every interval (see
             * [withMsgSendTransmissionInterval]). This function overrides any value set through
             * [setDataGenerator].
             *
             * @param c The data to be sent
             * @return The modified [Builder]
             */
            fun setDataConstant(c: Data): Builder<ID, Data, Rx, Tx>

            /**
             * Sets a function for generating the messages that will be sent out from the device at
             * every interval (see [withMsgSendTransmissionInterval]). This function overrides any
             * value set through [setDataConstant].
             *
             * @param f The generator-function
             * @return The modified [Builder]
             */
            fun setDataGenerator(f: () -> Data): Builder<ID, Data, Rx, Tx>

            /**
             * Sets a constant `ID`. This `ID` will be used as the `ID` for each message, and will
             * be stored by every node for some time (See [withMsgCacheDelete]). This overrides any
             * value specified with [setIDGenerator].
             *
             * @param i `ID`
             * @return The modified [Builder]
             */
            fun setIDConstant(i: ID): Builder<ID, Data, Rx, Tx>

            /**
             * Sets a function for generating `ID`s. This function acts as the `ID` for each
             * message, and will be stored by every node for some time (See [withMsgCacheDelete]).
             * The `ID` should be unique for at least the duration which it can be saved. This
             * overrides any value specified with [setIDConstant].
             *
             * @param f The generator-function
             * @return The modified [Builder]
             */
            fun setIDGenerator(f: () -> ID): Builder<ID, Data, Rx, Tx>

            /**
             * Sets the size for `Data` in hte byte array. The decode function given in
             * [setDataDecodeFunction] will be given an array of this size.
             * @param size The size of `Data`
             * @return The modified [Builder]
             */
            fun setDataSize(size: Int): Builder<ID, Data, Rx, Tx>

            /**
             * Adds a filtering function. These functions filter messages by their ID. Only IDs that
             * return `true` from the filters will be sent called with the handle function (See
             * [setMessageCallback]). The filters are applied in the order they are set.
             *
             * @param f The filter-function
             * @return The modified [Builder]
             */
            fun addFilterFunction(f: (ID) -> Boolean): Builder<ID, Data, Rx, Tx>

            /**
             * Adds multiple filtering functions. These functions filter messages by their ID. Only
             * IDs that return `true` from the filters will be sent called with the handle function
             * (See [setMessageCallback]). The filters are applied in the order they are set.
             *
             * @param fs The filter-functions
             * @return The modified [Builder]
             */
            fun addFilterFunction(vararg fs: (ID) -> Boolean): Builder<ID, Data, Rx, Tx>

            /**
             * Sets the time a message ID should be saved in the cache. This is to reduce the number
             * of redundant relays, since relaying the same message several times is not needed.
             *
             * @param d Duration the message should be saved
             * @return The modified [Builder]
             */
            fun withMsgCacheDelete(d: Duration): Builder<ID, Data, Rx, Tx>

            /**
             * Sets the message Time TO Live (TTL). This number denotes how many times a node can
             * relay the message in the network.
             *
             * @param t Number of relays
             * @return The modified [Builder]
             */
            fun withMsgTTL(t: Byte): Builder<ID, Data, Rx, Tx>

            /**
             * Sets the interval between message sending sessions
             *
             * @param d Waiting time
             * @return The modified [Builder]
             */
            fun withMsgSendInterval(d: Duration): Builder<ID, Data, Rx, Tx>

            /*
            /**
             * Sets the message sending interval, I.E. the discrete events of transmissions.
             *
             * @param d Waiting time
             * @return The modified [Builder]
             */
             fun withMsgSendTransmissionInterval(d: Duration): Builder<ID, Data, Rx, Tx>
            */
            /**
             * Sets the sending duration timeout (cap). This is the max time duration the message
             * defined in either [setDataConstant] or [setDataGenerator] will be transmitted. It
             * will transmit in this time, or until echo.
             *
             * @param d Sending time
             * @return The modified [Builder]
             */
            fun withMsgSendTimeout(d: Duration): Builder<ID, Data, Rx, Tx>

            /**
             * Sets the scanning interval.
             *
             * @param d Waiting time
             * @return The modified [Builder]
             */
            fun withMsgScanInterval(d: Duration): Builder<ID, Data, Rx, Tx>

            /**
             * Sets the scanning duration.
             *
             * @param d Scanning time
             * @return The modified [Builder]
             */
            fun withMsgScanDuration(d: Duration): Builder<ID, Data, Rx, Tx>

            /**
             * Sets a message cache. If `null`, it disables the message cache
             *
             * @param mc Boolean enabling/disabling the message cache
             * @return The modified [Builder]
             */
            fun withMsgCache(mc: MessageCache<ID>?): Builder<ID, Data, Rx, Tx>

            /*
            /**
             * Sets the limit of elements saved in the message cache
             *
             * @param l Limit
             * @return The modified [Builder]
             */
            fun withMsgCacheLimit(l: Long): Builder<ID, Data, Rx, Tx>
             */

            /*
            /**
             * Sets the [EventMeshDevice] used.
             *
             * @param d Device
             * @return The modified [Builder]
             */
            fun withDevice(d: Device): BuilderBuilder<ID, Data, Rx, Tx>
            */

            /**
             * Sets a callback function for when an echo is not received when transmitting your
             * message. Set to null to omit.
             *
             * @param f The function
             * @return The modified [Builder]
             */
            fun withEchoCallback(f: (() -> Unit)?): Builder<ID, Data, Rx, Tx>

            /**
             * Adds a logger which can be called inside EventMesh. Useful for debugging Android
             * Projects.
             *
             * @param func The debug function that will be called with
             * ```
             *        func(tag: String, message: String)
             * @return
             * ```
             * The modified [Builder]
             */
            fun withLogger(func: (String, String) -> Unit): Builder<ID, Data, Rx, Tx>

            /**
             * Builds the [Builder]
             *
             * @return [EventMesh] with the needed properties
             * @throws IllegalStateException If the needed variables hasn't been set
             */
            fun build(): EventMesh<ID, Data>
        }

        private class BuilderImpl<ID, Data, Rx, Tx>(
            val device: EventMeshDevice.Builder<Rx, Tx>,
            var msgCache: MessageCache<ID>? = null,
        ) : Builder<ID, Data, Rx, Tx> {
            lateinit var callback: (ID, Data) -> Unit
            lateinit var decodeID: (ByteArray) -> ID
            lateinit var decodeData: (ByteArray) -> Data
            lateinit var encodeID: (ID) -> ByteArray
            lateinit var encodeData: (Data) -> ByteArray
            lateinit var msgData: Either<Data, () -> Data>
            lateinit var msgID: Either<ID, () -> ID>
            var dataSize: Int = 0

            val filterID: MutableList<(ID) -> Boolean> = mutableListOf()

            // var msgDelete: Duration? = null
            var msgTTL: Byte? = null

            var msgSendInterval: Duration? = null

            // var msgSendTimeout: Duration? = null
            var msgScanInterval: Duration? = null
            // var msgCacheLimit: Long? = null
            var logger: (String, String) -> Unit = { t, m -> System.err.println("$t: $m") }

            override fun build(): EventMesh<ID, Data> {
                // check(device != null) { "EventMesh Device is needed" }
                check(::callback.isInitialized) { "Function for callbacks is necessary" }
                check(::decodeID.isInitialized) {
                    "Function to convert binary data into `ID` is necessary"
                }
                check(::decodeData.isInitialized) {
                    "Function to convert binary data into `Data` is necessary"
                }
                check(::encodeID.isInitialized) {
                    "Function to convert `ID` into binary data is necessary"
                }
                check(::encodeData.isInitialized) {
                    "Function to convert `Data` into binary data is necessary"
                }
                check(::msgData.isInitialized) {
                    "Either a constant or a generator function for the advertising `Data` must be set"
                }
                check(::msgID.isInitialized) {
                    "A generator function for the advertising `ID` must be set"
                }
                check(dataSize > 0) { "The size of `Data` must be set" }

                return EventMesh(this)
            }

            override fun withLogger(func: (String, String) -> Unit): Builder<ID, Data, Rx, Tx> {
                logger = func
                return this
            }

            override fun withEchoCallback(f: (() -> Unit)?): Builder<ID, Data, Rx, Tx> {
                device.withEchoCallback(f)
                return this
            }

            override fun setIDDecodeFunction(f: (ByteArray) -> ID): Builder<ID, Data, Rx, Tx> {
                decodeID = f
                return this
            }

            override fun setDataDecodeFunction(f: (ByteArray) -> Data): Builder<ID, Data, Rx, Tx> {
                decodeData = f
                return this
            }

            override fun setIDEncodeFunction(f: (ID) -> ByteArray): Builder<ID, Data, Rx, Tx> {
                encodeID = f
                return this
            }

            override fun setDataEncodeFunction(f: (Data) -> ByteArray): Builder<ID, Data, Rx, Tx> {
                encodeData = f
                return this
            }

            override fun setDataConstant(c: Data): Builder<ID, Data, Rx, Tx> {
                msgData = Either.left(c)
                return this
            }

            override fun setDataGenerator(f: () -> Data): Builder<ID, Data, Rx, Tx> {
                msgData = Either.right(f)
                return this
            }

            override fun addFilterFunction(f: (ID) -> Boolean): Builder<ID, Data, Rx, Tx> {
                filterID.add(f)
                return this
            }

            override fun addFilterFunction(vararg fs: (ID) -> Boolean): Builder<ID, Data, Rx, Tx> {
                filterID.addAll(fs)
                return this
            }

            override fun setIDGenerator(f: () -> ID): Builder<ID, Data, Rx, Tx> {
                msgID = Either.right(f)
                return this
            }

            override fun setMessageCallback(f: (ID, Data) -> Unit): Builder<ID, Data, Rx, Tx> {
                callback = f
                return this
            }

            override fun setIDConstant(i: ID): Builder<ID, Data, Rx, Tx> {
                msgID = Either.left(i)
                return this
            }

            override fun setReceiver(rx: EventMeshReceiver<Rx>): Builder<ID, Data, Rx, Tx> {
                device.withReceiver(rx)
                return this
            }

            override fun setTransmitter(tx: EventMeshTransmitter<Tx>): Builder<ID, Data, Rx, Tx> {
                device.withTransmitter(tx)
                return this
            }

            override fun setDataSize(size: Int): Builder<ID, Data, Rx, Tx> {
                check(size <= DATA_MAX_SIZE) {
                    "Provided size too large, expected value equal to or less than $DATA_MAX_SIZE, got $size"
                }
                dataSize = size
                return this
            }

            override fun setMessageCache(mc: MessageCache<ID>?): Builder<ID, Data, Rx, Tx> {
                msgCache = mc
                return this
            }

            override fun withMsgCacheDelete(d: Duration): Builder<ID, Data, Rx, Tx> {
                msgCache?.changeCacheTime(d.toMillis())
                return this
            }

            override fun withMsgTTL(t: Byte): Builder<ID, Data, Rx, Tx> {
                msgTTL = t
                return this
            }

            override fun withMsgSendInterval(d: Duration): Builder<ID, Data, Rx, Tx> {
                msgSendInterval = d
                return this
            }

            /*
            // TODO: IS bad with BT android
            override fun withMsgSendTransmissionInterval(d: Duration): Builder<ID, Data, Rx, Tx> {
                msgSendInterval = d // TODO: skal i device
                return this
            }
             */

            override fun withMsgSendTimeout(d: Duration): Builder<ID, Data, Rx, Tx> {
                device.withTransmitTimeout(d)
                return this
            }

            override fun withMsgScanInterval(d: Duration): Builder<ID, Data, Rx, Tx> {
                msgScanInterval = d
                return this
            }

            override fun withMsgScanDuration(d: Duration): Builder<ID, Data, Rx, Tx> {
                device.withReceiveDuration(d)
                return this
            }

            override fun withMsgCache(mc: MessageCache<ID>?): Builder<ID, Data, Rx, Tx> {
                msgCache = mc
                return this
            }

            /*
            override fun withMsgCacheLimit(l: Long): Builder<ID, Data, Rx, Tx> {
                msgCacheLimit = l
                return this
            }
             */
        }
    }
}
