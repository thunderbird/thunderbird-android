package com.fsck.k9.helper;

import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.mail.MessagingException;

/**
 * Validate based on local folder status only when the given account
 * is not copy/moved allowed.
 */
public class LocalFolderValidator implements RangeValidator {

    private Account mAccount;
    private boolean isCopyMovedAllowed;

    public LocalFolderValidator() {
        isCopyMovedAllowed = true;
    }

    /**
     * Constructor
     * @param account the relevant account
     */
    public LocalFolderValidator(Account account) {
        mAccount = account;
        isCopyMovedAllowed = false;
        try {
            isCopyMovedAllowed = account.isCopyMoveCapable();
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "Failed test on account capability of move and copy actions");
        }
    }

    /**
     * Constructor
     * @param account the relevant account
     * @param forceLocalFolderTest if true strictly forces only local folders
     */
    public LocalFolderValidator(Account account, boolean forceLocalFolderTest) {
        mAccount = account;
        isCopyMovedAllowed = false;
        if (!forceLocalFolderTest) {
            try {
                isCopyMovedAllowed = account.isCopyMoveCapable();
            } catch (MessagingException e) {
                Log.e(K9.LOG_TAG, "Failed test on account capability of move and copy actions");
            }
        }

    }

    @Override
    public Boolean validateField(String name) {
        if (mAccount==null) return isCopyMovedAllowed;
        boolean isLocalFolder = false;
        try {
            isLocalFolder = mAccount.isLocalFolder(name);
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "Failed test on checking folder type");
        }
        return isCopyMovedAllowed || isLocalFolder;

    }
}
