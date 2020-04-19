package com.fsck.k9.search;


import com.fsck.k9.BaseAccount;
import com.fsck.k9.CoreResourceProvider;
import com.fsck.k9.DI;
import com.fsck.k9.search.SearchSpecification.Attribute;
import com.fsck.k9.search.SearchSpecification.SearchField;


/**
 * This class is basically a wrapper around a LocalSearch. It allows to expose it as
 * an account. This is a meta-account containing all the email that matches the search.
 */
public class SearchAccount implements BaseAccount {
    public static final String UNIFIED_INBOX = "unified_inbox";


    // create the unified inbox meta account ( all accounts is default when none specified )
    public static SearchAccount createUnifiedInboxAccount() {
        CoreResourceProvider resourceProvider = DI.get(CoreResourceProvider.class);
        String name = resourceProvider.searchUnifiedInboxTitle();
        LocalSearch tmpSearch = new LocalSearch(name);
        tmpSearch.setId(UNIFIED_INBOX);
        tmpSearch.and(SearchField.INTEGRATE, "1", Attribute.EQUALS);
        return new SearchAccount(UNIFIED_INBOX, tmpSearch, name, resourceProvider.searchUnifiedInboxDetail());
    }

    private String mId;
    private String mEmail;
    private String mDescription;
    private LocalSearch mSearch;

    public SearchAccount(String id, LocalSearch search, String description, String email)
            throws IllegalArgumentException {

        if (search == null) {
            throw new IllegalArgumentException("Provided LocalSearch was null");
        }

        mId = id;
        mSearch = search;
        mDescription = description;
        mEmail = email;
    }

    public String getId() {
        return mId;
    }

    @Override
    public synchronized String getEmail() {
        return mEmail;
    }

    public synchronized void setEmail(String email) {
        this.mEmail = email;
    }

    @Override
    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        this.mDescription = description;
    }

    public LocalSearch getRelatedSearch() {
        return mSearch;
    }

    /**
     * Returns the ID of this {@code SearchAccount} instance.
     *
     * <p>
     * This isn't really a UUID. But since we don't expose this value to other apps and we only
     * use the account UUID as opaque string (e.g. as key in a {@code Map}) we're fine.<br>
     * Using a constant string is necessary to identify the same search account even when the
     * corresponding {@link SearchAccount} object has been recreated.
     * </p>
     */
    @Override
    public String getUuid() {
        return mId;
    }
}
