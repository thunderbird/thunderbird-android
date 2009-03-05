
package com.android.email;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.util.Config;
import android.util.Log;

import com.android.email.activity.FolderMessageList;
import com.android.email.mail.Address;
import com.android.email.mail.Body;
import com.android.email.mail.FetchProfile;
import com.android.email.mail.Flag;
import com.android.email.mail.Folder;
import com.android.email.mail.Message;
import com.android.email.mail.MessageRetrievalListener;
import com.android.email.mail.MessagingException;
import com.android.email.mail.Part;
import com.android.email.mail.Store;
import com.android.email.mail.Transport;
import com.android.email.mail.Folder.FolderType;
import com.android.email.mail.Folder.OpenMode;
import com.android.email.mail.internet.MimeMessage;
import com.android.email.mail.internet.MimeUtility;
import com.android.email.mail.internet.TextBody;
import com.android.email.mail.store.LocalStore;
import com.android.email.mail.store.LocalStore.LocalFolder;
import com.android.email.mail.store.LocalStore.LocalMessage;
import com.android.email.mail.store.LocalStore.PendingCommand;

/**
 * Starts a long running (application) Thread that will run through commands
 * that require remote mailbox access. This class is used to serialize and
 * prioritize these commands. Each method that will submit a command requires a
 * MessagingListener instance to be provided. It is expected that that listener
 * has also been added as a registered listener using addListener(). When a
 * command is to be executed, if the listener that was provided with the command
 * is no longer registered the command is skipped. The design idea for the above
 * is that when an Activity starts it registers as a listener. When it is paused
 * it removes itself. Thus, any commands that that activity submitted are
 * removed from the queue once the activity is no longer active.
 */
public class MessagingController implements Runnable {
    /**
     * The maximum message size that we'll consider to be "small". A small message is downloaded
     * in full immediately instead of in pieces. Anything over this size will be downloaded in
     * pieces with attachments being left off completely and downloaded on demand.
     *
     *
     * 25k for a "small" message was picked by educated trial and error.
     * http://answers.google.com/answers/threadview?id=312463 claims that the
     * average size of an email is 59k, which I feel is too large for our
     * blind download. The following tests were performed on a download of
     * 25 random messages.
     * <pre>
     * 5k - 61 seconds,
     * 25k - 51 seconds,
     * 55k - 53 seconds,
     * </pre>
     * So 25k gives good performance and a reasonable data footprint. Sounds good to me.
     */
	// Daniel I. Applebaum Changing to 5k for faster syncing
    private static final int MAX_SMALL_MESSAGE_SIZE = (5 * 1024);

    private static final String PENDING_COMMAND_MOVE_OR_COPY =
        "com.android.email.MessagingController.moveOrCopy";
    private static final String PENDING_COMMAND_EMPTY_TRASH =
      "com.android.email.MessagingController.emptyTrash";
    private static final String PENDING_COMMAND_SET_FLAG =
        "com.android.email.MessagingController.setFlag";
    private static final String PENDING_COMMAND_APPEND =
        "com.android.email.MessagingController.append";
    private static final String PENDING_COMMAND_MARK_ALL_AS_READ =
      "com.android.email.MessagingController.markAllAsRead";
    private static final String PENDING_COMMAND_CLEAR_ALL_PENDING =
      "com.android.email.MessagingController.clearAllPending";


    private static MessagingController inst = null;
    private BlockingQueue<Command> mCommands = new LinkedBlockingQueue<Command>();
    private BlockingQueue<Command> backCommands = new LinkedBlockingQueue<Command>();

    private Thread mThread;
    //private Set<MessagingListener> mListeners = Collections.synchronizedSet(new HashSet<MessagingListener>());
    private Set<MessagingListener> mListeners = new CopyOnWriteArraySet<MessagingListener>();
    
    private HashMap<SORT_TYPE, Boolean> sortAscending = new HashMap<SORT_TYPE, Boolean>();
  
    public enum SORT_TYPE { 
      SORT_DATE(R.string.sort_earliest_first, R.string.sort_latest_first, false),
      SORT_SUBJECT(R.string.sort_subject_alpha, R.string.sort_subject_re_alpha, true), 
      SORT_SENDER(R.string.sort_sender_alpha, R.string.sort_sender_re_alpha, true), 
      SORT_UNREAD(R.string.sort_unread_first, R.string.sort_unread_last, true),
      SORT_FLAGGED(R.string.sort_flagged_first, R.string.sort_flagged_last, true), 
      SORT_ATTACHMENT(R.string.sort_attach_first, R.string.sort_unattached_first, true);
      
      private int ascendingToast;
      private int descendingToast;
      private boolean defaultAscending;
      
      SORT_TYPE(int ascending, int descending, boolean ndefaultAscending)
      {
        ascendingToast = ascending;
        descendingToast = descending;
        defaultAscending = ndefaultAscending;
      }
      
      public int getToast(boolean ascending)
      {
        if (ascending)
        {
          return ascendingToast;
        }
        else
        {
          return descendingToast;
        }
      }
      public boolean isDefaultAscending()
      {
        return defaultAscending;
      }
    };
    private SORT_TYPE sortType = SORT_TYPE.SORT_DATE;
    
    private MessagingListener checkMailListener = null;
    
    private boolean mBusy;
    private Application mApplication;
    
    // Key is accountUuid:folderName:messageUid   ,   value is unimportant
    private ConcurrentHashMap<String, String> deletedUids = new ConcurrentHashMap<String, String>();
    
    // Key is accountUuid:folderName   ,  value is a long of the highest message UID ever emptied from Trash
    private ConcurrentHashMap<String, Long> expungedUid = new ConcurrentHashMap<String, Long>();

    
    private String createMessageKey(Account account, String folder, Message message)
    {
      return createMessageKey(account, folder, message.getUid());
    }
    
    private String createMessageKey(Account account, String folder, String uid)
    {
      return account.getUuid() + ":" + folder + ":" + uid;
    }
    
    private String createFolderKey(Account account, String folder)
    {
      return account.getUuid() + ":" + folder;
    }
    
    private void suppressMessage(Account account, String folder, Message message)
    {
      
      if (account == null || folder == null || message == null)
      {
        return;
      }
      String messKey = createMessageKey(account, folder, message);
     // Log.d(Email.LOG_TAG, "Suppressing message with key " + messKey);
      deletedUids.put(messKey, "true");
    }
    
    private void unsuppressMessage(Account account, String folder, Message message)
    {
      if (account == null || folder == null || message == null)
      {
        return;
      }
      unsuppressMessage(account, folder, message.getUid());
    }
    
    private void unsuppressMessage(Account account, String folder, String uid)
    {
      if (account == null || folder == null || uid == null)
      {
        return;
      }
      String messKey = createMessageKey(account, folder, uid);
      //Log.d(Email.LOG_TAG, "Unsuppressing message with key " + messKey);
      deletedUids.remove(messKey);
    }
    
    
    private boolean isMessageSuppressed(Account account, String folder, Message message)
    {
      if (account == null || folder == null || message == null)
      {
        return false;
      }
      String messKey = createMessageKey(account, folder, message);
      //Log.d(Email.LOG_TAG, "Checking suppression of message with key " + messKey);
      if (deletedUids.containsKey(messKey))
      {
        //Log.d(Email.LOG_TAG, "Message with key " + messKey + " is suppressed");
        return true;
      }
      Long expungedUidL = expungedUid.get(createFolderKey(account, folder));
      if (expungedUidL != null)
      {
        long expungedUid = expungedUidL;
        String messageUidS = message.getUid();
        try
        {
          long messageUid = Long.parseLong(messageUidS);
          if (messageUid <= expungedUid)
          {
            return false;
          }
        }
        catch (NumberFormatException nfe)
        {
          // Nothing to do
        }
      }
      return false;
    }
    
    
    
    
    private MessagingController(Application application) {
        mApplication = application;
        mThread = new Thread(this);
        mThread.start();
    }
    
    public void log(String logmess)
    {
      Log.d(Email.LOG_TAG, logmess);
      if (Email.logFile != null)
      {
        FileOutputStream fos = null;
        try
        {
          File logFile = new File(Email.logFile);
          fos = new FileOutputStream(logFile, true);
          PrintStream ps = new PrintStream(fos);
          ps.println(new Date() + ":" + Email.LOG_TAG + ":" + logmess);
          ps.flush();
          ps.close();
          fos.flush();
          fos.close();
        }
        catch (Exception e)
        {
          Log.e(Email.LOG_TAG, "Unable to log message '" + logmess + "'", e);
        }
        finally
        {
          if (fos != null)
          {
            try
            {
              fos.close();
            }
            catch (Exception e)
            {
              
            }
          }
        }
      }
    }

    /**
     * Gets or creates the singleton instance of MessagingController. Application is used to
     * provide a Context to classes that need it.
     * @param application
     * @return
     */
    public synchronized static MessagingController getInstance(Application application) {
        if (inst == null) {
            inst = new MessagingController(application);
        }
        return inst;
    }

    public boolean isBusy() {
        return mBusy;
    }

    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        while (true) {
          String commandDescription = null;
            try {
                Command command = mCommands.poll();
                if (command == null)
                {
                	 command = backCommands.poll();
                }
                if (command == null)
                {
                	command = mCommands.poll(1, TimeUnit.SECONDS);
                }
                
                if (command != null)
                {
                  commandDescription = command.description;
                  Log.d(Email.LOG_TAG, "Running background command '" + command.description + "'");
                  mBusy = true;
                  command.runnable.run();
                  Log.d(Email.LOG_TAG, "Background command '" + command.description + "' completed");
                  for (MessagingListener l : getListeners()) {
                      l.controllerCommandCompleted(mCommands.size() > 0);
                  }
                  if (command.listener != null && !mListeners.contains(command.listener)) {
                    command.listener.controllerCommandCompleted(mCommands.size() > 0);
                  }
	              }
            }
            catch (Exception e) {
                Log.e(Email.LOG_TAG, "Error running command '" + commandDescription + "'", e);
            }
            mBusy = false;
        }
    }

    private void put(String description, MessagingListener listener, Runnable runnable) {
        try {
            Command command = new Command();
            command.listener = listener;
            command.runnable = runnable;
            command.description = description;
            mCommands.put(command);
        }
        catch (InterruptedException ie) {
            throw new Error(ie);
        }
    }
    
    private void putBackground(String description, MessagingListener listener, Runnable runnable) {
      try {
          Command command = new Command();
          command.listener = listener;
          command.runnable = runnable;
          command.description = description;
          backCommands.put(command);
      }
      catch (InterruptedException ie) {
          throw new Error(ie);
      }
  }


    public void addListener(MessagingListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(MessagingListener listener) {
        mListeners.remove(listener);
    }
    
    public Set<MessagingListener> getListeners()
    {
    	return mListeners;
    }

    /**
     * Lists folders that are available locally and remotely. This method calls
     * listFoldersCallback for local folders before it returns, and then for
     * remote folders at some later point. If there are no local folders
     * includeRemote is forced by this method. This method should be called from
     * a Thread as it may take several seconds to list the local folders. 
     * TODO this needs to cache the remote folder list
     *
     * @param account
     * @param includeRemote
     * @param listener
     * @throws MessagingException
     */
    public void listFolders(
            final Account account,
            boolean refreshRemote,
            MessagingListener listener) {
        for (MessagingListener l : getListeners()) {
            l.listFoldersStarted(account);
        }
        if (listener != null) {
          listener.listFoldersStarted(account);
        }
        try {
            Store localStore = Store.getInstance(account.getLocalStoreUri(), mApplication);
            Folder[] localFolders = localStore.getPersonalNamespaces();

            if ( refreshRemote || localFolders == null || localFolders.length == 0) {
                doRefreshRemote(account, listener);
                return;
            }
            for (MessagingListener l : getListeners()) {
                l.listFolders(account, localFolders);
            }
            if (listener != null)
            {
              listener.listFolders(account, localFolders);
            }
        }
        catch (Exception e) {
            for (MessagingListener l : getListeners()) {
                l.listFoldersFailed(account, e.getMessage());
            }
            if (listener != null) {
              listener.listFoldersFailed(account, e.getMessage());
            }
            addErrorMessage(account, e);
            return;
        }
        for (MessagingListener l : getListeners()) {
                l.listFoldersFinished(account);
        }
        if (listener != null) {
          listener.listFoldersFinished(account);
        }
       
    }

    private void doRefreshRemote (final Account account, MessagingListener listener) {
            put("doRefreshRemote", listener, new Runnable() {
                public void run() {
                    try {
                        Store store = Store.getInstance(account.getStoreUri(), mApplication);

                        Folder[] remoteFolders = store.getPersonalNamespaces();

                        LocalStore localStore = (LocalStore)Store.getInstance(
                                account.getLocalStoreUri(),
                                mApplication);
                        HashSet<String> remoteFolderNames = new HashSet<String>();
                        for (int i = 0, count = remoteFolders.length; i < count; i++) {
                            LocalFolder localFolder = localStore.getFolder(remoteFolders[i].getName());
                            if (!localFolder.exists()) {
                                localFolder.create(FolderType.HOLDS_MESSAGES, account.getDisplayCount());
                            }
                            remoteFolderNames.add(remoteFolders[i].getName());
                        }

                        Folder[] localFolders = localStore.getPersonalNamespaces();

                        /*
                         * Clear out any folders that are no longer on the remote store.
                         */
                        for (Folder localFolder : localFolders) {
                            String localFolderName = localFolder.getName();
                            if (localFolderName.equalsIgnoreCase(Email.INBOX) ||
                                    localFolderName.equals(account.getTrashFolderName()) ||
                                    localFolderName.equals(account.getOutboxFolderName()) ||
                                    localFolderName.equals(account.getDraftsFolderName()) ||
                                    localFolderName.equals(account.getSentFolderName()) ||
                                    localFolderName.equals(account.getErrorFolderName())) {
                                continue;
                            }
                            if (!remoteFolderNames.contains(localFolder.getName())) {
                                localFolder.delete(false);
                            }
                        }

                        localFolders = localStore.getPersonalNamespaces();

                        for (MessagingListener l : getListeners()) {
                            l.listFolders(account, localFolders);
                        }
                        for (MessagingListener l : getListeners()) {
                            l.listFoldersFinished(account);
                        }
                    }
                    catch (Exception e) {
                        for (MessagingListener l : getListeners()) {
                            l.listFoldersFailed(account, "");
                        }
                        addErrorMessage(account, e);
                    }
                }
            });
    }



    /**
     * List the local message store for the given folder. This work is done
     * synchronously.
     *
     * @param account
     * @param folder
     * @param listener
     * @throws MessagingException
     */
    public void listLocalMessages(final Account account, final String folder,
            MessagingListener listener) {
        for (MessagingListener l : getListeners()) {
            l.listLocalMessagesStarted(account, folder);
        }

        try {
            Store localStore = Store.getInstance(account.getLocalStoreUri(), mApplication);
            Folder localFolder = localStore.getFolder(folder);
            localFolder.open(OpenMode.READ_WRITE);
            Message[] localMessages = localFolder.getMessages(null);
            ArrayList<Message> messages = new ArrayList<Message>();
            for (Message message : localMessages) {
              
                if (!message.isSet(Flag.DELETED) &&
                    isMessageSuppressed(account, localFolder.getName(), message) == false) {
                    messages.add(message);
                }
            }
            for (MessagingListener l : getListeners()) {
                l.listLocalMessages(account, folder, messages.toArray(new Message[0]));
            }
            for (MessagingListener l : getListeners()) {
                l.listLocalMessagesFinished(account, folder);
            }
        }
        catch (Exception e) {
            for (MessagingListener l : getListeners()) {
                l.listLocalMessagesFailed(account, folder, e.getMessage());
            }
            addErrorMessage(account, e);
        }
    }

    public void loadMoreMessages(Account account, String folder, MessagingListener listener) {
        try {
            LocalStore localStore = (LocalStore) Store.getInstance(
                    account.getLocalStoreUri(),
                    mApplication);
            LocalFolder localFolder = (LocalFolder) localStore.getFolder(folder);
            localFolder.setVisibleLimit(localFolder.getVisibleLimit()
                    + account.getDisplayCount());
            synchronizeMailbox(account, folder, listener);
        }
        catch (MessagingException me) {
          addErrorMessage(account, me);

            throw new RuntimeException("Unable to set visible limit on folder", me);
        }
    }

    public void resetVisibleLimits(Account[] accounts) {
        for (Account account : accounts) {
            try {
                LocalStore localStore =
                    (LocalStore) Store.getInstance(account.getLocalStoreUri(), mApplication);
                localStore.resetVisibleLimits(account.getDisplayCount());
            }
            catch (MessagingException e) {
              addErrorMessage(account, e);

                Log.e(Email.LOG_TAG, "Unable to reset visible limits", e);
            }
        }
    }

    /**
     * Start background synchronization of the specified folder.
     * @param account
     * @param folder
     * @param listener
     */
    public void synchronizeMailbox(final Account account, final String folder,
            MessagingListener listener) {
        put("synchronizeMailbox", listener, new Runnable() {
            public void run() {
                synchronizeMailboxSynchronous(account, folder);
            }
        });
    }

    /**
     * Start foreground synchronization of the specified folder. This is generally only called
     * by synchronizeMailbox.
     * @param account
     * @param folder
     *
     * TODO Break this method up into smaller chunks.
     */
    public void synchronizeMailboxSynchronous(final Account account, final String folder) {
      /*
       * We don't ever sync the Outbox.
       */
      if (folder.equals(account.getOutboxFolderName())) {
          return;
      }
    	if (account.getErrorFolderName().equals(folder)){
    		return;
    	}
    	String debugLine = "Synchronizing folder " + account.getDescription() + ":" + folder;
    	if (Config.LOGV) {
    		Log.v(Email.LOG_TAG, debugLine);
    	}
    	log(debugLine);
 
        for (MessagingListener l : getListeners()) {
            l.synchronizeMailboxStarted(account, folder);
        }
        LocalFolder tLocalFolder = null;
        Exception commandException = null;
        try {
        		if (Config.LOGV) {
        			Log.v(Email.LOG_TAG, "SYNC: About to process pending commands for folder " + 
        					account.getDescription() + ":" + folder);
        		}
        		try
        		{
        			processPendingCommandsSynchronous(account);
        		}
        		catch (Exception e)
        		{
              addErrorMessage(account, e);

        			Log.e(Email.LOG_TAG, "Failure processing command, but allow message sync attempt", e);
        			commandException = e;
        		}

            /*
             * Get the message list from the local store and create an index of
             * the uids within the list.
             */
        		if (Config.LOGV) {
        			Log.v(Email.LOG_TAG, "SYNC: About to get local folder " + folder);
        		}
            final LocalStore localStore =
                (LocalStore) Store.getInstance(account.getLocalStoreUri(), mApplication);
            tLocalFolder = (LocalFolder) localStore.getFolder(folder);
            final LocalFolder localFolder = tLocalFolder; 
            localFolder.open(OpenMode.READ_WRITE);
            Message[] localMessages = localFolder.getMessages(null);
            HashMap<String, Message> localUidMap = new HashMap<String, Message>();
            for (Message message : localMessages) {
                localUidMap.put(message.getUid(), message);
            }

         		if (Config.LOGV) {
        			Log.v(Email.LOG_TAG, "SYNC: About to get remote store for " + folder);
        		}

            Store remoteStore = Store.getInstance(account.getStoreUri(), mApplication);
         		if (Config.LOGV) {
        			Log.v(Email.LOG_TAG, "SYNC: About to get remote folder " + folder);
        		}

            Folder remoteFolder = remoteStore.getFolder(folder);

            /*
             * If the folder is a "special" folder we need to see if it exists
             * on the remote server. It if does not exist we'll try to create it. If we
             * can't create we'll abort. This will happen on every single Pop3 folder as
             * designed and on Imap folders during error conditions. This allows us
             * to treat Pop3 and Imap the same in this code.
             */
            if (folder.equals(account.getTrashFolderName()) ||
                    folder.equals(account.getSentFolderName()) ||
                    folder.equals(account.getDraftsFolderName())) {
                if (!remoteFolder.exists()) {
                    if (!remoteFolder.create(FolderType.HOLDS_MESSAGES)) {
                        for (MessagingListener l : getListeners()) {
                            l.synchronizeMailboxFinished(account, folder, 0, 0);
                        }
                        Log.i(Email.LOG_TAG, "Done synchronizing folder " + folder);
                        return;
                    }
                }
            }

            /*
             * Synchronization process:
                Open the folder
                Upload any local messages that are marked as PENDING_UPLOAD (Drafts, Sent, Trash)
                Get the message count
                Get the list of the newest Email.DEFAULT_VISIBLE_LIMIT messages
                    getMessages(messageCount - Email.DEFAULT_VISIBLE_LIMIT, messageCount)
                See if we have each message locally, if not fetch it's flags and envelope
                Get and update the unread count for the folder
                Update the remote flags of any messages we have locally with an internal date
                    newer than the remote message.
                Get the current flags for any messages we have locally but did not just download
                    Update local flags
                For any message we have locally but not remotely, delete the local message to keep
                    cache clean.
                Download larger parts of any new messages.
                (Optional) Download small attachments in the background.
             */

            /*
             * Open the remote folder. This pre-loads certain metadata like message count.
             */
         		if (Config.LOGV) {
        			Log.v(Email.LOG_TAG, "SYNC: About to open remote folder " + folder);
        		}
            remoteFolder.open(OpenMode.READ_WRITE);


            /*
             * Get the remote message count.
             */
            int remoteMessageCount = remoteFolder.getMessageCount();

            int visibleLimit = localFolder.getVisibleLimit();

            Message[] remoteMessageArray = new Message[0];
            final ArrayList<Message> remoteMessages = new ArrayList<Message>();
            final ArrayList<Message> unsyncedMessages = new ArrayList<Message>();
            HashMap<String, Message> remoteUidMap = new HashMap<String, Message>();

         		if (Config.LOGV) {
        			Log.v(Email.LOG_TAG, "SYNC: Remote message count for folder " + folder + " is " + 
        					remoteMessageCount);
        		}

            if (remoteMessageCount > 0) {
                /*
                 * Message numbers start at 1.
                 */
                int remoteStart = Math.max(0, remoteMessageCount - visibleLimit) + 1;
                int remoteEnd = remoteMessageCount;
            		if (Config.LOGV) {
            			Log.v(Email.LOG_TAG, "SYNC: About to get messages " + remoteStart + " through "
            					+ remoteEnd + " for folder " + folder);
            		}

                remoteMessageArray = remoteFolder.getMessages(remoteStart, remoteEnd, null);
                for (Message thisMess : remoteMessageArray)
                {
                	remoteMessages.add(thisMess);
                }
             		if (Config.LOGV) {
            			Log.v(Email.LOG_TAG, "SYNC: Got messages for folder " + folder);
            		}

                for (Message message : remoteMessages) {
                    remoteUidMap.put(message.getUid(), message);
                }


                
                
                
                
                
                /*
                 * Get a list of the messages that are in the remote list but not on the
                 * local store, or messages that are in the local store but failed to download
                 * on the last sync. These are the new messages that we will download.
                 */
                Iterator<Message> iter = remoteMessages.iterator();
                while (iter.hasNext()) {
                	Message message = iter.next();
                    Message localMessage = localUidMap.get(message.getUid());
                    if (localMessage == null ||
                            (!localMessage.isSet(Flag.DELETED) &&
                             !localMessage.isSet(Flag.X_DOWNLOADED_FULL) &&
                             !localMessage.isSet(Flag.X_DOWNLOADED_PARTIAL))) {
                        unsyncedMessages.add(message);
                        iter.remove();
                    }
                }
            }



            /*
             * A list of messages that were downloaded and which did not have the Seen flag set.
             * This will serve to indicate the true "new" message count that will be reported to
             * the user via notification.
             */
            final ArrayList<Message> newMessages = new ArrayList<Message>();

            /*
             * Fetch the flags and envelope only of the new messages. This is intended to get us
s             * critical data as fast as possible, and then we'll fill in the details.
             */
            if (unsyncedMessages.size() > 0) {

                /*
                 * Reverse the order of the messages. Depending on the server this may get us
                 * fetch results for newest to oldest. If not, no harm done.
                 */
                Collections.reverse(unsyncedMessages);

                FetchProfile fp = new FetchProfile();
                if (remoteFolder.supportsFetchingFlags()) {
                    fp.add(FetchProfile.Item.FLAGS);
                }
                fp.add(FetchProfile.Item.ENVELOPE);

                if (Config.LOGV) {
            			Log.v(Email.LOG_TAG, "SYNC: About to sync unsynced messages for folder " + folder);
            		}

                remoteFolder.fetch(unsyncedMessages.toArray(new Message[0]), fp,
                        new MessageRetrievalListener() {
                            public void messageFinished(Message message, int number, int ofTotal) {
                                try {
                                		if (!message.isSet(Flag.SEEN)) {
                                			newMessages.add(message);
                                		}

                                    // Store the new message locally
                                    localFolder.appendMessages(new Message[] {
                                        message
                                    });

                                    // And include it in the view
                                    if (message.getSubject() != null &&
                                            message.getFrom() != null) {
                                        /*
                                         * We check to make sure that we got something worth
                                         * showing (subject and from) because some protocols
                                         * (POP) may not be able to give us headers for
                                         * ENVELOPE, only size.
                                         */
                                      if (isMessageSuppressed(account, folder, message) == false)
                                      {
                                        for (MessagingListener l : getListeners()) {
                                            l.synchronizeMailboxNewMessage(account, folder,
                                                    localFolder.getMessage(message.getUid()));
                                        }
                                      }
                                    }

                                }
                                catch (Exception e) {
                                    Log.e(Email.LOG_TAG,
                                            "Error while storing downloaded message.",
                                            e);
                                    addErrorMessage(account, e);

                                }
                            }

                            public void messageStarted(String uid, int number, int ofTotal) {
                            }
                        });
                if (Config.LOGV) {
            			Log.v(Email.LOG_TAG, "SYNC: Synced unsynced messages for folder " + folder);
            		}

            }

            /*
             * Refresh the flags for any messages in the local store that we didn't just
             * download.
             */
            if (remoteFolder.supportsFetchingFlags()) {
               
              if (Config.LOGV) {
          			Log.v(Email.LOG_TAG, "SYNC: About to sync remote messages for folder " + folder);
          		}
  
              FetchProfile fp = new FetchProfile();
              fp.add(FetchProfile.Item.FLAGS);
              remoteFolder.fetch(remoteMessages.toArray(new Message[0]), fp, null);
              for (Message remoteMessage : remoteMessages) {
                boolean messageChanged = false;
                  Message localMessage = localFolder.getMessage(remoteMessage.getUid());
                  if (localMessage == null) {
                      continue;
                  }
                  for (Flag flag : new Flag[] { Flag.SEEN, Flag.FLAGGED, Flag.ANSWERED } )
                  {
                    if (remoteMessage.isSet(flag) != localMessage.isSet(flag)) {
                      localMessage.setFlag(flag, remoteMessage.isSet(flag));
                      messageChanged = true;
                    }
                  }
                  if (messageChanged && isMessageSuppressed(account, folder, localMessage) == false)
                  {
                    for (MessagingListener l : getListeners()) {
                      l.synchronizeMailboxNewMessage(account, folder, localMessage);
                    }
                  }
               }
            }
            if (Config.LOGV) {
        			Log.v(Email.LOG_TAG, "SYNC: Synced remote messages for folder " + folder);
        		}


            /*
             * Get and store the unread message count.
             */
            int remoteUnreadMessageCount = remoteFolder.getUnreadMessageCount();
            if (remoteUnreadMessageCount == -1) {
                localFolder.setUnreadMessageCount(localFolder.getUnreadMessageCount()
                        + newMessages.size());
            }
            else {
                localFolder.setUnreadMessageCount(remoteUnreadMessageCount);
            }

            /*
             * Remove any messages that are in the local store but no longer on the remote store.
             */
            for (Message localMessage : localMessages) {
                if (remoteUidMap.get(localMessage.getUid()) == null) {
                    localMessage.setFlag(Flag.X_DESTROYED, true);
                    for (MessagingListener l : getListeners()) {
                        l.synchronizeMailboxRemovedMessage(account, folder, localMessage);
                    }
                }
            }

            /*
             * Now we download the actual content of messages.
             */
            ArrayList<Message> largeMessages = new ArrayList<Message>();
            ArrayList<Message> smallMessages = new ArrayList<Message>();
            for (Message message : unsyncedMessages) {
                /*
                 * Sort the messages into two buckets, small and large. Small messages will be
                 * downloaded fully and large messages will be downloaded in parts. By sorting
                 * into two buckets we can pipeline the commands for each set of messages
                 * into a single command to the server saving lots of round trips.
                 */
                if (message.getSize() > (MAX_SMALL_MESSAGE_SIZE)) {
                    largeMessages.add(message);
                } else {
                    smallMessages.add(message);
                }
            }
            
            if (Config.LOGV) {
        			Log.v(Email.LOG_TAG, "SYNC: Have " + largeMessages.size() + " large messages and "
        					+ smallMessages.size() + " small messages to fetch for folder " + folder);
        		}

            
            /*
             * Grab the content of the small messages first. This is going to
             * be very fast and at very worst will be a single up of a few bytes and a single
             * download of 625k.
             */
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.BODY);
            if (Config.LOGV) {
        			Log.v(Email.LOG_TAG, "SYNC: Fetching small messages for folder " + folder);
        		}

            remoteFolder.fetch(smallMessages.toArray(new Message[smallMessages.size()]),
                    fp, new MessageRetrievalListener() {
                public void messageFinished(Message message, int number, int ofTotal) {
                    try {
                        // Store the updated message locally
                        localFolder.appendMessages(new Message[] {
                            message
                        });

                        Message localMessage = localFolder.getMessage(message.getUid());

                        // Set a flag indicating this message has now be fully downloaded
                        localMessage.setFlag(Flag.X_DOWNLOADED_FULL, true);
                        if (isMessageSuppressed(account, folder, localMessage) == false)
                        {
                          // Update the listener with what we've found
                          for (MessagingListener l : getListeners()) {
                              l.synchronizeMailboxNewMessage(
                                      account,
                                      folder,
                                      localMessage);
                          }
                        }
                    }
                    catch (MessagingException me) {
                      addErrorMessage(account, me);

                    	Log.e(Email.LOG_TAG, "SYNC: fetch small messages", me);
                    }
                }

                public void messageStarted(String uid, int number, int ofTotal) {
                }
            });
            if (Config.LOGV) {
        			Log.v(Email.LOG_TAG, "SYNC: Done fetching small messages for folder " + folder);
        		}

            /*
             * Now do the large messages that require more round trips.
             */
            fp.clear();
            fp.add(FetchProfile.Item.STRUCTURE);
            if (Config.LOGV) {
        			Log.v(Email.LOG_TAG, "SYNC: Fetching large messages for folder " + folder);
        		}

            remoteFolder.fetch(largeMessages.toArray(new Message[largeMessages.size()]),
                    fp, null);
            for (Message message : largeMessages) {
                if (message.getBody() == null) {
                    /*
                     * The provider was unable to get the structure of the message, so
                     * we'll download a reasonable portion of the messge and mark it as
                     * incomplete so the entire thing can be downloaded later if the user
                     * wishes to download it.
                     */
                    fp.clear();
                    fp.add(FetchProfile.Item.BODY_SANE);
                    /*
                     *  TODO a good optimization here would be to make sure that all Stores set
                     *  the proper size after this fetch and compare the before and after size. If
                     *  they equal we can mark this SYNCHRONIZED instead of PARTIALLY_SYNCHRONIZED
                     */

                    remoteFolder.fetch(new Message[] { message }, fp, null);
                    // Store the updated message locally
                    localFolder.appendMessages(new Message[] {
                        message
                    });

                    Message localMessage = localFolder.getMessage(message.getUid());

                    /*
                     * Mark the message as fully downloaded if the message size is smaller than
                     * the FETCH_BODY_SANE_SUGGESTED_SIZE, otherwise mark as only a partial
                     * download.  This will prevent the system from downloading the same message 
                     * twice.
                     */
                    if (message.getSize() < Store.FETCH_BODY_SANE_SUGGESTED_SIZE) {
                        localMessage.setFlag(Flag.X_DOWNLOADED_FULL, true);
                    } else {
                        // Set a flag indicating that the message has been partially downloaded and
                        // is ready for view.
                        localMessage.setFlag(Flag.X_DOWNLOADED_PARTIAL, true);
                    }
                } else {
                    /*
                     * We have a structure to deal with, from which
                     * we can pull down the parts we want to actually store.
                     * Build a list of parts we are interested in. Text parts will be downloaded
                     * right now, attachments will be left for later.
                     */

                    ArrayList<Part> viewables = new ArrayList<Part>();
                    ArrayList<Part> attachments = new ArrayList<Part>();
                    MimeUtility.collectParts(message, viewables, attachments);

                    /*
                     * Now download the parts we're interested in storing.
                     */
                    for (Part part : viewables) {
                        fp.clear();
                        fp.add(part);
                        // TODO what happens if the network connection dies? We've got partial
                        // messages with incorrect status stored.
                        remoteFolder.fetch(new Message[] { message }, fp, null);
                    }
                    // Store the updated message locally
                    localFolder.appendMessages(new Message[] {
                        message
                    });

                    Message localMessage = localFolder.getMessage(message.getUid());

                    // Set a flag indicating this message has been fully downloaded and can be
                    // viewed.
                    localMessage.setFlag(Flag.X_DOWNLOADED_FULL, true);
                }
                if (isMessageSuppressed(account, folder, message) == false)
                {
                  // Update the listener with what we've found
                  for (MessagingListener l : getListeners()) {
                      l.synchronizeMailboxNewMessage(
                              account,
                              folder,
                              localFolder.getMessage(message.getUid()));
                  }
                }
            }
            if (Config.LOGV) {
        			Log.v(Email.LOG_TAG, "SYNC: Done fetching large messages for folder " + folder);
        		}


            /*
             * Notify listeners that we're finally done.
             */
  
            localFolder.setLastChecked(System.currentTimeMillis());
            localFolder.setStatus(null);
            
            remoteFolder.close(false);
            localFolder.close(false);
            if (Config.LOGD) {
            	log( "Done synchronizing folder " + 
            			account.getDescription() + ":" + folder + " @ " + new Date() + 
            			" with " + newMessages.size() + " new messages"); 
            }
          
            for (MessagingListener l : getListeners()) {
              l.synchronizeMailboxFinished(
                      account,
                      folder,
                      remoteMessageCount, newMessages.size());
            }
             
           if (commandException != null)
           {
             String rootMessage = getRootCauseMessage(commandException);
             localFolder.setStatus(rootMessage);
           	for (MessagingListener l : getListeners()) {
               l.synchronizeMailboxFailed(
                       account,
                       folder,
                       rootMessage);
           	}
           }

 
        }
        catch (Exception e) {
	        	Log.e(Email.LOG_TAG, "synchronizeMailbox", e);
	        	// If we don't set the last checked, it can try too often during
	        	// failure conditions
	        	String rootMessage = getRootCauseMessage(e);
	        	if (tLocalFolder != null)
	        	{
	        		try
	        		{
	        			tLocalFolder.setStatus(rootMessage);
	        			tLocalFolder.setLastChecked(System.currentTimeMillis());
	        			tLocalFolder.close(false);
	        		}
	        		catch (MessagingException me)
	        		{
	        			Log.e(Email.LOG_TAG, "Could not set last checked on folder " + account.getDescription() + ":" + 
	        					tLocalFolder.getName(), e);
	        		}
	        	}
	
            for (MessagingListener l : getListeners()) {
                l.synchronizeMailboxFailed(
                        account,
                        folder,
                        rootMessage);
            }
            addErrorMessage(account, e);
           	log("Failed synchronizing folder " + 
          			account.getDescription() + ":" + folder + " @ " + new Date()); 

        }
        
    }
    
    private String getRootCauseMessage(Throwable t)
    {
    	Throwable rootCause = t;
    	Throwable nextCause = rootCause;
    	do
    	{
    		nextCause = rootCause.getCause();
    		if (nextCause != null)
    		{
    			rootCause = nextCause;
    		}
    	} while (nextCause != null);
      return rootCause.getMessage();
    }

    private void queuePendingCommand(Account account, PendingCommand command) {
        try {
            LocalStore localStore = (LocalStore) Store.getInstance(
                    account.getLocalStoreUri(),
                    mApplication);
            localStore.addPendingCommand(command);
        }
        catch (Exception e) {
          addErrorMessage(account, e);

            throw new RuntimeException("Unable to enqueue pending command", e);
        }
    }

    private void processPendingCommands(final Account account) {
        put("processPendingCommands", null, new Runnable() {
            public void run() {
                try {
                    processPendingCommandsSynchronous(account);
                }
                catch (MessagingException me) {
                    if (Config.LOGV) {
                        Log.v(Email.LOG_TAG, "processPendingCommands", me);
                    }
                    addErrorMessage(account, me);

                    /*
                     * Ignore any exceptions from the commands. Commands will be processed
                     * on the next round.
                     */
                }
            }
        });
    }

    private void processPendingCommandsSynchronous(Account account) throws MessagingException {
        LocalStore localStore = (LocalStore) Store.getInstance(
                account.getLocalStoreUri(),
                mApplication);
        ArrayList<PendingCommand> commands = localStore.getPendingCommands();
        PendingCommand processingCommand = null;
        try
        {
	        for (PendingCommand command : commands) {
	        	processingCommand = command;
	        	Log.d(Email.LOG_TAG, "Processing pending command '" + command + "'");
	            /*
	             * We specifically do not catch any exceptions here. If a command fails it is
	             * most likely due to a server or IO error and it must be retried before any
	             * other command processes. This maintains the order of the commands.
	             */
	            if (PENDING_COMMAND_APPEND.equals(command.command)) {
	                processPendingAppend(command, account);
	            }
	            else if (PENDING_COMMAND_SET_FLAG.equals(command.command)) {
	                processPendingSetFlag(command, account);
	            }
	            else if (PENDING_COMMAND_MARK_ALL_AS_READ.equals(command.command)) {
                processPendingMarkAllAsRead(command, account);
	            }
	            else if (PENDING_COMMAND_MOVE_OR_COPY.equals(command.command)) {
	                processPendingMoveOrCopy(command, account);
	            }
	            else if (PENDING_COMMAND_EMPTY_TRASH.equals(command.command)) {
                processPendingEmptyTrash(command, account);
	            }
	            localStore.removePendingCommand(command);
	            Log.d(Email.LOG_TAG, "Done processing pending command '" + command + "'");

	        }
        }
        catch (MessagingException me)
        {
          addErrorMessage(account, me);

        	Log.e(Email.LOG_TAG, "Could not process command '" + processingCommand + "'", me);
        	throw me;
        }
    }

    /**
     * Process a pending append message command. This command uploads a local message to the
     * server, first checking to be sure that the server message is not newer than
     * the local message. Once the local message is successfully processed it is deleted so
     * that the server message will be synchronized down without an additional copy being
     * created.
     * TODO update the local message UID instead of deleteing it
     *
     * @param command arguments = (String folder, String uid)
     * @param account
     * @throws MessagingException
     */
    private void processPendingAppend(PendingCommand command, Account account)
            throws MessagingException {
    	
    	
        String folder = command.arguments[0];
        String uid = command.arguments[1];

        if (account.getErrorFolderName().equals(folder))
    		{
    			return;
    		}

        LocalStore localStore = (LocalStore) Store.getInstance(
                account.getLocalStoreUri(),
                mApplication);
        LocalFolder localFolder = (LocalFolder) localStore.getFolder(folder);
        LocalMessage localMessage = (LocalMessage) localFolder.getMessage(uid);

        if (localMessage == null) {
            return;
        }
        
        Store remoteStore = Store.getInstance(account.getStoreUri(), mApplication);
        Folder remoteFolder = remoteStore.getFolder(folder);
        if (!remoteFolder.exists()) {
            if (!remoteFolder.create(FolderType.HOLDS_MESSAGES)) {
                return;
            }
        }
        remoteFolder.open(OpenMode.READ_WRITE);
        if (remoteFolder.getMode() != OpenMode.READ_WRITE) {
            return;
        }

        Message remoteMessage = null;
        if (!localMessage.getUid().startsWith("Local")
                && !localMessage.getUid().contains("-")) {
            remoteMessage = remoteFolder.getMessage(localMessage.getUid());
        }

        if (remoteMessage == null) {
        		if (localMessage.isSet(Flag.X_REMOTE_COPY_STARTED))
        		{
         			Log.w(Email.LOG_TAG, "Local message with uid " + localMessage.getUid() + 
          				" has flag " + Flag.X_REMOTE_COPY_STARTED + " already set, checking for remote message with " +
          				" same message id");
        			String rUid = remoteFolder.getUidFromMessageId(localMessage);
        			if (rUid != null)
        			{
	        			Log.w(Email.LOG_TAG, "Local message has flag " + Flag.X_REMOTE_COPY_STARTED + " already set, and there is a remote message with " +
	          				" uid " + rUid + ", assuming message was already copied and aborting this copy");
	        			
	        			String oldUid = localMessage.getUid();
	        			localMessage.setUid(rUid);
	        			localFolder.changeUid(localMessage);
	        			for (MessagingListener l : getListeners()) {
	                l.messageUidChanged(account, folder, oldUid, localMessage.getUid());
	        			}
	        			return;
        			}
        			else
        			{
        				Log.w(Email.LOG_TAG, "No remote message with message-id found, proceeding with append");
        			}
        		}

            /*
             * If the message does not exist remotely we just upload it and then
             * update our local copy with the new uid.
             */
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.BODY);
            localFolder.fetch(new Message[] { localMessage }, fp, null);
            String oldUid = localMessage.getUid();
            localMessage.setFlag(Flag.X_REMOTE_COPY_STARTED, true);
            remoteFolder.appendMessages(new Message[] { localMessage });
             
            localFolder.changeUid(localMessage);
            for (MessagingListener l : getListeners()) {
                l.messageUidChanged(account, folder, oldUid, localMessage.getUid());
            }
        }
        else {
            /*
             * If the remote message exists we need to determine which copy to keep.
             */
            /*
             * See if the remote message is newer than ours.
             */
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            remoteFolder.fetch(new Message[] { remoteMessage }, fp, null);
            Date localDate = localMessage.getInternalDate();
            Date remoteDate = remoteMessage.getInternalDate();
            if (remoteDate.compareTo(localDate) > 0) {
                /*
                 * If the remote message is newer than ours we'll just
                 * delete ours and move on. A sync will get the server message
                 * if we need to be able to see it.
                 */
                localMessage.setFlag(Flag.DELETED, true);
            }
            else {
                /*
                 * Otherwise we'll upload our message and then delete the remote message.
                 */
                fp.clear();
                fp = new FetchProfile();
                fp.add(FetchProfile.Item.BODY);
                localFolder.fetch(new Message[] { localMessage }, fp, null);
                String oldUid = localMessage.getUid();

                localMessage.setFlag(Flag.X_REMOTE_COPY_STARTED, true);

                remoteFolder.appendMessages(new Message[] { localMessage });
                localFolder.changeUid(localMessage);
                for (MessagingListener l : getListeners()) {
                    l.messageUidChanged(account, folder, oldUid, localMessage.getUid());
                }
                remoteMessage.setFlag(Flag.DELETED, true);
                remoteFolder.expunge();
            }
        }
    }

    /**
     * Process a pending trash message command.
     *
     * @param command arguments = (String folder, String uid)
     * @param account
     * @throws MessagingException
     */
    private void processPendingMoveOrCopy(PendingCommand command, Account account)
            throws MessagingException {
        String srcFolder = command.arguments[0];
        String uid = command.arguments[1];
        String destFolder = command.arguments[2];
        String isCopyS = command.arguments[3];
        
        boolean isCopy = false;
        if (isCopyS != null)
        {
          isCopy = Boolean.parseBoolean(isCopyS);
        }

        if (account.getErrorFolderName().equals(srcFolder))
    		{
    			return;
    		}

        Store remoteStore = Store.getInstance(account.getStoreUri(), mApplication);
        Folder remoteSrcFolder = remoteStore.getFolder(srcFolder);
        Folder remoteDestFolder = remoteStore.getFolder(destFolder);
        
        if (!remoteSrcFolder.exists()) {
        	Log.w(Email.LOG_TAG, "processingPendingMoveOrCopy: remoteFolder " + srcFolder + " does not exist");
            return;
        }
        remoteSrcFolder.open(OpenMode.READ_WRITE);
        if (remoteSrcFolder.getMode() != OpenMode.READ_WRITE) {
         	Log.w(Email.LOG_TAG, "processingPendingMoveOrCopy: could not open remoteSrcFolder " + srcFolder + " read/write");
            return;
        }
        
        remoteDestFolder.open(OpenMode.READ_WRITE);
        if (remoteDestFolder.getMode() != OpenMode.READ_WRITE) {
          Log.w(Email.LOG_TAG, "processingPendingMoveOrCopy: could not open remoteDestFolder " + srcFolder + " read/write");
            return;
        }
        
        Message remoteMessage = null;
        if (!uid.startsWith("Local")
                && !uid.contains("-")) {
       // Why bother with this, perhaps just pass the UID to the store to save a roundtrip?  And check for error returns, of course
       // Same applies for deletion
            remoteMessage = remoteSrcFolder.getMessage(uid);      
        }
        if (remoteMessage == null) {
            Log.w(Email.LOG_TAG, "processingPendingMoveOrCopy: remoteMessage " + uid + " does not exist");
            return;
        }
        if (Config.LOGD)
        {
        	Log.d(Email.LOG_TAG, "processingPendingMoveOrCopy: source folder = " + srcFolder 
        	    + ", uid = " + uid + ", destination folder = " + destFolder + ", isCopy = " + isCopy);
        }
        
        if (isCopy) {
          remoteSrcFolder.copyMessages(new Message[] { remoteMessage }, remoteDestFolder);
        }
        else {
          remoteSrcFolder.moveMessages(new Message[] { remoteMessage }, remoteDestFolder);
        }
        remoteSrcFolder.close(true);
        remoteDestFolder.close(true);
        
 
    }

    /**
     * Processes a pending mark read or unread command.
     *
     * @param command arguments = (String folder, String uid, boolean read)
     * @param account
     */
    private void processPendingSetFlag(PendingCommand command, Account account)
            throws MessagingException {
        String folder = command.arguments[0];
        String uid = command.arguments[1];
        
        if (account.getErrorFolderName().equals(folder))
    		{
    			return;
    		}
        
        boolean newState = Boolean.parseBoolean(command.arguments[2]);
        
        Flag flag = Flag.valueOf(command.arguments[3]);

        Store remoteStore = Store.getInstance(account.getStoreUri(), mApplication);
        Folder remoteFolder = remoteStore.getFolder(folder);
        if (!remoteFolder.exists()) {
            return;
        }
        remoteFolder.open(OpenMode.READ_WRITE);
        if (remoteFolder.getMode() != OpenMode.READ_WRITE) {
            return;
        }
        Message remoteMessage = null;
        if (!uid.startsWith("Local")
                && !uid.contains("-")) {
            remoteMessage = remoteFolder.getMessage(uid);
        }
        if (remoteMessage == null) {
            return;
        }
        remoteMessage.setFlag(flag, newState);
    }
    
    private void processPendingMarkAllAsRead(PendingCommand command, Account account) throws MessagingException {
				String folder = command.arguments[0];
				
	      Store localStore = Store.getInstance(account.getLocalStoreUri(), mApplication);
	      LocalFolder localFolder = (LocalFolder)localStore.getFolder(folder);
	      localFolder.open(OpenMode.READ_WRITE);
	      Message[] messages = localFolder.getMessages(null);
	      for (Message message : messages)
	      {
	      	if (message.isSet(Flag.SEEN) == false) 
	      	{
	      		message.setFlag(Flag.SEEN, true);
	      	}
	      }
	      localFolder.setUnreadMessageCount(0);
        for (MessagingListener l : getListeners()) {
          l.folderStatusChanged(account, folder);
        }
				try
				{
	        if (account.getErrorFolderName().equals(folder))
	    		{
	    			return;
	    		}
					
	        Store remoteStore = Store.getInstance(account.getStoreUri(), mApplication);
					Folder remoteFolder = remoteStore.getFolder(folder);
		      
					if (!remoteFolder.exists()) {
					    return;
					}
					remoteFolder.open(OpenMode.READ_WRITE);
					if (remoteFolder.getMode() != OpenMode.READ_WRITE) {
					    return;
					}
								
					remoteFolder.setFlags(new Flag[] { Flag.SEEN }, true);
					remoteFolder.close(false);
				}
				catch (UnsupportedOperationException uoe)
				{
					Log.w(Email.LOG_TAG, "Could not mark all server-side as read because store doesn't support operation", uoe);
				}
				finally
				{
		      localFolder.close(false);
				}

    }

    static long uidfill = 0;
    static AtomicBoolean loopCatch = new AtomicBoolean();
    public void addErrorMessage(Account account, Throwable t)
    {
      if (Email.ENABLE_ERROR_FOLDER == false)
      {
        return;
      }
    	if (loopCatch.compareAndSet(false, true) == false)
    	{
    		return;
    	}
    	try
    	{
	    	if (t == null)
	    	{
	    		return;
	    	}

	    	String rootCauseMessage = getRootCauseMessage(t);
	    	log("Error" + "'" + rootCauseMessage + "'");
	    	
    		Store localStore = Store.getInstance(account.getLocalStoreUri(), mApplication);
    		LocalFolder localFolder = (LocalFolder)localStore.getFolder(account.getErrorFolderName());
    		Message[] messages = new Message[1];
    		Message message = new MimeMessage();
    		ByteArrayOutputStream baos = new ByteArrayOutputStream();
    		PrintStream ps = new PrintStream(baos);
    		t.printStackTrace(ps);
    		ps.close();
    		message.setBody(new TextBody(baos.toString()));
    		message.setFlag(Flag.X_DOWNLOADED_FULL, true);
    		message.setSubject(rootCauseMessage);
    		
    		long nowTime = System.currentTimeMillis();
    		Date nowDate = new Date(nowTime);
    		message.setInternalDate(nowDate);
    		message.setSentDate(nowDate);
    		message.setFrom(new Address(account.getEmail(), "K9mail internal"));
    		messages[0] = message;
    		
    		localFolder.appendMessages(messages);
    		
    		localFolder.deleteMessagesOlderThan(nowTime - (15 * 60 * 1000));
    		
    	}
    	catch (Throwable it)
    	{
    			Log.e(Email.LOG_TAG, "Could not save error message to " + account.getErrorFolderName(), it);
    	}
    	finally
    	{
    		loopCatch.set(false);
    	}
    }
    
    
    public void markAllMessagesRead(final Account account, final String folder)
    {
    	Log.v(Email.LOG_TAG, "Marking all messages in " + account.getDescription() + ":" + folder + " as read");
      List<String> args = new ArrayList<String>();
      args.add(folder);
      PendingCommand command = new PendingCommand();
      command.command = PENDING_COMMAND_MARK_ALL_AS_READ;
      command.arguments = args.toArray(new String[0]);
      queuePendingCommand(account, command);
      processPendingCommands(account);
    }
    

    
    
    
    /**
     * Mark the message with the given account, folder and uid either Seen or not Seen.
     * @param account
     * @param folder
     * @param uid
     * @param seen
     */
    public void markMessageRead(
            final Account account,
            final String folder,
            final String uid,
            final boolean seen) {
      setMessageFlag(account, folder, uid, Flag.SEEN, seen);
    }
    
    
    public void setMessageFlag(
        final Account account,
        final String folder,
        final String uid,
        final Flag flag,
        final boolean newState) {
      // TODO: put this into the background, but right now that causes odd behavior
      // because the FolderMessageList doesn't have its own cache of the flag states
        try {
            Store localStore = Store.getInstance(account.getLocalStoreUri(), mApplication);
            Folder localFolder = localStore.getFolder(folder);
            localFolder.open(OpenMode.READ_WRITE);

            Message message = localFolder.getMessage(uid);
            message.setFlag(flag, newState);
            
            for (MessagingListener l : getListeners()) {
              l.folderStatusChanged(account, folder);
          }
            
            if (account.getErrorFolderName().equals(folder))
        		{
        			return;
        		}
            
            PendingCommand command = new PendingCommand();
            command.command = PENDING_COMMAND_SET_FLAG;
            command.arguments = new String[] { folder, uid, Boolean.toString(newState), flag.toString() };
            queuePendingCommand(account, command);
            processPendingCommands(account);
        }
        catch (MessagingException me) {
          addErrorMessage(account, me);

            throw new RuntimeException(me);
        }
    }
    
    public void clearAllPending(final Account account) 
    {
    	try
    	{
    		Log.w(Email.LOG_TAG, "Clearing pending commands!");
    		LocalStore localStore = (LocalStore)Store.getInstance(account.getLocalStoreUri(), mApplication);
    		localStore.removePendingCommands();
    	}
    	catch (MessagingException me)
    	{
    			Log.e(Email.LOG_TAG, "Unable to clear pending command", me);
    			addErrorMessage(account, me);
    	}
    }

    private void loadMessageForViewRemote(final Account account, final String folder,
            final String uid, MessagingListener listener) {
        put("loadMessageForViewRemote", listener, new Runnable() {
            public void run() {
                try {
                    Store localStore = Store.getInstance(account.getLocalStoreUri(), mApplication);
                    LocalFolder localFolder = (LocalFolder) localStore.getFolder(folder);
                    localFolder.open(OpenMode.READ_WRITE);

                    Message message = localFolder.getMessage(uid);

                    // This is a view message request, so mark it read
                    if (!message.isSet(Flag.SEEN)) {
                        markMessageRead(account, folder, uid, true);
                    }

                    if (message.isSet(Flag.X_DOWNLOADED_FULL)) {
                        /*
                         * If the message has been synchronized since we were called we'll
                         * just hand it back cause it's ready to go.
                         */
                        FetchProfile fp = new FetchProfile();
                        fp.add(FetchProfile.Item.ENVELOPE);
                        fp.add(FetchProfile.Item.BODY);
                        localFolder.fetch(new Message[] { message }, fp, null);

                        for (MessagingListener l : getListeners()) {
                            l.loadMessageForViewBodyAvailable(account, folder, uid, message);
                        }
                        for (MessagingListener l : getListeners()) {
                            l.loadMessageForViewFinished(account, folder, uid, message);
                        }
                        localFolder.close(false);
                        return;
                    }

                    /*
                     * At this point the message is not available, so we need to download it
                     * fully if possible.
                     */

                    Store remoteStore = Store.getInstance(account.getStoreUri(), mApplication);
                    Folder remoteFolder = remoteStore.getFolder(folder);
                    remoteFolder.open(OpenMode.READ_WRITE);

                    // Get the remote message and fully download it
                    Message remoteMessage = remoteFolder.getMessage(uid);
                    FetchProfile fp = new FetchProfile();
                    fp.add(FetchProfile.Item.BODY);
                    remoteFolder.fetch(new Message[] { remoteMessage }, fp, null);

                    // Store the message locally and load the stored message into memory
                    localFolder.appendMessages(new Message[] { remoteMessage });
                    message = localFolder.getMessage(uid);
                    localFolder.fetch(new Message[] { message }, fp, null);

 
                    // Mark that this message is now fully synched
                    message.setFlag(Flag.X_DOWNLOADED_FULL, true);

                    for (MessagingListener l : getListeners()) {
                        l.loadMessageForViewBodyAvailable(account, folder, uid, message);
                    }
                    for (MessagingListener l : getListeners()) {
                        l.loadMessageForViewFinished(account, folder, uid, message);
                    }
                    remoteFolder.close(false);
                    localFolder.close(false);
                }
                catch (Exception e) {
                    for (MessagingListener l : getListeners()) {
                        l.loadMessageForViewFailed(account, folder, uid, e.getMessage());
                    }
                    addErrorMessage(account, e);

                }
            }
        });
    }

    public void loadMessageForView(final Account account, final String folder, final String uid,
            MessagingListener listener) {
        for (MessagingListener l : getListeners()) {
            l.loadMessageForViewStarted(account, folder, uid);
        }
        try {
            Store localStore = Store.getInstance(account.getLocalStoreUri(), mApplication);
            LocalFolder localFolder = (LocalFolder) localStore.getFolder(folder);
            localFolder.open(OpenMode.READ_WRITE);

            Message message = localFolder.getMessage(uid);
            
            if (!message.isSet(Flag.SEEN)) {
              markMessageRead(account, folder, uid, true);
            }
            
            for (MessagingListener l : getListeners()) {
                l.loadMessageForViewHeadersAvailable(account, folder, uid, message);
            }
            
            if (!message.isSet(Flag.X_DOWNLOADED_FULL)) {
                loadMessageForViewRemote(account, folder, uid, listener);
                localFolder.close(false);
                return;
            }

            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            fp.add(FetchProfile.Item.BODY);
            localFolder.fetch(new Message[] {
                message
            }, fp, null);

            for (MessagingListener l : getListeners()) {
                l.loadMessageForViewBodyAvailable(account, folder, uid, message);
            }
            if (listener != null)
            {
            	listener.loadMessageForViewBodyAvailable(account, folder, uid, message);
            }

            for (MessagingListener l : getListeners()) {
                l.loadMessageForViewFinished(account, folder, uid, message);
            }
            if (listener != null)
            {
            	listener.loadMessageForViewFinished(account, folder, uid, message);
            }
            localFolder.close(false);
        }
        catch (Exception e) {
            for (MessagingListener l : getListeners()) {
                l.loadMessageForViewFailed(account, folder, uid, e.getMessage());
            }
            addErrorMessage(account, e);

        }
    }

    /**
     * Attempts to load the attachment specified by part from the given account and message.
     * @param account
     * @param message
     * @param part
     * @param listener
     */
    public void loadAttachment(
            final Account account,
            final Message message,
            final Part part,
            final Object tag,
            MessagingListener listener) {
        /*
         * Check if the attachment has already been downloaded. If it has there's no reason to
         * download it, so we just tell the listener that it's ready to go.
         */
        try {
            if (part.getBody() != null) {
                for (MessagingListener l : getListeners()) {
                    l.loadAttachmentStarted(account, message, part, tag, false);
                }

                for (MessagingListener l : getListeners()) {
                    l.loadAttachmentFinished(account, message, part, tag);
                }
                return;
            }
        }
        catch (MessagingException me) {
            /*
             * If the header isn't there the attachment isn't downloaded yet, so just continue
             * on.
             */
        }

        for (MessagingListener l : getListeners()) {
            l.loadAttachmentStarted(account, message, part, tag, true);
        }

        put("loadAttachment", listener, new Runnable() {
            public void run() {
                try {
                    LocalStore localStore =
                        (LocalStore) Store.getInstance(account.getLocalStoreUri(), mApplication);
                    /*
                     * We clear out any attachments already cached in the entire store and then
                     * we update the passed in message to reflect that there are no cached
                     * attachments. This is in support of limiting the account to having one
                     * attachment downloaded at a time.
                     */
                    localStore.pruneCachedAttachments();
                    ArrayList<Part> viewables = new ArrayList<Part>();
                    ArrayList<Part> attachments = new ArrayList<Part>();
                    MimeUtility.collectParts(message, viewables, attachments);
                    for (Part attachment : attachments) {
                        attachment.setBody(null);
                    }
                    Store remoteStore = Store.getInstance(account.getStoreUri(), mApplication);
                    LocalFolder localFolder =
                        (LocalFolder) localStore.getFolder(message.getFolder().getName());
                    Folder remoteFolder = remoteStore.getFolder(message.getFolder().getName());
                    remoteFolder.open(OpenMode.READ_WRITE);

                    FetchProfile fp = new FetchProfile();
                    fp.add(part);
                    remoteFolder.fetch(new Message[] { message }, fp, null);
                    localFolder.updateMessage((LocalMessage)message);
                    localFolder.close(false);
                    for (MessagingListener l : getListeners()) {
                        l.loadAttachmentFinished(account, message, part, tag);
                    }
                }
                catch (MessagingException me) {
                    if (Config.LOGV) {
                        Log.v(Email.LOG_TAG, "", me);
                    }
                    for (MessagingListener l : getListeners()) {
                        l.loadAttachmentFailed(account, message, part, tag, me.getMessage());
                    }
                    addErrorMessage(account, me);

                }
            }
        });
    }

    /**
     * Stores the given message in the Outbox and starts a sendPendingMessages command to
     * attempt to send the message.
     * @param account
     * @param message
     * @param listener
     */
    public void sendMessage(final Account account,
            final Message message,
            MessagingListener listener) {
        try {
            Store localStore = Store.getInstance(account.getLocalStoreUri(), mApplication);
            LocalFolder localFolder =
                (LocalFolder) localStore.getFolder(account.getOutboxFolderName());
            localFolder.open(OpenMode.READ_WRITE);
            localFolder.appendMessages(new Message[] {
                message
            });
            Message localMessage = localFolder.getMessage(message.getUid());
            localMessage.setFlag(Flag.X_DOWNLOADED_FULL, true);
            localFolder.close(false);
            sendPendingMessages(account, null);
        }
        catch (Exception e) {
            for (MessagingListener l : getListeners()) {
                // TODO general failed
            }
            addErrorMessage(account, e);

        }
    }

    /**
     * Attempt to send any messages that are sitting in the Outbox.
     * @param account
     * @param listener
     */
    public void sendPendingMessages(final Account account,
            MessagingListener listener) {
        put("sendPendingMessages", listener, new Runnable() {
            public void run() {
                sendPendingMessagesSynchronous(account);
            }
        });
    }

    /**
     * Attempt to send any messages that are sitting in the Outbox.
     * @param account
     * @param listener
     */
    public void sendPendingMessagesSynchronous(final Account account) {
        try {
            Store localStore = Store.getInstance(
                    account.getLocalStoreUri(),
                    mApplication);
            Folder localFolder = localStore.getFolder(
                    account.getOutboxFolderName());
            if (!localFolder.exists()) {
                return;
            }
            for (MessagingListener l : getListeners()) {
              l.sendPendingMessagesStarted(account);
            }
            localFolder.open(OpenMode.READ_WRITE);

            Message[] localMessages = localFolder.getMessages(null);

            /*
             * The profile we will use to pull all of the content
             * for a given local message into memory for sending.
             */
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            fp.add(FetchProfile.Item.BODY);

            LocalFolder localSentFolder =
                (LocalFolder) localStore.getFolder(
                        account.getSentFolderName());

            Transport transport = Transport.getInstance(account.getTransportUri());
            for (Message message : localMessages) {
              if (message.isSet(Flag.DELETED)) {
                message.setFlag(Flag.X_DESTROYED, true);
                continue;
              }
                try {
                    localFolder.fetch(new Message[] { message }, fp, null);
                    try {
                        message.setFlag(Flag.X_SEND_IN_PROGRESS, true);
                        transport.sendMessage(message);
                        message.setFlag(Flag.X_SEND_IN_PROGRESS, false);
                        message.setFlag(Flag.SEEN, true);
                        localFolder.copyMessages(
                                new Message[] { message },
                                localSentFolder);
                        
                        PendingCommand command = new PendingCommand();
                        command.command = PENDING_COMMAND_APPEND;
                        command.arguments =
                            new String[] {
                                localSentFolder.getName(),
                                message.getUid() };
                        queuePendingCommand(account, command);
                        processPendingCommands(account);
                        message.setFlag(Flag.X_DESTROYED, true);
                    }
                    catch (Exception e) {
                        message.setFlag(Flag.X_SEND_FAILED, true);
                        Log.e(Email.LOG_TAG, "Failed to send message", e);
                        for (MessagingListener l : getListeners()) {
                          l.synchronizeMailboxFailed(
                                  account,
                                  localFolder.getName(),
                                  getRootCauseMessage(e));
                        }
                        addErrorMessage(account, e);

                    }
                }
                catch (Exception e) {
                	Log.e(Email.LOG_TAG, "Failed to fetch message for sending", e);
                	for (MessagingListener l : getListeners()) {
                    l.synchronizeMailboxFailed(
                            account,
                            localFolder.getName(),
                            getRootCauseMessage(e));
                  }
                  addErrorMessage(account, e);

                    /*
                     * We ignore this exception because a future refresh will retry this
                     * message.
                     */
                }
            }
            localFolder.expunge();
            if (localFolder.getMessageCount() == 0) {
                localFolder.delete(false);
            }
            for (MessagingListener l : getListeners()) {
                l.sendPendingMessagesCompleted(account);
            }
        }
        catch (Exception e) {
            for (MessagingListener l : getListeners()) {
                l.sendPendingMessagesFailed(account);
            }
            addErrorMessage(account, e);

        }
    }
    
    public void getAccountUnreadCount(final Context context, final Account account, 
        final MessagingListener l)
    {
      Runnable unreadRunnable = new Runnable() {
        public void run() {
      
          int unreadMessageCount = 0;
          try {
            unreadMessageCount = account.getUnreadMessageCount(context, mApplication);
          }
          catch (MessagingException me) {
              Log.e(Email.LOG_TAG, "Count not get unread count for account " + account.getDescription(),
                  me);
          }
          l.accountStatusChanged(account, unreadMessageCount);
        }
      };
      
      
      putBackground("getAccountUnread:" + account.getDescription(), l, unreadRunnable);
    }
    
    public boolean moveMessage(final Account account, final String srcFolder, final Message message, final String destFolder,
        final MessagingListener listener)
    {
      if (!message.getUid().startsWith("Local")
          && !message.getUid().contains("-")) { 
        put("moveMessage", null, new Runnable() {
          public void run() {
            moveOrCopyMessageSynchronous(account, srcFolder, message, destFolder, false, listener);
          }
        });
        return true;
      }
      else
      {
        return false;
      }
    }
    
    public boolean isMoveCapable(Message message) {
      if (!message.getUid().startsWith("Local")
          && !message.getUid().contains("-")) { 
        return true;
      }
      else {
        return false;
      }
    }
    public boolean isCopyCapable(Message message) {
      return isMoveCapable(message);
    }
    
    public boolean isMoveCapable(final Account account)
    {
      try {
        Store localStore = Store.getInstance(account.getLocalStoreUri(), mApplication);
        Store remoteStore = Store.getInstance(account.getStoreUri(), mApplication);
        return localStore.isMoveCapable() && remoteStore.isMoveCapable();
      }
      catch (MessagingException me)
      {

        Log.e(Email.LOG_TAG, "Exception while ascertaining move capability", me);
        return false;
       }
    }
    public boolean isCopyCapable(final Account account)
    {
      try {
        Store localStore = Store.getInstance(account.getLocalStoreUri(), mApplication);
        Store remoteStore = Store.getInstance(account.getStoreUri(), mApplication);
        return localStore.isCopyCapable() && remoteStore.isCopyCapable();
      }
      catch (MessagingException me)
      {
        Log.e(Email.LOG_TAG, "Exception while ascertaining copy capability", me);
        return false;
       }
    }
    
    public boolean copyMessage(final Account account, final String srcFolder, final Message message, final String destFolder,
        final MessagingListener listener)
    {
      if (!message.getUid().startsWith("Local")
          && !message.getUid().contains("-")) { 
        put("copyMessage", null, new Runnable() {
          public void run() {
            moveOrCopyMessageSynchronous(account, srcFolder, message, destFolder, true, listener);
          }
        });
        return true;
      }
      else
      {
        return false;
      }
    }
    
    private void moveOrCopyMessageSynchronous(final Account account, final String srcFolder, final Message message, 
        final String destFolder, final boolean isCopy, MessagingListener listener)
    {
      try {
        Store localStore = Store.getInstance(account.getLocalStoreUri(), mApplication);
        Store remoteStore = Store.getInstance(account.getStoreUri(), mApplication);
        if (isCopy == false && (remoteStore.isMoveCapable() == false || localStore.isMoveCapable() == false)) {
          return;
        }
        if (isCopy == true && (remoteStore.isCopyCapable() == false || localStore.isCopyCapable() == false)) {
          return;
        }
        
        Folder localSrcFolder = localStore.getFolder(srcFolder);
        Folder localDestFolder = localStore.getFolder(destFolder);
        Message lMessage = localSrcFolder.getMessage(message.getUid());
        String origUid = message.getUid();
        if (lMessage != null)
        {
          if (Config.LOGD)
          {
            Log.d(Email.LOG_TAG, "moveOrCopyMessageSynchronous: source folder = " + srcFolder
                + ", uid = " + origUid + ", destination folder = " + destFolder + ", isCopy = " + isCopy);
          }
          if (isCopy) {
            localSrcFolder.copyMessages(new Message[] { message }, localDestFolder);
          }
          else {
            localSrcFolder.moveMessages(new Message[] { message }, localDestFolder);
            for (MessagingListener l : getListeners()) {
              l.messageUidChanged(account, srcFolder, origUid, message.getUid());
            }
            unsuppressMessage(account, srcFolder, origUid);
          }
        }
        PendingCommand command = new PendingCommand();
        command.command = PENDING_COMMAND_MOVE_OR_COPY;
        command.arguments = new String[] { srcFolder, origUid, destFolder, Boolean.toString(isCopy) };
        queuePendingCommand(account, command);
        processPendingCommands(account);
      }
      catch (MessagingException me) {
        addErrorMessage(account, me);

          throw new RuntimeException("Error moving message", me);
      }
    }
    
    public void deleteMessage(final Account account, final String folder, final Message message,
            final MessagingListener listener) {
      suppressMessage(account, folder, message);
      
      put("deleteMessage", null, new Runnable() {
        public void run() {
          deleteMessageSynchronous(account, folder, message, listener);
        }
      });
    }
  
    private void deleteMessageSynchronous(final Account account, final String folder, final Message message,
        MessagingListener listener) {
      
        try {
            Store localStore = Store.getInstance(account.getLocalStoreUri(), mApplication);
            Folder localFolder = localStore.getFolder(folder);
            Message lMessage = localFolder.getMessage(message.getUid());
            if (lMessage != null)
            {
              if (folder.equals(account.getTrashFolderName()))
              {
                if (Config.LOGD)
                {
                  Log.d(Email.LOG_TAG, "Deleting message in trash folder, not copying");
                }
                lMessage.setFlag(Flag.DELETED, true);
              }
              else
              {
                Folder localTrashFolder = localStore.getFolder(account.getTrashFolderName());
                if (localTrashFolder.exists() == false)
                {
                  localTrashFolder.create(Folder.FolderType.HOLDS_MESSAGES);
                }
                if (localTrashFolder.exists() == true)
                {
                  if (Config.LOGD)
                  {
                    Log.d(Email.LOG_TAG, "Deleting message in normal folder, copying");
                  }
                  FetchProfile fp = new FetchProfile();
                  fp.add(FetchProfile.Item.ENVELOPE);
                  fp.add(FetchProfile.Item.BODY);
                  // TODO: Turn the fetch/copy/delete into an atomic move
                  localFolder.fetch(new Message[] { lMessage }, fp, null);
                  localFolder.copyMessages(new Message[] { lMessage }, localTrashFolder);
                  if (folder.equals(account.getOutboxFolderName())) {
                    lMessage.setFlag(Flag.X_DESTROYED, true);
                  }
                  else {
                    lMessage.setFlag(Flag.DELETED, true);
                  }
                }
              }
            }
            localFolder.close(false);
            unsuppressMessage(account, folder, message);
            if (listener != null) {
              listener.messageDeleted(account, folder, message);
            }
            for (MessagingListener l : getListeners()) {
              l.folderStatusChanged(account, account.getTrashFolderName());
          }
            
            if (Config.LOGD)
          	{
            	Log.d(Email.LOG_TAG, "Delete policy for account " + account.getDescription() + " is " + account.getDeletePolicy());
          	}
            if (folder.equals(account.getOutboxFolderName()))
            {
              // If the message was in the Outbox, then it has been copied to local Trash, and has
              // to be copied to remote trash
              PendingCommand command = new PendingCommand();
              command.command = PENDING_COMMAND_APPEND;
              command.arguments =
                  new String[] {
                      account.getTrashFolderName(),
                      message.getUid() };
              queuePendingCommand(account, command);
              processPendingCommands(account);
            }
            else if  (folder.equals(account.getTrashFolderName()) && account.getDeletePolicy() == Account.DELETE_POLICY_ON_DELETE)
            {
              PendingCommand command = new PendingCommand();
              command.command = PENDING_COMMAND_SET_FLAG;
              command.arguments = new String[] { folder, message.getUid(), Boolean.toString(true), Flag.DELETED.toString() };
              queuePendingCommand(account, command);
              processPendingCommands(account);
            }
            else if (account.getDeletePolicy() == Account.DELETE_POLICY_ON_DELETE) {
                PendingCommand command = new PendingCommand();
                command.command = PENDING_COMMAND_MOVE_OR_COPY;
                command.arguments = new String[] { folder, message.getUid(), account.getTrashFolderName(), "false" };
                queuePendingCommand(account, command);
                processPendingCommands(account);
            }
            else if (account.getDeletePolicy() == Account.DELETE_POLICY_MARK_AS_READ)
            {
              PendingCommand command = new PendingCommand();
              command.command = PENDING_COMMAND_SET_FLAG;
              command.arguments = new String[] { folder, message.getUid(), Boolean.toString(true), Flag.SEEN.toString() };
              queuePendingCommand(account, command);
              processPendingCommands(account);
            }
            else
            {
            	if (Config.LOGD)
            	{
            		Log.d(Email.LOG_TAG, "Delete policy " + account.getDeletePolicy() + " prevents delete from server");
            	}
            }
        }
        catch (MessagingException me) {
          addErrorMessage(account, me);

            throw new RuntimeException("Error deleting message from local store.", me);
        }
    }
    
    private void processPendingEmptyTrash(PendingCommand command, Account account) throws MessagingException {
      Store remoteStore = Store.getInstance(account.getStoreUri(), mApplication);
      
      Folder remoteFolder = remoteStore.getFolder(account.getTrashFolderName());
      if (remoteFolder.exists())
      {
        remoteFolder.open(OpenMode.READ_WRITE);
        remoteFolder.setFlags(new Flag [] { Flag.DELETED }, true);
        remoteFolder.close(true);
      }
    }

    public void emptyTrash(final Account account, MessagingListener listener) {
        put("emptyTrash", listener, new Runnable() {
            public void run() {
                try {
                    Store localStore = Store.getInstance(account.getLocalStoreUri(), mApplication);
                    Folder localFolder = localStore.getFolder(account.getTrashFolderName());
                    localFolder.open(OpenMode.READ_WRITE);
                    localFolder.setFlags(new Flag[] { Flag.DELETED }, true);
                    localFolder.close(true);
                    
                    for (MessagingListener l : getListeners()) {
                      l.emptyTrashCompleted(account);
                    }
                    List<String> args = new ArrayList<String>();
                    PendingCommand command = new PendingCommand();
                    command.command = PENDING_COMMAND_EMPTY_TRASH;
                    command.arguments = args.toArray(new String[0]);
                    queuePendingCommand(account, command);
                    processPendingCommands(account);
                }
                catch (Exception e) {
                    Log.e(Email.LOG_TAG, "emptyTrash failed", e);
                    
                    addErrorMessage(account, e);
                }
            }
        });
    }

  	public void sendAlternate(final Context context, Account account, Message message)
  	{
  			if (Config.LOGD)
  			{
  				Log.d(Email.LOG_TAG, "About to load message " + account.getDescription() + ":" + message.getFolder().getName()
  						+ ":" + message.getUid() + " for sendAlternate");
  			}
   			loadMessageForView(account, message.getFolder().getName(), 
  					message.getUid(), new MessagingListener()
  			{
  				@Override
  				public void loadMessageForViewBodyAvailable(Account account, String folder, String uid,
              Message message)
  				{
  					if (Config.LOGD)
  					{
  						Log.d(Email.LOG_TAG, "Got message " + account.getDescription() + ":" + folder
  								+ ":" + message.getUid() + " for sendAlternate");
  					}

  					try
  					{
  						Intent msg=new Intent(Intent.ACTION_SEND);  
  					  String quotedText = null;
  						Part part = MimeUtility.findFirstPartByMimeType(message,
  			       			"text/plain");
  					  if (part == null) {
  				        part = MimeUtility.findFirstPartByMimeType(message, "text/html");
  				    }
  						if (part != null) {
  						   quotedText = MimeUtility.getTextFromPart(part);
  						}
  						if (quotedText != null)
  						{
  							msg.putExtra(Intent.EXTRA_TEXT, quotedText);
  						}
  					  msg.putExtra(Intent.EXTRA_SUBJECT, "Fwd: " + message.getSubject());  
  					  msg.setType("text/plain");  
  					  context.startActivity(Intent.createChooser(msg, context.getString(R.string.send_alternate_chooser_title)));  
  					}
  					catch (MessagingException me)
  					{
  						Log.e(Email.LOG_TAG, "Unable to send email through alternate program", me);
  					}
  				}
  			});
  	   
  	}

    
    
    /**
     * Checks mail for one or multiple accounts. If account is null all accounts
     * are checked.
     *
     * @param context
     * @param account
     * @param listener
     */
    public void checkMail(final Context context, final Account account,
            final MessagingListener listener) {
    	
        for (MessagingListener l : getListeners()) {
            l.checkMailStarted(context, account);
        }
        put("checkMail", listener, new Runnable() {
            public void run() {

                final NotificationManager notifMgr = (NotificationManager)context
                  .getSystemService(Context.NOTIFICATION_SERVICE);
            	  try
            	  {
	              	Log.i(Email.LOG_TAG, "Starting mail check");
          				Preferences prefs = Preferences.getPreferences(context);

	                Account[] accounts;
	                if (account != null) {
	                    accounts = new Account[] {
	                        account
	                    };
	                } else {
	                    accounts = prefs.getAccounts();
	                }

	                for (final Account account : accounts) {
	                  	final long accountInterval = account.getAutomaticCheckIntervalMinutes() * 60 * 1000;
	                  	if (accountInterval <= 0)
	                  	{
		                  	if (Config.LOGV || true)
		                  	{
		                  		Log.v(Email.LOG_TAG, "Skipping synchronizing account " + account.getDescription());
		                  	}

	                  		continue;
	                  	}

	                  	if (Config.LOGV || true)
	                  	{
	                  		Log.v(Email.LOG_TAG, "Synchronizing account " + account.getDescription());
	                  	}
                    	putBackground("sendPending " + account.getDescription(), null, new Runnable() {
                        public void run() {
                          if (account.isShowOngoing()) {
                            Notification notif = new Notification(R.drawable.ic_menu_refresh, 
                                context.getString(R.string.notification_bg_send_ticker, account.getDescription()), System.currentTimeMillis());                         
                            Intent intent = FolderMessageList.actionHandleAccountIntent(context, account, Email.INBOX);
                            PendingIntent pi = PendingIntent.getActivity(context, 0, intent, 0);
                              notif.setLatestEventInfo(context, context.getString(R.string.notification_bg_send_title), 
                                  account.getDescription() , pi);
                              notif.flags = Notification.FLAG_ONGOING_EVENT;
                              
                              if (Email.NOTIFICATION_LED_WHILE_SYNCING) {
                                notif.flags |= Notification.FLAG_SHOW_LIGHTS;
                                notif.ledARGB = Email.NOTIFICATION_LED_DIM_COLOR;
                                notif.ledOnMS = Email.NOTIFICATION_LED_FAST_ON_TIME;
                                notif.ledOffMS = Email.NOTIFICATION_LED_FAST_OFF_TIME;
                              }
                              
                              notifMgr.notify(Email.FETCHING_EMAIL_NOTIFICATION_ID, notif);
                          }
                          try
                          {
                            sendPendingMessagesSynchronous(account);
                          }
                        	finally {
                        	  if (account.isShowOngoing()) {
                        	    notifMgr.cancel(Email.FETCHING_EMAIL_NOTIFICATION_ID);
                        	  }
                          }
                        }
                    	}
                    	);
	                    try
	                    {
	                    	Account.FolderMode aDisplayMode = account.getFolderDisplayMode();
	                    	Account.FolderMode aSyncMode = account.getFolderSyncMode();

		                    Store localStore = Store.getInstance(account.getLocalStoreUri(), mApplication);
		                    for (final Folder folder : localStore.getPersonalNamespaces())
		                    {
		                    	
		                    	folder.open(Folder.OpenMode.READ_WRITE);
		                    	folder.refresh(prefs);
		                    	
		                    	Folder.FolderClass fDisplayMode = folder.getDisplayClass();
		                    	Folder.FolderClass fSyncMode = folder.getSyncClass();

		                    	if ((aDisplayMode == Account.FolderMode.FIRST_CLASS && 
		                    					fDisplayMode != Folder.FolderClass.FIRST_CLASS) 
		                    			|| (aDisplayMode == Account.FolderMode.FIRST_AND_SECOND_CLASS &&
		                      					fDisplayMode != Folder.FolderClass.FIRST_CLASS &&
		                      					fDisplayMode != Folder.FolderClass.SECOND_CLASS) 
		                      		|| (aDisplayMode == Account.FolderMode.NOT_SECOND_CLASS &&
		                      					fDisplayMode == Folder.FolderClass.SECOND_CLASS))
		                      {
		                    		// Never sync a folder that isn't displayed
			                    	if (Config.LOGV) {
			                    		Log.v(Email.LOG_TAG, "Not syncing folder " + folder.getName() + 
			                    				" which is in display mode " + fDisplayMode + " while account is in display mode " + aDisplayMode);
			                    	}

		                       	continue;
		                      }

		                    	if ((aSyncMode == Account.FolderMode.FIRST_CLASS && 
		                    			fSyncMode != Folder.FolderClass.FIRST_CLASS)
		                    			|| (aSyncMode == Account.FolderMode.FIRST_AND_SECOND_CLASS &&
		                      					fSyncMode != Folder.FolderClass.FIRST_CLASS &&
		                      					fSyncMode != Folder.FolderClass.SECOND_CLASS) 
		                    			|| (aSyncMode == Account.FolderMode.NOT_SECOND_CLASS &&
		                    					fSyncMode == Folder.FolderClass.SECOND_CLASS))
		                      {
		                    		// Do not sync folders in the wrong class
			                    	if (Config.LOGV) {
			                    		Log.v(Email.LOG_TAG, "Not syncing folder " + folder.getName() + 
			                    				" which is in sync mode " + fSyncMode + " while account is in sync mode " + aSyncMode);
			                    	}

		                       	continue;
		                      }
	                    	
		                    	
	
		                    	if (Config.LOGV) {
		                    		Log.v(Email.LOG_TAG, "Folder " + folder.getName() + " was last synced @ " +
		                    				new Date(folder.getLastChecked()));
		                    	}
		                    	
		                    	if (folder.getLastChecked() > 
		                    		(System.currentTimeMillis() - accountInterval))
		                    	{
			                    		if (Config.LOGV) {
			                    			Log.v(Email.LOG_TAG, "Not syncing folder " + folder.getName()
			                    					+ ", previously synced @ " + new Date(folder.getLastChecked())
			                    							+ " which would be too recent for the account period");
			                    		}					

		                    			continue;
		                    	}
		                    	putBackground("sync" + folder.getName(), null, new Runnable() {
		                        public void run() {
				                    	try {
				                    		// In case multiple Commands get enqueued, don't run more than
				                    		// once
				                    		final LocalStore localStore =
				                          (LocalStore) Store.getInstance(account.getLocalStoreUri(), mApplication);
				                    		LocalFolder tLocalFolder = (LocalFolder) localStore.getFolder(folder.getName());
				                    		tLocalFolder.open(Folder.OpenMode.READ_WRITE);
				                    						                    		
				                    		if (tLocalFolder.getLastChecked() > 
				                    			    (System.currentTimeMillis() - accountInterval))
				                    		{
				                    			if (Config.LOGV) {
					                    			Log.v(Email.LOG_TAG, "Not running Command for folder " + folder.getName()
					                    					+ ", previously synced @ " + new Date(folder.getLastChecked())
					                    							+ " which would be too recent for the account period");
				                    			}
				                    			return;
				                    		}
				                    		if (account.isShowOngoing()) {
  				                    		Notification notif = new Notification(R.drawable.ic_menu_refresh, 
  				                    		    context.getString(R.string.notification_bg_sync_ticker, account.getDescription(), folder.getName()), 
  				                    		    System.currentTimeMillis());                         
  			                          Intent intent = FolderMessageList.actionHandleAccountIntent(context, account, Email.INBOX);
  			                          PendingIntent pi = PendingIntent.getActivity(context, 0, intent, 0);
  			                            notif.setLatestEventInfo(context, context.getString(R.string.notification_bg_sync_title), account.getDescription()
  			                                + context.getString(R.string.notification_bg_title_separator) + folder.getName(), pi);
  			                            notif.flags = Notification.FLAG_ONGOING_EVENT;
  			                            if (Email.NOTIFICATION_LED_WHILE_SYNCING) {
    			                            notif.flags |= Notification.FLAG_SHOW_LIGHTS;
    			                            notif.ledARGB = Email.NOTIFICATION_LED_DIM_COLOR;
    			                            notif.ledOnMS = Email.NOTIFICATION_LED_FAST_ON_TIME;
    			                            notif.ledOffMS = Email.NOTIFICATION_LED_FAST_OFF_TIME;
  			                            }
  
  			                            notifMgr.notify(Email.FETCHING_EMAIL_NOTIFICATION_ID, notif);
				                    		}
			                          try
			                          {
			                            synchronizeMailboxSynchronous(account, folder.getName());
			                          }
				                    	  
		                            finally {
		                              if (account.isShowOngoing()) {
		                                notifMgr.cancel(Email.FETCHING_EMAIL_NOTIFICATION_ID);
		                              }
		                            }
				                    	}
				                    	catch (Exception e)
				                    	{
				                    		
				                    		Log.e(Email.LOG_TAG, "Exception while processing folder " + 
				                    				account.getDescription() + ":" + folder.getName(), e);
				                    		addErrorMessage(account, e);
				                    	}
		                        }
		                    	}
		                    	);
		                    } 
	                    }
	                    catch (MessagingException e) {
	                      Log.e(Email.LOG_TAG, "Unable to synchronize account " + account.getName(), e);
	                      addErrorMessage(account, e);
	                    }
	                }
            	  }
            	  catch (Exception e)
            	  {
            	  	 Log.e(Email.LOG_TAG, "Unable to synchronize mail", e);
            	  	 addErrorMessage(account, e);
            	  }
              	putBackground("finalize sync", null, new Runnable() {
                  public void run() {

		            	  Log.i(Email.LOG_TAG, "Finished mail sync");
		             	 
		                for (MessagingListener l : getListeners()) {
		                    l.checkMailFinished(context, account);
		                }
		                
                  }
              	}
              	);
            }
        });
    }
    
    public void compact(final Account account, final MessagingListener ml)
    {
      putBackground("compact:" + account.getDescription(), ml, new Runnable() 
      {
        public void run()
        {
          try
          {
            LocalStore localStore = (LocalStore)Store.getInstance(account.getLocalStoreUri(), mApplication);
            long oldSize = localStore.getSize();
            localStore.compact();
            long newSize = localStore.getSize();
            if (ml != null)
            {
              ml.accountSizeChanged(account, oldSize, newSize);
            }
            for (MessagingListener l : getListeners()) {
              l.accountSizeChanged(account, oldSize, newSize);
              l.accountReset(account);
            }
          }
          catch (Exception e)
          {
            Log.e(Email.LOG_TAG, "Failed to compact account " + account.getDescription(), e);
          }
        }
      });
    }

    public void clear(final Account account, final MessagingListener ml)
    {
      putBackground("clear:" + account.getDescription(), ml, new Runnable() 
      {
        public void run()
        {
          try
          {
            LocalStore localStore = (LocalStore)Store.getInstance(account.getLocalStoreUri(), mApplication);
            long oldSize = localStore.getSize();
            localStore.clear();
            localStore.resetVisibleLimits(account.getDisplayCount());
            long newSize = localStore.getSize();
            if (ml != null)
            {
              ml.accountSizeChanged(account, oldSize, newSize);
            }
            for (MessagingListener l : getListeners()) {
              l.accountSizeChanged(account, oldSize, newSize);
              l.accountReset(account);
            }
          }
          catch (Exception e)
          {
            Log.e(Email.LOG_TAG, "Failed to compact account " + account.getDescription(), e);
          }
        }
      });
    }
    public void saveDraft(final Account account, final Message message) {
        try {
            Store localStore = Store.getInstance(account.getLocalStoreUri(), mApplication);
            LocalFolder localFolder =
                (LocalFolder) localStore.getFolder(account.getDraftsFolderName());
            localFolder.open(OpenMode.READ_WRITE);
            localFolder.appendMessages(new Message[] {
                message
            });
            Message localMessage = localFolder.getMessage(message.getUid());
            localMessage.setFlag(Flag.X_DOWNLOADED_FULL, true);

            PendingCommand command = new PendingCommand();
            command.command = PENDING_COMMAND_APPEND;
            command.arguments = new String[] {
                    localFolder.getName(),
                    localMessage.getUid() };
            queuePendingCommand(account, command);
            processPendingCommands(account);
        }
        catch (MessagingException e) {
            Log.e(Email.LOG_TAG, "Unable to save message as draft.", e);
            addErrorMessage(account, e);
        }
    }

    class Command {
        public Runnable runnable;

        public MessagingListener listener;

        public String description;
    }

    public MessagingListener getCheckMailListener()
    {
      return checkMailListener;
    }

    public void setCheckMailListener(MessagingListener checkMailListener)
    {
      if (this.checkMailListener != null)
      {
        removeListener(this.checkMailListener);
      }
      this.checkMailListener = checkMailListener;
      if (this.checkMailListener != null)
      {
        addListener(this.checkMailListener);
      }
    }

    public SORT_TYPE getSortType()
    {
      return sortType;
    }

    public void setSortType(SORT_TYPE sortType)
    {
      this.sortType = sortType;
    }

    public boolean isSortAscending(SORT_TYPE sortType)
    {
      Boolean sortAsc = sortAscending.get(sortType);
      if (sortAsc == null)
      {
        return sortType.isDefaultAscending();
      }
      else return sortAsc;
    }

    public void setSortAscending(SORT_TYPE sortType, boolean nsortAscending)
    {
      sortAscending.put(sortType, nsortAscending);
    }
}
