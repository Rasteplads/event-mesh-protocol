package rasteplads.api

// import kotlin.time.Duration
import java.time.Duration
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.*
import rasteplads.messageCache.MessageCache
import rasteplads.util.Either

// buffer.copyOf(position)

fun main() = runBlocking {
    println("start")

    val f =
        EventMesh.builder<Int, Byte>()
            .setDataConstant(0)
            .setIDGenerator { 10 }
            .setHandleMessage { _, _ -> }
            .setIntoIDFunction { _ -> 9 }
            .setIntoDataFunction { _ -> 0 }
            .setFromIDFunction { _ -> byteArrayOf(0, 1, 2, 3) }
            .setFromDataFunction { byteArrayOf(it) }
            .build()
    println("f")
    // val b = runBlocking {f.start()}
    // val s = launch {f.start()}
    f.start()
    delay(1000)
    println("I want it to stop")
    f.stop()
    // b.cancelAndJoin()
    // delay(1000)
    println("works I guess")
}

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
final class EventMesh<ID, Data, Device, MC : MessageCache<ID>> // TODO EventMeshDevice interface pls
private constructor(
    private val device: Device,
    private val messageCache: MC?,
    private val callback: (ID, Data) -> Unit,
    private val intoID: (ByteArray) -> ID,
    private val intoData: (ByteArray) -> Data,
    private val fromID: (ID) -> ByteArray,
    private val fromData: (Data) -> ByteArray,
    private val msgData: Either<Data, () -> Data>,
    private val msgId: Either<ID, () -> ID>,
    private val filterID: List<(ID) -> Boolean>,
) : CoroutineScope {

    // TODO: set correct default values
    /** Time a message is stored in the cache */
    private var msgDelete: Duration = Duration.ofHours(1)

    /** Time TO Live (TTL) for the messages. */
    private var msgTTL: Long = 32

    /** The interval the messages from [msgData] will be sent. Defined in seconds. */
    private var msgSendInterval: Duration = Duration.ofSeconds(60)

    /** The duration the messages from [msgData] will be sent. Defined in seconds. */
    private var msgSendDuration: Duration = Duration.ofSeconds(60)

    /** The interval incoming messages will be scanned for. Defined in seconds. */
    private var msgScanInterval: Duration = Duration.ofSeconds(60)

    /** The duration incoming messages will be scanned for. Defined in seconds. */
    private var msgScanDuration: Duration = Duration.ofSeconds(60)

    /** The maximum number of elements stored in the cache */
    private var msgCacheLimit: Long = 32

    private lateinit var btScanner: Job
    private lateinit var btSender: Job

    // TODO: MESSAGE CACHE
    // private var msgCache: MsgCache

    private constructor(
        builder: BuilderImpl<ID, Data, Device, MC>
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
        builder.msgSendInterval?.let { msgSendInterval = it }
        builder.msgSendDuration?.let { msgSendDuration = it }
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
                    while (isActive) {
                        delay(msgSendInterval.toMillis())
                    }
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
                        // print a message twice a second
                        // println("SCANNING")
                    } while (isActive)
                } finally {
                    println("STOP HW (IF LOCAL)")
                }
            }
    }

    /** TODO */
    fun stop() = runBlocking {
        if (btSender.isActive) btSender.cancelAndJoin()
        if (btScanner.isActive) btScanner.cancelAndJoin()
        println("STOP HW (IF GLOBAL)")
    }

    companion object {
        /**
         * Default value for the size of a message's data/content (when converted to buffer).
         *
         * The default value is 29.
         */
        val DATA_MAX_SIZE = 29u

        /**
         * Default value for the size of a message's ID (when converted to buffer).
         *
         * The default value is 4.
         */
        val ID_MAX_SIZE = 4u

        /**
         * Creates a [Builder] for [EventMesh] with the default message cache ([MessageCache]) and
         * [EventMeshDevice].
         *
         * @param ID The messages' ID
         * @param Data The messages' content
         */
        fun <ID, Data> builder(): Builder<ID, Data, Int, MessageCache<ID>> =
            BuilderImpl(7, MessageCache()) // TODO: REAL TYPE AND INITIALISATION

        /**
         * Creates a [Builder] for [EventMesh] with a provided message cache (set to `null` to
         * disable). Uses the default [EventMeshDevice].
         *
         * @param ID The messages' ID
         * @param Data The messages' content
         * @param MC The message cache
         * @param mc The instance of the [MC] (set to `null` to disable)
         */
        fun <ID, Data, MC : MessageCache<ID>> builder(mc: MC?): Builder<ID, Data, Int, MC> =
            BuilderImpl(7, mc) // TODO: REAL TYPE AND INITIALISATION

        /**
         * Creates a [Builder] for [EventMesh] with a provided [EventMeshDevice]. Uses the default
         * message cache ([MessageCache]).
         *
         * @param ID The messages' ID
         * @param Data The messages' content
         * @param Device The [EventMeshDevice]
         * @param device The instance of the [Device]
         */
        fun <ID, Data, Device> builder(
            device: Device
        ): Builder<ID, Data, Device, MessageCache<ID>> =
            BuilderImpl(device, MessageCache()) // TODO: REAL TYPE AND INITIALISATION

        /**
         * Creates a [Builder] for [EventMesh] with a provided message cache (set to `null` to
         * disable) and [EventMeshDevice].
         *
         * @param ID The messages' ID
         * @param Data The messages' content
         * @param Device The [EventMeshDevice]
         * @param device The instance of the [Device]
         * @param MC The message cache
         * @param mc The instance of the [MC] (set to `null` to disable)
         */
        fun <ID, Data, Device, MC : MessageCache<ID>> builder(
            device: Device,
            mc: MC?
        ): Builder<ID, Data, Device, MC> = BuilderImpl(device, mc)

        interface Builder<ID, Data, Device, MC : MessageCache<ID>> {

            /**
             * Sets a function that will be called on every message that is not filtered.
             *
             * @param f The function
             * @return the modified [Builder]]
             * @see addFilterFunction
             */
            fun setHandleMessage(f: (ID, Data) -> Unit): Builder<ID, Data, Device, MC>

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
            fun setIntoIDFunction(f: (ByteArray) -> ID): Builder<ID, Data, Device, MC>

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
            fun setIntoDataFunction(f: (ByteArray) -> Data): Builder<ID, Data, Device, MC>

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
            fun setFromIDFunction(f: (ID) -> ByteArray): Builder<ID, Data, Device, MC>

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
            fun setFromDataFunction(f: (Data) -> ByteArray): Builder<ID, Data, Device, MC>

            /**
             * Sets a constant message that will be sent out from the device at every interval (see
             * [withMsgSendInterval]). This function overrides any value set through
             * [setDataGenerator].
             *
             * @param c The data to be sent
             * @return The modified [Builder]
             */
            fun setDataConstant(c: Data): Builder<ID, Data, Device, MC>

            /**
             * Sets a function for generating the messages that will be sent out from the device at
             * every interval (see [withMsgSendInterval]). This function overrides any value set
             * through [setDataConstant].
             *
             * @param f The generator-function
             * @return The modified [Builder]
             */
            fun setDataGenerator(f: () -> Data): Builder<ID, Data, Device, MC>

            /**
             * Sets a constant `ID`. This `ID` will be used as the `ID` for each message, and will
             * be stored by every node for some time (See [withMsgCacheDelete]). This overrides any
             * value specified with [setIDGenerator].
             *
             * @param i `ID`
             * @return The modified [Builder]
             */
            fun setIDConstant(i: ID): Builder<ID, Data, Device, MC>

            /**
             * Sets a function for generating `ID`s. This function acts as the `ID` for each
             * message, and will be stored by every node for some time (See [withMsgCacheDelete]).
             * The `ID` should be unique for at least the duration which it can be saved. This
             * overrides any value specified with [setIDConstant].
             *
             * @param f The generator-function
             * @return The modified [Builder]
             */
            fun setIDGenerator(f: () -> ID): Builder<ID, Data, Device, MC>

            /**
             * Adds a filtering function. These functions filter messages by their ID. Only IDs that
             * return `true` from the filters will be sent called with the handle function (See
             * [setHandleMessage]). The filters are applied in the order they are set.
             *
             * @param f The filter-function
             * @return The modified [Builder]
             */
            fun addFilterFunction(f: (ID) -> Boolean): Builder<ID, Data, Device, MC>

            /**
             * Adds multiple filtering functions. These functions filter messages by their ID. Only
             * IDs that return `true` from the filters will be sent called with the handle function
             * (See [setHandleMessage]). The filters are applied in the order they are set.
             *
             * @param fs The filter-functions
             * @return The modified [Builder]
             */
            fun addFilterFunction(vararg fs: (ID) -> Boolean): Builder<ID, Data, Device, MC>

            /**
             * Sets the time a message ID should be saved in the cache. This is to reduce the number
             * of redundant relays, since relaying the same message several times is not needed.
             *
             * @param d Duration the message should be saved
             * @return The modified [Builder]
             */
            fun withMsgCacheDelete(d: Duration): Builder<ID, Data, Device, MC>

            /**
             * Sets the message Time TO Live (TTL). This number denotes how many times a node can
             * relay the message in the network.
             *
             * @param t Number of relays
             * @return The modified [Builder]
             */
            fun withMsgTTL(t: Long): Builder<ID, Data, Device, MC>

            /**
             * Sets the message sending interval
             *
             * @param d Waiting time
             * @return The modified [Builder]
             */
            fun withMsgSendInterval(d: Duration): Builder<ID, Data, Device, MC>

            /**
             * Sets the sending duration. This is the time duration the message defined in either
             * [setDataConstant] or [setDataGenerator] will be transmitted.
             *
             * @param d Sending time
             * @return The modified [Builder]
             */
            fun withMsgSendDuration(d: Duration): Builder<ID, Data, Device, MC>

            /**
             * Sets the scanning interval.
             *
             * @param d Waiting time
             * @return The modified [Builder]
             */
            fun withMsgScanInterval(d: Duration): Builder<ID, Data, Device, MC>

            /**
             * Sets the scanning duration.
             *
             * @param d Scanning time
             * @return The modified [Builder]
             */
            fun withMsgScanDuration(d: Duration): Builder<ID, Data, Device, MC>

            /**
             * Sets a message cache. If `null`, it disables the message cache
             *
             * @param b Boolean enabling/disabling the message cache
             * @return The modified [Builder]
             */
            fun withMsgCache(b: MC?): Builder<ID, Data, Device, MC>

            /**
             * Sets the limit of elements saved in the message cache
             *
             * @param l Limit
             * @return The modified [Builder]
             */
            fun withMsgCacheLimit(l: Long): Builder<ID, Data, Device, MC>

            /*
            /**
             * Sets the [EventMeshDevice] used.
             *
             * @param d Device
             * @return The modified [Builder]
             */
            fun withDevice(d: Device): Builder<ID, Data, Device, MC>
            */

            /**
             * Builds the [Builder]
             *
             * @return [EventMesh] with the needed properties
             * @throws IllegalStateException If the needed variables hasn't been set
             */
            fun build(): EventMesh<ID, Data, Device, MC>
        }

        private class BuilderImpl<ID, Data, Device, MC : MessageCache<ID>>(
            val device: Device,
            var msgCache: MC? = null,
        ) : Builder<ID, Data, Device, MC> {
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

            var msgSendInterval: Duration? = null
            var msgSendDuration: Duration? = null
            var msgScanInterval: Duration? = null
            var msgScanDuration: Duration? = null
            var msgCacheLimit: Long? = null

            override fun build(): EventMesh<ID, Data, Device, MC> {
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

            override fun setIntoIDFunction(f: (ByteArray) -> ID): Builder<ID, Data, Device, MC> {
                intoID = f
                return this
            }

            override fun setIntoDataFunction(
                f: (ByteArray) -> Data
            ): Builder<ID, Data, Device, MC> {
                intoData = f
                return this
            }

            override fun setFromIDFunction(f: (ID) -> ByteArray): Builder<ID, Data, Device, MC> {
                fromID = f
                return this
            }

            override fun setFromDataFunction(
                f: (Data) -> ByteArray
            ): Builder<ID, Data, Device, MC> {
                fromData = f
                return this
            }

            override fun setDataConstant(c: Data): Builder<ID, Data, Device, MC> {
                msgData = Either.left(c)
                return this
            }

            override fun setDataGenerator(f: () -> Data): Builder<ID, Data, Device, MC> {
                msgData = Either.right(f)
                return this
            }

            override fun addFilterFunction(f: (ID) -> Boolean): Builder<ID, Data, Device, MC> {
                filterID.add(f)
                return this
            }

            override fun addFilterFunction(
                vararg fs: (ID) -> Boolean
            ): Builder<ID, Data, Device, MC> {
                filterID.addAll(fs)
                return this
            }

            override fun setIDGenerator(f: () -> ID): Builder<ID, Data, Device, MC> {
                msgID = Either.right(f)
                return this
            }

            override fun setHandleMessage(f: (ID, Data) -> Unit): Builder<ID, Data, Device, MC> {
                callback = f
                return this
            }

            override fun setIDConstant(i: ID): Builder<ID, Data, Device, MC> {
                msgID = Either.left(i)
                return this
            }

            override fun withMsgCacheDelete(d: Duration): Builder<ID, Data, Device, MC> {
                msgDelete = d
                return this
            }

            override fun withMsgTTL(t: Long): Builder<ID, Data, Device, MC> {
                msgTTL = t
                return this
            }

            override fun withMsgSendInterval(d: Duration): Builder<ID, Data, Device, MC> {
                msgSendInterval = d
                return this
            }

            override fun withMsgSendDuration(d: Duration): Builder<ID, Data, Device, MC> {
                msgSendDuration = d
                return this
            }

            override fun withMsgScanInterval(d: Duration): Builder<ID, Data, Device, MC> {
                msgScanInterval = d
                return this
            }

            override fun withMsgScanDuration(d: Duration): Builder<ID, Data, Device, MC> {
                msgScanDuration = d
                return this
            }

            override fun withMsgCache(mc: MC?): Builder<ID, Data, Device, MC> {
                this.msgCache = mc
                return this
            }

            override fun withMsgCacheLimit(l: Long): Builder<ID, Data, Device, MC> {
                msgCacheLimit = l
                return this
            }

            /*
            override fun withDevice(d: Device): Builder<ID, Data, Device, MC> {
                device = d
                return this
            }

             */
        }
    }

    override val coroutineContext: CoroutineContext
        get() = EmptyCoroutineContext
}
