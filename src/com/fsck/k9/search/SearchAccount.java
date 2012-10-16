package com.fsck.k9.search;

import java.util.UUID;

import android.content.Context;

import com.fsck.k9.BaseAccount;
import com.fsck.k9.R;

/**
 * This class is basically a wrapper around a LocalSearch. It allows to expose it as
 * an account. This is a meta-account containing all the e-mail that matches the search.
 */
public class SearchAccount implements BaseAccount {

    // create the all messages search ( all accounts is default when none specified )
    public static SearchAccount createAllMessagesAccount(Context context) {
        String name = context.getString(R.string.search_all_messages_title);
        LocalSearch tmpSearch = new LocalSearch(name);
        return new SearchAccount(tmpSearch, name,
                context.getString(R.string.search_all_messages_detail));
    }


    // create the unified inbox meta account ( all accounts is default when none specified )
    public static SearchAccount createUnifiedInboxAccount(Context context) {
        String name = context.getString(R.string.integrated_inbox_title);
        LocalSearch tmpSearch = new LocalSearch(name);
        tmpSearch.addAllowedFolder(SearchSpecification.GENERIC_INBOX_NAME);
        return new SearchAccount(tmpSearch, name,
                context.getString(R.string.integrated_inbox_detail));
    }

    private String mEmail = null;
    private String mDescription = null;
    private LocalSearch mSearch = null;
    private String mFakeUuid = null;

    public SearchAccount(LocalSearch search, String description, String email) throws IllegalArgumentException{
        if (search == null) {
            throw new IllegalArgumentException("Provided LocalSearch was null");
        }

        this.mSearch = search;
        this.mDescription = description;
        this.mEmail = email;
    }

    @Override
    public synchronized String getEmail() {
        return mEmail;
    }

    @Override
    public synchronized void setEmail(String email) {
        this.mEmail = email;
    }

    @Override
    public String getDescription() {
        return mDescription;
    }

    @Override
    public void setDescription(String description) {
        this.mDescription = description;
    }

    public LocalSearch getRelatedSearch() {
        return mSearch;
    }

    @Override
    /*
     * This will only be used when accessed as an Account. If that
     * is the case we don't want to return the uuid of a real account since
     * this is posing as a fake meta-account. If this object is accesed as
     * a Search then methods from LocalSearch will be called which do handle
     * things nice.
     */
    public String getUuid() {
        if (mFakeUuid == null){
            mFakeUuid = UUID.randomUUID().toString();
        }
        return mFakeUuid;
    }
}
