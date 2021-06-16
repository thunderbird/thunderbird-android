package com.fsck.k9.backends

import com.fsck.k9.backend.imap.SystemAlarmManager

class AndroidAlarmManager : SystemAlarmManager {
    override fun setAlarm(triggerTime: Long, callback: () -> Unit) {
        TODO("implement")
    }

    override fun cancelAlarm() {
        TODO("implement")
    }

    override fun now(): Long {
        TODO("implement")
    }
}
