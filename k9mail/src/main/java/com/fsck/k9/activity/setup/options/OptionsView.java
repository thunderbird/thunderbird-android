package com.fsck.k9.activity.setup.options;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

import com.fsck.k9.Account;
import com.fsck.k9.R;
import com.fsck.k9.activity.setup.AbstractAccountSetup;
import com.fsck.k9.activity.setup.AccountSetupView;
import com.fsck.k9.activity.setup.SpinnerOption;
import com.fsck.k9.activity.setup.options.OptionsContract.Presenter;
import com.fsck.k9.mail.Store;

import timber.log.Timber;


public class OptionsView extends AccountSetupView implements OptionsContract.View,
        View.OnClickListener {

    private Spinner checkFrequencyView;

    private Spinner displayCountView;

    private CheckBox notifyView;
    private CheckBox notifySyncView;
    private CheckBox pushEnable;

    private Presenter presenter;

    public OptionsView(AbstractAccountSetup activity) {
        super(activity);
    }

    @Override
    public void start() {
        checkFrequencyView = (Spinner) activity.findViewById(R.id.account_check_frequency);
        displayCountView = (Spinner) activity.findViewById(R.id.account_display_count);
        notifyView = (CheckBox) activity.findViewById(R.id.account_notify);
        notifySyncView = (CheckBox) activity.findViewById(R.id.account_notify_sync);
        pushEnable = (CheckBox) activity.findViewById(R.id.account_enable_push);

        activity.findViewById(R.id.next).setOnClickListener(this);

        SpinnerOption checkFrequencies[] = {
                new SpinnerOption(-1,
                        getString(R.string.account_setup_options_mail_check_frequency_never)),
                new SpinnerOption(1,
                        getString(R.string.account_setup_options_mail_check_frequency_1min)),
                new SpinnerOption(5,
                        getString(R.string.account_setup_options_mail_check_frequency_5min)),
                new SpinnerOption(10,
                        getString(R.string.account_setup_options_mail_check_frequency_10min)),
                new SpinnerOption(15,
                        getString(R.string.account_setup_options_mail_check_frequency_15min)),
                new SpinnerOption(30,
                        getString(R.string.account_setup_options_mail_check_frequency_30min)),
                new SpinnerOption(60,
                        getString(R.string.account_setup_options_mail_check_frequency_1hour)),
                new SpinnerOption(120,
                        getString(R.string.account_setup_options_mail_check_frequency_2hour)),
                new SpinnerOption(180,
                        getString(R.string.account_setup_options_mail_check_frequency_3hour)),
                new SpinnerOption(360,
                        getString(R.string.account_setup_options_mail_check_frequency_6hour)),
                new SpinnerOption(720,
                        getString(R.string.account_setup_options_mail_check_frequency_12hour)),
                new SpinnerOption(1440,
                        getString(R.string.account_setup_options_mail_check_frequency_24hour)),

        };

        ArrayAdapter<SpinnerOption> checkFrequenciesAdapter = new ArrayAdapter<SpinnerOption>(activity,
                android.R.layout.simple_spinner_item, checkFrequencies);
        checkFrequenciesAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        checkFrequencyView.setAdapter(checkFrequenciesAdapter);

        SpinnerOption displayCounts[] = {
                new SpinnerOption(10, getString(R.string.account_setup_options_mail_display_count_10)),
                new SpinnerOption(25, getString(R.string.account_setup_options_mail_display_count_25)),
                new SpinnerOption(50, getString(R.string.account_setup_options_mail_display_count_50)),
                new SpinnerOption(100, getString(R.string.account_setup_options_mail_display_count_100)),
                new SpinnerOption(250, getString(R.string.account_setup_options_mail_display_count_250)),
                new SpinnerOption(500, getString(R.string.account_setup_options_mail_display_count_500)),
                new SpinnerOption(1000, getString(R.string.account_setup_options_mail_display_count_1000)),
        };

        ArrayAdapter<SpinnerOption> displayCountsAdapter = new ArrayAdapter<SpinnerOption>(activity,
                android.R.layout.simple_spinner_item, displayCounts);
        displayCountsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        displayCountView.setAdapter(displayCountsAdapter);

        Account account = state.getAccount();

        notifyView.setChecked(account.isNotifyNewMail());
        notifySyncView.setChecked(account.isShowOngoing());
        SpinnerOption.setSpinnerOptionValue(checkFrequencyView, account
                .getAutomaticCheckIntervalMinutes());
        SpinnerOption.setSpinnerOptionValue(displayCountView, account
                .getDisplayCount());


        boolean isPushCapable = false;
        try {
            Store store = account.getRemoteStore();
            isPushCapable = store.isPushCapable();
        } catch (Exception e) {
            Timber.e(e, "Could not get remote store");
        }


        if (!isPushCapable) {
            pushEnable.setVisibility(View.GONE);
        } else {
            pushEnable.setChecked(true);
        }


        presenter = new OptionsPresenter(this, state);
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.next:
            presenter.onNextButtonClicked(notifyView.isChecked(), notifySyncView.isChecked(),
                    (int)((SpinnerOption) checkFrequencyView .getSelectedItem()).value,
                    (int)((SpinnerOption) displayCountView .getSelectedItem()).value,
                    pushEnable.isChecked());
            break;
        }
    }

    @Override
    public void next() {
        activity.goToAccountNames();
    }
}
