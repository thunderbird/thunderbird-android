package com.fsck.k9.provider;


import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import android.annotation.TargetApi;
import android.app.Application;
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
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.provider.BaseColumns;
import timber.log.Timber;

import com.fsck.k9.Account;
import com.fsck.k9.AccountStats;
import com.fsck.k9.BuildConfig;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.FolderInfoHolder;
import com.fsck.k9.activity.MessageInfoHolder;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.SimpleMessagingListener;
import com.fsck.k9.helper.MessageHelper;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.search.SearchAccount;


public class MessageProvider extends ContentProvider {
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".messageprovider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    private static final String[] DEFAULT_MESSAGE_PROJECTION = new String[] {
            MessageColumns._ID,
            MessageColumns.SEND_DATE,
            MessageColumns.SENDER,
            MessageColumns.SUBJECT,
            MessageColumns.PREVIEW,
            MessageColumns.ACCOUNT,
            MessageColumns.URI,
            MessageColumns.DELETE_URI,
            MessageColumns.SENDER_ADDRESS
    };
    private static final String[] DEFAULT_ACCOUNT_PROJECTION = new String[] {
            AccountColumns.ACCOUNT_NUMBER,
            AccountColumns.ACCOUNT_NAME,
    };
    private static final String[] UNREAD_PROJECTION = new String[] {
            UnreadColumns.ACCOUNT_NAME,
            UnreadColumns.UNREAD
    };


    private UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private List<QueryHandler> queryHandlers = new ArrayList<QueryHandler>();
    private MessageHelper messageHelper;

    /**
     * How many simultaneous cursors we can afford to expose at once
     */
    Semaphore semaphore = new Semaphore(1);

    ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(1);


    @Override
    public boolean onCreate() {
        messageHelper = MessageHelper.getInstance(getContext());

        registerQueryHandler(new ThrottlingQueryHandler(new AccountsQueryHandler()));
        registerQueryHandler(new ThrottlingQueryHandler(new MessagesQueryHandler()));
        registerQueryHandler(new ThrottlingQueryHandler(new UnreadQueryHandler()));

        K9.registerApplicationAware(new K9.ApplicationAware() {
            @Override
            public void initializeComponent(final Application application) {
                Timber.v("Registering content resolver notifier");

                MessagingController.getInstance(application).addListener(new SimpleMessagingListener() {
                    @Override
                    public void folderStatusChanged(Account account, String folderId, String folderName, int unreadMessageCount) {
                        application.getContentResolver().notifyChange(CONTENT_URI, null);
                    }
                });
            }
        });

        return true;
    }

    @Override
    public String getType(Uri uri) {
        if (K9.app == null) {
            return null;
        }

        Timber.v("MessageProvider/getType: %s", uri);

        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (K9.app == null) {
            return null;
        }

        Timber.v("MessageProvider/query: %s", uri);

        int code = uriMatcher.match(uri);
        if (code == -1) {
            throw new IllegalStateException("Unrecognized URI: " + uri);
        }

        Cursor cursor;
        try {
            QueryHandler handler = queryHandlers.get(code);
            cursor = handler.query(uri, projection, selection, selectionArgs, sortOrder);
        } catch (Exception e) {
            Timber.e(e, "Unable to execute query for URI: %s", uri);
            return null;
        }

        return cursor;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (K9.app == null) {
            return 0;
        }

        Timber.v("MessageProvider/delete: %s", uri);

        // Note: can only delete a message

        List<String> segments = uri.getPathSegments();
        int accountId = Integer.parseInt(segments.get(1));
        String folderName = segments.get(2);
        String msgUid = segments.get(3);

        // get account
        Account myAccount = null;
        for (Account account : Preferences.getPreferences(getContext()).getAccounts()) {
            if (account.getAccountNumber() == accountId) {
                myAccount = account;
                if (!account.isAvailable(getContext())) {
                    Timber.w("not deleting messages because account is unavailable at the moment");
                    return 0;
                }
            }
        }

        if (myAccount == null) {
            Timber.e("Could not find account with id %d", accountId);
        }

        if (myAccount != null) {
            MessageReference messageReference = new MessageReference(myAccount.getUuid(), folderName, msgUid, null);
            MessagingController controller = MessagingController.getInstance(getContext());
            controller.deleteMessage(messageReference, null);
        }

        // FIXME return the actual number of deleted messages
        return 0;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (K9.app == null) {
            return null;
        }

        Timber.v("MessageProvider/insert: %s", uri);

        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (K9.app == null) {
            return 0;
        }

        Timber.v("MessageProvider/update: %s", uri);

        // TBD

        return 0;
    }

    /**
     * Register a {@link QueryHandler} to handle a certain {@link Uri} for
     * {@link #query(Uri, String[], String, String[], String)}
     */
    protected void registerQueryHandler(QueryHandler handler) {
        if (queryHandlers.contains(handler)) {
            return;
        }
        queryHandlers.add(handler);

        int code = queryHandlers.indexOf(handler);
        uriMatcher.addURI(AUTHORITY, handler.getPath(), code);
    }


    public static class ReverseDateComparator implements Comparator<MessageInfoHolder> {
        @Override
        public int compare(MessageInfoHolder object2, MessageInfoHolder object1) {
            if (object1.compareDate == null) {
                return (object2.compareDate == null ? 0 : 1);
            } else if (object2.compareDate == null) {
                return -1;
            } else {
                return object1.compareDate.compareTo(object2.compareDate);
            }
        }
    }

    public interface MessageColumns extends BaseColumns {
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
        String SENDER_ADDRESS = "senderAddress";

        /**
         * <P>Type: TEXT</P>
         */
        String SUBJECT = "subject";

        /**
         * <P>Type: TEXT</P>
         */
        String PREVIEW = "preview";

        /**
         * <P>Type: BOOLEAN</P>
         */
        String UNREAD = "unread";

        /**
         * <P>Type: TEXT</P>
         */
        String ACCOUNT = "account";

        /**
         * <P>Type: INTEGER</P>
         */
        String ACCOUNT_NUMBER = "accountNumber";

        /**
         * <P>Type: BOOLEAN</P>
         */
        String HAS_ATTACHMENTS = "hasAttachments";

        /**
         * <P>Type: BOOLEAN</P>
         */
        String HAS_STAR = "hasStar";

        /**
         * <P>Type: INTEGER</P>
         */
        String ACCOUNT_COLOR = "accountColor";

        String URI = "uri";
        String DELETE_URI = "delUri";

        /**
         * @deprecated the field value is misnamed/misleading - present for compatibility purpose only. To be removed.
         */
        @Deprecated
        String INCREMENT = "id";
    }

    public interface AccountColumns {
        /**
         * <P>Type: INTEGER</P>
         */
        String ACCOUNT_NUMBER = "accountNumber";
        /**
         * <P>Type: String</P>
         */
        String ACCOUNT_NAME = "accountName";


        String ACCOUNT_UUID = "accountUuid";
        String ACCOUNT_COLOR = "accountColor";
    }

    public interface UnreadColumns {
        /**
         * <P>Type: String</P>
         */
        String ACCOUNT_NAME = "accountName";
        /**
         * <P>Type: INTEGER</P>
         */
        String UNREAD = "unread";
    }

    protected interface QueryHandler {
        /**
         * The path this instance is able to respond to.
         */
        String getPath();

        Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
                throws Exception;
    }

    /**
     * Extracts a value from an object.
     */
    public interface FieldExtractor<T, K> {
        K getField(T source);
    }

    /**
     * Extracts the {@link LocalMessage#getDatabaseId() ID} from the given {@link MessageInfoHolder}. The underlying
     * {@link Message} is expected to be a {@link LocalMessage}.
     */
    public static class IdExtractor implements FieldExtractor<MessageInfoHolder, Long> {
        @Override
        public Long getField(MessageInfoHolder source) {
            return source.message.getDatabaseId();
        }
    }

    public static class CountExtractor<T> implements FieldExtractor<T, Integer> {
        private Integer count;

        public CountExtractor(int count) {
            this.count = count;
        }

        @Override
        public Integer getField(T source) {
            return count;
        }
    }

    public static class SubjectExtractor implements FieldExtractor<MessageInfoHolder, String> {
        @Override
        public String getField(MessageInfoHolder source) {
            return source.message.getSubject();
        }
    }

    public static class SendDateExtractor implements FieldExtractor<MessageInfoHolder, Long> {
        @Override
        public Long getField(MessageInfoHolder source) {
            return source.message.getSentDate().getTime();
        }
    }

    public static class PreviewExtractor implements FieldExtractor<MessageInfoHolder, String> {
        @Override
        public String getField(MessageInfoHolder source) {
            return source.message.getPreview();
        }
    }

    public static class UriExtractor implements FieldExtractor<MessageInfoHolder, String> {
        @Override
        public String getField(MessageInfoHolder source) {
            return source.uri;
        }
    }

    public static class DeleteUriExtractor implements FieldExtractor<MessageInfoHolder, String> {
        @Override
        public String getField(MessageInfoHolder source) {
            LocalMessage message = source.message;
            int accountNumber = message.getAccount().getAccountNumber();
            return CONTENT_URI.buildUpon()
                    .appendPath("delete_message")
                    .appendPath(Integer.toString(accountNumber))
                    .appendPath(message.getFolder().getId())
                    .appendPath(message.getUid())
                    .build()
                    .toString();
        }
    }

    public static class SenderExtractor implements FieldExtractor<MessageInfoHolder, CharSequence> {
        @Override
        public CharSequence getField(MessageInfoHolder source) {
            return source.sender;
        }
    }

    public static class SenderAddressExtractor implements FieldExtractor<MessageInfoHolder, String> {
        @Override
        public String getField(MessageInfoHolder source) {
            return source.senderAddress;
        }
    }

    public static class AccountExtractor implements FieldExtractor<MessageInfoHolder, String> {
        @Override
        public String getField(MessageInfoHolder source) {
            return source.message.getAccount().getDescription();
        }
    }

    public static class AccountColorExtractor implements FieldExtractor<MessageInfoHolder, Integer> {
        @Override
        public Integer getField(MessageInfoHolder source) {
            return source.message.getAccount().getChipColor();
        }
    }

    public static class AccountNumberExtractor implements FieldExtractor<MessageInfoHolder, Integer> {
        @Override
        public Integer getField(MessageInfoHolder source) {
            return source.message.getAccount().getAccountNumber();
        }
    }

    public static class HasAttachmentsExtractor implements FieldExtractor<MessageInfoHolder, Boolean> {
        @Override
        public Boolean getField(MessageInfoHolder source) {
            return source.message.hasAttachments();
        }
    }

    public static class HasStarExtractor implements FieldExtractor<MessageInfoHolder, Boolean> {
        @Override
        public Boolean getField(MessageInfoHolder source) {
            return source.message.isSet(Flag.FLAGGED);
        }
    }

    public static class UnreadExtractor implements FieldExtractor<MessageInfoHolder, Boolean> {
        @Override
        public Boolean getField(MessageInfoHolder source) {
            return !source.read;
        }
    }

    /**
     * @deprecated having an incremental value has no real interest, implemented for compatibility only
     */
    @Deprecated
    public static class IncrementExtractor implements FieldExtractor<MessageInfoHolder, Integer> {
        private int count = 0;


        @Override
        public Integer getField(MessageInfoHolder source) {
            return count++;
        }
    }

    /**
     * Retrieve messages from the integrated inbox.
     */
    protected class MessagesQueryHandler implements QueryHandler {

        @Override
        public String getPath() {
            return "inbox_messages/";
        }

        @Override
        public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
                throws Exception {
            return getMessages(projection);
        }

        protected MatrixCursor getMessages(String[] projection) throws InterruptedException {
            BlockingQueue<List<MessageInfoHolder>> queue = new SynchronousQueue<List<MessageInfoHolder>>();

            // new code for integrated inbox, only execute this once as it will be processed afterwards via the listener
            SearchAccount integratedInboxAccount = SearchAccount.createUnifiedInboxAccount(getContext());
            MessagingController msgController = MessagingController.getInstance(getContext());

            msgController.searchLocalMessages(integratedInboxAccount.getRelatedSearch(),
                    new MessageInfoHolderRetrieverListener(queue));

            List<MessageInfoHolder> holders = queue.take();

            // TODO add sort order parameter
            Collections.sort(holders, new ReverseDateComparator());

            String[] projectionToUse;
            if (projection == null) {
                projectionToUse = DEFAULT_MESSAGE_PROJECTION;
            } else {
                projectionToUse = projection;
            }

            LinkedHashMap<String, FieldExtractor<MessageInfoHolder, ?>> extractors =
                    resolveMessageExtractors(projectionToUse, holders.size());
            int fieldCount = extractors.size();

            String[] actualProjection = extractors.keySet().toArray(new String[fieldCount]);
            MatrixCursor cursor = new MatrixCursor(actualProjection);

            for (MessageInfoHolder holder : holders) {
                Object[] o = new Object[fieldCount];

                int i = 0;
                for (FieldExtractor<MessageInfoHolder, ?> extractor : extractors.values()) {
                    o[i] = extractor.getField(holder);
                    i += 1;
                }

                cursor.addRow(o);
            }

            return cursor;
        }

        protected LinkedHashMap<String, FieldExtractor<MessageInfoHolder, ?>> resolveMessageExtractors(
                String[] projection, int count) {
            LinkedHashMap<String, FieldExtractor<MessageInfoHolder, ?>> extractors =
                    new LinkedHashMap<String, FieldExtractor<MessageInfoHolder, ?>>();

            for (String field : projection) {
                if (extractors.containsKey(field)) {
                    continue;
                }
                if (MessageColumns._ID.equals(field)) {
                    extractors.put(field, new IdExtractor());
                } else if (MessageColumns._COUNT.equals(field)) {
                    extractors.put(field, new CountExtractor<MessageInfoHolder>(count));
                } else if (MessageColumns.SUBJECT.equals(field)) {
                    extractors.put(field, new SubjectExtractor());
                } else if (MessageColumns.SENDER.equals(field)) {
                    extractors.put(field, new SenderExtractor());
                } else if (MessageColumns.SENDER_ADDRESS.equals(field)) {
                    extractors.put(field, new SenderAddressExtractor());
                } else if (MessageColumns.SEND_DATE.equals(field)) {
                    extractors.put(field, new SendDateExtractor());
                } else if (MessageColumns.PREVIEW.equals(field)) {
                    extractors.put(field, new PreviewExtractor());
                } else if (MessageColumns.URI.equals(field)) {
                    extractors.put(field, new UriExtractor());
                } else if (MessageColumns.DELETE_URI.equals(field)) {
                    extractors.put(field, new DeleteUriExtractor());
                } else if (MessageColumns.UNREAD.equals(field)) {
                    extractors.put(field, new UnreadExtractor());
                } else if (MessageColumns.ACCOUNT.equals(field)) {
                    extractors.put(field, new AccountExtractor());
                } else if (MessageColumns.ACCOUNT_COLOR.equals(field)) {
                    extractors.put(field, new AccountColorExtractor());
                } else if (MessageColumns.ACCOUNT_NUMBER.equals(field)) {
                    extractors.put(field, new AccountNumberExtractor());
                } else if (MessageColumns.HAS_ATTACHMENTS.equals(field)) {
                    extractors.put(field, new HasAttachmentsExtractor());
                } else if (MessageColumns.HAS_STAR.equals(field)) {
                    extractors.put(field, new HasStarExtractor());
                } else if (MessageColumns.INCREMENT.equals(field)) {
                    extractors.put(field, new IncrementExtractor());
                }
            }
            return extractors;
        }
    }

    /**
     * Retrieve the account list.
     */
    protected class AccountsQueryHandler implements QueryHandler {


        @Override
        public String getPath() {
            return "accounts";
        }

        @Override
        public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
                throws Exception {
            return getAllAccounts(projection);
        }

        public Cursor getAllAccounts(String[] projection) {
            if (projection == null) {
                projection = DEFAULT_ACCOUNT_PROJECTION;
            }

            MatrixCursor cursor = new MatrixCursor(projection);

            for (Account account : Preferences.getPreferences(getContext()).getAccounts()) {
                Object[] values = new Object[projection.length];

                int fieldIndex = 0;
                for (String field : projection) {
                    if (AccountColumns.ACCOUNT_NUMBER.equals(field)) {
                        values[fieldIndex] = account.getAccountNumber();
                    } else if (AccountColumns.ACCOUNT_NAME.equals(field)) {
                        values[fieldIndex] = account.getDescription();
                    } else if (AccountColumns.ACCOUNT_UUID.equals(field)) {
                        values[fieldIndex] = account.getUuid();
                    } else if (AccountColumns.ACCOUNT_COLOR.equals(field)) {
                        values[fieldIndex] = account.getChipColor();
                    } else {
                        values[fieldIndex] = null;
                    }
                    ++fieldIndex;
                }

                cursor.addRow(values);
            }

            return cursor;
        }
    }

    /**
     * Retrieve the unread message count for a given account specified by its {@link Account#getAccountNumber() number}.
     */
    protected class UnreadQueryHandler implements QueryHandler {

        @Override
        public String getPath() {
            return "account_unread/#";
        }

        @Override
        public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
                throws Exception {
            List<String> segments = uri.getPathSegments();
            int accountId = Integer.parseInt(segments.get(1));

            /*
             * This method below calls Account.getStats() which uses EmailProvider to do its work.
             * For this to work we need to clear the calling identity. Otherwise accessing
             * EmailProvider will fail because it's not exported so third-party apps can't access it
             * directly.
             */
            long identityToken = Binder.clearCallingIdentity();
            try {
                return getAccountUnread(accountId);
            } finally {
                Binder.restoreCallingIdentity(identityToken);
            }
        }

        private Cursor getAccountUnread(int accountNumber) {

            MatrixCursor cursor = new MatrixCursor(UNREAD_PROJECTION);

            Account myAccount;
            AccountStats myAccountStats;

            Object[] values = new Object[2];

            for (Account account : Preferences.getPreferences(getContext()).getAvailableAccounts()) {
                if (account.getAccountNumber() == accountNumber) {
                    myAccount = account;
                    try {
                        myAccountStats = account.getStats(getContext());
                        values[0] = myAccount.getDescription();
                        if (myAccountStats == null) {
                            values[1] = 0;
                        } else {
                            values[1] = myAccountStats.unreadMessageCount;
                        }
                    } catch (MessagingException e) {
                        Timber.e(e.getMessage());
                        values[0] = "Unknown";
                        values[1] = 0;
                    }
                    cursor.addRow(values);
                }
            }

            return cursor;
        }
    }

    /**
     * Cursor wrapper that release a semaphore on close. Close is also triggered on {@link #finalize()}.
     */
    protected static class MonitoredCursor implements CrossProcessCursor {
        /**
         * The underlying cursor implementation that handles regular requests
         */
        private CrossProcessCursor cursor;

        /**
         * Whether {@link #close()} was invoked
         */
        private AtomicBoolean closed = new AtomicBoolean(false);

        private Semaphore semaphore;


        protected MonitoredCursor(CrossProcessCursor cursor, Semaphore semaphore) {
            this.cursor = cursor;
            this.semaphore = semaphore;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                cursor.close();
                Timber.d("Cursor closed, null'ing & releasing semaphore");
                cursor = null;
                semaphore.release();
            }
        }

        @Override
        public boolean isClosed() {
            return closed.get() || cursor.isClosed();
        }

        @Override
        protected void finalize() throws Throwable {
            close();
            super.finalize();
        }

        protected void checkClosed() throws IllegalStateException {
            if (closed.get()) {
                throw new IllegalStateException("Cursor was closed");
            }
        }

        @Override
        public void fillWindow(int pos, CursorWindow winow) {
            checkClosed();
            cursor.fillWindow(pos, winow);
        }

        @Override
        public CursorWindow getWindow() {
            checkClosed();
            return cursor.getWindow();
        }

        @Override
        public boolean onMove(int oldPosition, int newPosition) {
            checkClosed();
            return cursor.onMove(oldPosition, newPosition);
        }

        @Override
        public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {
            checkClosed();
            cursor.copyStringToBuffer(columnIndex, buffer);
        }

        @Override
        public void deactivate() {
            checkClosed();
            cursor.deactivate();
        }

        @Override
        public byte[] getBlob(int columnIndex) {
            checkClosed();
            return cursor.getBlob(columnIndex);
        }

        @Override
        public int getColumnCount() {
            checkClosed();
            return cursor.getColumnCount();
        }

        @Override
        public int getColumnIndex(String columnName) {
            checkClosed();
            return cursor.getColumnIndex(columnName);
        }

        @Override
        public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException {
            checkClosed();
            return cursor.getColumnIndexOrThrow(columnName);
        }

        @Override
        public String getColumnName(int columnIndex) {
            checkClosed();
            return cursor.getColumnName(columnIndex);
        }

        @Override
        public String[] getColumnNames() {
            checkClosed();
            return cursor.getColumnNames();
        }

        @Override
        public int getCount() {
            checkClosed();
            return cursor.getCount();
        }

        @Override
        public double getDouble(int columnIndex) {
            checkClosed();
            return cursor.getDouble(columnIndex);
        }

        @Override
        public Bundle getExtras() {
            checkClosed();
            return cursor.getExtras();
        }

        @Override
        public float getFloat(int columnIndex) {
            checkClosed();
            return cursor.getFloat(columnIndex);
        }

        @Override
        public int getInt(int columnIndex) {
            checkClosed();
            return cursor.getInt(columnIndex);
        }

        @Override
        public long getLong(int columnIndex) {
            checkClosed();
            return cursor.getLong(columnIndex);
        }

        @Override
        public int getPosition() {
            checkClosed();
            return cursor.getPosition();
        }

        @Override
        public short getShort(int columnIndex) {
            checkClosed();
            return cursor.getShort(columnIndex);
        }

        @Override
        public String getString(int columnIndex) {
            checkClosed();
            return cursor.getString(columnIndex);
        }

        @Override
        public boolean getWantsAllOnMoveCalls() {
            checkClosed();
            return cursor.getWantsAllOnMoveCalls();
        }

        @TargetApi(Build.VERSION_CODES.M)
        @Override
        public void setExtras(Bundle extras) {
            cursor.setExtras(extras);
        }

        @Override
        public boolean isAfterLast() {
            checkClosed();
            return cursor.isAfterLast();
        }

        @Override
        public boolean isBeforeFirst() {
            checkClosed();
            return cursor.isBeforeFirst();
        }

        @Override
        public boolean isFirst() {
            checkClosed();
            return cursor.isFirst();
        }

        @Override
        public boolean isLast() {
            checkClosed();
            return cursor.isLast();
        }

        @Override
        public boolean isNull(int columnIndex) {
            checkClosed();
            return cursor.isNull(columnIndex);
        }

        @Override
        public boolean move(int offset) {
            checkClosed();
            return cursor.move(offset);
        }

        @Override
        public boolean moveToFirst() {
            checkClosed();
            return cursor.moveToFirst();
        }

        @Override
        public boolean moveToLast() {
            checkClosed();
            return cursor.moveToLast();
        }

        @Override
        public boolean moveToNext() {
            checkClosed();
            return cursor.moveToNext();
        }

        @Override
        public boolean moveToPosition(int position) {
            checkClosed();
            return cursor.moveToPosition(position);
        }

        @Override
        public boolean moveToPrevious() {
            checkClosed();
            return cursor.moveToPrevious();
        }

        @Override
        public void registerContentObserver(ContentObserver observer) {
            checkClosed();
            cursor.registerContentObserver(observer);
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
            checkClosed();
            cursor.registerDataSetObserver(observer);
        }

        @SuppressWarnings("deprecation")
        @Override
        public boolean requery() {
            checkClosed();
            return cursor.requery();
        }

        @Override
        public Bundle respond(Bundle extras) {
            checkClosed();
            return cursor.respond(extras);
        }

        @Override
        public void setNotificationUri(ContentResolver cr, Uri uri) {
            checkClosed();
            cursor.setNotificationUri(cr, uri);
        }

        @Override
        public void unregisterContentObserver(ContentObserver observer) {
            checkClosed();
            cursor.unregisterContentObserver(observer);
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            checkClosed();
            cursor.unregisterDataSetObserver(observer);
        }

        @Override
        public int getType(int columnIndex) {
            checkClosed();
            return cursor.getType(columnIndex);
        }

        @Override
        public Uri getNotificationUri() {
            return null;
        }
    }

    protected class ThrottlingQueryHandler implements QueryHandler {
        private QueryHandler delegate;


        public ThrottlingQueryHandler(QueryHandler delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getPath() {
            return delegate.getPath();
        }

        @Override
        public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
                throws Exception {
            semaphore.acquire();

            Cursor cursor = null;
            try {
                cursor = delegate.query(uri, projection, selection, selectionArgs, sortOrder);
            } finally {
                if (cursor == null) {
                    semaphore.release();
                }
            }

            // Android content resolvers can only process CrossProcessCursor instances
            if (!(cursor instanceof CrossProcessCursor)) {
                Timber.w("Unsupported cursor, returning null: %s", cursor);
                semaphore.release();
                return null;
            }

            MonitoredCursor wrapped = new MonitoredCursor((CrossProcessCursor) cursor, semaphore);

            // Use a weak reference not to actively prevent garbage collection
            final WeakReference<MonitoredCursor> weakReference = new WeakReference<MonitoredCursor>(wrapped);

            // Make sure the cursor is closed after 30 seconds
            scheduledPool.schedule(new Runnable() {

                @Override
                public void run() {
                    MonitoredCursor monitored = weakReference.get();
                    if (monitored != null && !monitored.isClosed()) {
                        Timber.w("Forcibly closing remotely exposed cursor");
                        try {
                            monitored.close();
                        } catch (Exception e) {
                            Timber.w(e, "Exception while forcibly closing cursor");
                        }
                    }
                }
            }, 30, TimeUnit.SECONDS);

            return wrapped;
        }
    }

    /**
     * Synchronized listener used to retrieve {@link MessageInfoHolder}s using a given {@link BlockingQueue}.
     */
    protected class MessageInfoHolderRetrieverListener extends SimpleMessagingListener {
        private final BlockingQueue<List<MessageInfoHolder>> queue;
        private List<MessageInfoHolder> holders = new ArrayList<MessageInfoHolder>();


        public MessageInfoHolderRetrieverListener(BlockingQueue<List<MessageInfoHolder>> queue) {
            this.queue = queue;
        }

        @Override
        public void listLocalMessagesAddMessages(Account account, String folderId, String folderName, List<LocalMessage> messages) {
            Context context = getContext();

            for (LocalMessage message : messages) {
                MessageInfoHolder messageInfoHolder = new MessageInfoHolder();
                LocalFolder messageFolder = message.getFolder();
                Account messageAccount = message.getAccount();

                FolderInfoHolder folderInfoHolder = new FolderInfoHolder(context, messageFolder, messageAccount);
                messageHelper.populate(messageInfoHolder, message, folderInfoHolder, messageAccount);

                holders.add(messageInfoHolder);
            }
        }

        @Override
        public void searchStats(AccountStats stats) {
            try {
                queue.put(holders);
            } catch (InterruptedException e) {
                Timber.e(e, "Unable to return message list back to caller");
            }
        }
    }
}
