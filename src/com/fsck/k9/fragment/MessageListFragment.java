package com.fsck.k9.fragment;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
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
import com.fsck.k9.activity.ActivityListener;
import com.fsck.k9.activity.ChooseFolder;
import com.fsck.k9.activity.FolderInfoHolder;
import com.fsck.k9.activity.MessageInfoHolder;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.fragment.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;
import com.fsck.k9.helper.MessageHelper;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mail.store.LocalStore;
import com.fsck.k9.mail.store.LocalStore.LocalFolder;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;


public class MessageListFragment extends SherlockFragment implements OnItemClickListener,
        ConfirmationDialogFragmentListener {

    public static MessageListFragment newInstance(Account account, String folderName) {
        MessageListFragment fragment = new MessageListFragment();

        Bundle args = new Bundle();
        args.putString(ARG_ACCOUNT, account.getUuid());
        args.putString(ARG_FOLDER, folderName);
        fragment.setArguments(args);

        return fragment;
    }

    public static MessageListFragment newInstance(String title, String[] accountUuids,
            String[] folderNames, String queryString, Flag[] flags,
            Flag[] forbiddenFlags, boolean integrate) {

        MessageListFragment fragment = new MessageListFragment();

        Bundle args = new Bundle();
        args.putStringArray(ARG_ACCOUNT_UUIDS, accountUuids);
        args.putStringArray(ARG_FOLDER_NAMES, folderNames);
        args.putString(ARG_QUERY, queryString);
        if (flags != null) {
            args.putString(ARG_QUERY_FLAGS, Utility.combine(flags, ','));
        }
        if (forbiddenFlags != null) {
            args.putString(ARG_FORBIDDEN_FLAGS, Utility.combine(forbiddenFlags, ','));
        }
        args.putBoolean(ARG_INTEGRATE, integrate);
        args.putString(ARG_TITLE, title);
        fragment.setArguments(args);

        return fragment;
    }

    public static MessageListFragment newInstance(String searchAccount, String searchFolder,
            String queryString, boolean remoteSearch) {
        MessageListFragment fragment = new MessageListFragment();

        Bundle args = new Bundle();
        args.putString(ARG_SEARCH_ACCOUNT, searchAccount);
        args.putString(ARG_SEARCH_FOLDER, searchFolder);
        args.putString(ARG_QUERY, queryString);
        args.putBoolean(ARG_REMOTE_SEARCH, remoteSearch);
        fragment.setArguments(args);

        return fragment;
    }


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
            if (object1.compareCounterparty == null) {
                return (object2.compareCounterparty == null ? 0 : 1);
            } else if (object2.compareCounterparty == null) {
                return -1;
            } else {
                return object1.compareCounterparty.toLowerCase().compareTo(object2.compareCounterparty.toLowerCase());
            }
        }

    }

    public static class DateComparator implements Comparator<MessageInfoHolder> {

        @Override
        public int compare(MessageInfoHolder object1, MessageInfoHolder object2) {
            if (object1.compareDate == null) {
                return (object2.compareDate == null ? 0 : 1);
            } else if (object2.compareDate == null) {
                return -1;
            } else {
                return object1.compareDate.compareTo(object2.compareDate);
            }
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


    private static final int ACTIVITY_CHOOSE_FOLDER_MOVE = 1;
    private static final int ACTIVITY_CHOOSE_FOLDER_COPY = 2;

    private static final String ARG_ACCOUNT = "account";
    private static final String ARG_FOLDER  = "folder";
    private static final String ARG_REMOTE_SEARCH = "remote_search";
    private static final String ARG_QUERY = "query";
    private static final String ARG_SEARCH_ACCOUNT = "search_account";
    private static final String ARG_SEARCH_FOLDER = "search_folder";
    private static final String ARG_QUERY_FLAGS = "queryFlags";
    private static final String ARG_FORBIDDEN_FLAGS = "forbiddenFlags";
    private static final String ARG_INTEGRATE = "integrate";
    private static final String ARG_ACCOUNT_UUIDS = "accountUuids";
    private static final String ARG_FOLDER_NAMES = "folderNames";
    private static final String ARG_TITLE = "title";

    private static final String STATE_LIST_POSITION = "listPosition";

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
    private boolean mRemoteSearch = false;
    private String mSearchAccount = null;
    private String mSearchFolder = null;
    private Future mRemoteSearchFuture = null;
    private boolean mIntegrate = false;
    private String[] mAccountUuids = null;
    private String[] mFolderNames = null;
    private String mTitle;

    private MessageListHandler mHandler = new MessageListHandler();

    private SortType mSortType = SortType.SORT_DATE;
    private boolean mSortAscending = true;
    private boolean mSortDateAscending = false;
    private boolean mSenderAboveSubject = false;
    private boolean mCheckboxes = true;

    private int mSelectedCount = 0;

    private FontSizes mFontSizes = K9.getFontSizes();

    private ActionMode mActionMode;
    private Bundle mState = null;

    private Boolean mHasConnectivity;

    /**
     * Relevant messages for the current context when we have to remember the
     * chosen messages between user interactions (eg. Selecting a folder for
     * move operation)
     */
    private List<MessageInfoHolder> mActiveMessages;

    /* package visibility for faster inner class access */
    MessageHelper mMessageHelper;

    private ActionModeCallback mActionModeCallback = new ActionModeCallback();


    private MessageListFragmentListener mFragmentListener;


    private DateFormat mTimeFormat;


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

        public void updateFooter(final String message) {
            //TODO: use message
            post(new Runnable() {
                @Override
                public void run() {
                    MessageListFragment.this.updateFooter(message);
                }
            });
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
                    MessageListFragment.this.folderLoading(folder, loading);
                    break;
                }
                case ACTION_REFRESH_TITLE: {
                    MessageListFragment.this.refreshTitle();
                    break;
                }
                case ACTION_PROGRESS: {
                    boolean progress = (msg.arg1 == 1);
                    MessageListFragment.this.progress(progress);
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
        if (!mRemoteSearch) {
            setWindowProgress();
        }
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

        mFragmentListener.setMessageListProgress(level);
    }

    private void setWindowTitle() {
        // regular folder content display
        if (mFolderName != null) {
            Activity activity = getActivity();
            String displayName = FolderInfoHolder.getDisplayName(activity, mAccount,
                mFolderName);

            mFragmentListener.setMessageListTitle(displayName);

            String operation = mAdapter.mListener.getOperation(activity, getTimeFormat()).trim();
            if (operation.length() < 1) {
                mFragmentListener.setMessageListSubTitle(mAccount.getEmail());
            } else {
                mFragmentListener.setMessageListSubTitle(operation);
            }
        } else if (mQueryString != null) {
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
        if (mUnreadMessageCount == 0) {
            mFragmentListener.setUnreadCount(0);
        } else {
            if (mQueryString != null && mTitle == null) {
                // This is a search result.  The unread message count is easily confused
                // with total number of messages in the search result, so let's hide it.
                mFragmentListener.setUnreadCount(0);
            } else {
                mFragmentListener.setUnreadCount(mUnreadMessageCount);
            }
        }
    }

    private void setupFormats() {
        mTimeFormat = android.text.format.DateFormat.getTimeFormat(getActivity());
    }

    private DateFormat getTimeFormat() {
        return mTimeFormat;
    }

    private void progress(final boolean progress) {
        mFragmentListener.enableActionBarProgress(progress);
        if (mPullToRefreshView != null && !progress) {
            mPullToRefreshView.onRefreshComplete();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (view == mFooterView) {
            if (mCurrentFolder != null && !mRemoteSearch) {
                mController.loadMoreMessages(mAccount, mFolderName, mAdapter.mListener);
            } else if (mRemoteSearch && mAdapter.mExtraSearchResults != null && mAdapter.mExtraSearchResults.size() > 0 && mSearchAccount != null) {
                int numResults = mAdapter.mExtraSearchResults.size();
                Context appContext = getActivity().getApplicationContext();
                Account account = Preferences.getPreferences(appContext).getAccount(mSearchAccount);
                if (account == null) {
                    mHandler.updateFooter("");
                    return;
                }
                int limit = account.getRemoteSearchNumResults();
                List<Message> toProcess = mAdapter.mExtraSearchResults;
                if (limit > 0 && numResults > limit) {
                    toProcess = toProcess.subList(0, limit);
                    mAdapter.mExtraSearchResults = mAdapter.mExtraSearchResults.subList(limit, mAdapter.mExtraSearchResults.size());
                } else {
                    mAdapter.mExtraSearchResults = null;
                    mHandler.updateFooter("");
                }
                mController.loadSearchResults(account, mSearchFolder, toProcess, mAdapter.mListener);
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
    public void onAttach(Activity activity) {
        super.onAttach(activity);

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

        mController = MessagingController.getInstance(getActivity().getApplication());

        mPreviewLines = K9.messageListPreviewLines();
        mCheckboxes = K9.messageListCheckboxes();

        decodeArguments();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mInflater = inflater;

        View view = inflater.inflate(R.layout.message_list_fragment, container, false);

        mPullToRefreshView = (PullToRefreshListView) view.findViewById(R.id.message_list);

        initializeLayout();
        mListView.setVerticalFadingEdgeEnabled(false);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mMessageHelper = MessageHelper.getInstance(getActivity());

        initializeMessageList();
    }

    private void decodeArguments() {
        Bundle args = getArguments();

        mQueryString = args.getString(SearchManager.QUERY);
        mFolderName = args.getString(ARG_FOLDER);
        mRemoteSearch = args.getBoolean(ARG_REMOTE_SEARCH, false);
        mSearchAccount = args.getString(ARG_SEARCH_ACCOUNT);
        mSearchFolder = args.getString(ARG_SEARCH_FOLDER);

        String accountUuid = args.getString(ARG_ACCOUNT);

        Context appContext = getActivity().getApplicationContext();
        mAccount = Preferences.getPreferences(appContext).getAccount(accountUuid);

        String queryFlags = args.getString(ARG_QUERY_FLAGS);
        if (queryFlags != null) {
            String[] flagStrings = queryFlags.split(",");
            mQueryFlags = new Flag[flagStrings.length];
            for (int i = 0; i < flagStrings.length; i++) {
                mQueryFlags[i] = Flag.valueOf(flagStrings[i]);
            }
        }

        String forbiddenFlags = args.getString(ARG_FORBIDDEN_FLAGS);
        if (forbiddenFlags != null) {
            String[] flagStrings = forbiddenFlags.split(",");
            mForbiddenFlags = new Flag[flagStrings.length];
            for (int i = 0; i < flagStrings.length; i++) {
                mForbiddenFlags[i] = Flag.valueOf(flagStrings[i]);
            }
        }

        mIntegrate = args.getBoolean(ARG_INTEGRATE, false);
        mAccountUuids = args.getStringArray(ARG_ACCOUNT_UUIDS);
        mFolderNames = args.getStringArray(ARG_FOLDER_NAMES);
        mTitle = args.getString(ARG_TITLE);
    }

    private void initializeMessageList() {
        mAdapter = new MessageListAdapter();

        if (mFolderName != null) {
            mCurrentFolder = mAdapter.getFolder(mFolderName, mAccount);
        }

        // Hide "Load up to x more" footer for local search views
        mFooterView.setVisibility((mQueryString != null  && !mRemoteSearch) ? View.GONE : View.VISIBLE);

        mController = MessagingController.getInstance(getActivity().getApplication());
        mListView.setAdapter(mAdapter);
    }

    @Override
    public void onPause() {
        super.onPause();
        mController.removeListener(mAdapter.mListener);
        saveListState();
    }

    public void saveListState() {
        mState = new Bundle();
        mState.putInt(STATE_LIST_POSITION, mListView.getSelectedItemPosition());
    }

    public void restoreListState() {
        if (mState == null) {
            return;
        }

        int pos = mState.getInt(STATE_LIST_POSITION, ListView.INVALID_POSITION);

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

        setupFormats();

        Context appContext = getActivity().getApplicationContext();

        mSenderAboveSubject = K9.messageListSenderAboveSubject();

        final Preferences prefs = Preferences.getPreferences(appContext);

        boolean allowRemoteSearch = false;
        if (mSearchAccount != null) {
            final Account searchAccount = prefs.getAccount(mSearchAccount);
            if (searchAccount != null) {
                allowRemoteSearch = searchAccount.allowRemoteSearch();
            }
        }

        // Check if we have connectivity.  Cache the value.
        if (mHasConnectivity == null) {
            final ConnectivityManager connectivityManager =
                (ConnectivityManager) getActivity().getApplication().getSystemService(
                        Context.CONNECTIVITY_SERVICE);
            final NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
            if (netInfo != null && netInfo.getState() == NetworkInfo.State.CONNECTED) {
                mHasConnectivity = true;
            } else {
                mHasConnectivity = false;
            }
        }

        if (mQueryString == null) {
            mPullToRefreshView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>() {
                @Override
                public void onRefresh(PullToRefreshBase<ListView> refreshView) {
                    checkMail();
                }
            });
        } else if (allowRemoteSearch && !mRemoteSearch && !mIntegrate && mHasConnectivity) {
            // mQueryString != null is implied if we get this far.
            mPullToRefreshView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>() {
                @Override
                public void onRefresh(PullToRefreshBase<ListView> refreshView) {
                    mPullToRefreshView.onRefreshComplete();
                    onRemoteSearchRequested(true);
                }
            });
            mPullToRefreshView.setPullLabel(getString(R.string.pull_to_refresh_remote_search_from_local_search_pull));
            mPullToRefreshView.setReleaseLabel(getString(R.string.pull_to_refresh_remote_search_from_local_search_release));
        } else {
            mPullToRefreshView.setMode(PullToRefreshBase.Mode.DISABLED);
        }

        mController.addListener(mAdapter.mListener);

        //Cancel pending new mail notifications when we open an account
        Account[] accountsWithNotification;

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
            mController.notifyAccountCancel(appContext, accountWithNotification);
        }

        if (mAdapter.isEmpty()) {
            if (mRemoteSearch) {
                //TODO: Support flag based search
                mRemoteSearchFuture = mController.searchRemoteMessages(mSearchAccount, mSearchFolder, mQueryString, null, null, mAdapter.mListener);
            } else if (mFolderName != null) {
                mController.listLocalMessages(mAccount, mFolderName,  mAdapter.mListener);
                // Hide the archive button if we don't have an archive folder.
                if (!mAccount.hasArchiveFolder()) {
//                    mBatchArchiveButton.setVisibility(View.GONE);
                }
            } else if (mQueryString != null) {
                if (mSearchAccount != null) {
                    mController.searchLocalMessages(new String[] {mSearchAccount}, new String[] {mSearchFolder}, null, mQueryString, mIntegrate, mQueryFlags, mForbiddenFlags, mAdapter.mListener);
                } else {
                    mController.searchLocalMessages(mAccountUuids, mFolderNames, null, mQueryString, mIntegrate, mQueryFlags, mForbiddenFlags, mAdapter.mListener);
                }
            }

        } else {
            // reread the selected date format preference in case it has changed
            mMessageHelper.refresh();

            mAdapter.markAllMessagesAsDirty();

            if (!mRemoteSearch) {
                new Thread() {
                    @Override
                    public void run() {
                        if (mFolderName != null) {
                            mController.listLocalMessagesSynchronous(mAccount, mFolderName,  mAdapter.mListener);
                        } else if (mQueryString != null) {
                            mController.searchLocalMessagesSynchronous(mAccountUuids, mFolderNames, null, mQueryString, mIntegrate, mQueryFlags, mForbiddenFlags, mAdapter.mListener);
                        }

                        mHandler.post(new Runnable() {
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
        }

        if (mAccount != null && mFolderName != null && !mRemoteSearch) {
            mController.getFolderUnreadMessageCount(mAccount, mFolderName, mAdapter.mListener);
        }

        refreshTitle();
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

    private void onOpenMessage(MessageInfoHolder message) {
        mFragmentListener.openMessage(message.message.makeMessageReference());
    }

    public void onCompose() {
        if (mQueryString != null) {
            /*
             * If we have a query string, we don't have an account to let
             * compose start the default action.
             */
            mFragmentListener.onCompose(null);
        } else {
            mFragmentListener.onCompose(mAccount);
        }
    }

    public void onReply(MessageInfoHolder holder) {
        mFragmentListener.onReply(holder.message);
    }

    public void onReplyAll(MessageInfoHolder holder) {
        mFragmentListener.onReplyAll(holder.message);
    }

    public void onForward(MessageInfoHolder holder) {
        mFragmentListener.onForward(holder.message);
    }

    public void onResendMessage(MessageInfoHolder holder) {
        mFragmentListener.onResendMessage(holder.message);
    }

    public void changeSort(SortType sortType) {
        Boolean sortAscending = (mSortType == sortType) ? !mSortAscending : null;
        changeSort(sortType, sortAscending);
    }

    /**
     * User has requested a remote search.  Setup the bundle and start the intent.
     * @param fromLocalSearch true if this is being called from a local search result screen.  This affects
     *                        where we pull the account and folder info used for the next search.
     */
    public void onRemoteSearchRequested(final boolean fromLocalSearch) {
        String searchAccount;
        String searchFolder;

        if (fromLocalSearch) {
            searchAccount = mSearchAccount;
            searchFolder = mSearchFolder;
        } else {
            searchAccount = mAccount.getUuid();
            searchFolder = mCurrentFolder.name;
        }

        mFragmentListener.remoteSearch(searchAccount, searchFolder, mQueryString);
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

        Preferences prefs = Preferences.getPreferences(getActivity().getApplicationContext());
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

        Toast toast = Toast.makeText(getActivity(), toastString, Toast.LENGTH_SHORT);
        toast.show();

        mAdapter.sortMessages();
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

    public void onExpunge() {
        if (mCurrentFolder != null) {
            onExpunge(mAccount, mCurrentFolder.name);
        }
    }

    private void onExpunge(final Account account, String folderName) {
        mController.expunge(account, folderName, null);
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
            default: {
                throw new RuntimeException("Called showDialog(int) with unknown dialog id.");
            }
        }

        fragment.setTargetFragment(this, dialogId);
        fragment.show(getFragmentManager(), getDialogTag(dialogId));
    }

    private String getDialogTag(int dialogId) {
        return String.format("dialog-%d", dialogId);
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
            setSelectionState(true);
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
        mController.sendPendingMessages(mAccount, mAdapter.mListener);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        final MessageInfoHolder message = (MessageInfoHolder) mListView.getItemAtPosition(info.position);


        final List<MessageInfoHolder> selection = getSelectionFromMessage(message);
            switch (item.getItemId()) {
                case R.id.reply: {
                    onReply(message);
                    break;
                }
                case R.id.reply_all: {
                    onReplyAll(message);
                    break;
                }
                case R.id.forward: {
                    onForward(message);
                    break;
                }
                case R.id.send_again: {
                    onResendMessage(message);
                    mSelectedCount = 0;
                    break;
                }
                case R.id.same_sender: {
                    mFragmentListener.showMoreFromSameSender(message.senderAddress);
                    break;
                }
                case R.id.delete: {
                    onDelete(selection);
                    break;
                }
                case R.id.mark_as_read: {
                    setFlag(selection, Flag.SEEN, true);
                    break;
                }
                case R.id.mark_as_unread: {
                    setFlag(selection, Flag.SEEN, false);
                    break;
                }
                case R.id.flag: {
                    setFlag(selection, Flag.FLAGGED, true);
                    break;
                }
                case R.id.unflag: {
                    setFlag(selection, Flag.FLAGGED, false);
                    break;
                }

                // only if the account supports this
                case R.id.archive: {
                    onArchive(selection);
                    break;
                }
                case R.id.spam: {
                    onSpam(selection);
                    break;
                }
                case R.id.move: {
                    onMove(selection);
                    break;
                }
                case R.id.copy: {
                    onCopy(selection);
                    break;
                }
            }

            return true;
        }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        MessageInfoHolder message = (MessageInfoHolder) mListView.getItemAtPosition(info.position);

        if (message == null) {
            return;
        }

        getActivity().getMenuInflater().inflate(R.menu.message_list_item_context, menu);

        menu.setHeaderTitle(message.message.getSubject());

        if (message.read) {
            menu.findItem(R.id.mark_as_read).setVisible(false);
        } else {
            menu.findItem(R.id.mark_as_unread).setVisible(false);
        }

        if (message.flagged) {
            menu.findItem(R.id.flag).setVisible(false);
        } else {
            menu.findItem(R.id.unflag).setVisible(false);
        }

        Account account = message.message.getFolder().getAccount();
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
     * Handle a select or unselect swipe event
     * @param downMotion Event that started the swipe
     * @param selected true if this was an attempt to select (i.e. left to right).
     */
    private void handleSwipe(final MotionEvent downMotion, final boolean selected) {
        int[] listPosition = new int[2];
        mListView.getLocationOnScreen(listPosition);
        int position = mListView.pointToPosition((int) downMotion.getRawX() - listPosition[0], (int) downMotion.getRawY() - listPosition[1]);
        if (position != AdapterView.INVALID_POSITION) {
            final MessageInfoHolder message = (MessageInfoHolder) mListView.getItemAtPosition(position);
            toggleMessageSelect(message);
        }
    }

    class MessageListAdapter extends BaseAdapter {
        private final List<MessageInfoHolder> mMessages =
                Collections.synchronizedList(new ArrayList<MessageInfoHolder>());

        public List<Message> mExtraSearchResults;

        private final ActivityListener mListener = new ActivityListener() {

            @Override
            public void remoteSearchAddMessage(Account account, String folderName, Message message, final int numDone, final int numTotal) {

                if (numTotal > 0 && numDone < numTotal) {
                    mFragmentListener.setMessageListProgress(Window.PROGRESS_END / numTotal * numDone);
                } else {
                    mFragmentListener.setMessageListProgress(Window.PROGRESS_END);
                }

                mHandler.addOrUpdateMessages(account, folderName, Collections.singletonList(message), false);
            }

            @Override
            public void remoteSearchFailed(Account acct, String folder, final String err) {
                //TODO: Better error handling
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(), err, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void remoteSearchStarted(Account acct, String folder) {
                mHandler.progress(true);
                mHandler.updateFooter(getString(R.string.remote_search_sending_query));
            }

            @Override
            public void enableProgressIndicator(boolean enable) {
                mHandler.progress(enable);
            }


            @Override
            public void remoteSearchFinished(Account acct, String folder, int numResults, List<Message> extraResults) {
                mHandler.progress(false);
                if (extraResults != null && extraResults.size() > 0) {
                    mExtraSearchResults = extraResults;
                    mHandler.updateFooter(String.format(getString(R.string.load_more_messages_fmt), acct.getRemoteSearchNumResults()));
                } else {
                    mHandler.updateFooter("");
                }
                mFragmentListener.setMessageListProgress(Window.PROGRESS_END);

            }

            @Override
            public void remoteSearchServerQueryComplete(Account account, String folderName, int numResults) {
                mHandler.progress(true);
                if (account != null &&  account.getRemoteSearchNumResults() != 0 && numResults > account.getRemoteSearchNumResults()) {
                    mHandler.updateFooter(getString(R.string.remote_search_downloading_limited, account.getRemoteSearchNumResults(), numResults));
                } else {
                    mHandler.updateFooter(getString(R.string.remote_search_downloading, numResults));
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
            computeSelectAllVisibility();
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
            computeSelectAllVisibility();
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
                                    getActivity(), messageFolder, messageAccount);
                            messageHelper.populate(m, message, folderInfoHolder, messageAccount);
                            messagesToAdd.add(m);
                        } else {
                            if (mQueryString != null) {
                                if (verifyAgainstSearch) {
                                    messagesToSearch.add(message);
                                } else {
                                    m = new MessageInfoHolder();
                                    FolderInfoHolder folderInfoHolder = new FolderInfoHolder(
                                            getActivity(), messageFolder, messageAccount);
                                    messageHelper.populate(m, message, folderInfoHolder,
                                            messageAccount);
                                    messagesToAdd.add(m);
                                }
                            }
                        }
                    } else {
                        m.dirty = false; // as we reload the message, unset its dirty flag
                        FolderInfoHolder folderInfoHolder = new FolderInfoHolder(getActivity(),
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
                if (uid != null && (holder.uid == uid || uid.equals(holder.uid))) {
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
                return new FolderInfoHolder(getActivity(), local_folder, account);
            } catch (Exception e) {
                Log.e(K9.LOG_TAG, "getFolder(" + folder + ") goes boom: ", e);
                return null;
            } finally {
                if (local_folder != null) {
                    local_folder.close();
                }
            }
        }


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
                holder.date = (TextView) view.findViewById(R.id.date);
                holder.chip = view.findViewById(R.id.chip);
                holder.selected = (CheckBox) view.findViewById(R.id.selected_checkbox);
                holder.preview = (TextView) view.findViewById(R.id.preview);
                if (mCheckboxes) {
                    holder.selected.setVisibility(View.VISIBLE);
                }

                if (holder.selected != null) {
                    holder.selected.setOnCheckedChangeListener(holder);
                }


                if (mSenderAboveSubject) {
                    holder.from = (TextView) view.findViewById(R.id.subject);
                    holder.from.setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSizes.getMessageListSender());
                } else {
                    holder.subject = (TextView) view.findViewById(R.id.subject);
                    holder.subject.setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSizes.getMessageListSubject());
                }

                holder.date.setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSizes.getMessageListDate());

                holder.preview.setLines(mPreviewLines);
                holder.preview.setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSizes.getMessageListPreview());

                view.setTag(holder);
            }

            if (message != null) {
                bindView(position, view, holder, message);
            } else {
                // This branch code is triggered when the local store
                // hands us an invalid message

                holder.chip.getBackground().setAlpha(0);
                if (holder.subject != null) {
                    holder.subject.setText(getString(R.string.general_no_subject));
                    holder.subject.setTypeface(null, Typeface.NORMAL);
                }

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

            int maybeBoldTypeface = message.read ? Typeface.NORMAL : Typeface.BOLD;

            // So that the mSelectedCount is only incremented/decremented
            // when a user checks the checkbox (vs code)
            holder.position = -1;

            holder.selected.setChecked(message.selected);

            if (!mCheckboxes && message.selected) {

                holder.chip.setBackgroundDrawable(message.message.getFolder().getAccount().getCheckmarkChip().drawable());
            }

            else {
                holder.chip.setBackgroundDrawable(message.message.getFolder().getAccount().generateColorChip(message.read,message.message.toMe(), message.message.ccMe(), message.message.fromMe(), message.flagged).drawable());

            }

            if (K9.useBackgroundAsUnreadIndicator()) {
                int res = (message.read) ? R.attr.messageListReadItemBackgroundColor :
                        R.attr.messageListUnreadItemBackgroundColor;

                TypedValue outValue = new TypedValue();
                getActivity().getTheme().resolveAttribute(res, outValue, true);
                view.setBackgroundColor(outValue.data);
            }

            String subject = null;

            if ((message.message.getSubject() == null) || message.message.getSubject().equals("")) {
                subject = (String) getText(R.string.general_no_subject);

            } else {
                subject = message.message.getSubject();
            }

            // We'll get badge support soon --jrv
//            if (holder.badge != null) {
//                String email = message.counterpartyAddress;
//                holder.badge.assignContactFromEmail(email, true);
//                if (email != null) {
//                    mContactsPictureLoader.loadContactPicture(email, holder.badge);
//                }
//            }

            if (holder.preview != null) {
                /*
                 * In the touchable UI, we have previews. Otherwise, we
                 * have just a "from" line.
                 * Because text views can't wrap around each other(?) we
                 * compose a custom view containing the preview and the
                 * from.
                 */

                CharSequence beforePreviewText = null;
                if (mSenderAboveSubject) {
                    beforePreviewText = subject;
                } else {
                    beforePreviewText = message.sender;
                }

                holder.preview.setText(new SpannableStringBuilder(recipientSigil(message))
                                       .append(beforePreviewText).append(" ").append(message.message.getPreview()),
                                       TextView.BufferType.SPANNABLE);
                Spannable str = (Spannable)holder.preview.getText();

                // Create a span section for the sender, and assign the correct font size and weight.
                str.setSpan(new AbsoluteSizeSpan((mSenderAboveSubject ? mFontSizes.getMessageListSubject(): mFontSizes.getMessageListSender()), true),
                            0, beforePreviewText.length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                int color = (K9.getK9Theme() == K9.THEME_LIGHT) ?
                        Color.rgb(105, 105, 105) :
                        Color.rgb(160, 160, 160);

                // set span for preview message.
                str.setSpan(new ForegroundColorSpan(color), // How do I can specify the android.R.attr.textColorTertiary
                            beforePreviewText.length() + 1,
                            str.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }


            if (holder.from != null ) {
                holder.from.setTypeface(null, maybeBoldTypeface);
                if (mSenderAboveSubject) {
                    holder.from.setCompoundDrawablesWithIntrinsicBounds(
                            message.answered ? mAnsweredIcon : null, // left
                            null, // top
                            message.message.hasAttachments() ? mAttachmentIcon : null, // right
                            null); // bottom

                    holder.from.setText(message.sender);
                } else {
                    holder.from.setText(new SpannableStringBuilder(recipientSigil(message)).append(message.sender));
                }
            }

            if (holder.subject != null ) {
                if (!mSenderAboveSubject) {
                    holder.subject.setCompoundDrawablesWithIntrinsicBounds(
                            message.answered ? mAnsweredIcon : null, // left
                            null, // top
                            message.message.hasAttachments() ? mAttachmentIcon : null, // right
                            null); // bottom
                }

                holder.subject.setTypeface(null, maybeBoldTypeface);
                holder.subject.setText(subject);
            }

            holder.date.setText(message.getDate(mMessageHelper));
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
        public View chip;
        public CheckBox selected;
        public int position = -1;

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (position != -1) {
                MessageInfoHolder message = (MessageInfoHolder) mAdapter.getItem(position);
                    toggleMessageSelect(message);


            }
        }
    }


    private View getFooterView(ViewGroup parent) {
        if (mFooterView == null) {
            mFooterView = mInflater.inflate(R.layout.message_list_item_footer, parent, false);
            mFooterView.setId(R.layout.message_list_item_footer);
            FooterViewHolder holder = new FooterViewHolder();
            holder.main = (TextView) mFooterView.findViewById(R.id.main_text);
            mFooterView.setTag(holder);
        }

        return mFooterView;
    }

    private void updateFooterView() {
        if (mCurrentFolder != null && mAccount != null) {
            if (mCurrentFolder.loading) {
                updateFooter(getString(R.string.status_loading_more));
            } else {
                String message;
                if (!mCurrentFolder.lastCheckFailed) {
                    if (mAccount.getDisplayCount() == 0) {
                        message = getString(R.string.message_list_load_more_messages_action);
                    } else {
                        message = String.format(getString(R.string.load_more_messages_fmt), mAccount.getDisplayCount());
                    }
                } else {
                    message = getString(R.string.status_loading_more_failed);
                }
                updateFooter(message);
            }
        } else {
            updateFooter(null);
        }
    }

    public void updateFooter(final String text) {
        FooterViewHolder holder = (FooterViewHolder) mFooterView.getTag();

        if (text != null) {
            holder.main.setText(text);
        }
        if (holder.main.getText().length() > 0) {
            holder.main.setVisibility(View.VISIBLE);
        } else {
            holder.main.setVisibility(View.GONE);
        }
    }

    static class FooterViewHolder {
        public TextView main;
    }

    private void setAllSelected(boolean isSelected) {
        mSelectedCount = 0;

        for (MessageInfoHolder holder : mAdapter.getMessages()) {
            holder.selected = isSelected;
            mSelectedCount += (isSelected ? 1 : 0);
        }

        computeBatchDirection();
        mAdapter.notifyDataSetChanged();

        if (isSelected) {
            updateActionModeTitle();
            computeSelectAllVisibility();
        }
    }

    /**
     * Set selection state for all messages.
     *
     * @param selected
     *         If {@code true} all messages get selected. Otherwise, all messages get deselected and
     *         action mode is finished.
     */
    private void setSelectionState(boolean selected) {
        mAdapter.setSelectionForAllMesages(selected);

        if (selected) {
            mSelectedCount = mAdapter.getCount();
            mActionMode = getSherlockActivity().startActionMode(mActionModeCallback);
            updateActionModeTitle();
            computeSelectAllVisibility();
            computeBatchDirection();
        } else {
            mSelectedCount = 0;
            if (mActionMode != null) {
                mActionMode.finish();
            }
        }
    }

    private void toggleMessageSelect(final MessageInfoHolder holder){
        if (mActionMode != null) {
            if (mSelectedCount == 1 && holder.selected) {
                mActionMode.finish();
                return;
            }
        } else {
            mActionMode = getSherlockActivity().startActionMode(mActionModeCallback);
        }

        if (holder.selected) {
            holder.selected = false;
            mSelectedCount -= 1;
        } else {
            holder.selected = true;
            mSelectedCount += 1;
        }
        mAdapter.notifyDataSetChanged();

        computeBatchDirection();
        updateActionModeTitle();

        // make sure the onPrepareActionMode is called
        mActionMode.invalidate();

        computeSelectAllVisibility();
    }

    private void updateActionModeTitle() {
        mActionMode.setTitle(String.format(getString(R.string.actionbar_selected), mSelectedCount));
    }

    private void computeSelectAllVisibility() {
        mActionModeCallback.showSelectAll(mSelectedCount != mAdapter.getCount());
    }

    private void computeBatchDirection() {
        boolean isBatchFlag = false;
        boolean isBatchRead = false;

        for (MessageInfoHolder holder : mAdapter.getMessages()) {
            if (holder.selected) {
                if (!holder.flagged) {
                    isBatchFlag = true;
                }
                if (!holder.read) {
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

        computeBatchDirection();
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
        final Intent intent = new Intent(getActivity(), ChooseFolder.class);
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
                final Toast toast = Toast.makeText(getActivity(), R.string.move_copy_cannot_copy_unsynced_message,
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
                final Toast toast = Toast.makeText(getActivity(), R.string.move_copy_cannot_copy_unsynced_message,
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
            mSelectAll = null;
            mMarkAsRead = null;
            mMarkAsUnread = null;
            mFlag = null;
            mUnflag = null;
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
            case R.id.mark_as_read: {
                setFlag(selection, Flag.SEEN, true);
                break;
            }
            case R.id.mark_as_unread: {
                setFlag(selection, Flag.SEEN, false);
                break;
            }
            case R.id.flag: {
                setFlag(selection, Flag.FLAGGED, true);
                break;
            }
            case R.id.unflag: {
                setFlag(selection, Flag.FLAGGED, false);
                break;
            }
            case R.id.select_all: {
                setAllSelected(true);
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
        }
    }

    @Override
    public void doNegativeClick(int dialogId) {
        switch (dialogId) {
            case R.id.dialog_confirm_spam: {
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
        mController.synchronizeMailbox(mAccount, mFolderName, mAdapter.mListener, null);
        mController.sendPendingMessages(mAccount, mAdapter.mListener);
    }

    /**
     * We need to do some special clean up when leaving a remote search result screen.  If no remote search is
     * in progress, this method does nothing special.
     */
    @Override
    public void onStop() {
        // If we represent a remote search, then kill that before going back.
        if (mSearchAccount != null && mSearchFolder != null && mRemoteSearchFuture != null) {
            try {
                Log.i(K9.LOG_TAG, "Remote search in progress, attempting to abort...");
                // Canceling the future stops any message fetches in progress.
                final boolean cancelSuccess = mRemoteSearchFuture.cancel(true);   // mayInterruptIfRunning = true
                if (!cancelSuccess) {
                    Log.e(K9.LOG_TAG, "Could not cancel remote search future.");
                }
                // Closing the folder will kill off the connection if we're mid-search.
                Context appContext = getActivity().getApplicationContext();
                final Account searchAccount = Preferences.getPreferences(appContext).getAccount(mSearchAccount);
                final Store remoteStore = searchAccount.getRemoteStore();
                final Folder remoteFolder = remoteStore.getFolder(mSearchFolder);
                remoteFolder.close();
                // Send a remoteSearchFinished() message for good measure.
                mAdapter.mListener.remoteSearchFinished(searchAccount, mSearchFolder, 0, null);
            } catch (Exception e) {
                // Since the user is going back, log and squash any exceptions.
                Log.e(K9.LOG_TAG, "Could not abort remote search before going back", e);
            }
        }
        super.onStop();
    }

    public ArrayList<MessageReference> getMessageReferences() {
        ArrayList<MessageReference> messageRefs = new ArrayList<MessageReference>();

        for (MessageInfoHolder holder : mAdapter.getMessages()) {
            MessageReference ref = holder.message.makeMessageReference();
            messageRefs.add(ref);
        }

        return messageRefs;
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

    public interface MessageListFragmentListener {
        void enableActionBarProgress(boolean enable);
        void setMessageListProgress(int level);
        void remoteSearch(String searchAccount, String searchFolder, String queryString);
        void showMoreFromSameSender(String senderAddress);
        void onResendMessage(Message message);
        void onForward(Message message);
        void onReply(Message message);
        void onReplyAll(Message message);
        void openMessage(MessageReference messageReference);
        void setMessageListTitle(String title);
        void setMessageListSubTitle(String subTitle);
        void setUnreadCount(int unread);
        void onCompose(Account account);
        boolean startSearch(Account account, String folderName);
    }

    public void onReverseSort() {
        changeSort(mSortType);
    }

    private MessageInfoHolder getSelection() {
        return (MessageInfoHolder) mListView.getSelectedItem();
    }

    public void onDelete() {
        MessageInfoHolder message = getSelection();
        if (message != null) {
            onDelete(Collections.singletonList(message));
        }
    }

    public void toggleMessageSelect() {
        MessageInfoHolder message = getSelection();
        if (message != null) {
            toggleMessageSelect(message);
        }
    }

    public void onToggleFlag() {
        MessageInfoHolder message = getSelection();
        if (message != null) {
            setFlag(Collections.singletonList(message), Flag.FLAGGED, !message.flagged);
        }
    }

    public void onMove() {
        MessageInfoHolder message = getSelection();
        if (message != null) {
            onMove(Collections.singletonList(message));
        }
    }

    public void onArchive() {
        MessageInfoHolder message = getSelection();
        if (message != null) {
            onArchive(Collections.singletonList(message));
        }
    }

    public void onCopy() {
        MessageInfoHolder message = getSelection();
        if (message != null) {
            onCopy(Collections.singletonList(message));
        }
    }

    public void onToggleRead() {
        MessageInfoHolder message = getSelection();
        if (message != null) {
            setFlag(Collections.singletonList(message), Flag.SEEN, !message.read);
        }
    }

    public boolean isSearchQuery() {
        return (mQueryString != null || mIntegrate);
    }

    public boolean isOutbox() {
        return (mFolderName != null && mFolderName.equals(mAccount.getOutboxFolderName()));
    }

    public boolean isErrorFolder() {
        return K9.ERROR_FOLDER_NAME.equals(mFolderName);
    }

    public boolean isRemoteFolder() {
        if (isSearchQuery() || isOutbox() || isErrorFolder()) {
            return false;
        }

        if (!mController.isMoveCapable(mAccount)) {
            // For POP3 accounts only the Inbox is a remote folder.
            return (mFolderName != null && !mFolderName.equals(mAccount.getInboxFolderName()));
        }

        return true;
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
            onRemoteSearchRequested(true);
        } else {
            Toast.makeText(getActivity(), getText(R.string.remote_search_unavailable_no_network),
                    Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isRemoteSearch() {
        return mRemoteSearch;
    }

    public boolean isRemoteSearchAllowed() {
        if (!isSearchQuery() || mRemoteSearch || mSearchFolder == null || mSearchAccount == null) {
            return false;
        }

        Context appContext = getActivity().getApplicationContext();
        final Preferences prefs = Preferences.getPreferences(appContext);

        boolean allowRemoteSearch = false;
        final Account searchAccount = prefs.getAccount(mSearchAccount);
        if (searchAccount != null) {
            allowRemoteSearch = searchAccount.allowRemoteSearch();
        }

        return allowRemoteSearch;
    }

    public boolean onSearchRequested() {
        String folderName = (mCurrentFolder != null) ? mCurrentFolder.name : null;
        return mFragmentListener.startSearch(mAccount, folderName);
   }
}
