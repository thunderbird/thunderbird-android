package com.fsck.k9.helper

import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.sendBlocking

/**
 * Like [sendBlocking], but ignores [ClosedSendChannelException].
 */
fun <E> SendChannel<E>.sendBlockingSilently(element: E) {
    try {
        sendBlocking(element)
    } catch (e: ClosedSendChannelException) {
        // Ignore
    }
}
