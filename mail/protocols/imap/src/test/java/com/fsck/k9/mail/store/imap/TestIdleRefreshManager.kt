package com.fsck.k9.mail.store.imap

class TestIdleRefreshManager : IdleRefreshManager {
    private val timers = mutableListOf<TestIdleRefreshTimer>()

    @Synchronized
    override fun startTimer(timeout: Long, callback: () -> Unit): TestIdleRefreshTimer {
        return TestIdleRefreshTimer(timeout, callback).also { timer -> timers.add(timer) }
    }

    @Synchronized
    override fun resetTimers() {
        for (timer in timers) {
            timer.trigger()
        }
        timers.clear()
    }

    fun getTimeoutValue(): Long? = timers.map { it.timeout }.minOrNull()
}

class TestIdleRefreshTimer(val timeout: Long, private val callback: () -> Unit) : IdleRefreshTimer {
    override var isWaiting: Boolean = true
        private set

    @Synchronized
    override fun cancel() {
        isWaiting = false
    }

    fun trigger() {
        isWaiting = false
        callback()
    }
}
