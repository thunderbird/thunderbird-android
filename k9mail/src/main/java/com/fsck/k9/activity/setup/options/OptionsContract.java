package com.fsck.k9.activity.setup.options;


import com.fsck.k9.BasePresenter;
import com.fsck.k9.BaseView;

class OptionsContract {
    interface View extends BaseView<Presenter> {
        void next();
    }

    interface Presenter extends BasePresenter {
        void onNextButtonClicked(boolean isNotifyViewChecked, boolean isNotifySyncViewClicked,
                                 int checkFrequencyViewSelectedValue, int displayCountViewSelectedValue,
                                 boolean isPushEnableClicked);
    }
}
