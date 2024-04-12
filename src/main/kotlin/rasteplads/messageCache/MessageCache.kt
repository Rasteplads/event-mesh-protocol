package rasteplads.messageCache

import java.time.LocalTime

class MessageCache<T> {
    val cacheTime: Long = 2
    private val cache = LinkedHashMap<Int, Pair<LocalTime, T>>()

    fun cacheMessage(key: Int, msg: T) {
        if (!cache.containsKey(key)) cache[key] = Pair(LocalTime.now().plusMinutes(cacheTime), msg)

        checkForOutdatedMessages()
    }

    fun checkForOutdatedMessages() {
        val oldmsg = mutableListOf<Int>()
        for (msg in cache.iterator()) {
            when (msg.value.first.compareTo(LocalTime.now())) {
                -1 -> { // Message exceeded its cacheTime and needs to be removed
                    oldmsg.add(msg.key)
                }
                else -> { // The rest of the messages hasn't exceeded its cacheTime
                    break
                }
            }
        }
        removeCachedMessage(oldmsg)
    }

    fun getCachedMessage(key: Int): Pair<LocalTime, T>? {
        return cache[key]
    }

    fun printAllMessages() {
        val cacheItr = cache.iterator()

        while (cacheItr.hasNext()) println(cacheItr.next())
    }

    fun removeCachedMessage(oldmsg: List<Int>) {
        for (m in oldmsg) {
            cache.remove(m)
        }
    }

    fun clearCache() {
        cache.clear()
    }
}
