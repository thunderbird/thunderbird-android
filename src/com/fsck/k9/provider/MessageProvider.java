package com.fsck.k9.provider;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Date;
import java.text.SimpleDateFormat;

import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;
import android.os.Bundle;
import android.util.Config;
import android.text.format.DateFormat;
import android.provider.Settings;
import android.content.Intent;

import com.fsck.k9.Account;
import com.fsck.k9.AccountStats;
import com.fsck.k9.K9;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.Preferences;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.LocalStore;
import com.fsck.k9.SearchAccount;
import com.fsck.k9.Account;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.R;
import com.fsck.k9.mail.store.LocalStore.LocalMessage;
import com.fsck.k9.AccountStats;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.controller.MessagingController.SORT_TYPE;
import com.fsck.k9.activity.DateFormatter;
import com.fsck.k9.activity.MessageInfoHolder;
import com.fsck.k9.controller.MessagingController.SORT_TYPE;

public class MessageProvider extends ContentProvider
{

    public static final String AUTHORITY = "com.fsck.k9.messageprovider";

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    private static final int URI_INBOX_MESSAGES = 0;
    private static final int URI_DELETE_MESSAGE = 1;
    private static final int URI_ACCOUNTS = 2;
    private static final int URI_ACCOUNT_UNREAD = 3;

    private static String mCurrentEmailAccount = "";
    private static Context context = null;
    private static boolean mIsListenerRegister = false;
    private static boolean inSync = false;

    private static Application mApp;

    /**
     * list is synchronized, iterations have to be synchronized on the list
     * instance in order to prevent concurrency issue
     */
    private final List<MessageInfoHolder> glb_messages = Collections.synchronizedList(new ArrayList<MessageInfoHolder>());
    private static MatrixCursor mCursor;

    static
    {
        URI_MATCHER.addURI(AUTHORITY, "inbox_messages/", URI_INBOX_MESSAGES);
        URI_MATCHER.addURI(AUTHORITY, "delete_message/", URI_DELETE_MESSAGE);
        URI_MATCHER.addURI(AUTHORITY, "accounts", URI_ACCOUNTS);
        URI_MATCHER.addURI(AUTHORITY, "account_unread/#", URI_ACCOUNT_UNREAD);
    }

    static String[] messages_projection = new String[]
    {
        "id",
        "date",
        "sender",
        "subject",
        "preview",
        "account",
        "uri",
        "delUri"
    };
    MessagingListener mListener = new MessagingListener()
    {

        public void messageDeleted(Account account, String folder, Message message)
        {
        }

        public void folderStatusChanged(Account account, String folderName, int unreadMessageCount)
        {
            if (inSync == false)
            {
                inSync = true;
                glb_messages.clear();
                SearchAccount integratedInboxAccount = new SearchAccount(getContext(), true, null,  null);
                MessagingController msgController = MessagingController.getInstance(mApp);
                msgController.searchLocalMessages(integratedInboxAccount, null, mListener);
            }
        }

        public void listLocalMessagesStarted(Account account, String folder)
        {
        }

        public void listLocalMessagesFinished(Account account, String folder)
        {
        }

        public void searchStats(AccountStats stats)
        {
            int id = -1;
            synchronized (glb_messages)
            {
                Collections.sort(glb_messages);
            }
            MatrixCursor tmpCur = new MatrixCursor(messages_projection);
            synchronized (glb_messages)
            {
                for (MessageInfoHolder mi : glb_messages)
                {
                    ++id;
                    Message msg = mi.message;
                    tmpCur.addRow(new Object[]
                    {
                            id,
                            mi.fullDate,
                            mi.sender,
                            mi.subject,
                            mi.preview,
                            mi.account,
                            mi.uri,
                            CONTENT_URI + "/delete_message/"
                                    + msg.getFolder().getAccount().getAccountNumber() + "/"
                                    + msg.getFolder().getName() + "/" + msg.getUid() });
                }
            }
            mCursor = tmpCur;
            inSync=false;
            notifyDatabaseModification();
        }

        public void listLocalMessagesAddMessages(Account account, String folder, List<Message> messages)
        {
// We will by default sort by DATE desc
            SORT_TYPE t_sort = SORT_TYPE.SORT_DATE;

            for (Message m : messages)
            {
                MessageInfoHolder m1 = new MessageInfoHolder(context,m,t_sort,false);
                glb_messages.add(m1);
            }
        }

    };

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

    public void setApplication(Application app)
    {
        if (context == null)
        {
            context = app.getApplicationContext();
        }
        if (app != null)
        {
            mApp = app;
            MessagingController msgController = MessagingController.getInstance(mApp);
            if ((msgController != null) && (!mIsListenerRegister))
            {
                msgController.addListener(mListener);
                mIsListenerRegister = true;
            }
        }

    }

    @Override
    public boolean onCreate()
    {
        context = getContext();

        if (mApp != null)
        {
            MessagingController msgController = MessagingController.getInstance(mApp);
            if ((msgController != null) && (!mIsListenerRegister))
            {
                msgController.addListener(mListener);
                mIsListenerRegister = true;
            }
        }

        return false;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs)
    {

        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "delete");

        if (mApp == null)
        {
            Log.d(K9.LOG_TAG, "K9 not ready");
            return 0;
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
            Folder lf = LocalStore.getLocalInstance(myAccount, mApp).getFolder(folderName);
            int msgCount = lf.getMessageCount();
            if (K9.DEBUG)
                Log.d(K9.LOG_TAG, "folder msg count = " + msgCount);
            msg = lf.getMessage(msgUid);
        }
        catch (MessagingException e)
        {
            Log.e(K9.LOG_TAG, e.getMessage());
        }

        // launch command to delete the message
        if ((myAccount != null) && (msg != null))
        {
            MessagingController.getInstance(mApp).deleteMessages(new Message[] { msg }, mListener);
        }

        return 0;
    }

    @Override
    public String getType(Uri uri)
    {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values)
    {
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
    {

        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "query");

        if (mApp == null)
        {
            Log.d(K9.LOG_TAG, "K9 not ready");
            return null;
        }

        Cursor cursor;
        switch (URI_MATCHER.match(uri))
        {
            case URI_INBOX_MESSAGES:


                if (mCursor == null)
                {
                    mCursor = new MatrixCursor(messages_projection);
                    // new code for integrated inbox, only execute this once as it will be processed afterwards via the listener
                    glb_messages.clear();
                    SearchAccount integratedInboxAccount = new SearchAccount(getContext(), true, null,  null);
                    MessagingController msgController = MessagingController.getInstance(mApp);
                    msgController.searchLocalMessages(integratedInboxAccount, null, mListener);
                }

                int id = -1;

// Process messages

//cursor = getAllMessages(projection, selection, selectionArgs, sortOrder);
                cursor = (Cursor)mCursor;
                break;

            case URI_ACCOUNTS:
                cursor = getAllAccounts();
                break;

            case URI_ACCOUNT_UNREAD:

                List<String> segments = null;
                int accountId = -1;
                segments = uri.getPathSegments();
                accountId = Integer.parseInt(segments.get(1));
                cursor = getAccountUnread(accountId);
                break;

            default:
                throw new IllegalStateException("Unrecognized URI:" + uri);
        }

        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
    {

        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "update");

//TBD

        return 0;
    }

    public static void notifyDatabaseModification()
    {

        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "notifyDatabaseModification -> UPDATE");

        Intent intent = new Intent(K9.Intents.EmailReceived.ACTION_REFRESH_OBSERVER, null);
        context.sendBroadcast(intent);

        context.getContentResolver().notifyChange(CONTENT_URI, null);

    }

}
