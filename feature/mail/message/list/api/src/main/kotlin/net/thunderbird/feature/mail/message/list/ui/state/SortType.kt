package net.thunderbird.feature.mail.message.list.ui.state

/**
 * Represents the different ways a list of messages can be sorted.
 * Each enum constant defines a specific sorting criterion and its direction (ascending or descending).
 */
enum class SortType {
    DateAsc,
    DateDesc,
    ArrivalAsc,
    ArrivalDesc,
    SenderAsc,
    SenderDesc,
    UnreadAsc,
    UnreadDesc,
    FlaggedAsc,
    FlaggedDesc,
    AttachmentAsc,
    AttachmentDesc,
    SubjectAsc,
    SubjectDesc,
}
