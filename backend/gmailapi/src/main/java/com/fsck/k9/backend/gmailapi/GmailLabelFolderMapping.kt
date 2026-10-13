package com.fsck.k9.backend.gmailapi

private val SYSTEM_FOLDER_LABELS = setOf(
    "INBOX",
    "SENT",
    "DRAFT",
    "TRASH",
    "SPAM",
    "ALL_MAIL",
)

internal fun shouldMapGmailLabelToFolder(label: GmailLabel): Boolean =
    label.type != "system" || label.id in SYSTEM_FOLDER_LABELS
