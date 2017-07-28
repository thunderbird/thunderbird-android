package com.fsck.k9.activity.setup;

import com.fsck.k9.activity.setup.AbstractAccountSetup.AccountState;

public abstract class AccountSetupView {
    protected AbstractAccountSetup activity;
    protected AccountState state;

    public AccountSetupView(AbstractAccountSetup activity) {
        this.activity = activity;
        this.state = activity.getState();
    }

    public abstract void start();
}
