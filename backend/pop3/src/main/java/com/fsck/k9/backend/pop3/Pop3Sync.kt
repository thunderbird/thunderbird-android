package com.fsck.k9.backend.pop3

import com.fsck.k9.backend.api.BackendFolder
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.SyncConfig
import com.fsck.k9.backend.api.SyncListener
import com.fsck.k9.helper.ExceptionHelper
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.FetchProfile
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.MessageDownloadState
import com.fsck.k9.mail.MessageRetrievalListener
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.store.pop3.Pop3Folder
import com.fsck.k9.mail.store.pop3.Pop3Message
import com.fsck.k9.mail.store.pop3.Pop3Store
import java.lang.Exception
import java.util.ArrayList
import java.util.Date
import java.util.HashMap
import java.util.concurrent.atomic.AtomicInteger
import net.thunderbird.core.logging.legacy.Log

@Suppress("TooManyFunctions")
internal class Pop3Sync(
    private val accountName: String,
    private val backendStorage: BackendStorage,
    private val remoteStore: Pop3Store,
) {

    fun sync(folder: String, syncConfig: SyncConfig, listener: SyncListener) {
        synchronizeMailboxSynchronous(folder, syncConfig, listener)
    }

    @Suppress(
        "TooGenericExceptionCaught",
        "TooGenericExceptionThrown",
        "LongMethod",
        "CyclomaticComplexMethod",
        "NestedBlockDepth",
    )
    fun synchronizeMailboxSynchronous(folder: String, syncConfig: SyncConfig, listener: SyncListener) {
        var remoteFolder: Pop3Folder? = null

        Log.i("Synchronizing folder %s:%s", accountName, folder)

        var backendFolder: BackendFolder? = null
        try {
            Log.d("SYNC: About to process pending commands for account %s", accountName)

            Log.v("SYNC: About to get local folder %s", folder)
            backendFolder = backendStorage.getFolder(folder)

            listener.syncStarted(folder)

            /*
             * Get the message list from the local store and create an index of
             * the uids within the list.
             */
            var localUidMap: Map<String, Long?> = backendFolder.getAllMessagesAndEffectiveDates()

            Log.v("SYNC: About to get remote folder %s", folder)
            remoteFolder = remoteStore.getFolder(folder)

            /*
             * Synchronization process:
             *
            Open the folder
            Upload any local messages that are marked as PENDING_UPLOAD (Drafts, Sent, Trash)
            Get the message count
            Get the list of the newest K9.DEFAULT_VISIBLE_LIMIT messages
            getMessages(messageCount - K9.DEFAULT_VISIBLE_LIMIT, messageCount)
            See if we have each message locally, if not fetch it's flags and envelope
            Get and update the unread count for the folder
            Update the remote flags of any messages we have locally with an internal date newer than the remote message.
            Get the current flags for any messages we have locally but did not just download
            Update local flags
            For any message we have locally but not remotely, delete the local message to keep cache clean.
            Download larger parts of any new messages.
            (Optional) Download small attachments in the background.
             */

            /*
             * Open the remote folder. This pre-loads certain metadata like message count.
             */
            Log.v("SYNC: About to open remote folder %s", folder)

            remoteFolder.open()

            listener.syncAuthenticationSuccess()

            /*
             * Get the remote message count.
             */
            val remoteMessageCount = remoteFolder.messageCount

            var visibleLimit = backendFolder.visibleLimit

            if (visibleLimit < 0) {
                visibleLimit = syncConfig.defaultVisibleLimit
            }

            val remoteMessages: MutableList<Pop3Message?> = ArrayList<Pop3Message?>()
            val remoteUidMap: MutableMap<String?, Pop3Message?> = HashMap<String?, Pop3Message?>()

            Log.v("SYNC: Remote message count for folder %s is %d", folder, remoteMessageCount)

            val earliestDate = syncConfig.earliestPollDate
            val earliestTimestamp = if (earliestDate != null) earliestDate.time else 0L

            /* Message numbers start at 1. */
            var remoteStart = 1
            if (remoteMessageCount > 0) {
                // Adjust the starting message number based on the visible limit
                if (visibleLimit > 0) {
                    remoteStart += (remoteMessageCount - visibleLimit).coerceAtLeast(0)
                }

                Log.v(
                    "SYNC: About to get messages %d through %d for folder %s",
                    remoteStart,
                    remoteMessageCount,
                    folder,
                )

                val headerProgress = AtomicInteger(0)
                listener.syncHeadersStarted(folder)

                val remoteMessageArray = remoteFolder.getMessages(remoteStart, remoteMessageCount, null)

                val messageCount = remoteMessageArray.size

                for (thisMess in remoteMessageArray) {
                    headerProgress.incrementAndGet()
                    listener.syncHeadersProgress(folder, headerProgress.get(), messageCount)

                    val localMessageTimestamp = localUidMap[thisMess.uid]
                    if (localMessageTimestamp == null || localMessageTimestamp >= earliestTimestamp) {
                        remoteMessages.add(thisMess)
                        remoteUidMap.put(thisMess.uid, thisMess)
                    }
                }

                Log.v("SYNC: Got %d messages for folder %s", remoteUidMap.size, folder)

                listener.syncHeadersFinished(folder, headerProgress.get(), remoteUidMap.size)
            } else if (remoteMessageCount < 0) {
                throw Exception("Message count $remoteMessageCount for folder $folder")
            }

            /*
             * Remove any messages that are in the local store but no longer on the remote store or are too old
             */
            var moreMessages = backendFolder.getMoreMessages()
            if (syncConfig.syncRemoteDeletions) {
                val destroyMessageUids: MutableList<String> = ArrayList<String>()
                for (localMessageUid in localUidMap.keys) {
                    if (remoteUidMap[localMessageUid] == null) {
                        destroyMessageUids.add(localMessageUid)
                    }
                }

                if (!destroyMessageUids.isEmpty()) {
                    moreMessages = BackendFolder.MoreMessages.UNKNOWN

                    backendFolder.destroyMessages(destroyMessageUids)
                    for (uid in destroyMessageUids) {
                        listener.syncRemovedMessage(folder, uid)
                    }
                }
            }

            if (moreMessages == BackendFolder.MoreMessages.UNKNOWN) {
                updateMoreMessages(remoteFolder, backendFolder, remoteStart)
            }

            /*
             * Now we download the actual content of messages.
             */
            val newMessages = downloadMessages(
                syncConfig,
                remoteFolder,
                backendFolder,
                remoteMessages,
                listener,
            )

            listener.folderStatusChanged(folder)

            /* Notify listeners that we're finally done. */
            backendFolder.setLastChecked(System.currentTimeMillis())
            backendFolder.setStatus(null)

            Log.d(
                "Done synchronizing folder %s:%s @ %tc with %d new messages",
                accountName,
                folder,
                System.currentTimeMillis(),
                newMessages,
            )

            listener.syncFinished(folder)

            Log.i("Done synchronizing folder %s:%s", accountName, folder)
        } catch (e: AuthenticationFailedException) {
            listener.syncFailed(folder, "Authentication failure", e)
        } catch (e: Exception) {
            Log.e(e, "synchronizeMailbox")
            // If we don't set the last checked, it can try too often during
            // failure conditions
            val rootMessage = ExceptionHelper.getRootCauseMessage(e)
            if (backendFolder != null) {
                try {
                    backendFolder.setStatus(rootMessage)
                    backendFolder.setLastChecked(System.currentTimeMillis())
                } catch (e1: Exception) {
                    Log.e(e1, "Could not set last checked on folder %s:%s", accountName, folder)
                }
            }

            listener.syncFailed(folder, rootMessage, e)

            Log.e(
                "Failed synchronizing folder %s:%s @ %tc",
                accountName,
                folder,
                System.currentTimeMillis(),
            )
        } finally {
            remoteFolder?.close()
        }
    }

    private fun updateMoreMessages(
        remoteFolder: Pop3Folder,
        backendFolder: BackendFolder,
        remoteStart: Int,
    ) {
        if (remoteStart == 1) {
            backendFolder.setMoreMessages(BackendFolder.MoreMessages.FALSE)
        } else {
            val moreMessagesAvailable = remoteFolder.areMoreMessagesAvailable(remoteStart)

            val newMoreMessages =
                if ((moreMessagesAvailable)) BackendFolder.MoreMessages.TRUE else BackendFolder.MoreMessages.FALSE
            backendFolder.setMoreMessages(newMoreMessages)
        }
    }

    @Suppress("TooGenericExceptionCaught", "LongMethod")
    @Throws(MessagingException::class)
    private fun downloadMessages(
        syncConfig: SyncConfig,
        remoteFolder: Pop3Folder,
        backendFolder: BackendFolder,
        inputMessages: MutableList<Pop3Message?>,
        listener: SyncListener,
    ): Int {
        val earliestDate = syncConfig.earliestPollDate
        val downloadStarted = Date() // now

        if (earliestDate != null) {
            Log.d("Only syncing messages after %s", earliestDate)
        }
        val folder = remoteFolder.serverId

        val syncFlagMessages: MutableList<Pop3Message?> = ArrayList<Pop3Message?>()
        var unsyncedMessages: MutableList<Pop3Message> = ArrayList<Pop3Message>()
        val newMessages = AtomicInteger(0)

        val messages: MutableList<Pop3Message> = ArrayList<Pop3Message>(inputMessages)

        for (message in messages) {
            evaluateMessageForDownload(message, folder, backendFolder, unsyncedMessages, syncFlagMessages, listener)
        }

        val progress = AtomicInteger(0)
        val todo = unsyncedMessages.size + syncFlagMessages.size
        listener.syncProgress(folder, progress.get(), todo)

        Log.d("SYNC: Have %d unsynced messages", unsyncedMessages.size)

        messages.clear()
        val largeMessages: MutableList<Pop3Message> = ArrayList<Pop3Message>()
        val smallMessages: MutableList<Pop3Message> = ArrayList<Pop3Message>()
        if (!unsyncedMessages.isEmpty()) {
            val visibleLimit = backendFolder.visibleLimit
            val listSize = unsyncedMessages.size

            if ((visibleLimit > 0) && (listSize > visibleLimit)) {
                unsyncedMessages = unsyncedMessages.subList(0, visibleLimit)
            }

            val fp = FetchProfile()
            fp.add(FetchProfile.Item.ENVELOPE)

            Log.d("SYNC: About to fetch %d unsynced messages for folder %s", unsyncedMessages.size, folder)

            fetchUnsyncedMessages(
                syncConfig, remoteFolder, unsyncedMessages, smallMessages, largeMessages, progress,
                todo, fp, listener,
            )

            Log.d("SYNC: Synced unsynced messages for folder %s", folder)
        }

        Log.d(
            "SYNC: Have %d large messages and %d small messages out of %d unsynced messages",
            largeMessages.size,
            smallMessages.size,
            unsyncedMessages.size,
        )

        unsyncedMessages.clear()
        /*
         * Grab the content of the small messages first. This is going to
         * be very fast and at very worst will be a single up of a few bytes and a single
         * download of 625k.
         */
        var fp = FetchProfile()
        // TODO: Only fetch small and large messages if we have some
        fp.add(FetchProfile.Item.BODY)
        //        fp.add(FetchProfile.Item.FLAGS);
        //        fp.add(FetchProfile.Item.ENVELOPE);
        downloadSmallMessages(remoteFolder, backendFolder, smallMessages, progress, newMessages, todo, fp, listener)
        smallMessages.clear()
        /*
         * Now do the large messages that require more round trips.
         */
        fp = FetchProfile()
        fp.add(FetchProfile.Item.STRUCTURE)
        downloadLargeMessages(
            syncConfig,
            remoteFolder,
            backendFolder,
            largeMessages,
            progress,
            newMessages,
            todo,
            fp,
            listener,
        )
        largeMessages.clear()

        Log.d("SYNC: Synced remote messages for folder %s, %d new messages", folder, newMessages.get())

        // If the oldest message seen on this sync is newer than the oldest message seen on the previous sync, then
        // we want to move our high-water mark forward.
        val oldestMessageTime = backendFolder.getOldestMessageDate()
        if (oldestMessageTime != null) {
            if (oldestMessageTime.before(downloadStarted) &&
                oldestMessageTime.after(getLatestOldMessageSeenTime(backendFolder))
            ) {
                setLatestOldMessageSeenTime(backendFolder, oldestMessageTime)
            }
        }

        return newMessages.get()
    }

    private fun getLatestOldMessageSeenTime(backendFolder: BackendFolder): Date {
        val latestOldMessageSeenTime = backendFolder.getFolderExtraNumber(EXTRA_LATEST_OLD_MESSAGE_SEEN_TIME)
        val timestamp = if (latestOldMessageSeenTime != null) latestOldMessageSeenTime else 0L
        return Date(timestamp)
    }

    private fun setLatestOldMessageSeenTime(backendFolder: BackendFolder, oldestMessageTime: Date) {
        backendFolder.setFolderExtraNumber(EXTRA_LATEST_OLD_MESSAGE_SEEN_TIME, oldestMessageTime.time)
    }

    private fun evaluateMessageForDownload(
        message: Pop3Message,
        folder: String,
        backendFolder: BackendFolder,
        unsyncedMessages: MutableList<Pop3Message>,
        syncFlagMessages: MutableList<Pop3Message?>,
        listener: SyncListener,
    ) {
        val messageServerId = message.uid
        if (message.isSet(Flag.DELETED)) {
            Log.v("Message with uid %s is marked as deleted", messageServerId)

            syncFlagMessages.add(message)
            return
        }

        val messagePresentLocally = backendFolder.isMessagePresent(messageServerId)

        if (!messagePresentLocally) {
            if (!message.isSet(Flag.X_DOWNLOADED_FULL) && !message.isSet(Flag.X_DOWNLOADED_PARTIAL)) {
                Log.v("Message with uid %s has not yet been downloaded", messageServerId)

                unsyncedMessages.add(message)
            } else {
                Log.v("Message with uid %s is partially or fully downloaded", messageServerId)

                // Store the updated message locally
                val completeMessage = message.isSet(Flag.X_DOWNLOADED_FULL)
                if (completeMessage) {
                    backendFolder.saveMessage(message, MessageDownloadState.FULL)
                } else {
                    backendFolder.saveMessage(message, MessageDownloadState.PARTIAL)
                }

                val isOldMessage = isOldMessage(backendFolder, message)
                listener.syncNewMessage(folder, messageServerId, isOldMessage)
            }
            return
        }

        val messageFlags: Set<Flag> = backendFolder.getMessageFlags(messageServerId)
        if (!messageFlags.contains(Flag.DELETED)) {
            Log.v("Message with uid %s is present in the local store", messageServerId)

            if (!messageFlags.contains(Flag.X_DOWNLOADED_FULL) && !messageFlags.contains(Flag.X_DOWNLOADED_PARTIAL)) {
                Log.v("Message with uid %s is not downloaded, even partially; trying again", messageServerId)

                unsyncedMessages.add(message)
            } else {
                syncFlagMessages.add(message)
            }
        } else {
            Log.v("Local copy of message with uid %s is marked as deleted", messageServerId)
        }
    }

    @Suppress("LongParameterList")
    @Throws(MessagingException::class)
    private fun fetchUnsyncedMessages(
        syncConfig: SyncConfig,
        remoteFolder: Pop3Folder,
        unsyncedMessages: MutableList<Pop3Message>?,
        smallMessages: MutableList<Pop3Message>,
        largeMessages: MutableList<Pop3Message>,
        progress: AtomicInteger,
        todo: Int,
        fp: FetchProfile?,
        listener: SyncListener,
    ) {
        val folder = remoteFolder.serverId

        val earliestDate = syncConfig.earliestPollDate
        remoteFolder.fetch(
            unsyncedMessages,
            fp,
            object : MessageRetrievalListener<Pop3Message> {

                @Suppress("TooGenericExceptionCaught")
                override fun messageFinished(message: Pop3Message) {
                    try {
                        if (message.isSet(Flag.DELETED) || message.olderThan(earliestDate)) {
                            if (message.isSet(Flag.DELETED)) {
                                Log.v(
                                    "Newly downloaded message %s:%s:%s was marked deleted on server, " +
                                        "skipping",
                                    accountName,
                                    folder,
                                    message.uid,
                                )
                            } else {
                                Log.d(
                                    "Newly downloaded message %s is older than %s, skipping",
                                    message.uid,
                                    earliestDate,
                                )
                            }

                            progress.incrementAndGet()

                            // TODO: This might be the source of poll count errors in the UI.
                            // Is todo always the same as ofTotal
                            listener.syncProgress(folder, progress.get(), todo)
                            return
                        }

                        if (syncConfig.maximumAutoDownloadMessageSize > 0 &&
                            message.size > syncConfig.maximumAutoDownloadMessageSize
                        ) {
                            largeMessages.add(message)
                        } else {
                            smallMessages.add(message)
                        }
                    } catch (e: Exception) {
                        Log.e(e, "Error while storing downloaded message.")
                    }
                }
            },
            syncConfig.maximumAutoDownloadMessageSize,
        )
    }

    @Suppress("LongParameterList")
    @Throws(MessagingException::class)
    private fun downloadSmallMessages(
        remoteFolder: Pop3Folder,
        backendFolder: BackendFolder,
        smallMessages: MutableList<Pop3Message>,
        progress: AtomicInteger,
        newMessages: AtomicInteger,
        todo: Int,
        fp: FetchProfile?,
        listener: SyncListener,
    ) {
        val folder = remoteFolder.serverId

        Log.d("SYNC: Fetching %d small messages for folder %s", smallMessages.size, folder)

        remoteFolder.fetch(
            smallMessages,
            fp,
            object : MessageRetrievalListener<Pop3Message> {

                @Suppress("TooGenericExceptionCaught")
                override fun messageFinished(message: Pop3Message) {
                    try {
                        // Store the updated message locally

                        backendFolder.saveMessage(message, MessageDownloadState.FULL)
                        progress.incrementAndGet()

                        // Increment the number of "new messages" if the newly downloaded message is
                        // not marked as read.
                        if (!message.isSet(Flag.SEEN)) {
                            newMessages.incrementAndGet()
                        }

                        val messageServerId = message.uid
                        Log.v(
                            "About to notify listeners that we got a new small message %s:%s:%s",
                            accountName,
                            folder,
                            messageServerId,
                        )

                        // Update the listener with what we've found
                        listener.syncProgress(folder, progress.get(), todo)

                        val isOldMessage = isOldMessage(backendFolder, message)
                        listener.syncNewMessage(folder, messageServerId, isOldMessage)
                    } catch (e: Exception) {
                        Log.e(e, "SYNC: fetch small messages")
                    }
                }
            },
            -1,
        )

        Log.d("SYNC: Done fetching small messages for folder %s", folder)
    }

    private fun isOldMessage(backendFolder: BackendFolder, message: Pop3Message): Boolean {
        return message.olderThan(getLatestOldMessageSeenTime(backendFolder))
    }

    @Suppress("LongParameterList")
    @Throws(MessagingException::class)
    private fun downloadLargeMessages(
        syncConfig: SyncConfig,
        remoteFolder: Pop3Folder,
        backendFolder: BackendFolder,
        largeMessages: MutableList<Pop3Message>,
        progress: AtomicInteger,
        newMessages: AtomicInteger,
        todo: Int,
        fp: FetchProfile?,
        listener: SyncListener,
    ) {
        val folder = remoteFolder.serverId

        Log.d("SYNC: Fetching large messages for folder %s", folder)

        val maxDownloadSize = syncConfig.maximumAutoDownloadMessageSize
        remoteFolder.fetch(largeMessages, fp, null, maxDownloadSize)
        for (message in largeMessages) {
            downloadSaneBody(syncConfig, remoteFolder, backendFolder, message)

            val messageServerId = message.uid
            Log.v(
                "About to notify listeners that we got a new large message %s:%s:%s",
                accountName,
                folder,
                messageServerId,
            )

            // Update the listener with what we've found
            progress.incrementAndGet()

            // TODO do we need to re-fetch this here?
            val flags: Set<Flag> = backendFolder.getMessageFlags(messageServerId)
            // Increment the number of "new messages" if the newly downloaded message is
            // not marked as read.
            if (!flags.contains(Flag.SEEN)) {
                newMessages.incrementAndGet()
            }

            listener.syncProgress(folder, progress.get(), todo)

            val isOldMessage = isOldMessage(backendFolder, message)
            listener.syncNewMessage(folder, messageServerId, isOldMessage)
        }

        Log.d("SYNC: Done fetching large messages for folder %s", folder)
    }

    @Throws(MessagingException::class)
    private fun downloadSaneBody(
        syncConfig: SyncConfig,
        remoteFolder: Pop3Folder,
        backendFolder: BackendFolder,
        message: Pop3Message,
    ) {
        /*
         * The provider was unable to get the structure of the message, so
         * we'll download a reasonable portion of the message and mark it as
         * incomplete so the entire thing can be downloaded later if the user
         * wishes to download it.
         */
        val fp = FetchProfile()
        fp.add(FetchProfile.Item.BODY_SANE)

        /*
         *  TODO a good optimization here would be to make sure that all Stores set
         *  the proper size after this fetch and compare the before and after size. If
         *  they equal we can mark this SYNCHRONIZED instead of PARTIALLY_SYNCHRONIZED
         */
        val maxDownloadSize = syncConfig.maximumAutoDownloadMessageSize
        remoteFolder.fetch(mutableListOf<Pop3Message?>(message), fp, null, maxDownloadSize)

        var completeMessage = false
        // Certain (POP3) servers give you the whole message even when you ask for only the first x Kb
        if (!message.isSet(Flag.X_DOWNLOADED_FULL)) {
            /*
             * Mark the message as fully downloaded if the message size is smaller than
             * the account's autodownload size limit, otherwise mark as only a partial
             * download.  This will prevent the system from downloading the same message
             * twice.
             *
             * If there is no limit on autodownload size, that's the same as the message
             * being smaller than the max size
             */
            if (syncConfig.maximumAutoDownloadMessageSize == 0 ||
                message.size < syncConfig.maximumAutoDownloadMessageSize
            ) {
                completeMessage = true
            }
        }

        // Store the updated message locally
        if (completeMessage) {
            backendFolder.saveMessage(message, MessageDownloadState.FULL)
        } else {
            backendFolder.saveMessage(message, MessageDownloadState.PARTIAL)
        }
    }

    companion object {
        private const val EXTRA_LATEST_OLD_MESSAGE_SEEN_TIME = "latestOldMessageSeenTime"
    }
}
