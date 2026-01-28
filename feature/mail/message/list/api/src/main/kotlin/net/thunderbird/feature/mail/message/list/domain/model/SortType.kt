package net.thunderbird.feature.mail.message.list.domain.model

import androidx.annotation.StringRes
import net.thunderbird.feature.mail.message.list.R

/**
 * Represents the different ways a list of messages can be sorted.
 * Each enum constant defines a specific sorting criterion and its direction (ascending or descending).
 */
@Suppress("detekt.UnnecessaryAnnotationUseSiteTarget") // https://github.com/detekt/detekt/issues/8212
enum class SortType(@param:StringRes val labelResId: Int) {
    DateAsc(labelResId = R.string.sort_by_date_asc),
    DateDesc(labelResId = R.string.sort_by_date_desc),
    ArrivalAsc(labelResId = R.string.sort_by_arrival_asc),
    ArrivalDesc(labelResId = R.string.sort_by_arrival_desc),
    SubjectAsc(labelResId = R.string.sort_by_subject_asc),
    SubjectDesc(labelResId = R.string.sort_by_subject_desc),
    SenderAsc(labelResId = R.string.sort_by_sender_asc),
    SenderDesc(labelResId = R.string.sort_by_sender_desc),
    FlaggedAsc(labelResId = R.string.sort_by_flag_asc),
    FlaggedDesc(labelResId = R.string.sort_by_flag_desc),
    UnreadAsc(labelResId = R.string.sort_by_unread_asc),
    UnreadDesc(labelResId = R.string.sort_by_unread_desc),
    AttachmentAsc(labelResId = R.string.sort_by_attach_asc),
    AttachmentDesc(labelResId = R.string.sort_by_attach_desc),
}
