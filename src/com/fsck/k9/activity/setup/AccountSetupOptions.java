
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

        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);

        SpinnerOption.setSpinnerOptionValue(mCheckFrequencyView, mAccount
                                            .getAutomaticCheckIntervalMinutes());


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
        mAccount.setAutomaticCheckIntervalMinutes((Integer)((SpinnerOption)mCheckFrequencyView
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
