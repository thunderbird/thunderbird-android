package com.fsck.k9.backend.imap

import com.fsck.k9.backend.api.BackendFolder
import com.fsck.k9.backend.api.BackendFolder.MoreMessages
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.SyncConfig
import com.fsck.k9.backend.api.SyncConfig.ExpungePolicy
import com.fsck.k9.backend.api.SyncListener
import com.fsck.k9.helper.ExceptionHelper
import com.fsck.k9.logging.Timber
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.BodyFactory
import com.fsck.k9.mail.DefaultBodyFactory
import com.fsck.k9.mail.FetchProfile
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.MessageDownloadState
import com.fsck.k9.mail.internet.MessageExtractor
import com.fsck.k9.mail.store.imap.FetchListener
import com.fsck.k9.mail.store.imap.ImapFolder
import com.fsck.k9.mail.store.imap.ImapMessage
import com.fsck.k9.mail.store.imap.ImapStore
import com.fsck.k9.mail.store.imap.OpenMode
import java.util.Collections
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

internal class ImapSync(
    private val accountName: String,
    private val backendStorage: BackendStorage,
    private val imapStore: ImapStore,
) {
    fun sync(folder: String, syncConfig: SyncConfig, listener: SyncListener) {
        synchronizeMailboxSynchronous(folder, syncConfig, listener)
    }

    private fun synchronizeMailboxSynchronous(folder: String, syncConfig: SyncConfig, listener: SyncListener) {
        Timber.i("Synchronizing folder %s:%s", accountName, folder)

        var remoteFolder: ImapFolder? = null
        var backendFolder: BackendFolder? = null
        var newHighestKnownUid: Long = 0
        try {
            Timber.v("SYNC: About to get local folder %s", folder)

            backendFolder = backendStorage.getFolder(folder)

            listener.syncStarted(folder)

            Timber.v("SYNC: About to get remote folder %s", folder)
            remoteFolder = imapStore.getFolder(folder)

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
            Timber.v("SYNC: About to open remote folder %s", folder)

            if (syncConfig.expungePolicy === ExpungePolicy.ON_POLL) {
                Timber.d("SYNC: Expunging folder %s:%s", accountName, folder)
                if (!remoteFolder.isOpen || remoteFolder.mode != OpenMode.READ_WRITE) {
                    remoteFolder.open(OpenMode.READ_WRITE)
                }
                remoteFolder.expunge()
            }

            remoteFolder.open(OpenMode.READ_ONLY)

            listener.syncAuthenticationSuccess()

            val uidValidity = remoteFolder.getUidValidity()
            val oldUidValidity = backendFolder.getFolderExtraNumber(EXTRA_UID_VALIDITY)
            if (oldUidValidity == null && uidValidity != null) {
                Timber.d("SYNC: Saving UIDVALIDITY for %s", folder)
                backendFolder.setFolderExtraNumber(EXTRA_UID_VALIDITY, uidValidity)
            } else if (oldUidValidity != null && oldUidValidity != uidValidity) {
                Timber.d("SYNC: UIDVALIDITY for %s changed; clearing local message cache", folder)
                backendFolder.clearAllMessages()
                backendFolder.setFolderExtraNumber(EXTRA_UID_VALIDITY, uidValidity!!)
                backendFolder.setFolderExtraNumber(EXTRA_HIGHEST_KNOWN_UID, 0)
            }

            /*
             * Get the message list from the local store and create an index of
             * the uids within the list.
             */

            val highestKnownUid = backendFolder.getFolderExtraNumber(EXTRA_HIGHEST_KNOWN_UID) ?: 0
            var localUidMap: Map<String, Long?>? = backendFolder.getAllMessagesAndEffectiveDates()

            /*
             * Get the remote message count.
             */
            val remoteMessageCount = remoteFolder.messageCount

            var visibleLimit = backendFolder.visibleLimit
            if (visibleLimit < 0) {
                visibleLimit = syncConfig.defaultVisibleLimit
            }

            val remoteMessages = mutableListOf<ImapMessage>()
            val remoteUidMap = mutableMapOf<String, ImapMessage>()

            Timber.v("SYNC: Remote message count for folder %s is %d", folder, remoteMessageCount)

            val earliestDate = syncConfig.earliestPollDate
            val earliestTimestamp = earliestDate?.time ?: 0L

            var remoteStart = 1
            if (remoteMessageCount > 0) {
                /* Message numbers start at 1.  */
                remoteStart = if (visibleLimit > 0) {
                    max(0, remoteMessageCount - visibleLimit) + 1
                } else {
                    1
                }

                Timber.v(
                    "SYNC: About to get messages %d through %d for folder %s",
                    remoteStart,
                    remoteMessageCount,
                    folder,
                )

                val headerProgress = AtomicInteger(0)
                listener.syncHeadersStarted(folder)

                val remoteMessageArray = remoteFolder.getMessages(remoteStart, remoteMessageCount, earliestDate, null)

                val messageCount = remoteMessageArray.size

                for (thisMess in remoteMessageArray) {
                    headerProgress.incrementAndGet()
                    listener.syncHeadersProgress(folder, headerProgress.get(), messageCount)

                    val uid = thisMess.uid.toLong()
                    if (uid > highestKnownUid && uid > newHighestKnownUid) {
                        newHighestKnownUid = uid
                    }

                    val localMessageTimestamp = localUidMap!![thisMess.uid]
                    if (localMessageTimestamp == null || localMessageTimestamp >= earliestTimestamp) {
                        remoteMessages.add(thisMess)
                        remoteUidMap[thisMess.uid] = thisMess
                    }
                }

                Timber.v("SYNC: Got %d messages for folder %s", remoteUidMap.size, folder)

                listener.syncHeadersFinished(folder, headerProgress.get(), remoteUidMap.size)
            } else if (remoteMessageCount < 0) {
                throw Exception("Message count $remoteMessageCount for folder $folder")
            }

            /*
             * Remove any messages that are in the local store but no longer on the remote store or are too old
             */
            var moreMessages = backendFolder.getMoreMessages()
            if (syncConfig.syncRemoteDeletions) {
                val destroyMessageUids = mutableListOf<String>()
                for (localMessageUid in localUidMap!!.keys) {
                    if (remoteUidMap[localMessageUid] == null) {
                        destroyMessageUids.add(localMessageUid)
                    }
                }

                if (destroyMessageUids.isNotEmpty()) {
                    moreMessages = MoreMessages.UNKNOWN
                    backendFolder.destroyMessages(destroyMessageUids)
                    for (uid in destroyMessageUids) {
                        listener.syncRemovedMessage(folder, uid)
                    }
                }
            }

            @Suppress("UNUSED_VALUE") // free memory early? (better break up the method!)
            localUidMap = null

            if (moreMessages === MoreMessages.UNKNOWN) {
                updateMoreMessages(remoteFolder, backendFolder, earliestDate, remoteStart)
            }

            /*
             * Now we download the actual content of messages.
             */
            downloadMessages(
                syncConfig,
                remoteFolder,
                backendFolder,
                remoteMessages,
                highestKnownUid,
                listener,
            )

            listener.folderStatusChanged(folder)

            /* Notify listeners that we're finally done. */

            backendFolder.setLastChecked(System.currentTimeMillis())
            backendFolder.setStatus(null)

            Timber.d("Done synchronizing folder %s:%s @ %tc", accountName, folder, System.currentTimeMillis())

            listener.syncFinished(folder)

            Timber.i("Done synchronizing folder %s:%s", accountName, folder)
        } catch (e: AuthenticationFailedException) {
            listener.syncFailed(folder, "Authentication failure", e)
        } catch (e: Exception) {
            Timber.e(e, "synchronizeMailbox")
            // If we don't set the last checked, it can try too often during
            // failure conditions
            val rootMessage = ExceptionHelper.getRootCauseMessage(e)
            if (backendFolder != null) {
                try {
                    backendFolder.setStatus(rootMessage)
                    backendFolder.setLastChecked(System.currentTimeMillis())
                } catch (e: Exception) {
                    Timber.e(e, "Could not set last checked on folder %s:%s", accountName, folder)
                }
            }

            listener.syncFailed(folder, rootMessage, e)

            Timber.e(
                "Failed synchronizing folder %s:%s @ %tc",
                accountName,
                folder,
                System.currentTimeMillis(),
            )
        } finally {
            if (newHighestKnownUid > 0 && backendFolder != null) {
                Timber.v("Saving new highest known UID: %d", newHighestKnownUid)
                backendFolder.setFolderExtraNumber(EXTRA_HIGHEST_KNOWN_UID, newHighestKnownUid)
            }
            remoteFolder?.close()
        }
    }

    fun downloadMessage(syncConfig: SyncConfig, folderServerId: String, messageServerId: String) {
        val backendFolder = backendStorage.getFolder(folderServerId)
        val remoteFolder = imapStore.getFolder(folderServerId)
        try {
            remoteFolder.open(OpenMode.READ_ONLY)
            val remoteMessage = remoteFolder.getMessage(messageServerId)

            downloadMessages(
                syncConfig,
                remoteFolder,
                backendFolder,
                listOf(remoteMessage),
                null,
                SimpleSyncListener(),
            )
        } finally {
            remoteFolder.close()
        }
    }

    /**
     * Fetches the messages described by inputMessages from the remote store and writes them to local storage.
     *
     * @param remoteFolder
     * The remote folder to download messages from.
     * @param backendFolder
     * The [BackendFolder] instance corresponding to the remote folder.
     * @param inputMessages
     * A list of messages objects that store the UIDs of which messages to download.
     */
    private fun downloadMessages(
        syncConfig: SyncConfig,
        remoteFolder: ImapFolder,
        backendFolder: BackendFolder,
        inputMessages: List<ImapMessage>,
        highestKnownUid: Long?,
        listener: SyncListener,
    ) {
        val folder = remoteFolder.serverId

        val syncFlagMessages = mutableListOf<ImapMessage>()
        var unsyncedMessages = mutableListOf<ImapMessage>()
        val downloadedMessageCount = AtomicInteger(0)

        val messages = inputMessages.toMutableList()
        for (message in messages) {
            evaluateMessageForDownload(
                message,
                backendFolder,
                unsyncedMessages,
                syncFlagMessages,
            )
        }

        val progress = AtomicInteger(0)
        val todo = unsyncedMessages.size + syncFlagMessages.size
        listener.syncProgress(folder, progress.get(), todo)

        Timber.d("SYNC: Have %d unsynced messages", unsyncedMessages.size)

        messages.clear()
        val largeMessages = mutableListOf<ImapMessage>()
        val smallMessages = mutableListOf<ImapMessage>()
        if (unsyncedMessages.isNotEmpty()) {
            Collections.sort(unsyncedMessages, UidReverseComparator())
            val visibleLimit = backendFolder.visibleLimit
            val listSize = unsyncedMessages.size

            if (visibleLimit in 1 until listSize) {
                unsyncedMessages = unsyncedMessages.subList(0, visibleLimit)
            }

            Timber.d("SYNC: About to fetch %d unsynced messages for folder %s", unsyncedMessages.size, folder)

            fetchUnsyncedMessages(
                syncConfig,
                remoteFolder,
                unsyncedMessages,
                smallMessages,
                largeMessages,
                progress,
                todo,
                listener,
            )

            Timber.d("SYNC: Synced unsynced messages for folder %s", folder)
        }

        Timber.d(
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
        val maxDownloadSize = syncConfig.maximumAutoDownloadMessageSize
        // TODO: Only fetch small and large messages if we have some
        downloadSmallMessages(
            remoteFolder,
            backendFolder,
            smallMessages,
            progress,
            downloadedMessageCount,
            todo,
            highestKnownUid,
            listener,
        )
        smallMessages.clear()

        /*
         * Now do the large messages that require more round trips.
         */
        downloadLargeMessages(
            remoteFolder,
            backendFolder,
            largeMessages,
            progress,
            downloadedMessageCount,
            todo,
            highestKnownUid,
            listener,
            maxDownloadSize,
        )
        largeMessages.clear()

        /*
         * Refresh the flags for any messages in the local store that we didn't just
         * download.
         */
        refreshLocalMessageFlags(syncConfig, remoteFolder, backendFolder, syncFlagMessages, progress, todo, listener)

        Timber.d("SYNC: Synced remote messages for folder %s, %d new messages", folder, downloadedMessageCount.get())
    }

    private fun evaluateMessageForDownload(
        message: ImapMessage,
        backendFolder: BackendFolder,
        unsyncedMessages: MutableList<ImapMessage>,
        syncFlagMessages: MutableList<ImapMessage>,
    ) {
        val messageServerId = message.uid
        if (message.isSet(Flag.DELETED)) {
            Timber.v("Message with uid %s is marked as deleted", messageServerId)
            syncFlagMessages.add(message)
            return
        }

        val messagePresentLocally = backendFolder.isMessagePresent(messageServerId)
        if (!messagePresentLocally) {
            Timber.v("Message with uid %s has not yet been downloaded", messageServerId)
            unsyncedMessages.add(message)
            return
        }

        val messageFlags = backendFolder.getMessageFlags(messageServerId)
        if (!messageFlags.contains(Flag.DELETED)) {
            Timber.v("Message with uid %s is present in the local store", messageServerId)
            if (!messageFlags.contains(Flag.X_DOWNLOADED_FULL) && !messageFlags.contains(Flag.X_DOWNLOADED_PARTIAL)) {
                Timber.v("Message with uid %s is not downloaded, even partially; trying again", messageServerId)
                unsyncedMessages.add(message)
            } else {
                syncFlagMessages.add(message)
            }
        } else {
            Timber.v("Local copy of message with uid %s is marked as deleted", messageServerId)
        }
    }

    private fun isOldMessage(messageServerId: String, highestKnownUid: Long?): Boolean {
        if (highestKnownUid == null) return false

        try {
            val messageUid = messageServerId.toLong()
            return messageUid <= highestKnownUid
        } catch (e: NumberFormatException) {
            Timber.w(e, "Couldn't parse UID: %s", messageServerId)
        }

        return false
    }

    private fun fetchUnsyncedMessages(
        syncConfig: SyncConfig,
        remoteFolder: ImapFolder,
        unsyncedMessages: List<ImapMessage>,
        smallMessages: MutableList<ImapMessage>,
        largeMessages: MutableList<ImapMessage>,
        progress: AtomicInteger,
        todo: Int,
        listener: SyncListener,
    ) {
        val folder = remoteFolder.serverId
        val fetchProfile = FetchProfile().apply {
            add(FetchProfile.Item.FLAGS)
            add(FetchProfile.Item.ENVELOPE)
        }

        remoteFolder.fetch(
            unsyncedMessages,
            fetchProfile,
            object : FetchListener {
                override fun onFetchResponse(message: ImapMessage, isFirstResponse: Boolean) {
                    try {
                        if (message.isSet(Flag.DELETED)) {
                            Timber.v(
                                "Newly downloaded message %s:%s:%s was marked deleted on server, skipping",
                                accountName,
                                folder,
                                message.uid,
                            )

                            if (isFirstResponse) {
                                progress.incrementAndGet()
                            }

                            // TODO: This might be the source of poll count errors in the UI. Is todo always the same as ofTotal
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
                        Timber.e(e, "Error while storing downloaded message.")
                    }
                }
            },
            syncConfig.maximumAutoDownloadMessageSize,
        )
    }

    private fun downloadSmallMessages(
        remoteFolder: ImapFolder,
        backendFolder: BackendFolder,
        smallMessages: List<ImapMessage>,
        progress: AtomicInteger,
        downloadedMessageCount: AtomicInteger,
        todo: Int,
        highestKnownUid: Long?,
        listener: SyncListener,
    ) {
        val folder = remoteFolder.serverId
        val fetchProfile = FetchProfile().apply {
            add(FetchProfile.Item.BODY)
        }

        Timber.d("SYNC: Fetching %d small messages for folder %s", smallMessages.size, folder)

        remoteFolder.fetch(
            smallMessages,
            fetchProfile,
            object : FetchListener {
                override fun onFetchResponse(message: ImapMessage, isFirstResponse: Boolean) {
                    try {
                        // Store the updated message locally
                        backendFolder.saveMessage(message, MessageDownloadState.FULL)

                        if (isFirstResponse) {
                            progress.incrementAndGet()
                            downloadedMessageCount.incrementAndGet()
                        }

                        val messageServerId = message.uid
                        Timber.v(
                            "About to notify listeners that we got a new small message %s:%s:%s",
                            accountName,
                            folder,
                            messageServerId,
                        )

                        // Update the listener with what we've found
                        listener.syncProgress(folder, progress.get(), todo)

                        val isOldMessage = isOldMessage(messageServerId, highestKnownUid)
                        listener.syncNewMessage(folder, messageServerId, isOldMessage)
                    } catch (e: Exception) {
                        Timber.e(e, "SYNC: fetch small messages")
                    }
                }
            },
            -1,
        )

        Timber.d("SYNC: Done fetching small messages for folder %s", folder)
    }

    private fun downloadLargeMessages(
        remoteFolder: ImapFolder,
        backendFolder: BackendFolder,
        largeMessages: List<ImapMessage>,
        progress: AtomicInteger,
        downloadedMessageCount: AtomicInteger,
        todo: Int,
        highestKnownUid: Long?,
        listener: SyncListener,
        maxDownloadSize: Int,
    ) {
        val folder = remoteFolder.serverId
        val fetchProfile = FetchProfile().apply {
            add(FetchProfile.Item.STRUCTURE)
        }

        Timber.d("SYNC: Fetching large messages for folder %s", folder)

        remoteFolder.fetch(largeMessages, fetchProfile, null, maxDownloadSize)
        for (message in largeMessages) {
            if (message.body == null) {
                downloadSaneBody(remoteFolder, backendFolder, message, maxDownloadSize)
            } else {
                downloadPartial(remoteFolder, backendFolder, message, maxDownloadSize)
            }

            val messageServerId = message.uid
            Timber.v(
                "About to notify listeners that we got a new large message %s:%s:%s",
                accountName,
                folder,
                messageServerId,
            )

            // Update the listener with what we've found
            progress.incrementAndGet()
            downloadedMessageCount.incrementAndGet()

            listener.syncProgress(folder, progress.get(), todo)

            val isOldMessage = isOldMessage(messageServerId, highestKnownUid)
            listener.syncNewMessage(folder, messageServerId, isOldMessage)
        }

        Timber.d("SYNC: Done fetching large messages for folder %s", folder)
    }

    private fun refreshLocalMessageFlags(
        syncConfig: SyncConfig,
        remoteFolder: ImapFolder,
        backendFolder: BackendFolder,
        syncFlagMessages: List<ImapMessage>,
        progress: AtomicInteger,
        todo: Int,
        listener: SyncListener,
    ) {
        val folder = remoteFolder.serverId
        Timber.d("SYNC: About to sync flags for %d remote messages for folder %s", syncFlagMessages.size, folder)

        val fetchProfile = FetchProfile()
        fetchProfile.add(FetchProfile.Item.FLAGS)

        val undeletedMessages = mutableListOf<ImapMessage>()
        for (message in syncFlagMessages) {
            if (!message.isSet(Flag.DELETED)) {
                undeletedMessages.add(message)
            }
        }

        val maxDownloadSize = syncConfig.maximumAutoDownloadMessageSize
        remoteFolder.fetch(undeletedMessages, fetchProfile, null, maxDownloadSize)
        for (remoteMessage in syncFlagMessages) {
            val messageChanged = syncFlags(syncConfig, backendFolder, remoteMessage)
            if (messageChanged) {
                listener.syncFlagChanged(folder, remoteMessage.uid)
            }
            progress.incrementAndGet()
            listener.syncProgress(folder, progress.get(), todo)
        }
    }

    private fun downloadSaneBody(
        remoteFolder: ImapFolder,
        backendFolder: BackendFolder,
        message: ImapMessage,
        maxDownloadSize: Int,
    ) {
        /*
         * The provider was unable to get the structure of the message, so
         * we'll download a reasonable portion of the message and mark it as
         * incomplete so the entire thing can be downloaded later if the user
         * wishes to download it.
         */
        val fetchProfile = FetchProfile()
        fetchProfile.add(FetchProfile.Item.BODY_SANE)
        /*
         *  TODO a good optimization here would be to make sure that all Stores set
         *  the proper size after this fetch and compare the before and after size. If
         *  they equal we can mark this SYNCHRONIZED instead of PARTIALLY_SYNCHRONIZED
         */
        remoteFolder.fetch(listOf(message), fetchProfile, null, maxDownloadSize)

        // Store the updated message locally
        backendFolder.saveMessage(message, MessageDownloadState.PARTIAL)
    }

    private fun downloadPartial(
        remoteFolder: ImapFolder,
        backendFolder: BackendFolder,
        message: ImapMessage,
        maxDownloadSize: Int,
    ) {
        /*
         * We have a structure to deal with, from which
         * we can pull down the parts we want to actually store.
         * Build a list of parts we are interested in. Text parts will be downloaded
         * right now, attachments will be left for later.
         */
        val viewables = MessageExtractor.collectTextParts(message)

        /*
         * Now download the parts we're interested in storing.
         */
        val bodyFactory: BodyFactory = DefaultBodyFactory()
        for (part in viewables) {
            remoteFolder.fetchPart(message, part, bodyFactory, maxDownloadSize)
        }

        // Store the updated message locally
        backendFolder.saveMessage(message, MessageDownloadState.PARTIAL)
    }

    private fun syncFlags(syncConfig: SyncConfig, backendFolder: BackendFolder, remoteMessage: ImapMessage): Boolean {
        val messageServerId = remoteMessage.uid
        if (!backendFolder.isMessagePresent(messageServerId)) return false

        val localMessageFlags = backendFolder.getMessageFlags(messageServerId)
        if (localMessageFlags.contains(Flag.DELETED)) return false

        var messageChanged = false
        if (remoteMessage.isSet(Flag.DELETED)) {
            if (syncConfig.syncRemoteDeletions) {
                backendFolder.setMessageFlag(messageServerId, Flag.DELETED, true)
                messageChanged = true
            }
        } else {
            for (flag in syncConfig.syncFlags) {
                if (remoteMessage.isSet(flag) != localMessageFlags.contains(flag)) {
                    backendFolder.setMessageFlag(messageServerId, flag, remoteMessage.isSet(flag))
                    messageChanged = true
                }
            }
        }

        return messageChanged
    }

    private fun updateMoreMessages(
        remoteFolder: ImapFolder,
        backendFolder: BackendFolder,
        earliestDate: Date?,
        remoteStart: Int,
    ) {
        if (remoteStart == 1) {
            backendFolder.setMoreMessages(MoreMessages.FALSE)
        } else {
            val moreMessagesAvailable = remoteFolder.areMoreMessagesAvailable(remoteStart, earliestDate)
            val newMoreMessages = if (moreMessagesAvailable) MoreMessages.TRUE else MoreMessages.FALSE
            backendFolder.setMoreMessages(newMoreMessages)
        }
    }

    companion object {
        private const val EXTRA_UID_VALIDITY = "imapUidValidity"
        private const val EXTRA_HIGHEST_KNOWN_UID = "imapHighestKnownUid"
    }
}
