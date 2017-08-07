package com.fsck.k9.activity;


import java.util.StringTokenizer;

import android.support.annotation.Nullable;

import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.filter.Base64;

import static com.fsck.k9.helper.Preconditions.checkNotNull;


public class MessageReference {
    private static final char IDENTITY_VERSION_1 = '!';
    private static final String IDENTITY_SEPARATOR = ":";


    private final String accountUuid;
    private final String folderName;
    private final String uid;
    private final Flag flag;


    @Nullable
    public static MessageReference parse(String identity) {
        if (identity == null || identity.length() < 1 || identity.charAt(0) != IDENTITY_VERSION_1) {
            return null;
        }

        StringTokenizer tokens = new StringTokenizer(identity.substring(2), IDENTITY_SEPARATOR, false);
        if (tokens.countTokens() < 3) {
            return null;
        }

        String accountUuid = Base64.decode(tokens.nextToken());
        String folderName = Base64.decode(tokens.nextToken());
        String uid = Base64.decode(tokens.nextToken());

        if (!tokens.hasMoreTokens()) {
            return new MessageReference(accountUuid, folderName, uid, null);
        }

        Flag flag;
        try {
            flag = Flag.valueOf(tokens.nextToken());
        } catch (IllegalArgumentException e) {
            return null;
        }

        return new MessageReference(accountUuid, folderName, uid, flag);
    }

    public MessageReference(String accountUuid, String folderName, String uid, Flag flag) {
        this.accountUuid = checkNotNull(accountUuid);
        this.folderName = checkNotNull(folderName);
        this.uid = checkNotNull(uid);
        this.flag = flag;
    }

    public String toIdentityString() {
        StringBuilder refString = new StringBuilder();

        refString.append(IDENTITY_VERSION_1);
        refString.append(IDENTITY_SEPARATOR);
        refString.append(Base64.encode(accountUuid));
        refString.append(IDENTITY_SEPARATOR);
        refString.append(Base64.encode(folderName));
        refString.append(IDENTITY_SEPARATOR);
        refString.append(Base64.encode(uid));
        if (flag != null) {
            refString.append(IDENTITY_SEPARATOR);
            refString.append(flag.getCode());
        }

        return refString.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MessageReference)) {
            return false;
        }
        MessageReference other = (MessageReference) o;
        return equals(other.accountUuid, other.folderName, other.uid);
    }

    public boolean equals(String accountUuid, String folderName, String uid) {
        return this.accountUuid.equals(accountUuid) && this.folderName.equals(folderName) && this.uid.equals(uid);
    }

    @Override
    public int hashCode() {
        final int MULTIPLIER = 31;

        int result = 1;
        result = MULTIPLIER * result + accountUuid.hashCode();
        result = MULTIPLIER * result + folderName.hashCode();
        result = MULTIPLIER * result + uid.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "MessageReference{" +
               "accountUuid='" + accountUuid + '\'' +
               ", folderName='" + folderName + '\'' +
               ", uid='" + uid + '\'' +
               ", flag=" + flag +
               '}';
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public String getFolderName() {
        return folderName;
    }

    public String getUid() {
        return uid;
    }

    public Flag getFlag() {
        return flag;
    }

    public MessageReference withModifiedUid(String newUid) {
        return new MessageReference(accountUuid, folderName, newUid, flag);
    }

    public MessageReference withModifiedFlag(Flag newFlag) {
        return new MessageReference(accountUuid, folderName, uid, newFlag);
    }
}
