package com.fsck.k9.activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.fsck.k9.Account;
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;


/**
 * Activity displaying the list of accounts.
 *
 * <p>
 * Classes extending this abstract class have to provide an {@link #onAccountSelected(Account)}
 * method to perform an action when an account is selected.
 * </p>
 */
public abstract class AccountList extends K9ListActivity implements OnItemClickListener {
    private FontSizes mFontSizes = K9.getFontSizes();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setResult(RESULT_CANCELED);

        setContentView(R.layout.account_list);

        ListView listView = getListView();
        listView.setOnItemClickListener(this);
        listView.setItemsCanFocus(false);
    }

    /**
     * Reload list of accounts when this activity is resumed.
     */
    @Override
    public void onResume() {
        super.onResume();
        new LoadAccounts().execute();
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Account account = (Account) parent.getItemAtPosition(position);
        onAccountSelected(account);
    }

    /**
     * Create a new {@link AccountsAdapter} instance and assign it to the {@link ListView}.
     *
     * @param accounts
     *         An array of accounts to display.
     */
    public void populateListView(Account[] accounts) {
        AccountsAdapter adapter = new AccountsAdapter(accounts);
        ListView listView = getListView();
        listView.setAdapter(adapter);
        listView.invalidate();
    }

    /**
     * This method will be called when an account was selected.
     *
     * @param account
     *         The account the user selected.
     */
    protected abstract void onAccountSelected(Account account);

    class AccountsAdapter extends ArrayAdapter<Account> {
        private Account[] mAccounts;

        public AccountsAdapter(Account[] accounts) {
            super(AccountList.this, 0, accounts);
            mAccounts = accounts;
        }

        public Account[] getAccounts() {
            return mAccounts;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final Account account = getItem(position);

            final View view;
            if (convertView != null) {
                view = convertView;
            } else {
                view = getLayoutInflater().inflate(R.layout.accounts_item, parent, false);
                view.findViewById(R.id.active_icons).setVisibility(View.GONE);
                view.findViewById(R.id.folders).setVisibility(View.GONE);
                view.getBackground().setAlpha(0);
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

            holder.chip.setBackgroundColor(account.getChipColor());
            holder.chip.getBackground().setAlpha(255);

            holder.description.setTextSize(TypedValue.COMPLEX_UNIT_SP,
                    mFontSizes.getAccountName());
            holder.email.setTextSize(TypedValue.COMPLEX_UNIT_SP,
                    mFontSizes.getAccountDescription());

            return view;
        }

        class AccountViewHolder {
            public TextView description;
            public TextView email;
            public View chip;
        }
    }

    /**
     * Load accounts in a background thread
     */
    class LoadAccounts extends AsyncTask<Void, Void, Account[]> {
        @Override
        protected Account[] doInBackground(Void... params) {
            Account[] accounts = Preferences.getPreferences(getApplicationContext()).getAccounts();
            return accounts;
        }

        @Override
        protected void onPostExecute(Account[] accounts) {
            populateListView(accounts);
        }
    }
}
