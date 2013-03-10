
package com.fsck.k9.activity.setup;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.TextKeyListener;
import android.text.method.TextKeyListener.Capitalize;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.fsck.k9.*;
import com.fsck.k9.activity.Accounts;
import com.fsck.k9.activity.ColorPickerDialog;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.helper.Utility;

public class AccountSetupNames extends K9Activity implements OnClickListener, ColorPickerDialog.OnColorChangedListener {
    private static final String EXTRA_ACCOUNT = "account";

    private EditText mDescription;

    private EditText mName;

    private Spinner mDisplayCountView;

    private CheckBox mNotifyView;
    private CheckBox mNotifySyncView;
    private Button mColorChip;
    private int mChipColor;

    private Account mAccount;

    private Button mDoneButton;

    public static void actionSetNames(Context context, Account account) {
        Intent i = new Intent(context, AccountSetupNames.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_setup_names);
        mDescription = (EditText)findViewById(R.id.account_description);
        mName = (EditText)findViewById(R.id.account_name);
        mDisplayCountView = (Spinner)findViewById(R.id.account_display_count);
        mNotifyView = (CheckBox)findViewById(R.id.account_notify);
        mNotifySyncView = (CheckBox)findViewById(R.id.account_notify_sync);
        mDoneButton = (Button)findViewById(R.id.done);
        mColorChip = (Button)findViewById(R.id.account_chip);

        mDoneButton.setOnClickListener(this);

        TextWatcher validationTextWatcher = new TextWatcher() {
            public void afterTextChanged(Editable s) {
                validateFields();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        };
        mName.addTextChangedListener(validationTextWatcher);

        mName.setKeyListener(TextKeyListener.getInstance(false, Capitalize.WORDS));

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
        mDisplayCountView.setAdapter(displayCountsAdapter);

        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);

        mNotifyView.setChecked(mAccount.isNotifyNewMail());
        mNotifySyncView.setChecked(mAccount.isShowOngoing());
        SpinnerOption.setSpinnerOptionValue(mDisplayCountView, mAccount
                                            .getDisplayCount());

        mChipColor = mAccount.getChipColor();
        onRefreshColorChip();

        /*
         * Since this field is considered optional, we don't set this here. If
         * the user fills in a value we'll reset the current value, otherwise we
         * just leave the saved value alone.
         */
        // mDescription.setText(mAccount.getDescription());
        if (mAccount.getName() != null) {
            mName.setText(mAccount.getName());
        }
        if (!Utility.requiredFieldValid(mName)) {
            mDoneButton.setEnabled(false);
        }
    }

    private void validateFields() {
        mDoneButton.setEnabled(Utility.requiredFieldValid(mName));
        Utility.setCompoundDrawablesAlpha(mDoneButton, mDoneButton.isEnabled() ? 255 : 128);
    }

    protected void onPickColor() {
        new ColorPickerDialog(this, this, mAccount.getChipColor()).show();
    }

    protected void onNext() {
        if (Utility.requiredFieldValid(mDescription)) {
            mAccount.setDescription(mDescription.getText().toString());
        }
        mAccount.setName(mName.getText().toString());
        mAccount.setNotifyNewMail(mNotifyView.isChecked());
        mAccount.setShowOngoing(mNotifySyncView.isChecked());
        mAccount.setDisplayCount((Integer)((SpinnerOption)mDisplayCountView
                                           .getSelectedItem()).value);
        mAccount.setChipColor(mChipColor);
        mAccount.save(Preferences.getPreferences(this));
        Accounts.listAccounts(this);
        finish();
    }

    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.account_chip:
            onPickColor();
            break;
        case R.id.done:
            onNext();
            break;
        }
    }

    public void colorChanged(int color) {
        /*
         * Temporarily set account chip color to picked one to generate color chip drawable.
         */
        mChipColor = mAccount.getChipColor();
        mAccount.setChipColor(color);
        onRefreshColorChip();
        mAccount.setChipColor(mChipColor);
        mChipColor = color;
    }

    private void onRefreshColorChip() {
        Drawable chipDrawable;
        int size = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32, getResources().getDisplayMetrics());
        chipDrawable = mAccount.generateColorChip().drawable();
        chipDrawable.setBounds(0, 0, size, size);
        mColorChip.setCompoundDrawables(null, null, chipDrawable, null);
        mColorChip.setOnClickListener(this);
    }
}
