package com.fsck.k9.message

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.IntentCompat
import app.k9mail.legacy.di.DI
import com.ciphermail.smime.api.ISmimeService
import com.ciphermail.smime.api.SmimeError
import com.ciphermail.smime.api.util.SmimeApi
import com.ciphermail.smime.api.util.SmimeServiceConnection
import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.mail.Message.RecipientType
import com.fsck.k9.mail.BoundaryGenerator
import com.fsck.k9.mail.internet.MessageIdGenerator
import com.fsck.k9.mail.internet.MimeMessage
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.preference.GeneralSettingsManager
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Send-side S/MIME orchestration for the compose path.
 *
 * Companion to `PgpMessageBuilder`: builds the plain MIME message via [MessageBuilder.build],
 * binds to the user's configured S/MIME provider, calls `ACTION_SIGN_AND_ENCRYPT` over the AIDL
 * pipe, and parses the resulting wrapped message back into a [MimeMessage] that can be queued
 * for SMTP transport.
 *
 * **Threading.** [buildMessageInternal] is called on `AsyncTask.doInBackground` from the compose
 * activity, so this class blocks on a [CountDownLatch] for the service-bind callback rather than
 * threading callbacks through to the UI. The blocking is intentional and safe because the caller
 * is already off the main thread.
 *
 * **Drafts and "send without crypto" short-circuit.** If [isDraft] or both [shouldSign] and
 * [shouldEncrypt] are false, the plain MIME message is returned without involving the provider.
 *
 * If the provider returns `RESULT_CODE_USER_INTERACTION_REQUIRED` (locked keystore), the
 * returned [PendingIntent] is queued via `queueMessageBuildPendingIntent` so the host activity
 * can launch it; [buildMessageOnActivityResult] resumes the build after the user unlocks.
 */
class SmimeMessageBuilder internal constructor(
    context: Context,
    messageIdGenerator: MessageIdGenerator,
    boundaryGenerator: BoundaryGenerator,
    resourceProvider: CoreResourceProvider,
    settingsManager: GeneralSettingsManager,
) : MessageBuilder(messageIdGenerator, boundaryGenerator, resourceProvider, settingsManager) {

    private val context: Context = context.applicationContext
    private var smimeProvider: String? = null
    private var shouldSign: Boolean = true
    private var shouldEncrypt: Boolean = true

    private var currentProcessedMimeMessage: MimeMessage? = null

    /**
     * Set the provider package this builder will bind to. Required before [buildMessageInternal]
     * runs. Typically pulled from the account's `smimeProvider` setting.
     */
    fun setSmimeProvider(smimeProvider: String) {
        this.smimeProvider = smimeProvider
    }

    /** Toggle outgoing-signature production. Defaults to `true`. */
    fun setShouldSign(shouldSign: Boolean) {
        this.shouldSign = shouldSign
    }

    /**
     * Toggle outgoing encryption. Defaults to `true`. When false, only a signature is produced;
     * the message body remains in plaintext.
     */
    fun setShouldEncrypt(shouldEncrypt: Boolean) {
        this.shouldEncrypt = shouldEncrypt
    }

    override fun buildMessageInternal() {
        check(currentProcessedMimeMessage == null) { "message can only be built once!" }

        val mimeMessage = try {
            build()
        } catch (me: MessagingException) {
            queueMessageBuildException(me)
            return
        }
        currentProcessedMimeMessage = mimeMessage

        if (isDraft || (!shouldSign && !shouldEncrypt)) {
            queueMessageBuildSuccess(mimeMessage)
            return
        }

        startOrContinueBuildMessage()
    }

    override fun buildMessageOnActivityResult(requestCode: Int, userInteractionResult: Intent) {
        checkNotNull(currentProcessedMimeMessage) {
            "build message from activity result must not be called individually"
        }
        // Retry after user interaction (e.g. keystore unlock); the service handles the rest.
        startOrContinueBuildMessage()
    }

    private fun startOrContinueBuildMessage() {
        // We're on a background thread (AsyncTask.doInBackground), so we can block for bind.
        val latch = CountDownLatch(1)
        val serviceRef = AtomicReference<ISmimeService?>()
        val errorRef = AtomicReference<Exception?>()

        val connection = SmimeServiceConnection(
            context,
            smimeProvider,
            object : SmimeServiceConnection.OnBound {
                override fun onBound(service: ISmimeService) {
                    serviceRef.set(service)
                    latch.countDown()
                }

                override fun onError(e: Exception) {
                    errorRef.set(e)
                    latch.countDown()
                }
            },
        )
        connection.bindToService()

        val bound = try {
            latch.await(SERVICE_BIND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            queueMessageBuildException(MessagingException("Interrupted waiting for S/MIME service"))
            return
        }

        if (!bound) {
            // bindToService() returned without error but the connection never landed
            // (provider gone or service never came up). Don't block the compose thread
            // forever — release the registered connection and fail the build so the
            // host can save the message instead of losing it.
            connection.unbindFromService()
            queueMessageBuildException(
                MessagingException("Timed out connecting to S/MIME service"),
            )
            return
        }

        errorRef.get()?.let { error ->
            queueMessageBuildException(
                MessagingException("Could not connect to S/MIME service: ${error.message}"),
            )
            return
        }

        try {
            performSignAndEncrypt(SmimeApi(serviceRef.get()))
        } finally {
            connection.unbindFromService()
        }
    }

    private fun performSignAndEncrypt(api: SmimeApi) {
        val mimeMessage = currentProcessedMimeMessage ?: error("currentProcessedMimeMessage is null")
        val messageBytes = try {
            ByteArrayOutputStream().apply { mimeMessage.writeTo(this) }.toByteArray()
        } catch (e: IOException) {
            queueMessageBuildException(MessagingException("Failed to serialize message for S/MIME signing", e))
            return
        } catch (e: MessagingException) {
            queueMessageBuildException(MessagingException("Failed to serialize message for S/MIME signing", e))
            return
        }

        val smimeOutput = ByteArrayOutputStream()
        val result = api.executeApi(buildSignAndEncryptIntent(), ByteArrayInputStream(messageBytes), smimeOutput)
        result.setExtrasClassLoader(SmimeError::class.java.classLoader)

        when (result.getIntExtra(SmimeApi.RESULT_CODE, SmimeApi.RESULT_CODE_ERROR)) {
            SmimeApi.RESULT_CODE_SUCCESS -> handleSuccess(smimeOutput.toByteArray())

            SmimeApi.RESULT_CODE_USER_INTERACTION_REQUIRED -> {
                val pendingIntent = IntentCompat.getParcelableExtra(
                    result, SmimeApi.RESULT_INTENT, PendingIntent::class.java,
                )
                if (pendingIntent == null) {
                    queueMessageBuildException(
                        MessagingException("S/MIME service requires user interaction but returned no PendingIntent"),
                    )
                } else {
                    queueMessageBuildPendingIntent(pendingIntent, REQUEST_USER_INTERACTION)
                }
            }

            else -> {
                val error = IntentCompat.getParcelableExtra(result, SmimeApi.RESULT_ERROR, SmimeError::class.java)
                queueMessageBuildException(MessagingException(error?.message ?: "Unknown S/MIME error"))
            }
        }
    }

    private fun buildSignAndEncryptIntent(): Intent = Intent(SmimeApi.ACTION_SIGN_AND_ENCRYPT).apply {
        putExtra(SmimeApi.EXTRA_API_VERSION, SmimeApi.API_VERSION)
        putExtra(SmimeApi.EXTRA_SIGN, shouldSign)
        putExtra(SmimeApi.EXTRA_ENCRYPT, shouldEncrypt)
        currentProcessedMimeMessage?.from?.firstOrNull()?.address?.let { fromAddress ->
            putExtra(SmimeApi.EXTRA_FROM, fromAddress)
        }
        if (shouldEncrypt) {
            putExtra(SmimeApi.EXTRA_USER_IDS, collectRecipientAddresses())
        }
    }

    private fun collectRecipientAddresses(): Array<String> {
        val message = currentProcessedMimeMessage ?: return emptyArray()
        return (
            message.getRecipients(RecipientType.TO).asSequence() +
                message.getRecipients(RecipientType.CC).asSequence() +
                message.getRecipients(RecipientType.BCC).asSequence()
            ).map { it.address }.toList().toTypedArray()
    }

    private fun handleSuccess(smimeBytes: ByteArray) {
        try {
            val smimeMessage = MimeMessage.parseMimeMessage(ByteArrayInputStream(smimeBytes), true)
            queueMessageBuildSuccess(smimeMessage)
        } catch (e: IOException) {
            queueMessageBuildException(MessagingException("Failed to parse S/MIME output message", e))
        } catch (e: MessagingException) {
            queueMessageBuildException(MessagingException("Failed to parse S/MIME output message", e))
        }
    }

    companion object {
        private const val REQUEST_USER_INTERACTION = 1

        /** Upper bound on waiting for the provider's service-bind callback. */
        private const val SERVICE_BIND_TIMEOUT_SECONDS = 30L

        @JvmStatic
        fun newInstance(context: Context): SmimeMessageBuilder = SmimeMessageBuilder(
            context,
            MessageIdGenerator.getInstance(),
            BoundaryGenerator.getInstance(),
            DI.get(CoreResourceProvider::class.java),
            DI.get(GeneralSettingsManager::class.java),
        )
    }
}
