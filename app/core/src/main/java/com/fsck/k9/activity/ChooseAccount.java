package com.fsck.k9.activity;

import android.content.Intent;

import com.fsck.k9.BaseAccount;

public class ChooseAccount extends AccountList {

    public static final String EXTRA_ACCOUNT_UUID = "com.fsck.k9.ChooseAccount_account_uuid";

    @Override
    protected boolean displaySpecialAccounts() {
        return true;
    }

    @Override
    protected void onAccountSelected(BaseAccount account) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_ACCOUNT_UUID, account.getUuid());
        setResult(RESULT_OK, intent);
        finish();
    }
}
