package com.fsck.k9.backend.imap

import com.fsck.k9.backend.api.BackendPusher
import com.fsck.k9.backend.api.BackendPusherCallback
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.power.PowerManager
import com.fsck.k9.mail.store.imap.IdleRefreshManager
import com.fsck.k9.mail.store.imap.IdleRefreshTimer
import com.fsck.k9.mail.store.imap.ImapStore
import java.io.IOException
import timber.log.Timber

private const val IO_ERROR_TIMEOUT = 5 * 60 * 1000L
private const val UNEXPECTED_ERROR_TIMEOUT = 60 * 60 * 1000L

/**
 * Manages [ImapFolderPusher] instances that listen for changes to individual folders.
 */
internal class ImapBackendPusher(
    private val imapStore: ImapStore,
    private val powerManager: PowerManager,
    private val idleRefreshManager: IdleRefreshManager,
    private val callback: BackendPusherCallback,
    private val accountName: String
) : BackendPusher, ImapPusherCallback {
    private val lock = Any()
    private val pushFolders = mutableMapOf<String, ImapFolderPusher>()
    private var currentFolderServerIds: Collection<String> = emptySet()
    private val pushFolderSleeping = mutableMapOf<String, IdleRefreshTimer>()

    override fun updateFolders(folderServerIds: Collection<String>) {
        Timber.v("ImapBackendPusher.updateFolders(): %s", folderServerIds)

        val stopFolderPushers: List<ImapFolderPusher>
        val startFolderPushers: List<ImapFolderPusher>
        synchronized(lock) {
            currentFolderServerIds = folderServerIds

            val oldRunningFolderServerIds = pushFolders.keys
            val oldFolderServerIds = oldRunningFolderServerIds + pushFolderSleeping.keys
            val removeFolderServerIds = oldFolderServerIds - folderServerIds
            stopFolderPushers = removeFolderServerIds
                .asSequence()
                .onEach { folderServerId -> cancelRetryTimer(folderServerId) }
                .map { folderServerId -> pushFolders.remove(folderServerId) }
                .filterNotNull()
                .toList()

            val startFolderServerIds = folderServerIds - oldRunningFolderServerIds
            startFolderPushers = startFolderServerIds
                .asSequence()
                .filterNot { folderServerId -> isWaitingForRetry(folderServerId) }
                .onEach { folderServerId -> pushFolderSleeping.remove(folderServerId) }
                .map { folderServerId ->
                    createImapFolderPusher(folderServerId).also { folderPusher ->
                        pushFolders[folderServerId] = folderPusher
                    }
                }
                .toList()
        }

        for (folderPusher in stopFolderPushers) {
            folderPusher.stop()
        }

        for (folderPusher in startFolderPushers) {
            folderPusher.start()
        }
    }

    override fun stop() {
        Timber.v("ImapBackendPusher.stop()")

        synchronized(lock) {
            for (pushFolder in pushFolders.values) {
                pushFolder.stop()
            }
            pushFolders.clear()

            for (retryTimer in pushFolderSleeping.values) {
                retryTimer.cancel()
            }
            pushFolderSleeping.clear()

            currentFolderServerIds = emptySet()
        }
    }

    private fun createImapFolderPusher(folderServerId: String): ImapFolderPusher {
        // TODO: use value from account settings
        val idleRefreshTimeoutMs = 15 * 60 * 1000L
        return ImapFolderPusher(
            imapStore,
            powerManager,
            idleRefreshManager,
            this,
            accountName,
            folderServerId,
            idleRefreshTimeoutMs
        )
    }

    override fun onPushEvent(folderServerId: String) {
        callback.onPushEvent(folderServerId)
        idleRefreshManager.resetTimers()
    }

    override fun onPushError(folderServerId: String, exception: Exception) {
        synchronized(lock) {
            pushFolders.remove(folderServerId)

            when (exception) {
                is AuthenticationFailedException -> {
                    Timber.v(exception, "Authentication failure when attempting to use IDLE")
                    // TODO: This could be happening because of too many connections to the host. Ideally we'd want to
                    //  detect this case and use a lower timeout.

                    startRetryTimer(folderServerId, UNEXPECTED_ERROR_TIMEOUT)
                }
                is IOException -> {
                    Timber.v(exception, "I/O error while trying to use IDLE")

                    startRetryTimer(folderServerId, IO_ERROR_TIMEOUT)
                }
                is MessagingException -> {
                    Timber.v(exception, "MessagingException")

                    if (exception.isPermanentFailure) {
                        startRetryTimer(folderServerId, UNEXPECTED_ERROR_TIMEOUT)
                    } else {
                        startRetryTimer(folderServerId, IO_ERROR_TIMEOUT)
                    }
                }
                else -> {
                    Timber.v(exception, "Unexpected error")
                    startRetryTimer(folderServerId, UNEXPECTED_ERROR_TIMEOUT)
                }
            }

            if (pushFolders.isEmpty()) {
                callback.onPushError(exception)
            }
        }
    }

    private fun startRetryTimer(folderServerId: String, timeout: Long) {
        Timber.v("ImapBackendPusher for folder %s sleeping for %d ms", folderServerId, timeout)
        pushFolderSleeping[folderServerId] = idleRefreshManager.startTimer(timeout, ::refresh)
    }

    private fun cancelRetryTimer(folderServerId: String) {
        Timber.v("Canceling ImapBackendPusher retry timer for folder %s", folderServerId)
        pushFolderSleeping.remove(folderServerId)?.cancel()
    }

    private fun isWaitingForRetry(folderServerId: String): Boolean {
        return pushFolderSleeping[folderServerId]?.isWaiting == true
    }

    private fun refresh() {
        Timber.v("Refreshing ImapBackendPusher (at least one retry timer has expired)")

        val currentFolderServerIds = synchronized(lock) { currentFolderServerIds }
        updateFolders(currentFolderServerIds)
    }
}
