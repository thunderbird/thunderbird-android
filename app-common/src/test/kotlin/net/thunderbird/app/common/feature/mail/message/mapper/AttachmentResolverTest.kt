package net.thunderbird.app.common.feature.mail.message.mapper

import android.net.Uri
import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.matches
import assertk.assertions.prop
import assertk.assertions.single
import assertk.assertions.startsWith
import com.eygraber.uri.toKmpUri
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.internet.BinaryTempFileBody
import com.fsck.k9.mail.internet.MimeBodyPart
import com.fsck.k9.mail.internet.MimeHeader
import com.fsck.k9.mail.internet.MimeMessageHelper
import com.fsck.k9.mail.internet.MimeUtility
import com.fsck.k9.mailstore.BinaryMemoryBody
import com.fsck.k9.mailstore.FileBackedBody
import com.fsck.k9.mailstore.LocalBodyPart
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import net.thunderbird.core.android.testing.RobolectricTest
import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.message.MessageAttachment
import net.thunderbird.feature.mail.message.MessageAttachmentContentId
import net.thunderbird.feature.mail.message.MessageAttachmentDisplayName
import net.thunderbird.feature.mail.message.MimeType
import net.thunderbird.legacy.logging.Log
import okio.HashingSource
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.robolectric.RuntimeEnvironment

class AttachmentResolverTest : RobolectricTest() {

    private val testLogger = TestLogger()
    private val context = RuntimeEnvironment.getApplication()
    private val testSubject = AttachmentResolver(testLogger, context)

    @JvmField
    @Rule
    val folder = TemporaryFolder()

    @BeforeTest
    fun setUp() {
        Log.logger = TestLogger()
        BinaryTempFileBody.setTempDirectory(folder.root)
    }

    @Test
    fun `resolveAttachmentName should return Content-Disposition filename when present`() {
        // Arrange
        val part = MimeBodyPart.create(null, "application/pdf; name=type-name.pdf").apply {
            setHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, "attachment; filename=disposition-name.pdf")
        }

        // Act
        val result = with(testSubject) { part.resolveAttachmentName() }

        // Assert
        assertThat(result).isEqualTo("disposition-name.pdf")
    }

    @Test
    fun `resolveAttachmentName should fall back to Content-Type name when filename is missing`() {
        // Arrange
        val part = MimeBodyPart.create(null, "application/pdf; name=type-name.pdf").apply {
            setHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, "attachment")
        }

        // Act
        val result = with(testSubject) { part.resolveAttachmentName() }

        // Assert
        assertThat(result).isEqualTo("type-name.pdf")
    }

    @Test
    fun `resolveAttachmentName should return null when neither filename nor name is present`() {
        // Arrange
        val part = MimeBodyPart.create(null, "application/pdf")

        // Act
        val result = with(testSubject) { part.resolveAttachmentName() }

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `resolveAttachmentFile should copy decoded content into the cache directory`() {
        // Arrange
        val body = BinaryMemoryBody("QUJD".toByteArray(Charsets.US_ASCII), "base64")
        val part = MimeBodyPart.create(body, "application/octet-stream")

        // Act
        val result = testSubject.resolveAttachmentFile(part, ACCOUNT_ID)

        // Assert
        assertNotNull(result)
        assertThat(result.parentFile).isEqualTo(context.cacheDir)
        assertThat(result.readBytes()).isEqualTo("ABC".toByteArray(Charsets.US_ASCII))
        assertThat(testLogger.events).isEmpty()
    }

    @Test
    fun `resolveAttachmentFile should not delete the temporary file backing the part body`() {
        // Arrange
        val body = BinaryTempFileBody("8bit").apply {
            outputStream.use { it.write(CONTENT) }
        }
        val part = MimeBodyPart.create(body, "application/octet-stream")

        // Act
        val result = testSubject.resolveAttachmentFile(part, ACCOUNT_ID)

        // Assert
        assertNotNull(result)
        assertThat(result.readBytes()).isEqualTo(CONTENT)
        val input = body.inputStream
        try {
            assertThat(input.readBytes()).isEqualTo(CONTENT)
        } finally {
            MimeUtility.closeInputStreamWithoutDeletingTemporaryFiles(input)
        }
    }

    @Test
    fun `resolveAttachmentFile should return null and log a warning when the part has no body`() {
        // Arrange
        val part = MimeBodyPart.create(null, "application/octet-stream")

        // Act
        val result = testSubject.resolveAttachmentFile(part, ACCOUNT_ID)

        // Assert
        assertThat(result).isNull()
        assertThat(testLogger.events).single().transform { it.level }.isEqualTo(LogLevel.WARN)
    }

    @Test
    fun `resolveAttachmentFile should reuse the cached file when resolving the same content twice`() {
        // Arrange
        val first = MimeBodyPart.create(BinaryMemoryBody(CONTENT, "8bit"), "application/octet-stream")
        val second = MimeBodyPart.create(BinaryMemoryBody(CONTENT, "8bit"), "application/octet-stream")

        // Act
        val firstFile = testSubject.resolveAttachmentFile(first, ACCOUNT_ID)
        val secondFile = testSubject.resolveAttachmentFile(second, ACCOUNT_ID)

        // Assert
        assertThat(secondFile).isEqualTo(firstFile)
        assertThat(cachedAttachmentFiles()).single().isEqualTo(firstFile)
    }

    @Test
    fun `resolveAttachmentFile should use different files for different content`() {
        // Arrange
        val first = MimeBodyPart.create(BinaryMemoryBody(CONTENT, "8bit"), "application/octet-stream")
        val second = MimeBodyPart.create(BinaryMemoryBody(OTHER_CONTENT, "8bit"), "application/octet-stream")

        // Act
        val firstFile = testSubject.resolveAttachmentFile(first, ACCOUNT_ID)
        val secondFile = testSubject.resolveAttachmentFile(second, ACCOUNT_ID)

        // Assert
        assertNotNull(firstFile)
        assertNotNull(secondFile)
        assertThat(secondFile).isNotEqualTo(firstFile)
        assertThat(firstFile.readBytes()).isEqualTo(CONTENT)
        assertThat(secondFile.readBytes()).isEqualTo(OTHER_CONTENT)
        assertThat(cachedAttachmentFiles().size).isEqualTo(2)
    }

    @Test
    fun `resolveAttachmentFile should use different files for the same content in different accounts`() {
        // Arrange
        val part = MimeBodyPart.create(BinaryMemoryBody(CONTENT, "8bit"), "application/octet-stream")

        // Act
        val firstFile = testSubject.resolveAttachmentFile(part, ACCOUNT_ID)
        val secondFile = testSubject.resolveAttachmentFile(part, OTHER_ACCOUNT_ID)

        // Assert
        assertThat(secondFile).isNotEqualTo(firstFile)
        assertThat(cachedAttachmentFiles().size).isEqualTo(2)
    }

    @Test
    fun `resolveAttachmentFile should replace the cached file of a LocalPart when its content changes`() {
        // Arrange
        val first = buildLocalPart(PART_ID, CONTENT)
        val second = buildLocalPart(PART_ID, OTHER_CONTENT)

        // Act
        val firstFile = testSubject.resolveAttachmentFile(first, ACCOUNT_ID)
        val secondFile = testSubject.resolveAttachmentFile(second, ACCOUNT_ID)

        // Assert
        assertNotNull(firstFile)
        assertNotNull(secondFile)
        assertThat(secondFile).isNotEqualTo(firstFile)
        assertThat(firstFile.exists()).isFalse()
        assertThat(secondFile.readBytes()).isEqualTo(OTHER_CONTENT)
        assertThat(cachedAttachmentFiles()).single().isEqualTo(secondFile)
    }

    @Test
    fun `resolveAttachmentFile should replace the cached file of a file backed LocalPart when its file changes`() {
        // Arrange
        val backingFile = folder.newFile().apply {
            writeBytes(CONTENT)
            setLastModified(FIRST_MODIFIED_TIME)
        }
        val part = buildFileBackedLocalPart(PART_ID, backingFile)
        val firstFile = requireNotNull(testSubject.resolveAttachmentFile(part, ACCOUNT_ID))
        backingFile.writeBytes(OTHER_CONTENT)
        backingFile.setLastModified(SECOND_MODIFIED_TIME)

        // Act
        val secondFile = testSubject.resolveAttachmentFile(part, ACCOUNT_ID)

        // Assert
        assertNotNull(secondFile)
        assertThat(secondFile).isNotEqualTo(firstFile)
        assertThat(firstFile.exists()).isFalse()
        assertThat(secondFile.readBytes()).isEqualTo(OTHER_CONTENT)
        assertThat(cachedAttachmentFiles()).single().isEqualTo(secondFile)
    }

    @Test
    fun `resolveAttachmentFile should not rewrite the cached file of an unchanged file backed LocalPart`() {
        // Arrange
        val backingFile = folder.newFile().apply { writeBytes(CONTENT) }
        val part = buildFileBackedLocalPart(PART_ID, backingFile)
        val cachedFile = requireNotNull(testSubject.resolveAttachmentFile(part, ACCOUNT_ID))
        cachedFile.writeBytes(OTHER_CONTENT)

        // Act
        val result = testSubject.resolveAttachmentFile(part, ACCOUNT_ID)

        // Assert
        assertNotNull(result)
        assertThat(result).isEqualTo(cachedFile)
        assertThat(result.readBytes()).isEqualTo(OTHER_CONTENT)
        assertThat(cachedAttachmentFiles()).single().isEqualTo(result)
    }

    @Test
    fun `resolveAttachmentFile should not rewrite the cached file of an unchanged non local part`() {
        // Arrange
        val part = MimeBodyPart.create(BinaryMemoryBody(CONTENT, "8bit"), "application/octet-stream")
        val cachedFile = requireNotNull(testSubject.resolveAttachmentFile(part, ACCOUNT_ID))
        cachedFile.writeBytes(OTHER_CONTENT)

        // Act
        val result = testSubject.resolveAttachmentFile(part, ACCOUNT_ID)

        // Assert
        assertNotNull(result)
        assertThat(result).isEqualTo(cachedFile)
        assertThat(result.readBytes()).isEqualTo(OTHER_CONTENT)
        assertThat(cachedAttachmentFiles()).single().isEqualTo(result)
    }

    @Test
    fun `resolveAttachmentFile should use a new file when the content of a non local part changes`() {
        // Arrange
        val body = BinaryMemoryBody(CONTENT, "8bit")
        val part = MimeBodyPart.create(body, "application/octet-stream")
        val firstFile = requireNotNull(testSubject.resolveAttachmentFile(part, ACCOUNT_ID))
        MimeMessageHelper.setBody(part, BinaryMemoryBody(OTHER_CONTENT, "8bit"))

        // Act
        val secondFile = testSubject.resolveAttachmentFile(part, ACCOUNT_ID)

        // Assert
        assertNotNull(secondFile)
        assertThat(secondFile).isNotEqualTo(firstFile)
        assertThat(firstFile.readBytes()).isEqualTo(CONTENT)
        assertThat(secondFile.readBytes()).isEqualTo(OTHER_CONTENT)
    }

    @Test
    fun `resolveAttachmentFile should use different files for LocalParts with different part ids`() {
        // Arrange
        val first = buildLocalPart(PART_ID, CONTENT)
        val second = buildLocalPart(OTHER_PART_ID, CONTENT)

        // Act
        val firstFile = testSubject.resolveAttachmentFile(first, ACCOUNT_ID)
        val secondFile = testSubject.resolveAttachmentFile(second, ACCOUNT_ID)

        // Assert
        assertThat(secondFile).isNotEqualTo(firstFile)
        assertThat(cachedAttachmentFiles().size).isEqualTo(2)
    }

    @Test
    fun `resolveAttachmentFile should not hash the content of a file backed LocalPart`() {
        // Arrange
        var hashingSourcesCreated = 0
        val testSubject = AttachmentResolver(testLogger, context) { source ->
            hashingSourcesCreated++
            HashingSource.sha256(source)
        }
        val backingFile = folder.newFile().apply { writeBytes(CONTENT) }
        val part = buildFileBackedLocalPart(PART_ID, backingFile)

        // Act
        val result = testSubject.resolveAttachmentFile(part, ACCOUNT_ID)

        // Assert
        assertNotNull(result)
        assertThat(result.readBytes()).isEqualTo(CONTENT)
        assertThat(hashingSourcesCreated).isEqualTo(0)
    }

    @Test
    fun `resolveAttachmentFile should hash the content of an in memory LocalPart`() {
        // Arrange
        var hashingSourcesCreated = 0
        val testSubject = AttachmentResolver(testLogger, context) { source ->
            hashingSourcesCreated++
            HashingSource.sha256(source)
        }
        val part = buildLocalPart(PART_ID, CONTENT)

        // Act
        val result = testSubject.resolveAttachmentFile(part, ACCOUNT_ID)

        // Assert
        assertNotNull(result)
        assertThat(result.readBytes()).isEqualTo(CONTENT)
        assertThat(hashingSourcesCreated).isEqualTo(1)
    }

    @Test
    fun `resolveAttachmentFile should hash the content of a non local part`() {
        // Arrange
        var hashingSourcesCreated = 0
        val testSubject = AttachmentResolver(testLogger, context) { source ->
            hashingSourcesCreated++
            HashingSource.sha256(source)
        }
        val part = MimeBodyPart.create(BinaryMemoryBody(CONTENT, "8bit"), "application/octet-stream")

        // Act
        val result = testSubject.resolveAttachmentFile(part, ACCOUNT_ID)

        // Assert
        assertNotNull(result)
        assertThat(result.readBytes()).isEqualTo(CONTENT)
        assertThat(hashingSourcesCreated).isEqualTo(1)
    }

    @Test
    fun `resolveAttachmentFile should not use header values in the cache file name`() {
        // Arrange
        val part = MimeBodyPart.create(BinaryMemoryBody(CONTENT, "8bit"), "application/octet-stream").apply {
            setHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, "attachment; filename=\"../../evil.bin\"")
            setHeader(MimeHeader.HEADER_CONTENT_ID, "<../../evil@example.com>")
        }

        // Act
        val result = testSubject.resolveAttachmentFile(part, ACCOUNT_ID)

        // Assert
        assertNotNull(result)
        assertThat(result.parentFile).isEqualTo(context.cacheDir)
        assertThat(result.name).matches(Regex("attachment_[0-9a-f]{64}_[0-9a-f]{64}"))
    }

    @Test
    fun `resolveAttachmentSize should prefer the copied file length`() {
        // Arrange
        val file = File.createTempFile("attachment", null, context.cacheDir).apply {
            writeBytes(ByteArray(size = 5))
        }
        val part = LocalBodyPart(ACCOUNT_UUID, null, PART_ID, 100L).apply {
            setHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, "attachment; size=200")
        }

        // Act
        val result = with(testSubject) { part.resolveAttachmentSize(file) }

        // Assert
        assertThat(result).isEqualTo(5L)
    }

    @Test
    fun `resolveAttachmentSize should use the LocalPart size when there is no copied file`() {
        // Arrange
        val part = LocalBodyPart(ACCOUNT_UUID, null, PART_ID, 100L).apply {
            setHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, "attachment; size=200")
        }

        // Act
        val result = with(testSubject) { part.resolveAttachmentSize(copiedFile = null) }

        // Assert
        assertThat(result).isEqualTo(100L)
    }

    @Test
    fun `resolveAttachmentSize should use the Content-Disposition size for non local parts`() {
        // Arrange
        val part = MimeBodyPart.create(null, "application/octet-stream").apply {
            setHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, "attachment; size=200")
        }

        // Act
        val result = with(testSubject) { part.resolveAttachmentSize(copiedFile = null) }

        // Assert
        assertThat(result).isEqualTo(200L)
    }

    @Test
    fun `resolveAttachmentSize should default to zero when no size is available`() {
        // Arrange
        val part = MimeBodyPart.create(null, "application/octet-stream")

        // Act
        val result = with(testSubject) { part.resolveAttachmentSize(copiedFile = null) }

        // Assert
        assertThat(result).isEqualTo(0L)
    }

    @Test
    fun `isInline should return true for an inline image with a content id`() {
        // Arrange
        val part = buildPart(disposition = "INLINE; filename=logo.png", contentId = "<logo@example.com>")

        // Act
        val result = with(testSubject) { part.isInline() }

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun `isInline should return false when disposition is attachment`() {
        // Arrange
        val part = buildPart(disposition = "attachment; filename=logo.png", contentId = "<logo@example.com>")

        // Act
        val result = with(testSubject) { part.isInline() }

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `isInline should return false when disposition is missing`() {
        // Arrange
        val part = buildPart(disposition = null, contentId = "<logo@example.com>")

        // Act
        val result = with(testSubject) { part.isInline() }

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `isInline should return false when content id is missing`() {
        // Arrange
        val part = buildPart(disposition = "inline", contentId = null)

        // Act
        val result = with(testSubject) { part.isInline() }

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `isInline should return false when mime type is not an image`() {
        // Arrange
        val part = buildPart(disposition = "inline", contentId = "<doc@example.com>", mimeType = "application/pdf")

        // Act
        val result = with(testSubject) { part.isInline() }

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `toBodyPart should embed base64 encoded content read from internalUri`() {
        // Arrange
        val file = File.createTempFile("attachment", null, context.cacheDir)
            .apply { writeBytes("ABC".toByteArray(Charsets.US_ASCII)) }
        val attachment = MessageAttachment.Regular(
            mimeType = MimeType("application/pdf"),
            displayName = MessageAttachmentDisplayName("document.pdf"),
            size = 3L,
            internalUri = Uri.fromFile(file).toKmpUri(),
        )

        // Act
        val result = with(testSubject) { attachment.toBodyPart(context) }

        // Assert
        assertThat(result).all {
            prop(MimeBodyPart::getMimeType).isEqualTo("application/pdf")
            headerParameter(parameterName = "name") { contentType }
                .isEqualTo("document.pdf")
            prop(MimeBodyPart::getDisposition)
                .isNotNull()
                .startsWith("attachment")
            headerParameter(parameterName = "filename") { disposition }
                .isEqualTo("document.pdf")
            headerParameter(parameterName = "size") { disposition }
                .isEqualTo("3")
            prop(MimeBodyPart::getContentId).isNull()
            transform { MimeUtility.decodeBody(requireNotNull(it.body)).use { it.readBytes() } }
                .isEqualTo("ABC".toByteArray(Charsets.US_ASCII))
        }
    }

    @Test
    fun `toBodyPart should create a part without body when internalUri is null`() {
        // Arrange
        val attachment = MessageAttachment.Regular(
            mimeType = MimeType("application/pdf"),
            displayName = MessageAttachmentDisplayName("document.pdf"),
            size = 42L,
            internalUri = null,
        )

        // Act
        val result = with(testSubject) { attachment.toBodyPart(context) }

        // Assert
        assertThat(result).all {
            prop(MimeBodyPart::getBody).isNull()
            headerParameter(parameterName = "filename") { disposition }
                .isEqualTo("document.pdf")
            headerParameter(parameterName = "size") { disposition }
                .isEqualTo("42")
        }
    }

    @Test
    fun `toBodyPart should use default mime type and name when they are missing`() {
        // Arrange
        val attachment = MessageAttachment.Regular(
            mimeType = null,
            displayName = null,
            size = 0L,
            internalUri = null,
        )

        // Act
        val result = with(testSubject) { attachment.toBodyPart(context) }

        // Assert
        assertThat(result).all {
            prop(MimeBodyPart::getMimeType).isEqualTo(MimeType.AttachmentDefault.value)
            headerParameter(parameterName = "name") { contentType }
                .isEqualTo("attachment")
            headerParameter(parameterName = "filename") { disposition }
                .isEqualTo("attachment")
        }
        assertThat(result.mimeType)
    }

    @Test
    fun `toBodyPart should omit size parameter when size is negative`() {
        // Arrange
        val attachment = MessageAttachment.Regular(
            mimeType = MimeType("application/pdf"),
            displayName = MessageAttachmentDisplayName("document.pdf"),
            size = -1L,
            internalUri = null,
        )

        // Act
        val result = with(testSubject) { attachment.toBodyPart(context) }

        // Assert
        assertThat(result)
            .headerParameter(parameterName = "size") { disposition }
            .isNull()
    }

    @Test
    fun `toBodyPart should add inline disposition and content id for inline attachments`() {
        // Arrange
        val attachment = MessageAttachment.Inline(
            mimeType = MimeType("image/png"),
            displayName = MessageAttachmentDisplayName("logo.png"),
            size = 0L,
            internalUri = null,
            contentId = MessageAttachmentContentId("logo@example.com"),
        )

        // Act
        val result = with(testSubject) { attachment.toBodyPart(context) }

        // Assert
        assertThat(result).all {
            prop(MimeBodyPart::getDisposition)
                .isNotNull()
                .startsWith("inline")
            prop(MimeBodyPart::getContentId).isEqualTo("logo@example.com")
        }
        assertThat(with(testSubject) { result.isInline() }).isTrue()
    }

    @Test
    fun `toBodyPart should not add content id for inline attachments without one`() {
        // Arrange
        val attachment = MessageAttachment.Inline(
            mimeType = MimeType("image/png"),
            displayName = MessageAttachmentDisplayName("logo.png"),
            size = 0L,
            internalUri = null,
            contentId = null,
        )

        // Act
        val result = with(testSubject) { attachment.toBodyPart(context) }

        // Assert
        assertThat(result.contentId).isNull()
    }

    private fun buildPart(
        disposition: String?,
        contentId: String?,
        mimeType: String = "image/png",
    ): MimeBodyPart = MimeBodyPart.create(null, mimeType).apply {
        disposition?.let { setHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, it) }
        contentId?.let { setHeader(MimeHeader.HEADER_CONTENT_ID, it) }
    }

    private inline fun <T> Assert<T>.headerParameter(parameterName: String, selector: T.() -> String) =
        transform { MimeUtility.getHeaderParameter(it.selector(), parameterName) }

    private fun buildLocalPart(partId: Long, content: ByteArray): LocalBodyPart =
        LocalBodyPart(ACCOUNT_UUID, null, partId, content.size.toLong()).apply {
            MimeMessageHelper.setBody(this, BinaryMemoryBody(content, "8bit"))
        }

    private fun AttachmentResolver.resolveAttachmentFile(part: Part, accountId: AccountId): File? =
        with(this) {
            part.resolveAttachmentFile(
                messageId = MESSAGE_ID,
                messageServerId = MESSAGE_SERVER_ID,
                messageHeaderId = MESSAGE_HEADER_ID,
                accountId = accountId,
            )
        }

    private fun buildFileBackedLocalPart(partId: Long, file: File): LocalBodyPart =
        LocalBodyPart(ACCOUNT_UUID, null, partId, file.length()).apply {
            MimeMessageHelper.setBody(this, FileBackedBody(file, "8bit"))
        }

    private fun cachedAttachmentFiles(): List<File> =
        context.cacheDir.listFiles { file -> file.name.startsWith("attachment") }.orEmpty().toList()

    private companion object {
        const val ACCOUNT_UUID = "00000000-0000-4000-8000-000000000000"
        const val OTHER_ACCOUNT_UUID = "00000000-0000-4000-8000-000000000001"
        const val PART_ID = 1L
        const val OTHER_PART_ID = 2L
        const val MESSAGE_ID = 10L
        const val MESSAGE_SERVER_ID = "message-server-id"
        const val MESSAGE_HEADER_ID = "<message-header-id@example.com>"
        val ACCOUNT_ID = AccountIdFactory.of(ACCOUNT_UUID)
        val OTHER_ACCOUNT_ID = AccountIdFactory.of(OTHER_ACCOUNT_UUID)
        val CONTENT = "ABC".toByteArray(Charsets.US_ASCII)
        val OTHER_CONTENT = "XYZ".toByteArray(Charsets.US_ASCII)
        const val FIRST_MODIFIED_TIME = 1_000_000L
        const val SECOND_MODIFIED_TIME = 2_000_000L
    }
}
