package com.fsck.k9.account

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import org.koin.core.KoinComponent
import org.koin.core.inject

/**
 * A [JobIntentService] to clean an account in the background.
 */
class AccountCleanerService : JobIntentService(), KoinComponent {
    private val accountCleaner: AccountCleaner by inject()

    override fun onHandleWork(intent: Intent) {
        val accountUuid = intent.getStringExtra(ARG_ACCOUNT_UUID)
                ?: throw IllegalArgumentException("No account UUID provided")

        accountCleaner.clearAccount(accountUuid)
    }

    companion object {
        private const val JOB_ID = 1
        private const val ARG_ACCOUNT_UUID = "accountUuid"

        fun enqueueClearAccountJob(context: Context, accountUuid: String) {
            val workIntent = Intent().apply {
                putExtra(ARG_ACCOUNT_UUID, accountUuid)
            }

            JobIntentService.enqueueWork(context, AccountCleanerService::class.java, JOB_ID, workIntent)
        }
    }
}
