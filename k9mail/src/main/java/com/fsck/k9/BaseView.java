package com.fsck.k9;


import com.fsck.k9.activity.setup.AbstractAccountSetup;
import com.fsck.k9.activity.setup.AbstractAccountSetup.AccountState;


public interface BaseView<T> {
    void setPresenter(T presenter);

    void setActivity(AbstractAccountSetup activity);

    void start();
}
