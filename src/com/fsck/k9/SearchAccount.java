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
    /**
     * Create a {@code SearchAccount} instance for the Unified Inbox.
     *
     * @param context
     *         A {@link Context} instance that will be used to get localized strings and will be
     *         passed on to the {@code SearchAccount} instance.
     *
     * @return The {@link SearchAccount} instance for the Unified Inbox.
     */
    public static SearchAccount createUnifiedInboxAccount(Context context) {
        SearchAccount unifiedInbox = new SearchAccount(context, true, null, null);
        unifiedInbox.setDescription(context.getString(R.string.integrated_inbox_title));
        unifiedInbox.setEmail(context.getString(R.string.integrated_inbox_detail));
        return unifiedInbox;
    }

    /**
     * Create a {@code SearchAccount} instance for the special account "All messages".
     *
     * @param context
     *         A {@link Context} instance that will be used to get localized strings and will be
     *         passed on to the {@code SearchAccount} instance.
     *
     * @return The {@link SearchAccount} instance for the Unified Inbox.
     */
    public static SearchAccount createAllMessagesAccount(Context context) {
        SearchAccount allMessages = new SearchAccount(context, false, null, null);
        allMessages.setDescription(context.getString(R.string.search_all_messages_title));
        allMessages.setEmail(context.getString(R.string.search_all_messages_detail));
        return allMessages;
    }


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
