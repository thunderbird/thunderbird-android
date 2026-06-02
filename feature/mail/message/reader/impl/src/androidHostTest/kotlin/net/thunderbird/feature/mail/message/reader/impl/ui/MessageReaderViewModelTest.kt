package net.thunderbird.feature.mail.message.reader.impl.ui

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isInstanceOf
import com.fsck.k9.mail.Part
import kotlin.test.Test
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.test.runTest
import net.thunderbird.feature.mail.message.reader.api.domain.mapper.AttachmentViewInfoMapper
import net.thunderbird.feature.mail.message.reader.api.domain.mapper.AttachmentViewInfoMapper.AttachmentMetadata
import net.thunderbird.feature.mail.message.reader.api.ui.MessageReaderViewContract
import net.thunderbird.feature.mail.message.reader.api.ui.attachment.AttachmentId
import net.thunderbird.feature.mail.message.reader.api.ui.attachment.AttachmentUiItem

class MessageReaderViewModelTest {

    @Test
    fun `state should start with empty attachments`() = runTest {
        // Arrange
        val testSubject = createTestSubject()

        // Act
        testSubject.state.test {
            val state = awaitItem()
            // Assert
            assertThat(state.attachments).isEmpty()
        }
    }

    @Test
    fun `event UpdateAttachments should map non-inline attachments to ui items preserving order`() = runTest {
        // Arrange
        val testSubject = createTestSubject()
        val first = fakeAttachment(uri = "uri-1", filename = "first.pdf", size = 10L, mimeType = "application/pdf")
        val second = fakeAttachment(uri = "uri-2", filename = "second.txt", size = 20L, mimeType = "text/plain")

        // Act
        testSubject.event(
            MessageReaderViewContract.Event.UpdateAttachments(
                nonInlineAttachments = listOf(first, second),
                extraNonInlineAttachments = emptyList(),
            ),
        )

        // Assert
        testSubject.state.test {
            val state = awaitItem()
            assertThat(state.attachments).containsExactly(
                expectedUiItem(uri = "uri-1", filename = "first.pdf", size = 10L, mimeType = "application/pdf"),
                expectedUiItem(uri = "uri-2", filename = "second.txt", size = 20L, mimeType = "text/plain"),
            )
        }
    }

    @Test
    fun `event UpdateAttachments should filter out inline attachments`() = runTest {
        // Arrange
        val testSubject = createTestSubject()
        val regular = fakeAttachment(uri = "uri-1", filename = "regular.pdf")
        val inline = fakeAttachment(uri = "uri-2", filename = "inline.png", inline = true)

        // Act
        testSubject.event(
            MessageReaderViewContract.Event.UpdateAttachments(
                nonInlineAttachments = listOf(regular, inline),
                extraNonInlineAttachments = emptyList(),
            ),
        )

        // Assert
        testSubject.state.test {
            val state = awaitItem()
            assertThat(state.attachments).containsExactly(
                expectedUiItem(uri = "uri-1", filename = "regular.pdf"),
            )
        }
    }

    @Test
    fun `event UpdateAttachments should not map inline attachments`() = runTest {
        // Arrange
        val mapper = FakeAttachmentViewInfoMapper()
        val testSubject = MessageReaderViewModel(attachmentViewInfoMapper = mapper)
        val regular = fakeAttachment(uri = "uri-1", filename = "regular.pdf")
        val inline = fakeAttachment(uri = "uri-2", filename = "inline.png", inline = true)

        // Act
        testSubject.event(
            MessageReaderViewContract.Event.UpdateAttachments(
                nonInlineAttachments = listOf(regular, inline),
                extraNonInlineAttachments = emptyList(),
            ),
        )

        // Assert
        assertThat(mapper.mappedFilenames).containsExactly("regular.pdf")
    }

    @Test
    fun `event UpdateAttachments with only inline attachments should result in empty state`() = runTest {
        // Arrange
        val testSubject = createTestSubject()
        val attachments = listOf(
            fakeAttachment(uri = "uri-1", filename = "inline-1.png", inline = true),
            fakeAttachment(uri = "uri-2", filename = "inline-2.png", inline = true),
        )

        // Act
        testSubject.event(
            MessageReaderViewContract.Event.UpdateAttachments(
                nonInlineAttachments = attachments,
                extraNonInlineAttachments = emptyList(),
            ),
        )

        // Assert
        testSubject.state.test {
            val state = awaitItem()
            assertThat(state.attachments).isEmpty()
        }
    }

    @Test
    fun `event UpdateAttachments with empty list should result in empty state`() = runTest {
        // Arrange
        val testSubject = createTestSubject()

        // Act
        testSubject.event(
            MessageReaderViewContract.Event.UpdateAttachments(
                nonInlineAttachments = emptyList(),
                extraNonInlineAttachments = emptyList(),
            ),
        )

        // Assert
        testSubject.state.test {
            val state = awaitItem()
            assertThat(state.attachments).isEmpty()
        }
    }

    @Test
    fun `event UpdateAttachments should replace previously set attachments`() = runTest {
        // Arrange
        val testSubject = createTestSubject()
        testSubject.event(
            MessageReaderViewContract.Event.UpdateAttachments(
                nonInlineAttachments = listOf(fakeAttachment(uri = "uri-1", filename = "old.pdf")),
                extraNonInlineAttachments = emptyList(),
            ),
        )

        // Act
        testSubject.event(
            MessageReaderViewContract.Event.UpdateAttachments(
                nonInlineAttachments = listOf(fakeAttachment(uri = "uri-2", filename = "new.pdf")),
                extraNonInlineAttachments = emptyList(),
            ),
        )

        // Assert
        testSubject.state.test {
            val state = awaitItem()
            assertThat(state.attachments).containsExactly(
                expectedUiItem(uri = "uri-2", filename = "new.pdf"),
            )
        }
    }

    @Test
    fun `state attachments should be an immutable list`() = runTest {
        // Arrange
        val testSubject = createTestSubject()

        // Act
        testSubject.event(
            MessageReaderViewContract.Event.UpdateAttachments(
                nonInlineAttachments = listOf(fakeAttachment(uri = "uri-1", filename = "file.pdf")),
                extraNonInlineAttachments = emptyList(),
            ),
        )

        // Assert
        testSubject.state.test {
            val state = awaitItem()
            assertThat(state.attachments).isInstanceOf<ImmutableList<*>>()
        }
    }

    @Test
    fun `event UpdateAttachments should update attachments`() = runTest {
        // Arrange
        val testSubject = createTestSubject()
        val attachment = fakeAttachment(uri = "uri-1", filename = "file.pdf", size = 42L, mimeType = "application/pdf")

        // Act
        testSubject.event(
            MessageReaderViewContract.Event.UpdateAttachments(
                nonInlineAttachments = listOf(attachment),
                extraNonInlineAttachments = emptyList(),
            ),
        )

        // Assert
        testSubject.state.test {
            val state = awaitItem()
            assertThat(state.attachments).containsExactly(
                expectedUiItem(uri = "uri-1", filename = "file.pdf", size = 42L, mimeType = "application/pdf"),
            )
        }
    }

    @Test
    fun `event UpdateAttachments should flag extra attachments as encrypted`() = runTest {
        // Arrange
        val testSubject = createTestSubject()
        val extra = fakeAttachment(uri = "uri-1", filename = "encrypted.pdf", size = 42L, mimeType = "application/pdf")

        // Act
        testSubject.event(
            MessageReaderViewContract.Event.UpdateAttachments(
                nonInlineAttachments = emptyList(),
                extraNonInlineAttachments = listOf(extra),
            ),
        )

        // Assert
        testSubject.state.test {
            val state = awaitItem()
            assertThat(state.attachments).containsExactly(
                expectedUiItem(
                    uri = "uri-1",
                    filename = "encrypted.pdf",
                    size = 42L,
                    mimeType = "application/pdf",
                    encrypted = true,
                ),
            )
        }
    }

    @Test
    fun `event UpdateAttachments should only flag extra attachments as encrypted preserving order`() = runTest {
        // Arrange
        val testSubject = createTestSubject()
        val regular = fakeAttachment(uri = "uri-1", filename = "regular.pdf")
        val extra = fakeAttachment(uri = "uri-2", filename = "encrypted.pdf")

        // Act
        testSubject.event(
            MessageReaderViewContract.Event.UpdateAttachments(
                nonInlineAttachments = listOf(regular),
                extraNonInlineAttachments = listOf(extra),
            ),
        )

        // Assert
        testSubject.state.test {
            val state = awaitItem()
            assertThat(state.attachments).containsExactly(
                expectedUiItem(uri = "uri-1", filename = "regular.pdf", encrypted = false),
                expectedUiItem(uri = "uri-2", filename = "encrypted.pdf", encrypted = true),
            )
        }
    }

    private fun createTestSubject(): MessageReaderViewModel =
        MessageReaderViewModel(attachmentViewInfoMapper = FakeAttachmentViewInfoMapper())

    private fun fakeAttachment(
        uri: String,
        filename: String,
        size: Long = 0L,
        inline: Boolean = false,
        mimeType: String? = null,
    ): FakeAttachment = FakeAttachment(
        uri = uri,
        filename = filename,
        sizeValue = size,
        inline = inline,
        mimeTypeValue = mimeType,
    )

    private fun expectedUiItem(
        uri: String,
        filename: String,
        size: Long = 0L,
        mimeType: String? = null,
        encrypted: Boolean = false,
    ): AttachmentUiItem<Part> = AttachmentUiItem.File(
        id = AttachmentId(uri),
        filename = filename,
        formattedSize = "$size bytes",
        size = size,
        mimeType = mimeType,
        part = null,
        encrypted = encrypted,
    )
}

/**
 * Fake mapper that deterministically maps every [AttachmentMetadata] to an
 * [AttachmentUiItem.File] so the [MessageReaderViewModel] mapping behaviour can be asserted without
 * relying on the real mapping implementation. It also records which attachments were mapped, so
 * tests can verify that inline attachments are never mapped.
 */
private class FakeAttachmentViewInfoMapper : AttachmentViewInfoMapper<Part> {

    val mappedFilenames = mutableListOf<String?>()

    override fun AttachmentMetadata<Part>.toUiItem(encrypted: Boolean): AttachmentUiItem<Part> {
        mappedFilenames += filename
        return AttachmentUiItem.File(
            id = AttachmentId(uri),
            filename = filename,
            formattedSize = "${getSize()} bytes",
            size = getSize(),
            mimeType = getMimeType(),
            part = getPart(),
            encrypted = encrypted,
        )
    }

    override fun AttachmentUiItem<Part>.toDomainItem(): AttachmentMetadata<Part> =
        error("toDomainItem() is not used by MessageReaderViewModel")
}

private class FakeAttachment(
    override val uri: String,
    override val filename: String,
    override val isSupportedImage: Boolean = false,
    override val isContentAvailable: Boolean = true,
    private val sizeValue: Long = 0L,
    private val inline: Boolean = false,
    private val mimeTypeValue: String? = null,
    private val partValue: Part? = null,
) : AttachmentMetadata<Part> {
    override fun getSize(): Long = sizeValue
    override fun isInlineAttachment(): Boolean = inline
    override fun getMimeType(): String? = mimeTypeValue
    override fun getPart(): Part? = partValue
}
