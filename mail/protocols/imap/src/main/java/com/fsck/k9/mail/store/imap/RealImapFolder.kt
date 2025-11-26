package com.fsck.k9.mail.store.imap

import com.fsck.k9.mail.Body
import com.fsck.k9.mail.BodyFactory
import com.fsck.k9.mail.FetchProfile
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.K9MailLib
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessageRetrievalListener
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.filter.EOLConvertingOutputStream
import com.fsck.k9.mail.internet.MimeBodyPart
import com.fsck.k9.mail.internet.MimeHeader
import com.fsck.k9.mail.internet.MimeMessageHelper
import com.fsck.k9.mail.internet.MimeMultipart
import com.fsck.k9.mail.internet.MimeParameterEncoder.isToken
import com.fsck.k9.mail.internet.MimeParameterEncoder.quotedUtf8
import com.fsck.k9.mail.internet.MimeUtility
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.protocols.imap.folder.attributeName

internal class RealImapFolder(
    private val internalImapStore: InternalImapStore,
    private val connectionManager: ImapConnectionManager,
    override val serverId: String,
    private val folderNameCodec: FolderNameCodec,
) : ImapFolder {
    private var uidNext = -1L
    internal var connection: ImapConnection? = null
        private set
    private var exists = false
    private var inSearch = false
    private var canCreateKeywords = false
    private var uidValidity: Long? = null

    /**
     * Specifies whether the folder was opened in read-only or read-write mode based on the tagged OK response to
     * the SELECT or EXAMINE command (READ-ONLY or READ-WRITE).
     *
     * Most of the time this will match the [mode] value. But it's possible for the SELECT command to open a folder in
     * read-only mode. See RFC 3501, section 6.3.1.
     *
     * Note: Currently unused.
     */
    private var serverOpenMode: OpenMode? = null

    override var messageCount = -1
        private set

    /**
     * The [OpenMode] value that was passed when this folder was [opened][open].
     */
    override var mode: OpenMode? = null
        private set

    override val isOpen: Boolean
        get() = connection != null

    override fun getUidValidity(): Long? {
        check(isOpen) { "ImapFolder needs to be open" }
        return uidValidity
    }

    @get:Throws(MessagingException::class)
    private val prefixedName: String
        get() {
            val imapPrefix = if (!INBOX.equals(serverId, ignoreCase = true)) {
                val connection = synchronized(this) {
                    this.connection ?: connectionManager.getConnection()
                }

                try {
                    connection.open()
                } catch (ioe: IOException) {
                    throw MessagingException("Unable to get IMAP prefix", ioe)
                } finally {
                    if (this.connection == null) {
                        connectionManager.releaseConnection(connection)
                    }
                }
                internalImapStore.combinedPrefix.orEmpty()
            } else {
                ""
            }
            val normalizedServerId = serverId.removePrefix(imapPrefix)

            return "$imapPrefix$normalizedServerId"
        }

    @get:Throws(MessagingException::class)
    private val encodedName: String
        get() {
            return folderNameCodec.encode(prefixedName)
        }

    @Throws(MessagingException::class, IOException::class)
    private fun executeSimpleCommand(command: String): List<ImapResponse> {
        return handleUntaggedResponses(connection!!.executeSimpleCommand(command))
    }

    /**
     * Opens the folder in either read-only or read-write mode.
     *
     * When [OpenMode.READ_ONLY] is passed the EXAMINE command is used to open the folder.
     * When [OpenMode.READ_WRITE] is passed the SELECT command is used to open the folder.
     */
    @Throws(MessagingException::class)
    override fun open(mode: OpenMode) {
        internalOpen(mode)

        if (messageCount == -1) {
            throw MessagingException("Did not find message count during open")
        }
    }

    @Throws(MessagingException::class)
    private fun internalOpen(mode: OpenMode): List<ImapResponse> {
        if (isOpen && this.mode == mode) {
            // Make sure the connection is valid. If it's not we'll close it down and continue on to get a new one.
            try {
                return executeSimpleCommand(Commands.NOOP)
            } catch (ioe: IOException) {
                /* don't throw */
                ioExceptionHandler(connection, ioe)
            }
        }

        connectionManager.releaseConnection(connection)

        synchronized(this) {
            connection = connectionManager.getConnection()
        }

        try {
            val openCommand = if (mode == OpenMode.READ_WRITE) "SELECT" else "EXAMINE"
            val escapedFolderName = ImapUtility.encodeString(encodedName)
            val command = String.format("%s %s", openCommand, escapedFolderName)
            val responses = executeSimpleCommand(command)

            this.mode = mode

            for (response in responses) {
                extractUidValidity(response)
                handlePermanentFlags(response)
            }

            handleSelectOrExamineOkResponse(ImapUtility.getLastResponse(responses))

            exists = true

            return responses
        } catch (ioe: IOException) {
            throw ioExceptionHandler(connection, ioe)
        } catch (me: MessagingException) {
            Log.e(me, "Unable to open connection for %s", logId)
            throw me
        }
    }

    private fun extractUidValidity(response: ImapResponse) {
        val uidValidityResponse = UidValidityResponse.parse(response)
        if (uidValidityResponse != null) {
            uidValidity = uidValidityResponse.uidValidity
        }
    }

    private fun handlePermanentFlags(response: ImapResponse) {
        val permanentFlagsResponse = PermanentFlagsResponse.parse(response) ?: return

        val permanentFlags = internalImapStore.getPermanentFlagsIndex()
        permanentFlags.addAll(permanentFlagsResponse.flags)
        canCreateKeywords = permanentFlagsResponse.canCreateKeywords()
    }

    private fun handleSelectOrExamineOkResponse(response: ImapResponse) {
        val selectOrExamineResponse = SelectOrExamineResponse.parse(response) ?: return // This shouldn't happen
        if (selectOrExamineResponse.hasOpenMode()) {
            serverOpenMode = selectOrExamineResponse.openMode
        }
    }

    override fun close() {
        messageCount = -1

        if (!isOpen) {
            return
        }

        synchronized(this) {
            // If we are mid-search and we get a close request, we gotta trash the connection.
            if (inSearch && connection != null) {
                Log.i("IMAP search was aborted, shutting down connection.")
                connection!!.close()
            } else {
                connectionManager.releaseConnection(connection)
            }

            connection = null
        }
    }

    @Throws(MessagingException::class)
    override fun exists(): Boolean {
        if (exists) {
            return true
        }

        /*
         * This method needs to operate in the unselected mode as well as the selected mode
         * so we must get the connection ourselves if it's not there. We are specifically
         * not calling checkOpen() since we don't care if the folder is open.
         */
        val connection = synchronized(this) {
            this.connection ?: connectionManager.getConnection()
        }

        return try {
            val escapedFolderName = ImapUtility.encodeString(encodedName)
            connection.executeSimpleCommand(String.format("STATUS %s (UIDVALIDITY)", escapedFolderName))

            exists = true

            true
        } catch (e: NegativeImapResponseException) {
            false
        } catch (ioe: IOException) {
            throw ioExceptionHandler(connection, ioe)
        } finally {
            if (this.connection == null) {
                connectionManager.releaseConnection(connection)
            }
        }
    }

    @Throws(MessagingException::class)
    override fun create(folderType: FolderType): Boolean {
        /*
         * This method needs to operate in the unselected mode as well as the selected mode
         * so we must get the connection ourselves if it's not there. We are specifically
         * not calling checkOpen() since we don't care if the folder is open.
         */
        val connection = synchronized(this) {
            this.connection ?: connectionManager.getConnection()
        }

        val hasSpecialUseCapability = connection.hasCapability(Capabilities.CREATE_SPECIAL_USE)

        return try {
            val escapedFolderName = ImapUtility.encodeString(encodedName)

            // https://www.rfc-editor.org/rfc/rfc6154.html#section-5.3
            val specialUseAttribute = folderType
                .takeIf {
                    hasSpecialUseCapability &&
                        folderType != FolderType.REGULAR &&
                        folderType != FolderType.INBOX &&
                        folderType != FolderType.OUTBOX
                }
                ?.let { "(USE (${folderType.attributeName}))" }
                .orEmpty()

            // https://datatracker.ietf.org/doc/html/rfc3501#section-6.3.3
            val command = "CREATE $escapedFolderName $specialUseAttribute".trim()
            val responses = connection.executeSimpleCommand(command)
            responses.any { ImapResponseParser.equalsIgnoreCase(it[0], Responses.OK) }
        } catch (e: NegativeImapResponseException) {
            Log.e(e, "Unable to create folder %s for %s", serverId, logId)
            false
        } catch (ioe: IOException) {
            throw ioExceptionHandler(this.connection, ioe)
        } finally {
            if (this.connection == null) {
                connectionManager.releaseConnection(connection)
            }
        }
    }

    /**
     * Copies the given messages to the specified folder.
     *
     * **Note:**
     * Only the UIDs of the given [ImapMessage] instances are used. It is assumed that all
     * UIDs represent valid messages in this folder.
     *
     * @param messages The messages to copy to the specified folder.
     * @param folder The name of the target folder.
     *
     * @return The mapping of original message UIDs to the new server UIDs.
     */
    @Throws(MessagingException::class)
    override fun copyMessages(messages: List<ImapMessage>, folder: ImapFolder): Map<String, String>? {
        require(folder is RealImapFolder) { "'folder' needs to be a RealImapFolder instance" }

        if (messages.isEmpty()) {
            return null
        }

        checkOpen() // only need READ access

        val uids = messages.map { it.uid.toLong() }.toSet()
        val encodedDestinationFolderName =
            folderNameCodec.encode(folder.prefixedName)
        val escapedDestinationFolderName = ImapUtility.encodeString(encodedDestinationFolderName)

        return try {
            val imapResponses = connection!!.executeCommandWithIdSet(
                Commands.UID_COPY,
                escapedDestinationFolderName,
                uids,
            )

            UidCopyResponse.parse(imapResponses)?.uidMapping
        } catch (ioe: IOException) {
            throw ioExceptionHandler(connection, ioe)
        }
    }

    @Throws(MessagingException::class)
    override fun moveMessages(messages: List<ImapMessage>, folder: ImapFolder): Map<String, String>? {
        if (messages.isEmpty()) {
            return null
        }

        checkOpenWithWriteAccess()

        return if (connection.hasCapability(Capabilities.MOVE)) {
            moveMessagesUsingMoveExtension(messages, folder)
        } else {
            moveMessagesUsingCopyAndDelete(messages, folder)
        }
    }

    private fun moveMessagesUsingMoveExtension(messages: List<ImapMessage>, folder: ImapFolder): Map<String, String>? {
        require(folder is RealImapFolder) { "'folder' needs to be a RealImapFolder instance" }

        val uids = messages.map { it.uid.toLong() }.toSet()
        val encodedDestinationFolderName =
            folderNameCodec.encode(folder.prefixedName)
        val escapedDestinationFolderName = ImapUtility.encodeString(encodedDestinationFolderName)

        return try {
            val imapResponses = connection!!.executeCommandWithIdSet(
                Commands.UID_MOVE,
                escapedDestinationFolderName,
                uids,
            )

            UidCopyResponse.parse(imapResponses, allowUntaggedResponse = true)?.uidMapping
        } catch (ioe: IOException) {
            throw ioExceptionHandler(connection, ioe)
        }
    }

    private fun moveMessagesUsingCopyAndDelete(messages: List<ImapMessage>, folder: ImapFolder): Map<String, String>? {
        val uids = messages.map { it.uid }

        val uidMapping = copyMessages(messages, folder)
        setFlags(messages, setOf(Flag.DELETED), true)
        expungeUidsAfterDelete(uids)

        return uidMapping
    }

    @Throws(MessagingException::class)
    private fun getRemoteMessageCount(criteria: String): Int {
        checkOpen()

        try {
            val command = String.format(Locale.US, "SEARCH 1:* %s", criteria)
            val responses = executeSimpleCommand(command)

            return responses.sumOf { response ->
                if (ImapResponseParser.equalsIgnoreCase(response[0], "SEARCH")) {
                    response.size - 1
                } else {
                    0
                }
            }
        } catch (ioe: IOException) {
            throw ioExceptionHandler(connection, ioe)
        }
    }

    @get:Throws(MessagingException::class)
    val unreadMessageCount: Int
        get() = getRemoteMessageCount("UNSEEN NOT DELETED")

    @get:Throws(MessagingException::class)
    val flaggedMessageCount: Int
        get() = getRemoteMessageCount("FLAGGED NOT DELETED")

    @get:Throws(MessagingException::class)
    internal val highestUid: Long
        get() = try {
            val responses = executeSimpleCommand("UID SEARCH *:*")
            val searchResponse = SearchResponse.parse(responses)

            extractHighestUid(searchResponse)
        } catch (e: NegativeImapResponseException) {
            -1L
        } catch (ioe: IOException) {
            throw ioExceptionHandler(connection, ioe)
        }

    private fun extractHighestUid(searchResponse: SearchResponse): Long {
        return searchResponse.numbers.maxOrNull() ?: -1L
    }

    override fun getMessage(uid: String): ImapMessage {
        return ImapMessage(uid)
    }

    @Throws(MessagingException::class)
    override fun getMessages(
        start: Int,
        end: Int,
        earliestDate: Date?,
        listener: MessageRetrievalListener<ImapMessage>?,
    ): List<ImapMessage> {
        return getMessages(start, end, earliestDate, false, listener)
    }

    @Throws(MessagingException::class)
    private fun getMessages(
        start: Int,
        end: Int,
        earliestDate: Date?,
        includeDeleted: Boolean,
        listener: MessageRetrievalListener<ImapMessage>?,
    ): List<ImapMessage> {
        if (start < 1 || end < 1 || end < start) {
            throw MessagingException(String.format(Locale.US, "Invalid message set %d %d", start, end))
        }

        checkOpen()

        val dateSearchString = getDateSearchString(earliestDate)
        val command = String.format(
            Locale.US,
            "UID SEARCH %d:%d%s%s",
            start,
            end,
            dateSearchString,
            if (includeDeleted) "" else " NOT DELETED",
        )

        try {
            val imapResponses = connection!!.executeSimpleCommand(command)
            val searchResponse = SearchResponse.parse(imapResponses)
            return getMessages(searchResponse, listener)
        } catch (ioe: IOException) {
            throw ioExceptionHandler(connection, ioe)
        }
    }

    private fun getDateSearchString(earliestDate: Date?): String {
        return if (earliestDate == null) {
            ""
        } else {
            " SINCE " + RFC3501_DATE.get()!!.format(earliestDate)
        }
    }

    @Throws(IOException::class, MessagingException::class)
    override fun areMoreMessagesAvailable(indexOfOldestMessage: Int, earliestDate: Date?): Boolean {
        checkOpen()

        if (indexOfOldestMessage == 1) {
            return false
        }

        var endIndex = indexOfOldestMessage - 1
        val dateSearchString = getDateSearchString(earliestDate)

        while (endIndex > 0) {
            val startIndex = max(0, endIndex - MORE_MESSAGES_WINDOW_SIZE) + 1
            if (existsNonDeletedMessageInRange(startIndex, endIndex, dateSearchString)) {
                return true
            }
            endIndex -= MORE_MESSAGES_WINDOW_SIZE
        }

        return false
    }

    @Throws(MessagingException::class, IOException::class)
    private fun existsNonDeletedMessageInRange(startIndex: Int, endIndex: Int, dateSearchString: String): Boolean {
        val command = String.format(
            Locale.US,
            "SEARCH %d:%d%s NOT DELETED",
            startIndex,
            endIndex,
            dateSearchString,
        )
        val imapResponses = executeSimpleCommand(command)

        val response = SearchResponse.parse(imapResponses)
        return response.numbers.size > 0
    }

    @Throws(MessagingException::class)
    internal fun getMessages(
        mesgSeqs: Set<Long>,
        includeDeleted: Boolean,
        listener: MessageRetrievalListener<ImapMessage>?,
    ): List<ImapMessage> {
        checkOpen()

        try {
            val commandSuffix = if (includeDeleted) "" else " NOT DELETED"
            val imapResponses = connection!!.executeCommandWithIdSet(Commands.UID_SEARCH, commandSuffix, mesgSeqs)

            val searchResponse = SearchResponse.parse(imapResponses)
            return getMessages(searchResponse, listener)
        } catch (ioe: IOException) {
            throw ioExceptionHandler(connection, ioe)
        }
    }

    @Throws(MessagingException::class)
    internal fun getMessagesFromUids(mesgUids: List<String>): List<ImapMessage> {
        checkOpen()

        val uidSet = mesgUids.map { it.toLong() }.toSet()

        try {
            val imapResponses = connection!!.executeCommandWithIdSet("UID SEARCH UID", "", uidSet)

            val searchResponse = SearchResponse.parse(imapResponses)
            return getMessages(searchResponse, null)
        } catch (ioe: IOException) {
            throw ioExceptionHandler(connection, ioe)
        }
    }

    private fun getMessages(
        searchResponse: SearchResponse,
        listener: MessageRetrievalListener<ImapMessage>?,
    ): List<ImapMessage> {
        // Sort the uids in numerically decreasing order
        // By doing it in decreasing order, we ensure newest messages are dealt with first
        // This makes the most sense when a limit is imposed, and also prevents UI from going
        // crazy adding stuff at the top.
        val uids = searchResponse.numbers.sortedDescending()

        return uids.map { uidLong ->
            val uid = uidLong.toString()
            val message = ImapMessage(uid)
            listener?.messageFinished(message)

            message
        }
    }

    @Throws(MessagingException::class)
    override fun fetch(
        messages: List<ImapMessage>,
        fetchProfile: FetchProfile,
        listener: FetchListener?,
        maxDownloadSize: Int,
    ) {
        if (messages.isEmpty()) {
            return
        }

        checkOpen()

        val messageMap = messages.associateBy { it.uid }
        val uids = messages.map { it.uid }

        val fetchFields: MutableSet<String> = LinkedHashSet()
        fetchFields.add("UID")
        if (fetchProfile.contains(FetchProfile.Item.FLAGS)) {
            fetchFields.add("FLAGS")
        }

        if (fetchProfile.contains(FetchProfile.Item.ENVELOPE)) {
            fetchFields.add("INTERNALDATE")
            fetchFields.add("RFC822.SIZE")
            fetchFields.add(
                "BODY.PEEK[HEADER.FIELDS (date subject from content-type to cc bcc " +
                    "reply-to message-id references in-reply-to list-post list-unsubscribe sender " +
                    K9MailLib.IDENTITY_HEADER + " " + K9MailLib.CHAT_HEADER + ")]",
            )
        }

        if (fetchProfile.contains(FetchProfile.Item.STRUCTURE)) {
            fetchFields.add("BODYSTRUCTURE")
        }

        if (fetchProfile.contains(FetchProfile.Item.BODY_SANE)) {
            if (maxDownloadSize > 0) {
                fetchFields.add(String.format(Locale.US, "BODY.PEEK[]<0.%d>", maxDownloadSize))
            } else {
                fetchFields.add("BODY.PEEK[]")
            }
        }

        if (fetchProfile.contains(FetchProfile.Item.BODY)) {
            fetchFields.add("BODY.PEEK[]")
        }

        val spaceSeparatedFetchFields = ImapUtility.join(" ", fetchFields)
        var windowStart = 0
        val processedUids = mutableSetOf<String>()
        while (windowStart < messages.size) {
            val windowEnd = min(windowStart + FETCH_WINDOW_SIZE, messages.size)
            val uidWindow = uids.subList(windowStart, windowEnd)

            try {
                val commaSeparatedUids = ImapUtility.join(",", uidWindow)
                val command = String.format("UID FETCH %s (%s)", commaSeparatedUids, spaceSeparatedFetchFields)
                connection!!.sendCommand(command, false)

                var callback: ImapResponseCallback? = null
                if (fetchProfile.contains(FetchProfile.Item.BODY) ||
                    fetchProfile.contains(FetchProfile.Item.BODY_SANE)
                ) {
                    callback = FetchBodyCallback(messageMap)
                }

                var response: ImapResponse
                do {
                    response = connection!!.readResponse(callback)
                    if (response.tag == null && ImapResponseParser.equalsIgnoreCase(response[1], "FETCH")) {
                        val fetchList = response.getKeyedValue("FETCH") as ImapList
                        val uid = fetchList.getKeyedString("UID")

                        val message = messageMap[uid]
                        if (message == null) {
                            if (K9MailLib.isDebug()) {
                                Log.d("Do not have message in messageMap for UID %s for %s", uid, logId)
                            }
                            handleUntaggedResponse(response)
                            continue
                        }

                        val literal = handleFetchResponse(message, fetchList)
                        if (literal != null) {
                            when (literal) {
                                is String -> {
                                    val bodyStream: InputStream = literal.toByteArray().inputStream()
                                    message.parse(bodyStream)
                                }

                                is Int -> {
                                    // All the work was done in FetchBodyCallback.foundLiteral()
                                }

                                else -> {
                                    // This shouldn't happen
                                    throw MessagingException("Got FETCH response with bogus parameters")
                                }
                            }
                        }

                        val isFirstResponse = uid !in processedUids
                        processedUids.add(uid)

                        listener?.onFetchResponse(message, isFirstResponse)
                    } else {
                        handleUntaggedResponse(response)
                    }
                } while (response.tag == null)
            } catch (ioe: IOException) {
                throw ioExceptionHandler(connection, ioe)
            }

            windowStart += FETCH_WINDOW_SIZE
        }
    }

    @Throws(MessagingException::class)
    override fun fetchPart(
        message: ImapMessage,
        part: Part,
        bodyFactory: BodyFactory,
        maxDownloadSize: Int,
    ) {
        checkOpen()

        val partId = part.serverExtra

        val fetch = if ("TEXT".equals(partId, ignoreCase = true)) {
            String.format(Locale.US, "BODY.PEEK[TEXT]<0.%d>", maxDownloadSize)
        } else {
            String.format("BODY.PEEK[%s]", partId)
        }

        try {
            val command = String.format("UID FETCH %s (UID %s)", message.uid, fetch)
            connection!!.sendCommand(command, false)

            val callback: ImapResponseCallback = FetchPartCallback(part, bodyFactory)

            var response: ImapResponse
            do {
                response = connection!!.readResponse(callback)

                if (response.tag == null && ImapResponseParser.equalsIgnoreCase(response[1], "FETCH")) {
                    val fetchList = response.getKeyedValue("FETCH") as ImapList
                    val uid = fetchList.getKeyedString("UID")
                    if (message.uid != uid) {
                        if (K9MailLib.isDebug()) {
                            Log.d("Did not ask for UID %s for %s", uid, logId)
                        }
                        handleUntaggedResponse(response)
                        continue
                    }

                    val literal = handleFetchResponse(message, fetchList)
                    if (literal != null) {
                        when (literal) {
                            is Body -> {
                                // Most of the work was done in FetchAttachmentCallback.foundLiteral()
                                MimeMessageHelper.setBody(part, literal as Body?)
                            }

                            is String -> {
                                val bodyStream: InputStream = literal.toByteArray().inputStream()
                                val contentTransferEncoding =
                                    part.getHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING)[0]
                                val contentType = part.getHeader(MimeHeader.HEADER_CONTENT_TYPE)[0]
                                val body = bodyFactory.createBody(contentTransferEncoding, contentType, bodyStream)
                                MimeMessageHelper.setBody(part, body)
                            }

                            else -> {
                                // This shouldn't happen
                                throw MessagingException("Got FETCH response with bogus parameters")
                            }
                        }
                    }
                } else {
                    handleUntaggedResponse(response)
                }
            } while (response.tag == null)
        } catch (ioe: IOException) {
            throw ioExceptionHandler(connection, ioe)
        }
    }

    // Returns value of body field
    @Throws(MessagingException::class)
    private fun handleFetchResponse(message: ImapMessage, fetchList: ImapList): Any? {
        var result: Any? = null
        if (fetchList.containsKey("FLAGS")) {
            val flags = fetchList.getKeyedList("FLAGS")
            if (flags != null) {
                for (i in flags.indices) {
                    val flag = flags.getString(i)
                    when {
                        flag.equals("\\Deleted", ignoreCase = true) -> {
                            message.setFlag(Flag.DELETED, true)
                        }

                        flag.equals("\\Answered", ignoreCase = true) -> {
                            message.setFlag(Flag.ANSWERED, true)
                        }

                        flag.equals("\\Seen", ignoreCase = true) -> {
                            message.setFlag(Flag.SEEN, true)
                        }

                        flag.equals("\\Flagged", ignoreCase = true) -> {
                            message.setFlag(Flag.FLAGGED, true)
                        }

                        flag.equals("\$Forwarded", ignoreCase = true) -> {
                            message.setFlag(Flag.FORWARDED, true)
                            // a message contains FORWARDED FLAG -> so we can also create them
                            internalImapStore.getPermanentFlagsIndex().add(Flag.FORWARDED)
                        }

                        flag.equals("\\Draft", ignoreCase = true) -> {
                            message.setFlag(Flag.DRAFT, true)
                        }
                    }
                }
            }
        }

        if (fetchList.containsKey("INTERNALDATE")) {
            message.internalDate = fetchList.getKeyedDate("INTERNALDATE")
        }

        if (fetchList.containsKey("RFC822.SIZE")) {
            val size = fetchList.getKeyedNumber("RFC822.SIZE")
            message.setSize(size)
        }

        if (fetchList.containsKey("BODYSTRUCTURE")) {
            val bs = fetchList.getKeyedList("BODYSTRUCTURE")
            if (bs != null) {
                try {
                    parseBodyStructure(bs, message, "TEXT")
                } catch (e: MessagingException) {
                    if (K9MailLib.isDebug()) {
                        Log.d(e, "Error handling message for %s", logId)
                    }
                    message.body = null
                }
            }
        }

        if (fetchList.containsKey("BODY")) {
            val index = fetchList.getKeyIndex("BODY") + 2
            val size = fetchList.size
            if (index < size) {
                result = fetchList.getObject(index)

                // Check if there's an origin octet
                if (result is String) {
                    if (result.startsWith("<") && index + 1 < size) {
                        result = fetchList.getObject(index + 1)
                    }
                }
            }
        }

        return result
    }

    private fun handleUntaggedResponses(responses: List<ImapResponse>): List<ImapResponse> {
        for (response in responses) {
            handleUntaggedResponse(response)
        }
        return responses
    }

    private fun handlePossibleUidNext(response: ImapResponse) {
        if (ImapResponseParser.equalsIgnoreCase(response[0], "OK") && response.size > 1) {
            val bracketed = response[1] as? ImapList ?: return
            val key = bracketed.firstOrNull() as? String ?: return
            if ("UIDNEXT".equals(key, ignoreCase = true)) {
                uidNext = bracketed.getLong(1)
                if (K9MailLib.isDebug()) {
                    Log.d("Got UidNext = %s for %s", uidNext, logId)
                }
            }
        }
    }

    /**
     * Handle an untagged response that the caller doesn't care to handle themselves.
     */
    private fun handleUntaggedResponse(response: ImapResponse) {
        if (response.tag == null && response.size > 1) {
            if (ImapResponseParser.equalsIgnoreCase(response[1], "EXISTS")) {
                messageCount = response.getNumber(0)
                if (K9MailLib.isDebug()) {
                    Log.d("Got untagged EXISTS with value %d for %s", messageCount, logId)
                }
            }

            handlePossibleUidNext(response)

            if (ImapResponseParser.equalsIgnoreCase(response[1], "EXPUNGE") && messageCount > 0) {
                messageCount--
                if (K9MailLib.isDebug()) {
                    Log.d("Got untagged EXPUNGE with messageCount %d for %s", messageCount, logId)
                }
            }
        }
    }

    @Throws(MessagingException::class)
    private fun parseBodyStructure(bs: ImapList, part: Part, id: String) {
        if (bs[0] is ImapList) {
            // This is a multipart
            val mp = MimeMultipart.newInstance()
            for (i in bs.indices) {
                if (bs[i] is ImapList) {
                    // For each part in the message we're going to add a new BodyPart and parse into it.
                    val bp = MimeBodyPart()
                    val bodyPartId = (i + 1).toString()
                    if (id.equals("TEXT", ignoreCase = true)) {
                        parseBodyStructure(bs.getList(i), bp, bodyPartId)
                    } else {
                        parseBodyStructure(bs.getList(i), bp, "$id.$bodyPartId")
                    }
                    mp.addBodyPart(bp)
                } else {
                    // We've got to the end of the children of the part, so now we can find out what type it is and
                    // bail out.
                    val subType = bs.getString(i)
                    mp.setSubType(subType.lowercase())
                    break
                }
            }
            MimeMessageHelper.setBody(part, mp)
        } else {
            // This is a body. We need to add as much information as we can find out about it to the Part.

            /*
             *  0| 0  body type
             *  1| 1  body subtype
             *  2| 2  body parameter parenthesized list
             *  3| 3  body id (unused)
             *  4| 4  body description (unused)
             *  5| 5  body encoding
             *  6| 6  body size
             *  -| 7  text lines (only for type TEXT, unused)
             * Extensions (optional):
             *  7| 8  body MD5 (unused)
             *  8| 9  body disposition
             *  9|10  body language (unused)
             * 10|11  body location (unused)
             */
            val type = bs.getString(0)
            val subType = bs.getString(1)
            val mimeType = "$type/$subType".lowercase()

            var bodyParams: ImapList? = null
            if (bs[2] is ImapList) {
                bodyParams = bs.getList(2)
            }
            val encoding = bs.getString(5)
            val size = bs.getNumber(6)

            if (MimeUtility.isMessage(mimeType)) {
//                  A body type of type MESSAGE and subtype RFC822
//                  contains, immediately after the basic fields, the
//                  envelope structure, body structure, and size in
//                  text lines of the encapsulated message.
//                    [MESSAGE, RFC822, [NAME, Fwd: [#HTR-517941]:  update plans at 1am Friday - Memory allocation - displayware.eml], NIL, NIL, 7BIT, 5974, NIL, [INLINE, [FILENAME*0, Fwd: [#HTR-517941]:  update plans at 1am Friday - Memory all, FILENAME*1, ocation - displayware.eml]], NIL]
                /*
                 * This will be caught by fetch and handled appropriately.
                 */
                throw MessagingException("BODYSTRUCTURE message/rfc822 not yet supported.")
            }

            // Set the content type with as much information as we know right now.
            val contentType = StringBuilder()
            contentType.append(mimeType)

            if (bodyParams != null) {
                // If there are body params we might be able to get some more information out of them.
                for (i in bodyParams.indices step 2) {
                    val paramName = bodyParams.getString(i)
                    val paramValue = bodyParams.getString(i + 1)
                    val encodedValue = if (paramValue.isToken()) paramValue else paramValue.quotedUtf8()
                    contentType.append(String.format(";\r\n %s=%s", paramName, encodedValue))
                }
            }

            part.setHeader(MimeHeader.HEADER_CONTENT_TYPE, contentType.toString())

            // Extension items
            var bodyDisposition: ImapList? = null
            if ("text".equals(type, ignoreCase = true) && bs.size > 9 && bs[9] is ImapList) {
                bodyDisposition = bs.getList(9)
            } else if (!"text".equals(type, ignoreCase = true) && bs.size > 8 && bs[8] is ImapList) {
                bodyDisposition = bs.getList(8)
            }

            val contentDisposition = StringBuilder()
            if (bodyDisposition != null && !bodyDisposition.isEmpty()) {
                if (!"NIL".equals(bodyDisposition.getString(0), ignoreCase = true)) {
                    contentDisposition.append(bodyDisposition.getString(0).lowercase())
                }
                if (bodyDisposition.size > 1 && bodyDisposition[1] is ImapList) {
                    val bodyDispositionParams = bodyDisposition.getList(1)
                    // If there is body disposition information we can pull some more information
                    // about the attachment out.
                    for (i in bodyDispositionParams.indices step 2) {
                        val paramName = bodyDispositionParams.getString(i).lowercase()
                        val paramValue = bodyDispositionParams.getString(i + 1)
                        val encodedValue = if (paramValue.isToken()) paramValue else paramValue.quotedUtf8()
                        contentDisposition.append(String.format(";\r\n %s=%s", paramName, encodedValue))
                    }
                }
            }

            if (MimeUtility.getHeaderParameter(contentDisposition.toString(), "size") == null) {
                contentDisposition.append(String.format(Locale.US, ";\r\n size=%d", size))
            }

            // Set the content disposition containing at least the size. Attachment  handling code will use this
            // down the road.
            part.setHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, contentDisposition.toString())

            // Set the Content-Transfer-Encoding header. Attachment code will use this to parse the body.
            part.setHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING, encoding)

            if (part is ImapMessage) {
                part.setSize(size)
            }

            part.serverExtra = id
        }
    }

    /**
     * Appends the given messages to the selected folder.
     *
     * This implementation also determines the new UIDs of the given messages on the IMAP
     * server and changes the messages' UIDs to the new server UIDs.
     *
     * @param messages
     * The messages to append to the folder.
     *
     * @return The mapping of original message UIDs to the new server UIDs.
     */
    @Throws(MessagingException::class)
    override fun appendMessages(messages: List<Message>): Map<String, String>? {
        checkOpenWithWriteAccess()

        return try {
            val uidMap: MutableMap<String, String> = HashMap()
            for (message in messages) {
                val messageSize = message.calculateSize()

                val escapedFolderName = ImapUtility.encodeString(encodedName)
                val canCreateForwardedFlag = canCreateKeywords ||
                    internalImapStore.getPermanentFlagsIndex().contains(Flag.FORWARDED)

                val combinedFlags = ImapUtility.combineFlags(
                    message.flags,
                    canCreateForwardedFlag,
                )
                val command = String.format(
                    Locale.US,
                    "APPEND %s (%s) {%d}",
                    escapedFolderName,
                    combinedFlags,
                    messageSize,
                )
                connection!!.sendCommand(command, false)

                var response: ImapResponse
                do {
                    response = connection!!.readResponse()

                    handleUntaggedResponse(response)

                    if (response.isContinuationRequested) {
                        val eolOut = EOLConvertingOutputStream(connection!!.outputStream)
                        message.writeTo(eolOut)
                        eolOut.write('\r'.code)
                        eolOut.write('\n'.code)
                        eolOut.flush()
                    }
                } while (response.tag == null)

                if (response.size < 1 || !ImapResponseParser.equalsIgnoreCase(response[0], Responses.OK)) {
                    throw NegativeImapResponseException("APPEND failed", listOf(response))
                } else if (response.size > 1) {
                    /*
                     * If the server supports UIDPLUS, then along with the APPEND response it
                     * will return an APPENDUID response code, e.g.
                     *
                     * 11 OK [APPENDUID 2 238268] APPEND completed
                     *
                     * We can use the UID included in this response to update our records.
                     */
                    val appendList = response[1]
                    if (appendList is ImapList) {
                        if (appendList.size >= 3 && appendList.getString(0) == "APPENDUID") {
                            val newUid = appendList.getString(2)
                            if (newUid.isNotEmpty()) {
                                uidMap[message.uid] = newUid
                                message.uid = newUid
                                continue
                            }
                        }
                    }
                }

                // This part is executed in case the server does not support UIDPLUS or does not implement the
                // APPENDUID response code.
                val messageId = extractMessageId(message)
                val newUid = messageId?.let { getUidFromMessageId(it) }
                if (K9MailLib.isDebug()) {
                    Log.d("Got UID %s for message for %s", newUid, logId)
                }

                newUid?.let {
                    uidMap[message.uid] = newUid
                    message.uid = newUid
                }
            }

            // We need uidMap to be null if new UIDs are not available to maintain consistency with the behavior of
            // other similar methods (copyMessages, moveMessages) which return null.
            if (uidMap.isEmpty()) null else uidMap
        } catch (ioe: IOException) {
            throw ioExceptionHandler(connection, ioe)
        }
    }

    private fun extractMessageId(message: Message): String? {
        return message.getHeader("Message-ID").firstOrNull()
    }

    @Throws(MessagingException::class)
    override fun getUidFromMessageId(messageId: String): String? {
        if (K9MailLib.isDebug()) {
            Log.d("Looking for UID for message with message-id %s for %s", messageId, logId)
        }

        val command = String.format("UID SEARCH HEADER MESSAGE-ID %s", ImapUtility.encodeString(messageId))
        val imapResponses = try {
            executeSimpleCommand(command)
        } catch (ioe: IOException) {
            throw ioExceptionHandler(connection, ioe)
        }

        val searchResponse = SearchResponse.parse(imapResponses)
        return searchResponse.numbers.firstOrNull()?.toString()
    }

    @Throws(MessagingException::class)
    override fun deleteMessages(messages: List<ImapMessage>) {
        checkOpenWithWriteAccess()

        setFlags(messages, setOf(Flag.DELETED), true)

        if (internalImapStore.config.isExpungeImmediately()) {
            val uids = messages.map { it.uid }
            expungeUids(uids)
        }
    }

    @Throws(MessagingException::class)
    override fun deleteAllMessages() {
        checkOpenWithWriteAccess()

        setFlagsForAllMessages(setOf(Flag.DELETED), true)

        if (internalImapStore.config.isExpungeImmediately()) {
            expunge()
        }
    }

    @Throws(MessagingException::class)
    override fun expunge() {
        checkOpenWithWriteAccess()

        try {
            executeSimpleCommand("EXPUNGE")
        } catch (ioe: IOException) {
            throw ioExceptionHandler(connection, ioe)
        }
    }

    override fun expungeUids(uids: List<String>) {
        checkOpenWithWriteAccess()
        expungeUids(uids, fullExpungeFallback = true)
    }

    private fun expungeUidsAfterDelete(uids: List<String>) {
        expungeUids(uids, fullExpungeFallback = internalImapStore.config.isExpungeImmediately())
    }

    private fun expungeUids(uids: List<String>, fullExpungeFallback: Boolean) {
        require(uids.isNotEmpty()) { "expungeUids() must be called with a non-empty set of UIDs" }

        try {
            if (connection!!.isUidPlusCapable) {
                val longUids = uids.map { it.toLong() }.toSet()
                connection!!.executeCommandWithIdSet(Commands.UID_EXPUNGE, "", longUids)
            } else if (fullExpungeFallback) {
                executeSimpleCommand("EXPUNGE")
            } else {
                Log.v("Server doesn't support expunging individual messages: %s", uids)
            }
        } catch (ioe: IOException) {
            throw ioExceptionHandler(connection, ioe)
        }
    }

    @Throws(MessagingException::class)
    override fun setFlagsForAllMessages(flags: Set<Flag>, value: Boolean) {
        checkOpenWithWriteAccess()

        val canCreateForwardedFlag = canCreateKeywords ||
            internalImapStore.getPermanentFlagsIndex().contains(Flag.FORWARDED)

        try {
            val combinedFlags = ImapUtility.combineFlags(flags, canCreateForwardedFlag)
            val command = String.format(
                "%s 1:* %sFLAGS.SILENT (%s)",
                Commands.UID_STORE,
                if (value) "+" else "-",
                combinedFlags,
            )

            executeSimpleCommand(command)
        } catch (ioe: IOException) {
            throw ioExceptionHandler(connection, ioe)
        }
    }

    @Throws(MessagingException::class)
    override fun setFlags(messages: List<ImapMessage>, flags: Set<Flag>, value: Boolean) {
        checkOpenWithWriteAccess()

        val uids = messages.map { it.uid.toLong() }.toSet()
        val canCreateForwardedFlag = canCreateKeywords ||
            internalImapStore.getPermanentFlagsIndex().contains(Flag.FORWARDED)
        val combinedFlags = ImapUtility.combineFlags(flags, canCreateForwardedFlag)
        val commandSuffix = String.format("%sFLAGS.SILENT (%s)", if (value) "+" else "-", combinedFlags)
        try {
            connection!!.executeCommandWithIdSet(Commands.UID_STORE, commandSuffix, uids)
        } catch (ioe: IOException) {
            throw ioExceptionHandler(connection, ioe)
        }
    }

    private fun checkOpen() {
        check(isOpen) { "Folder '$serverId' is not open." }
    }

    private fun checkOpenWithWriteAccess() {
        checkOpen()
        check(mode == OpenMode.READ_WRITE) { "Folder '$serverId' needs to be opened for read-write access." }
    }

    private fun ioExceptionHandler(connection: ImapConnection?, ioe: IOException): MessagingException {
        Log.e(ioe, "IOException for %s", logId)
        connection?.close()
        close()
        return MessagingException("IO Error", ioe)
    }

    override fun equals(other: Any?): Boolean {
        if (other is ImapFolder) {
            return other.serverId == serverId
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return serverId.hashCode()
    }

    private val logId: String
        get() {
            var id = internalImapStore.logLabel + ":" + serverId + "/" + Thread.currentThread().name
            if (connection != null) {
                id += "/" + connection!!.logId
            }
            return id
        }

    /**
     * Search the remote ImapFolder.
     */
    @Throws(MessagingException::class)
    override fun search(
        queryString: String?,
        requiredFlags: Set<Flag>?,
        forbiddenFlags: Set<Flag>?,
        performFullTextSearch: Boolean,
    ): List<ImapMessage> {
        try {
            checkOpen()

            inSearch = true

            val searchCommand = UidSearchCommandBuilder()
                .queryString(queryString)
                .performFullTextSearch(performFullTextSearch)
                .requiredFlags(requiredFlags)
                .forbiddenFlags(forbiddenFlags)
                .build()

            try {
                val imapResponses = executeSimpleCommand(searchCommand)
                val searchResponse = SearchResponse.parse(imapResponses)

                return getMessages(searchResponse, null)
            } catch (ioe: IOException) {
                throw ioExceptionHandler(connection, ioe)
            }
        } finally {
            inSearch = false
        }
    }

    companion object {
        private const val MORE_MESSAGES_WINDOW_SIZE = 500
        private const val FETCH_WINDOW_SIZE = 100

        const val INBOX = "INBOX"

        private val RFC3501_DATE: ThreadLocal<SimpleDateFormat> = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue(): SimpleDateFormat {
                return SimpleDateFormat("dd-MMM-yyyy", Locale.US)
            }
        }
    }
}

private fun ImapConnection?.hasCapability(capability: String): Boolean {
    return this?.hasCapability(capability) == true
}

enum class OpenMode {
    READ_WRITE,
    READ_ONLY,
}
