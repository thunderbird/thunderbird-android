package com.fsck.k9.activity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.TextAppearanceSpan;
import android.util.Config;
import android.util.Log;
import android.util.TypedValue;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnClickListener;
import android.view.KeyEvent;
import android.widget.*;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.fsck.k9.Account;
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
import com.fsck.k9.controller.MessagingController.SORT_TYPE;
import com.fsck.k9.crypto.PgpData;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.helper.MessageHelper;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.*;
import com.fsck.k9.mail.store.LocalStore;
import com.fsck.k9.mail.store.StorageManager;
import com.fsck.k9.mail.store.LocalStore.LocalFolder;

import com.fsck.k9.view.SingleMessageView;
import com.fsck.k9.view.AttachmentView;
import com.fsck.k9.view.ToggleScrollView;
import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.AbstractAction;
import net.londatiga.android.QuickAction;
import net.londatiga.android.ActionItem;
/**
 * MessageList is the primary user interface for the program. This Activity
 * shows a list of messages.
 * From this Activity the user can perform all standard message operations.
 */
public class MessageList
    extends K9Activity
    implements OnClickListener, AdapterView.OnItemClickListener, AnimationListener {

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
    private static final int ACTIVITY_CHOOSE_FOLDER_MOVE_BATCH = 3;
    private static final int ACTIVITY_CHOOSE_FOLDER_COPY_BATCH = 4;

    private static final String SHOW_PICTURES = "showPictures";
    private static final String STATE_PGP_DATA = "pgpData";
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
    private static final String EXTRA_NEXT = "com.fsck.k9.MessageView_next";


    private MessageInfoHolder mNextMessage;
    private MessageInfoHolder mPreviousMessage;
    private MessageInfoHolder mCurrentMessageInfo;
    private ToggleScrollView mToggleScrollView;
    private SingleMessageView mMessageView;
    private Message mMessage;
    private PgpData mPgpData = null;

    private View mNext;
    private View mPrevious;
    private View mDelete;
    private View mArchive;
    private View mMove;
    private View mSpam;
    private View mReply;
    private View mReplyAll;
    private View mForward;
    private Contacts mContacts;
    private ActionBar mActionBar;

    private static final int PREVIOUS = 1;
    private static final int NEXT = 2;
    private int mLastDirection = PREVIOUS;

    /**
     * Maps a {@link SORT_TYPE} to a {@link Comparator} implementation.
     */
    private static final Map<SORT_TYPE, Comparator<MessageInfoHolder>> SORT_COMPARATORS;

    static {
        // fill the mapping at class time loading

        final Map<SORT_TYPE, Comparator<MessageInfoHolder>> map = new EnumMap<SORT_TYPE, Comparator<MessageInfoHolder>>(SORT_TYPE.class);
        map.put(SORT_TYPE.SORT_ATTACHMENT, new AttachmentComparator());
        map.put(SORT_TYPE.SORT_DATE, new DateComparator());
        map.put(SORT_TYPE.SORT_FLAGGED, new FlaggedComparator());
        map.put(SORT_TYPE.SORT_SENDER, new SenderComparator());
        map.put(SORT_TYPE.SORT_SUBJECT, new SubjectComparator());
        map.put(SORT_TYPE.SORT_UNREAD, new UnreadComparator());

        // make it immutable to prevent accidental alteration (content is immutable already)
        SORT_COMPARATORS = Collections.unmodifiableMap(map);
    }

    private ListView mListView;



    private boolean mTouchView = true;
    private int mPreviewLines = 0;


    private MessageListAdapter mAdapter;
    private View mFooterView;

    private FolderInfoHolder mCurrentFolder;

    private LayoutInflater mInflater;

    private MessagingController mController;

    private Account mAccount;
    private int mUnreadMessageCount = 0;

    private GestureDetector gestureDetector;
    private View.OnTouchListener gestureListener;
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

    private SORT_TYPE sortType = SORT_TYPE.SORT_DATE;

    private boolean sortAscending = true;
    private boolean sortDateAscending = false;

    private boolean mStars = true;
    private boolean mCheckboxes = true;
    private int mSelectedCount = 0;

    private View mBatchButtonArea;
    private ImageButton mBatchReadButton;
    private ImageButton mBatchDeleteButton;
    private ImageButton mBatchFlagButton;
    private ImageButton mBatchDoneButton;

    private FontSizes mFontSizes = K9.getFontSizes();

    private Bundle mState = null;
    private MessageInfoHolder mSelectedMessage = null;

    private Context context = null;

    /* package visibility for faster inner class access */
    MessageHelper mMessageHelper = MessageHelper.getInstance(this);

    private StorageManager.StorageListener mStorageListener = new StorageListenerImplementation();

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

    class MessageListHandler extends Handler {
        public void removeMessage(final List<MessageInfoHolder> messages) {
            runOnUiThread(new Runnable() {
                public void run() {
                    for (MessageInfoHolder message : messages) {
                        if (message != null) {
                            if (mFolderName == null || (message.folder != null && message.folder.name.equals(mFolderName))) {
                                if (message.selected && mSelectedCount > 0) {
                                    mSelectedCount--;
                                }
                                mAdapter.messages.remove(message);
                            }
                        }
                    }
                    resetUnreadCountOnThread();

                    mAdapter.notifyDataSetChanged();
                    toggleBatchButtons();
                }
            });
        }

        public void addMessages(final List<MessageInfoHolder> messages) {
            final boolean wasEmpty = mAdapter.messages.isEmpty();
            runOnUiThread(new Runnable() {
                public void run() {
                    for (final MessageInfoHolder message : messages) {
                        if (mFolderName == null || (message.folder != null && message.folder.name.equals(mFolderName))) {
                            int index;
                            synchronized (mAdapter.messages) {
                                index = Collections.binarySearch(mAdapter.messages, message, getComparator());
                            }

                            if (index < 0) {
                                index = (index * -1) - 1;
                            }

                            mAdapter.messages.add(index, message);
                        }
                    }

                    if (wasEmpty) {
                        mListView.setSelection(0);
                        mListView.setItemChecked(0, true);
                    }
                    resetUnreadCountOnThread();

                    mAdapter.notifyDataSetChanged();
                }
            });
        }

        private void resetUnreadCount() {
            runOnUiThread(new Runnable() {
                public void run() {
                    resetUnreadCountOnThread();
                }
            });
        }

        private void resetUnreadCountOnThread() {
            if (mQueryString != null) {
                int unreadCount = 0;
                synchronized (mAdapter.messages) {
                    for (MessageInfoHolder holder : mAdapter.messages) {
                        unreadCount += holder.read ? 0 : 1;
                    }
                }
                mUnreadMessageCount = unreadCount;
                refreshTitleOnThread();
            }
        }

        private void sortMessages() {
            final Comparator<MessageInfoHolder> chainComparator = getComparator();

            runOnUiThread(new Runnable() {
                public void run() {
                    synchronized (mAdapter.messages) {
                        Collections.sort(mAdapter.messages, chainComparator);
                    }
                    mAdapter.notifyDataSetChanged();
                }
            });
        }

        /**
         * @return The comparator to use to display messages in an ordered
         *         fashion. Never <code>null</code>.
         */
        protected Comparator<MessageInfoHolder> getComparator() {
            final List<Comparator<MessageInfoHolder>> chain = new ArrayList<Comparator<MessageInfoHolder>>(2 /* we add 2 comparators at most */);

            {
                // add the specified comparator
                final Comparator<MessageInfoHolder> comparator = SORT_COMPARATORS.get(sortType);
                if (sortAscending) {
                    chain.add(comparator);
                } else {
                    chain.add(new ReverseComparator<MessageInfoHolder>(comparator));
                }
            }

            {
                // add the date comparator if not already specified
                if (sortType != SORT_TYPE.SORT_DATE) {
                    final Comparator<MessageInfoHolder> comparator = SORT_COMPARATORS.get(SORT_TYPE.SORT_DATE);
                    if (sortDateAscending) {
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

        public void folderLoading(String folder, boolean loading) {
            if (mCurrentFolder != null && mCurrentFolder.name.equals(folder)) {
                mCurrentFolder.loading = loading;
            }
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    updateFooterView();
                }
            });
        }

        private void refreshTitle() {
            runOnUiThread(new Runnable() {
                public void run() {
                    refreshTitleOnThread();
                }
            });
        }

        private void refreshTitleOnThread() {
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

            getWindow().setFeatureInt(Window.FEATURE_PROGRESS, level);
        }

        private void setWindowTitle() {
            if (mActionBar != null) {
                setActionBarTitle();
            }
        }


        public void progress(final boolean progress) {
            runOnUiThread(new Runnable() {
                public void run() {
                    showProgressIndicator(progress);
                }
            });
        }

        public void addAttachment(final View attachmentView) {
            runOnUiThread(new Runnable() {
                public void run() {
                    mMessageView.addAttachment(attachmentView);
                }
            });
        }

        public void networkError() {
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MessageList.this, R.string.status_network_error, Toast.LENGTH_LONG).show();
                }
            });
        }

        public void invalidIdError() {
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MessageList.this, R.string.status_invalid_id_error, Toast.LENGTH_LONG).show();
                }
            });
        }


        public void fetchingAttachment() {
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MessageList.this,
                                   getString(R.string.message_view_fetching_attachment_toast),
                                   Toast.LENGTH_SHORT).show();
                }
            });
        }


        public void setHeaders(final Message message, final Account account) {
            runOnUiThread(new Runnable() {
                public void run() {
                    mMessageView.setHeaders(message, account);
                }
            });
        }

    }

    public static void actionHandleFolder(Context context, Account account, String folder) {
        Intent intent = actionHandleFolderIntent(context, account, folder);
        context.startActivity(intent);
    }

    public static Intent actionHandleFolderIntent(Context context, Account account, String folder) {
        Intent intent = new Intent(context, MessageList.class);
        intent.putExtra(EXTRA_ACCOUNT, account.getUuid());

        if (folder != null) {
            intent.putExtra(EXTRA_FOLDER, folder);
        }
        return intent;
    }

    public static void actionHandle(Context context, String title, String queryString, boolean integrate, Flag[] flags, Flag[] forbiddenFlags) {
        Intent intent = new Intent(context, MessageList.class);
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

    public static void actionHandle(Context context, String title, SearchSpecification searchSpecification) {
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
        context.startActivity(intent);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Use mListView.getAdapter() to get the WrapperListAdapter that includes the footer view.
        if (mCurrentFolder != null && ((position + 1) == mListView.getAdapter().getCount())) {
            mController.loadMoreMessages(mAccount, mFolderName, mAdapter.mListener);
            return;
        }

        MessageInfoHolder message = (MessageInfoHolder) mAdapter.getItem(position);
        if (mSelectedCount > 0) {
            // In multiselect mode make sure that clicking on the item results
            // in toggling the 'selected' checkbox.
            setSelected(message, !message.selected);
        } else {
            onOpenMessage(message);
            //mListView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        context = this;
        super.onCreate(savedInstanceState);

        mInflater = getLayoutInflater();
        initializeLayout();
        onNewIntent(getIntent());
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent); // onNewIntent doesn't autoset our "internal" intent

        // Only set "touchable" when we're first starting up the activity.
        // Otherwise we get force closes when the user toggles it midstream.
        mTouchView = K9.messageListTouchable();
        mPreviewLines = K9.messageListPreviewLines();

        String accountUuid = intent.getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);
        mFolderName = intent.getStringExtra(EXTRA_FOLDER);
        mQueryString = intent.getStringExtra(EXTRA_QUERY);

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

        mAdapter = new MessageListAdapter();
        restorePreviousData();

        if (mFolderName != null) {
            mCurrentFolder = mAdapter.getFolder(mFolderName, mAccount);
        }

        mController = MessagingController.getInstance(getApplication());
        mListView.setAdapter(mAdapter);

        mListView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {

                mListView.setItemChecked(position, true);
                //TODO   mHandler.post(new Runnable() {
                //       public void run() {
                mCurrentMessageInfo = (MessageInfoHolder) mAdapter.getItem(position);
                onOpenMessage(mCurrentMessageInfo);
                //       }
                //   });
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}



        }



                                           );

        /// Loading up by a URL TODO!!!
        showMessageFromIntentURL(intent);

    }
    public void showMessageFromIntentURL(Intent intent) {
        //TODO
        Uri uri = intent.getData();
        if (uri != null) {
            List<String> segmentList = uri.getPathSegments();
            if (segmentList.size() != 3) {
                //TODO: Use ressource to externalize message
                Toast.makeText(this, "Invalid intent uri: " + uri.toString(), Toast.LENGTH_LONG).show();
                return;
            }

            String accountId = segmentList.get(0);
            Collection<Account> accounts = Preferences.getPreferences(this).getAvailableAccounts();
            boolean found = false;
            for (Account account : accounts) {
                if (String.valueOf(account.getAccountNumber()).equals(accountId)) {
                    mAccount = account;
                    found = true;
                    break;
                }
            }
            if (!found) {
                //TODO: Use ressource to externalize message
                Toast.makeText(this, "Invalid account id: " + accountId, Toast.LENGTH_LONG).show();
                return;
            }

            MessageReference messageReference = new MessageReference();
            messageReference.accountUuid = mAccount.getUuid();
            messageReference.folderName = segmentList.get(1);
            messageReference.uid = segmentList.get(2);
        }
    }

    @SuppressWarnings("unchecked")
    private void restorePreviousData() {
        final Object previousData = getLastNonConfigurationInstance();

        if (previousData != null) {
            mAdapter.messages.addAll((List<MessageInfoHolder>) previousData);
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
        mState.putSerializable(STATE_PGP_DATA, mPgpData);
        mState.putBoolean(SHOW_PICTURES, mMessageView.showPictures());
    }

    public void restoreListState() {
        if (mState == null) {
            return;
        }


        mPgpData = (PgpData) mState.getSerializable(STATE_PGP_DATA);
        mMessageView.updateCryptoLayout(mAccount.getCryptoProvider(), mPgpData, mMessage);
        mMessageView.setLoadPictures(mState.getBoolean(SHOW_PICTURES));


        int pos = mState.getInt(EXTRA_LIST_POSITION, ListView.INVALID_POSITION);

        if (pos >= mListView.getCount()) {
            pos = mListView.getCount() - 1;
        }

        if (pos == ListView.INVALID_POSITION) {
            mListView.setSelected(false);
        } else {
            mListView.setSelection(pos);
            mListView.setItemChecked(pos, true);
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

        sortType = mController.getSortType();
        sortAscending = mController.isSortAscending(sortType);
        sortDateAscending = mController.isSortAscending(SORT_TYPE.SORT_DATE);

        mController.addListener(mAdapter.mListener);
        if (mAccount != null) {
            mController.notifyAccountCancel(this, mAccount);
            MessagingController.getInstance(getApplication()).notifyAccountCancel(this, mAccount);
        }

        if (mAdapter.messages.isEmpty()) {
            if (mFolderName != null) {
                mController.listLocalMessages(mAccount, mFolderName,  mAdapter.mListener);
            } else if (mQueryString != null) {
                mController.searchLocalMessages(mAccountUuids, mFolderNames, null, mQueryString, mIntegrate, mQueryFlags, mForbiddenFlags, mAdapter.mListener);
            }

        } else {
            new Thread() {
                @Override
                public void run() {
                    mAdapter.markAllMessagesAsDirty();

                    if (mFolderName != null) {
                        mController.listLocalMessagesSynchronous(mAccount, mFolderName,  mAdapter.mListener);
                    } else if (mQueryString != null) {
                        mController.searchLocalMessagesSynchronous(mAccountUuids, mFolderNames, null, mQueryString, mIntegrate, mQueryFlags, mForbiddenFlags, mAdapter.mListener);
                    }


                    mAdapter.pruneDirtyMessages();
                    runOnUiThread(new Runnable() {
                        public void run() {
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
    }
    private void initializeLayout() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.message_list);
        initializeActionBar();

        mListView = (ListView) findViewById(R.id.message_list);
        mListView.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        mListView.setLongClickable(true);
        mListView.setFastScrollEnabled(true);
        mListView.setScrollingCacheEnabled(true);
        mListView.setFocusableInTouchMode(true);
        mListView.setOnItemClickListener(this);
        mListView.addFooterView(getFooterView(mListView));


        mListView.setItemsCanFocus(false);
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        registerForContextMenu(mListView);


        initializeMessageView();
        setupButtonViews();
        setupBatchButtonViews();

        // Gesture detection
        gestureDetector = new GestureDetector(new MyGestureDetector());
        gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    return true;
                }
                return false;
            }
        };

        mListView.setOnTouchListener(gestureListener);
    }

    private void initializeActionBar() {
        ActionBar mActionBar = (ActionBar) findViewById(R.id.actionbar);
        Log.v(K9.LOG_TAG, "Action bar exists! " + mActionBar);
        //actionBar.setHomeAction(new IntentAction(this, HomeActivity.createIntent(this), R.drawable.ic_title_home_default));
        //actionBar.addAction(new IntentAction(this, createShareIntent(), R.drawable.ic_title_share_default));
        mActionBar.addAction(new SearchAction());
        mActionBar.addAction(new SyncAction());
        mActionBar.addAction(new ComposeAction());
        mActionBar.setOnTitleClickListener(
        new OnClickListener() {
            @Override public void onClick(View v) {
                onBackPressed();
            }
        });

    }

    public void setActionBarTitle() {
        if (mAccount != null && mFolderName != null) {
            String displayName  = mFolderName;

            if (K9.INBOX.equalsIgnoreCase(displayName)) {
                displayName = getString(R.string.special_mailbox_name_inbox);
            }

            mActionBar.setTitle(mAccount.getDescription() + " / " + displayName);
        } else if (mQueryString != null) {
            mActionBar.setTitle(getString(R.string.search_results) + " / " + mQueryString);
        }
    }

    public class SearchAction extends AbstractAction {
        public SearchAction() {
            super(R.drawable.ic_actionbar_search);
        }
        @Override
        public void performAction(View view) {
            onSearchRequested();
        }
    }

    public class SyncAction extends AbstractAction {
        public SyncAction() {
            super(R.drawable.ic_actionbar_refresh);
        }
        @Override
        public void performAction(View view) {
            checkMail(mAccount, mFolderName);
        }
    }
    public class ComposeAction extends AbstractAction {
        public ComposeAction() {
            super(R.drawable.ic_actionbar_compose);
        }
        @Override
        public void performAction(View view) {
            QuickAction qa = new QuickAction(view);
            ActionItem composeAction = new ActionItem();
            composeAction.setTitle(getString(R.string.compose_action));
            composeAction.setIcon(getResources().getDrawable(R.drawable.ic_menu_compose));
            composeAction.setOnClickListener(new OnClickListener() {
                @Override public void onClick(View v) {
                    onCompose();
                }
            });
            qa.addActionItem(composeAction);

            if (mCurrentMessageInfo != null) {
                ActionItem replyAction = new ActionItem();
                replyAction.setTitle(getString(R.string.reply_action));
                replyAction.setIcon(getResources().getDrawable(R.drawable.ic_menu_reply));
                replyAction.setOnClickListener(new OnClickListener() {
                    @Override public void onClick(View v) {
                        onReply(mCurrentMessageInfo);
                    }
                });
                qa.addActionItem(replyAction);
                ActionItem ReplyAllAction = new ActionItem();
                ReplyAllAction.setTitle(getString(R.string.reply_all_action));
                ReplyAllAction.setIcon(getResources().getDrawable(R.drawable.ic_menu_reply_all));
                ReplyAllAction.setOnClickListener(new OnClickListener() {
                    @Override public void onClick(View v) {
                        onReplyAll(mCurrentMessageInfo);
                    }
                });
                qa.addActionItem(ReplyAllAction);
                ActionItem forwardAction = new ActionItem();
                forwardAction.setTitle(getString(R.string.forward_action));
                forwardAction.setIcon(getResources().getDrawable(R.drawable.ic_menu_forward_mail));
                forwardAction.setOnClickListener(new OnClickListener() {
                    @Override public void onClick(View v) {
                        onForward(mCurrentMessageInfo);
                    }
                });
                qa.addActionItem(forwardAction);

                /*
                ActionItem shareAction = new ActionItem();
                shareAction.setTitle(getString(R.string.share_action));
                shareAction.setIcon(getResources().getDrawable(R.drawable.ic_menu_share));
                shareAction.setOnClickListener(new OnClickListener() {
                    @Override public void onClick(View v) {
                        onCompose();
                    }
                });
                qa.addActionItem(shareAction);
                */




            }



            qa.show();
        }
    };



    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            // Text selection is finished. Allow scrolling again.
            mToggleScrollView.setScrolling(true);
        } else if (K9.zoomControlsEnabled()) {
            // If we have system zoom controls enabled, disable scrolling so the screen isn't wiggling around while
            // trying to zoom.
            if (ev.getAction() == MotionEvent.ACTION_POINTER_2_DOWN) {
                mToggleScrollView.setScrolling(false);
            } else if (ev.getAction() == MotionEvent.ACTION_POINTER_2_UP) {
                mToggleScrollView.setScrolling(true);
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void initializeMessageView() {
        mTopView = mToggleScrollView = (ToggleScrollView) findViewById(R.id.top_view);
        mMessageView = (SingleMessageView) findViewById(R.id.message_view);

        mMessageView.initialize(this);

    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return mAdapter.messages;
    }

    private void displayMessage(MessageInfoHolder holder) {
        mCurrentMessageInfo = holder;
        mAccount = Preferences.getPreferences(this).getAccount(mCurrentMessageInfo.accountUuid);
        clearMessageDisplay();
        // TODO fTindSurroundingMessagesUid();
        // start with fresh, empty PGP data
        mPgpData = new PgpData();
        mTopView.setVisibility(View.VISIBLE);
        mController.loadMessageForView(mAccount, holder.folder.name, holder.uid, mAdapter.mListener);
        setupDisplayMessageButtons();
    }


    public void displayMessageBody(final Account account, final String folder, final String uid, final Message message) {
        runOnUiThread(new Runnable() {
            public void run() {
                mTopView.scrollTo(0, 0);
                try {
                    if (MessageList.this.mMessage != null
                            && MessageList.this.mMessage.isSet(Flag.X_DOWNLOADED_PARTIAL)
                    && message.isSet(Flag.X_DOWNLOADED_FULL)) {
                        mMessageView.setHeaders(message, account);
                    }
                    MessageList.this.mMessage = message;
                    mMessageView.displayMessageBody(account, folder, uid, message, mPgpData);
                    mMessageView.renderAttachments(mMessage, 0, mMessage, mAccount, mController, mAdapter.mListener);
                } catch (MessagingException e) {
                    if (Config.LOGV) {
                        Log.v(K9.LOG_TAG, "loadMessageForViewBodyAvailable", e);
                    }
                }
            }
        });
    }


    private void clearMessageDisplay() {
        mTopView.setVisibility(View.GONE);
        mTopView.scrollTo(0, 0);
        mMessageView.resetView();

    }

    @Override
    public void onBackPressed() {
        // This will be called either automatically for you on 2.0
        // or later, or by the code above on earlier versions of the
        // platform.
        //

        if (mListView.getVisibility() != View.VISIBLE) {
            //mListView.setVisibility(View.VISIBLE);
        } else if (K9.manageBack()) {



            if (mQueryString == null) {
                onShowFolderList();
            } else {
                onAccounts();
            }
        } else {
            finish();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, final KeyEvent event) {
        if (
            // XXX TODO - when we go to android 2.0, uncomment this
            // android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ECLAIR &&
            keyCode == KeyEvent.KEYCODE_BACK
            && event.getRepeatCount() == 0
        ) {
            // Take care of calling this method on earlier versions of
            // the platform where it doesn't exist.
            onBackPressed();
            return true;
        }

        // Shortcuts that work no matter what is selected
        switch (keyCode) {

            // messagelist is actually a K9Activity, not a K9ListActivity
            // This saddens me greatly, but to support volume key navigation
            // in MessageView, we implement this bit of wrapper code
        case KeyEvent.KEYCODE_VOLUME_UP: {
            if (K9.useVolumeKeysForListNavigationEnabled()) {
                gotoPreviousItem();
                return true;
            }
            return false;
        }
        case KeyEvent.KEYCODE_VOLUME_DOWN: {
            if (K9.useVolumeKeysForListNavigationEnabled()) {

                gotoNextItem();
                return true;
            }
            return false;
        }
        case KeyEvent.KEYCODE_DPAD_LEFT: {
            if (mBatchButtonArea.hasFocus()) {
                return false;
            } else {
                return true;
            }
        }
        case KeyEvent.KEYCODE_DPAD_RIGHT: {
            if (mBatchButtonArea.hasFocus()) {
                return false;
            } else {
                return true;
            }
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
            onToggleSortAscending();
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

                if (message != null) {
                    switch (keyCode) {
                    case KeyEvent.KEYCODE_D:
                    case KeyEvent.KEYCODE_DEL: {
                        onDelete(message);
                        return true;
                    }
                    case KeyEvent.KEYCODE_S: {
                        setSelected(message, !message.selected);
                        return true;
                    }
                    case KeyEvent.KEYCODE_F: {
                        onForward(message);
                        return true;
                    }
                    case KeyEvent.KEYCODE_A: {
                        onReplyAll(message);
                        return true;
                    }
                    case KeyEvent.KEYCODE_R: {
                        onReply(message);
                        return true;
                    }
                    case KeyEvent.KEYCODE_G: {
                        onToggleFlag(message);
                        return true;
                    }
                    case KeyEvent.KEYCODE_M: {
                        onMove(message);
                        return true;
                    }
                    case KeyEvent.KEYCODE_V: {
                        onArchive(message);
                        return true;
                    }
                    case KeyEvent.KEYCODE_Y: {
                        onCopy(message);
                        return true;
                    }
                    case KeyEvent.KEYCODE_Z: {
                        onToggleRead(message);
                        return true;
                    }
                    case KeyEvent.KEYCODE_J:
                    case KeyEvent.KEYCODE_P: {
                        gotoPreviousItem();
                        return true;
                    }
                    case KeyEvent.KEYCODE_N:
                    case KeyEvent.KEYCODE_K: {
                        gotoNextItem();
                        return true;
                    }
                    /* TODO - MESSAGE VIEW SPECIAL KEYS
                    case KeyEvent.KEYCODE_SHIFT_LEFT:
                    case KeyEvent.KEYCODE_SHIFT_RIGHT: {
                        // Selecting text started via shift key. Disable scrolling as
                        // this causes problems when selecting text.
                        mToggleScrollView.setScrolling(false);
                        break;
                    }
                    case KeyEvent.KEYCODE_S: {
                        onRefile(mAccount.getSpamFolderName());
                        return true;
                    }
                    case KeyEvent.KEYCODE_Z: {
                        mHandler.post(new Runnable() {
                            public void run() {
                                mMessageView.zoom(event);
                            }
                        });
                        return true;
                    }
                        */
                    case KeyEvent.KEYCODE_H: {
                        Toast toast = Toast.makeText(this, R.string.message_help_key, Toast.LENGTH_LONG);
                        toast.show();
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

    /** TODO
    private void findSurroundingMessagesUid() {
        mNextMessage = mPreviousMessage = null;
        int i = mMessageReferences.indexOf(mMessageReference);
        if (i < 0)
            return;
        if (i != 0)
            mNextMessage = mMessageReferences.get(i - 1);
        if (i != (mMessageReferences.size() - 1))
            mPreviousMessage = mMessageReferences.get(i + 1);
    }

    */
    private void showNextMessageOrReturn() {
        if (K9.messageViewReturnToList()) {
            finish();
        } else {
            showNextMessage();
        }
    }

    private void showNextMessage() {
        // TODO findSurroundingMessagesUid();
        if (mLastDirection == NEXT && mNextMessage != null) {
            gotoNextItem();
        } else if (mLastDirection == PREVIOUS && mPreviousMessage != null) {
            gotoPreviousItem();
        } else if (mNextMessage != null) {
            gotoNextItem();
        } else if (mPreviousMessage != null) {
            gotoPreviousItem();
        } else {
            finish();
        }
    }


    private void gotoNextItem() {
        /*       if (mNextMessage == null)
               {
                   Toast.makeText(this, getString(R.string.end_of_folder), Toast.LENGTH_SHORT).show();
                   return;
               } */
        //        mLastDirection = NEXT;
        mListView.requestFocus();
        simulateKeystroke(KeyEvent.KEYCODE_DPAD_DOWN);
        mListView.smoothScrollToPosition(mListView.getSelectedItemPosition());
    }

    private void gotoPreviousItem() {
        /*
                if (mPreviousMessage == null)
                {
                    Toast.makeText(this, getString(R.string.end_of_folder), Toast.LENGTH_SHORT).show();
                    return;
                } */
        //    mLastDirection = PREVIOUS;
        mListView.requestFocus();
        simulateKeystroke(KeyEvent.KEYCODE_DPAD_UP);
        mListView.smoothScrollToPosition(mListView.getSelectedItemPosition());
    }


    private void onResendMessage(MessageInfoHolder message) {
        MessageCompose.actionEditDraft(this, message.message.getFolder().getAccount(), message.message);
    }

    private void onOpenMessage(final MessageInfoHolder message) {
        if (message == null) {
            return;
        }
        if (message.folder.name.equals(message.message.getFolder().getAccount().getDraftsFolderName())) {
            MessageCompose.actionEditDraft(this, message.message.getFolder().getAccount(), message.message);
        } else {
            mHandler.post(new Runnable() {
                public void run() {
                    displayMessage(message);
                }
            });
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
    private void onDownloadRemainder() {
        if (mMessage.isSet(Flag.X_DOWNLOADED_FULL)) {
            return;
        }
        mMessageView.downloadRemainderButton().setEnabled(false);
        mController.loadMessageForViewRemote(mAccount, mCurrentMessageInfo.folder.name, mCurrentMessageInfo.uid, mAdapter.mListener);
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

    private void onEditPrefs() {
        Prefs.actionPrefs(this);
    }

    private void onEditAccount() {
        AccountSettings.actionSettings(this, mAccount);
    }

    private void changeSort(SORT_TYPE newSortType) {
        if (sortType == newSortType) {
            onToggleSortAscending();
        } else {
            sortType = newSortType;
            mController.setSortType(sortType);
            sortAscending = mController.isSortAscending(sortType);
            sortDateAscending = mController.isSortAscending(SORT_TYPE.SORT_DATE);
            reSort();
        }
    }

    private void reSort() {
        int toastString = sortType.getToast(sortAscending);

        Toast toast = Toast.makeText(this, toastString, Toast.LENGTH_SHORT);
        toast.show();

        mHandler.sortMessages();
    }

    private void onCycleSort() {
        SORT_TYPE[] sorts = SORT_TYPE.values();
        int curIndex = 0;

        for (int i = 0; i < sorts.length; i++) {
            if (sorts[i] == sortType) {
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

    private void onToggleSortAscending() {
        mController.setSortAscending(sortType, !sortAscending);

        sortAscending = mController.isSortAscending(sortType);
        sortDateAscending = mController.isSortAscending(SORT_TYPE.SORT_DATE);

        reSort();
    }

    private void onDelete(MessageInfoHolder holder) {
        if (K9.confirmDelete()) {
            // TODO: how to pass a messageinfoholder to the dialog?
            //
            // showDialog(R.id.dialog_confirm_delete);
        } else {
            delete(holder);
        }
    }

    private void delete(MessageInfoHolder holder) {
        mAdapter.removeMessage(holder);
        mController.deleteMessages(new Message[] { holder.message }, null);
        gotoNextItem();
    }

    private void onMove(MessageInfoHolder holder) {
        if (holder == null) {
            return;
        }
        if (!mController.isMoveCapable(holder.message.getFolder().getAccount())) {
            return;
        }

        if (!mController.isMoveCapable(holder.message)) {
            Toast toast = Toast.makeText(this, R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        final Account account = holder.message.getFolder().getAccount();

        Intent intent = new Intent(this, ChooseFolder.class);
        intent.putExtra(ChooseFolder.EXTRA_ACCOUNT, account.getUuid());
        intent.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, holder.folder.name);
        intent.putExtra(ChooseFolder.EXTRA_SEL_FOLDER, account.getLastSelectedFolderName());
        intent.putExtra(ChooseFolder.EXTRA_MESSAGE, holder.message.makeMessageReference());
        startActivityForResult(intent, ACTIVITY_CHOOSE_FOLDER_MOVE);
    }

    private void onArchive(MessageInfoHolder holder) {
        if (holder == null) {
            return;
        }
        onRefile(holder, holder.message.getFolder().getAccount().getArchiveFolderName());
    }

    private void onSpam(MessageInfoHolder holder) {
        if (holder == null) {
            return;
        }
        onRefile(holder, holder.message.getFolder().getAccount().getSpamFolderName());
    }

    private void onRefile(MessageInfoHolder holder, String folder) {

        if (!mController.isMoveCapable(holder.message)) {
            Toast toast = Toast.makeText(this, R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        if (folder == null || K9.FOLDER_NONE.equalsIgnoreCase(folder)) {
            return;
        }
        mAdapter.removeMessage(holder);
        // TODO showNextMessageOrReturn();
        mController.moveMessage(holder.message.getFolder().getAccount(), holder.message.getFolder().getName(), holder.message, folder, null);
        if (holder.uid == mCurrentMessageInfo.uid) {
            gotoNextItem();
        }
    }


    private void onCopy(MessageInfoHolder holder) {
        if (holder == null) {
            return;
        }
        if (!mController.isCopyCapable(holder.message.getFolder().getAccount())) {
            return;
        }

        if (!mController.isCopyCapable(holder.message)) {
            Toast toast = Toast.makeText(this, R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        final Account account = holder.message.getFolder().getAccount();

        Intent intent = new Intent(this, ChooseFolder.class);
        intent.putExtra(ChooseFolder.EXTRA_ACCOUNT, account.getUuid());
        intent.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, holder.folder.name);
        intent.putExtra(ChooseFolder.EXTRA_SEL_FOLDER, account.getLastSelectedFolderName());
        intent.putExtra(ChooseFolder.EXTRA_MESSAGE, holder.message.makeMessageReference());
        startActivityForResult(intent, ACTIVITY_CHOOSE_FOLDER_COPY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK)
            return;

        switch (requestCode) {
        case ACTIVITY_CHOOSE_FOLDER_MOVE:
        case ACTIVITY_CHOOSE_FOLDER_COPY: {
            if (data == null)
                return;

            final String destFolderName = data.getStringExtra(ChooseFolder.EXTRA_NEW_FOLDER);
            final MessageReference ref = data.getParcelableExtra(ChooseFolder.EXTRA_MESSAGE);
            final MessageInfoHolder m = mAdapter.getMessage(ref);

            if ((destFolderName != null) && (m != null)) {
                final Account account = m.message.getFolder().getAccount();

                account.setLastSelectedFolderName(destFolderName);

                switch (requestCode) {
                case ACTIVITY_CHOOSE_FOLDER_MOVE:
                    onRefile(m, destFolderName);
                    // TODO showNextMessageOrReturn();
                    break;

                case ACTIVITY_CHOOSE_FOLDER_COPY:
                    onCopyChosen(m, destFolderName);
                    break;
                }
            }
            break;
        }
        case ACTIVITY_CHOOSE_FOLDER_MOVE_BATCH:
        case ACTIVITY_CHOOSE_FOLDER_COPY_BATCH: {
            final String destFolderName = data.getStringExtra(ChooseFolder.EXTRA_NEW_FOLDER);
            final String accountUuid = data.getStringExtra(ChooseFolder.EXTRA_ACCOUNT);
            final Account account = Preferences.getPreferences(this).getAccount(accountUuid);

            account.setLastSelectedFolderName(destFolderName);

            switch (requestCode) {
            case ACTIVITY_CHOOSE_FOLDER_MOVE_BATCH:
                onMoveChosenBatch(destFolderName);
                break;

            case ACTIVITY_CHOOSE_FOLDER_COPY_BATCH:
                onCopyChosenBatch(destFolderName);
                break;
            }
        }
        }
    }


    private void onCopyChosen(MessageInfoHolder holder, String folderName) {
        if (holder == null) {
            return;
        }
        if (mController.isCopyCapable(holder.message.getFolder().getAccount()) && folderName != null) {
            mController.copyMessage(holder.message.getFolder().getAccount(),
                                    holder.message.getFolder().getName(), holder.message, folderName, null);
        }
    }

    private void onReply(MessageInfoHolder holder) {
        // TODO - need to re-enable pgp support here
        MessageCompose.actionReply(this, holder.message.getFolder().getAccount(), holder.message, false, null);
    }

    private void onReplyAll(MessageInfoHolder holder) {
        // TODO - need to re-enable pgp support here
        MessageCompose.actionReply(this, holder.message.getFolder().getAccount(), holder.message, true, null);
    }

    private void onForward(MessageInfoHolder holder) {
        // TODO - need to re-enable pgp support here
        MessageCompose.actionForward(this, holder.message.getFolder().getAccount(), holder.message, null);
    }

    private void onMarkAllAsRead(final Account account, final String folder) {
        showDialog(DIALOG_MARK_ALL_AS_READ);
    }

    private void onExpunge(final Account account, String folderName) {
        mController.expunge(account, folderName, null);
    }

    // TODO: when switching to API version 8, override onCreateDialog(int, Bundle)

    /**
     * @param id The id of the dialog.
     * @return The dialog. If you return null, the dialog will not be created.
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    public Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_MARK_ALL_AS_READ:
            return createMarkAllAsReadDialog();
        case R.id.dialog_confirm_delete: {
            return createConfirmDeleteDialog(id);
        }
        }

        return super.onCreateDialog(id);
    }

    @Override
    public void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
        case DIALOG_MARK_ALL_AS_READ: {
            if (mCurrentFolder != null) {
                ((AlertDialog)dialog).setMessage(getString(R.string.mark_all_as_read_dlg_instructions_fmt,
                                                 mCurrentFolder.displayName));
            }
            break;
        }
        default: {
            super.onPrepareDialog(id, dialog);
        }
        }
    }

    private Dialog createMarkAllAsReadDialog() {
        return new AlertDialog.Builder(this)
               .setTitle(R.string.mark_all_as_read_dlg_title)
               .setMessage(getString(R.string.mark_all_as_read_dlg_instructions_fmt,
                                     mCurrentFolder.displayName))
        .setPositiveButton(R.string.okay_action, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dismissDialog(DIALOG_MARK_ALL_AS_READ);

                try {
                    mController.markAllMessagesRead(mAccount, mCurrentFolder.name);

                    synchronized (mAdapter.messages) {
                        for (MessageInfoHolder holder : mAdapter.messages) {
                            holder.read = true;
                        }
                    }
                    mHandler.sortMessages();
                } catch (Exception e) {
                    // Ignore
                }
            }
        })
        .setNegativeButton(R.string.cancel_action, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dismissDialog(DIALOG_MARK_ALL_AS_READ);
            }
        })
               .create();
    }


    /**
     * @param id
     * @return Never <code>null</code>
     */
    protected Dialog createConfirmDeleteDialog(final int id) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_confirm_delete_title);
        builder.setMessage(R.string.dialog_confirm_delete_message);
        builder.setPositiveButton(R.string.dialog_confirm_delete_confirm_button,
        new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismissDialog(id);
                // TODO delete();
            }
        });
        builder.setNegativeButton(R.string.dialog_confirm_delete_cancel_button,
        new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismissDialog(id);
            }
        });
        return builder.create();
    }

    private void onToggleRead(MessageInfoHolder holder) {
        if (holder == null) {
            return;
        }
        mController.setFlag(holder.message.getFolder().getAccount(), holder.message.getFolder().getName(), new String[] { holder.uid }, Flag.SEEN, !holder.read);
        try {
            holder.read = !holder.read;
            holder.message.setFlag(Flag.SEEN, holder.read);
            if (mCurrentMessageInfo.uid == holder.uid) {
                mCurrentMessageInfo = holder;
                mMessageView.setHeaders(holder.message, holder.message.getFolder().getAccount());
            }
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Unable to unset SEEN flag on message", e);
        }
        mHandler.sortMessages();
    }

    private void onToggleFlag(MessageInfoHolder holder) {
        if (holder == null) {
            return;
        }
        mController.setFlag(holder.message.getFolder().getAccount(), holder.message.getFolder().getName(), new String[] { holder.uid }, Flag.FLAGGED, !holder.flagged);
        try {
            holder.flagged = !holder.flagged;
            holder.message.setFlag(Flag.FLAGGED, holder.flagged);
            if (holder.uid == mCurrentMessageInfo.uid) {
                mCurrentMessageInfo = holder;
                mMessageView.setHeaders(holder.message, holder.message.getFolder().getAccount());
            }

        } catch (MessagingException me) {
            Log.e(K9.LOG_TAG, "Could not set flag on local message", me);
        }
        mHandler.sortMessages();
    }

    private void checkMail(Account account, String folderName) {
        mController.synchronizeMailbox(account, folderName, mAdapter.mListener, null);
        mController.sendPendingMessages(account, mAdapter.mListener);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
        case R.id.accounts: {
            onAccounts();
            return true;
        }
        case R.id.set_sort_date: {
            changeSort(SORT_TYPE.SORT_DATE);
            return true;
        }
        case R.id.set_sort_subject: {
            changeSort(SORT_TYPE.SORT_SUBJECT);
            return true;
        }
        case R.id.set_sort_sender: {
            changeSort(SORT_TYPE.SORT_SENDER);
            return true;
        }
        case R.id.set_sort_flag: {
            changeSort(SORT_TYPE.SORT_FLAGGED);
            return true;
        }
        case R.id.set_sort_unread: {
            changeSort(SORT_TYPE.SORT_UNREAD);
            return true;
        }
        case R.id.set_sort_attach: {
            changeSort(SORT_TYPE.SORT_ATTACHMENT);
            return true;
        }
        case R.id.select_all:
        case R.id.batch_select_all: {
            setAllSelected(true);
            toggleBatchButtons();
            return true;
        }
        case R.id.batch_deselect_all: {
            setAllSelected(false);
            toggleBatchButtons();
            return true;
        }
        case R.id.batch_delete_op: {
            deleteSelected();
            return true;
        }
        case R.id.batch_mark_read_op: {
            flagSelected(Flag.SEEN, true);
            return true;
        }
        case R.id.batch_mark_unread_op: {
            flagSelected(Flag.SEEN, false);
            return true;
        }
        case R.id.batch_flag_op: {
            flagSelected(Flag.FLAGGED, true);
            return true;
        }
        case R.id.batch_unflag_op: {
            flagSelected(Flag.FLAGGED, false);
            return true;
        }
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
        case R.id.list_folders: {
            onShowFolderList();
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
        case R.id.batch_copy_op: {
            onCopyBatch();
            return true;
        }
        case R.id.batch_archive_op: {
            onArchiveBatch();
            return true;
        }
        case R.id.batch_spam_op: {
            onSpamBatch();
            return true;
        }
        case R.id.batch_move_op: {
            onMoveBatch();
            return true;
        }
        case R.id.expunge: {
            if (mCurrentFolder != null) {
                onExpunge(mAccount, mCurrentFolder.name);
            }
            return true;
        }
        case R.id.show_full_header:
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mMessageView.showAllHeaders();
                }
            });
            return true;
        case R.id.select_text:
            mToggleScrollView.setScrolling(false);
            mMessageView.beginSelectingText();
            return true;
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }

    private final int[] batch_ops = { R.id.batch_copy_op, R.id.batch_delete_op, R.id.batch_flag_op,
                                      R.id.batch_unflag_op, R.id.batch_mark_read_op, R.id.batch_mark_unread_op,
                                      R.id.batch_archive_op, R.id.batch_spam_op, R.id.batch_move_op,
                                      R.id.batch_select_all, R.id.batch_deselect_all
                                    };

    private void setOpsState(Menu menu, boolean state, boolean enabled) {
        for (int id : batch_ops) {
            menu.findItem(id).setVisible(state);
            menu.findItem(id).setEnabled(enabled);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean anySelected = anySelected();

        menu.findItem(R.id.select_all).setVisible(! anySelected);
        menu.findItem(R.id.batch_ops).setVisible(anySelected);

        setOpsState(menu, true, anySelected);

        if (mQueryString != null) {
            menu.findItem(R.id.mark_all_as_read).setVisible(false);
            menu.findItem(R.id.list_folders).setVisible(false);
            menu.findItem(R.id.expunge).setVisible(false);
            menu.findItem(R.id.batch_archive_op).setVisible(false);
            menu.findItem(R.id.batch_spam_op).setVisible(false);
            menu.findItem(R.id.batch_move_op).setVisible(false);
            menu.findItem(R.id.batch_copy_op).setVisible(false);
            menu.findItem(R.id.folder_settings).setVisible(false);
            menu.findItem(R.id.account_settings).setVisible(false);
        } else {
            if (mCurrentFolder != null && K9.ERROR_FOLDER_NAME.equals(mCurrentFolder.name)) {
                menu.findItem(R.id.expunge).setVisible(false);
            }
            if (K9.FOLDER_NONE.equalsIgnoreCase(mAccount.getArchiveFolderName())) {
                menu.findItem(R.id.batch_archive_op).setVisible(false);
            }
            if (K9.FOLDER_NONE.equalsIgnoreCase(mAccount.getSpamFolderName())) {
                menu.findItem(R.id.batch_spam_op).setVisible(false);
            }
        }

        boolean newFlagState = computeBatchDirection(true);
        boolean newReadState = computeBatchDirection(false);
        menu.findItem(R.id.batch_flag_op).setVisible(newFlagState);
        menu.findItem(R.id.batch_unflag_op).setVisible(!newFlagState);
        menu.findItem(R.id.batch_mark_read_op).setVisible(newReadState);
        menu.findItem(R.id.batch_mark_unread_op).setVisible(!newReadState);
        menu.findItem(R.id.batch_deselect_all).setVisible(anySelected);
        menu.findItem(R.id.batch_select_all).setEnabled(true);

        MenuItem additionalHeadersItem = menu.findItem(R.id.show_full_header);
        if (additionalHeadersItem != null) {
            additionalHeadersItem.setTitle(mMessageView.additionalHeadersVisible() ?
                                           R.string.hide_full_header_action : R.string.show_full_header_action);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.message_list_option, menu);

        return true;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        MessageInfoHolder holder = mSelectedMessage;
        // don't need this anymore
        mSelectedMessage = null;
        if (holder == null) {
            holder = (MessageInfoHolder) mAdapter.getItem(info.position);
        }

        switch (item.getItemId()) {
        case R.id.open: {
            onOpenMessage(mCurrentMessageInfo);
            break;
        }
        case R.id.select: {
            setSelected(mCurrentMessageInfo, true);
            break;
        }
        case R.id.deselect: {
            setSelected(mCurrentMessageInfo, false);
            break;
        }
        case R.id.delete: {
            onDelete(mCurrentMessageInfo);
            break;
        }
        case R.id.reply: {
            onReply(mCurrentMessageInfo);
            break;
        }
        case R.id.reply_all: {
            onReplyAll(mCurrentMessageInfo);
            break;
        }
        case R.id.forward: {
            onForward(mCurrentMessageInfo);
            break;
        }
        case R.id.send_again: {
            onResendMessage(mCurrentMessageInfo);
            break;

        }
        case R.id.mark_as_read: {
            onToggleRead(mCurrentMessageInfo);
            break;
        }
        case R.id.flag: {
            onToggleFlag(mCurrentMessageInfo);
            break;
        }
        case R.id.archive: {
            onArchive(mCurrentMessageInfo);
            break;
        }
        case R.id.spam: {
            onSpam(mCurrentMessageInfo);
            break;
        }
        case R.id.move: {
            onMove(mCurrentMessageInfo);
            break;
        }
        case R.id.copy: {
            onCopy(mCurrentMessageInfo);
            break;
        }
        case R.id.send_alternate: {
            onSendAlternate(mAccount, mCurrentMessageInfo);
            break;
        }
        case R.id.same_sender: {
            MessageList.actionHandle(MessageList.this,
                                     "From " + mCurrentMessageInfo.sender, mCurrentMessageInfo.senderAddress, true,
                                     null, null);
            break;
        }
        }
        return super.onContextItemSelected(item);
    }

    public void onSendAlternate(Account account, MessageInfoHolder holder) {
        mController.sendAlternate(this, account, holder.message);
    }

    public void showProgressIndicator(boolean status) {
        setProgressBarIndeterminateVisibility(status);
        ProgressBar bar = (ProgressBar)mListView.findViewById(R.id.message_list_progress);
        if (bar == null) {
            return;
        }

        bar.setIndeterminate(true);
        if (status) {
            bar.setVisibility(ProgressBar.VISIBLE);
        } else {
            bar.setVisibility(ProgressBar.INVISIBLE);
        }
    }

    class MyGestureDetector extends SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e2 == null || e1 == null)
                return true;

            float deltaX = e2.getX() - e1.getX(),
                  deltaY = e2.getY() - e1.getY();

            boolean movedAcross = (Math.abs(deltaX) > Math.abs(deltaY * 4));
            boolean steadyHand = (Math.abs(deltaX / deltaY) > 2);

            if (movedAcross && steadyHand) {
                boolean selected = (deltaX > 0);
                int position = mListView.pointToPosition((int)e1.getX(), (int)e1.getY());

                if (position != AdapterView.INVALID_POSITION) {
                    MessageInfoHolder msgInfoHolder = (MessageInfoHolder) mAdapter.getItem(position);

                    if (msgInfoHolder != null && msgInfoHolder.selected != selected) {
                        msgInfoHolder.selected = selected;
                        mSelectedCount += (selected ? 1 : -1);
                        mAdapter.notifyDataSetChanged();
                        toggleBatchButtons();
                    }
                }
            }

            return false;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        MessageInfoHolder message = (MessageInfoHolder) mAdapter.getItem(info.position);
        // remember which message was originally selected, in case the list changes while the
        // dialog is up
        mSelectedMessage = message;

        if (message == null) {
            return;
        }

        getMenuInflater().inflate(R.menu.message_list_context, menu);

        menu.setHeaderTitle(message.message.getSubject());

        if (message.read) {
            menu.findItem(R.id.mark_as_read).setTitle(R.string.mark_as_unread_action);
        }

        if (message.flagged) {
            menu.findItem(R.id.flag).setTitle(R.string.unflag_action);
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

        if (K9.FOLDER_NONE.equalsIgnoreCase(account.getArchiveFolderName())) {
            menu.findItem(R.id.archive).setVisible(false);
        }
        if (K9.FOLDER_NONE.equalsIgnoreCase(account.getSpamFolderName())) {
            menu.findItem(R.id.spam).setVisible(false);
        }

        if (message.selected) {
            menu.findItem(R.id.select).setVisible(false);
            menu.findItem(R.id.deselect).setVisible(true);
        } else {
            menu.findItem(R.id.select).setVisible(true);
            menu.findItem(R.id.deselect).setVisible(false);
        }
    }

    class MessageListAdapter extends BaseAdapter {
        private final List<MessageInfoHolder> messages = java.util.Collections.synchronizedList(new ArrayList<MessageInfoHolder>());

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
                addOrUpdateMessage(account, folder, message, true);
            }

            @Override
            public void synchronizeMailboxRemovedMessage(Account account, String folder, Message message) {
                MessageInfoHolder holder = getMessage(message);
                if (holder == null) {
                    Log.w(K9.LOG_TAG, "Got callback to remove non-existent message with UID " + message.getUid());
                } else {
                    removeMessage(holder);
                }
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
                MessageInfoHolder holder = getMessage(message);
                if (holder != null) {
                    removeMessage(holder);
                }
            }

            @Override
            public void listLocalMessagesAddMessages(Account account, String folder, List<Message> messages) {
                addOrUpdateMessages(account, folder, messages, false);
            }

            @Override
            public void listLocalMessagesUpdateMessage(Account account, String folder, Message message) {
                addOrUpdateMessage(account, folder, message, false);
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

                MessageInfoHolder holder = getMessage(ref);
                if (holder != null) {
                    holder.uid = newUid;
                    holder.message.setUid(newUid);
                }
            }
            @Override
            public void loadMessageForViewHeadersAvailable(final Account account, String folder, String uid,
            final Message message) {
                if (!mCurrentMessageInfo.uid.equals(uid) || !mCurrentMessageInfo.folder.name.equals(folder)
                || !mCurrentMessageInfo.accountUuid.equals(account.getUuid())) {
                    return;
                }
                MessageList.this.mMessage = message;
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (!message.isSet(Flag.X_DOWNLOADED_FULL) && !message.isSet(Flag.X_DOWNLOADED_PARTIAL)) {
                            mMessageView.loadBodyFromUrl("file:///android_asset/downloading.html");
                        }
                        mMessageView.setHeaders(message, account);
                        mMessageView.setOnFlagListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                onToggleFlag(mCurrentMessageInfo);
                            }
                        });
                    }
                });
            }

            @Override
            public void loadMessageForViewBodyAvailable(Account account, String folder, String uid,
            Message message) {
                if (!mCurrentMessageInfo.uid.equals(uid) || !mCurrentMessageInfo.folder.name.equals(folder)
                || !mCurrentMessageInfo.accountUuid.equals(account.getUuid())) {
                    return;
                }

                displayMessageBody(account, folder, uid, message);
            }//loadMessageForViewBodyAvailable


            @Override
            public void loadMessageForViewFailed(Account account, String folder, String uid,
            final Throwable t) {
                if (!mCurrentMessageInfo.uid.equals(uid) || !mCurrentMessageInfo.folder.name.equals(folder)
                || !mCurrentMessageInfo.accountUuid.equals(account.getUuid())) {
                    return;
                }
                mHandler.post(new Runnable() {
                    public void run() {
                        setProgressBarIndeterminateVisibility(false);
                        if (t instanceof IllegalArgumentException) {
                            mHandler.invalidIdError();
                        } else {
                            mHandler.networkError();
                        }
                        if ((MessageList.this.mMessage == null) ||
                        !MessageList.this.mMessage.isSet(Flag.X_DOWNLOADED_PARTIAL)) {
                            mMessageView.loadBodyFromUrl("file:///android_asset/empty.html");
                        }
                    }
                });
            }

            @Override
            public void loadMessageForViewFinished(Account account, String folder, String uid,
            final Message message) {
                if (!mCurrentMessageInfo.uid.equals(uid) || !mCurrentMessageInfo.folder.name.equals(folder)
                || !mCurrentMessageInfo.accountUuid.equals(account.getUuid())) {
                    return;
                }
                mHandler.post(new Runnable() {
                    public void run() {
                        setProgressBarIndeterminateVisibility(false);
                        mMessageView.setShowDownloadButton(message);
                    }
                });
            }

            @Override
            public void loadMessageForViewStarted(Account account, String folder, String uid) {
                if (!mCurrentMessageInfo.uid.equals(uid) || !mCurrentMessageInfo.folder.name.equals(folder)
                || !mCurrentMessageInfo.accountUuid.equals(account.getUuid())) {
                    return;
                }
                mHandler.post(new Runnable() {
                    public void run() {
                        setProgressBarIndeterminateVisibility(true);
                    }
                });
            }

            @Override
            public void loadAttachmentStarted(Account account, Message message,
            Part part, Object tag, boolean requiresDownload) {
                if (mMessage != message) {
                    return;
                }
                mMessageView.setAttachmentsEnabled(false);
                mHandler.progress(true);
                if (requiresDownload) {
                    mHandler.fetchingAttachment();
                }
            }

            @Override
            public void loadAttachmentFinished(Account account, Message message, Part part, Object tag) {
                if (mMessage != message) {
                    return;
                }
                mMessageView.setAttachmentsEnabled(true);
                mHandler.progress(false);
                Object[] params = (Object[]) tag;
                boolean download = (Boolean) params[0];
                AttachmentView attachment = (AttachmentView) params[1];
                if (download) {
                    attachment.writeFile();

                } else {
                    attachment.showFile();
                }
            }

            @Override
            public void loadAttachmentFailed(Account account, Message message, Part part,
            Object tag, String reason) {
                if (mMessage != message) {
                    return;
                }
                mMessageView.setAttachmentsEnabled(true);
                mHandler.progress(false);
                mHandler.networkError();
            }


        };

        private boolean updateForMe(Account account, String folder) {
            if ((account.equals(mAccount) && mFolderName != null && folder.equals(mFolderName))) {
                return true;
            } else {
                return false;
            }
        }

        private Drawable mAttachmentIcon;
        private Drawable mAnsweredIcon;
        private View footerView = null;

        MessageListAdapter() {
            mAttachmentIcon = getResources().getDrawable(R.drawable.ic_email_attachment_small);
            mAnsweredIcon = getResources().getDrawable(R.drawable.ic_email_answered_small);
        }

        public void markAllMessagesAsDirty() {
            for (MessageInfoHolder holder : mAdapter.messages) {
                holder.dirty = true;
            }
        }
        public void pruneDirtyMessages() {
            synchronized (mAdapter.messages) {
                Iterator<MessageInfoHolder> iter = mAdapter.messages.iterator();
                while (iter.hasNext()) {
                    MessageInfoHolder holder = iter.next();
                    if (holder.dirty) {
                        if (holder.selected) {
                            mSelectedCount--;
                            toggleBatchButtons();
                        }
                        mAdapter.removeMessage(holder);
                    }
                }
            }
        }

        public void removeMessages(List<MessageInfoHolder> holders) {
            if (holders != null) {
                mHandler.removeMessage(holders);
            }
        }

        public void removeMessage(MessageInfoHolder holder) {
            if (holder == null) {
                return;
            }
            List<MessageInfoHolder> messages = new ArrayList<MessageInfoHolder>();
            messages.add(holder);
            removeMessages(messages);
        }

        private void addOrUpdateMessage(Account account, String folderName, Message message, boolean verifyAgainstSearch) {
            List<Message> messages = new ArrayList<Message>();
            messages.add(message);
            addOrUpdateMessages(account, folderName, messages, verifyAgainstSearch);
        }

        private void addOrUpdateMessages(final Account account, final String folderName, final List<Message> providedMessages, final boolean verifyAgainstSearch) {
            // we copy the message list because the callback doesn't expect
            // the callbacks to mutate it.
            final List<Message> messages = new ArrayList<Message>(providedMessages);

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
                            messageHelper.populate(m, message, new FolderInfoHolder(MessageList.this, messageFolder, messageAccount), messageAccount);
                            messagesToAdd.add(m);
                        } else {
                            if (mQueryString != null) {
                                if (verifyAgainstSearch) {
                                    messagesToSearch.add(message);
                                } else {
                                    m = new MessageInfoHolder();
                                    messageHelper.populate(m, message, new FolderInfoHolder(MessageList.this, messageFolder, messageAccount), messageAccount);
                                    messagesToAdd.add(m);
                                }
                            }
                        }
                    } else {
                        m.dirty = false; // as we reload the message, unset its dirty flag
                        messageHelper.populate(m, message, new FolderInfoHolder(MessageList.this, messageFolder, account), account);
                        needsSort = true;
                    }
                }
            }

            if (messagesToSearch.size() > 0) {
                mController.searchLocalMessages(mAccountUuids, mFolderNames, messagesToSearch.toArray(EMPTY_MESSAGE_ARRAY), mQueryString, mIntegrate, mQueryFlags, mForbiddenFlags,
                new MessagingListener() {
                    @Override
                    public void listLocalMessagesAddMessages(Account account, String folder, List<Message> messages) {
                        addOrUpdateMessages(account, folder, messages, false);
                    }
                });
            }

            if (messagesToRemove.size() > 0) {
                removeMessages(messagesToRemove);
            }

            if (messagesToAdd.size() > 0) {
                mHandler.addMessages(messagesToAdd);
            }

            if (needsSort) {
                mHandler.sortMessages();
                mHandler.resetUnreadCount();
            }
        }
        public MessageInfoHolder getMessage(Message message) {
            return getMessage(message.makeMessageReference());
        }

        // XXX TODO - make this not use a for loop
        public MessageInfoHolder getMessage(MessageReference messageReference) {
            synchronized (mAdapter.messages) {
                for (MessageInfoHolder holder : mAdapter.messages) {
                    /*
                     * 2010-06-21 - cketti
                     * Added null pointer check. Not sure what's causing 'holder'
                     * to be null. See log provided in issue 1749, comment #15.
                     *
                     * Please remove this comment once the cause was found and the
                     * bug(?) fixed.
                     */
                    if ((holder != null) && holder.message.equalsReference(messageReference)) {
                        return holder;
                    }
                }
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
            if (mTouchView) {
                view = mInflater.inflate(R.layout.message_list_item_touchable, parent, false);
                view.setId(R.layout.message_list_item);
            } else {
                view = mInflater.inflate(R.layout.message_list_item, parent, false);
                view.setId(R.layout.message_list_item);
            }
        }
            return view;
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

        private static final int NON_MESSAGE_ITEMS = 1;

        private final OnClickListener flagClickListener = new OnClickListener() {
            public void onClick(View v) {
                // Perform action on clicks
                MessageInfoHolder message = (MessageInfoHolder) getItem((Integer)v.getTag());
                onToggleFlag(message);
            }
        };

        @Override
        public int getCount() {
            return messages.size() + NON_MESSAGE_ITEMS;
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

        public Object getItem(long position) {
            return getItem((int)position);
        }

        @Override
        public Object getItem(int position) {
            try {
                synchronized (mAdapter.messages) {
                    if (position < mAdapter.messages.size()) {
                        return mAdapter.messages.get(position);
                    }
                }
            } catch (Exception e) {
                Log.e(K9.LOG_TAG, "getItem(" + position + "), but folder.messages.size() = " + mAdapter.messages.size(), e);
            }
            return null;
        }


        public View getItemView(int position, View convertView, ViewGroup parent) {
            MessageInfoHolder message = (MessageInfoHolder) getItem(position);
            View view;

            if ((convertView != null) && (convertView.getId() == R.layout.message_list_item)) {
                view = convertView;
            } else {
                if (mTouchView) {
                    view = mInflater.inflate(R.layout.message_list_item_touchable, parent, false);
                    view.setId(R.layout.message_list_item);
                } else {
                    view = mInflater.inflate(R.layout.message_list_item, parent, false);
                    view.setId(R.layout.message_list_item);
                }
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

                holder.flagged.setOnClickListener(flagClickListener);

                if (!mStars) {
                    holder.flagged.setVisibility(View.GONE);
                }

                if (mCheckboxes) {
                    holder.selected.setVisibility(View.VISIBLE);
                }

                if (holder.selected != null) {
                    holder.selected.setOnCheckedChangeListener(holder);
                }
                holder.subject.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mFontSizes.getMessageListSubject());
                holder.date.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mFontSizes.getMessageListDate());

                if (mTouchView) {
                    holder.preview.setLines(mPreviewLines);
                    holder.preview.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mFontSizes.getMessageListPreview());

                } else {
                    holder.from.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mFontSizes.getMessageListSender());
                }

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

                    ColorStateList color = holder.subject.getTextColors();
                    ColorStateList linkColor = holder.subject.getLinkTextColors();
                    str.setSpan(new TextAppearanceSpan(null, Typeface.NORMAL, mFontSizes.getMessageListSender(), color, linkColor),
                                0,
                                noSender.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                               );
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



            holder.chip.setBackgroundDrawable(message.message.getFolder().getAccount().generateColorChip().drawable());
            holder.chip.getBackground().setAlpha(message.read ? 127 : 255);
            view.getBackground().setAlpha(message.downloaded ? 0 : 127);

            if ((message.message.getSubject() == null) || message.message.getSubject().equals("")) {
                holder.subject.setText(getText(R.string.general_no_subject));
            } else {
                holder.subject.setText(message.message.getSubject());
            }

            int senderTypeface = message.read ? Typeface.NORMAL : Typeface.BOLD;
            if (holder.preview != null) {
                /*
                 * In the touchable UI, we have previews. Otherwise, we
                 * have just a "from" line.
                 * Because text views can't wrap around each other(?) we
                 * compose a custom view containing the preview and the
                 * from.
                 */

                holder.preview.setText(new SpannableStringBuilder(recipientSigil(message))
                                       .append(message.sender).append(" ").append(message.message.getPreview()),
                                       TextView.BufferType.SPANNABLE);
                Spannable str = (Spannable)holder.preview.getText();

                // Create a span section for the sender, and assign the correct font size and weight.
                ColorStateList color = holder.subject.getTextColors();
                ColorStateList linkColor = holder.subject.getLinkTextColors();
                str.setSpan(new TextAppearanceSpan(null, senderTypeface, mFontSizes.getMessageListSender(), color, linkColor),
                            0,
                            message.sender.length() + 1,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                           );
            } else {
                holder.from.setText(new SpannableStringBuilder(recipientSigil(message)).append(message.sender));

                holder.from.setTypeface(null, senderTypeface);
            }

            holder.date.setText(message.getDate(mMessageHelper));
            holder.subject.setCompoundDrawablesWithIntrinsicBounds(
                message.answered ? mAnsweredIcon : null, // left
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


        public View getFooterView(int position, View convertView, ViewGroup parent) {
            if (footerView == null) {
                footerView = mInflater.inflate(R.layout.message_list_item_footer, parent, false);
                if (mQueryString != null) {
                    footerView.setVisibility(View.GONE);
                }
                footerView.setId(R.layout.message_list_item_footer);
                FooterViewHolder holder = new FooterViewHolder();
                holder.progress = (ProgressBar)footerView.findViewById(R.id.message_list_progress);
                holder.progress.setIndeterminate(true);
                holder.main = (TextView)footerView.findViewById(R.id.main_text);
                footerView.setTag(holder);
            }

            FooterViewHolder holder = (FooterViewHolder)footerView.getTag();

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

            return footerView;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        public boolean isItemSelectable(int position) {
            if (position < mAdapter.messages.size()) {
                return true;
            } else {
                return false;
            }
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
        public int position = -1;

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (position != -1) {
                MessageInfoHolder message = (MessageInfoHolder) mAdapter.getItem(position);
                if (message.selected != isChecked) {
                    if (isChecked) {
                        mSelectedCount++;
                    } else if (mSelectedCount > 0) {
                        mSelectedCount--;
                    }

                    // We must set the flag before showing the buttons as the
                    // buttons text depends on what is selected.
                    message.selected = isChecked;
                    if (!mCheckboxes) {
                        if (isChecked) {
                            selected.setVisibility(View.VISIBLE);
                        } else {
                            selected.setVisibility(View.GONE);
                        }
                    }
                    toggleBatchButtons();
                }
            }
        }
    }


    private View getFooterView(ViewGroup parent) {
        if (mFooterView == null) {
            mFooterView = mInflater.inflate(R.layout.message_list_item_footer, parent, false);
            if (mQueryString != null) {
                mFooterView.setVisibility(View.GONE);
            }
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

    private void hideBatchButtons() {
        if (mBatchButtonArea.getVisibility() != View.GONE) {
            mBatchButtonArea.setVisibility(View.GONE);
            mBatchButtonArea.startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.footer_disappear));
        }
    }

    private void showBatchButtons() {
        if (mBatchButtonArea.getVisibility() != View.VISIBLE) {
            mBatchButtonArea.setVisibility(View.VISIBLE);
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.footer_appear);
            animation.setAnimationListener(this);
            mBatchButtonArea.startAnimation(animation);
        }
    }

    private void toggleBatchButtons() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (mSelectedCount < 0) {
                    mSelectedCount = 0;
                }

                int readButtonIconId;
                int flagButtonIconId;

                if (mSelectedCount == 0) {
                    readButtonIconId = R.drawable.ic_button_mark_read;
                    flagButtonIconId = R.drawable.ic_button_flag;
                    hideBatchButtons();
                } else {
                    boolean newReadState = computeBatchDirection(false);
                    if (newReadState) {
                        readButtonIconId = R.drawable.ic_button_mark_read;
                    } else {
                        readButtonIconId = R.drawable.ic_button_mark_unread;
                    }
                    boolean newFlagState = computeBatchDirection(true);
                    if (newFlagState) {
                        flagButtonIconId = R.drawable.ic_button_flag;
                    } else {
                        flagButtonIconId = R.drawable.ic_button_unflag;
                    }
                    showBatchButtons();
                }

                mBatchReadButton.setImageResource(readButtonIconId);
                mBatchFlagButton.setImageResource(flagButtonIconId);


            }
        });


    }

    static class FooterViewHolder {
        public ProgressBar progress;
        public TextView main;
    }


    private boolean computeBatchDirection(boolean flagged) {
        boolean newState = false;

        synchronized (mAdapter.messages) {
            for (MessageInfoHolder holder : mAdapter.messages) {
                if (holder.selected) {
                    if (flagged) {
                        if (!holder.flagged) {
                            newState = true;
                            break;
                        }
                    } else {
                        if (!holder.read) {
                            newState = true;
                            break;
                        }
                    }
                }
            }
        }
        return newState;
    }

    private boolean anySelected() {
        synchronized (mAdapter.messages) {
            for (MessageInfoHolder holder : mAdapter.messages) {
                if (holder.selected) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        boolean newState = false;
        List<Message> messageList = new ArrayList<Message>();
        List<MessageInfoHolder> removeHolderList = new ArrayList<MessageInfoHolder>();


        switch (v.getId()) {
        case R.id.reply:
            onReply(mCurrentMessageInfo);
            return;
        case R.id.reply_all:
            onReplyAll(mCurrentMessageInfo);
            return;
        case R.id.delete:
            onDelete(mCurrentMessageInfo);
            return;
        case R.id.forward:
            onForward(mCurrentMessageInfo);
            return;
        case R.id.archive:
            onRefile(mCurrentMessageInfo, mAccount.getArchiveFolderName());
            return;
        case R.id.spam:
            onRefile(mCurrentMessageInfo, mAccount.getSpamFolderName());
            return;
        case R.id.move:
            onMove(mCurrentMessageInfo);
            return;
        case R.id.next:
            onNext();
            return;
        case R.id.previous:
            onPrevious();
            return;
        case R.id.download:
            ((AttachmentView)v).saveFile();
            return;
        case R.id.show_pictures:
            mMessageView.setLoadPictures(true);
            return;
        case R.id.download_remainder:
            onDownloadRemainder();
            return;
        }


        if (v == mBatchDoneButton) {
            setAllSelected(false);
            return;
        }

        if (v == mBatchFlagButton) {
            newState = computeBatchDirection(true);
        } else {
            newState = computeBatchDirection(false);
        }

        synchronized (mAdapter.messages) {
            for (MessageInfoHolder holder : mAdapter.messages) {
                if (holder.selected) {
                    if (v == mBatchDeleteButton) {
                        removeHolderList.add(holder);
                    } else if (v == mBatchFlagButton) {
                        holder.flagged = newState;
                    } else if (v == mBatchReadButton) {
                        holder.read = newState;
                    }
                    messageList.add(holder.message);
                }
            }
        }
        mAdapter.removeMessages(removeHolderList);

        if (!messageList.isEmpty()) {
            if (v == mBatchDeleteButton) {
                mController.deleteMessages(messageList.toArray(EMPTY_MESSAGE_ARRAY), null);
                mSelectedCount = 0;
                toggleBatchButtons();
            } else {
                mController.setFlag(messageList.toArray(EMPTY_MESSAGE_ARRAY), (v == mBatchReadButton ? Flag.SEEN : Flag.FLAGGED), newState);
            }
        } else {
            // Should not happen
            Toast.makeText(this, R.string.no_message_seletected_toast, Toast.LENGTH_SHORT).show();
        }
        mHandler.sortMessages();
    }

    public void onAnimationEnd(Animation animation) {
    }

    public void onAnimationRepeat(Animation animation) {
    }

    public void onAnimationStart(Animation animation) {
    }



    private void setAllSelected(boolean isSelected) {
        mSelectedCount = 0;
        synchronized (mAdapter.messages) {
            for (MessageInfoHolder holder : mAdapter.messages) {
                holder.selected = isSelected;
                mSelectedCount += (isSelected ? 1 : 0);
            }
        }
        mAdapter.notifyDataSetChanged();
        toggleBatchButtons();
    }

    private void setSelected(MessageInfoHolder holder, boolean newState) {
        if (holder.selected != newState) {
            holder.selected = newState;
            mSelectedCount += (newState ? 1 : -1);
        }
        mAdapter.notifyDataSetChanged();
        toggleBatchButtons();
    }

    private void flagSelected(Flag flag, boolean newState) {
        List<Message> messageList = new ArrayList<Message>();
        synchronized (mAdapter.messages) {
            for (MessageInfoHolder holder : mAdapter.messages) {
                if (holder.selected) {
                    messageList.add(holder.message);
                    if (flag == Flag.SEEN) {
                        holder.read = newState;
                    } else if (flag == Flag.FLAGGED) {
                        holder.flagged = newState;
                    }
                }
            }
        }
        mController.setFlag(messageList.toArray(EMPTY_MESSAGE_ARRAY), flag, newState);
        mHandler.sortMessages();
    }

    private void deleteSelected() {
        List<Message> messageList = new ArrayList<Message>();
        List<MessageInfoHolder> removeHolderList = new ArrayList<MessageInfoHolder>();
        synchronized (mAdapter.messages) {
            for (MessageInfoHolder holder : mAdapter.messages) {
                if (holder.selected) {
                    removeHolderList.add(holder);
                    messageList.add(holder.message);
                }
            }
        }
        mAdapter.removeMessages(removeHolderList);

        mController.deleteMessages(messageList.toArray(EMPTY_MESSAGE_ARRAY), null);
        mSelectedCount = 0;
        toggleBatchButtons();
    }

    private void onMoveBatch() {
        if (!mController.isMoveCapable(mAccount)) {
            return;
        }

        synchronized (mAdapter.messages) {
            for (MessageInfoHolder holder : mAdapter.messages) {
                if (holder.selected) {
                    Message message = holder.message;
                    if (!mController.isMoveCapable(message)) {
                        Toast toast = Toast.makeText(this,
                                                     R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
                        toast.show();
                        return;
                    }
                }
            }
        }

        final Folder folder = mCurrentFolder.folder;
        final Intent intent = new Intent(this, ChooseFolder.class);
        intent.putExtra(ChooseFolder.EXTRA_ACCOUNT, mAccount.getUuid());
        intent.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, folder.getName());
        intent.putExtra(ChooseFolder.EXTRA_SEL_FOLDER, folder.getAccount().getLastSelectedFolderName());
        startActivityForResult(intent, ACTIVITY_CHOOSE_FOLDER_MOVE_BATCH);
    }

    private void onMoveChosenBatch(String folderName) {
        if (!mController.isMoveCapable(mAccount)) {
            return;
        }
        List<Message> messageList = new ArrayList<Message>();

        List<MessageInfoHolder> removeHolderList = new ArrayList<MessageInfoHolder>();
        synchronized (mAdapter.messages) {
            for (MessageInfoHolder holder : mAdapter.messages) {
                if (holder.selected) {
                    Message message = holder.message;
                    if (!mController.isMoveCapable(message)) {
                        Toast toast = Toast.makeText(this,
                                                     R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
                        toast.show();
                        return;
                    }
                    messageList.add(holder.message);
                    removeHolderList.add(holder);
                }
            }
        }
        mAdapter.removeMessages(removeHolderList);

        mController.moveMessages(mAccount, mCurrentFolder.name, messageList.toArray(EMPTY_MESSAGE_ARRAY), folderName, null);
        mSelectedCount = 0;
        toggleBatchButtons();
    }

    private void onArchiveBatch() {
        String folderName = mAccount.getArchiveFolderName();
        if (K9.FOLDER_NONE.equalsIgnoreCase(folderName)) {
            return;
        }
        onMoveChosenBatch(folderName);
    }

    private void onSpamBatch() {
        String folderName = mAccount.getSpamFolderName();
        if (K9.FOLDER_NONE.equalsIgnoreCase(folderName)) {
            return;
        }
        onMoveChosenBatch(folderName);
    }

    private void onCopyBatch() {
        if (!mController.isCopyCapable(mAccount)) {
            return;
        }

        synchronized (mAdapter.messages) {
            for (MessageInfoHolder holder : mAdapter.messages) {
                if (holder.selected) {
                    Message message = holder.message;
                    if (!mController.isCopyCapable(message)) {
                        Toast toast = Toast.makeText(this,
                                                     R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
                        toast.show();
                        return;
                    }
                }
            }
        }

        final Folder folder = mCurrentFolder.folder;
        final Intent intent = new Intent(this, ChooseFolder.class);
        intent.putExtra(ChooseFolder.EXTRA_ACCOUNT, mAccount.getUuid());
        intent.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, folder.getName());
        intent.putExtra(ChooseFolder.EXTRA_SEL_FOLDER, folder.getAccount().getLastSelectedFolderName());
        startActivityForResult(intent, ACTIVITY_CHOOSE_FOLDER_COPY_BATCH);
    }

    private void onCopyChosenBatch(String folderName) {
        if (!mController.isCopyCapable(mAccount)) {
            return;
        }

        List<Message> messageList = new ArrayList<Message>();
        synchronized (mAdapter.messages) {
            for (MessageInfoHolder holder : mAdapter.messages) {
                if (holder.selected) {
                    Message message = holder.message;
                    if (!mController.isCopyCapable(message)) {
                        Toast toast = Toast.makeText(this,
                                                     R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
                        toast.show();
                        return;
                    }
                    messageList.add(holder.message);
                }
            }
        }
        mController.copyMessages(mAccount, mCurrentFolder.name, messageList.toArray(EMPTY_MESSAGE_ARRAY), folderName, null);
    }

    protected void onAccountUnavailable() {
        finish();
        // TODO inform user about account unavailability using Toast
        Accounts.listAccounts(this);
    }

// utilities


    /** Wrapper-function taking a KeyCode.
     * A complete KeyStroke is DOWN and UP Action on a key! */
    private void simulateKeystroke(int KeyCode) {
        doInjectKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyCode));
        doInjectKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyCode));
    }

    /** This function actually handles the KeyStroke-Injection. */
    private void doInjectKeyEvent(KeyEvent keyEvent) {

        dispatchKeyEvent(keyEvent);

    }


// XXX do we actually need this? why? - presumably for API v ??? compatibility
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean ret = false;
        if (KeyEvent.ACTION_DOWN == event.getAction()) {
            ret = onKeyDown(event.getKeyCode(), event);
        }
        if (!ret) {
            ret = super.dispatchKeyEvent(event);
        }
        return ret;
    }



// Message Buttons
//
//
//

    private void setupBatchButtonViews() {
        mBatchButtonArea = findViewById(R.id.batch_button_area);
        mBatchReadButton = (ImageButton) findViewById(R.id.batch_read_button);
        mBatchReadButton.setOnClickListener(this);
        mBatchDeleteButton = (ImageButton) findViewById(R.id.batch_delete_button);
        mBatchDeleteButton.setOnClickListener(this);
        mBatchFlagButton = (ImageButton) findViewById(R.id.batch_flag_button);
        mBatchFlagButton.setOnClickListener(this);
        mBatchDoneButton = (ImageButton) findViewById(R.id.batch_done_button);

        mBatchDoneButton.setOnClickListener(this);
    }

    private void setupButtonViews() {
        setOnClickListener(R.id.from);
        setOnClickListener(R.id.reply);
        setOnClickListener(R.id.reply_all);
        setOnClickListener(R.id.delete);
        setOnClickListener(R.id.forward);
        setOnClickListener(R.id.next);
        setOnClickListener(R.id.previous);
        setOnClickListener(R.id.archive);
        setOnClickListener(R.id.move);
        setOnClickListener(R.id.spam);
        // To show full header
        setOnClickListener(R.id.header_container);
        setOnClickListener(R.id.show_pictures);
        setOnClickListener(R.id.download_remainder);


        staticButtons();
    }

    private void setupDisplayMessageButtons() {
        mDelete.setEnabled(true);
        mNext.setEnabled(mNextMessage != null);
        mPrevious.setEnabled(mPreviousMessage != null);
        // If moving isn't support at all, then all of them must be disabled anyway.
        findViewById(R.id.move_buttons).setVisibility(View.GONE);
        if (mController.isMoveCapable(mAccount)) {
            // Only enable the button if the Archive folder is not the current folder and not NONE.
            mArchive.setEnabled(!mCurrentMessageInfo.folder.name.equals(mAccount.getArchiveFolderName()) &&
                                !K9.FOLDER_NONE.equalsIgnoreCase(mAccount.getArchiveFolderName()));
            // Only enable the button if the Spam folder is not the current folder and not NONE.
            mSpam.setEnabled(!mCurrentMessageInfo.folder.name.equals(mAccount.getSpamFolderName()) &&
                             !K9.FOLDER_NONE.equalsIgnoreCase(mAccount.getSpamFolderName()));
            mMove.setEnabled(true);
        } else {
            disableMoveButtons();
        }
    }

    private void staticButtons() {
        mNext = findViewById(R.id.next);
        mPrevious = findViewById(R.id.previous);
        mDelete = findViewById(R.id.delete);
        mArchive = findViewById(R.id.archive);
        mMove = findViewById(R.id.move);
        mSpam = findViewById(R.id.spam);
    }


    private void disableButtons() {
        mMessageView.setLoadPictures(false);
        disableMoveButtons();
        mNext.setEnabled(false);
        mPrevious.setEnabled(false);
        mDelete.setEnabled(false);
    }

    private void disableMoveButtons() {
        mArchive.setEnabled(false);
        mMove.setEnabled(false);
        mSpam.setEnabled(false);
    }

    private void setOnClickListener(int viewCode) {
        View thisView = findViewById(viewCode);
        if (thisView != null) {
            thisView.setOnClickListener(this);
        }
    }
// This REALLY should be in MessageCryptoView
    public void onDecryptDone(PgpData pgpData) {
        // TODO: this might not be enough if the orientation was changed while in APG,
        // sometimes shows the original encrypted content
        mMessageView.loadBodyFromText(mAccount.getCryptoProvider(), mPgpData, mMessage, mPgpData.getDecryptedData(), "text/plain");
    }

}
