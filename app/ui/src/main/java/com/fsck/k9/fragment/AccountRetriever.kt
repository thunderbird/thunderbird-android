package com.fsck.k9.fragment

import android.database.Cursor
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.fragment.MLFProjectionInfo.ACCOUNT_UUID_COLUMN

class AccountRetriever constructor(
        private val preferences: Preferences
) : (Cursor) -> Account {

    override fun invoke(cursor: Cursor): Account {
        val accountUuid = cursor.getString(ACCOUNT_UUID_COLUMN)
        return preferences.getAccount(accountUuid)
    }
}