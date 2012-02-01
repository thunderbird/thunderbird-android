package com.fsck.k9.activity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.fsck.k9.Account;
import com.fsck.k9.BaseAccount;
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.SearchAccount;
import com.fsck.k9.SearchSpecification;
import com.fsck.k9.helper.Utility;

public class LauncherShortcuts extends K9ListActivity implements OnItemClickListener {
    private AccountsAdapter mAdapter;
    private FontSizes mFontSizes = K9.getFontSizes();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // finish() immediately if we aren't supposed to be here
        if (!Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction())) {
            finish();
            return;
        }

        setContentView(R.layout.launcher_shortcuts);
        ListView listView = getListView();
        listView.setOnItemClickListener(this);
        listView.setItemsCanFocus(false);

        refresh();
    }

    private void refresh() {
        List<BaseAccount> accounts = new ArrayList<BaseAccount>();

        if (!K9.isHideSpecialAccounts()) {
            BaseAccount integratedInboxAccount = new SearchAccount(this, true, null,  null);
            integratedInboxAccount.setDescription(
                    getString(R.string.integrated_inbox_title));
            integratedInboxAccount.setEmail(
                    getString(R.string.integrated_inbox_detail));

            BaseAccount unreadAccount = new SearchAccount(this, false, null, null);
            unreadAccount.setDescription(
                    getString(R.string.search_all_messages_title));
            unreadAccount.setEmail(
                    getString(R.string.search_all_messages_detail));

            accounts.add(integratedInboxAccount);
            accounts.add(unreadAccount);
        }

        accounts.addAll(Arrays.asList(Preferences.getPreferences(this).getAccounts()));

        mAdapter = new AccountsAdapter(accounts);
        getListView().setAdapter(mAdapter);
    }

    private void setupShortcut(BaseAccount account) {
        Intent shortcutIntent = null;

        if (account instanceof SearchSpecification) {
            shortcutIntent = MessageList.actionHandleAccountIntent(
                    this, account.getDescription(), (SearchSpecification) account);
        } else {
            shortcutIntent = FolderList.actionHandleAccountIntent(this,
                    (Account) account, null, true);
        }

        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        String description = account.getDescription();
        if (description == null || description.length() == 0) {
            description = account.getEmail();
        }
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, description);
        Parcelable iconResource = Intent.ShortcutIconResource.fromContext(this, R.drawable.icon);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);

        setResult(RESULT_OK, intent);
        finish();
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        BaseAccount account = (BaseAccount) parent.getItemAtPosition(position);
        setupShortcut(account);
    }

    class AccountsAdapter extends ArrayAdapter<BaseAccount> {
        public AccountsAdapter(List<BaseAccount> accounts) {
            super(LauncherShortcuts.this, 0, accounts);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final BaseAccount account = getItem(position);

            final View view;
            if (convertView != null) {
                view = convertView;
            } else {
                view = getLayoutInflater().inflate(R.layout.accounts_item, parent, false);
                view.findViewById(R.id.active_icons).setVisibility(View.GONE);
            }

            AccountViewHolder holder = (AccountViewHolder) view.getTag();
            if (holder == null) {
                holder = new AccountViewHolder();
                holder.description = (TextView) view.findViewById(R.id.description);
                holder.email = (TextView) view.findViewById(R.id.email);
                holder.chip = view.findViewById(R.id.chip);

                view.setTag(holder);
            }

            String description = account.getDescription();
            if (account.getEmail().equals(description)) {
                holder.email.setVisibility(View.GONE);
            } else {
                holder.email.setVisibility(View.VISIBLE);
                holder.email.setText(account.getEmail());
            }

            if (description == null || description.length() == 0) {
                description = account.getEmail();
            }

            holder.description.setText(description);

            if (account instanceof Account) {
                holder.chip.setBackgroundColor(((Account) account).getChipColor());
            }

            holder.chip.getBackground().setAlpha(255);

            holder.description.setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSizes.getAccountName());
            holder.email.setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSizes.getAccountDescription());

            return view;
        }

        class AccountViewHolder {
            public TextView description;
            public TextView email;
            public View chip;
        }
    }
}
