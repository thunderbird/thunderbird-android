
package com.android.email.activity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.ContextMenu;
import android.view.KeyEvent;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.android.email.Account;
import com.android.email.Email;
import com.android.email.MessagingController;
import com.android.email.MessagingListener;
import com.android.email.Preferences;
import com.android.email.R;
import com.android.email.activity.setup.Prefs;
import com.android.email.activity.setup.AccountSettings;
import com.android.email.activity.setup.AccountSetupBasics;
import com.android.email.activity.setup.AccountSetupCheckSettings;
import com.android.email.mail.Folder;
import com.android.email.mail.MessagingException;
import com.android.email.mail.Store;
import com.android.email.mail.store.LocalStore;
import com.android.email.mail.store.LocalStore.LocalFolder;

public class Accounts extends ListActivity implements OnItemClickListener, OnClickListener {
    private static final int DIALOG_REMOVE_ACCOUNT = 1;
    private ConcurrentHashMap<String, Integer> unreadMessageCounts = new ConcurrentHashMap<String, Integer>();
    
    private ConcurrentHashMap<Account, String> pendingWork = new ConcurrentHashMap<Account, String>();
    /**
     * Key codes used to open a debug settings screen.
     */
    private static int[] secretKeyCodes = {
            KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_B, KeyEvent.KEYCODE_U,
            KeyEvent.KEYCODE_G
    };

    private int mSecretKeyCodeIndex = 0;
    private Account mSelectedContextAccount;
    
    private AccountsHandler mHandler = new AccountsHandler();
    private AccountsAdapter mAdapter;
    
    private class AccountSizeChangedHolder {
      Account account;
      long oldSize;
      long newSize;
    }
    
    class AccountsHandler extends Handler
    {
      private static final int DATA_CHANGED = 1;
      private static final int MSG_ACCOUNT_SIZE_CHANGED = 2;
      private static final int MSG_WORKING_ACCOUNT = 3;
      private static final int MSG_PROGRESS = 4;
      private static final int MSG_FOLDER_SYNCING = 5;
      private static final int MSG_DEFINITE_PROGRESS = 6;

      public void handleMessage(android.os.Message msg)
      {
        switch (msg.what)
        {
          case DATA_CHANGED:
            if (mAdapter != null)
            {
              mAdapter.notifyDataSetChanged();
            }
            break;
          case MSG_WORKING_ACCOUNT:
          {
            Account account = (Account)msg.obj;
            int res = msg.arg1;
            String toastText = getString(res, account.getDescription());
            
            Toast toast = Toast.makeText(getApplication(), toastText, Toast.LENGTH_SHORT);
            toast.show();
            break;
          }
          case MSG_ACCOUNT_SIZE_CHANGED:
          {
            AccountSizeChangedHolder holder = (AccountSizeChangedHolder)msg.obj;
            Account account = holder.account;
            Long oldSize = holder.oldSize;
            Long newSize = holder.newSize;
            String toastText = getString(R.string.account_size_changed, account.getDescription(), 
                SizeFormatter.formatSize(getApplication(), oldSize), SizeFormatter.formatSize(getApplication(), newSize));;
            
            Toast toast = Toast.makeText(getApplication(), toastText, Toast.LENGTH_LONG);
            toast.show();
            break;
          }
          case MSG_FOLDER_SYNCING:
          {
            String folderName = (String) ((Object[]) msg.obj)[0];
            String dispString;
            dispString = getString(R.string.accounts_title);
            if (folderName != null)
            {
              dispString += " (" + getString(R.string.status_loading)
              + folderName + ")";
            }
            setTitle(dispString);
            break;
          }
          case MSG_PROGRESS:
            setProgressBarIndeterminateVisibility(msg.arg1 != 0);
            //setProgressBarVisibility(msg.arg1 != 0);
            break;          
          case MSG_DEFINITE_PROGRESS:
            getWindow().setFeatureInt(Window.FEATURE_PROGRESS, msg.arg1);
              break;
          default:
            super.handleMessage(msg);
        }
      }
      
      public void dataChanged()
      {
              sendEmptyMessage(DATA_CHANGED);
      }
      
      public void workingAccount(Account account, int res)
      {
              android.os.Message msg = new android.os.Message();
              msg.what = MSG_WORKING_ACCOUNT;
              msg.obj = account;
              msg.arg1 = res;
       
              sendMessage(msg);
      }
      
      public void accountSizeChanged(Account account, long oldSize, long newSize)
      {
              android.os.Message msg = new android.os.Message();
              msg.what = MSG_ACCOUNT_SIZE_CHANGED;
              AccountSizeChangedHolder holder = new AccountSizeChangedHolder();
              holder.account = account;
              holder.oldSize = oldSize;
              holder.newSize = newSize;
              msg.obj = holder;
              sendMessage(msg);
      }
      
      public void progress(boolean progress)
      {
              android.os.Message msg = new android.os.Message();
              msg.what = MSG_PROGRESS;
              msg.arg1 = progress ? 1 : 0;
              sendMessage(msg);
      }
      public void progress(int progress)
      {
              android.os.Message msg = new android.os.Message();
              msg.what = MSG_DEFINITE_PROGRESS;
              msg.arg1 = progress ;
              sendMessage(msg);
      }
      public void folderSyncing(String folder)
      {
              android.os.Message msg = new android.os.Message();
        msg.what = MSG_FOLDER_SYNCING;
        msg.obj = new String[]
        { folder };
              sendMessage(msg);
          }

    }
    
    MessagingListener mListener = new MessagingListener() {
      @Override
      public void accountStatusChanged(Account account, int unreadMessageCount)
      {
        unreadMessageCounts.put(account.getUuid(), unreadMessageCount);
        mHandler.dataChanged();
        pendingWork.remove(account);
       

        if (pendingWork.isEmpty())
        {
          mHandler.progress(Window.PROGRESS_END);
        }
        else {
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
          int numNewMessages) {
        MessagingController.getInstance(getApplication()).getAccountUnreadCount(Accounts.this, account, mListener);

        mHandler.progress(false);
        mHandler.folderSyncing(null);
      }
      
      @Override
      public void synchronizeMailboxStarted(Account account, String folder)
      {
        mHandler.progress(true);
        mHandler.folderSyncing(account.getDescription()
            + getString(R.string.notification_bg_title_separator) + folder);
      }

            @Override
      public void synchronizeMailboxFailed(Account account, String folder,
          String message)
      {
            mHandler.progress(false);
            mHandler.folderSyncing(null);
       }

    };

    private static String UNREAD_MESSAGE_COUNTS = "unreadMessageCounts";
    private static String SELECTED_CONTEXT_ACCOUNT = "selectedContextAccount";
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        
        setContentView(R.layout.accounts);
        ListView listView = getListView();
        listView.setOnItemClickListener(this);
        listView.setItemsCanFocus(false);
        listView.setEmptyView(findViewById(R.id.empty));
        findViewById(R.id.add_new_account).setOnClickListener(this);
        registerForContextMenu(listView);

        if (icicle != null && icicle.containsKey(SELECTED_CONTEXT_ACCOUNT)) {
            mSelectedContextAccount = (Account) icicle.getSerializable("selectedContextAccount");
        }
        
        if (icicle != null) {
          Map<String, Integer> oldUnreadMessageCounts = (Map<String, Integer>)icicle.get(UNREAD_MESSAGE_COUNTS);
          if (oldUnreadMessageCounts != null) {
            unreadMessageCounts.putAll(oldUnreadMessageCounts);
          }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSelectedContextAccount != null) {
            outState.putSerializable(SELECTED_CONTEXT_ACCOUNT, mSelectedContextAccount);
        }
        outState.putSerializable(UNREAD_MESSAGE_COUNTS, unreadMessageCounts);
    }

    @Override
    public void onResume() {
        super.onResume();

        refresh();
        MessagingController.getInstance(getApplication()).addListener(mListener);
    }
    
    @Override
    public void onPause() {
      super.onPause();
      MessagingController.getInstance(getApplication()).removeListener(mListener);
    }

    private void refresh() {
        Account[] accounts = Preferences.getPreferences(this).getAccounts();
        mAdapter = new AccountsAdapter(accounts);
        getListView().setAdapter(mAdapter);
        if (accounts.length > 0) {
          mHandler.progress(Window.PROGRESS_START);
        }
        
        for (Account account : accounts) {
          MessagingController.getInstance(getApplication()).getAccountUnreadCount(Accounts.this, account, mListener);
          pendingWork.put(account, "true");
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


    private void onCheckMail(Account account) {
        MessagingController.getInstance(getApplication()).checkMail(this, account, true, true, null);
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
        }
        else {
            onAddNewAccount();
        }
    }

    private void onOpenAccount(Account account) {
      FolderList.actionHandleAccount(this, account);
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
            .setMessage(getString(R.string.account_delete_dlg_instructions_fmt, mSelectedContextAccount.getDescription()))
            .setPositiveButton(R.string.okay_action, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dismissDialog(DIALOG_REMOVE_ACCOUNT);
                    try {
                        ((LocalStore)Store.getInstance( mSelectedContextAccount.getLocalStoreUri(), getApplication())).delete();
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
            case R.id.edit_prefs:
                onEditPrefs();
                break;
            case R.id.delete_account:
                onDeleteAccount(account);
                break;
            case R.id.edit_account:
                onEditAccount(account);
                break;
            case R.id.open:
                onOpenAccount(account);
                break;
            case R.id.check_mail:
              onCheckMail(account);
              break;
            case R.id.clear_pending:
              onClearCommands(account);
              break;
            case R.id.empty_trash:
              onEmptyTrash(account);
              break;
            case R.id.compact:
              onCompact(account);
              break;
            case R.id.clear:
              onClear(account);
              break;
        }
        return true;
    }
    
    private void onCompact(Account account) {
      mHandler.workingAccount(account, R.string.compacting_account);
      MessagingController.getInstance(getApplication()).compact(account, null);
    }
    
    private void onClear(Account account) {
      mHandler.workingAccount(account, R.string.clearing_account);
      MessagingController.getInstance(getApplication()).clear(account, null);
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
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void onAbout() {
    	String appName = getString(R.string.app_name);
    	WebView wv = new WebView(this);
    	String html = "<h1>" + String.format(getString(R.string.about_title_fmt).toString(),
        		"<a href=\"" + getString(R.string.app_webpage_url) + "\">" + appName + "</a>") + "</h1>" +
    		"<p>" + appName + " " + 
		String.format(getString(R.string.debug_version_fmt).toString(),
				getVersionNumber()) + "</p>" +
				"<p>" + String.format(getString(R.string.app_authors_fmt).toString(), 
						getString(R.string.app_authors)) + "</p>" +
				"<p>" + String.format(getString(R.string.app_revision_fmt).toString(), 
						"<a href=\"" + getString(R.string.app_revision_url) + "\">" + 
						getString(R.string.app_revision_url) + "</a></p>");
    	wv.loadData(html, "text/html", "utf-8");
    	new AlertDialog.Builder(this)
        .setView(wv)
        .setCancelable(true)
        .setPositiveButton(R.string.okay_action, new DialogInterface.OnClickListener () {
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
            };
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

            Integer unreadMessageCount = unreadMessageCounts.get(account.getUuid());
            if (unreadMessageCount != null) {
              holder.newMessageCount.setText(Integer.toString(unreadMessageCount));
              holder.newMessageCount.setVisibility(unreadMessageCount > 0 ? View.VISIBLE : View.GONE);
            }
            else {
              //holder.newMessageCount.setText("-");
              holder.newMessageCount.setVisibility(View.GONE); 
            }
            
            return view;
        }

        class AccountViewHolder {
            public TextView description;
            public TextView email;
            public TextView newMessageCount;
        }
    }
}


