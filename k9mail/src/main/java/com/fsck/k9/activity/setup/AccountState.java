package com.fsck.k9.activity.setup;


import com.fsck.k9.Account;


public class AccountState {
    Account account;
    String email;
    String password;
    boolean makeDefault;
    boolean editSettings;

    public static final int STEP_EMAIL_AND_PASSWORD = 0;
    public static final int STEP_AUTO_CONFIGURATION = 1;
    public static final int STEP_CHECK_INCOMING = 2;
    public static final int STEP_CHECK_OUTGOING = 3;

    public boolean isMakeDefault() {
        return makeDefault;
    }

    public void setMakeDefault(boolean makeDefault) {
        this.makeDefault = makeDefault;
    }

    public boolean isEditSettings() {
        return editSettings;
    }

    public void setEditSettings(boolean editSettings) {
        this.editSettings = editSettings;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
