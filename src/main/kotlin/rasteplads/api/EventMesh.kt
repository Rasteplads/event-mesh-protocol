package rasteplads.api

import java.time.Duration
import kotlinx.coroutines.*
import rasteplads.messageCache.MessageCache
import rasteplads.util.Either
import rasteplads.util.toInt

fun main() {
    println("start")

    val f =
        EventMesh.builder<Int, Byte>()
            .setDataConstant(0)
            .setIDGenerator { 10 }
            .setHandleMessage { _, _ -> }
            .setIntoIDFunction { it.toInt() }
            .setIntoDataFunction { _ -> 0 }
            .setFromIDFunction { _ -> byteArrayOf(0, 1, 2, 3) }
            .setFromDataFunction { byteArrayOf(it) }
            .addFilterFunction { id -> id and 1 == 0 } // isEven
            .addFilterFunction { id -> id < 5_000_000 } // isEven
            .addFilterFunction { id -> id > 1_000_000 } // isEven
            .build()

    val b = byteArrayOf(0, 3, 5, 6, 8, 0)
    println(b.toList())
    // f.scanningCallback(b)
    println(b.toList())

    println("f")
    f.start()
    println("MAIN")

    // delay(1000)
    println("I want it to stop")
    f.stop()

    println("works I guess")
}

/**
 * This class handles communicating messages to the dynamic mesh network. `ID` represents the IDs
 * for each message, `Data` represents the message itself, and `MC` is the message cache for storing
 * already known messages.
 *
 * If the default message cache is used, `ID` must af [equals] overridden.
 *
 * To use, use the one of the [builder] functions, that returns a [Builder], which needs some values
 * to work (more or less all methods following the 'set*' naming scheme). After this, you can simply
 * run [start], that runs everything in the background. When you want to stop, simply run [stop].
 *
 * @author t-lohse
 */
final class EventMesh<ID, Data> // TODO EventMeshDevice interface pls
private constructor(
    private val device: Int, // TODO: EVENTMESHDEVICE
    private val messageCache: MessageCache<ID>?,
    private val callback: (ID, Data) -> Unit,
    private val intoID: (ByteArray) -> ID,
    private val intoData: (ByteArray) -> Data,
    private val fromID: (ID) -> ByteArray,
    private val fromData: (Data) -> ByteArray,
    private val msgData: Either<Data, () -> Data>,
    private val msgId: Either<ID, () -> ID>,
    private val filterID: List<(ID) -> Boolean>,
) {

    // TODO: set correct default values
    /** Time a message is stored in the cache */
    private var msgDelete: Duration = Duration.ofSeconds(30)

    /** Time TO Live (TTL) for the messages. */
    private var msgTTL: Long = 10

    /**
     * The interval the messages from [msgData] will be sent. This corresponds to 'Advertising
     * Interval' in Bluetooth
     */
    private var msgSendInterval: Duration = Duration.ofMillis(100)

    /** The interval the between a message session. */
    private var msgSendSessionInterval: Duration = Duration.ofSeconds(10)

    /**
     * The duration the messages from [msgData] will be sent. A message session will only be this
     * lon, or until an echo. If `Duration.ZERO`, then there is no cap. This corresponds to
     * 'Advertising Timeout' in Bluetooth
     */
    private var msgSendTimeout: Duration = Duration.ofSeconds(1)

    /** The interval incoming messages will be scanned for. */
    private var msgScanInterval: Duration = Duration.ofSeconds(5)

    /** The duration incoming messages will be scanned for. */
    private var msgScanDuration: Duration = Duration.ofSeconds(1)

    /** The maximum number of elements stored in the cache */
    private var msgCacheLimit: Long = 32

    private lateinit var btScanner: Job
    private lateinit var btSender: Job

    private constructor(
        builder: BuilderImpl<ID, Data>
    ) : this(
        builder.device,
        builder.msgCache,
        builder.callback,
        builder.intoID,
        builder.intoData,
        builder.fromID,
        builder.fromData,
        builder.msgData,
        builder.msgID,
        builder.filterID,
    ) {
        builder.msgDelete?.let { msgDelete = it }
        builder.msgTTL?.let { msgTTL = it }

        builder.msgSendSessionInterval?.let { msgSendSessionInterval = it }
        builder.msgSendInterval?.let { msgSendInterval = it }
        builder.msgSendTimeout?.let { msgSendTimeout = it }

        check(msgSendTimeout.isZero || msgSendTimeout >= msgSendInterval) {
            "non-zero message duration cap cannot be less than the sending interval"
        }

        builder.msgScanInterval?.let { msgScanInterval = it }
        builder.msgScanDuration?.let { msgScanDuration = it }

        builder.msgCacheLimit?.let { msgCacheLimit = it }
    }

    /** TODO */
    fun start() = runBlocking {
        // TODO: SETUP BT TO HANDLE MESSAGES (CONVERT, FILTER, CALLBACK)
        println("START HW (IF GLOBAL)")
        btSender =
            GlobalScope.launch(Dispatchers.Default) {
                try {
                    println("START HW (IF LOCAL)")
                    do {
                        delay(msgSendSessionInterval.toMillis())
                        val data = msgData.getLeft() ?: msgData.getRight()!!()
                        // TODO: SEND BYTES WITH TTL
                    } while (isActive)
                } finally {
                    println("STOP HW (IF LOCAL)")
                }
            }
        btScanner =
            GlobalScope.launch(Dispatchers.Default) {
                try {
                    println("START HW (IF LOCAL)")
                    do {
                        delay(msgScanInterval.toMillis())
                        val k =
                            withTimeout(msgScanDuration.toMillis()) {
                                // TODO: CALL DEVICE.FUNC WITH [scanningCallback]
                                // TODO: HANDLE RELAYING
                            }
                    } while (isActive)
                } finally {
                    println("STOP HW (IF LOCAL)")
                }
            }
        delay(3000)
        println("FUNC")
    }

    /** TODO */
    fun stop() = runBlocking {
        if (btSender.isActive) btSender.cancelAndJoin()
        if (btScanner.isActive) btScanner.cancelAndJoin()
        println("STOP HW (IF GLOBAL)")
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
        val id = intoID(idB)

        // TODO: CACHE CHECK

        if (filterID.any { f -> !f(id) }) return // FILTER

        callback(id, intoData(dataB))
    }

    private fun relay(msg: ByteArray) {
        /*
        val out = msg.copyOf()
        println(out.toList())
        out[0]--
        println(out.toList())
         */
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
            BuilderImpl(7, MessageCache()) // TODO: REAL TYPE AND INITIALISATION

        /**
         * Creates a [Builder] for [EventMesh] with a provided message cache (set to `null` to
         * disable). Uses the default [EventMeshDevice].
         *
         * @param ID The messages' ID
         * @param Data The messages' content
         * @param mc The instance of the [MessageCache] (set to `null` to disable)
         */
        fun <ID, Data> builder(mc: MessageCache<ID>?): Builder<ID, Data> =
            BuilderImpl(7, mc) // TODO: REAL TYPE AND INITIALISATION

        /**
         * Creates a [Builder] for [EventMesh] with a provided [EventMeshDevice]. Uses the default
         * message cache ([MessageCache]).
         *
         * @param ID The messages' ID
         * @param Data The messages' content
         * @param device The instance of the [EventMeshDevice] (Or derivative)
         */
        fun <ID, Data> builder(
            device: Int // TODO: SIMON
        ): Builder<ID, Data> =
            BuilderImpl(device, MessageCache()) // TODO: REAL TYPE AND INITIALISATION

        /**
         * Creates a [Builder] for [EventMesh] with a provided message cache (set to `null` to
         * disable) and [EventMeshDevice].
         *
         * @param ID The messages' ID
         * @param Data The messages' content
         * @param device The instance of the [EventMeshDevice] (Or derivative)
         * @param mc The instance of the [MessageCache] (Or derivative) (set to `null` to disable)
         */
        fun <ID, Data> builder(
            device: Int, // TODO: SIMON
            mc: MessageCache<ID>?
        ): Builder<ID, Data> = BuilderImpl(device, mc)

        interface Builder<ID, Data> {

            /**
             * Sets a function that will be called on every message that is not filtered.
             *
             * @param f The function
             * @return the modified [Builder]]
             * @see addFilterFunction
             */
            fun setHandleMessage(f: (ID, Data) -> Unit): Builder<ID, Data>

            /**
             * Sets a function that converts a binary representation into an `ID`. This is what is
             * read from the other nodes. The array will always have the length [ID_MAX_SIZE],
             * otherwise an exception will be thrown. This function should uphold have the same
             * identity when using the function set in [setFromIDFunction], meaning:
             * ```
             * x = g(f(x))
             * ```
             *
             * where `f` is the function set here, and `g` is the function set in
             * [setFromIDFunction] Some functions have been made that may be of use, see
             * [rasteplads.util]
             *
             * @param f The function
             * @return The modified [Builder]
             */
            fun setIntoIDFunction(f: (ByteArray) -> ID): Builder<ID, Data>

            // TODO: MAKE SURE THE LENGTH IS CORRECT
            /**
             * Sets a function that converts a binary representation into an `ID`. This is what is
             * read from the other nodes. The array will always have the length [DATA_MAX_SIZE],
             * otherwise an exception will be thrown. This function should uphold have the same
             * identity when using the function set in [setFromDataFunction], meaning:
             * ```
             * x = g(f(x))
             * ```
             *
             * where `f` is the function set here, and `g` is the function set in
             * [setFromDataFunction] Some functions have been made that may be of use, see
             * [rasteplads.util]
             *
             * @param f The function
             * @return The modified [Builder]
             */
            fun setIntoDataFunction(f: (ByteArray) -> Data): Builder<ID, Data>

            /**
             * Sets a function that converts the `ID` into a binary representation. This is needed
             * for transmitting the data. The array returned should have length [ID_MAX_SIZE],
             * otherwise an exception will be thrown. This function should uphold have the same
             * identity when using the function set in [setIntoIDFunction], meaning:
             * ```
             * x = g(f(x))
             * ```
             *
             * where `f` is the function set here, and `g` is the function set in
             * [setIntoIDFunction] Some functions have been made that may be of use, see
             * [rasteplads.util]
             *
             * @param f The function
             * @return The modified [Builder]
             */
            fun setFromIDFunction(f: (ID) -> ByteArray): Builder<ID, Data>

            // TODO: MAKE SURE THE LENGTH IS CORRECT
            /**
             * Sets a function that converts the `Data` into a binary representation. This is needed
             * for transmitting the data. The array returned should have length [DATA_MAX_SIZE],
             * otherwise an exception will be thrown. This function should uphold have the same
             * identity when using the function set in [setIntoDataFunction], meaning:
             * ```
             * x = g(f(x))
             * ```
             *
             * where `f` is the function set here, and `g` is the function set in
             * [setIntoDataFunction] Some functions have been made that may be of use, see
             * [rasteplads.util]
             *
             * @param f The function
             * @return The modified [Builder]
             */
            fun setFromDataFunction(f: (Data) -> ByteArray): Builder<ID, Data>

            /**
             * Sets a constant message that will be sent out from the device at every interval (see
             * [withMsgSendInterval]). This function overrides any value set through
             * [setDataGenerator].
             *
             * @param c The data to be sent
             * @return The modified [Builder]
             */
            fun setDataConstant(c: Data): Builder<ID, Data>

            /**
             * Sets a function for generating the messages that will be sent out from the device at
             * every interval (see [withMsgSendInterval]). This function overrides any value set
             * through [setDataConstant].
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
             * [setHandleMessage]). The filters are applied in the order they are set.
             *
             * @param f The filter-function
             * @return The modified [Builder]
             */
            fun addFilterFunction(f: (ID) -> Boolean): Builder<ID, Data>

            /**
             * Adds multiple filtering functions. These functions filter messages by their ID. Only
             * IDs that return `true` from the filters will be sent called with the handle function
             * (See [setHandleMessage]). The filters are applied in the order they are set.
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
            fun withMsgTTL(t: Long): Builder<ID, Data>

            /**
             * Sets the interval between message sending sessions
             *
             * @param d Waiting time
             * @return The modified [Builder]
             */
            fun withMsgSendSessionInterval(d: Duration): Builder<ID, Data>

            /**
             * Sets the message sending interval
             *
             * @param d Waiting time
             * @return The modified [Builder]
             */
            fun withMsgSendInterval(d: Duration): Builder<ID, Data>

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
            val device: Int, // TODO SIMON
            var msgCache: MessageCache<ID>? = null,
        ) : Builder<ID, Data> {
            lateinit var callback: (ID, Data) -> Unit
            lateinit var intoID: (ByteArray) -> ID
            lateinit var intoData: (ByteArray) -> Data
            lateinit var fromID: (ID) -> ByteArray
            lateinit var fromData: (Data) -> ByteArray
            lateinit var msgData: Either<Data, () -> Data>
            lateinit var msgID: Either<ID, () -> ID>

            val filterID: MutableList<(ID) -> Boolean> = mutableListOf()

            var msgDelete: Duration? = null
            var msgTTL: Long? = null

            var msgSendSessionInterval: Duration? = null
            var msgSendInterval: Duration? = null
            var msgSendTimeout: Duration? = null
            var msgScanInterval: Duration? = null
            var msgScanDuration: Duration? = null
            var msgCacheLimit: Long? = null

            override fun build(): EventMesh<ID, Data> {
                // check(device != null) { "EventMesh Device is needed" }
                check(::callback.isInitialized) { "Function for callbacks is necessary" }
                check(::intoID.isInitialized) {
                    "Function to convert binary data into `ID` is necessary"
                }
                check(::intoData.isInitialized) {
                    "Function to convert binary data into `Data` is necessary"
                }
                check(::fromID.isInitialized) {
                    "Function to convert `ID` into binary data is necessary"
                }
                check(::fromData.isInitialized) {
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

            override fun setIntoIDFunction(f: (ByteArray) -> ID): Builder<ID, Data> {
                intoID = f
                return this
            }

            override fun setIntoDataFunction(f: (ByteArray) -> Data): Builder<ID, Data> {
                intoData = f
                return this
            }

            override fun setFromIDFunction(f: (ID) -> ByteArray): Builder<ID, Data> {
                fromID = f
                return this
            }

            override fun setFromDataFunction(f: (Data) -> ByteArray): Builder<ID, Data> {
                fromData = f
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

            override fun setHandleMessage(f: (ID, Data) -> Unit): Builder<ID, Data> {
                callback = f
                return this
            }

            override fun setIDConstant(i: ID): Builder<ID, Data> {
                msgID = Either.left(i)
                return this
            }

            override fun withMsgCacheDelete(d: Duration): Builder<ID, Data> {
                msgDelete = d
                return this
            }

            override fun withMsgTTL(t: Long): Builder<ID, Data> {
                msgTTL = t
                return this
            }

            override fun withMsgSendSessionInterval(d: Duration): Builder<ID, Data> {
                msgSendSessionInterval = d
                return this
            }

            override fun withMsgSendInterval(d: Duration): Builder<ID, Data> {
                msgSendInterval = d
                return this
            }

            override fun withMsgSendTimeout(d: Duration): Builder<ID, Data> {
                msgSendTimeout = d
                return this
            }

            override fun withMsgScanInterval(d: Duration): Builder<ID, Data> {
                msgScanInterval = d
                return this
            }

            override fun withMsgScanDuration(d: Duration): Builder<ID, Data> {
                msgScanDuration = d
                return this
            }

            override fun withMsgCache(mc: MessageCache<ID>?): Builder<ID, Data> {
                this.msgCache = mc
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
