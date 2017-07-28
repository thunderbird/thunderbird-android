package com.fsck.k9.activity.setup;

import android.support.annotation.StringRes;

import com.fsck.k9.activity.setup.AbstractAccountSetup.AccountState;

public abstract class AccountSetupView {
    protected AbstractAccountSetup activity;
    protected AccountState state;

    public AccountSetupView(AbstractAccountSetup activity) {
        this.activity = activity;
        this.state = activity.getState();
    }

    public abstract void start();

    protected String getString(@StringRes int resId) {
        return activity.getString(resId);
    }

    protected String getString(@StringRes int resId, Object... formatArgs) {
        return activity.getString(resId, formatArgs);
    }
}
