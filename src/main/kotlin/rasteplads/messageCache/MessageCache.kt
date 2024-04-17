package org.example.cache

import java.time.LocalTime
import java.util.*

class MessageCache<T> {
    val cacheTime: Long = 1
    private val cacheID: Queue<Pair<T, LocalTime>> = ArrayDeque()

    fun cacheMessage(msg: T) {
        if (!cacheID.any { it.first == msg })
            cacheID.add(Pair(msg, LocalTime.now().plusMinutes(cacheTime)))

        checkForOutdatedMessages()
    }

    fun checkForOutdatedMessages() {
        for (msg in cacheID.iterator()) {
            when (msg.second.compareTo(LocalTime.now())) {
                -1 -> { // Message exceeded its cacheTime and needs to be removed
                    cacheID.remove(msg)
                }
                else -> { // The rest of the messages hasn't exceeded its cacheTime
                    break
                }
            }
        }
    }

    fun containsMessage(msg: T): Boolean = cacheID.any { it.first == msg }
    fun getsize(): Int {
        return cacheID.size
    }

    fun printAllMessages() {
        for (msg in cacheID) {
            println(msg)
        }
    }

    fun clearCache() {
        cacheID.clear()
    }
}
