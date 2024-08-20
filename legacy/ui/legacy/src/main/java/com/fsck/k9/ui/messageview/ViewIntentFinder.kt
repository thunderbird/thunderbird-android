package com.fsck.k9.ui.messageview

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.annotation.WorkerThread
import com.fsck.k9.helper.MimeTypeUtil
import com.fsck.k9.helper.MimeTypeUtil.DEFAULT_ATTACHMENT_MIME_TYPE
import com.fsck.k9.provider.AttachmentTempFileProvider

/**
 * Tries to find an [Intent.ACTION_VIEW] Intent that can be used to view an attachment.
 */
internal class ViewIntentFinder(private val context: Context) {
    @WorkerThread
    fun getBestViewIntent(contentUri: Uri, displayName: String?, mimeType: String): Intent {
        val inferredMimeType = MimeTypeUtil.getMimeTypeByExtension(displayName)

        var resolvedIntentInfo: QueryIntentResult
        if (MimeTypeUtil.isDefaultMimeType(mimeType)) {
            resolvedIntentInfo = getViewIntentForMimeType(contentUri, inferredMimeType)
        } else {
            resolvedIntentInfo = getViewIntentForMimeType(contentUri, mimeType)
            if (resolvedIntentInfo.hasNoResolvedActivities() && inferredMimeType != mimeType) {
                resolvedIntentInfo = getViewIntentForMimeType(contentUri, inferredMimeType)
            }
        }

        if (resolvedIntentInfo.hasNoResolvedActivities()) {
            resolvedIntentInfo = getViewIntentForMimeType(contentUri, DEFAULT_ATTACHMENT_MIME_TYPE)
        }

        return resolvedIntentInfo.intent
    }

    private fun getViewIntentForMimeType(contentUri: Uri, mimeType: String): QueryIntentResult {
        val intent = createViewIntentForAttachmentProviderUri(contentUri, mimeType)
        val activitiesCount = getResolvedIntentActivitiesCount(intent)

        return QueryIntentResult(intent, activitiesCount)
    }

    private fun createViewIntentForAttachmentProviderUri(contentUri: Uri, mimeType: String): Intent {
        val uri = AttachmentTempFileProvider.getMimeTypeUri(contentUri, mimeType)

        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addUiIntentFlags()
        }
    }

    private fun Intent.addUiIntentFlags() {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
    }

    private fun getResolvedIntentActivitiesCount(intent: Intent): Int {
        val packageManager = context.packageManager
        val resolveInfos = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)

        return resolveInfos.size
    }
}

private class QueryIntentResult(
    val intent: Intent,
    private val activitiesCount: Int,
) {
    fun hasNoResolvedActivities(): Boolean {
        return activitiesCount == 0
    }
}
