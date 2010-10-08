package com.fsck.k9.provider;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.CrossProcessCursor;
import android.database.Cursor;
import android.database.CursorWindow;
import android.database.DataSetObserver;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.AccountStats;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.SearchAccount;
import com.fsck.k9.activity.FolderInfoHolder;
import com.fsck.k9.activity.MessageInfoHolder;
import com.fsck.k9.activity.MessageList;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.helper.MessageHelper;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.LocalStore;

public class MessageProvider extends ContentProvider
{

    public static interface MessageColumns extends BaseColumns
    {
        /**
         * The number of milliseconds since Jan. 1, 1970, midnight GMT.
         *
         * <P>Type: INTEGER (long)</P>
         */
        String SEND_DATE = "date";

        /**
         * <P>Type: TEXT</P>
         */
        String SENDER = "sender";

        /**
         * <P>Type: TEXT</P>
         */
        String SUBJECT = "subject";

        /**
         * <P>Type: TEXT</P>
         */
        String PREVIEW = "preview";
    }

    protected interface QueryHandler
    {
        /**
         * The path this instance is able to respond to.
         *
         * @return Never <code>null</code>.
         */
        String getPath();

        /**
         * @param uri
         * @param projection
         * @param selection
         * @param selectionArgs
         * @param sortOrder
         * @return
         * @throws Exception
         * @see {@link ContentProvider#query(Uri, String[], String, String[], String)}
         */
        Cursor query(Uri uri, String[] projection,
                     String selection, String[] selectionArgs, String sortOrder) throws Exception;
    }

    /**
     * Retrieve messages from the integrated inbox.
     */
    protected class MessagesQueryHandler implements QueryHandler
    {

        @Override
        public String getPath()
        {
            return "inbox_messages/";
        }

        @Override
        public Cursor query(final Uri uri, final String[] projection, final String selection,
                            final String[] selectionArgs, final String sortOrder) throws Exception
        {
            return getMessages(projection);
        }

        /**
         * @param projection
         *            Projection to use. If <code>null</code>, use the default
         *            projection.
         * @return Never <code>null</code>.
         * @throws InterruptedException
         */
        protected MatrixCursor getMessages(final String[] projection) throws InterruptedException
        {
            // TODO use the given projection if prevent
            final MatrixCursor cursor = new MatrixCursor(DEFAULT_MESSAGE_PROJECTION);
            final BlockingQueue<List<MessageInfoHolder>> queue = new SynchronousQueue<List<MessageInfoHolder>>();

            // new code for integrated inbox, only execute this once as it will be processed afterwards via the listener
            final SearchAccount integratedInboxAccount = new SearchAccount(getContext(), true, null, null);
            final MessagingController msgController = MessagingController.getInstance(K9.app);

            msgController.searchLocalMessages(integratedInboxAccount, null,
                                              new MesssageInfoHolderRetrieverListener(queue));

            final List<MessageInfoHolder> holders = queue.take();

            // TODO add sort order parameter
            Collections.sort(holders, new MessageList.ReverseComparator<MessageInfoHolder>(
                                 new MessageList.DateComparator()));

            int id = -1;
            for (final MessageInfoHolder holder : holders)
            {
                final Message message = holder.message;
                id++;

                cursor.addRow(new Object[]
                              {
                                  id,
                                  message.getSentDate().getTime(),
                                  holder.sender,
                                  holder.subject,
                                  holder.preview,
                                  holder.account,
                                  holder.uri,
                                  CONTENT_URI + "/delete_message/"
                                  + message.getFolder().getAccount().getAccountNumber() + "/"
                                  + message.getFolder().getName() + "/" + message.getUid()
                              });
            }
            return cursor;
        }

    }

    /**
     * Retrieve the account list.
     */
    protected class AccountsQueryHandler implements QueryHandler
    {

        @Override
        public String getPath()
        {
            return "accounts";
        }

        @Override
        public Cursor query(final Uri uri, String[] projection, String selection,
                            String[] selectionArgs, String sortOrder) throws Exception
        {
            return getAllAccounts();
        }

        public Cursor getAllAccounts()
        {
            String[] projection = new String[] { "accountNumber", "accountName" };

            MatrixCursor ret = new MatrixCursor(projection);

            for (Account account : Preferences.getPreferences(getContext()).getAccounts())
            {
                Object[] values = new Object[2];
                values[0] = account.getAccountNumber();
                values[1] = account.getDescription();
                ret.addRow(values);
            }

            return ret;
        }

    }

    /**
     * Retrieve the unread message count for a given account specified by its
     * {@link Account#getAccountNumber() number}.
     */
    protected class UnreadQueryHandler implements QueryHandler
    {

        @Override
        public String getPath()
        {
            return "account_unread/#";
        }

        @Override
        public Cursor query(final Uri uri, String[] projection, String selection,
                            String[] selectionArgs, String sortOrder) throws Exception
        {
            List<String> segments = null;
            int accountId = -1;
            segments = uri.getPathSegments();
            accountId = Integer.parseInt(segments.get(1));
            return getAccountUnread(accountId);
        }

        public Cursor getAccountUnread(int accountNumber)
        {
            String[] projection = new String[] { "accountName", "unread" };

            MatrixCursor ret = new MatrixCursor(projection);

            Account myAccount;
            AccountStats myAccountStats = null;

            Object[] values = new Object[2];

            for (Account account : Preferences.getPreferences(getContext()).getAccounts())
            {
                if (account.getAccountNumber()==accountNumber)
                {
                    myAccount = account;
                    try
                    {
                        myAccountStats = account.getStats(getContext());
                        values[0] = myAccount.getDescription();
                        values[1] = myAccountStats.unreadMessageCount;
                        ret.addRow(values);
                    }
                    catch (MessagingException e)
                    {
                        Log.e(K9.LOG_TAG, e.getMessage());
                        values[0] = "Unknown";
                        values[1] = 0;
                    }
                }
            }

            return ret;
        }
    }

    protected class ThrottlingQueryHandler implements QueryHandler
    {

        protected final class MonitoredCursor implements CrossProcessCursor
        {
            private CrossProcessCursor mCursor;

            @Override
            public void close()
            {
                mCursor.close();
                if (mClosed.compareAndSet(false, true))
                {
                    Log.d(K9.LOG_TAG, "Cursor closed, releasing semaphore");
                    mSemaphore.release();
                }
            }

            @Override
            public void fillWindow(int pos, CursorWindow winow)
            {
                mCursor.fillWindow(pos, winow);
            }

            @Override
            public CursorWindow getWindow()
            {
                return mCursor.getWindow();
            }

            @Override
            public boolean onMove(int oldPosition, int newPosition)
            {
                return mCursor.onMove(oldPosition, newPosition);
            }

            private AtomicBoolean mClosed = new AtomicBoolean(false);


            protected MonitoredCursor(final CrossProcessCursor cursor)
            {
                this.mCursor = cursor;
            }

            @Override
            public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer)
            {
                mCursor.copyStringToBuffer(columnIndex, buffer);
            }

            @Override
            public void deactivate()
            {
                mCursor.deactivate();
            }

            @Override
            public byte[] getBlob(int columnIndex)
            {
                return mCursor.getBlob(columnIndex);
            }

            @Override
            public int getColumnCount()
            {
                return mCursor.getColumnCount();
            }

            @Override
            public int getColumnIndex(String columnName)
            {
                return mCursor.getColumnIndex(columnName);
            }

            @Override
            public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException
            {
                return mCursor.getColumnIndexOrThrow(columnName);
            }

            @Override
            public String getColumnName(int columnIndex)
            {
                return mCursor.getColumnName(columnIndex);
            }

            @Override
            public String[] getColumnNames()
            {
                return mCursor.getColumnNames();
            }

            @Override
            public int getCount()
            {
                return mCursor.getCount();
            }

            @Override
            public double getDouble(int columnIndex)
            {
                return mCursor.getDouble(columnIndex);
            }

            @Override
            public Bundle getExtras()
            {
                return mCursor.getExtras();
            }

            @Override
            public float getFloat(int columnIndex)
            {
                return mCursor.getFloat(columnIndex);
            }

            @Override
            public int getInt(int columnIndex)
            {
                return mCursor.getInt(columnIndex);
            }

            @Override
            public long getLong(int columnIndex)
            {
                return mCursor.getLong(columnIndex);
            }

            @Override
            public int getPosition()
            {
                return mCursor.getPosition();
            }

            @Override
            public short getShort(int columnIndex)
            {
                return mCursor.getShort(columnIndex);
            }

            @Override
            public String getString(int columnIndex)
            {
                return mCursor.getString(columnIndex);
            }

            @Override
            public boolean getWantsAllOnMoveCalls()
            {
                return mCursor.getWantsAllOnMoveCalls();
            }

            @Override
            public boolean isAfterLast()
            {
                return mCursor.isAfterLast();
            }

            @Override
            public boolean isBeforeFirst()
            {
                return mCursor.isBeforeFirst();
            }

            @Override
            public boolean isClosed()
            {
                return mCursor.isClosed();
            }

            @Override
            public boolean isFirst()
            {
                return mCursor.isFirst();
            }

            public boolean isLast()
            {
                return mCursor.isLast();
            }

            @Override
            public boolean isNull(int columnIndex)
            {
                return mCursor.isNull(columnIndex);
            }

            @Override
            public boolean move(int offset)
            {
                return mCursor.move(offset);
            }

            @Override
            public boolean moveToFirst()
            {
                return mCursor.moveToFirst();
            }

            @Override
            public boolean moveToLast()
            {
                return mCursor.moveToLast();
            }

            @Override
            public boolean moveToNext()
            {
                return mCursor.moveToNext();
            }

            @Override
            public boolean moveToPosition(int position)
            {
                return mCursor.moveToPosition(position);
            }

            @Override
            public boolean moveToPrevious()
            {
                return mCursor.moveToPrevious();
            }

            @Override
            public void registerContentObserver(ContentObserver observer)
            {
                mCursor.registerContentObserver(observer);
            }

            @Override
            public void registerDataSetObserver(DataSetObserver observer)
            {
                mCursor.registerDataSetObserver(observer);
            }

            @Override
            public boolean requery()
            {
                return mCursor.requery();
            }

            @Override
            public Bundle respond(Bundle extras)
            {
                return mCursor.respond(extras);
            }

            @Override
            public void setNotificationUri(ContentResolver cr, Uri uri)
            {
                mCursor.setNotificationUri(cr, uri);
            }

            @Override
            public void unregisterContentObserver(ContentObserver observer)
            {
                mCursor.unregisterContentObserver(observer);
            }

            @Override
            public void unregisterDataSetObserver(DataSetObserver observer)
            {
                mCursor.unregisterDataSetObserver(observer);
            }
        }

        private QueryHandler mDelegate;

        public ThrottlingQueryHandler(final QueryHandler delegate)
        {
            mDelegate = delegate;
        }

        @Override
        public String getPath()
        {
            return mDelegate.getPath();
        }

        @Override
        public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                            String sortOrder) throws Exception
        {
            mSemaphore.acquire();

            final Cursor cursor;
            cursor = mDelegate.query(uri, projection, selection, selectionArgs, sortOrder);

            /* Android content resolvers can only process CrossProcessCursor instances */
            if (cursor == null || !(cursor instanceof CrossProcessCursor))
            {
                return null;
            }

            final MonitoredCursor wrapped = new MonitoredCursor((CrossProcessCursor) cursor);

            /* use a weak reference not to actively prevent garbage collection */
            final WeakReference<MonitoredCursor> weakReference = new WeakReference<MonitoredCursor>(wrapped);

            /* make sure the cursor is closed after 30 seconds */
            mScheduledPool.schedule(new Runnable()
            {

                @Override
                public void run()
                {
                    final MonitoredCursor monitored = weakReference.get();
                    if (monitored != null && monitored.mClosed.compareAndSet(false, true))
                    {
                        Log.w(K9.LOG_TAG, "Forcibly closing remotely exposed cursor");
                        try
                        {
                            monitored.close();
                        }
                        catch (Exception e)
                        {
                            Log.w(K9.LOG_TAG, "Exception while forcibly closing cursor", e);
                        }
                    }
                }
            }, 30, TimeUnit.SECONDS);

            return wrapped;
        }

    }

    /**
     * Synchronized listener used to retrieve {@link MessageInfoHolder}s using a
     * given {@link BlockingQueue}.
     */
    protected class MesssageInfoHolderRetrieverListener extends MessagingListener
    {
        private final BlockingQueue<List<MessageInfoHolder>> queue;

        private List<MessageInfoHolder> mHolders = new ArrayList<MessageInfoHolder>();

        /**
         * @param queue
         *            Never <code>null</code>. The synchronized channel to use
         *            to retrieve {@link MessageInfoHolder}s.
         */
        public MesssageInfoHolderRetrieverListener(final BlockingQueue<List<MessageInfoHolder>> queue)
        {
            this.queue = queue;
        }

        @Override
        public void listLocalMessagesAddMessages(final Account account,
                final String folderName, final List<Message> messages)
        {
            // cache fields into local variables for faster access on JVM without JIT
            final MessageHelper helper = mMessageHelper;
            final List<MessageInfoHolder> holders = mHolders;

            final Context context = getContext();

            for (final Message message : messages)
            {
                final MessageInfoHolder messageInfoHolder = new MessageInfoHolder();
                final Folder messageFolder = message.getFolder();
                final Account messageAccount = messageFolder.getAccount();

                helper.populate(messageInfoHolder, message, new FolderInfoHolder(context,
                                messageFolder, messageAccount), messageAccount);

                holders.add(messageInfoHolder);
            }
        }

        @Override
        public void searchStats(AccountStats stats)
        {
            try
            {
                queue.put(mHolders);
            }
            catch (InterruptedException e)
            {
                Log.e(K9.LOG_TAG, "Unable to return message list back to caller", e);
            }
        }
    }

    public static final String AUTHORITY = "com.fsck.k9.messageprovider";

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    private static final String[] DEFAULT_MESSAGE_PROJECTION = new String[]
    {
        MessageColumns._ID,
        MessageColumns.SEND_DATE,
        MessageColumns.SENDER,
        MessageColumns.SUBJECT,
        MessageColumns.PREVIEW,
        "account",
        "uri",
        "delUri"
    };

    /**
     * URI matcher used for
     * {@link #query(Uri, String[], String, String[], String)}
     */
    private UriMatcher mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    /**
     * Handlers registered to respond to
     * {@link #query(Uri, String[], String, String[], String)}
     */
    private List<QueryHandler> mQueryHandlers = new ArrayList<QueryHandler>();

    private MessageHelper mMessageHelper;

    /* package */
    Semaphore mSemaphore = new Semaphore(1);

    /* package */
    ScheduledExecutorService mScheduledPool = Executors.newScheduledThreadPool(1);

    @Override
    public boolean onCreate()
    {
        mMessageHelper = MessageHelper.getInstance(getContext());

        registerQueryHandler(new ThrottlingQueryHandler(new AccountsQueryHandler()));
        registerQueryHandler(new ThrottlingQueryHandler(new MessagesQueryHandler()));
        registerQueryHandler(new ThrottlingQueryHandler(new UnreadQueryHandler()));

        K9.registerApplicationAware(new K9.ApplicationAware()
        {
            @Override
            public void initializeComponent(final K9 application) throws Exception
            {
                Log.v(K9.LOG_TAG, "Registering content resolver notifier");

                MessagingController.getInstance(application).addListener(new MessagingListener()
                {
                    @Override
                    public void searchStats(final AccountStats stats)
                    {
                        application.getContentResolver().notifyChange(CONTENT_URI, null);
                    }
                });
            }
        });

        return true;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs)
    {
        if (K9.app == null)
        {
            return 0;
        }

        if (K9.DEBUG)
        {
            Log.v(K9.LOG_TAG, "MessageProvider/delete: " + uri);
        }

        // Nota : can only delete a message

        List<String> segments = null;
        int accountId = -1;
        String folderName = null;
        String msgUid = null;

        segments = uri.getPathSegments();
        accountId = Integer.parseInt(segments.get(1));
        folderName = segments.get(2);
        msgUid = segments.get(3);

        // get account
        Account myAccount = null;
        for (Account account : Preferences.getPreferences(getContext()).getAccounts())
        {
            if (account.getAccountNumber() == accountId)
            {
                myAccount = account;
            }
        }

        // get localstore parameter
        Message msg = null;
        try
        {
            Folder lf = LocalStore.getLocalInstance(myAccount, K9.app).getFolder(folderName);
            int msgCount = lf.getMessageCount();
            if (K9.DEBUG)
            {
                Log.d(K9.LOG_TAG, "folder msg count = " + msgCount);
            }
            msg = lf.getMessage(msgUid);
        }
        catch (MessagingException e)
        {
            Log.e(K9.LOG_TAG, "Unable to retrieve message", e);
        }

        // launch command to delete the message
        if ((myAccount != null) && (msg != null))
        {
            MessagingController.getInstance(K9.app).deleteMessages(new Message[] { msg }, null);
        }

        // FIXME return the actual number of deleted messages
        return 0;
    }

    @Override
    public String getType(Uri uri)
    {
        if (K9.app == null)
        {
            return null;
        }

        if (K9.DEBUG)
        {
            Log.v(K9.LOG_TAG, "MessageProvider/getType: " + uri);
        }

        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values)
    {
        if (K9.app == null)
        {
            return null;
        }

        if (K9.DEBUG)
        {
            Log.v(K9.LOG_TAG, "MessageProvider/insert: " + uri);
        }

        return null;
    }

    @Override
    public Cursor query(final Uri uri, final String[] projection, final String selection,
                        final String[] selectionArgs, final String sortOrder)
    {
        if (K9.app == null)
        {
            return null;
        }

        if (K9.DEBUG)
        {
            Log.v(K9.LOG_TAG, "MessageProvider/query: " + uri);
        }

        final Cursor cursor;

        final int code = mUriMatcher.match(uri);

        if (code == -1)
        {
            throw new IllegalStateException("Unrecognized URI: " + uri);
        }

        try
        {
            // since we used the list index as the UriMatcher code, using it
            // back to retrieve the handler from the list
            final QueryHandler handler = mQueryHandlers.get(code);
            cursor = handler.query(uri, projection, selection, selectionArgs, sortOrder);
        }
        catch (Exception e)
        {
            Log.e(K9.LOG_TAG, "Unable to execute query for URI: " + uri, e);
            return null;
        }

        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
    {
        if (K9.app == null)
        {
            return 0;
        }

        if (K9.DEBUG)
        {
            Log.v(K9.LOG_TAG, "MessageProvider/update: " + uri);
        }

//TBD

        return 0;
    }

    /**
     * Register a {@link QueryHandler} to handle a certain {@link Uri} for
     * {@link #query(Uri, String[], String, String[], String)}
     *
     * @param handler
     *            Never <code>null</code>.
     */
    protected void registerQueryHandler(final QueryHandler handler)
    {
        if (mQueryHandlers.contains(handler))
        {
            return;
        }
        mQueryHandlers.add(handler);

        // use the index inside the list as the UriMatcher code for that handler
        final int code = mQueryHandlers.indexOf(handler);
        mUriMatcher.addURI(AUTHORITY, handler.getPath(), code);
    }

}
