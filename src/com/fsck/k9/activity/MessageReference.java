package com.fsck.k9.activity;

import java.io.Serializable;

public class MessageReference implements Serializable
{
    public String accountUuid;
    public String folderName;
    public String uid;

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof MessageReference == false)
        {
            return false;
        }
        MessageReference other = (MessageReference)o;
        if ((accountUuid == other.accountUuid || (accountUuid != null && accountUuid.equals(other.accountUuid)))
                && (folderName == other.folderName || (folderName != null && folderName.equals(other.folderName)))
                && (uid == other.uid || (uid != null && uid.equals(other.uid))))
        {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        final int MULTIPLIER = 31;

        int result = 1;
        result = MULTIPLIER * result + ((accountUuid == null) ? 0 : accountUuid.hashCode());
        result = MULTIPLIER * result + ((folderName == null) ? 0 : folderName.hashCode());
        result = MULTIPLIER * result + ((uid == null) ? 0 : uid.hashCode());
        return result;
    }

    @Override
    public String toString()
    {
        return "MessageReference{accountUuid = '" +
               accountUuid
               + "', folderName = '" + folderName
               + "', uid = '" + uid
               + "'}";
    }
}
