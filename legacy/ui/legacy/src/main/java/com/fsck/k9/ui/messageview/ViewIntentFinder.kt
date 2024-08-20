package com.fsck.k9.ui.messageview

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.annotation.WorkerThread
import com.fsck.k9.helper.MimeTypeUtil
import com.fsck.k9.provider.AttachmentTempFileProvider

/**
 * Tries to find an [Intent.ACTION_VIEW] Intent that can be used to view an attachment.
 */
internal class ViewIntentFinder(private val context: Context) {
    @WorkerThread
    fun getBestViewIntent(contentUri: Uri, displayName: String?, mimeType: String): Intent {
        val inferredMimeType = MimeTypeUtil.getMimeTypeByExtension(displayName)

        var resolvedIntentInfo: IntentAndResolvedActivitiesCount
        if (MimeTypeUtil.isDefaultMimeType(mimeType)) {
            resolvedIntentInfo = getViewIntentForMimeType(contentUri, inferredMimeType)
        } else {
            resolvedIntentInfo = getViewIntentForMimeType(contentUri, mimeType)
            if (!resolvedIntentInfo.hasResolvedActivities() && inferredMimeType != mimeType) {
                resolvedIntentInfo = getViewIntentForMimeType(contentUri, inferredMimeType)
            }
        }

        if (!resolvedIntentInfo.hasResolvedActivities()) {
            resolvedIntentInfo = getViewIntentForMimeType(contentUri, MimeTypeUtil.DEFAULT_ATTACHMENT_MIME_TYPE)
        }

        return resolvedIntentInfo.intent
    }

    private fun getViewIntentForMimeType(contentUri: Uri, mimeType: String): IntentAndResolvedActivitiesCount {
        val contentUriIntent = createViewIntentForAttachmentProviderUri(contentUri, mimeType)
        val contentUriActivitiesCount = getResolvedIntentActivitiesCount(contentUriIntent)

        return IntentAndResolvedActivitiesCount(contentUriIntent, contentUriActivitiesCount)
    }

    private fun createViewIntentForAttachmentProviderUri(contentUri: Uri, mimeType: String): Intent {
        val uri = AttachmentTempFileProvider.getMimeTypeUri(contentUri, mimeType)

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, mimeType)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addUiIntentFlags(intent)

        return intent
    }

    private fun addUiIntentFlags(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
    }

    private fun getResolvedIntentActivitiesCount(intent: Intent): Int {
        val packageManager = context.packageManager

        val resolveInfos =
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)

        return resolveInfos.size
    }

    private class IntentAndResolvedActivitiesCount(val intent: Intent, private val activitiesCount: Int) {
        fun hasResolvedActivities(): Boolean {
            return activitiesCount > 0
        }
    }
}
