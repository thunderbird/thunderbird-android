package com.fsck.k9.activity.setup.names;


import com.fsck.k9.BasePresenter;
import com.fsck.k9.BaseView;


interface NamesContract {
    interface View extends BaseView<Presenter> {
        void next();
    }

    interface Presenter extends BasePresenter {
        void onNextButtonClicked(String name, String description);
    }
}
