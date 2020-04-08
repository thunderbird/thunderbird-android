package com.fsck.k9.mailstore

import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import androidx.core.database.getStringOrNull
import com.fsck.k9.Account
import com.fsck.k9.Account.FolderMode
import com.fsck.k9.Preferences
import com.fsck.k9.mail.FolderClass
import com.fsck.k9.mail.FolderType as RemoteFolderType

class FolderRepository(
    private val localStoreProvider: LocalStoreProvider,
    private val preferences: Preferences,
    private val account: Account
) {
    private val sortForDisplay =
            compareByDescending<DisplayFolder> { it.folder.serverId == account.inboxFolder }
            .thenByDescending { it.folder.serverId == account.outboxFolder }
            .thenByDescending { account.isSpecialFolder(it.folder.serverId) }
            .thenByDescending { it.isInTopGroup }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.folder.name }

    fun getRemoteFolders(): List<Folder> {
        val folders = localStoreProvider.getInstance(account).getPersonalNamespaces(false)

        return folders
                .filterNot { it.isLocalOnly }
                .map { Folder(it.databaseId, it.serverId, it.name, it.type.toFolderType()) }
    }

    fun getDisplayFolders(displayMode: FolderMode?): List<DisplayFolder> {
        val database = localStoreProvider.getInstance(account).database
        val displayFolders = database.execute(false) { db ->
            val displayModeFilter = displayMode ?: account.folderDisplayMode
            getDisplayFolders(db, displayModeFilter)
        }

        return displayFolders.sortedWith(sortForDisplay)
    }

    fun getFolderDetails(folderId: Long): FolderDetails? {
        val database = localStoreProvider.getInstance(account).database
        return database.execute(false) { db ->
            db.query(
                "folders",
                arrayOf(
                    "server_id",
                    "name",
                    "top_group",
                    "integrate",
                    "poll_class",
                    "display_class",
                    "notify_class",
                    "push_class"
                ),
                "id = ?",
                arrayOf(folderId.toString()),
                null,
                null,
                null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    val serverId = cursor.getString(0)
                    FolderDetails(
                        folder = Folder(
                            id = folderId,
                            serverId = serverId,
                            name = cursor.getString(1),
                            type = folderTypeOf(serverId)
                        ),
                        isInTopGroup = cursor.getInt(2) == 1,
                        isIntegrate = cursor.getInt(3) == 1,
                        syncClass = cursor.getStringOrNull(4).toFolderClass(),
                        displayClass = cursor.getStringOrNull(5).toFolderClass(),
                        notifyClass = cursor.getStringOrNull(6).toFolderClass(),
                        pushClass = cursor.getStringOrNull(7).toFolderClass()
                    )
                } else {
                    null
                }
            }
        }
    }

    fun updateFolderDetails(folderDetails: FolderDetails) {
        val database = localStoreProvider.getInstance(account).database
        database.execute(false) { db ->
            val contentValues = contentValuesOf(
                "top_group" to folderDetails.isInTopGroup,
                "integrate" to folderDetails.isIntegrate,
                "poll_class" to folderDetails.syncClass.name,
                "display_class" to folderDetails.displayClass.name,
                "notify_class" to folderDetails.notifyClass.name,
                "push_class" to folderDetails.pushClass.name
            )
            db.update("folders", contentValues, "id = ?", arrayOf(folderDetails.folder.id.toString()))
        }

        saveFolderDetailsToPreferences(folderDetails)
    }

    private fun saveFolderDetailsToPreferences(folderDetails: FolderDetails) {
        val folder = folderDetails.folder
        val editor = preferences.createStorageEditor()

        val id = "${account.uuid}:${folderDetails.folder.serverId}"

        // There can be a lot of folders. For the defaults, let's not save prefs, saving space, except for INBOX.
        val inboxServerId = account.inboxFolder
        if (folderDetails.displayClass == FolderClass.NO_CLASS && folder.serverId != inboxServerId) {
            editor.remove("$id.displayMode")
        } else {
            editor.putString("$id.displayMode", folderDetails.displayClass.name)
        }

        if (folderDetails.syncClass == FolderClass.INHERITED && folder.serverId != inboxServerId) {
            editor.remove("$id.syncMode")
        } else {
            editor.putString("$id.syncMode", folderDetails.syncClass.name)
        }

        if (folderDetails.notifyClass == FolderClass.INHERITED && folder.serverId != inboxServerId) {
            editor.remove("$id.notifyMode")
        } else {
            editor.putString("$id.notifyMode", folderDetails.notifyClass.name)
        }

        if (folderDetails.pushClass == FolderClass.SECOND_CLASS && folder.serverId != inboxServerId) {
            editor.remove("$id.pushMode")
        } else {
            editor.putString("$id.pushMode", folderDetails.pushClass.name)
        }

        editor.putBoolean("$id.inTopGroup", folderDetails.isInTopGroup)
        editor.putBoolean("$id.integrate", folderDetails.isIntegrate)

        editor.commit()
    }

    private fun getDisplayFolders(db: SQLiteDatabase, displayMode: FolderMode): List<DisplayFolder> {
        val queryBuilder = StringBuilder("""
            SELECT f.id, f.server_id, f.name, f.top_group, (
                SELECT COUNT(m.id) 
                FROM messages m 
                WHERE m.folder_id = f.id AND m.empty = 0 AND m.deleted = 0 AND m.read = 0
            )
            FROM folders f
            """.trimIndent()
        )

        addDisplayClassSelection(queryBuilder, displayMode)

        val query = queryBuilder.toString()
        db.rawQuery(query, null).use { cursor ->
            val displayFolders = mutableListOf<DisplayFolder>()

            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val serverId = cursor.getString(1)
                val name = cursor.getString(2)
                val type = folderTypeOf(serverId)
                val isInTopGroup = cursor.getInt(3) == 1
                val unreadCount = cursor.getInt(4)

                val folder = Folder(id, serverId, name, type)
                displayFolders.add(DisplayFolder(folder, isInTopGroup, unreadCount))
            }

            return displayFolders
        }
    }

    private fun addDisplayClassSelection(query: StringBuilder, displayMode: FolderMode) {
        when (displayMode) {
            FolderMode.ALL -> Unit // Return all folders
            FolderMode.FIRST_CLASS -> {
                query.append(" WHERE f.display_class = '")
                        .append(FolderClass.FIRST_CLASS.name)
                        .append("'")
            }
            FolderMode.FIRST_AND_SECOND_CLASS -> {
                query.append(" WHERE f.display_class IN ('")
                        .append(FolderClass.FIRST_CLASS.name)
                        .append("', '")
                        .append(FolderClass.SECOND_CLASS.name)
                        .append("')")
            }
            FolderMode.NOT_SECOND_CLASS -> {
                query.append(" WHERE f.display_class != '")
                        .append(FolderClass.SECOND_CLASS.name)
                        .append("'")
            }
            FolderMode.NONE -> throw AssertionError("Invalid folder display mode: $displayMode")
        }
    }

    private fun folderTypeOf(serverId: String) = when (serverId) {
        account.inboxFolder -> FolderType.INBOX
        account.outboxFolder -> FolderType.OUTBOX
        account.sentFolder -> FolderType.SENT
        account.trashFolder -> FolderType.TRASH
        account.draftsFolder -> FolderType.DRAFTS
        account.archiveFolder -> FolderType.ARCHIVE
        account.spamFolder -> FolderType.SPAM
        else -> FolderType.REGULAR
    }

    private fun RemoteFolderType.toFolderType(): FolderType = when (this) {
        RemoteFolderType.REGULAR -> FolderType.REGULAR
        RemoteFolderType.INBOX -> FolderType.INBOX
        RemoteFolderType.OUTBOX -> FolderType.REGULAR // We currently don't support remote Outbox folders
        RemoteFolderType.DRAFTS -> FolderType.DRAFTS
        RemoteFolderType.SENT -> FolderType.SENT
        RemoteFolderType.TRASH -> FolderType.TRASH
        RemoteFolderType.SPAM -> FolderType.SPAM
        RemoteFolderType.ARCHIVE -> FolderType.ARCHIVE
    }

    private fun String?.toFolderClass(): FolderClass {
        return this?.let { FolderClass.valueOf(this) } ?: FolderClass.NO_CLASS
    }

    fun setIncludeInUnifiedInbox(serverId: String, includeInUnifiedInbox: Boolean) {
        val localStore = localStoreProvider.getInstance(account)
        val folder = localStore.getFolder(serverId)
        folder.isIntegrate = includeInUnifiedInbox
    }

    fun setDisplayClass(serverId: String, folderClass: FolderClass) {
        val localStore = localStoreProvider.getInstance(account)
        val folder = localStore.getFolder(serverId)
        folder.displayClass = folderClass
    }

    fun setSyncClass(serverId: String, folderClass: FolderClass) {
        val localStore = localStoreProvider.getInstance(account)
        val folder = localStore.getFolder(serverId)
        folder.syncClass = folderClass
    }

    fun setNotificationClass(serverId: String, folderClass: FolderClass) {
        val localStore = localStoreProvider.getInstance(account)
        val folder = localStore.getFolder(serverId)
        folder.notifyClass = folderClass
    }
}

data class Folder(val id: Long, val serverId: String, val name: String, val type: FolderType)

data class FolderDetails(
    val folder: Folder,
    val isInTopGroup: Boolean,
    val isIntegrate: Boolean,
    val syncClass: FolderClass,
    val displayClass: FolderClass,
    val notifyClass: FolderClass,
    val pushClass: FolderClass
)

data class DisplayFolder(
    val folder: Folder,
    val isInTopGroup: Boolean,
    val unreadCount: Int
)

enum class FolderType {
    REGULAR,
    INBOX,
    OUTBOX,
    SENT,
    TRASH,
    DRAFTS,
    ARCHIVE,
    SPAM
}
