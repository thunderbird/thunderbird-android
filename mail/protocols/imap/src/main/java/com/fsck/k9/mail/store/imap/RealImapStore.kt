package com.fsck.k9.mail.store.imap

import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import com.fsck.k9.mail.ssl.TrustedSocketFactory
import com.fsck.k9.mail.store.imap.ImapStoreSettings.autoDetectNamespace
import com.fsck.k9.mail.store.imap.ImapStoreSettings.isSendClientInfo
import com.fsck.k9.mail.store.imap.ImapStoreSettings.isUseCompression
import com.fsck.k9.mail.store.imap.ImapStoreSettings.pathPrefix
import java.io.IOException
import java.util.Deque
import java.util.LinkedList
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.logging.legacy.Log

private const val LAST_ASCII_CODE = 127

internal open class RealImapStore(
    private val serverSettings: ServerSettings,
    override val config: ImapStoreConfig,
    private val trustedSocketFactory: TrustedSocketFactory,
    private val oauthTokenProvider: OAuth2TokenProvider?,
) : ImapStore, ImapConnectionManager, InternalImapStore {
    private val folderNameCodec: FolderNameCodec = FolderNameCodec()

    private val host: String = checkNotNull(serverSettings.host)

    private var pathPrefix: String?
    private var combinedPrefix: String? = null
    private var pathDelimiter: String? = null

    private val permanentFlagsIndex: MutableSet<Flag> = mutableSetOf()
    private val connections: Deque<ImapConnection> = LinkedList()

    @Volatile
    private var connectionGeneration = 1

    init {
        require(serverSettings.type == "imap") { "Expected IMAP ServerSettings" }

        val autoDetectNamespace = serverSettings.autoDetectNamespace
        val pathPrefixSetting = serverSettings.pathPrefix

        // Make extra sure pathPrefix is null if "auto-detect namespace" is configured
        pathPrefix = if (autoDetectNamespace) null else pathPrefixSetting
    }

    override fun getFolder(name: String): ImapFolder {
        return RealImapFolder(
            internalImapStore = this,
            connectionManager = this,
            serverId = name,
            folderNameCodec = folderNameCodec,
        )
    }

    override fun getCombinedPrefix(): String {
        return combinedPrefix ?: buildCombinedPrefix().also { combinedPrefix = it }
    }

    private fun buildCombinedPrefix(): String {
        val pathPrefix = pathPrefix ?: return ""

        val trimmedPathPrefix = pathPrefix.trim { it <= ' ' }
        val trimmedPathDelimiter = pathDelimiter?.trim { it <= ' ' }.orEmpty()

        return if (trimmedPathPrefix.endsWith(trimmedPathDelimiter)) {
            trimmedPathPrefix
        } else if (trimmedPathPrefix.isNotEmpty()) {
            trimmedPathPrefix + trimmedPathDelimiter
        } else {
            ""
        }
    }

    @Throws(MessagingException::class)
    override fun getFolders(): List<FolderListItem> {
        val connection = getConnection()

        return try {
            val folders = listFolders(connection, false)
            if (!config.isSubscribedFoldersOnly()) {
                return folders
            }

            val subscribedFolders = listFolders(connection, true)
            limitToSubscribedFolders(folders, subscribedFolders)
        } catch (e: AuthenticationFailedException) {
            connection.close()
            throw e
        } catch (e: IOException) {
            connection.close()
            throw MessagingException("Unable to get folder list.", e)
        } catch (e: MessagingException) {
            connection.close()
            throw MessagingException("Unable to get folder list.", e)
        } finally {
            releaseConnection(connection)
        }
    }

    private fun limitToSubscribedFolders(
        folders: List<FolderListItem>,
        subscribedFolders: List<FolderListItem>,
    ): List<FolderListItem> {
        val subscribedFolderServerIds = subscribedFolders.map { it.serverId }.toSet()
        return folders.filter { it.serverId in subscribedFolderServerIds }
    }

    @Throws(IOException::class, MessagingException::class)
    private fun listFolders(connection: ImapConnection, subscribedOnly: Boolean): List<FolderListItem> {
        val commandFormat = when {
            subscribedOnly -> {
                "LSUB \"\" %s"
            }
            connection.supportsListExtended -> {
                "LIST \"\" %s RETURN (SPECIAL-USE)"
            }
            else -> {
                "LIST \"\" %s"
            }
        }

        val encodedListPrefix = ImapUtility.encodeString(getCombinedPrefix() + "*")
        val responses = connection.executeSimpleCommand(String.format(commandFormat, encodedListPrefix))

        val listResponses = if (subscribedOnly) {
            ListResponse.parseLsub(responses)
        } else {
            ListResponse.parseList(responses)
        }

        val folderMap = mutableMapOf<String, FolderListItem>()
        for (listResponse in listResponses) {
            val serverId = listResponse.name

            if (pathDelimiter == null) {
                pathDelimiter = listResponse.hierarchyDelimiter
                combinedPrefix = null
            }

            if (RealImapFolder.INBOX.equals(serverId, ignoreCase = true)) {
                // We always add our own inbox entry to the returned list.
                continue
            } else if (listResponse.hasAttribute("\\NoSelect")) {
                // RFC 3501, section 7.2.2: It is not possible to use this name as a selectable mailbox.
                continue
            } else if (listResponse.hasAttribute("\\NonExistent")) {
                // RFC 5258, section 3: The "\NonExistent" attribute implies "\NoSelect".
                continue
            }

            val name = getFolderDisplayName(serverId)

            val type = when {
                listResponse.hasAttribute("\\Archive") -> FolderType.ARCHIVE
                listResponse.hasAttribute("\\All") -> FolderType.ARCHIVE
                listResponse.hasAttribute("\\Drafts") -> FolderType.DRAFTS
                listResponse.hasAttribute("\\Sent") -> FolderType.SENT
                listResponse.hasAttribute("\\Junk") -> FolderType.SPAM
                listResponse.hasAttribute("\\Trash") -> FolderType.TRASH
                else -> FolderType.REGULAR
            }

            val existingItem = folderMap[serverId]
            if (existingItem == null || existingItem.type == FolderType.REGULAR) {
                folderMap[serverId] = FolderListItem(serverId, name, type)
            }
        }

        return buildList {
            add(FolderListItem(RealImapFolder.INBOX, RealImapFolder.INBOX, FolderType.INBOX))
            addAll(folderMap.values)
        }
    }

    private fun getFolderDisplayName(serverId: String): String {
        val decodedFolderName = try {
            if (serverId.all { it.code <= LAST_ASCII_CODE }) {
                folderNameCodec.decode(serverId)
            } else {
                serverId
            }
        } catch (e: CharacterCodingException) {
            Log.w(e, "Folder name not correctly encoded with the UTF-7 variant as defined by RFC 3501: %s", serverId)
            serverId
        }

        val folderNameWithoutPrefix = removePrefixFromFolderName(decodedFolderName)
        return folderNameWithoutPrefix ?: decodedFolderName
    }

    private fun removePrefixFromFolderName(folderName: String): String? {
        val prefix = getCombinedPrefix()
        val prefixLength = prefix.length
        if (prefixLength == 0) {
            return folderName
        }

        if (!folderName.startsWith(prefix)) {
            // Folder name doesn't start with our configured prefix. But right now when building commands we prefix all
            // folders except the INBOX with the prefix. So we won't be able to use this folder.
            return null
        }

        return folderName.substring(prefixLength)
    }

    @Suppress("TooGenericExceptionCaught")
    @Throws(MessagingException::class, IOException::class)
    override fun checkSettings() {
        try {
            val connection = createImapConnection()

            connection.open()
            connection.close()
        } catch (e: Exception) {
            Log.e(e, "Error while checking server settings")
            throw e
        }
    }

    @Throws(MessagingException::class)
    override fun getConnection(): ImapConnection {
        while (true) {
            val connection = pollConnection() ?: return createImapConnection()

            try {
                connection.executeSimpleCommand(Commands.NOOP)

                // If the command completes without an error this connection is still usable.
                return connection
            } catch (ioe: IOException) {
                connection.close()
            }
        }
    }

    private fun pollConnection(): ImapConnection? {
        return synchronized(connections) {
            connections.poll()
        }
    }

    override fun releaseConnection(connection: ImapConnection?) {
        if (connection != null && connection.isConnected) {
            if (connection.connectionGeneration == connectionGeneration) {
                synchronized(connections) {
                    connections.offer(connection)
                }
            } else {
                connection.close()
            }
        }
    }

    override fun closeAllConnections() {
        Log.v("ImapStore.closeAllConnections()")

        val connectionsToClose = synchronized(connections) {
            val connectionsToClose = connections.toList()

            connectionGeneration++
            connections.clear()

            connectionsToClose
        }

        for (connection in connectionsToClose) {
            connection.close()
        }
    }

    open fun createImapConnection(): ImapConnection {
        return RealImapConnection(
            StoreImapSettings(),
            trustedSocketFactory,
            oauthTokenProvider,
            folderNameCodec,
            connectionGeneration,
        )
    }

    override val logLabel: String
        get() = config.logLabel

    override fun getPermanentFlagsIndex(): MutableSet<Flag> {
        return permanentFlagsIndex
    }

    private inner class StoreImapSettings : ImapSettings {
        override val host: String = this@RealImapStore.host
        override val port: Int = serverSettings.port
        override val connectionSecurity: ConnectionSecurity = serverSettings.connectionSecurity
        override val authType: AuthType = serverSettings.authenticationType
        override val username: String = serverSettings.username
        override val password: String? = serverSettings.password
        override val clientCertificateAlias: String? = serverSettings.clientCertificateAlias

        override val useCompression: Boolean = serverSettings.isUseCompression

        override val clientInfo: ImapClientInfo? = config.clientInfo().takeIf { serverSettings.isSendClientInfo }

        override var pathPrefix: String?
            get() = this@RealImapStore.pathPrefix
            set(value) {
                this@RealImapStore.pathPrefix = value
            }

        override var pathDelimiter: String?
            get() = this@RealImapStore.pathDelimiter
            set(value) {
                this@RealImapStore.pathDelimiter = value
            }

        override fun setCombinedPrefix(prefix: String?) {
            combinedPrefix = prefix
        }
    }
}

private val ImapConnection.supportsListExtended: Boolean
    get() = hasCapability(Capabilities.SPECIAL_USE) && hasCapability(Capabilities.LIST_EXTENDED)
