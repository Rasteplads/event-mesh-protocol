package org.example.api

import org.example.util.Either



/**
 * Default value for the size of a message's data/content (when converted to buffer).
 *
 * The default value is 29
 */
private const val DATA_MAX_SIZE = 29u

/**
 * Default value for the size of a message's ID (when converted to buffer).
 *
 * The default value is 4
 */
private const val ID_MAX_SIZE = 4u

// buffer.copyOf(position)

/**
 * @author t-lohse
 *
 * TODO: WRITE
 */
final class FatClass<ID, Data>
private constructor(
    private val callback: (ID, Data) -> Unit,
    private val intoID: (ByteArray) -> ID, // Should we make custom structs for the Binary IDs and Datas?
    private val intoData: (ByteArray) -> Data,
    private val fromID: (ID) -> ByteArray,
    private val fromData: (Data) -> ByteArray,
    private val msgData: Either<Data, () -> Data>,
    private val msgId: () -> ID,
    private val filterID: List<(ID) -> Boolean>
) {
    /**
     * Time a message is stored in the cache. Defined in seconds.
     */
    private var msgDelete: UInt = 600u

    /**
     * Time TO Live (TTL) for the messages.
     */
    private var msgTTL: UInt = 32u

    /**
     * The interval the messages from [msgData] will be sent. Defined in seconds.
     */
    private var msgSendInterval: UInt = 32u

    /**
     * The duration the messages from [msgData] will be sent. Defined in seconds.
     */
    private var msgSendDuration: UInt = 32u

    /**
     * The interval incoming messages will be scanned for. Defined in seconds.
     */
    private var msgScanInterval: UInt = 32u

    /**
     * The duration incoming messages will be scanned for. Defined in seconds.
     */
    private var msgScanDuration: UInt = 32u

    // TODO: MESSAGE CACHE

    private constructor(
        builder: BuilderImpl<ID, Data>
    ) : this(
        builder.callback,
        builder.intoID,
        builder.intoData,
        builder.fromID,
        builder.fromData,
        builder.msgData,
        builder.msgID,
        builder.filterID) {
        builder.msgDelete?.let { msgDelete = it }
        builder.msgTTL?.let { msgTTL = it }
        builder.msgSendInterval?.let { msgSendInterval = it }
        builder.msgSendDuration?.let { msgSendDuration = it }
        builder.msgScanInterval?.let { msgScanInterval = it }
        builder.msgScanDuration?.let { msgScanDuration = it }
    }

    /** Executes (lol) */
    suspend fun execute() {
        // TODO: EXEC IN BG
    }

    companion object {
        /**
         * Creates a [Builder] for [FatClass]
         *
         * @param ID The messages' ID
         * @param Data The messages' content
         */
        fun <ID, Data> builder(): Builder<ID, Data> = BuilderImpl()

        interface Builder<ID, Data> {

            /**
             * Sets a function that will be called on every message that is not filtered (See
             * [addFilterFunction]).
             *
             * @param f The function
             * @return the modified [Builder]]
             */
            fun withHandleMessage(f: (ID, Data) -> Unit): Builder<ID, Data>

            /**
             * Sets a function that converts a binary representation into an `ID`. This is what is
             * read from the other nodes. The array will always have the length [ID_MAX_SIZE],
             * otherwise an exception will be thrown. This function should uphold have the same
             * identity when using the function set in [withFromIDFunction], meaning:
             * ```
             * x = g(f(x))
             * ```
             *
             * where `f` is the function set here, and `g` is the function set in
             * [withFromIDFunction] Some functions have been made that may be of use, see
             * [org.example.util]
             *
             * @param f The function
             * @return The modified [Builder]
             */
            fun withIntoIDFunction(f: (ByteArray) -> ID): Builder<ID, Data>

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
             * [withFromDataFunction] Some functions have been made that may be of use, see
             * [org.example.util]
             *
             * @param f The function
             * @return The modified [Builder]
             */
            fun withIntoDataFunction(f: (ByteArray) -> Data): Builder<ID, Data>

            /**
             * Sets a function that converts the `ID` into a binary representation. This is needed
             * for transmitting the data. The array returned should have length [ID_MAX_SIZE],
             * otherwise an exception will be thrown. This function should uphold have the same
             * identity when using the function set in [withIntoIDFunction], meaning:
             * ```
             * x = g(f(x))
             * ```
             *
             * where `f` is the function set here, and `g` is the function set in
             * [withIntoIDFunction] Some functions have been made that may be of use, see
             * [org.example.util]
             *
             * @param f The function
             * @return The modified [Builder]
             */
            fun withFromIDFunction(f: (ID) -> ByteArray): Builder<ID, Data>

            // TODO: MAKE SURE THE LENGTH IS CORRECT
            /**
             * Sets a function that converts the `Data` into a binary representation. This is needed
             * for transmitting the data. The array returned should have length [DATA_MAX_SIZE],
             * otherwise an exception will be thrown. This function should uphold have the same
             * identity when using the function set in [withIntoDataFunction], meaning:
             * ```
             * x = g(f(x))
             * ```
             *
             * where `f` is the function set here, and `g` is the function set in
             * [withIntoDataFunction] Some functions have been made that may be of use, see
             * [org.example.util]
             *
             * @param f The function
             * @return The modified [Builder]
             */
            fun withFromDataFunction(f: (Data) -> ByteArray): Builder<ID, Data>

            /**
             * Sets a constant message that will be sent out from the device at every interval (see
             * [withMsgSendInterval]). This function overrides any value set through
             * [withDataGenerator].
             *
             * @param c The data to be sent
             * @return The modified [Builder]
             */
            fun withDataConstant(c: Data): Builder<ID, Data>

            /**
             * Sets a function for generating the messages that will be sent out from the devic at
             * every interval (see [withMsgSendInterval]). This function overrides any value set
             * through [withDataConstant].
             *
             * @param f The generator-function
             * @return The modified [Builder]
             */
            fun withDataGenerator(f: () -> Data): Builder<ID, Data>

            /**
             * Sets a function for generating `ID`s. This function acts as the ID for each message,
             * and will be stored by every node for some time (See [withMsgCacheDelete]). The ID
             * should be unique for at least the duration which it can be saved.
             *
             * @param f The generator-function
             * @return The modified [Builder]
             */
            fun withIDGenerator(f: () -> ID): Builder<ID, Data>

            /**
             * Adds a filtering function. These functions filter messages by their ID. Only IDs that
             * return `true` from the filters will be sent called with the handle function (See
             * [withHandleCallback]). The filters are applied in the order they are set.
             *
             * @param f The filter-function
             * @return The modified [Builder]
             */
            fun addFilterFunction(f: (ID) -> Boolean): Builder<ID, Data>

            /**
             * Adds multiple filtering functions. These functions filter messages by their ID. Only
             * IDs that return `true` from the filters will be sent called with the handle function
             * (See [withHandleCallback]). The filters are applied in the order they are set.
             *
             * @param fs The filter-functions
             * @return The modified [Builder]
             */
            fun addFilterFunction(vararg fs: (ID) -> Boolean): Builder<ID, Data>

            /**
             * Sets the time a message ID should be saved in the cache. This is to reduce the number
             * of redundant relays, since relaying the same message several times is not needed.
             *
             * @param t Time the message should be saved (seconds)
             * @return The modified [Builder]
             */
            fun withMsgCacheDelete(t: UInt): Builder<ID, Data>

            /**
             * Sets the message Time TO Live (TTL). This number denotes how many times a node can
             * relay the message in the network.
             *
             * @param t Number of relays
             * @return The modified [Builder]
             */
            fun withMsgTTL(t: UInt): Builder<ID, Data>

            /**
             * Sets the message sending interval (in seconds)
             *
             * @param t Waiting time
             * @return The modified [Builder]
             */
            fun withMsgSendInterval(t: UInt): Builder<ID, Data>

            /**
             * Sets the sending duration (in seconds). This is the time duration the message defined
             * in either [withDataConstant] or [withDataGenerator] will be transmitted
             *
             * @param t Sending time
             * @return The modified [Builder]
             */
            fun withMsgSendDuration(t: UInt): Builder<ID, Data>

            /**
             * Sets the scanning interval (in seconds)
             *
             * @param t Waiting time
             * @return The modified [Builder]
             */
            fun withMsgScanInterval(t: UInt): Builder<ID, Data>
            /**
             * Sets the scanning duration (in seconds)
             *
             * @param t Scanning time
             * @return The modified [Builder]
             */
            fun withMsgScanDuration(t: UInt): Builder<ID, Data>

            /**
             * Builds the [Builder]
             *
             * @return [FatClass] with the needed properties
             * @throws IllegalStateException If the needed variables hasn't been set
             */
            fun build(): FatClass<ID, Data>
        }

        private class BuilderImpl<ID, Data> : Builder<ID, Data> {
            lateinit var callback: (ID, Data) -> Unit
            lateinit var intoID: (ByteArray) -> ID
            lateinit var intoData: (ByteArray) -> Data
            lateinit var fromID: (ID) -> ByteArray
            lateinit var fromData: (Data) -> ByteArray
            lateinit var msgData: Either<Data, () -> Data>
            lateinit var msgID: () -> ID

            val filterID: MutableList<(ID) -> Boolean> = mutableListOf()

            var msgDelete: UInt? = null
            var msgTTL: UInt? = null

            var msgSendInterval: UInt? = null
            var msgSendDuration: UInt? = null
            var msgScanInterval: UInt? = null
            var msgScanDuration: UInt? = null

            override fun build(): FatClass<ID, Data> {
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
                    "Either a constant or a generator function for the advertising `ID` must be set"
                }

                return FatClass(this)
            }

            override fun withIntoIDFunction(f: (ByteArray) -> ID): Builder<ID, Data> {
                intoID = f
                return this
            }

            override fun withIntoDataFunction(f: (ByteArray) -> Data): Builder<ID, Data> {
                intoData = f
                return this
            }

            override fun withFromIDFunction(f: (ID) -> ByteArray): Builder<ID, Data> {
                fromID = f
                return this
            }

            override fun withFromDataFunction(f: (Data) -> ByteArray): Builder<ID, Data> {
                fromData = f
                return this
            }

            override fun withDataConstant(c: Data): Builder<ID, Data> {
                msgData = Either.left(c)
                return this
            }

            override fun withDataGenerator(f: () -> Data): Builder<ID, Data> {
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

            override fun withIDGenerator(f: () -> ID): Builder<ID, Data> {
                msgID = f
                return this
            }

            override fun withHandleMessage(f: (ID, Data) -> Unit): Builder<ID, Data> {
                callback = f
                return this
            }

            override fun withMsgCacheDelete(t: UInt): Builder<ID, Data> {
                msgDelete = t
                return this
            }

            override fun withMsgTTL(t: UInt): Builder<ID, Data> {
                msgTTL = t
                return this
            }

            override fun withMsgSendInterval(t: UInt): Builder<ID, Data> {
                msgSendInterval = t
                return this
            }

            override fun withMsgSendDuration(t: UInt): Builder<ID, Data> {
                msgSendDuration = t
                return this
            }

            override fun withMsgScanInterval(t: UInt): Builder<ID, Data> {
                msgScanInterval = t
                return this
            }

            override fun withMsgScanDuration(t: UInt): Builder<ID, Data> {
                msgScanDuration = t
                return this
            }
        }
    }
}
