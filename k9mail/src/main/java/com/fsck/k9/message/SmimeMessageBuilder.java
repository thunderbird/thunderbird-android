package com.fsck.k9.message;


import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.activity.compose.ComposeCryptoStatus;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.BinaryTempFileBody;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMessageHelper;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.mailstore.BinaryMemoryBody;

import org.apache.james.mime4j.util.MimeUtil;
import org.openintents.smime.util.SMimeApi;
import org.openintents.smime.util.SMimeApi.SMimeDataSource;
import org.openintents.smime.SmimeError;

import java.io.IOException;
import java.io.OutputStream;


public class SmimeMessageBuilder extends MessageBuilder {

    public static final int REQUEST_SIGN_INTERACTION = 1;
    public static final int REQUEST_ENCRYPT_INTERACTION = 2;

    private final SMimeApi smimeApi;

    private MimeMessage currentProcessedMimeMessage;
    private ComposeCryptoStatus cryptoStatus;

    public SmimeMessageBuilder(Context context, SMimeApi smimeApi) {
        super(context);
        this.smimeApi = smimeApi;
    }

    /** This class keeps track of its internal state explicitly. */
    private enum State {
        IDLE, START, FAILURE,
        SMIME_SIGN, SMIME_SIGN_UI, SMIME_SIGN_OK,
        SMIME_ENCRYPT, SMIME_ENCRYPT_UI, SMIME_ENCRYPT_OK;

        public boolean isBreakState() {
            return this == SMIME_SIGN_UI || this == SMIME_ENCRYPT_UI || this == FAILURE;
        }

        public boolean isReentrantState() {
            return this == SMIME_SIGN || this == SMIME_ENCRYPT;
        }

        public boolean isSignOk() {
            return this == SMIME_SIGN_OK || this == SMIME_ENCRYPT
                    || this == SMIME_ENCRYPT_UI || this == SMIME_ENCRYPT_OK;
        }
    }

    State currentState = State.IDLE;

    @Override
    protected void buildMessageInternal() {
        if (currentState != State.IDLE) {
            throw new IllegalStateException("internal error, a PgpMessageBuilder can only be built once!");
        }

        try {
            currentProcessedMimeMessage = build();
        } catch (MessagingException me) {
            queueMessageBuildException(me);
            return;
        }

        currentState = State.START;
        startOrContinueBuildMessage(null);
    }

    @Override
    public void buildMessageOnActivityResult(int requestCode, Intent userInteractionResult) {
        if (requestCode == REQUEST_SIGN_INTERACTION && currentState == State.SMIME_SIGN_UI) {
            currentState = State.SMIME_SIGN;
            startOrContinueBuildMessage(userInteractionResult);
        } else if (requestCode == REQUEST_ENCRYPT_INTERACTION && currentState == State.SMIME_ENCRYPT_UI) {
            currentState = State.SMIME_ENCRYPT;
            startOrContinueBuildMessage(userInteractionResult);
        } else {
            throw new IllegalStateException("illegal state!");
        }
    }

    private void startOrContinueBuildMessage(@Nullable Intent userInteractionResult) {
        if (currentState != State.START && !currentState.isReentrantState()) {
            throw new IllegalStateException("bad state!");
        }

        try {
            startOrContinueSigningIfRequested(userInteractionResult);

            if (currentState.isBreakState()) {
                return;
            }

            startOrContinueEncryptionIfRequested(userInteractionResult);

            if (currentState.isBreakState()) {
                return;
            }

            queueMessageBuildSuccess(currentProcessedMimeMessage);
        } catch (MessagingException me) {
            queueMessageBuildException(me);
        }
    }

    private void startOrContinueEncryptionIfRequested(Intent userInteractionResult) throws MessagingException {
        boolean reenterOperation = currentState == State.SMIME_ENCRYPT;
        if (reenterOperation) {
            mimeIntentLaunch(userInteractionResult);
            return;
        }

        if (!cryptoStatus.isEncryptionEnabled()) {
            return;
        }

        Intent encryptIntent = new Intent(SMimeApi.ACTION_ENCRYPT);
        //TODO: ??
//        encryptIntent.putExtra(SMimeApi.EXTRA_REQUEST_ASCII_ARMOR, true);

        long[] encryptCertificateIds = cryptoStatus.getEncryptCertificateIds();
        if (encryptCertificateIds != null) {
            encryptIntent.putExtra(SMimeApi.EXTRA_CERTIFICATE_IDS, encryptCertificateIds);
        }

        if(!isDraft()) {
            String[] encryptRecipientAddresses = cryptoStatus.getRecipientAddresses();
            boolean hasRecipientAddresses = encryptRecipientAddresses != null && encryptRecipientAddresses.length > 0;
            if (!hasRecipientAddresses) {
                // TODO safeguard here once this is better handled by the caller?
                // throw new MessagingException("Encryption is enabled, but no encryption key specified!");
                return;
            }
            encryptIntent.putExtra(SMimeApi.EXTRA_USER_IDS, encryptRecipientAddresses);
            encryptIntent.putExtra(SMimeApi.EXTRA_ENCRYPT_OPPORTUNISTIC, cryptoStatus.isEncryptionOpportunistic());
        }

        currentState = State.SMIME_ENCRYPT;
        mimeIntentLaunch(encryptIntent);
    }

    private void startOrContinueSigningIfRequested(Intent userInteractionResult) throws MessagingException {
        boolean reenterOperation = currentState == State.SMIME_SIGN;
        if (reenterOperation) {
            mimeIntentLaunch(userInteractionResult);
            return;
        }

        boolean signingDisabled = !cryptoStatus.isSigningEnabled();
        boolean alreadySigned = currentState.isSignOk();
        boolean isDraft = isDraft();
        if (signingDisabled || alreadySigned || isDraft) {
            return;
        }

        Intent signIntent = new Intent(SMimeApi.ACTION_SIGN);
        signIntent.putExtra(SMimeApi.EXTRA_SIGN_CERTIFICATE_ID, cryptoStatus.getSigningKeyId());

        currentState = State.SMIME_SIGN;
        mimeIntentLaunch(signIntent);
    }

    /** This method executes the given Intent with the OpenPGP Api. It will pass the
     * entire current message as input. On success, either mimeBuildSignedMessage() or
     * mimeBuildEncryptedMessage() will be called with their appropriate inputs. If an
     * error or PendingInput is returned, this will be passed as a result to the
     * operation.
     */
    private void mimeIntentLaunch(Intent smimeIntent) throws MessagingException {
        final MimeBodyPart bodyPart = currentProcessedMimeMessage.toBodyPart();

        String[] contentType = currentProcessedMimeMessage.getHeader(MimeHeader.HEADER_CONTENT_TYPE);
        if (contentType.length > 0) {
            bodyPart.setHeader(MimeHeader.HEADER_CONTENT_TYPE, contentType[0]);
        }
        bodyPart.setUsing7bitTransport();

        // This data will be read in a worker thread
        SMimeDataSource dataSource = new SMimeDataSource() {
            @Override
            public void writeTo(OutputStream os) throws IOException {
                try {
                    bodyPart.writeTo(os);
                } catch (MessagingException e) {
                    throw new IOException(e);
                }
            }
        };

        BinaryTempFileBody encryptedTempBody = null;
        OutputStream outputStream = null;
        if (currentState == State.SMIME_ENCRYPT) {
            try {
                encryptedTempBody = new BinaryTempFileBody(MimeUtil.ENC_7BIT);
                outputStream = encryptedTempBody.getOutputStream();
            } catch (IOException e) {
                throw new MessagingException("Could not allocate temp file for storage!", e);
            }
        }

        Intent result = smimeApi.executeApi(smimeIntent, dataSource, outputStream);

        switch (result.getIntExtra(SMimeApi.RESULT_CODE, SMimeApi.RESULT_CODE_ERROR)) {
            case SMimeApi.RESULT_CODE_SUCCESS:
                if (currentState == State.SMIME_SIGN) {
                    mimeBuildSignedMessage(bodyPart, result);
                } else if (currentState == State.SMIME_ENCRYPT) {
                    mimeBuildEncryptedMessage(encryptedTempBody, result);
                } else {
                    throw new IllegalStateException("state error!");
                }
                return;

            case SMimeApi.RESULT_CODE_USER_INTERACTION_REQUIRED:
                launchUserInteraction(result);
                return;

            case SMimeApi.RESULT_CODE_ERROR:
                SmimeError error = result.getParcelableExtra(SMimeApi.RESULT_ERROR);
                boolean isOpportunisticError = error.getErrorId() == SmimeError.OPPORTUNISTIC_MISSING_KEYS;
                if (isOpportunisticError) {
                    skipEncryptingMessage();
                    return;
                }
                throw new MessagingException(error.getMessage());

            default:
                throw new IllegalStateException("unreachable code segment reached - this is a bug");
        }
    }

    private void launchUserInteraction(Intent result) {
        PendingIntent pendingIntent = result.getParcelableExtra(SMimeApi.RESULT_INTENT);

        if (currentState == State.SMIME_ENCRYPT) {
            currentState = State.SMIME_ENCRYPT_UI;
            queueMessageBuildPendingIntent(pendingIntent, REQUEST_ENCRYPT_INTERACTION);
        } else if (currentState == State.SMIME_SIGN) {
            currentState = State.SMIME_SIGN_UI;
            queueMessageBuildPendingIntent(pendingIntent, REQUEST_SIGN_INTERACTION);
        } else {
            throw new IllegalStateException("illegal state!");
        }
    }

    private void mimeBuildSignedMessage(BodyPart signedBodyPart, Intent result) throws MessagingException {
        byte[] signedData = result.getByteArrayExtra(SMimeApi.RESULT_DETACHED_SIGNATURE);
//todo: rewrite
        /**
        MimeMultipart multipartSigned = new MimeMultipart();
        multipartSigned.setSubType("signed");
        multipartSigned.addBodyPart(signedBodyPart);
        multipartSigned.addBodyPart(
                new MimeBodyPart(new BinaryMemoryBody(signedData, MimeUtil.ENC_7BIT), "application/pgp-signature"));

        MimeMessageHelper.setBody(currentProcessedMimeMessage, multipartSigned);

        String contentType = String.format(
                "multipart/signed; boundary=\"%s\";\r\n  protocol=\"application/pgp-signature\"",
                multipartSigned.getBoundary());
        if (result.hasExtra(SMimeApi.RESULT_SIGNATURE_MICALG)) {
            String micAlgParameter = result.getStringExtra(SMimeApi.RESULT_SIGNATURE_MICALG);
            contentType += String.format("; micalg=\"%s\"", micAlgParameter);
        } else {
            Log.e(K9.LOG_TAG, "missing micalg parameter for pgp multipart/signed!");
        }
        currentProcessedMimeMessage.setHeader(MimeHeader.HEADER_CONTENT_TYPE, contentType);
**/
        currentState = State.SMIME_SIGN_OK;
    }

    @SuppressWarnings("UnusedParameters")
    private void mimeBuildEncryptedMessage(Body encryptedBodyPart, Intent result) throws MessagingException {
        MimeMultipart multipartEncrypted = new MimeMultipart();
        multipartEncrypted.setSubType("encrypted");

        multipartEncrypted.addBodyPart(new MimeBodyPart(new TextBody("Version: 1"), "application/pgp-encrypted"));
        multipartEncrypted.addBodyPart(new MimeBodyPart(encryptedBodyPart, "application/octet-stream"));
        MimeMessageHelper.setBody(currentProcessedMimeMessage, multipartEncrypted);
        String contentType = String.format(
                "multipart/encrypted; boundary=\"%s\";\r\n  protocol=\"application/pgp-encrypted\"",
                multipartEncrypted.getBoundary());
        currentProcessedMimeMessage.setHeader(MimeHeader.HEADER_CONTENT_TYPE, contentType);

        currentState = State.SMIME_ENCRYPT_OK;
    }

    private void skipEncryptingMessage() throws MessagingException {
        if (!cryptoStatus.isEncryptionOpportunistic()) {
            throw new AssertionError("Got opportunistic error, but encryption wasn't supposed to be opportunistic!");
        }
        currentState = State.SMIME_ENCRYPT_OK;
    }

    public void setCryptoStatus(ComposeCryptoStatus cryptoStatus) {
        this.cryptoStatus = cryptoStatus;
    }
}
