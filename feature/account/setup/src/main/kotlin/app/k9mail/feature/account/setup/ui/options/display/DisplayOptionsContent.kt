package app.k9mail.feature.account.setup.ui.options.display

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import app.k9mail.core.ui.compose.designsystem.molecule.input.TextInput
import app.k9mail.feature.account.common.ui.item.defaultItemPadding
import app.k9mail.feature.account.setup.R
import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsContract.Event
import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsContract.State
import net.thunderbird.core.ui.compose.common.modifier.testTagAsResourceId
import net.thunderbird.core.ui.compose.theme2.MainTheme

@Suppress("LongMethod")
@Composable
internal fun DisplayOptionsContent(
    state: State,
    onEvent: (Event) -> Unit,
    contentPadding: PaddingValues,
    maxWidth: Dp,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    val resources = LocalResources.current
    LazyColumn(
        modifier = modifier
            .widthIn(max = maxWidth)
            .fillMaxSize()
            .imePadding(),
        contentPadding = contentPadding,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
        state = listState,
    ) {
        item {
            TextInput(
                text = state.accountName.value,
                errorMessage = state.accountName.error?.toResourceString(resources),
                onTextChange = { onEvent(Event.OnAccountNameChanged(it)) },
                label = stringResource(id = R.string.account_setup_options_account_name_label),
                contentPadding = defaultItemPadding(),
                modifier = Modifier.testTagAsResourceId("account_setup_display_options_account_name_input"),
            )
        }

        item {
            TextInput(
                text = state.displayName.value,
                errorMessage = state.displayName.error?.toResourceString(resources),
                onTextChange = { onEvent(Event.OnDisplayNameChanged(it)) },
                label = stringResource(id = R.string.account_setup_options_your_name_label),
                contentPadding = defaultItemPadding(),
                isRequired = true,
                modifier = Modifier.testTagAsResourceId("account_setup_display_options_display_name_input"),
            )
        }

        item {
            TextInput(
                text = state.emailSignature.value,
                errorMessage = state.emailSignature.error?.toResourceString(resources),
                onTextChange = { onEvent(Event.OnEmailSignatureChanged(it)) },
                label = stringResource(id = R.string.account_setup_options_email_signature_label),
                contentPadding = defaultItemPadding(),
                isSingleLine = false,
                modifier = Modifier.testTagAsResourceId("account_setup_display_options_signature_input"),
            )
        }

        item {
            Spacer(modifier = Modifier.requiredHeight(MainTheme.sizes.smaller))
        }
    }
}
