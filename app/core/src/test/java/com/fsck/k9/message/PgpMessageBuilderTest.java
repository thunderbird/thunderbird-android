package com.fsck.k9.message;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;

import com.fsck.k9.Account.QuoteStyle;
import com.fsck.k9.Identity;
import com.fsck.k9.K9RobolectricTest;
import com.fsck.k9.TestCoreResourceProvider;
import com.fsck.k9.autocrypt.AutocryptOpenPgpApiInteractor;
import com.fsck.k9.autocrypt.AutocryptOperations;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.BoundaryGenerator;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.BinaryTempFileBody;
import com.fsck.k9.mail.internet.MessageIdGenerator;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.message.MessageBuilder.Callback;
import com.fsck.k9.message.quote.InsertableHtmlContent;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.util.MimeUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpApi.OpenPgpDataSource;

import static com.fsck.k9.autocrypt.AutocryptOperationsHelper.assertMessageHasAutocryptHeader;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


public class PgpMessageBuilderTest extends K9RobolectricTest {
    private static final long TEST_KEY_ID = 123L;
    private static final String TEST_MESSAGE_TEXT = "message text with a ☭ CCCP symbol";
    private static final byte[] AUTOCRYPT_KEY_MATERIAL = { 1, 2, 3 };
    private static final String SENDER_EMAIL = "test@example.org";


    private CryptoStatus cryptoStatus = createCryptoStatus();
    private OpenPgpApi openPgpApi = mock(OpenPgpApi.class);
    private AutocryptOpenPgpApiInteractor autocryptOpenPgpApiInteractor = mock(AutocryptOpenPgpApiInteractor.class);
    private PgpMessageBuilder pgpMessageBuilder = createDefaultPgpMessageBuilder(openPgpApi,
            autocryptOpenPgpApiInteractor, cryptoStatus);

    @Before
    public void setUp() throws Exception {
        when(autocryptOpenPgpApiInteractor.getKeyMaterialForKeyId(openPgpApi, TEST_KEY_ID, SENDER_EMAIL))
                .thenReturn(AUTOCRYPT_KEY_MATERIAL);
    }

    @Test
    public void build__withCryptoProviderNotOk__shouldThrow() {
        configureEncryptAndSign();
        when(cryptoStatus.isProviderStateOk()).thenReturn(false);

        Callback mockCallback = mock(Callback.class);
        pgpMessageBuilder.buildAsync(mockCallback);

        verify(mockCallback).onMessageBuildException(any(MessagingException.class));
        verifyNoMoreInteractions(mockCallback);
    }

    @Test
    public void buildCleartext__withNoSigningKey__shouldBuildTrivialMessage() {
        configureCleartext();
        when(cryptoStatus.getOpenPgpKeyId()).thenReturn(null);

        Callback mockCallback = mock(Callback.class);
        pgpMessageBuilder.buildAsync(mockCallback);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mockCallback).onMessageBuildSuccess(captor.capture(), eq(false));
        verifyNoMoreInteractions(mockCallback);

        MimeMessage message = captor.getValue();
        assertEquals("text/plain", message.getMimeType());
    }

    @Test
    public void buildCleartext__shouldSucceed() {
        configureCleartext();

        Callback mockCallback = mock(Callback.class);
        pgpMessageBuilder.buildAsync(mockCallback);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mockCallback).onMessageBuildSuccess(captor.capture(), eq(false));
        verifyNoMoreInteractions(mockCallback);

        MimeMessage message = captor.getValue();
        assertMessageHasAutocryptHeader(message, SENDER_EMAIL, false, AUTOCRYPT_KEY_MATERIAL);
    }

    @Test
    public void buildSign__withNoDetachedSignatureInResult__shouldThrow() {
        configureSignOnly();

        Intent returnIntent = new Intent();
        returnIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);
        when(openPgpApi.executeApi(any(Intent.class), any(OpenPgpDataSource.class), nullable(OutputStream.class)))
                .thenReturn(returnIntent);

        Callback mockCallback = mock(Callback.class);
        pgpMessageBuilder.buildAsync(mockCallback);

        verify(mockCallback).onMessageBuildException(any(MessagingException.class));
        verifyNoMoreInteractions(mockCallback);
    }

    @Test
    public void buildSign__withDetachedSignatureInResult__shouldSucceed() {
        configureSignOnly();

        ArgumentCaptor<Intent> capturedApiIntent = ArgumentCaptor.forClass(Intent.class);

        Intent returnIntent = new Intent();
        returnIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);
        returnIntent.putExtra(OpenPgpApi.RESULT_DETACHED_SIGNATURE, new byte[] { 1, 2, 3 });
        when(openPgpApi.executeApi(capturedApiIntent.capture(), any(OpenPgpDataSource.class),
                nullable(OutputStream.class))).thenReturn(returnIntent);

        Callback mockCallback = mock(Callback.class);
        pgpMessageBuilder.buildAsync(mockCallback);

        Intent expectedIntent = new Intent(OpenPgpApi.ACTION_DETACHED_SIGN);
        expectedIntent.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, TEST_KEY_ID);
        expectedIntent.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);
        assertIntentEqualsActionAndExtras(expectedIntent, capturedApiIntent.getValue());

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mockCallback).onMessageBuildSuccess(captor.capture(), eq(false));
        verifyNoMoreInteractions(mockCallback);

        MimeMessage message = captor.getValue();
        Assert.assertEquals("message must be multipart/signed", "multipart/signed", message.getMimeType());

        MimeMultipart multipart = (MimeMultipart) message.getBody();
        Assert.assertEquals("multipart/signed must consist of two parts", 2, multipart.getCount());

        BodyPart contentBodyPart = multipart.getBodyPart(0);
        Assert.assertEquals("first part must have content type text/plain",
                "text/plain", MimeUtility.getHeaderParameter(contentBodyPart.getContentType(), null));
        assertTrue("signed message body must be TextBody", contentBodyPart.getBody() instanceof TextBody);
        Assert.assertEquals(MimeUtil.ENC_QUOTED_PRINTABLE, ((TextBody) contentBodyPart.getBody()).getEncoding());
        assertContentOfBodyPartEquals("content must match the message text", contentBodyPart, TEST_MESSAGE_TEXT);

        BodyPart signatureBodyPart = multipart.getBodyPart(1);
        String contentType = signatureBodyPart.getContentType();
        Assert.assertEquals("second part must be pgp signature", "application/pgp-signature",
                MimeUtility.getHeaderParameter(contentType, null));
        Assert.assertEquals("second part must be called signature.asc", "signature.asc",
                MimeUtility.getHeaderParameter(contentType, "name"));
        assertContentOfBodyPartEquals("content must match the supplied detached signature",
                signatureBodyPart, new byte[] { 1, 2, 3 });

        assertMessageHasAutocryptHeader(message, SENDER_EMAIL, false, AUTOCRYPT_KEY_MATERIAL);
    }

    @Test
    public void buildSign__withUserInteractionResult__shouldReturnUserInteraction() {
        configureSignOnly();

        Intent returnIntent = mock(Intent.class);
        when(returnIntent.getIntExtra(eq(OpenPgpApi.RESULT_CODE), anyInt()))
                .thenReturn(OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED);
        final PendingIntent mockPendingIntent = mock(PendingIntent.class);
        when(returnIntent.getParcelableExtra(eq(OpenPgpApi.RESULT_INTENT)))
                .thenReturn(mockPendingIntent);

        when(openPgpApi.executeApi(any(Intent.class), any(OpenPgpDataSource.class), nullable(OutputStream.class)))
                .thenReturn(returnIntent);

        Callback mockCallback = mock(Callback.class);
        pgpMessageBuilder.buildAsync(mockCallback);

        ArgumentCaptor<PendingIntent> captor = ArgumentCaptor.forClass(PendingIntent.class);
        verify(mockCallback).onMessageBuildReturnPendingIntent(captor.capture(), anyInt());
        verifyNoMoreInteractions(mockCallback);

        PendingIntent pendingIntent = captor.getValue();
        Assert.assertSame(pendingIntent, mockPendingIntent);
    }

    @Test
    public void buildSign__withReturnAfterUserInteraction__shouldSucceed() {
        configureSignOnly();

        int returnedRequestCode;
        {
            Intent returnIntent = spy(new Intent());
            returnIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED);

            PendingIntent mockPendingIntent = mock(PendingIntent.class);
            when(returnIntent.getParcelableExtra(eq(OpenPgpApi.RESULT_INTENT)))
                    .thenReturn(mockPendingIntent);

            when(openPgpApi.executeApi(any(Intent.class), any(OpenPgpDataSource.class), nullable(OutputStream.class)))
                    .thenReturn(returnIntent);

            Callback mockCallback = mock(Callback.class);
            pgpMessageBuilder.buildAsync(mockCallback);

            verify(returnIntent).getIntExtra(eq(OpenPgpApi.RESULT_CODE), anyInt());
            ArgumentCaptor<PendingIntent> piCaptor = ArgumentCaptor.forClass(PendingIntent.class);
            ArgumentCaptor<Integer> rcCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(mockCallback).onMessageBuildReturnPendingIntent(piCaptor.capture(), rcCaptor.capture());
            verifyNoMoreInteractions(mockCallback);

            returnedRequestCode = rcCaptor.getValue();
            Assert.assertSame(mockPendingIntent, piCaptor.getValue());
        }

        {
            Intent returnIntent = spy(new Intent());
            returnIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);

            Intent mockReturnIntent = mock(Intent.class);
            when(openPgpApi.executeApi(same(mockReturnIntent), any(OpenPgpDataSource.class),
                    nullable(OutputStream.class))).thenReturn(returnIntent);

            Callback mockCallback = mock(Callback.class);
            pgpMessageBuilder.onActivityResult(returnedRequestCode, Activity.RESULT_OK, mockReturnIntent, mockCallback);
            verify(openPgpApi).executeApi(same(mockReturnIntent), any(OpenPgpDataSource.class),
                    nullable(OutputStream.class));
            verify(returnIntent).getIntExtra(eq(OpenPgpApi.RESULT_CODE), anyInt());
        }
    }

    @Test
    public void buildEncrypt__withoutRecipients__shouldThrow() {
        configureEncryptAndSign();

        Intent returnIntent = spy(new Intent());
        returnIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);
        when(openPgpApi.executeApi(any(Intent.class), any(OpenPgpDataSource.class), nullable(OutputStream.class)))
                .thenReturn(returnIntent);

        Callback mockCallback = mock(Callback.class);
        pgpMessageBuilder.buildAsync(mockCallback);

        verify(mockCallback).onMessageBuildException(any(MessagingException.class));
        verifyNoMoreInteractions(mockCallback);
    }

    @Test
    public void buildEncrypt__checkGossip() {
        configureEncryptAndSign();
        configureRecipients("alice@example.org", "bob@example.org");

        Intent returnIntent = new Intent();
        returnIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);
        when(openPgpApi.executeApi(any(Intent.class), any(OpenPgpDataSource.class), nullable(OutputStream.class)))
                .thenReturn(returnIntent);
        pgpMessageBuilder.buildAsync(mock(Callback.class));

        verify(autocryptOpenPgpApiInteractor).getKeyMaterialForUserId(same(openPgpApi), eq("alice@example.org"));
        verify(autocryptOpenPgpApiInteractor).getKeyMaterialForUserId(same(openPgpApi), eq("bob@example.org"));
    }

    @Test
    public void buildEncrypt__checkGossip__filterBcc() {
        configureEncryptAndSign();
        configureRecipients("alice@example.org", "bob@example.org", "carol@example.org");
        pgpMessageBuilder.setBcc(Collections.singletonList(new Address("carol@example.org")));

        Intent returnIntent = new Intent();
        returnIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);
        when(openPgpApi.executeApi(any(Intent.class), any(OpenPgpDataSource.class), nullable(OutputStream.class)))
                .thenReturn(returnIntent);
        pgpMessageBuilder.buildAsync(mock(Callback.class));

        verify(autocryptOpenPgpApiInteractor).getKeyMaterialForUserId(same(openPgpApi), eq("alice@example.org"));
        verify(autocryptOpenPgpApiInteractor).getKeyMaterialForUserId(same(openPgpApi), eq("bob@example.org"));
    }

    @Test
    public void buildEncrypt__checkGossip__filterBccSingleRecipient() {
        configureEncryptAndSign();
        configureRecipients("alice@example.org", "carol@example.org");
        pgpMessageBuilder.setBcc(Collections.singletonList(new Address("carol@example.org")));

        Intent returnIntent = new Intent();
        returnIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);
        when(openPgpApi.executeApi(any(Intent.class), any(OpenPgpDataSource.class), nullable(OutputStream.class)))
                .thenReturn(returnIntent);
        pgpMessageBuilder.buildAsync(mock(Callback.class));

        verify(autocryptOpenPgpApiInteractor).getKeyMaterialForKeyId(any(OpenPgpApi.class), any(Long.class), any(String.class));
        verifyNoMoreInteractions(autocryptOpenPgpApiInteractor);
    }

    @Test
    public void buildEncrypt__shouldSucceed() {
        configureEncryptAndSign();
        configureRecipients("test@example.org");

        ArgumentCaptor<Intent> capturedApiIntent = ArgumentCaptor.forClass(Intent.class);

        Intent returnIntent = new Intent();
        returnIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);

        when(openPgpApi.executeApi(capturedApiIntent.capture(), any(OpenPgpDataSource.class),
                nullable(OutputStream.class))).thenReturn(returnIntent);

        Callback mockCallback = mock(Callback.class);
        pgpMessageBuilder.buildAsync(mockCallback);

        Intent expectedApiIntent = new Intent(OpenPgpApi.ACTION_SIGN_AND_ENCRYPT);
        expectedApiIntent.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, TEST_KEY_ID);
        expectedApiIntent.putExtra(OpenPgpApi.EXTRA_KEY_IDS, new long[] { TEST_KEY_ID });
        expectedApiIntent.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);
        expectedApiIntent.putExtra(OpenPgpApi.EXTRA_USER_IDS, cryptoStatus.getRecipientAddresses());
        assertIntentEqualsActionAndExtras(expectedApiIntent, capturedApiIntent.getValue());

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mockCallback).onMessageBuildSuccess(captor.capture(), eq(false));
        verifyNoMoreInteractions(mockCallback);

        MimeMessage message = captor.getValue();

        Assert.assertEquals("message must be multipart/encrypted", "multipart/encrypted", message.getMimeType());

        MimeMultipart multipart = (MimeMultipart) message.getBody();
        Assert.assertEquals("multipart/encrypted must consist of two parts", 2, multipart.getCount());

        BodyPart dummyBodyPart = multipart.getBodyPart(0);
        Assert.assertEquals("first part must be pgp encrypted dummy part",
                "application/pgp-encrypted", dummyBodyPart.getContentType());
        assertContentOfBodyPartEquals("content must match the supplied detached signature",
                dummyBodyPart, "Version: 1");

        BodyPart encryptedBodyPart = multipart.getBodyPart(1);
        Assert.assertEquals("second part must be octet-stream of encrypted data",
                "application/octet-stream; name=\"encrypted.asc\"", encryptedBodyPart.getContentType());
        assertTrue("message body must be BinaryTempFileBody",
                encryptedBodyPart.getBody() instanceof BinaryTempFileBody);
        Assert.assertEquals(MimeUtil.ENC_7BIT, ((BinaryTempFileBody) encryptedBodyPart.getBody()).getEncoding());

        assertMessageHasAutocryptHeader(message, SENDER_EMAIL, false, AUTOCRYPT_KEY_MATERIAL);
    }

    @Test
    public void buildEncrypt__withInlineEnabled__shouldSucceed() {
        configureEncryptAndSign();
        configureRecipients("test@example.org");
        configurePgpInline();

        ArgumentCaptor<Intent> capturedApiIntent = ArgumentCaptor.forClass(Intent.class);

        Intent returnIntent = new Intent();
        returnIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);

        when(openPgpApi.executeApi(capturedApiIntent.capture(), any(OpenPgpDataSource.class),
                nullable(OutputStream.class))).thenReturn(returnIntent);

        Callback mockCallback = mock(Callback.class);
        pgpMessageBuilder.buildAsync(mockCallback);

        Intent expectedApiIntent = new Intent(OpenPgpApi.ACTION_SIGN_AND_ENCRYPT);
        expectedApiIntent.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, TEST_KEY_ID);
        expectedApiIntent.putExtra(OpenPgpApi.EXTRA_KEY_IDS, new long[] { TEST_KEY_ID });
        expectedApiIntent.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);
        expectedApiIntent.putExtra(OpenPgpApi.EXTRA_USER_IDS, cryptoStatus.getRecipientAddresses());
        assertIntentEqualsActionAndExtras(expectedApiIntent, capturedApiIntent.getValue());

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mockCallback).onMessageBuildSuccess(captor.capture(), eq(false));
        verifyNoMoreInteractions(mockCallback);

        MimeMessage message = captor.getValue();
        Assert.assertEquals("text/plain", message.getMimeType());
        assertTrue("message body must be BinaryTempFileBody", message.getBody() instanceof BinaryTempFileBody);
        Assert.assertEquals(MimeUtil.ENC_7BIT, ((BinaryTempFileBody) message.getBody()).getEncoding());

        assertMessageHasAutocryptHeader(message, SENDER_EMAIL, false, AUTOCRYPT_KEY_MATERIAL);
    }

    @Test
    public void buildSign__withInlineEnabled__shouldSucceed() {
        configureSignOnly();
        configureRecipients("test@example.org");
        configurePgpInline();

        ArgumentCaptor<Intent> capturedApiIntent = ArgumentCaptor.forClass(Intent.class);

        Intent returnIntent = new Intent();
        returnIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);

        when(openPgpApi.executeApi(capturedApiIntent.capture(), any(OpenPgpDataSource.class),
                nullable(OutputStream.class))).thenReturn(returnIntent);

        Callback mockCallback = mock(Callback.class);
        pgpMessageBuilder.buildAsync(mockCallback);

        Intent expectedApiIntent = new Intent(OpenPgpApi.ACTION_SIGN);
        expectedApiIntent.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, TEST_KEY_ID);
        expectedApiIntent.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);
        assertIntentEqualsActionAndExtras(expectedApiIntent, capturedApiIntent.getValue());

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mockCallback).onMessageBuildSuccess(captor.capture(), eq(false));
        verifyNoMoreInteractions(mockCallback);

        MimeMessage message = captor.getValue();
        Assert.assertEquals("message must be text/plain", "text/plain", message.getMimeType());

        assertMessageHasAutocryptHeader(message, SENDER_EMAIL, false, AUTOCRYPT_KEY_MATERIAL);
    }

    @Test
    public void buildSignWithAttach__withInlineEnabled__shouldThrow() {
        configureSignOnly();
        configurePgpInline();
        pgpMessageBuilder.setAttachments(createAttachmentList());

        Callback mockCallback = mock(Callback.class);
        pgpMessageBuilder.buildAsync(mockCallback);

        verify(mockCallback).onMessageBuildException(any(MessagingException.class));
        verifyNoMoreInteractions(mockCallback);
        verifyNoMoreInteractions(openPgpApi);
    }

    @Test
    public void buildEncryptWithAttach__withInlineEnabled__shouldThrow() {
        configureEncryptAndSign();
        configurePgpInline();
        pgpMessageBuilder.setAttachments(createAttachmentList());

        Callback mockCallback = mock(Callback.class);
        pgpMessageBuilder.buildAsync(mockCallback);

        verify(mockCallback).onMessageBuildException(any(MessagingException.class));
        verifyNoMoreInteractions(mockCallback);
        verifyNoMoreInteractions(openPgpApi);
    }

    @Test
    public void buildSign__withNoDetachedSignatureExtra__shouldFail() {
        configureSignOnly();

        Intent returnIntentSigned = new Intent();
        returnIntentSigned.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);
        // no OpenPgpApi.EXTRA_DETACHED_SIGNATURE!


        when(openPgpApi.executeApi(any(Intent.class), any(OpenPgpDataSource.class), nullable(OutputStream.class)))
                .thenReturn(returnIntentSigned);
        Callback mockCallback = mock(Callback.class);
        pgpMessageBuilder.buildAsync(mockCallback);


        verify(mockCallback).onMessageBuildException(any(MessagingException.class));
        verifyNoMoreInteractions(mockCallback);
    }

    private CryptoStatus createCryptoStatus() {
        CryptoStatus cryptoStatus = mock(CryptoStatus.class);
        when(cryptoStatus.isPgpInlineModeEnabled()).thenReturn(false);
        when(cryptoStatus.isSenderPreferEncryptMutual()).thenReturn(false);
        when(cryptoStatus.isEncryptSubject()).thenReturn(true);
        when(cryptoStatus.getOpenPgpKeyId()).thenReturn(TEST_KEY_ID);
        when(cryptoStatus.getRecipientAddresses()).thenReturn(new String[0]);
        when(cryptoStatus.hasRecipients()).thenReturn(false);
        when(cryptoStatus.isProviderStateOk()).thenReturn(true);
        return cryptoStatus;
    }

    private void configureEncryptAndSign() {
        when(cryptoStatus.isEncryptionEnabled()).thenReturn(true);
        when(cryptoStatus.isSigningEnabled()).thenReturn(true);
    }

    private void configureSignOnly() {
        when(cryptoStatus.isEncryptionEnabled()).thenReturn(false);
        when(cryptoStatus.isSigningEnabled()).thenReturn(true);
    }

    private void configureCleartext() {
        when(cryptoStatus.isEncryptionEnabled()).thenReturn(false);
        when(cryptoStatus.isSigningEnabled()).thenReturn(false);
    }

    private void configurePgpInline() {
        when(cryptoStatus.isPgpInlineModeEnabled()).thenReturn(true);
    }

    private void configureRecipients(String... recipients) {
        when(cryptoStatus.hasRecipients()).thenReturn(true);
        when(cryptoStatus.getRecipientAddresses()).thenReturn(recipients);
    }

    private static PgpMessageBuilder createDefaultPgpMessageBuilder(OpenPgpApi openPgpApi,
            AutocryptOpenPgpApiInteractor autocryptOpenPgpApiInteractor, CryptoStatus cryptoStatus) {
        PgpMessageBuilder builder = new PgpMessageBuilder(
                MessageIdGenerator.getInstance(), BoundaryGenerator.getInstance(),
                AutocryptOperations.getInstance(), autocryptOpenPgpApiInteractor, new TestCoreResourceProvider());
        builder.setOpenPgpApi(openPgpApi);
        builder.setCryptoStatus(cryptoStatus);

        Identity identity = new Identity();
        identity.setName("tester");
        identity.setEmail(SENDER_EMAIL);
        identity.setDescription("test identity");
        identity.setSignatureUse(false);

        builder.setSubject("subject")
                .setSentDate(new Date())
                .setHideTimeZone(false)
                .setTo(new ArrayList<Address>())
                .setCc(new ArrayList<Address>())
                .setBcc(new ArrayList<Address>())
                .setInReplyTo("inreplyto")
                .setReferences("references")
                .setRequestReadReceipt(false)
                .setIdentity(identity)
                .setMessageFormat(SimpleMessageFormat.TEXT)
                .setText(TEST_MESSAGE_TEXT)
                .setAttachments(new ArrayList<Attachment>())
                .setSignature("signature")
                .setQuoteStyle(QuoteStyle.PREFIX)
                .setQuotedTextMode(QuotedTextMode.NONE)
                .setQuotedText("quoted text")
                .setQuotedHtmlContent(new InsertableHtmlContent())
                .setReplyAfterQuote(false)
                .setSignatureBeforeQuotedText(false)
                .setIdentityChanged(false)
                .setSignatureChanged(false)
                .setCursorPosition(0)
                .setMessageReference(null)
                .setDraft(false);

        return builder;
    }

    private static List<Attachment> createAttachmentList() {
        Attachment attachment = mock(Attachment.class);
        when(attachment.getState()).thenReturn(Attachment.LoadingState.URI_ONLY);
        return Collections.singletonList(attachment);
    }

    private static void assertContentOfBodyPartEquals(String reason, BodyPart signatureBodyPart, byte[] expected) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            signatureBodyPart.getBody().writeTo(bos);
            Assert.assertArrayEquals(reason, expected, bos.toByteArray());
        } catch (IOException | MessagingException e) {
            Assert.fail();
        }
    }

    private static void assertContentOfBodyPartEquals(String reason, BodyPart signatureBodyPart, String expected) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            InputStream inputStream = MimeUtility.decodeBody(signatureBodyPart.getBody());
            IOUtils.copy(inputStream, bos);
            Assert.assertEquals(reason, expected, new String(bos.toByteArray(), Charsets.UTF_8));
        } catch (IOException | MessagingException e) {
            Assert.fail();
        }
    }

    private static void assertIntentEqualsActionAndExtras(Intent expected, Intent actual) {
        Assert.assertEquals(expected.getAction(), actual.getAction());

        Bundle expectedExtras = expected.getExtras();
        Bundle intentExtras = actual.getExtras();

        if (expectedExtras.size() != intentExtras.size()) {
            Assert.assertEquals(expectedExtras.size(), intentExtras.size());
        }

        for (String key : expectedExtras.keySet()) {
            Object intentExtra = intentExtras.get(key);
            Object expectedExtra = expectedExtras.get(key);
            if (intentExtra == null) {
                if (expectedExtra == null) {
                    continue;
                }
                Assert.fail("found null for an expected non-null extra: " + key);
            }
            if (intentExtra instanceof long[]) {
                if (!Arrays.equals((long[]) intentExtra, (long[]) expectedExtra)) {
                    Assert.assertArrayEquals("error in " + key, (long[]) expectedExtra, (long[]) intentExtra);
                }
            } else {
                if (!intentExtra.equals(expectedExtra)) {
                    Assert.assertEquals("error in " + key, expectedExtra, intentExtra);
                }
            }
        }
    }
}
