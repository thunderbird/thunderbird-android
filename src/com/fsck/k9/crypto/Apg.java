package com.fsck.k9.crypto;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.widget.Toast;

import com.fsck.k9.R;
import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.activity.MessageView;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeUtility;

public class Apg extends CryptoProvider
{
    static final long serialVersionUID = 0x21071235;
    public static final String NAME = "apg";

    private static final String mApgPackageName = "org.thialfihar.android.apg";
    private static final int mMinRequiredVersion = 16;

    public static final String AUTHORITY = "org.thialfihar.android.apg.provider";
    public static final Uri CONTENT_URI_SECRET_KEY_RING_BY_KEY_ID =
        Uri.parse("content://" + AUTHORITY + "/key_rings/secret/key_id/");
    public static final Uri CONTENT_URI_SECRET_KEY_RING_BY_EMAILS =
        Uri.parse("content://" + AUTHORITY + "/key_rings/secret/emails/");

    public static final Uri CONTENT_URI_PUBLIC_KEY_RING_BY_KEY_ID =
        Uri.parse("content://" + AUTHORITY + "/key_rings/public/key_id/");
    public static final Uri CONTENT_URI_PUBLIC_KEY_RING_BY_EMAILS =
        Uri.parse("content://" + AUTHORITY + "/key_rings/public/emails/");

    public static class Intent
    {
        public static final String DECRYPT = "org.thialfihar.android.apg.intent.DECRYPT";
        public static final String ENCRYPT = "org.thialfihar.android.apg.intent.ENCRYPT";
        public static final String DECRYPT_FILE = "org.thialfihar.android.apg.intent.DECRYPT_FILE";
        public static final String ENCRYPT_FILE = "org.thialfihar.android.apg.intent.ENCRYPT_FILE";
        public static final String DECRYPT_AND_RETURN = "org.thialfihar.android.apg.intent.DECRYPT_AND_RETURN";
        public static final String ENCRYPT_AND_RETURN = "org.thialfihar.android.apg.intent.ENCRYPT_AND_RETURN";
        public static final String SELECT_PUBLIC_KEYS = "org.thialfihar.android.apg.intent.SELECT_PUBLIC_KEYS";
        public static final String SELECT_SECRET_KEY = "org.thialfihar.android.apg.intent.SELECT_SECRET_KEY";
    }

    public static final String EXTRA_TEXT = "text";
    public static final String EXTRA_DATA = "data";
    public static final String EXTRA_STATUS = "status";
    public static final String EXTRA_ERROR = "error";
    public static final String EXTRA_DECRYPTED_MESSAGE = "decryptedMessage";
    public static final String EXTRA_ENCRYPTED_MESSAGE = "decryptedMessage";
    public static final String EXTRA_SIGNATURE = "signature";
    public static final String EXTRA_SIGNATURE_KEY_ID = "signatureKeyId";
    public static final String EXTRA_SIGNATURE_USER_ID = "signatureUserId";
    public static final String EXTRA_SIGNATURE_SUCCESS = "signatureSuccess";
    public static final String EXTRA_SIGNATURE_UNKNOWN = "signatureUnknown";
    public static final String EXTRA_USER_ID = "userId";
    public static final String EXTRA_KEY_ID = "keyId";
    public static final String EXTRA_REPLY_TO = "replyTo";
    public static final String EXTRA_SEND_TO = "sendTo";
    public static final String EXTRA_SUBJECT = "subject";
    public static final String EXTRA_ENCRYPTION_KEY_IDS = "encryptionKeyIds";
    public static final String EXTRA_SELECTION = "selection";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_PROGRESS = "progress";
    public static final String EXTRA_MAX = "max";
    public static final String EXTRA_ACCOUNT = "account";

    public static final int DECRYPT_MESSAGE = 0x21070001;
    public static final int ENCRYPT_MESSAGE = 0x21070002;
    public static final int SELECT_PUBLIC_KEYS = 0x21070003;
    public static final int SELECT_SECRET_KEY = 0x21070004;

    public static Pattern PGP_MESSAGE =
        Pattern.compile(".*?(-----BEGIN PGP MESSAGE-----.*?-----END PGP MESSAGE-----).*",
                        Pattern.DOTALL);

    public static Pattern PGP_SIGNED_MESSAGE =
        Pattern.compile(".*?(-----BEGIN PGP SIGNED MESSAGE-----.*?-----BEGIN PGP SIGNATURE-----.*?-----END PGP SIGNATURE-----).*",
                        Pattern.DOTALL);

    /**
     * Check whether APG is installed and at a high enough version.
     *
     * @param context
     * @return whether a suitable version of APG was found
     */
    @Override
    public boolean isAvailable(Context context)
    {
        try
        {
            PackageInfo pi = context.getPackageManager().getPackageInfo(mApgPackageName, 0);
            if (pi.versionCode >= mMinRequiredVersion)
            {
                return true;
            }
            else
            {
                Toast.makeText(context,
                               R.string.error_apg_version_not_supported, Toast.LENGTH_SHORT).show();
            }
        }
        catch (NameNotFoundException e)
        {
            // not found
        }

        return false;
    }

    /**
     * Select the signature key.
     *
     * @param activity
     * @return success or failure
     */
    @Override
    public boolean selectSecretKey(Activity activity)
    {
        android.content.Intent intent = new android.content.Intent(Intent.SELECT_SECRET_KEY);
        try
        {
            activity.startActivityForResult(intent, Apg.SELECT_SECRET_KEY);
            return true;
        }
        catch (ActivityNotFoundException e)
        {
            Toast.makeText(activity,
                           R.string.error_activity_not_found,
                           Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * Select encryption keys.
     *
     * @param activity
     * @param emails The emails that should be used for preselection.
     * @return success or failure
     */
    @Override
    public boolean selectEncryptionKeys(Activity activity, String emails)
    {
        android.content.Intent intent = new android.content.Intent(Apg.Intent.SELECT_PUBLIC_KEYS);
        long[] initialKeyIds = null;
        if (!hasEncryptionKeys())
        {
            Vector<Long> keyIds = new Vector<Long>();
            if (hasSignatureKey())
            {
                keyIds.add(getSignatureKeyId());
            }

            Uri contentUri = Uri.withAppendedPath(
                                 Apg.CONTENT_URI_PUBLIC_KEY_RING_BY_EMAILS,
                                 emails);
            Cursor c = activity.getContentResolver().query(contentUri,
                       new String[] { "master_key_id" },
                       null, null, null);
            if (c != null)
            {
                while (c.moveToNext())
                {
                    keyIds.add(c.getLong(0));
                }
            }

            if (c != null)
            {
                c.close();
            }

            if (keyIds.size() > 0)
            {
                initialKeyIds = new long[keyIds.size()];
                for (int i = 0, size = keyIds.size(); i < size; ++i)
                {
                    initialKeyIds[i] = keyIds.get(i);
                }
            }
        }
        else
        {
            initialKeyIds = mEncryptionKeyIds;
        }
        intent.putExtra(Apg.EXTRA_SELECTION, initialKeyIds);
        try
        {
            activity.startActivityForResult(intent, Apg.SELECT_PUBLIC_KEYS);
            return true;
        }
        catch (ActivityNotFoundException e)
        {
            Toast.makeText(activity,
                           R.string.error_activity_not_found,
                           Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * Get secret key ids based on a given email.
     *
     * @param context
     * @param email The email in question.
     * @return key ids
     */
    @Override
    public long[] getSecretKeyIdsFromEmail(Context context, String email)
    {
        Uri contentUri = Uri.withAppendedPath(Apg.CONTENT_URI_SECRET_KEY_RING_BY_EMAILS,
                                              email);
        Cursor c = context.getContentResolver().query(contentUri,
                   new String[] { "master_key_id" },
                   null, null, null);
        long ids[] = null;
        if (c != null && c.getCount() > 0)
        {
            ids = new long[c.getCount()];
            while (c.moveToNext())
            {
                ids[c.getPosition()] = c.getLong(0);
            }
        }

        if (c != null)
        {
            c.close();
        }

        return ids;
    }

    /**
     * Get the user id based on the key id.
     *
     * @param context
     * @param keyId
     * @return user id
     */
    @Override
    public String getUserId(Context context, long keyId)
    {
        Uri contentUri = ContentUris.withAppendedId(
                             Apg.CONTENT_URI_SECRET_KEY_RING_BY_KEY_ID,
                             keyId);
        Cursor c = context.getContentResolver().query(contentUri,
                   new String[] { "user_id" },
                   null, null, null);
        String userId = null;
        if (c != null && c.moveToFirst())
        {
            userId = c.getString(0);
        }

        if (c != null)
        {
            c.close();
        }

        if (userId == null)
        {
            userId = context.getString(R.string.unknown_signature_user_id);
        }
        return userId;
    }

    /**
     * Handle the activity results that concern us.
     *
     * @param activity
     * @param requestCode
     * @param resultCode
     * @param data
     * @return handled or not
     */
    @Override
    public boolean onActivityResult(Activity activity, int requestCode, int resultCode,
                                    android.content.Intent data)
    {
        switch (requestCode)
        {
            case Apg.SELECT_SECRET_KEY:
                if (resultCode != Activity.RESULT_OK || data == null)
                {
                    break;
                }
                setSignatureKeyId(data.getLongExtra(Apg.EXTRA_KEY_ID, 0));
                ((MessageCompose) activity).updateEncryptLayout();
                break;

            case Apg.SELECT_PUBLIC_KEYS:
                if (resultCode != Activity.RESULT_OK || data == null)
                {
                    break;
                }
                mEncryptionKeyIds = data.getLongArrayExtra(Apg.EXTRA_SELECTION);
                ((MessageCompose) activity).updateEncryptLayout();
                break;

            case Apg.ENCRYPT_MESSAGE:
                if (resultCode != Activity.RESULT_OK || data == null)
                {
                    break;
                }
                mEncryptedData = data.getStringExtra(Apg.EXTRA_ENCRYPTED_MESSAGE);
                if (mEncryptedData != null)
                {
                    ((MessageCompose) activity).onEncryptDone();
                }
                break;

            case Apg.DECRYPT_MESSAGE:
                if (resultCode != Activity.RESULT_OK || data == null)
                {
                    break;
                }

                mSignatureUserId = data.getStringExtra(Apg.EXTRA_SIGNATURE_USER_ID);
                mSignatureKeyId = data.getLongExtra(Apg.EXTRA_SIGNATURE_KEY_ID, 0);
                mSignatureSuccess = data.getBooleanExtra(Apg.EXTRA_SIGNATURE_SUCCESS, false);
                mSignatureUnknown = data.getBooleanExtra(Apg.EXTRA_SIGNATURE_UNKNOWN, false);

                mDecryptedData = data.getStringExtra(Apg.EXTRA_DECRYPTED_MESSAGE);
                ((MessageView) activity).onDecryptDone();

                break;

            default:
                return false;
        }

        return true;
    }

    /**
     * Start the encrypt activity.
     *
     * @param activity
     * @param data
     * @return success or failure
     */
    @Override
    public boolean encrypt(Activity activity, String data)
    {
        android.content.Intent intent = new android.content.Intent(Intent.ENCRYPT_AND_RETURN);
        intent.setType("text/plain");
        intent.putExtra(Apg.EXTRA_TEXT, data);
        intent.putExtra(Apg.EXTRA_ENCRYPTION_KEY_IDS, mEncryptionKeyIds);
        intent.putExtra(Apg.EXTRA_SIGNATURE_KEY_ID, mSignatureKeyId);
        try
        {
            activity.startActivityForResult(intent, Apg.ENCRYPT_MESSAGE);
            return true;
        }
        catch (ActivityNotFoundException e)
        {
            Toast.makeText(activity,
                           R.string.error_activity_not_found,
                           Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * Start the decrypt activity.
     *
     * @param activity
     * @param data
     * @return success or failure
     */
    @Override
    public boolean decrypt(Activity activity, String data)
    {
        android.content.Intent intent = new android.content.Intent(Apg.Intent.DECRYPT_AND_RETURN);
        intent.setType("text/plain");
        if (data == null)
        {
            return false;
        }
        try
        {
            intent.putExtra(EXTRA_TEXT, data);
            activity.startActivityForResult(intent, Apg.DECRYPT_MESSAGE);
            return true;
        }
        catch (ActivityNotFoundException e)
        {
            Toast.makeText(activity,
                           R.string.error_activity_not_found,
                           Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    public boolean isEncrypted(Message message)
    {
        String data = null;
        try
        {
            Part part = MimeUtility.findFirstPartByMimeType(message, "text/plain");
            if (part == null)
            {
                part = MimeUtility.findFirstPartByMimeType(message, "text/html");
            }
            if (part != null)
            {
                data = MimeUtility.getTextFromPart(part);
            }
        }
        catch (MessagingException e)
        {
            // guess not...
            // TODO: maybe log this?
        }

        if (data == null)
        {
            return false;
        }

        Matcher matcher = PGP_MESSAGE.matcher(data);
        return matcher.matches();
    }

    public boolean isSigned(Message message)
    {
        String data = null;
        try
        {
            Part part = MimeUtility.findFirstPartByMimeType(message, "text/plain");
            if (part == null)
            {
                part = MimeUtility.findFirstPartByMimeType(message, "text/html");
            }
            if (part != null)
            {
                data = MimeUtility.getTextFromPart(part);
            }
        }
        catch (MessagingException e)
        {
            // guess not...
            // TODO: maybe log this?
        }

        if (data == null)
        {
            return false;
        }

        Matcher matcher = PGP_SIGNED_MESSAGE.matcher(data);
        return matcher.matches();
    }

    /**
     * Get the name of the provider.
     *
     * @return provider name
     */
    @Override
    public String getName()
    {
        return NAME;
    }
}
