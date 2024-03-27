package com.fsck.k9.backend.imap

interface SystemAlarmManager {
    fun setAlarm(triggerTime: Long, callback: () -> Unit)
    fun cancelAlarm()
    fun now(): Long
}
