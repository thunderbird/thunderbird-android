package com.fsck.k9.ui.messageview

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.annotation.WorkerThread
import app.k9mail.legacy.message.controller.SimpleMessagingListener
import com.fsck.k9.Preferences.Companion.getPreferences
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.Part
import com.fsck.k9.mailstore.AttachmentViewInfo
import com.fsck.k9.mailstore.LocalPart
import com.fsck.k9.provider.AttachmentTempFileProvider
import com.fsck.k9.ui.R
import java.io.IOException
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.logging.legacy.Log.e
import org.apache.commons.io.IOUtils

class AttachmentController internal constructor(
    private val context: Context,
    private val controller: MessagingController,
    private val messageViewFragment: MessageViewFragment,
    private val attachment: AttachmentViewInfo,
) {
    private val viewIntentFinder = ViewIntentFinder(context)

    fun viewAttachment(scope: CoroutineScope) {
        scope.launch {
            if (!attachment.isContentAvailable) {
                val success = downloadAttachment()
                if (success) {
                    messageViewFragment.refreshAttachmentThumbnail(attachment)
                    viewLocalAttachment()
                }
            } else {
                viewLocalAttachment()
            }
        }
    }

    fun saveAttachmentTo(scope: CoroutineScope, documentUri: Uri?) {
        if (documentUri == null) return
        scope.launch {
            if (!attachment.isContentAvailable) {
                val success = downloadAttachment()
                if (success) {
                    messageViewFragment.refreshAttachmentThumbnail(attachment)
                    saveLocalAttachmentTo(documentUri)
                }
            } else {
                saveLocalAttachmentTo(documentUri)
            }
        }
    }

    fun saveAttachmentToDirectory(scope: CoroutineScope, directoryUri: Uri?) {
        if (directoryUri == null) return
        scope.launch {
            if (!attachment.isContentAvailable) {
                val success = downloadAttachment()
                if (success) {
                    messageViewFragment.refreshAttachmentThumbnail(attachment)
                    saveLocalAttachmentToDirectory(directoryUri)
                }
            } else {
                saveLocalAttachmentToDirectory(directoryUri)
            }
        }
    }

    private suspend fun saveLocalAttachmentTo(documentUri: Uri) {
        val success = withContext(Dispatchers.IO) {
            try {
                writeAttachment(documentUri)
                true
            } catch (e: IOException) {
                e(e, "Error saving attachment")
                false
            }
        }
        if (!success) displayAttachmentNotSavedMessage()
    }

    private suspend fun saveLocalAttachmentToDirectory(directoryUri: Uri) {
        val success = withContext(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val treeId = DocumentsContract.getTreeDocumentId(directoryUri)
                val rootDocUri = DocumentsContract.buildDocumentUriUsingTree(directoryUri, treeId)

                val documentUri = DocumentsContract.createDocument(
                    contentResolver,
                    rootDocUri,
                    attachment.mimeType,
                    attachment.displayName
                ) ?: return@withContext false

                writeAttachment(documentUri)
                true
            } catch (e: IOException) {
                e(e, "Error saving attachment to directory")
                false
            }
        }
        if (!success) displayAttachmentNotSavedMessage()
    }

    private suspend fun downloadAttachment(): Boolean = suspendCancellableCoroutine { continuation ->
        val localPart = attachment.part as LocalPart
        val account = getPreferences().getAccount(localPart.accountUuid)

        messageViewFragment.showAttachmentLoadingDialog()
        controller.loadAttachment(account, localPart.message, attachment.part,
            object : SimpleMessagingListener() {
            override fun loadAttachmentFinished(account: LegacyAccountDto?, message: Message?, part: Part?) {
                attachment.setContentAvailable()
                messageViewFragment.hideAttachmentLoadingDialogOnMainThread()
                if (continuation.isActive) {
                    continuation.resume(true)
                }
            }

            override fun loadAttachmentFailed(account: LegacyAccountDto?, message: Message?, part: Part?, reason: String?) {
                messageViewFragment.hideAttachmentLoadingDialogOnMainThread()
                if (continuation.isActive) {
                    continuation.resume(false)
                }
            }
        })
    }

    private suspend fun viewLocalAttachment() {
        val intent = withContext(Dispatchers.IO) { getBestViewIntent() }
        if (intent != null) {
            try {
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                val errorMsg = context.getString(R.string.message_view_no_viewer, attachment.mimeType)
                displayMessageToUser(errorMsg)
            }
        }
    }

    @Throws(IOException::class)
    private fun writeAttachment(documentUri: Uri) {
        val contentResolver = context.contentResolver
        contentResolver.openInputStream(attachment.internalUri).use { inputStream ->
            contentResolver.openOutputStream(documentUri, "wt").use { outputStream ->
                if (inputStream != null && outputStream != null) {
                    IOUtils.copy(inputStream, outputStream)
                    outputStream.flush()
                }
            }
        }
    }

    @WorkerThread
    private fun getBestViewIntent(): Intent? {
        return try {
            val intentDataUri = AttachmentTempFileProvider.createTempUriForContentUri(
                context, attachment.internalUri, attachment.displayName
            )
            viewIntentFinder.getBestViewIntent(intentDataUri, attachment.displayName, attachment.mimeType)
        } catch (e: IOException) {
            e(e, "Error creating temp file for attachment!")
            null
        }
    }

    private fun displayAttachmentNotSavedMessage() {
        displayMessageToUser(context.getString(R.string.message_view_status_attachment_not_saved))
    }

    private fun displayMessageToUser(message: String?) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}
