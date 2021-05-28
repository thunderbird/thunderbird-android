package com.fsck.k9.resources

import android.content.Context
import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.LocaleHelper
import com.fsck.k9.jmap.R

class K9CoreResourceProvider(private val context: Context) : CoreResourceProvider {
    override fun defaultSignature(): String = resolve(R.string.default_signature)
    override fun defaultIdentityDescription(): String = resolve(R.string.default_identity_description)

    override fun internalStorageProviderName(): String = resolve(R.string.local_storage_provider_internal_label)

    override fun externalStorageProviderName(): String = resolve(R.string.local_storage_provider_external_label)

    override fun contactDisplayNamePrefix(): String = resolve(R.string.message_to_label)
    override fun contactUnknownSender(): String = resolve(R.string.unknown_sender)
    override fun contactUnknownRecipient(): String = resolve(R.string.unknown_recipient)

    override fun messageHeaderFrom(): String = resolve(R.string.message_compose_quote_header_from)
    override fun messageHeaderTo(): String = resolve(R.string.message_compose_quote_header_to)
    override fun messageHeaderCc(): String = resolve(R.string.message_compose_quote_header_cc)
    override fun messageHeaderDate(): String = resolve(R.string.message_compose_quote_header_send_date)
    override fun messageHeaderSubject(): String = resolve(R.string.message_compose_quote_header_subject)
    override fun messageHeaderSeparator(): String = resolve(R.string.message_compose_quote_header_separator)

    override fun noSubject(): String = resolve(R.string.general_no_subject)

    override fun userAgent(): String = resolve(R.string.message_header_mua)
    override fun encryptedSubject(): String = resolve(R.string.encrypted_subject)

    override fun replyHeader(sender: String): String = resolve(R.string.message_compose_reply_header_fmt, sender)

    override fun replyHeader(sender: String, sentDate: String): String =
        resolve(R.string.message_compose_reply_header_fmt_with_date, sentDate, sender)

    override fun searchAllMessagesTitle(): String = resolve(R.string.search_all_messages_title)
    override fun searchAllMessagesDetail(): String = resolve(R.string.search_all_messages_detail)
    override fun searchUnifiedInboxTitle(): String = resolve(R.string.integrated_inbox_title)
    override fun searchUnifiedInboxDetail(): String = resolve(R.string.integrated_inbox_detail)

    override fun outboxFolderName(): String = resolve(R.string.special_mailbox_name_outbox)

    private fun resolve(id: Int): String {
        LocaleHelper.initializeLocale(context.resources)
        return context.getString(id)
    }

    private fun resolve(id: Int, vararg args: String): String {
        LocaleHelper.initializeLocale(context.resources)
        return context.getString(id, *args)
    }
}
