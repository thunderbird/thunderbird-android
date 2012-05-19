
package com.fsck.k9.activity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.fsck.k9.Account;
import com.fsck.k9.AccountStats;
import com.fsck.k9.BaseAccount;
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.SearchAccount;
import com.fsck.k9.SearchSpecification;
import com.fsck.k9.activity.setup.AccountSettings;
import com.fsck.k9.activity.setup.AccountSetupBasics;
import com.fsck.k9.activity.setup.Prefs;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.helper.SizeFormatter;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mail.store.StorageManager;
import com.fsck.k9.view.ColorChip;
import com.fsck.k9.preferences.StorageFormat;


public class Accounts extends K9ListActivity implements OnItemClickListener, OnClickListener {

    /**
     * Immutable empty {@link BaseAccount} array
     */
    private static final BaseAccount[] EMPTY_BASE_ACCOUNT_ARRAY = new BaseAccount[0];

    /**
     * Immutable empty {@link Flag} array
     */
    private static final Flag[] EMPTY_FLAG_ARRAY = new Flag[0];

    private static final int DIALOG_REMOVE_ACCOUNT = 1;
    private static final int DIALOG_CLEAR_ACCOUNT = 2;
    private static final int DIALOG_RECREATE_ACCOUNT = 3;
    private ConcurrentHashMap<String, AccountStats> accountStats = new ConcurrentHashMap<String, AccountStats>();

    private ConcurrentHashMap<BaseAccount, String> pendingWork = new ConcurrentHashMap<BaseAccount, String>();

    private BaseAccount mSelectedContextAccount;
    private int mUnreadMessageCount = 0;

    private AccountsHandler mHandler = new AccountsHandler();
    private AccountsAdapter mAdapter;
    private SearchAccount unreadAccount = null;
    private SearchAccount integratedInboxAccount = null;
    private FontSizes mFontSizes = K9.getFontSizes();

    private static final int ACTIVITY_REQUEST_PICK_SETTINGS_FILE = 1;

    class AccountsHandler extends Handler {
        private void setViewTitle() {
            String dispString = mListener.formatHeader(Accounts.this, getString(R.string.accounts_title), mUnreadMessageCount, getTimeFormat());

            setTitle(dispString);
        }
        public void refreshTitle() {
            runOnUiThread(new Runnable() {
                public void run() {
                    setViewTitle();
                }
            });
        }

        public void dataChanged() {
            runOnUiThread(new Runnable() {
                public void run() {
                    if (mAdapter != null) {
                        mAdapter.notifyDataSetChanged();
                    }
                }
            });
        }

        public void workingAccount(final Account account, final int res) {
            runOnUiThread(new Runnable() {
                public void run() {
                    String toastText = getString(res, account.getDescription());

                    Toast toast = Toast.makeText(getApplication(), toastText, Toast.LENGTH_SHORT);
                    toast.show();
                }
            });
        }

        public void accountSizeChanged(final Account account, final long oldSize, final long newSize) {
            runOnUiThread(new Runnable() {
                public void run() {
                    AccountStats stats = accountStats.get(account.getUuid());
                    if (newSize != -1 && stats != null && K9.measureAccounts()) {
                        stats.size = newSize;
                    }
                    String toastText = getString(R.string.account_size_changed, account.getDescription(),
                                                 SizeFormatter.formatSize(getApplication(), oldSize), SizeFormatter.formatSize(getApplication(), newSize));

                    Toast toast = Toast.makeText(getApplication(), toastText, Toast.LENGTH_LONG);
                    toast.show();
                    if (mAdapter != null) {
                        mAdapter.notifyDataSetChanged();
                    }
                }
            });
        }

        public void progress(final boolean progress) {
            runOnUiThread(new Runnable() {
                public void run() {
                    setProgressBarIndeterminateVisibility(progress);
                }
            });
        }
        public void progress(final int progress) {
            runOnUiThread(new Runnable() {
                public void run() {
                    getWindow().setFeatureInt(Window.FEATURE_PROGRESS, progress);
                }
            });
        }
    }

    public void setProgress(boolean progress) {
        mHandler.progress(progress);
    }

    ActivityListener mListener = new ActivityListener() {
        @Override
        public void informUserOfStatus() {
            mHandler.refreshTitle();
        }

        @Override
        public void folderStatusChanged(Account account, String folderName, int unreadMessageCount) {
            try {
                AccountStats stats = account.getStats(Accounts.this);
                if (stats == null) {
                    Log.w(K9.LOG_TAG, "Unable to get account stats");
                } else {
                    accountStatusChanged(account, stats);
                }
            } catch (Exception e) {
                Log.e(K9.LOG_TAG, "Unable to get account stats", e);
            }
        }
        @Override
        public void accountStatusChanged(BaseAccount account, AccountStats stats) {
            AccountStats oldStats = accountStats.get(account.getUuid());
            int oldUnreadMessageCount = 0;
            if (oldStats != null) {
                oldUnreadMessageCount = oldStats.unreadMessageCount;
            }
            if (stats == null) {
                stats = new AccountStats(); // empty stats for unavailable accounts
                stats.available = false;
            }
            accountStats.put(account.getUuid(), stats);
            if (account instanceof Account) {
                mUnreadMessageCount += stats.unreadMessageCount - oldUnreadMessageCount;
            }
            mHandler.dataChanged();
            pendingWork.remove(account);

            if (pendingWork.isEmpty()) {
                mHandler.progress(Window.PROGRESS_END);
                mHandler.refreshTitle();
            } else {
                int level = (Window.PROGRESS_END / mAdapter.getCount()) * (mAdapter.getCount() - pendingWork.size()) ;
                mHandler.progress(level);
            }
        }

        @Override
        public void accountSizeChanged(Account account, long oldSize, long newSize) {
            mHandler.accountSizeChanged(account, oldSize, newSize);
        }

        @Override
        public void synchronizeMailboxFinished(
            Account account,
            String folder,
            int totalMessagesInMailbox,
        int numNewMessages) {
            MessagingController.getInstance(getApplication()).getAccountStats(Accounts.this, account, mListener);
            super.synchronizeMailboxFinished(account, folder, totalMessagesInMailbox, numNewMessages);

            mHandler.progress(false);

        }

        @Override
        public void synchronizeMailboxStarted(Account account, String folder) {
            super.synchronizeMailboxStarted(account, folder);
            mHandler.progress(true);
        }

        @Override
        public void synchronizeMailboxFailed(Account account, String folder,
        String message) {
            super.synchronizeMailboxFailed(account, folder, message);
            mHandler.progress(false);

        }

    };

    private static String ACCOUNT_STATS = "accountStats";
    private static String SELECTED_CONTEXT_ACCOUNT = "selectedContextAccount";

    public static final String EXTRA_STARTUP = "startup";


    public static void actionLaunch(Context context) {
        Intent intent = new Intent(context, Accounts.class);
        intent.putExtra(EXTRA_STARTUP, true);
        context.startActivity(intent);
    }

    public static void listAccounts(Context context) {
        Intent intent = new Intent(context, Accounts.class);
        intent.putExtra(EXTRA_STARTUP, false);
        context.startActivity(intent);
    }

    @Override
    public void onNewIntent(Intent intent) {
        Uri uri = intent.getData();
        Log.i(K9.LOG_TAG, "Accounts Activity got uri " + uri);
        if (uri != null) {
            ContentResolver contentResolver = getContentResolver();

            Log.i(K9.LOG_TAG, "Accounts Activity got content of type " + contentResolver.getType(uri));

            String contentType = contentResolver.getType(uri);
            if (MimeUtility.K9_SETTINGS_MIME_TYPE.equals(contentType)) {
                onImport(uri);
            }
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        unreadAccount = new SearchAccount(this, false, null, null);
        unreadAccount.setDescription(getString(R.string.search_all_messages_title));
        unreadAccount.setEmail(getString(R.string.search_all_messages_detail));

        integratedInboxAccount = new SearchAccount(this, true, null,  null);
        integratedInboxAccount.setDescription(getString(R.string.integrated_inbox_title));
        integratedInboxAccount.setEmail(getString(R.string.integrated_inbox_detail));

        Account[] accounts = Preferences.getPreferences(this).getAccounts();
        Intent intent = getIntent();
        boolean startup = intent.getData() == null && intent.getBooleanExtra(EXTRA_STARTUP, true);
        onNewIntent(intent);
        if (startup && K9.startIntegratedInbox()) {
            onOpenAccount(integratedInboxAccount);
            finish();
        } else if (startup && accounts.length == 1 && onOpenAccount(accounts[0])) {
            // fall through to "else" if !onOpenAccount()
            finish();
        } else {
            requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
            requestWindowFeature(Window.FEATURE_PROGRESS);

            setContentView(R.layout.accounts);
            ListView listView = getListView();
            listView.setOnItemClickListener(this);
            listView.setItemsCanFocus(false);
            listView.setEmptyView(findViewById(R.id.empty));
            findViewById(R.id.next).setOnClickListener(this);
            registerForContextMenu(listView);

            if (icicle != null && icicle.containsKey(SELECTED_CONTEXT_ACCOUNT)) {
                String accountUuid = icicle.getString("selectedContextAccount");
                mSelectedContextAccount = Preferences.getPreferences(this).getAccount(accountUuid);
            }

            restoreAccountStats(icicle);
        }



    }

    @SuppressWarnings("unchecked")
    private void restoreAccountStats(Bundle icicle) {
        if (icicle != null) {
            Map<String, AccountStats> oldStats = (Map<String, AccountStats>)icicle.get(ACCOUNT_STATS);
            if (oldStats != null) {
                accountStats.putAll(oldStats);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSelectedContextAccount != null) {
            outState.putString(SELECTED_CONTEXT_ACCOUNT, mSelectedContextAccount.getUuid());
        }
        outState.putSerializable(ACCOUNT_STATS, accountStats);
    }

    private StorageManager.StorageListener storageListener = new StorageManager.StorageListener() {

        @Override
        public void onUnmount(String providerId) {
            refresh();
        }

        @Override
        public void onMount(String providerId) {
            refresh();
        }
    };

    @Override
    public void onResume() {
        super.onResume();

        refresh();
        MessagingController.getInstance(getApplication()).addListener(mListener);
        StorageManager.getInstance(getApplication()).addListener(storageListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        MessagingController.getInstance(getApplication()).removeListener(mListener);
        StorageManager.getInstance(getApplication()).removeListener(storageListener);

    }

    private void refresh() {
        BaseAccount[] accounts = Preferences.getPreferences(this).getAccounts();

        List<BaseAccount> newAccounts = new ArrayList<BaseAccount>(accounts.length + 4);
        if (accounts.length > 0) {
            newAccounts.add(integratedInboxAccount);
            newAccounts.add(unreadAccount);
        }

        newAccounts.addAll(Arrays.asList(accounts));

        mAdapter = new AccountsAdapter(newAccounts.toArray(EMPTY_BASE_ACCOUNT_ARRAY));
        getListView().setAdapter(mAdapter);
        if (newAccounts.size() > 0) {
            mHandler.progress(Window.PROGRESS_START);
        }
        pendingWork.clear();

        for (BaseAccount account : newAccounts) {

            if (account instanceof Account) {
                pendingWork.put(account, "true");
                Account realAccount = (Account)account;
                MessagingController.getInstance(getApplication()).getAccountStats(Accounts.this, realAccount, mListener);
            } else if (K9.countSearchMessages() && account instanceof SearchAccount) {
                pendingWork.put(account, "true");
                final SearchAccount searchAccount = (SearchAccount)account;

                MessagingController.getInstance(getApplication()).searchLocalMessages(searchAccount, null, new MessagingListener() {
                    @Override
                    public void searchStats(AccountStats stats) {
                        mListener.accountStatusChanged(searchAccount, stats);
                    }
                });
            }
        }

    }

    private void onAddNewAccount() {
        AccountSetupBasics.actionNewAccount(this);
    }

    private void onEditAccount(Account account) {
        AccountSettings.actionSettings(this, account);
    }

    private void onEditPrefs() {
        Prefs.actionPrefs(this);
    }


    /*
     * This method is called with 'null' for the argument 'account' if
     * all accounts are to be checked. This is handled accordingly in
     * MessagingController.checkMail().
     */
    private void onCheckMail(Account account) {
        MessagingController.getInstance(getApplication()).checkMail(this, account, true, true, null);
        if (account == null) {
            MessagingController.getInstance(getApplication()).sendPendingMessages(null);
        } else {
            MessagingController.getInstance(getApplication()).sendPendingMessages(account, null);
        }

    }

    private void onClearCommands(Account account) {
        MessagingController.getInstance(getApplication()).clearAllPending(account);
    }

    private void onEmptyTrash(Account account) {
        MessagingController.getInstance(getApplication()).emptyTrash(account, null);
    }


    private void onCompose() {
        Account defaultAccount = Preferences.getPreferences(this).getDefaultAccount();
        if (defaultAccount != null) {
            MessageCompose.actionCompose(this, defaultAccount);
        } else {
            onAddNewAccount();
        }
    }

    /**
     * Show that account's inbox or folder-list
     * or return false if the account is not available.
     * @param account the account to open ({@link SearchAccount} or {@link Account})
     * @return false if unsuccessfull
     */
    private boolean onOpenAccount(BaseAccount account) {
        if (account instanceof SearchAccount) {
            SearchAccount searchAccount = (SearchAccount)account;
            MessageList.actionHandle(this, searchAccount.getDescription(), searchAccount);
        } else {
            Account realAccount = (Account)account;
            if (!realAccount.isAvailable(this)) {
                String toastText = getString(R.string.account_unavailable, account.getDescription());
                Toast toast = Toast.makeText(getApplication(), toastText, Toast.LENGTH_SHORT);
                toast.show();

                Log.i(K9.LOG_TAG, "refusing to open account that is not available");
                return false;
            }
            if (K9.FOLDER_NONE.equals(realAccount.getAutoExpandFolderName())) {
                FolderList.actionHandleAccount(this, realAccount);
            } else {
                MessageList.actionHandleFolder(this, realAccount, realAccount.getAutoExpandFolderName());
            }
        }
        return true;
    }

    public void onClick(View view) {
        if (view.getId() == R.id.next) {
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
        case DIALOG_CLEAR_ACCOUNT:
            return createClearAccountDialog();
        case DIALOG_RECREATE_ACCOUNT:
            return createRecreateAccountDialog();
        }
        return super.onCreateDialog(id);
    }

    @Override
    public void onPrepareDialog(int id, Dialog d) {

        AlertDialog alert = (AlertDialog) d;
        switch (id) {
        case DIALOG_REMOVE_ACCOUNT:
            alert.setMessage(getString(R.string.account_delete_dlg_instructions_fmt,
                                       mSelectedContextAccount.getDescription()));
            break;
        case DIALOG_CLEAR_ACCOUNT:
            alert.setMessage(getString(R.string.account_clear_dlg_instructions_fmt,
                                       mSelectedContextAccount.getDescription()));
            break;
        case DIALOG_RECREATE_ACCOUNT:
            alert.setMessage(getString(R.string.account_recreate_dlg_instructions_fmt,
                                       mSelectedContextAccount.getDescription()));
            break;
        }

        super.onPrepareDialog(id, d);
    }


    private Dialog createRemoveAccountDialog() {
        return new AlertDialog.Builder(this)
               .setTitle(R.string.account_delete_dlg_title)
               .setMessage(getString(R.string.account_delete_dlg_instructions_fmt, mSelectedContextAccount.getDescription()))
        .setPositiveButton(R.string.okay_action, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dismissDialog(DIALOG_REMOVE_ACCOUNT);
                removeDialog(DIALOG_REMOVE_ACCOUNT);

                if (mSelectedContextAccount instanceof Account) {
                    Account realAccount = (Account)mSelectedContextAccount;
                    try {
                        realAccount.getLocalStore().delete();
                    } catch (Exception e) {
                        // Ignore, this may lead to localStores on sd-cards that are currently not inserted to be left
                    }
                    MessagingController.getInstance(getApplication()).notifyAccountCancel(Accounts.this, realAccount);
                    Preferences.getPreferences(Accounts.this).deleteAccount(realAccount);
                    K9.setServicesEnabled(Accounts.this);
                    refresh();
                }
            }
        })
        .setNegativeButton(R.string.cancel_action, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dismissDialog(DIALOG_REMOVE_ACCOUNT);
                removeDialog(DIALOG_REMOVE_ACCOUNT);
            }
        })
               .create();
    }

    private Dialog createClearAccountDialog() {
        return new AlertDialog.Builder(this)
               .setTitle(R.string.account_clear_dlg_title)
               .setMessage(getString(R.string.account_clear_dlg_instructions_fmt, mSelectedContextAccount.getDescription()))
        .setPositiveButton(R.string.okay_action, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dismissDialog(DIALOG_CLEAR_ACCOUNT);
                removeDialog(DIALOG_CLEAR_ACCOUNT);

                if (mSelectedContextAccount instanceof Account) {
                    Account realAccount = (Account)mSelectedContextAccount;
                    mHandler.workingAccount(realAccount, R.string.clearing_account);
                    MessagingController.getInstance(getApplication()).clear(realAccount, null);
                }
            }
        })
        .setNegativeButton(R.string.cancel_action, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dismissDialog(DIALOG_CLEAR_ACCOUNT);
                removeDialog(DIALOG_CLEAR_ACCOUNT);
            }
        })
               .create();
    }

    private Dialog createRecreateAccountDialog() {
        return new AlertDialog.Builder(this)
               .setTitle(R.string.account_recreate_dlg_title)
               .setMessage(getString(R.string.account_recreate_dlg_instructions_fmt, mSelectedContextAccount.getDescription()))
        .setPositiveButton(R.string.okay_action, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dismissDialog(DIALOG_RECREATE_ACCOUNT);
                removeDialog(DIALOG_RECREATE_ACCOUNT);

                if (mSelectedContextAccount instanceof Account) {
                    Account realAccount = (Account)mSelectedContextAccount;
                    mHandler.workingAccount(realAccount, R.string.recreating_account);
                    MessagingController.getInstance(getApplication()).recreate(realAccount, null);
                }
            }
        })
        .setNegativeButton(R.string.cancel_action, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dismissDialog(DIALOG_RECREATE_ACCOUNT);
                removeDialog(DIALOG_RECREATE_ACCOUNT);
            }
        })
               .create();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo)item.getMenuInfo();
        // submenus don't actually set the menuInfo, so the "advanced"
        // submenu wouldn't work.
        if (menuInfo != null) {
            mSelectedContextAccount = (BaseAccount)getListView().getItemAtPosition(menuInfo.position);
        }
        Account realAccount = null;
        if (mSelectedContextAccount instanceof Account) {
            realAccount = (Account)mSelectedContextAccount;
        }
        switch (item.getItemId()) {
        case R.id.delete_account:
            onDeleteAccount(realAccount);
            break;
        case R.id.edit_account:
            onEditAccount(realAccount);
            break;
        case R.id.open:
            onOpenAccount(mSelectedContextAccount);
            break;
        case R.id.check_mail:
            onCheckMail(realAccount);
            break;
        case R.id.clear_pending:
            onClearCommands(realAccount);
            break;
        case R.id.empty_trash:
            onEmptyTrash(realAccount);
            break;
        case R.id.compact:
            onCompact(realAccount);
            break;
        case R.id.clear:
            onClear(realAccount);
            break;
        case R.id.recreate:
            onRecreate(realAccount);
            break;
        case R.id.export:
            onExport(false, realAccount, false);
            break;
        }
        return true;
    }



    private void onCompact(Account account) {
        mHandler.workingAccount(account, R.string.compacting_account);
        MessagingController.getInstance(getApplication()).compact(account, null);
    }

    private void onClear(Account account) {
        showDialog(DIALOG_CLEAR_ACCOUNT);

    }
    private void onRecreate(Account account) {
        showDialog(DIALOG_RECREATE_ACCOUNT);
    }


    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        BaseAccount account = (BaseAccount)parent.getItemAtPosition(position);
        onOpenAccount(account);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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
        case R.id.export_all:
            onExport(true, null, true);
            break;
        case R.id.export_only_globals:
            onExport(true, null, false);
            break;
        case R.id.import_settings:
            onImport();
            break;
        default:
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private static String[][] USED_LIBRARIES = new String[][] {
        new String[] {"jutf7", "http://jutf7.sourceforge.net/"},
        new String[] {"JZlib", "http://www.jcraft.com/jzlib/"},
        new String[] {"Commons IO", "http://commons.apache.org/io/"},
        new String[] {"Mime4j", "http://james.apache.org/mime4j/"},
    };

    private void onAbout() {
        String appName = getString(R.string.app_name);
        String year = "2011";
        WebView wv = new WebView(this);
        StringBuilder html = new StringBuilder()
        .append("<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />")
        .append("<img src=\"file:///android_asset/icon.png\" alt=\"").append(appName).append("\"/>")
        .append("<h1>")
        .append(String.format(getString(R.string.about_title_fmt),
                              "<a href=\"" + getString(R.string.app_webpage_url)) + "\">")
        .append(appName)
        .append("</a>")
        .append("</h1><p>")
        .append(appName)
        .append(" ")
        .append(String.format(getString(R.string.debug_version_fmt), getVersionNumber()))
        .append("</p><p>")
        .append(String.format(getString(R.string.app_authors_fmt),
                              getString(R.string.app_authors)))
        .append("</p><p>")
        .append(String.format(getString(R.string.app_revision_fmt),
                              "<a href=\"" + getString(R.string.app_revision_url) + "\">" +
                              getString(R.string.app_revision_url) +
                              "</a>"))
        .append("</p><hr/><p>")
        .append(String.format(getString(R.string.app_copyright_fmt), year, year))
        .append("</p><hr/><p>")
        .append(getString(R.string.app_license))
        .append("</p><hr/><p>");

        StringBuilder libs = new StringBuilder().append("<ul>");
        for (String[] library : USED_LIBRARIES) {
            libs.append("<li><a href=\"" + library[1] + "\">" + library[0] + "</a></li>");
        }
        libs.append("</ul>");

        html.append(String.format(getString(R.string.app_libraries), libs.toString()))
        .append("</p><hr/><p>")
        .append(String.format(getString(R.string.app_emoji_icons),
                              "<div>TypePad \u7d75\u6587\u5b57\u30a2\u30a4\u30b3\u30f3\u753b\u50cf " +
                              "(<a href=\"http://typepad.jp/\">Six Apart Ltd</a>) / " +
                              "<a href=\"http://creativecommons.org/licenses/by/2.1/jp/\">CC BY 2.1</a></div>"))
        .append("</p>");

        wv.loadDataWithBaseURL("file:///android_res/drawable/", html.toString(), "text/html", "utf-8", null);
        new AlertDialog.Builder(this)
        .setView(wv)
        .setCancelable(true)
        .setPositiveButton(R.string.okay_action, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int c) {
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
    private String getVersionNumber() {
        String version = "?";
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            //Log.e(TAG, "Package name not found", e);
        }
        return version;
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

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        BaseAccount account =  mAdapter.getItem(info.position);
        if (account instanceof SearchAccount) {
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                if (item.getItemId() != R.id.open) {
                    item.setVisible(false);
                }
            }
        }
    }

    private void onImport() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(MimeUtility.K9_SETTINGS_MIME_TYPE);
        startActivityForResult(Intent.createChooser(i, null), ACTIVITY_REQUEST_PICK_SETTINGS_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(K9.LOG_TAG, "onActivityResult requestCode = " + requestCode + ", resultCode = " + resultCode + ", data = " + data);
        if (resultCode != RESULT_OK)
            return;
        if (data == null) {
            return;
        }
        switch (requestCode) {
        case ACTIVITY_REQUEST_PICK_SETTINGS_FILE:
            onImport(data.getData());
            break;
        }
    }

    private void onImport(Uri uri) {
        Log.i(K9.LOG_TAG, "onImport importing from URI " + uri.getPath());

        final String fileName = uri.getPath();
        AsyncUIProcessor.getInstance(Accounts.this.getApplication()).importSettings(this, uri, new ImportListener() {
            @Override
            public void success(int numAccounts) {
                mHandler.progress(false);
                String accountQtyText = getResources().getQuantityString(R.plurals.settings_import_success, numAccounts, numAccounts);
                String messageText = getString(R.string.settings_import_success, accountQtyText, fileName);
                showDialog(Accounts.this, R.string.settings_import_success_header, messageText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        refresh();
                    }
                });
            }

            @Override
            public void failure(String message, Exception e) {
                mHandler.progress(false);
                showDialog(Accounts.this, R.string.settings_import_failed_header, Accounts.this.getString(R.string.settings_import_failure, fileName, e.getLocalizedMessage()));
            }

            @Override
            public void canceled() {
                mHandler.progress(false);
            }

            @Override
            public void started() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mHandler.progress(true);
                        String toastText = Accounts.this.getString(R.string.settings_importing);
                        Toast toast = Toast.makeText(Accounts.this, toastText, Toast.LENGTH_SHORT);
                        toast.show();
                    }
                });

            }
        });
    }
    private void showDialog(final Context context, final int headerRes, final String message) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(headerRes);
                builder.setMessage(message);
                builder.setPositiveButton(R.string.okay_action,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.show();
            }
        });
    }

    class AccountsAdapter extends ArrayAdapter<BaseAccount> {
        public AccountsAdapter(BaseAccount[] accounts) {
            super(Accounts.this, 0, accounts);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final BaseAccount account = getItem(position);
            View view;
            if (convertView != null) {
                view = convertView;
            } else {
                view = getLayoutInflater().inflate(R.layout.accounts_item, parent, false);
            }
            AccountViewHolder holder = (AccountViewHolder) view.getTag();
            if (holder == null) {
                holder = new AccountViewHolder();
                holder.description = (TextView) view.findViewById(R.id.description);
                holder.email = (TextView) view.findViewById(R.id.email);
                holder.newMessageCount = (TextView) view.findViewById(R.id.new_message_count);
                holder.flaggedMessageCount = (TextView) view.findViewById(R.id.flagged_message_count);
                holder.activeIcons = (RelativeLayout) view.findViewById(R.id.active_icons);

                holder.chip = view.findViewById(R.id.chip);
                holder.folders = (ImageButton) view.findViewById(R.id.folders);
                holder.accountsItemLayout = (LinearLayout)view.findViewById(R.id.accounts_item_layout);

                view.setTag(holder);
            }
            AccountStats stats = accountStats.get(account.getUuid());

            if (stats != null && account instanceof Account && stats.size >= 0) {
                holder.email.setText(SizeFormatter.formatSize(Accounts.this, stats.size));
                holder.email.setVisibility(View.VISIBLE);
            } else {
                if (account.getEmail().equals(account.getDescription())) {
                    holder.email.setVisibility(View.GONE);
                } else {
                    holder.email.setVisibility(View.VISIBLE);
                    holder.email.setText(account.getEmail());
                }
            }

            String description = account.getDescription();
            if (description == null || description.length() == 0) {
                description = account.getEmail();
            }

            holder.description.setText(description);

            Integer unreadMessageCount = null;
            if (stats != null) {
                unreadMessageCount = stats.unreadMessageCount;
                holder.newMessageCount.setText(Integer.toString(unreadMessageCount));
                holder.newMessageCount.setVisibility(unreadMessageCount > 0 ? View.VISIBLE : View.GONE);

                holder.flaggedMessageCount.setText(Integer.toString(stats.flaggedMessageCount));
                holder.flaggedMessageCount.setVisibility(K9.messageListStars() && stats.flaggedMessageCount > 0 ? View.VISIBLE : View.GONE);

                holder.flaggedMessageCount.setOnClickListener(new AccountClickListener(account, SearchModifier.FLAGGED));
                holder.newMessageCount.setOnClickListener(new AccountClickListener(account, SearchModifier.UNREAD));

                view.getBackground().setAlpha(stats.available ? 0 : 127);

                holder.activeIcons.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        Toast toast = Toast.makeText(getApplication(), getString(R.string.tap_hint), Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
                                                     );

            } else {
                holder.newMessageCount.setVisibility(View.GONE);
                holder.flaggedMessageCount.setVisibility(View.GONE);
                view.getBackground().setAlpha(0);
            }
            if (account instanceof Account) {
                Account realAccount = (Account)account;

                holder.chip.setBackgroundDrawable(realAccount.generateColorChip().drawable());
                if (unreadMessageCount == null) {
                    holder.chip.getBackground().setAlpha(0);
                } else if (unreadMessageCount == 0) {
                    holder.chip.getBackground().setAlpha(127);
                } else {
                    holder.chip.getBackground().setAlpha(255);
                }

            } else {
                holder.chip.setBackgroundDrawable(new ColorChip(0xff999999).drawable());
            }


            holder.description.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mFontSizes.getAccountName());
            holder.email.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mFontSizes.getAccountDescription());

            if (K9.useCompactLayouts()) {
                holder.accountsItemLayout.setMinimumHeight(0);
            }
            if (account instanceof SearchAccount || K9.useCompactLayouts()) {

                holder.folders.setVisibility(View.GONE);
            } else {
                holder.folders.setVisibility(View.VISIBLE);
                holder.folders.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        FolderList.actionHandleAccount(Accounts.this, (Account)account);

                    }
                });
            }

            return view;
        }

        class AccountViewHolder {
            public TextView description;
            public TextView email;
            public TextView newMessageCount;
            public TextView flaggedMessageCount;
            public RelativeLayout activeIcons;
            public View chip;
            public ImageButton folders;
            public LinearLayout accountsItemLayout;
        }
    }
    private Flag[] combine(Flag[] set1, Flag[] set2) {
        if (set1 == null) {
            return set2;
        }
        if (set2 == null) {
            return set1;
        }
        Set<Flag> flags = new HashSet<Flag>();
        flags.addAll(Arrays.asList(set1));
        flags.addAll(Arrays.asList(set2));
        return flags.toArray(EMPTY_FLAG_ARRAY);
    }

    private class AccountClickListener implements OnClickListener {

        final BaseAccount account;
        final SearchModifier searchModifier;
        AccountClickListener(BaseAccount nAccount, SearchModifier nSearchModifier) {
            account = nAccount;
            searchModifier = nSearchModifier;
        }
        @Override
        public void onClick(View v) {
            String description = getString(R.string.search_title, account.getDescription(), getString(searchModifier.resId));
            if (account instanceof SearchAccount) {
                SearchAccount searchAccount = (SearchAccount)account;

                MessageList.actionHandle(Accounts.this,
                                         description, "", searchAccount.isIntegrate(),
                                         combine(searchAccount.getRequiredFlags(), searchModifier.requiredFlags),
                                         combine(searchAccount.getForbiddenFlags(), searchModifier.forbiddenFlags));
            } else {
                SearchSpecification searchSpec = new SearchSpecification() {
                    @Override
                    public String[] getAccountUuids() {
                        return new String[] { account.getUuid() };
                    }

                    @Override
                    public Flag[] getForbiddenFlags() {
                        return searchModifier.forbiddenFlags;
                    }

                    @Override
                    public String getQuery() {
                        return "";
                    }

                    @Override
                    public Flag[] getRequiredFlags() {
                        return searchModifier.requiredFlags;
                    }

                    @Override
                    public boolean isIntegrate() {
                        return false;
                    }

                    @Override
                    public String[] getFolderNames() {
                        return null;
                    }

                };
                MessageList.actionHandle(Accounts.this, description, searchSpec);
            }
        }

    }

    public void onExport(final boolean includeGlobals, final Account singleAccount, final boolean allAccounts) {
        // TODO, prompt to allow a user to choose which accounts to export
        HashSet<String> accountUuids = null;
        if (allAccounts) {
            accountUuids = new HashSet<String>();
            Account[] accounts = Preferences.getPreferences(this).getAccounts();
            for (Account account : accounts) {
                accountUuids.add(account.getUuid());
            }
        }
        else if (singleAccount != null) {
            accountUuids = new HashSet<String>();
            accountUuids.add(singleAccount.getUuid());
        }

        List<String> formats = StorageFormat.getPresentableFormats();
        Log.i(K9.LOG_TAG, "Got formats to present to user: " + formats);
       
        if (formats.size() > 1) {
            chooseFormat(formats, includeGlobals, accountUuids, allAccounts);
        }
        else if (formats.size() == 1) {
            finishExport(formats.get(0), includeGlobals, accountUuids, allAccounts);
        }
        else {
            showDialog(Accounts.this, R.string.settings_export_failed_header, Accounts.this.getString(R.string.settings_export_no_available_formats));
        }
    }
    
    private void chooseFormat(final List<String> formats, final boolean includeGlobals, final Set<String> accountUuids, final boolean allAccounts) {
        for (String format : formats) {
            Integer res = StorageFormat.getPresentableResource(format);
            if (res != null) {
                String presentableString = getString(res);
                Log.i(K9.LOG_TAG, "For format \'" + format + "\' got presentable String \"" + presentableString + "\"");
            }
        }
     // TODO: Once there are more file formats, build a UI to select which one to use.  For now, use the safe one.
        String storageFormat = StorageFormat.ENCRYPTED_BLOB;

        Log.i(K9.LOG_TAG, "Chosen format is \'" + storageFormat + "\'");
        finishExport(storageFormat, includeGlobals, accountUuids, allAccounts);
    }
    
    private void finishExport(final String storageFormat, final boolean includeGlobals, final Set<String> accountUuids, final boolean allAccounts) {
        AsyncUIProcessor.getInstance(this.getApplication()).exportSettings(this, storageFormat, includeGlobals, accountUuids, new ExportListener() {

            @Override
            public void canceled() {
                setProgress(false);
            }

            @Override
            public void failure(String message, Exception e) {
                setProgress(false);
                showDialog(Accounts.this, R.string.settings_export_failed_header, Accounts.this.getString(R.string.settings_export_failure, message));
            }

            @Override
            public void started() {
                setProgress(true);
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        String toastText = Accounts.this.getString(R.string.settings_exporting);
                        Toast toast = Toast.makeText(Accounts.this, toastText, Toast.LENGTH_SHORT);
                        toast.show();
                    }
                });
            }

            @Override
            public void success(String fileName) {
                setProgress(false);
                showDialog(Accounts.this, R.string.settings_export_success_header, Accounts.this.getString(R.string.settings_export_success, fileName));
            }

            @Override
            public void success() {
                // This one should never be called here because the AsyncUIProcessor will generate a filename
                setProgress(false);
            }
        });



    }

}
