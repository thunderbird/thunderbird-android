package com.fsck.k9.activity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.internal.view.menu.MenuBuilder;
import com.actionbarsherlock.internal.view.menu.MenuPopupHelper;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.fsck.k9.Account;
import com.fsck.k9.Account.SortType;
import com.fsck.k9.AccountStats;
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.SearchSpecification;
import com.fsck.k9.activity.setup.AccountSettings;
import com.fsck.k9.activity.setup.FolderSettings;
import com.fsck.k9.activity.setup.Prefs;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.helper.MessageHelper;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.store.LocalStore;
import com.fsck.k9.mail.store.LocalStore.LocalFolder;
import com.fsck.k9.mail.store.LocalStore.LocalMessage;
import com.fsck.k9.mail.store.StorageManager;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;


/**
 * MessageList is the primary user interface for the program. This Activity
 * shows a list of messages.
 * From this Activity the user can perform all standard message operations.
 */
public class MessageList extends K9ListActivity implements OnItemClickListener {

    /**
     * Reverses the result of a {@link Comparator}.
     *
     * @param <T>
     */
    public static class ReverseComparator<T> implements Comparator<T> {
        private Comparator<T> mDelegate;

        /**
         * @param delegate
         *            Never <code>null</code>.
         */
        public ReverseComparator(final Comparator<T> delegate) {
            mDelegate = delegate;
        }

        @Override
        public int compare(final T object1, final T object2) {
            // arg1 & 2 are mixed up, this is done on purpose
            return mDelegate.compare(object2, object1);
        }

    }

    /**
     * Chains comparator to find a non-0 result.
     *
     * @param <T>
     */
    public static class ComparatorChain<T> implements Comparator<T> {

        private List<Comparator<T>> mChain;

        /**
         * @param chain
         *            Comparator chain. Never <code>null</code>.
         */
        public ComparatorChain(final List<Comparator<T>> chain) {
            mChain = chain;
        }

        @Override
        public int compare(T object1, T object2) {
            int result = 0;
            for (final Comparator<T> comparator : mChain) {
                result = comparator.compare(object1, object2);
                if (result != 0) {
                    break;
                }
            }
            return result;
        }

    }

    public static class AttachmentComparator implements Comparator<MessageInfoHolder> {

        @Override
        public int compare(MessageInfoHolder object1, MessageInfoHolder object2) {
            return (object1.message.hasAttachments() ? 0 : 1) - (object2.message.hasAttachments() ? 0 : 1);
        }

    }

    public static class FlaggedComparator implements Comparator<MessageInfoHolder> {

        @Override
        public int compare(MessageInfoHolder object1, MessageInfoHolder object2) {
            return (object1.flagged ? 0 : 1) - (object2.flagged ? 0 : 1);
        }

    }

    public static class UnreadComparator implements Comparator<MessageInfoHolder> {

        @Override
        public int compare(MessageInfoHolder object1, MessageInfoHolder object2) {
            return (object1.read ? 1 : 0) - (object2.read ? 1 : 0);
        }

    }

    public static class SenderComparator implements Comparator<MessageInfoHolder> {

        @Override
        public int compare(MessageInfoHolder object1, MessageInfoHolder object2) {
            return object1.compareCounterparty.toLowerCase().compareTo(object2.compareCounterparty.toLowerCase());
        }

    }

    public static class DateComparator implements Comparator<MessageInfoHolder> {

        @Override
        public int compare(MessageInfoHolder object1, MessageInfoHolder object2) {
            return object1.compareDate.compareTo(object2.compareDate);
        }

    }

    public static class ArrivalComparator implements Comparator<MessageInfoHolder> {

        @Override
        public int compare(MessageInfoHolder object1, MessageInfoHolder object2) {
            return object1.compareArrival.compareTo(object2.compareArrival);
        }

    }

    public static class SubjectComparator implements Comparator<MessageInfoHolder> {

        @Override
        public int compare(MessageInfoHolder arg0, MessageInfoHolder arg1) {
            // XXX doesn't respect the Comparator contract since it alters the compared object
            if (arg0.compareSubject == null) {
                arg0.compareSubject = Utility.stripSubject(arg0.message.getSubject());
            }
            if (arg1.compareSubject == null) {
                arg1.compareSubject = Utility.stripSubject(arg1.message.getSubject());
            }
            return arg0.compareSubject.compareToIgnoreCase(arg1.compareSubject);
        }

    }

    /**
     * Immutable empty {@link Message} array
     */
    private static final Message[] EMPTY_MESSAGE_ARRAY = new Message[0];

    private static final int DIALOG_MARK_ALL_AS_READ = 1;

    private static final int ACTIVITY_CHOOSE_FOLDER_MOVE = 1;
    private static final int ACTIVITY_CHOOSE_FOLDER_COPY = 2;

    private static final String EXTRA_ACCOUNT = "account";
    private static final String EXTRA_FOLDER  = "folder";
    private static final String EXTRA_QUERY = "query";
    private static final String EXTRA_QUERY_FLAGS = "queryFlags";
    private static final String EXTRA_FORBIDDEN_FLAGS = "forbiddenFlags";
    private static final String EXTRA_INTEGRATE = "integrate";
    private static final String EXTRA_ACCOUNT_UUIDS = "accountUuids";
    private static final String EXTRA_FOLDER_NAMES = "folderNames";
    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_LIST_POSITION = "listPosition";
    private static final String EXTRA_RETURN_FROM_MESSAGE_VIEW = "returnFromMessageView";

    /**
     * Maps a {@link SortType} to a {@link Comparator} implementation.
     */
    private static final Map<SortType, Comparator<MessageInfoHolder>> SORT_COMPARATORS;

    static {
        // fill the mapping at class time loading

        final Map<SortType, Comparator<MessageInfoHolder>> map = new EnumMap<SortType, Comparator<MessageInfoHolder>>(SortType.class);
        map.put(SortType.SORT_ATTACHMENT, new AttachmentComparator());
        map.put(SortType.SORT_DATE, new DateComparator());
        map.put(SortType.SORT_ARRIVAL, new ArrivalComparator());
        map.put(SortType.SORT_FLAGGED, new FlaggedComparator());
        map.put(SortType.SORT_SENDER, new SenderComparator());
        map.put(SortType.SORT_SUBJECT, new SubjectComparator());
        map.put(SortType.SORT_UNREAD, new UnreadComparator());

        // make it immutable to prevent accidental alteration (content is immutable already)
        SORT_COMPARATORS = Collections.unmodifiableMap(map);
    }

    private ListView mListView;
    private PullToRefreshListView mPullToRefreshView;

    private int mPreviewLines = 0;


    private MessageListAdapter mAdapter;
    private View mFooterView;

    private FolderInfoHolder mCurrentFolder;

    private LayoutInflater mInflater;

    private MessagingController mController;

    private Account mAccount;
    private int mUnreadMessageCount = 0;

    /**
     * Stores the name of the folder that we want to open as soon as possible
     * after load.
     */
    private String mFolderName;

    /**
     * If we're doing a search, this contains the query string.
     */
    private String mQueryString;
    private Flag[] mQueryFlags = null;
    private Flag[] mForbiddenFlags = null;
    private boolean mIntegrate = false;
    private String[] mAccountUuids = null;
    private String[] mFolderNames = null;
    private String mTitle;

    private MessageListHandler mHandler = new MessageListHandler();

    private SortType mSortType = SortType.SORT_DATE;
    private boolean mSortAscending = true;
    private boolean mSortDateAscending = false;

    private boolean mStars = true;
    private boolean mCheckboxes = true;
    private int mSelectedCount = 0;

    private FontSizes mFontSizes = K9.getFontSizes();

    private MenuItem mRefreshMenuItem;
    private ActionBar mActionBar;
    private ActionMode mActionMode;
    private View mActionBarProgressView;
    private Bundle mState = null;

    /**
     * Relevant messages for the current context when we have to remember the
     * chosen messages between user interactions (eg. Selecting a folder for
     * move operation)
     */
    private List<MessageInfoHolder> mActiveMessages;

    private Context context;

    /* package visibility for faster inner class access */
    MessageHelper mMessageHelper = MessageHelper.getInstance(this);

    private StorageManager.StorageListener mStorageListener = new StorageListenerImplementation();

    private TextView mActionBarTitle;
    private TextView mActionBarSubTitle;
    private TextView mActionBarUnread;

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

    /**
     * This class is used to run operations that modify UI elements in the UI thread.
     *
     * <p>We are using convenience methods that add a {@link android.os.Message} instance or a
     * {@link Runnable} to the message queue.</p>
     *
     * <p><strong>Note:</strong> If you add a method to this class make sure you don't accidentally
     * perform the operation in the calling thread.</p>
     */
    class MessageListHandler extends Handler {
        private static final int ACTION_REMOVE_MESSAGE = 1;
        private static final int ACTION_RESET_UNREAD_COUNT = 2;
        private static final int ACTION_SORT_MESSAGES = 3;
        private static final int ACTION_FOLDER_LOADING = 4;
        private static final int ACTION_REFRESH_TITLE = 5;
        private static final int ACTION_PROGRESS = 6;


        public void removeMessage(MessageReference messageReference) {
            android.os.Message msg = android.os.Message.obtain(this, ACTION_REMOVE_MESSAGE,
                    messageReference);
            sendMessage(msg);
        }

        public void sortMessages() {
            android.os.Message msg = android.os.Message.obtain(this, ACTION_SORT_MESSAGES);
            sendMessage(msg);
        }

        public void folderLoading(String folder, boolean loading) {
            android.os.Message msg = android.os.Message.obtain(this, ACTION_FOLDER_LOADING,
                    (loading) ? 1 : 0, 0, folder);
            sendMessage(msg);
        }

        public void refreshTitle() {
            android.os.Message msg = android.os.Message.obtain(this, ACTION_REFRESH_TITLE);
            sendMessage(msg);
        }

        public void progress(final boolean progress) {
            android.os.Message msg = android.os.Message.obtain(this, ACTION_PROGRESS,
                    (progress) ? 1 : 0, 0);
            sendMessage(msg);
        }

        public void changeMessageUid(final MessageReference ref, final String newUid) {
            // Instead of explicitly creating a container to be able to pass both arguments in a
            // Message we post a Runnable to the message queue.
            post(new Runnable() {
                @Override
                public void run() {
                    mAdapter.changeMessageUid(ref, newUid);
                }
            });
        }

        public void addOrUpdateMessages(final Account account, final String folderName,
                final List<Message> providedMessages, final boolean verifyAgainstSearch) {
            // We copy the message list because it's later modified by MessagingController
            final List<Message> messages = new ArrayList<Message>(providedMessages);

            post(new Runnable() {
                @Override
                public void run() {
                    mAdapter.addOrUpdateMessages(account, folderName, messages,
                            verifyAgainstSearch);
                }
            });
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case ACTION_REMOVE_MESSAGE: {
                    MessageReference messageReference = (MessageReference) msg.obj;
                    mAdapter.removeMessage(messageReference);
                    break;
                }
                case ACTION_RESET_UNREAD_COUNT: {
                    mAdapter.resetUnreadCount();
                    break;
                }
                case ACTION_SORT_MESSAGES: {
                    mAdapter.sortMessages();
                    break;
                }
                case ACTION_FOLDER_LOADING: {
                    String folder = (String) msg.obj;
                    boolean loading = (msg.arg1 == 1);
                    MessageList.this.folderLoading(folder, loading);
                    break;
                }
                case ACTION_REFRESH_TITLE: {
                    MessageList.this.refreshTitle();
                    break;
                }
                case ACTION_PROGRESS: {
                    boolean progress = (msg.arg1 == 1);
                    MessageList.this.progress(progress);
                    break;
                }
            }
        }
    }

    /**
     * @return The comparator to use to display messages in an ordered
     *         fashion. Never <code>null</code>.
     */
    protected Comparator<MessageInfoHolder> getComparator() {
        final List<Comparator<MessageInfoHolder>> chain = new ArrayList<Comparator<MessageInfoHolder>>(2 /* we add 2 comparators at most */);

        {
            // add the specified comparator
            final Comparator<MessageInfoHolder> comparator = SORT_COMPARATORS.get(mSortType);
            if (mSortAscending) {
                chain.add(comparator);
            } else {
                chain.add(new ReverseComparator<MessageInfoHolder>(comparator));
            }
        }

        {
            // add the date comparator if not already specified
            if (mSortType != SortType.SORT_DATE && mSortType != SortType.SORT_ARRIVAL) {
                final Comparator<MessageInfoHolder> comparator = SORT_COMPARATORS.get(SortType.SORT_DATE);
                if (mSortDateAscending) {
                    chain.add(comparator);
                } else {
                    chain.add(new ReverseComparator<MessageInfoHolder>(comparator));
                }
            }
        }

        // build the comparator chain
        final Comparator<MessageInfoHolder> chainComparator = new ComparatorChain<MessageInfoHolder>(chain);

        return chainComparator;
    }

    private void folderLoading(String folder, boolean loading) {
        if (mCurrentFolder != null && mCurrentFolder.name.equals(folder)) {
            mCurrentFolder.loading = loading;
        }
        updateFooterView();
    }

    private void refreshTitle() {
        setWindowTitle();
        setWindowProgress();
    }

    private void setWindowProgress() {
        int level = Window.PROGRESS_END;

        if (mCurrentFolder != null && mCurrentFolder.loading && mAdapter.mListener.getFolderTotal() > 0) {
            int divisor = mAdapter.mListener.getFolderTotal();
            if (divisor != 0) {
                level = (Window.PROGRESS_END / divisor) * (mAdapter.mListener.getFolderCompleted()) ;
                if (level > Window.PROGRESS_END) {
                    level = Window.PROGRESS_END;
                }
            }
        }

        setSupportProgress(level);
    }

    private void setWindowTitle() {
        // regular folder content display
        if (mFolderName != null) {
            String displayName = FolderInfoHolder.getDisplayName(MessageList.this, mAccount,
                mFolderName);

            mActionBarTitle.setText(displayName);

            String operation = mAdapter.mListener.getOperation(MessageList.this, getTimeFormat()).trim();
            if (operation.length() < 1) {
                mActionBarSubTitle.setText(mAccount.getEmail());
            } else {
                mActionBarSubTitle.setText(operation);
            }
        } else if (mQueryString != null) {
            // query result display.  This may be for a search folder as opposed to a user-initiated search.
            if (mTitle != null) {
                // This was a search folder; the search folder has overridden our title.
                mActionBarTitle.setText(mTitle);
            } else {
                // This is a search result; set it to the default search result line.
                mActionBarTitle.setText(getString(R.string.search_results));
            }
        }

        // set unread count
        if (mUnreadMessageCount == 0) {
            mActionBarUnread.setVisibility(View.GONE);
        } else {
            if (mQueryString != null && mTitle == null) {
                // This is a search result.  The unread message count is easily confused
                // with total number of messages in the search result, so let's hide it.
                mActionBarUnread.setVisibility(View.GONE);
            } else {
                mActionBarUnread.setText(Integer.toString(mUnreadMessageCount));
                mActionBarUnread.setVisibility(View.VISIBLE);
            }
        }
    }

    private void progress(final boolean progress) {
        // Make sure we don't try this before the menu is initialized
        // this could happen while the activity is initialized.
        if (mRefreshMenuItem != null) {
            if (progress) {
                mRefreshMenuItem.setActionView(mActionBarProgressView);
            } else {
                mRefreshMenuItem.setActionView(null);
            }
        }

        if (mPullToRefreshView != null && !progress) {
            mPullToRefreshView.onRefreshComplete();
        }
    }

    /**
     * Show the message list that was used to open the {@link MessageView} for a message.
     *
     * <p>
     * <strong>Note:</strong>
     * The {@link MessageList} instance should still be around and all we do is bring it back to
     * the front (see the activity flags).<br>
     * Out of sheer paranoia we also set the extras that were used to create the original
     * {@code MessageList} instance. Using those, the activity can be recreated in the unlikely
     * case of it having been killed by the OS.
     * </p>
     *
     * @param context
     *         The {@link Context} instance to invoke the {@link Context#startActivity(Intent)}
     *         method on.
     * @param extras
     *         The extras used to create the original {@code MessageList} instance.
     *
     * @see MessageView#actionView(Context, MessageReference, ArrayList, Bundle)
     */
    public static void actionHandleFolder(Context context, Bundle extras) {
        Intent intent = new Intent(context, MessageList.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtras(extras);
        intent.putExtra(EXTRA_RETURN_FROM_MESSAGE_VIEW, true);
        context.startActivity(intent);
    }

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
        intent.putExtra(EXTRA_QUERY, queryString);
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
        intent.putExtra(EXTRA_QUERY, searchSpecification.getQuery());
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

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (view == mFooterView) {
            if (mCurrentFolder != null) {
                mController.loadMoreMessages(mAccount, mFolderName, mAdapter.mListener);
            }
            return;
        }

        final MessageInfoHolder message = (MessageInfoHolder) parent.getItemAtPosition(position);
        if (mSelectedCount > 0) {
            toggleMessageSelect(message);
        } else {
            onOpenMessage(message);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        context = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.message_list);

        mActionBarProgressView = getLayoutInflater().inflate(R.layout.actionbar_indeterminate_progress_actionview, null);

        // need this for actionbar initialization
        mQueryString = getIntent().getStringExtra(EXTRA_QUERY);

        mPullToRefreshView = (PullToRefreshListView) findViewById(R.id.message_list);

        mInflater = getLayoutInflater();
        mActionBar = getSupportActionBar();
        initializeActionBar();
        initializeLayout();

        mPreviewLines = K9.messageListPreviewLines();

        initializeMessageList(getIntent(), true);
        mListView.setVerticalFadingEdgeEnabled(false);

        // Enable context action bar behaviour
        mListView.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                    int position, long id) {
                final MessageInfoHolder message = (MessageInfoHolder) parent.getItemAtPosition(position);
                toggleMessageSelect(message);
                return true;
            }});

        // Correcting for screen rotation when in ActionMode
        mSelectedCount = getSelectionFromCheckboxes().size();
        if (mSelectedCount > 0) {
            mActionMode = MessageList.this.startActionMode(mActionModeCallback);
            mActionMode.setTitle(String.format(
                    getString(R.string.actionbar_selected),
                    mSelectedCount));
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent); // onNewIntent doesn't autoset our "internal" intent
        initializeMessageList(intent, false);
    }

    private void initializeMessageList(Intent intent, boolean create) {
        boolean returnFromMessageView = intent.getBooleanExtra(
                                            EXTRA_RETURN_FROM_MESSAGE_VIEW, false);

        if (!create && returnFromMessageView) {
            // We're returning from the MessageView activity with "Manage back button" enabled.
            // So just leave the activity in the state it was left in.
            return;
        }

        String accountUuid = intent.getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);

        if (mAccount != null && !mAccount.isAvailable(this)) {
            Log.i(K9.LOG_TAG, "not opening MessageList of unavailable account");
            onAccountUnavailable();
            return;
        }

        mFolderName = intent.getStringExtra(EXTRA_FOLDER);
        mQueryString = intent.getStringExtra(EXTRA_QUERY);

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

        mAdapter = new MessageListAdapter();
        restorePreviousData();

        if (mFolderName != null) {
            mCurrentFolder = mAdapter.getFolder(mFolderName, mAccount);
        }

        // Hide "Load up to x more" footer for search views
        mFooterView.setVisibility((mQueryString != null) ? View.GONE : View.VISIBLE);

        mController = MessagingController.getInstance(getApplication());
        mListView.setAdapter(mAdapter);
    }

    private void restorePreviousData() {
        final ActivityState previousData = getLastNonConfigurationInstance();

        if (previousData != null) {
            mAdapter.restoreMessages(previousData.messages);
            mActiveMessages = previousData.activeMessages;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mController.removeListener(mAdapter.mListener);
        saveListState();

        StorageManager.getInstance(getApplication()).removeListener(mStorageListener);
    }

    public void saveListState() {
        mState = new Bundle();
        mState.putInt(EXTRA_LIST_POSITION, mListView.getSelectedItemPosition());
    }

    public void restoreListState() {
        if (mState == null) {
            return;
        }

        int pos = mState.getInt(EXTRA_LIST_POSITION, ListView.INVALID_POSITION);

        if (pos >= mListView.getCount()) {
            pos = mListView.getCount() - 1;
        }

        if (pos == ListView.INVALID_POSITION) {
            mListView.setSelected(false);
        } else {
            mListView.setSelection(pos);
        }
    }

    /**
     * On resume we refresh messages for the folder that is currently open.
     * This guarantees that things like unread message count and read status
     * are updated.
     */
    @Override
    public void onResume() {
        super.onResume();

        if (mAccount != null && !mAccount.isAvailable(this)) {
            onAccountUnavailable();
            return;
        }
        StorageManager.getInstance(getApplication()).addListener(mStorageListener);

        mStars = K9.messageListStars();
        mCheckboxes = K9.messageListCheckboxes();

        // TODO Add support for pull to fresh on searches.
        if(mQueryString == null) {
            mPullToRefreshView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>() {
                @Override
                public void onRefresh(PullToRefreshBase<ListView> refreshView) {
                    checkMail(mAccount, mFolderName);
                }
            });
        } else {
            mPullToRefreshView.setMode(PullToRefreshBase.Mode.DISABLED);
        }

        mController.addListener(mAdapter.mListener);

        Account[] accountsWithNotification;

        Preferences prefs = Preferences.getPreferences(getApplicationContext());
        Account account = getCurrentAccount(prefs);

        if (account != null) {
            accountsWithNotification = new Account[] { account };
            mSortType = account.getSortType();
            mSortAscending = account.isSortAscending(mSortType);
            mSortDateAscending = account.isSortAscending(SortType.SORT_DATE);
        } else {
            accountsWithNotification = prefs.getAccounts();
            mSortType = K9.getSortType();
            mSortAscending = K9.isSortAscending(mSortType);
            mSortDateAscending = K9.isSortAscending(SortType.SORT_DATE);
        }

        for (Account accountWithNotification : accountsWithNotification) {
            mController.notifyAccountCancel(this, accountWithNotification);
        }

        if (mAdapter.isEmpty()) {
            if (mFolderName != null) {
                mController.listLocalMessages(mAccount, mFolderName,  mAdapter.mListener);
                // Hide the archive button if we don't have an archive folder.
                if (!mAccount.hasArchiveFolder()) {
//                    mBatchArchiveButton.setVisibility(View.GONE);
                }
            } else if (mQueryString != null) {
                mController.searchLocalMessages(mAccountUuids, mFolderNames, null, mQueryString, mIntegrate, mQueryFlags, mForbiddenFlags, mAdapter.mListener);
                // Don't show the archive button if this is a search.
//                mBatchArchiveButton.setVisibility(View.GONE);
            }

        } else {
            // reread the selected date format preference in case it has changed
            mMessageHelper.refresh();

            mAdapter.markAllMessagesAsDirty();

            new Thread() {
                @Override
                public void run() {
                    if (mFolderName != null) {
                        mController.listLocalMessagesSynchronous(mAccount, mFolderName,  mAdapter.mListener);
                    } else if (mQueryString != null) {
                        mController.searchLocalMessagesSynchronous(mAccountUuids, mFolderNames, null, mQueryString, mIntegrate, mQueryFlags, mForbiddenFlags, mAdapter.mListener);
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter.pruneDirtyMessages();
                            mAdapter.notifyDataSetChanged();
                            restoreListState();
                        }
                    });
                }

            }
            .start();
        }

        if (mAccount != null && mFolderName != null) {
            mController.getFolderUnreadMessageCount(mAccount, mFolderName, mAdapter.mListener);
        }

        refreshTitle();
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

    private void initializeLayout() {
        mListView = mPullToRefreshView.getRefreshableView();
        mListView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        mListView.setLongClickable(true);
        mListView.setFastScrollEnabled(true);
        mListView.setScrollingCacheEnabled(false);
        mListView.setOnItemClickListener(this);
        mListView.addFooterView(getFooterView(mListView));

        registerForContextMenu(mListView);
    }


    /**
     * Container for values to be kept while the device configuration is
     * modified at runtime (keyboard, orientation, etc.) and Android restarts
     * this activity.
     *
     * @see MessageList#onRetainNonConfigurationInstance()
     * @see MessageList#getLastNonConfigurationInstance()
     */
    static class ActivityState {
        public List<MessageInfoHolder> messages;
        public List<MessageInfoHolder> activeMessages;
    }

    /* (non-Javadoc)
     *
     * Method overridden for proper typing within this class (the return type is
     * more specific than the super implementation)
     *
     * @see android.app.Activity#onRetainNonConfigurationInstance()
     */
    @Override
    public ActivityState onRetainNonConfigurationInstance() {
        final ActivityState state = new ActivityState();
        state.messages = mAdapter.getMessages();
        state.activeMessages = mActiveMessages;
        return state;
    }

    /*
     * (non-Javadoc)
     *
     * Method overridden for proper typing within this class (the return type is
     * more specific than the super implementation)
     *
     * @see android.app.Activity#getLastNonConfigurationInstance()
     */
    @Override
    public ActivityState getLastNonConfigurationInstance() {
        return (ActivityState) super.getLastNonConfigurationInstance();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Shortcuts that work no matter what is selected
        switch (keyCode) {

            // messagelist is actually a K9Activity, not a K9ListActivity
            // This saddens me greatly, but to support volume key navigation
            // in MessageView, we implement this bit of wrapper code
        case KeyEvent.KEYCODE_VOLUME_UP: {
            if (K9.useVolumeKeysForListNavigationEnabled()) {
                int currentPosition = mListView.getSelectedItemPosition();
                if (currentPosition == AdapterView.INVALID_POSITION || mListView.isInTouchMode()) {
                    currentPosition = mListView.getFirstVisiblePosition();
                }
                if (currentPosition > 0) {
                    mListView.setSelection(currentPosition - 1);
                }
                return true;
            }
            return false;
        }
        case KeyEvent.KEYCODE_VOLUME_DOWN: {
            if (K9.useVolumeKeysForListNavigationEnabled()) {
                int currentPosition = mListView.getSelectedItemPosition();
                if (currentPosition == AdapterView.INVALID_POSITION || mListView.isInTouchMode()) {
                    currentPosition = mListView.getFirstVisiblePosition();
                }

                if (currentPosition < mListView.getCount()) {
                    mListView.setSelection(currentPosition + 1);
                }
                return true;
            }
            return false;
        }
        case KeyEvent.KEYCODE_C: {
            onCompose();
            return true;
        }
        case KeyEvent.KEYCODE_Q: {
            onShowFolderList();
            return true;
        }
        case KeyEvent.KEYCODE_O: {
            onCycleSort();
            return true;
        }
        case KeyEvent.KEYCODE_I: {
            changeSort(mSortType);
            return true;
        }
        case KeyEvent.KEYCODE_H: {
            Toast toast = Toast.makeText(this, R.string.message_list_help_key, Toast.LENGTH_LONG);
            toast.show();
            return true;
        }
        }

        boolean retval = true;
        int position = mListView.getSelectedItemPosition();
        try {
            if (position >= 0) {
                MessageInfoHolder message = (MessageInfoHolder) mAdapter.getItem(position);

                final List<MessageInfoHolder> selection = getSelectionFromMessage(message);

                if (message != null) {
                    switch (keyCode) {
                    case KeyEvent.KEYCODE_DEL: {
                        onDelete(selection);
                        return true;
                    }
                    case KeyEvent.KEYCODE_S: {
                        setSelected(selection, !message.selected);
                        return true;
                    }
                    case KeyEvent.KEYCODE_D: {
                        onDelete(selection);
                        return true;
                    }
                    case KeyEvent.KEYCODE_G: {
                        setFlag(selection, Flag.FLAGGED, !message.flagged);
                        return true;
                    }
                    case KeyEvent.KEYCODE_M: {
                        onMove(selection);
                        return true;
                    }
                    case KeyEvent.KEYCODE_V: {
                        onArchive(selection);
                        return true;
                    }
                    case KeyEvent.KEYCODE_Y: {
                        onCopy(selection);
                        return true;
                    }
                    case KeyEvent.KEYCODE_Z: {
                        setFlag(selection, Flag.SEEN, !message.read);
                        return true;
                    }
                    }
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

    private void onOpenMessage(MessageInfoHolder message) {
        if (message.folder.name.equals(message.message.getFolder().getAccount().getDraftsFolderName())) {
            MessageCompose.actionEditDraft(this, message.message.getFolder().getAccount(), message.message);
        } else {
            // Need to get the list before the sort starts
            ArrayList<MessageReference> messageRefs = new ArrayList<MessageReference>();

            for (MessageInfoHolder holder : mAdapter.getMessages()) {
                MessageReference ref = holder.message.makeMessageReference();
                messageRefs.add(ref);
            }

            MessageReference ref = message.message.makeMessageReference();
            Log.i(K9.LOG_TAG, "MessageList sending message " + ref);

            MessageView.actionView(this, ref, messageRefs, getIntent().getExtras());
        }

        /*
         * We set read=true here for UI performance reasons. The actual value
         * will get picked up on the refresh when the Activity is resumed but
         * that may take a second or so and we don't want this to show and
         * then go away. I've gone back and forth on this, and this gives a
         * better UI experience, so I am putting it back in.
         */
        if (!message.read) {
            message.read = true;
        }
    }

    private void onAccounts() {
        Accounts.listAccounts(this);
        finish();
    }

    private void onShowFolderList() {
        FolderList.actionHandleAccount(this, mAccount);
        finish();
    }

    private void onCompose() {
        if (mQueryString != null) {
            /*
             * If we have a query string, we don't have an account to let
             * compose start the default action.
             */
            MessageCompose.actionCompose(this, null);
        } else {
            MessageCompose.actionCompose(this, mAccount);
        }
    }

    private void onReply(MessageInfoHolder holder) {
        MessageCompose.actionReply(this, holder.message.getFolder().getAccount(), holder.message, false, null);
    }

    private void onReplyAll(MessageInfoHolder holder) {
        MessageCompose.actionReply(this, holder.message.getFolder().getAccount(), holder.message, true, null);
    }

    private void onForward(MessageInfoHolder holder) {
        MessageCompose.actionForward(this, holder.message.getFolder().getAccount(), holder.message, null);
    }

    private void onResendMessage(MessageInfoHolder message) {
        MessageCompose.actionEditDraft(this, message.message.getFolder().getAccount(), message.message);
    }

    private void onEditPrefs() {
        Prefs.actionPrefs(this);
    }

    private void onEditAccount() {
        AccountSettings.actionSettings(this, mAccount);
    }

    private void changeSort(SortType sortType) {
        Boolean sortAscending = (mSortType == sortType) ? !mSortAscending : null;
        changeSort(sortType, sortAscending);
    }

    /**
     * Change the sort type and sort order used for the message list.
     *
     * @param sortType
     *         Specifies which field to use for sorting the message list.
     * @param sortAscending
     *         Specifies the sort order. If this argument is {@code null} the default search order
     *         for the sort type is used.
     */
    // FIXME: Don't save the changes in the UI thread
    private void changeSort(SortType sortType, Boolean sortAscending) {
        mSortType = sortType;

        Preferences prefs = Preferences.getPreferences(getApplicationContext());
        Account account = getCurrentAccount(prefs);

        if (account != null) {
            account.setSortType(mSortType);

            if (sortAscending == null) {
                mSortAscending = account.isSortAscending(mSortType);
            } else {
                mSortAscending = sortAscending;
            }
            account.setSortAscending(mSortType, mSortAscending);
            mSortDateAscending = account.isSortAscending(SortType.SORT_DATE);

            account.save(prefs);
        } else {
            K9.setSortType(mSortType);

            if (sortAscending == null) {
                mSortAscending = K9.isSortAscending(mSortType);
            } else {
                mSortAscending = sortAscending;
            }
            K9.setSortAscending(mSortType, mSortAscending);
            mSortDateAscending = K9.isSortAscending(SortType.SORT_DATE);

            Editor editor = prefs.getPreferences().edit();
            K9.save(editor);
            editor.commit();
        }

        reSort();
    }

    private void reSort() {
        int toastString = mSortType.getToast(mSortAscending);

        Toast toast = Toast.makeText(this, toastString, Toast.LENGTH_SHORT);
        toast.show();

        mAdapter.sortMessages();
    }

    private void onCycleSort() {
        SortType[] sorts = SortType.values();
        int curIndex = 0;

        for (int i = 0; i < sorts.length; i++) {
            if (sorts[i] == mSortType) {
                curIndex = i;
                break;
            }
        }

        curIndex++;

        if (curIndex == sorts.length) {
            curIndex = 0;
        }

        changeSort(sorts[curIndex]);
    }

    /**
     * @param holders
     *            Never {@code null}.
     */
    private void onDelete(final List<MessageInfoHolder> holders) {
        final List<Message> messagesToRemove = new ArrayList<Message>();
        for (MessageInfoHolder holder : holders) {
            messagesToRemove.add(holder.message);
        }
        mAdapter.removeMessages(holders);
        mController.deleteMessages(messagesToRemove.toArray(EMPTY_MESSAGE_ARRAY), null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }

        switch (requestCode) {
        case ACTIVITY_CHOOSE_FOLDER_MOVE:
        case ACTIVITY_CHOOSE_FOLDER_COPY: {
            if (data == null) {
                return;
            }

            final String destFolderName = data.getStringExtra(ChooseFolder.EXTRA_NEW_FOLDER);
            final List<MessageInfoHolder> holders = mActiveMessages;

            if (destFolderName != null) {

                mActiveMessages = null; // don't need it any more

                final Account account = holders.get(0).message.getFolder().getAccount();
                account.setLastSelectedFolderName(destFolderName);

                switch (requestCode) {
                case ACTIVITY_CHOOSE_FOLDER_MOVE:
                    move(holders, destFolderName);
                    break;

                case ACTIVITY_CHOOSE_FOLDER_COPY:
                    copy(holders, destFolderName);
                    break;
                }
            }
            break;
        }
        }
    }

    private void onMarkAllAsRead(final Account account, final String folder) {
        if (K9.confirmMarkAllAsRead()) {
            showDialog(DIALOG_MARK_ALL_AS_READ);
        } else {
            markAllAsRead();
        }
    }

    private void markAllAsRead() {
        try {
            mController.markAllMessagesRead(mAccount, mCurrentFolder.name);

            for (MessageInfoHolder holder : mAdapter.getMessages()) {
                holder.read = true;
            }
            mAdapter.sortMessages();
        } catch (Exception e) {
            // Ignore
        }
    }

    private void onExpunge(final Account account, String folderName) {
        mController.expunge(account, folderName, null);
    }

    @Override
    public Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_MARK_ALL_AS_READ:
            return ConfirmationDialog.create(this, id,
                                             R.string.mark_all_as_read_dlg_title,
                                             getString(R.string.mark_all_as_read_dlg_instructions_fmt,
                                                     mCurrentFolder.displayName),
                                             R.string.okay_action,
                                             R.string.cancel_action,
            new Runnable() {
                @Override
                public void run() {
                    markAllAsRead();
                }
            });
        case R.id.dialog_confirm_spam:
            return ConfirmationDialog.create(this, id,
                                             R.string.dialog_confirm_spam_title,
                                             "" /* text is refreshed by #onPrepareDialog(int, Dialog) below */,
                                             R.string.dialog_confirm_spam_confirm_button,
                                             R.string.dialog_confirm_spam_cancel_button,
            new Runnable() {
                @Override
                public void run() {
                    onSpamConfirmed(mActiveMessages);
                    // No further need for this reference
                    mActiveMessages = null;
                }
            }, new Runnable() {
                @Override
                public void run() {
                    // event for cancel, we don't need this reference any more
                    mActiveMessages = null;
                }
            });
        }

        return super.onCreateDialog(id);
    }

    /*
     * (non-Javadoc)
     *
     * Android happens to invoke this method even if the given dialog is not
     * shown (eg. a dismissed dialog) as part of the automatic activity
     * reloading following a configuration change (orientation, keyboard,
     * locale, etc.).
     */
    @Override
    public void onPrepareDialog(final int id, final Dialog dialog) {
        switch (id) {
        case DIALOG_MARK_ALL_AS_READ: {
            if (mCurrentFolder != null) {
                ((AlertDialog)dialog).setMessage(getString(R.string.mark_all_as_read_dlg_instructions_fmt,
                                                 mCurrentFolder.displayName));
            }
            break;
        }
        case R.id.dialog_confirm_spam: {
            // mActiveMessages can be null if Android restarts the activity
            // while this dialog is not actually shown (but was displayed at
            // least once)
            if (mActiveMessages != null) {
                final int selectionSize = mActiveMessages.size();
                final String message;
                message = getResources().getQuantityString(R.plurals.dialog_confirm_spam_message, selectionSize,
                          Integer.valueOf(selectionSize));
                ((AlertDialog) dialog).setMessage(message);
            }
            break;
        }
        default: {
            super.onPrepareDialog(id, dialog);
        }
        }
    }

    private void onToggleRead(final List<MessageInfoHolder> holders) {
        LocalMessage message;
        Folder folder;
        Account account;
        String folderName;

        int i = 0;
        for (final Iterator<MessageInfoHolder> iterator = holders.iterator(); iterator.hasNext(); i++) {
            final MessageInfoHolder messageInfo = iterator.next();
            message = messageInfo.message;
            folder = message.getFolder();
            account = folder.getAccount();
            folderName = message.getFolder().getName();

            mController.setFlag(account, folderName, new Message[]{message}, Flag.SEEN, !messageInfo.read);

            messageInfo.read = !messageInfo.read;
            mAdapter.sortMessages();
        }
    }

    private void onToggleFlag(final List<MessageInfoHolder> holders) {
        LocalMessage message;
        Folder folder;
        Account account;
        String folderName;

        int i = 0;
        for (final Iterator<MessageInfoHolder> iterator = holders.iterator(); iterator.hasNext(); i++) {
            final MessageInfoHolder messageInfo = iterator.next();
            message = messageInfo.message;
            folder = message.getFolder();
            account = folder.getAccount();
            folderName = message.getFolder().getName();

            mController.setFlag(account, folderName, new Message[]{message}, Flag.FLAGGED, !messageInfo.flagged);

            messageInfo.flagged = !messageInfo.flagged;
            mAdapter.sortMessages();
        }
    }

    private void checkMail(Account account, String folderName) {
        mController.synchronizeMailbox(account, folderName, mAdapter.mListener, null);
        mController.sendPendingMessages(account, mAdapter.mListener);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
        case android.R.id.home: {
            if (mQueryString == null) {
        onShowFolderList();
            } else {
        onAccounts();
            }
            return true;
        }
        case R.id.compose: {
            onCompose();
            return true;
        }
        case R.id.check_mail: {
        checkMail(mAccount, mFolderName);
        return true;
        }
        case R.id.set_sort_date: {
            changeSort(SortType.SORT_DATE);
            return true;
        }
        case R.id.set_sort_arrival: {
            changeSort(SortType.SORT_ARRIVAL);
            return true;
        }
        case R.id.set_sort_subject: {
            changeSort(SortType.SORT_SUBJECT);
            return true;
        }
        case R.id.set_sort_sender: {
            changeSort(SortType.SORT_SENDER);
            return true;
        }
        case R.id.set_sort_flag: {
            changeSort(SortType.SORT_FLAGGED);
            return true;
        }
        case R.id.set_sort_unread: {
            changeSort(SortType.SORT_UNREAD);
            return true;
        }
        case R.id.set_sort_attach: {
            changeSort(SortType.SORT_ATTACHMENT);
            return true;
        }
        case R.id.select_all:
        toggleAllSelected();
        return true;
        case R.id.app_settings: {
            onEditPrefs();
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
            mController.sendPendingMessages(mAccount, mAdapter.mListener);
            return true;
        }
        case R.id.mark_all_as_read: {
            if (mFolderName != null) {
                onMarkAllAsRead(mAccount, mFolderName);
            }
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
            if (mCurrentFolder != null) {
                onExpunge(mAccount, mCurrentFolder.name);
            }
            return true;
        }
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getSupportMenuInflater().inflate(R.menu.message_list_option, menu);
        mRefreshMenuItem = menu.findItem(R.id.check_mail);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        if (mQueryString != null) {
            menu.findItem(R.id.mark_all_as_read).setVisible(false);
            menu.findItem(R.id.expunge).setVisible(false);
            menu.findItem(R.id.check_mail).setVisible(false);
            menu.findItem(R.id.send_messages).setVisible(false);
            menu.findItem(R.id.folder_settings).setVisible(false);
            menu.findItem(R.id.account_settings).setVisible(false);
        } else {
            if (mCurrentFolder != null && mCurrentFolder.name.equals(mAccount.getOutboxFolderName())) {
                menu.findItem(R.id.check_mail).setVisible(false);
            } else {
                menu.findItem(R.id.send_messages).setVisible(false);
            }

            if (mCurrentFolder != null && K9.ERROR_FOLDER_NAME.equals(mCurrentFolder.name)) {
                menu.findItem(R.id.expunge).setVisible(false);
            }

            if (!mController.isMoveCapable(mAccount)) {
                // FIXME: Really we want to do this for all local-only folders
                if (mCurrentFolder != null &&
                        !mAccount.getInboxFolderName().equals(mCurrentFolder.name)) {
                    menu.findItem(R.id.check_mail).setVisible(false);
                }
                menu.findItem(R.id.expunge).setVisible(false);
            }
        }

        return true;
    }

    class MessageListAdapter extends BaseAdapter {
        private final List<MessageInfoHolder> mMessages =
                Collections.synchronizedList(new ArrayList<MessageInfoHolder>());

        private final ActivityListener mListener = new ActivityListener() {

            @Override
            public void informUserOfStatus() {
                mHandler.refreshTitle();
            }

            @Override
            public void synchronizeMailboxStarted(Account account, String folder) {
                if (updateForMe(account, folder)) {
                    mHandler.progress(true);
                    mHandler.folderLoading(folder, true);
                }
                super.synchronizeMailboxStarted(account, folder);
            }

            @Override
            public void synchronizeMailboxFinished(Account account, String folder,
            int totalMessagesInMailbox, int numNewMessages) {

                if (updateForMe(account, folder)) {
                    mHandler.progress(false);
                    mHandler.folderLoading(folder, false);
                    mHandler.sortMessages();
                }
                super.synchronizeMailboxFinished(account, folder, totalMessagesInMailbox, numNewMessages);
            }

            @Override
            public void synchronizeMailboxFailed(Account account, String folder, String message) {

                if (updateForMe(account, folder)) {
                    mHandler.progress(false);
                    mHandler.folderLoading(folder, false);
                    mHandler.sortMessages();
                }
                super.synchronizeMailboxFailed(account, folder, message);
            }

            @Override
            public void synchronizeMailboxAddOrUpdateMessage(Account account, String folder, Message message) {
                mHandler.addOrUpdateMessages(account, folder, Collections.singletonList(message), true);
            }

            @Override
            public void synchronizeMailboxRemovedMessage(Account account, String folder, Message message) {
                mHandler.removeMessage(message.makeMessageReference());
            }

            @Override
            public void listLocalMessagesStarted(Account account, String folder) {
                if ((mQueryString != null && folder == null) || (account != null && account.equals(mAccount))) {
                    mHandler.progress(true);
                    if (folder != null) {
                        mHandler.folderLoading(folder, true);
                    }
                }
            }

            @Override
            public void listLocalMessagesFailed(Account account, String folder, String message) {
                if ((mQueryString != null && folder == null) || (account != null && account.equals(mAccount))) {
                    mHandler.sortMessages();
                    mHandler.progress(false);
                    if (folder != null) {
                        mHandler.folderLoading(folder, false);
                    }
                }
            }

            @Override
            public void listLocalMessagesFinished(Account account, String folder) {
                if ((mQueryString != null && folder == null) || (account != null && account.equals(mAccount))) {
                    mHandler.sortMessages();
                    mHandler.progress(false);
                    if (folder != null) {
                        mHandler.folderLoading(folder, false);
                    }
                }
            }

            @Override
            public void listLocalMessagesRemoveMessage(Account account, String folder, Message message) {
                mHandler.removeMessage(message.makeMessageReference());
            }

            @Override
            public void listLocalMessagesAddMessages(Account account, String folder, List<Message> messages) {
                mHandler.addOrUpdateMessages(account, folder, messages, false);
            }

            @Override
            public void listLocalMessagesUpdateMessage(Account account, String folder, Message message) {
                mHandler.addOrUpdateMessages(account, folder, Collections.singletonList(message), false);
            }

            @Override
            public void searchStats(AccountStats stats) {
                mUnreadMessageCount = stats.unreadMessageCount;
                super.searchStats(stats);
            }

            @Override
            public void folderStatusChanged(Account account, String folder, int unreadMessageCount) {
                if (updateForMe(account, folder)) {
                    mUnreadMessageCount = unreadMessageCount;
                }
                super.folderStatusChanged(account, folder, unreadMessageCount);
            }

            @Override
            public void messageUidChanged(Account account, String folder, String oldUid, String newUid) {
                MessageReference ref = new MessageReference();
                ref.accountUuid = account.getUuid();
                ref.folderName = folder;
                ref.uid = oldUid;

                mHandler.changeMessageUid(ref, newUid);
            }
        };

        private boolean updateForMe(Account account, String folder) {
            if ((account.equals(mAccount) && mFolderName != null && folder.equals(mFolderName))) {
                return true;
            } else {
                return false;
            }
        }

        public List<MessageInfoHolder> getMessages() {
            return mMessages;
        }

        public void restoreMessages(List<MessageInfoHolder> messages) {
            mMessages.addAll(messages);
        }

        private Drawable mAttachmentIcon;
        private Drawable mForwardedIcon;
        private Drawable mAnsweredIcon;
        private Drawable mForwardedAnsweredIcon;

        MessageListAdapter() {
            mAttachmentIcon = getResources().getDrawable(R.drawable.ic_email_attachment_small);
            mAnsweredIcon = getResources().getDrawable(R.drawable.ic_email_answered_small);
            mForwardedIcon = getResources().getDrawable(R.drawable.ic_email_forwarded_small);
            mForwardedAnsweredIcon = getResources().getDrawable(R.drawable.ic_email_forwarded_answered_small);
        }

        public void markAllMessagesAsDirty() {
            for (MessageInfoHolder holder : mMessages) {
                holder.dirty = true;
            }
        }

        public void pruneDirtyMessages() {
            List<MessageInfoHolder> messagesToRemove = new ArrayList<MessageInfoHolder>();

            for (MessageInfoHolder holder : mMessages) {
                if (holder.dirty) {
                    messagesToRemove.add(holder);
                }
            }
            removeMessages(messagesToRemove);
        }

        public void removeMessage(MessageReference messageReference) {
            MessageInfoHolder holder = getMessage(messageReference);
            if (holder == null) {
                Log.w(K9.LOG_TAG, "Got callback to remove non-existent message with UID " +
                        messageReference.uid);
            } else {
                removeMessages(Collections.singletonList(holder));
            }
        }

        public void removeMessages(final List<MessageInfoHolder> messages) {
            if (messages.isEmpty()) {
                return;
            }

            for (MessageInfoHolder message : messages) {
                if (message != null && (mFolderName == null || (
                        message.folder != null &&
                        message.folder.name.equals(mFolderName)))) {
                    if (message.selected && mSelectedCount > 0) {
                        mSelectedCount--;
                    }
                    mMessages.remove(message);
                }
            }
            resetUnreadCount();

            notifyDataSetChanged();
        }

        /**
         * Set the selection state for all messages at once.
         * @param selected Selection state to set.
         */
        public void setSelectionForAllMesages(final boolean selected) {
            for (MessageInfoHolder message : mMessages) {
                message.selected = selected;
            }

            notifyDataSetChanged();
        }

        public void addMessages(final List<MessageInfoHolder> messages) {
            if (messages.isEmpty()) {
                return;
            }

            final boolean wasEmpty = mMessages.isEmpty();

            for (final MessageInfoHolder message : messages) {
                if (mFolderName == null || (message.folder != null && message.folder.name.equals(mFolderName))) {
                    int index = Collections.binarySearch(mMessages, message, getComparator());

                    if (index < 0) {
                        index = (index * -1) - 1;
                    }

                    mMessages.add(index, message);
                }
            }

            if (wasEmpty) {
                mListView.setSelection(0);
            }
            resetUnreadCount();

            notifyDataSetChanged();
        }

        public void changeMessageUid(MessageReference ref, String newUid) {
            MessageInfoHolder holder = getMessage(ref);
            if (holder != null) {
                holder.uid = newUid;
                holder.message.setUid(newUid);
            }
        }

        public void resetUnreadCount() {
            if (mQueryString != null) {
                int unreadCount = 0;

                for (MessageInfoHolder holder : mMessages) {
                    unreadCount += holder.read ? 0 : 1;
                }

                mUnreadMessageCount = unreadCount;
                refreshTitle();
            }
        }

        public void sortMessages() {
            final Comparator<MessageInfoHolder> chainComparator = getComparator();

            Collections.sort(mMessages, chainComparator);

            notifyDataSetChanged();
        }

        public void addOrUpdateMessages(final Account account, final String folderName,
                final List<Message> messages, final boolean verifyAgainstSearch) {

            boolean needsSort = false;
            final List<MessageInfoHolder> messagesToAdd = new ArrayList<MessageInfoHolder>();
            List<MessageInfoHolder> messagesToRemove = new ArrayList<MessageInfoHolder>();
            List<Message> messagesToSearch = new ArrayList<Message>();

            // cache field into local variable for faster access for JVM without JIT
            final MessageHelper messageHelper = mMessageHelper;

            for (Message message : messages) {
                MessageInfoHolder m = getMessage(message);
                if (message.isSet(Flag.DELETED)) {
                    if (m != null) {
                        messagesToRemove.add(m);
                    }
                } else {
                    final Folder messageFolder = message.getFolder();
                    final Account messageAccount = messageFolder.getAccount();
                    if (m == null) {
                        if (updateForMe(account, folderName)) {
                            m = new MessageInfoHolder();
                            FolderInfoHolder folderInfoHolder = new FolderInfoHolder(
                                    MessageList.this, messageFolder, messageAccount);
                            messageHelper.populate(m, message, folderInfoHolder, messageAccount);
                            messagesToAdd.add(m);
                        } else {
                            if (mQueryString != null) {
                                if (verifyAgainstSearch) {
                                    messagesToSearch.add(message);
                                } else {
                                    m = new MessageInfoHolder();
                                    FolderInfoHolder folderInfoHolder = new FolderInfoHolder(
                                            MessageList.this, messageFolder, messageAccount);
                                    messageHelper.populate(m, message, folderInfoHolder,
                                            messageAccount);
                                    messagesToAdd.add(m);
                                }
                            }
                        }
                    } else {
                        m.dirty = false; // as we reload the message, unset its dirty flag
                        FolderInfoHolder folderInfoHolder = new FolderInfoHolder(MessageList.this,
                                messageFolder, account);
                        messageHelper.populate(m, message, folderInfoHolder, account);
                        needsSort = true;
                    }
                }
            }

            if (!messagesToSearch.isEmpty()) {
                mController.searchLocalMessages(mAccountUuids, mFolderNames,
                        messagesToSearch.toArray(EMPTY_MESSAGE_ARRAY), mQueryString, mIntegrate,
                        mQueryFlags, mForbiddenFlags,
                new MessagingListener() {
                    @Override
                    public void listLocalMessagesAddMessages(Account account, String folder,
                            List<Message> messages) {
                        mHandler.addOrUpdateMessages(account, folder, messages, false);
                    }
                });
            }

            if (!messagesToRemove.isEmpty()) {
                removeMessages(messagesToRemove);
            }

            if (!messagesToAdd.isEmpty()) {
                addMessages(messagesToAdd);
            }

            if (needsSort) {
                sortMessages();
                resetUnreadCount();
            }
        }

        /**
         * Find a specific message in the message list.
         *
         * <p><strong>Note:</strong>
         * This method was optimized because it is called a lot. Don't change it unless you know
         * what you are doing.</p>
         *
         * @param message
         *         A {@link Message} instance describing the message to look for.
         *
         * @return The corresponding {@link MessageInfoHolder} instance if the message was found in
         *         the message list. {@code null} otherwise.
         */
        private MessageInfoHolder getMessage(Message message) {
            String uid;
            Folder folder;
            for (MessageInfoHolder holder : mMessages) {
                uid = message.getUid();
                if (holder.uid == uid || uid.equals(holder.uid)) {
                    folder = message.getFolder();
                     if (holder.folder.name.equals(folder.getName()) &&
                             holder.account.equals(folder.getAccount().getUuid())) {
                         return holder;
                     }
                }
            }

            return null;
        }

        /**
         * Find a specific message in the message list.
         *
         * <p><strong>Note:</strong>
         * This method was optimized because it is called a lot. Don't change it unless you know
         * what you are doing.</p>
         *
         * @param messageReference
         *         A {@link MessageReference} instance describing the message to look for.
         *
         * @return The corresponding {@link MessageInfoHolder} instance if the message was found in
         *         the message list. {@code null} otherwise.
         */
        private MessageInfoHolder getMessage(MessageReference messageReference) {
            String uid;
            for (MessageInfoHolder holder : mMessages) {
                uid = messageReference.uid;
                if ((holder.uid == uid || uid.equals(holder.uid)) &&
                        holder.folder.name.equals(messageReference.folderName) &&
                        holder.account.equals(messageReference.accountUuid)) {
                     return holder;
                }
            }

            return null;
        }

        public FolderInfoHolder getFolder(String folder, Account account) {
            LocalFolder local_folder = null;
            try {
                LocalStore localStore = account.getLocalStore();
                local_folder = localStore.getFolder(folder);
                return new FolderInfoHolder(context, local_folder, account);
            } catch (Exception e) {
                Log.e(K9.LOG_TAG, "getFolder(" + folder + ") goes boom: ", e);
                return null;
            } finally {
                if (local_folder != null) {
                    local_folder.close();
                }
            }
        }

        private final OnClickListener flagClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Perform action on clicks
                MessageInfoHolder message = (MessageInfoHolder) getItem((Integer)v.getTag());
                onToggleFlag(Arrays.asList(new MessageInfoHolder[]{message}));
            }
        };

        private final OnClickListener itemMenuClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Perform action on clicks
//                setAllSelected(false);
//                openContextMenu(v);
                MenuBuilder menu = new MenuBuilder(MessageList.this);
                menu.add("something");
                new MenuPopupHelper(MessageList.this, menu, v).show();
            }
        };

        @Override
        public int getCount() {
            return mMessages.size();
        }

        @Override
        public long getItemId(int position) {
            try {
                MessageInfoHolder messageHolder = (MessageInfoHolder) getItem(position);
                if (messageHolder != null) {
                    return messageHolder.message.getId();
                }
            } catch (Exception e) {
                Log.i(K9.LOG_TAG, "getItemId(" + position + ") ", e);
            }
            return -1;
        }

        @Override
        public Object getItem(int position) {
            try {
                if (position < mMessages.size()) {
                    return mMessages.get(position);
                }
            } catch (Exception e) {
                Log.e(K9.LOG_TAG, "getItem(" + position + "), but folder.messages.size() = " + mMessages.size(), e);
            }
            return null;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            MessageInfoHolder message = (MessageInfoHolder) getItem(position);
            View view;

            if ((convertView != null) && (convertView.getId() == R.layout.message_list_item)) {
                view = convertView;
            } else {
                view = mInflater.inflate(R.layout.message_list_item, parent, false);
                view.setId(R.layout.message_list_item);
            }

            MessageViewHolder holder = (MessageViewHolder) view.getTag();

            if (holder == null) {
                holder = new MessageViewHolder();
                holder.subject = (TextView) view.findViewById(R.id.subject);
                holder.from = (TextView) view.findViewById(R.id.from);
                holder.date = (TextView) view.findViewById(R.id.date);
                holder.chip = view.findViewById(R.id.chip);
                holder.preview = (TextView) view.findViewById(R.id.preview);
                holder.selected = (CheckBox) view.findViewById(R.id.selected_checkbox);
                holder.flagged = (CheckBox) view.findViewById(R.id.flagged);
                holder.itemMenu = (ImageButton) view.findViewById(R.id.item_menu);
                holder.flagged.setOnClickListener(flagClickListener);
                holder.itemMenu.setOnClickListener(itemMenuClickListener);

                if (!mStars) {
                    holder.flagged.setVisibility(View.GONE);
                }

                if (mCheckboxes) {
                    holder.selected.setVisibility(View.VISIBLE);
                }

                if (holder.selected != null) {
                    holder.selected.setOnCheckedChangeListener(holder);
                }
                holder.subject.setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSizes.getMessageListSubject());
                holder.date.setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSizes.getMessageListDate());

                holder.preview.setLines(mPreviewLines);
                holder.preview.setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSizes.getMessageListPreview());
                holder.itemMenu.setFocusable(false);

                view.setTag(holder);
            }

            if (message != null) {
                bindView(position, view, holder, message);
            } else {
                // This branch code is triggered when the local store
                // hands us an invalid message

                holder.chip.getBackground().setAlpha(0);
                holder.subject.setText(getString(R.string.general_no_subject));
                holder.subject.setTypeface(null, Typeface.NORMAL);
                String noSender = getString(R.string.general_no_sender);
                if (holder.preview != null) {
                    holder.preview.setText(noSender, TextView.BufferType.SPANNABLE);
                    Spannable str = (Spannable) holder.preview.getText();

                    str.setSpan(new StyleSpan(Typeface.NORMAL),
                                0,
                                noSender.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    str.setSpan(new AbsoluteSizeSpan(mFontSizes.getMessageListSender(), true),
                                0,
                                noSender.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else {
                    holder.from.setText(noSender);
                    holder.from.setTypeface(null, Typeface.NORMAL);
                    holder.from.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                }

                holder.date.setText(getString(R.string.general_no_date));

                //WARNING: Order of the next 2 lines matter
                holder.position = -1;
                holder.selected.setChecked(false);

                if (!mCheckboxes) {
                    holder.selected.setVisibility(View.GONE);
                }
                holder.flagged.setChecked(false);
            }


            return view;
        }

        /**
         * Associate model data to view object.
         *
         * @param position
         *            The position of the item within the adapter's data set of
         *            the item whose view we want.
         * @param view
         *            Main view component to alter. Never <code>null</code>.
         * @param holder
         *            Convenience view holder - eases access to <tt>view</tt>
         *            child views. Never <code>null</code>.
         * @param message
         *            Never <code>null</code>.
         */
        private void bindView(final int position, final View view, final MessageViewHolder holder,
                              final MessageInfoHolder message) {
            holder.subject.setTypeface(null, message.read ? Typeface.NORMAL : Typeface.BOLD);

            // XXX TODO there has to be some way to walk our view hierarchy and get this
            holder.flagged.setTag(position);
            holder.flagged.setChecked(message.flagged);

            // So that the mSelectedCount is only incremented/decremented
            // when a user checks the checkbox (vs code)
            holder.position = -1;
            holder.selected.setChecked(message.selected);

            if (!mCheckboxes) {
                holder.selected.setVisibility(message.selected ? View.VISIBLE : View.GONE);
            }



            holder.chip.setBackgroundDrawable(message.message.getFolder().getAccount().generateColorChip(message.read).drawable());
            // TODO: Make these colors part of the theme

//            if (K9.getK9Theme() == K9.THEME_LIGHT) {
//                // Light theme: light grey background for read messages
//                view.setBackgroundColor(message.read ?  Color.rgb(229, 229, 229) : Color.rgb(255, 255, 255));
//            } else {
//                // Dark theme: dark grey background for unread messages
//                view.setBackgroundColor(message.read ? 0 : Color.rgb(45, 45, 45));
//            }

            if ((message.message.getSubject() == null) || message.message.getSubject().equals("")) {
                holder.subject.setText(getText(R.string.general_no_subject));
            } else {
                holder.subject.setText(message.message.getSubject());
            }

            int senderTypeface = message.read ? Typeface.NORMAL : Typeface.BOLD;
            if (holder.preview != null) {
                /*
                 * Because text views can't wrap around each other(?) we
                 * compose a custom view containing the preview and the
                 * from.
                 */

                holder.preview.setText(new SpannableStringBuilder(recipientSigil(message))
                                       .append(message.sender).append(" ").append(message.message.getPreview()),
                                       TextView.BufferType.SPANNABLE);
                Spannable str = (Spannable)holder.preview.getText();

                // Create a span section for the sender, and assign the correct font size and weight.
                str.setSpan(new StyleSpan(senderTypeface),
                            0,
                            message.sender.length() + 1,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                str.setSpan(new AbsoluteSizeSpan(mFontSizes.getMessageListSender(), true),
                            0,
                            message.sender.length() + 1,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                // TODO: Make these colors part of the theme
                int color = (K9.getK9Theme() == K9.THEME_LIGHT) ?
                        Color.rgb(105, 105, 105) :
                        Color.rgb(160, 160, 160);

                // set span for preview message.
                str.setSpan(new ForegroundColorSpan(color), // How do I can specify the android.R.attr.textColorTertiary
                            message.sender.length() + 1,
                            str.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                holder.from.setText(new SpannableStringBuilder(recipientSigil(message)).append(message.sender));

                holder.from.setTypeface(null, senderTypeface);
            }

            holder.date.setText(message.getDate(mMessageHelper));

            Drawable statusHolder = null;
            if (message.forwarded && message.answered) {
                statusHolder = mForwardedAnsweredIcon;
            } else if (message.answered) {
                statusHolder = mAnsweredIcon;
            } else if (message.forwarded) {
                statusHolder = mForwardedIcon;
            }
            holder.subject.setCompoundDrawablesWithIntrinsicBounds(statusHolder, // left
                    null, // top
                    message.message.hasAttachments() ? mAttachmentIcon : null, // right
                    null); // bottom
            holder.position = position;
        }

        private String recipientSigil(MessageInfoHolder message) {
            if (message.message.toMe()) {
                return getString(R.string.messagelist_sent_to_me_sigil);
            } else if (message.message.ccMe()) {
                return getString(R.string.messagelist_sent_cc_me_sigil);
            } else {
                return "";
            }
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }

    class MessageViewHolder
        implements OnCheckedChangeListener {
        public TextView subject;
        public TextView preview;
        public TextView from;
        public TextView time;
        public TextView date;
        public CheckBox flagged;
        public View chip;
        public CheckBox selected;
        public ImageButton itemMenu;
        public int position = -1;

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (position != -1) {
                MessageInfoHolder message = (MessageInfoHolder) mAdapter.getItem(position);
                    toggleMessageSelect(message);
                    if (!mCheckboxes) {
                         selected.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                    }


            }
        }
    }


    private View getFooterView(ViewGroup parent) {
        if (mFooterView == null) {
            mFooterView = mInflater.inflate(R.layout.message_list_item_footer, parent, false);
            mFooterView.setId(R.layout.message_list_item_footer);
            FooterViewHolder holder = new FooterViewHolder();
            holder.progress = (ProgressBar) mFooterView.findViewById(R.id.message_list_progress);
            holder.progress.setIndeterminate(true);
            holder.main = (TextView) mFooterView.findViewById(R.id.main_text);
            mFooterView.setTag(holder);
        }

        return mFooterView;
    }

    private void updateFooterView() {
        FooterViewHolder holder = (FooterViewHolder) mFooterView.getTag();

        if (mCurrentFolder != null && mAccount != null) {
            if (mCurrentFolder.loading) {
                holder.main.setText(getString(R.string.status_loading_more));
                holder.progress.setVisibility(ProgressBar.VISIBLE);
            } else {
                if (!mCurrentFolder.lastCheckFailed) {
                    if (mAccount.getDisplayCount() == 0) {
                        holder.main.setText(getString(R.string.message_list_load_more_messages_action));
                    } else {
                        holder.main.setText(String.format(getString(R.string.load_more_messages_fmt), mAccount.getDisplayCount()));
                    }
                } else {
                    holder.main.setText(getString(R.string.status_loading_more_failed));
                }
                holder.progress.setVisibility(ProgressBar.INVISIBLE);
            }
        } else {
            holder.progress.setVisibility(ProgressBar.INVISIBLE);
        }
    }

    static class FooterViewHolder {
        public ProgressBar progress;
        public TextView main;
    }

    private void setAllSelected(boolean isSelected) {
        mSelectedCount = 0;

        for (MessageInfoHolder holder : mAdapter.getMessages()) {
            holder.selected = isSelected;
            mSelectedCount += (isSelected ? 1 : 0);
        }

        mAdapter.notifyDataSetChanged();
    }

    /**
     * Toggle all selected message states.  Sort of.  If anything selected, unselect everything.  If nothing is
     * selected, select everything.
     */
    private void toggleAllSelected() {
        boolean newState = true;

        // If there was anything selected, unselect everything.
        if (mSelectedCount > 0) {
            newState = false;
        }

        mAdapter.setSelectionForAllMesages(newState);

        if (newState) {
            mSelectedCount = mAdapter.getCount();
            mActionMode = MessageList.this.startActionMode(mActionModeCallback);
            mActionMode.setTitle(String.format(getString(R.string.actionbar_selected), mSelectedCount));
        } else {
            mSelectedCount = 0;
            mActionMode.finish();
        }
    }

    private void setSelected(final List<MessageInfoHolder> holders, final boolean newState) {
        for (final MessageInfoHolder holder : holders) {
            if (holder.selected != newState) {
                holder.selected = newState;
                mSelectedCount += (newState ? 1 : -1);
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    private void toggleMessageSelect(final MessageInfoHolder holder){
        if (mActionMode != null) {
            if (mSelectedCount == 1 && holder.selected) {
                mActionMode.finish();
                return;
            }
        } else {
            mActionMode = MessageList.this.startActionMode(mActionModeCallback);
     }



    if (holder.selected) {
        holder.selected = false;
        mSelectedCount -= 1;
    } else {
        holder.selected = true;
        mSelectedCount += 1;
    }
        mAdapter.notifyDataSetChanged();
        mActionMode.setTitle(String.format(getString(R.string.actionbar_selected), mSelectedCount));

        // make sure the onPrepareActionMode is called
        mActionMode.invalidate();
    }

    /**
     * @param holders
     *            Messages to update. Never {@code null}.
     * @param flag
     *            Flag to be updated on the specified messages. Never
     *            {@code null}.
     * @param newState
     *            State to set for the given flag.
     */
    private void setFlag(final List<MessageInfoHolder> holders, final Flag flag, final boolean newState) {
        if (holders.isEmpty()) {
            return;
        }
        final Message[] messageList = new Message[holders.size()];
        int i = 0;
        for (final Iterator<MessageInfoHolder> iterator = holders.iterator(); iterator.hasNext(); i++) {
            final MessageInfoHolder holder = iterator.next();
            messageList[i] = holder.message;
            if (flag == Flag.SEEN) {
                holder.read = newState;
            } else if (flag == Flag.FLAGGED) {
                holder.flagged = newState;
            }
        }
        mController.setFlag(messageList, flag, newState);
        mAdapter.sortMessages();
    }

    /**
     * Display the message move activity.
     *
     * @param holders
     *            Never {@code null}.
     */
    private void onMove(final List<MessageInfoHolder> holders) {
        if (!checkCopyOrMovePossible(holders, FolderOperation.MOVE)) {
            return;
        }

        final Folder folder = holders.size() == 1 ? holders.get(0).message.getFolder() : mCurrentFolder.folder;
        displayFolderChoice(ACTIVITY_CHOOSE_FOLDER_MOVE, folder, holders);
    }

    /**
     * Display the message copy activity.
     *
     * @param holders
     *            Never {@code null}.
     */
    private void onCopy(final List<MessageInfoHolder> holders) {
        if (!checkCopyOrMovePossible(holders, FolderOperation.COPY)) {
            return;
        }

        final Folder folder = holders.size() == 1 ? holders.get(0).message.getFolder() : mCurrentFolder.folder;
        displayFolderChoice(ACTIVITY_CHOOSE_FOLDER_COPY, folder, holders);
    }

    /**
     * Helper method to manage the invocation of
     * {@link #startActivityForResult(Intent, int)} for a folder operation
     * ({@link ChooseFolder} activity), while saving a list of associated
     * messages.
     *
     * @param requestCode
     *            If >= 0, this code will be returned in onActivityResult() when
     *            the activity exits.
     * @param folder
     *            Never {@code null}.
     * @param holders
     *            Messages to be affected by the folder operation. Never
     *            {@code null}.
     * @see #startActivityForResult(Intent, int)
     */
    private void displayFolderChoice(final int requestCode, final Folder folder, final List<MessageInfoHolder> holders) {
        final Intent intent = new Intent(this, ChooseFolder.class);
        intent.putExtra(ChooseFolder.EXTRA_ACCOUNT, folder.getAccount().getUuid());
        intent.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, folder.getName());
        intent.putExtra(ChooseFolder.EXTRA_SEL_FOLDER, folder.getAccount().getLastSelectedFolderName());
        // remember the selected messages for #onActivityResult
        mActiveMessages = holders;
        startActivityForResult(intent, requestCode);
    }

    /**
     * @param holders
     *            Never {@code null}.
     */
    private void onArchive(final List<MessageInfoHolder> holders) {
        final String folderName = holders.get(0).message.getFolder().getAccount().getArchiveFolderName();
        if (K9.FOLDER_NONE.equalsIgnoreCase(folderName)) {
            return;
        }
        // TODO one should separate messages by account and call move afterwards
        // (because each account might have a specific Archive folder name)
        move(holders, folderName);
    }

    /**
     * @param holders
     *            Never {@code null}.
     */
    private void onSpam(final List<MessageInfoHolder> holders) {
        if (K9.confirmSpam()) {
            // remember the message selection for #onCreateDialog(int)
            mActiveMessages = holders;
            showDialog(R.id.dialog_confirm_spam);
        } else {
            onSpamConfirmed(holders);
        }
    }

    /**
     * @param holders
     *            Never {@code null}.
     */
    private void onSpamConfirmed(final List<MessageInfoHolder> holders) {
        final String folderName = holders.get(0).message.getFolder().getAccount().getSpamFolderName();
        if (K9.FOLDER_NONE.equalsIgnoreCase(folderName)) {
            return;
        }
        // TODO one should separate messages by account and call move afterwards
        // (because each account might have a specific Spam folder name)
        move(holders, folderName);
    }

    private static enum FolderOperation {
        COPY, MOVE
    }

    /**
     * Display an Toast message if any message isn't synchronized
     *
     * @param holders
     *            Never <code>null</code>.
     * @param operation
     *            Never {@code null}.
     *
     * @return <code>true</code> if operation is possible
     */
    private boolean checkCopyOrMovePossible(final List<MessageInfoHolder> holders, final FolderOperation operation) {
        if (holders.isEmpty()) {
            return false;
        }
        boolean first = true;
        for (final MessageInfoHolder holder : holders) {
            final Message message = holder.message;
            if (first) {
                first = false;
                // account check
                final Account account = message.getFolder().getAccount();
                if ((operation == FolderOperation.MOVE && !mController.isMoveCapable(account)) || (operation == FolderOperation.COPY && !mController.isCopyCapable(account))) {
                    return false;
                }
            }
            // message check
            if ((operation == FolderOperation.MOVE && !mController.isMoveCapable(message)) || (operation == FolderOperation.COPY && !mController.isCopyCapable(message))) {
                final Toast toast = Toast.makeText(this, R.string.move_copy_cannot_copy_unsynced_message,
                                                   Toast.LENGTH_LONG);
                toast.show();
                return false;
            }
        }
        return true;
    }

    /**
     * Helper method to get a List of message ready to be processed. This implementation will return a list containing the sole argument.
     *
     * @param holder Never {@code null}.
     * @return Never {@code null}.
     */
    private List<MessageInfoHolder> getSelectionFromMessage(final MessageInfoHolder holder) {
        final List<MessageInfoHolder> selection = Collections.singletonList(holder);
        return selection;
    }

    /**
     * Helper method to get a List of message ready to be processed. This implementation will iterate over messages and choose the checked ones.
     *
     * @return Never {@code null}.
     */
    private List<MessageInfoHolder> getSelectionFromCheckboxes() {
        final List<MessageInfoHolder> selection = new ArrayList<MessageInfoHolder>();

        for (final MessageInfoHolder holder : mAdapter.getMessages()) {
            if (holder.selected) {
                selection.add(holder);
            }
        }

        return selection;
    }

    /**
     * Copy the specified messages to the specified folder.
     *
     * @param holders Never {@code null}.
     * @param destination Never {@code null}.
     */
    private void copy(final List<MessageInfoHolder> holders, final String destination) {
        copyOrMove(holders, destination, FolderOperation.COPY);
    }

    /**
     * Move the specified messages to the specified folder.
     *
     * @param holders Never {@code null}.
     * @param destination Never {@code null}.
     */
    private void move(final List<MessageInfoHolder> holders, final String destination) {
        copyOrMove(holders, destination, FolderOperation.MOVE);
    }

    /**
     * The underlying implementation for {@link #copy(List, String)} and
     * {@link #move(List, String)}. This method was added mainly because those 2
     * methods share common behavior.
     *
     * Note: Must be called from the UI thread!
     *
     * @param holders
     *            Never {@code null}.
     * @param destination
     *            Never {@code null}.
     * @param operation
     *            Never {@code null}.
     */
    private void copyOrMove(final List<MessageInfoHolder> holders, final String destination, final FolderOperation operation) {
        if (K9.FOLDER_NONE.equalsIgnoreCase(destination)) {
            return;
        }

        boolean first = true;
        Account account = null;
        String folderName = null;

        final List<Message> messages = new ArrayList<Message>(holders.size());

        for (final MessageInfoHolder holder : holders) {
            final Message message = holder.message;
            if (first) {
                first = false;
                folderName = message.getFolder().getName();
                account = message.getFolder().getAccount();
                if ((operation == FolderOperation.MOVE && !mController.isMoveCapable(account)) || (operation == FolderOperation.COPY && !mController.isCopyCapable(account))) {
                    // account is not copy/move capable
                    return;
                }
            } else if (!account.equals(message.getFolder().getAccount())
                       || !folderName.equals(message.getFolder().getName())) {
                // make sure all messages come from the same account/folder?
                return;
            }
            if ((operation == FolderOperation.MOVE && !mController.isMoveCapable(message)) || (operation == FolderOperation.COPY && !mController.isCopyCapable(message))) {
                final Toast toast = Toast.makeText(this, R.string.move_copy_cannot_copy_unsynced_message,
                                                   Toast.LENGTH_LONG);
                toast.show();

                // XXX return meaningful error value?

                // message isn't synchronized
                return;
            }
            messages.add(message);
        }

        if (operation == FolderOperation.MOVE) {
            mController.moveMessages(account, folderName, messages.toArray(new Message[messages.size()]), destination,
                                     null);
            mAdapter.removeMessages(holders);
        } else {
            mController.copyMessages(account, folderName, messages.toArray(new Message[messages.size()]), destination,
                                     null);
        }
    }

    protected void onAccountUnavailable() {
        finish();
        // TODO inform user about account unavailability using Toast
        Accounts.listAccounts(this);
    }


    /**
     * Return the currently "open" account if available.
     *
     * @param prefs
     *         A {@link Preferences} instance that might be used to retrieve the current
     *         {@link Account}.
     *
     * @return The {@code Account} all displayed messages belong to.
     */
    private Account getCurrentAccount(Preferences prefs) {
        Account account = null;
        if (mQueryString != null && !mIntegrate && mAccountUuids != null &&
                mAccountUuids.length == 1) {
            String uuid = mAccountUuids[0];
            account = prefs.getAccount(uuid);
        } else if (mAccount != null) {
            account = mAccount;
        }

        return account;
    }


    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {

            // enable or disable forward, reply,....
            menu.findItem(R.id.single_message_options)
                    .setVisible(mSelectedCount > 1 ? false : true);

            if (mQueryString != null) {
                // show all
                menu.findItem(R.id.move).setVisible(true);
                menu.findItem(R.id.archive).setVisible(true);
                menu.findItem(R.id.spam).setVisible(true);
                menu.findItem(R.id.copy).setVisible(true);

                // hide uncapable
                /*
                 *  TODO think of a better way then looping over all
                 *  messages.
                 */
                final List<MessageInfoHolder> selection = getSelectionFromCheckboxes();
                Account account;

                for (MessageInfoHolder holder : selection) {
                    account = holder.message.getFolder().getAccount();
                    setContextCapabilities(account, menu);
                }

            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            setAllSelected(false);
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.message_list_context, menu);

            // check capabilities
            if (mQueryString == null) {
                setContextCapabilities(mAccount, menu);
            }

            return true;
        }

        /**
         * Disables menu options based on if the account supports it or not.
         * It also checks the controller and for now the 'mode' the messagelist
         * is operation in ( query or not ).
         *
         * @param mAccount Account to check capabilities of.
         * @param menu Menu to adapt.
         */
        private void setContextCapabilities(Account mAccount, Menu menu) {
            /*
             * TODO get rid of this when we finally split the messagelist into
             * a folder content display and a search result display
             */
            if (mQueryString != null) {
                menu.findItem(R.id.move).setVisible(false);
                menu.findItem(R.id.copy).setVisible(false);

                menu.findItem(R.id.archive).setVisible(false);
                menu.findItem(R.id.spam).setVisible(false);

                return;
            }

            // hide unsupported
            if (!mController.isCopyCapable(mAccount)) {
                menu.findItem(R.id.copy).setVisible(false);
            }

            if (!mController.isMoveCapable(mAccount)) {
                menu.findItem(R.id.move).setVisible(false);
                menu.findItem(R.id.archive).setVisible(false);
                menu.findItem(R.id.spam).setVisible(false);
            }

            if (!mAccount.hasArchiveFolder()) {
                menu.findItem(R.id.archive).setVisible(false);
            }

            if (!mAccount.hasSpamFolder()) {
                menu.findItem(R.id.spam).setVisible(false);
            }
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            final List<MessageInfoHolder> selection = getSelectionFromCheckboxes();

            /*
             * In the following we assume that we can't move or copy
             * mails to the same folder. Also that spam isn't available if we are
             * in the spam folder,same for archive.
             *
             * This is the case currently so safe assumption.
             */
            switch (item.getItemId()) {
            case R.id.delete: {
                onDelete(selection);
                mSelectedCount = 0;
                break;
            }
            case R.id.read_toggle: {
                onToggleRead(selection);
                break;
            }
            case R.id.flag_toggle: {
                onToggleFlag(selection);
                break;
            }

            // only if the account supports this
            case R.id.archive: {
                onArchive(selection);
                mSelectedCount = 0;
                break;
            }
            case R.id.spam: {
                onSpam(selection);
                mSelectedCount = 0;
                break;
            }
            case R.id.move: {
                onMove(selection);
                mSelectedCount = 0;
                break;
            }
            case R.id.copy: {
                onCopy(selection);
                mSelectedCount = 0;
                break;
            }

            // only if a single message is selected
            case R.id.reply: {
                onReply(selection.get(0));
                mSelectedCount = 0;
                break;
            }
            case R.id.reply_all: {
                onReplyAll(selection.get(0));
                mSelectedCount = 0;
                break;
            }
            case R.id.forward: {
                onForward(selection.get(0));
                mSelectedCount = 0;
                break;
            }
            case R.id.send_again: {
                onResendMessage(selection.get(0));
                mSelectedCount = 0;
                break;
            }
            case R.id.same_sender: {
                MessageList.actionHandle(MessageList.this, "From " + selection.get(0).sender,
                    selection.get(0).senderAddress, false, null, null);
                mSelectedCount = 0;
                break;
            }
            }

            if (mSelectedCount == 0) {
                mActionMode.finish();
            }

            return true;
        }
    };
}
