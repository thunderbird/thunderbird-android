
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

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.MessageList;
import com.fsck.k9.ui.base.K9Activity;
import com.fsck.k9.ui.R;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.ui.permissions.K9PermissionUiHelper;
import com.fsck.k9.ui.permissions.Permission;
import com.fsck.k9.ui.permissions.PermissionUiHelper;

public class AccountSetupNames extends K9Activity implements OnClickListener, PermissionUiHelper,
    ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String EXTRA_ACCOUNT = "account";

    private EditText mDescription;

    private EditText mName;

    private Account mAccount;

    private Button mDoneButton;

    private final PermissionUiHelper permissionUiHelper = new K9PermissionUiHelper(this);

    public static void actionSetNames(Context context, Account account) {
        Intent i = new Intent(context, AccountSetupNames.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLayout(R.layout.account_setup_names);
        setTitle(R.string.account_setup_names_title);

        mDescription = findViewById(R.id.account_description);
        mName = findViewById(R.id.account_name);
        mDoneButton = findViewById(R.id.done);
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

        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences().getAccount(accountUuid);

        String senderName = mAccount.getSenderName();
        if (senderName != null) {
            mName.setText(senderName);
        }

        if (!Utility.requiredFieldValid(mName)) {
            mDoneButton.setEnabled(false);
        }
    }

    private void validateFields() {
        mDoneButton.setEnabled(Utility.requiredFieldValid(mName));
        Utility.setCompoundDrawablesAlpha(mDoneButton, mDoneButton.isEnabled() ? 255 : 128);
    }

    protected void onNext() {
        if (Utility.requiredFieldValid(mDescription)) {
            mAccount.setName(mDescription.getText().toString());
        }
        mAccount.setSenderName(mName.getText().toString());
        mAccount.markSetupFinished();
        Preferences.getPreferences().saveAccount(mAccount);
        checkAndRequestPermissions();
    }

    public void onClick(View v) {
        if (v.getId() == R.id.done) {
            onNext();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Permission.READ_CONTACTS.getRequestCode()) {
            finishAffinity();
            MessageList.launch(this, mAccount);
        }
    }

    private void checkAndRequestPermissions() {
        if (!hasPermission(Permission.READ_CONTACTS)) {
            requestPermissionOrShowRationale(Permission.READ_CONTACTS);
        }
    }

    @Override
    public boolean hasPermission(@NonNull Permission permission) {
        return permissionUiHelper.hasPermission(permission);
    }

    @Override
    public void requestPermissionOrShowRationale(@NonNull Permission permission) {
        permissionUiHelper.requestPermissionOrShowRationale(permission);
    }

    @Override
    public void requestPermission(@NonNull Permission permission) {
        permissionUiHelper.requestPermission(permission);
    }
}
