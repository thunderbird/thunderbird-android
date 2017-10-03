package com.fsck.k9.message;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.fsck.k9.Globals;
import com.fsck.k9.activity.compose.ComposeCryptoStatus;
import com.fsck.k9.autocrypt.AutocryptOpenPgpApiInteractor;
import com.fsck.k9.autocrypt.AutocryptOperations;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.BoundaryGenerator;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.filter.EOLConvertingOutputStream;
import com.fsck.k9.mail.internet.BinaryTempFileBody;
import com.fsck.k9.mail.internet.MessageIdGenerator;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMessageHelper;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.mailstore.BinaryMemoryBody;
import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.util.MimeUtil;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpApi.OpenPgpDataSource;
import timber.log.Timber;


public class PgpMessageBuilder extends MessageBuilder {
    private static final int REQUEST_USER_INTERACTION = 1;


    private final AutocryptOperations autocryptOperations;
    private final AutocryptOpenPgpApiInteractor autocryptOpenPgpApiInteractor;


    private OpenPgpApi openPgpApi;

    private MimeMessage currentProcessedMimeMessage;
    private ComposeCryptoStatus cryptoStatus;


    public static PgpMessageBuilder newInstance() {
        Context context = Globals.getContext();
        MessageIdGenerator messageIdGenerator = MessageIdGenerator.getInstance();
        BoundaryGenerator boundaryGenerator = BoundaryGenerator.getInstance();
        AutocryptOperations autocryptOperations = AutocryptOperations.getInstance();
        AutocryptOpenPgpApiInteractor autocryptOpenPgpApiInteractor = AutocryptOpenPgpApiInteractor.getInstance();
        return new PgpMessageBuilder(context, messageIdGenerator, boundaryGenerator, autocryptOperations,
                autocryptOpenPgpApiInteractor);
    }

    @VisibleForTesting
    PgpMessageBuilder(Context context, MessageIdGenerator messageIdGenerator, BoundaryGenerator boundaryGenerator,
            AutocryptOperations autocryptOperations, AutocryptOpenPgpApiInteractor autocryptOpenPgpApiInteractor) {
        super(context, messageIdGenerator, boundaryGenerator);

        this.autocryptOperations = autocryptOperations;
        this.autocryptOpenPgpApiInteractor = autocryptOpenPgpApiInteractor;
    }


    public void setOpenPgpApi(OpenPgpApi openPgpApi) {
        this.openPgpApi = openPgpApi;
    }

    @Override
    protected void buildMessageInternal() {
        if (currentProcessedMimeMessage != null) {
            throw new IllegalStateException("message can only be built once!");
        }
        if (cryptoStatus == null) {
            throw new IllegalStateException("PgpMessageBuilder must have cryptoStatus set before building!");
        }

        Long openPgpKeyId = cryptoStatus.getOpenPgpKeyId();
        try {
            currentProcessedMimeMessage = build();
        } catch (MessagingException me) {
            queueMessageBuildException(me);
            return;
        }

        if (openPgpKeyId == null) {
            queueMessageBuildSuccess(currentProcessedMimeMessage);
            return;
        }

        if (!cryptoStatus.isProviderStateOk()) {
            queueMessageBuildException(new MessagingException("OpenPGP Provider is not ready!"));
            return;
        }

        Address address = currentProcessedMimeMessage.getFrom()[0];
        byte[] keyData = autocryptOpenPgpApiInteractor.getKeyMaterialFromApi(
                openPgpApi, openPgpKeyId, address.getAddress());
        if (keyData != null) {
            autocryptOperations.addAutocryptHeaderToMessage(
                    currentProcessedMimeMessage, keyData, address.getAddress(), false);
        }

        startOrContinueBuildMessage(null);
    }

    @Override
    public void buildMessageOnActivityResult(int requestCode, @NonNull Intent userInteractionResult) {
        if (currentProcessedMimeMessage == null) {
            throw new AssertionError("build message from activity result must not be called individually");
        }
        startOrContinueBuildMessage(userInteractionResult);
    }

    private void startOrContinueBuildMessage(@Nullable Intent pgpApiIntent) {
        try {
            boolean shouldSign = cryptoStatus.isSigningEnabled();
            boolean shouldEncrypt = cryptoStatus.isEncryptionEnabled();
            boolean isPgpInlineMode = cryptoStatus.isPgpInlineModeEnabled();

            if (!shouldSign && !shouldEncrypt) {
                queueMessageBuildSuccess(currentProcessedMimeMessage);
                return;
            }

            boolean isSimpleTextMessage =
                    MimeUtility.isSameMimeType("text/plain", currentProcessedMimeMessage.getMimeType());
            if (isPgpInlineMode && !isSimpleTextMessage) {
                throw new MessagingException("Attachments are not supported in PGP/INLINE format!");
            }

            if (shouldEncrypt && !cryptoStatus.hasRecipients()) {
                throw new MessagingException("Must have recipients to build message!");
            }

            if (pgpApiIntent == null) {
                pgpApiIntent = buildOpenPgpApiIntent(shouldSign, shouldEncrypt, isPgpInlineMode);
            }

            PendingIntent returnedPendingIntent = launchOpenPgpApiIntent(
                    pgpApiIntent, shouldEncrypt || isPgpInlineMode, shouldEncrypt || !isPgpInlineMode, isPgpInlineMode);
            if (returnedPendingIntent != null) {
                queueMessageBuildPendingIntent(returnedPendingIntent, REQUEST_USER_INTERACTION);
                return;
            }

            queueMessageBuildSuccess(currentProcessedMimeMessage);
        } catch (MessagingException me) {
            queueMessageBuildException(me);
        }
    }

    @NonNull
    private Intent buildOpenPgpApiIntent(boolean shouldSign, boolean shouldEncrypt, boolean isPgpInlineMode) {
        Intent pgpApiIntent;

        Long openPgpKeyId = cryptoStatus.getOpenPgpKeyId();
        if (shouldEncrypt) {
            if (!shouldSign) {
                throw new IllegalStateException("encrypt-only is not supported at this point and should never happen!");
            }
            // pgpApiIntent = new Intent(shouldSign ? OpenPgpApi.ACTION_SIGN_AND_ENCRYPT : OpenPgpApi.ACTION_ENCRYPT);
            pgpApiIntent = new Intent(OpenPgpApi.ACTION_SIGN_AND_ENCRYPT);

            long[] selfEncryptIds = { openPgpKeyId };
            pgpApiIntent.putExtra(OpenPgpApi.EXTRA_KEY_IDS, selfEncryptIds);

            if(!isDraft()) {
                pgpApiIntent.putExtra(OpenPgpApi.EXTRA_USER_IDS, cryptoStatus.getRecipientAddresses());
//                pgpApiIntent.putExtra(OpenPgpApi.EXTRA_ENCRYPT_OPPORTUNISTIC, cryptoStatus.isEncryptionOpportunistic());
            }
        } else {
            pgpApiIntent = new Intent(isPgpInlineMode ? OpenPgpApi.ACTION_SIGN : OpenPgpApi.ACTION_DETACHED_SIGN);
        }

        if (shouldSign) {
            pgpApiIntent.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, openPgpKeyId);
        }

        pgpApiIntent.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);
        return pgpApiIntent;
    }

    private PendingIntent launchOpenPgpApiIntent(@NonNull Intent openPgpIntent,
            boolean captureOutputPart, boolean capturedOutputPartIs7Bit, boolean writeBodyContentOnly) throws MessagingException {
        final MimeBodyPart bodyPart = currentProcessedMimeMessage.toBodyPart();
        String[] contentType = currentProcessedMimeMessage.getHeader(MimeHeader.HEADER_CONTENT_TYPE);
        if (contentType.length > 0) {
            bodyPart.setHeader(MimeHeader.HEADER_CONTENT_TYPE, contentType[0]);
        }

        OpenPgpDataSource dataSource = createOpenPgpDataSourceFromBodyPart(bodyPart, writeBodyContentOnly);

        BinaryTempFileBody pgpResultTempBody = null;
        OutputStream outputStream = null;
        if (captureOutputPart) {
            try {
                pgpResultTempBody = new BinaryTempFileBody(
                        capturedOutputPartIs7Bit ? MimeUtil.ENC_7BIT : MimeUtil.ENC_8BIT);
                outputStream = pgpResultTempBody.getOutputStream();
                // OpenKeychain/BouncyCastle at this point use the system newline for formatting, which is LF on android.
                // we need this to be CRLF, so we convert the data after receiving.
                outputStream = new EOLConvertingOutputStream(outputStream);
            } catch (IOException e) {
                throw new MessagingException("could not allocate temp file for storage!", e);
            }
        }

        Intent result = openPgpApi.executeApi(openPgpIntent, dataSource, outputStream);

        switch (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
            case OpenPgpApi.RESULT_CODE_SUCCESS:
                mimeBuildMessage(result, bodyPart, pgpResultTempBody);
                return null;

            case OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED:
                PendingIntent returnedPendingIntent = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);
                if (returnedPendingIntent == null) {
                    throw new MessagingException("openpgp api needs user interaction, but returned no pendingintent!");
                }
                return returnedPendingIntent;

            case OpenPgpApi.RESULT_CODE_ERROR:
                OpenPgpError error = result.getParcelableExtra(OpenPgpApi.RESULT_ERROR);
                if (error == null) {
                    throw new MessagingException("internal openpgp api error");
                }
                /*
                boolean isOpportunisticError = error.getErrorId() == OpenPgpError.OPPORTUNISTIC_MISSING_KEYS;
                if (isOpportunisticError) {
                    if (!cryptoStatus.isEncryptionOpportunistic()) {
                        throw new IllegalStateException(
                                "Got opportunistic error, but encryption wasn't supposed to be opportunistic!");
                    }
                    Timber.d("Skipping encryption due to opportunistic mode");
                    return null;
                }
                */
                throw new MessagingException(error.getMessage());
        }

        throw new IllegalStateException("unreachable code segment reached");
    }

    @NonNull
    private OpenPgpDataSource createOpenPgpDataSourceFromBodyPart(final MimeBodyPart bodyPart,
            final boolean writeBodyContentOnly)
            throws MessagingException {
        return new OpenPgpDataSource() {
            @Override
            public void writeTo(OutputStream os) throws IOException {
                try {
                    if (writeBodyContentOnly) {
                        Body body = bodyPart.getBody();
                        InputStream inputStream = body.getInputStream();
                        IOUtils.copy(inputStream, os);
                    } else {
                        bodyPart.writeTo(os);
                    }
                } catch (MessagingException e) {
                    throw new IOException(e);
                }
            }
        };
    }

    private void mimeBuildMessage(
            @NonNull Intent result, @NonNull MimeBodyPart bodyPart, @Nullable BinaryTempFileBody pgpResultTempBody)
            throws MessagingException {
        if (pgpResultTempBody == null) {
            boolean shouldHaveResultPart = cryptoStatus.isPgpInlineModeEnabled() || cryptoStatus.isEncryptionEnabled();
            if (shouldHaveResultPart) {
                throw new AssertionError("encryption or pgp/inline is enabled, but no output part!");
            }

            mimeBuildSignedMessage(bodyPart, result);
            return;
        }

        if (cryptoStatus.isPgpInlineModeEnabled()) {
            mimeBuildInlineMessage(pgpResultTempBody);
            return;
        }

        mimeBuildEncryptedMessage(pgpResultTempBody);
    }

    private void mimeBuildSignedMessage(@NonNull BodyPart signedBodyPart, Intent result) throws MessagingException {
        if (!cryptoStatus.isSigningEnabled()) {
            throw new IllegalStateException("call to mimeBuildSignedMessage while signing isn't enabled!");
        }

        byte[] signedData = result.getByteArrayExtra(OpenPgpApi.RESULT_DETACHED_SIGNATURE);
        if (signedData == null) {
            throw new MessagingException("didn't find expected RESULT_DETACHED_SIGNATURE in api call result");
        }

        MimeMultipart multipartSigned = createMimeMultipart();
        multipartSigned.setSubType("signed");
        multipartSigned.addBodyPart(signedBodyPart);
        multipartSigned.addBodyPart(
                new MimeBodyPart(new BinaryMemoryBody(signedData, MimeUtil.ENC_7BIT),
                        "application/pgp-signature; name=\"signature.asc\""));
        MimeMessageHelper.setBody(currentProcessedMimeMessage, multipartSigned);

        String contentType = String.format(
                "multipart/signed; boundary=\"%s\";\r\n  protocol=\"application/pgp-signature\"",
                multipartSigned.getBoundary());
        if (result.hasExtra(OpenPgpApi.RESULT_SIGNATURE_MICALG)) {
            String micAlgParameter = result.getStringExtra(OpenPgpApi.RESULT_SIGNATURE_MICALG);
            contentType += String.format("; micalg=\"%s\"", micAlgParameter);
        } else {
            Timber.e("missing micalg parameter for pgp multipart/signed!");
        }
        currentProcessedMimeMessage.setHeader(MimeHeader.HEADER_CONTENT_TYPE, contentType);
    }

    private void mimeBuildEncryptedMessage(@NonNull Body encryptedBodyPart) throws MessagingException {
        if (!cryptoStatus.isEncryptionEnabled()) {
            throw new IllegalStateException("call to mimeBuildEncryptedMessage while encryption isn't enabled!");
        }

        MimeMultipart multipartEncrypted = createMimeMultipart();
        multipartEncrypted.setSubType("encrypted");
        multipartEncrypted.addBodyPart(new MimeBodyPart(new TextBody("Version: 1"), "application/pgp-encrypted"));
        MimeBodyPart encryptedPart = new MimeBodyPart(encryptedBodyPart, "application/octet-stream; name=\"encrypted.asc\"");
        encryptedPart.addHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, "inline; filename=\"encrypted.asc\"");
        multipartEncrypted.addBodyPart(encryptedPart);
        MimeMessageHelper.setBody(currentProcessedMimeMessage, multipartEncrypted);

        String contentType = String.format(
                "multipart/encrypted; boundary=\"%s\";\r\n  protocol=\"application/pgp-encrypted\"",
                multipartEncrypted.getBoundary());
        currentProcessedMimeMessage.setHeader(MimeHeader.HEADER_CONTENT_TYPE, contentType);
    }

    private void mimeBuildInlineMessage(@NonNull Body inlineBodyPart) throws MessagingException {
        if (!cryptoStatus.isPgpInlineModeEnabled()) {
            throw new IllegalStateException("call to mimeBuildInlineMessage while pgp/inline isn't enabled!");
        }

        boolean isCleartextSignature = !cryptoStatus.isEncryptionEnabled();
        if (isCleartextSignature) {
            inlineBodyPart.setEncoding(MimeUtil.ENC_QUOTED_PRINTABLE);
        }
        MimeMessageHelper.setBody(currentProcessedMimeMessage, inlineBodyPart);
    }

    public void setCryptoStatus(ComposeCryptoStatus cryptoStatus) {
        this.cryptoStatus = cryptoStatus;
    }
}
