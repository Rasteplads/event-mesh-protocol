package org.example.cache

import java.time.LocalTime
import java.util.*

class MessageCache<T>(private val cacheTimeInSeconds: Long) {
    private val cacheID: Queue<Pair<T, LocalTime>> = ArrayDeque()

    fun cacheMessage(msg: T) {
        if (!cacheID.any { it.first == msg })
            cacheID.add(Pair(msg, LocalTime.now().plusSeconds(cacheTimeInSeconds)))

        checkForOutdatedMessages()
    }

    private fun checkForOutdatedMessages() {
        val msg = cacheID.iterator()
        val time = LocalTime.now()
        while (msg.next().second.compareTo(time) ==
            -1) { // While message exceeded its cacheTime remove them
            msg.remove()
        }
    }

    fun containsMessage(msg: T): Boolean = cacheID.any { it.first == msg }

    fun getSize(): Int = cacheID.size

    override fun toString() = cacheID.fold("[", { acc, e -> "$acc, ${e.first}: ${e.second}" }) + "]"

    fun clearCache() {
        cacheID.clear()
    }
}
