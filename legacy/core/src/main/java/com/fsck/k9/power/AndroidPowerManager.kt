package com.fsck.k9.power

import android.annotation.SuppressLint
import android.os.SystemClock
import com.fsck.k9.mail.power.PowerManager
import com.fsck.k9.mail.power.WakeLock
import java.util.concurrent.atomic.AtomicInteger
import net.thunderbird.legacy.logging.Log
import android.os.PowerManager as SystemPowerManager
import android.os.PowerManager.WakeLock as SystemWakeLock

private const val TAG = "AndroidPowerManager"

internal class AndroidPowerManager(private val systemPowerManager: SystemPowerManager) : PowerManager {
    override fun newWakeLock(tag: String): WakeLock {
        return AndroidWakeLock(SystemPowerManager.PARTIAL_WAKE_LOCK, tag)
    }

    inner class AndroidWakeLock(flags: Int, val tag: String?) : WakeLock {
        private val wakeLock: SystemWakeLock = systemPowerManager.newWakeLock(flags, tag)
        private val id = wakeLockId.getAndIncrement()

        @Volatile
        private var startTime: Long? = null

        @Volatile
        private var timeout: Long? = null

        init {
            Log.v(TAG, "AndroidWakeLock for tag %s / id %d: Create", tag, id)
        }

        override fun acquire(timeout: Long) {
            synchronized(wakeLock) {
                wakeLock.acquire(timeout)
            }

            Log.v(TAG, "AndroidWakeLock for tag %s / id %d for %d ms: acquired", tag, id, timeout)

            if (startTime == null) {
                startTime = SystemClock.elapsedRealtime()
            }

            this.timeout = timeout
        }

        @SuppressLint("WakelockTimeout")
        override fun acquire() {
            synchronized(wakeLock) {
                wakeLock.acquire()
            }

            Log.v(TAG, "AndroidWakeLock for tag %s / id %d: acquired with no timeout.", tag, id)

            if (startTime == null) {
                startTime = SystemClock.elapsedRealtime()
            }

            timeout = null
        }

        override fun setReferenceCounted(counted: Boolean) {
            synchronized(wakeLock) {
                wakeLock.setReferenceCounted(counted)
            }
        }

        override fun release() {
            val startTime = this.startTime
            if (startTime != null) {
                val endTime = SystemClock.elapsedRealtime()

                Log.v(
                    TAG,
                    "AndroidWakeLock for tag %s / id %d: releasing after %d ms, timeout = %d ms",
                    tag,
                    id,
                    endTime - startTime,
                    timeout,
                )
            } else {
                Log.v(TAG, "AndroidWakeLock for tag %s / id %d, timeout = %d ms: releasing", tag, id, timeout)
            }

            synchronized(wakeLock) {
                wakeLock.release()
            }

            this.startTime = null
        }
    }

    companion object {
        private val wakeLockId = AtomicInteger(0)
    }
}
