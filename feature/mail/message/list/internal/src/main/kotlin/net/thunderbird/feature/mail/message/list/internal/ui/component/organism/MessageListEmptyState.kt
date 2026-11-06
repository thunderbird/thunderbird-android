package net.thunderbird.feature.mail.message.list.internal.ui.component.organism

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.atom.text.TextTitleMedium
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.feature.mail.message.list.internal.R

const val TEST_TAG_MESSAGE_LIST_EMPTY_STATE = "TestMessageList_EmptyState"
private val EMPTY_STATE_ICON_SIZE = 56.dp

@Composable
internal fun MessageListEmptyState(
    isSearch: Boolean,
    modifier: Modifier = Modifier,
) {
    val icon = if (isSearch) Icons.Outlined.AllInbox else Icons.Outlined.Inbox
    val title = if (isSearch) {
        stringResource(R.string.message_list_empty_search_title)
    } else {
        stringResource(R.string.message_list_empty_folder_title)
    }
    val description = if (isSearch) {
        stringResource(R.string.message_list_empty_search_description)
    } else {
        stringResource(R.string.message_list_empty_folder_description)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(TEST_TAG_MESSAGE_LIST_EMPTY_STATE)
            .verticalScroll(rememberScrollState())
            .padding(BoltTheme.sizes.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(EMPTY_STATE_ICON_SIZE),
            tint = BoltTheme.colors.onSurfaceVariant.copy(alpha = 0.6f),
        )

        Spacer(modifier = Modifier.height(BoltTheme.sizes.medium))

        TextTitleMedium(
            text = title,
            color = BoltTheme.colors.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(BoltTheme.sizes.small))

        TextBodyMedium(
            text = description,
            color = BoltTheme.colors.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}
