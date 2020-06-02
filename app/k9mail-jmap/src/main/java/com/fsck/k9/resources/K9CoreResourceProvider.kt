package com.fsck.k9.resources

import android.content.Context
import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.jmap.R

class K9CoreResourceProvider(private val context: Context) : CoreResourceProvider {
    override fun defaultSignature(): String = context.getString(R.string.default_signature)
    override fun defaultIdentityDescription(): String = context.getString(R.string.default_identity_description)

    override fun sendAlternateChooserTitle(): String = context.getString(R.string.send_alternate_chooser_title)

    override fun internalStorageProviderName(): String =
            context.getString(R.string.local_storage_provider_internal_label)

    override fun externalStorageProviderName(): String =
            context.getString(R.string.local_storage_provider_external_label)

    override fun contactDisplayNamePrefix(): String = context.getString(R.string.message_to_label)

    override fun messageHeaderFrom(): String = context.getString(R.string.message_compose_quote_header_from)
    override fun messageHeaderTo(): String = context.getString(R.string.message_compose_quote_header_to)
    override fun messageHeaderCc(): String = context.getString(R.string.message_compose_quote_header_cc)
    override fun messageHeaderDate(): String = context.getString(R.string.message_compose_quote_header_send_date)
    override fun messageHeaderSubject(): String = context.getString(R.string.message_compose_quote_header_subject)
    override fun messageHeaderSeparator(): String = context.getString(R.string.message_compose_quote_header_separator)

    override fun noSubject(): String = context.getString(R.string.general_no_subject)

    override fun userAgent(): String = context.getString(R.string.message_header_mua)
    override fun encryptedSubject(): String = context.getString(R.string.encrypted_subject)

    override fun replyHeader(sender: String): String =
            context.getString(R.string.message_compose_reply_header_fmt, sender)

    override fun replyHeader(sender: String, sentDate: String): String =
            context.getString(R.string.message_compose_reply_header_fmt_with_date, sentDate, sender)

    override fun searchAllMessagesTitle(): String = context.getString(R.string.search_all_messages_title)
    override fun searchAllMessagesDetail(): String = context.getString(R.string.search_all_messages_detail)
    override fun searchUnifiedInboxTitle(): String = context.getString(R.string.integrated_inbox_title)
    override fun searchUnifiedInboxDetail(): String = context.getString(R.string.integrated_inbox_detail)

    override fun outboxFolderName(): String = context.getString(R.string.special_mailbox_name_outbox)
}
