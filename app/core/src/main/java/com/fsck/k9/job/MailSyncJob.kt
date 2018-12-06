package com.fsck.k9.job

import com.evernote.android.job.Job
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController


class MailSyncJob(
        private val messagingController: MessagingController,
        private val preferences: Preferences
) : Job() {

    override fun onRunJob(params: Params): Result {

        params.extras.getString(K9JobManager.EXTRA_KEY_ACCOUNT_UUID, null)
                ?.let { accountUuid ->

                    preferences.getAccount(accountUuid)?.let { account ->
                        messagingController.checkMailBlocking(account)
                    }
                }

        return Result.SUCCESS
    }

    companion object {
        const val TAG: String = "MailSyncJob"
    }

}
