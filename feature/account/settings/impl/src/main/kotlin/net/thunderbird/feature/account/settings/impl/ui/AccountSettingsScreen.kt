package net.thunderbird.feature.account.settings.impl.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.organism.SubtitleTopAppBarWithBackButton
import app.k9mail.core.ui.compose.designsystem.template.Scaffold

@Composable
fun AccountSettingsScreen(
    accountId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            SubtitleTopAppBarWithBackButton(
                title = "Account settings",
                subtitle = accountId,
                onBackClick = onBack,
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding),
        ) {
            TextBodyLarge(text = "accountId: $accountId")
        }
    }
}
