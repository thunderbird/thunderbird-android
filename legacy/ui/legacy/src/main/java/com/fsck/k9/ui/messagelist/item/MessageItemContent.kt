package com.fsck.k9.ui.messagelist.item

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
import com.fsck.k9.ui.messagelist.MessageListItem

@Composable
internal fun MessageItemContent(item: MessageListItem, isActive: Boolean, isSelected: Boolean) {
    Column(
        modifier = Modifier
            .padding(MainTheme.spacings.default),
    ) {
        TextBodyMedium(text = "UniqueId: ${item.uniqueId}")
        TextBodyMedium(text = "Active: $isActive, Selected: $isSelected")
    }
}
