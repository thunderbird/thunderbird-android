package com.fsck.k9.crypto;

import java.io.Serializable;

public class PgpData implements Serializable
{
    protected long mEncryptionKeyIds[] = null;
    protected long mSignatureKeyId = 0;
    protected String mSignatureUserId = null;
    protected boolean mSignatureSuccess = false;
    protected boolean mSignatureUnknown = false;
    protected String mDecryptedData = null;
    protected String mEncryptedData = null;

    public void setSignatureKeyId(long keyId)
    {
        mSignatureKeyId = keyId;
    }

    public long getSignatureKeyId()
    {
        return mSignatureKeyId;
    }

    public void setEncryptionKeys(long keyIds[])
    {
        mEncryptionKeyIds = keyIds;
    }

    public long[] getEncryptionKeys()
    {
        return mEncryptionKeyIds;
    }

    public boolean hasSignatureKey()
    {
        return mSignatureKeyId != 0;
    }

    public boolean hasEncryptionKeys()
    {
        return (mEncryptionKeyIds != null) && (mEncryptionKeyIds.length > 0);
    }

    public String getEncryptedData()
    {
        return mEncryptedData;
    }

    public void setEncryptedData(String data)
    {
        mEncryptedData = data;
    }

    public String getDecryptedData()
    {
        return mDecryptedData;
    }

    public void setDecryptedData(String data)
    {
        mDecryptedData = data;
    }

    public void setSignatureUserId(String userId)
    {
        mSignatureUserId = userId;
    }

    public String getSignatureUserId()
    {
        return mSignatureUserId;
    }

    public boolean getSignatureSuccess()
    {
        return mSignatureSuccess;
    }

    public void setSignatureSuccess(boolean success)
    {
        mSignatureSuccess = success;
    }

    public boolean getSignatureUnknown()
    {
        return mSignatureUnknown;
    }

    public void setSignatureUnknown(boolean unknown)
    {
        mSignatureUnknown = unknown;
    }
}
