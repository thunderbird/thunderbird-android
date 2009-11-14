
package com.android.email.activity.setup;

import com.android.email.K9Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

import com.android.email.Account;
import com.android.email.Email;
import com.android.email.Preferences;
import com.android.email.R;

public class AccountSetupOptions extends K9Activity implements OnClickListener {
    private static final String EXTRA_ACCOUNT = "account";

    private static final String EXTRA_MAKE_DEFAULT = "makeDefault";

    private Spinner mCheckFrequencyView;

    private Spinner mDisplayCountView;
    
    private CheckBox mDefaultView;

    private CheckBox mNotifyView;
    private CheckBox mNotifySyncView;

    private Account mAccount;

    public static void actionOptions(Context context, Account account, boolean makeDefault) {
        Intent i = new Intent(context, AccountSetupOptions.class);
        i.putExtra(EXTRA_ACCOUNT, account);
        i.putExtra(EXTRA_MAKE_DEFAULT, makeDefault);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_setup_options);

        mCheckFrequencyView = (Spinner)findViewById(R.id.account_check_frequency);
        mDisplayCountView = (Spinner)findViewById(R.id.account_display_count);
        mDefaultView = (CheckBox)findViewById(R.id.account_default);
        mNotifyView = (CheckBox)findViewById(R.id.account_notify);
        mNotifySyncView = (CheckBox)findViewById(R.id.account_notify_sync);

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
        mCheckFrequencyView.setAdapter(checkFrequenciesAdapter);

        SpinnerOption displayCounts[] = {
            new SpinnerOption(10,
                              getString(R.string.account_setup_options_mail_display_count_10)),
            new SpinnerOption(25,
                              getString(R.string.account_setup_options_mail_display_count_25)),
            new SpinnerOption(50,
                              getString(R.string.account_setup_options_mail_display_count_50)),
            new SpinnerOption(100,
                              getString(R.string.account_setup_options_mail_display_count_100)),
        };

        ArrayAdapter<SpinnerOption> displayCountsAdapter = new ArrayAdapter<SpinnerOption>(this,
                android.R.layout.simple_spinner_item, displayCounts);
        displayCountsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mDisplayCountView.setAdapter(displayCountsAdapter);
        
        mAccount = (Account)getIntent().getSerializableExtra(EXTRA_ACCOUNT);
        boolean makeDefault = getIntent().getBooleanExtra(EXTRA_MAKE_DEFAULT, false);

        if (mAccount.equals(Preferences.getPreferences(this).getDefaultAccount()) || makeDefault) {
            mDefaultView.setChecked(true);
        }
        mNotifyView.setChecked(mAccount.isNotifyNewMail());
        mNotifySyncView.setChecked(mAccount.isShowOngoing());
        SpinnerOption.setSpinnerOptionValue(mCheckFrequencyView, mAccount
                .getAutomaticCheckIntervalMinutes());
        SpinnerOption.setSpinnerOptionValue(mDisplayCountView, mAccount
                .getDisplayCount());
    }

    private void onDone() {
        mAccount.setDescription(mAccount.getEmail());
        mAccount.setNotifyNewMail(mNotifyView.isChecked());
        mAccount.setShowOngoing(mNotifySyncView.isChecked());
        mAccount.setAutomaticCheckIntervalMinutes((Integer)((SpinnerOption)mCheckFrequencyView
                .getSelectedItem()).value);
        mAccount.setDisplayCount((Integer)((SpinnerOption)mDisplayCountView
                .getSelectedItem()).value);
        mAccount.save(Preferences.getPreferences(this));
        if (mDefaultView.isChecked()) {
            Preferences.getPreferences(this).setDefaultAccount(mAccount);
        }
        Email.setServicesEnabled(this);
        AccountSetupNames.actionSetNames(this, mAccount);
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
