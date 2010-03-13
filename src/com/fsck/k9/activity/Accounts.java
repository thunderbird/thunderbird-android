
package com.fsck.k9.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.*;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import com.fsck.k9.*;
import com.fsck.k9.activity.setup.AccountSettings;
import com.fsck.k9.activity.setup.AccountSetupBasics;
import com.fsck.k9.activity.setup.Prefs;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Accounts extends K9ListActivity implements OnItemClickListener, OnClickListener
{
    private static final int DIALOG_REMOVE_ACCOUNT = 1;
    private ConcurrentHashMap<String, Integer> unreadMessageCounts = new ConcurrentHashMap<String, Integer>();

    private ConcurrentHashMap<Account, String> pendingWork = new ConcurrentHashMap<Account, String>();

    private Account mSelectedContextAccount;
    private int mUnreadMessageCount = 0;

    private AccountsHandler mHandler = new AccountsHandler();
    private AccountsAdapter mAdapter;


    class AccountsHandler extends Handler
    {
        private void setViewTitle()
        {
            String dispString = mListener.formatHeader(Accounts.this, getString(R.string.accounts_title), mUnreadMessageCount, getTimeFormat());

            setTitle(dispString);
        }
        public void refreshTitle()
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    setViewTitle();
                }
            });
        }

        public void dataChanged()
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    if (mAdapter != null)
                    {
                        mAdapter.notifyDataSetChanged();
                    }
                }
            });
        }

        public void workingAccount(final Account account, final int res)
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    String toastText = getString(res, account.getDescription());

                    Toast toast = Toast.makeText(getApplication(), toastText, Toast.LENGTH_SHORT);
                    toast.show();
                }
            });
        }

        public void accountSizeChanged(final Account account, final long oldSize, final long newSize)
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    String toastText = getString(R.string.account_size_changed, account.getDescription(),
                                                 SizeFormatter.formatSize(getApplication(), oldSize), SizeFormatter.formatSize(getApplication(), newSize));;

                    Toast toast = Toast.makeText(getApplication(), toastText, Toast.LENGTH_LONG);
                    toast.show();
                }
            });
        }

        public void progress(final boolean progress)
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    setProgressBarIndeterminateVisibility(progress);
                }
            });
        }
        public void progress(final int progress)
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    getWindow().setFeatureInt(Window.FEATURE_PROGRESS, progress);
                }
            });
        }
    }

    ActivityListener mListener = new ActivityListener()
    {
        @Override
        public void accountStatusChanged(Account account, int unreadMessageCount)
        {
            Integer oldUnreadMessageCountInteger = unreadMessageCounts.get(account.getUuid());
            int oldUnreadMessageCount = 0;
            if (oldUnreadMessageCountInteger != null)
            {
                oldUnreadMessageCount = oldUnreadMessageCountInteger;
            }

            unreadMessageCounts.put(account.getUuid(), unreadMessageCount);
            mUnreadMessageCount += unreadMessageCount - oldUnreadMessageCount;
            mHandler.dataChanged();
            pendingWork.remove(account);


            if (pendingWork.isEmpty())
            {
                mHandler.progress(Window.PROGRESS_END);
                mHandler.refreshTitle();
            }
            else
            {
                int level = (Window.PROGRESS_END / mAdapter.getCount()) * (mAdapter.getCount() - pendingWork.size()) ;
                mHandler.progress(level);
            }
        }

        @Override
        public void accountSizeChanged(Account account, long oldSize, long newSize)
        {

            mHandler.accountSizeChanged(account, oldSize, newSize);

        }

        @Override
        public void synchronizeMailboxFinished(
            Account account,
            String folder,
            int totalMessagesInMailbox,
            int numNewMessages)
        {
            super.synchronizeMailboxFinished(account, folder, totalMessagesInMailbox, numNewMessages);
            MessagingController.getInstance(getApplication()).getAccountUnreadCount(Accounts.this, account, mListener);

            mHandler.progress(false);

            mHandler.refreshTitle();
        }

        @Override
        public void synchronizeMailboxStarted(Account account, String folder)
        {
            super.synchronizeMailboxStarted(account, folder);
            mHandler.progress(true);
            mHandler.refreshTitle();
        }

        public void synchronizeMailboxProgress(Account account, String folder, int completed, int total)
        {
            super.synchronizeMailboxProgress(account, folder, completed, total);
            mHandler.refreshTitle();
        }

        @Override
        public void synchronizeMailboxFailed(Account account, String folder,
                                             String message)
        {
            super.synchronizeMailboxFailed(account, folder, message);
            mHandler.progress(false);
            mHandler.refreshTitle();

        }

        @Override
        public void sendPendingMessagesStarted(Account account)
        {
            super.sendPendingMessagesStarted(account);
            mHandler.refreshTitle();
        }

        @Override
        public void sendPendingMessagesCompleted(Account account)
        {
            super.sendPendingMessagesCompleted(account);
            mHandler.refreshTitle();
        }


        @Override
        public void sendPendingMessagesFailed(Account account)
        {
            super.sendPendingMessagesFailed(account);
            mHandler.refreshTitle();
        }

        public void pendingCommandsProcessing(Account account)
        {
            super.pendingCommandsProcessing(account);
            mHandler.refreshTitle();
        }
        public void pendingCommandsFinished(Account account)
        {
            super.pendingCommandsFinished(account);
            mHandler.refreshTitle();
        }
        public void pendingCommandStarted(Account account, String commandTitle)
        {
            super.pendingCommandStarted(account, commandTitle);
            mHandler.refreshTitle();
        }
        public void pendingCommandCompleted(Account account, String commandTitle)
        {
            super.pendingCommandCompleted(account, commandTitle);
            mHandler.refreshTitle();
        }


    };

    private static String UNREAD_MESSAGE_COUNTS = "unreadMessageCounts";
    private static String SELECTED_CONTEXT_ACCOUNT = "selectedContextAccount";

    public static final String EXTRA_STARTUP = "startup";


    public static void actionLaunch(Context context)
    {
        Intent intent = new Intent(context, Accounts.class);
        intent.putExtra(EXTRA_STARTUP, true);
        context.startActivity(intent);
    }

    public static void listAccounts(Context context)
    {
        Intent intent = new Intent(context, Accounts.class);
        intent.putExtra(EXTRA_STARTUP, false);
        context.startActivity(intent);
    }


    @Override
    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);

        Account[] accounts = Preferences.getPreferences(this).getAccounts();
        Intent intent = getIntent();
        boolean startup = (boolean)intent.getBooleanExtra(EXTRA_STARTUP, true);
        if (startup && accounts.length == 1)
        {
            onOpenAccount(accounts[0]);
            finish();
        }
        else
        {
            requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
            requestWindowFeature(Window.FEATURE_PROGRESS);

            setContentView(R.layout.accounts);
            ListView listView = getListView();
            listView.setOnItemClickListener(this);
            listView.setItemsCanFocus(false);
            listView.setEmptyView(findViewById(R.id.empty));
            findViewById(R.id.next).setOnClickListener(this);
            registerForContextMenu(listView);

            if (icicle != null && icicle.containsKey(SELECTED_CONTEXT_ACCOUNT))
            {
                String accountUuid = icicle.getString("selectedContextAccount");
                mSelectedContextAccount = Preferences.getPreferences(this).getAccount(accountUuid);
            }

            if (icicle != null)
            {
                Map<String, Integer> oldUnreadMessageCounts = (Map<String, Integer>)icicle.get(UNREAD_MESSAGE_COUNTS);
                if (oldUnreadMessageCounts != null)
                {
                    unreadMessageCounts.putAll(oldUnreadMessageCounts);
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        if (mSelectedContextAccount != null)
        {
            outState.putString(SELECTED_CONTEXT_ACCOUNT, mSelectedContextAccount.getUuid());
        }
        outState.putSerializable(UNREAD_MESSAGE_COUNTS, unreadMessageCounts);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        refresh();
        MessagingController.getInstance(getApplication()).addListener(mListener);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        MessagingController.getInstance(getApplication()).removeListener(mListener);
    }

    private void refresh()
    {
        Account[] accounts = Preferences.getPreferences(this).getAccounts();
        mAdapter = new AccountsAdapter(accounts);
        getListView().setAdapter(mAdapter);
        if (accounts.length > 0)
        {
            mHandler.progress(Window.PROGRESS_START);
        }
        pendingWork.clear();
        mUnreadMessageCount = 0;
        unreadMessageCounts.clear();

        for (Account account : accounts)
        {
            pendingWork.put(account, "true");
            MessagingController.getInstance(getApplication()).getAccountUnreadCount(Accounts.this, account, mListener);

        }
    }

    private void onAddNewAccount()
    {
        AccountSetupBasics.actionNewAccount(this);
    }

    private void onEditAccount(Account account)
    {
        AccountSettings.actionSettings(this, account);
    }

    private void onEditPrefs()
    {
        Prefs.actionPrefs(this);
    }


    /*
     * This method is called with 'null' for the argument 'account' if
     * all accounts are to be checked. This is handled accordingly in
     * MessagingController.checkMail().
     */
    private void onCheckMail(Account account)
    {
        MessagingController.getInstance(getApplication()).checkMail(this, account, true, true, null);
    }

    private void onClearCommands(Account account)
    {
        MessagingController.getInstance(getApplication()).clearAllPending(account);
    }

    private void onEmptyTrash(Account account)
    {
        MessagingController.getInstance(getApplication()).emptyTrash(account, null);
    }


    private void onCompose()
    {
        Account defaultAccount = Preferences.getPreferences(this).getDefaultAccount();
        if (defaultAccount != null)
        {
            MessageCompose.actionCompose(this, defaultAccount);
        }
        else
        {
            onAddNewAccount();
        }
    }

    private void onOpenAccount(Account account)
    {
        if (K9.FOLDER_NONE.equals(account.getAutoExpandFolderName()))
        {
            FolderList.actionHandleAccount(this, account);
        }
        else
        {
            MessageList.actionHandleFolder(this, account, account.getAutoExpandFolderName());
        }
    }

    public void onClick(View view)
    {
        if (view.getId() == R.id.next)
        {
            onAddNewAccount();
        }
    }

    private void onDeleteAccount(Account account)
    {
        mSelectedContextAccount = account;
        showDialog(DIALOG_REMOVE_ACCOUNT);
    }

    @Override
    public Dialog onCreateDialog(int id)
    {
        switch (id)
        {
            case DIALOG_REMOVE_ACCOUNT:
                return createRemoveAccountDialog();
        }
        return super.onCreateDialog(id);
    }

    public void onPrepareDialog(int id, Dialog d)
    {
        switch (id)
        {
            case DIALOG_REMOVE_ACCOUNT:
                AlertDialog alert = (AlertDialog) d;
                alert.setMessage(getString(R.string.account_delete_dlg_instructions_fmt,
                                           mSelectedContextAccount.getDescription()));
                break;
        }

        super.onPrepareDialog(id, d);
    }


    private Dialog createRemoveAccountDialog()
    {
        return new AlertDialog.Builder(this)
               .setTitle(R.string.account_delete_dlg_title)
               .setMessage(getString(R.string.account_delete_dlg_instructions_fmt, mSelectedContextAccount.getDescription()))
               .setPositiveButton(R.string.okay_action, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                dismissDialog(DIALOG_REMOVE_ACCOUNT);
                try
                {
                    mSelectedContextAccount.getLocalStore().delete();
                }
                catch (Exception e)
                {
                    // Ignore
                }
                MessagingController.getInstance(getApplication()).notifyAccountCancel(Accounts.this, mSelectedContextAccount);
                Preferences.getPreferences(Accounts.this).deleteAccount(mSelectedContextAccount);
                K9.setServicesEnabled(Accounts.this);
                refresh();
            }
        })
               .setNegativeButton(R.string.cancel_action, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                dismissDialog(DIALOG_REMOVE_ACCOUNT);
            }
        })
               .create();
    }

    public boolean onContextItemSelected(MenuItem item)
    {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo)item.getMenuInfo();
        // submenus don't actually set the menuInfo, so the "advanced"
        // submenu wouldn't work.
        if (menuInfo != null)
        {
            mSelectedContextAccount = (Account)getListView().getItemAtPosition(menuInfo.position);
        }
        switch (item.getItemId())
        {
            case R.id.delete_account:
                onDeleteAccount(mSelectedContextAccount);
                break;
            case R.id.edit_account:
                onEditAccount(mSelectedContextAccount);
                break;
            case R.id.open:
                onOpenAccount(mSelectedContextAccount);
                break;
            case R.id.check_mail:
                onCheckMail(mSelectedContextAccount);
                break;
            case R.id.clear_pending:
                onClearCommands(mSelectedContextAccount);
                break;
            case R.id.empty_trash:
                onEmptyTrash(mSelectedContextAccount);
                break;
            case R.id.compact:
                onCompact(mSelectedContextAccount);
                break;
            case R.id.clear:
                onClear(mSelectedContextAccount);
                break;
        }
        return true;
    }



    private void onCompact(Account account)
    {
        mHandler.workingAccount(account, R.string.compacting_account);
        MessagingController.getInstance(getApplication()).compact(account, null);
    }

    private void onClear(Account account)
    {
        mHandler.workingAccount(account, R.string.clearing_account);
        MessagingController.getInstance(getApplication()).clear(account, null);
    }


    public void onItemClick(AdapterView parent, View view, int position, long id)
    {
        Account account = (Account)parent.getItemAtPosition(position);
        onOpenAccount(account);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.add_new_account:
                onAddNewAccount();
                break;
            case R.id.edit_prefs:
                onEditPrefs();
                break;
            case R.id.check_mail:
                onCheckMail(null);
                break;
            case R.id.compose:
                onCompose();
                break;
            case R.id.about:
                onAbout();
                break;
            case R.id.search:
                onSearchRequested();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void onAbout()
    {
        String appName = getString(R.string.app_name);
        WebView wv = new WebView(this);
        String html = "<h1>" + String.format(getString(R.string.about_title_fmt),
                                             "<a href=\"" + getString(R.string.app_webpage_url) + "\">" + appName + "</a>") + "</h1>" +
                      "<p>" + appName + " " +
                      String.format(getString(R.string.debug_version_fmt),
                                    getVersionNumber()) + "</p>" +
                      "<p>" + String.format(getString(R.string.app_authors_fmt),
                                            getString(R.string.app_authors)) + "</p>" +
                      "<p>" + String.format(getString(R.string.app_revision_fmt),
                                            "<a href=\"" + getString(R.string.app_revision_url) + "\">" +
                                            getString(R.string.app_revision_url) + "</a></p>");
        wv.loadData(html, "text/html", "utf-8");
        new AlertDialog.Builder(this)
        .setView(wv)
        .setCancelable(true)
        .setPositiveButton(R.string.okay_action, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface d, int c)
            {
                d.dismiss();
            }
        })
        .show();
    }

    /**
     * Get current version number.
     *
     * @return String version
     */
    private String getVersionNumber()
    {
        String version = "?";
        try
        {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pi.versionName;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            //Log.e(TAG, "Package name not found", e);
        };
        return version;
    }

    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
    {
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.accounts_option, menu);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle(R.string.accounts_context_menu_title);
        getMenuInflater().inflate(R.menu.accounts_context, menu);
    }

    class AccountsAdapter extends ArrayAdapter<Account>
    {
        public AccountsAdapter(Account[] accounts)
        {
            super(Accounts.this, 0, accounts);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            Account account = getItem(position);
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

                holder.chip = view.findViewById(R.id.chip);

                view.setTag(holder);
            }
            holder.description.setText(account.getDescription());
            holder.email.setText(account.getEmail());
            if (account.getEmail().equals(account.getDescription()))
            {
                holder.email.setVisibility(View.GONE);
            }

            Integer unreadMessageCount = unreadMessageCounts.get(account.getUuid());
            if (unreadMessageCount != null)
            {
                holder.newMessageCount.setText(Integer.toString(unreadMessageCount));
                holder.newMessageCount.setVisibility(unreadMessageCount > 0 ? View.VISIBLE : View.GONE);
            }
            else
            {
                //holder.newMessageCount.setText("-");
                holder.newMessageCount.setVisibility(View.GONE);
            }
            holder.chip.setBackgroundResource(K9.COLOR_CHIP_RES_IDS[account.getAccountNumber() % K9.COLOR_CHIP_RES_IDS.length]);

            if (unreadMessageCount == null)
            {
                holder.chip.getBackground().setAlpha(0);
            }
            else if (unreadMessageCount == 0)
            {
                holder.chip.getBackground().setAlpha(127);
            }
            else
            {
                holder.chip.getBackground().setAlpha(255);
            }


            return view;
        }

        class AccountViewHolder
        {
            public TextView description;
            public TextView email;
            public TextView newMessageCount;
            public View chip;
        }
    }
}
