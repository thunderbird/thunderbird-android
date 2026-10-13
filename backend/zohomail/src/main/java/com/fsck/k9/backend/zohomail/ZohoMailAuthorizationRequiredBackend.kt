package com.fsck.k9.backend.zohomail

import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.api.BackendPusher
import com.fsck.k9.backend.api.BackendPusherCallback
import com.fsck.k9.backend.api.SyncConfig
import com.fsck.k9.backend.api.SyncListener
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.BodyFactory
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.Part
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.feature.mail.folder.api.FolderPathDelimiter

/** Returned when a Zoho account has no usable OAuth state; it never makes network requests. */
object ZohoMailAuthorizationRequiredBackend : Backend {
    private const val MESSAGE = "Zoho Mail authorization is required. Sign in again."

    override val supportsFlags = false
    override val supportsExpunge = false
    override val supportsMove = false
    override val supportsCopy = false
    override val supportsUpload = false
    override val savesSentMessagesToServer = false
    override val supportsTrashFolder = false
    override val supportsSearchByDate = false
    override val supportsFolderSubscriptions = false
    override val isPushCapable = false
    override val isCacheable = false

    override fun refreshFolderList(): FolderPathDelimiter? = loginRequired()

    override fun sync(folderServerId: String, config: SyncConfig, listener: SyncListener) {
        listener.syncFailed(folderServerId, MESSAGE, authenticationFailure())
    }

    override fun downloadMessage(syncConfig: SyncConfig, folderServerId: String, messageServerId: String) = loginRequired()

    override fun downloadMessageStructure(folderServerId: String, messageServerId: String) = loginRequired()

    override suspend fun downloadCompleteMessage(folderServerId: String, messageServerId: String) = loginRequired()

    override fun setFlag(folderServerId: String, messageServerIds: List<String>, flag: Flag, newState: Boolean) =
        loginRequired()

    override fun markAllAsRead(folderServerId: String) = loginRequired()

    override fun expunge(folderServerId: String) = loginRequired()

    override fun deleteMessages(folderServerId: String, messageServerIds: List<String>) = loginRequired()

    override fun deleteAllMessages(folderServerId: String) = loginRequired()

    override fun moveMessages(
        sourceFolderServerId: String,
        targetFolderServerId: String,
        messageServerIds: List<String>,
    ): Map<String, String>? = loginRequired()

    override fun moveMessagesAndMarkAsRead(
        sourceFolderServerId: String,
        targetFolderServerId: String,
        messageServerIds: List<String>,
    ): Map<String, String>? = loginRequired()

    override fun copyMessages(
        sourceFolderServerId: String,
        targetFolderServerId: String,
        messageServerIds: List<String>,
    ): Map<String, String>? = loginRequired()

    override fun search(
        folderServerId: String,
        query: String?,
        requiredFlags: Set<Flag>?,
        forbiddenFlags: Set<Flag>?,
        performFullTextSearch: Boolean,
    ): List<String> = loginRequired()

    override fun fetchPart(folderServerId: String, messageServerId: String, part: Part, bodyFactory: BodyFactory) =
        loginRequired()

    override fun findByMessageId(folderServerId: String, messageId: String): String? = loginRequired()

    override fun uploadMessage(folderServerId: String, message: Message): String? = loginRequired()

    override fun sendMessage(message: Message) = loginRequired()

    @Suppress("UNUSED_PARAMETER")
    override fun createPusher(callback: BackendPusherCallback): BackendPusher = NoOpPusher

    private fun authenticationFailure() = AuthenticationFailedException(MESSAGE)

    private fun loginRequired(): Nothing = throw authenticationFailure()

    private object NoOpPusher : BackendPusher {
        override fun start() = Unit
        override fun updateFolders(folderServerIds: Collection<String>) = Unit
        override fun stop() = Unit
        override fun reconnect() = Unit
    }
}
