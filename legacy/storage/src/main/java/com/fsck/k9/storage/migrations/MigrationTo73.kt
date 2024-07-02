package com.fsck.k9.storage.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import app.k9mail.core.android.common.database.map
import com.fsck.k9.controller.MessagingControllerCommands.PendingAppend
import com.fsck.k9.controller.MessagingControllerCommands.PendingCommand
import com.fsck.k9.controller.MessagingControllerCommands.PendingDelete
import com.fsck.k9.controller.MessagingControllerCommands.PendingExpunge
import com.fsck.k9.controller.MessagingControllerCommands.PendingMarkAllAsRead
import com.fsck.k9.controller.MessagingControllerCommands.PendingMoveAndMarkAsRead
import com.fsck.k9.controller.MessagingControllerCommands.PendingMoveOrCopy
import com.fsck.k9.controller.MessagingControllerCommands.PendingSetFlag
import com.fsck.k9.controller.PendingCommandSerializer
import com.squareup.moshi.Moshi
import timber.log.Timber

internal class MigrationTo73(private val db: SQLiteDatabase) {
    private val serializer = PendingCommandSerializer.getInstance()
    private val moshi = Moshi.Builder().build()
    private val legacyAdapters = listOf(
        "append" to moshi.adapter(LegacyPendingAppend::class.java),
        "mark_all_as_read" to moshi.adapter(LegacyPendingMarkAllAsRead::class.java),
        "set_flag" to moshi.adapter(LegacyPendingSetFlag::class.java),
        "delete" to moshi.adapter(LegacyPendingDelete::class.java),
        "expunge" to moshi.adapter(LegacyPendingExpunge::class.java),
        "move_or_copy" to moshi.adapter(LegacyPendingMoveOrCopy::class.java),
        "move_and_mark_as_read" to moshi.adapter(LegacyPendingMoveAndMarkAsRead::class.java),
    ).toMap()

    fun rewritePendingCommandsToUseFolderIds() {
        val pendingCommands = loadPendingCommands()
        rewritePendingCommands(pendingCommands)
    }

    private fun loadPendingCommands(): Map<Long, LegacyPendingCommand> {
        return db.rawQuery(
            "SELECT id, command, data FROM pending_commands WHERE command != 'empty_trash'",
            null,
        ).use { cursor ->
            cursor.map {
                val commandId = cursor.getLong(0)
                val command = cursor.getString(1)
                val data = cursor.getString(2)

                val pendingCommand = deserialize(command, data)
                commandId to pendingCommand
            }.toMap()
        }
    }

    private fun rewritePendingCommands(pendingCommands: Map<Long, LegacyPendingCommand>) {
        for ((commandId, pendingCommand) in pendingCommands) {
            when (pendingCommand) {
                is LegacyPendingAppend -> rewritePendingAppend(commandId, pendingCommand)
                is LegacyPendingMarkAllAsRead -> rewritePendingMarkAllAsRead(commandId, pendingCommand)
                is LegacyPendingSetFlag -> rewritePendingSetFlag(commandId, pendingCommand)
                is LegacyPendingDelete -> rewritePendingDelete(commandId, pendingCommand)
                is LegacyPendingExpunge -> rewritePendingExpunge(commandId, pendingCommand)
                is LegacyPendingMoveOrCopy -> rewritePendingMoveOrCopy(commandId, pendingCommand)
                is LegacyPendingMoveAndMarkAsRead -> rewritePendingMoveAndMarkAsRead(commandId, pendingCommand)
            }
        }
    }

    private fun rewritePendingAppend(commandId: Long, legacyPendingCommand: LegacyPendingAppend) {
        rewriteOrRemovePendingCommand(commandId, legacyPendingCommand.folder) { (folderId) ->
            PendingAppend.create(folderId, legacyPendingCommand.uid)
        }
    }

    private fun rewritePendingMarkAllAsRead(commandId: Long, legacyPendingCommand: LegacyPendingMarkAllAsRead) {
        rewriteOrRemovePendingCommand(commandId, legacyPendingCommand.folder) { (folderId) ->
            PendingMarkAllAsRead.create(folderId)
        }
    }

    private fun rewritePendingSetFlag(commandId: Long, legacyPendingCommand: LegacyPendingSetFlag) {
        rewriteOrRemovePendingCommand(commandId, legacyPendingCommand.folder) { (folderId) ->
            PendingSetFlag.create(
                folderId,
                legacyPendingCommand.newState,
                legacyPendingCommand.flag,
                legacyPendingCommand.uids,
            )
        }
    }

    private fun rewritePendingDelete(commandId: Long, legacyPendingCommand: LegacyPendingDelete) {
        rewriteOrRemovePendingCommand(commandId, legacyPendingCommand.folder) { (folderId) ->
            PendingDelete.create(folderId, legacyPendingCommand.uids)
        }
    }

    private fun rewritePendingExpunge(commandId: Long, legacyPendingCommand: LegacyPendingExpunge) {
        rewriteOrRemovePendingCommand(commandId, legacyPendingCommand.folder) { (folderId) ->
            PendingExpunge.create(folderId)
        }
    }

    private fun rewritePendingMoveOrCopy(commandId: Long, legacyPendingCommand: LegacyPendingMoveOrCopy) {
        rewriteOrRemovePendingCommand(
            commandId,
            legacyPendingCommand.srcFolder,
            legacyPendingCommand.destFolder,
        ) { (srcFolderId, destFolderId) ->
            PendingMoveOrCopy.create(
                srcFolderId,
                destFolderId,
                legacyPendingCommand.isCopy,
                legacyPendingCommand.newUidMap,
            )
        }
    }

    private fun rewritePendingMoveAndMarkAsRead(commandId: Long, legacyPendingCommand: LegacyPendingMoveAndMarkAsRead) {
        rewriteOrRemovePendingCommand(
            commandId,
            legacyPendingCommand.srcFolder,
            legacyPendingCommand.destFolder,
        ) { (srcFolderId, destFolderId) ->
            PendingMoveAndMarkAsRead.create(srcFolderId, destFolderId, legacyPendingCommand.newUidMap)
        }
    }

    private fun rewriteOrRemovePendingCommand(
        commandId: Long,
        vararg folderServerIds: String,
        convertPendingCommand: (folderIds: List<Long>) -> PendingCommand,
    ) {
        val folderIds = folderServerIds.map {
            loadFolderId(it)
        }

        if (folderIds.any { it == null }) {
            Timber.w("Couldn't find folder ID for pending command with database ID $commandId. Removing entry.")
            removePendingCommand(commandId)
        } else {
            val pendingCommand = convertPendingCommand(folderIds.filterNotNull())
            updatePendingCommand(commandId, pendingCommand)
        }
    }

    private fun updatePendingCommand(commandId: Long, pendingCommand: PendingCommand) {
        val contentValues = ContentValues().apply {
            put("data", serializer.serialize(pendingCommand))
        }
        db.update("pending_commands", contentValues, "id = ?", arrayOf(commandId.toString()))
    }

    private fun removePendingCommand(commandId: Long) {
        db.delete("pending_commands", "id = ?", arrayOf(commandId.toString()))
    }

    private fun loadFolderId(folderServerId: String): Long? {
        return db.rawQuery("SELECT id from folders WHERE server_id = ?", arrayOf(folderServerId)).use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getLong(0)
            } else {
                null
            }
        }
    }

    private fun deserialize(commandName: String, data: String): LegacyPendingCommand {
        val adapter = legacyAdapters[commandName] ?: error("Unsupported pending command type!")
        return adapter.fromJson(data) ?: error("Error deserializing pending command")
    }
}
