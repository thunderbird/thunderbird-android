package com.fsck.k9.backend.jmap

import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.SyncConfig
import com.fsck.k9.backend.api.SyncListener
import com.fsck.k9.mail.BodyFactory
import com.fsck.k9.mail.FetchProfile
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.PushReceiver
import com.fsck.k9.mail.Pusher
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import rs.ltt.jmap.client.JmapClient
import rs.ltt.jmap.client.http.BasicAuthHttpAuthentication
import rs.ltt.jmap.client.http.HttpAuthentication
import rs.ltt.jmap.common.method.call.core.EchoMethodCall

class JmapBackend(
    backendStorage: BackendStorage,
    okHttpClient: OkHttpClient,
    config: JmapConfig
) : Backend {
    private val httpAuthentication = config.toHttpAuthentication()
    private val jmapClient = createJmapClient(config, httpAuthentication)
    private val accountId = config.accountId
    private val commandRefreshFolderList = CommandRefreshFolderList(backendStorage, jmapClient, accountId)
    private val commandSync = CommandSync(backendStorage, jmapClient, okHttpClient, accountId, httpAuthentication)
    private val commandSetFlag = CommandSetFlag(jmapClient, accountId)
    override val supportsSeenFlag = true
    override val supportsExpunge = false
    override val supportsMove = true
    override val supportsCopy = true
    override val supportsUpload = true
    override val supportsTrashFolder = true
    override val supportsSearchByDate = true
    override val isPushCapable = false // FIXME
    override val isDeleteMoveToTrash = false

    override fun refreshFolderList() {
        commandRefreshFolderList.refreshFolderList()
    }

    override fun sync(folder: String, syncConfig: SyncConfig, listener: SyncListener) {
        commandSync.sync(folder, syncConfig, listener)
    }

    override fun downloadMessage(syncConfig: SyncConfig, folderServerId: String, messageServerId: String) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun setFlag(folderServerId: String, messageServerIds: List<String>, flag: Flag, newState: Boolean) {
        commandSetFlag.setFlag(messageServerIds, flag, newState)
    }

    override fun markAllAsRead(folderServerId: String) {
        commandSetFlag.markAllAsRead(folderServerId)
    }

    override fun expunge(folderServerId: String) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun expungeMessages(folderServerId: String, messageServerIds: List<String>) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun deleteMessages(folderServerId: String, messageServerIds: List<String>) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun deleteAllMessages(folderServerId: String) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun moveMessages(sourceFolderServerId: String, targetFolderServerId: String, messageServerIds: List<String>): Map<String, String>? {
        throw UnsupportedOperationException("not implemented")
    }

    override fun moveMessagesAndMarkAsRead(sourceFolderServerId: String, targetFolderServerId: String, messageServerIds: List<String>): Map<String, String>? {
        throw UnsupportedOperationException("not implemented")
    }

    override fun copyMessages(sourceFolderServerId: String, targetFolderServerId: String, messageServerIds: List<String>): Map<String, String>? {
        throw UnsupportedOperationException("not implemented")
    }

    override fun search(folderServerId: String, query: String?, requiredFlags: Set<Flag>?, forbiddenFlags: Set<Flag>?): List<String> {
        throw UnsupportedOperationException("not implemented")
    }

    override fun fetchMessage(folderServerId: String, messageServerId: String, fetchProfile: FetchProfile): Message {
        throw UnsupportedOperationException("not implemented")
    }

    override fun fetchPart(folderServerId: String, messageServerId: String, part: Part, bodyFactory: BodyFactory) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun findByMessageId(folderServerId: String, messageId: String): String? {
        throw UnsupportedOperationException("not implemented")
    }

    override fun uploadMessage(folderServerId: String, message: Message): String? {
        throw UnsupportedOperationException("not implemented")
    }

    override fun createPusher(receiver: PushReceiver): Pusher {
        throw UnsupportedOperationException("not implemented")
    }

    override fun checkIncomingServerSettings() {
        jmapClient.call(EchoMethodCall()).get()
    }

    override fun sendMessage(message: Message) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun checkOutgoingServerSettings() {
        checkIncomingServerSettings()
    }

    private fun JmapConfig.toHttpAuthentication(): HttpAuthentication {
        return BasicAuthHttpAuthentication(username, password)
    }

    private fun createJmapClient(jmapConfig: JmapConfig, httpAuthentication: HttpAuthentication): JmapClient {
        return if (jmapConfig.baseUrl == null) {
            JmapClient(httpAuthentication)
        } else {
            val baseHttpUrl = jmapConfig.baseUrl.toHttpUrlOrNull()
            JmapClient(httpAuthentication, baseHttpUrl)
        }
    }
}
