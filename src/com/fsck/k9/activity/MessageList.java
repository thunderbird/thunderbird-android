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
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;
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
import com.fsck.k9.activity.misc.SwipeGestureDetector.OnSwipeGestureListener;
import com.fsck.k9.activity.setup.AccountSettings;
import com.fsck.k9.activity.setup.FolderSettings;
import com.fsck.k9.activity.setup.Prefs;
import com.fsck.k9.fragment.MessageListFragment;
import com.fsck.k9.fragment.MessageListFragment.MessageListFragmentListener;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.store.StorageManager;
import com.fsck.k9.search.LocalSearch;
import com.fsck.k9.search.SearchAccount;
import com.fsck.k9.search.SearchSpecification;
import com.fsck.k9.search.SearchSpecification.Attribute;
import com.fsck.k9.search.SearchSpecification.Searchfield;
import com.fsck.k9.search.SearchSpecification.SearchCondition;

import de.cketti.library.changelog.ChangeLog;


/**
 * MessageList is the primary user interface for the program. This Activity
 * shows a list of messages.
 * From this Activity the user can perform all standard message operations.
 */
public class MessageList extends K9FragmentActivity implements MessageListFragmentListener,
        OnBackStackChangedListener, OnSwipeGestureListener {

    // for this activity
    private static final String EXTRA_SEARCH = "search";
    private static final String EXTRA_NO_THREADING = "no_threading";

    private static final String ACTION_SHORTCUT = "shortcut";
    private static final String EXTRA_SPECIAL_FOLDER = "special_folder";

    // used for remote search
    public static final String EXTRA_SEARCH_ACCOUNT = "com.fsck.k9.search_account";
    private static final String EXTRA_SEARCH_FOLDER = "com.fsck.k9.search_folder";

    public static void actionDisplaySearch(Context context, SearchSpecification search,
            boolean noThreading, boolean newTask) {
        actionDisplaySearch(context, search, noThreading, newTask, true);
    }

    public static void actionDisplaySearch(Context context, SearchSpecification search,
            boolean noThreading, boolean newTask, boolean clearTop) {
        context.startActivity(
                intentDisplaySearch(context, search, noThreading, newTask, clearTop));
    }

    public static Intent intentDisplaySearch(Context context, SearchSpecification search,
            boolean noThreading, boolean newTask, boolean clearTop) {
        Intent intent = new Intent(context, MessageList.class);
        intent.putExtra(EXTRA_SEARCH, search);
        intent.putExtra(EXTRA_NO_THREADING, noThreading);

        if (clearTop) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        if (newTask) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        return intent;
    }

    public static Intent shortcutIntent(Context context, String specialFolder) {
        Intent intent = new Intent(context, MessageList.class);
        intent.setAction(ACTION_SHORTCUT);
        intent.putExtra(EXTRA_SPECIAL_FOLDER, specialFolder);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return intent;
    }


    private StorageManager.StorageListener mStorageListener = new StorageListenerImplementation();

    private ActionBar mActionBar;
    private TextView mActionBarTitle;
    private TextView mActionBarSubTitle;
    private TextView mActionBarUnread;
    private Menu mMenu;

    private MessageListFragment mMessageListFragment;

    private Account mAccount;
    private String mFolderName;
    private LocalSearch mSearch;
    private boolean mSingleFolderMode;
    private boolean mSingleAccountMode;
    private ProgressBar mActionBarProgress;
    private MenuItem mMenuButtonCheckMail;
    private View mActionButtonIndeterminateProgress;

    /**
     * {@code true} if the message list should be displayed as flat list (i.e. no threading)
     * regardless whether or not message threading was enabled in the settings. This is used for
     * filtered views, e.g. when only displaying the unread messages in a folder.
     */
    private boolean mNoThreading;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (UpgradeDatabases.actionUpgradeDatabases(this, getIntent())) {
            finish();
            return;
        }

        setContentView(R.layout.message_list);

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
            mMessageListFragment = MessageListFragment.newInstance(mSearch, false,
                    (K9.isThreadedViewEnabled() && !mNoThreading));
            ft.add(R.id.message_list_container, mMessageListFragment);
            ft.commit();
        }

        ChangeLog cl = new ChangeLog(this);
        if (cl.isFirstRun()) {
            cl.getLogDialog().show();
        }
    }

    private void decodeExtras(Intent intent) {
        if (ACTION_SHORTCUT.equals(intent.getAction())) {
            // Handle shortcut intents
            String specialFolder = intent.getStringExtra(EXTRA_SPECIAL_FOLDER);
            if (SearchAccount.UNIFIED_INBOX.equals(specialFolder)) {
                mSearch = SearchAccount.createUnifiedInboxAccount(this).getRelatedSearch();
            } else if (SearchAccount.ALL_MESSAGES.equals(specialFolder)) {
                mSearch = SearchAccount.createAllMessagesAccount(this).getRelatedSearch();
            }
        } else if (intent.getStringExtra(SearchManager.QUERY) != null) {
            // check if this intent comes from the system search ( remote )
            if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
                //Query was received from Search Dialog
                String query = intent.getStringExtra(SearchManager.QUERY);

                mSearch = new LocalSearch(getString(R.string.search_results));
                mSearch.setManualSearch(true);
                mNoThreading = true;

                mSearch.or(new SearchCondition(Searchfield.SENDER, Attribute.CONTAINS, query));
                mSearch.or(new SearchCondition(Searchfield.SUBJECT, Attribute.CONTAINS, query));
                mSearch.or(new SearchCondition(Searchfield.MESSAGE_CONTENTS, Attribute.CONTAINS, query));

                Bundle appData = getIntent().getBundleExtra(SearchManager.APP_DATA);
                if (appData != null) {
                    mSearch.addAccountUuid(appData.getString(EXTRA_SEARCH_ACCOUNT));
                    // searches started from a folder list activity will provide an account, but no folder
                    if (appData.getString(EXTRA_SEARCH_FOLDER) != null) {
                        mSearch.addAllowedFolder(appData.getString(EXTRA_SEARCH_FOLDER));
                    }
                } else {
                    mSearch.addAccountUuid(LocalSearch.ALL_ACCOUNTS);
                }
            }
        } else {
            // regular LocalSearch object was passed
            mSearch = intent.getParcelableExtra(EXTRA_SEARCH);
            mNoThreading = intent.getBooleanExtra(EXTRA_NO_THREADING, false);
        }

        String[] accountUuids = mSearch.getAccountUuids();
        mSingleAccountMode = (accountUuids.length == 1 && !mSearch.searchAllAccounts());
        mSingleFolderMode = mSingleAccountMode && (mSearch.getFolderNames().size() == 1);

        if (mSingleAccountMode) {
            Preferences prefs = Preferences.getPreferences(getApplicationContext());
            mAccount = prefs.getAccount(accountUuids[0]);

            if (mAccount != null && !mAccount.isAvailable(this)) {
                Log.i(K9.LOG_TAG, "not opening MessageList of unavailable account");
                onAccountUnavailable();
                return;
            }
        }

        if (mSingleFolderMode) {
            mFolderName = mSearch.getFolderNames().get(0);
        }

        // now we know if we are in single account mode and need a subtitle
        mActionBarSubTitle.setVisibility((!mSingleFolderMode) ? View.GONE : View.VISIBLE);
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
        mActionBarProgress = (ProgressBar) customView.findViewById(R.id.actionbar_progress);
        mActionButtonIndeterminateProgress =
                getLayoutInflater().inflate(R.layout.actionbar_indeterminate_progress_actionview, null);

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
                    mMessageListFragment.onToggleFlagged();
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
                goBack();
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
//            case R.id.set_sort_sender: {
//                mMessageListFragment.changeSort(SortType.SORT_SENDER);
//                return true;
//            }
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
            case R.id.account_settings: {
                onEditAccount();
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

        if (!mSingleFolderMode) {
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
        mMenuButtonCheckMail= menu.findItem(R.id.check_mail);
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

            if (!mSingleAccountMode) {
                menu.findItem(R.id.expunge).setVisible(false);
                menu.findItem(R.id.check_mail).setVisible(false);
                menu.findItem(R.id.send_messages).setVisible(false);
                menu.findItem(R.id.folder_settings).setVisible(false);
                menu.findItem(R.id.account_settings).setVisible(false);
            } else {
                menu.findItem(R.id.folder_settings).setVisible(mSingleFolderMode);
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

            // If this is an explicit local search, show the option to search the cloud.
            if (!mMessageListFragment.isRemoteSearch() &&
                    mMessageListFragment.isRemoteSearchAllowed()) {
                menu.findItem(R.id.search_remote).setVisible(true);
            } else if (!mMessageListFragment.isManualSearch()) {
                menu.findItem(R.id.search).setVisible(true);
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

            Intent i = MessageView.actionViewIntent(this, messageReference, messageRefs);
            startActivity(i);
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
        LocalSearch tmpSearch = new LocalSearch("From " + senderAddress);
        tmpSearch.addAccountUuids(mSearch.getAccountUuids());
        tmpSearch.and(Searchfield.SENDER, senderAddress, Attribute.CONTAINS);

        MessageListFragment fragment = MessageListFragment.newInstance(tmpSearch, false, false);

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

    @Override
    public void showThread(Account account, String folderName, long threadRootId) {
        LocalSearch tmpSearch = new LocalSearch();
        tmpSearch.addAccountUuid(account.getUuid());
        tmpSearch.and(Searchfield.THREAD_ID, String.valueOf(threadRootId), Attribute.EQUALS);

        MessageListFragment fragment = MessageListFragment.newInstance(tmpSearch, true, false);
        addMessageListFragment(fragment, true);
    }

    @Override
    public void remoteSearchStarted() {
        // Remove action button for remote search
        configureMenu(mMenu);
    }

    @Override
    public void goBack() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else if (mMessageListFragment.isManualSearch()) {
            onBackPressed();
        } else if (!mSingleFolderMode) {
            onAccounts();
        } else {
            onShowFolderList();
        }
    }

    public void enableActionBarProgress(boolean enable) {
        if (mMenuButtonCheckMail != null && mMenuButtonCheckMail.isVisible()) {
            mActionBarProgress.setVisibility(ProgressBar.GONE);
            if (enable) {
                mMenuButtonCheckMail
                        .setActionView(mActionButtonIndeterminateProgress);
            } else {
                mMenuButtonCheckMail.setActionView(null);
            }
        } else {
            if (mMenuButtonCheckMail != null)
                mMenuButtonCheckMail.setActionView(null);
            if (enable) {
                mActionBarProgress.setVisibility(ProgressBar.VISIBLE);
            } else {
                mActionBarProgress.setVisibility(ProgressBar.GONE);
            }
        }
    }
}
