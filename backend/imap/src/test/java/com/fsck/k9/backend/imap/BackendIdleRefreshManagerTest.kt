package com.fsck.k9.backend.imap

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.Test

private const val START_TIME = 100_000_000L

class BackendIdleRefreshManagerTest {
    val alarmManager = MockSystemAlarmManager(START_TIME)
    val idleRefreshManager = BackendIdleRefreshManager(alarmManager)

    @Test
    fun `single timer`() {
        val timeout = 15 * 60 * 1000L
        val callback = RecordingCallback()

        idleRefreshManager.startTimer(timeout, callback::alarm)
        alarmManager.advanceTime(timeout)

        assertThat(alarmManager.alarmTimes).isEqualTo(listOf(START_TIME + timeout))
        assertThat(callback.wasCalled).isTrue()
    }

    @Test
    fun `starting two timers in quick succession`() {
        val timeout = 15 * 60 * 1000L
        val callback1 = RecordingCallback()
        val callback2 = RecordingCallback()

        idleRefreshManager.startTimer(timeout, callback1::alarm)
        // Advance clock less than MIN_TIMER_DELTA
        alarmManager.advanceTime(100)
        idleRefreshManager.startTimer(timeout, callback2::alarm)
        alarmManager.advanceTime(timeout)

        assertThat(alarmManager.alarmTimes).isEqualTo(listOf(START_TIME + timeout))
        assertThat(callback1.wasCalled).isTrue()
        assertThat(callback2.wasCalled).isTrue()
    }

    @Test
    fun `starting second timer some time after first should trigger both at initial trigger time`() {
        val timeout = 15 * 60 * 1000L
        val waitTime = 10 * 60 * 1000L
        val callback1 = RecordingCallback()
        val callback2 = RecordingCallback()

        idleRefreshManager.startTimer(timeout, callback1::alarm)
        // Advance clock by more than MIN_TIMER_DELTA but less than 'timeout'
        alarmManager.advanceTime(waitTime)

        assertThat(callback1.wasCalled).isFalse()

        idleRefreshManager.startTimer(timeout, callback2::alarm)
        alarmManager.advanceTime(timeout - waitTime)

        assertThat(alarmManager.alarmTimes).isEqualTo(listOf(START_TIME + timeout))
        assertThat(callback1.wasCalled).isTrue()
        assertThat(callback2.wasCalled).isTrue()
    }

    @Test
    fun `second timer with lower timeout should reschedule alarm`() {
        val timeout1 = 15 * 60 * 1000L
        val timeout2 = 10 * 60 * 1000L
        val callback1 = RecordingCallback()
        val callback2 = RecordingCallback()

        idleRefreshManager.startTimer(timeout1, callback1::alarm)

        assertThat(alarmManager.triggerTime).isEqualTo(START_TIME + timeout1)

        idleRefreshManager.startTimer(timeout2, callback2::alarm)
        alarmManager.advanceTime(timeout2)

        assertThat(alarmManager.alarmTimes).isEqualTo(listOf(START_TIME + timeout1, START_TIME + timeout2))
        assertThat(callback1.wasCalled).isTrue()
        assertThat(callback2.wasCalled).isTrue()
    }

    @Test
    fun `do not trigger timers earlier than necessary`() {
        val timeout1 = 10 * 60 * 1000L
        val timeout2 = 23 * 60 * 1000L
        val callback1 = RecordingCallback()
        val callback2 = RecordingCallback()
        val callback3 = RecordingCallback()

        idleRefreshManager.startTimer(timeout1, callback1::alarm)
        idleRefreshManager.startTimer(timeout2, callback2::alarm)

        alarmManager.advanceTime(timeout1)
        assertThat(callback1.wasCalled).isTrue()
        assertThat(callback2.wasCalled).isFalse()

        idleRefreshManager.startTimer(timeout1, callback3::alarm)

        alarmManager.advanceTime(timeout1)

        assertThat(alarmManager.alarmTimes).isEqualTo(
            listOf(START_TIME + timeout1, START_TIME + timeout2, START_TIME + timeout1 + timeout1),
        )
        assertThat(callback2.wasCalled).isTrue()
        assertThat(callback3.wasCalled).isTrue()
    }

    @Test
    fun `reset timers`() {
        val timeout = 10 * 60 * 1000L
        val callback = RecordingCallback()

        idleRefreshManager.startTimer(timeout, callback::alarm)

        alarmManager.advanceTime(5 * 60 * 1000L)
        assertThat(callback.wasCalled).isFalse()

        idleRefreshManager.resetTimers()

        assertThat(alarmManager.triggerTime).isEqualTo(NO_TRIGGER_TIME)
        assertThat(callback.wasCalled).isTrue()
    }

    @Test
    fun `cancel timer`() {
        val timeout = 10 * 60 * 1000L
        val callback = RecordingCallback()

        val timer = idleRefreshManager.startTimer(timeout, callback::alarm)

        alarmManager.advanceTime(5 * 60 * 1000L)
        timer.cancel()

        assertThat(alarmManager.triggerTime).isEqualTo(NO_TRIGGER_TIME)
        assertThat(callback.wasCalled).isFalse()
    }
}

class RecordingCallback {
    var wasCalled = false
        private set

    fun alarm() {
        wasCalled = true
    }
}

typealias Callback = () -> Unit
private const val NO_TRIGGER_TIME = -1L

class MockSystemAlarmManager(startTime: Long) : SystemAlarmManager {
    var now = startTime
    var triggerTime = NO_TRIGGER_TIME
    var callback: Callback? = null
    val alarmTimes = mutableListOf<Long>()

    override fun setAlarm(triggerTime: Long, callback: () -> Unit) {
        this.triggerTime = triggerTime
        this.callback = callback
        alarmTimes.add(triggerTime)
    }

    override fun cancelAlarm() {
        this.triggerTime = NO_TRIGGER_TIME
        this.callback = null
    }

    override fun now(): Long = now

    fun advanceTime(delta: Long) {
        now += delta
        if (now >= triggerTime) {
            trigger()
        }
    }

    private fun trigger() {
        callback?.invoke().also {
            triggerTime = NO_TRIGGER_TIME
            callback = null
        }
    }
}
