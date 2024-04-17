package org.example.cache

import java.time.LocalTime
import java.util.*

class MessageCache<T> (private val cacheTime: Long){
    private val cacheID: Queue<Pair<T, LocalTime>> = ArrayDeque()



    fun cacheMessage(msg: T) {
        if (!cacheID.any { it.first == msg })
            cacheID.add(Pair(msg, LocalTime.now().plusMinutes(cacheTime)))

        checkForOutdatedMessages()
    }

    fun checkForOutdatedMessages() {
        val msg = cacheID.iterator()

        while (msg.next().second.compareTo(LocalTime.now()) == -1) {// While message exceeded its cacheTime remove them
            msg.remove()
        }
    }

    fun containsMessage(msg: T): Boolean = cacheID.any { it.first == msg }


    fun getSize(): Int = cacheID.size


    override fun toString() = cacheID.fold( "[", {acc, e -> "$acc, ${e.first}: ${e.second}"}) + "]"


    fun clearCache() {
        cacheID.clear()
    }
}
