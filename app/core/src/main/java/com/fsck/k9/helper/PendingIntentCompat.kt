package com.fsck.k9.helper

import android.app.PendingIntent
import android.os.Build

object PendingIntentCompat {
    @JvmField
    val FLAG_IMMUTABLE = if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0

    @JvmField
    val FLAG_MUTABLE = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
}
