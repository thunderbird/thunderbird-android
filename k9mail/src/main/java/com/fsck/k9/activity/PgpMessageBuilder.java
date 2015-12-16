package com.fsck.k9.activity;


import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.fsck.k9.Account;
import com.fsck.k9.activity.RecipientPresenter.CryptoMode;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.BinaryTempFileBody;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMessageHelper;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.mailstore.BinaryMemoryBody;
import com.fsck.k9.message.MessageBuilder;
import org.apache.james.mime4j.util.MimeUtil;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpApi.OpenPgpDataSource;


public class PgpMessageBuilder extends MessageBuilder {

    public static final int REQUEST_SIGN_INTERACTION = 1;
    public static final int REQUEST_ENCRYPT_INTERACTION = 2;

    private final OpenPgpApi openPgpApi;

    private MimeMessage currentProcessedMimeMessage;
    private Intent currentOpenPgpIntent;
    private Callback currentCallback;
    private long signingKeyId = Account.NO_OPENPGP_KEY;
    private CryptoMode cryptoMode;

    public PgpMessageBuilder(Context context, OpenPgpApi openPgpApi) {
        super(context);
        this.openPgpApi = openPgpApi;
    }

    enum State {
        IDLE, START,
        OPENPGP_SIGN, OPENPGP_SIGN_UI, OPENPGP_SIGN_OK,
        OPENPGP_ENCRYPT, OPENPGP_ENCRYPT_UI, OPENPGP_ENCRYPT_OK;

        public boolean isBreakState() {
            return this == OPENPGP_SIGN_UI || this == OPENPGP_ENCRYPT_UI;
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
    public void buildAsync(Callback callback) {
        if (currentState != State.IDLE) {
            throw new IllegalStateException("bad state!");
        }

        try {
            currentProcessedMimeMessage = build();
        } catch (MessagingException me) {
            callback.onException(me);
            return;
        }

        currentCallback = callback;
        currentState = State.START;
        continueAsyncBuildMessage();
    }

    private void continueAsyncBuildMessage() {
        if (currentState != State.START && !currentState.isReentrantState()) {
            throw new IllegalStateException("bad state!");
        }

        try {
            maybeProcessMessageWithPgpMimeCrypto();
        } catch (MessagingException me) {
            currentCallback.onException(me);
            return;
        }

        if (currentState.isBreakState()) {
            return;
        }

        currentCallback.onSuccess(currentProcessedMimeMessage);
    }

    private void maybeProcessMessageWithPgpMimeCrypto() throws MessagingException {
        if (currentState == State.OPENPGP_SIGN || currentState == State.OPENPGP_ENCRYPT) {
            Intent openPgpIntent = currentOpenPgpIntent;
            currentOpenPgpIntent = null;
            mimeIntentLaunch(openPgpIntent);
        }

        maybeStartPgpSigning();

        if (currentState.isBreakState()) {
            return;
        }

        maybeStartPgpEncryption();
    }

    private void maybeStartPgpEncryption() throws MessagingException {
        if (cryptoMode == CryptoMode.SIGN_ONLY) {
            return;
        }

        Intent encryptIntent = new Intent(OpenPgpApi.ACTION_ENCRYPT);
        {
            ArrayList<String> recipientAddresses = new ArrayList<String>();
            for (Address address : currentProcessedMimeMessage.getRecipients(RecipientType.TO)) {
                recipientAddresses.add(address.getAddress());
            }
            for (Address address : currentProcessedMimeMessage.getRecipients(RecipientType.CC)) {
                recipientAddresses.add(address.getAddress());
            }
            for (Address address : currentProcessedMimeMessage.getRecipients(RecipientType.BCC)) {
                recipientAddresses.add(address.getAddress());
            }
            String[] addressStrings = recipientAddresses.toArray(new String[recipientAddresses.size()]);

            encryptIntent.putExtra(OpenPgpApi.EXTRA_USER_IDS, addressStrings);
        }
        encryptIntent.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);

        currentState = State.OPENPGP_ENCRYPT;
        mimeIntentLaunch(encryptIntent);
    }

    private void maybeStartPgpSigning() throws MessagingException {
        boolean isSignEnabled = signingKeyId != Account.NO_OPENPGP_KEY;
        boolean alreadySigned = currentState.isSignOk();
        if (!isSignEnabled || alreadySigned) {
            return;
        }

        Intent signIntent = new Intent(OpenPgpApi.ACTION_DETACHED_SIGN);
        signIntent.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, signingKeyId);

        currentState = State.OPENPGP_SIGN;
        mimeIntentLaunch(signIntent);
    }

    /**
     * Process the message in order to encapsulate it in a multipart/signed message.
     * @see <a href="http://tools.ietf.org/html/rfc2015">RFC-2015</a>
     * @throws MessagingException
     */
    private void mimeIntentLaunch(Intent openPgpIntent) throws MessagingException {

        /*
         * Once set to true, text messages will be sign safe (RFC-3156 §3) until K9Mail is stopped.
         * This "global parameter" is made to avoid a double generation (regular/sign safe) on the
         * whole Part/Body hierarchy and still limit the scope of this extra encoding to pgp/mime users
         * Downside it that even unsigned messages will contain extra-encoding once a message has
         * been signed. But it will be transparent if the recipient decodes properly quoted-printable.
         */
//        TextBody.setSignSafe(true);

        final MimeBodyPart bodyPart = currentProcessedMimeMessage.toBodyPart();

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
                resetToIdleState();
                OpenPgpError error = result.getParcelableExtra(OpenPgpApi.RESULT_ERROR);
                currentCallback.onException(new MessagingException(error.getMessage()));
                return;

            default:
                throw new IllegalStateException("unreachable code segment reached - this is a bug");
        }
    }

    private void launchUserInteraction(Intent result) {
        PendingIntent pendingIntent = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);

        if (currentState == State.OPENPGP_ENCRYPT) {
            currentState = State.OPENPGP_ENCRYPT_UI;
            currentCallback.onReturnPendingIntent(pendingIntent, REQUEST_ENCRYPT_INTERACTION);
        } else if (currentState == State.OPENPGP_SIGN) {
            currentState = State.OPENPGP_SIGN_UI;
            currentCallback.onReturnPendingIntent(pendingIntent, REQUEST_SIGN_INTERACTION);
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
        currentProcessedMimeMessage.addContentTypeParameter("protocol", "\"application/pgp-signature\"");

        currentState = State.OPENPGP_SIGN_OK;
    }

    @SuppressWarnings("UnusedParameters")
    private void mimeBuildEncryptedMessage(Body encryptedBodyPart, Intent result) throws MessagingException {
        MimeMultipart multipartEncrypted = new MimeMultipart();
        multipartEncrypted.setSubType("encrypted");

        // The getBytes() here should strictly use US-ASCII encoding. However, we know that for this static
        // string the byte data is equivalent, and avoid an UnsupportedEncodingException by using this way.
        multipartEncrypted.addBodyPart(new MimeBodyPart(new TextBody("Version: 1"), "application/pgp-encrypted"));
        multipartEncrypted.addBodyPart(new MimeBodyPart(encryptedBodyPart, "application/octet-stream"));
        MimeMessageHelper.setBody(currentProcessedMimeMessage, multipartEncrypted);
        currentProcessedMimeMessage.addContentTypeParameter("protocol", "\"application/pgp-encrypted\"");

        currentState = State.OPENPGP_ENCRYPT_OK;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_CANCELED) {
            resetToIdleState();
            return;
        }
        if (resultCode != Activity.RESULT_OK) {
            throw new IllegalStateException("error during interaction!");
        }

        if (requestCode == REQUEST_SIGN_INTERACTION && currentState == State.OPENPGP_SIGN_UI) {
            currentOpenPgpIntent = data;
            currentState = State.OPENPGP_SIGN;
            continueAsyncBuildMessage();
        } else if (requestCode == REQUEST_ENCRYPT_INTERACTION && currentState == State.OPENPGP_ENCRYPT_UI) {
            currentOpenPgpIntent = data;
            currentState = State.OPENPGP_ENCRYPT;
            continueAsyncBuildMessage();
        } else {
            throw new IllegalStateException("illegal state!");
        }
    }

    private void resetToIdleState() {
        currentOpenPgpIntent = null;
        currentProcessedMimeMessage = null;
        currentState = State.IDLE;
    }

    public void setSigningKeyId(long signingKeyId) {
        this.signingKeyId = signingKeyId;
    }

    public void setCryptoMode(CryptoMode cryptoMode) {
        this.cryptoMode = cryptoMode;
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
