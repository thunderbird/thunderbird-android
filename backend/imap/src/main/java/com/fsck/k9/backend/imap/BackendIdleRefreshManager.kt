package com.fsck.k9.backend.imap

import com.fsck.k9.mail.store.imap.IdleRefreshManager
import com.fsck.k9.mail.store.imap.IdleRefreshTimer

private typealias Callback = () -> Unit

private const val MIN_TIMER_DELTA = 1 * 60 * 1000L
private const val NO_TRIGGER_TIME = 0L

/**
 * Timer mechanism to refresh IMAP IDLE connections.
 *
 * Triggers timers early if necessary to reduce the number of times the device has to be woken up.
 */
class BackendIdleRefreshManager(private val alarmManager: SystemAlarmManager) : IdleRefreshManager {
    private var timers = mutableSetOf<BackendIdleRefreshTimer>()
    private var currentTriggerTime = NO_TRIGGER_TIME
    private var minTimeout = Long.MAX_VALUE
    private var minTimeoutTimestamp = 0L

    @Synchronized
    override fun startTimer(timeout: Long, callback: Callback): IdleRefreshTimer {
        require(timeout > MIN_TIMER_DELTA) { "Timeout needs to be greater than $MIN_TIMER_DELTA ms" }

        val now = alarmManager.now()
        val triggerTime = now + timeout

        updateMinTimeout(timeout, now)
        setOrUpdateAlarm(triggerTime)

        return BackendIdleRefreshTimer(triggerTime, callback).also { timer ->
            timers.add(timer)
        }
    }

    override fun resetTimers() {
        synchronized(this) {
            cancelAlarm()
        }

        onTimeout()
    }

    private fun updateMinTimeout(timeout: Long, now: Long) {
        if (minTimeoutTimestamp + minTimeout * 2 < now) {
            minTimeout = Long.MAX_VALUE
        }

        if (timeout <= minTimeout) {
            minTimeout = timeout
            minTimeoutTimestamp = now
        }
    }

    private fun setOrUpdateAlarm(triggerTime: Long) {
        if (currentTriggerTime == NO_TRIGGER_TIME) {
            setAlarm(triggerTime)
        } else if (currentTriggerTime - triggerTime > MIN_TIMER_DELTA) {
            adjustAlarm(triggerTime)
        }
    }

    private fun setAlarm(triggerTime: Long) {
        currentTriggerTime = triggerTime
        alarmManager.setAlarm(triggerTime, ::onTimeout)
    }

    private fun adjustAlarm(triggerTime: Long) {
        currentTriggerTime = triggerTime
        alarmManager.cancelAlarm()
        alarmManager.setAlarm(triggerTime, ::onTimeout)
    }

    private fun cancelAlarm() {
        currentTriggerTime = NO_TRIGGER_TIME
        alarmManager.cancelAlarm()
    }

    private fun onTimeout() {
        val triggerTimers = synchronized(this) {
            currentTriggerTime = NO_TRIGGER_TIME

            if (timers.isEmpty()) return

            val now = alarmManager.now()
            val minNextTriggerTime = now + minTimeout

            val triggerTimers = timers.filter { it.triggerTime < minNextTriggerTime - MIN_TIMER_DELTA }
            timers.removeAll(triggerTimers)

            timers.minOfOrNull { it.triggerTime }?.let { nextTriggerTime ->
                setAlarm(nextTriggerTime)
            }

            triggerTimers
        }

        for (timer in triggerTimers) {
            timer.onTimeout()
        }
    }

    @Synchronized
    private fun removeTimer(timer: BackendIdleRefreshTimer) {
        timers.remove(timer)

        if (timers.isEmpty()) {
            cancelAlarm()
        }
    }

    internal inner class BackendIdleRefreshTimer(
        val triggerTime: Long,
        val callback: Callback,
    ) : IdleRefreshTimer {
        override var isWaiting: Boolean = true
            private set

        @Synchronized
        override fun cancel() {
            if (isWaiting) {
                isWaiting = false
                removeTimer(this)
            }
        }

        internal fun onTimeout() {
            synchronized(this) {
                isWaiting = false
            }

            callback.invoke()
        }
    }
}
