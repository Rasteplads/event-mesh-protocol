package rasteplads.messageCache

import java.util.concurrent.LinkedBlockingQueue

class MessageCache<T>(private var cacheTimeInMilliseconds: Long) {
    private val cacheID: LinkedBlockingQueue<Pair<T, Long>> = LinkedBlockingQueue()

    fun cacheMessage(msg: T) {
        if (!cacheID.any { it.first == msg })
            cacheID.add(Pair(msg, System.currentTimeMillis() + cacheTimeInMilliseconds))

        checkForOutdatedMessages()
    }

    private fun checkForOutdatedMessages() {
        val now = System.currentTimeMillis()
        while ((cacheID.firstOrNull()?.second ?: Long.MAX_VALUE) < now) cacheID.remove()
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

    fun clearCache() = cacheID.clear()
}
