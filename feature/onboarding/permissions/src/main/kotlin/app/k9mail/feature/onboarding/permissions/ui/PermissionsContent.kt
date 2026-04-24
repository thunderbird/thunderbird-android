package app.k9mail.feature.onboarding.permissions.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import app.k9mail.core.ui.compose.common.visibility.hide
import app.k9mail.core.ui.compose.designsystem.atom.DelayedCircularProgressIndicator
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonFilled
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.feature.account.common.ui.AppTitleTopHeader
import app.k9mail.feature.onboarding.permissions.R
import app.k9mail.feature.onboarding.permissions.ui.PermissionsContract.Event
import app.k9mail.feature.onboarding.permissions.ui.PermissionsContract.State
import net.thunderbird.core.ui.compose.common.modifier.testTagAsResourceId
import net.thunderbird.core.ui.compose.designsystem.atom.icon.IconsWithBottomRightOverlay
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.thundermail.ui.brandBackground
import net.thunderbird.feature.thundermail.ui.component.template.ThundermailScaffold
import net.thunderbird.feature.thundermail.ui.screen.ThundermailConstants
import app.k9mail.feature.account.common.R as CommonR

@Composable
internal fun SharedTransitionScope.PermissionsContent(
    state: State,
    onEvent: (Event) -> Unit,
    brandName: String,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    ThundermailScaffold(
        header = {
            AppTitleTopHeader(
                title = brandName,
                sharedTransitionScope = this,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        },
        subHeaderText = stringResource(R.string.onboarding_permissions_screen_title),
        bottomBar = { paddingValues, containerColor ->
            BottomBar(
                state = state,
                onEvent = onEvent,
                modifier = Modifier
                    .imePadding()
                    .background(containerColor)
                    .padding(paddingValues)
                    .padding(top = MainTheme.spacings.default)
                    .padding(horizontal = MainTheme.spacings.quadruple),
            )
        },
        maxWidth = Dp.Unspecified,
        modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars),
        canScrollForward = scrollState.canScrollForward,
    ) { scaffoldPaddingValues, responsivePaddingValues, maxWidth ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .brandBackground()
                .padding(scaffoldPaddingValues)
                .padding(responsivePaddingValues),
        ) {
            Column(modifier = Modifier.widthIn(max = ThundermailConstants.MaxContainerWidth)) {
                Spacer(modifier = Modifier.height(MainTheme.spacings.quadruple))
                ContentArea(
                    state = state,
                    onEvent = onEvent,
                    modifier = Modifier.padding(bottom = MainTheme.sizes.large),
                )
            }
            Spacer(modifier = Modifier.weight(weight = .15f))
        }
    }
}

@Composable
private fun ContentArea(state: State, onEvent: (Event) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.quadruple),
    ) {
        if (state.isLoading) {
            DelayedCircularProgressIndicator()
        } else {
            PermissionBoxes(state, onEvent)
        }
    }
}

@Composable
private fun ColumnScope.PermissionBoxes(
    state: State,
    onEvent: (Event) -> Unit,
) {
    PermissionBox(
        icon = IconsWithBottomRightOverlay.person,
        permissionState = state.contactsPermissionState,
        title = stringResource(R.string.onboarding_permissions_contacts_title),
        description = stringResource(R.string.onboarding_permissions_contacts_description),
        onAllowClick = { onEvent(Event.AllowContactsPermissionClicked) },
    )

    if (state.isNotificationsPermissionVisible) {
        PermissionBox(
            icon = IconsWithBottomRightOverlay.notification,
            permissionState = state.notificationsPermissionState,
            title = stringResource(R.string.onboarding_permissions_notifications_title),
            description = stringResource(R.string.onboarding_permissions_notifications_description),
            onAllowClick = { onEvent(Event.AllowNotificationsPermissionClicked) },
        )
    }
}

@Composable
private fun BottomBar(
    state: State,
    onEvent: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = MainTheme.spacings.default,
                bottom = MainTheme.spacings.double,
            )
            .padding(horizontal = MainTheme.spacings.quadruple),
        horizontalArrangement = Arrangement.End,
    ) {
        Crossfade(
            targetState = state.isNextButtonVisible,
            label = "NextButton",
        ) { isNextButtonVisible ->
            ButtonFilled(
                text = stringResource(CommonR.string.account_common_button_next),
                onClick = { onEvent(Event.NextClicked) },
                modifier = Modifier
                    .hide(!isNextButtonVisible)
                    .testTagAsResourceId("onboarding_permissions_next_button"),
            )

            ButtonText(
                text = stringResource(R.string.onboarding_permissions_skip_button),
                onClick = { onEvent(Event.NextClicked) },
                modifier = Modifier
                    .hide(isNextButtonVisible)
                    .testTagAsResourceId("onboarding_permissions_skip_button"),
            )
        }
    }
}
