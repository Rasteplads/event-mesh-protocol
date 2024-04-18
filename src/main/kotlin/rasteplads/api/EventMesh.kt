package rasteplads.api

import java.time.Duration
import kotlinx.coroutines.*
import rasteplads.messageCache.MessageCache
import rasteplads.util.Either

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
final class EventMesh<ID, Data>
private constructor(
    private val device: EventMeshDevice,
    private val messageCache: MessageCache<ID>?,
    private val callback: (ID, Data) -> Unit,
    private val decodeID: (ByteArray) -> ID,
    private val decodeData: (ByteArray) -> Data,
    private val encodeID: (ID) -> ByteArray,
    private val encodeData: (Data) -> ByteArray,
    private val msgData: Either<Data, () -> Data>,
    private val msgId: Either<ID, () -> ID>,
    private val filterID: List<(ID) -> Boolean>,
) {

    // TODO: set correct default values
    /** Time a message is stored in the cache */
    private var msgDelete: Duration = Duration.ofSeconds(30)

    /** Time TO Live (TTL) for the messages. */
    private var msgTTL: UInt = 10u

    /**
     * The interval the messages from [msgData] will be sent. This corresponds to 'Advertising
     * Interval' in Bluetooth
     */
    // private var msgSendInterval: Duration = Duration.ofMillis(100)

    /** The interval the between a message session. */
    private var msgSendInterval: Duration = Duration.ofSeconds(10)

    /**
     * The duration the messages from [msgData] will be sent. A message session will only be this
     * lon, or until an echo. If `Duration.ZERO`, then there is no cap. This corresponds to
     * 'Advertising Timeout' in Bluetooth
     */
    private var msgSendTimeout: Duration = Duration.ofSeconds(1)

    /** The interval incoming messages will be scanned for. */
    private var msgScanInterval: Duration = Duration.ofSeconds(5)

    /** The duration incoming messages will be scanned for. */
    // private var msgScanDuration: Duration = Duration.ofSeconds(1)

    /** The maximum number of elements stored in the cache */
    private var msgCacheLimit: Long = 32

    private lateinit var btScanner: Job
    private lateinit var btSender: Job

    private constructor(
        builder: BuilderImpl<ID, Data>
    ) : this(
        builder.device.build(),
        builder.msgCache,
        builder.callback,
        builder.decodeID,
        builder.decodeData,
        builder.encodeID,
        builder.encodeData,
        builder.msgData,
        builder.msgID,
        builder.filterID,
    ) {
        builder.msgDelete?.let { msgDelete = it }
        builder.msgTTL?.let { msgTTL = it }

        builder.msgSendInterval?.let { msgSendInterval = it }
        builder.msgSendTimeout?.let { msgSendTimeout = it }

        check(msgSendTimeout.isZero || msgSendTimeout >= msgSendInterval) {
            "non-zero message duration cap cannot be less than the sending interval"
        }

        builder.msgScanInterval?.let { msgScanInterval = it }

        builder.msgCacheLimit?.let { msgCacheLimit = it }
    }

    /** TODO */
    fun start() = runBlocking {
        // TODO: SETUP BT TO HANDLE MESSAGES (CONVERT, FILTER, CALLBACK)
        // TODO: Setup callback
        btSender =
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    do {
                        delay(msgSendInterval.toMillis())
                        val id = msgId.getLeft() ?: msgId.getRight()!!()
                        val data = msgData.getLeft() ?: msgData.getRight()!!()
                        device.startTransmitting(msgTTL.toByte(), encodeID(id), encodeData(data))
                    } while (isActive)
                } finally {
                    // TODO: Handle cancel?
                }
            }
        btScanner =
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    do {
                        delay(msgScanInterval.toMillis())
                        device.startReceiving()
                    } while (isActive)
                } finally {
                    // TODO: Handle cancel?
                }
            }
    }

    /** TODO */
    fun stop() = runBlocking {
        if (btSender.isActive) btSender.cancel()
        if (btScanner.isActive) btScanner.cancel()
    }

    private fun ByteArray.split(i: Int): Pair<ByteArray, ByteArray> =
        Pair(this.sliceArray(0 until i), this.sliceArray(i until this.size))

    private fun scanningCallback(msg: ByteArray) {
        // TODO: cache check before or after relay?
        require(msg.size >= 1 + ID_MAX_SIZE) {
            "Message does not conform with the minimum requirement (TTL + ID)"
        }

        if (msg[0] > Byte.MIN_VALUE) relay(msg) // TTL

        val (idB, dataB) = msg.sliceArray(1 until msg.size).split(ID_MAX_SIZE)
        val id = decodeID(idB)

        // TODO: CACHE CHECK

        if (filterID.any { f -> !f(id) }) return // FILTER

        callback(id, decodeData(dataB))
    }

    private fun relay(msg: ByteArray) {
        println(msg.toList())
        msg[0]--
        println(msg.toList())
        // TODO: Mod TTL, then send
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
         * Creates a [Builder] for [EventMesh] with the default message cache ([MessageCache]) and
         * [EventMeshDevice].
         *
         * @param ID The messages' ID
         * @param Data The messages' content
         */
        fun <ID, Data> builder(): Builder<ID, Data> =
            BuilderImpl(EventMeshDevice.Builder(), MessageCache(60))

        /**
         * Creates a [Builder] for [EventMesh] with a provided message cache (set to `null` to
         * disable). Uses the default [EventMeshDevice].
         *
         * @param ID The messages' ID
         * @param Data The messages' content
         * @param mc The instance of the [MessageCache] (set to `null` to disable)
         */
        fun <ID, Data> builder(mc: MessageCache<ID>?): Builder<ID, Data> =
            BuilderImpl(EventMeshDevice.Builder(), mc) // TODO: REAL TYPE AND INITIALISATION

        /**
         * Creates a [Builder] for [EventMesh] with a provided [TransportDevice]. This device is
         * used for both the [EventMeshReceiver] and [EventMeshTransmitter]. The default message
         * cache ([MessageCache]) is used.
         *
         * @param ID The messages' ID
         * @param Data The messages' content
         * @param device The instance of the [EventMeshDevice] (Or derivative)
         */
        fun <ID, Data> builder(device: TransportDevice): Builder<ID, Data> =
            BuilderImpl(EventMeshDevice.Builder().withDevice(device), MessageCache(60))

        /**
         * Creates a [Builder] for [EventMesh] with a provided [TransportDevice]. This device is
         * used for both the [EventMeshReceiver] and [EventMeshTransmitter]. The provided message
         * cache ([MessageCache]) is used (set to `null` to disable).
         *
         * @param ID The messages' ID
         * @param Data The messages' content
         * @param device The instance of the [EventMeshDevice] (Or derivative)
         * @param mc The instance of the [MessageCache] (Or derivative) (set to `null` to disable)
         */
        fun <ID, Data> builder(device: TransportDevice, mc: MessageCache<ID>?): Builder<ID, Data> =
            BuilderImpl(EventMeshDevice.Builder().withDevice(device), mc)

        /**
         * Creates a [Builder] for [EventMesh] with a provided [EventMeshReceiver] and
         * [EventMeshTransmitter]. The default message cache ([MessageCache]) is used.
         *
         * @param ID The messages' ID
         * @param Data The messages' content
         * @param rx The [EventMeshReceiver] used
         * @param tx The [EventMeshTransmitter] used
         */
        fun <ID, Data> builder(rx: EventMeshReceiver, tx: EventMeshTransmitter): Builder<ID, Data> =
            BuilderImpl(
                EventMeshDevice.Builder().withReceiver(rx).withTransmitter(tx), MessageCache(60))

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
        fun <ID, Data> builder(
            rx: EventMeshReceiver,
            tx: EventMeshTransmitter,
            mc: MessageCache<ID>?
        ): Builder<ID, Data> =
            BuilderImpl(EventMeshDevice.Builder().withReceiver(rx).withTransmitter(tx), mc)

        interface Builder<ID, Data> {

            /**
             * Sets the [EventMeshReceiver] in [EventMeshDevice].
             *
             * @param rx The [EventMeshReceiver]
             * @return the modified [Builder]
             */
            fun setReceiver(rx: EventMeshReceiver): Builder<ID, Data>

            /**
             * Sets the [EventMeshTransmitter] in [EventMeshDevice].
             *
             * @param tx The [EventMeshTransmitter]
             * @return the modified [Builder]
             */
            fun setTransmitter(tx: EventMeshTransmitter): Builder<ID, Data>

            /**
             * Sets the [MessageCache] (`null` to disable).
             *
             * @param mc The [MessageCache]
             * @return the modified [Builder]
             */
            fun setMessageCache(mc: MessageCache<ID>?): Builder<ID, Data>

            /**
             * Sets a function that will be called on every message that is not filtered.
             *
             * @param f The function
             * @return the modified [Builder]
             * @see addFilterFunction
             */
            fun setMessageCallback(f: (ID, Data) -> Unit): Builder<ID, Data>

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
            fun setIDDecodeFunction(f: (ByteArray) -> ID): Builder<ID, Data>

            // TODO: MAKE SURE THE LENGTH IS CORRECT
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
            fun setDataDecodeFunction(f: (ByteArray) -> Data): Builder<ID, Data>

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
            fun setIDEncodeFunction(f: (ID) -> ByteArray): Builder<ID, Data>

            // TODO: MAKE SURE THE LENGTH IS CORRECT
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
            fun setDataEncodeFunction(f: (Data) -> ByteArray): Builder<ID, Data>

            /**
             * Sets a constant message that will be sent out from the device at every interval (see
             * [withMsgSendTransmissionInterval]). This function overrides any value set through
             * [setDataGenerator].
             *
             * @param c The data to be sent
             * @return The modified [Builder]
             */
            fun setDataConstant(c: Data): Builder<ID, Data>

            /**
             * Sets a function for generating the messages that will be sent out from the device at
             * every interval (see [withMsgSendTransmissionInterval]). This function overrides any
             * value set through [setDataConstant].
             *
             * @param f The generator-function
             * @return The modified [Builder]
             */
            fun setDataGenerator(f: () -> Data): Builder<ID, Data>

            /**
             * Sets a constant `ID`. This `ID` will be used as the `ID` for each message, and will
             * be stored by every node for some time (See [withMsgCacheDelete]). This overrides any
             * value specified with [setIDGenerator].
             *
             * @param i `ID`
             * @return The modified [Builder]
             */
            fun setIDConstant(i: ID): Builder<ID, Data>

            /**
             * Sets a function for generating `ID`s. This function acts as the `ID` for each
             * message, and will be stored by every node for some time (See [withMsgCacheDelete]).
             * The `ID` should be unique for at least the duration which it can be saved. This
             * overrides any value specified with [setIDConstant].
             *
             * @param f The generator-function
             * @return The modified [Builder]
             */
            fun setIDGenerator(f: () -> ID): Builder<ID, Data>

            /**
             * Adds a filtering function. These functions filter messages by their ID. Only IDs that
             * return `true` from the filters will be sent called with the handle function (See
             * [setMessageCallback]). The filters are applied in the order they are set.
             *
             * @param f The filter-function
             * @return The modified [Builder]
             */
            fun addFilterFunction(f: (ID) -> Boolean): Builder<ID, Data>

            /**
             * Adds multiple filtering functions. These functions filter messages by their ID. Only
             * IDs that return `true` from the filters will be sent called with the handle function
             * (See [setMessageCallback]). The filters are applied in the order they are set.
             *
             * @param fs The filter-functions
             * @return The modified [Builder]
             */
            fun addFilterFunction(vararg fs: (ID) -> Boolean): Builder<ID, Data>

            /**
             * Sets the time a message ID should be saved in the cache. This is to reduce the number
             * of redundant relays, since relaying the same message several times is not needed.
             *
             * @param d Duration the message should be saved
             * @return The modified [Builder]
             */
            fun withMsgCacheDelete(d: Duration): Builder<ID, Data>

            /**
             * Sets the message Time TO Live (TTL). This number denotes how many times a node can
             * relay the message in the network.
             *
             * @param t Number of relays
             * @return The modified [Builder]
             */
            fun withMsgTTL(t: UInt): Builder<ID, Data>

            /**
             * Sets the interval between message sending sessions
             *
             * @param d Waiting time
             * @return The modified [Builder]
             */
            fun withMsgSendInterval(d: Duration): Builder<ID, Data>

            /**
             * Sets the message sending interval, I.E. the discrete events of transmissions.
             *
             * @param d Waiting time
             * @return The modified [Builder]
             */
            // fun withMsgSendTransmissionInterval(d: Duration): Builder<ID, Data>

            /**
             * Sets the sending duration timeout (cap). This is the max time duration the message
             * defined in either [setDataConstant] or [setDataGenerator] will be transmitted. It
             * will transmit in this time, or until echo.
             *
             * @param d Sending time
             * @return The modified [Builder]
             */
            fun withMsgSendTimeout(d: Duration): Builder<ID, Data>

            /**
             * Sets the scanning interval.
             *
             * @param d Waiting time
             * @return The modified [Builder]
             */
            fun withMsgScanInterval(d: Duration): Builder<ID, Data>

            /**
             * Sets the scanning duration.
             *
             * @param d Scanning time
             * @return The modified [Builder]
             */
            fun withMsgScanDuration(d: Duration): Builder<ID, Data>

            /**
             * Sets a message cache. If `null`, it disables the message cache
             *
             * @param b Boolean enabling/disabling the message cache
             * @return The modified [Builder]
             */
            fun withMsgCache(b: MessageCache<ID>?): Builder<ID, Data>

            /**
             * Sets the limit of elements saved in the message cache
             *
             * @param l Limit
             * @return The modified [Builder]
             */
            fun withMsgCacheLimit(l: Long): Builder<ID, Data>

            /*
            /**
             * Sets the [EventMeshDevice] used.
             *
             * @param d Device
             * @return The modified [Builder]
             */
            fun withDevice(d: Device): BuilderBuilder<ID, Data>
            */

            /**
             * Builds the [Builder]
             *
             * @return [EventMesh] with the needed properties
             * @throws IllegalStateException If the needed variables hasn't been set
             */
            fun build(): EventMesh<ID, Data>
        }

        private class BuilderImpl<ID, Data>(
            val device: EventMeshDevice.Builder, // TODO SIMON
            var msgCache: MessageCache<ID>? = null,
        ) : Builder<ID, Data> {
            lateinit var callback: (ID, Data) -> Unit
            lateinit var decodeID: (ByteArray) -> ID
            lateinit var decodeData: (ByteArray) -> Data
            lateinit var encodeID: (ID) -> ByteArray
            lateinit var encodeData: (Data) -> ByteArray
            lateinit var msgData: Either<Data, () -> Data>
            lateinit var msgID: Either<ID, () -> ID>

            val filterID: MutableList<(ID) -> Boolean> = mutableListOf()

            var msgDelete: Duration? = null
            var msgTTL: UInt? = null

            var msgSendInterval: Duration? = null
            var msgSendTimeout: Duration? = null
            var msgScanInterval: Duration? = null
            var msgCacheLimit: Long? = null

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

                return EventMesh(this)
            }

            override fun setIDDecodeFunction(f: (ByteArray) -> ID): Builder<ID, Data> {
                decodeID = f
                return this
            }

            override fun setDataDecodeFunction(f: (ByteArray) -> Data): Builder<ID, Data> {
                decodeData = f
                return this
            }

            override fun setIDEncodeFunction(f: (ID) -> ByteArray): Builder<ID, Data> {
                encodeID = f
                return this
            }

            override fun setDataEncodeFunction(f: (Data) -> ByteArray): Builder<ID, Data> {
                encodeData = f
                return this
            }

            override fun setDataConstant(c: Data): Builder<ID, Data> {
                msgData = Either.left(c)
                return this
            }

            override fun setDataGenerator(f: () -> Data): Builder<ID, Data> {
                msgData = Either.right(f)
                return this
            }

            override fun addFilterFunction(f: (ID) -> Boolean): Builder<ID, Data> {
                filterID.add(f)
                return this
            }

            override fun addFilterFunction(vararg fs: (ID) -> Boolean): Builder<ID, Data> {
                filterID.addAll(fs)
                return this
            }

            override fun setIDGenerator(f: () -> ID): Builder<ID, Data> {
                msgID = Either.right(f)
                return this
            }

            override fun setMessageCallback(f: (ID, Data) -> Unit): Builder<ID, Data> {
                callback = f // TODO: should end in the receiver somehow
                return this
            }

            override fun setIDConstant(i: ID): Builder<ID, Data> {
                msgID = Either.left(i)
                return this
            }

            override fun setReceiver(rx: EventMeshReceiver): Builder<ID, Data> {
                device.withReceiver(rx)
                return this
            }

            override fun setTransmitter(tx: EventMeshTransmitter): Builder<ID, Data> {
                device.withTransmitter(tx)
                return this
            }

            override fun setMessageCache(mc: MessageCache<ID>?): Builder<ID, Data> {
                msgCache = mc
                return this
            }

            override fun withMsgCacheDelete(d: Duration): Builder<ID, Data> {
                msgDelete = d // TODO: Handle this
                return this
            }

            override fun withMsgTTL(t: UInt): Builder<ID, Data> {
                msgTTL = t
                return this
            }

            override fun withMsgSendInterval(d: Duration): Builder<ID, Data> {
                msgSendInterval = d // TODO
                return this
            }

            /*
            // TODO: IS bad with BT android
            override fun withMsgSendTransmissionInterval(d: Duration): Builder<ID, Data> {
                msgSendInterval = d // TODO: skal i device
                return this
            }
             */

            override fun withMsgSendTimeout(d: Duration): Builder<ID, Data> {
                device.withTransmitTimeout(d)
                return this
            }

            override fun withMsgScanInterval(d: Duration): Builder<ID, Data> {
                msgScanInterval = d
                return this
            }

            override fun withMsgScanDuration(d: Duration): Builder<ID, Data> {
                device.withReceiveDuration(d)
                return this
            }

            override fun withMsgCache(mc: MessageCache<ID>?): Builder<ID, Data> {
                msgCache = mc
                return this
            }

            override fun withMsgCacheLimit(l: Long): Builder<ID, Data> {
                msgCacheLimit = l
                return this
            }

            /*
            override fun withDevice(d: Device): Builder<ID, Data> {
                device = d
                return this
            }
             */
        }
    }
}
