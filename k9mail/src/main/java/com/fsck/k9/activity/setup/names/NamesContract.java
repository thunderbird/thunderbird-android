package com.fsck.k9.activity.setup.names;


import com.fsck.k9.BasePresenter;
import com.fsck.k9.BaseView;


public interface NamesContract {
    interface View extends BaseView<Presenter> {
        void onSetupFinished();
    }

    interface Presenter extends BasePresenter {
        void onNextButtonClicked(String name, String description);
    }
}
