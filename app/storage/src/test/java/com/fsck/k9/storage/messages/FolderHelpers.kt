package com.fsck.k9.storage.messages

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

fun SQLiteDatabase.createFolder(
    name: String = "irrelevant",
    type: String = "regular",
    serverId: String? = null,
    isLocalOnly: Boolean = true,
    integrate: Boolean = false,
    inTopGroup: Boolean = false,
    displayClass: String = "NO_CLASS",
    syncClass: String = "INHERITED",
    notifyClass: String = "INHERITED",
    pushClass: String = "SECOND_CLASS",
    lastUpdated: Long = 0L,
    unreadCount: Int = 0,
    visibleLimit: Int = 25,
    status: String? = null,
    flaggedCount: Int = 0,
    moreMessages: String = "unknown",
): Long {
    val values = ContentValues().apply {
        put("name", name)
        put("type", type)
        put("server_id", serverId)
        put("local_only", isLocalOnly)
        put("integrate", integrate)
        put("top_group", inTopGroup)
        put("display_class", displayClass)
        put("poll_class", syncClass)
        put("notify_class", notifyClass)
        put("push_class", pushClass)
        put("last_updated", lastUpdated)
        put("unread_count", unreadCount)
        put("visible_limit", visibleLimit)
        put("status", status)
        put("flagged_count", flaggedCount)
        put("more_messages", moreMessages)
    }

    return insert("folders", null, values)
}
