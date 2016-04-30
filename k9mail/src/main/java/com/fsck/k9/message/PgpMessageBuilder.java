package com.fsck.k9.message;


import java.io.IOException;
import java.io.OutputStream;

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
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpApi.OpenPgpDataSource;


public class PgpMessageBuilder extends MessageBuilder {

    public static final int REQUEST_SIGN_INTERACTION = 1;
    public static final int REQUEST_ENCRYPT_INTERACTION = 2;

    private OpenPgpApi openPgpApi;

    private MimeMessage currentProcessedMimeMessage;
    private ComposeCryptoStatus cryptoStatus;

    public PgpMessageBuilder(Context context) {
        super(context);
    }

    public void setOpenPgpApi(OpenPgpApi openPgpApi) {
        this.openPgpApi = openPgpApi;
    }

    /** This class keeps track of its internal state explicitly. */
    private enum State {
        IDLE, START, FAILURE,
        OPENPGP_SIGN, OPENPGP_SIGN_UI, OPENPGP_SIGN_OK,
        OPENPGP_ENCRYPT, OPENPGP_ENCRYPT_UI, OPENPGP_ENCRYPT_OK;

        public boolean isBreakState() {
            return this == OPENPGP_SIGN_UI || this == OPENPGP_ENCRYPT_UI || this == FAILURE;
        }

        public boolean isReentrantState() {
            return this == OPENPGP_SIGN || this == OPENPGP_ENCRYPT;
        }

        public boolean isSignOk() {
            return this == OPENPGP_SIGN_OK || this == OPENPGP_ENCRYPT
                    || this == OPENPGP_ENCRYPT_UI || this == OPENPGP_ENCRYPT_OK;
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
        if (requestCode == REQUEST_SIGN_INTERACTION && currentState == State.OPENPGP_SIGN_UI) {
            currentState = State.OPENPGP_SIGN;
            startOrContinueBuildMessage(userInteractionResult);
        } else if (requestCode == REQUEST_ENCRYPT_INTERACTION && currentState == State.OPENPGP_ENCRYPT_UI) {
            currentState = State.OPENPGP_ENCRYPT;
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
        boolean reenterOperation = currentState == State.OPENPGP_ENCRYPT;
        if (reenterOperation) {
            mimeIntentLaunch(userInteractionResult);
            return;
        }

        if (!cryptoStatus.isEncryptionEnabled()) {
            return;
        }

        Intent encryptIntent = new Intent(OpenPgpApi.ACTION_ENCRYPT);
        encryptIntent.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);

        long[] encryptKeyIds = cryptoStatus.getEncryptKeyIds();
        if (encryptKeyIds != null) {
            encryptIntent.putExtra(OpenPgpApi.EXTRA_KEY_IDS, encryptKeyIds);
        }

        if(!isDraft()) {
            String[] encryptRecipientAddresses = cryptoStatus.getRecipientAddresses();
            boolean hasRecipientAddresses = encryptRecipientAddresses != null && encryptRecipientAddresses.length > 0;
            if (!hasRecipientAddresses) {
                // TODO safeguard here once this is better handled by the caller?
                // throw new MessagingException("Encryption is enabled, but no encryption key specified!");
                return;
            }
            encryptIntent.putExtra(OpenPgpApi.EXTRA_USER_IDS, encryptRecipientAddresses);
            encryptIntent.putExtra(OpenPgpApi.EXTRA_ENCRYPT_OPPORTUNISTIC, cryptoStatus.isEncryptionOpportunistic());
        }

        currentState = State.OPENPGP_ENCRYPT;
        mimeIntentLaunch(encryptIntent);
    }

    private void startOrContinueSigningIfRequested(Intent userInteractionResult) throws MessagingException {
        boolean reenterOperation = currentState == State.OPENPGP_SIGN;
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

        Intent signIntent = new Intent(OpenPgpApi.ACTION_DETACHED_SIGN);
        signIntent.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, cryptoStatus.getSigningKeyId());

        currentState = State.OPENPGP_SIGN;
        mimeIntentLaunch(signIntent);
    }

    /** This method executes the given Intent with the OpenPGP Api. It will pass the
     * entire current message as input. On success, either mimeBuildSignedMessage() or
     * mimeBuildEncryptedMessage() will be called with their appropriate inputs. If an
     * error or PendingInput is returned, this will be passed as a result to the
     * operation.
     */
    private void mimeIntentLaunch(Intent openPgpIntent) throws MessagingException {
        final MimeBodyPart bodyPart = currentProcessedMimeMessage.toBodyPart();

        String[] contentType = currentProcessedMimeMessage.getHeader(MimeHeader.HEADER_CONTENT_TYPE);
        if (contentType.length > 0) {
            bodyPart.setHeader(MimeHeader.HEADER_CONTENT_TYPE, contentType[0]);
        }
        bodyPart.setUsing7bitTransport();

        // This data will be read in a worker thread
        OpenPgpDataSource dataSource = new OpenPgpDataSource() {
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
        if (currentState == State.OPENPGP_ENCRYPT) {
            try {
                encryptedTempBody = new BinaryTempFileBody(MimeUtil.ENC_7BIT);
                outputStream = encryptedTempBody.getOutputStream();
            } catch (IOException e) {
                throw new MessagingException("Could not allocate temp file for storage!", e);
            }
        }

        Intent result = openPgpApi.executeApi(openPgpIntent, dataSource, outputStream);

        switch (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
            case OpenPgpApi.RESULT_CODE_SUCCESS:
                if (currentState == State.OPENPGP_SIGN) {
                    mimeBuildSignedMessage(bodyPart, result);
                } else if (currentState == State.OPENPGP_ENCRYPT) {
                    mimeBuildEncryptedMessage(encryptedTempBody, result);
                } else {
                    throw new IllegalStateException("state error!");
                }
                return;

            case OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED:
                launchUserInteraction(result);
                return;

            case OpenPgpApi.RESULT_CODE_ERROR:
                OpenPgpError error = result.getParcelableExtra(OpenPgpApi.RESULT_ERROR);
                boolean isOpportunisticError = error.getErrorId() == OpenPgpError.OPPORTUNISTIC_MISSING_KEYS;
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
        PendingIntent pendingIntent = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);

        if (currentState == State.OPENPGP_ENCRYPT) {
            currentState = State.OPENPGP_ENCRYPT_UI;
            queueMessageBuildPendingIntent(pendingIntent, REQUEST_ENCRYPT_INTERACTION);
        } else if (currentState == State.OPENPGP_SIGN) {
            currentState = State.OPENPGP_SIGN_UI;
            queueMessageBuildPendingIntent(pendingIntent, REQUEST_SIGN_INTERACTION);
        } else {
            throw new IllegalStateException("illegal state!");
        }
    }

    private void mimeBuildSignedMessage(BodyPart signedBodyPart, Intent result) throws MessagingException {
        byte[] signedData = result.getByteArrayExtra(OpenPgpApi.RESULT_DETACHED_SIGNATURE);

        MimeMultipart multipartSigned = new MimeMultipart();
        multipartSigned.setSubType("signed");
        multipartSigned.addBodyPart(signedBodyPart);
        multipartSigned.addBodyPart(
                new MimeBodyPart(new BinaryMemoryBody(signedData, MimeUtil.ENC_7BIT), "application/pgp-signature"));

        MimeMessageHelper.setBody(currentProcessedMimeMessage, multipartSigned);

        String contentType = String.format(
                "multipart/signed; boundary=\"%s\";\r\n  protocol=\"application/pgp-signature\"",
                multipartSigned.getBoundary());
        if (result.hasExtra(OpenPgpApi.RESULT_SIGNATURE_MICALG)) {
            String micAlgParameter = result.getStringExtra(OpenPgpApi.RESULT_SIGNATURE_MICALG);
            contentType += String.format("; micalg=\"%s\"", micAlgParameter);
        } else {
            Log.e(K9.LOG_TAG, "missing micalg parameter for pgp multipart/signed!");
        }
        currentProcessedMimeMessage.setHeader(MimeHeader.HEADER_CONTENT_TYPE, contentType);

        currentState = State.OPENPGP_SIGN_OK;
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

        currentState = State.OPENPGP_ENCRYPT_OK;
    }

    private void skipEncryptingMessage() throws MessagingException {
        if (!cryptoStatus.isEncryptionOpportunistic()) {
            throw new AssertionError("Got opportunistic error, but encryption wasn't supposed to be opportunistic!");
        }
        currentState = State.OPENPGP_ENCRYPT_OK;
    }

    public void setCryptoStatus(ComposeCryptoStatus cryptoStatus) {
        this.cryptoStatus = cryptoStatus;
    }

    /* TODO re-add PGP/INLINE
    if (isCryptoProviderEnabled() && ! mAccount.isUsePgpMime()) {
        // OpenPGP Provider API

        // If not already encrypted but user wants to encrypt...
        if (mPgpData.getEncryptedData() == null &&
                (mEncryptCheckbox.isChecked() || mCryptoSignatureCheckbox.isChecked())) {

            String[] emailsArray = null;
            if (mEncryptCheckbox.isChecked()) {
                // get emails as array
                List<String> emails = new ArrayList<String>();

                for (Address address : recipientPresenter.getAllRecipientAddresses()) {
                    emails.add(address.getAddress());
                }
                emailsArray = emails.toArray(new String[emails.size()]);
            }
            if (mEncryptCheckbox.isChecked() && mCryptoSignatureCheckbox.isChecked()) {
                Intent intent = new Intent(OpenPgpApi.ACTION_SIGN_AND_ENCRYPT);
                intent.putExtra(OpenPgpApi.EXTRA_USER_IDS, emailsArray);
                intent.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, mAccount.getCryptoKey());
                executeOpenPgpMethod(intent);
            } else if (mCryptoSignatureCheckbox.isChecked()) {
                Intent intent = new Intent(OpenPgpApi.ACTION_SIGN);
                intent.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, mAccount.getCryptoKey());
                executeOpenPgpMethod(intent);
            } else if (mEncryptCheckbox.isChecked()) {
                Intent intent = new Intent(OpenPgpApi.ACTION_ENCRYPT);
                intent.putExtra(OpenPgpApi.EXTRA_USER_IDS, emailsArray);
                executeOpenPgpMethod(intent);
            }

            // onSend() is called again in SignEncryptCallback and with
            // encryptedData set in pgpData!
            return;
        }
    }
    */

    /* TODO re-add attach public key
    private Attachment attachedPublicKey() throws OpenPgpApiException {
        try {
            Attachment publicKey = new Attachment();
            publicKey.contentType = "application/pgp-keys";

            String keyName = "0x" +  Long.toString(mAccount.getCryptoKey(), 16).substring(8);
            publicKey.name = keyName + ".asc";
            Intent intent = new Intent(OpenPgpApi.ACTION_GET_KEY);
            intent.putExtra(OpenPgpApi.EXTRA_KEY_ID, mAccount.getCryptoKey());
            intent.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);
            OpenPgpApi api = new OpenPgpApi(this, mOpenPgpServiceConnection.getService());
            File keyTempFile = File.createTempFile("key", ".asc", getCacheDir());
            keyTempFile.deleteOnExit();
            try {
                CountingOutputStream keyFileStream = new CountingOutputStream(new BufferedOutputStream(
                        new FileOutputStream(keyTempFile)));
                Intent res = api.executeApi(intent, null, new EOLConvertingOutputStream(keyFileStream));
                if (res.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR) != OpenPgpApi.RESULT_CODE_SUCCESS
                        || keyFileStream.getByteCount() == 0) {
                    keyTempFile.delete();
                    throw new OpenPgpApiException(String.format(getString(R.string.openpgp_no_public_key_returned),
                            getString(R.string.btn_attach_key)));
                }
                publicKey.filename = keyTempFile.getAbsolutePath();
                publicKey.state = Attachment.LoadingState.COMPLETE;
                publicKey.size = keyFileStream.getByteCount();
                return publicKey;
            } catch(RuntimeException e){
                keyTempFile.delete();
                throw e;
            }
        } catch(IOException e){
            throw new RuntimeException(getString(R.string.error_cant_create_temporary_file), e);
        }
    }
     */

}
