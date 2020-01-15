package com.fsck.k9.widget.unread

import android.content.Intent

data class UnreadWidgetData(
    val configuration: UnreadWidgetConfiguration,
    val title: String,
    val unreadCount: Int,
    val clickIntent: Intent
)
