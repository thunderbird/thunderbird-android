
package com.fsck.k9.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fsck.k9.Account;
import com.fsck.k9.BaseAccount;
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;

public class LauncherShortcuts extends K9ListActivity implements OnItemClickListener
{
    private AccountsAdapter mAdapter;
    private FontSizes mFontSizes = K9.getFontSizes();

    @Override
    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);

        // finish() immediately if we aren't supposed to be here
        if (!Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction()))
        {
            finish();
            return;
        }

        setContentView(R.layout.launcher_shortcuts);
        ListView listView = getListView();
        listView.setOnItemClickListener(this);
        listView.setItemsCanFocus(false);

        refresh();
    }

    private void refresh()
    {
        Account[] accounts = Preferences.getPreferences(this).getAccounts();

        mAdapter = new AccountsAdapter(accounts);
        getListView().setAdapter(mAdapter);
    }

    private void setupShortcut(Account account)
    {
        final Intent shortcutIntent = FolderList.actionHandleAccountIntent(this, account, null, true);

        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        String description = account.getDescription();
        if (description == null || description.length() == 0)
        {
            description = account.getEmail();
        }
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, description);
        Parcelable iconResource = Intent.ShortcutIconResource.fromContext(this, R.drawable.icon);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);

        setResult(RESULT_OK, intent);
        finish();
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        Account account = (Account) parent.getItemAtPosition(position);
        setupShortcut(account);
    }

    class AccountsAdapter extends ArrayAdapter<Account>
    {
        public AccountsAdapter(Account[] accounts)
        {
            super(LauncherShortcuts.this, 0, accounts);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            final BaseAccount account = getItem(position);
            View view;
            if (convertView != null)
            {
                view = convertView;
            }
            else
            {
                view = getLayoutInflater().inflate(R.layout.accounts_item, parent, false);
            }
            AccountViewHolder holder = (AccountViewHolder) view.getTag();
            if (holder == null)
            {
                holder = new AccountViewHolder();
                holder.description = (TextView) view.findViewById(R.id.description);
                holder.email = (TextView) view.findViewById(R.id.email);
                holder.newMessageCount = (TextView) view.findViewById(R.id.new_message_count);
                holder.flaggedMessageCount = (TextView) view.findViewById(R.id.flagged_message_count);
                holder.activeIcons = (RelativeLayout) view.findViewById(R.id.active_icons);

                holder.chip = view.findViewById(R.id.chip);
                holder.folders = (ImageButton) view.findViewById(R.id.folders);

                view.setTag(holder);
            }

            if (account.getEmail().equals(account.getDescription()))
            {
                holder.email.setVisibility(View.GONE);
            }
            else
            {
                holder.email.setVisibility(View.VISIBLE);
                holder.email.setText(account.getEmail());
            }

            String description = account.getDescription();
            if (description == null || description.length() == 0)
            {
                description = account.getEmail();
            }

            holder.description.setText(description);

            holder.newMessageCount.setVisibility(View.GONE);
            holder.flaggedMessageCount.setVisibility(View.GONE);
            if (account instanceof Account)
            {
                Account realAccount = (Account)account;

                holder.chip.setBackgroundColor(realAccount.getChipColor());
                holder.chip.getBackground().setAlpha(255);

                // show unavailable accounts as translucent
                if (realAccount.isAvailable(getContext()))
                {
                    holder.email.getBackground().setAlpha(255);
                    holder.description.getBackground().setAlpha(255);
                }
                else
                {
                    holder.email.getBackground().setAlpha(127);
                    holder.description.getBackground().setAlpha(127);
                }
            }
            else
            {
                holder.chip.setBackgroundColor(0x00000000);
            }

            holder.description.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mFontSizes.getAccountName());
            holder.email.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mFontSizes.getAccountDescription());


            holder.folders.setVisibility(View.VISIBLE);
            holder.folders.setOnClickListener(new OnClickListener()
            {
                public void onClick(View v)
                {
                    Account account2 = (Account)account;
                    if (!account2.isAvailable(getContext()))
                    {
                        Log.i(K9.LOG_TAG, "refusing selection of unavailable account");
                        return ;
                    }
                    FolderList.actionHandleAccount(LauncherShortcuts.this, account2);

                }
            });

            return view;
        }

        class AccountViewHolder
        {
            public TextView description;
            public TextView email;
            public TextView newMessageCount;
            public TextView flaggedMessageCount;
            public RelativeLayout activeIcons;
            public View chip;
            public ImageButton folders;
        }
    }
}
