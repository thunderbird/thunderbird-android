package com.fsck.k9.backend.eas

import com.fsck.k9.backend.eas.dto.Ping
import com.fsck.k9.backend.eas.dto.PingFolder
import com.fsck.k9.backend.eas.dto.PingFolders
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.power.PowerManager
import com.fsck.k9.mail.PushReceiver
import com.fsck.k9.mail.Pusher
import com.fsck.k9.mail.power.WakeLock
import timber.log.Timber

class EasPusher(private val client: EasClient,
                private val powerManager: PowerManager,
                private val receiver: PushReceiver) : Pusher {

    var thread: Thread? = null
    var wakeLock: WakeLock? = null

    var BASE_POLL_DURATION_SEC = 800
    var TIMEOUT_MS = BASE_POLL_DURATION_SEC * 1000 + 20000L

    var lastRefreshTime = -1L

    override fun start(folderServerIds: MutableList<String>) {
        stop()

        lastRefresh = System.currentTimeMillis()

        thread = Thread {
            try {
                wakeLock = powerManager.newWakeLock("K9-EasPusher")
                wakeLock!!.setReferenceCounted(false)
                wakeLock!!.acquire(TIMEOUT_MS)

                folderServerIds.forEach { receiver.setPushActive(it, true) }

                try {
                    while (!thread!!.isInterrupted) {
                        Timber.i("Start Ping Request")
                        println(folderServerIds)
                        val result = client.ping(Ping(BASE_POLL_DURATION_SEC, PingFolders(
                                folderServerIds.map {
                                    PingFolder("Email", it)
                                }
                        )), TIMEOUT_MS)
                        Timber.i("Got Ping Response")
                        println(result)
                        println(thread!!.isInterrupted)

                        result.pingFolders?.folderId?.forEach {
                            receiver.syncFolder(EasMessage.EasFolder(it))
                        }
                        println("AFTER SYNC")
                    }
                } catch (e: AuthenticationFailedException) {
                    receiver.authenticationFailed()
                }
            } finally {
                wakeLock?.release()
                folderServerIds.forEach { receiver.setPushActive(it, false) }
            }
        }.also {
            it.start()
        }
    }

    override fun refresh() {
        if (thread?.isInterrupted == false) {
            Timber.i("Refresh Wakelock")
            wakeLock!!.acquire(TIMEOUT_MS)
        }
    }

    override fun stop() {
        Timber.i("STOPPED")
        thread?.interrupt()
        wakeLock?.release()
    }

    override fun getRefreshInterval() = BASE_POLL_DURATION_SEC * 1000


    override fun setLastRefresh(lastRefresh: Long) {
        lastRefreshTime = lastRefresh
    }

    override fun getLastRefresh() = lastRefreshTime
}
