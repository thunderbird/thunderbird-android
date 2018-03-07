package com.fsck.k9.activity;


import java.util.Collection;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.FragmentManager;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import timber.log.Timber;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.Account.SortType;
import com.fsck.k9.K9;
import com.fsck.k9.K9.SplitViewMode;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.compose.MessageActions;
import com.fsck.k9.activity.misc.SwipeGestureDetector.OnSwipeGestureListener;
import com.fsck.k9.activity.setup.AccountSettings;
import com.fsck.k9.activity.setup.FolderSettings;
import com.fsck.k9.activity.setup.Prefs;
import com.fsck.k9.fragment.MessageListFragment;
import com.fsck.k9.fragment.MessageListFragment.MessageListFragmentListener;
import com.fsck.k9.helper.ParcelableUtil;
import com.fsck.k9.mailstore.StorageManager;
import com.fsck.k9.preferences.StorageEditor;
import com.fsck.k9.search.LocalSearch;
import com.fsck.k9.search.SearchAccount;
import com.fsck.k9.search.SearchSpecification;
import com.fsck.k9.search.SearchSpecification.Attribute;
import com.fsck.k9.search.SearchSpecification.SearchCondition;
import com.fsck.k9.search.SearchSpecification.SearchField;
import com.fsck.k9.ui.messageview.MessageViewFragment;
import com.fsck.k9.ui.messageview.MessageViewFragment.MessageViewFragmentListener;
import com.fsck.k9.view.MessageHeader;
import com.fsck.k9.view.MessageTitleView;
import com.fsck.k9.view.ViewSwitcher;
import com.fsck.k9.view.ViewSwitcher.OnSwitchCompleteListener;
import de.cketti.library.changelog.ChangeLog;


/**
 * MessageList is the primary user interface for the program. This Activity
 * shows a list of messages.
 * From this Activity the user can perform all standard message operations.
 */
public class MessageList extends K9Activity implements MessageListFragmentListener,
        MessageViewFragmentListener, OnBackStackChangedListener, OnSwipeGestureListener,
        OnSwitchCompleteListener {

    private static final String EXTRA_SEARCH = "search_bytes";
    private static final String EXTRA_NO_THREADING = "no_threading";

    private static final String ACTION_SHORTCUT = "shortcut";
    private static final String EXTRA_SPECIAL_FOLDER = "special_folder";

    private static final String EXTRA_MESSAGE_REFERENCE = "message_reference";

    // used for remote search
    public static final String EXTRA_SEARCH_ACCOUNT = "com.fsck.k9.search_account";
    private static final String EXTRA_SEARCH_FOLDER = "com.fsck.k9.search_folder";

    private static final String STATE_DISPLAY_MODE = "displayMode";
    private static final String STATE_MESSAGE_LIST_WAS_DISPLAYED = "messageListWasDisplayed";
    private static final String STATE_FIRST_BACK_STACK_ID = "firstBackstackId";

    // Used for navigating to next/previous message
    private static final int PREVIOUS = 1;
    private static final int NEXT = 2;

    public static final int REQUEST_MASK_PENDING_INTENT = 1 << 15;

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
        intent.putExtra(EXTRA_SEARCH, ParcelableUtil.marshall(search));
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

    public static Intent actionDisplayMessageIntent(Context context,
            MessageReference messageReference) {
        Intent intent = new Intent(context, MessageList.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(EXTRA_MESSAGE_REFERENCE, messageReference.toIdentityString());
        return intent;
    }


    private enum DisplayMode {
        MESSAGE_LIST,
        MESSAGE_VIEW,
        SPLIT_VIEW
    }


    private StorageManager.StorageListener mStorageListener = new StorageListenerImplementation();

    private ActionBar actionBar;
    private View actionBarMessageList;
    private View actionBarMessageView;
    private MessageTitleView actionBarSubject;
    private TextView actionBarTitle;
    private TextView actionBarSubTitle;
    private TextView actionBarUnread;
    private Menu menu;

    private ViewGroup messageViewContainer;
    private View messageViewPlaceHolder;

    private MessageListFragment messageListFragment;
    private MessageViewFragment messageViewFragment;
    private int firstBackStackId = -1;

    private Account account;
    private String folderName;
    private LocalSearch search;
    private boolean singleFolderMode;
    private boolean singleAccountMode;

    private ProgressBar actionBarProgress;
    private MenuItem menuButtonCheckMail;
    private View actionButtonIndeterminateProgress;
    private int lastDirection = (K9.messageViewShowNext()) ? NEXT : PREVIOUS;

    /**
     * {@code true} if the message list should be displayed as flat list (i.e. no threading)
     * regardless whether or not message threading was enabled in the settings. This is used for
     * filtered views, e.g. when only displaying the unread messages in a folder.
     */
    private boolean noThreading;

    private DisplayMode displayMode;
    private MessageReference messageReference;

    /**
     * {@code true} when the message list was displayed once. This is used in
     * {@link #onBackPressed()} to decide whether to go from the message view to the message list or
     * finish the activity.
     */
    private boolean messageListWasDisplayed = false;
    private ViewSwitcher viewSwitcher;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (UpgradeDatabases.actionUpgradeDatabases(this, getIntent())) {
            finish();
            return;
        }

        if (useSplitView()) {
            setContentView(R.layout.split_message_list);
        } else {
            setContentView(R.layout.message_list);
            viewSwitcher = (ViewSwitcher) findViewById(R.id.container);
            viewSwitcher.setFirstInAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_left));
            viewSwitcher.setFirstOutAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_out_right));
            viewSwitcher.setSecondInAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_right));
            viewSwitcher.setSecondOutAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_out_left));
            viewSwitcher.setOnSwitchCompleteListener(this);
        }

        initializeActionBar();

        // Enable gesture detection for MessageLists
        setupGestureDetector(this);

        if (!decodeExtras(getIntent())) {
            return;
        }

        findFragments();
        initializeDisplayMode(savedInstanceState);
        initializeLayout();
        initializeFragments();
        displayViews();

        ChangeLog cl = new ChangeLog(this);
        if (cl.isFirstRun()) {
            cl.getLogDialog().show();
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (isFinishing()) {
            return;
        }

        setIntent(intent);

        if (firstBackStackId >= 0) {
            getFragmentManager().popBackStackImmediate(firstBackStackId,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
            firstBackStackId = -1;
        }
        removeMessageListFragment();
        removeMessageViewFragment();

        messageReference = null;
        search = null;
        folderName = null;

        if (!decodeExtras(intent)) {
            return;
        }

        initializeDisplayMode(null);
        initializeFragments();
        displayViews();
    }

    /**
     * Get references to existing fragments if the activity was restarted.
     */
    private void findFragments() {
        FragmentManager fragmentManager = getFragmentManager();
        messageListFragment = (MessageListFragment) fragmentManager.findFragmentById(
                R.id.message_list_container);
        messageViewFragment = (MessageViewFragment) fragmentManager.findFragmentById(
                R.id.message_view_container);
    }

    /**
     * Create fragment instances if necessary.
     *
     * @see #findFragments()
     */
    private void initializeFragments() {
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.addOnBackStackChangedListener(this);

        boolean hasMessageListFragment = (messageListFragment != null);

        if (!hasMessageListFragment) {
            FragmentTransaction ft = fragmentManager.beginTransaction();
            messageListFragment = MessageListFragment.newInstance(search, false,
                    (K9.isThreadedViewEnabled() && !noThreading));
            ft.add(R.id.message_list_container, messageListFragment);
            ft.commit();
        }

        // Check if the fragment wasn't restarted and has a MessageReference in the arguments. If
        // so, open the referenced message.
        if (!hasMessageListFragment && messageViewFragment == null &&
                messageReference != null) {
            openMessage(messageReference);
        }
    }

    /**
     * Set the initial display mode (message list, message view, or split view).
     *
     * <p><strong>Note:</strong>
     * This method has to be called after {@link #findFragments()} because the result depends on
     * the availability of a {@link MessageViewFragment} instance.
     * </p>
     *
     * @param savedInstanceState
     *         The saved instance state that was passed to the activity as argument to
     *         {@link #onCreate(Bundle)}. May be {@code null}.
     */
    private void initializeDisplayMode(Bundle savedInstanceState) {
        if (useSplitView()) {
            displayMode = DisplayMode.SPLIT_VIEW;
            return;
        }

        if (savedInstanceState != null) {
            DisplayMode savedDisplayMode =
                    (DisplayMode) savedInstanceState.getSerializable(STATE_DISPLAY_MODE);
            if (savedDisplayMode != DisplayMode.SPLIT_VIEW) {
                displayMode = savedDisplayMode;
                return;
            }
        }

        if (messageViewFragment != null || messageReference != null) {
            displayMode = DisplayMode.MESSAGE_VIEW;
        } else {
            displayMode = DisplayMode.MESSAGE_LIST;
        }
    }

    private boolean useSplitView() {
        SplitViewMode splitViewMode = K9.getSplitViewMode();
        int orientation = getResources().getConfiguration().orientation;

        return (splitViewMode == SplitViewMode.ALWAYS ||
                (splitViewMode == SplitViewMode.WHEN_IN_LANDSCAPE &&
                orientation == Configuration.ORIENTATION_LANDSCAPE));
    }

    private void initializeLayout() {
        messageViewContainer = (ViewGroup) findViewById(R.id.message_view_container);

        LayoutInflater layoutInflater = getLayoutInflater();
        messageViewPlaceHolder = layoutInflater.inflate(R.layout.empty_message_view, messageViewContainer, false);
    }

    private void displayViews() {
        switch (displayMode) {
            case MESSAGE_LIST: {
                showMessageList();
                break;
            }
            case MESSAGE_VIEW: {
                showMessageView();
                break;
            }
            case SPLIT_VIEW: {
                messageListWasDisplayed = true;
                if (messageViewFragment == null) {
                    showMessageViewPlaceHolder();
                } else {
                    MessageReference activeMessage = messageViewFragment.getMessageReference();
                    if (activeMessage != null) {
                        messageListFragment.setActiveMessage(activeMessage);
                    }
                }
                break;
            }
        }
    }

    private boolean decodeExtras(Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action) && intent.getData() != null) {
            Uri uri = intent.getData();
            List<String> segmentList = uri.getPathSegments();

            String accountId = segmentList.get(0);
            Collection<Account> accounts = Preferences.getPreferences(this).getAvailableAccounts();
            for (Account account : accounts) {
                if (String.valueOf(account.getAccountNumber()).equals(accountId)) {
                    String folderName = segmentList.get(1);
                    String messageUid = segmentList.get(2);
                    messageReference = new MessageReference(account.getUuid(), folderName, messageUid, null);
                    break;
                }
            }
        } else if (ACTION_SHORTCUT.equals(action)) {
            // Handle shortcut intents
            String specialFolder = intent.getStringExtra(EXTRA_SPECIAL_FOLDER);
            if (SearchAccount.UNIFIED_INBOX.equals(specialFolder)) {
                search = SearchAccount.createUnifiedInboxAccount(this).getRelatedSearch();
            } else if (SearchAccount.ALL_MESSAGES.equals(specialFolder)) {
                search = SearchAccount.createAllMessagesAccount(this).getRelatedSearch();
            }
        } else if (intent.getStringExtra(SearchManager.QUERY) != null) {
            // check if this intent comes from the system search ( remote )
            if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
                //Query was received from Search Dialog
                String query = intent.getStringExtra(SearchManager.QUERY).trim();

                search = new LocalSearch(getString(R.string.search_results));
                search.setManualSearch(true);
                noThreading = true;

                search.or(new SearchCondition(SearchField.SENDER, Attribute.CONTAINS, query));
                search.or(new SearchCondition(SearchField.SUBJECT, Attribute.CONTAINS, query));
                search.or(new SearchCondition(SearchField.MESSAGE_CONTENTS, Attribute.CONTAINS, query));

                Bundle appData = intent.getBundleExtra(SearchManager.APP_DATA);
                if (appData != null) {
                    search.addAccountUuid(appData.getString(EXTRA_SEARCH_ACCOUNT));
                    // searches started from a folder list activity will provide an account, but no folder
                    if (appData.getString(EXTRA_SEARCH_FOLDER) != null) {
                        search.addAllowedFolder(appData.getString(EXTRA_SEARCH_FOLDER));
                    }
                } else {
                    search.addAccountUuid(LocalSearch.ALL_ACCOUNTS);
                }
            }
        } else {
            // regular LocalSearch object was passed
            search = intent.hasExtra(EXTRA_SEARCH) ?
                    ParcelableUtil.unmarshall(intent.getByteArrayExtra(EXTRA_SEARCH), LocalSearch.CREATOR) : null;
            noThreading = intent.getBooleanExtra(EXTRA_NO_THREADING, false);
        }

        if (messageReference == null) {
            String messageReferenceString = intent.getStringExtra(EXTRA_MESSAGE_REFERENCE);
            messageReference = MessageReference.parse(messageReferenceString);
        }

        if (messageReference != null) {
            search = new LocalSearch();
            search.addAccountUuid(messageReference.getAccountUuid());
            search.addAllowedFolder(messageReference.getFolderName());
        }

        if (search == null) {
            // We've most likely been started by an old unread widget
            String accountUuid = intent.getStringExtra("account");
            String folderName = intent.getStringExtra("folder");

            search = new LocalSearch(folderName);
            search.addAccountUuid((accountUuid == null) ? "invalid" : accountUuid);
            if (folderName != null) {
                search.addAllowedFolder(folderName);
            }
        }

        Preferences prefs = Preferences.getPreferences(getApplicationContext());

        String[] accountUuids = search.getAccountUuids();
        if (search.searchAllAccounts()) {
            List<Account> accounts = prefs.getAccounts();
            singleAccountMode = (accounts.size() == 1);
            if (singleAccountMode) {
                account = accounts.get(0);
            }
        } else {
            singleAccountMode = (accountUuids.length == 1);
            if (singleAccountMode) {
                account = prefs.getAccount(accountUuids[0]);
            }
        }
        singleFolderMode = singleAccountMode && (search.getFolderNames().size() == 1);

        if (singleAccountMode && (account == null || !account.isAvailable(this))) {
            Timber.i("not opening MessageList of unavailable account");
            onAccountUnavailable();
            return false;
        }

        if (singleFolderMode) {
            folderName = search.getFolderNames().get(0);
        }

        // now we know if we are in single account mode and need a subtitle
        actionBarSubTitle.setVisibility((!singleFolderMode) ? View.GONE : View.VISIBLE);

        return true;
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

        if (account != null && !account.isAvailable(this)) {
            onAccountUnavailable();
            return;
        }
        StorageManager.getInstance(getApplication()).addListener(mStorageListener);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(STATE_DISPLAY_MODE, displayMode);
        outState.putBoolean(STATE_MESSAGE_LIST_WAS_DISPLAYED, messageListWasDisplayed);
        outState.putInt(STATE_FIRST_BACK_STACK_ID, firstBackStackId);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        messageListWasDisplayed = savedInstanceState.getBoolean(STATE_MESSAGE_LIST_WAS_DISPLAYED);
        firstBackStackId = savedInstanceState.getInt(STATE_FIRST_BACK_STACK_ID);
    }

    private void initializeActionBar() {
        actionBar = getActionBar();

        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(R.layout.actionbar_custom);

        View customView = actionBar.getCustomView();
        actionBarMessageList = customView.findViewById(R.id.actionbar_message_list);
        actionBarMessageView = customView.findViewById(R.id.actionbar_message_view);
        actionBarSubject = (MessageTitleView) customView.findViewById(R.id.message_title_view);
        actionBarTitle = (TextView) customView.findViewById(R.id.actionbar_title_first);
        actionBarSubTitle = (TextView) customView.findViewById(R.id.actionbar_title_sub);
        actionBarUnread = (TextView) customView.findViewById(R.id.actionbar_unread_count);
        actionBarProgress = (ProgressBar) customView.findViewById(R.id.actionbar_progress);
        actionButtonIndeterminateProgress = getActionButtonIndeterminateProgress();

        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @SuppressLint("InflateParams")
    private View getActionButtonIndeterminateProgress() {
        return getLayoutInflater().inflate(R.layout.actionbar_indeterminate_progress_actionview, null);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean ret = false;
        if (KeyEvent.ACTION_DOWN == event.getAction()) {
            ret = onCustomKeyDown(event.getKeyCode(), event);
        }
        if (!ret) {
            ret = super.dispatchKeyEvent(event);
        }
        return ret;
    }

    @Override
    public void onBackPressed() {
        if (displayMode == DisplayMode.MESSAGE_VIEW && messageListWasDisplayed) {
            showMessageList();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Handle hotkeys
     *
     * <p>
     * This method is called by {@link #dispatchKeyEvent(KeyEvent)} before any view had the chance
     * to consume this key event.
     * </p>
     *
     * @param keyCode
     *         The value in {@code event.getKeyCode()}.
     * @param event
     *         Description of the key event.
     *
     * @return {@code true} if this event was consumed.
     */
    public boolean onCustomKeyDown(final int keyCode, final KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP: {
                if (messageViewFragment != null && displayMode != DisplayMode.MESSAGE_LIST &&
                        K9.useVolumeKeysForNavigationEnabled()) {
                    showPreviousMessage();
                    return true;
                } else if (displayMode != DisplayMode.MESSAGE_VIEW &&
                        K9.useVolumeKeysForListNavigationEnabled()) {
                    messageListFragment.onMoveUp();
                    return true;
                }

                break;
            }
            case KeyEvent.KEYCODE_VOLUME_DOWN: {
                if (messageViewFragment != null && displayMode != DisplayMode.MESSAGE_LIST &&
                        K9.useVolumeKeysForNavigationEnabled()) {
                    showNextMessage();
                    return true;
                } else if (displayMode != DisplayMode.MESSAGE_VIEW &&
                        K9.useVolumeKeysForListNavigationEnabled()) {
                    messageListFragment.onMoveDown();
                    return true;
                }

                break;
            }
            case KeyEvent.KEYCODE_C: {
                messageListFragment.onCompose();
                return true;
            }
            case KeyEvent.KEYCODE_Q: {
                if (messageListFragment != null && messageListFragment.isSingleAccountMode()) {
                    onShowFolderList();
                }
                return true;
            }
            case KeyEvent.KEYCODE_O: {
                messageListFragment.onCycleSort();
                return true;
            }
            case KeyEvent.KEYCODE_I: {
                messageListFragment.onReverseSort();
                return true;
            }
            case KeyEvent.KEYCODE_DEL:
            case KeyEvent.KEYCODE_D: {
                if (displayMode == DisplayMode.MESSAGE_LIST) {
                    messageListFragment.onDelete();
                } else if (messageViewFragment != null) {
                    messageViewFragment.onDelete();
                }
                return true;
            }
            case KeyEvent.KEYCODE_S: {
                messageListFragment.toggleMessageSelect();
                return true;
            }
            case KeyEvent.KEYCODE_G: {
                if (displayMode == DisplayMode.MESSAGE_LIST) {
                    messageListFragment.onToggleFlagged();
                } else if (messageViewFragment != null) {
                    messageViewFragment.onToggleFlagged();
                }
                return true;
            }
            case KeyEvent.KEYCODE_M: {
                if (displayMode == DisplayMode.MESSAGE_LIST) {
                    messageListFragment.onMove();
                } else if (messageViewFragment != null) {
                    messageViewFragment.onMove();
                }
                return true;
            }
            case KeyEvent.KEYCODE_V: {
                if (displayMode == DisplayMode.MESSAGE_LIST) {
                    messageListFragment.onArchive();
                } else if (messageViewFragment != null) {
                    messageViewFragment.onArchive();
                }
                return true;
            }
            case KeyEvent.KEYCODE_Y: {
                if (displayMode == DisplayMode.MESSAGE_LIST) {
                    messageListFragment.onCopy();
                } else if (messageViewFragment != null) {
                    messageViewFragment.onCopy();
                }
                return true;
            }
            case KeyEvent.KEYCODE_Z: {
                if (displayMode == DisplayMode.MESSAGE_LIST) {
                    messageListFragment.onToggleRead();
                } else if (messageViewFragment != null) {
                    messageViewFragment.onToggleRead();
                }
                return true;
            }
            case KeyEvent.KEYCODE_F: {
                if (messageViewFragment != null) {
                    messageViewFragment.onForward();
                }
                return true;
            }
            case KeyEvent.KEYCODE_A: {
                if (messageViewFragment != null) {
                    messageViewFragment.onReplyAll();
                }
                return true;
            }
            case KeyEvent.KEYCODE_R: {
                if (messageViewFragment != null) {
                    messageViewFragment.onReply();
                }
                return true;
            }
            case KeyEvent.KEYCODE_J:
            case KeyEvent.KEYCODE_P: {
                if (messageViewFragment != null) {
                    showPreviousMessage();
                }
                return true;
            }
            case KeyEvent.KEYCODE_N:
            case KeyEvent.KEYCODE_K: {
                if (messageViewFragment != null) {
                    showNextMessage();
                }
                return true;
            }
            /* FIXME
            case KeyEvent.KEYCODE_Z: {
                messageViewFragment.zoom(event);
                return true;
            }*/
            case KeyEvent.KEYCODE_H: {
                Toast toast;
                if (displayMode == DisplayMode.MESSAGE_LIST) {
                    toast = Toast.makeText(this, R.string.message_list_help_key, Toast.LENGTH_LONG);
                } else {
                    toast = Toast.makeText(this, R.string.message_view_help_key, Toast.LENGTH_LONG);
                }
                toast.show();
                return true;
            }
            case KeyEvent.KEYCODE_DPAD_LEFT: {
                if (messageViewFragment != null && displayMode == DisplayMode.MESSAGE_VIEW) {
                    return showPreviousMessage();
                }
                return false;
            }
            case KeyEvent.KEYCODE_DPAD_RIGHT: {
                if (messageViewFragment != null && displayMode == DisplayMode.MESSAGE_VIEW) {
                    return showNextMessage();
                }
                return false;
            }

        }

        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Swallow these events too to avoid the audible notification of a volume change
        if (K9.useVolumeKeysForListNavigationEnabled()) {
            if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP) || (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
                Timber.v("Swallowed key up.");
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
        FolderList.actionHandleAccount(this, account);
        finish();
    }

    private void onEditPrefs() {
        Prefs.actionPrefs(this);
    }

    private void onEditAccount() {
        AccountSettings.actionSettings(this, account);
    }

    @Override
    public boolean onSearchRequested() {
        return messageListFragment.onSearchRequested();
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
                messageListFragment.onCompose();
                return true;
            }
            case R.id.toggle_message_view_theme: {
                onToggleTheme();
                return true;
            }
            // MessageList
            case R.id.check_mail: {
                messageListFragment.checkMail();
                return true;
            }
            case R.id.set_sort_date: {
                messageListFragment.changeSort(SortType.SORT_DATE);
                return true;
            }
            case R.id.set_sort_arrival: {
                messageListFragment.changeSort(SortType.SORT_ARRIVAL);
                return true;
            }
            case R.id.set_sort_subject: {
                messageListFragment.changeSort(SortType.SORT_SUBJECT);
                return true;
            }
            case R.id.set_sort_sender: {
                messageListFragment.changeSort(SortType.SORT_SENDER);
                return true;
            }
            case R.id.set_sort_flag: {
                messageListFragment.changeSort(SortType.SORT_FLAGGED);
                return true;
            }
            case R.id.set_sort_unread: {
                messageListFragment.changeSort(SortType.SORT_UNREAD);
                return true;
            }
            case R.id.set_sort_attach: {
                messageListFragment.changeSort(SortType.SORT_ATTACHMENT);
                return true;
            }
            case R.id.select_all: {
                messageListFragment.selectAll();
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
                messageListFragment.onSearchRequested();
                return true;
            }
            case R.id.search_remote: {
                messageListFragment.onRemoteSearch();
                return true;
            }
            case R.id.mark_all_as_read: {
                messageListFragment.confirmMarkAllAsRead();
                return true;
            }
            case R.id.show_folder_list: {
                onShowFolderList();
                return true;
            }
            // MessageView
            case R.id.next_message: {
                showNextMessage();
                return true;
            }
            case R.id.previous_message: {
                showPreviousMessage();
                return true;
            }
            case R.id.delete: {
                messageViewFragment.onDelete();
                return true;
            }
            case R.id.reply: {
                messageViewFragment.onReply();
                return true;
            }
            case R.id.reply_all: {
                messageViewFragment.onReplyAll();
                return true;
            }
            case R.id.forward: {
                messageViewFragment.onForward();
                return true;
            }
            case R.id.forward_as_attachment: {
                messageViewFragment.onForwardAsAttachment();
                return true;
            }
            case R.id.share: {
                messageViewFragment.onSendAlternate();
                return true;
            }
            case R.id.toggle_unread: {
                messageViewFragment.onToggleRead();
                return true;
            }
            case R.id.archive:
            case R.id.refile_archive: {
                messageViewFragment.onArchive();
                return true;
            }
            case R.id.spam:
            case R.id.refile_spam: {
                messageViewFragment.onSpam();
                return true;
            }
            case R.id.move:
            case R.id.refile_move: {
                messageViewFragment.onMove();
                return true;
            }
            case R.id.copy:
            case R.id.refile_copy: {
                messageViewFragment.onCopy();
                return true;
            }
            case R.id.select_text: {
                messageViewFragment.onSelectText();
                return true;
            }
            case R.id.show_headers:
            case R.id.hide_headers: {
                messageViewFragment.onToggleAllHeadersView();
                updateMenu();
                return true;
            }
        }

        if (!singleFolderMode) {
            // None of the options after this point are "safe" for search results
            //TODO: This is not true for "unread" and "starred" searches in regular folders
            return false;
        }

        switch (itemId) {
            case R.id.send_messages: {
                messageListFragment.onSendPendingMessages();
                return true;
            }
            case R.id.folder_settings: {
                if (folderName != null) {
                    FolderSettings.actionSettings(this, account, folderName);
                }
                return true;
            }
            case R.id.expunge: {
                messageListFragment.onExpunge();
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.message_list_option, menu);
        this.menu = menu;
        menuButtonCheckMail = menu.findItem(R.id.check_mail);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        configureMenu(menu);
        return true;
    }

    /**
     * Hide menu items not appropriate for the current context.
     *
     * <p><strong>Note:</strong>
     * Please adjust the comments in {@code res/menu/message_list_option.xml} if you change the
     * visibility of a menu item in this method.
     * </p>
     *
     * @param menu
     *         The {@link Menu} instance that should be modified. May be {@code null}; in that case
     *         the method does nothing and immediately returns.
     */
    private void configureMenu(Menu menu) {
        if (menu == null) {
            return;
        }

        // Set visibility of account/folder settings menu items
        if (messageListFragment == null) {
            menu.findItem(R.id.account_settings).setVisible(false);
            menu.findItem(R.id.folder_settings).setVisible(false);
        } else {
            menu.findItem(R.id.account_settings).setVisible(
                    messageListFragment.isSingleAccountMode());
            menu.findItem(R.id.folder_settings).setVisible(
                    messageListFragment.isSingleFolderMode());
        }

        /*
         * Set visibility of menu items related to the message view
         */

        if (displayMode == DisplayMode.MESSAGE_LIST
                || messageViewFragment == null
                || !messageViewFragment.isInitialized()) {
            menu.findItem(R.id.next_message).setVisible(false);
            menu.findItem(R.id.previous_message).setVisible(false);
            menu.findItem(R.id.single_message_options).setVisible(false);
            menu.findItem(R.id.delete).setVisible(false);
            menu.findItem(R.id.compose).setVisible(false);
            menu.findItem(R.id.archive).setVisible(false);
            menu.findItem(R.id.move).setVisible(false);
            menu.findItem(R.id.copy).setVisible(false);
            menu.findItem(R.id.spam).setVisible(false);
            menu.findItem(R.id.refile).setVisible(false);
            menu.findItem(R.id.toggle_unread).setVisible(false);
            menu.findItem(R.id.select_text).setVisible(false);
            menu.findItem(R.id.toggle_message_view_theme).setVisible(false);
            menu.findItem(R.id.show_headers).setVisible(false);
            menu.findItem(R.id.hide_headers).setVisible(false);
        } else {
            // hide prev/next buttons in split mode
            if (displayMode != DisplayMode.MESSAGE_VIEW) {
                menu.findItem(R.id.next_message).setVisible(false);
                menu.findItem(R.id.previous_message).setVisible(false);
            } else {
                MessageReference ref = messageViewFragment.getMessageReference();
                boolean initialized = (messageListFragment != null &&
                        messageListFragment.isLoadFinished());
                boolean canDoPrev = (initialized && !messageListFragment.isFirst(ref));
                boolean canDoNext = (initialized && !messageListFragment.isLast(ref));

                MenuItem prev = menu.findItem(R.id.previous_message);
                prev.setEnabled(canDoPrev);
                prev.getIcon().setAlpha(canDoPrev ? 255 : 127);

                MenuItem next = menu.findItem(R.id.next_message);
                next.setEnabled(canDoNext);
                next.getIcon().setAlpha(canDoNext ? 255 : 127);
            }

            MenuItem toggleTheme = menu.findItem(R.id.toggle_message_view_theme);
            if (K9.useFixedMessageViewTheme()) {
                toggleTheme.setVisible(false);
            } else {
                // Set title of menu item to switch to dark/light theme
                if (K9.getK9MessageViewTheme() == K9.Theme.DARK) {
                    toggleTheme.setTitle(R.string.message_view_theme_action_light);
                } else {
                    toggleTheme.setTitle(R.string.message_view_theme_action_dark);
                }
                toggleTheme.setVisible(true);
            }

            // Set title of menu item to toggle the read state of the currently displayed message
            if (messageViewFragment.isMessageRead()) {
                menu.findItem(R.id.toggle_unread).setTitle(R.string.mark_as_unread_action);
            } else {
                menu.findItem(R.id.toggle_unread).setTitle(R.string.mark_as_read_action);
            }

            // Jellybean has built-in long press selection support
            menu.findItem(R.id.select_text).setVisible(Build.VERSION.SDK_INT < 16);

            menu.findItem(R.id.delete).setVisible(K9.isMessageViewDeleteActionVisible());

            /*
             * Set visibility of copy, move, archive, spam in action bar and refile submenu
             */
            if (messageViewFragment.isCopyCapable()) {
                menu.findItem(R.id.copy).setVisible(K9.isMessageViewCopyActionVisible());
                menu.findItem(R.id.refile_copy).setVisible(true);
            } else {
                menu.findItem(R.id.copy).setVisible(false);
                menu.findItem(R.id.refile_copy).setVisible(false);
            }

            if (messageViewFragment.isMoveCapable()) {
                boolean canMessageBeArchived = messageViewFragment.canMessageBeArchived();
                boolean canMessageBeMovedToSpam = messageViewFragment.canMessageBeMovedToSpam();

                menu.findItem(R.id.move).setVisible(K9.isMessageViewMoveActionVisible());
                menu.findItem(R.id.archive).setVisible(canMessageBeArchived &&
                        K9.isMessageViewArchiveActionVisible());
                menu.findItem(R.id.spam).setVisible(canMessageBeMovedToSpam &&
                        K9.isMessageViewSpamActionVisible());

                menu.findItem(R.id.refile_move).setVisible(true);
                menu.findItem(R.id.refile_archive).setVisible(canMessageBeArchived);
                menu.findItem(R.id.refile_spam).setVisible(canMessageBeMovedToSpam);
            } else {
                menu.findItem(R.id.move).setVisible(false);
                menu.findItem(R.id.archive).setVisible(false);
                menu.findItem(R.id.spam).setVisible(false);

                menu.findItem(R.id.refile).setVisible(false);
            }

            if (messageViewFragment.allHeadersVisible()) {
                menu.findItem(R.id.show_headers).setVisible(false);
            } else {
                menu.findItem(R.id.hide_headers).setVisible(false);
            }
        }


        /*
         * Set visibility of menu items related to the message list
         */

        // Hide both search menu items by default and enable one when appropriate
        menu.findItem(R.id.search).setVisible(false);
        menu.findItem(R.id.search_remote).setVisible(false);

        if (displayMode == DisplayMode.MESSAGE_VIEW || messageListFragment == null ||
                !messageListFragment.isInitialized()) {
            menu.findItem(R.id.check_mail).setVisible(false);
            menu.findItem(R.id.set_sort).setVisible(false);
            menu.findItem(R.id.select_all).setVisible(false);
            menu.findItem(R.id.send_messages).setVisible(false);
            menu.findItem(R.id.expunge).setVisible(false);
            menu.findItem(R.id.mark_all_as_read).setVisible(false);
            menu.findItem(R.id.show_folder_list).setVisible(false);
        } else {
            menu.findItem(R.id.set_sort).setVisible(true);
            menu.findItem(R.id.select_all).setVisible(true);
            menu.findItem(R.id.compose).setVisible(true);
            menu.findItem(R.id.mark_all_as_read).setVisible(
                    messageListFragment.isMarkAllAsReadSupported());

            if (!messageListFragment.isSingleAccountMode()) {
                menu.findItem(R.id.expunge).setVisible(false);
                menu.findItem(R.id.send_messages).setVisible(false);
                menu.findItem(R.id.show_folder_list).setVisible(false);
            } else {
                menu.findItem(R.id.send_messages).setVisible(messageListFragment.isOutbox());
                menu.findItem(R.id.expunge).setVisible(messageListFragment.isRemoteFolder() &&
                        messageListFragment.isAccountExpungeCapable());
                menu.findItem(R.id.show_folder_list).setVisible(true);
            }

            menu.findItem(R.id.check_mail).setVisible(messageListFragment.isCheckMailSupported());

            // If this is an explicit local search, show the option to search on the server
            if (!messageListFragment.isRemoteSearch() &&
                    messageListFragment.isRemoteSearchAllowed()) {
                menu.findItem(R.id.search_remote).setVisible(true);
            } else if (!messageListFragment.isManualSearch()) {
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
        actionBarTitle.setText(title);
    }

    public void setActionBarSubTitle(String subTitle) {
        actionBarSubTitle.setText(subTitle);
    }

    public void setActionBarUnread(int unread) {
        if (unread == 0) {
            actionBarUnread.setVisibility(View.GONE);
        } else {
            actionBarUnread.setVisibility(View.VISIBLE);
            actionBarUnread.setText(String.format("%d", unread));
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
        setProgress(progress);
    }

    @Override
    public void openMessage(MessageReference messageReference) {
        Preferences prefs = Preferences.getPreferences(getApplicationContext());
        Account account = prefs.getAccount(messageReference.getAccountUuid());
        String folderName = messageReference.getFolderName();

        if (folderName.equals(account.getDraftsFolderName())) {
            MessageActions.actionEditDraft(this, messageReference);
        } else {
            messageViewContainer.removeView(messageViewPlaceHolder);

            if (messageListFragment != null) {
                messageListFragment.setActiveMessage(messageReference);
            }

            MessageViewFragment fragment = MessageViewFragment.newInstance(messageReference);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.message_view_container, fragment);
            messageViewFragment = fragment;
            ft.commit();

            if (displayMode != DisplayMode.SPLIT_VIEW) {
                showMessageView();
            }
        }
    }

    @Override
    public void onResendMessage(MessageReference messageReference) {
        MessageActions.actionEditDraft(this, messageReference);
    }

    @Override
    public void onForward(MessageReference messageReference) {
        onForward(messageReference, null);
    }

    @Override
    public void onForward(MessageReference messageReference, Parcelable decryptionResultForReply) {
        MessageActions.actionForward(this, messageReference, decryptionResultForReply);
    }

    @Override
    public void onForwardAsAttachment(MessageReference messageReference) {
        onForwardAsAttachment(messageReference, null);
    }

    @Override
    public void onForwardAsAttachment(MessageReference messageReference, Parcelable decryptionResultForReply) {
        MessageActions.actionForwardAsAttachment(this, messageReference, decryptionResultForReply);
    }

    @Override
    public void onReply(MessageReference messageReference) {
        onReply(messageReference, null);
    }

    @Override
    public void onReply(MessageReference messageReference, Parcelable decryptionResultForReply) {
        MessageActions.actionReply(this, messageReference, false, decryptionResultForReply);
    }

    @Override
    public void onReplyAll(MessageReference messageReference) {
        onReplyAll(messageReference, null);
    }

    @Override
    public void onReplyAll(MessageReference messageReference, Parcelable decryptionResultForReply) {
        MessageActions.actionReply(this, messageReference, true, decryptionResultForReply);
    }

    @Override
    public void onCompose(Account account) {
        MessageActions.actionCompose(this, account);
    }

    @Override
    public void showMoreFromSameSender(String senderAddress) {
        LocalSearch tmpSearch = new LocalSearch(getString(R.string.search_from_format, senderAddress));
        tmpSearch.addAccountUuids(search.getAccountUuids());
        tmpSearch.and(SearchField.SENDER, senderAddress, Attribute.CONTAINS);

        MessageListFragment fragment = MessageListFragment.newInstance(tmpSearch, false, false);

        addMessageListFragment(fragment, true);
    }

    @Override
    public void onBackStackChanged() {
        findFragments();

        if (displayMode == DisplayMode.SPLIT_VIEW) {
            showMessageViewPlaceHolder();
        }

        configureMenu(menu);
    }

    @Override
    public void onSwipeRightToLeft(MotionEvent e1, MotionEvent e2) {
        if (messageListFragment != null && displayMode != DisplayMode.MESSAGE_VIEW) {
            messageListFragment.onSwipeRightToLeft(e1, e2);
        }
    }

    @Override
    public void onSwipeLeftToRight(MotionEvent e1, MotionEvent e2) {
        if (messageListFragment != null && displayMode != DisplayMode.MESSAGE_VIEW) {
            messageListFragment.onSwipeLeftToRight(e1, e2);
        }
    }

    private final class StorageListenerImplementation implements StorageManager.StorageListener {
        @Override
        public void onUnmount(String providerId) {
            if (account != null && providerId.equals(account.getLocalStorageProviderId())) {
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
        FragmentTransaction ft = getFragmentManager().beginTransaction();

        ft.replace(R.id.message_list_container, fragment);
        if (addToBackStack)
            ft.addToBackStack(null);

        messageListFragment = fragment;

        int transactionId = ft.commit();
        if (transactionId >= 0 && firstBackStackId < 0) {
            firstBackStackId = transactionId;
        }
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
        showMessageViewPlaceHolder();

        LocalSearch tmpSearch = new LocalSearch();
        tmpSearch.addAccountUuid(account.getUuid());
        tmpSearch.and(SearchField.THREAD_ID, String.valueOf(threadRootId), Attribute.EQUALS);

        MessageListFragment fragment = MessageListFragment.newInstance(tmpSearch, true, false);
        addMessageListFragment(fragment, true);
    }

    private void showMessageViewPlaceHolder() {
        removeMessageViewFragment();

        // Add placeholder view if necessary
        if (messageViewPlaceHolder.getParent() == null) {
            messageViewContainer.addView(messageViewPlaceHolder);
        }

        messageListFragment.setActiveMessage(null);
    }

    /**
     * Remove MessageViewFragment if necessary.
     */
    private void removeMessageViewFragment() {
        if (messageViewFragment != null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.remove(messageViewFragment);
            messageViewFragment = null;
            ft.commit();

            showDefaultTitleView();
        }
    }

    private void removeMessageListFragment() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.remove(messageListFragment);
        messageListFragment = null;
        ft.commit();
    }

    @Override
    public void remoteSearchStarted() {
        // Remove action button for remote search
        configureMenu(menu);
    }

    @Override
    public void goBack() {
        FragmentManager fragmentManager = getFragmentManager();
        if (displayMode == DisplayMode.MESSAGE_VIEW) {
            showMessageList();
        } else if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else if (messageListFragment.isManualSearch()) {
            finish();
        } else if (!singleFolderMode) {
            onAccounts();
        } else {
            onShowFolderList();
        }
    }

    @Override
    public void enableActionBarProgress(boolean enable) {
        if (menuButtonCheckMail != null && menuButtonCheckMail.isVisible()) {
            actionBarProgress.setVisibility(ProgressBar.GONE);
            if (enable) {
                menuButtonCheckMail
                        .setActionView(actionButtonIndeterminateProgress);
            } else {
                menuButtonCheckMail.setActionView(null);
            }
        } else {
            if (menuButtonCheckMail != null)
                menuButtonCheckMail.setActionView(null);
            if (enable) {
                actionBarProgress.setVisibility(ProgressBar.VISIBLE);
            } else {
                actionBarProgress.setVisibility(ProgressBar.GONE);
            }
        }
    }

    @Override
    public void displayMessageSubject(String subject) {
        if (displayMode == DisplayMode.MESSAGE_VIEW) {
            actionBarSubject.setText(subject);
        } else {
            actionBarSubject.showSubjectInMessageHeader();
        }
    }

    @Override
    public void showNextMessageOrReturn() {
        if (K9.messageViewReturnToList() || !showLogicalNextMessage()) {
            if (displayMode == DisplayMode.SPLIT_VIEW) {
                showMessageViewPlaceHolder();
            } else {
                showMessageList();
            }
        }
    }

    /**
     * Shows the next message in the direction the user was displaying messages.
     *
     * @return {@code true}
     */
    private boolean showLogicalNextMessage() {
        boolean result = false;
        if (lastDirection == NEXT) {
            result = showNextMessage();
        } else if (lastDirection == PREVIOUS) {
            result = showPreviousMessage();
        }

        if (!result) {
            result = showNextMessage() || showPreviousMessage();
        }

        return result;
    }

    @Override
    public void setProgress(boolean enable) {
        setProgressBarIndeterminateVisibility(enable);
    }

    @Override
    public void messageHeaderViewAvailable(MessageHeader header) {
        actionBarSubject.setMessageHeader(header);
    }

    private boolean showNextMessage() {
        MessageReference ref = messageViewFragment.getMessageReference();
        if (ref != null) {
            if (messageListFragment.openNext(ref)) {
                lastDirection = NEXT;
                return true;
            }
        }
        return false;
    }

    private boolean showPreviousMessage() {
        MessageReference ref = messageViewFragment.getMessageReference();
        if (ref != null) {
            if (messageListFragment.openPrevious(ref)) {
                lastDirection = PREVIOUS;
                return true;
            }
        }
        return false;
    }

    private void showMessageList() {
        messageListWasDisplayed = true;
        displayMode = DisplayMode.MESSAGE_LIST;
        viewSwitcher.showFirstView();

        messageListFragment.setActiveMessage(null);

        showDefaultTitleView();
        configureMenu(menu);
    }

    private void showMessageView() {
        displayMode = DisplayMode.MESSAGE_VIEW;

        if (!messageListWasDisplayed) {
            viewSwitcher.setAnimateFirstView(false);
        }
        viewSwitcher.showSecondView();

        showMessageTitleView();
        configureMenu(menu);
    }

    @Override
    public void updateMenu() {
        invalidateOptionsMenu();
    }

    @Override
    public void disableDeleteAction() {
        menu.findItem(R.id.delete).setEnabled(false);
    }

    private void onToggleTheme() {
        if (K9.getK9MessageViewTheme() == K9.Theme.DARK) {
            K9.setK9MessageViewThemeSetting(K9.Theme.LIGHT);
        } else {
            K9.setK9MessageViewThemeSetting(K9.Theme.DARK);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                Context appContext = getApplicationContext();
                Preferences prefs = Preferences.getPreferences(appContext);
                StorageEditor editor = prefs.getStorage().edit();
                K9.save(editor);
                editor.commit();
            }
        }).start();

        recreate();
    }

    private void showDefaultTitleView() {
        actionBarMessageView.setVisibility(View.GONE);
        actionBarMessageList.setVisibility(View.VISIBLE);

        if (messageListFragment != null) {
            messageListFragment.updateTitle();
        }

        actionBarSubject.setMessageHeader(null);
    }

    private void showMessageTitleView() {
        actionBarMessageList.setVisibility(View.GONE);
        actionBarMessageView.setVisibility(View.VISIBLE);

        if (messageViewFragment != null) {
            displayMessageSubject(null);
            messageViewFragment.updateTitle();
        }
    }

    @Override
    public void onSwitchComplete(int displayedChild) {
        if (displayedChild == 0) {
            removeMessageViewFragment();
        }
    }

    @Override
    public void startIntentSenderForResult(IntentSender intent, int requestCode, Intent fillInIntent,
            int flagsMask, int flagsValues, int extraFlags) throws SendIntentException {
        requestCode |= REQUEST_MASK_PENDING_INTENT;
        super.startIntentSenderForResult(intent, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ((requestCode & REQUEST_MASK_PENDING_INTENT) == REQUEST_MASK_PENDING_INTENT) {
            requestCode ^= REQUEST_MASK_PENDING_INTENT;
            if (messageViewFragment != null) {
                messageViewFragment.onPendingIntentResult(requestCode, resultCode, data);
            }
        }
    }
}
