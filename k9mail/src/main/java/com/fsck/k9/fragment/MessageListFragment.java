package com.fsck.k9.fragment;


import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Future;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.Account.SortType;
import com.fsck.k9.BuildConfig;
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.ActivityListener;
import com.fsck.k9.activity.ChooseFolder;
import com.fsck.k9.activity.FolderInfoHolder;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.activity.misc.ContactPictureLoader;
import com.fsck.k9.cache.EmailProviderCache;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.fragment.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;
import com.fsck.k9.fragment.MessageListFragmentComparators.ArrivalComparator;
import com.fsck.k9.fragment.MessageListFragmentComparators.AttachmentComparator;
import com.fsck.k9.fragment.MessageListFragmentComparators.ComparatorChain;
import com.fsck.k9.fragment.MessageListFragmentComparators.DateComparator;
import com.fsck.k9.fragment.MessageListFragmentComparators.FlaggedComparator;
import com.fsck.k9.fragment.MessageListFragmentComparators.ReverseComparator;
import com.fsck.k9.fragment.MessageListFragmentComparators.ReverseIdComparator;
import com.fsck.k9.fragment.MessageListFragmentComparators.SenderComparator;
import com.fsck.k9.fragment.MessageListFragmentComparators.SubjectComparator;
import com.fsck.k9.fragment.MessageListFragmentComparators.UnreadComparator;
import com.fsck.k9.helper.ContactPicture;
import com.fsck.k9.helper.MergeCursorWithUniqueId;
import com.fsck.k9.helper.MessageHelper;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.DatabasePreviewType;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.preferences.StorageEditor;
import com.fsck.k9.provider.EmailProvider;
import com.fsck.k9.provider.EmailProvider.MessageColumns;
import com.fsck.k9.provider.EmailProvider.SpecialColumns;
import com.fsck.k9.provider.EmailProvider.ThreadColumns;
import com.fsck.k9.search.ConditionsTreeNode;
import com.fsck.k9.search.LocalSearch;
import com.fsck.k9.search.SearchSpecification;
import com.fsck.k9.search.SearchSpecification.SearchCondition;
import com.fsck.k9.search.SearchSpecification.SearchField;
import com.fsck.k9.search.SqlQueryBuilder;


public class MessageListFragment extends Fragment implements OnItemClickListener,
        ConfirmationDialogFragmentListener, LoaderCallbacks<Cursor> {

    private static final String[] THREADED_PROJECTION = {
        MessageColumns.ID,
        MessageColumns.UID,
        MessageColumns.INTERNAL_DATE,
        MessageColumns.SUBJECT,
        MessageColumns.DATE,
        MessageColumns.SENDER_LIST,
        MessageColumns.TO_LIST,
        MessageColumns.CC_LIST,
        MessageColumns.READ,
        MessageColumns.FLAGGED,
        MessageColumns.ANSWERED,
        MessageColumns.FORWARDED,
        MessageColumns.ATTACHMENT_COUNT,
        MessageColumns.FOLDER_ID,
        MessageColumns.PREVIEW_TYPE,
        MessageColumns.PREVIEW,
        ThreadColumns.ROOT,
        SpecialColumns.ACCOUNT_UUID,
        SpecialColumns.FOLDER_NAME,

        SpecialColumns.THREAD_COUNT,
    };

    private static final int ID_COLUMN = 0;
    private static final int UID_COLUMN = 1;
    static final int INTERNAL_DATE_COLUMN = 2;
    static final int SUBJECT_COLUMN = 3;
    static final int DATE_COLUMN = 4;
    private static final int SENDER_LIST_COLUMN = 5;
    private static final int TO_LIST_COLUMN = 6;
    private static final int CC_LIST_COLUMN = 7;
    static final int READ_COLUMN = 8;
    static final int FLAGGED_COLUMN = 9;
    private static final int ANSWERED_COLUMN = 10;
    private static final int FORWARDED_COLUMN = 11;
    static final int ATTACHMENT_COUNT_COLUMN = 12;
    private static final int FOLDER_ID_COLUMN = 13;
    private static final int PREVIEW_TYPE_COLUMN = 14;
    private static final int PREVIEW_COLUMN = 15;
    private static final int THREAD_ROOT_COLUMN = 16;
    private static final int ACCOUNT_UUID_COLUMN = 17;
    private static final int FOLDER_NAME_COLUMN = 18;
    private static final int THREAD_COUNT_COLUMN = 19;

    private static final String[] PROJECTION = Arrays.copyOf(THREADED_PROJECTION,
            THREAD_COUNT_COLUMN);



    public static MessageListFragment newInstance(LocalSearch search, boolean isThreadDisplay, boolean threadedList) {
        MessageListFragment fragment = new MessageListFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_SEARCH, search);
        args.putBoolean(ARG_IS_THREAD_DISPLAY, isThreadDisplay);
        args.putBoolean(ARG_THREADED_LIST, threadedList);
        fragment.setArguments(args);
        return fragment;
    }



    private static final int ACTIVITY_CHOOSE_FOLDER_MOVE = 1;
    private static final int ACTIVITY_CHOOSE_FOLDER_COPY = 2;

    private static final String ARG_SEARCH = "searchObject";
    private static final String ARG_THREADED_LIST = "threadedList";
    private static final String ARG_IS_THREAD_DISPLAY = "isThreadedDisplay";

    private static final String STATE_SELECTED_MESSAGES = "selectedMessages";
    private static final String STATE_ACTIVE_MESSAGE = "activeMessage";
    private static final String STATE_REMOTE_SEARCH_PERFORMED = "remoteSearchPerformed";
    private static final String STATE_MESSAGE_LIST = "listState";

    /**
     * Maps a {@link SortType} to a {@link Comparator} implementation.
     */
    private static final Map<SortType, Comparator<Cursor>> SORT_COMPARATORS;

    static {
        // fill the mapping at class time loading

        final Map<SortType, Comparator<Cursor>> map =
                new EnumMap<>(SortType.class);
        map.put(SortType.SORT_ATTACHMENT, new AttachmentComparator());
        map.put(SortType.SORT_DATE, new DateComparator());
        map.put(SortType.SORT_ARRIVAL, new ArrivalComparator());
        map.put(SortType.SORT_FLAGGED, new FlaggedComparator());
        map.put(SortType.SORT_SUBJECT, new SubjectComparator());
        map.put(SortType.SORT_SENDER, new SenderComparator());
        map.put(SortType.SORT_UNREAD, new UnreadComparator());

        // make it immutable to prevent accidental alteration (content is immutable already)
        SORT_COMPARATORS = Collections.unmodifiableMap(map);
    }

    private ListView mListView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private Parcelable mSavedListState;

    private int mPreviewLines = 0;


    private MessageListAdapter mAdapter;
    private View mFooterView;

    private FolderInfoHolder mCurrentFolder;

    private LayoutInflater mInflater;

    private MessagingController mController;

    private Account mAccount;
    private String[] mAccountUuids;
    private int mUnreadMessageCount = 0;

    private Cursor[] mCursors;
    private boolean[] mCursorValid;
    private int mUniqueIdColumn;

    /**
     * Stores the name of the folder that we want to open as soon as possible after load.
     */
    private String mFolderName;

    private boolean mRemoteSearchPerformed = false;
    private Future<?> mRemoteSearchFuture = null;
    public List<Message> mExtraSearchResults;

    private String mTitle;
    private LocalSearch mSearch = null;
    private boolean mSingleAccountMode;
    private boolean mSingleFolderMode;
    private boolean mAllAccounts;

    private MessageListHandler mHandler = new MessageListHandler(this);

    private SortType mSortType = SortType.SORT_DATE;
    private boolean mSortAscending = true;
    private boolean mSortDateAscending = false;
    private boolean mSenderAboveSubject = false;
    private boolean mCheckboxes = true;
    private boolean mStars = true;

    private int mSelectedCount = 0;
    private Set<Long> mSelected = new HashSet<>();

    private FontSizes mFontSizes = K9.getFontSizes();

    private ActionMode mActionMode;

    private Boolean mHasConnectivity;

    /**
     * Relevant messages for the current context when we have to remember the chosen messages
     * between user interactions (e.g. selecting a folder for move operation).
     */
    private List<MessageReference> mActiveMessages;

    /* package visibility for faster inner class access */
    MessageHelper mMessageHelper;

    private ActionModeCallback mActionModeCallback = new ActionModeCallback();


    private MessageListFragmentListener mFragmentListener;

    private boolean mThreadedList;

    private boolean mIsThreadDisplay;

    private Context mContext;

    private final ActivityListener mListener = new MessageListActivityListener();

    private Preferences mPreferences;

    private boolean mLoaderJustInitialized;
    private MessageReference mActiveMessage;

    /**
     * {@code true} after {@link #onCreate(Bundle)} was executed. Used in {@link #updateTitle()} to
     * make sure we don't access member variables before initialization is complete.
     */
    private boolean mInitialized = false;

    private ContactPictureLoader mContactsPictureLoader;

    private LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mCacheBroadcastReceiver;
    private IntentFilter mCacheIntentFilter;

    /**
     * Stores the unique ID of the message the context menu was opened for.
     *
     * We have to save this because the message list might change between the time the menu was
     * opened and when the user clicks on a menu item. When this happens the 'adapter position' that
     * is accessible via the {@code ContextMenu} object might correspond to another list item and we
     * would end up using/modifying the wrong message.
     *
     * The value of this field is {@code 0} when no context menu is currently open.
     */
    private long mContextMenuUniqueId = 0;


    /**
     * This class is used to run operations that modify UI elements in the UI thread.
     *
     * <p>We are using convenience methods that add a {@link android.os.Message} instance or a
     * {@link Runnable} to the message queue.</p>
     *
     * <p><strong>Note:</strong> If you add a method to this class make sure you don't accidentally
     * perform the operation in the calling thread.</p>
     */
    static class MessageListHandler extends Handler {
        private static final int ACTION_FOLDER_LOADING = 1;
        private static final int ACTION_REFRESH_TITLE = 2;
        private static final int ACTION_PROGRESS = 3;
        private static final int ACTION_REMOTE_SEARCH_FINISHED = 4;
        private static final int ACTION_GO_BACK = 5;
        private static final int ACTION_RESTORE_LIST_POSITION = 6;
        private static final int ACTION_OPEN_MESSAGE = 7;

        private WeakReference<MessageListFragment> mFragment;

        public MessageListHandler(MessageListFragment fragment) {
            mFragment = new WeakReference<>(fragment);
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

        public void remoteSearchFinished() {
            android.os.Message msg = android.os.Message.obtain(this, ACTION_REMOTE_SEARCH_FINISHED);
            sendMessage(msg);
        }

        public void updateFooter(final String message) {
            post(new Runnable() {
                @Override
                public void run() {
                    MessageListFragment fragment = mFragment.get();
                    if (fragment != null) {
                        fragment.updateFooter(message);
                    }
                }
            });
        }

        public void goBack() {
            android.os.Message msg = android.os.Message.obtain(this, ACTION_GO_BACK);
            sendMessage(msg);
        }

        public void restoreListPosition() {
            MessageListFragment fragment = mFragment.get();
            if (fragment != null) {
                android.os.Message msg = android.os.Message.obtain(this, ACTION_RESTORE_LIST_POSITION,
                        fragment.mSavedListState);
                fragment.mSavedListState = null;
                sendMessage(msg);
            }
        }

        public void openMessage(MessageReference messageReference) {
            android.os.Message msg = android.os.Message.obtain(this, ACTION_OPEN_MESSAGE,
                    messageReference);
            sendMessage(msg);
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            MessageListFragment fragment = mFragment.get();
            if (fragment == null) {
                return;
            }

            // The following messages don't need an attached activity.
            switch (msg.what) {
                case ACTION_REMOTE_SEARCH_FINISHED: {
                    fragment.remoteSearchFinished();
                    return;
                }
            }

            // Discard messages if the fragment isn't attached to an activity anymore.
            Activity activity = fragment.getActivity();
            if (activity == null) {
                return;
            }

            switch (msg.what) {
                case ACTION_FOLDER_LOADING: {
                    String folder = (String) msg.obj;
                    boolean loading = (msg.arg1 == 1);
                    fragment.folderLoading(folder, loading);
                    break;
                }
                case ACTION_REFRESH_TITLE: {
                    fragment.updateTitle();
                    break;
                }
                case ACTION_PROGRESS: {
                    boolean progress = (msg.arg1 == 1);
                    fragment.progress(progress);
                    break;
                }
                case ACTION_GO_BACK: {
                    fragment.mFragmentListener.goBack();
                    break;
                }
                case ACTION_RESTORE_LIST_POSITION: {
                    fragment.mListView.onRestoreInstanceState((Parcelable) msg.obj);
                    break;
                }
                case ACTION_OPEN_MESSAGE: {
                    MessageReference messageReference = (MessageReference) msg.obj;
                    fragment.mFragmentListener.openMessage(messageReference);
                    break;
                }
            }
        }
    }

    /**
     * @return The comparator to use to display messages in an ordered
     *         fashion. Never {@code null}.
     */
    protected Comparator<Cursor> getComparator() {
        final List<Comparator<Cursor>> chain =
                new ArrayList<>(3 /* we add 3 comparators at most */);

        // Add the specified comparator
        final Comparator<Cursor> comparator = SORT_COMPARATORS.get(mSortType);
        if (mSortAscending) {
            chain.add(comparator);
        } else {
            chain.add(new ReverseComparator<>(comparator));
        }

        // Add the date comparator if not already specified
        if (mSortType != SortType.SORT_DATE && mSortType != SortType.SORT_ARRIVAL) {
            final Comparator<Cursor> dateComparator = SORT_COMPARATORS.get(SortType.SORT_DATE);
            if (mSortDateAscending) {
                chain.add(dateComparator);
            } else {
                chain.add(new ReverseComparator<>(dateComparator));
            }
        }

        // Add the id comparator
        chain.add(new ReverseIdComparator());

        // Build the comparator chain
        return new ComparatorChain<>(chain);
    }

    private void folderLoading(String folder, boolean loading) {
        if (mCurrentFolder != null && mCurrentFolder.name.equals(folder)) {
            mCurrentFolder.loading = loading;
        }
        updateMoreMessagesOfCurrentFolder();
        updateFooterView();
    }

    public void updateTitle() {
        if (!mInitialized) {
            return;
        }

        setWindowTitle();
        if (!mSearch.isManualSearch()) {
            setWindowProgress();
        }
    }

    private void setWindowProgress() {
        int level = Window.PROGRESS_END;

        if (mCurrentFolder != null && mCurrentFolder.loading && mListener.getFolderTotal() > 0) {
            int divisor = mListener.getFolderTotal();
            if (divisor != 0) {
                level = (Window.PROGRESS_END / divisor) * (mListener.getFolderCompleted()) ;
                if (level > Window.PROGRESS_END) {
                    level = Window.PROGRESS_END;
                }
            }
        }

        mFragmentListener.setMessageListProgress(level);
    }

    private void setWindowTitle() {
        // regular folder content display
        if (!isManualSearch() && mSingleFolderMode) {
            Activity activity = getActivity();
            String displayName = FolderInfoHolder.getDisplayName(activity, mAccount,
                mFolderName);

            mFragmentListener.setMessageListTitle(displayName);

            String operation = mListener.getOperation(activity);
            if (operation.length() < 1) {
                mFragmentListener.setMessageListSubTitle(mAccount.getEmail());
            } else {
                mFragmentListener.setMessageListSubTitle(operation);
            }
        } else {
            // query result display.  This may be for a search folder as opposed to a user-initiated search.
            if (mTitle != null) {
                // This was a search folder; the search folder has overridden our title.
                mFragmentListener.setMessageListTitle(mTitle);
            } else {
                // This is a search result; set it to the default search result line.
                mFragmentListener.setMessageListTitle(getString(R.string.search_results));
            }

            mFragmentListener.setMessageListSubTitle(null);
        }

        // set unread count
        if (mUnreadMessageCount <= 0) {
            mFragmentListener.setUnreadCount(0);
        } else {
            if (!mSingleFolderMode && mTitle == null) {
                // The unread message count is easily confused
                // with total number of messages in the search result, so let's hide it.
                mFragmentListener.setUnreadCount(0);
            } else {
                mFragmentListener.setUnreadCount(mUnreadMessageCount);
            }
        }
    }

    private void progress(final boolean progress) {
        mFragmentListener.enableActionBarProgress(progress);
        if (mSwipeRefreshLayout != null && !progress) {
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (view == mFooterView) {
            if (mCurrentFolder != null && !mSearch.isManualSearch() && mCurrentFolder.moreMessages) {

                mController.loadMoreMessages(mAccount, mFolderName, null);

            } else if (mCurrentFolder != null && isRemoteSearch() &&
                    mExtraSearchResults != null && mExtraSearchResults.size() > 0) {

                int numResults = mExtraSearchResults.size();
                int limit = mAccount.getRemoteSearchNumResults();

                List<Message> toProcess = mExtraSearchResults;

                if (limit > 0 && numResults > limit) {
                    toProcess = toProcess.subList(0, limit);
                    mExtraSearchResults = mExtraSearchResults.subList(limit,
                            mExtraSearchResults.size());
                } else {
                    mExtraSearchResults = null;
                    updateFooter(null);
                }

                mController.loadSearchResults(mAccount, mCurrentFolder.name, toProcess, mListener);
            }

            return;
        }

        Cursor cursor = (Cursor) parent.getItemAtPosition(position);
        if (cursor == null) {
            return;
        }

        if (mSelectedCount > 0) {
            toggleMessageSelect(position);
        } else {
            if (mThreadedList && cursor.getInt(THREAD_COUNT_COLUMN) > 1) {
                Account account = getAccountFromCursor(cursor);
                String folderName = cursor.getString(FOLDER_NAME_COLUMN);

                // If threading is enabled and this item represents a thread, display the thread contents.
                long rootId = cursor.getLong(THREAD_ROOT_COLUMN);
                mFragmentListener.showThread(account, folderName, rootId);
            } else {
                // This item represents a message; just display the message.
                openMessageAtPosition(listViewToAdapterPosition(position));
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mContext = activity.getApplicationContext();

        try {
            mFragmentListener = (MessageListFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.getClass() +
                    " must implement MessageListFragmentListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context appContext = getActivity().getApplicationContext();

        mPreferences = Preferences.getPreferences(appContext);
        mController = MessagingController.getInstance(getActivity().getApplication());

        mPreviewLines = K9.messageListPreviewLines();
        mCheckboxes = K9.messageListCheckboxes();
        mStars = K9.messageListStars();

        if (K9.showContactPicture()) {
            mContactsPictureLoader = ContactPicture.getContactPictureLoader(getActivity());
        }

        restoreInstanceState(savedInstanceState);
        decodeArguments();

        createCacheBroadcastReceiver(appContext);

        mInitialized = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mInflater = inflater;

        View view = inflater.inflate(R.layout.message_list_fragment, container, false);

        initializePullToRefresh(view);

        initializeLayout();
        mListView.setVerticalFadingEdgeEnabled(false);

        return view;
    }

    @Override
    public void onDestroyView() {
        mSavedListState = mListView.onSaveInstanceState();
        super.onDestroyView();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mMessageHelper = MessageHelper.getInstance(getActivity());

        initializeMessageList();

        // This needs to be done before initializing the cursor loader below
        initializeSortSettings();

        mLoaderJustInitialized = true;
        LoaderManager loaderManager = getLoaderManager();
        int len = mAccountUuids.length;
        mCursors = new Cursor[len];
        mCursorValid = new boolean[len];
        for (int i = 0; i < len; i++) {
            loaderManager.initLoader(i, null, this);
            mCursorValid[i] = false;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        saveSelectedMessages(outState);
        saveListState(outState);

        outState.putBoolean(STATE_REMOTE_SEARCH_PERFORMED, mRemoteSearchPerformed);
        outState.putParcelable(STATE_ACTIVE_MESSAGE, mActiveMessage);
    }

    /**
     * Restore the state of a previous {@link MessageListFragment} instance.
     *
     * @see #onSaveInstanceState(Bundle)
     */
    private void restoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }

        restoreSelectedMessages(savedInstanceState);

        mRemoteSearchPerformed = savedInstanceState.getBoolean(STATE_REMOTE_SEARCH_PERFORMED);
        mSavedListState = savedInstanceState.getParcelable(STATE_MESSAGE_LIST);
        mActiveMessage = savedInstanceState.getParcelable(STATE_ACTIVE_MESSAGE);
    }

    /**
     * Write the unique IDs of selected messages to a {@link Bundle}.
     */
    private void saveSelectedMessages(Bundle outState) {
        long[] selected = new long[mSelected.size()];
        int i = 0;
        for (Long id : mSelected) {
            selected[i++] = id;
        }
        outState.putLongArray(STATE_SELECTED_MESSAGES, selected);
    }

    /**
     * Restore selected messages from a {@link Bundle}.
     */
    private void restoreSelectedMessages(Bundle savedInstanceState) {
        long[] selected = savedInstanceState.getLongArray(STATE_SELECTED_MESSAGES);
        for (long id : selected) {
            mSelected.add(Long.valueOf(id));
        }
    }

    private void saveListState(Bundle outState) {
        if (mSavedListState != null) {
            // The previously saved state was never restored, so just use that.
            outState.putParcelable(STATE_MESSAGE_LIST, mSavedListState);
        } else if (mListView != null) {
            outState.putParcelable(STATE_MESSAGE_LIST, mListView.onSaveInstanceState());
        }
    }

    private void initializeSortSettings() {
        if (mSingleAccountMode) {
            mSortType = mAccount.getSortType();
            mSortAscending = mAccount.isSortAscending(mSortType);
            mSortDateAscending = mAccount.isSortAscending(SortType.SORT_DATE);
        } else {
            mSortType = K9.getSortType();
            mSortAscending = K9.isSortAscending(mSortType);
            mSortDateAscending = K9.isSortAscending(SortType.SORT_DATE);
        }
    }

    private void decodeArguments() {
        Bundle args = getArguments();

        mThreadedList = args.getBoolean(ARG_THREADED_LIST, false);
        mIsThreadDisplay = args.getBoolean(ARG_IS_THREAD_DISPLAY, false);
        mSearch = args.getParcelable(ARG_SEARCH);
        mTitle = mSearch.getName();

        String[] accountUuids = mSearch.getAccountUuids();

        mSingleAccountMode = false;
        if (accountUuids.length == 1 && !mSearch.searchAllAccounts()) {
            mSingleAccountMode = true;
            mAccount = mPreferences.getAccount(accountUuids[0]);
        }

        mSingleFolderMode = false;
        if (mSingleAccountMode && (mSearch.getFolderNames().size() == 1)) {
            mSingleFolderMode = true;
            mFolderName = mSearch.getFolderNames().get(0);
            mCurrentFolder = getFolderInfoHolder(mFolderName, mAccount);
        }

        mAllAccounts = false;
        if (mSingleAccountMode) {
            mAccountUuids = new String[] { mAccount.getUuid() };
        } else {
            if (accountUuids.length == 1 &&
                    accountUuids[0].equals(SearchSpecification.ALL_ACCOUNTS)) {
                mAllAccounts = true;

                List<Account> accounts = mPreferences.getAccounts();

                mAccountUuids = new String[accounts.size()];
                for (int i = 0, len = accounts.size(); i < len; i++) {
                    mAccountUuids[i] = accounts.get(i).getUuid();
                }

                if (mAccountUuids.length == 1) {
                    mSingleAccountMode = true;
                    mAccount = accounts.get(0);
                }
            } else {
                mAccountUuids = accountUuids;
            }
        }
    }

    private void initializeMessageList() {
        mAdapter = new MessageListAdapter();

        if (mFolderName != null) {
            mCurrentFolder = getFolderInfoHolder(mFolderName, mAccount);
        }

        if (mSingleFolderMode) {
            mListView.addFooterView(getFooterView(mListView));
            updateFooterView();
        }

        mListView.setAdapter(mAdapter);
    }

    private void createCacheBroadcastReceiver(Context appContext) {
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(appContext);

        mCacheBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mAdapter.notifyDataSetChanged();
            }
        };

        mCacheIntentFilter = new IntentFilter(EmailProviderCache.ACTION_CACHE_UPDATED);
    }

    private FolderInfoHolder getFolderInfoHolder(String folderName, Account account) {
        try {
            LocalFolder localFolder = getFolder(folderName, account);
            return new FolderInfoHolder(mContext, localFolder, account);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    private LocalFolder getFolder(String folderName, Account account) throws MessagingException {
        LocalStore localStore = account.getLocalStore();
        LocalFolder localFolder = localStore.getFolder(folderName);
        localFolder.open(Folder.OPEN_MODE_RO);
        return localFolder;
    }

    @Override
    public void onPause() {
        super.onPause();

        mLocalBroadcastManager.unregisterReceiver(mCacheBroadcastReceiver);
        mListener.onPause(getActivity());
        mController.removeListener(mListener);
    }

    /**
     * On resume we refresh messages for the folder that is currently open.
     * This guarantees that things like unread message count and read status
     * are updated.
     */
    @Override
    public void onResume() {
        super.onResume();

        Context appContext = getActivity().getApplicationContext();

        mSenderAboveSubject = K9.messageListSenderAboveSubject();

        if (!mLoaderJustInitialized) {
            restartLoader();
        } else {
            mLoaderJustInitialized = false;
        }

        // Check if we have connectivity.  Cache the value.
        if (mHasConnectivity == null) {
            mHasConnectivity = Utility.hasConnectivity(getActivity().getApplication());
        }

        mLocalBroadcastManager.registerReceiver(mCacheBroadcastReceiver, mCacheIntentFilter);
        mListener.onResume(getActivity());
        mController.addListener(mListener);

        //Cancel pending new mail notifications when we open an account
        List<Account> accountsWithNotification;

        Account account = mAccount;
        if (account != null) {
            accountsWithNotification = Collections.singletonList(account);
        } else {
            accountsWithNotification = mPreferences.getAccounts();
        }

        for (Account accountWithNotification : accountsWithNotification) {
            mController.cancelNotificationsForAccount(accountWithNotification);
        }

        if (mAccount != null && mFolderName != null && !mSearch.isManualSearch()) {
            mController.getFolderUnreadMessageCount(mAccount, mFolderName, mListener);
        }

        updateTitle();
    }

    private void restartLoader() {
        if (mCursorValid == null) {
            return;
        }

        // Refresh the message list
        LoaderManager loaderManager = getLoaderManager();
        for (int i = 0; i < mAccountUuids.length; i++) {
            loaderManager.restartLoader(i, null, this);
            mCursorValid[i] = false;
        }
    }

    private void initializePullToRefresh(View layout) {
        mSwipeRefreshLayout = (SwipeRefreshLayout) layout.findViewById(R.id.swiperefresh);
        mListView = (ListView) layout.findViewById(R.id.message_list);

        if (isRemoteSearchAllowed()) {
            mSwipeRefreshLayout.setOnRefreshListener(
                    new SwipeRefreshLayout.OnRefreshListener() {
                        @Override
                        public void onRefresh() {
                            onRemoteSearchRequested();
                        }
                    }
            );
        } else if (isCheckMailSupported()) {
            mSwipeRefreshLayout.setOnRefreshListener(
                    new SwipeRefreshLayout.OnRefreshListener() {
                        @Override
                        public void onRefresh() {
                            checkMail();
                        }
                    }
            );
        }

        // Disable pull-to-refresh until the message list has been loaded
        mSwipeRefreshLayout.setEnabled(false);
    }

    private void initializeLayout() {
        mListView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        mListView.setLongClickable(true);
        mListView.setFastScrollEnabled(true);
        mListView.setScrollingCacheEnabled(false);
        mListView.setOnItemClickListener(this);

        registerForContextMenu(mListView);
    }

    public void onCompose() {
        if (!mSingleAccountMode) {
            /*
             * If we have a query string, we don't have an account to let
             * compose start the default action.
             */
            mFragmentListener.onCompose(null);
        } else {
            mFragmentListener.onCompose(mAccount);
        }
    }

    public void onReply(MessageReference messageReference) {
        mFragmentListener.onReply(messageReference);
    }

    public void onReplyAll(MessageReference messageReference) {
        mFragmentListener.onReplyAll(messageReference);
    }

    public void onForward(MessageReference messageReference) {
        mFragmentListener.onForward(messageReference);
    }

    public void onResendMessage(MessageReference messageReference) {
        mFragmentListener.onResendMessage(messageReference);
    }

    public void changeSort(SortType sortType) {
        Boolean sortAscending = (mSortType == sortType) ? !mSortAscending : null;
        changeSort(sortType, sortAscending);
    }

    /**
     * User has requested a remote search.  Setup the bundle and start the intent.
     */
    public void onRemoteSearchRequested() {
        String searchAccount;
        String searchFolder;

        searchAccount = mAccount.getUuid();
        searchFolder = mCurrentFolder.name;

        String queryString = mSearch.getRemoteSearchArguments();

        mRemoteSearchPerformed = true;
        mRemoteSearchFuture = mController.searchRemoteMessages(searchAccount, searchFolder,
                queryString, null, null, mListener);

        mSwipeRefreshLayout.setEnabled(false);

        mFragmentListener.remoteSearchStarted();
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

        Account account = mAccount;

        if (account != null) {
            account.setSortType(mSortType);

            if (sortAscending == null) {
                mSortAscending = account.isSortAscending(mSortType);
            } else {
                mSortAscending = sortAscending;
            }
            account.setSortAscending(mSortType, mSortAscending);
            mSortDateAscending = account.isSortAscending(SortType.SORT_DATE);

            account.save(mPreferences);
        } else {
            K9.setSortType(mSortType);

            if (sortAscending == null) {
                mSortAscending = K9.isSortAscending(mSortType);
            } else {
                mSortAscending = sortAscending;
            }
            K9.setSortAscending(mSortType, mSortAscending);
            mSortDateAscending = K9.isSortAscending(SortType.SORT_DATE);

            StorageEditor editor = mPreferences.getStorage().edit();
            K9.save(editor);
            editor.commit();
        }

        reSort();
    }

    private void reSort() {
        int toastString = mSortType.getToast(mSortAscending);

        Toast toast = Toast.makeText(getActivity(), toastString, Toast.LENGTH_SHORT);
        toast.show();

        LoaderManager loaderManager = getLoaderManager();
        for (int i = 0, len = mAccountUuids.length; i < len; i++) {
            loaderManager.restartLoader(i, null, this);
        }
    }

    public void onCycleSort() {
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

    private void onDelete(MessageReference message) {
        onDelete(Collections.singletonList(message));
    }

    private void onDelete(List<MessageReference> messages) {
        if (K9.confirmDelete()) {
            // remember the message selection for #onCreateDialog(int)
            mActiveMessages = messages;
            showDialog(R.id.dialog_confirm_delete);
        } else {
            onDeleteConfirmed(messages);
        }
    }

    private void onDeleteConfirmed(List<MessageReference> messages) {
        if (mThreadedList) {
            mController.deleteThreads(messages);
        } else {
            mController.deleteMessages(messages, null);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
        case ACTIVITY_CHOOSE_FOLDER_MOVE:
        case ACTIVITY_CHOOSE_FOLDER_COPY: {
            if (data == null) {
                return;
            }

            final String destFolderName = data.getStringExtra(ChooseFolder.EXTRA_NEW_FOLDER);
            final List<MessageReference> messages = mActiveMessages;

            if (destFolderName != null) {

                mActiveMessages = null; // don't need it any more

                if (messages.size() > 0) {
                    try {
                        MessageReference firstMsg = messages.get(0);
                        Account account = mPreferences.getAccount(firstMsg.getAccountUuid());
                        LocalFolder firstMsgFolder = getFolder(firstMsg.getFolderName(), account);
                        firstMsgFolder.setLastSelectedFolderName(destFolderName);
                    } catch (MessagingException e) {
                        Log.e(K9.LOG_TAG, "Error getting folder for setLastSelectedFolderName()", e);
                    }
                }

                switch (requestCode) {
                case ACTIVITY_CHOOSE_FOLDER_MOVE:
                    move(messages, destFolderName);
                    break;

                case ACTIVITY_CHOOSE_FOLDER_COPY:
                    copy(messages, destFolderName);
                    break;
                }
            }
            break;
        }
        }
    }

    public void onExpunge() {
        if (mCurrentFolder != null) {
            onExpunge(mAccount, mCurrentFolder.name);
        }
    }

    private void onExpunge(final Account account, String folderName) {
        mController.expunge(account, folderName);
    }

    private void showDialog(int dialogId) {
        DialogFragment fragment;
        switch (dialogId) {
            case R.id.dialog_confirm_spam: {
                String title = getString(R.string.dialog_confirm_spam_title);

                int selectionSize = mActiveMessages.size();
                String message = getResources().getQuantityString(
                        R.plurals.dialog_confirm_spam_message, selectionSize,
                        Integer.valueOf(selectionSize));

                String confirmText = getString(R.string.dialog_confirm_spam_confirm_button);
                String cancelText = getString(R.string.dialog_confirm_spam_cancel_button);

                fragment = ConfirmationDialogFragment.newInstance(dialogId, title, message,
                        confirmText, cancelText);
                break;
            }
            case R.id.dialog_confirm_delete: {
                String title = getString(R.string.dialog_confirm_delete_title);

                int selectionSize = mActiveMessages.size();
                String message = getResources().getQuantityString(
                        R.plurals.dialog_confirm_delete_messages, selectionSize,
                        Integer.valueOf(selectionSize));

                String confirmText = getString(R.string.dialog_confirm_delete_confirm_button);
                String cancelText = getString(R.string.dialog_confirm_delete_cancel_button);

                fragment = ConfirmationDialogFragment.newInstance(dialogId, title, message,
                        confirmText, cancelText);
                break;
            }
            case R.id.dialog_confirm_mark_all_as_read: {
                String title = getString(R.string.dialog_confirm_mark_all_as_read_title);
                String message = getString(R.string.dialog_confirm_mark_all_as_read_message);

                String confirmText = getString(R.string.dialog_confirm_mark_all_as_read_confirm_button);
                String cancelText = getString(R.string.dialog_confirm_mark_all_as_read_cancel_button);

                fragment = ConfirmationDialogFragment.newInstance(dialogId, title, message, confirmText, cancelText);
                break;
            }
            default: {
                throw new RuntimeException("Called showDialog(int) with unknown dialog id.");
            }
        }

        fragment.setTargetFragment(this, dialogId);
        fragment.show(getFragmentManager(), getDialogTag(dialogId));
    }

    private String getDialogTag(int dialogId) {
        return "dialog-" + dialogId;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
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
        case R.id.select_all: {
            selectAll();
            return true;
        }
        }

        if (!mSingleAccountMode) {
            // None of the options after this point are "safe" for search results
            //TODO: This is not true for "unread" and "starred" searches in regular folders
            return false;
        }

        switch (itemId) {
        case R.id.send_messages: {
            onSendPendingMessages();
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

    public void onSendPendingMessages() {
        mController.sendPendingMessages(mAccount, null);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        if (mContextMenuUniqueId == 0) {
            return false;
        }

        int adapterPosition = getPositionForUniqueId(mContextMenuUniqueId);
        if (adapterPosition == AdapterView.INVALID_POSITION) {
            return false;
        }

        switch (item.getItemId()) {
            case R.id.deselect:
            case R.id.select: {
                toggleMessageSelectWithAdapterPosition(adapterPosition);
                break;
            }
            case R.id.reply: {
                onReply(getMessageAtPosition(adapterPosition));
                break;
            }
            case R.id.reply_all: {
                onReplyAll(getMessageAtPosition(adapterPosition));
                break;
            }
            case R.id.forward: {
                onForward(getMessageAtPosition(adapterPosition));
                break;
            }
            case R.id.send_again: {
                onResendMessage(getMessageAtPosition(adapterPosition));
                mSelectedCount = 0;
                break;
            }
            case R.id.same_sender: {
                Cursor cursor = (Cursor) mAdapter.getItem(adapterPosition);
                String senderAddress = getSenderAddressFromCursor(cursor);
                if (senderAddress != null) {
                    mFragmentListener.showMoreFromSameSender(senderAddress);
                }
                break;
            }
            case R.id.delete: {
                MessageReference message = getMessageAtPosition(adapterPosition);
                onDelete(message);
                break;
            }
            case R.id.mark_as_read: {
                setFlag(adapterPosition, Flag.SEEN, true);
                break;
            }
            case R.id.mark_as_unread: {
                setFlag(adapterPosition, Flag.SEEN, false);
                break;
            }
            case R.id.flag: {
                setFlag(adapterPosition, Flag.FLAGGED, true);
                break;
            }
            case R.id.unflag: {
                setFlag(adapterPosition, Flag.FLAGGED, false);
                break;
            }

            // only if the account supports this
            case R.id.archive: {
                onArchive(getMessageAtPosition(adapterPosition));
                break;
            }
            case R.id.spam: {
                onSpam(getMessageAtPosition(adapterPosition));
                break;
            }
            case R.id.move: {
                onMove(getMessageAtPosition(adapterPosition));
                break;
            }
            case R.id.copy: {
                onCopy(getMessageAtPosition(adapterPosition));
                break;
            }

            // debug options
            case R.id.debug_delete_locally: {
                onDebugClearLocally(getMessageAtPosition(adapterPosition));
                break;
            }
        }

        mContextMenuUniqueId = 0;
        return true;
    }


    static String getSenderAddressFromCursor(Cursor cursor) {
        String fromList = cursor.getString(SENDER_LIST_COLUMN);
        Address[] fromAddrs = Address.unpack(fromList);
        return (fromAddrs.length > 0) ? fromAddrs[0].getAddress() : null;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        Cursor cursor = (Cursor) mListView.getItemAtPosition(info.position);

        if (cursor == null) {
            return;
        }

        getActivity().getMenuInflater().inflate(R.menu.message_list_item_context, menu);
        menu.findItem(R.id.debug_delete_locally).setVisible(BuildConfig.DEBUG);

        mContextMenuUniqueId = cursor.getLong(mUniqueIdColumn);
        Account account = getAccountFromCursor(cursor);

        String subject = cursor.getString(SUBJECT_COLUMN);
        boolean read = (cursor.getInt(READ_COLUMN) == 1);
        boolean flagged = (cursor.getInt(FLAGGED_COLUMN) == 1);

        menu.setHeaderTitle(subject);

        if(  mSelected.contains(mContextMenuUniqueId)) {
            menu.findItem(R.id.select).setVisible(false);
        } else {
            menu.findItem(R.id.deselect).setVisible(false);
        }

        if (read) {
            menu.findItem(R.id.mark_as_read).setVisible(false);
        } else {
            menu.findItem(R.id.mark_as_unread).setVisible(false);
        }

        if (flagged) {
            menu.findItem(R.id.flag).setVisible(false);
        } else {
            menu.findItem(R.id.unflag).setVisible(false);
        }

        if (!mController.isCopyCapable(account)) {
            menu.findItem(R.id.copy).setVisible(false);
        }

        if (!mController.isMoveCapable(account)) {
            menu.findItem(R.id.move).setVisible(false);
            menu.findItem(R.id.archive).setVisible(false);
            menu.findItem(R.id.spam).setVisible(false);
        }

        if (!account.hasArchiveFolder()) {
            menu.findItem(R.id.archive).setVisible(false);
        }

        if (!account.hasSpamFolder()) {
            menu.findItem(R.id.spam).setVisible(false);
        }

    }

    public void onSwipeRightToLeft(final MotionEvent e1, final MotionEvent e2) {
        // Handle right-to-left as an un-select
        handleSwipe(e1, false);
    }

    public void onSwipeLeftToRight(final MotionEvent e1, final MotionEvent e2) {
        // Handle left-to-right as a select.
        handleSwipe(e1, true);
    }

    /**
     * Handle a select or unselect swipe event.
     *
     * @param downMotion
     *         Event that started the swipe
     * @param selected
     *         {@code true} if this was an attempt to select (i.e. left to right).
     */
    private void handleSwipe(final MotionEvent downMotion, final boolean selected) {
        int x = (int) downMotion.getRawX();
        int y = (int) downMotion.getRawY();

        Rect headerRect = new Rect();
        mListView.getGlobalVisibleRect(headerRect);

        // Only handle swipes in the visible area of the message list
        if (headerRect.contains(x, y)) {
            int[] listPosition = new int[2];
            mListView.getLocationOnScreen(listPosition);

            int listX = x - listPosition[0];
            int listY = y - listPosition[1];

            int listViewPosition = mListView.pointToPosition(listX, listY);

            toggleMessageSelect(listViewPosition);
        }
    }

    private int listViewToAdapterPosition(int position) {
        if (position >= 0 && position < mAdapter.getCount()) {
            return position;
        }

        return AdapterView.INVALID_POSITION;
    }

    private int adapterToListViewPosition(int position) {
        if (position >= 0 && position < mAdapter.getCount()) {
            return position;
        }

        return AdapterView.INVALID_POSITION;
    }

    class MessageListActivityListener extends ActivityListener {
        @Override
        public void remoteSearchFailed(String folder, final String err) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Activity activity = getActivity();
                    if (activity != null) {
                        Toast.makeText(activity, R.string.remote_search_error,
                                Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

        @Override
        public void remoteSearchStarted(String folder) {
            mHandler.progress(true);
            mHandler.updateFooter(mContext.getString(R.string.remote_search_sending_query));
        }

        @Override
        public void enableProgressIndicator(boolean enable) {
            mHandler.progress(enable);
        }

        @Override
        public void remoteSearchFinished(String folder, int numResults, int maxResults, List<Message> extraResults) {
            mHandler.progress(false);
            mHandler.remoteSearchFinished();
            mExtraSearchResults = extraResults;
            if (extraResults != null && extraResults.size() > 0) {
                mHandler.updateFooter(String.format(mContext.getString(R.string.load_more_messages_fmt), maxResults));
            } else {
                mHandler.updateFooter(null);
            }
            mFragmentListener.setMessageListProgress(Window.PROGRESS_END);

        }

        @Override
        public void remoteSearchServerQueryComplete(String folderName, int numResults, int maxResults) {
            mHandler.progress(true);
            if (maxResults != 0 && numResults > maxResults) {
                mHandler.updateFooter(mContext.getString(R.string.remote_search_downloading_limited,
                        maxResults, numResults));
            } else {
                mHandler.updateFooter(mContext.getString(R.string.remote_search_downloading, numResults));
            }
            mFragmentListener.setMessageListProgress(Window.PROGRESS_START);
        }

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
            }
            super.synchronizeMailboxFinished(account, folder, totalMessagesInMailbox, numNewMessages);
        }

        @Override
        public void synchronizeMailboxFailed(Account account, String folder, String message) {

            if (updateForMe(account, folder)) {
                mHandler.progress(false);
                mHandler.folderLoading(folder, false);
            }
            super.synchronizeMailboxFailed(account, folder, message);
        }

        @Override
        public void folderStatusChanged(Account account, String folder, int unreadMessageCount) {
            if (isSingleAccountMode() && isSingleFolderMode() && mAccount.equals(account) &&
                    mFolderName.equals(folder)) {
                mUnreadMessageCount = unreadMessageCount;
            }
            super.folderStatusChanged(account, folder, unreadMessageCount);
        }

        private boolean updateForMe(Account account, String folder) {
            if (account == null || folder == null) {
                return false;
            }

            if (!Utility.arrayContains(mAccountUuids, account.getUuid())) {
                return false;
            }

            List<String> folderNames = mSearch.getFolderNames();
            return (folderNames.isEmpty() || folderNames.contains(folder));
        }
    }


    class MessageListAdapter extends CursorAdapter {

        private Drawable mAttachmentIcon;
        private Drawable mForwardedIcon;
        private Drawable mAnsweredIcon;
        private Drawable mForwardedAnsweredIcon;

        MessageListAdapter() {
            super(getActivity(), null, 0);
            mAttachmentIcon = getResources().getDrawable(R.drawable.ic_email_attachment_small);
            mAnsweredIcon = getResources().getDrawable(R.drawable.ic_email_answered_small);
            mForwardedIcon = getResources().getDrawable(R.drawable.ic_email_forwarded_small);
            mForwardedAnsweredIcon = getResources().getDrawable(R.drawable.ic_email_forwarded_answered_small);
        }

        private String recipientSigil(boolean toMe, boolean ccMe) {
            if (toMe) {
                return getString(R.string.messagelist_sent_to_me_sigil);
            } else if (ccMe) {
                return getString(R.string.messagelist_sent_cc_me_sigil);
            } else {
                return "";
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = mInflater.inflate(R.layout.message_list_item, parent, false);

            MessageViewHolder holder = new MessageViewHolder();
            holder.date = (TextView) view.findViewById(R.id.date);
            holder.chip = view.findViewById(R.id.chip);


            if (mPreviewLines == 0 && mContactsPictureLoader == null) {
                view.findViewById(R.id.preview).setVisibility(View.GONE);
                holder.preview = (TextView) view.findViewById(R.id.sender_compact);
                holder.flagged = (CheckBox) view.findViewById(R.id.flagged_center_right);
                view.findViewById(R.id.flagged_bottom_right).setVisibility(View.GONE);



            } else {
                view.findViewById(R.id.sender_compact).setVisibility(View.GONE);
                holder.preview = (TextView) view.findViewById(R.id.preview);
                holder.flagged = (CheckBox) view.findViewById(R.id.flagged_bottom_right);
                view.findViewById(R.id.flagged_center_right).setVisibility(View.GONE);

            }

            QuickContactBadge contactBadge =
                    (QuickContactBadge) view.findViewById(R.id.contact_badge);
            if (mContactsPictureLoader != null) {
                holder.contactBadge = contactBadge;
            } else {
                contactBadge.setVisibility(View.GONE);
            }

            if (mSenderAboveSubject) {
                holder.from = (TextView) view.findViewById(R.id.subject);
                mFontSizes.setViewTextSize(holder.from, mFontSizes.getMessageListSender());

            } else {
                holder.subject = (TextView) view.findViewById(R.id.subject);
                mFontSizes.setViewTextSize(holder.subject, mFontSizes.getMessageListSubject());

            }

            mFontSizes.setViewTextSize(holder.date, mFontSizes.getMessageListDate());


            // 1 preview line is needed even if it is set to 0, because subject is part of the same text view
            holder.preview.setLines(Math.max(mPreviewLines,1));
            mFontSizes.setViewTextSize(holder.preview, mFontSizes.getMessageListPreview());
            holder.threadCount = (TextView) view.findViewById(R.id.thread_count);
            mFontSizes.setViewTextSize(holder.threadCount, mFontSizes.getMessageListSubject()); // thread count is next to subject
            view.findViewById(R.id.selected_checkbox_wrapper).setVisibility((mCheckboxes) ? View.VISIBLE : View.GONE);

            holder.flagged.setVisibility(mStars ? View.VISIBLE : View.GONE);
            holder.flagged.setOnClickListener(holder);


            holder.selected = (CheckBox) view.findViewById(R.id.selected_checkbox);
            holder.selected.setOnClickListener(holder);


            view.setTag(holder);

            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            Account account = getAccountFromCursor(cursor);

            String fromList = cursor.getString(SENDER_LIST_COLUMN);
            String toList = cursor.getString(TO_LIST_COLUMN);
            String ccList = cursor.getString(CC_LIST_COLUMN);
            Address[] fromAddrs = Address.unpack(fromList);
            Address[] toAddrs = Address.unpack(toList);
            Address[] ccAddrs = Address.unpack(ccList);

            boolean fromMe = mMessageHelper.toMe(account, fromAddrs);
            boolean toMe = mMessageHelper.toMe(account, toAddrs);
            boolean ccMe = mMessageHelper.toMe(account, ccAddrs);

            CharSequence displayName = mMessageHelper.getDisplayName(account, fromAddrs, toAddrs);
            CharSequence displayDate = DateUtils.getRelativeTimeSpanString(context, cursor.getLong(DATE_COLUMN));

            Address counterpartyAddress = null;
            if (fromMe) {
                if (toAddrs.length > 0) {
                    counterpartyAddress = toAddrs[0];
                } else if (ccAddrs.length > 0) {
                    counterpartyAddress = ccAddrs[0];
                }
            } else if (fromAddrs.length > 0) {
                counterpartyAddress = fromAddrs[0];
            }

            int threadCount = (mThreadedList) ? cursor.getInt(THREAD_COUNT_COLUMN) : 0;

            String subject = cursor.getString(SUBJECT_COLUMN);
            if (TextUtils.isEmpty(subject)) {
                subject = getString(R.string.general_no_subject);
            } else if (threadCount > 1) {
                // If this is a thread, strip the RE/FW from the subject.  "Be like Outlook."
                subject = Utility.stripSubject(subject);
            }

            boolean read = (cursor.getInt(READ_COLUMN) == 1);
            boolean flagged = (cursor.getInt(FLAGGED_COLUMN) == 1);
            boolean answered = (cursor.getInt(ANSWERED_COLUMN) == 1);
            boolean forwarded = (cursor.getInt(FORWARDED_COLUMN) == 1);

            boolean hasAttachments = (cursor.getInt(ATTACHMENT_COUNT_COLUMN) > 0);

            MessageViewHolder holder = (MessageViewHolder) view.getTag();

            int maybeBoldTypeface = (read) ? Typeface.NORMAL : Typeface.BOLD;

            long uniqueId = cursor.getLong(mUniqueIdColumn);
            boolean selected = mSelected.contains(uniqueId);


            holder.chip.setBackgroundColor(account.getChipColor());

            if (mCheckboxes) {
                holder.selected.setChecked(selected);
            }

            if (mStars) {
                holder.flagged.setChecked(flagged);
            }
            holder.position = cursor.getPosition();

            if (holder.contactBadge != null) {
                if (counterpartyAddress != null) {
                    Utility.setContactForBadge(holder.contactBadge, counterpartyAddress);
                    /*
                     * At least in Android 2.2 a different background + padding is used when no
                     * email address is available. ListView reuses the views but QuickContactBadge
                     * doesn't reset the padding, so we do it ourselves.
                     */
                    holder.contactBadge.setPadding(0, 0, 0, 0);
                    mContactsPictureLoader.loadContactPicture(counterpartyAddress, holder.contactBadge);
                } else {
                    holder.contactBadge.assignContactUri(null);
                    holder.contactBadge.setImageResource(R.drawable.ic_contact_picture);
                }
            }

            // Background color
            if (selected || K9.useBackgroundAsUnreadIndicator()) {
                int res;
                if (selected) {
                    res = R.attr.messageListSelectedBackgroundColor;
                } else if (read) {
                    res = R.attr.messageListReadItemBackgroundColor;
                } else {
                    res = R.attr.messageListUnreadItemBackgroundColor;
                }

                TypedValue outValue = new TypedValue();
                getActivity().getTheme().resolveAttribute(res, outValue, true);
                view.setBackgroundColor(outValue.data);
            } else {
                view.setBackgroundColor(Color.TRANSPARENT);
            }

            if (mActiveMessage != null) {
                String uid = cursor.getString(UID_COLUMN);
                String folderName = cursor.getString(FOLDER_NAME_COLUMN);

                if (account.getUuid().equals(mActiveMessage.getAccountUuid()) &&
                        folderName.equals(mActiveMessage.getFolderName()) &&
                        uid.equals(mActiveMessage.getUid())) {
                    int res = R.attr.messageListActiveItemBackgroundColor;

                    TypedValue outValue = new TypedValue();
                    getActivity().getTheme().resolveAttribute(res, outValue, true);
                    view.setBackgroundColor(outValue.data);
                }
            }

            // Thread count
            if (threadCount > 1) {
                holder.threadCount.setText(String.format("%d", threadCount));
                holder.threadCount.setVisibility(View.VISIBLE);
            } else {
                holder.threadCount.setVisibility(View.GONE);
            }

            CharSequence beforePreviewText = (mSenderAboveSubject) ? subject : displayName;

            String sigil = recipientSigil(toMe, ccMe);

            SpannableStringBuilder messageStringBuilder = new SpannableStringBuilder(sigil)
                    .append(beforePreviewText);

            if (mPreviewLines > 0) {
                String preview = getPreview(cursor);
                messageStringBuilder.append(" ").append(preview);
            }

            holder.preview.setText(messageStringBuilder, TextView.BufferType.SPANNABLE);

            Spannable str = (Spannable)holder.preview.getText();

            // Create a span section for the sender, and assign the correct font size and weight
            int fontSize = (mSenderAboveSubject) ?
                    mFontSizes.getMessageListSubject():
                    mFontSizes.getMessageListSender();

            AbsoluteSizeSpan span = new AbsoluteSizeSpan(fontSize, true);
            str.setSpan(span, 0, beforePreviewText.length() + sigil.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            //TODO: make this part of the theme
            int color = (K9.getK9Theme() == K9.Theme.LIGHT) ?
                    Color.rgb(105, 105, 105) :
                    Color.rgb(160, 160, 160);

            // Set span (color) for preview message
            str.setSpan(new ForegroundColorSpan(color), beforePreviewText.length() + sigil.length(),
                    str.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            Drawable statusHolder = null;
            if (forwarded && answered) {
                statusHolder = mForwardedAnsweredIcon;
            } else if (answered) {
                statusHolder = mAnsweredIcon;
            } else if (forwarded) {
                statusHolder = mForwardedIcon;
            }

            if (holder.from != null ) {
                holder.from.setTypeface(Typeface.create(holder.from.getTypeface(), maybeBoldTypeface));
                if (mSenderAboveSubject) {
                    holder.from.setCompoundDrawablesWithIntrinsicBounds(
                            statusHolder, // left
                            null, // top
                            hasAttachments ? mAttachmentIcon : null, // right
                            null); // bottom

                    holder.from.setText(displayName);
                } else {
                    holder.from.setText(new SpannableStringBuilder(sigil).append(displayName));
                }
            }

            if (holder.subject != null ) {
                if (!mSenderAboveSubject) {
                    holder.subject.setCompoundDrawablesWithIntrinsicBounds(
                            statusHolder, // left
                            null, // top
                            hasAttachments ? mAttachmentIcon : null, // right
                            null); // bottom
                }

                holder.subject.setTypeface(Typeface.create(holder.subject.getTypeface(), maybeBoldTypeface));
                holder.subject.setText(subject);
            }

            holder.date.setText(displayDate);
        }

        private String getPreview(Cursor cursor) {
            String previewTypeString = cursor.getString(PREVIEW_TYPE_COLUMN);
            DatabasePreviewType previewType = DatabasePreviewType.fromDatabaseValue(previewTypeString);

            switch (previewType) {
                case NONE:
                case ERROR: {
                    return "";
                }
                case ENCRYPTED: {
                    return getString(R.string.preview_encrypted);
                }
                case TEXT: {
                    return cursor.getString(PREVIEW_COLUMN);
                }
            }

            throw new AssertionError("Unknown preview type: " + previewType);
        }
    }

    class MessageViewHolder implements View.OnClickListener {
        public TextView subject;
        public TextView preview;
        public TextView from;
        public TextView time;
        public TextView date;
        public View chip;
        public TextView threadCount;
        public CheckBox flagged;
        public CheckBox selected;
        public int position = -1;
        public QuickContactBadge contactBadge;
        @Override
        public void onClick(View view) {
            if (position != -1) {

                switch (view.getId()) {
                    case R.id.selected_checkbox:
                        toggleMessageSelectWithAdapterPosition(position);
                        break;
                    case R.id.flagged_bottom_right:
                    case R.id.flagged_center_right:
                        toggleMessageFlagWithAdapterPosition(position);
                        break;
                }
            }
        }
    }


    private View getFooterView(ViewGroup parent) {
        if (mFooterView == null) {
            mFooterView = mInflater.inflate(R.layout.message_list_item_footer, parent, false);
            FooterViewHolder holder = new FooterViewHolder();
            holder.main = (TextView) mFooterView.findViewById(R.id.main_text);
            mFooterView.setTag(holder);
        }

        return mFooterView;
    }

    private void updateFooterView() {
        if (!mSearch.isManualSearch() && mCurrentFolder != null && mAccount != null) {
            if (mCurrentFolder.loading) {
                updateFooter(mContext.getString(R.string.status_loading_more));
            } else if (!mCurrentFolder.moreMessages) {
                updateFooter(null);
            } else {
                String message;
                if (!mCurrentFolder.lastCheckFailed) {
                    if (mAccount.getDisplayCount() == 0) {
                        message = mContext.getString(R.string.message_list_load_more_messages_action);
                    } else {
                        message = String.format(mContext.getString(R.string.load_more_messages_fmt),
                                mAccount.getDisplayCount());
                    }
                } else {
                    message = mContext.getString(R.string.status_loading_more_failed);
                }
                updateFooter(message);
            }
        } else {
            updateFooter(null);
        }
    }

    public void updateFooter(final String text) {
        if (mFooterView == null) {
            return;
        }

        FooterViewHolder holder = (FooterViewHolder) mFooterView.getTag();

        if (text != null) {
            holder.main.setText(text);
            holder.main.setVisibility(View.VISIBLE);
        } else {
            holder.main.setVisibility(View.GONE);
        }
    }

    static class FooterViewHolder {
        public TextView main;
    }

    /**
     * Set selection state for all messages.
     *
     * @param selected
     *         If {@code true} all messages get selected. Otherwise, all messages get deselected and
     *         action mode is finished.
     */
    private void setSelectionState(boolean selected) {
        if (selected) {
            if (mAdapter.getCount() == 0) {
                // Nothing to do if there are no messages
                return;
            }

            mSelectedCount = 0;
            for (int i = 0, end = mAdapter.getCount(); i < end; i++) {
                Cursor cursor = (Cursor) mAdapter.getItem(i);
                long uniqueId = cursor.getLong(mUniqueIdColumn);
                mSelected.add(uniqueId);

                if (mThreadedList) {
                    int threadCount = cursor.getInt(THREAD_COUNT_COLUMN);
                    mSelectedCount += (threadCount > 1) ? threadCount : 1;
                } else {
                    mSelectedCount++;
                }
            }

            if (mActionMode == null) {
                startAndPrepareActionMode();
            }
            computeBatchDirection();
            updateActionModeTitle();
            computeSelectAllVisibility();
        } else {
            mSelected.clear();
            mSelectedCount = 0;
            if (mActionMode != null) {
                mActionMode.finish();
                mActionMode = null;
            }
        }

        mAdapter.notifyDataSetChanged();
    }

    private void toggleMessageSelect(int listViewPosition) {
        int adapterPosition = listViewToAdapterPosition(listViewPosition);
        if (adapterPosition == AdapterView.INVALID_POSITION) {
            return;
        }

        toggleMessageSelectWithAdapterPosition(adapterPosition);
    }

    private void toggleMessageFlagWithAdapterPosition(int adapterPosition) {
        Cursor cursor = (Cursor) mAdapter.getItem(adapterPosition);
        boolean flagged = (cursor.getInt(FLAGGED_COLUMN) == 1);

        setFlag(adapterPosition,Flag.FLAGGED, !flagged);
    }

    private void toggleMessageSelectWithAdapterPosition(int adapterPosition) {
        Cursor cursor = (Cursor) mAdapter.getItem(adapterPosition);
        long uniqueId = cursor.getLong(mUniqueIdColumn);

        boolean selected = mSelected.contains(uniqueId);
        if (!selected) {
            mSelected.add(uniqueId);
        } else {
            mSelected.remove(uniqueId);
        }

        int selectedCountDelta = 1;
        if (mThreadedList) {
            int threadCount = cursor.getInt(THREAD_COUNT_COLUMN);
            if (threadCount > 1) {
                selectedCountDelta = threadCount;
            }
        }

        if (mActionMode != null) {
            if (mSelectedCount == selectedCountDelta && selected) {
                mActionMode.finish();
                mActionMode = null;
                return;
            }
        } else {
            startAndPrepareActionMode();
        }

        if (selected) {
            mSelectedCount -= selectedCountDelta;
        } else {
            mSelectedCount += selectedCountDelta;
        }

        computeBatchDirection();
        updateActionModeTitle();

        computeSelectAllVisibility();

        mAdapter.notifyDataSetChanged();
    }

    private void updateActionModeTitle() {
        mActionMode.setTitle(String.format(getString(R.string.actionbar_selected), mSelectedCount));
    }

    private void computeSelectAllVisibility() {
        mActionModeCallback.showSelectAll(mSelected.size() != mAdapter.getCount());
    }

    private void computeBatchDirection() {
        boolean isBatchFlag = false;
        boolean isBatchRead = false;

        for (int i = 0, end = mAdapter.getCount(); i < end; i++) {
            Cursor cursor = (Cursor) mAdapter.getItem(i);
            long uniqueId = cursor.getLong(mUniqueIdColumn);

            if (mSelected.contains(uniqueId)) {
                boolean read = (cursor.getInt(READ_COLUMN) == 1);
                boolean flagged = (cursor.getInt(FLAGGED_COLUMN) == 1);

                if (!flagged) {
                    isBatchFlag = true;
                }
                if (!read) {
                    isBatchRead = true;
                }

                if (isBatchFlag && isBatchRead) {
                    break;
                }
            }
        }

        mActionModeCallback.showMarkAsRead(isBatchRead);
        mActionModeCallback.showFlag(isBatchFlag);
    }

    private void setFlag(int adapterPosition, final Flag flag, final boolean newState) {
        if (adapterPosition == AdapterView.INVALID_POSITION) {
            return;
        }

        Cursor cursor = (Cursor) mAdapter.getItem(adapterPosition);
        Account account = mPreferences.getAccount(cursor.getString(ACCOUNT_UUID_COLUMN));

        if (mThreadedList && cursor.getInt(THREAD_COUNT_COLUMN) > 1) {
            long threadRootId = cursor.getLong(THREAD_ROOT_COLUMN);
            mController.setFlagForThreads(account,
                    Collections.singletonList(Long.valueOf(threadRootId)), flag, newState);
        } else {
            long id = cursor.getLong(ID_COLUMN);
            mController.setFlag(account, Collections.singletonList(Long.valueOf(id)), flag,
                    newState);
        }

        computeBatchDirection();
    }

    private void setFlagForSelected(final Flag flag, final boolean newState) {
        if (mSelected.isEmpty()) {
            return;
        }

        Map<Account, List<Long>> messageMap = new HashMap<>();
        Map<Account, List<Long>> threadMap = new HashMap<>();
        Set<Account> accounts = new HashSet<>();

        for (int position = 0, end = mAdapter.getCount(); position < end; position++) {
            Cursor cursor = (Cursor) mAdapter.getItem(position);
            long uniqueId = cursor.getLong(mUniqueIdColumn);

            if (mSelected.contains(uniqueId)) {
                String uuid = cursor.getString(ACCOUNT_UUID_COLUMN);
                Account account = mPreferences.getAccount(uuid);
                accounts.add(account);

                if (mThreadedList && cursor.getInt(THREAD_COUNT_COLUMN) > 1) {
                    List<Long> threadRootIdList = threadMap.get(account);
                    if (threadRootIdList == null) {
                        threadRootIdList = new ArrayList<>();
                        threadMap.put(account, threadRootIdList);
                    }

                    threadRootIdList.add(cursor.getLong(THREAD_ROOT_COLUMN));
                } else {
                    List<Long> messageIdList = messageMap.get(account);
                    if (messageIdList == null) {
                        messageIdList = new ArrayList<>();
                        messageMap.put(account, messageIdList);
                    }

                    messageIdList.add(cursor.getLong(ID_COLUMN));
                }
            }
        }

        for (Account account : accounts) {
            List<Long> messageIds = messageMap.get(account);
            List<Long> threadRootIds = threadMap.get(account);

            if (messageIds != null) {
                mController.setFlag(account, messageIds, flag, newState);
            }

            if (threadRootIds != null) {
                mController.setFlagForThreads(account, threadRootIds, flag, newState);
            }
        }

        computeBatchDirection();
    }

    private void onMove(MessageReference message) {
        onMove(Collections.singletonList(message));
    }

    /**
     * Display the message move activity.
     *
     * @param messages
     *         Never {@code null}.
     */
    private void onMove(List<MessageReference> messages) {
        if (!checkCopyOrMovePossible(messages, FolderOperation.MOVE)) {
            return;
        }

        String folderName;
        if (mIsThreadDisplay) {
            folderName = messages.get(0).getFolderName();
        } else if (mSingleFolderMode) {
            folderName = mCurrentFolder.folder.getName();
        } else {
            folderName = null;
        }


        displayFolderChoice(ACTIVITY_CHOOSE_FOLDER_MOVE, folderName,
                messages.get(0).getAccountUuid(), null,
                messages);
    }

    private void onCopy(MessageReference message) {
        onCopy(Collections.singletonList(message));
    }

    /**
     * Display the message copy activity.
     *
     * @param messages
     *         Never {@code null}.
     */
    private void onCopy(List<MessageReference> messages) {
        if (!checkCopyOrMovePossible(messages, FolderOperation.COPY)) {
            return;
        }

        String folderName;
        if (mIsThreadDisplay) {
            folderName = messages.get(0).getFolderName();
        } else if (mSingleFolderMode) {
            folderName = mCurrentFolder.folder.getName();
        } else {
            folderName = null;
        }

        displayFolderChoice(ACTIVITY_CHOOSE_FOLDER_COPY, folderName,
                messages.get(0).getAccountUuid(),
                null,
                messages);
    }

    private void onDebugClearLocally(MessageReference message) {
        mController.debugClearMessagesLocally(Collections.singletonList(message));
    }

    /**
     * Helper method to manage the invocation of {@link #startActivityForResult(Intent, int)} for a
     * folder operation ({@link ChooseFolder} activity), while saving a list of associated messages.
     *
     * @param requestCode
     *         If {@code >= 0}, this code will be returned in {@code onActivityResult()} when the
     *         activity exits.
     *
     * @see #startActivityForResult(Intent, int)
     */
    private void displayFolderChoice(int requestCode, String sourceFolderName,
            String accountUuid, String lastSelectedFolderName,
            List<MessageReference> messages) {
        Intent intent = new Intent(getActivity(), ChooseFolder.class);
        intent.putExtra(ChooseFolder.EXTRA_ACCOUNT, accountUuid);
        intent.putExtra(ChooseFolder.EXTRA_SEL_FOLDER, lastSelectedFolderName);

        if (sourceFolderName == null) {
            intent.putExtra(ChooseFolder.EXTRA_SHOW_CURRENT, "yes");
        } else {
            intent.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, sourceFolderName);
        }

        // remember the selected messages for #onActivityResult
        mActiveMessages = messages;
        startActivityForResult(intent, requestCode);
    }

    private void onArchive(MessageReference message) {
        onArchive(Collections.singletonList(message));
    }

    private void onArchive(final List<MessageReference> messages) {
        Map<Account, List<MessageReference>> messagesByAccount = groupMessagesByAccount(messages);

        for (Entry<Account, List<MessageReference>> entry : messagesByAccount.entrySet()) {
            Account account = entry.getKey();
            String archiveFolder = account.getArchiveFolderName();

            if (!K9.FOLDER_NONE.equals(archiveFolder)) {
                move(entry.getValue(), archiveFolder);
            }
        }
    }

    private Map<Account, List<MessageReference>> groupMessagesByAccount(final List<MessageReference> messages) {
        Map<Account, List<MessageReference>> messagesByAccount = new HashMap<>();
        for (MessageReference message : messages) {
            Account account = mPreferences.getAccount(message.getAccountUuid());

            List<MessageReference> msgList = messagesByAccount.get(account);
            if (msgList == null) {
                msgList = new ArrayList<>();
                messagesByAccount.put(account, msgList);
            }

            msgList.add(message);
        }
        return messagesByAccount;
    }

    private void onSpam(MessageReference message) {
        onSpam(Collections.singletonList(message));
    }

    /**
     * Move messages to the spam folder.
     *
     * @param messages
     *         The messages to move to the spam folder. Never {@code null}.
     */
    private void onSpam(List<MessageReference> messages) {
        if (K9.confirmSpam()) {
            // remember the message selection for #onCreateDialog(int)
            mActiveMessages = messages;
            showDialog(R.id.dialog_confirm_spam);
        } else {
            onSpamConfirmed(messages);
        }
    }

    private void onSpamConfirmed(List<MessageReference> messages) {
        Map<Account, List<MessageReference>> messagesByAccount = groupMessagesByAccount(messages);

        for (Entry<Account, List<MessageReference>> entry : messagesByAccount.entrySet()) {
            Account account = entry.getKey();
            String spamFolder = account.getSpamFolderName();

            if (!K9.FOLDER_NONE.equals(spamFolder)) {
                move(entry.getValue(), spamFolder);
            }
        }
    }

    private static enum FolderOperation {
        COPY, MOVE
    }

    /**
     * Display a Toast message if any message isn't synchronized
     *
     * @param messages
     *         The messages to copy or move. Never {@code null}.
     * @param operation
     *         The type of operation to perform. Never {@code null}.
     *
     * @return {@code true}, if operation is possible.
     */
    private boolean checkCopyOrMovePossible(final List<MessageReference> messages,
            final FolderOperation operation) {

        if (messages.isEmpty()) {
            return false;
        }

        boolean first = true;
        for (MessageReference message : messages) {
            if (first) {
                first = false;
                Account account = mPreferences.getAccount(message.getAccountUuid());
                if ((operation == FolderOperation.MOVE && !mController.isMoveCapable(account)) ||
                        (operation == FolderOperation.COPY && !mController.isCopyCapable(account))) {
                    return false;
                }
            }
            // message check
            if ((operation == FolderOperation.MOVE && !mController.isMoveCapable(message)) ||
                    (operation == FolderOperation.COPY && !mController.isCopyCapable(message))) {
                final Toast toast = Toast.makeText(getActivity(), R.string.move_copy_cannot_copy_unsynced_message,
                                                   Toast.LENGTH_LONG);
                toast.show();
                return false;
            }
        }
        return true;
    }

    /**
     * Copy the specified messages to the specified folder.
     *
     * @param messages
     *         List of messages to copy. Never {@code null}.
     * @param destination
     *         The name of the destination folder. Never {@code null}.
     */
    private void copy(List<MessageReference> messages, final String destination) {
        copyOrMove(messages, destination, FolderOperation.COPY);
    }

    /**
     * Move the specified messages to the specified folder.
     *
     * @param messages
     *         The list of messages to move. Never {@code null}.
     * @param destination
     *         The name of the destination folder. Never {@code null}.
     */
    private void move(List<MessageReference> messages, final String destination) {
        copyOrMove(messages, destination, FolderOperation.MOVE);
    }

    /**
     * The underlying implementation for {@link #copy(List, String)} and
     * {@link #move(List, String)}. This method was added mainly because those 2
     * methods share common behavior.
     *
     * @param messages
     *         The list of messages to copy or move. Never {@code null}.
     * @param destination
     *         The name of the destination folder. Never {@code null} or {@link K9#FOLDER_NONE}.
     * @param operation
     *         Specifies what operation to perform. Never {@code null}.
     */
    private void copyOrMove(List<MessageReference> messages, final String destination,
            final FolderOperation operation) {

        Map<String, List<MessageReference>> folderMap = new HashMap<>();

        for (MessageReference message : messages) {
            if ((operation == FolderOperation.MOVE && !mController.isMoveCapable(message)) ||
                    (operation == FolderOperation.COPY && !mController.isCopyCapable(message))) {

                Toast.makeText(getActivity(), R.string.move_copy_cannot_copy_unsynced_message,
                        Toast.LENGTH_LONG).show();

                // XXX return meaningful error value?

                // message isn't synchronized
                return;
            }

            String folderName = message.getFolderName();
            if (folderName.equals(destination)) {
                // Skip messages already in the destination folder
                continue;
            }

            List<MessageReference> outMessages = folderMap.get(folderName);
            if (outMessages == null) {
                outMessages = new ArrayList<>();
                folderMap.put(folderName, outMessages);
            }

            outMessages.add(message);
        }

        for (Map.Entry<String, List<MessageReference>> entry : folderMap.entrySet()) {
            String folderName = entry.getKey();
            List<MessageReference> outMessages = entry.getValue();
            Account account = mPreferences.getAccount(outMessages.get(0).getAccountUuid());

            if (operation == FolderOperation.MOVE) {
                if (mThreadedList) {
                    mController.moveMessagesInThread(account, folderName, outMessages, destination);
                } else {
                    mController.moveMessages(account, folderName, outMessages, destination);
                }
            } else {
                if (mThreadedList) {
                    mController.copyMessagesInThread(account, folderName, outMessages, destination);
                } else {
                    mController.copyMessages(account, folderName, outMessages, destination);
                }
            }
        }
    }


    class ActionModeCallback implements ActionMode.Callback {
        private MenuItem mSelectAll;
        private MenuItem mMarkAsRead;
        private MenuItem mMarkAsUnread;
        private MenuItem mFlag;
        private MenuItem mUnflag;

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            mSelectAll = menu.findItem(R.id.select_all);
            mMarkAsRead = menu.findItem(R.id.mark_as_read);
            mMarkAsUnread = menu.findItem(R.id.mark_as_unread);
            mFlag = menu.findItem(R.id.flag);
            mUnflag = menu.findItem(R.id.unflag);

            // we don't support cross account actions atm
            if (!mSingleAccountMode) {
                // show all
                menu.findItem(R.id.move).setVisible(true);
                menu.findItem(R.id.archive).setVisible(true);
                menu.findItem(R.id.spam).setVisible(true);
                menu.findItem(R.id.copy).setVisible(true);

                Set<String> accountUuids = getAccountUuidsForSelected();

                for (String accountUuid : accountUuids) {
                    Account account = mPreferences.getAccount(accountUuid);
                    if (account != null) {
                        setContextCapabilities(account, menu);
                    }
                }

            }
            return true;
        }

        /**
         * Get the set of account UUIDs for the selected messages.
         */
        private Set<String> getAccountUuidsForSelected() {
            int maxAccounts = mAccountUuids.length;
            Set<String> accountUuids = new HashSet<>(maxAccounts);

            for (int position = 0, end = mAdapter.getCount(); position < end; position++) {
                Cursor cursor = (Cursor) mAdapter.getItem(position);
                long uniqueId = cursor.getLong(mUniqueIdColumn);

                if (mSelected.contains(uniqueId)) {
                    String accountUuid = cursor.getString(ACCOUNT_UUID_COLUMN);
                    accountUuids.add(accountUuid);

                    if (accountUuids.size() == mAccountUuids.length) {
                        break;
                    }
                }
            }

            return accountUuids;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            mSelectAll = null;
            mMarkAsRead = null;
            mMarkAsUnread = null;
            mFlag = null;
            mUnflag = null;
            setSelectionState(false);
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.message_list_context, menu);

            // check capabilities
            setContextCapabilities(mAccount, menu);

            return true;
        }

        /**
         * Disables menu options not supported by the account type or current "search view".
         *
         * @param account
         *         The account to query for its capabilities.
         * @param menu
         *         The menu to adapt.
         */
        private void setContextCapabilities(Account account, Menu menu) {
            if (!mSingleAccountMode) {
                // We don't support cross-account copy/move operations right now
                menu.findItem(R.id.move).setVisible(false);
                menu.findItem(R.id.copy).setVisible(false);

                //TODO: we could support the archive and spam operations if all selected messages
                // belong to non-POP3 accounts
                menu.findItem(R.id.archive).setVisible(false);
                menu.findItem(R.id.spam).setVisible(false);

            } else {
                // hide unsupported
                if (!mController.isCopyCapable(account)) {
                    menu.findItem(R.id.copy).setVisible(false);
                }

                if (!mController.isMoveCapable(account)) {
                    menu.findItem(R.id.move).setVisible(false);
                    menu.findItem(R.id.archive).setVisible(false);
                    menu.findItem(R.id.spam).setVisible(false);
                }

                if (!account.hasArchiveFolder()) {
                    menu.findItem(R.id.archive).setVisible(false);
                }

                if (!account.hasSpamFolder()) {
                    menu.findItem(R.id.spam).setVisible(false);
                }
            }
        }

        public void showSelectAll(boolean show) {
            if (mActionMode != null) {
                mSelectAll.setVisible(show);
            }
        }

        public void showMarkAsRead(boolean show) {
            if (mActionMode != null) {
                mMarkAsRead.setVisible(show);
                mMarkAsUnread.setVisible(!show);
            }
        }

        public void showFlag(boolean show) {
            if (mActionMode != null) {
                mFlag.setVisible(show);
                mUnflag.setVisible(!show);
            }
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            /*
             * In the following we assume that we can't move or copy
             * mails to the same folder. Also that spam isn't available if we are
             * in the spam folder,same for archive.
             *
             * This is the case currently so safe assumption.
             */
            switch (item.getItemId()) {
            case R.id.delete: {
                List<MessageReference> messages = getCheckedMessages();
                onDelete(messages);
                mSelectedCount = 0;
                break;
            }
            case R.id.mark_as_read: {
                setFlagForSelected(Flag.SEEN, true);
                break;
            }
            case R.id.mark_as_unread: {
                setFlagForSelected(Flag.SEEN, false);
                break;
            }
            case R.id.flag: {
                setFlagForSelected(Flag.FLAGGED, true);
                break;
            }
            case R.id.unflag: {
                setFlagForSelected(Flag.FLAGGED, false);
                break;
            }
            case R.id.select_all: {
                selectAll();
                break;
            }

            // only if the account supports this
            case R.id.archive: {
                onArchive(getCheckedMessages());
                mSelectedCount = 0;
                break;
            }
            case R.id.spam: {
                onSpam(getCheckedMessages());
                mSelectedCount = 0;
                break;
            }
            case R.id.move: {
                onMove(getCheckedMessages());
                mSelectedCount = 0;
                break;
            }
            case R.id.copy: {
                onCopy(getCheckedMessages());
                mSelectedCount = 0;
                break;
            }
            }
            if (mSelectedCount == 0) {
                mActionMode.finish();
            }

            return true;
        }
    }

    @Override
    public void doPositiveClick(int dialogId) {
        switch (dialogId) {
            case R.id.dialog_confirm_spam: {
                onSpamConfirmed(mActiveMessages);
                // No further need for this reference
                mActiveMessages = null;
                break;
            }
            case R.id.dialog_confirm_delete: {
                onDeleteConfirmed(mActiveMessages);
                mActiveMessage = null;
                break;
            }
            case R.id.dialog_confirm_mark_all_as_read: {
                markAllAsRead();
                break;
            }
        }
    }

    @Override
    public void doNegativeClick(int dialogId) {
        switch (dialogId) {
            case R.id.dialog_confirm_spam:
            case R.id.dialog_confirm_delete: {
                // No further need for this reference
                mActiveMessages = null;
                break;
            }
        }
    }

    @Override
    public void dialogCancelled(int dialogId) {
        doNegativeClick(dialogId);
    }

    public void checkMail() {
        if (isSingleAccountMode() && isSingleFolderMode()) {
            mController.synchronizeMailbox(mAccount, mFolderName, mListener, null);
            mController.sendPendingMessages(mAccount, mListener);
        } else if (mAllAccounts) {
            mController.checkMail(mContext, null, true, true, mListener);
        } else {
            for (String accountUuid : mAccountUuids) {
                Account account = mPreferences.getAccount(accountUuid);
                mController.checkMail(mContext, account, true, true, mListener);
            }
        }
    }

    /**
     * We need to do some special clean up when leaving a remote search result screen. If no
     * remote search is in progress, this method does nothing special.
     */
    @Override
    public void onStop() {
        // If we represent a remote search, then kill that before going back.
        if (isRemoteSearch() && mRemoteSearchFuture != null) {
            try {
                Log.i(K9.LOG_TAG, "Remote search in progress, attempting to abort...");
                // Canceling the future stops any message fetches in progress.
                final boolean cancelSuccess = mRemoteSearchFuture.cancel(true);   // mayInterruptIfRunning = true
                if (!cancelSuccess) {
                    Log.e(K9.LOG_TAG, "Could not cancel remote search future.");
                }
                // Closing the folder will kill off the connection if we're mid-search.
                final Account searchAccount = mAccount;
                final Folder remoteFolder = mCurrentFolder.folder;
                remoteFolder.close();
                // Send a remoteSearchFinished() message for good measure.
                mListener.remoteSearchFinished(mCurrentFolder.name, 0, searchAccount.getRemoteSearchNumResults(), null);
            } catch (Exception e) {
                // Since the user is going back, log and squash any exceptions.
                Log.e(K9.LOG_TAG, "Could not abort remote search before going back", e);
            }
        }
        super.onStop();
    }

    public void selectAll() {
        setSelectionState(true);
    }

    public void onMoveUp() {
        int currentPosition = mListView.getSelectedItemPosition();
        if (currentPosition == AdapterView.INVALID_POSITION || mListView.isInTouchMode()) {
            currentPosition = mListView.getFirstVisiblePosition();
        }
        if (currentPosition > 0) {
            mListView.setSelection(currentPosition - 1);
        }
    }

    public void onMoveDown() {
        int currentPosition = mListView.getSelectedItemPosition();
        if (currentPosition == AdapterView.INVALID_POSITION || mListView.isInTouchMode()) {
            currentPosition = mListView.getFirstVisiblePosition();
        }

        if (currentPosition < mListView.getCount()) {
            mListView.setSelection(currentPosition + 1);
        }
    }

    public boolean openPrevious(MessageReference messageReference) {
        int position = getPosition(messageReference);
        if (position <= 0) {
            return false;
        }

        openMessageAtPosition(position - 1);
        return true;
    }

    public boolean openNext(MessageReference messageReference) {
        int position = getPosition(messageReference);
        if (position < 0 || position == mAdapter.getCount() - 1) {
            return false;
        }

        openMessageAtPosition(position + 1);
        return true;
    }

    public boolean isFirst(MessageReference messageReference) {
        return mAdapter.isEmpty() || messageReference.equals(getReferenceForPosition(0));
    }

    public boolean isLast(MessageReference messageReference) {
        return mAdapter.isEmpty() || messageReference.equals(getReferenceForPosition(mAdapter.getCount() - 1));
    }

    private MessageReference getReferenceForPosition(int position) {
        Cursor cursor = (Cursor) mAdapter.getItem(position);

        String accountUuid = cursor.getString(ACCOUNT_UUID_COLUMN);
        String folderName = cursor.getString(FOLDER_NAME_COLUMN);
        String messageUid = cursor.getString(UID_COLUMN);
        return new MessageReference(accountUuid, folderName, messageUid, null);
    }

    private void openMessageAtPosition(int position) {
        // Scroll message into view if necessary
        int listViewPosition = adapterToListViewPosition(position);
        if (listViewPosition != AdapterView.INVALID_POSITION &&
                (listViewPosition < mListView.getFirstVisiblePosition() ||
                listViewPosition > mListView.getLastVisiblePosition())) {
            mListView.setSelection(listViewPosition);
        }

        MessageReference ref = getReferenceForPosition(position);

        // For some reason the mListView.setSelection() above won't do anything when we call
        // onOpenMessage() (and consequently mAdapter.notifyDataSetChanged()) right away. So we
        // defer the call using MessageListHandler.
        mHandler.openMessage(ref);
    }

    private int getPosition(MessageReference messageReference) {
        for (int i = 0, len = mAdapter.getCount(); i < len; i++) {
            Cursor cursor = (Cursor) mAdapter.getItem(i);

            String accountUuid = cursor.getString(ACCOUNT_UUID_COLUMN);
            String folderName = cursor.getString(FOLDER_NAME_COLUMN);
            String uid = cursor.getString(UID_COLUMN);

            if (accountUuid.equals(messageReference.getAccountUuid()) &&
                    folderName.equals(messageReference.getFolderName()) &&
                    uid.equals(messageReference.getUid())) {
                return i;
            }
        }

        return -1;
    }

    public interface MessageListFragmentListener {
        void enableActionBarProgress(boolean enable);
        void setMessageListProgress(int level);
        void showThread(Account account, String folderName, long rootId);
        void showMoreFromSameSender(String senderAddress);
        void onResendMessage(MessageReference message);
        void onForward(MessageReference message);
        void onReply(MessageReference message);
        void onReplyAll(MessageReference message);
        void openMessage(MessageReference messageReference);
        void setMessageListTitle(String title);
        void setMessageListSubTitle(String subTitle);
        void setUnreadCount(int unread);
        void onCompose(Account account);
        boolean startSearch(Account account, String folderName);
        void remoteSearchStarted();
        void goBack();
        void updateMenu();
    }

    public void onReverseSort() {
        changeSort(mSortType);
    }

    private MessageReference getSelectedMessage() {
        int listViewPosition = mListView.getSelectedItemPosition();
        int adapterPosition = listViewToAdapterPosition(listViewPosition);

        return getMessageAtPosition(adapterPosition);
    }

    private int getAdapterPositionForSelectedMessage() {
        int listViewPosition = mListView.getSelectedItemPosition();
        return listViewToAdapterPosition(listViewPosition);
    }

    private int getPositionForUniqueId(long uniqueId) {
        for (int position = 0, end = mAdapter.getCount(); position < end; position++) {
            Cursor cursor = (Cursor) mAdapter.getItem(position);
            if (cursor.getLong(mUniqueIdColumn) == uniqueId) {
                return position;
            }
        }

        return AdapterView.INVALID_POSITION;
    }

    private MessageReference getMessageAtPosition(int adapterPosition) {
        if (adapterPosition == AdapterView.INVALID_POSITION) {
            return null;
        }

        Cursor cursor = (Cursor) mAdapter.getItem(adapterPosition);

        String accountUuid = cursor.getString(ACCOUNT_UUID_COLUMN);
        String folderName = cursor.getString(FOLDER_NAME_COLUMN);
        String messageUid = cursor.getString(UID_COLUMN);

        return new MessageReference(accountUuid, folderName, messageUid, null);
    }

    private List<MessageReference> getCheckedMessages() {
        List<MessageReference> messages = new ArrayList<>(mSelected.size());
        for (int position = 0, end = mAdapter.getCount(); position < end; position++) {
            Cursor cursor = (Cursor) mAdapter.getItem(position);
            long uniqueId = cursor.getLong(mUniqueIdColumn);

            if (mSelected.contains(uniqueId)) {
                MessageReference message = getMessageAtPosition(position);
                if (message != null) {
                    messages.add(message);
                }
            }
        }

        return messages;
    }

    public void onDelete() {
        MessageReference message = getSelectedMessage();
        if (message != null) {
            onDelete(Collections.singletonList(message));
        }
    }

    public void toggleMessageSelect() {
        toggleMessageSelect(mListView.getSelectedItemPosition());
    }

    public void onToggleFlagged() {
        onToggleFlag(Flag.FLAGGED, FLAGGED_COLUMN);
    }

    public void onToggleRead() {
        onToggleFlag(Flag.SEEN, READ_COLUMN);
    }

    private void onToggleFlag(Flag flag, int flagColumn) {
        int adapterPosition = getAdapterPositionForSelectedMessage();
        if (adapterPosition == ListView.INVALID_POSITION) {
            return;
        }

        Cursor cursor = (Cursor) mAdapter.getItem(adapterPosition);
        boolean flagState = (cursor.getInt(flagColumn) == 1);
        setFlag(adapterPosition, flag, !flagState);
    }

    public void onMove() {
        MessageReference message = getSelectedMessage();
        if (message != null) {
            onMove(message);
        }
    }

    public void onArchive() {
        MessageReference message = getSelectedMessage();
        if (message != null) {
            onArchive(message);
        }
    }

    public void onCopy() {
        MessageReference message = getSelectedMessage();
        if (message != null) {
            onCopy(message);
        }
    }

    public boolean isOutbox() {
        return (mFolderName != null && mFolderName.equals(mAccount.getOutboxFolderName()));
    }

    public boolean isErrorFolder() {
        return K9.ERROR_FOLDER_NAME.equals(mFolderName);
    }

    public boolean isRemoteFolder() {
        if (mSearch.isManualSearch() || isOutbox() || isErrorFolder()) {
            return false;
        }

        if (!mController.isMoveCapable(mAccount)) {
            // For POP3 accounts only the Inbox is a remote folder.
            return (mFolderName != null && mFolderName.equals(mAccount.getInboxFolderName()));
        }

        return true;
    }

    public boolean isManualSearch() {
        return mSearch.isManualSearch();
    }

    public boolean isAccountExpungeCapable() {
        try {
            return (mAccount != null && mAccount.getRemoteStore().isExpungeCapable());
        } catch (Exception e) {
            return false;
        }
    }

    public void onRemoteSearch() {
        // Remote search is useless without the network.
        if (mHasConnectivity) {
            onRemoteSearchRequested();
        } else {
            Toast.makeText(getActivity(), getText(R.string.remote_search_unavailable_no_network),
                    Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isRemoteSearch() {
        return mRemoteSearchPerformed;
    }

    public boolean isRemoteSearchAllowed() {
        if (!mSearch.isManualSearch() || mRemoteSearchPerformed || !mSingleFolderMode) {
            return false;
        }

        boolean allowRemoteSearch = false;
        final Account searchAccount = mAccount;
        if (searchAccount != null) {
            allowRemoteSearch = searchAccount.allowRemoteSearch();
        }

        return allowRemoteSearch;
    }

    public boolean onSearchRequested() {
        String folderName = (mCurrentFolder != null) ? mCurrentFolder.name : null;
        return mFragmentListener.startSearch(mAccount, folderName);
   }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String accountUuid = mAccountUuids[id];
        Account account = mPreferences.getAccount(accountUuid);

        String threadId = getThreadId(mSearch);

        Uri uri;
        String[] projection;
        boolean needConditions;
        if (threadId != null) {
            uri = Uri.withAppendedPath(EmailProvider.CONTENT_URI, "account/" + accountUuid + "/thread/" + threadId);
            projection = PROJECTION;
            needConditions = false;
        } else if (mThreadedList) {
            uri = Uri.withAppendedPath(EmailProvider.CONTENT_URI, "account/" + accountUuid + "/messages/threaded");
            projection = THREADED_PROJECTION;
            needConditions = true;
        } else {
            uri = Uri.withAppendedPath(EmailProvider.CONTENT_URI, "account/" + accountUuid + "/messages");
            projection = PROJECTION;
            needConditions = true;
        }

        StringBuilder query = new StringBuilder();
        List<String> queryArgs = new ArrayList<>();
        if (needConditions) {
            boolean selectActive = mActiveMessage != null && mActiveMessage.getAccountUuid().equals(accountUuid);

            if (selectActive) {
                query.append("(" + MessageColumns.UID + " = ? AND " + SpecialColumns.FOLDER_NAME + " = ?) OR (");
                queryArgs.add(mActiveMessage.getUid());
                queryArgs.add(mActiveMessage.getFolderName());
            }

            SqlQueryBuilder.buildWhereClause(account, mSearch.getConditions(), query, queryArgs);

            if (selectActive) {
                query.append(')');
            }
        }

        String selection = query.toString();
        String[] selectionArgs = queryArgs.toArray(new String[0]);

        String sortOrder = buildSortOrder();

        return new CursorLoader(getActivity(), uri, projection, selection, selectionArgs,
                sortOrder);
    }

    private String getThreadId(LocalSearch search) {
        for (ConditionsTreeNode node : search.getLeafSet()) {
            SearchCondition condition = node.mCondition;
            if (condition.field == SearchField.THREAD_ID) {
                return condition.value;
            }
        }

        return null;
    }

    private String buildSortOrder() {
        String sortColumn;
        switch (mSortType) {
            case SORT_ARRIVAL: {
                sortColumn = MessageColumns.INTERNAL_DATE;
                break;
            }
            case SORT_ATTACHMENT: {
                sortColumn = "(" + MessageColumns.ATTACHMENT_COUNT + " < 1)";
                break;
            }
            case SORT_FLAGGED: {
                sortColumn = "(" + MessageColumns.FLAGGED + " != 1)";
                break;
            }
            case SORT_SENDER: {
                //FIXME
                sortColumn = MessageColumns.SENDER_LIST;
                break;
            }
            case SORT_SUBJECT: {
                sortColumn = MessageColumns.SUBJECT + " COLLATE NOCASE";
                break;
            }
            case SORT_UNREAD: {
                sortColumn = MessageColumns.READ;
                break;
            }
            case SORT_DATE:
            default: {
                sortColumn = MessageColumns.DATE;
            }
        }

        String sortDirection = (mSortAscending) ? " ASC" : " DESC";
        String secondarySort;
        if (mSortType == SortType.SORT_DATE || mSortType == SortType.SORT_ARRIVAL) {
            secondarySort = "";
        } else {
            secondarySort = MessageColumns.DATE + ((mSortDateAscending) ? " ASC, " : " DESC, ");
        }

        return sortColumn + sortDirection + ", " + secondarySort + MessageColumns.ID + " DESC";
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (mIsThreadDisplay && data.getCount() == 0) {
            mHandler.goBack();
            return;
        }

        mSwipeRefreshLayout.setRefreshing(false);
        mSwipeRefreshLayout.setEnabled(isPullToRefreshAllowed());

        final int loaderId = loader.getId();
        mCursors[loaderId] = data;
        mCursorValid[loaderId] = true;

        Cursor cursor;
        if (mCursors.length > 1) {
            cursor = new MergeCursorWithUniqueId(mCursors, getComparator());
            mUniqueIdColumn = cursor.getColumnIndex("_id");
        } else {
            cursor = data;
            mUniqueIdColumn = ID_COLUMN;
        }

        if (mIsThreadDisplay) {
            if (cursor.moveToFirst()) {
                mTitle = cursor.getString(SUBJECT_COLUMN);
                if (!TextUtils.isEmpty(mTitle)) {
                    mTitle = Utility.stripSubject(mTitle);
                }
                if (TextUtils.isEmpty(mTitle)) {
                    mTitle = getString(R.string.general_no_subject);
                }
                updateTitle();
            } else {
                //TODO: empty thread view -> return to full message list
            }
        }

        cleanupSelected(cursor);
        updateContextMenu(cursor);

        mAdapter.swapCursor(cursor);

        resetActionMode();
        computeBatchDirection();

        if (isLoadFinished()) {
            if (mSavedListState != null) {
                mHandler.restoreListPosition();
            }

            mFragmentListener.updateMenu();
        }
    }

    private void updateMoreMessagesOfCurrentFolder() {
        if (mFolderName != null) {
            try {
                LocalFolder folder = getFolder(mFolderName, mAccount);
                mCurrentFolder.setMoreMessagesFromFolder(folder);
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public boolean isLoadFinished() {
        if (mCursorValid == null) {
            return false;
        }

        for (boolean cursorValid : mCursorValid) {
            if (!cursorValid) {
                return false;
            }
        }

        return true;
    }

    /**
     * Close the context menu when the message it was opened for is no longer in the message list.
     */
    private void updateContextMenu(Cursor cursor) {
        if (mContextMenuUniqueId == 0) {
            return;
        }

        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            long uniqueId = cursor.getLong(mUniqueIdColumn);
            if (uniqueId == mContextMenuUniqueId) {
                return;
            }
        }

        mContextMenuUniqueId = 0;
        Activity activity = getActivity();
        if (activity != null) {
            activity.closeContextMenu();
        }
    }

    private void cleanupSelected(Cursor cursor) {
        if (mSelected.isEmpty()) {
            return;
        }

        Set<Long> selected = new HashSet<>();
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            long uniqueId = cursor.getLong(mUniqueIdColumn);
            if (mSelected.contains(uniqueId)) {
                selected.add(uniqueId);
            }
        }

        mSelected = selected;
    }

    /**
     * Starts or finishes the action mode when necessary.
     */
    private void resetActionMode() {
        if (mSelected.isEmpty()) {
            if (mActionMode != null) {
                mActionMode.finish();
            }
            return;
        }

        if (mActionMode == null) {
            startAndPrepareActionMode();
        }

        recalculateSelectionCount();
        updateActionModeTitle();
    }

    private void startAndPrepareActionMode() {
        mActionMode = getActivity().startActionMode(mActionModeCallback);
        mActionMode.invalidate();
    }

    /**
     * Recalculates the selection count.
     *
     * <p>
     * For non-threaded lists this is simply the number of visibly selected messages. If threaded
     * view is enabled this method counts the number of messages in the selected threads.
     * </p>
     */
    private void recalculateSelectionCount() {
        if (!mThreadedList) {
            mSelectedCount = mSelected.size();
            return;
        }

        mSelectedCount = 0;
        for (int i = 0, end = mAdapter.getCount(); i < end; i++) {
            Cursor cursor = (Cursor) mAdapter.getItem(i);
            long uniqueId = cursor.getLong(mUniqueIdColumn);

            if (mSelected.contains(uniqueId)) {
                int threadCount = cursor.getInt(THREAD_COUNT_COLUMN);
                mSelectedCount += (threadCount > 1) ? threadCount : 1;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mSelected.clear();
        mAdapter.swapCursor(null);
    }

    private Account getAccountFromCursor(Cursor cursor) {
        String accountUuid = cursor.getString(ACCOUNT_UUID_COLUMN);
        return mPreferences.getAccount(accountUuid);
    }

    private void remoteSearchFinished() {
        mRemoteSearchFuture = null;
    }

    /**
     * Mark a message as 'active'.
     *
     * <p>
     * The active message is the one currently displayed in the message view portion of the split
     * view.
     * </p>
     *
     * @param messageReference
     *         {@code null} to not mark any message as being 'active'.
     */
    public void setActiveMessage(MessageReference messageReference) {
        mActiveMessage = messageReference;

        // Reload message list with modified query that always includes the active message
        if (isAdded()) {
            restartLoader();
        }

        // Redraw list immediately
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    public boolean isSingleAccountMode() {
        return mSingleAccountMode;
    }

    public boolean isSingleFolderMode() {
        return mSingleFolderMode;
    }

    public boolean isInitialized() {
        return mInitialized;
    }

    public boolean isMarkAllAsReadSupported() {
        return (isSingleAccountMode() && isSingleFolderMode());
    }

    public void confirmMarkAllAsRead() {
        if (K9.confirmMarkAllRead()) {
            showDialog(R.id.dialog_confirm_mark_all_as_read);
        } else {
            markAllAsRead();
        }
    }

    public void markAllAsRead() {
        if (isMarkAllAsReadSupported()) {
            mController.markAllMessagesRead(mAccount, mFolderName);
        }
    }

    public boolean isCheckMailSupported() {
        return (mAllAccounts || !isSingleAccountMode() || !isSingleFolderMode() ||
                isRemoteFolder());
    }

    private boolean isCheckMailAllowed() {
        return (!isManualSearch() && isCheckMailSupported());
    }

    private boolean isPullToRefreshAllowed() {
        return (isRemoteSearchAllowed() || isCheckMailAllowed());
    }
}
