package com.fsck.k9.backend.imap

import com.fsck.k9.logging.Timber
import com.fsck.k9.mail.power.PowerManager
import com.fsck.k9.mail.store.imap.IdleRefreshManager
import com.fsck.k9.mail.store.imap.IdleRefreshTimeoutProvider
import com.fsck.k9.mail.store.imap.IdleResult
import com.fsck.k9.mail.store.imap.ImapFolderIdler
import com.fsck.k9.mail.store.imap.ImapStore
import kotlin.concurrent.thread

/**
 * Listens for changes to an IMAP folder in a dedicated thread.
 */
class ImapFolderPusher(
    private val imapStore: ImapStore,
    private val powerManager: PowerManager,
    private val idleRefreshManager: IdleRefreshManager,
    private val callback: ImapPusherCallback,
    private val accountName: String,
    private val folderServerId: String,
    private val idleRefreshTimeoutProvider: IdleRefreshTimeoutProvider,
) {
    @Volatile
    private var folderIdler: ImapFolderIdler? = null

    @Volatile
    private var stopPushing = false

    fun start() {
        Timber.v("Starting ImapFolderPusher for %s / %s", accountName, folderServerId)

        thread(name = "ImapFolderPusher-$accountName-$folderServerId") {
            Timber.v("Starting ImapFolderPusher thread for %s / %s", accountName, folderServerId)

            runPushLoop()

            Timber.v("Exiting ImapFolderPusher thread for %s / %s", accountName, folderServerId)
        }
    }

    fun refresh() {
        Timber.v("Refreshing ImapFolderPusher for %s / %s", accountName, folderServerId)

        folderIdler?.refresh()
    }

    fun stop() {
        Timber.v("Stopping ImapFolderPusher for %s / %s", accountName, folderServerId)

        stopPushing = true
        folderIdler?.stop()
    }

    private fun runPushLoop() {
        val wakeLock = powerManager.newWakeLock("ImapFolderPusher-$accountName-$folderServerId")
        wakeLock.acquire()

        performInitialSync()

        val folderIdler = ImapFolderIdler.create(
            idleRefreshManager,
            wakeLock,
            imapStore,
            folderServerId,
            idleRefreshTimeoutProvider,
        ).also {
            folderIdler = it
        }

        try {
            while (!stopPushing) {
                when (folderIdler.idle()) {
                    IdleResult.SYNC -> {
                        callback.onPushEvent(folderServerId)
                    }
                    IdleResult.STOPPED -> {
                        // ImapFolderIdler only stops when we ask it to.
                        // But it can't hurt to make extra sure we exit the loop.
                        stopPushing = true
                    }
                    IdleResult.NOT_SUPPORTED -> {
                        stopPushing = true
                        callback.onPushNotSupported()
                    }
                }
            }
        } catch (e: Exception) {
            Timber.v(e, "Exception in ImapFolderPusher")

            this.folderIdler = null
            callback.onPushError(folderServerId, e)
        }

        wakeLock.release()
    }

    private fun performInitialSync() {
        callback.onPushEvent(folderServerId)
    }
}
