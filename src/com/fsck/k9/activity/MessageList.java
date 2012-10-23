package com.fsck.k9.activity;

import java.util.ArrayList;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.fsck.k9.Account;
import com.fsck.k9.Account.SortType;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.SearchSpecification;
import com.fsck.k9.activity.misc.SwipeGestureDetector.OnSwipeGestureListener;
import com.fsck.k9.activity.setup.AccountSettings;
import com.fsck.k9.activity.setup.FolderSettings;
import com.fsck.k9.activity.setup.Prefs;
import com.fsck.k9.fragment.MessageListFragment;
import com.fsck.k9.fragment.MessageListFragment.MessageListFragmentListener;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.store.StorageManager;


/**
 * MessageList is the primary user interface for the program. This Activity
 * shows a list of messages.
 * From this Activity the user can perform all standard message operations.
 */
public class MessageList extends K9FragmentActivity implements MessageListFragmentListener,
        OnBackStackChangedListener, OnSwipeGestureListener {
    private static final String EXTRA_ACCOUNT = "account";
    private static final String EXTRA_FOLDER  = "folder";
    private static final String EXTRA_SEARCH_ACCOUNT = "com.fsck.k9.search_account";
    private static final String EXTRA_SEARCH_FOLDER = "com.fsck.k9.search_folder";
    private static final String EXTRA_QUERY_FLAGS = "queryFlags";
    private static final String EXTRA_FORBIDDEN_FLAGS = "forbiddenFlags";
    private static final String EXTRA_INTEGRATE = "integrate";
    private static final String EXTRA_ACCOUNT_UUIDS = "accountUuids";
    private static final String EXTRA_FOLDER_NAMES = "folderNames";
    private static final String EXTRA_TITLE = "title";


    public static void actionHandleFolder(Context context, Account account, String folder) {
        Intent intent = new Intent(context, MessageList.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(EXTRA_ACCOUNT, account.getUuid());

        if (folder != null) {
            intent.putExtra(EXTRA_FOLDER, folder);
        }
        context.startActivity(intent);
    }

    public static Intent actionHandleFolderIntent(Context context, Account account, String folder) {
        Intent intent = new Intent(context, MessageList.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(EXTRA_ACCOUNT, account.getUuid());

        if (folder != null) {
            intent.putExtra(EXTRA_FOLDER, folder);
        }
        return intent;
    }

    public static void actionHandle(Context context, String title, String queryString, boolean integrate, Flag[] flags, Flag[] forbiddenFlags) {
        Intent intent = new Intent(context, MessageList.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(SearchManager.QUERY, queryString);
        if (flags != null) {
            intent.putExtra(EXTRA_QUERY_FLAGS, Utility.combine(flags, ','));
        }
        if (forbiddenFlags != null) {
            intent.putExtra(EXTRA_FORBIDDEN_FLAGS, Utility.combine(forbiddenFlags, ','));
        }
        intent.putExtra(EXTRA_INTEGRATE, integrate);
        intent.putExtra(EXTRA_TITLE, title);
        context.startActivity(intent);
    }

    /**
     * Creates and returns an intent that opens Unified Inbox or All Messages screen.
     */
    public static Intent actionHandleAccountIntent(Context context, String title,
            SearchSpecification searchSpecification) {
        Intent intent = new Intent(context, MessageList.class);
        intent.putExtra(SearchManager.QUERY, searchSpecification.getQuery());
        if (searchSpecification.getRequiredFlags() != null) {
            intent.putExtra(EXTRA_QUERY_FLAGS, Utility.combine(searchSpecification.getRequiredFlags(), ','));
        }
        if (searchSpecification.getForbiddenFlags() != null) {
            intent.putExtra(EXTRA_FORBIDDEN_FLAGS, Utility.combine(searchSpecification.getForbiddenFlags(), ','));
        }
        intent.putExtra(EXTRA_INTEGRATE, searchSpecification.isIntegrate());
        intent.putExtra(EXTRA_ACCOUNT_UUIDS, searchSpecification.getAccountUuids());
        intent.putExtra(EXTRA_FOLDER_NAMES, searchSpecification.getFolderNames());
        intent.putExtra(EXTRA_TITLE, title);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        return intent;
    }

    public static void actionHandle(Context context, String title,
                                    SearchSpecification searchSpecification) {
        Intent intent = actionHandleAccountIntent(context, title, searchSpecification);
        context.startActivity(intent);
    }


    private StorageManager.StorageListener mStorageListener = new StorageListenerImplementation();

    private ActionBar mActionBar;
    private TextView mActionBarTitle;
    private TextView mActionBarSubTitle;
    private TextView mActionBarUnread;
    private String mTitle;
    private Menu mMenu;

    private MessageListFragment mMessageListFragment;

    private Account mAccount;
    private String mQueryString;
    private String mFolderName;
    private Flag[] mQueryFlags;
    private Flag[] mForbiddenFlags;
    private String mSearchAccount = null;
    private String mSearchFolder = null;
    private boolean mIntegrate;
    private String[] mAccountUuids;
    private String[] mFolderNames;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.message_list);

        // need this for actionbar initialization
        mQueryString = getIntent().getStringExtra(SearchManager.QUERY);

        mActionBar = getSupportActionBar();
        initializeActionBar();

        // Enable gesture detection for MessageLists
        setupGestureDetector(this);

        decodeExtras(getIntent());

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.addOnBackStackChangedListener(this);

        mMessageListFragment = (MessageListFragment) fragmentManager.findFragmentById(R.id.message_list_container);

        if (mMessageListFragment == null) {
            FragmentTransaction ft = fragmentManager.beginTransaction();
            if (mQueryString == null) {
                mMessageListFragment = MessageListFragment.newInstance(mAccount, mFolderName);
            } else if (mSearchAccount != null) {
                mMessageListFragment = MessageListFragment.newInstance(mSearchAccount,
                        mSearchFolder, mQueryString, false);
            } else {
                mMessageListFragment = MessageListFragment.newInstance(mTitle, mAccountUuids,
                        mFolderNames, mQueryString, mQueryFlags, mForbiddenFlags, mIntegrate);
            }
            ft.add(R.id.message_list_container, mMessageListFragment);
            ft.commit();
        }
    }

    private void decodeExtras(Intent intent) {
        mQueryString = intent.getStringExtra(SearchManager.QUERY);
        mFolderName = null;
        mSearchAccount = null;
        mSearchFolder = null;
        if (mQueryString != null) {
            if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
                //Query was received from Search Dialog
                Bundle appData = getIntent().getBundleExtra(SearchManager.APP_DATA);
                if (appData != null) {
                    mSearchAccount = appData.getString(EXTRA_SEARCH_ACCOUNT);
                    mSearchFolder = appData.getString(EXTRA_SEARCH_FOLDER);
                }
            } else {
                mSearchAccount = intent.getStringExtra(EXTRA_SEARCH_ACCOUNT);
                mSearchFolder = intent.getStringExtra(EXTRA_SEARCH_FOLDER);
            }
        }

        String accountUuid = intent.getStringExtra(EXTRA_ACCOUNT);
        mFolderName = intent.getStringExtra(EXTRA_FOLDER);

        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);

        if (mAccount != null && !mAccount.isAvailable(this)) {
            Log.i(K9.LOG_TAG, "not opening MessageList of unavailable account");
            onAccountUnavailable();
            return;
        }

        String queryFlags = intent.getStringExtra(EXTRA_QUERY_FLAGS);
        if (queryFlags != null) {
            String[] flagStrings = queryFlags.split(",");
            mQueryFlags = new Flag[flagStrings.length];
            for (int i = 0; i < flagStrings.length; i++) {
                mQueryFlags[i] = Flag.valueOf(flagStrings[i]);
            }
        }
        String forbiddenFlags = intent.getStringExtra(EXTRA_FORBIDDEN_FLAGS);
        if (forbiddenFlags != null) {
            String[] flagStrings = forbiddenFlags.split(",");
            mForbiddenFlags = new Flag[flagStrings.length];
            for (int i = 0; i < flagStrings.length; i++) {
                mForbiddenFlags[i] = Flag.valueOf(flagStrings[i]);
            }
        }
        mIntegrate = intent.getBooleanExtra(EXTRA_INTEGRATE, false);
        mAccountUuids = intent.getStringArrayExtra(EXTRA_ACCOUNT_UUIDS);
        mFolderNames = intent.getStringArrayExtra(EXTRA_FOLDER_NAMES);
        mTitle = intent.getStringExtra(EXTRA_TITLE);

        // Take the initial folder into account only if we are *not* restoring
        // the activity already.
        if (mFolderName == null && mQueryString == null) {
            mFolderName = mAccount.getAutoExpandFolderName();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        StorageManager.getInstance(getApplication()).removeListener(mStorageListener);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!(this instanceof Search)) {
            //necessary b/c no guarantee Search.onStop will be called before MessageList.onResume
            //when returning from search results
            Search.setActive(false);
        }

        if (mAccount != null && !mAccount.isAvailable(this)) {
            onAccountUnavailable();
            return;
        }
        StorageManager.getInstance(getApplication()).addListener(mStorageListener);
    }

    private void initializeActionBar() {
        mActionBar.setDisplayShowCustomEnabled(true);
        mActionBar.setCustomView(R.layout.actionbar_custom);

        View customView = mActionBar.getCustomView();
        mActionBarTitle = (TextView) customView.findViewById(R.id.actionbar_title_first);
        mActionBarSubTitle = (TextView) customView.findViewById(R.id.actionbar_title_sub);
        mActionBarUnread = (TextView) customView.findViewById(R.id.actionbar_unread_count);

        if (mQueryString != null) {
            mActionBarSubTitle.setVisibility(View.GONE);
        }

        mActionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Shortcuts that work no matter what is selected
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP: {
                if (K9.useVolumeKeysForListNavigationEnabled()) {
                    mMessageListFragment.onMoveUp();
                    return true;
                }
                return false;
            }
            case KeyEvent.KEYCODE_VOLUME_DOWN: {
                if (K9.useVolumeKeysForListNavigationEnabled()) {
                    mMessageListFragment.onMoveDown();
                    return true;
                }
                return false;
            }
            case KeyEvent.KEYCODE_C: {
                mMessageListFragment.onCompose();
                return true;
            }
            case KeyEvent.KEYCODE_Q: {
                onShowFolderList();
                return true;
            }
            case KeyEvent.KEYCODE_O: {
                mMessageListFragment.onCycleSort();
                return true;
            }
            case KeyEvent.KEYCODE_I: {
                mMessageListFragment.onReverseSort();
                return true;
            }
            case KeyEvent.KEYCODE_H: {
                Toast toast = Toast.makeText(this, R.string.message_list_help_key, Toast.LENGTH_LONG);
                toast.show();
                return true;
            }
        }

        boolean retval = true;
        try {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DEL:
                case KeyEvent.KEYCODE_D: {
                    mMessageListFragment.onDelete();
                    return true;
                }
                case KeyEvent.KEYCODE_S: {
                    mMessageListFragment.toggleMessageSelect();
                    return true;
                }
                case KeyEvent.KEYCODE_G: {
                    mMessageListFragment.onToggleFlag();
                    return true;
                }
                case KeyEvent.KEYCODE_M: {
                    mMessageListFragment.onMove();
                    return true;
                }
                case KeyEvent.KEYCODE_V: {
                    mMessageListFragment.onArchive();
                    return true;
                }
                case KeyEvent.KEYCODE_Y: {
                    mMessageListFragment.onCopy();
                    return true;
                }
                case KeyEvent.KEYCODE_Z: {
                    mMessageListFragment.onToggleRead();
                    return true;
                }
            }
        } finally {
            retval = super.onKeyDown(keyCode, event);
        }
        return retval;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Swallow these events too to avoid the audible notification of a volume change
        if (K9.useVolumeKeysForListNavigationEnabled()) {
            if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP) || (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
                if (K9.DEBUG)
                    Log.v(K9.LOG_TAG, "Swallowed key up.");
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    private void onAccounts() {
        Accounts.listAccounts(this);
        finish();
    }

    private void onShowFolderList() {
        FolderList.actionHandleAccount(this, mAccount);
        finish();
    }

    private void onEditPrefs() {
        Prefs.actionPrefs(this);
    }

    private void onEditAccount() {
        AccountSettings.actionSettings(this, mAccount);
    }

    @Override
    public boolean onSearchRequested() {
        return mMessageListFragment.onSearchRequested();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home: {
                FragmentManager fragmentManager = getSupportFragmentManager();
                if (fragmentManager.getBackStackEntryCount() > 0) {
                    fragmentManager.popBackStack();
                } else if (mIntegrate) {
                    // If we were in one of the integrated mailboxes (think All Mail or Integrated Inbox), then
                    // go to accounts.
                    onAccounts();
                } else if (mQueryString != null) {
                    // We did a search of some sort.  Go back to wherever the user searched from.
                    onBackPressed();
                } else {
                    // In a standard message list of a folder.  Go to folder list.
                    onShowFolderList();
                }
                return true;
            }
            case R.id.compose: {
                mMessageListFragment.onCompose();
                return true;
            }
            case R.id.check_mail: {
                mMessageListFragment.checkMail();
                return true;
            }
            case R.id.set_sort_date: {
                mMessageListFragment.changeSort(SortType.SORT_DATE);
                return true;
            }
            case R.id.set_sort_arrival: {
                mMessageListFragment.changeSort(SortType.SORT_ARRIVAL);
                return true;
            }
            case R.id.set_sort_subject: {
                mMessageListFragment.changeSort(SortType.SORT_SUBJECT);
                return true;
            }
            case R.id.set_sort_sender: {
                mMessageListFragment.changeSort(SortType.SORT_SENDER);
                return true;
            }
            case R.id.set_sort_flag: {
                mMessageListFragment.changeSort(SortType.SORT_FLAGGED);
                return true;
            }
            case R.id.set_sort_unread: {
                mMessageListFragment.changeSort(SortType.SORT_UNREAD);
                return true;
            }
            case R.id.set_sort_attach: {
                mMessageListFragment.changeSort(SortType.SORT_ATTACHMENT);
                return true;
            }
            case R.id.select_all: {
                mMessageListFragment.selectAll();
                return true;
            }
            case R.id.app_settings: {
                onEditPrefs();
                return true;
            }
            case R.id.search: {
                mMessageListFragment.onSearchRequested();
                return true;
            }
            case R.id.search_remote: {
                mMessageListFragment.onRemoteSearch();
                return true;
            }
        }

        if (mQueryString != null) {
            // None of the options after this point are "safe" for search results
            //TODO: This is not true for "unread" and "starred" searches in regular folders
            return false;
        }

        switch (itemId) {
            case R.id.send_messages: {
                mMessageListFragment.onSendPendingMessages();
                return true;
            }
            case R.id.folder_settings: {
                if (mFolderName != null) {
                    FolderSettings.actionSettings(this, mAccount, mFolderName);
                }
                return true;
            }
            case R.id.account_settings: {
                onEditAccount();
                return true;
            }
            case R.id.expunge: {
                mMessageListFragment.onExpunge();
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.message_list_option, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        configureMenu(menu);
        return true;
    }

    private void configureMenu(Menu menu) {
        if (menu == null) {
            return;
        }

        menu.findItem(R.id.search).setVisible(false);
        menu.findItem(R.id.search_remote).setVisible(false);

        if (mMessageListFragment == null) {
            // Hide everything (except "compose") if no MessageListFragment instance is available
            menu.findItem(R.id.check_mail).setVisible(false);
            menu.findItem(R.id.set_sort).setVisible(false);
            menu.findItem(R.id.select_all).setVisible(false);
            menu.findItem(R.id.send_messages).setVisible(false);
            menu.findItem(R.id.expunge).setVisible(false);
            menu.findItem(R.id.settings).setVisible(false);
        } else {
            menu.findItem(R.id.set_sort).setVisible(true);
            menu.findItem(R.id.select_all).setVisible(true);
            menu.findItem(R.id.settings).setVisible(true);

            if (mMessageListFragment.isSearchQuery()) {
                menu.findItem(R.id.expunge).setVisible(false);
                menu.findItem(R.id.check_mail).setVisible(false);
                menu.findItem(R.id.send_messages).setVisible(false);
                menu.findItem(R.id.folder_settings).setVisible(false);
                menu.findItem(R.id.account_settings).setVisible(false);

                // If this is an explicit local search, show the option to search the cloud.
                if (!mMessageListFragment.isRemoteSearch() &&
                        mMessageListFragment.isRemoteSearchAllowed()) {
                    menu.findItem(R.id.search_remote).setVisible(true);
                }

            } else {
                menu.findItem(R.id.search).setVisible(true);
                menu.findItem(R.id.folder_settings).setVisible(true);
                menu.findItem(R.id.account_settings).setVisible(true);

                if (mMessageListFragment.isOutbox()) {
                    menu.findItem(R.id.send_messages).setVisible(true);
                } else {
                    menu.findItem(R.id.send_messages).setVisible(false);
                }

                if (mMessageListFragment.isRemoteFolder()) {
                    menu.findItem(R.id.check_mail).setVisible(true);
                    menu.findItem(R.id.expunge).setVisible(mMessageListFragment.isAccountExpungeCapable());
                } else {
                    menu.findItem(R.id.check_mail).setVisible(false);
                    menu.findItem(R.id.expunge).setVisible(false);
                }
            }
        }
    }

    protected void onAccountUnavailable() {
        finish();
        // TODO inform user about account unavailability using Toast
        Accounts.listAccounts(this);
    }

    public void setActionBarTitle(String title) {
        mActionBarTitle.setText(title);
    }

    public void setActionBarSubTitle(String subTitle) {
        mActionBarSubTitle.setText(subTitle);
    }

    public void setActionBarUnread(int unread) {
        if (unread == 0) {
            mActionBarUnread.setVisibility(View.GONE);
        } else {
            mActionBarUnread.setVisibility(View.VISIBLE);
            mActionBarUnread.setText(Integer.toString(unread));
        }
    }

    @Override
    public void setMessageListTitle(String title) {
        setActionBarTitle(title);
    }

    @Override
    public void setMessageListSubTitle(String subTitle) {
        setActionBarSubTitle(subTitle);
    }

    @Override
    public void setUnreadCount(int unread) {
        setActionBarUnread(unread);
    }

    @Override
    public void setMessageListProgress(int progress) {
        setSupportProgress(progress);
    }

    @Override
    public void openMessage(MessageReference messageReference) {
        Preferences prefs = Preferences.getPreferences(getApplicationContext());
        Account account = prefs.getAccount(messageReference.accountUuid);
        String folderName = messageReference.folderName;

        if (folderName.equals(account.getDraftsFolderName())) {
            MessageCompose.actionEditDraft(this, messageReference);
        } else {
            ArrayList<MessageReference> messageRefs = mMessageListFragment.getMessageReferences();

            Log.i(K9.LOG_TAG, "MessageList sending message " + messageReference);

            MessageView.actionView(this, messageReference, messageRefs, getIntent().getExtras());
        }

        /*
         * We set read=true here for UI performance reasons. The actual value
         * will get picked up on the refresh when the Activity is resumed but
         * that may take a second or so and we don't want this to show and
         * then go away. I've gone back and forth on this, and this gives a
         * better UI experience, so I am putting it back in.
         */
//        if (!message.read) {
//            message.read = true;
//        }
    }

    @Override
    public void onResendMessage(Message message) {
        MessageCompose.actionEditDraft(this, message.makeMessageReference());
    }

    @Override
    public void onForward(Message message) {
        MessageCompose.actionForward(this, message.getFolder().getAccount(), message, null);
    }

    @Override
    public void onReply(Message message) {
        MessageCompose.actionReply(this, message.getFolder().getAccount(), message, false, null);
    }

    @Override
    public void onReplyAll(Message message) {
        MessageCompose.actionReply(this, message.getFolder().getAccount(), message, true, null);
    }

    @Override
    public void onCompose(Account account) {
        MessageCompose.actionCompose(this, account);
    }

    @Override
    public void showMoreFromSameSender(String senderAddress) {
        MessageListFragment fragment = MessageListFragment.newInstance("From " + senderAddress,
                null, null, senderAddress, null, null, false);

        addMessageListFragment(fragment, true);
    }

    @Override
    public void onBackStackChanged() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        mMessageListFragment = (MessageListFragment) fragmentManager.findFragmentById(
                R.id.message_list_container);

        configureMenu(mMenu);
    }

    @Override
    public void onSwipeRightToLeft(MotionEvent e1, MotionEvent e2) {
        if (mMessageListFragment != null) {
            mMessageListFragment.onSwipeRightToLeft(e1, e2);
        }
    }

    @Override
    public void onSwipeLeftToRight(MotionEvent e1, MotionEvent e2) {
        if (mMessageListFragment != null) {
            mMessageListFragment.onSwipeLeftToRight(e1, e2);
        }
    }

    private final class StorageListenerImplementation implements StorageManager.StorageListener {
        @Override
        public void onUnmount(String providerId) {
            if (mAccount != null && providerId.equals(mAccount.getLocalStorageProviderId())) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onAccountUnavailable();
                    }
                });
            }
        }

        @Override
        public void onMount(String providerId) {
            // no-op
        }
    }

    public void remoteSearch(String searchAccount, String searchFolder, String queryString) {
        MessageListFragment fragment = MessageListFragment.newInstance(searchAccount, searchFolder,
                queryString, true);
        mMenu.findItem(R.id.search_remote).setVisible(false);
        addMessageListFragment(fragment, false);
    }

    private void addMessageListFragment(MessageListFragment fragment, boolean addToBackStack) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        ft.replace(R.id.message_list_container, fragment);
        if (addToBackStack)
            ft.addToBackStack(null);

        mMessageListFragment = fragment;
        ft.commit();
    }

    @Override
    public boolean startSearch(Account account, String folderName) {
        // If this search was started from a MessageList of a single folder, pass along that folder info
        // so that we can enable remote search.
        if (account != null && folderName != null) {
            final Bundle appData = new Bundle();
            appData.putString(EXTRA_SEARCH_ACCOUNT, account.getUuid());
            appData.putString(EXTRA_SEARCH_FOLDER, folderName);
            startSearch(null, false, appData, false);
        } else {
            // TODO Handle the case where we're searching from within a search result.
            startSearch(null, false, null, false);
        }

        return true;
    }
}
