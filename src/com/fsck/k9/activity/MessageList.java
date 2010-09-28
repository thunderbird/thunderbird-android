package com.fsck.k9.activity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.Toast;
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
import com.fsck.k9.grouping.DateMessageGrouper;
import com.fsck.k9.grouping.MessageGroup;
import com.fsck.k9.grouping.MessageGrouper;
import com.fsck.k9.grouping.MessageInfo;
import com.fsck.k9.grouping.SenderMessageGrouper;
import com.fsck.k9.grouping.SingletonMessageGrouper;
import com.fsck.k9.grouping.thread.ThreadMessageGrouper;
import com.fsck.k9.helper.UiThrottler;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.LocalStore;
import com.fsck.k9.mail.store.LocalStore.LocalFolder;
import com.fsck.k9.mail.store.LocalStore.LocalMessage;

/**
 * MessageList is the primary user interface for the program. This Activity
 * shows a list of messages.
 * From this Activity the user can perform all standard message operations.
 */
public class MessageList
        extends K9Activity
 implements OnClickListener,
        ExpandableListView.OnGroupExpandListener, ExpandableListView.OnGroupCollapseListener
{

    protected static enum StateRestorationStatus
    {
        NONE,
        PLANNED,
        PENDING,
        READY;
    }
    protected class Listener extends ActivityListener
    {
        @Override
        public void synchronizeMailboxStarted(Account account, String folder)
        {
            super.synchronizeMailboxStarted(account, folder);
    
            if (updateForMe(account, folder))
            {
                mHandler.progress(true);
                mHandler.folderLoading(folder, true);
            }
            mHandler.refreshTitle();
        }

        @Override
        public void synchronizeMailboxHeadersProgress(Account account, String folder, int completed, int total)
        {
            super.synchronizeMailboxHeadersProgress(account,folder,completed, total);
            mHandler.refreshTitle();
        }

        @Override
        public void synchronizeMailboxHeadersFinished(Account account, String folder,
                int total, int completed)
        {
            super.synchronizeMailboxHeadersFinished(account,folder, total, completed);
            mHandler.refreshTitle();
        }

        @Override
        public void synchronizeMailboxFinished(Account account, String folder,
                                               int totalMessagesInMailbox, int numNewMessages)
        {
            super.synchronizeMailboxFinished(account, folder, totalMessagesInMailbox, numNewMessages);
    
            if (updateForMe(account, folder))
            {
                mHandler.progress(false);
                mHandler.folderLoading(folder, false);
                mHandler.sortMessages();
            }
            mHandler.refreshTitle();
        }

        @Override
        public void synchronizeMailboxFailed(Account account, String folder, String message)
        {
            super.synchronizeMailboxFailed(account, folder, message);
    
            if (updateForMe(account, folder))
            {
                mHandler.progress(false);
                mHandler.folderLoading(folder, false);
                mHandler.sortMessages();
            }
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

        @Override
        public void synchronizeMailboxProgress(Account account, String folder, int completed, int total)
        {
            super.synchronizeMailboxProgress(account, folder, completed, total);
            mHandler.refreshTitle();
        }

        @Override
        public void synchronizeMailboxAddOrUpdateMessage(Account account, String folder, Message message)
        {
            addOrUpdateMessages(account, folder, Collections.singletonList(message), true);
        }

        @Override
        public void synchronizeMailboxRemovedMessage(Account account, String folder,Message message)
        {
            MessageInfoHolder holder = mStore.getMessage(message);
            if (holder == null)
            {
                Log.w(K9.LOG_TAG, "Got callback to remove non-existent message with UID " + message.getUid());
            }
            else
            {
                mHandler.removeMessages(Collections.singletonList(holder));
            }
        }

        @Override
        public void listLocalMessagesStarted(Account account, String folder)
        {
            if ((mSearchMode && folder == null) ||
                    (account != null && account.equals(mAccount))
               )
            {
                mHandler.progress(true);
                if (folder != null)
                {
                    mHandler.folderLoading(folder, true);
                }
            }
        }

        @Override
        public void listLocalMessagesFailed(Account account, String folder, String message)
        {
            listLocalMessagesFinished(account, folder);
        }

        @Override
        public void listLocalMessagesFinished(Account account, String folder)
        {
            if ((mSearchMode && folder == null) ||
                    (account != null && account.equals(mAccount)))
            {
                mHandler.sortMessages();
                mHandler.progress(false);
                if (folder != null)
                {
                    mHandler.folderLoading(folder, false);
                }
            }
        }

        @Override
        public void listLocalMessagesRemoveMessage(Account account, String folder,Message message)
        {
            MessageInfoHolder holder = mStore.getMessage(message);
            if (holder != null)
            {
                mHandler.removeMessages(Collections.singletonList(holder));
            }
        }

        @Override
        public void listLocalMessagesAddMessages(Account account, String folder, List<Message> messages)
        {
            addOrUpdateMessages(account, folder, messages, false);
        }

        @Override
        public void listLocalMessagesUpdateMessage(Account account, String folder, Message message)
        {
            addOrUpdateMessages(account, folder, Collections.singletonList(message), false);
        }

        @Override
        public void searchStats(AccountStats stats)
        {
            mUnreadMessageCount = stats.unreadMessageCount;
            mHandler.refreshTitle();
        }

        @Override
        public void folderStatusChanged(Account account, String folder, int unreadMessageCount)
        {
            super.folderStatusChanged(account, folder, unreadMessageCount);
            if (updateForMe(account, folder))
            {
                mUnreadMessageCount = unreadMessageCount;
                mHandler.refreshTitle();
            }
        }

        @Override
        public void pendingCommandsProcessing(Account account)
        {
            super.pendingCommandsProcessing(account);
            mHandler.refreshTitle();
        }

        @Override
        public void pendingCommandsFinished(Account account)
        {
            super.pendingCommandsFinished(account);
            mHandler.refreshTitle();
        }

        @Override
        public void pendingCommandStarted(Account account, String commandTitle)
        {
            super.pendingCommandStarted(account, commandTitle);
            mHandler.refreshTitle();
        }

        @Override
        public void pendingCommandCompleted(Account account, String commandTitle)
        {
            super.pendingCommandCompleted(account, commandTitle);
            mHandler.refreshTitle();
        }

        @Override
        public void messageUidChanged(Account account, String folder, String oldUid, String newUid)
        {
            MessageReference ref = new MessageReference();
            ref.accountUuid = account.getUuid();
            ref.folderName = folder;
            ref.uid = oldUid;
    
            MessageInfoHolder holder = mStore.getMessage(ref);
            if (holder != null)
            {
                holder.uid = newUid;
                holder.message.setUid(newUid);
            }
        }
    }

    /**
     * Reverses the result of a {@link Comparator}.
     * 
     * @param <T>
     */
    public static class ReverseComparator<T> implements Comparator<T>
    {
        private Comparator<T> mDelegate;

        /**
         * @param delegate
         *            Never <code>null</code>.
         */
        public ReverseComparator(final Comparator<T> delegate)
        {
            mDelegate = delegate;
        }

        @Override
        public int compare(final T object1, final T object2)
        {
            // arg1 & 2 are mixed up, this is done on purpose
            return mDelegate.compare(object2, object1);
        }

    }

    /**
     * Chains comparator to find a non-0 result.
     * 
     * @param <T>
     */
    public static class ComparatorChain<T> implements Comparator<T>
    {

        private List<Comparator<T>> mChain;

        /**
         * @param chain
         *            Comparator chain. Never <code>null</code>.
         */
        public ComparatorChain(final List<Comparator<T>> chain)
        {
            mChain = chain;
        }

        @Override
        public int compare(T object1, T object2)
        {
            int result = 0;
            for (final Comparator<T> comparator : mChain)
            {
                result = comparator.compare(object1, object2);
                if (result != 0)
                {
                    break;
                }
            }
            return result;
        }
        
    }

    public static class AttachmentComparator implements Comparator<MessageInfoHolder>
    {

        @Override
        public int compare(MessageInfoHolder object1, MessageInfoHolder object2)
        {
            return (object1.hasAttachments ? 0 : 1) - (object2.hasAttachments ? 0 : 1);
        }

    }

    public static class FlaggedComparator implements Comparator<MessageInfoHolder>
    {

        @Override
        public int compare(MessageInfoHolder object1, MessageInfoHolder object2)
        {
            return (object1.flagged ? 0 : 1) - (object2.flagged ? 0 : 1);
        }

    }

    public static class UnreadComparator implements Comparator<MessageInfoHolder>
    {

        @Override
        public int compare(MessageInfoHolder object1, MessageInfoHolder object2)
        {
            return (object1.read ? 1 : 0) - (object2.read ? 1 : 0);
        }

    }

    public static class SenderComparator implements Comparator<MessageInfoHolder>
    {

        @Override
        public int compare(MessageInfoHolder object1, MessageInfoHolder object2)
        {
            return object1.compareCounterparty.toLowerCase().compareTo(object2.compareCounterparty.toLowerCase());
        }

    }

    public static class DateComparator implements Comparator<MessageInfoHolder>
    {

        @Override
        public int compare(MessageInfoHolder object1, MessageInfoHolder object2)
        {
            return object1.compareDate.compareTo(object2.compareDate);
        }

    }

    public static class SubjectComparator implements Comparator<MessageInfoHolder>
    {

        @Override
        public int compare(MessageInfoHolder arg0, MessageInfoHolder arg1)
        {
            // XXX doesn't respect the Comparator contract since it alters the compared object
            if (arg0.compareSubject == null)
            {
                arg0.compareSubject = Utility.stripSubject(arg0.subject);
            }
            if (arg1.compareSubject == null)
            {
                arg1.compareSubject = Utility.stripSubject(arg1.subject);
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

    private ExpandableListView mListView;

    private View mFooterView;

    private boolean mTouchView = true;

    private MessageListAdapter mAdapter;

    private FolderInfoHolder mCurrentFolder;

    private LayoutInflater mInflater;

    private MessagingController mController;

    private Account mAccount;
    private int mUnreadMessageCount = 0;

    private ItemListener itemListener = new ItemListener();

    /**
     * Stores the name of the folder that we want to open as soon as possible
     * after load.
     */
    private String mFolderName;

    private boolean mSearchMode;
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

    private boolean mStars = true;
    private boolean mCheckboxes = true;
    private int mSelectedCount = 0;

    private View mBatchButtonArea;
    private ImageButton mBatchReadButton;
    private ImageButton mBatchDeleteButton;
    private ImageButton mBatchFlagButton;
    private ImageButton mBatchDoneButton;

    private FontSizes mFontSizes = K9.getFontSizes();

    /**
     * <code>true</code> if the activity is in state restoration mode
     */
    private AtomicReference<StateRestorationStatus> restore = new AtomicReference<StateRestorationStatus>(StateRestorationStatus.NONE);

    private Bundle mState = null;

    /**
     * Remember the selection to be consistent between menu display and menu item
     * selection
     */
    private MessageInfoHolder mSelectedMessage = null;

    /**
     * Remember the selection to be consistent between menu display and menu item
     * selection
     */
    private MessageGroup<MessageInfoHolder> mSelectedGroup = null;

    /**
     * Relevant messages for the current context when we have to remember the
     * chosen messages between user interactions (eg. Selecting a folder for
     * move operation)
     */
    private List<MessageInfoHolder> mActiveMessages;

    private ProgressDialog mProgressDialog;

    /**
     * Manage the backend store and the UI component (ListAdapter) to make sure
     * they act accordingly.
     */
    public class MessageListHandler
    {

        private UiThrottler<Void> mThrottler;

        public MessageListHandler()
        {
            mThrottler = new UiThrottler<Void>(MessageList.this, new Callable<Void>()
            {
                @Override
                public Void call()
                {
                    mStore.synchronizeGroups();
                    return null;
                }
            }, null); // not setting Executor now as we want to integrate into Activity onResume/onPause
            mThrottler.setCoolDownDuration(200L);
            mThrottler.setPostExecute(new Runnable()
            {
                @Override
                public void run()
                {
                    synchronized (mStore.mGroups)
                    {
                        // duplicate the list in order to have a "thread stable" list in the UI thread
                        // (Android framework doesn't expect it to mutate)
                        mAdapter.mUiGroups = new ArrayList<MessageGroup<MessageInfoHolder>>(
                                mStore.mGroups);
                    }
                    // trigger the actual list refresh
                    mAdapter.notifyDataSetChanged();

                    // auto expand groups at initial display
                    restoreExpandState();
                }
            });
            mThrottler.setCompleted(new Runnable()
            {
                @Override
                public void run()
                {
                    mAdapter.mGroupingInProgress = false;
                    updateFooterView(mFooterView);
                    if (mAdapter.mGroupLessMode)
                    {
                        // making sure we expand the sole group in groupless mode
                        expandAll();
                    }
                    mAdapter.synchronizeFastScroll();

                    if (restore.compareAndSet(StateRestorationStatus.PENDING, StateRestorationStatus.READY))
                    {
                        MessageList.this.restoreListState();
                    }
                }
            });
        }

        public void removeMessages(final List<MessageInfoHolder> messages)
        {
            final List<MessageInfoHolder> toRemove = new ArrayList<MessageInfoHolder>();

            for (final MessageInfoHolder message : messages)
            {
                if (message != null)
                {
                    if (mFolderName == null || (message.folder != null && message.folder.name.equals(mFolderName)))
                    {
                        if (message.selected && mSelectedCount > 0)
                        {
                            mSelectedCount--;
                        }
                        toRemove.add(message);
                    }
                }
            }

            mStore.removeMessages(toRemove);

            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    resetUnreadCountOnThread();

                    synchronizeDisplay();
                    toggleBatchButtons();
                }
            });
        }

        public void addMessages(final List<MessageInfoHolder> messages)
        {
            final boolean wasEmpty = mStore.messages.isEmpty();

            final List<MessageInfoHolder> toAdd = new ArrayList<MessageInfoHolder>();
            for (final MessageInfoHolder message : messages)
            {
                if (mFolderName == null || (message.folder != null && message.folder.name.equals(mFolderName)))
                {
                    toAdd.add(message);
                }
            }

            mStore.addMessages(toAdd);

            if (restore.get() == StateRestorationStatus.NONE)
            {
                runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        if (wasEmpty)
                        {
                            mListView.setSelection(0);
                        }
                        resetUnreadCountOnThread();

                        synchronizeDisplay();
                    }
                });
            }
        }

        public void sortMessages()
        {
            mStore.sortMessages();
            synchronizeDisplay();
        }

        private void resetUnreadCount()
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    resetUnreadCountOnThread();
                }
            });
        }

        private void resetUnreadCountOnThread()
        {
            if (mSearchMode)
            {
                int unreadCount = 0;
                synchronized (mStore.messages)
                {
                    for (MessageInfoHolder holder : mStore.messages)
                    {
                        unreadCount += holder.read ? 0 : 1;
                    }
                }
                mUnreadMessageCount = unreadCount;
                refreshTitleOnThread();
            }
        }

        public void folderLoading(String folder, boolean loading)
        {
            if (mCurrentFolder != null && mCurrentFolder.name.equals(folder))
            {
                mCurrentFolder.loading = loading;
            }
        }

        private void refreshTitle()
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    refreshTitleOnThread();
                }
            });
        }

        private void refreshTitleOnThread()
        {
            setWindowTitle();
            setWindowProgress();
        }

        private void setWindowProgress()
        {
            int level = Window.PROGRESS_END;

            if (mCurrentFolder != null && mCurrentFolder.loading && mStore.mListener.getFolderTotal() > 0)
            {
                int divisor = mStore.mListener.getFolderTotal();
                if (divisor != 0)
                {
                    level = (Window.PROGRESS_END / divisor) * (mStore.mListener.getFolderCompleted()) ;
                    if (level > Window.PROGRESS_END)
                    {
                        level = Window.PROGRESS_END;
                    }
                }
            }

            getWindow().setFeatureInt(Window.FEATURE_PROGRESS, level);
        }

        private void setWindowTitle()
        {
            String displayName;

            if (mFolderName != null)
            {
                displayName  = mFolderName;

                if (K9.INBOX.equalsIgnoreCase(displayName))
                {
                    displayName = getString(R.string.special_mailbox_name_inbox);
                }

                String dispString = mStore.mListener.formatHeader(MessageList.this, getString(R.string.message_list_title, mAccount.getDescription(), displayName), mUnreadMessageCount, getTimeFormat());
                setTitle(dispString);
            }
            else if (mSearchMode)
            {
                if (mTitle != null)
                {
                    String dispString = mStore.mListener.formatHeader(MessageList.this, mTitle, mUnreadMessageCount, getTimeFormat());
                    setTitle(dispString);
                }
                else
                {
                    setTitle(getString(R.string.search_results) + ": "+ mQueryString);
                }
            }
        }

        public void progress(final boolean progress)
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    showProgressIndicator(progress);
                }
            });
        }

        /**
         * Refreshes the message list to reflect the backend store content.
         */
        public void synchronizeDisplay()
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    mAdapter.mGroupingInProgress = mCurrentFolder != null && mAccount != null && mCurrentFolder.loading;
                    mThrottler.attempt();
                }
            });
        }

        public void restoreListState()
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    MessageList.this.restoreListState();
                }
            });
        }
    }

    public static void actionHandleFolder(Context context, Account account, String folder)
    {
        Intent intent = actionHandleFolderIntent(context,account,folder);
        context.startActivity(intent);
    }

    public static Intent actionHandleFolderIntent(Context context, Account account, String folder)
    {
        Intent intent = new Intent(context, MessageList.class);
        intent.putExtra(EXTRA_ACCOUNT, account.getUuid());

        if (folder != null)
        {
            intent.putExtra(EXTRA_FOLDER, folder);
        }
        return intent;
    }

    public static void actionHandle(Context context, String title, String queryString, boolean integrate, Flag[] flags, Flag[] forbiddenFlags)
    {
        Intent intent = new Intent(context, MessageList.class);
        intent.putExtra(EXTRA_QUERY, queryString);
        if (flags != null)
        {
            intent.putExtra(EXTRA_QUERY_FLAGS, Utility.combine(flags, ','));
        }
        if (forbiddenFlags != null)
        {
            intent.putExtra(EXTRA_FORBIDDEN_FLAGS, Utility.combine(forbiddenFlags, ','));
        }
        intent.putExtra(EXTRA_INTEGRATE, integrate);
        intent.putExtra(EXTRA_TITLE, title);
        context.startActivity(intent);
    }

    public static void actionHandle(Context context, String title, SearchSpecification searchSpecification)
    {
        Intent intent = new Intent(context, MessageList.class);
        intent.putExtra(EXTRA_QUERY, searchSpecification.getQuery());
        if (searchSpecification.getRequiredFlags() != null)
        {
            intent.putExtra(EXTRA_QUERY_FLAGS, Utility.combine(searchSpecification.getRequiredFlags(), ','));
        }
        if (searchSpecification.getForbiddenFlags() != null)
        {
            intent.putExtra(EXTRA_FORBIDDEN_FLAGS, Utility.combine(searchSpecification.getForbiddenFlags(), ','));
        }
        intent.putExtra(EXTRA_INTEGRATE, searchSpecification.isIntegrate());
        intent.putExtra(EXTRA_ACCOUNT_UUIDS, searchSpecification.getAccountUuids());
        intent.putExtra(EXTRA_FOLDER_NAMES, searchSpecification.getFolderNames());
        intent.putExtra(EXTRA_TITLE, title);
        context.startActivity(intent);
    }

    @Override
    public void onGroupExpand(final int groupPosition)
    {
        mAdapter.synchronizeFastScroll();
        mAdapter.mAutoExpanded.add(mAdapter.getGroupId(groupPosition));
    }

    @Override
    public void onGroupCollapse(final int groupPosition)
    {
        mAdapter.synchronizeFastScroll();
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mInflater = getLayoutInflater();
        initializeLayout();
        initialize();
    }

    @Override
    public void onNewIntent(Intent intent)
    {
        setIntent(intent); // onNewIntent doesn't autoset our "internal" intent
        initialize();
    }

    protected void initialize()
    {
        final Intent intent = getIntent();

        // Only set "touchable" when we're first starting up the activity.
        // Otherwise we get force closes when the user toggles it midstream.
        mTouchView = K9.messageListTouchable();

        String accountUuid = intent.getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);
        mFolderName = intent.getStringExtra(EXTRA_FOLDER);
        mQueryString = intent.getStringExtra(EXTRA_QUERY);

        mSearchMode = mQueryString != null;

        String queryFlags = intent.getStringExtra(EXTRA_QUERY_FLAGS);
        if (queryFlags != null)
        {
            String[] flagStrings = queryFlags.split(",");
            mQueryFlags = new Flag[flagStrings.length];
            for (int i = 0; i < flagStrings.length; i++)
            {
                mQueryFlags[i] = Flag.valueOf(flagStrings[i]);
            }
        }
        String forbiddenFlags = intent.getStringExtra(EXTRA_FORBIDDEN_FLAGS);
        if (forbiddenFlags != null)
        {
            String[] flagStrings = forbiddenFlags.split(",");
            mForbiddenFlags = new Flag[flagStrings.length];
            for (int i = 0; i < flagStrings.length; i++)
            {
                mForbiddenFlags[i] = Flag.valueOf(flagStrings[i]);
            }
        }
        mIntegrate = intent.getBooleanExtra(EXTRA_INTEGRATE, false);
        mAccountUuids = intent.getStringArrayExtra(EXTRA_ACCOUNT_UUIDS);
        mFolderNames = intent.getStringArrayExtra(EXTRA_FOLDER_NAMES);
        mTitle = intent.getStringExtra(EXTRA_TITLE);

        // Take the initial folder into account only if we are *not* restoring
        // the activity already.
        if (mFolderName == null && !mSearchMode)
        {
            mFolderName = mAccount.getAutoExpandFolderName();
        }

        mAdapter = new MessageListAdapter();
        final List<MessageInfoHolder> previousData = getLastNonConfigurationInstance();

        if (previousData != null)
        {
            //noinspection unchecked
            mStore.messages.addAll(previousData);
        }

        if (mFolderName != null)
        {
            mCurrentFolder = getFolder(mFolderName, mAccount);
        }

        mController = MessagingController.getInstance(getApplication());

        final boolean footerViewEnabled = !mSearchMode;
        if (footerViewEnabled)
        {
            mListView.addFooterView(mFooterView, null, true);
        }
        else
        {
            mListView.removeFooterView(mFooterView);
        }

        mListView.setAdapter(mAdapter);

        if (footerViewEnabled)
        {
            updateFooterView(mFooterView);
        }

        restore.set(StateRestorationStatus.NONE);
        mState = null;
        mAdapter.mAutoExpanded.clear();
        mProgressDialog = null;
    }

    @Override
    public void onPause()
    {
        super.onPause();
        mController.removeListener(mStore.mListener);

        // prevent any throttled UI processing (we're stopping!)
        // (don't set it to null since it needed if a processing is
        // occuring)
        mHandler.mThrottler.getScheduledExecutorService().shutdown();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        saveListState();
    }

    public void saveListState()
    {
        // put the activity in restoration mode for future loading
        restore.set(StateRestorationStatus.PLANNED);

        mState = new Bundle();
        mState.putInt(EXTRA_LIST_POSITION, mListView.isInTouchMode() ? mListView.getFirstVisiblePosition() : mListView.getSelectedItemPosition());

        final int groupCount = mAdapter.getGroupCount();
        for (int i = 0; i < groupCount; i++)
        {
            if (mListView.isGroupExpanded(i))
            {
                mAdapter.mAutoExpanded.remove(mAdapter.getGroupId(i));
            }
        }
    }

    public void restoreListState()
    {
        if (!restore.compareAndSet(StateRestorationStatus.READY, StateRestorationStatus.NONE))
        {
            return;
        }
        mListView.setSelection(mState.getInt(EXTRA_LIST_POSITION, 0));
        mHandler.resetUnreadCountOnThread();
        mProgressDialog.dismiss();
        mProgressDialog = null;
        mState = null;

    }

    /**
     * 
     */
    protected void restoreExpandState()
    {
        final int groupCount = mAdapter.getGroupCount();
        for (int i = 0; i < groupCount; i++)
        {
            if (!mListView.isGroupExpanded(i)
                    && !mAdapter.mAutoExpanded.contains(mAdapter.getGroupId(i)))
            {
                mListView.expandGroup(i);
            }
        }
    }

    /**
     * On resume we refresh messages for the folder that is currently open.
     * This guarantees that things like unread message count and read status
     * are updated.
     */
    @Override
    public void onResume()
    {
        super.onResume();

        mStars = K9.messageListStars();
        mCheckboxes = K9.messageListCheckboxes();

        mStore.sortType = mController.getSortType();
        mStore.sortAscending = mController.isSortAscending(mStore.sortType);
        mStore.sortDateAscending = mController.isSortAscending(SORT_TYPE.SORT_DATE);

        if (mHandler.mThrottler.getScheduledExecutorService() == null
                || mHandler.mThrottler.getScheduledExecutorService().isShutdown())
        {
            mHandler.mThrottler.setScheduledExecutorService(Executors.newScheduledThreadPool(1));
        }

        mController.addListener(mStore.mListener);
        mStore.messages.clear();
        if (restore.get() != StateRestorationStatus.PLANNED)
        {
            restore.set(StateRestorationStatus.NONE);
            mHandler.synchronizeDisplay();
        }
        else
        {
            restoreExpandState();
            // TODO localization
            mProgressDialog = ProgressDialog.show(this, "", "Loading, please wait...", true);
        }

        if (mFolderName != null)
        {
            mController.listLocalMessages(mAccount, mFolderName, new MessagingListener()
            {
                @Override
                public void listLocalMessagesFinished(Account account, String folder)
                {
                    restore.compareAndSet(StateRestorationStatus.PLANNED, StateRestorationStatus.PENDING);
                }
                @Override
                public void listLocalMessagesFailed(Account account, String folder, String message)
                {
                    listLocalMessagesFinished(account, folder);
                }
            });
            mController.notifyAccountCancel(this, mAccount);

            MessagingController.getInstance(getApplication()).notifyAccountCancel(this, mAccount);

            mController.getFolderUnreadMessageCount(mAccount, mFolderName, mStore.mListener);
        }
        else if (mSearchMode)
        {
            mController.searchLocalMessages(mAccountUuids, mFolderNames, null, mQueryString, mIntegrate, mQueryFlags, mForbiddenFlags, mStore.mListener);
        }

        mHandler.refreshTitle();
    }

    private void initializeLayout()
    {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.message_list);

        mListView = (ExpandableListView) findViewById(R.id.message_list);
        mListView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_INSET);
        mListView.setLongClickable(true);
        mListView.setFastScrollEnabled(true);
        mListView.setScrollingCacheEnabled(true);
        mListView.setOnChildClickListener(itemListener);
        mListView.setOnTouchListener(itemListener);
        mListView.setOnGroupCollapseListener(this);
        mListView.setOnGroupExpandListener(this);

        registerForContextMenu(mListView);

        mBatchButtonArea = findViewById(R.id.batch_button_area);
        mBatchReadButton = (ImageButton) findViewById(R.id.batch_read_button);
        mBatchReadButton.setOnClickListener(this);
        mBatchDeleteButton = (ImageButton) findViewById(R.id.batch_delete_button);
        mBatchDeleteButton.setOnClickListener(this);
        mBatchFlagButton = (ImageButton) findViewById(R.id.batch_flag_button);
        mBatchFlagButton.setOnClickListener(this);
        mBatchDoneButton = (ImageButton) findViewById(R.id.batch_done_button);

        mBatchDoneButton.setOnClickListener(this);

        mFooterView = createFooterView();
    }

    protected View createFooterView()
    {
        final View view = mInflater.inflate(R.layout.message_list_item_footer, mListView, false);

        view.setId(R.layout.message_list_item_footer);

        final FooterViewHolder holder = new FooterViewHolder();

        holder.progress = (ProgressBar) view.findViewById(R.id.message_list_progress);
        holder.main = (TextView) view.findViewById(R.id.main_text);

        holder.progress.setIndeterminate(true);

        view.setTag(holder);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id)
            {
                if (mCurrentFolder != null)
                {
                    // XXX worker thread invocation
                    mController.loadMoreMessages(mAccount, mFolderName, mStore.mListener);
                }
            }
        });

        return view;
    }

    private void updateFooterView(final View footerView)
    {
        // there's no footer view in search mode
        if (mSearchMode)
        {
            return;
        }

        if (footerView == null)
        {
            // can happen when configration is changed (screen orientation)
            return;
        }
        FooterViewHolder holder = (FooterViewHolder) footerView.getTag();

        if (mCurrentFolder != null && mAccount != null)
        {
            if (mCurrentFolder.loading || mAdapter.mGroupingInProgress)
            {
                holder.main.setText(getString(R.string.status_loading_more));
                holder.progress.setVisibility(ProgressBar.VISIBLE);
            }
            else
            {
                if (!mCurrentFolder.lastCheckFailed)
                {
                    holder.main.setText(String.format(getString(R.string.load_more_messages_fmt), mAccount.getDisplayCount()));
                }
                else
                {
                    holder.main.setText(getString(R.string.status_loading_more_failed));
                }
                holder.progress.setVisibility(ProgressBar.INVISIBLE);
            }
        }
        else
        {
            holder.progress.setVisibility(ProgressBar.INVISIBLE);
        }
    }

    @Override
    protected void onDestroy()
    {
        mFooterView = null;

        super.onDestroy();
    }

    @Override
    public List<MessageInfoHolder> onRetainNonConfigurationInstance()
    {
        return new ArrayList<MessageInfoHolder>(mStore.messages);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<MessageInfoHolder> getLastNonConfigurationInstance()
    {
        return (List<MessageInfoHolder>) super.getLastNonConfigurationInstance();
    }

    @Override
    public void onBackPressed()
    {
        // This will be called either automatically for you on 2.0
        // or later, or by the code above on earlier versions of the
        // platform.
        if (K9.manageBack())
        {
            if (!mSearchMode)
            {
                onShowFolderList();
            }
            else
            {
                onAccounts();
            }
        }
        else
        {
            finish();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (
            // XXX TODO - when we go to android 2.0, uncomment this
            // android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ECLAIR &&
            keyCode == KeyEvent.KEYCODE_BACK
            && event.getRepeatCount() == 0
        )
        {
            // Take care of calling this method on earlier versions of
            // the platform where it doesn't exist.
            onBackPressed();
            return true;
        }

        // Shortcuts that work no matter what is selected
        switch (keyCode)
        {

            // messagelist is actually a K9Activity, not a K9ListActivity
            // This saddens me greatly, but to support volume key navigation
            // in MessageView, we implement this bit of wrapper code
            case KeyEvent.KEYCODE_VOLUME_UP:
            {
                if (K9.useVolumeKeysForListNavigationEnabled())
                {
                    int currentPosition = mListView.getSelectedItemPosition();
                    if (currentPosition == AdapterView.INVALID_POSITION || mListView.isInTouchMode())
                    {
                        currentPosition = mListView.getFirstVisiblePosition();
                    }
                    if (currentPosition > 0)
                    {
                        mListView.setSelection(currentPosition - 1);
                    }
                    return true;
                }
                return false;
            }
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            {
                if (K9.useVolumeKeysForListNavigationEnabled())
                {
                    int currentPosition = mListView.getSelectedItemPosition();
                    if (currentPosition == AdapterView.INVALID_POSITION || mListView.isInTouchMode())
                    {
                        currentPosition = mListView.getFirstVisiblePosition();
                    }
                    
                    if (currentPosition < mListView.getCount())
                    {
                        mListView.setSelection(currentPosition + 1);
                    }
                    return true;
                }
                return false;
            }
            case KeyEvent.KEYCODE_DPAD_LEFT:
            {
                if (mBatchButtonArea.hasFocus())
                {
                    return false;
                }
                else
                {
                    return true;
                }
            }
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            {
                if (mBatchButtonArea.hasFocus())
                {
                    return false;
                }
                else
                {
                    return true;
                }
            }
            case KeyEvent.KEYCODE_C:
            {
                onCompose();
                return true;
            }
            case KeyEvent.KEYCODE_Q:
            {
                onShowFolderList();
                return true;
            }
            case KeyEvent.KEYCODE_O:
            {
                onCycleSort();
                return true;
            }
            case KeyEvent.KEYCODE_I:
            {
                onToggleSortAscending();
                return true;
            }
            case KeyEvent.KEYCODE_H:
            {
                Toast toast = Toast.makeText(this, R.string.message_list_help_key, Toast.LENGTH_LONG);
                toast.show();
                return true;
            }
        }

        int position = mListView.getSelectedItemPosition();
        try
        {
            if (position >= 0)
            {
                final Object item = mListView.getItemAtPosition(position);
                if (!(item instanceof MessageInfoHolder))
                {
                    return false;
                }
                final MessageInfoHolder message = (MessageInfoHolder) item;

                final List<MessageInfoHolder> selection = getSelectionFromMessage(message);

                if (message != null)
                {
                    switch (keyCode)
                    {
                        case KeyEvent.KEYCODE_DEL:
                        {
                            onDelete(selection);
                            return true;
                        }
                        case KeyEvent.KEYCODE_S:
                        {
                            setSelected(selection, !message.selected);
                            return true;
                        }
                        case KeyEvent.KEYCODE_D:
                        {
                            onDelete(selection);
                            return true;
                        }
                        case KeyEvent.KEYCODE_F:
                        {
                            onForward(message);
                            return true;
                        }
                        case KeyEvent.KEYCODE_A:
                        {
                            onReplyAll(message);
                            return true;
                        }
                        case KeyEvent.KEYCODE_R:
                        {
                            onReply(message);
                            return true;
                        }
                        case KeyEvent.KEYCODE_G:
                        {
                            setFlag(selection, Flag.FLAGGED, !message.flagged);
                            return true;
                        }
                        case KeyEvent.KEYCODE_M:
                        {
                            onMove(selection);
                            return true;
                        }
                        case KeyEvent.KEYCODE_V:
                        {
                            onArchive(selection);
                            return true;
                        }
                        case KeyEvent.KEYCODE_Y:
                        {
                            onCopy(selection);
                            return true;
                        }
                        case KeyEvent.KEYCODE_Z:
                        {
                            setFlag(selection, Flag.SEEN, !message.read);
                            return true;
                        }
                    }
                }
            }
        }
        finally
        {
            return super.onKeyDown(keyCode, event);
        }

    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        // Swallow these events too to avoid the audible notification of a volume change
        if (K9.useVolumeKeysForListNavigationEnabled())
        {
            if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP) || (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN))
            {
                if (K9.DEBUG)
                    Log.v(K9.LOG_TAG, "Swallowed key up.");
                return true;
            }
        }
        return super.onKeyUp(keyCode,event);
    }


    private void onOpenMessage(MessageInfoHolder message)
    {
        if (message.folder.name.equals(message.message.getFolder().getAccount().getDraftsFolderName()))
        {
            MessageCompose.actionEditDraft(this, message.message.getFolder().getAccount(), message.message);
        }
        else
        {
            // Need to get the list before the sort starts
            final List<MessageReference> messageRefs = new ArrayList<MessageReference>(mStore.messages.size());

            for (final MessageGroup<MessageInfoHolder> group : mAdapter.mUiGroups)
            {
                for (final MessageInfo<MessageInfoHolder> info : group.getMessages())
                {
                    final MessageInfoHolder holder = info.getTag();
                    final MessageReference reference = holder.message.makeMessageReference();
                    messageRefs.add(reference);
                }
            }

            MessageReference ref = message.message.makeMessageReference();
            Log.i(K9.LOG_TAG, "MessageList sending message " + ref);

            MessageView.actionView(this, ref, messageRefs);
        }

        /*
         * We set read=true here for UI performance reasons. The actual value
         * will get picked up on the refresh when the Activity is resumed but
         * that may take a second or so and we don't want this to show and
         * then go away. I've gone back and forth on this, and this gives a
         * better UI experience, so I am putting it back in.
         */
        if (!message.read)
        {
            message.read = true;
        }
    }

    private void onAccounts()
    {
        Accounts.listAccounts(this);
        finish();
    }

    private void onShowFolderList()
    {
        FolderList.actionHandleAccount(this, mAccount);
        finish();
    }

    private void onCompose()
    {
        if (mSearchMode)
        {
            /*
             * If we have a query string, we don't have an account to let
             * compose start the default action.
             */
            MessageCompose.actionCompose(this, null);
        }
        else
        {
            MessageCompose.actionCompose(this, mAccount);
        }
    }

    private void onEditPrefs()
    {
        Prefs.actionPrefs(this);
    }

    private void onEditAccount()
    {
        AccountSettings.actionSettings(this, mAccount);
    }

    private void changeSort(SORT_TYPE newSortType)
    {
        if (mStore.sortType == newSortType)
        {
            onToggleSortAscending();
        }
        else
        {
            mStore.sortType = newSortType;
            mController.setSortType(mStore.sortType);
            mStore.sortAscending = mController.isSortAscending(mStore.sortType);
            mStore.sortDateAscending = mController.isSortAscending(SORT_TYPE.SORT_DATE);
            reSort();
        }
    }

    private void reSort()
    {
        int toastString = mStore.sortType.getToast(mStore.sortAscending);

        Toast toast = Toast.makeText(this, toastString, Toast.LENGTH_SHORT);
        toast.show();

        mHandler.sortMessages();
    }

    private void onCycleSort()
    {
        SORT_TYPE[] sorts = SORT_TYPE.values();
        int curIndex = 0;

        for (int i = 0; i < sorts.length; i++)
        {
            if (sorts[i] == mStore.sortType)
            {
                curIndex = i;
                break;
            }
        }

        curIndex++;

        if (curIndex == sorts.length)
        {
            curIndex = 0;
        }

        changeSort(sorts[curIndex]);
    }

    private void onToggleSortAscending()
    {
        mController.setSortAscending(mStore.sortType, !mStore.sortAscending);

        mStore.sortAscending = mController.isSortAscending(mStore.sortType);
        mStore.sortDateAscending = mController.isSortAscending(SORT_TYPE.SORT_DATE);

        reSort();
    }

    private void onDelete(final List<MessageInfoHolder> holders)
    {
        final Message[] messages = new Message[holders.size()];
        int i = 0;
        for (final Iterator<MessageInfoHolder> iterator = holders.iterator(); iterator.hasNext(); i++)
        {
            final MessageInfoHolder holder = iterator.next();
            messages[i] = holder.message;
        }
        mHandler.removeMessages(holders);
        mController.deleteMessages(messages, null);
    }

    /**
     * Display an Toast message if any message isn't synchronized
     * 
     * @param holders
     *            Never <code>null</code>.
     * @param move
     *            <code>true</code> to check move availability,
     *            <code>false</code> to check the copy availability
     * 
     * @return <code>true</code> if operation is possible
     */
    protected boolean checkCopyOrMovePossible(final List<MessageInfoHolder> holders, final boolean move)
    {
        boolean first = true;
        for (final MessageInfoHolder holder : holders)
        {
            final Message message = holder.message;
            if (first)
            {
                first = false;
                final Account account = message.getFolder().getAccount();
                if ((move && !mController.isMoveCapable(account))
                        || (!move && !mController.isCopyCapable(account)))
                {
                    return false;
                }
            }
            if ((move && !mController.isMoveCapable(message))
                    || (!move && !mController.isCopyCapable(message)))
            {
                final Toast toast = Toast.makeText(this,
                        R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
                toast.show();
                return false;
            }
        }
        return true;
    }


    /**
     * @param holders
     *            Never <code>null</code>.
     */
    private void onArchive(final List<MessageInfoHolder> holders)
    {
        // TODO one should separate messages by account and call move afterwards (because each account might have a specific Archive folder name)
        move(holders, holders.get(0).message.getFolder().getAccount().getArchiveFolderName());
    }

    /**
     * @param holders
     *            Never <code>null</code>.
     */
    private void onSpam(final List<MessageInfoHolder> holders)
    {
        // TODO one should separate messages by account and call move afterwards (because each account might have a specific Spam folder name)
        move(holders, holders.get(0).message.getFolder().getAccount().getSpamFolderName());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode != RESULT_OK)
        {
            return;
        }

        switch (requestCode)
        {
            case ACTIVITY_CHOOSE_FOLDER_MOVE:
            case ACTIVITY_CHOOSE_FOLDER_COPY:
            {
                if (data == null)
                {
                    return;
                }

                final String destFolderName = data.getStringExtra(ChooseFolder.EXTRA_NEW_FOLDER);

                if (destFolderName != null)
                {
                    final List<MessageInfoHolder> holders = mActiveMessages;

                    mActiveMessages = null; // don't need it any more

                    final Account account = holders.get(0).message.getFolder().getAccount();

                    account.setLastSelectedFolderName(destFolderName);

                    switch (requestCode)
                    {
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

    private void onReply(MessageInfoHolder holder)
    {
        MessageCompose.actionReply(this, holder.message.getFolder().getAccount(), holder.message, false, null);
    }

    private void onReplyAll(MessageInfoHolder holder)
    {
        MessageCompose.actionReply(this, holder.message.getFolder().getAccount(), holder.message, true, null);
    }

    private void onForward(MessageInfoHolder holder)
    {
        MessageCompose.actionForward(this, holder.message.getFolder().getAccount(), holder.message, null);
    }

    private void onMarkAllAsRead(final Account account, final String folder)
    {
        showDialog(DIALOG_MARK_ALL_AS_READ);
    }

    private void onExpunge(final Account account, String folderName)
    {
        mController.expunge(account, folderName, null);
    }

    @Override
    public Dialog onCreateDialog(int id)
    {
        switch (id)
        {
            case DIALOG_MARK_ALL_AS_READ:
                return createMarkAllAsReadDialog();
        }

        return super.onCreateDialog(id);
    }

    @Override
    public void onPrepareDialog(int id, Dialog dialog)
    {
        switch (id)
        {
            case DIALOG_MARK_ALL_AS_READ:
            {
                if (mCurrentFolder != null)
                {
                    ((AlertDialog)dialog).setMessage(getString(R.string.mark_all_as_read_dlg_instructions_fmt,
                                                     mCurrentFolder.displayName));
                }
                break;
            }
            default:
            {
                super.onPrepareDialog(id, dialog);
            }
        }
    }

    private Dialog createMarkAllAsReadDialog()
    {
        return new AlertDialog.Builder(this)
               .setTitle(R.string.mark_all_as_read_dlg_title)
               .setMessage(getString(R.string.mark_all_as_read_dlg_instructions_fmt,
                                     mCurrentFolder.displayName))
               .setPositiveButton(R.string.okay_action, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                dismissDialog(DIALOG_MARK_ALL_AS_READ);

                try
                {
                    mController.markAllMessagesRead(mAccount, mCurrentFolder.name);

                    synchronized (mStore.messages)
                    {
                        for (MessageInfoHolder holder : mStore.messages)
                        {
                            holder.read = true;
                        }
                    }
                    mHandler.sortMessages();
                }
                catch (Exception e)
                {
                    // Ignore
                }
            }
        })
               .setNegativeButton(R.string.cancel_action, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                dismissDialog(DIALOG_MARK_ALL_AS_READ);
            }
        })
               .create();
    }

    private void checkMail(Account account, String folderName)
    {
        mController.synchronizeMailbox(account, folderName, mStore.mListener, null);
        sendMail(account);
    }

    private void sendMail(Account account)
    {
        mController.sendPendingMessages(account, mStore.mListener);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        final List<MessageInfoHolder> selection = getSelectionFromCheckboxes();
        int itemId = item.getItemId();
        switch (itemId)
        {
            case R.id.compose:
            {
                onCompose();
                return true;
            }
            case R.id.accounts:
            {
                onAccounts();
                return true;
            }
            case R.id.set_sort_date:
            {
                changeSort(SORT_TYPE.SORT_DATE);
                return true;
            }
            case R.id.set_sort_subject:
            {
                changeSort(SORT_TYPE.SORT_SUBJECT);
                return true;
            }
            case R.id.set_sort_sender:
            {
                changeSort(SORT_TYPE.SORT_SENDER);
                return true;
            }
            case R.id.set_sort_flag:
            {
                changeSort(SORT_TYPE.SORT_FLAGGED);
                return true;
            }
            case R.id.set_sort_unread:
            {
                changeSort(SORT_TYPE.SORT_UNREAD);
                return true;
            }
            case R.id.set_sort_attach:
            {
                changeSort(SORT_TYPE.SORT_ATTACHMENT);
                return true;
            }
            case R.id.set_group_by_none:
                mStore.mMessageGrouper = new SingletonMessageGrouper();
                mAdapter.mGroupLessMode = true;
                reSort();
                return true;
            case R.id.set_group_by_thread:
                mStore.mMessageGrouper = new ThreadMessageGrouper();
                mAdapter.mGroupLessMode = false;
                reSort();
                return true;
            case R.id.set_group_by_sender:
                mStore.mMessageGrouper = new SenderMessageGrouper();
                mAdapter.mGroupLessMode = false;
                reSort();
                return true;
            case R.id.set_group_by_date:
                mStore.mMessageGrouper = new DateMessageGrouper(this);
                mAdapter.mGroupLessMode = false;
                reSort();
                return true;
            case R.id.expand_all:
                expandAll();
                return true;
            case R.id.collapse_all:
                collapseAll();
                return true;
            case R.id.select_all:
            case R.id.batch_select_all:
            {
                setAllSelected(true);
                toggleBatchButtons();
                return true;
            }
            case R.id.batch_deselect_all:
            {
                setAllSelected(false);
                toggleBatchButtons();
                return true;
            }
            case R.id.batch_delete_op:
            {
                onDelete(selection);
                return true;
            }
            case R.id.batch_mark_read_op:
            {
                setFlag(selection, Flag.SEEN, true);
                return true;
            }
            case R.id.batch_mark_unread_op:
            {
                setFlag(selection, Flag.SEEN, false);
                return true;
            }
            case R.id.batch_flag_op:
            {
                setFlag(selection, Flag.FLAGGED, true);
                return true;
            }
            case R.id.batch_unflag_op:
            {
                setFlag(selection, Flag.FLAGGED, false);
                return true;
            }
            case R.id.settings:
            {
                if (!mSearchMode)
                {
                    break;
                }

                /*
                 * Fall-through in search results view. Otherwise a sub-menu
                 * with only one option would be opened.
                 */
            }
            case R.id.app_settings:
            {
                onEditPrefs();
                return true;
            }
        }

        if (mSearchMode)
        {
            // None of the options after this point are "safe" for search results
            //TODO: This is not true for "unread" and "starred" searches in regular folders
            return false;
        }

        switch (itemId)
        {
            case R.id.check_mail:
            {
                if (mFolderName != null)
                {
                    checkMail(mAccount, mFolderName);
                }
                return true;
            }
            case R.id.send_messages:
            {
                sendMail(mAccount);
                return true;
            }
            case R.id.list_folders:
            {
                onShowFolderList();
                return true;
            }
            case R.id.mark_all_as_read:
            {
                if (mFolderName != null)
                {
                    onMarkAllAsRead(mAccount, mFolderName);
                }
                return true;
            }
            case R.id.folder_settings:
            {
                if (mFolderName != null)
                {
                    FolderSettings.actionSettings(this, mAccount, mFolderName);
                }
                return true;
            }
            case R.id.account_settings:
            {
                onEditAccount();
                return true;
            }
            case R.id.batch_copy_op:
            {
                onCopy(selection);
                return true;
            }
            case R.id.batch_archive_op:
            {
                onArchive(selection);
                return true;
            }
            case R.id.batch_spam_op:
            {
                onSpam(selection);
                return true;
            }
            case R.id.batch_move_op:
            {
                onMove(selection);
                return true;
            }
            case R.id.expunge:
            {
                if (mCurrentFolder != null)
                {
                    onExpunge(mAccount, mCurrentFolder.name);
                }
                return true;
            }
            default:
            {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    private final int[] batch_ops = { R.id.batch_copy_op, R.id.batch_delete_op, R.id.batch_flag_op,
                                      R.id.batch_unflag_op, R.id.batch_mark_read_op, R.id.batch_mark_unread_op,
                                      R.id.batch_archive_op, R.id.batch_spam_op, R.id.batch_move_op,
                                      R.id.batch_select_all, R.id.batch_deselect_all
                                    };

    private void setOpsState(Menu menu, boolean state, boolean enabled)
    {
        for (int id : batch_ops)
        {
            menu.findItem(id).setVisible(state);
            menu.findItem(id).setEnabled(enabled);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        boolean anySelected = anySelected();

        menu.findItem(R.id.select_all).setVisible(! anySelected);
        menu.findItem(R.id.batch_ops).setVisible(anySelected);

        setOpsState(menu, true, anySelected);

        if (mSearchMode)
        {
            menu.findItem(R.id.mark_all_as_read).setVisible(false);
            menu.findItem(R.id.list_folders).setVisible(false);
            menu.findItem(R.id.expunge).setVisible(false);
            menu.findItem(R.id.batch_archive_op).setVisible(false);
            menu.findItem(R.id.batch_spam_op).setVisible(false);
            menu.findItem(R.id.batch_move_op).setVisible(false);
            menu.findItem(R.id.batch_copy_op).setVisible(false);
            menu.findItem(R.id.check_mail).setVisible(false);
            menu.findItem(R.id.send_messages).setVisible(false);
        }
        else
        {
            if (mCurrentFolder != null && mCurrentFolder.outbox)
            {
                menu.findItem(R.id.check_mail).setVisible(false);
            }
            else
            {
                menu.findItem(R.id.send_messages).setVisible(false);
            }

            if (mCurrentFolder != null && K9.ERROR_FOLDER_NAME.equals(mCurrentFolder.name))
            {
                menu.findItem(R.id.expunge).setVisible(false);
            }
            if (K9.FOLDER_NONE.equalsIgnoreCase(mAccount.getArchiveFolderName()))
            {
                menu.findItem(R.id.batch_archive_op).setVisible(false);
            }
            if (K9.FOLDER_NONE.equalsIgnoreCase(mAccount.getSpamFolderName()))
            {
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

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.message_list_option, menu);

        return true;
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item)
    {
        final MessageInfoHolder holder = mSelectedMessage;
        final MessageGroup<MessageInfoHolder> group = mSelectedGroup;

        // don't need them anymore
        mSelectedMessage = null;
        mSelectedGroup = null;

        if (holder != null && group == null)
        {
            return onContextItemSelectedForMessage(item, holder);
        }
        else if (holder == null && group != null)
        {
            return onContextItemSelectedForGroup(item, group);
        }
        else
        {
            return super.onContextItemSelected(item);
        }

    }

    /**
     * @param item
     *            Never <code>null</code>.
     * @param holder
     *            Never <code>null</code>.
     * @return See {@link Activity#onContextItemSelected(MenuItem)}
     */
    private boolean onContextItemSelectedForMessage(MenuItem item, final MessageInfoHolder holder)
    {
        final List<MessageInfoHolder> selection = getSelectionFromMessage(holder);
        switch (item.getItemId())
        {
            case R.id.open:
            {
                onOpenMessage(holder);
                break;
            }
            case R.id.select:
            {
                setSelected(selection, true);
                break;
            }
            case R.id.deselect:
            {
                setSelected(selection, false);
                break;
            }
            case R.id.delete:
            {
                onDelete(selection);
                break;
            }
            case R.id.reply:
            {
                onReply(holder);
                break;
            }
            case R.id.reply_all:
            {
                onReplyAll(holder);
                break;
            }
            case R.id.forward:
            {
                onForward(holder);
                break;
            }
            case R.id.mark_as_read:
            {
                setFlag(selection, Flag.SEEN, !holder.read);
                break;
            }
            case R.id.flag:
            {
                setFlag(selection, Flag.FLAGGED, !holder.flagged);
                break;
            }
            case R.id.archive:
            {
                onArchive(selection);
                break;
            }
            case R.id.spam:
            {
                onSpam(selection);
                break;
            }
            case R.id.move:
            {
                onMove(selection);
                break;
            }
            case R.id.copy:
            {
                onCopy(selection);
                break;
            }
            case R.id.send_alternate:
            {
                onSendAlternate(mAccount, holder);
                break;
            }
            case R.id.same_sender:
            {
                MessageList.actionHandle(MessageList.this,
                                         "From "+holder.sender, holder.senderAddress, true,
                                         null, null);
                break;
            }
        }
        return super.onContextItemSelected(item);
    }

    /**
     * @param item
     *            Never <code>null</code>.
     * @param group
     *            Never <code>null</code>.
     * @return See {@link Activity#onContextItemSelected(MenuItem)}
     */
    private boolean onContextItemSelectedForGroup(final MenuItem item,
            final MessageGroup<MessageInfoHolder> group)
    {
        final List<MessageInfoHolder> selection = getSelectionFromGroup(group);
        final int itemId = item.getItemId();
        switch (itemId)
        {
            case R.id.expand:
            {
                int i = findGroupPosition(group);
                if (i >= 0)
                {
                    mListView.expandGroup(i);
                }
                return true;
            }
            case R.id.collapse:
            {
                int i = findGroupPosition(group);
                if (i >= 0)
                {
                    mListView.collapseGroup(i);
                }
                return true;
            }
            case R.id.group_delete:
                onDelete(selection);
                return true;
            case R.id.group_select:
            case R.id.group_deselect:
                setSelected(selection, itemId == R.id.group_select);
                if (itemId == R.id.group_select)
                {
                    // display selection to user
                    int i = findGroupPosition(group);
                    if (i >= 0)
                    {
                        mListView.expandGroup(i);
                    }
                }
                return true;
            case R.id.group_flag:
            case R.id.group_unflag:
                setFlag(selection, Flag.FLAGGED, itemId == R.id.group_flag);
                return true;
            case R.id.group_mark_as_read:
            case R.id.group_mark_as_unread:
                setFlag(selection, Flag.SEEN, itemId == R.id.group_mark_as_read);
                return true;
            case R.id.group_copy:
                displayFolderChoice(ACTIVITY_CHOOSE_FOLDER_COPY, selection.get(0).message.getFolder(), null, selection);
                return true;
            case R.id.group_move:
                displayFolderChoice(ACTIVITY_CHOOSE_FOLDER_MOVE, selection.get(0).message.getFolder(), null, selection);
                return true;
            case R.id.group_archive:
                onArchive(selection);
                return true;
            case R.id.group_spam:
                onSpam(selection);
                return true;
            default:
                break;
        }
        return super.onContextItemSelected(item);
    }

    /**
     * @param group
     *            Never <code>null</code>.
     * @return -1 if not found
     */
    private int findGroupPosition(final MessageGroup<MessageInfoHolder> group)
    {
        final int groupCount = mAdapter.getGroupCount();
        if (groupCount > 0)
        {
            int i = 0;
            for (MessageGroup<MessageInfoHolder> otherGroup; i < groupCount; i++)
            {
                otherGroup = mAdapter.getGroup(i);
                if (group.getId() == otherGroup.getId())
                {
                    break;
                }
            }
            return i;
        }
        else
        {
            return -1;
        }
    }

    private void collapseAll()
    {
        final int groupCount = mAdapter.getGroupCount();
        for (int i = 0; i < groupCount; i++)
        {
            if (mListView.isGroupExpanded(i))
            {
                mListView.collapseGroup(i);
            }
        }
    }

    private void expandAll()
    {
        final int groupCount = mAdapter.getGroupCount();
        for (int i = 0; i < groupCount; i++)
        {
            if (!mListView.isGroupExpanded(i))
            {
                mListView.expandGroup(i);
            }
        }
    }

    public void onSendAlternate(Account account, MessageInfoHolder holder)
    {
        mController.sendAlternate(this, account, holder.message);
    }

    public void showProgressIndicator(boolean status)
    {
        setProgressBarIndeterminateVisibility(status);
        ProgressBar bar = (ProgressBar)mListView.findViewById(R.id.message_list_progress);
        if (bar == null)
        {
            return;
        }

        bar.setIndeterminate(true);
        if (status)
        {
            bar.setVisibility(ProgressBar.VISIBLE);
        }
        else
        {
            bar.setVisibility(ProgressBar.INVISIBLE);
        }
    }

    /**
     * Handle touch/click events on list items
     */
    private class ItemListener implements View.OnTouchListener,
            ExpandableListView.OnChildClickListener
    {
        /**
         * When switching from ListView to ExpandableListView, the onChildClick
         * method is still invoked, this flag allow to ignore subsequent "click"
         * behavior when sliding
         */
        private boolean prevent = false;

        private GestureDetector gestureDetector = new GestureDetector(new SimpleOnGestureListener()
        {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
            {
                if (e2 == null || e1 == null)
                {
                    return true;
                }

                float deltaX = e2.getX() - e1.getX(), deltaY = e2.getY() - e1.getY();

                boolean movedAcross = (Math.abs(deltaX) > Math.abs(deltaY * 4));
                boolean steadyHand = (Math.abs(deltaX / deltaY) > 2);

                if (movedAcross && steadyHand)
                {
                    boolean selected = (deltaX > 0);
                    int position = mListView.pointToPosition((int) e1.getX(), (int) e1.getY());

                    if (position != AdapterView.INVALID_POSITION)
                    {
                        final Object item = mListView.getItemAtPosition(position);
                        if (item instanceof MessageInfoHolder)
                        {
                            MessageInfoHolder msgInfoHolder = (MessageInfoHolder) item;

                            if (msgInfoHolder != null && msgInfoHolder.selected != selected)
                            {
                                msgInfoHolder.selected = selected;
                                mSelectedCount += (selected ? 1 : -1);
                                mHandler.synchronizeDisplay();
                                toggleBatchButtons();
                                prevent = true;
                            }
                        }
                    }
                }

                return false;
            }
        });

        @Override
        public boolean onTouch(View v, MotionEvent event)
        {
            if (gestureDetector.onTouchEvent(event))
            {
                return true;
            }
            return false;
        }

        @Override
        public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                int childPosition, long id)
        {
            if (prevent)
            {
                prevent = false;
                return true;
            }

            final MessageInfoHolder message = mAdapter.getChild(groupPosition, childPosition);
            if (mSelectedCount > 0)
            {
                // In multiselect mode make sure that clicking on the item results
                // in toggling the 'selected' checkbox.
                final List<MessageInfoHolder> selection = getSelectionFromMessage(message);
                setSelected(selection, !message.selected);
                return true;
            }
            else
            {
                onOpenMessage(message);
                return true;
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
        // reset remembered selection
        mSelectedGroup = null;
        mSelectedMessage = null;

        // hide by default
        menu.setGroupVisible(R.id.message_group, false);
        menu.setGroupVisible(R.id.single_message, false);

        if (menuInfo instanceof ExpandableListView.ExpandableListContextMenuInfo)
        {
            final ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
            final long packedPosition = info.packedPosition;
            final int packedPositionType = ExpandableListView.getPackedPositionType(packedPosition);

            switch (packedPositionType)
            {
                case ExpandableListView.PACKED_POSITION_TYPE_GROUP:
                    // group
                    final int groupPosition = ExpandableListView
                            .getPackedPositionGroup(packedPosition);
                    mSelectedGroup = mAdapter.getGroup(groupPosition);

                    if (mSelectedGroup == null)
                    {
                        break;
                    }

                    onCreateContextMenuForGroup(menu, v, menuInfo, mSelectedGroup, groupPosition);

                    break;
                case ExpandableListView.PACKED_POSITION_TYPE_CHILD:
                    // message
                    final int packedPositionChild = ExpandableListView
                            .getPackedPositionChild(packedPosition);
                    final int packedPositionGroup = ExpandableListView
                            .getPackedPositionGroup(packedPosition);

                    // remember which message was originally selected, in case the list changes while the
                    // dialog is up
                    mSelectedMessage = mAdapter.getChild(packedPositionGroup, packedPositionChild);

                    if (mSelectedMessage == null)
                    {
                        break;
                    }

                    onCreateContextMenuForMessage(menu, mSelectedMessage);

                    break;
            }
        }
    }

    /**
     * @param menu
     *            Never <code>null</code>.
     * @param message
     *            Never <code>null</code>.
     * @see Activity#onCreateContextMenu(ContextMenu, View, ContextMenuInfo)
     */
    private void onCreateContextMenuForMessage(final ContextMenu menu, final MessageInfoHolder message)
    {
        getMenuInflater().inflate(R.menu.message_list_context, menu);

        menu.setHeaderTitle((CharSequence) message.subject);

        menu.setGroupVisible(R.id.single_message, true);

        if (message.read)
        {
            menu.findItem(R.id.mark_as_read).setTitle(R.string.mark_as_unread_action);
        }

        if (message.flagged)
        {
            menu.findItem(R.id.flag).setTitle(R.string.unflag_action);
        }

        Account account = message.message.getFolder().getAccount();
        if (!mController.isCopyCapable(account))
        {
            menu.findItem(R.id.copy).setVisible(false);
        }

        if (!mController.isMoveCapable(account))
        {
            menu.findItem(R.id.move).setVisible(false);
            menu.findItem(R.id.archive).setVisible(false);
            menu.findItem(R.id.spam).setVisible(false);
        }

        if (K9.FOLDER_NONE.equalsIgnoreCase(account.getArchiveFolderName()))
        {
            menu.findItem(R.id.archive).setVisible(false);
        }
        if (K9.FOLDER_NONE.equalsIgnoreCase(account.getSpamFolderName()))
        {
            menu.findItem(R.id.spam).setVisible(false);
        }

        if (message.selected)
        {
            menu.findItem(R.id.select).setVisible(false);
            menu.findItem(R.id.deselect).setVisible(true);
        }
        else
        {
            menu.findItem(R.id.select).setVisible(true);
            menu.findItem(R.id.deselect).setVisible(false);
        }
    }

    /**
     * @param menu
     *            Never <code>null</code>.
     * @param v
     *            Never <code>null</code>.
     * @param menuInfo
     *            Never <code>null</code>.
     * @param group
     *            Never <code>null</code>.
     * @param groupPosition
     * @see Activity#onCreateContextMenu(ContextMenu, View, ContextMenuInfo)
     */
    private void onCreateContextMenuForGroup(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo,
            final MessageGroup<MessageInfoHolder> group, final int groupPosition)
    {
        getMenuInflater().inflate(R.menu.message_list_context, menu);
        menu.setHeaderTitle(group.getSubject());
        menu.setGroupVisible(R.id.message_group, true);

        if (mListView.isGroupExpanded(groupPosition))
        {
            menu.findItem(R.id.expand).setVisible(false);
            menu.findItem(R.id.collapse).setVisible(true);
        }
        else
        {
            menu.findItem(R.id.expand).setVisible(true);
            menu.findItem(R.id.collapse).setVisible(false);
        }
        boolean unread = false;
        boolean read = false;
        boolean unflagged = false;
        boolean flagged = false;
        boolean selected = false;
        boolean unselected = false;
        for (final MessageInfo<MessageInfoHolder> info : group.getMessages())
        {
            final MessageInfoHolder holder = info.getTag();
            if (!read && holder.read)
            {
                read = true;
            }
            else if (!unread && !holder.read)
            {
                unread = true;
            }
            if (!flagged && holder.flagged)
            {
                flagged = true;
            }
            else if (!unflagged && !holder.flagged)
            {
                unflagged = true;
            }
            if (!selected && holder.selected)
            {
                selected = true;
            }
            else if (!unselected && !holder.selected)
            {
                unselected = true;
            }
        }
        menu.findItem(R.id.group_mark_as_read).setVisible(unread);
        menu.findItem(R.id.group_mark_as_unread).setVisible(read);
        menu.findItem(R.id.group_flag).setVisible(unflagged);
        menu.findItem(R.id.group_unflag).setVisible(flagged);
        menu.findItem(R.id.group_select).setVisible(unselected);
        menu.findItem(R.id.group_deselect).setVisible(selected);
        // TODO don't use mAccount for when the group contains messages from multiple accounts
        menu.findItem(R.id.group_archive).setVisible(
                !K9.FOLDER_NONE.equalsIgnoreCase(mAccount.getArchiveFolderName()));
        menu.findItem(R.id.group_spam).setVisible(
                !K9.FOLDER_NONE.equalsIgnoreCase(mAccount.getSpamFolderName()));
    }

    private static final Path GROUP_CHIP_PATH = new Path();
    private static final Path LAST_CHIP_PATH = new Path();

    static
    {
        GROUP_CHIP_PATH.moveTo(10, 0);
        GROUP_CHIP_PATH.quadTo(0, 0, 0, 10);
        GROUP_CHIP_PATH.lineTo(10, 10);
        GROUP_CHIP_PATH.close();

        LAST_CHIP_PATH.quadTo(0, 10, 10, 10);
        LAST_CHIP_PATH.lineTo(10, 0);
        LAST_CHIP_PATH.close();
    }

    /**
     * Intermediate backend storage used by MessageList.
     * 
     * <p>
     * Takes care of providing messages for the UI components (ListAdapter) and
     * manage message list data manipulation (add, remove, sort).
     * </p>
     * 
     * <p>
     * Modification to this backend storage doesn't trigger an UI refresh.
     * </p>
     * 
     * <p>
     * Should not include UI API calls and should not be used from the UI thread.
     * </p>
     */
    public static class MessageListStore
    {

        /**
         * Maps a {@link SORT_TYPE} to a {@link Comparator} implementation.
         */
        private static final Map<SORT_TYPE, Comparator<MessageInfoHolder>> SORT_COMPARATORS;

        static
        {
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

        private final List<MessageInfoHolder> messages = Collections.synchronizedList(new ArrayList<MessageInfoHolder>());

        private final List<MessageGroup<MessageInfoHolder>> mGroups = Collections
                .synchronizedList(new ArrayList<MessageGroup<MessageInfoHolder>>());

        private final ActivityListener mListener;

        private MessageGrouper mMessageGrouper = new ThreadMessageGrouper();

        public MessageListStore(final ActivityListener listener)
        {
            this.mListener = listener;
        }
        public MessageInfoHolder getMessage(Message message)
        {
            return getMessage(message.makeMessageReference());
        }

        // XXX TODO - make this not use a for loop
        public MessageInfoHolder getMessage(MessageReference messageReference)
        {
            synchronized (messages)
            {
                for (MessageInfoHolder holder : messages)
                {
                    /*
                     * 2010-06-21 - cketti
                     * Added null pointer check. Not sure what's causing 'holder'
                     * to be null. See log provided in issue 1749, comment #15.
                     *
                     * Please remove this comment once the cause was found and the
                     * bug(?) fixed.
                     */
                    if ((holder != null) && holder.message.equalsReference(messageReference))
                    {
                        return holder;
                    }
                }
            }
            return null;
        }

        /**
         * @param holders
         *            Never <code>null</code>.
         */
        public void removeMessages(List<MessageInfoHolder> holders)
        {
            messages.removeAll(holders);
        }

        public void addMessages(final List<MessageInfoHolder> holders)
        {
            for (final MessageInfoHolder messageInfoHolder : holders)
            {
                addMessage(messageInfoHolder);
            }
        }

        public void addMessage(final MessageInfoHolder message)
        {
            int index;
            synchronized (messages)
            {
                index = Collections.binarySearch(messages, message, getComparator());

                if (index < 0)
                {
                    index = (index * -1) - 1;
                }

                messages.add(index, message);
            }
        }

        /**
         * Synchronize the group list to match the message list
         */
        public void synchronizeGroups()
        {
            final List<MessageInfo<MessageInfoHolder>> toGroup = new ArrayList<MessageInfo<MessageInfoHolder>>(
                    messages.size());
            synchronized (messages)
            {
                for (final MessageInfoHolder holder : messages)
                {
                    final MessageInfo<MessageInfoHolder> messageInfo = new MessageInfo<MessageInfoHolder>();
                    final Message message = holder.message;
                    try
                    {
                        messageInfo.setId(message.getMessageId());
                        final String[] references = message.getReferences();
                        if (references != null)
                        {
                            messageInfo.getReferences().addAll(getReferences(references));
                        }
                        final String[] inReplyTo = message.getHeader("In-Reply-To");
                        if (inReplyTo != null && inReplyTo.length > 0)
                        {
                            messageInfo.getReferences().add(inReplyTo[0]);
                        }
                    }
                    catch (MessagingException e)
                    {
                        // should not happen?
                        Log.w(K9.LOG_TAG, "Unable to retrieve header from "
                                + message, e);
                        continue;
                    }
                    messageInfo.setDate(holder.compareDate);
                    messageInfo.setSubject(holder.subject);
                    messageInfo.setSender(holder.sender.toString());
        
                    messageInfo.setTag(holder);
        
                    toGroup.add(messageInfo);
                }
            }
            final List<MessageGroup<MessageInfoHolder>> messageGroups = mMessageGrouper
                    .group(toGroup);
        
            synchronized (mGroups)
            {
                mGroups.clear();
                mGroups.addAll(messageGroups);
            }
        }

        /**
         * @param references
         * @return
         */
        private List<String> getReferences(final String[] references)
        {
            final List<String> result = new ArrayList<String>();
            for (final String reference : references)
            {
                List<String> split = Arrays.asList(splitter.split(reference));
                result.addAll(split);
            }
            for (final Iterator<String> iterator = result.iterator(); iterator.hasNext();)
            {
                String string = iterator.next();
                if (string.length() == 0)
                {
                    iterator.remove();
                }
            }
            return result;
        }

        private static final Pattern splitter = Pattern.compile("\\s");

        private SORT_TYPE sortType = SORT_TYPE.SORT_DATE;

        private boolean sortAscending = true;

        private boolean sortDateAscending = false;

        /**
         * @return The comparator to use to display messages in an ordered
         *         fashion. Never <code>null</code>.
         */
        protected Comparator<MessageInfoHolder> getComparator()
        {
            final List<Comparator<MessageInfoHolder>> chain = new ArrayList<Comparator<MessageInfoHolder>>(2 /* we add 2 comparators at most */ );
        
            {
                // add the specified comparator
                final Comparator<MessageInfoHolder> comparator = SORT_COMPARATORS.get(sortType);
                if (sortAscending)
                {
                    chain.add(comparator);
                }
                else
                {
                    chain.add(new ReverseComparator<MessageInfoHolder>(comparator));
                }
            }
        
            {
                // add the date comparator if not already specified
                if (sortType != SORT_TYPE.SORT_DATE)
                {
                    final Comparator<MessageInfoHolder> comparator = SORT_COMPARATORS.get(SORT_TYPE.SORT_DATE);
                    if (sortDateAscending)
                    {
                        chain.add(comparator);
                    }
                    else
                    {
                        chain.add(new ReverseComparator<MessageInfoHolder>(comparator));
                    }
                }
            }
        
            // build the comparator chain
            final Comparator<MessageInfoHolder> chainComparator = new ComparatorChain<MessageInfoHolder>(chain);
        
            return chainComparator;
        }

        public void sortMessages()
        {
            final Comparator<MessageInfoHolder> chainComparator = getComparator();
            synchronized (messages)
            {
                Collections.sort(messages, chainComparator);
            }
        }

    }

    private MessageListStore mStore = new MessageListStore(new Listener());

    /**
     * {@inheritDoc}
     */
    class MessageListAdapter extends BaseExpandableListAdapter implements SectionIndexer
    {

        /**
         * Track groups expanded at load-time, to prevent from expanding at
         * subsequent loading
         */
        private Set<Long> mAutoExpanded = new HashSet<Long>();

        private Drawable mAttachmentIcon;
        private Drawable mAnsweredIcon;

        private boolean mGroupLessMode = false;

        private boolean mGroupingInProgress = false;

        /**
         * This differs from the store list as this instance must only be
         * modified from the UI thread, to prevent concurrency issues with the
         * Android UI thread which doesn't lock the instance
         */
        private List<MessageGroup<MessageInfoHolder>> mUiGroups = Collections.emptyList();

        private MessageListAdapter()
        {
            mAttachmentIcon = getResources().getDrawable(R.drawable.ic_mms_attachment_small);
            mAnsweredIcon = getResources().getDrawable(R.drawable.ic_mms_answered_small);
        }

        public void synchronizeFastScroll()
        {
            // only way to make getSections() invoked again: disable/enable back
            mListView.setFastScrollEnabled(false);
            mListView.setFastScrollEnabled(true);
        }

        private final View.OnClickListener flaggedClickListener = new View.OnClickListener()
        {
            @Override
            public void onClick(final View v)
            {
                if (v.getId() != R.id.flagged)
                {
                    return;
                }
                // Perform action on clicks
                final int position = mListView.getPositionForView(v);
                if (position == AdapterView.INVALID_POSITION)
                {
                    return;
                }
                final Object item = mListView.getItemAtPosition(position);
                if (item instanceof MessageInfoHolder)
                {
                    final MessageInfoHolder message = (MessageInfoHolder) item;
                    final List<MessageInfoHolder> selection = getSelectionFromMessage(message);
                    setFlag(selection, Flag.FLAGGED, !message.flagged);
                }
            }
        };

        /**
         * @param groupPosition
         *            the position of the group that contains the child
         * @param position
         *            the position of the child (for which the View is returned)
         *            within the group
         * @param isLastChild TODO
         * @param convertView
         *            the old view to reuse, if possible. You should check that
         *            this view is non-null and of an appropriate type before
         *            using. If it is not possible to convert this view to
         *            display the correct data, this method can create a new
         *            view. It is not guaranteed that the convertView will have
         *            been previously created by
         *            {@link #getChildView(int, int, boolean, View, ViewGroup)}.
         * @param parent
         *            the parent that this view will eventually be attached to
         * @return the View corresponding to the child at the specified position
         */
        private View getItemView(int groupPosition, int position, boolean isLastChild, View convertView, ViewGroup parent)
        {
            MessageInfoHolder message = getChild(groupPosition, position);
            View view;

            if ((convertView != null) && (convertView.getId() == R.layout.message_list_item))
            {
                view = convertView;
            }
            else
            {
                if (mTouchView)
                {
                    view = mInflater.inflate(R.layout.message_list_item_touchable, parent, false);
                    view.setId(R.layout.message_list_item);
                }
                else
                {
                    view = mInflater.inflate(R.layout.message_list_item, parent, false);
                    view.setId(R.layout.message_list_item);
                }
            }

            MessageViewHolder holder = (MessageViewHolder) view.getTag();

            if (holder == null)
            {
                holder = new MessageViewHolder();
                holder.subject = (TextView) view.findViewById(R.id.subject);
                holder.from = (TextView) view.findViewById(R.id.from);
                holder.date = (TextView) view.findViewById(R.id.date);
                holder.chip = view.findViewById(R.id.chip);
                holder.preview = (TextView) view.findViewById(R.id.preview);
                holder.selected = (CheckBox) view.findViewById(R.id.selected_checkbox);
                holder.flagged = (CheckBox) view.findViewById(R.id.flagged);

                holder.flagged.setOnClickListener(flaggedClickListener);

                if (!mStars)
                {
                    holder.flagged.setVisibility(View.GONE);
                }

                if (mCheckboxes)
                {
                    holder.selected.setVisibility(View.VISIBLE);
                }

                if (holder.selected != null)
                {
                    holder.selected.setOnCheckedChangeListener(holder);
                }

                view.setTag(holder);
            }

            if (message != null)
            {
                bindView(groupPosition, position, view, holder, message, isLastChild);
            }
            else
            {
                // TODO is this branch ever reached/executed?

                holder.chip.getBackground().setAlpha(0);
                holder.subject.setText("No subject");
                holder.subject.setTypeface(null, Typeface.NORMAL);
                if (holder.preview != null)
                {
                    holder.preview.setText("No sender");
                    holder.preview.setTypeface(null, Typeface.NORMAL);
                    holder.preview.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                }
                else
                {
                    holder.from.setText("No sender");
                    holder.from.setTypeface(null, Typeface.NORMAL);
                    holder.from.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                }

                holder.date.setText("No date");

                //WARNING: Order of the next 2 lines matter
                holder.position = -1;
                holder.selected.setChecked(false);
                holder.groupPosition = -1;

                if (!mCheckboxes)
                {
                    holder.selected.setVisibility(View.GONE);
                }
                holder.flagged.setChecked(false);
            }

            holder.subject.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mFontSizes.getMessageListSubject());
            holder.date.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mFontSizes.getMessageListDate());

            if (mTouchView)
            {
                holder.preview.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mFontSizes.getMessageListSender());
            }
            else
            {
                holder.from.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mFontSizes.getMessageListSender());
            }

            return view;
        }

        /**
         * Associate model data to view object.
         *
         * @param groupPosition
         *            the position of the group that contains the child
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
         * @param isLastChild TODO
         */
        private void bindView(int groupPosition, final int position, final View view,
                final MessageViewHolder holder, final MessageInfoHolder message, boolean isLastChild)
        {
            // XXX TODO there has to be some way to walk our view hierarchy and get this
            holder.flagged.setTag((Integer)position);
            holder.flagged.setChecked(message.flagged);

            // So that the mSelectedCount is only incremented/decremented
            // when a user checks the checkbox (vs code)
            holder.position = -1;
            holder.selected.setChecked(message.selected);
            holder.groupPosition = -1;

            if (!mCheckboxes)
            {
                holder.selected.setVisibility(message.selected ? View.VISIBLE : View.GONE);
            }

            if (isLastChild)
            {
                final ShapeDrawable lastChipDrawable = new ShapeDrawable(new PathShape(LAST_CHIP_PATH, 10, 10));
                lastChipDrawable.getPaint().setColor(message.message.getFolder().getAccount().getChipColor());
                lastChipDrawable.getPaint().setAlpha(message.read ? 127 : 255);
                holder.chip.setBackgroundDrawable(lastChipDrawable);
            }
            else
            {
                holder.chip.setBackgroundColor(message.message.getFolder().getAccount().getChipColor());
                holder.chip.getBackground().setAlpha(message.read ? 127 : 255);
            }
            view.getBackground().setAlpha(message.downloaded ? 0 : 127);

            if ((message.subject == null) || "".equals(message.subject))
            {
                holder.subject.setText(getText(R.string.general_no_subject));
                holder.subject.setTypeface(null, message.read ? Typeface.ITALIC : Typeface.BOLD_ITALIC);
            }
            else
            {
                holder.subject.setText(message.subject);
                holder.subject.setTypeface(null,  message.read ? Typeface.NORMAL : Typeface.BOLD);
            }

            if (holder.preview != null)
            {
                /*
                 * In the touchable UI, we have previews. Otherwise, we
                 * have just a "from" line.
                 * Because text views can't wrap around each other(?) we
                 * compose a custom view containing the preview and the
                 * from.
                 */
                holder.preview.setText(new SpannableStringBuilder(message.sender).append(" ").append(message.preview),
                                       TextView.BufferType.SPANNABLE);
                Spannable str = (Spannable)holder.preview.getText();

                // Create our span sections, and assign a format to each.
                str.setSpan(new StyleSpan(Typeface.BOLD),
                            0,
                            message.sender.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            );
                str.setSpan(new ForegroundColorSpan(Color.rgb(128,128,128)), // TODO: How do I can specify the android.R.attr.textColorTertiary
                            message.sender.length(),
                            str.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            );
            }
            else
            {
                holder.from.setText(message.sender);
                holder.from.setTypeface(null, message.read ? Typeface.NORMAL : Typeface.BOLD);
            }

            holder.date.setText(message.date);
            holder.subject.setCompoundDrawablesWithIntrinsicBounds(
                message.answered ? mAnsweredIcon : null, // left
                null, // top
                message.hasAttachments ? mAttachmentIcon : null, // right
                null); // bottom
            holder.position = position;
            holder.groupPosition = position;
        }

        @Override
        public boolean hasStableIds()
        {
            return true;
        }

        @Override
        public int getGroupCount()
        {
            return mUiGroups.size();
        }

        @Override
        public int getChildrenCount(int groupPosition)
        {
            return getGroup(groupPosition).getMessages().size();
        }

        @Override
        public MessageGroup<MessageInfoHolder> getGroup(int groupPosition)
        {
            return mUiGroups.get(groupPosition);
        }


        @Override
        public MessageInfoHolder getChild(int groupPosition, int childPosition)
        {
            final MessageGroup<MessageInfoHolder> group = getGroup(groupPosition);
            if (group == null)
            {
                return null;
            }
            return group.getMessages().get(childPosition).getTag();
        }


        @Override
        public long getGroupId(int groupPosition)
        {
            final MessageGroup<MessageInfoHolder> group = getGroup(groupPosition);
            if (group == null)
            {
                // the last group should match this case (as any other invalid
                // position)
                return -1;
            }
            // UI should stay consistent with the underlying data and needs to
            // keep track of the groups when list is updating (ie the selection/
            // expanded state should remain the "same" group if possible)

            if (mGroupLessMode)
            {
                // make sure we always get the same ID in groupless mode
                return 0;
            }

            return group.getId();
        }


        @Override
        public long getChildId(int groupPosition, int childPosition)
        {
            final MessageInfoHolder child = getChild(groupPosition, childPosition);
            if (child == null)
            {
                // unlikely to occur but still possible, returning fixed value
                return -1;
            }
            return ((LocalMessage) child.message).getId();
        }


        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                ViewGroup parent)
        {
            return getMessageGroupView(groupPosition, isExpanded, convertView, parent);
        }

        private View getMessageGroupView(int groupPosition, boolean isExpanded, View convertView,
                ViewGroup parent)
        {
            final MessageGroup<MessageInfoHolder> group = getGroup(groupPosition);
            final View view;
            if (convertView == null || R.layout.message_list_group_header != convertView.getId())
            {
                // create new view
                view = mInflater.inflate(R.layout.message_list_group_header, parent, false);
                view.setId(R.layout.message_list_group_header);
                final Path path = GROUP_CHIP_PATH;
                view.findViewById(R.id.chip).setBackgroundDrawable(new ShapeDrawable(new PathShape(path, 10, 10)));
            }
            else
            {
                // reuse view
                view = convertView;
            }
            final TextView subjectView = (TextView) view.findViewById(R.id.subject);
            final TextView countView = (TextView) view.findViewById(R.id.count);
            final TextView flagCountView = (TextView) view.findViewById(R.id.flagged_message_count);
            final TextView dateView = (TextView) view.findViewById(R.id.date);
            final TextView fromView = (TextView) view.findViewById(R.id.from);
            final View chipView = view.findViewById(R.id.chip);

            final Date date = group.getDate();
            if (date == null)
            {
                dateView.setVisibility(View.GONE);
            }
            else
            {
                if (Utility.isDateToday(date))
                {
                    dateView.setText(getTimeFormat().format(date));
                }
                else
                {
                    dateView.setText(getDateFormat().format(date));
                }
                dateView.setVisibility(View.VISIBLE);
                dateView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mFontSizes.getMessageListDate());
            }

            if (mGroupLessMode)
            {
                // TODO set localized text (or hide view?)
                subjectView.setText(group.getSubject());
            }
            else
            {
                subjectView.setText(group.getSubject());
            }
            subjectView
                    .setTextSize(TypedValue.COMPLEX_UNIT_DIP, mFontSizes.getMessageListSubject());

            int unreadCount = 0;
            int flagCount = 0;

            // remember senders for later
            final Set<String> unreadSenders;
            final Map<String, String> senders;
            if (mGroupLessMode)
            {
                unreadSenders = null;
                senders = null;
            }
            else
            {
                unreadSenders = new HashSet<String>();
                senders = new LinkedHashMap<String, String>();
            }
            boolean first = true;
            Account account = null;
            for (final MessageInfo<MessageInfoHolder> messageInfo : group.getMessages())
            {
                final MessageInfoHolder holder = messageInfo.getTag();
                if (!holder.read)
                {
                    unreadCount++;
                }
                if (holder.flagged)
                {
                    flagCount++;
                }
                if (!mGroupLessMode)
                {
                    try
                    {
                        final Address[] from = holder.message.getFrom();
                        // TODO handle user's identities to display 'me'
                        if (from.length > 0)
                        {
                            final String address = from[0].getAddress().toLowerCase(Locale.US);
                            if (!senders.containsKey(address))
                            {
                                final String friendly = from[0].toFriendly().toString();
                                // XXX toFriendly isn't as friendly as Gmail implementation which display only the first part to gain space
                                senders.put(address, friendly);
                            }
                            if (!holder.read)
                            {
                                unreadSenders.add(address);
                            }
                        }
                    }
                    catch (MessagingException e)
                    {
                        // should this happen?
                        Log.w(K9.LOG_TAG, e);
                    }
                }

                if (first)
                {
                    first = false;
                    account = holder.message.getFolder().getAccount();
                }
                else if (account != null)
                {
                    if (!account.equals(holder.message.getFolder().getAccount()))
                    {
                        account = null;
                    }
                }
            }

            if (account == null)
            {
                chipView.setVisibility(View.INVISIBLE);
            }
            else
            {
                chipView.setVisibility(View.VISIBLE);
                ((ShapeDrawable) chipView.getBackground()).getPaint().setColor(account.getChipColor());
            }

            final int count = group.getMessages().size();
            if (unreadCount == 0)
            {
                // all read
                subjectView.setTypeface(null, Typeface.NORMAL);
                countView.setText(Integer.toString(count));
                countView.setTypeface(null, Typeface.NORMAL);

                if (account != null)
                {
                    chipView.getBackground().setAlpha(127);
                }
            }
            else
            {
                // at least 1 unread

                subjectView.setTypeface(null, Typeface.BOLD);

                if (account != null)
                {
                    chipView.getBackground().setAlpha(255);
                }

                if (unreadCount == count)
                {
                    // none read
                    countView.setText(Integer.toString(count));
                    countView.setTypeface(null, Typeface.BOLD);
                }
                else
                {
                    // mixed
                    final String unreadString = Integer.toString(unreadCount);
                    final String totalString = Integer.toString(count);

                    final Spannable spannableStringBuilder = new SpannableStringBuilder(
                            unreadString + '/' + totalString);
                    spannableStringBuilder.setSpan(new StyleSpan(Typeface.BOLD), 0,
                            unreadString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                    countView.setTypeface(null, Typeface.NORMAL);
                    countView.setText(spannableStringBuilder);
                }
            }

            if (!mStars || flagCount == 0)
            {
                flagCountView.setVisibility(View.GONE);
            }
            else
            {
                flagCountView.setText(Integer.toString(flagCount));
                flagCountView.setVisibility(View.VISIBLE);
            }

            if (mGroupLessMode)
            {
                fromView.setVisibility(View.GONE);
            }
            else
            {
                // display unread sender in bold
                // XXX do that in 2 phases: StringBuilder for the text then styles
                final SpannableStringBuilder fromText = new SpannableStringBuilder();
                for (final Iterator<Map.Entry<String, String>> iterator = senders.entrySet()
                        .iterator(); iterator.hasNext();)
                {
                    final Entry<String, String> entry = iterator.next();
                    final String name = entry.getValue();
                    fromText.append(name);
                    if (unreadSenders.contains(entry.getKey()))
                    {
                        fromText.setSpan(new StyleSpan(Typeface.BOLD), fromText.toString().length()
                                - name.length(), fromText.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    if (iterator.hasNext())
                    {
                        fromText.append(", ");
                    }
                }
                fromView.setText(fromText);
                fromView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mFontSizes.getMessageListSender());
            }

            return view;
        }


        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                View convertView, ViewGroup parent)
        {
            return getItemView(groupPosition, childPosition, isLastChild, convertView, parent);
        }


        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition)
        {
            return true;
        }

        @Override
        public Object[] getSections()
        {
            // setFastScrollEnabled HAS to be disabled then enabled back for this method to be called again!!!
            final int count = mListView.getCount();

            final String[] sections = new String[count];
            Arrays.fill(sections, 0, count, " ");
            return sections;
        }

        @Override
        public int getPositionForSection(int section)
        {
            // for some obscure reason (or I didn't read the documentation right), the returned
            // value must match a group position
            final long packedPosition = mListView.getExpandableListPosition(section);
            final int index = ExpandableListView.getPackedPositionGroup(packedPosition);
            return index;
        }

        @Override
        public int getSectionForPosition(int position)
        {
            // doesn't seem to be used by the Android framework?
            return position;
        }

    }

    class MessageViewHolder
            implements OnCheckedChangeListener
    {
        public TextView subject;
        public TextView preview;
        public TextView from;
        public TextView time;
        public TextView date;
        public CheckBox flagged;
        public View chip;
        public CheckBox selected;
        public int position = -1;
        private int groupPosition = -1;

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
        {
            if (position!=-1)
            {
                MessageInfoHolder message = mAdapter.getChild(position, groupPosition);
                if (message.selected!=isChecked)
                {
                    if (isChecked)
                    {
                        mSelectedCount++;
                    }
                    else if (mSelectedCount > 0)
                    {
                        mSelectedCount--;
                    }

                    // We must set the flag before showing the buttons as the
                    // buttons text depends on what is selected.
                    message.selected = isChecked;
                    if (!mCheckboxes)
                    {
                        if (isChecked)
                        {
                            selected.setVisibility(View.VISIBLE);
                        }
                        else
                        {
                            selected.setVisibility(View.GONE);
                        }
                    }
                    toggleBatchButtons();
                }
            }
        }
    }

    private void addOrUpdateMessages(final Account account, final String folder, final List<Message> providedMessages, final boolean verifyAgainstSearch)
    {
        // we copy the message list because the callback doesn't expect
        // the callbacks to mutate it.
        final List<Message> messages = new ArrayList<Message>(providedMessages);
    
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                boolean needsSort = false;
                final List<MessageInfoHolder> messagesToAdd = new ArrayList<MessageInfoHolder>();
                List<MessageInfoHolder> messagesToRemove = new ArrayList<MessageInfoHolder>();
                List<Message> messagesToSearch = new ArrayList<Message>();
    
                for (Message message : messages)
                {
                    MessageInfoHolder m = mStore.getMessage(message);
                    if (message.isSet(Flag.DELETED))
                    {
                        if (m != null)
                        {
                            messagesToRemove.add(m);
                        }
                    }
                    else if (m == null)
                    {
                        if (updateForMe(account, folder))
                        {
                            m = new MessageInfoHolder(MessageList.this, message);
                            messagesToAdd.add(m);
                        }
                        else
                        {
                            if (mSearchMode)
                            {
                                if (verifyAgainstSearch)
                                {
                                    messagesToSearch.add(message);
                                }
                                else
                                {
                                    m = new MessageInfoHolder(MessageList.this, message);
                                    messagesToAdd.add(m);
                                }
                            }
                        }
                    }
                    else
                    {
                        m.populate(MessageList.this, message, new FolderInfoHolder(MessageList.this, message.getFolder(), account), account);
                        needsSort = true;
                    }
                }
    
                if (messagesToSearch.size() > 0)
                {
                    mController.searchLocalMessages(mAccountUuids, mFolderNames, messagesToSearch.toArray(EMPTY_MESSAGE_ARRAY), mQueryString, mIntegrate, mQueryFlags, mForbiddenFlags,
                                                    new MessagingListener()
                    {
                        @Override
                        public void listLocalMessagesAddMessages(Account account, String folder, List<Message> messages)
                        {
                            addOrUpdateMessages(account, folder, messages, false);
                        }
                    });
                }
    
                if (messagesToRemove.size() > 0)
                {
                    mHandler.removeMessages(messagesToRemove);
                }
    
                if (messagesToAdd.size() > 0)
                {
                    mHandler.addMessages(messagesToAdd);
                }
    
                if (needsSort)
                {
                    mHandler.sortMessages();
                    mHandler.resetUnreadCount();
                }
            }
        });
    }

    private FolderInfoHolder getFolder(String folder, Account account)
    {
        LocalFolder local_folder = null;
        try
        {
            LocalStore localStore = account.getLocalStore();
            local_folder = localStore.getFolder(folder);
            return new FolderInfoHolder(this, (Folder)local_folder, account);
        }
        catch (Exception e)
        {
            Log.e(K9.LOG_TAG, "getFolder(" + folder + ") goes boom: ",e);
            return null;
        }
        finally
        {
            if (local_folder != null)
            {
                local_folder.close();
            }
        }
    }

    /**
     * @param account
     * @param folder
     * @return Whether the given account/folders match the activity's ones
     */
    private boolean updateForMe(Account account, String folder)
    {
        if ((account.equals(mAccount) && mFolderName != null && folder.equals(mFolderName)))
        {
            return true;
        }
        else
        {
            return false;
        }
    }


    private void hideBatchButtons()
    {
        //TODO: Fade out animation
        mBatchButtonArea.setVisibility(View.GONE);
    }

    private void showBatchButtons()
    {
        //TODO: Fade in animation
        mBatchButtonArea.setVisibility(View.VISIBLE);
    }

    private void toggleBatchButtons()
    {
        if (mSelectedCount < 0)
        {
            mSelectedCount = 0;
        }

        int readButtonIconId;
        int flagButtonIconId;

        if (mSelectedCount==0)
        {
            readButtonIconId = R.drawable.ic_button_mark_read;
            flagButtonIconId = R.drawable.ic_button_flag;
            hideBatchButtons();
        }
        else
        {
            boolean newReadState = computeBatchDirection(false);
            if (newReadState)
            {
                readButtonIconId = R.drawable.ic_button_mark_read;
            }
            else
            {
                readButtonIconId = R.drawable.ic_button_mark_unread;
            }
            boolean newFlagState = computeBatchDirection(true);
            if (newFlagState)
            {
                flagButtonIconId = R.drawable.ic_button_flag;
            }
            else
            {
                flagButtonIconId = R.drawable.ic_button_unflag;
            }
            showBatchButtons();
        }

        mBatchReadButton.setImageResource(readButtonIconId);
        mBatchFlagButton.setImageResource(flagButtonIconId);
    }

    class FooterViewHolder
    {
        public ProgressBar progress;
        public TextView main;
    }


    private boolean computeBatchDirection(boolean flagged)
    {
        boolean newState = false;

        synchronized (mStore.messages)
        {
            for (MessageInfoHolder holder : mStore.messages)
            {
                if (holder.selected)
                {
                    if (flagged)
                    {
                        if (!holder.flagged)
                        {
                            newState = true;
                            break;
                        }
                    }
                    else
                    {
                        if (!holder.read)
                        {
                            newState = true;
                            break;
                        }
                    }
                }
            }
        }
        return newState;
    }

    private boolean anySelected()
    {
        synchronized (mStore.messages)
        {
            for (MessageInfoHolder holder : mStore.messages)
            {
                if (holder.selected)
                {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onClick(View v)
    {
        boolean newState = false;
        List<Message> messageList = new ArrayList<Message>();
        List<MessageInfoHolder> removeHolderList = new ArrayList<MessageInfoHolder>();

        if (v == mBatchDoneButton)
        {
            setAllSelected(false);
            return;
        }

        if (v == mBatchFlagButton)
        {
            newState = computeBatchDirection(true);
        }
        else
        {
            newState = computeBatchDirection(false);
        }

        synchronized (mStore.messages)
        {
            for (MessageInfoHolder holder : mStore.messages)
            {
                if (holder.selected)
                {
                    if (v == mBatchDeleteButton)
                    {
                        removeHolderList.add(holder);
                    }
                    else if (v == mBatchFlagButton)
                    {
                        holder.flagged = newState;
                    }
                    else if (v == mBatchReadButton)
                    {
                        holder.read = newState;
                    }
                    messageList.add(holder.message);
                }
            }
        }
        mHandler.removeMessages(removeHolderList);

        if (!messageList.isEmpty())
        {
            if (v == mBatchDeleteButton)
            {
                mController.deleteMessages(messageList.toArray(EMPTY_MESSAGE_ARRAY), null);
                mSelectedCount = 0;
                toggleBatchButtons();
            }
            else
            {
                mController.setFlag(messageList.toArray(EMPTY_MESSAGE_ARRAY), (v == mBatchReadButton ? Flag.SEEN : Flag.FLAGGED), newState);
            }
        }
        else
        {
            // Should not happen
            Toast.makeText(this, R.string.no_message_seletected_toast, Toast.LENGTH_SHORT).show();
        }
        mHandler.sortMessages();
    }

    private void setAllSelected(boolean isSelected)
    {
        mSelectedCount = 0;
        synchronized (mStore.messages)
        {
            for (MessageInfoHolder holder : mStore.messages)
            {
                holder.selected = isSelected;
                mSelectedCount += (isSelected ? 1 : 0);
            }
        }
        mHandler.synchronizeDisplay();
        toggleBatchButtons();
    }

    private void setSelected(final List<MessageInfoHolder> holders, final boolean newState)
    {
        for (final MessageInfoHolder holder : holders)
        {
            if (holder.selected != newState)
            {
                holder.selected = newState;
                mSelectedCount += (newState ? 1 : -1);
            }
        }
        mHandler.synchronizeDisplay();
        toggleBatchButtons();
    }

    /**
     * @param holders
     *            Never <code>null</code>.
     * @param flag
     *            Only {@link Flag#SEEN} or {@link Flag#FLAGGED} are handled.
     *            Never <code>null</code>.
     * @param newState
     */
    protected void setFlag(final List<MessageInfoHolder> holders, final Flag flag, final boolean newState)
    {
        if (holders.isEmpty())
        {
            return;
        }
        final Message[] messageList = new Message[holders.size()];
        int i = 0;
        for (final Iterator<MessageInfoHolder> iterator = holders.iterator(); iterator.hasNext(); i++)
        {
            final MessageInfoHolder holder = iterator.next();
            messageList[i] = holder.message;
            if (flag == Flag.SEEN)
            {
                holder.read = newState;
            }
            else if (flag == Flag.FLAGGED)
            {
                holder.flagged = newState;
            }
        }
        mController.setFlag(messageList, flag, newState);
        mHandler.sortMessages();
    }

    private void onMove(final List<MessageInfoHolder> holders)
    {
        if (!checkCopyOrMovePossible(holders, true))
        {
            return;
        }

        final Folder folder = holders.size() == 1 ? holders.get(0).message.getFolder() : mCurrentFolder.folder;
        displayFolderChoice(ACTIVITY_CHOOSE_FOLDER_MOVE, folder, holders.size() == 1 ? holders.get(0).message.makeMessageReference(): null, holders);
    }

    protected List<MessageInfoHolder> getSelectionFromCheckboxes()
    {
        final List<MessageInfoHolder> selection = new ArrayList<MessageInfoHolder>();
        synchronized (mStore.messages)
        {
            for (final MessageInfoHolder holder : mStore.messages)
            {
                if (holder.selected)
                {
                    selection.add(holder);
                }
            }
        }
        return selection;
    }

    /**
     * @param group
     *            Never <code>null</code>.
     * @return Never <code>null</code>.
     */
    protected List<MessageInfoHolder> getSelectionFromGroup(final MessageGroup<MessageInfoHolder> group)
    {
        final List<MessageInfoHolder> selection = new ArrayList<MessageInfoHolder>(group.getMessages().size());
        for (final MessageInfo<MessageInfoHolder> info : group.getMessages())
        {
            selection.add(info.getTag());
        }
        return selection;
    }

    /**
     * @param holder
     *            Never <code>null</code>.
     * @return Never <code>null</code>.
     */
    protected List<MessageInfoHolder> getSelectionFromMessage(final MessageInfoHolder holder)
    {
        final List<MessageInfoHolder> selection = Collections.singletonList(holder);
        return selection;
    }

    /**
     * @return Never <code>null</code>.
     * 
     */
    protected List<MessageInfoHolder> getSelectionFromAll()
    {
        final List<MessageInfoHolder> selection;
        synchronized (mStore.messages)
        {
            selection = new ArrayList<MessageInfoHolder>(mStore.messages);
        }
        return selection;
    }

    protected void copy(final List<MessageInfoHolder> holders, final String destination)
    {
        copyOrMove(holders, destination, false);
    }

    protected void move(final List<MessageInfoHolder> holders, final String destination)
    {
        copyOrMove(holders, destination, true);
    }

    private void copyOrMove(final List<MessageInfoHolder> holders, final String destination, final boolean move)
    {
        if (K9.FOLDER_NONE.equalsIgnoreCase(destination))
        {
            return;
        }

        boolean first = true;
        Account account = null;
        String folderName = null;

        final List<Message> messages = new ArrayList<Message>(holders.size());

        for (final MessageInfoHolder holder : holders)
        {
            final Message message = holder.message;
            if (first)
            {
                first = false;
                folderName = message.getFolder().getName();
                account = message.getFolder().getAccount();
                if ((move && !mController.isMoveCapable(account)) || (!move && !mController.isCopyCapable(account)))
                {
                    // account is not copy/move capable
                    return;
                }
            }
            else if (!account.equals(message.getFolder().getAccount()) || !folderName.equals(message.getFolder().getName()))
            {
                // make sure all messages come from the same account/folder?
                return;
            }
            if ((move && !mController.isMoveCapable(message)) || (!move && !mController.isCopyCapable(message)))
            {
                final Toast toast = Toast.makeText(this,
                        R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
                toast.show();

                // XXX return meaningful error value?

                // message isn't synchronized
                return;
            }
            messages.add(message);
        }

        if (move)
        {
            mController.moveMessages(account, folderName,
                    messages.toArray(new Message[messages.size()]), destination, null);
            mHandler.removeMessages(holders);
        }
        else
        {
            mController.copyMessages(account, folderName,
                    messages.toArray(new Message[messages.size()]), destination, null);
        }
    }

    private void onCopy(final List<MessageInfoHolder> holders)
    {
        if (!checkCopyOrMovePossible(holders, false))
        {
            return;
        }

        final Folder folder = holders.size() == 1 ? holders.get(0).message.getFolder() : mCurrentFolder.folder;
        displayFolderChoice(ACTIVITY_CHOOSE_FOLDER_COPY, folder, holders.size() == 1 ? holders.get(0).message.makeMessageReference() : null, holders);
    }

    /**
     * @param requestCode
     * @param folder
     * @param reference
     * @param holders
     *            Never <code>null</code>.
     */
    private void displayFolderChoice(final int requestCode, final Folder folder, final MessageReference reference, final List<MessageInfoHolder> holders)
    {
        final Intent intent = new Intent(this, ChooseFolder.class);
        intent.putExtra(ChooseFolder.EXTRA_ACCOUNT, folder.getAccount().getUuid());
        intent.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, folder.getName());
        intent.putExtra(ChooseFolder.EXTRA_SEL_FOLDER, folder.getAccount().getLastSelectedFolderName());
        if (reference != null)
        {
            intent.putExtra(ChooseFolder.EXTRA_MESSAGE, reference);
        }
        // remember the selected messages for #onActivityResult
        mActiveMessages = holders;
        startActivityForResult(intent, requestCode);
    }

}
