package com.fsck.k9.mail.power

interface PowerManager {
    fun newWakeLock(tag: String): WakeLock
}
