
package com.fsck.k9.activity.setup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

    private Spinner mCheckFrequencyView;

    private Spinner mFetchAmountView;


    private CheckBox mNotifyView;
    private CheckBox mNotifySyncView;
    private CheckBox mPushEnable;

    private Account mAccount;

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

        mCheckFrequencyView = (Spinner)findViewById(R.id.account_check_frequency);
        mFetchAmountView = (Spinner)findViewById(R.id.account_fetch_amount);
        mNotifyView = (CheckBox)findViewById(R.id.account_notify);
        mNotifySyncView = (CheckBox)findViewById(R.id.account_notify_sync);
        mPushEnable = (CheckBox)findViewById(R.id.account_enable_push);

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

        SpinnerOption fetchAmounts[] = {
            new SpinnerOption(10, getString(R.string.account_setup_options_mail_fetch_amount_10)),
            new SpinnerOption(25, getString(R.string.account_setup_options_mail_fetch_amount_25)),
            new SpinnerOption(50, getString(R.string.account_setup_options_mail_fetch_amount_50)),
            new SpinnerOption(100, getString(R.string.account_setup_options_mail_fetch_amount_100)),
            new SpinnerOption(250, getString(R.string.account_setup_options_mail_fetch_amount_250)),
            new SpinnerOption(500, getString(R.string.account_setup_options_mail_fetch_amount_500)),
            new SpinnerOption(1000, getString(R.string.account_setup_options_mail_fetch_amount_1000)),
        };

        ArrayAdapter<SpinnerOption> fetchAmountsAdapter = new ArrayAdapter<SpinnerOption>(this,
                android.R.layout.simple_spinner_item, fetchAmounts);
        fetchAmountsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mFetchAmountView.setAdapter(fetchAmountsAdapter);

        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);

        mNotifyView.setChecked(mAccount.isNotifyNewMail());
        mNotifySyncView.setChecked(mAccount.isShowOngoing());
        SpinnerOption.setSpinnerOptionValue(mCheckFrequencyView, mAccount
                                            .getAutomaticCheckIntervalMinutes());
        SpinnerOption.setSpinnerOptionValue(mFetchAmountView, mAccount
                                            .getFetchAmount());


        boolean isPushCapable = false;
        try {
            Store store = mAccount.getRemoteStore();
            isPushCapable = store.isPushCapable();
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Could not get remote store", e);
        }


        if (!isPushCapable) {
            mPushEnable.setVisibility(View.GONE);
        } else {
            mPushEnable.setChecked(true);
        }


    }

    private void onDone() {
        mAccount.setDescription(mAccount.getEmail());
        mAccount.setNotifyNewMail(mNotifyView.isChecked());
        mAccount.setShowOngoing(mNotifySyncView.isChecked());
        mAccount.setAutomaticCheckIntervalMinutes((Integer)((SpinnerOption)mCheckFrequencyView
                .getSelectedItem()).value);
        mAccount.setFetchAmount((Integer) ((SpinnerOption) mFetchAmountView
                .getSelectedItem()).value);

        if (mPushEnable.isChecked()) {
            mAccount.setFolderPushMode(Account.FolderMode.FIRST_CLASS);
        } else {
            mAccount.setFolderPushMode(Account.FolderMode.NONE);
        }

        mAccount.save(Preferences.getPreferences(this));
        if (mAccount.equals(Preferences.getPreferences(this).getDefaultAccount()) ||
                getIntent().getBooleanExtra(EXTRA_MAKE_DEFAULT, false)) {
            Preferences.getPreferences(this).setDefaultAccount(mAccount);
        }
        K9.setServicesEnabled(this);
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
