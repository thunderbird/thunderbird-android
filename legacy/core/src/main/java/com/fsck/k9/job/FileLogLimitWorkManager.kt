package com.fsck.k9.job

import androidx.lifecycle.LiveData
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class FileLogLimitWorkManager(
    private val workManager: WorkManager,
) {
    fun scheduleFileLogTimeLimit(contentUriString: String): LiveData<WorkInfo?> {
        val data = workDataOf("exportUriString" to contentUriString)
        val workRequest: OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<SyncDebugWorker>()
                .setInitialDelay(TWENTY_FOUR_HOURS, TimeUnit.HOURS)
                .setInputData(data)
                .addTag(SYNC_TAG)
                .setBackoffCriteria(BackoffPolicy.LINEAR, INITIAL_BACKOFF_DELAY_MINUTES, TimeUnit.MINUTES)
                .build()
        workManager.enqueueUniqueWork(SYNC_TAG, ExistingWorkPolicy.REPLACE, workRequest)
        return workManager.getWorkInfoByIdLiveData(workRequest.id)
    }

    fun cancelFileLogTimeLimit() {
        workManager.cancelUniqueWork(SYNC_TAG)
    }

    companion object {
        const val SYNC_TAG = "sync_debug_timer"
        private const val INITIAL_BACKOFF_DELAY_MINUTES = 5L
        private const val TWENTY_FOUR_HOURS = 24L
    }
}
