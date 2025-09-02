package com.fsck.k9.backend.jmap

import com.fsck.k9.backend.api.BackendFolderUpdater
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.FolderType
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.feature.mail.folder.api.FOLDER_DEFAULT_PATH_DELIMITER
import net.thunderbird.feature.mail.folder.api.FolderPathDelimiter
import rs.ltt.jmap.client.JmapClient
import rs.ltt.jmap.client.api.ErrorResponseException
import rs.ltt.jmap.client.api.InvalidSessionResourceException
import rs.ltt.jmap.client.api.MethodErrorResponseException
import rs.ltt.jmap.client.api.UnauthorizedException
import rs.ltt.jmap.common.Request.Invocation.ResultReference
import rs.ltt.jmap.common.entity.Mailbox
import rs.ltt.jmap.common.entity.Role
import rs.ltt.jmap.common.method.call.mailbox.ChangesMailboxMethodCall
import rs.ltt.jmap.common.method.call.mailbox.GetMailboxMethodCall
import rs.ltt.jmap.common.method.response.mailbox.ChangesMailboxMethodResponse
import rs.ltt.jmap.common.method.response.mailbox.GetMailboxMethodResponse

internal class CommandRefreshFolderList(
    private val backendStorage: BackendStorage,
    private val jmapClient: JmapClient,
    private val accountId: String,
) {
    @Suppress("ThrowsCount", "TooGenericExceptionCaught")
    fun refreshFolderList(): FolderPathDelimiter? {
        try {
            backendStorage.createFolderUpdater().use { folderUpdater ->
                val state = backendStorage.getExtraString(STATE)
                if (state == null) {
                    fetchMailboxes(folderUpdater)
                } else {
                    fetchMailboxUpdates(folderUpdater, state)
                }
            }
        } catch (e: UnauthorizedException) {
            throw AuthenticationFailedException("Authentication failed", e)
        } catch (e: InvalidSessionResourceException) {
            throw MessagingException(e.message, true, e)
        } catch (e: ErrorResponseException) {
            throw MessagingException(e.message, true, e)
        } catch (e: MethodErrorResponseException) {
            throw MessagingException(e.message, e.isPermanentError, e)
        } catch (e: Exception) {
            throw MessagingException(e)
        }
        return FOLDER_DEFAULT_PATH_DELIMITER
    }

    private fun fetchMailboxes(folderUpdater: BackendFolderUpdater) {
        val call = jmapClient.call(
            GetMailboxMethodCall.builder().accountId(accountId).build(),
        )
        val response = call.getMainResponseBlocking<GetMailboxMethodResponse>()
        val foldersOnServer = response.list

        val oldFolderServerIds = backendStorage.getFolderServerIds()
        val (foldersToUpdate, foldersToCreate) = foldersOnServer.partition { it.id in oldFolderServerIds }

        for (folder in foldersToUpdate) {
            folderUpdater.changeFolder(folder.id, folder.name, folder.type)
        }

        val newFolders = foldersToCreate.map { folder ->
            FolderInfo(folder.id, folder.name, folder.type)
        }
        folderUpdater.createFolders(newFolders)

        val newFolderServerIds = foldersOnServer.map { it.id }
        val removedFolderServerIds = oldFolderServerIds - newFolderServerIds
        folderUpdater.deleteFolders(removedFolderServerIds)

        backendStorage.setExtraString(STATE, response.state)
    }

    private fun fetchMailboxUpdates(folderUpdater: BackendFolderUpdater, state: String) {
        try {
            fetchAllMailboxChanges(folderUpdater, state)
        } catch (e: MethodErrorResponseException) {
            if (e.methodErrorResponse.type == ERROR_CANNOT_CALCULATE_CHANGES) {
                fetchMailboxes(folderUpdater)
            } else {
                throw e
            }
        }
    }

    private fun fetchAllMailboxChanges(folderUpdater: BackendFolderUpdater, state: String) {
        var currentState = state
        do {
            val (newState, hasMoreChanges) = fetchMailboxChanges(folderUpdater, currentState)
            currentState = newState
        } while (hasMoreChanges)
    }

    private fun fetchMailboxChanges(folderUpdater: BackendFolderUpdater, state: String): UpdateState {
        val multiCall = jmapClient.newMultiCall()
        val mailboxChangesCall = multiCall.call(
            ChangesMailboxMethodCall.builder()
                .accountId(accountId)
                .sinceState(state)
                .build(),
        )
        val createdMailboxesCall = multiCall.call(
            GetMailboxMethodCall.builder()
                .accountId(accountId)
                .idsReference(mailboxChangesCall.createResultReference(ResultReference.Path.CREATED))
                .build(),
        )
        val changedMailboxesCall = multiCall.call(
            GetMailboxMethodCall.builder()
                .accountId(accountId)
                .idsReference(mailboxChangesCall.createResultReference(ResultReference.Path.UPDATED))
                .build(),
        )
        multiCall.execute()

        val mailboxChangesResponse = mailboxChangesCall.getMainResponseBlocking<ChangesMailboxMethodResponse>()
        val createdMailboxResponse = createdMailboxesCall.getMainResponseBlocking<GetMailboxMethodResponse>()
        val changedMailboxResponse = changedMailboxesCall.getMainResponseBlocking<GetMailboxMethodResponse>()

        val foldersToCreate = createdMailboxResponse.list.map { folder ->
            FolderInfo(folder.id, folder.name, folder.type)
        }
        folderUpdater.createFolders(foldersToCreate)

        for (folder in changedMailboxResponse.list) {
            folderUpdater.changeFolder(folder.id, folder.name, folder.type)
        }

        val destroyed = mailboxChangesResponse.destroyed
        destroyed?.let {
            folderUpdater.deleteFolders(it.toList())
        }

        backendStorage.setExtraString(STATE, mailboxChangesResponse.newState)

        return UpdateState(
            state = mailboxChangesResponse.newState,
            hasMoreChanges = mailboxChangesResponse.isHasMoreChanges,
        )
    }

    private val Mailbox.type: FolderType
        get() = when (role) {
            Role.INBOX -> FolderType.INBOX
            Role.ARCHIVE -> FolderType.ARCHIVE
            Role.DRAFTS -> FolderType.DRAFTS
            Role.SENT -> FolderType.SENT
            Role.TRASH -> FolderType.TRASH
            Role.JUNK -> FolderType.SPAM
            else -> FolderType.REGULAR
        }

    private val MethodErrorResponseException.isPermanentError: Boolean
        get() = methodErrorResponse.type != ERROR_SERVER_UNAVAILABLE

    companion object {
        private const val STATE = "jmapState"
        private const val ERROR_SERVER_UNAVAILABLE = "serverUnavailable"
        private const val ERROR_CANNOT_CALCULATE_CHANGES = "cannotCalculateChanges"
    }

    private data class UpdateState(val state: String, val hasMoreChanges: Boolean)
}
