package com.fsck.k9;

import java.io.Serializable;
import java.util.UUID;

import android.content.Context;

import com.fsck.k9.mail.Flag;

/**
 * This is a meta-Account that represents one or more accounts with filters on them.  The filter specification
 * is defined by {@link com.fsck.k9.activity.SearchModifier}.
 */
public class SearchAccount implements BaseAccount, SearchSpecification, Serializable {
    private static final long serialVersionUID = -4388420303235543976L;
    private Flag[] mRequiredFlags = null;
    private Flag[] mForbiddenFlags = null;
    private String email = null;
    private String description = null;
    private String query = "";
    private boolean integrate = false;
    private String mUuid = null;
    private boolean builtin = false;
    private String[] accountUuids = null;
    private String[] folderNames = null;

    public SearchAccount(Preferences preferences) {
    }

    protected synchronized void delete(Preferences preferences) {
    }

    public synchronized void save(Preferences preferences) {
    }

    public SearchAccount(Context context, boolean nintegrate, Flag[] requiredFlags, Flag[] forbiddenFlags) {
        mRequiredFlags = requiredFlags;
        mForbiddenFlags = forbiddenFlags;
        integrate = nintegrate;
    }

    @Override
    public synchronized String getEmail() {
        return email;
    }

    @Override
    public synchronized void setEmail(String email) {
        this.email = email;
    }

    public Flag[] getRequiredFlags() {
        return mRequiredFlags;
    }

    public Flag[] getForbiddenFlags() {
        return mForbiddenFlags;
    }

    public boolean isIntegrate() {
        return integrate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getUuid() {
        if (mUuid == null) {
            setUuid(UUID.randomUUID().toString());
        }
        return mUuid;
    }

    public void setUuid(String nUuid) {
        mUuid = nUuid;
    }

    public void setIntegrate(boolean integrate) {
        this.integrate = integrate;
    }

    public boolean isBuiltin() {
        return builtin;
    }

    public void setBuiltin(boolean builtin) {
        this.builtin = builtin;
    }

    public String[] getAccountUuids() {
        return accountUuids;
    }

    public void setAccountUuids(String[] accountUuids) {
        this.accountUuids = accountUuids;
    }

    @Override
    public String[] getFolderNames() {
        return folderNames;
    }

    public void setFolderNames(String[] folderNames) {
        this.folderNames = folderNames;
    }
}
