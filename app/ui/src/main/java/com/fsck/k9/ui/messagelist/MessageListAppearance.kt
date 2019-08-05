package com.fsck.k9.ui.messagelist

data class MessageListAppearance(
        val checkboxes: Boolean,
        val previewLines: Int,
        val stars: Boolean,
        val senderAboveSubject: Boolean,
        val showContactPicture: Boolean,
        val showingThreadedList: Boolean = false
)