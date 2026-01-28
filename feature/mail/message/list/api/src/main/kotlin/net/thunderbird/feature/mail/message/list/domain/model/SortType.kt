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
    SubjectAsc(labelResId = R.string.sort_by_subject),
    SubjectDesc(labelResId = R.string.sort_by_subject),
    SenderAsc(labelResId = R.string.sort_by_sender),
    SenderDesc(labelResId = R.string.sort_by_sender),
    FlaggedAsc(labelResId = R.string.sort_by_flag),
    FlaggedDesc(labelResId = R.string.sort_by_flag),
    UnreadAsc(labelResId = R.string.sort_by_unread),
    UnreadDesc(labelResId = R.string.sort_by_unread),
    AttachmentAsc(labelResId = R.string.sort_by_attach),
    AttachmentDesc(labelResId = R.string.sort_by_attach),
}
