
package com.fsck.k9.activity.setup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import timber.log.Timber;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import com.fsck.k9.*;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.mail.Store;

public class AccountSetupOptions extends K9Activity implements OnClickListener {
    private static final String EXTRA_ACCOUNT = "account";

    private static final String EXTRA_MAKE_DEFAULT = "makeDefault";

    private Spinner checkFrequencyView;

    private Spinner displayCountView;


    private CheckBox notifyView;
    private CheckBox notifySyncView;
    private CheckBox pushEnable;

    private Account account;

    public static void actionOptions(Context context, Account account, boolean makeDefault) {
        Intent i = new Intent(context, AccountSetupOptions.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        i.putExtra(EXTRA_MAKE_DEFAULT, makeDefault);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_setup_options);

        checkFrequencyView = (Spinner)findViewById(R.id.account_check_frequency);
        displayCountView = (Spinner)findViewById(R.id.account_display_count);
        notifyView = (CheckBox)findViewById(R.id.account_notify);
        notifySyncView = (CheckBox)findViewById(R.id.account_notify_sync);
        pushEnable = (CheckBox)findViewById(R.id.account_enable_push);

        findViewById(R.id.next).setOnClickListener(this);

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

        ArrayAdapter<SpinnerOption> checkFrequenciesAdapter = new ArrayAdapter<SpinnerOption>(this,
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

        ArrayAdapter<SpinnerOption> displayCountsAdapter = new ArrayAdapter<SpinnerOption>(this,
                android.R.layout.simple_spinner_item, displayCounts);
        displayCountsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        displayCountView.setAdapter(displayCountsAdapter);

        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        account = Preferences.getPreferences(this).getAccount(accountUuid);

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


    }

    private void onDone() {
        account.setDescription(account.getEmail());
        account.setNotifyNewMail(notifyView.isChecked());
        account.setShowOngoing(notifySyncView.isChecked());
        account.setAutomaticCheckIntervalMinutes((Integer)((SpinnerOption) checkFrequencyView
                .getSelectedItem()).value);
        account.setDisplayCount((Integer)((SpinnerOption) displayCountView
                                           .getSelectedItem()).value);

        if (pushEnable.isChecked()) {
            account.setFolderPushMode(Account.FolderMode.FIRST_CLASS);
        } else {
            account.setFolderPushMode(Account.FolderMode.NONE);
        }

        account.save(Preferences.getPreferences(this));
        if (account.equals(Preferences.getPreferences(this).getDefaultAccount()) ||
                getIntent().getBooleanExtra(EXTRA_MAKE_DEFAULT, false)) {
            Preferences.getPreferences(this).setDefaultAccount(account);
        }
        K9.setServicesEnabled(this);
        AccountSetupNames.actionSetNames(this, account);
        finish();
    }

    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.next:
            onDone();
            break;
        }
    }
}
