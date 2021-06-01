package com.fsck.k9.activity;

import android.content.Intent;
import android.os.Bundle;

import com.fsck.k9.BaseAccount;
import com.fsck.k9.ui.R;


public class ChooseAccount extends AccountList {

    public static final String EXTRA_ACCOUNT_UUID = "com.fsck.k9.ChooseAccount_account_uuid";

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setTitle(R.string.choose_account_title);
    }

    @Override
    protected void onAccountSelected(BaseAccount account) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_ACCOUNT_UUID, account.getUuid());
        setResult(RESULT_OK, intent);
        finish();
    }
}
