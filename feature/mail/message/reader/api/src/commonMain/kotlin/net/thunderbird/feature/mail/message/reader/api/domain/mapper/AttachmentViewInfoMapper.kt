package net.thunderbird.feature.mail.message.reader.api.domain.mapper

import net.thunderbird.feature.mail.message.reader.api.ui.attachment.AttachmentUiItem

/**
 * Mapper interface for converting attachment data between domain and UI representation layers.
 *
 * Provides bidirectional mapping capabilities for attachment objects, supporting a generic
 * type parameter TPart that represents the underlying message part implementation.
 *
 * @param TPart The type representing the underlying message part structure
 */
interface AttachmentViewInfoMapper<TPart> {
    /**
     * Represents the domain model of an email attachment with metadata and content access capabilities.
     *
     * @param TPart The type representing the underlying message part implementation
     */
    interface AttachmentMetadata<TPart> {
        val uri: String
        val filename: String?
        val isSupportedImage: Boolean
        val isContentAvailable: Boolean

        // Should be val size: Long, but temporary get method to avoid
        // a larger changeset. Must be replaced when MessageReader rewrite
        // happen. Kotlin overridden properties can't be annotated with `@JvmField`
        fun getSize(): Long

        // Should be val inlineAttachment: Boolean, but temporary get method to avoid
        // a larger changeset. Must be replaced when MessageReader rewrite
        // happen. Kotlin overridden properties can't be annotated with `@JvmField`
        fun isInlineAttachment(): Boolean

        // Should be val mimeType: String?, but temporary get method to avoid
        // a larger changeset. Must be replaced when MessageReader rewrite
        // happen. Kotlin overridden properties can't be annotated with `@JvmField`
        fun getMimeType(): String?

        fun getPart(): TPart?
    }

    /**
     * Converts this domain attachment model to its UI representation.
     *
     * Maps attachment metadata and content availability to the appropriate AttachmentUiItem subtype
     * based on whether the attachment is an image, inline content, or regular file.
     *
     * @return The UI layer representation of this attachment
     */
    fun AttachmentMetadata<TPart>.toUiItem(encrypted: Boolean): AttachmentUiItem<TPart>

    /**
     * Converts this UI attachment model to its domain representation.
     *
     * Maps the UI layer attachment representation back to the domain model by restoring
     * attachment metadata and content availability information.
     *
     * @return The domain layer representation of this attachment
     */
    fun AttachmentUiItem<TPart>.toDomainItem(): AttachmentMetadata<TPart>
}
