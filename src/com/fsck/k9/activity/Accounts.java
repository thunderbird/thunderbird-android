
package com.android.email.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.android.email.Account;
import com.android.email.Email;
import com.android.email.MessagingController;
import com.android.email.Preferences;
import com.android.email.R;
import com.android.email.activity.setup.AccountSettings;
import com.android.email.activity.setup.AccountSetupBasics;
import com.android.email.mail.MessagingException;
import com.android.email.mail.Store;
import com.android.email.mail.store.LocalStore;
import com.android.email.mail.store.LocalStore.LocalFolder;

public class Accounts extends ListActivity implements OnItemClickListener, OnClickListener {
    private static final int DIALOG_REMOVE_ACCOUNT = 1;
    /**
     * Key codes used to open a debug settings screen.
     */
    private static int[] secretKeyCodes = {
            KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_B, KeyEvent.KEYCODE_U,
            KeyEvent.KEYCODE_G
    };

    private int mSecretKeyCodeIndex = 0;
    private Account mSelectedContextAccount;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.accounts);
        ListView listView = getListView();
        listView.setOnItemClickListener(this);
        listView.setItemsCanFocus(false);
        listView.setEmptyView(findViewById(R.id.empty));
        findViewById(R.id.add_new_account).setOnClickListener(this);
        registerForContextMenu(listView);

        if (icicle != null && icicle.containsKey("selectedContextAccount")) {
            mSelectedContextAccount = (Account) icicle.getSerializable("selectedContextAccount");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSelectedContextAccount != null) {
            outState.putSerializable("selectedContextAccount", mSelectedContextAccount);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        NotificationManager notifMgr = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        notifMgr.cancel(1);

        refresh();
    }

    private void refresh() {
        Account[] accounts = Preferences.getPreferences(this).getAccounts();
        getListView().setAdapter(new AccountsAdapter(accounts));
    }

    private void onAddNewAccount() {
        AccountSetupBasics.actionNewAccount(this);
    }

    private void onEditAccount(Account account) {
        AccountSettings.actionSettings(this, account);
    }

    private void onRefresh() {
        MessagingController.getInstance(getApplication()).checkMail(this, null, null);
    }

    private void onCompose() {
        Account defaultAccount =
                Preferences.getPreferences(this).getDefaultAccount();
        if (defaultAccount != null) {
            MessageCompose.actionCompose(this, defaultAccount);
        }
        else {
            onAddNewAccount();
        }
    }

    private void onOpenAccount(Account account) {
        FolderMessageList.actionHandleAccount(this, account);
    }

    public void onClick(View view) {
        if (view.getId() == R.id.add_new_account) {
            onAddNewAccount();
        }
    }

    private void onDeleteAccount(Account account) {
        mSelectedContextAccount = account;
        showDialog(DIALOG_REMOVE_ACCOUNT);
    }

    @Override
    public Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_REMOVE_ACCOUNT:
                return createRemoveAccountDialog();
        }
        return super.onCreateDialog(id);
    }

    private Dialog createRemoveAccountDialog() {
        return new AlertDialog.Builder(this)
            .setTitle(R.string.account_delete_dlg_title)
            .setMessage(getString(R.string.account_delete_dlg_instructions_fmt,
                    mSelectedContextAccount.getDescription()))
            .setPositiveButton(R.string.okay_action, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dismissDialog(DIALOG_REMOVE_ACCOUNT);
                    try {
                        ((LocalStore)Store.getInstance(
                                mSelectedContextAccount.getLocalStoreUri(),
                                getApplication())).delete();
                    } catch (Exception e) {
                            // Ignore
                    }
                    mSelectedContextAccount.delete(Preferences.getPreferences(Accounts.this));
                    Email.setServicesEnabled(Accounts.this);
                    refresh();
                }
            })
            .setNegativeButton(R.string.cancel_action, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dismissDialog(DIALOG_REMOVE_ACCOUNT);
                }
            })
            .create();
    }

    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo)item.getMenuInfo();
        Account account = (Account)getListView().getItemAtPosition(menuInfo.position);
        switch (item.getItemId()) {
            case R.id.delete_account:
                onDeleteAccount(account);
                break;
            case R.id.edit_account:
                onEditAccount(account);
                break;
            case R.id.open:
                onOpenAccount(account);
                break;
        }
        return true;
    }

    public void onItemClick(AdapterView parent, View view, int position, long id) {
        Account account = (Account)parent.getItemAtPosition(position);
        onOpenAccount(account);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_new_account:
                onAddNewAccount();
                break;
            case R.id.check_mail:
                onRefresh();
                break;
            case R.id.compose:
                onCompose();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.accounts_option, menu);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle(R.string.accounts_context_menu_title);
        getMenuInflater().inflate(R.menu.accounts_context, menu);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == secretKeyCodes[mSecretKeyCodeIndex]) {
            mSecretKeyCodeIndex++;
            if (mSecretKeyCodeIndex == secretKeyCodes.length) {
                mSecretKeyCodeIndex = 0;
                startActivity(new Intent(this, Debug.class));
            }
        } else {
            mSecretKeyCodeIndex = 0;
        }
        return super.onKeyDown(keyCode, event);
    }

    class AccountsAdapter extends ArrayAdapter<Account> {
        public AccountsAdapter(Account[] accounts) {
            super(Accounts.this, 0, accounts);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Account account = getItem(position);
            View view;
            if (convertView != null) {
                view = convertView;
            }
            else {
                view = getLayoutInflater().inflate(R.layout.accounts_item, parent, false);
            }
            AccountViewHolder holder = (AccountViewHolder) view.getTag();
            if (holder == null) {
                holder = new AccountViewHolder();
                holder.description = (TextView) view.findViewById(R.id.description);
                holder.email = (TextView) view.findViewById(R.id.email);
                holder.newMessageCount = (TextView) view.findViewById(R.id.new_message_count);
                view.setTag(holder);
            }
            holder.description.setText(account.getDescription());
            holder.email.setText(account.getEmail());
            if (account.getEmail().equals(account.getDescription())) {
                holder.email.setVisibility(View.GONE);
            }
            int unreadMessageCount = 0;
            try {
                LocalStore localStore = (LocalStore) Store.getInstance(
                        account.getLocalStoreUri(),
                        getApplication());
                LocalFolder localFolder = (LocalFolder) localStore.getFolder(Email.INBOX);
                if (localFolder.exists()) {
                    unreadMessageCount = localFolder.getUnreadMessageCount();
                }
            }
            catch (MessagingException me) {
                /*
                 * This is not expected to fail under normal circumstances.
                 */
                throw new RuntimeException("Unable to get unread count from local store.", me);
            }
            holder.newMessageCount.setText(Integer.toString(unreadMessageCount));
            holder.newMessageCount.setVisibility(unreadMessageCount > 0 ? View.VISIBLE : View.GONE);
            return view;
        }

        class AccountViewHolder {
            public TextView description;
            public TextView email;
            public TextView newMessageCount;
        }
    }
}


