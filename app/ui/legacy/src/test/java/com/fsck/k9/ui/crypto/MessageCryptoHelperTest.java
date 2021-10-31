package com.fsck.k9.ui.crypto;


import java.io.InputStream;
import java.io.OutputStream;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.fsck.k9.RobolectricTest;
import com.fsck.k9.autocrypt.AutocryptOperations;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.mailstore.CryptoResultAnnotation;
import com.fsck.k9.mailstore.CryptoResultAnnotation.CryptoError;
import com.fsck.k9.mailstore.MessageCryptoAnnotations;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openintents.openpgp.IOpenPgpService2;
import org.openintents.openpgp.OpenPgpDecryptionResult;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpApi.IOpenPgpCallback;
import org.openintents.openpgp.util.OpenPgpApi.IOpenPgpSinkResultCallback;
import org.openintents.openpgp.util.OpenPgpApi.OpenPgpDataSink;
import org.openintents.openpgp.util.OpenPgpApi.OpenPgpDataSource;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.LooperMode;

import static com.fsck.k9.mail.TestMessageConstructionUtils.bodypart;
import static com.fsck.k9.mail.TestMessageConstructionUtils.messageFromBody;
import static com.fsck.k9.mail.TestMessageConstructionUtils.multipart;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


@SuppressWarnings("unchecked")
@LooperMode(LooperMode.Mode.LEGACY)
public class MessageCryptoHelperTest extends RobolectricTest {
    private MessageCryptoHelper messageCryptoHelper;
    private OpenPgpApi openPgpApi;
    private Intent capturedApiIntent;
    private IOpenPgpSinkResultCallback capturedCallback;
    private MessageCryptoCallback messageCryptoCallback;
    private AutocryptOperations autocryptOperations;


    @Before
    public void setUp() throws Exception {
        openPgpApi = mock(OpenPgpApi.class);
        autocryptOperations = mock(AutocryptOperations.class);

        OpenPgpApiFactory openPgpApiFactory = mock(OpenPgpApiFactory.class);
        when(openPgpApiFactory.createOpenPgpApi(any(Context.class), nullable(IOpenPgpService2.class)))
                .thenReturn(openPgpApi);

        messageCryptoHelper = new MessageCryptoHelper(RuntimeEnvironment.application, openPgpApiFactory,
                autocryptOperations, "org.example.dummy");
        messageCryptoCallback = mock(MessageCryptoCallback.class);
    }

    @Test
    public void textPlain() throws Exception {
        MimeMessage message = new MimeMessage();
        message.setUid("msguid");
        message.setHeader("Content-Type", "text/plain");

        MessageCryptoCallback messageCryptoCallback = mock(MessageCryptoCallback.class);
        messageCryptoHelper.asyncStartOrResumeProcessingMessage(message, messageCryptoCallback, null, false);

        ArgumentCaptor<MessageCryptoAnnotations> captor = ArgumentCaptor.forClass(MessageCryptoAnnotations.class);
        verify(messageCryptoCallback).onCryptoOperationsFinished(captor.capture());
        MessageCryptoAnnotations annotations = captor.getValue();
        assertTrue(annotations.isEmpty());
        verifyNoMoreInteractions(messageCryptoCallback);

        verify(autocryptOperations).hasAutocryptHeader(message);
        verifyNoMoreInteractions(autocryptOperations);
    }

    @Test
    public void textPlain_withAutocrypt() throws Exception {
        MimeMessage message = new MimeMessage();
        message.setUid("msguid");
        message.setHeader("Content-Type", "text/plain");

        when(autocryptOperations.hasAutocryptHeader(message)).thenReturn(true);
        when(autocryptOperations.addAutocryptPeerUpdateToIntentIfPresent(same(message), any(Intent.class))).thenReturn(true);


        MessageCryptoCallback messageCryptoCallback = mock(MessageCryptoCallback.class);
        messageCryptoHelper.asyncStartOrResumeProcessingMessage(message, messageCryptoCallback, null, false);


        ArgumentCaptor<MessageCryptoAnnotations> captor = ArgumentCaptor.forClass(MessageCryptoAnnotations.class);
        verify(messageCryptoCallback).onCryptoOperationsFinished(captor.capture());
        MessageCryptoAnnotations annotations = captor.getValue();
        assertTrue(annotations.isEmpty());
        verifyNoMoreInteractions(messageCryptoCallback);

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(autocryptOperations).addAutocryptPeerUpdateToIntentIfPresent(same(message), intentCaptor.capture());
        verify(openPgpApi).executeApiAsync(same(intentCaptor.getValue()), same((InputStream) null),
                same((OutputStream) null), any(IOpenPgpCallback.class));
    }

    @Test
    public void multipartSigned__withNullBody__shouldReturnSignedIncomplete() throws Exception {
        Message message = messageFromBody(
                multipart("signed", "protocol=\"application/pgp-signature\"",
                        bodypart("text/plain"),
                        bodypart("application/pgp-signature")
                )
        );

        MessageCryptoCallback messageCryptoCallback = mock(MessageCryptoCallback.class);
        messageCryptoHelper.asyncStartOrResumeProcessingMessage(message, messageCryptoCallback, null, true);

        assertPartAnnotationHasState(message, messageCryptoCallback, CryptoError.OPENPGP_SIGNED_BUT_INCOMPLETE, null,
                null, null, null);
    }

    @Test
    public void multipartEncrypted__withNullBody__shouldReturnEncryptedIncomplete() throws Exception {
        Message message = messageFromBody(
                multipart("encrypted", "protocol=\"application/pgp-encrypted\"",
                        bodypart("application/pgp-encrypted"),
                        bodypart("application/octet-stream")
                )
        );

        MessageCryptoCallback messageCryptoCallback = mock(MessageCryptoCallback.class);
        messageCryptoHelper.asyncStartOrResumeProcessingMessage(message, messageCryptoCallback, null, false);

        assertPartAnnotationHasState(
                message, messageCryptoCallback, CryptoError.OPENPGP_ENCRYPTED_BUT_INCOMPLETE, null, null, null, null);
    }

    @Test
    public void multipartEncrypted__withUnknownProtocol__shouldReturnEncryptedUnsupported() throws Exception {
        Message message = messageFromBody(
                multipart("encrypted", "protocol=\"application/bad-protocol\"",
                        bodypart("application/bad-protocol", "content"),
                        bodypart("application/octet-stream", "content")
                )
        );

        MessageCryptoCallback messageCryptoCallback = mock(MessageCryptoCallback.class);
        messageCryptoHelper.asyncStartOrResumeProcessingMessage(message, messageCryptoCallback, null, false);

        assertPartAnnotationHasState(message, messageCryptoCallback, CryptoError.ENCRYPTED_BUT_UNSUPPORTED, null, null,
                null, null);
    }

    @Test
    public void multipartSigned__withUnknownProtocol__shouldReturnSignedUnsupported() throws Exception {
        Message message = messageFromBody(
                multipart("signed", "protocol=\"application/bad-protocol\"",
                        bodypart("text/plain", "content"),
                        bodypart("application/bad-protocol", "content")
                )
        );

        MessageCryptoCallback messageCryptoCallback = mock(MessageCryptoCallback.class);
        messageCryptoHelper.asyncStartOrResumeProcessingMessage(message, messageCryptoCallback, null, true);

        assertPartAnnotationHasState(message, messageCryptoCallback, CryptoError.SIGNED_BUT_UNSUPPORTED, null, null,
                null, null);
    }

    @Test
    public void multipartSigned__shouldCallOpenPgpApiAsync() throws Exception {
        BodyPart signedBodyPart = spy(bodypart("text/plain", "content"));
        Message message = messageFromBody(
                multipart("signed", "protocol=\"application/pgp-signature\"",
                        signedBodyPart,
                        bodypart("application/pgp-signature", "content")
                )
        );
        message.setFrom(Address.parse("Test <test@example.org>")[0]);

        OutputStream outputStream = mock(OutputStream.class);


        processSignedMessageAndCaptureMocks(message, signedBodyPart, outputStream);


        assertEquals(OpenPgpApi.ACTION_DECRYPT_VERIFY, capturedApiIntent.getAction());
        assertEquals("test@example.org", capturedApiIntent.getStringExtra(OpenPgpApi.EXTRA_SENDER_ADDRESS));

        verify(autocryptOperations).addAutocryptPeerUpdateToIntentIfPresent(message, capturedApiIntent);
        verifyNoMoreInteractions(autocryptOperations);
    }

    @Test
    public void multipartSigned__withSignOnlyDisabled__shouldReturnNothing() throws Exception {
        Message message = messageFromBody(
                multipart("signed", "protocol=\"application/pgp-signature\"",
                        bodypart("text/plain", "content"),
                        bodypart("application/pgp-signature", "content")
                )
        );

        MessageCryptoCallback messageCryptoCallback = mock(MessageCryptoCallback.class);
        messageCryptoHelper.asyncStartOrResumeProcessingMessage(message, messageCryptoCallback, null, false);

        assertReturnsWithNoCryptoAnnotations(messageCryptoCallback);
    }

    @Test
    public void multipartSigned__withSignOnlyDisabledAndNullBody__shouldReturnNothing() throws Exception {
        Message message = messageFromBody(
                multipart("signed", "protocol=\"application/pgp-signature\"",
                        bodypart("text/plain"),
                        bodypart("application/pgp-signature")
                )
        );

        MessageCryptoCallback messageCryptoCallback = mock(MessageCryptoCallback.class);
        messageCryptoHelper.asyncStartOrResumeProcessingMessage(message, messageCryptoCallback, null, false);

        assertReturnsWithNoCryptoAnnotations(messageCryptoCallback);
    }

    @Test
    public void multipartEncrypted__shouldCallOpenPgpApiAsync() throws Exception {
        Body encryptedBody = spy(new TextBody("encrypted data"));
        Message message = messageFromBody(
                multipart("encrypted", "protocol=\"application/pgp-encrypted\"",
                        bodypart("application/pgp-encrypted", "content"),
                        bodypart("application/octet-stream", encryptedBody)
                )
        );
        message.setFrom(Address.parse("Test <test@example.org>")[0]);

        OutputStream outputStream = mock(OutputStream.class);

        Intent resultIntent = new Intent();
        resultIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);
        OpenPgpDecryptionResult decryptionResult = mock(OpenPgpDecryptionResult.class);
        resultIntent.putExtra(OpenPgpApi.RESULT_DECRYPTION, decryptionResult);
        OpenPgpSignatureResult signatureResult = mock(OpenPgpSignatureResult.class);
        resultIntent.putExtra(OpenPgpApi.RESULT_SIGNATURE, signatureResult);
        PendingIntent pendingIntent = mock(PendingIntent.class);
        resultIntent.putExtra(OpenPgpApi.RESULT_INTENT, pendingIntent);


        processEncryptedMessageAndCaptureMocks(message, encryptedBody, outputStream);

        MimeBodyPart decryptedPart = new MimeBodyPart(new TextBody("text"));
        capturedCallback.onReturn(resultIntent, decryptedPart);


        assertEquals(OpenPgpApi.ACTION_DECRYPT_VERIFY, capturedApiIntent.getAction());
        assertEquals("test@example.org", capturedApiIntent.getStringExtra(OpenPgpApi.EXTRA_SENDER_ADDRESS));
        assertPartAnnotationHasState(message, messageCryptoCallback, CryptoError.OPENPGP_OK, decryptedPart,
                decryptionResult, signatureResult, pendingIntent);
        verify(autocryptOperations).addAutocryptPeerUpdateToIntentIfPresent(message, capturedApiIntent);
        verifyNoMoreInteractions(autocryptOperations);
    }

    private void processEncryptedMessageAndCaptureMocks(Message message, Body encryptedBody, OutputStream outputStream)
            throws Exception {
        messageCryptoHelper.asyncStartOrResumeProcessingMessage(message, messageCryptoCallback, null, false);

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        ArgumentCaptor<OpenPgpDataSource> dataSourceCaptor = ArgumentCaptor.forClass(OpenPgpDataSource.class);
        ArgumentCaptor<IOpenPgpSinkResultCallback> callbackCaptor = ArgumentCaptor.forClass(IOpenPgpSinkResultCallback.class);
        verify(openPgpApi).executeApiAsync(intentCaptor.capture(), dataSourceCaptor.capture(),
                any(OpenPgpDataSink.class), callbackCaptor.capture());

        capturedApiIntent = intentCaptor.getValue();
        capturedCallback = callbackCaptor.getValue();

        OpenPgpDataSource dataSource = dataSourceCaptor.getValue();
        dataSource.writeTo(outputStream);
        verify(encryptedBody).writeTo(outputStream);
    }

    private void processSignedMessageAndCaptureMocks(Message message, BodyPart signedBodyPart,
            OutputStream outputStream) throws Exception {
        messageCryptoHelper.asyncStartOrResumeProcessingMessage(message, messageCryptoCallback, null, true);

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        ArgumentCaptor<OpenPgpDataSource> dataSourceCaptor = ArgumentCaptor.forClass(OpenPgpDataSource.class);
        ArgumentCaptor<IOpenPgpSinkResultCallback> callbackCaptor = ArgumentCaptor.forClass(IOpenPgpSinkResultCallback.class);
        verify(openPgpApi).executeApiAsync(intentCaptor.capture(), dataSourceCaptor.capture(),
                callbackCaptor.capture());

        capturedApiIntent = intentCaptor.getValue();
        capturedCallback = callbackCaptor.getValue();

        OpenPgpDataSource dataSource = dataSourceCaptor.getValue();
        dataSource.writeTo(outputStream);
        verify(signedBodyPart).writeTo(outputStream);
    }

    private void assertReturnsWithNoCryptoAnnotations(MessageCryptoCallback messageCryptoCallback) {
        ArgumentCaptor<MessageCryptoAnnotations> captor = ArgumentCaptor.forClass(MessageCryptoAnnotations.class);
        verify(messageCryptoCallback).onCryptoOperationsFinished(captor.capture());
        verifyNoMoreInteractions(messageCryptoCallback);

        MessageCryptoAnnotations annotations = captor.getValue();
        assertTrue(annotations.isEmpty());
    }

    private void assertPartAnnotationHasState(Message message, MessageCryptoCallback messageCryptoCallback,
            CryptoError cryptoErrorState, MimeBodyPart replacementPart, OpenPgpDecryptionResult openPgpDecryptionResult,
            OpenPgpSignatureResult openPgpSignatureResult, PendingIntent openPgpPendingIntent) {
        ArgumentCaptor<MessageCryptoAnnotations> captor = ArgumentCaptor.forClass(MessageCryptoAnnotations.class);
        verify(messageCryptoCallback).onCryptoOperationsFinished(captor.capture());
        MessageCryptoAnnotations annotations = captor.getValue();
        CryptoResultAnnotation cryptoResultAnnotation = annotations.get(message);
        assertEquals(cryptoErrorState, cryptoResultAnnotation.getErrorType());
        if (replacementPart != null) {
            assertSame(replacementPart, cryptoResultAnnotation.getReplacementData());
        }
        assertSame(openPgpDecryptionResult, cryptoResultAnnotation.getOpenPgpDecryptionResult());
        assertSame(openPgpSignatureResult, cryptoResultAnnotation.getOpenPgpSignatureResult());
        assertSame(openPgpPendingIntent, cryptoResultAnnotation.getOpenPgpPendingIntent());
        verifyNoMoreInteractions(messageCryptoCallback);
    }

}
