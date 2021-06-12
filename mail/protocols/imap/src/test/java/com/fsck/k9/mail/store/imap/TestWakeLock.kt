package com.fsck.k9.mail.store.imap

import com.fsck.k9.mail.power.WakeLock
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class TestWakeLock(private val timeoutSeconds: Long, isHeld: Boolean = false) : WakeLock {
    var isHeld = isHeld
        private set

    private val lock = ReentrantLock()
    private val lockCondition = lock.newCondition()

    override fun acquire() {
        lock.withLock {
            if (isHeld) throw AssertionError("Tried to acquire wakelock we're already holding")

            isHeld = true
        }
    }

    override fun release() {
        lock.withLock {
            if (!isHeld) throw AssertionError("Tried to release a wakelock we're not holding")

            isHeld = false
            lockCondition.signal()
        }
    }

    override fun acquire(timeout: Long) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun setReferenceCounted(counted: Boolean) {
        throw UnsupportedOperationException("not implemented")
    }

    fun waitForRelease() {
        lock.withLock {
            if (isHeld) {
                lockCondition.await(timeoutSeconds, TimeUnit.SECONDS)
            }
        }
    }
}
