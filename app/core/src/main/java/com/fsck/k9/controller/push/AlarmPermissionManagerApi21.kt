package com.fsck.k9.controller.push

/**
 * On Android versions prior to 12 there's no permission to limit an app's ability to schedule exact alarms.
 */
internal class AlarmPermissionManagerApi21 : AlarmPermissionManager {
    override fun canScheduleExactAlarms(): Boolean {
        return true
    }

    override fun registerListener(listener: AlarmPermissionListener) = Unit

    override fun unregisterListener() = Unit
}
