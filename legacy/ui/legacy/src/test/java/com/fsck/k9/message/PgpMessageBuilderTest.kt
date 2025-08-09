package com.fsck.k9.message

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.os.Parcelable
import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.K9RobolectricTest
import com.fsck.k9.activity.compose.ComposeCryptoStatus
import com.fsck.k9.activity.compose.RecipientPresenter.CryptoMode
import com.fsck.k9.activity.misc.Attachment
import com.fsck.k9.autocrypt.AutocryptOpenPgpApiInteractor
import com.fsck.k9.autocrypt.AutocryptOperations
import com.fsck.k9.autocrypt.AutocryptOperationsHelper.assertMessageHasAutocryptHeader
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.BoundaryGenerator
import com.fsck.k9.mail.internet.BinaryTempFileBody
import com.fsck.k9.mail.internet.MessageIdGenerator
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.mail.internet.MimeMultipart
import com.fsck.k9.mail.internet.TextBody
import com.fsck.k9.mail.testing.assertk.asBytes
import com.fsck.k9.mail.testing.assertk.asText
import com.fsck.k9.mail.testing.assertk.body
import com.fsck.k9.mail.testing.assertk.bodyPart
import com.fsck.k9.mail.testing.assertk.bodyParts
import com.fsck.k9.mail.testing.assertk.contentTransferEncoding
import com.fsck.k9.mail.testing.assertk.contentType
import com.fsck.k9.mail.testing.assertk.mimeType
import com.fsck.k9.mail.testing.assertk.parameter
import com.fsck.k9.mail.testing.assertk.value
import com.fsck.k9.message.MessageBuilder.Callback
import com.fsck.k9.message.quote.InsertableHtmlContent
import com.fsck.k9.view.RecipientSelectView
import java.io.OutputStream
import java.util.Date
import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.QuoteStyle
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.preference.GeneralSettings
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.privacy.PrivacySettings
import org.apache.james.mime4j.util.MimeUtil
import org.junit.Before
import org.junit.Test
import org.koin.core.component.inject
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.same
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import org.mockito.kotlin.anyOrNull
import org.openintents.openpgp.OpenPgpApiManager.OpenPgpProviderState
import org.openintents.openpgp.OpenPgpError
import org.openintents.openpgp.util.OpenPgpApi
import org.openintents.openpgp.util.OpenPgpApi.OpenPgpDataSource
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
class PgpMessageBuilderTest : K9RobolectricTest() {

    private val defaultCryptoStatus = ComposeCryptoStatus(
        OpenPgpProviderState.OK,
        TEST_KEY_ID,
        emptyList<RecipientSelectView.Recipient>(),
        false,
        false,
        false,
        true,
        true,
        CryptoMode.NO_CHOICE,
    )
    private val resourceProvider: CoreResourceProvider by inject()
    private val openPgpApi = mock(OpenPgpApi::class.java)
    private val autocryptOpenPgpApiInteractor = mock(AutocryptOpenPgpApiInteractor::class.java)
    private val pgpMessageBuilder =
        createDefaultPgpMessageBuilder(openPgpApi, autocryptOpenPgpApiInteractor, resourceProvider)

    @Before
    @Throws(Exception::class)
    fun setUp() {
        Log.logger = TestLogger()
        BinaryTempFileBody.setTempDirectory(RuntimeEnvironment.getApplication().cacheDir)
        `when`(autocryptOpenPgpApiInteractor.getKeyMaterialForKeyId(openPgpApi, TEST_KEY_ID, SENDER_EMAIL))
            .thenReturn(AUTOCRYPT_KEY_MATERIAL)
    }

    @Test
    @Throws(MessagingException::class)
    fun build__withCryptoProviderUnconfigured__shouldThrow() {
        val cryptoStatus = defaultCryptoStatus.copy(openPgpProviderState = OpenPgpProviderState.UNCONFIGURED)

        pgpMessageBuilder.setCryptoStatus(cryptoStatus)

        val mockCallback = mock(Callback::class.java)
        pgpMessageBuilder.buildAsync(mockCallback)

        verify(mockCallback).onMessageBuildException(any<MessagingException>())
        verifyNoMoreInteractions(mockCallback)
    }

    @Test
    @Throws(MessagingException::class)
    fun build__withCryptoProviderUninitialized__shouldThrow() {
        val cryptoStatus = defaultCryptoStatus.copy(openPgpProviderState = OpenPgpProviderState.UNINITIALIZED)

        pgpMessageBuilder.setCryptoStatus(cryptoStatus)

        val mockCallback = mock(Callback::class.java)
        pgpMessageBuilder.buildAsync(mockCallback)

        verify(mockCallback).onMessageBuildException(any<MessagingException>())
        verifyNoMoreInteractions(mockCallback)
    }

    @Test
    @Throws(MessagingException::class)
    fun build__withCryptoProviderError__shouldThrow() {
        val cryptoStatus = defaultCryptoStatus.copy(openPgpProviderState = OpenPgpProviderState.ERROR)

        pgpMessageBuilder.setCryptoStatus(cryptoStatus)

        val mockCallback = mock(Callback::class.java)
        pgpMessageBuilder.buildAsync(mockCallback)

        verify(mockCallback).onMessageBuildException(any<MessagingException>())
        verifyNoMoreInteractions(mockCallback)
    }

    @Test
    fun buildCleartext__withNoSigningKey__shouldBuildTrivialMessage() {
        val cryptoStatus = defaultCryptoStatus.copy(openPgpKeyId = null)

        pgpMessageBuilder.setCryptoStatus(cryptoStatus)

        val mockCallback = mock(Callback::class.java)
        pgpMessageBuilder.buildAsync(mockCallback)

        val captor = ArgumentCaptor.forClass(MimeMessage::class.java)
        verify(mockCallback).onMessageBuildSuccess(captor.capture(), eq(false))
        verifyNoMoreInteractions(mockCallback)

        val message = captor.value
        assertThat(message.mimeType).isEqualTo("text/plain")
    }

    @Test
    fun buildCleartext__shouldSucceed() {
        pgpMessageBuilder.setCryptoStatus(defaultCryptoStatus)

        val mockCallback = mock(Callback::class.java)
        pgpMessageBuilder.buildAsync(mockCallback)

        val captor = ArgumentCaptor.forClass(MimeMessage::class.java)
        verify(mockCallback).onMessageBuildSuccess(captor.capture(), eq(false))
        verifyNoMoreInteractions(mockCallback)

        val message = captor.value
        assertMessageHasAutocryptHeader(message, SENDER_EMAIL, false, AUTOCRYPT_KEY_MATERIAL)
        assertThat(message.getHeader("Autocrypt-Draft-State")).isEmpty()
    }

    @Test
    @Throws(MessagingException::class)
    fun buildSign__withNoDetachedSignatureInResult__shouldThrow() {
        val cryptoStatus = defaultCryptoStatus.copy(cryptoMode = CryptoMode.SIGN_ONLY)
        pgpMessageBuilder.setCryptoStatus(cryptoStatus)

        val returnIntent = Intent()
        returnIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS)
        `when`(openPgpApi.executeApi(any<Intent>(), any<OpenPgpDataSource>(), any<OutputStream>())).thenReturn(
            returnIntent,
        )

        val mockCallback = mock(Callback::class.java)
        pgpMessageBuilder.buildAsync(mockCallback)

        verify(mockCallback).onMessageBuildException(any<MessagingException>())
        verifyNoMoreInteractions(mockCallback)
    }

    @Test
    @Throws(MessagingException::class)
    fun buildSign__withDetachedSignatureInResult__shouldSucceed() {
        val cryptoStatus = defaultCryptoStatus.copy(cryptoMode = CryptoMode.SIGN_ONLY)
        pgpMessageBuilder.setCryptoStatus(cryptoStatus)

        val capturedApiIntent = ArgumentCaptor.forClass(Intent::class.java)

        val returnIntent = Intent()
        returnIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS)
        returnIntent.putExtra(OpenPgpApi.RESULT_DETACHED_SIGNATURE, byteArrayOf(1, 2, 3))
        `when`(
            openPgpApi.executeApi(
                capturedApiIntent.capture(),
                any<OpenPgpDataSource>(),
                anyOrNull(),
            ),
        ).thenReturn(returnIntent)

        val mockCallback = mock(Callback::class.java)
        pgpMessageBuilder.buildAsync(mockCallback)

        val expectedIntent = Intent(OpenPgpApi.ACTION_DETACHED_SIGN)
        expectedIntent.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, TEST_KEY_ID)
        expectedIntent.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true)
        assertThat(capturedApiIntent.value).matches(expectedIntent)

        val captor = ArgumentCaptor.forClass(MimeMessage::class.java)
        verify(mockCallback).onMessageBuildSuccess(captor.capture(), eq(false))
        verifyNoMoreInteractions(mockCallback)

        val message = captor.value
        assertThat(message.mimeType).isEqualTo("multipart/signed")

        assertThat(message.body).isInstanceOf<MimeMultipart>().all {
            bodyParts().hasSize(2)
            bodyPart(0).all {
                contentType().value().isEqualTo("text/plain")
                body().isInstanceOf<TextBody>().all {
                    contentTransferEncoding().isEqualTo(MimeUtil.ENC_QUOTED_PRINTABLE)
                    asText().isEqualTo(TEST_MESSAGE_TEXT)
                }
            }
            bodyPart(1).all {
                contentType().all {
                    value().isEqualTo("application/pgp-signature")
                    parameter("name").isEqualTo("signature.asc")
                }
                body().asBytes().isEqualTo(byteArrayOf(1, 2, 3))
            }
        }

        assertMessageHasAutocryptHeader(message, SENDER_EMAIL, false, AUTOCRYPT_KEY_MATERIAL)
    }

    @Test
    @Throws(MessagingException::class)
    fun buildSign__withUserInteractionResult__shouldReturnUserInteraction() {
        val cryptoStatus = defaultCryptoStatus.copy(cryptoMode = CryptoMode.SIGN_ONLY)
        pgpMessageBuilder.setCryptoStatus(cryptoStatus)

        val returnIntent = mock(Intent::class.java)
        `when`(returnIntent.getIntExtra(eq(OpenPgpApi.RESULT_CODE), anyInt()))
            .thenReturn(OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED)
        val mockPendingIntent = mock(PendingIntent::class.java)
        `when`<Parcelable>(returnIntent.getParcelableExtra<Parcelable>(eq(OpenPgpApi.RESULT_INTENT)))
            .thenReturn(mockPendingIntent)

        `when`(openPgpApi.executeApi(any<Intent>(), any<OpenPgpDataSource>(), any<OutputStream>())).thenReturn(
            returnIntent,
        )

        val mockCallback = mock(Callback::class.java)
        pgpMessageBuilder.buildAsync(mockCallback)

        val captor = ArgumentCaptor.forClass(PendingIntent::class.java)
        verify(mockCallback).onMessageBuildReturnPendingIntent(captor.capture(), anyInt())
        verifyNoMoreInteractions(mockCallback)

        val pendingIntent = captor.value
        assertThat(pendingIntent).isSameInstanceAs(mockPendingIntent)
    }

    @Test
    @Throws(MessagingException::class)
    fun buildSign__withReturnAfterUserInteraction__shouldSucceed() {
        val cryptoStatus = defaultCryptoStatus.copy(cryptoMode = CryptoMode.SIGN_ONLY)
        pgpMessageBuilder.setCryptoStatus(cryptoStatus)

        var returnedRequestCode = 0
        run {
            val returnIntent = spy(Intent())
            returnIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED)

            val mockPendingIntent = mock(PendingIntent::class.java)
            `when`<Parcelable>(returnIntent.getParcelableExtra<Parcelable>(eq(OpenPgpApi.RESULT_INTENT)))
                .thenReturn(mockPendingIntent)

            `when`(openPgpApi.executeApi(any<Intent>(), any<OpenPgpDataSource>(), any<OutputStream>())).thenReturn(
                returnIntent,
            )

            val mockCallback = mock(Callback::class.java)
            pgpMessageBuilder.buildAsync(mockCallback)

            verify(returnIntent).getIntExtra(eq(OpenPgpApi.RESULT_CODE), anyInt())
            val piCaptor = ArgumentCaptor.forClass(PendingIntent::class.java)
            val rcCaptor = ArgumentCaptor.forClass(Int::class.java)
            verify(mockCallback).onMessageBuildReturnPendingIntent(piCaptor.capture(), rcCaptor.capture())
            verifyNoMoreInteractions(mockCallback)

            returnedRequestCode = rcCaptor.value
            assertThat(piCaptor.value).isEqualTo(mockPendingIntent)
        }

        run {
            val returnIntent = spy(Intent())
            returnIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS)

            val mockReturnIntent = mock(Intent::class.java)
            `when`(openPgpApi.executeApi(any<Intent>(), any<OpenPgpDataSource>(), any<OutputStream>())).thenReturn(
                returnIntent,
            )

            val mockCallback = mock(Callback::class.java)
            pgpMessageBuilder.onActivityResult(returnedRequestCode, Activity.RESULT_OK, mockReturnIntent, mockCallback)
            verify(openPgpApi).executeApi(same(mockReturnIntent), any<OpenPgpDataSource>(), any<OutputStream>())
            verify(returnIntent).getIntExtra(eq(OpenPgpApi.RESULT_CODE), anyInt())
        }
    }

    @Test
    @Throws(MessagingException::class)
    fun buildEncrypt__withoutRecipients__shouldThrow() {
        val cryptoStatus = defaultCryptoStatus.copy(cryptoMode = CryptoMode.CHOICE_ENABLED)
        pgpMessageBuilder.setCryptoStatus(cryptoStatus)

        val returnIntent = spy(Intent())
        returnIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS)
        `when`(
            openPgpApi.executeApi(
                any(Intent::class.java),
                any(OpenPgpDataSource::class.java),
                any(OutputStream::class.java),
            ),
        )
            .thenReturn(returnIntent)

        val mockCallback = mock(Callback::class.java)
        pgpMessageBuilder.buildAsync(mockCallback)

        verify(mockCallback).onMessageBuildException(any<MessagingException>())
        verifyNoMoreInteractions(mockCallback)
    }

    @Test
    @Throws(MessagingException::class)
    fun buildEncrypt__draftWithoutRecipients() {
        val cryptoStatus = defaultCryptoStatus.copy(cryptoMode = CryptoMode.CHOICE_ENABLED)
        pgpMessageBuilder.setCryptoStatus(cryptoStatus)
        pgpMessageBuilder.isDraft = true

        buildMessage()
    }

    @Test
    @Throws(MessagingException::class)
    fun buildDraft() {
        pgpMessageBuilder.setCryptoStatus(defaultCryptoStatus)
        pgpMessageBuilder.isDraft = true

        val mimeMessage = buildMessage()

        assertThat(mimeMessage.getHeader("Autocrypt-Draft-State")).containsExactly("encrypt=no; ")
    }

    @Test
    @Throws(MessagingException::class)
    fun buildDraft_replyToEncrypted() {
        val cryptoStatus = defaultCryptoStatus.copy(
            cryptoMode = CryptoMode.NO_CHOICE,
            isReplyToEncrypted = true,
        )
        pgpMessageBuilder.setCryptoStatus(cryptoStatus)
        pgpMessageBuilder.isDraft = true

        val mimeMessage = buildMessage()

        assertThat(mimeMessage.getHeader("Autocrypt-Draft-State"))
            .containsExactly("encrypt=yes; _is-reply-to-encrypted=yes; ")
    }

    @Test
    @Throws(MessagingException::class)
    fun buildDraft_encrypt() {
        val cryptoStatus = defaultCryptoStatus.copy(cryptoMode = CryptoMode.CHOICE_ENABLED)
        pgpMessageBuilder.setCryptoStatus(cryptoStatus)
        pgpMessageBuilder.isDraft = true

        val mimeMessage = buildMessage()

        assertThat(mimeMessage.getHeader("Autocrypt-Draft-State")).containsExactly("encrypt=yes; _by-choice=yes; ")
    }

    @Test
    @Throws(MessagingException::class)
    fun buildDraft_sign() {
        val cryptoStatus = defaultCryptoStatus.copy(cryptoMode = CryptoMode.SIGN_ONLY)
        pgpMessageBuilder.setCryptoStatus(cryptoStatus)
        pgpMessageBuilder.isDraft = true

        val mimeMessage = buildMessage()

        assertThat(mimeMessage.getHeader("Autocrypt-Draft-State"))
            .containsExactly("encrypt=no; _sign-only=yes; _by-choice=yes; ")
    }

    private fun buildMessage(): MimeMessage {
        val returnIntent = spy(Intent())
        returnIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS)
        `when`(
            openPgpApi.executeApi(
                any(Intent::class.java),
                any(OpenPgpDataSource::class.java),
                any(OutputStream::class.java),
            ),
        )
            .thenReturn(returnIntent)

        val mockCallback = mock(Callback::class.java)
        pgpMessageBuilder.buildAsync(mockCallback)

        val mimeMessageCaptor = ArgumentCaptor.forClass(MimeMessage::class.java)
        verify(mockCallback).onMessageBuildSuccess(mimeMessageCaptor.capture(), eq(true))
        verifyNoMoreInteractions(mockCallback)

        assertThat(mimeMessageCaptor.value).isNotNull()
        return mimeMessageCaptor.value
    }

    @Test
    @Throws(MessagingException::class)
    fun buildEncrypt__checkGossip() {
        val cryptoStatus = defaultCryptoStatus.copy(
            cryptoMode = CryptoMode.CHOICE_ENABLED,
            recipientAddresses = listOf("alice@example.org", "bob@example.org"),
        )
        pgpMessageBuilder.setCryptoStatus(cryptoStatus)

        val returnIntent = Intent()
        returnIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS)
        `when`(
            openPgpApi.executeApi(
                any(Intent::class.java),
                any(OpenPgpDataSource::class.java),
                any(OutputStream::class.java),
            ),
        )
            .thenReturn(returnIntent)
        pgpMessageBuilder.buildAsync(mock(Callback::class.java))

        verify(autocryptOpenPgpApiInteractor).getKeyMaterialForUserId(same(openPgpApi), eq("alice@example.org"))
        verify(autocryptOpenPgpApiInteractor).getKeyMaterialForUserId(same(openPgpApi), eq("bob@example.org"))
    }

    @Test
    @Throws(MessagingException::class)
    fun buildEncrypt__checkGossip__filterBcc() {
        val cryptoStatus = defaultCryptoStatus.copy(
            cryptoMode = CryptoMode.CHOICE_ENABLED,
            recipientAddresses = listOf("alice@example.org", "bob@example.org", "carol@example.org"),
        )
        pgpMessageBuilder.setCryptoStatus(cryptoStatus)
        pgpMessageBuilder.setBcc(listOf(Address("carol@example.org")))

        val returnIntent = Intent()
        returnIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS)
        `when`(
            openPgpApi.executeApi(
                any(Intent::class.java),
                any(OpenPgpDataSource::class.java),
                any(OutputStream::class.java),
            ),
        )
            .thenReturn(returnIntent)
        pgpMessageBuilder.buildAsync(mock(Callback::class.java))

        verify(autocryptOpenPgpApiInteractor).getKeyMaterialForKeyId(
            same(openPgpApi),
            eq(TEST_KEY_ID),
            eq(SENDER_EMAIL),
        )
        verify(autocryptOpenPgpApiInteractor).getKeyMaterialForUserId(same(openPgpApi), eq("alice@example.org"))
        verify(autocryptOpenPgpApiInteractor).getKeyMaterialForUserId(same(openPgpApi), eq("bob@example.org"))
    }

    @Test
    @Throws(MessagingException::class)
    fun buildEncrypt__checkGossip__filterBccSingleRecipient() {
        val cryptoStatus = defaultCryptoStatus.copy(
            cryptoMode = CryptoMode.CHOICE_ENABLED,
            isPgpInlineModeEnabled = true,
            recipientAddresses = listOf("alice@example.org", "carol@example.org"),
        )
        pgpMessageBuilder.setCryptoStatus(cryptoStatus)
        pgpMessageBuilder.setBcc(listOf(Address("carol@example.org")))

        val returnIntent = Intent()
        returnIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS)
        `when`(
            openPgpApi.executeApi(
                any(Intent::class.java),
                any(OpenPgpDataSource::class.java),
                any(OutputStream::class.java),
            ),
        )
            .thenReturn(returnIntent)
        pgpMessageBuilder.buildAsync(mock(Callback::class.java))

        verify(autocryptOpenPgpApiInteractor).getKeyMaterialForKeyId(
            any(OpenPgpApi::class.java),
            any(Long::class.java),
            any(String::class.java),
        )
        verifyNoMoreInteractions(autocryptOpenPgpApiInteractor)
    }

    @Test
    @Throws(MessagingException::class)
    fun buildEncrypt__shouldSucceed() {
        val cryptoStatus = defaultCryptoStatus.copy(
            cryptoMode = CryptoMode.CHOICE_ENABLED,
            recipientAddresses = listOf("test@example.org"),
        )
        pgpMessageBuilder.setCryptoStatus(cryptoStatus)

        val capturedApiIntent = ArgumentCaptor.forClass(Intent::class.java)

        val returnIntent = Intent()
        returnIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS)

        `when`(
            openPgpApi.executeApi(
                capturedApiIntent.capture(),
                any(OpenPgpDataSource::class.java),
                any(OutputStream::class.java),
            ),
        ).thenReturn(returnIntent)

        val mockCallback = mock(Callback::class.java)
        pgpMessageBuilder.buildAsync(mockCallback)

        val expectedApiIntent = Intent(OpenPgpApi.ACTION_SIGN_AND_ENCRYPT)
        expectedApiIntent.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, TEST_KEY_ID)
        expectedApiIntent.putExtra(OpenPgpApi.EXTRA_KEY_IDS, longArrayOf(TEST_KEY_ID))
        expectedApiIntent.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true)
        expectedApiIntent.putExtra(OpenPgpApi.EXTRA_USER_IDS, cryptoStatus.recipientAddressesAsArray)
        assertThat(capturedApiIntent.value).matches(expectedApiIntent)

        val captor = ArgumentCaptor.forClass(MimeMessage::class.java)
        verify(mockCallback).onMessageBuildSuccess(captor.capture(), eq(false))
        verifyNoMoreInteractions(mockCallback)

        val message = captor.value

        assertThat(message).all {
            mimeType().isEqualTo("multipart/encrypted")
            body().isInstanceOf<MimeMultipart>().all {
                bodyParts().hasSize(2)
                bodyPart(0).all {
                    contentType().value().isEqualTo("application/pgp-encrypted")
                    body().asText().isEqualTo("Version: 1")
                }
                bodyPart(1).all {
                    contentType().all {
                        value().isEqualTo("application/octet-stream")
                        parameter("name").isEqualTo("encrypted.asc")
                    }
                    body().isInstanceOf<BinaryTempFileBody>()
                        .contentTransferEncoding().isEqualTo(MimeUtil.ENC_7BIT)
                }
            }
        }

        assertMessageHasAutocryptHeader(message, SENDER_EMAIL, false, AUTOCRYPT_KEY_MATERIAL)
    }

    @Test
    @Throws(MessagingException::class)
    fun buildEncrypt__withInlineEnabled__shouldSucceed() {
        val cryptoStatus = defaultCryptoStatus.copy(
            cryptoMode = CryptoMode.CHOICE_ENABLED,
            isPgpInlineModeEnabled = true,
            recipientAddresses = listOf("test@example.org"),
        )
        pgpMessageBuilder.setCryptoStatus(cryptoStatus)

        val capturedApiIntent = ArgumentCaptor.forClass(Intent::class.java)

        val returnIntent = Intent()
        returnIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS)

        `when`(
            openPgpApi.executeApi(
                capturedApiIntent.capture(),
                any(OpenPgpDataSource::class.java),
                any(OutputStream::class.java),
            ),
        ).thenReturn(returnIntent)

        val mockCallback = mock(Callback::class.java)
        pgpMessageBuilder.buildAsync(mockCallback)

        val expectedApiIntent = Intent(OpenPgpApi.ACTION_SIGN_AND_ENCRYPT)
        expectedApiIntent.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, TEST_KEY_ID)
        expectedApiIntent.putExtra(OpenPgpApi.EXTRA_KEY_IDS, longArrayOf(TEST_KEY_ID))
        expectedApiIntent.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true)
        expectedApiIntent.putExtra(OpenPgpApi.EXTRA_USER_IDS, cryptoStatus.recipientAddressesAsArray)
        assertThat(capturedApiIntent.value).matches(expectedApiIntent)

        val captor = ArgumentCaptor.forClass(MimeMessage::class.java)
        verify(mockCallback).onMessageBuildSuccess(captor.capture(), eq(false))
        verifyNoMoreInteractions(mockCallback)

        val message = captor.value
        assertThat(message).all {
            mimeType().isEqualTo("text/plain")
            body().isInstanceOf<BinaryTempFileBody>()
                .contentTransferEncoding().isEqualTo(MimeUtil.ENC_7BIT)
        }

        assertMessageHasAutocryptHeader(message, SENDER_EMAIL, false, AUTOCRYPT_KEY_MATERIAL)
    }

    @Test
    @Throws(MessagingException::class)
    fun buildSign__withInlineEnabled__shouldSucceed() {
        val cryptoStatus = defaultCryptoStatus.copy(
            cryptoMode = CryptoMode.SIGN_ONLY,
            isPgpInlineModeEnabled = true,
            recipientAddresses = listOf("test@example.org"),
        )

        pgpMessageBuilder.setCryptoStatus(cryptoStatus)

        val capturedApiIntent = ArgumentCaptor.forClass(Intent::class.java)

        val returnIntent = Intent()
        returnIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS)

        `when`(
            openPgpApi.executeApi(
                capturedApiIntent.capture(),
                any(OpenPgpDataSource::class.java),
                any(OutputStream::class.java),
            ),
        ).thenReturn(returnIntent)

        val mockCallback = mock(Callback::class.java)
        pgpMessageBuilder.buildAsync(mockCallback)

        val expectedApiIntent = Intent(OpenPgpApi.ACTION_SIGN)
        expectedApiIntent.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, TEST_KEY_ID)
        expectedApiIntent.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true)
        assertThat(capturedApiIntent.value).matches(expectedApiIntent)

        val captor = ArgumentCaptor.forClass(MimeMessage::class.java)
        verify(mockCallback).onMessageBuildSuccess(captor.capture(), eq(false))
        verifyNoMoreInteractions(mockCallback)

        val message = captor.value
        assertThat(message.mimeType).isEqualTo("text/plain")

        assertMessageHasAutocryptHeader(message, SENDER_EMAIL, false, AUTOCRYPT_KEY_MATERIAL)
    }

    @Test
    @Throws(MessagingException::class)
    fun buildSignWithAttach__withInlineEnabled__shouldThrow() {
        val cryptoStatus = defaultCryptoStatus.copy(cryptoMode = CryptoMode.SIGN_ONLY, isPgpInlineModeEnabled = true)

        pgpMessageBuilder.setCryptoStatus(cryptoStatus)
        pgpMessageBuilder.setAttachments(listOf(Attachment.createAttachment(null, 0, null, true, true)))

        val mockCallback = mock(Callback::class.java)
        pgpMessageBuilder.buildAsync(mockCallback)

        verify(mockCallback).onMessageBuildException(any<MessagingException>())
        verifyNoMoreInteractions(mockCallback)
        verifyNoMoreInteractions(openPgpApi)
    }

    @Test
    @Throws(MessagingException::class)
    fun buildEncryptWithAttach__withInlineEnabled__shouldThrow() {
        val cryptoStatus =
            defaultCryptoStatus.copy(cryptoMode = CryptoMode.CHOICE_ENABLED, isPgpInlineModeEnabled = true)

        pgpMessageBuilder.setCryptoStatus(cryptoStatus)
        pgpMessageBuilder.setAttachments(listOf(Attachment.createAttachment(null, 0, null, true, true)))

        val mockCallback = mock(Callback::class.java)
        pgpMessageBuilder.buildAsync(mockCallback)

        verify(mockCallback).onMessageBuildException(any<MessagingException>())
        verifyNoMoreInteractions(mockCallback)
        verifyNoMoreInteractions(openPgpApi)
    }

    @Test
    @Throws(MessagingException::class)
    fun buildOpportunisticEncrypt__withNoKeysAndNoSignOnly__shouldNotBeSigned() {
        val cryptoStatus = defaultCryptoStatus.copy(recipientAddresses = listOf("test@example.org"))
        pgpMessageBuilder.setCryptoStatus(cryptoStatus)

        val returnIntent = Intent()
        returnIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)
        returnIntent.putExtra(
            OpenPgpApi.RESULT_ERROR,
            OpenPgpError(OpenPgpError.OPPORTUNISTIC_MISSING_KEYS, "Missing keys"),
        )

        `when`(
            openPgpApi.executeApi(
                any(Intent::class.java),
                any(OpenPgpDataSource::class.java),
                any(OutputStream::class.java),
            ),
        )
            .thenReturn(returnIntent)

        val mockCallback = mock(Callback::class.java)
        pgpMessageBuilder.buildAsync(mockCallback)

        val captor = ArgumentCaptor.forClass(MimeMessage::class.java)
        verify(mockCallback).onMessageBuildSuccess(captor.capture(), eq(false))
        verifyNoMoreInteractions(mockCallback)

        val message = captor.value
        assertThat(message.mimeType).isEqualTo("text/plain")
    }

    @Test
    @Throws(MessagingException::class)
    fun buildSign__withNoDetachedSignatureExtra__shouldFail() {
        val cryptoStatus = defaultCryptoStatus.copy(cryptoMode = CryptoMode.SIGN_ONLY)
        pgpMessageBuilder.setCryptoStatus(cryptoStatus)

        val returnIntentSigned = Intent()
        returnIntentSigned.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS)
        // no OpenPgpApi.EXTRA_DETACHED_SIGNATURE!

        `when`(openPgpApi.executeApi(any<Intent>(), any<OpenPgpDataSource>(), any<OutputStream>())).thenReturn(
            returnIntentSigned,
        )
        val mockCallback = mock(Callback::class.java)
        pgpMessageBuilder.buildAsync(mockCallback)

        verify(mockCallback).onMessageBuildException(any<MessagingException>())
        verifyNoMoreInteractions(mockCallback)
    }

    companion object {
        private const val TEST_KEY_ID = 123L
        private const val TEST_MESSAGE_TEXT = "message text with a ☭ CCCP symbol"
        private val AUTOCRYPT_KEY_MATERIAL = byteArrayOf(1, 2, 3)
        private const val SENDER_EMAIL = "test@example.org"

        private fun createDefaultPgpMessageBuilder(
            openPgpApi: OpenPgpApi,
            autocryptOpenPgpApiInteractor: AutocryptOpenPgpApiInteractor,
            resourceProvider: CoreResourceProvider,
        ): PgpMessageBuilder {
            val builder = PgpMessageBuilder(
                MessageIdGenerator.getInstance(),
                BoundaryGenerator.getInstance(),
                AutocryptOperations.getInstance(),
                autocryptOpenPgpApiInteractor,
                resourceProvider,
                fakeGeneralSettingsManager,
            )
            builder.setOpenPgpApi(openPgpApi)

            val identity = Identity(
                name = "tester",
                email = SENDER_EMAIL,
                description = "test identity",
                signatureUse = false,
            )

            builder.setSubject("subject")
                .setSentDate(Date())
                .setHideTimeZone(false)
                .setTo(ArrayList())
                .setCc(ArrayList())
                .setBcc(ArrayList())
                .setInReplyTo("inreplyto")
                .setReferences("references")
                .setRequestReadReceipt(false)
                .setIdentity(identity)
                .setMessageFormat(SimpleMessageFormat.TEXT)
                .setText(TEST_MESSAGE_TEXT)
                .setAttachments(ArrayList())
                .setSignature("signature")
                .setQuoteStyle(QuoteStyle.PREFIX)
                .setQuotedTextMode(QuotedTextMode.NONE)
                .setQuotedText("quoted text")
                .setQuotedHtmlContent(InsertableHtmlContent())
                .setReplyAfterQuote(false)
                .setSignatureBeforeQuotedText(false)
                .setIdentityChanged(false)
                .setSignatureChanged(false)
                .setCursorPosition(0)
                .setMessageReference(null).isDraft = false

            return builder
        }

        private fun Assert<Intent>.matches(expected: Intent) = given { actual ->
            assertThat(actual.action).isEqualTo(expected.action)

            val expectedExtras = checkNotNull(expected.extras)
            val intentExtras = checkNotNull(actual.extras)
            assertThat(intentExtras.size()).isEqualTo(expectedExtras.size())

            for (key in expectedExtras.keySet()) {
                val name = "extra[$key]"
                val intentExtra = intentExtras.get(key)
                val expectedExtra = expectedExtras.get(key)

                if (expectedExtra == null) {
                    assertThat(intentExtra, name).isNull()
                } else {
                    assertThat(intentExtra, name).isNotNull()
                }

                when (intentExtra) {
                    is LongArray -> assertThat(intentExtra, name).isEqualTo(expectedExtra as LongArray)
                    is Array<*> -> assertThat(intentExtra as Array<Any?>, name).isEqualTo(expectedExtra as Array<Any?>)
                    else -> assertThat(intentExtra, name).isEqualTo(expectedExtra)
                }
            }
        }

        private val fakeGeneralSettingsManager = object : GeneralSettingsManager {
            override fun getSettings() = GeneralSettings(
                privacy = PrivacySettings(isHideUserAgent = false, isHideTimeZone = false),
            )

            override fun getSettingsFlow(): Flow<GeneralSettings> = error("not implemented")
            override fun save(config: GeneralSettings) = error("not implemented")

            override fun getConfig(): GeneralSettings = error("not implemented")

            override fun getConfigFlow(): Flow<GeneralSettings> = error("not implemented")
        }
    }
}
