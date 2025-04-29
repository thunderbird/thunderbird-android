package com.fsck.k9.ui.choosefolder

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import app.k9mail.legacy.message.controller.MessageReference
import com.fsck.k9.ui.choosefolder.ChooseFolderActivity.Action

class ChooseFolderResultContract(
    private val action: Action,
) : ActivityResultContract<ChooseFolderResultContract.Input, ChooseFolderResultContract.Result?>() {

    override fun createIntent(context: Context, input: Input): Intent {
        return ChooseFolderActivity.buildLaunchIntent(
            context = context,
            action = action,
            accountUuid = input.accountUuid,
            currentFolderId = input.currentFolderId,
            scrollToFolderId = input.scrollToFolderId,
            messageReference = input.messageReference,
        )
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Result? {
        return intent.takeIf { resultCode == Activity.RESULT_OK }?.let {
            Result(
                folderId = it.getLongExtra(ChooseFolderActivity.RESULT_SELECTED_FOLDER_ID, -1L),
                folderDisplayName = it.getStringExtra(ChooseFolderActivity.RESULT_FOLDER_DISPLAY_NAME)!!,
                messageReference = it.getStringExtra(ChooseFolderActivity.RESULT_MESSAGE_REFERENCE),
            )
        }
    }

    data class Input(
        val accountUuid: String,
        val currentFolderId: Long? = null,
        val scrollToFolderId: Long? = null,
        val messageReference: MessageReference? = null,
    )

    data class Result(
        val folderId: Long,
        val folderDisplayName: String,
        val messageReference: String?,
    )
}
