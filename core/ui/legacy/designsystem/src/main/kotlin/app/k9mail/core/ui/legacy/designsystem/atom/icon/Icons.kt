package app.k9mail.core.ui.legacy.designsystem.atom.icon

import app.k9mail.core.ui.legacy.designsystem.R

/**
 * Icons used in the legacy design system.
 *
 * The icons are organized in two types: `Filled` and `Outlined`. Each object contains the icons as drawableRes.
 */
object Icons {
    object Filled {
        val Archive = R.drawable.ic_archive

        @JvmField
        val AttachmentImage = R.drawable.ic_attachment_image
        val CheckCircle = R.drawable.ic_check_circle

        @JvmField
        val ContactPicture = R.drawable.ic_contact_picture
        val Cog = R.drawable.ic_cog
        val Drafts = R.drawable.ic_drafts_folder
        val Folder = R.drawable.ic_folder
        val InboxMultiple = R.drawable.ic_inbox_multiple
        val MarkNew = R.drawable.ic_mark_new
        val Move = R.drawable.ic_move_to_folder
        val OpenedEnvelope = R.drawable.ic_opened_envelope
        val OpenBook = R.drawable.ic_open_book

        @JvmField
        val Reply = R.drawable.ic_reply

        @JvmField
        val ReplyAll = R.drawable.ic_reply_all

        val Send = R.drawable.ic_send
        val Star = R.drawable.ic_star
        val Spam = R.drawable.ic_alert_octagon
        val Trash = R.drawable.ic_trash_can
    }

    object Outlined {
        val AccountPlus = R.drawable.ic_account_plus
        val ArrowBack = R.drawable.ic_arrow_back
        val Close = R.drawable.ic_close
        val Export = R.drawable.ic_export
        val Help = R.drawable.ic_help
        val Import = R.drawable.ic_import
        val Info = R.drawable.ic_info
        val Inbox = R.drawable.ic_inbox
        val Menu = R.drawable.ic_menu
        val Outbox = R.drawable.ic_outbox
        val PushNotification = R.drawable.ic_push_notification
        val Star = R.drawable.ic_star_outline
    }
}
