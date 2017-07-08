
package com.fsck.k9.activity.setup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.TextKeyListener;
import android.text.method.TextKeyListener.Capitalize;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import com.fsck.k9.*;
import com.fsck.k9.activity.Accounts;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.helper.Utility;

public class AccountSetupNames extends K9Activity implements OnClickListener {
    private static final String EXTRA_ACCOUNT = "account";

    private EditText description;

    private EditText name;

    private Account account;

    private Button doneButton;

    public static void actionSetNames(Context context, Account account) {
        Intent i = new Intent(context, AccountSetupNames.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_setup_names);
        description = (EditText)findViewById(R.id.account_description);
        name = (EditText)findViewById(R.id.account_name);
        doneButton = (Button)findViewById(R.id.done);
        doneButton.setOnClickListener(this);

        TextWatcher validationTextWatcher = new TextWatcher() {
            public void afterTextChanged(Editable s) {
                validateFields();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        };
        name.addTextChangedListener(validationTextWatcher);

        name.setKeyListener(TextKeyListener.getInstance(false, Capitalize.WORDS));

        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        account = Preferences.getPreferences(this).getAccount(accountUuid);

        /*
         * Since this field is considered optional, we don't set this here. If
         * the user fills in a value we'll reset the current value, otherwise we
         * just leave the saved value alone.
         */
        // description.setText(account.getDescription());
        if (account.getName() != null) {
            name.setText(account.getName());
        }
        if (!Utility.requiredFieldValid(name)) {
            doneButton.setEnabled(false);
        }
    }

    private void validateFields() {
        doneButton.setEnabled(Utility.requiredFieldValid(name));
        Utility.setCompoundDrawablesAlpha(doneButton, doneButton.isEnabled() ? 255 : 128);
    }

    protected void onNext() {
        if (Utility.requiredFieldValid(description)) {
            account.setDescription(description.getText().toString());
        }
        account.setName(name.getText().toString());
        account.save(Preferences.getPreferences(this));
        Accounts.listAccounts(this);
        finish();
    }

    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.done:
            onNext();
            break;
        }
    }
}
