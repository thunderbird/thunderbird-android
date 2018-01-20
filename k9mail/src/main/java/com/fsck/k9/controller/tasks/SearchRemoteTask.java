package com.fsck.k9.controller.tasks;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import android.content.Context;
import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.controller.UidReverseComparator;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalStore;
import timber.log.Timber;


public class SearchRemoteTask implements Runnable {

    private final Context context;
    private final SearchResultsLoader searchResultsLoader;
    private final String acctUuid;
    private final String folderName;
    private final String query;
    private final Set<Flag> requiredFlags;
    private final Set<Flag> forbiddenFlags;
    private final MessagingListener taskListener;

    public SearchRemoteTask(
            Context context, SearchResultsLoader searchResultsLoader,
            String acctUuid, String folderName, String query,
            Set<Flag> requiredFlags, Set<Flag> forbiddenFlags,
            MessagingListener taskListener) {
        this.context = context;
        this.searchResultsLoader = searchResultsLoader;
        this.acctUuid = acctUuid;
        this.folderName = folderName;
        this.query = query;
        this.requiredFlags = requiredFlags;
        this.forbiddenFlags = forbiddenFlags;
        this.taskListener = taskListener;
    }

    @Override
    public void run() {
        searchRemoteMessagesSynchronous();
    }

    private void searchRemoteMessagesSynchronous() {
        final Account acct = Preferences.getPreferences(context).getAccount(acctUuid);

        if (taskListener != null) {
            taskListener.remoteSearchStarted(folderName);
        }

        List<Message> extraResults = new ArrayList<>();
        try {
            Store remoteStore = acct.getRemoteStore();
            LocalStore localStore = acct.getLocalStore();

            if (remoteStore == null || localStore == null) {
                throw new MessagingException("Could not get store");
            }

            Folder remoteFolder = remoteStore.getFolder(folderName);
            LocalFolder localFolder = localStore.getFolder(folderName);
            if (remoteFolder == null || localFolder == null) {
                throw new MessagingException("Folder not found");
            }

            List<Message> messages = remoteFolder.search(query, requiredFlags, forbiddenFlags);

            Timber.i("Remote search got %d results", messages.size());

            // There's no need to fetch messages already completely downloaded
            List<Message> remoteMessages = localFolder.extractNewMessages(messages);
            messages.clear();

            if (taskListener != null) {
                taskListener.remoteSearchServerQueryComplete(folderName, remoteMessages.size(),
                        acct.getRemoteSearchNumResults());
            }

            Collections.sort(remoteMessages, new UidReverseComparator());

            int resultLimit = acct.getRemoteSearchNumResults();
            if (resultLimit > 0 && remoteMessages.size() > resultLimit) {
                extraResults = remoteMessages.subList(resultLimit, remoteMessages.size());
                remoteMessages = remoteMessages.subList(0, resultLimit);
            }

            searchResultsLoader.load(remoteMessages, localFolder, remoteFolder, taskListener);


        } catch (Exception e) {
            if (Thread.currentThread().isInterrupted()) {
                Timber.i(e, "Caught exception on aborted remote search; safe to ignore.");
            } else {
                Timber.e(e, "Could not complete remote search");
                if (taskListener != null) {
                    taskListener.remoteSearchFailed(null, e.getMessage());
                }
                Timber.e(e);
            }
        } finally {
            if (taskListener != null) {
                taskListener.remoteSearchFinished(folderName, 0, acct.getRemoteSearchNumResults(), extraResults);
            }
        }

    }
}
