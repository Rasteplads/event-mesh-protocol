package rasteplads.api

import rasteplads.util.Either

/**
 * Default value for the size of a message's data/content (when converted to buffer).
 *
 * The default value is 29.
 */
private const val DATA_MAX_SIZE = 29u

/**
 * Default value for the size of a message's ID (when converted to buffer).
 *
 * The default value is 4.
 */
private const val ID_MAX_SIZE = 4u

// buffer.copyOf(position)

/**
 * @author t-lohse
 *
 * TODO: WRITE
 */
final class EventMesh<ID, Data, MC> // TODO: cache derive
private constructor(
    private val callback: (ID, Data) -> Unit,
    // Should we make custom structs for the Binary ID and Data? (replace ByteArray)
    private val intoID: (ByteArray) -> ID,
    private val intoData: (ByteArray) -> Data,
    private val fromID: (ID) -> ByteArray,
    private val fromData: (Data) -> ByteArray,
    private val msgData: Either<Data, () -> Data>,
    private val msgId: Either<ID, () -> ID>,
    private val filterID: List<(ID) -> Boolean>,
    private val messageCache: MC
) {

    // TODO: set correct default values
    /** Time a message is stored in the cache. Defined in seconds. */
    private var msgDelete: UInt = 600u

    /** Time TO Live (TTL) for the messages. */
    private var msgTTL: UInt = 32u

    /** The interval the messages from [msgData] will be sent. Defined in seconds. */
    private var msgSendInterval: UInt = 32u

    /** The duration the messages from [msgData] will be sent. Defined in seconds. */
    private var msgSendDuration: UInt = 32u

    /** The interval incoming messages will be scanned for. Defined in seconds. */
    private var msgScanInterval: UInt = 32u

    /** The duration incoming messages will be scanned for. Defined in seconds. */
    private var msgScanDuration: UInt = 32u

    /** The maximum number of elements stored in the cache */
    private var msgCacheLimit: UInt = 32u

    // TODO: MESSAGE CACHE
    // private var msgCache: MsgCache

    private constructor(
        builder: BuilderImpl<ID, Data, MC>
    ) : this(
        builder.callback,
        builder.intoID,
        builder.intoData,
        builder.fromID,
        builder.fromData,
        builder.msgData,
        builder.msgID,
        builder.filterID,
        builder.msgCache) {
        builder.msgDelete?.let { msgDelete = it }
        builder.msgTTL?.let { msgTTL = it }
        builder.msgSendInterval?.let { msgSendInterval = it }
        builder.msgSendDuration?.let { msgSendDuration = it }
        builder.msgScanInterval?.let { msgScanInterval = it }
        builder.msgScanDuration?.let { msgScanDuration = it }
        builder.msgCacheLimit?.let { msgCacheLimit = it }
    }

    /** Executes (lol) */
    suspend fun start() {
        // TODO: EXEC IN BG
    }

    companion object {
        /**
         * Creates a [Builder] for [EventMesh]
         *
         * @param ID The messages' ID
         * @param Data The messages' content
         */
        fun <ID, Data> builder(): Builder<ID, Data, Int> = BuilderImpl(7) // TODO: Default Class

        fun <ID, Data, MC> builder(mc: MC): Builder<ID, Data, MC> = BuilderImpl(mc)

        interface Builder<ID, Data, MC> {

            /**
             * Sets a function that will be called on every message that is not filtered (See
             * [addFilterFunction]).
             *
             * @param f The function
             * @return the modified [Builder]]
             */
            fun setHandleMessage(f: (ID, Data) -> Unit): Builder<ID, Data, MC>

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
             * [org.rasteplads.util]
             *
             * @param f The function
             * @return The modified [Builder]
             */
            fun setIntoIDFunction(f: (ByteArray) -> ID): Builder<ID, Data, MC>

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
             * [org.rasteplads.util]
             *
             * @param f The function
             * @return The modified [Builder]
             */
            fun setIntoDataFunction(f: (ByteArray) -> Data): Builder<ID, Data, MC>

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
             * [org.rasteplads.util]
             *
             * @param f The function
             * @return The modified [Builder]
             */
            fun setFromIDFunction(f: (ID) -> ByteArray): Builder<ID, Data, MC>

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
             * [org.rasteplads.util]
             *
             * @param f The function
             * @return The modified [Builder]
             */
            fun setFromDataFunction(f: (Data) -> ByteArray): Builder<ID, Data, MC>

            /**
             * Sets a constant message that will be sent out from the device at every interval (see
             * [withMsgSendInterval]). This function overrides any value set through
             * [setDataGenerator].
             *
             * @param c The data to be sent
             * @return The modified [Builder]
             */
            fun setDataConstant(c: Data): Builder<ID, Data, MC>

            /**
             * Sets a function for generating the messages that will be sent out from the device at
             * every interval (see [withMsgSendInterval]). This function overrides any value set
             * through [setDataConstant].
             *
             * @param f The generator-function
             * @return The modified [Builder]
             */
            fun setDataGenerator(f: () -> Data): Builder<ID, Data, MC>

            /**
             * Sets a constant `ID`. This `ID` will be used as the `ID` for each message, and will
             * be stored by every node for some time (See [withMsgCacheDelete]). This overrides any
             * value specified with [setIDGenerator].
             *
             * @param i `ID`
             * @return The modified [Builder]
             */
            fun setIDConstant(i: ID): Builder<ID, Data, MC>

            /**
             * Sets a function for generating `ID`s. This function acts as the `ID` for each
             * message, and will be stored by every node for some time (See [withMsgCacheDelete]).
             * The `ID` should be unique for at least the duration which it can be saved. This
             * overrides any value specified with [setIDConstant].
             *
             * @param f The generator-function
             * @return The modified [Builder]
             */
            fun setIDGenerator(f: () -> ID): Builder<ID, Data, MC>

            /**
             * Adds a filtering function. These functions filter messages by their ID. Only IDs that
             * return `true` from the filters will be sent called with the handle function (See
             * [setHandleMessage]). The filters are applied in the order they are set.
             *
             * @param f The filter-function
             * @return The modified [Builder]
             */
            fun addFilterFunction(f: (ID) -> Boolean): Builder<ID, Data, MC>

            /**
             * Adds multiple filtering functions. These functions filter messages by their ID. Only
             * IDs that return `true` from the filters will be sent called with the handle function
             * (See [setHandleMessage]). The filters are applied in the order they are set.
             *
             * @param fs The filter-functions
             * @return The modified [Builder]
             */
            fun addFilterFunction(vararg fs: (ID) -> Boolean): Builder<ID, Data, MC>

            /**
             * Sets the time a message ID should be saved in the cache. This is to reduce the number
             * of redundant relays, since relaying the same message several times is not needed.
             *
             * @param t Time the message should be saved (seconds)
             * @return The modified [Builder]
             */
            fun withMsgCacheDelete(t: UInt): Builder<ID, Data, MC>

            /**
             * Sets the message Time TO Live (TTL). This number denotes how many times a node can
             * relay the message in the network.
             *
             * @param t Number of relays
             * @return The modified [Builder]
             */
            fun withMsgTTL(t: UInt): Builder<ID, Data, MC>

            /**
             * Sets the message sending interval (in seconds)
             *
             * @param t Waiting time
             * @return The modified [Builder]
             */
            fun withMsgSendInterval(t: UInt): Builder<ID, Data, MC>

            /**
             * Sets the sending duration (in seconds). This is the time duration the message defined
             * in either [setDataConstant] or [setDataGenerator] will be transmitted
             *
             * @param t Sending time
             * @return The modified [Builder]
             */
            fun withMsgSendDuration(t: UInt): Builder<ID, Data, MC>

            /**
             * Sets the scanning interval (in seconds)
             *
             * @param t Waiting time
             * @return The modified [Builder]
             */
            fun withMsgScanInterval(t: UInt): Builder<ID, Data, MC>

            /**
             * Sets the scanning duration (in seconds)
             *
             * @param t Scanning time
             * @return The modified [Builder]
             */
            fun withMsgScanDuration(t: UInt): Builder<ID, Data, MC>

            /**
             * @param l Limit
             * @return The modified [Builder]
             *
             * TODO: LINK TO MESSAGE CACHE TYPE Sets the limit of elements saved in the message
             *   cache
             */
            fun withMsgCacheLimit(l: UInt): Builder<ID, Data, MC>

            /**
             * Builds the [Builder]
             *
             * @return [EventMesh] with the needed properties
             * @throws IllegalStateException If the needed variables hasn't been set
             */
            fun build(): EventMesh<ID, Data, MC>
        }

        // TODO: Do default MC
        private class BuilderImpl<ID, Data, MC>(val msgCache: MC) : Builder<ID, Data, MC> {
            lateinit var callback: (ID, Data) -> Unit
            lateinit var intoID: (ByteArray) -> ID
            lateinit var intoData: (ByteArray) -> Data
            lateinit var fromID: (ID) -> ByteArray
            lateinit var fromData: (Data) -> ByteArray
            lateinit var msgData: Either<Data, () -> Data>
            lateinit var msgID: Either<ID, () -> ID>

            val filterID: MutableList<(ID) -> Boolean> = mutableListOf()

            var msgDelete: UInt? = null
            var msgTTL: UInt? = null

            var msgSendInterval: UInt? = null
            var msgSendDuration: UInt? = null
            var msgScanInterval: UInt? = null
            var msgScanDuration: UInt? = null
            var msgCacheLimit: UInt? = null

            override fun build(): EventMesh<ID, Data, MC> {
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

            override fun setIntoIDFunction(f: (ByteArray) -> ID): Builder<ID, Data, MC> {
                intoID = f
                return this
            }

            override fun setIntoDataFunction(f: (ByteArray) -> Data): Builder<ID, Data, MC> {
                intoData = f
                return this
            }

            override fun setFromIDFunction(f: (ID) -> ByteArray): Builder<ID, Data, MC> {
                fromID = f
                return this
            }

            override fun setFromDataFunction(f: (Data) -> ByteArray): Builder<ID, Data, MC> {
                fromData = f
                return this
            }

            override fun setDataConstant(c: Data): Builder<ID, Data, MC> {
                msgData = Either.left(c)
                return this
            }

            override fun setDataGenerator(f: () -> Data): Builder<ID, Data, MC> {
                msgData = Either.right(f)
                return this
            }

            override fun addFilterFunction(f: (ID) -> Boolean): Builder<ID, Data, MC> {
                filterID.add(f)
                return this
            }

            override fun addFilterFunction(vararg fs: (ID) -> Boolean): Builder<ID, Data, MC> {
                filterID.addAll(fs)
                return this
            }

            override fun setIDGenerator(f: () -> ID): Builder<ID, Data, MC> {
                msgID = Either.right(f)
                return this
            }

            override fun setHandleMessage(f: (ID, Data) -> Unit): Builder<ID, Data, MC> {
                callback = f
                return this
            }

            override fun setIDConstant(i: ID): Builder<ID, Data, MC> {
                msgID = Either.left(i)
                return this
            }

            override fun withMsgCacheDelete(t: UInt): Builder<ID, Data, MC> {
                msgDelete = t
                return this
            }

            override fun withMsgTTL(t: UInt): Builder<ID, Data, MC> {
                msgTTL = t
                return this
            }

            override fun withMsgSendInterval(t: UInt): Builder<ID, Data, MC> {
                msgSendInterval = t
                return this
            }

            override fun withMsgSendDuration(t: UInt): Builder<ID, Data, MC> {
                msgSendDuration = t
                return this
            }

            override fun withMsgScanInterval(t: UInt): Builder<ID, Data, MC> {
                msgScanInterval = t
                return this
            }

            override fun withMsgScanDuration(t: UInt): Builder<ID, Data, MC> {
                msgScanDuration = t
                return this
            }

            override fun withMsgCacheLimit(l: UInt): Builder<ID, Data, MC> {
                msgCacheLimit = l
                return this
            }
        }
    }
}
