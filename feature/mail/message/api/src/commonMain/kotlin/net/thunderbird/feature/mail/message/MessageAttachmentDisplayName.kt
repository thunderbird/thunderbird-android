package net.thunderbird.feature.mail.message
/**
 * The display name for a message attachment.
 *
 * This represents the human-readable filename or label shown to users for an attachment,
 * such as "document" or "photo".
 */
@JvmInline
value class MessageAttachmentDisplayName(val value: String)

/**
 * @return This display name or a default [MessageAttachmentDisplayName] with value "attachment"
 */
fun MessageAttachmentDisplayName?.orDefault(): MessageAttachmentDisplayName =
    this ?: MessageAttachmentDisplayName("attachment")
