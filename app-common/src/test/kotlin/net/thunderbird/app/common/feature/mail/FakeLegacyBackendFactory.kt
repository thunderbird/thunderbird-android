package net.thunderbird.app.common.feature.mail

import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.api.BackendPusher
import com.fsck.k9.backend.api.BackendPusherCallback
import com.fsck.k9.backend.api.SyncConfig
import com.fsck.k9.backend.api.SyncListener
import com.fsck.k9.backends.ImapBackendFactory
import com.fsck.k9.mail.BodyFactory
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.Part
import net.thunderbird.core.android.account.LegacyAccountDto

internal class FakeLegacyBackendFactory : ImapBackendFactory {
    var lastAccount: LegacyAccountDto? = null

    override fun createBackend(account: LegacyAccountDto): Backend {
        lastAccount = account

        return object : Backend {
            override val supportsFlags = false
            override val supportsExpunge = false
            override val supportsMove = false
            override val supportsCopy = false
            override val supportsUpload = false
            override val supportsTrashFolder = false
            override val supportsSearchByDate = false
            override val supportsFolderSubscriptions = false
            override val isPushCapable = false
            override fun refreshFolderList() = null
            override fun sync(
                folderServerId: String,
                syncConfig: SyncConfig,
                listener: SyncListener,
            ) = Unit
            override fun downloadMessage(
                syncConfig: SyncConfig,
                folderServerId: String,
                messageServerId: String,
            ) = Unit
            override fun downloadMessageStructure(folderServerId: String, messageServerId: String) = Unit
            override fun downloadCompleteMessage(folderServerId: String, messageServerId: String) = Unit
            override fun setFlag(
                folderServerId: String,
                messageServerIds: List<String>,
                flag: Flag,
                newState: Boolean,
            ) = Unit
            override fun markAllAsRead(folderServerId: String) = Unit
            override fun expunge(folderServerId: String) = Unit
            override fun deleteMessages(folderServerId: String, messageServerIds: List<String>) = Unit
            override fun deleteAllMessages(folderServerId: String) = Unit
            override fun moveMessages(
                sourceFolderServerId: String,
                targetFolderServerId: String,
                messageServerIds: List<String>,
            ) = null
            override fun moveMessagesAndMarkAsRead(
                sourceFolderServerId: String,
                targetFolderServerId: String,
                messageServerIds: List<String>,
            ) = null
            override fun copyMessages(
                sourceFolderServerId: String,
                targetFolderServerId: String,
                messageServerIds: List<String>,
            ) = null
            override fun search(
                folderServerId: String,
                query: String?,
                requiredFlags: Set<Flag>?,
                forbiddenFlags: Set<Flag>?,
                performFullTextSearch: Boolean,
            ) = emptyList<String>()
            override fun fetchPart(
                folderServerId: String,
                messageServerId: String,
                part: Part,
                bodyFactory: BodyFactory,
            ) = Unit
            override fun findByMessageId(folderServerId: String, messageId: String) = null
            override fun uploadMessage(folderServerId: String, message: Message) = null
            override fun sendMessage(message: Message) = Unit
            override fun createPusher(
                callback: BackendPusherCallback,
            ) = object : BackendPusher {
                override fun start() = Unit
                override fun updateFolders(folderServerIds: Collection<String>) = Unit
                override fun stop() = Unit
                override fun reconnect() = Unit
            }
        }
    }
}
