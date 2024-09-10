package com.fsck.k9.storage.messages

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import app.k9mail.core.android.common.database.getIntOrNull
import app.k9mail.core.android.common.database.getLongOrNull
import app.k9mail.core.android.common.database.getStringOrNull
import app.k9mail.core.android.common.database.map

fun SQLiteDatabase.createFolder(
    name: String = "irrelevant",
    type: String = "regular",
    serverId: String? = null,
    isLocalOnly: Boolean = true,
    integrate: Boolean = false,
    inTopGroup: Boolean = false,
    displayClass: String = "NO_CLASS",
    syncClass: String? = "INHERITED",
    notificationsEnabled: Boolean = false,
    pushEnabled: Boolean = false,
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
        put("notifications_enabled", notificationsEnabled)
        put("push_enabled", pushEnabled)
        put("last_updated", lastUpdated)
        put("unread_count", unreadCount)
        put("visible_limit", visibleLimit)
        put("status", status)
        put("flagged_count", flaggedCount)
        put("more_messages", moreMessages)
    }

    return insert("folders", null, values)
}

fun SQLiteDatabase.readFolders(): List<FolderEntry> {
    val cursor = rawQuery("SELECT * FROM folders", null)
    return cursor.use {
        cursor.map {
            FolderEntry(
                id = cursor.getLongOrNull("id"),
                name = cursor.getStringOrNull("name"),
                type = cursor.getStringOrNull("type"),
                serverId = cursor.getStringOrNull("server_id"),
                isLocalOnly = cursor.getIntOrNull("local_only"),
                integrate = cursor.getIntOrNull("integrate"),
                inTopGroup = cursor.getIntOrNull("top_group"),
                displayClass = cursor.getStringOrNull("display_class"),
                syncClass = cursor.getStringOrNull("poll_class"),
                notificationsEnabled = cursor.getIntOrNull("notifications_enabled"),
                pushEnabled = cursor.getIntOrNull("push_enabled"),
                lastUpdated = cursor.getLongOrNull("last_updated"),
                unreadCount = cursor.getIntOrNull("unread_count"),
                visibleLimit = cursor.getIntOrNull("visible_limit"),
                status = cursor.getStringOrNull("status"),
                flaggedCount = cursor.getIntOrNull("flagged_count"),
                moreMessages = cursor.getStringOrNull("more_messages"),
            )
        }
    }
}

data class FolderEntry(
    val id: Long?,
    val name: String?,
    val type: String?,
    val serverId: String?,
    val isLocalOnly: Int?,
    val integrate: Int?,
    val inTopGroup: Int?,
    val displayClass: String?,
    val syncClass: String?,
    val notificationsEnabled: Int?,
    val pushEnabled: Int?,
    val lastUpdated: Long?,
    val unreadCount: Int?,
    val visibleLimit: Int?,
    val status: String?,
    val flaggedCount: Int?,
    val moreMessages: String?,
)
