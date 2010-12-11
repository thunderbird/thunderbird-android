/**
 *
 */
package com.fsck.k9;

import java.io.Serializable;
import java.util.UUID;

import android.content.Context;

import com.fsck.k9.mail.Flag;

public class SearchAccount implements BaseAccount, SearchSpecification, Serializable
{
    private Flag[] mRequiredFlags = null;
    private Flag[] mForbiddenFlags = null;
    private String email = null;
    private String description = null;
    private String query = "";
    private boolean integrate = false;
    private String mUuid = UUID.randomUUID().toString();
    private boolean builtin = false;
    private String[] accountUuids = null;
    private String[] folderNames = null;

    public SearchAccount(Preferences preferences)
    {

    }
    protected synchronized void delete(Preferences preferences)
    {

    }

    public synchronized void save(Preferences preferences)
    {

    }


    public SearchAccount(Context context, boolean nintegrate, Flag[] requiredFlags, Flag[] forbiddenFlags)
    {
        mRequiredFlags = requiredFlags;
        mForbiddenFlags = forbiddenFlags;
        integrate = nintegrate;
    }

    @Override
    public synchronized String getEmail()
    {
        return email;
    }

    @Override
    public synchronized void setEmail(String email)
    {
        this.email = email;
    }

    public Flag[] getRequiredFlags()
    {
        return mRequiredFlags;
    }

    public Flag[] getForbiddenFlags()
    {
        return mForbiddenFlags;
    }

    public boolean isIntegrate()
    {
        return integrate;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getQuery()
    {
        return query;
    }

    public void setQuery(String query)
    {
        this.query = query;
    }
    public String getUuid()
    {
        return mUuid;
    }
    public void setUuid(String nUuid)
    {
        mUuid = nUuid;
    }

    public void setIntegrate(boolean integrate)
    {
        this.integrate = integrate;
    }

    public boolean isBuiltin()
    {
        return builtin;
    }

    public void setBuiltin(boolean builtin)
    {
        this.builtin = builtin;
    }
    public String[] getAccountUuids()
    {
        return accountUuids;
    }
    public void setAccountUuids(String[] accountUuids)
    {
        this.accountUuids = accountUuids;
    }
    @Override
    public String[] getFolderNames()
    {
        return folderNames;
    }
    public void setFolderNames(String[] folderNames)
    {
        this.folderNames = folderNames;
    }
}