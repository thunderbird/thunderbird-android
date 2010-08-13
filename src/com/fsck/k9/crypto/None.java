package com.fsck.k9.crypto;

import android.app.Activity;
import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.mail.Message;

/**
 * Dummy CryptoProvider for when cryptography is disabled. It is never "available" and doesn't
 * do anything.
 */
public class None extends CryptoProvider
{
    static final long serialVersionUID = 0x21071230;
    public static final String NAME = "";

    public static None createInstance(Account account)
    {
        return new None();
    }

    @Override
    public boolean isAvailable(Context context)
    {
        return false;
    }

    @Override
    public boolean selectSecretKey(Activity activity)
    {
        return false;
    }

    @Override
    public boolean selectEncryptionKeys(Activity activity, String emails)
    {
        return false;
    }

    @Override
    public long[] getSecretKeyIdsFromEmail(Context context, String email)
    {
        return null;
    }

    @Override
    public String getUserId(Context context, long keyId)
    {
        return null;
    }

    @Override
    public boolean onActivityResult(Activity activity, int requestCode, int resultCode,
                                    android.content.Intent data)
    {
        return false;
    }

    @Override
    public boolean encrypt(Activity activity, String data)
    {
        return false;
    }

    @Override
    public boolean decrypt(Activity activity, String data)
    {
        return false;
    }

    @Override
    public boolean isEncrypted(Message message)
    {
        return false;
    }

    @Override
    public boolean isSigned(Message message)
    {
        return false;
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public boolean test(Context context)
    {
        return true;
    }
}
