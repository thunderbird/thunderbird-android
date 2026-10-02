package net.thunderbird.app.common.feature.mail.message.mapper

import android.content.Context
import android.util.Base64
import com.eygraber.uri.toAndroidUri
import com.fsck.k9.mail.Body
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.internet.Headers
import com.fsck.k9.mail.internet.MimeBodyPart
import com.fsck.k9.mail.internet.MimeHeader
import com.fsck.k9.mail.internet.MimeUtility
import com.fsck.k9.mailstore.BinaryMemoryBody
import com.fsck.k9.mailstore.FileBackedBody
import com.fsck.k9.mailstore.LocalPart
import java.io.File
import java.io.IOException
import net.thunderbird.app.common.feature.mail.message.mapper.DefaultMessageDataMapper.Companion.LOG_ID
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.message.MessageAttachment
import net.thunderbird.feature.mail.message.MimeType
import net.thunderbird.feature.mail.message.orDefault
import okio.ByteString.Companion.toByteString
import okio.HashingSource
import okio.Source
import okio.blackholeSink
import okio.buffer
import okio.sink
import okio.source

/**
 * Resolves attachment metadata and content when converting between legacy [Part]s and
 * [MessageAttachment]s.
 *
 * @param logger The logger used to report attachment resolution issues.
 * @param context The context providing the cache directory.
 * @param hashingSourceFactory Creates the [HashingSource] used to hash attachment content while it's
 * copied. Defaults to SHA-256.
 */
internal class AttachmentResolver(
    private val logger: Logger,
    private val context: Context,
    private val hashingSourceFactory: (Source) -> HashingSource = { source -> HashingSource.sha256(source) },
) {
    /**
     * Returns the attachment file name taken from the `Content-Disposition` `filename` parameter,
     * falling back to the `Content-Type` `name` parameter, or `null` when neither is present.
     */
    fun Part.resolveAttachmentName(): String? =
        MimeUtility.getHeaderParameter(disposition, "filename")
            ?: MimeUtility.getHeaderParameter(contentType, "name")

    /**
     * Determines the attachment size in bytes, preferring the length of [copiedFile], then the size
     * persisted for a [LocalPart], then the `Content-Disposition` `size` parameter, defaulting to `0`.
     *
     * @param copiedFile The file holding the attachment content, or `null` when unavailable.
     */
    fun Part.resolveAttachmentSize(copiedFile: File?): Long =
        copiedFile?.length()
            ?: (this as? LocalPart)?.size
            ?: MimeUtility.getHeaderParameter(disposition, "size")?.toLong()
            ?: 0L

    /**
     * Returns `true` when the part is an inline image, meaning it has an `inline` disposition, a
     * content ID and an `image/` MIME type.
     */
    fun Part.isInline(): Boolean = disposition?.let { MimeUtility.getHeaderParameter(it, null) }
        ?.equals("inline", ignoreCase = true) == true &&
        contentId != null &&
        mimeType?.startsWith(prefix = "image/", ignoreCase = true) == true

    /**
     * Converts this attachment into a base64 encoded [MimeBodyPart].
     *
     * @param context The context used to read the attachment content from [internalUri].
     */
    fun MessageAttachment.toBodyPart(context: Context): MimeBodyPart {
        val resolvedMimeType = mimeType?.value ?: MimeType.AttachmentDefault.value
        val resolvedName = displayName.orDefault().value

        val body = internalUri?.let { uri ->
            val bytes = context.contentResolver.openInputStream(uri.toAndroidUri())
                ?.use { it.readBytes() }
                ?: byteArrayOf()
            BinaryMemoryBody(Base64.encode(bytes, Base64.DEFAULT), "base64")
        }

        val bodyPart = MimeBodyPart.create(body, Headers.contentType(resolvedMimeType, resolvedName))
        bodyPart.addHeader(
            MimeHeader.HEADER_CONTENT_DISPOSITION,
            Headers.contentDisposition(toDisposition(), resolvedName, size.takeIf { it >= 0 }),
        )
        if (this is MessageAttachment.Inline) {
            contentId?.let { bodyPart.addHeader(MimeHeader.HEADER_CONTENT_ID, it.value) }
        }
        return bodyPart
    }

    /**
     * Returns a cache file holding the part's decoded content, copying it only when no file exists for
     * the current content.
     *
     * The file name is derived from hashes of the owner (account, message and, for a [LocalPart], part ID)
     * and of the content version, never from untrusted header values. For a file-backed [LocalPart], the
     * version comes from the backing file's metadata and stale versions are deleted; otherwise it's a hash
     * of the decoded content. Content is written to a temporary file and atomically renamed.
     *
     * @param messageId The local database ID of the message the part belongs to, if known.
     * @param messageServerId The server ID of the message the part belongs to, if known.
     * @param messageHeaderId The `Message-ID` header value of the message the part belongs to, if known.
     * @param accountId The ID of the account the part belongs to.
     * @return The cached file, or `null` when the part's content hasn't been downloaded yet.
     * @throws IOException if the content can't be copied into the cache directory.
     */
    fun Part.resolveAttachmentFile(
        messageId: Long?,
        messageServerId: String?,
        messageHeaderId: String?,
        accountId: AccountId,
    ): File? {
        val partBody = body ?: return null.also {
            logger.warn {
                "$LOG_ID attachment part has no body (content not downloaded yet). " +
                    "internalUri will be null. part = $this"
            }
        }
        val localPart = this as? LocalPart
        val partIdKey = localPart?.let { ":${it.partId}" }.orEmpty()
        val ownerKey = "${accountId.value}:${messageId ?: messageServerId ?: messageHeaderId}$partIdKey"
        val version = if (localPart != null && partBody is FileBackedBody) {
            // The backing file is rewritten whenever the content changes, so its metadata identifies the
            // content without reading it.
            val file = partBody.file
            "${file.length()}:${file.lastModified()}:${partBody.encoding}".sha256Hex()
        } else {
            partBody.contentHash()
        }
        val target = cacheFile(ownerKey, version)
        if (!target.exists()) {
            copyToCache(partBody, target)
            if (localPart != null) {
                deleteStaleVersions(ownerKey, keep = target)
            }
        }
        return target
    }

    /**
     * Returns the hex encoded hash of the decoded content of this body, without writing it anywhere.
     */
    private fun Body.contentHash(): String {
        val input = MimeUtility.decodeBody(this)
        try {
            // The source isn't closed on purpose since closing it would close input and delete its temporary files.
            val hashingSource = hashingSourceFactory(input.source())
            hashingSource.buffer().readAll(blackholeSink())
            return hashingSource.hash.hex()
        } finally {
            MimeUtility.closeInputStreamWithoutDeletingTemporaryFiles(input)
        }
    }

    /**
     * Copies the decoded [partBody] into [target].
     */
    private fun copyToCache(partBody: Body, target: File) {
        val tmp = File.createTempFile(CACHE_FILE_PREFIX, ".partial", context.cacheDir)
        var moved = false
        try {
            val input = MimeUtility.decodeBody(partBody)
            try {
                // The source isn't closed on purpose since closing it would close input and delete its temporary files.
                tmp.sink().buffer().use { sink -> sink.writeAll(input.source()) }
            } finally {
                MimeUtility.closeInputStreamWithoutDeletingTemporaryFiles(input)
            }
            if (!tmp.renameTo(target)) {
                throw IOException("Failed to move attachment content into the cache directory")
            }
            moved = true
        } finally {
            if (!moved) {
                tmp.delete()
            }
        }
    }

    private fun deleteStaleVersions(ownerKey: String, keep: File) {
        val prefix = cacheFilePrefix(ownerKey)
        context.cacheDir
            .listFiles { file -> file.name.startsWith(prefix) && file != keep }
            ?.forEach { it.delete() }
    }

    private fun cacheFile(ownerKey: String, version: String): File =
        File(context.cacheDir, "${cacheFilePrefix(ownerKey)}$version")

    private fun cacheFilePrefix(ownerKey: String): String = "${CACHE_FILE_PREFIX}_${ownerKey.sha256Hex()}_"

    private fun String.sha256Hex(): String = toByteArray().toByteString().sha256().hex()

    private companion object {
        const val CACHE_FILE_PREFIX = "attachment"
    }
}
