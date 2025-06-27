package net.thunderbird.feature.navigation.drawer.dropdown.ui.account

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.theme2.MainTheme
import kotlinx.collections.immutable.ImmutableList
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayAccount

@Composable
internal fun AccountList(
    accounts: ImmutableList<DisplayAccount>,
    selectedAccount: DisplayAccount?,
    onAccountClick: (DisplayAccount) -> Unit,
    showStarredCount: Boolean,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth(),
        contentPadding = PaddingValues(vertical = MainTheme.spacings.default),
    ) {
        items(
            items = accounts,
            key = { account -> account.id },
        ) { account ->
            AccountListItem(
                account = account,
                onClick = { onAccountClick(account) },
                selected = selectedAccount == account,
                showStarredCount = showStarredCount,
            )
        }
    }
}
