
package com.fsck.k9.mail.store;

import android.app.Application;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.util.Log;
import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.controller.MessageRemovalListener;
import com.fsck.k9.controller.MessageRetrievalListener;
import com.fsck.k9.helper.Regex;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.*;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.filter.Base64OutputStream;
import com.fsck.k9.mail.internet.*;
import com.fsck.k9.provider.AttachmentProvider;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;

/**
 * <pre>
 * Implements a SQLite database backed local store for Messages.
 * </pre>
 */
public class LocalStore extends Store implements Serializable
{
    private static final int DB_VERSION = 35;
    private static final Flag[] PERMANENT_FLAGS = { Flag.DELETED, Flag.X_DESTROYED, Flag.SEEN, Flag.FLAGGED };

    private String mPath;
    private SQLiteDatabase mDb;
    private File mAttachmentsDir;
    private Application mApplication;
    private String uUid = null;

    private static Set<String> HEADERS_TO_SAVE = new HashSet<String>();
    static
    {
        HEADERS_TO_SAVE.add(K9.K9MAIL_IDENTITY);
        HEADERS_TO_SAVE.add("In-Reply-To");
        HEADERS_TO_SAVE.add("References");
        HEADERS_TO_SAVE.add("X-User-Agent");
    }
    /*
     * a String containing the columns getMessages expects to work with
     * in the correct order.
     */
    static private String GET_MESSAGES_COLS =
        "subject, sender_list, date, uid, flags, id, to_list, cc_list, "
        + "bcc_list, reply_to_list, attachment_count, internal_date, message_id, folder_id, preview ";

    /**
     * local://localhost/path/to/database/uuid.db
     */
    public LocalStore(Account account, Application application) throws MessagingException
    {
        super(account);
        mApplication = application;
        URI uri = null;
        try
        {
            uri = new URI(mAccount.getLocalStoreUri());
        }
        catch (Exception e)
        {
            throw new MessagingException("Invalid uri for LocalStore");
        }
        if (!uri.getScheme().equals("local"))
        {
            throw new MessagingException("Invalid scheme");
        }
        mPath = uri.getPath();


        // We need to associate the localstore with the account.  Since we don't have the account
        // handy here, we'll take the filename from the DB and use the basename of the filename
        // Folders probably should have references to their containing accounts
        //TODO: We do have an account object now
        File dbFile = new File(mPath);
        String[] tokens = dbFile.getName().split("\\.");
        uUid = tokens[0];

        openOrCreateDataspace(application);

    }

    private void openOrCreateDataspace(Application application)
    {
        File parentDir = new File(mPath).getParentFile();
        if (!parentDir.exists())
        {
            parentDir.mkdirs();
        }

        mAttachmentsDir = new File(mPath + "_att");
        if (!mAttachmentsDir.exists())
        {
            mAttachmentsDir.mkdirs();
        }

        mDb = SQLiteDatabase.openOrCreateDatabase(mPath, null);
        if (mDb.getVersion() != DB_VERSION)
        {
            doDbUpgrade(mDb, application);
        }
    }

    private void doDbUpgrade(SQLiteDatabase mDb, Application application)
    {
        Log.i(K9.LOG_TAG, String.format("Upgrading database from version %d to version %d",
                                        mDb.getVersion(), DB_VERSION));


        AttachmentProvider.clear(application);

        try
        {
            // schema version 29 was when we moved to incremental updates
            // in the case of a new db or a < v29 db, we blow away and start from scratch
            if (mDb.getVersion() < 29)
            {

                mDb.execSQL("DROP TABLE IF EXISTS folders");
                mDb.execSQL("CREATE TABLE folders (id INTEGER PRIMARY KEY, name TEXT, "
                            + "last_updated INTEGER, unread_count INTEGER, visible_limit INTEGER, status TEXT, push_state TEXT, last_pushed INTEGER, flagged_count INTEGER default 0)");

                mDb.execSQL("CREATE INDEX IF NOT EXISTS folder_name ON folders (name)");
                mDb.execSQL("DROP TABLE IF EXISTS messages");
                mDb.execSQL("CREATE TABLE messages (id INTEGER PRIMARY KEY, deleted INTEGER default 0, folder_id INTEGER, uid TEXT, subject TEXT, "
                            + "date INTEGER, flags TEXT, sender_list TEXT, to_list TEXT, cc_list TEXT, bcc_list TEXT, reply_to_list TEXT, "
                            + "html_content TEXT, text_content TEXT, attachment_count INTEGER, internal_date INTEGER, message_id TEXT, preview TEXT)");

                mDb.execSQL("DROP TABLE IF EXISTS headers");
                mDb.execSQL("CREATE TABLE headers (id INTEGER PRIMARY KEY, message_id INTEGER, name TEXT, value TEXT)");
                mDb.execSQL("CREATE INDEX IF NOT EXISTS header_folder ON headers (message_id)");

                mDb.execSQL("CREATE INDEX IF NOT EXISTS msg_uid ON messages (uid, folder_id)");
                mDb.execSQL("DROP INDEX IF EXISTS msg_folder_id");
                mDb.execSQL("DROP INDEX IF EXISTS msg_folder_id_date");
                mDb.execSQL("CREATE INDEX IF NOT EXISTS msg_folder_id_deleted_date ON messages (folder_id,deleted,internal_date)");
                mDb.execSQL("DROP TABLE IF EXISTS attachments");
                mDb.execSQL("CREATE TABLE attachments (id INTEGER PRIMARY KEY, message_id INTEGER,"
                            + "store_data TEXT, content_uri TEXT, size INTEGER, name TEXT,"
                            + "mime_type TEXT)");

                mDb.execSQL("DROP TABLE IF EXISTS pending_commands");
                mDb.execSQL("CREATE TABLE pending_commands " +
                            "(id INTEGER PRIMARY KEY, command TEXT, arguments TEXT)");

                mDb.execSQL("DROP TRIGGER IF EXISTS delete_folder");
                mDb.execSQL("CREATE TRIGGER delete_folder BEFORE DELETE ON folders BEGIN DELETE FROM messages WHERE old.id = folder_id; END;");

                mDb.execSQL("DROP TRIGGER IF EXISTS delete_message");
                mDb.execSQL("CREATE TRIGGER delete_message BEFORE DELETE ON messages BEGIN DELETE FROM attachments WHERE old.id = message_id; "
                            + "DELETE FROM headers where old.id = message_id; END;");
            }
            else
            { // in the case that we're starting out at 29 or newer, run all the needed updates

                if (mDb.getVersion() < 30)
                {
                    try
                    {
                        mDb.execSQL("ALTER TABLE messages ADD deleted INTEGER default 0");
                    }
                    catch (SQLiteException e)
                    {
                        if (! e.toString().startsWith("duplicate column name: deleted"))
                        {
                            throw e;
                        }
                    }
                }
                if (mDb.getVersion() < 31)
                {
                    mDb.execSQL("DROP INDEX IF EXISTS msg_folder_id_date");
                    mDb.execSQL("CREATE INDEX IF NOT EXISTS msg_folder_id_deleted_date ON messages (folder_id,deleted,internal_date)");
                }
                if (mDb.getVersion() < 32)
                {
                    mDb.execSQL("UPDATE messages SET deleted = 1 WHERE flags LIKE '%DELETED%'");
                }
                if (mDb.getVersion() < 33)
                {

                    try
                    {
                        mDb.execSQL("ALTER TABLE messages ADD preview TEXT");
                    }
                    catch (SQLiteException e)
                    {
                        if (! e.toString().startsWith("duplicate column name: preview"))
                        {
                            throw e;
                        }
                    }

                }
                if (mDb.getVersion() < 34)
                {
                    try
                    {
                        mDb.execSQL("ALTER TABLE folders ADD flagged_count INTEGER default 0");
                    }
                    catch (SQLiteException e)
                    {
                        if (! e.getMessage().startsWith("duplicate column name: flagged_count"))
                        {
                            throw e;
                        }
                    }
                }
                if (mDb.getVersion() < 35)
                {
                    try
                    {
                        mDb.execSQL("update messages set flags = replace(flags, 'X_NO_SEEN_INFO', 'X_BAD_FLAG')");
                    }
                    catch (SQLiteException e)
                    {
                        Log.e(K9.LOG_TAG, "Unable to get rid of obsolete flag X_NO_SEEN_INFO", e);
                    }
                }


            }

        }
        catch (SQLiteException e)
        {
            Log.e(K9.LOG_TAG, "Exception while upgrading database. Resetting the DB to v0");
            mDb.setVersion(0);
            throw new Error("Database upgrade failed! Resetting your DB version to 0 to force a full schema recreation.");
        }



        mDb.setVersion(DB_VERSION);

        if (mDb.getVersion() != DB_VERSION)
        {
            throw new Error("Database upgrade failed!");
        }

        try
        {
            pruneCachedAttachments(true);
        }
        catch (Exception me)
        {
            Log.e(K9.LOG_TAG, "Exception while force pruning attachments during DB update", me);
        }
    }

    public long getSize()
    {
        long attachmentLength = 0;

        File[] files = mAttachmentsDir.listFiles();
        for (File file : files)
        {
            if (file.exists())
            {
                attachmentLength += file.length();
            }
        }


        File dbFile = new File(mPath);
        return dbFile.length() + attachmentLength;
    }

    public void compact() throws MessagingException
    {
        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "Before prune size = " + getSize());

        pruneCachedAttachments();
        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "After prune / before compaction size = " + getSize());

        mDb.execSQL("VACUUM");
        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "After compaction size = " + getSize());
    }


    public void clear() throws MessagingException
    {
        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "Before prune size = " + getSize());

        pruneCachedAttachments(true);
        if (K9.DEBUG)
        {
            Log.i(K9.LOG_TAG, "After prune / before compaction size = " + getSize());

            Log.i(K9.LOG_TAG, "Before clear folder count = " + getFolderCount());
            Log.i(K9.LOG_TAG, "Before clear message count = " + getMessageCount());

            Log.i(K9.LOG_TAG, "After prune / before clear size = " + getSize());
        }
        // don't delete messages that are Local, since there is no copy on the server.
        // Don't delete deleted messages.  They are essentially placeholders for UIDs of messages that have
        // been deleted locally.  They take up insignificant space
        mDb.execSQL("DELETE FROM messages WHERE deleted = 0 and uid not like 'Local%'");
        mDb.execSQL("update folders set flagged_count = 0, unread_count = 0");

        compact();
        if (K9.DEBUG)
        {
            Log.i(K9.LOG_TAG, "After clear message count = " + getMessageCount());

            Log.i(K9.LOG_TAG, "After clear size = " + getSize());
        }
    }

    public int getMessageCount() throws MessagingException
    {
        Cursor cursor = null;
        try
        {
            cursor = mDb.rawQuery("SELECT COUNT(*) FROM messages", null);
            cursor.moveToFirst();
            int messageCount = cursor.getInt(0);
            return messageCount;
        }
        finally
        {
            if (cursor != null)
            {
                cursor.close();
            }
        }
    }

    public int getFolderCount() throws MessagingException
    {
        Cursor cursor = null;
        try
        {
            cursor = mDb.rawQuery("SELECT COUNT(*) FROM folders", null);
            cursor.moveToFirst();
            int messageCount = cursor.getInt(0);
            return messageCount;
        }
        finally
        {
            if (cursor != null)
            {
                cursor.close();
            }
        }
    }

    @Override
    public LocalFolder getFolder(String name) throws MessagingException
    {
        return new LocalFolder(name);
    }

    // TODO this takes about 260-300ms, seems slow.
    @Override
    public List<? extends Folder> getPersonalNamespaces() throws MessagingException
    {
        LinkedList<LocalFolder> folders = new LinkedList<LocalFolder>();
        Cursor cursor = null;

        try
        {
            cursor = mDb.rawQuery("SELECT id, name, unread_count, visible_limit, last_updated, status, push_state, last_pushed, flagged_count FROM folders", null);
            while (cursor.moveToNext())
            {
                LocalFolder folder = new LocalFolder(cursor.getString(1));
                folder.open(cursor.getInt(0), cursor.getString(1), cursor.getInt(2), cursor.getInt(3), cursor.getLong(4), cursor.getString(5), cursor.getString(6), cursor.getLong(7), cursor.getInt(8));

                folders.add(folder);
            }
        }
        finally
        {
            if (cursor != null)
            {
                cursor.close();
            }
        }
        return folders;
    }

    @Override
    public void checkSettings() throws MessagingException
    {
    }

    /**
     * Delete the entire Store and it's backing database.
     */
    public void delete()
    {
        try
        {
            mDb.close();
        }
        catch (Exception e)
        {

        }
        try
        {
            File[] attachments = mAttachmentsDir.listFiles();
            for (File attachment : attachments)
            {
                if (attachment.exists())
                {
                    attachment.delete();
                }
            }
            if (mAttachmentsDir.exists())
            {
                mAttachmentsDir.delete();
            }
        }
        catch (Exception e)
        {
        }
        try
        {
            new File(mPath).delete();
        }
        catch (Exception e)
        {

        }
    }

    public void recreate()
    {
        delete();
        openOrCreateDataspace(mApplication);
    }

    public void pruneCachedAttachments() throws MessagingException
    {
        pruneCachedAttachments(false);
    }

    /**
     * Deletes all cached attachments for the entire store.
     */
    public void pruneCachedAttachments(boolean force) throws MessagingException
    {

        if (force)
        {
            ContentValues cv = new ContentValues();
            cv.putNull("content_uri");
            mDb.update("attachments", cv, null, null);
        }
        File[] files = mAttachmentsDir.listFiles();
        for (File file : files)
        {
            if (file.exists())
            {
                if (!force)
                {
                    Cursor cursor = null;
                    try
                    {
                        cursor = mDb.query(
                                     "attachments",
                                     new String[] { "store_data" },
                                     "id = ?",
                                     new String[] { file.getName() },
                                     null,
                                     null,
                                     null);
                        if (cursor.moveToNext())
                        {
                            if (cursor.getString(0) == null)
                            {
                                if (K9.DEBUG)
                                    Log.d(K9.LOG_TAG, "Attachment " + file.getAbsolutePath() + " has no store data, not deleting");
                                /*
                                 * If the attachment has no store data it is not recoverable, so
                                 * we won't delete it.
                                 */
                                continue;
                            }
                        }
                    }
                    finally
                    {
                        if (cursor != null)
                        {
                            cursor.close();
                        }
                    }
                }
                if (!force)
                {
                    try
                    {
                        ContentValues cv = new ContentValues();
                        cv.putNull("content_uri");
                        mDb.update("attachments", cv, "id = ?", new String[] { file.getName() });
                    }
                    catch (Exception e)
                    {
                        /*
                         * If the row has gone away before we got to mark it not-downloaded that's
                         * okay.
                         */
                    }
                }
                if (!file.delete())
                {
                    file.deleteOnExit();
                }
            }
        }
    }

    public void resetVisibleLimits()
    {
        resetVisibleLimits(K9.DEFAULT_VISIBLE_LIMIT);
    }

    public void resetVisibleLimits(int visibleLimit)
    {
        ContentValues cv = new ContentValues();
        cv.put("visible_limit", Integer.toString(visibleLimit));
        mDb.update("folders", cv, null, null);
    }

    public ArrayList<PendingCommand> getPendingCommands()
    {
        Cursor cursor = null;
        try
        {
            cursor = mDb.query("pending_commands",
                               new String[] { "id", "command", "arguments" },
                               null,
                               null,
                               null,
                               null,
                               "id ASC");
            ArrayList<PendingCommand> commands = new ArrayList<PendingCommand>();
            while (cursor.moveToNext())
            {
                PendingCommand command = new PendingCommand();
                command.mId = cursor.getLong(0);
                command.command = cursor.getString(1);
                String arguments = cursor.getString(2);
                command.arguments = arguments.split(",");
                for (int i = 0; i < command.arguments.length; i++)
                {
                    command.arguments[i] = Utility.fastUrlDecode(command.arguments[i]);
                }
                commands.add(command);
            }
            return commands;
        }
        finally
        {
            if (cursor != null)
            {
                cursor.close();
            }
        }
    }

    public void addPendingCommand(PendingCommand command)
    {
        try
        {
            for (int i = 0; i < command.arguments.length; i++)
            {
                command.arguments[i] = URLEncoder.encode(command.arguments[i], "UTF-8");
            }
            ContentValues cv = new ContentValues();
            cv.put("command", command.command);
            cv.put("arguments", Utility.combine(command.arguments, ','));
            mDb.insert("pending_commands", "command", cv);
        }
        catch (UnsupportedEncodingException usee)
        {
            throw new Error("Aparently UTF-8 has been lost to the annals of history.");
        }
    }

    public void removePendingCommand(PendingCommand command)
    {
        mDb.delete("pending_commands", "id = ?", new String[] { Long.toString(command.mId) });
    }

    public void removePendingCommands()
    {
        mDb.delete("pending_commands", null, null);
    }

    public static class PendingCommand
    {
        private long mId;
        public String command;
        public String[] arguments;

        @Override
        public String toString()
        {
            StringBuffer sb = new StringBuffer();
            sb.append(command);
            sb.append(": ");
            for (String argument : arguments)
            {
                sb.append(", ");
                sb.append(argument);
                //sb.append("\n");
            }
            return sb.toString();
        }
    }

    @Override
    public boolean isMoveCapable()
    {
        return true;
    }

    @Override
    public boolean isCopyCapable()
    {
        return true;
    }

    public Message[] searchForMessages(MessageRetrievalListener listener, String[] queryFields, String queryString,
                                       List<LocalFolder> folders, Message[] messages, final Flag[] requiredFlags, final Flag[] forbiddenFlags) throws MessagingException
    {
        List<String> args = new LinkedList<String>();

        StringBuilder whereClause = new StringBuilder();
        if (queryString != null && queryString.length() > 0)
        {
            boolean anyAdded = false;
            String likeString = "%"+queryString+"%";
            whereClause.append(" AND (");
            for (String queryField : queryFields) {
                
                if (anyAdded == true)
                {
                    whereClause.append(" OR ");
                }
                whereClause.append(queryField + " LIKE ? ");
                args.add(likeString);
                anyAdded = true;
            }


            whereClause.append(" )");
        }
        if (folders != null && folders.size() > 0)
        {
            whereClause.append(" AND folder_id in (");
            boolean anyAdded = false;
            for (LocalFolder folder : folders)
            {
                if (anyAdded == true)
                {
                    whereClause.append(",");
                }
                anyAdded = true;
                whereClause.append("?");
                args.add(Long.toString(folder.getId()));
            }
            whereClause.append(" )");
        }
        if (messages != null && messages.length > 0)
        {
            whereClause.append(" AND ( ");
            boolean anyAdded = false;
            for (Message message : messages)
            {
                if (anyAdded == true)
                {
                    whereClause.append(" OR ");
                }
                anyAdded = true;
                whereClause.append(" ( uid = ? AND folder_id = ? ) ");
                args.add(message.getUid());
                args.add(Long.toString(((LocalFolder)message.getFolder()).getId()));
            }
            whereClause.append(" )");
        }
        if (forbiddenFlags != null && forbiddenFlags.length > 0)
        {
            whereClause.append(" AND (");
            boolean anyAdded = false;
            for (Flag flag : forbiddenFlags)
            {
                if (anyAdded == true)
                {
                    whereClause.append(" AND ");
                }
                anyAdded = true;
                whereClause.append(" flags NOT LIKE ?");

                args.add("%" + flag.toString() + "%");
            }
            whereClause.append(" )");
        }
        if (requiredFlags != null && requiredFlags.length > 0)
        {
            whereClause.append(" AND (");
            boolean anyAdded = false;
            for (Flag flag : requiredFlags)
            {
                if (anyAdded == true)
                {
                    whereClause.append(" OR ");
                }
                anyAdded = true;
                whereClause.append(" flags LIKE ?");

                args.add("%" + flag.toString() + "%");
            }
            whereClause.append(" )");
        }

        if (K9.DEBUG)
        {
            Log.v(K9.LOG_TAG, "whereClause = " + whereClause.toString());
            Log.v(K9.LOG_TAG, "args = " + args);
        }
        return getMessages(
                   listener,
                   null,
                   "SELECT "
                   + GET_MESSAGES_COLS
                   + "FROM messages WHERE deleted = 0 " + whereClause.toString() + " ORDER BY date DESC"
                   , args.toArray(new String[0])
               );
    }
    /*
     * Given a query string, actually do the query for the messages and
     * call the MessageRetrievalListener for each one
     */
    private Message[] getMessages(
        MessageRetrievalListener listener,
        LocalFolder folder,
        String queryString, String[] placeHolders
    ) throws MessagingException
    {
        ArrayList<LocalMessage> messages = new ArrayList<LocalMessage>();
        Cursor cursor = null;
        try
        {
            // pull out messages most recent first, since that's what the default sort is
            cursor = mDb.rawQuery(queryString, placeHolders);


            int i = 0;
            while (cursor.moveToNext())
            {
                LocalMessage message = new LocalMessage(null, folder);
                message.populateFromGetMessageCursor(cursor);

                messages.add(message);
                if (listener != null)
                {
                    listener.messageFinished(message, i, -1);
                }
                i++;
            }
            if (listener != null)
            {
                listener.messagesFinished(i);
            }
        }
        finally
        {
            if (cursor != null)
            {
                cursor.close();
            }
        }

        return messages.toArray(new Message[] {});

    }


    public class LocalFolder extends Folder implements Serializable
    {
        private String mName = null;
        private long mFolderId = -1;
        private int mUnreadMessageCount = -1;
        private int mFlaggedMessageCount = -1;
        private int mVisibleLimit = -1;
        private FolderClass displayClass = FolderClass.NO_CLASS;
        private FolderClass syncClass = FolderClass.INHERITED;
        private FolderClass pushClass = FolderClass.SECOND_CLASS;
        private boolean inTopGroup = false;
        private String prefId = null;
        private String mPushState = null;
        private boolean mIntegrate = false;


        public LocalFolder(String name)
        {
            super(LocalStore.this.mAccount);
            this.mName = name;

            if (K9.INBOX.equals(getName()))
            {
                syncClass =  FolderClass.FIRST_CLASS;
                pushClass =  FolderClass.FIRST_CLASS;
                inTopGroup = true;
            }


        }

        public LocalFolder(long id)
        {
            super(LocalStore.this.mAccount);
            this.mFolderId = id;
        }

        public long getId()
        {
            return mFolderId;
        }

        @Override
        public void open(OpenMode mode) throws MessagingException
        {
            if (isOpen())
            {
                return;
            }
            Cursor cursor = null;
            try
            {
                String baseQuery =
                    "SELECT id, name,unread_count, visible_limit, last_updated, status, push_state, last_pushed, flagged_count FROM folders ";
                if (mName != null)
                {
                    cursor = mDb.rawQuery(baseQuery + "where folders.name = ?", new String[] { mName });
                }
                else
                {
                    cursor = mDb.rawQuery(baseQuery + "where folders.id = ?", new String[] { Long.toString(mFolderId) });


                }

                if (cursor.moveToFirst())
                {
                    int folderId = cursor.getInt(0);
                    if (folderId > 0)
                    {
                        open(folderId, cursor.getString(1), cursor.getInt(2), cursor.getInt(3), cursor.getLong(4), cursor.getString(5), cursor.getString(6), cursor.getLong(7), cursor.getInt(8));
                    }
                }
                else
                {
                    Log.w(K9.LOG_TAG, "Creating folder " + getName() + " with existing id " + getId());
                    create(FolderType.HOLDS_MESSAGES);
                    open(mode);
                }
            }
            finally
            {
                if (cursor != null)
                {
                    cursor.close();
                }
            }
        }

        private void open(int id, String name, int unreadCount, int visibleLimit, long lastChecked, String status, String pushState, long lastPushed, int flaggedCount) throws MessagingException
        {
            mFolderId = id;
            mName = name;
            mUnreadMessageCount = unreadCount;
            mVisibleLimit = visibleLimit;
            mPushState = pushState;
            mFlaggedMessageCount = flaggedCount;
            super.setStatus(status);
            // Only want to set the local variable stored in the super class.  This class
            // does a DB update on setLastChecked
            super.setLastChecked(lastChecked);
            super.setLastPush(lastPushed);
        }

        @Override
        public boolean isOpen()
        {
            return (mFolderId != -1 && mName != null);
        }

        @Override
        public OpenMode getMode() throws MessagingException
        {
            return OpenMode.READ_WRITE;
        }

        @Override
        public String getName()
        {
            return mName;
        }

        @Override
        public boolean exists() throws MessagingException
        {
            Cursor cursor = null;
            try
            {
                cursor = mDb.rawQuery("SELECT id FROM folders "
                                      + "where folders.name = ?", new String[] { this
                                              .getName()
                                                                               });
                if (cursor.moveToFirst())
                {
                    int folderId = cursor.getInt(0);
                    return (folderId > 0) ? true : false;
                }
                else
                {
                    return false;
                }
            }
            finally
            {
                if (cursor != null)
                {
                    cursor.close();
                }
            }
        }

        @Override
        public boolean create(FolderType type) throws MessagingException
        {
            if (exists())
            {
                throw new MessagingException("Folder " + mName + " already exists.");
            }
            mDb.execSQL("INSERT INTO folders (name, visible_limit) VALUES (?, ?)", new Object[]
                        {
                            mName,
                            K9.DEFAULT_VISIBLE_LIMIT
                        });
            return true;
        }

        @Override
        public boolean create(FolderType type, int visibleLimit) throws MessagingException
        {
            if (exists())
            {
                throw new MessagingException("Folder " + mName + " already exists.");
            }
            mDb.execSQL("INSERT INTO folders (name, visible_limit) VALUES (?, ?)", new Object[]
                        {
                            mName,
                            visibleLimit
                        });
            return true;
        }

        @Override
        public void close()
        {
            mFolderId = -1;
        }

        @Override
        public int getMessageCount() throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            Cursor cursor = null;
            try
            {
                cursor = mDb.rawQuery("SELECT COUNT(*) FROM messages WHERE messages.folder_id = ?",
                                      new String[]
                                      {
                                          Long.toString(mFolderId)
                                      });
                cursor.moveToFirst();
                int messageCount = cursor.getInt(0);
                return messageCount;
            }
            finally
            {
                if (cursor != null)
                {
                    cursor.close();
                }
            }
        }

        @Override
        public int getUnreadMessageCount() throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            return mUnreadMessageCount;
        }

        @Override
        public int getFlaggedMessageCount() throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            return mFlaggedMessageCount;
        }

        public void setUnreadMessageCount(int unreadMessageCount) throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            mUnreadMessageCount = Math.max(0, unreadMessageCount);
            mDb.execSQL("UPDATE folders SET unread_count = ? WHERE id = ?",
                        new Object[] { mUnreadMessageCount, mFolderId });
        }

        public void setFlaggedMessageCount(int flaggedMessageCount) throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            mFlaggedMessageCount = Math.max(0, flaggedMessageCount);
            mDb.execSQL("UPDATE folders SET flagged_count = ? WHERE id = ?",
                        new Object[] { mFlaggedMessageCount, mFolderId });
        }

        @Override
        public void setLastChecked(long lastChecked) throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            super.setLastChecked(lastChecked);
            mDb.execSQL("UPDATE folders SET last_updated = ? WHERE id = ?",
                        new Object[] { lastChecked, mFolderId });
        }

        @Override
        public void setLastPush(long lastChecked) throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            super.setLastPush(lastChecked);
            mDb.execSQL("UPDATE folders SET last_pushed = ? WHERE id = ?",
                        new Object[] { lastChecked, mFolderId });
        }

        public int getVisibleLimit() throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            return mVisibleLimit;
        }

        public void purgeToVisibleLimit(MessageRemovalListener listener) throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            Message[] messages = getMessages(null, false);
            for (int i = mVisibleLimit; i < messages.length; i++)
            {
                if (listener != null)
                {
                    listener.messageRemoved(messages[i]);
                }
                messages[i].setFlag(Flag.X_DESTROYED, true);

            }
        }


        public void setVisibleLimit(int visibleLimit) throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            mVisibleLimit = visibleLimit;
            mDb.execSQL("UPDATE folders SET visible_limit = ? WHERE id = ?",
                        new Object[] { mVisibleLimit, mFolderId });
        }

        @Override
        public void setStatus(String status) throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            super.setStatus(status);
            mDb.execSQL("UPDATE folders SET status = ? WHERE id = ?",
                        new Object[] { status, mFolderId });
        }
        public void setPushState(String pushState) throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            mPushState = pushState;
            mDb.execSQL("UPDATE folders SET push_state = ? WHERE id = ?",
                        new Object[] { pushState, mFolderId });
        }
        public String getPushState()
        {
            return mPushState;
        }
        @Override
        public FolderClass getDisplayClass()
        {
            return displayClass;
        }

        @Override
        public FolderClass getSyncClass()
        {
            if (FolderClass.INHERITED == syncClass)
            {
                return getDisplayClass();
            }
            else
            {
                return syncClass;
            }
        }

        public FolderClass getRawSyncClass()
        {
            return syncClass;

        }

        @Override
        public FolderClass getPushClass()
        {
            if (FolderClass.INHERITED == pushClass)
            {
                return getSyncClass();
            }
            else
            {
                return pushClass;
            }
        }

        public FolderClass getRawPushClass()
        {
            return pushClass;

        }

        public void setDisplayClass(FolderClass displayClass)
        {
            this.displayClass = displayClass;
        }

        public void setSyncClass(FolderClass syncClass)
        {
            this.syncClass = syncClass;
        }
        public void setPushClass(FolderClass pushClass)
        {
            this.pushClass = pushClass;
        }

        public boolean isIntegrate()
        {
            return mIntegrate;
        }
        public void setIntegrate(boolean integrate)
        {
            mIntegrate = integrate;
        }

        private String getPrefId() throws MessagingException
        {
            open(OpenMode.READ_WRITE);

            if (prefId == null)
            {
                prefId = uUid + "." + mName;
            }

            return prefId;
        }

        public void delete(Preferences preferences) throws MessagingException
        {
            String id = getPrefId();

            SharedPreferences.Editor editor = preferences.getPreferences().edit();

            editor.remove(id + ".displayMode");
            editor.remove(id + ".syncMode");
            editor.remove(id + ".pushMode");
            editor.remove(id + ".inTopGroup");
            editor.remove(id + ".integrate");

            editor.commit();
        }

        public void save(Preferences preferences) throws MessagingException
        {
            String id = getPrefId();

            SharedPreferences.Editor editor = preferences.getPreferences().edit();
            // there can be a lot of folders.  For the defaults, let's not save prefs, saving space, except for INBOX
            if (displayClass == FolderClass.NO_CLASS && !K9.INBOX.equals(getName()))
            {
                editor.remove(id + ".displayMode");
            }
            else
            {
                editor.putString(id + ".displayMode", displayClass.name());
            }

            if (syncClass == FolderClass.INHERITED && !K9.INBOX.equals(getName()))
            {
                editor.remove(id + ".syncMode");
            }
            else
            {
                editor.putString(id + ".syncMode", syncClass.name());
            }

            if (pushClass == FolderClass.SECOND_CLASS && !K9.INBOX.equals(getName()))
            {
                editor.remove(id + ".pushMode");
            }
            else
            {
                editor.putString(id + ".pushMode", pushClass.name());
            }
            editor.putBoolean(id + ".inTopGroup", inTopGroup);

            editor.putBoolean(id + ".integrate", mIntegrate);

            editor.commit();
        }


        public FolderClass getDisplayClass(Preferences preferences) throws MessagingException
        {
            String id = getPrefId();
            return FolderClass.valueOf(preferences.getPreferences().getString(id + ".displayMode",
                                       FolderClass.NO_CLASS.name()));
        }

        @Override
        public void refresh(Preferences preferences) throws MessagingException
        {

            String id = getPrefId();

            try
            {
                displayClass = FolderClass.valueOf(preferences.getPreferences().getString(id + ".displayMode",
                                                   FolderClass.NO_CLASS.name()));
            }
            catch (Exception e)
            {
                Log.e(K9.LOG_TAG, "Unable to load displayMode for " + getName(), e);

                displayClass = FolderClass.NO_CLASS;
            }
            if (displayClass == FolderClass.NONE)
            {
                displayClass = FolderClass.NO_CLASS;
            }


            FolderClass defSyncClass = FolderClass.INHERITED;
            if (K9.INBOX.equals(getName()))
            {
                defSyncClass =  FolderClass.FIRST_CLASS;
            }

            try
            {
                syncClass = FolderClass.valueOf(preferences.getPreferences().getString(id  + ".syncMode",
                                                defSyncClass.name()));
            }
            catch (Exception e)
            {
                Log.e(K9.LOG_TAG, "Unable to load syncMode for " + getName(), e);

                syncClass = defSyncClass;
            }
            if (syncClass == FolderClass.NONE)
            {
                syncClass = FolderClass.INHERITED;
            }

            FolderClass defPushClass = FolderClass.SECOND_CLASS;
            boolean defInTopGroup = false;
            boolean defIntegrate = false;
            if (K9.INBOX.equals(getName()))
            {
                defPushClass =  FolderClass.FIRST_CLASS;
                defInTopGroup = true;
                defIntegrate = true;
            }

            try
            {
                pushClass = FolderClass.valueOf(preferences.getPreferences().getString(id  + ".pushMode",
                                                defPushClass.name()));
            }
            catch (Exception e)
            {
                Log.e(K9.LOG_TAG, "Unable to load pushMode for " + getName(), e);

                pushClass = defPushClass;
            }
            if (pushClass == FolderClass.NONE)
            {
                pushClass = FolderClass.INHERITED;
            }
            inTopGroup = preferences.getPreferences().getBoolean(id + ".inTopGroup", defInTopGroup);
            mIntegrate = preferences.getPreferences().getBoolean(id + ".integrate", defIntegrate);

        }

        @Override
        public void fetch(Message[] messages, FetchProfile fp, MessageRetrievalListener listener)
        throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            if (fp.contains(FetchProfile.Item.BODY))
            {
                for (Message message : messages)
                {
                    LocalMessage localMessage = (LocalMessage)message;
                    Cursor cursor = null;
                    MimeMultipart mp = new MimeMultipart();
                    mp.setSubType("mixed");
                    try
                    {
                        cursor = mDb.rawQuery("SELECT html_content, text_content FROM messages "
                                              + "WHERE id = ?",
                                              new String[] { Long.toString(localMessage.mId) });
                        cursor.moveToNext();
                        String htmlContent = cursor.getString(0);
                        String textContent = cursor.getString(1);

                        if (textContent != null)
                        {
                            LocalTextBody body = new LocalTextBody(textContent, htmlContent);
                            MimeBodyPart bp = new MimeBodyPart(body, "text/plain");
                            mp.addBodyPart(bp);
                        }
                        else
                        {
                            TextBody body = new TextBody(htmlContent);
                            MimeBodyPart bp = new MimeBodyPart(body, "text/html");
                            mp.addBodyPart(bp);
                        }
                    }
                    finally
                    {
                        if (cursor != null)
                        {
                            cursor.close();
                        }
                    }

                    try
                    {
                        cursor = mDb.query(
                                     "attachments",
                                     new String[]
                                     {
                                         "id",
                                         "size",
                                         "name",
                                         "mime_type",
                                         "store_data",
                                         "content_uri"
                                     },
                                     "message_id = ?",
                                     new String[] { Long.toString(localMessage.mId) },
                                     null,
                                     null,
                                     null);

                        while (cursor.moveToNext())
                        {
                            long id = cursor.getLong(0);
                            int size = cursor.getInt(1);
                            String name = cursor.getString(2);
                            String type = cursor.getString(3);
                            String storeData = cursor.getString(4);
                            String contentUri = cursor.getString(5);
                            Body body = null;
                            if (contentUri != null)
                            {
                                body = new LocalAttachmentBody(Uri.parse(contentUri), mApplication);
                            }
                            MimeBodyPart bp = new LocalAttachmentBodyPart(body, id);
                            bp.setHeader(MimeHeader.HEADER_CONTENT_TYPE,
                                         String.format("%s;\n name=\"%s\"",
                                                       type,
                                                       name));
                            bp.setHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING, "base64");
                            bp.setHeader(MimeHeader.HEADER_CONTENT_DISPOSITION,
                                         String.format("attachment;\n filename=\"%s\";\n size=%d",
                                                       name,
                                                       size));

                            /*
                             * HEADER_ANDROID_ATTACHMENT_STORE_DATA is a custom header we add to that
                             * we can later pull the attachment from the remote store if neccesary.
                             */
                            bp.setHeader(MimeHeader.HEADER_ANDROID_ATTACHMENT_STORE_DATA, storeData);

                            mp.addBodyPart(bp);
                        }
                    }
                    finally
                    {
                        if (cursor != null)
                        {
                            cursor.close();
                        }
                    }

                    if (mp.getCount() == 1)
                    {
                        BodyPart part = mp.getBodyPart(0);
                        localMessage.setHeader(MimeHeader.HEADER_CONTENT_TYPE, part.getContentType());
                        localMessage.setBody(part.getBody());
                    }
                    else
                    {
                        localMessage.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "multipart/mixed");
                        localMessage.setBody(mp);
                    }
                }
            }
        }

        @Override
        public Message[] getMessages(int start, int end, MessageRetrievalListener listener)
        throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            throw new MessagingException(
                "LocalStore.getMessages(int, int, MessageRetrievalListener) not yet implemented");
        }

        /**
         * Populate the header fields of the given list of messages by reading
         * the saved header data from the database.
         * 
         * @param messages
         *            The messages whose headers should be loaded.
         */
        private void populateHeaders(List<LocalMessage> messages)
        {
            Cursor cursor = null;
            if (messages.size() == 0)
            {
                return;
            }
            try
            {
                Map<Long, LocalMessage> popMessages = new HashMap<Long, LocalMessage>();
                List<String> ids = new ArrayList<String>();
                StringBuffer questions = new StringBuffer();

                for (int i = 0; i < messages.size(); i++)
                {
                    if (i != 0)
                    {
                        questions.append(", ");
                    }
                    questions.append("?");
                    LocalMessage message = messages.get(i);
                    Long id = message.getId();
                    ids.add(Long.toString(id));
                    popMessages.put(id, message);

                }

                cursor = mDb.rawQuery(
                             "SELECT message_id, name, value FROM headers " + "WHERE message_id in ( " + questions + ") ",
                             ids.toArray(new String[] {}));


                while (cursor.moveToNext())
                {
                    Long id = cursor.getLong(0);
                    String name = cursor.getString(1);
                    String value = cursor.getString(2);
                    //Log.i(K9.LOG_TAG, "Retrieved header name= " + name + ", value = " + value + " for message " + id);
                    popMessages.get(id).addHeader(name, value);
                }
            }
            finally
            {
                if (cursor != null)
                {
                    cursor.close();
                }
            }
        }

        @Override
        public Message getMessage(String uid) throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            LocalMessage message = new LocalMessage(uid, this);
            Cursor cursor = null;

            try
            {
                cursor = mDb.rawQuery(
                             "SELECT "
                             + GET_MESSAGES_COLS
                             + "FROM messages WHERE uid = ? AND folder_id = ?",
                             new String[]
                             {
                                 message.getUid(), Long.toString(mFolderId)
                             });
                if (!cursor.moveToNext())
                {
                    return null;
                }
                message.populateFromGetMessageCursor(cursor);
            }
            finally
            {
                if (cursor != null)
                {
                    cursor.close();
                }
            }
            return message;
        }

        @Override
        public Message[] getMessages(MessageRetrievalListener listener) throws MessagingException
        {
            return getMessages(listener, true);
        }

        @Override
        public Message[] getMessages(MessageRetrievalListener listener, boolean includeDeleted) throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            return LocalStore.this.getMessages(
                       listener,
                       this,
                       "SELECT " + GET_MESSAGES_COLS
                       + "FROM messages WHERE "
                       + (includeDeleted ? "" : "deleted = 0 AND ")
                       + " folder_id = ? ORDER BY date DESC"
                       , new String[]
                       {
                           Long.toString(mFolderId)
                       }
                   );

        }


        @Override
        public Message[] getMessages(String[] uids, MessageRetrievalListener listener)
        throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            if (uids == null)
            {
                return getMessages(listener);
            }
            ArrayList<Message> messages = new ArrayList<Message>();
            for (String uid : uids)
            {
                Message message = getMessage(uid);
                if (message != null)
                {
                    messages.add(message);
                }
            }
            return messages.toArray(new Message[] {});
        }

        @Override
        public void copyMessages(Message[] msgs, Folder folder) throws MessagingException
        {
            if (!(folder instanceof LocalFolder))
            {
                throw new MessagingException("copyMessages called with incorrect Folder");
            }
            ((LocalFolder) folder).appendMessages(msgs, true);
        }

        @Override
        public void moveMessages(Message[] msgs, Folder destFolder) throws MessagingException
        {
            if (!(destFolder instanceof LocalFolder))
            {
                throw new MessagingException("moveMessages called with non-LocalFolder");
            }

            LocalFolder lDestFolder = (LocalFolder)destFolder;
            lDestFolder.open(OpenMode.READ_WRITE);
            for (Message message : msgs)
            {
                LocalMessage lMessage = (LocalMessage)message;

                if (!message.isSet(Flag.SEEN))
                {
                    setUnreadMessageCount(getUnreadMessageCount() - 1);
                    lDestFolder.setUnreadMessageCount(lDestFolder.getUnreadMessageCount() + 1);
                }

                if (message.isSet(Flag.FLAGGED))
                {
                    setFlaggedMessageCount(getFlaggedMessageCount() - 1);
                    lDestFolder.setFlaggedMessageCount(lDestFolder.getFlaggedMessageCount() + 1);
                }

                String oldUID = message.getUid();

                if (K9.DEBUG)
                    Log.d(K9.LOG_TAG, "Updating folder_id to " + lDestFolder.getId() + " for message with UID "
                          + message.getUid() + ", id " + lMessage.getId() + " currently in folder " + getName());

                message.setUid(K9.LOCAL_UID_PREFIX + UUID.randomUUID().toString());

                mDb.execSQL("UPDATE messages " + "SET folder_id = ?, uid = ? " + "WHERE id = ?", new Object[]
                            {
                                lDestFolder.getId(),
                                message.getUid(),
                                lMessage.getId()
                            });

                LocalMessage placeHolder = new LocalMessage(oldUID, this);
                placeHolder.setFlagInternal(Flag.DELETED, true);
                placeHolder.setFlagInternal(Flag.SEEN, true);
                appendMessages(new Message[] { placeHolder });
            }

        }

        /**
         * The method differs slightly from the contract; If an incoming message already has a uid
         * assigned and it matches the uid of an existing message then this message will replace the
         * old message. It is implemented as a delete/insert. This functionality is used in saving
         * of drafts and re-synchronization of updated server messages.
         *
         * NOTE that although this method is located in the LocalStore class, it is not guaranteed
         * that the messages supplied as parameters are actually {@link LocalMessage} instances (in
         * fact, in most cases, they are not). Therefore, if you want to make local changes only to a
         * message, retrieve the appropriate local message instance first (if it already exists).
         */
        @Override
        public void appendMessages(Message[] messages) throws MessagingException
        {
            appendMessages(messages, false);
        }

        /**
         * The method differs slightly from the contract; If an incoming message already has a uid
         * assigned and it matches the uid of an existing message then this message will replace the
         * old message. It is implemented as a delete/insert. This functionality is used in saving
         * of drafts and re-synchronization of updated server messages.
         *
         * NOTE that although this method is located in the LocalStore class, it is not guaranteed
         * that the messages supplied as parameters are actually {@link LocalMessage} instances (in
         * fact, in most cases, they are not). Therefore, if you want to make local changes only to a
         * message, retrieve the appropriate local message instance first (if it already exists).
         */
        private void appendMessages(Message[] messages, boolean copy) throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            for (Message message : messages)
            {
                if (!(message instanceof MimeMessage))
                {
                    throw new Error("LocalStore can only store Messages that extend MimeMessage");
                }

                String uid = message.getUid();
                if (uid == null || copy)
                {
                    uid = K9.LOCAL_UID_PREFIX + UUID.randomUUID().toString();
                    if (!copy)
                    {
                        message.setUid(uid);
                    }
                }
                else
                {
                    Message oldMessage = getMessage(uid);
                    if (oldMessage != null && oldMessage.isSet(Flag.SEEN) == false)
                    {
                        setUnreadMessageCount(getUnreadMessageCount() - 1);
                    }
                    if (oldMessage != null && oldMessage.isSet(Flag.FLAGGED) == true)
                    {
                        setFlaggedMessageCount(getFlaggedMessageCount() - 1);
                    }
                    /*
                     * The message may already exist in this Folder, so delete it first.
                     */
                    deleteAttachments(message.getUid());
                    mDb.execSQL("DELETE FROM messages WHERE folder_id = ? AND uid = ?",
                                new Object[] { mFolderId, message.getUid() });
                }

                ArrayList<Part> viewables = new ArrayList<Part>();
                ArrayList<Part> attachments = new ArrayList<Part>();
                MimeUtility.collectParts(message, viewables, attachments);

                StringBuffer sbHtml = new StringBuffer();
                StringBuffer sbText = new StringBuffer();
                for (Part viewable : viewables)
                {
                    try
                    {
                        String text = MimeUtility.getTextFromPart(viewable);
                        /*
                         * Anything with MIME type text/html will be stored as such. Anything
                         * else will be stored as text/plain.
                         */
                        if (viewable.getMimeType().equalsIgnoreCase("text/html"))
                        {
                            sbHtml.append(text);
                        }
                        else
                        {
                            sbText.append(text);
                        }
                    }
                    catch (Exception e)
                    {
                        throw new MessagingException("Unable to get text for message part", e);
                    }
                }

                String text = sbText.toString();
                String html = markupContent(text, sbHtml.toString());
                String preview = calculateContentPreview(text);

                try
                {
                    ContentValues cv = new ContentValues();
                    cv.put("uid", uid);
                    cv.put("subject", message.getSubject());
                    cv.put("sender_list", Address.pack(message.getFrom()));
                    cv.put("date", message.getSentDate() == null
                           ? System.currentTimeMillis() : message.getSentDate().getTime());
                    cv.put("flags", Utility.combine(message.getFlags(), ',').toUpperCase());
                    cv.put("deleted", message.isSet(Flag.DELETED) ? 1 : 0);
                    cv.put("folder_id", mFolderId);
                    cv.put("to_list", Address.pack(message.getRecipients(RecipientType.TO)));
                    cv.put("cc_list", Address.pack(message.getRecipients(RecipientType.CC)));
                    cv.put("bcc_list", Address.pack(message.getRecipients(RecipientType.BCC)));
                    cv.put("html_content", html.length() > 0 ? html : null);
                    cv.put("text_content", text.length() > 0 ? text : null);
                    cv.put("preview", preview.length() > 0 ? preview : null);
                    cv.put("reply_to_list", Address.pack(message.getReplyTo()));
                    cv.put("attachment_count", attachments.size());
                    cv.put("internal_date",  message.getInternalDate() == null
                           ? System.currentTimeMillis() : message.getInternalDate().getTime());
                    String messageId = message.getMessageId();
                    if (messageId != null)
                    {
                        cv.put("message_id", messageId);
                    }
                    long messageUid = mDb.insert("messages", "uid", cv);
                    for (Part attachment : attachments)
                    {
                        saveAttachment(messageUid, attachment, copy);
                    }
                    saveHeaders(messageUid, (MimeMessage)message);
                    if (message.isSet(Flag.SEEN) == false)
                    {
                        setUnreadMessageCount(getUnreadMessageCount() + 1);
                    }
                    if (message.isSet(Flag.FLAGGED) == true)
                    {
                        setFlaggedMessageCount(getFlaggedMessageCount() + 1);
                    }
                }
                catch (Exception e)
                {
                    throw new MessagingException("Error appending message", e);
                }
            }
        }

        /**
         * Update the given message in the LocalStore without first deleting the existing
         * message (contrast with appendMessages). This method is used to store changes
         * to the given message while updating attachments and not removing existing
         * attachment data.
         * TODO In the future this method should be combined with appendMessages since the Message
         * contains enough data to decide what to do.
         * @param message
         * @throws MessagingException
         */
        public void updateMessage(LocalMessage message) throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            ArrayList<Part> viewables = new ArrayList<Part>();
            ArrayList<Part> attachments = new ArrayList<Part>();

            message.buildMimeRepresentation();

            MimeUtility.collectParts(message, viewables, attachments);

            StringBuffer sbHtml = new StringBuffer();
            StringBuffer sbText = new StringBuffer();
            for (int i = 0, count = viewables.size(); i < count; i++)
            {
                Part viewable = viewables.get(i);
                try
                {
                    String text = MimeUtility.getTextFromPart(viewable);
                    /*
                     * Anything with MIME type text/html will be stored as such. Anything
                     * else will be stored as text/plain.
                     */
                    if (viewable.getMimeType().equalsIgnoreCase("text/html"))
                    {
                        sbHtml.append(text);
                    }
                    else
                    {
                        sbText.append(text);
                    }
                }
                catch (Exception e)
                {
                    throw new MessagingException("Unable to get text for message part", e);
                }
            }

            String text = sbText.toString();
            String html = markupContent(text, sbHtml.toString());
            String preview = calculateContentPreview(text);

            try
            {
                mDb.execSQL("UPDATE messages SET "
                            + "uid = ?, subject = ?, sender_list = ?, date = ?, flags = ?, "
                            + "folder_id = ?, to_list = ?, cc_list = ?, bcc_list = ?, "
                            + "html_content = ?, text_content = ?, preview = ?, reply_to_list = ?, "
                            + "attachment_count = ? WHERE id = ?",
                            new Object[]
                            {
                                message.getUid(),
                                message.getSubject(),
                                Address.pack(message.getFrom()),
                                message.getSentDate() == null ? System
                                .currentTimeMillis() : message.getSentDate()
                                .getTime(),
                                Utility.combine(message.getFlags(), ',').toUpperCase(),
                                mFolderId,
                                Address.pack(message
                                             .getRecipients(RecipientType.TO)),
                                Address.pack(message
                                             .getRecipients(RecipientType.CC)),
                                Address.pack(message
                                             .getRecipients(RecipientType.BCC)),
                                html.length() > 0 ? html : null,
                                text.length() > 0 ? text : null,
                                preview.length() > 0 ? preview : null,
                                Address.pack(message.getReplyTo()),
                                attachments.size(),
                                message.mId
                            });

                for (int i = 0, count = attachments.size(); i < count; i++)
                {
                    Part attachment = attachments.get(i);
                    saveAttachment(message.mId, attachment, false);
                }
                saveHeaders(message.getId(), message);
            }
            catch (Exception e)
            {
                throw new MessagingException("Error appending message", e);
            }
        }

        /**
         * Save the headers of the given message. Note that the message is not
         * necessarily a {@link LocalMessage} instance.
         */
        private void saveHeaders(long id, MimeMessage message) throws MessagingException
        {
            boolean saveAllHeaders = mAccount.isSaveAllHeaders();
            boolean gotAdditionalHeaders = false;

            deleteHeaders(id);
            for (String name : message.getHeaderNames())
            {
                if (saveAllHeaders || HEADERS_TO_SAVE.contains(name))
                {
                    String[] values = message.getHeader(name);
                    for (String value : values)
                    {
                        ContentValues cv = new ContentValues();
                        cv.put("message_id", id);
                        cv.put("name", name);
                        cv.put("value", value);
                        mDb.insert("headers", "name", cv);
                    }
                }
                else
                {
                    gotAdditionalHeaders = true;
                }
            }

            if (!gotAdditionalHeaders)
            {                
                // Remember that all headers for this message have been saved, so it is
                // not necessary to download them again in case the user wants to see all headers.
                List<Flag> appendedFlags = new ArrayList<Flag>();
                appendedFlags.addAll(Arrays.asList(message.getFlags()));
                appendedFlags.add(Flag.X_GOT_ALL_HEADERS);

                mDb.execSQL("UPDATE messages " + "SET flags = ? " + " WHERE id = ?", new Object[]
                    { Utility.combine(appendedFlags.toArray(), ',').toUpperCase(), id } );
            }
        }

        private void deleteHeaders(long id)
        {
            mDb.execSQL("DELETE FROM headers WHERE id = ?",
                        new Object[]
                        {
                            id
                        });
        }

        /**
         * @param messageId
         * @param attachment
         * @param attachmentId -1 to create a new attachment or >= 0 to update an existing
         * @throws IOException
         * @throws MessagingException
         */
        private void saveAttachment(long messageId, Part attachment, boolean saveAsNew)
        throws IOException, MessagingException
        {
            long attachmentId = -1;
            Uri contentUri = null;
            int size = -1;
            File tempAttachmentFile = null;

            if ((!saveAsNew) && (attachment instanceof LocalAttachmentBodyPart))
            {
                attachmentId = ((LocalAttachmentBodyPart) attachment).getAttachmentId();
            }

            if (attachment.getBody() != null)
            {
                Body body = attachment.getBody();
                if (body instanceof LocalAttachmentBody)
                {
                    contentUri = ((LocalAttachmentBody) body).getContentUri();
                }
                else
                {
                    /*
                     * If the attachment has a body we're expected to save it into the local store
                     * so we copy the data into a cached attachment file.
                     */
                    InputStream in = attachment.getBody().getInputStream();
                    tempAttachmentFile = File.createTempFile("att", null, mAttachmentsDir);
                    FileOutputStream out = new FileOutputStream(tempAttachmentFile);
                    size = IOUtils.copy(in, out);
                    in.close();
                    out.close();
                }
            }

            if (size == -1)
            {
                /*
                 * If the attachment is not yet downloaded see if we can pull a size
                 * off the Content-Disposition.
                 */
                String disposition = attachment.getDisposition();
                if (disposition != null)
                {
                    String s = MimeUtility.getHeaderParameter(disposition, "size");
                    if (s != null)
                    {
                        size = Integer.parseInt(s);
                    }
                }
            }
            if (size == -1)
            {
                size = 0;
            }

            String storeData =
                Utility.combine(attachment.getHeader(
                                    MimeHeader.HEADER_ANDROID_ATTACHMENT_STORE_DATA), ',');

            String name = MimeUtility.getHeaderParameter(attachment.getContentType(), "name");
            String contentDisposition = MimeUtility.unfoldAndDecode(attachment.getDisposition());
            if (name == null && contentDisposition != null)
            {
                name = MimeUtility.getHeaderParameter(contentDisposition, "filename");
            }
            if (attachmentId == -1)
            {
                ContentValues cv = new ContentValues();
                cv.put("message_id", messageId);
                cv.put("content_uri", contentUri != null ? contentUri.toString() : null);
                cv.put("store_data", storeData);
                cv.put("size", size);
                cv.put("name", name);
                cv.put("mime_type", attachment.getMimeType());

                attachmentId = mDb.insert("attachments", "message_id", cv);
            }
            else
            {
                ContentValues cv = new ContentValues();
                cv.put("content_uri", contentUri != null ? contentUri.toString() : null);
                cv.put("size", size);
                mDb.update(
                    "attachments",
                    cv,
                    "id = ?",
                    new String[] { Long.toString(attachmentId) });
            }

            if (tempAttachmentFile != null)
            {
                File attachmentFile = new File(mAttachmentsDir, Long.toString(attachmentId));
                tempAttachmentFile.renameTo(attachmentFile);
                contentUri = AttachmentProvider.getAttachmentUri(
                                 new File(mPath).getName(),
                                 attachmentId);
                attachment.setBody(new LocalAttachmentBody(contentUri, mApplication));
                ContentValues cv = new ContentValues();
                cv.put("content_uri", contentUri != null ? contentUri.toString() : null);
                mDb.update(
                    "attachments",
                    cv,
                    "id = ?",
                    new String[] { Long.toString(attachmentId) });
            }

            if (attachment instanceof LocalAttachmentBodyPart)
            {
                ((LocalAttachmentBodyPart) attachment).setAttachmentId(attachmentId);
            }
        }

        /**
         * Changes the stored uid of the given message (using it's internal id as a key) to
         * the uid in the message.
         * @param message
         */
        public void changeUid(LocalMessage message) throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            ContentValues cv = new ContentValues();
            cv.put("uid", message.getUid());
            mDb.update("messages", cv, "id = ?", new String[] { Long.toString(message.mId) });
        }

        @Override
        public void setFlags(Message[] messages, Flag[] flags, boolean value)
        throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            for (Message message : messages)
            {
                message.setFlags(flags, value);
            }
        }

        @Override
        public void setFlags(Flag[] flags, boolean value)
        throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            for (Message message : getMessages(null))
            {
                message.setFlags(flags, value);
            }
        }

        @Override
        public String getUidFromMessageId(Message message) throws MessagingException
        {
            throw new MessagingException("Cannot call getUidFromMessageId on LocalFolder");
        }

        public void deleteMessagesOlderThan(long cutoff) throws MessagingException
        {
            open(OpenMode.READ_ONLY);
            mDb.execSQL("DELETE FROM messages WHERE folder_id = ? and date < ?", new Object[]
                        {
                            Long.toString(mFolderId), new Long(cutoff)
                        });
            resetUnreadAndFlaggedCounts();
        }

        private void resetUnreadAndFlaggedCounts()
        {
            try
            {
                int newUnread = 0;
                int newFlagged = 0;
                Message[] messages = getMessages(null);
                for (Message message : messages)
                {
                    if (message.isSet(Flag.SEEN) == false)
                    {
                        newUnread++;
                    }
                    if (message.isSet(Flag.FLAGGED) == true)
                    {
                        newFlagged++;
                    }
                }
                setUnreadMessageCount(newUnread);
                setFlaggedMessageCount(newFlagged);
            }
            catch (Exception e)
            {
                Log.e(K9.LOG_TAG, "Unable to fetch all messages from LocalStore", e);
            }
        }


        @Override
        public void delete(boolean recurse) throws MessagingException
        {
            // We need to open the folder first to make sure we've got it's id
            open(OpenMode.READ_ONLY);
            Message[] messages = getMessages(null);
            for (Message message : messages)
            {
                deleteAttachments(message.getUid());
            }
            mDb.execSQL("DELETE FROM folders WHERE id = ?", new Object[]
                        {
                            Long.toString(mFolderId),
                        });
        }

        @Override
        public boolean equals(Object o)
        {
            if (o instanceof LocalFolder)
            {
                return ((LocalFolder)o).mName.equals(mName);
            }
            return super.equals(o);
        }

        @Override
        public int hashCode()
        {
            return mName.hashCode();
        }

        @Override
        public Flag[] getPermanentFlags() throws MessagingException
        {
            return PERMANENT_FLAGS;
        }

        private void deleteAttachments(String uid) throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            Cursor messagesCursor = null;
            try
            {
                messagesCursor = mDb.query(
                                     "messages",
                                     new String[] { "id" },
                                     "folder_id = ? AND uid = ?",
                                     new String[] { Long.toString(mFolderId), uid },
                                     null,
                                     null,
                                     null);
                while (messagesCursor.moveToNext())
                {
                    long messageId = messagesCursor.getLong(0);
                    Cursor attachmentsCursor = null;
                    try
                    {
                        attachmentsCursor = mDb.query(
                                                "attachments",
                                                new String[] { "id" },
                                                "message_id = ?",
                                                new String[] { Long.toString(messageId) },
                                                null,
                                                null,
                                                null);
                        while (attachmentsCursor.moveToNext())
                        {
                            long attachmentId = attachmentsCursor.getLong(0);
                            try
                            {
                                File file = new File(mAttachmentsDir, Long.toString(attachmentId));
                                if (file.exists())
                                {
                                    file.delete();
                                }
                            }
                            catch (Exception e)
                            {

                            }
                        }
                    }
                    finally
                    {
                        if (attachmentsCursor != null)
                        {
                            attachmentsCursor.close();
                        }
                    }
                }
            }
            finally
            {
                if (messagesCursor != null)
                {
                    messagesCursor.close();
                }
            }
        }

        /*
         * calcualteContentPreview
         * Takes a plain text message body as a string.
         * Returns a message summary as a string suitable for showing in a message list
         *
         * A message summary should be about the first 160 characters
         * of unique text written by the message sender
         * Quoted text, "On $date" and so on will be stripped out.
         * All newlines and whitespace will be compressed.
         *
         */
        public String calculateContentPreview(String text)
        {
            if (text == null)
            {
                return null;
            }

            text = text.replaceAll("^.*:","");
            text = text.replaceAll("(?m)^>.*$","");
            text = text.replaceAll("^On .*wrote.?$","");
            text = text.replaceAll("(\\r|\\n)+"," ");
            text = text.replaceAll("\\s+"," ");
            if (text.length() <= 160)
            {
                return text;
            }
            else
            {
                text = text.substring(0,160);
                return text;
            }

        }

        public String markupContent(String text, String html)
        {
            if (text.length() > 0 && html.length() == 0)
            {
                html = htmlifyString(text);
            }

            if (html.indexOf("cid:") != -1)
            {
                return html.replaceAll("cid:", "http://cid/");
            }
            else
            {
                return html;
            }
        }

        public String htmlifyString(String text)
        {
            StringReader reader = new StringReader(text);
            StringBuilder buff = new StringBuilder(text.length() + 512);
            int c = 0;
            try
            {
                while ((c = reader.read()) != -1)
                {
                    switch (c)
                    {
                        case '&':
                            buff.append("&amp;");
                            break;
                        case '<':
                            buff.append("&lt;");
                            break;
                        case '>':
                            buff.append("&gt;");
                            break;
                        case '\r':
                            break;
                        default:
                            buff.append((char)c);
                    }//switch
                }
            }
            catch (IOException e)
            {
                //Should never happen
                Log.e(K9.LOG_TAG, null, e);
            }
            text = buff.toString();
            text = text.replaceAll("\\s*([-=_]{30,}+)\\s*","<hr />");
            text = text.replaceAll("(?m)^([^\r\n]{4,}[\\s\\w,:;+/])(?:\r\n|\n|\r)(?=[a-z]\\S{0,10}[\\s\\n\\r])","$1 ");
            text = text.replaceAll("(?m)(\r\n|\n|\r){4,}","\n\n");

            Matcher m = Regex.WEB_URL_PATTERN.matcher(text);
            StringBuffer sb = new StringBuffer(text.length() + 512);
            sb.append("<html><body><pre style=\"white-space: pre-wrap; word-wrap:break-word; \">");
            while (m.find())
            {
                int start = m.start();
                if (start == 0 || (start != 0 && text.charAt(start - 1) != '@'))
                {
                    m.appendReplacement(sb, "<a href=\"$0\">$0</a>");
                }
                else
                {
                    m.appendReplacement(sb, "$0");
                }
            }




            m.appendTail(sb);
            sb.append("</pre></body></html>");
            text = sb.toString();

            return text;
        }

        @Override
        public boolean isInTopGroup()
        {
            return inTopGroup;
        }

        public void setInTopGroup(boolean inTopGroup)
        {
            this.inTopGroup = inTopGroup;
        }
    }

    public class LocalTextBody extends TextBody
    {
        private String mBodyForDisplay;

        public LocalTextBody(String body)
        {
            super(body);
        }

        public LocalTextBody(String body, String bodyForDisplay) throws MessagingException
        {
            super(body);
            this.mBodyForDisplay = bodyForDisplay;
        }

        public String getBodyForDisplay()
        {
            return mBodyForDisplay;
        }

        public void setBodyForDisplay(String mBodyForDisplay)
        {
            this.mBodyForDisplay = mBodyForDisplay;
        }

    }//LocalTextBody

    public class LocalMessage extends MimeMessage
    {
        private long mId;
        private int mAttachmentCount;
        private String mSubject;

        private String mPreview = "";

        private boolean mHeadersLoaded = false;
        private boolean mMessageDirty = false;

        public LocalMessage()
        {
        }

        LocalMessage(String uid, Folder folder) throws MessagingException
        {
            this.mUid = uid;
            this.mFolder = folder;
        }

        private void populateFromGetMessageCursor(Cursor cursor)
        throws MessagingException
        {
            this.setSubject(cursor.getString(0) == null ? "" : cursor.getString(0));
            Address[] from = Address.unpack(cursor.getString(1));
            if (from.length > 0)
            {
                this.setFrom(from[0]);
            }
            this.setInternalSentDate(new Date(cursor.getLong(2)));
            this.setUid(cursor.getString(3));
            String flagList = cursor.getString(4);
            if (flagList != null && flagList.length() > 0)
            {
                String[] flags = flagList.split(",");

                for (String flag : flags)
                {
                    try
                    {
                        this.setFlagInternal(Flag.valueOf(flag), true);
                    }

                    catch (Exception e)
                    {
                        if ("X_BAD_FLAG".equals(flag) == false)
                        {
                            Log.w(K9.LOG_TAG, "Unable to parse flag " + flag);
                        }
                    }
                }
            }
            this.mId = cursor.getLong(5);
            this.setRecipients(RecipientType.TO, Address.unpack(cursor.getString(6)));
            this.setRecipients(RecipientType.CC, Address.unpack(cursor.getString(7)));
            this.setRecipients(RecipientType.BCC, Address.unpack(cursor.getString(8)));
            this.setReplyTo(Address.unpack(cursor.getString(9)));

            this.mAttachmentCount = cursor.getInt(10);
            this.setInternalDate(new Date(cursor.getLong(11)));
            this.setMessageId(cursor.getString(12));
            mPreview = (cursor.getString(14) == null ? "" : cursor.getString(14));
            if (this.mFolder == null)
            {
                LocalFolder f = new LocalFolder(cursor.getInt(13));
                f.open(LocalFolder.OpenMode.READ_WRITE);
                this.mFolder = f;
            }
        }


        /* Custom version of writeTo that updates the MIME message based on localMessage
         * changes.
         */

        @Override
        public void writeTo(OutputStream out) throws IOException, MessagingException
        {
            if (mMessageDirty) buildMimeRepresentation();
            super.writeTo(out);
        }

        private void buildMimeRepresentation() throws MessagingException
        {
            if (!mMessageDirty)
            {
                return;
            }

            super.setSubject(mSubject);
            if (this.mFrom != null && this.mFrom.length > 0)
            {
                super.setFrom(this.mFrom[0]);
            }

            super.setReplyTo(mReplyTo);
            super.setSentDate(this.getSentDate());
            super.setRecipients(RecipientType.TO, mTo);
            super.setRecipients(RecipientType.CC, mCc);
            super.setRecipients(RecipientType.BCC, mBcc);
            if (mMessageId != null) super.setMessageId(mMessageId);

            mMessageDirty = false;
            return;
        }

        public String getPreview()
        {
            return mPreview;
        }

        @Override
        public String getSubject() throws MessagingException
        {
            return mSubject;
        }


        @Override
        public void setSubject(String subject) throws MessagingException
        {
            mSubject = subject;
            mMessageDirty = true;
        }


        @Override
        public void setMessageId(String messageId)
        {
            mMessageId = messageId;
            mMessageDirty = true;
        }



        public int getAttachmentCount()
        {
            return mAttachmentCount;
        }

        @Override
        public void setFrom(Address from) throws MessagingException
        {
            this.mFrom = new Address[] { from };
            mMessageDirty = true;
        }


        @Override
        public void setReplyTo(Address[] replyTo) throws MessagingException
        {
            if (replyTo == null || replyTo.length == 0)
            {
                mReplyTo = null;
            }
            else
            {
                mReplyTo = replyTo;
            }
            mMessageDirty = true;
        }


        /*
         * For performance reasons, we add headers instead of setting them (see super implementation)
         * which removes (expensive) them before adding them
         */
        @Override
        public void setRecipients(RecipientType type, Address[] addresses) throws MessagingException
        {
            if (type == RecipientType.TO)
            {
                if (addresses == null || addresses.length == 0)
                {
                    this.mTo = null;
                }
                else
                {
                    this.mTo = addresses;
                }
            }
            else if (type == RecipientType.CC)
            {
                if (addresses == null || addresses.length == 0)
                {
                    this.mCc = null;
                }
                else
                {
                    this.mCc = addresses;
                }
            }
            else if (type == RecipientType.BCC)
            {
                if (addresses == null || addresses.length == 0)
                {
                    this.mBcc = null;
                }
                else
                {
                    this.mBcc = addresses;
                }
            }
            else
            {
                throw new MessagingException("Unrecognized recipient type.");
            }
            mMessageDirty = true;
        }



        public void setFlagInternal(Flag flag, boolean set) throws MessagingException
        {
            super.setFlag(flag, set);
        }

        public long getId()
        {
            return mId;
        }

        @Override
        public void setFlag(Flag flag, boolean set) throws MessagingException
        {
            if (flag == Flag.DELETED && set)
            {
                /*
                 * If a message is being marked as deleted we want to clear out it's content
                 * and attachments as well. Delete will not actually remove the row since we need
                 * to retain the uid for synchronization purposes.
                 */

                /*
                 * Delete all of the messages' content to save space.
                 */
                ((LocalFolder) mFolder).deleteAttachments(getUid());

                mDb.execSQL(
                    "UPDATE messages SET " +
                    "deleted = 1," +
                    "subject = NULL, " +
                    "sender_list = NULL, " +
                    "date = NULL, " +
                    "to_list = NULL, " +
                    "cc_list = NULL, " +
                    "bcc_list = NULL, " +
                    "preview = NULL, " +
                    "html_content = NULL, " +
                    "text_content = NULL, " +
                    "reply_to_list = NULL " +
                    "WHERE id = ?",
                    new Object[]
                    {
                        mId
                    });

                /*
                 * Delete all of the messages' attachments to save space.
                 */
                mDb.execSQL("DELETE FROM attachments WHERE message_id = ?",
                            new Object[]
                            {
                                mId
                            });

                ((LocalFolder)mFolder).deleteHeaders(mId);

            }
            else if (flag == Flag.X_DESTROYED && set)
            {
                ((LocalFolder) mFolder).deleteAttachments(getUid());
                mDb.execSQL("DELETE FROM messages WHERE id = ?",
                            new Object[] { mId });
                ((LocalFolder)mFolder).deleteHeaders(mId);
            }

            /*
             * Update the unread count on the folder.
             */
            try
            {
                if (flag == Flag.DELETED || flag == Flag.X_DESTROYED
                        || (flag == Flag.SEEN && !isSet(Flag.DELETED)))
                {
                    LocalFolder folder = (LocalFolder)mFolder;
                    if (set && !isSet(Flag.SEEN))
                    {
                        folder.setUnreadMessageCount(folder.getUnreadMessageCount() - 1);
                    }
                    else if (!set && isSet(Flag.SEEN))
                    {
                        folder.setUnreadMessageCount(folder.getUnreadMessageCount() + 1);
                    }
                }
                if ((flag == Flag.DELETED || flag == Flag.X_DESTROYED) && isSet(Flag.FLAGGED))
                {
                    LocalFolder folder = (LocalFolder)mFolder;
                    if (set)
                    {
                        folder.setFlaggedMessageCount(folder.getFlaggedMessageCount() - 1);
                    }
                    else
                    {
                        folder.setFlaggedMessageCount(folder.getFlaggedMessageCount() + 1);
                    }
                }
                if (flag == Flag.FLAGGED && !isSet(Flag.DELETED))
                {
                    LocalFolder folder = (LocalFolder)mFolder;
                    if (set)
                    {
                        folder.setFlaggedMessageCount(folder.getFlaggedMessageCount() + 1);
                    }
                    else
                    {
                        folder.setFlaggedMessageCount(folder.getFlaggedMessageCount() - 1);
                    }
                }
            }
            catch (MessagingException me)
            {
                Log.e(K9.LOG_TAG, "Unable to update LocalStore unread message count",
                      me);
                throw new RuntimeException(me);
            }

            super.setFlag(flag, set);
            /*
             * Set the flags on the message.
             */
            mDb.execSQL("UPDATE messages " + "SET flags = ? " + " WHERE id = ?", new Object[]
                        {
                            Utility.combine(getFlags(), ',').toUpperCase(), mId
                        });
        }


        private void loadHeaders()
        {
            ArrayList<LocalMessage> messages = new ArrayList<LocalMessage>();
            messages.add(this);
            mHeadersLoaded = true; // set true before calling populate headers to stop recursion
            ((LocalFolder) mFolder).populateHeaders(messages);

        }

        @Override
        public void addHeader(String name, String value)
        {
            if (!mHeadersLoaded)
                loadHeaders();
            super.addHeader(name, value);
        }

        @Override
        public void setHeader(String name, String value)
        {
            if (!mHeadersLoaded)
                loadHeaders();
            super.setHeader(name, value);
        }

        @Override
        public String[] getHeader(String name)
        {
            if (!mHeadersLoaded)
                loadHeaders();
            return super.getHeader(name);
        }

        @Override
        public void removeHeader(String name)
        {
            if (!mHeadersLoaded)
                loadHeaders();
            super.removeHeader(name);
        }

        @Override
        public Set<String> getHeaderNames() {
            if (!mHeadersLoaded)
                loadHeaders();
            return super.getHeaderNames();
        }
    }

    public class LocalAttachmentBodyPart extends MimeBodyPart
    {
        private long mAttachmentId = -1;

        public LocalAttachmentBodyPart(Body body, long attachmentId) throws MessagingException
        {
            super(body);
            mAttachmentId = attachmentId;
        }

        /**
         * Returns the local attachment id of this body, or -1 if it is not stored.
         * @return
         */
        public long getAttachmentId()
        {
            return mAttachmentId;
        }

        public void setAttachmentId(long attachmentId)
        {
            mAttachmentId = attachmentId;
        }

        @Override
        public String toString()
        {
            return "" + mAttachmentId;
        }
    }

    public static class LocalAttachmentBody implements Body
    {
        private Application mApplication;
        private Uri mUri;

        public LocalAttachmentBody(Uri uri, Application application)
        {
            mApplication = application;
            mUri = uri;
        }

        public InputStream getInputStream() throws MessagingException
        {
            try
            {
                return mApplication.getContentResolver().openInputStream(mUri);
            }
            catch (FileNotFoundException fnfe)
            {
                /*
                 * Since it's completely normal for us to try to serve up attachments that
                 * have been blown away, we just return an empty stream.
                 */
                return new ByteArrayInputStream(new byte[0]);
            }
        }

        public void writeTo(OutputStream out) throws IOException, MessagingException
        {
            InputStream in = getInputStream();
            Base64OutputStream base64Out = new Base64OutputStream(out);
            IOUtils.copy(in, base64Out);
            base64Out.close();
        }

        public Uri getContentUri()
        {
            return mUri;
        }
    }
}
