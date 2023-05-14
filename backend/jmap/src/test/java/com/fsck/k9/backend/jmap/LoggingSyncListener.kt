package com.fsck.k9.backend.jmap

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.fail
import com.fsck.k9.backend.api.SyncListener

class LoggingSyncListener : SyncListener {
    private val events = mutableListOf<SyncListenerEvent>()

    fun assertSyncSuccess() {
        events.filterIsInstance<SyncListenerEvent.SyncFailed>().firstOrNull()?.let { syncFailed ->
            throw AssertionError("Expected sync success", syncFailed.exception)
        }

        if (events.none { it is SyncListenerEvent.SyncFinished }) {
            fail("Expected SyncFinished, but only got: $events")
        }
    }

    fun assertSyncEvents(vararg events: SyncListenerEvent) {
        for (event in events) {
            assertThat(getNextEvent()).isEqualTo(event)
        }

        assertThat(this.events).isEmpty()
    }

    fun getNextEvent(): SyncListenerEvent {
        require(events.isNotEmpty()) { "No events left" }
        return events.removeAt(0)
    }

    override fun syncStarted(folderServerId: String) {
        events.add(SyncListenerEvent.SyncStarted(folderServerId))
    }

    override fun syncAuthenticationSuccess() {
        throw UnsupportedOperationException("not implemented")
    }

    override fun syncHeadersStarted(folderServerId: String) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun syncHeadersProgress(folderServerId: String, completed: Int, total: Int) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun syncHeadersFinished(folderServerId: String, totalMessagesInMailbox: Int, numNewMessages: Int) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun syncProgress(folderServerId: String, completed: Int, total: Int) {
        events.add(SyncListenerEvent.SyncProgress(folderServerId, completed, total))
    }

    override fun syncNewMessage(folderServerId: String, messageServerId: String, isOldMessage: Boolean) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun syncRemovedMessage(folderServerId: String, messageServerId: String) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun syncFlagChanged(folderServerId: String, messageServerId: String) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun syncFinished(folderServerId: String) {
        events.add(SyncListenerEvent.SyncFinished(folderServerId))
    }

    override fun syncFailed(folderServerId: String, message: String, exception: Exception?) {
        events.add(SyncListenerEvent.SyncFailed(folderServerId, message, exception))
    }

    override fun folderStatusChanged(folderServerId: String) {
        throw UnsupportedOperationException("not implemented")
    }
}

sealed class SyncListenerEvent {
    data class SyncStarted(val folderServerId: String) : SyncListenerEvent()
    data class SyncFinished(val folderServerId: String) : SyncListenerEvent()
    data class SyncFailed(
        val folderServerId: String,
        val message: String,
        val exception: Exception?,
    ) : SyncListenerEvent()

    data class SyncProgress(val folderServerId: String, val completed: Int, val total: Int) : SyncListenerEvent()
}
