package com.fsck.k9.backend.eas

import timber.log.Timber

import com.fsck.k9.backend.eas.dto.Ping
import com.fsck.k9.backend.eas.dto.PingFolder
import com.fsck.k9.backend.eas.dto.PingFolders
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.power.PowerManager
import com.fsck.k9.mail.PushReceiver
import com.fsck.k9.mail.Pusher
import com.fsck.k9.mail.power.WakeLock

class EasPusher(private val client: EasClient,
                private val powerManager: PowerManager,
                private val receiver: PushReceiver) : Pusher {
    var thread: Thread? = null
    var wakeLock: WakeLock? = null

    // Maximum duration the Server holds the connection until responding
    val POLL_DURATION_SEC = 800
    val POLL_TIMEOUT_MS = POLL_DURATION_SEC * 1000 + 20000L

    // Set if the Server respond because ´POLL_DURATION_SEC` seconds are over with no changes
    val STATUS_NO_CHANGES_AVAILABLE = 1
    val STATUS_CHANGES_AVAILABLE = 2

    var lastRefreshTime = -1L

    @Synchronized
    override fun start(folderServerIds: List<String>) {
        stop()

        lastRefresh = System.currentTimeMillis()

        thread = Thread {
            try {
                wakeLock = powerManager.newWakeLock("K9-EasPusher").apply {
                    setReferenceCounted(false)
                    acquire(POLL_TIMEOUT_MS)
                }

                folderServerIds.forEach { receiver.setPushActive(it, true) }

                try {
                    while (!thread!!.isInterrupted) {
                        Timber.i("EasPusher: Start Ping request")
                        val result = client.ping(Ping(POLL_DURATION_SEC, PingFolders(
                                folderServerIds.map {
                                    PingFolder(SYNC_CLASS_EMAIL, it)
                                }
                        )), POLL_TIMEOUT_MS)
                        Timber.i("EasPusher: Got Ping response")

                        if (result.status != STATUS_NO_CHANGES_AVAILABLE && result.status != STATUS_CHANGES_AVAILABLE) {
                            Timber.e("EasPusher: Ping response contained non Ok status. Stopping")
                            break
                        }

                        result.pingFolders?.folderId?.forEach {
                            receiver.syncFolder(EasMessage.EasFolder(it))
                        }
                    }
                } catch (e: InterruptedException) {
                } catch (e: AuthenticationFailedException) {
                    receiver.authenticationFailed()
                } catch (e: Exception) {
                    receiver.pushError("EasPusher: Caught exception", e)
                }
            } finally {
                synchronized(this) {
                    wakeLock?.release()
                    wakeLock = null
                }
                folderServerIds.forEach { receiver.setPushActive(it, false) }
            }
        }.also {
            it.start()
        }
    }

    @Synchronized
    override fun refresh() {
        if (thread?.isInterrupted == false) {
            Timber.i("EasPusher: Refresh Wakelock")
            wakeLock!!.acquire(POLL_TIMEOUT_MS)
        }
    }

    @Synchronized
    override fun stop() {
        thread?.interrupt()
        wakeLock?.release()
        wakeLock = null
    }

    override fun getRefreshInterval() = POLL_DURATION_SEC * 1000

    override fun setLastRefresh(lastRefresh: Long) {
        lastRefreshTime = lastRefresh
    }

    override fun getLastRefresh() = lastRefreshTime
}
