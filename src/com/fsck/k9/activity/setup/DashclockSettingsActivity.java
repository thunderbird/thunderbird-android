package com.fsck.k9.activity.setup;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import com.fsck.k9.Account;
import com.fsck.k9.BaseAccount;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.K9ListActivity;
import com.fsck.k9.helper.StringUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class DashclockSettingsActivity extends K9ListActivity implements
        AdapterView.OnItemClickListener {

    public static final String PREFERENCE_DASHCLOCK_SELECTED_ACCOUNTS = "dashclockSelectedAccountsUuids";
    private List<BaseAccount> selectedAccounts = new ArrayList<BaseAccount>();
    private AccountsAdapter mAccountAdapter;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        ListView listView = getListView();
        listView.setOnItemClickListener(this);
        listView.setItemsCanFocus(false);

    }

    @Override
    public void onResume() {
        super.onResume();
        new LoadAccounts().execute();
    }

    @Override
    protected void onPause() {
        new SavePrefTask().execute();
        super.onPause();
    }

    private void saveSelectedAccounts() {
        Preferences preferences = Preferences.getPreferences(getApplicationContext());
        SharedPreferences.Editor edit = preferences.getPreferences().edit();

        Iterator<BaseAccount> iterator = selectedAccounts.iterator();
        if (iterator.hasNext()) {
            StringBuilder builder = new StringBuilder(iterator.next().getUuid());
            while( iterator.hasNext() )
            {
                builder.append(',').append(iterator.next().getUuid());
            }
            String selectedAccountsUuids = builder.toString();
            Log.d(K9.LOG_TAG, "dashclock saving uuids:  "+selectedAccountsUuids);
            edit.putString(PREFERENCE_DASHCLOCK_SELECTED_ACCOUNTS, selectedAccountsUuids);
        } else {
            Log.d(K9.LOG_TAG, "dashclock saving uuids:  "+null);
            edit.putString(PREFERENCE_DASHCLOCK_SELECTED_ACCOUNTS, null);
        }

        edit.commit();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        BaseAccount account = (BaseAccount) parent.getItemAtPosition(position);
        AccountsAdapter.AccountViewHolder holder = (AccountsAdapter.AccountViewHolder) view.getTag();
        onAccountSelectionChanged(account, holder.selectAccount.isChecked());
    }

    protected void onAccountSelectionChanged(BaseAccount account, boolean selected) {
        if (true==selected) {
            selectedAccounts.add(account);
        } else {
            selectedAccounts.remove(account);
        }

        mAccountAdapter.notifyDataSetChanged();
    }


    public void populateListView(Account[] realAccounts) {
        List<BaseAccount> accounts = new ArrayList<BaseAccount>();

        accounts.addAll(Arrays.asList(realAccounts));
        mAccountAdapter = new AccountsAdapter(accounts);
        ListView listView = getListView();
        listView.setAdapter(mAccountAdapter);
        listView.invalidate();
    }

    class AccountsAdapter extends ArrayAdapter<BaseAccount> {
        public AccountsAdapter(List<BaseAccount> accounts) {
            super(DashclockSettingsActivity.this, 0, accounts);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final BaseAccount account = getItem(position);

            final View view;
            if (convertView != null) {
                view = convertView;
            } else {
                view = getLayoutInflater().inflate(R.layout.accounts_item_selectable, parent, false);
            }

            AccountViewHolder holder = (AccountViewHolder) view.getTag();
            if (holder == null) {
                holder = new AccountViewHolder();
                holder.description = (TextView) view.findViewById(R.id.description);
                holder.email = (TextView) view.findViewById(R.id.email);
                holder.chip = view.findViewById(R.id.chip);
                holder.selectAccount = (CheckBox) view.findViewById(R.id.select_account);
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

            if (selectedAccounts.contains(account)) {
                holder.selectAccount.setChecked(true);
            } else {
               holder.selectAccount.setChecked(false);
            }
            holder.selectAccount.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    onAccountSelectionChanged(account, b);
                }
            });

            if (account instanceof Account) {
                Account realAccount = (Account) account;
                holder.chip.setBackgroundColor(realAccount.getChipColor());
            } else {
                holder.chip.setBackgroundColor(0xff999999);
            }

            holder.chip.getBackground().setAlpha(255);

            return view;
        }

        class AccountViewHolder {
            public TextView description;
            public TextView email;
            public View chip;
            public CheckBox selectAccount;
        }
    }

    /**
     * Load accounts in a background thread
     */
    class LoadAccounts extends AsyncTask<Void, Void, Account[]> {
        @Override
        protected Account[] doInBackground(Void... params) {
            Account[] accounts = Preferences.getPreferences(getApplicationContext()).getAccounts();
            String selectedAccountsUuids = Preferences.getPreferences(getApplicationContext())
                                            .getPreferences()
                                            .getString(PREFERENCE_DASHCLOCK_SELECTED_ACCOUNTS, null);

            selectedAccounts.clear();
            Log.d(K9.LOG_TAG, "dashclock loaded uuids:  "+selectedAccountsUuids);
            if(!StringUtils.isNullOrEmpty(selectedAccountsUuids)) {
                String[] selectedUuids = selectedAccountsUuids.split(",");

                for(String uuid : selectedUuids) {
                    Account account = Preferences.getPreferences(getApplicationContext()).getAccount(uuid);
                    if (account != null) {
                        selectedAccounts.add(account);
                    }
                }
            }

            //This will be used to populate list, so return all accounts.
            return accounts;
        }

        @Override
        protected void onPostExecute(Account[] accounts) {
            populateListView(accounts);
        }
    }

    /**
     *  Saves preferrences data in a background thread.
     */
    class SavePrefTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            saveSelectedAccounts();
            return null;
        }
    }
}