package com.fsck.k9.message


import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.os.Parcelable
import com.fsck.k9.Account.QuoteStyle
import com.fsck.k9.Identity
import com.fsck.k9.RobolectricTest
import com.fsck.k9.activity.compose.ComposeCryptoStatus
import com.fsck.k9.activity.compose.RecipientPresenter.CryptoMode
import com.fsck.k9.activity.misc.Attachment
import com.fsck.k9.autocrypt.AutocryptOpenPgpApiInteractor
import com.fsck.k9.autocrypt.AutocryptOperations
import com.fsck.k9.autocrypt.AutocryptOperationsHelper.assertMessageHasAutocryptHeader
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.BodyPart
import com.fsck.k9.mail.BoundaryGenerator
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.internet.*
import com.fsck.k9.message.MessageBuilder.Callback
import com.fsck.k9.message.quote.InsertableHtmlContent
import com.fsck.k9.view.RecipientSelectView
import com.nhaarman.mockito_kotlin.anyOrNull
import org.apache.commons.io.Charsets
import org.apache.commons.io.IOUtils
import org.apache.james.mime4j.util.MimeUtil
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import org.openintents.openpgp.OpenPgpApiManager.OpenPgpProviderState
import org.openintents.openpgp.OpenPgpError
import org.openintents.openpgp.util.OpenPgpApi
import org.openintents.openpgp.util.OpenPgpApi.OpenPgpDataSource
import org.robolectric.RuntimeEnvironment
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*


class PgpMessageBuilderTest : RobolectricTest() {


    private val defaultCryptoStatus = ComposeCryptoStatus(
            OpenPgpProviderState.OK,
            TEST_KEY_ID,
            emptyList<RecipientSelectView.Recipient>(),
            false,
            false,
            false,
            true,
            true,
            CryptoMode.NO_CHOICE
    )
    private val openPgpApi = mock(OpenPgpApi::class.java)
    private val autocryptOpenPgpApiInteractor = mock(AutocryptOpenPgpApiInteractor::class.java)
    private val pgpMessageBuilder = createDefaultPgpMessageBuilder(openPgpApi, autocryptOpenPgpApiInteractor)

    @Before
    @Throws(Exception::class)
    fun setUp() {
        BinaryTempFileBody.setTempDirectory(RuntimeEnvironment.application.cacheDir)
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
        assertEquals("text/plain", message.mimeType)
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
    }

    @Test
    @Throws(MessagingException::class)
    fun buildSign__withNoDetachedSignatureInResult__shouldThrow() {
        val cryptoStatus = defaultCryptoStatus.copy(cryptoMode = CryptoMode.SIGN_ONLY)
        pgpMessageBuilder.setCryptoStatus(cryptoStatus)

        val returnIntent = Intent()
        returnIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS)
        `when`(openPgpApi.executeApi(any<Intent>(), any<OpenPgpDataSource>(), any<OutputStream>())).thenReturn(returnIntent)

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
        `when`(openPgpApi.executeApi(capturedApiIntent.capture(),
                any<OpenPgpDataSource>(), anyOrNull())).thenReturn(returnIntent)

        val mockCallback = mock(Callback::class.java)
        pgpMessageBuilder.buildAsync(mockCallback)

        val expectedIntent = Intent(OpenPgpApi.ACTION_DETACHED_SIGN)
        expectedIntent.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, TEST_KEY_ID)
        expectedIntent.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true)
        assertIntentEqualsActionAndExtras(expectedIntent, capturedApiIntent.value)

        val captor = ArgumentCaptor.forClass(MimeMessage::class.java)
        verify(mockCallback).onMessageBuildSuccess(captor.capture(), eq(false))
        verifyNoMoreInteractions(mockCallback)

        val message = captor.value
        Assert.assertEquals("message must be multipart/signed", "multipart/signed", message.mimeType)

        val multipart = message.body as MimeMultipart
        Assert.assertEquals("multipart/signed must consist of two parts", 2, multipart.count.toLong())

        val contentBodyPart = multipart.getBodyPart(0)
        Assert.assertEquals("first part must have content type text/plain",
                "text/plain", MimeUtility.getHeaderParameter(contentBodyPart.contentType, null))
        assertTrue("signed message body must be TextBody", contentBodyPart.body is TextBody)
        Assert.assertEquals(MimeUtil.ENC_QUOTED_PRINTABLE, (contentBodyPart.body as TextBody).encoding)
        assertContentOfBodyPartEquals("content must match the message text", contentBodyPart, TEST_MESSAGE_TEXT)

        val signatureBodyPart = multipart.getBodyPart(1)
        val contentType = signatureBodyPart.contentType
        Assert.assertEquals("second part must be pgp signature", "application/pgp-signature",
                MimeUtility.getHeaderParameter(contentType, null))
        Assert.assertEquals("second part must be called signature.asc", "signature.asc",
                MimeUtility.getHeaderParameter(contentType, "name"))
        assertContentOfBodyPartEquals("content must match the supplied detached signature",
                signatureBodyPart, byteArrayOf(1, 2, 3))

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

        `when`(openPgpApi.executeApi(any<Intent>(), any<OpenPgpDataSource>(), any<OutputStream>())).thenReturn(returnIntent)

        val mockCallback = mock(Callback::class.java)
        pgpMessageBuilder.buildAsync(mockCallback)

        val captor = ArgumentCaptor.forClass(PendingIntent::class.java)
        verify(mockCallback).onMessageBuildReturnPendingIntent(captor.capture(), anyInt())
        verifyNoMoreInteractions(mockCallback)

        val pendingIntent = captor.value
        Assert.assertSame(pendingIntent, mockPendingIntent)
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

            `when`(openPgpApi.executeApi(any<Intent>(), any<OpenPgpDataSource>(), any<OutputStream>())).thenReturn(returnIntent)

            val mockCallback = mock(Callback::class.java)
            pgpMessageBuilder.buildAsync(mockCallback)

            verify(returnIntent).getIntExtra(eq(OpenPgpApi.RESULT_CODE), anyInt())
            val piCaptor = ArgumentCaptor.forClass(PendingIntent::class.java)
            val rcCaptor = ArgumentCaptor.forClass(Int::class.java)
            verify(mockCallback).onMessageBuildReturnPendingIntent(piCaptor.capture(), rcCaptor.capture())
            verifyNoMoreInteractions(mockCallback)

            returnedRequestCode = rcCaptor.value
            Assert.assertSame(mockPendingIntent, piCaptor.value)
        }

        run {
            val returnIntent = spy(Intent())
            returnIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS)

            val mockReturnIntent = mock(Intent::class.java)
            `when`(openPgpApi.executeApi(any<Intent>(), any<OpenPgpDataSource>(), any<OutputStream>())).thenReturn(returnIntent)

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
        `when`(openPgpApi.executeApi(any(Intent::class.java), any(OpenPgpDataSource::class.java), any(OutputStream::class.java)))
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

        val returnIntent = spy(Intent())
        returnIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS)
        `when`(openPgpApi.executeApi(any(Intent::class.java), any(OpenPgpDataSource::class.java), any(OutputStream::class.java)))
                .thenReturn(returnIntent)

        val mockCallback = mock(Callback::class.java)
        pgpMessageBuilder.buildAsync(mockCallback)

        verify(mockCallback).onMessageBuildSuccess(any<MimeMessage>(), eq(true))
        verifyNoMoreInteractions(mockCallback)
    }

    @Test
    @Throws(MessagingException::class)
    fun buildEncrypt__checkGossip() {
        val cryptoStatus = defaultCryptoStatus.copy(cryptoMode = CryptoMode.CHOICE_ENABLED,
                recipientAddresses = listOf("alice@example.org", "bob@example.org"))
        pgpMessageBuilder.setCryptoStatus(cryptoStatus)

        val returnIntent = Intent()
        returnIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS)
        `when`(openPgpApi.executeApi(any(Intent::class.java), any(OpenPgpDataSource::class.java), any(OutputStream::class.java)))
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
                recipientAddresses = listOf("alice@example.org", "bob@example.org", "carol@example.org"))
        pgpMessageBuilder.setCryptoStatus(cryptoStatus)
        pgpMessageBuilder.setBcc(listOf(Address("carol@example.org")))

        val returnIntent = Intent()
        returnIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS)
        `when`(openPgpApi.executeApi(any(Intent::class.java), any(OpenPgpDataSource::class.java), any(OutputStream::class.java)))
                .thenReturn(returnIntent)
        pgpMessageBuilder.buildAsync(mock(Callback::class.java))

        verify(autocryptOpenPgpApiInteractor).getKeyMaterialForKeyId(same(openPgpApi), eq(TEST_KEY_ID), eq(SENDER_EMAIL))
        verify(autocryptOpenPgpApiInteractor).getKeyMaterialForUserId(same(openPgpApi), eq("alice@example.org"))
        verify(autocryptOpenPgpApiInteractor).getKeyMaterialForUserId(same(openPgpApi), eq("bob@example.org"))
    }

    @Test
    @Throws(MessagingException::class)
    fun buildEncrypt__checkGossip__filterBccSingleRecipient() {
        val cryptoStatus = defaultCryptoStatus.copy(
                cryptoMode = CryptoMode.CHOICE_ENABLED,
                isPgpInlineModeEnabled = true,
                recipientAddresses = listOf("alice@example.org", "carol@example.org"))
        pgpMessageBuilder.setCryptoStatus(cryptoStatus)
        pgpMessageBuilder.setBcc(listOf(Address("carol@example.org")))

        val returnIntent = Intent()
        returnIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS)
        `when`(openPgpApi.executeApi(any(Intent::class.java), any(OpenPgpDataSource::class.java), any(OutputStream::class.java)))
                .thenReturn(returnIntent)
        pgpMessageBuilder.buildAsync(mock(Callback::class.java))

        verify(autocryptOpenPgpApiInteractor).getKeyMaterialForKeyId(any(OpenPgpApi::class.java), any(Long::class.java), any(String::class.java))
        verifyNoMoreInteractions(autocryptOpenPgpApiInteractor)
    }

    @Test
    @Throws(MessagingException::class)
    fun buildEncrypt__shouldSucceed() {
        val cryptoStatus = defaultCryptoStatus.copy(
                cryptoMode = CryptoMode.CHOICE_ENABLED,
                recipientAddresses = listOf("test@example.org"))
        pgpMessageBuilder.setCryptoStatus(cryptoStatus)

        val capturedApiIntent = ArgumentCaptor.forClass(Intent::class.java)

        val returnIntent = Intent()
        returnIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS)

        `when`(openPgpApi.executeApi(capturedApiIntent.capture(), any(OpenPgpDataSource::class.java),
                any(OutputStream::class.java))).thenReturn(returnIntent)

        val mockCallback = mock(Callback::class.java)
        pgpMessageBuilder.buildAsync(mockCallback)

        val expectedApiIntent = Intent(OpenPgpApi.ACTION_SIGN_AND_ENCRYPT)
        expectedApiIntent.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, TEST_KEY_ID)
        expectedApiIntent.putExtra(OpenPgpApi.EXTRA_KEY_IDS, longArrayOf(TEST_KEY_ID))
        expectedApiIntent.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true)
        expectedApiIntent.putExtra(OpenPgpApi.EXTRA_USER_IDS, cryptoStatus.recipientAddressesAsArray)
        assertIntentEqualsActionAndExtras(expectedApiIntent, capturedApiIntent.value)

        val captor = ArgumentCaptor.forClass(MimeMessage::class.java)
        verify(mockCallback).onMessageBuildSuccess(captor.capture(), eq(false))
        verifyNoMoreInteractions(mockCallback)

        val message = captor.value

        Assert.assertEquals("message must be multipart/encrypted", "multipart/encrypted", message.mimeType)

        val multipart = message.body as MimeMultipart
        Assert.assertEquals("multipart/encrypted must consist of two parts", 2, multipart.count.toLong())

        val dummyBodyPart = multipart.getBodyPart(0)
        Assert.assertEquals("first part must be pgp encrypted dummy part",
                "application/pgp-encrypted", dummyBodyPart.contentType)
        assertContentOfBodyPartEquals("content must match the supplied detached signature",
                dummyBodyPart, "Version: 1")

        val encryptedBodyPart = multipart.getBodyPart(1)
        Assert.assertEquals("second part must be octet-stream of encrypted data",
                "application/octet-stream; name=\"encrypted.asc\"", encryptedBodyPart.contentType)
        assertTrue("message body must be BinaryTempFileBody",
                encryptedBodyPart.body is BinaryTempFileBody)
        Assert.assertEquals(MimeUtil.ENC_7BIT, (encryptedBodyPart.body as BinaryTempFileBody).encoding)

        assertMessageHasAutocryptHeader(message, SENDER_EMAIL, false, AUTOCRYPT_KEY_MATERIAL)
    }

    @Test
    @Throws(MessagingException::class)
    fun buildEncrypt__withInlineEnabled__shouldSucceed() {
        val cryptoStatus = defaultCryptoStatus.copy(
                cryptoMode = CryptoMode.CHOICE_ENABLED,
                isPgpInlineModeEnabled = true,
                recipientAddresses = listOf("test@example.org"))
        pgpMessageBuilder.setCryptoStatus(cryptoStatus)

        val capturedApiIntent = ArgumentCaptor.forClass(Intent::class.java)

        val returnIntent = Intent()
        returnIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS)

        `when`(openPgpApi.executeApi(capturedApiIntent.capture(), any(OpenPgpDataSource::class.java),
                any(OutputStream::class.java))).thenReturn(returnIntent)

        val mockCallback = mock(Callback::class.java)
        pgpMessageBuilder.buildAsync(mockCallback)

        val expectedApiIntent = Intent(OpenPgpApi.ACTION_SIGN_AND_ENCRYPT)
        expectedApiIntent.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, TEST_KEY_ID)
        expectedApiIntent.putExtra(OpenPgpApi.EXTRA_KEY_IDS, longArrayOf(TEST_KEY_ID))
        expectedApiIntent.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true)
        expectedApiIntent.putExtra(OpenPgpApi.EXTRA_USER_IDS, cryptoStatus.recipientAddressesAsArray)
        assertIntentEqualsActionAndExtras(expectedApiIntent, capturedApiIntent.value)

        val captor = ArgumentCaptor.forClass(MimeMessage::class.java)
        verify(mockCallback).onMessageBuildSuccess(captor.capture(), eq(false))
        verifyNoMoreInteractions(mockCallback)

        val message = captor.value
        Assert.assertEquals("text/plain", message.mimeType)
        assertTrue("message body must be BinaryTempFileBody", message.body is BinaryTempFileBody)
        Assert.assertEquals(MimeUtil.ENC_7BIT, (message.body as BinaryTempFileBody).encoding)

        assertMessageHasAutocryptHeader(message, SENDER_EMAIL, false, AUTOCRYPT_KEY_MATERIAL)
    }

    @Test
    @Throws(MessagingException::class)
    fun buildSign__withInlineEnabled__shouldSucceed() {
        val cryptoStatus = defaultCryptoStatus.copy(
                cryptoMode = CryptoMode.SIGN_ONLY,
                isPgpInlineModeEnabled = true,
                recipientAddresses = listOf("test@example.org"))

        pgpMessageBuilder.setCryptoStatus(cryptoStatus)

        val capturedApiIntent = ArgumentCaptor.forClass(Intent::class.java)

        val returnIntent = Intent()
        returnIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS)

        `when`(openPgpApi.executeApi(capturedApiIntent.capture(), any(OpenPgpDataSource::class.java),
                any(OutputStream::class.java))).thenReturn(returnIntent)

        val mockCallback = mock(Callback::class.java)
        pgpMessageBuilder.buildAsync(mockCallback)

        val expectedApiIntent = Intent(OpenPgpApi.ACTION_SIGN)
        expectedApiIntent.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, TEST_KEY_ID)
        expectedApiIntent.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true)
        assertIntentEqualsActionAndExtras(expectedApiIntent, capturedApiIntent.value)

        val captor = ArgumentCaptor.forClass(MimeMessage::class.java)
        verify(mockCallback).onMessageBuildSuccess(captor.capture(), eq(false))
        verifyNoMoreInteractions(mockCallback)

        val message = captor.value
        Assert.assertEquals("message must be text/plain", "text/plain", message.mimeType)

        assertMessageHasAutocryptHeader(message, SENDER_EMAIL, false, AUTOCRYPT_KEY_MATERIAL)
    }

    @Test
    @Throws(MessagingException::class)
    fun buildSignWithAttach__withInlineEnabled__shouldThrow() {
        val cryptoStatus = defaultCryptoStatus.copy(cryptoMode = CryptoMode.SIGN_ONLY, isPgpInlineModeEnabled = true)

        pgpMessageBuilder.setCryptoStatus(cryptoStatus)
        pgpMessageBuilder.setAttachments(listOf(Attachment.createAttachment(null, 0, null, true)))

        val mockCallback = mock(Callback::class.java)
        pgpMessageBuilder.buildAsync(mockCallback)

        verify(mockCallback).onMessageBuildException(any<MessagingException>())
        verifyNoMoreInteractions(mockCallback)
        verifyNoMoreInteractions(openPgpApi)
    }

    @Test
    @Throws(MessagingException::class)
    fun buildEncryptWithAttach__withInlineEnabled__shouldThrow() {
        val cryptoStatus = defaultCryptoStatus.copy(cryptoMode = CryptoMode.CHOICE_ENABLED, isPgpInlineModeEnabled = true)

        pgpMessageBuilder.setCryptoStatus(cryptoStatus)
        pgpMessageBuilder.setAttachments(listOf(Attachment.createAttachment(null, 0, null, true)))

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
        returnIntent.putExtra(OpenPgpApi.RESULT_ERROR,
                OpenPgpError(OpenPgpError.OPPORTUNISTIC_MISSING_KEYS, "Missing keys"))


        `when`(openPgpApi.executeApi(any(Intent::class.java), any(OpenPgpDataSource::class.java), any(OutputStream::class.java)))
                .thenReturn(returnIntent)

        val mockCallback = mock(Callback::class.java)
        pgpMessageBuilder.buildAsync(mockCallback)


        val captor = ArgumentCaptor.forClass(MimeMessage::class.java)
        verify(mockCallback).onMessageBuildSuccess(captor.capture(), eq(false))
        verifyNoMoreInteractions(mockCallback)

        val message = captor.value
        Assert.assertEquals("text/plain", message.mimeType)
    }

    @Test
    @Throws(MessagingException::class)
    fun buildSign__withNoDetachedSignatureExtra__shouldFail() {
        val cryptoStatus = defaultCryptoStatus.copy(cryptoMode = CryptoMode.SIGN_ONLY)
        pgpMessageBuilder.setCryptoStatus(cryptoStatus)

        val returnIntentSigned = Intent()
        returnIntentSigned.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS)
        // no OpenPgpApi.EXTRA_DETACHED_SIGNATURE!


        `when`(openPgpApi.executeApi(any<Intent>(), any<OpenPgpDataSource>(), any<OutputStream>())).thenReturn(returnIntentSigned)
        val mockCallback = mock(Callback::class.java)
        pgpMessageBuilder.buildAsync(mockCallback)


        verify(mockCallback).onMessageBuildException(any<MessagingException>())
        verifyNoMoreInteractions(mockCallback)
    }

    companion object {
        private val TEST_KEY_ID = 123L
        private val TEST_MESSAGE_TEXT = "message text with a ☭ CCCP symbol"
        private val AUTOCRYPT_KEY_MATERIAL = byteArrayOf(1, 2, 3)
        private val SENDER_EMAIL = "test@example.org"

        private fun createDefaultPgpMessageBuilder(openPgpApi: OpenPgpApi,
                                                   autocryptOpenPgpApiInteractor: AutocryptOpenPgpApiInteractor): PgpMessageBuilder {
            val builder = PgpMessageBuilder(
                    RuntimeEnvironment.application, MessageIdGenerator.getInstance(), BoundaryGenerator.getInstance(),
                    AutocryptOperations.getInstance(), autocryptOpenPgpApiInteractor)
            builder.setOpenPgpApi(openPgpApi)

            val identity = Identity()
            identity.name = "tester"
            identity.email = SENDER_EMAIL
            identity.description = "test identity"
            identity.signatureUse = false

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

        private fun assertContentOfBodyPartEquals(reason: String, signatureBodyPart: BodyPart, expected: ByteArray) {
            try {
                val bos = ByteArrayOutputStream()
                signatureBodyPart.body.writeTo(bos)
                Assert.assertArrayEquals(reason, expected, bos.toByteArray())
            } catch (e: IOException) {
                Assert.fail()
            } catch (e: MessagingException) {
                Assert.fail()
            }

        }

        private fun assertContentOfBodyPartEquals(reason: String, signatureBodyPart: BodyPart, expected: String) {
            try {
                val bos = ByteArrayOutputStream()
                val inputStream = MimeUtility.decodeBody(signatureBodyPart.body)
                IOUtils.copy(inputStream, bos)
                Assert.assertEquals(reason, expected, String(bos.toByteArray(), Charsets.UTF_8))
            } catch (e: IOException) {
                Assert.fail()
            } catch (e: MessagingException) {
                Assert.fail()
            }

        }

        private fun assertIntentEqualsActionAndExtras(expected: Intent, actual: Intent) {
            Assert.assertEquals(expected.action, actual.action)

            val expectedExtras = expected.extras
            val intentExtras = actual.extras

            if (expectedExtras!!.size() != intentExtras!!.size()) {
                Assert.assertEquals(expectedExtras.size().toLong(), intentExtras.size().toLong())
            }

            for (key in expectedExtras.keySet()) {
                val intentExtra = intentExtras.get(key)
                val expectedExtra = expectedExtras.get(key)
                if (intentExtra == null) {
                    if (expectedExtra == null) {
                        continue
                    }
                    Assert.fail("found null for an expected non-null extra: $key")
                }
                if (intentExtra is LongArray) {
                    if (!Arrays.equals(intentExtra, expectedExtra as LongArray)) {
                        Assert.assertArrayEquals("error in $key", expectedExtra, intentExtra)
                    }
                } else {
                    if (intentExtra != expectedExtra) {
                        Assert.assertEquals("error in $key", expectedExtra, intentExtra)
                    }
                }
            }
        }
    }
}
