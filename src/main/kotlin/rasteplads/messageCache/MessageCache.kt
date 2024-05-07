package rasteplads.messageCache

import java.util.*
import java.util.concurrent.atomic.AtomicReference

class MessageCache<T>(private var cacheTimeInMilliseconds: Long) {
    private val cacheID: AtomicReference<Queue<Pair<T, Long>>> = AtomicReference(ArrayDeque())

    fun cacheMessage(msg: T) {
        if (!cacheID.get().any { it.first == msg })
            cacheID.get().add(Pair(msg, System.currentTimeMillis() + cacheTimeInMilliseconds))
        // cacheID.add(Pair(msg, LocalTime.now().plusSeconds(cacheTimeInSeconds)))

        checkForOutdatedMessages()
    }

    private fun checkForOutdatedMessages() {
        // val time = LocalTime.now()
        // While message exceeded its cacheTime remove them
        // while (cacheID.peek()?.second?.isBefore(time) == true) cacheID.remove()
        val now = System.currentTimeMillis()
        while ((cacheID.get().peek()?.second ?: Long.MAX_VALUE) < now) cacheID.get().remove()
    }

    fun changeCacheTime(cacheTime: Long) {
        cacheTimeInMilliseconds = cacheTime
    }

    fun containsMessage(msg: T): Boolean {
        checkForOutdatedMessages()
        return cacheID.get().any { it.first == msg }
    }

    fun getSize(): Int = cacheID.get().size

    override fun toString() =
        cacheID.get().fold("[") { acc, e -> "$acc, (${e.first}: ${e.second})" } + "]"

    fun clearCache() {
        cacheID.get().clear()
    }
}
