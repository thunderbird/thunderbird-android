package com.fsck.k9.mailstore

import java.util.concurrent.CopyOnWriteArraySet

class MessageListRepository {
    private val listeners = CopyOnWriteArraySet<Pair<String, MessageListChangedListener>>()

    fun addListener(accountUuid: String, listener: MessageListChangedListener) {
        listeners.add(accountUuid to listener)
    }

    fun removeListener(listener: MessageListChangedListener) {
        val entries = listeners.filter { it.second == listener }.toSet()
        listeners.removeAll(entries)
    }

    fun notifyMessageListChanged(accountUuid: String) {
        for (listener in listeners) {
            if (listener.first == accountUuid) {
                listener.second.onMessageListChanged()
            }
        }
    }
}

fun interface MessageListChangedListener {
    fun onMessageListChanged()
}
