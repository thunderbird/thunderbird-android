package com.fsck.k9.mailstore

import android.content.Context
import java.io.File
import net.thunderbird.feature.account.AccountId

internal class AndroidStorageFilesProvider(
    private val context: Context,
    private val accountId: AccountId,
) : StorageFilesProvider {
    override fun getDatabaseFile(): File {
        return context.getDatabasePath("$accountId.db")
    }

    override fun getAttachmentDirectory(): File {
        return context.getDatabasePath("$accountId.db_att")
    }
}
