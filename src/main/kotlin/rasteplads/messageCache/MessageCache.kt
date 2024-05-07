package rasteplads.messageCache

import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque

class MessageCache<T>(private var cacheTimeInMilliseconds: Long) {
    private val cacheID: ConcurrentLinkedDeque<Pair<T, Long>> =
        ConcurrentLinkedDeque() // AtomicReference(ArrayDeque())

    fun cacheMessage(msg: T) {

        if (!cacheID.any { it.first == msg })
            cacheID.add(Pair(msg, System.currentTimeMillis() + cacheTimeInMilliseconds))
        // cacheID.add(Pair(msg, LocalTime.now().plusSeconds(cacheTimeInSeconds)))

        checkForOutdatedMessages()
    }

    private fun checkForOutdatedMessages() {
        // val time = LocalTime.now()
        // While message exceeded its cacheTime remove them
        // while (cacheID.peek()?.second?.isBefore(time) == true) cacheID.remove()
        val now = System.currentTimeMillis()
        while ((cacheID.peek()?.second ?: Long.MAX_VALUE) < now) cacheID.remove()
    }

    fun changeCacheTime(cacheTime: Long) {
        cacheTimeInMilliseconds = cacheTime
    }

    fun containsMessage(msg: T): Boolean {
        checkForOutdatedMessages()
        return cacheID.any { it.first == msg }
    }

    fun getSize(): Int = cacheID.size

    override fun toString() =
        cacheID.fold("[") { acc, e -> "$acc, (${e.first}: ${e.second})" } + "]"

    fun clearCache() {
        cacheID.clear()
    }
}
