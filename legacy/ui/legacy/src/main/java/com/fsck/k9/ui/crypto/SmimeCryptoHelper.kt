package com.fsck.k9.ui.crypto

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.IntentCompat
import com.ciphermail.smime.api.ISmimeService
import com.ciphermail.smime.api.SmimeDecryptionResult
import com.ciphermail.smime.api.SmimeError
import com.ciphermail.smime.api.SmimeSignatureResult
import com.ciphermail.smime.api.util.SmimeApi
import com.ciphermail.smime.api.util.SmimeServiceConnection
import com.fsck.k9.crypto.MessageCryptoStructureDetector
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.internet.MimeBodyPart
import com.fsck.k9.mailstore.CryptoResultAnnotation
import com.fsck.k9.mailstore.MessageCryptoAnnotations
import com.fsck.k9.mailstore.MimePartStreamParser
import com.fsck.k9.provider.DecryptedFileProvider
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.logging.legacy.Log
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Receive-side S/MIME orchestration for the message-view path.
 *
 * Mirrors [MessageCryptoHelper] for OpenPGP: detects S/MIME parts in an incoming message,
 * binds to the user's configured S/MIME provider over AIDL, asks it to decrypt and verify, and
 * stitches the result back into the message's [MessageCryptoAnnotations]. Owned by
 * `MessageLoaderHelper` and lives for the lifetime of a single message view.
 *
 * The helper handles the asynchronous `RESULT_CODE_USER_INTERACTION_REQUIRED` dance: when the
 * provider's keystore is locked, the returned [PendingIntent] is queued and surfaced to the host
 * via `MessageCryptoCallback.startPendingIntentForCryptoHelper`; on `RESULT_OK` from the
 * resulting passphrase dialog, [onActivityResult] re-runs the decrypt path with the now-unlocked
 * keystore.
 *
 * One [SmimeCryptoHelper] instance is bound to one provider package (the account's
 * `smimeProvider` setting). If the account is later reconfigured to a different provider, the
 * host must construct a new instance — see [isConfiguredForSmimeProvider].
 */
@Suppress("TooManyFunctions") // state-machine helper; splitting would harm readability
class SmimeCryptoHelper(context: Context, private val smimeProvider: String) {
    private val context: Context = context.applicationContext
    private val callbackLock = Any()

    private var callback: MessageCryptoCallback? = null
    private var currentMessage: Message? = null
    private var messageAnnotations: MessageCryptoAnnotations? = null
    private var queuedResult: MessageCryptoAnnotations? = null
    private var queuedPendingIntent: PendingIntent? = null
    private var isCancelled: Boolean = false

    private var pendingSmimePart: Part? = null
    private var smimeServiceConnection: SmimeServiceConnection? = null

    /**
     * @return `true` if this helper is bound to the given provider package and can be reused;
     *         `false` if the host must rebuild it.
     */
    fun isConfiguredForSmimeProvider(provider: String): Boolean = smimeProvider == provider

    /**
     * Begin (or resume, after a configuration change) S/MIME processing for a message.
     *
     * If a previous call is still in flight for the same `message`, the callback is rebound to
     * the existing operation rather than starting a new one. Calling with a different message
     * after one is already in flight is a programming error.
     */
    fun asyncStartOrResumeProcessingMessage(message: Message, callback: MessageCryptoCallback) {
        if (currentMessage != null) {
            reattachCallback(message, callback)
            return
        }
        messageAnnotations = MessageCryptoAnnotations()
        currentMessage = message
        this.callback = callback
        startProcessing()
    }

    private fun startProcessing() {
        val message = currentMessage ?: return
        val smimePart = MessageCryptoStructureDetector.findPrimaryEncryptedOrSignedPart(message, ArrayList())
        if (smimePart == null || !MessageCryptoStructureDetector.isSmimePart(smimePart)) {
            callbackReturnResult()
            return
        }
        pendingSmimePart = smimePart
        connectToSmimeService(smimePart)
    }

    private fun connectToSmimeService(smimePart: Part) {
        smimeServiceConnection = SmimeServiceConnection(
            context,
            smimeProvider,
            object : SmimeServiceConnection.OnBound {
                override fun onBound(service: ISmimeService) {
                    processSmimePartAsync(service, smimePart)
                }

                override fun onError(e: Exception) {
                    Log.e(e, "Couldn't connect to SmimeService")
                    addGenericErrorAnnotation(smimePart)
                    callbackReturnResult()
                }
            },
        ).apply { bindToService() }
    }

    private fun processSmimePartAsync(service: ISmimeService, smimePart: Part) {
        val api = SmimeApi(service)
        val decryptIntent = Intent(SmimeApi.ACTION_DECRYPT_VERIFY).apply {
            putExtra(SmimeApi.EXTRA_API_VERSION, SmimeApi.API_VERSION)
        }

        val messageBytes = try {
            ByteArrayOutputStream().apply { currentMessage?.writeTo(this) }.toByteArray()
        } catch (e: IOException) {
            Log.e(e, "Failed to serialize message for S/MIME processing")
            addGenericErrorAnnotation(smimePart)
            callbackReturnResult()
            return
        } catch (e: MessagingException) {
            Log.e(e, "Failed to serialize message for S/MIME processing")
            addGenericErrorAnnotation(smimePart)
            callbackReturnResult()
            return
        }

        val decryptedOutput = ByteArrayOutputStream()
        api.executeApiAsync(decryptIntent, ByteArrayInputStream(messageBytes), decryptedOutput) { result ->
            if (!isCancelled) {
                onSmimeOperationResult(smimePart, result, decryptedOutput.toByteArray())
            }
        }
    }

    private fun onSmimeOperationResult(smimePart: Part, result: Intent, decryptedBytes: ByteArray) {
        result.setExtrasClassLoader(SmimeError::class.java.classLoader)
        when (result.getIntExtra(SmimeApi.RESULT_CODE, SmimeApi.RESULT_CODE_ERROR)) {
            SmimeApi.RESULT_CODE_USER_INTERACTION_REQUIRED -> {
                val pendingIntent = IntentCompat.getParcelableExtra(
                    result, SmimeApi.RESULT_INTENT, PendingIntent::class.java,
                )
                if (pendingIntent != null) {
                    callbackPendingIntent(pendingIntent)
                } else {
                    addGenericErrorAnnotation(smimePart)
                    callbackReturnResult()
                }
            }
            SmimeApi.RESULT_CODE_ERROR -> {
                val error = IntentCompat.getParcelableExtra(result, SmimeApi.RESULT_ERROR, SmimeError::class.java)
                Log.w("S/MIME API error: %s", error?.message ?: "unknown")
                addApiErrorAnnotation(smimePart, error)
                callbackReturnResult()
            }
            SmimeApi.RESULT_CODE_SUCCESS -> {
                onSmimeOperationSuccess(smimePart, result, decryptedBytes)
                callbackReturnResult()
            }
            else -> {
                Log.e("Unknown S/MIME result code: %d", result.getIntExtra(SmimeApi.RESULT_CODE, -1))
                addGenericErrorAnnotation(smimePart)
                callbackReturnResult()
            }
        }
    }

    private fun onSmimeOperationSuccess(smimePart: Part, result: Intent, decryptedBytes: ByteArray) {
        val decryptionResult = IntentCompat.getParcelableExtra(
            result, SmimeApi.RESULT_DECRYPTION, SmimeDecryptionResult::class.java,
        )
        val signatureResult = IntentCompat.getParcelableExtra(
            result, SmimeApi.RESULT_SIGNATURE, SmimeSignatureResult::class.java,
        )
        val pendingIntent = IntentCompat.getParcelableExtra(
            result, SmimeApi.RESULT_INTENT, PendingIntent::class.java,
        )

        val wasEncrypted = decryptionResult?.result == SmimeDecryptionResult.RESULT_ENCRYPTED
        val decryptedPart: MimeBodyPart? = if (wasEncrypted && decryptedBytes.isNotEmpty()) {
            try {
                MimePartStreamParser.parse(
                    DecryptedFileProvider.getFileFactory(context),
                    ByteArrayInputStream(decryptedBytes),
                )
            } catch (e: IOException) {
                Log.e(e, "Error parsing decrypted S/MIME part")
                null
            } catch (e: MessagingException) {
                Log.e(e, "Error parsing decrypted S/MIME part")
                null
            }
        } else {
            null
        }

        val annotation = CryptoResultAnnotation.createSmimeResultAnnotation(
            decryptionResult, signatureResult, pendingIntent, decryptedPart, false,
        )
        messageAnnotations?.put(smimePart, annotation)
    }

    private fun addGenericErrorAnnotation(smimePart: Part) {
        val annotation = CryptoResultAnnotation.createErrorAnnotation(
            CryptoResultAnnotation.CryptoError.SMIME_ENCRYPTED_API_ERROR, null,
        )
        messageAnnotations?.put(smimePart, annotation)
    }

    private fun addApiErrorAnnotation(smimePart: Part, error: SmimeError?) {
        val annotation = if (MessageCryptoStructureDetector.isSmimeSignedMultipart(smimePart)) {
            CryptoResultAnnotation.createSmimeSignatureErrorAnnotation(error, null)
        } else {
            CryptoResultAnnotation.createSmimeEncryptionErrorAnnotation(error)
        }
        messageAnnotations?.put(smimePart, annotation)
    }

    /**
     * Hook for the host activity to forward results from the provider's passphrase dialog
     * (launched via a [PendingIntent] when the service returned
     * `RESULT_CODE_USER_INTERACTION_REQUIRED`). On `RESULT_OK` the decrypt path is retried;
     * otherwise a generic error annotation is added and the host callback is invoked.
     */
    fun onActivityResult(requestCode: Int, resultCode: Int, @Suppress("UnusedParameter") data: Intent?) {
        if (isCancelled) return
        check(requestCode == REQUEST_CODE_USER_INTERACTION) {
            "got an activity result that wasn't meant for us. this is a bug!"
        }
        val smimePart = pendingSmimePart
        if (resultCode == Activity.RESULT_OK && smimePart != null) {
            synchronized(callbackLock) { queuedPendingIntent = null }
            connectToSmimeService(smimePart)
        } else {
            if (smimePart != null) addGenericErrorAnnotation(smimePart)
            callbackReturnResult()
        }
    }

    fun resumeCryptoOperationIfNecessary() {
        synchronized(callbackLock) {
            if (queuedPendingIntent != null) deliverResult()
        }
    }

    /**
     * Cancel any in-flight operation and release the service binding. Safe to call multiple times.
     * After this call, no callbacks fire.
     */
    fun cancelIfRunning() {
        isCancelled = true
        detachCallback()
        smimeServiceConnection?.unbindFromService()
        smimeServiceConnection = null
    }

    /**
     * Detach the host callback without cancelling the in-flight operation. Results produced while
     * detached are queued and delivered when a new callback is reattached via
     * [asyncStartOrResumeProcessingMessage]. Used when the host activity is being recreated
     * (configuration change).
     */
    fun detachCallback() {
        synchronized(callbackLock) { callback = null }
    }

    private fun reattachCallback(message: Message, callback: MessageCryptoCallback) {
        require(message == currentMessage) { "Callback may only be reattached for the same message!" }
        synchronized(callbackLock) {
            this.callback = callback
            if (queuedResult != null || queuedPendingIntent != null) deliverResult()
        }
    }

    private fun callbackReturnResult() {
        synchronized(callbackLock) {
            smimeServiceConnection?.unbindFromService()
            smimeServiceConnection = null
            queuedResult = messageAnnotations
            messageAnnotations = null
            deliverResult()
        }
    }

    private fun callbackPendingIntent(pendingIntent: PendingIntent) {
        synchronized(callbackLock) {
            queuedPendingIntent = pendingIntent
            deliverResult()
        }
    }

    private fun deliverResult() {
        if (isCancelled) return
        val cb = callback
        if (cb == null) {
            Log.d("Keeping S/MIME crypto result in queue for later delivery")
            return
        }
        val result = queuedResult
        val pending = queuedPendingIntent
        when {
            result != null -> {
                queuedResult = null
                cb.onCryptoOperationsFinished(result)
            }
            pending != null -> {
                if (cb.startPendingIntentForCryptoHelper(pending.intentSender, REQUEST_CODE_USER_INTERACTION)) {
                    queuedPendingIntent = null
                }
            }
            else -> error("deliverResult() called with no result!")
        }
    }

    companion object {
        private const val REQUEST_CODE_USER_INTERACTION = 125
    }
}
