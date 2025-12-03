package net.thunderbird.feature.mail.message.list.extension

import net.thunderbird.feature.mail.message.list.ui.state.SortType
import net.thunderbird.core.android.account.SortType as DomainSortType

/**
 * Maps a [DomainSortType] from the domain layer to a [SortType] in the UI layer.
 *
 * This extension function takes the domain-level sort criteria and a boolean indicating
 * the sort direction to produce the corresponding specific UI sort type.
 *
 * @param isAscending `true` for ascending order, `false` for descending order.
 * @return The corresponding [SortType] for the UI layer.
 */
@Suppress("ComplexMethod")
fun DomainSortType.toSortType(isAscending: Boolean): SortType = when (this) {
    DomainSortType.SORT_DATE if isAscending -> SortType.DateAsc
    DomainSortType.SORT_DATE -> SortType.DateDesc
    DomainSortType.SORT_ARRIVAL if isAscending -> SortType.ArrivalAsc
    DomainSortType.SORT_ARRIVAL -> SortType.ArrivalDesc
    DomainSortType.SORT_SUBJECT if isAscending -> SortType.SubjectAsc
    DomainSortType.SORT_SUBJECT -> SortType.SubjectDesc
    DomainSortType.SORT_SENDER if isAscending -> SortType.SenderAsc
    DomainSortType.SORT_SENDER -> SortType.SenderDesc
    DomainSortType.SORT_UNREAD if isAscending -> SortType.UnreadAsc
    DomainSortType.SORT_UNREAD -> SortType.UnreadDesc
    DomainSortType.SORT_FLAGGED if isAscending -> SortType.FlaggedAsc
    DomainSortType.SORT_FLAGGED -> SortType.FlaggedDesc
    DomainSortType.SORT_ATTACHMENT if isAscending -> SortType.AttachmentAsc
    DomainSortType.SORT_ATTACHMENT -> SortType.AttachmentDesc
}
