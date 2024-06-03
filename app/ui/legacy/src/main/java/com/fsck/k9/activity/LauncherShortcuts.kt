package com.fsck.k9.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

import com.fsck.k9.BaseAccount;
import com.fsck.k9.ui.R;
import com.fsck.k9.search.SearchAccount;

public class LauncherShortcuts extends AccountList {
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setTitle(R.string.shortcuts_title);

        // finish() immediately if we aren't supposed to be here
        if (!Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction())) {
            finish();
        }
    }

    @Override
    protected void onAccountSelected(BaseAccount account) {
        Intent shortcutIntent;
        if (account instanceof SearchAccount) {
            SearchAccount searchAccount = (SearchAccount) account;
            shortcutIntent = MessageList.shortcutIntent(this, searchAccount.getId());
        } else {
            shortcutIntent = MessageList.shortcutIntentForAccount(this, account.getUuid());
        }

        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        String accountName = account.getName();
        String displayName;
        if (accountName != null) {
            displayName = accountName;
        } else {
            displayName = account.getEmail();
        }
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, displayName);
        Parcelable iconResource = Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_launcher);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);

        setResult(RESULT_OK, intent);
        finish();
    }
}
