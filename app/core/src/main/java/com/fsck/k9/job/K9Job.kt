package com.fsck.k9.job

import com.evernote.android.job.Job
import com.fsck.k9.Account


abstract class K9Job : Job() {

    companion object {
        const val EXTRA_KEY_ACCOUNT_UUID = "param_key_account_uuid"
    }

    abstract fun scheduleJob(account: Account)
}