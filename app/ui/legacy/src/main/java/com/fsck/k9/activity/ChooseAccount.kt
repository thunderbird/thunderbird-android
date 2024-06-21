package com.fsck.k9.activity

import android.content.Intent
import android.os.Bundle
import com.fsck.k9.BaseAccount
import com.fsck.k9.ui.R

class ChooseAccount : AccountList() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.choose_account_title)
    }

    override fun onAccountSelected(account: BaseAccount) {
        val intent = Intent().apply {
            putExtra(EXTRA_ACCOUNT_UUID, account.uuid)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    companion object {
        const val EXTRA_ACCOUNT_UUID: String = "com.fsck.k9.ChooseAccount_account_uuid"
    }
}
