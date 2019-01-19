package com.fsck.k9.job

import com.evernote.android.job.Job
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import timber.log.Timber


class PusherRefreshJob(
        private val messagingController: MessagingController,
        private val preferences: Preferences
) : Job() {

    override fun onRunJob(params: Params): Result {

        params.extras.getString(K9JobManager.EXTRA_KEY_ACCOUNT_UUID, null)
                ?.let { accountUuid ->

                    preferences.getAccount(accountUuid)?.let { account ->
                        try {

                            // Refresh pushers
                            Timber.i("Refreshing pusher for ${account.description}")
                            messagingController.getPusher(account)?.refresh()

                            // Whenever we refresh our pushers, send any unsent messages
                            Timber.d("trying to send mail in all folders!")
                            messagingController.sendPendingMessages(null)

                        } catch (e: Exception) {
                            Timber.e(e, "Exception while refreshing pushers")
                            return Result.RESCHEDULE
                        }
                    }
                }

        return Result.SUCCESS
    }


    companion object {
        const val TAG: String = "PusherRefreshJob"
    }

}