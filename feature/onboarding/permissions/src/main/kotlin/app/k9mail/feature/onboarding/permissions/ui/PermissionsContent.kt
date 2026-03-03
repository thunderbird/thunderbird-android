package app.k9mail.feature.onboarding.permissions.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.CircularProgressIndicator
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonFilled
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadlineSmall
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.feature.account.common.ui.AppTitleTopHeader
import app.k9mail.feature.onboarding.permissions.R
import app.k9mail.feature.onboarding.permissions.ui.PermissionsContract.Event
import app.k9mail.feature.onboarding.permissions.ui.PermissionsContract.State
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.thunderbird.core.ui.compose.common.modifier.testTagAsResourceId
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.account.common.R as CommonR

private const val LOADING_INDICATOR_DELAY = 500L

@Composable
internal fun PermissionsContent(
    state: State,
    onEvent: (Event) -> Unit,
    brandName: String,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Scaffold(
        bottomBar = {
            BottomBar(state, onEvent, scrollState)
        },
        modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars),
    ) { innerPadding ->
        ResponsiveWidthContainer(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding),
        ) { contentPadding ->
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxHeight()
                    .verticalScroll(state = scrollState)
                    .padding(contentPadding),
            ) {
                HeaderArea(brandName = brandName)

                ContentArea(state, onEvent)

                // This provides some bottom padding but is also necessary to make the vertical arrangement have the
                // desired effect of putting ContentArea() in the middle.
                Spacer(modifier = Modifier.height(MainTheme.spacings.double))
            }
        }
    }
}

@Composable
private fun HeaderArea(
    brandName: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppTitleTopHeader(
            title = brandName,
        )

        TextHeadlineSmall(
            text = stringResource(R.string.onboarding_permissions_screen_title),
            modifier = Modifier.padding(horizontal = MainTheme.spacings.double),
        )

        Spacer(modifier = Modifier.height(MainTheme.spacings.double))
    }
}

@Composable
private fun ContentArea(state: State, onEvent: (Event) -> Unit) {
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxHeight()
            .padding(MainTheme.spacings.double),
    ) {
        if (state.isLoading) {
            DelayedCircularProgressIndicator()
        } else {
            PermissionBoxes(state, onEvent)
        }
    }
}

@Composable
private fun PermissionBoxes(
    state: State,
    onEvent: (Event) -> Unit,
) {
    PermissionBox(
        icon = Icons.Filled.Person,
        permissionState = state.contactsPermissionState,
        title = stringResource(R.string.onboarding_permissions_contacts_title),
        description = stringResource(R.string.onboarding_permissions_contacts_description),
        onAllowClick = { onEvent(Event.AllowContactsPermissionClicked) },
    )

    if (state.isNotificationsPermissionVisible) {
        Spacer(modifier = Modifier.height(MainTheme.spacings.quadruple))

        PermissionBox(
            icon = Icons.Filled.Notifications,
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
    scrollState: ScrollState,
) {
    // Elevate the bottom bar when some scrollable content is "underneath" it
    val elevation by animateDpAsState(
        targetValue = if (scrollState.canScrollForward) 8.dp else 0.dp,
        label = "BottomBarElevation",
    )

    Surface(
        tonalElevation = elevation,
    ) {
        ResponsiveWidthContainer(
            modifier = Modifier.fillMaxWidth(),
        ) { contentPadding ->
            Row(
                modifier = Modifier
                    .padding(
                        start = MainTheme.spacings.quadruple,
                        end = MainTheme.spacings.quadruple,
                        top = MainTheme.spacings.default,
                        bottom = MainTheme.spacings.double,
                    )
                    .fillMaxWidth()
                    .padding(contentPadding),
                horizontalArrangement = Arrangement.End,
            ) {
                Crossfade(
                    targetState = state.isNextButtonVisible,
                    label = "NextButton",
                ) { isNextButtonVisible ->
                    ButtonFilled(
                        text = stringResource(CommonR.string.account_common_button_next),
                        onClick = { onEvent(Event.NextClicked) },
                        modifier = Modifier.hide(!isNextButtonVisible)
                            .testTagAsResourceId("onboarding_permissions_next_button"),
                    )

                    ButtonText(
                        text = stringResource(R.string.onboarding_permissions_skip_button),
                        onClick = { onEvent(Event.NextClicked) },
                        modifier = Modifier.hide(isNextButtonVisible)
                            .testTagAsResourceId("onboarding_permissions_skip_button"),
                    )
                }
            }
        }
    }
}

@Composable
fun DelayedCircularProgressIndicator(
    modifier: Modifier = Modifier,
) {
    var progressIndicatorVisible by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = Unit) {
        launch {
            delay(LOADING_INDICATOR_DELAY)
            progressIndicatorVisible = true
        }
    }

    CircularProgressIndicator(
        modifier = Modifier
            .hide(!progressIndicatorVisible)
            .then(modifier),
    )
}

private fun Modifier.hide(hide: Boolean): Modifier {
    return if (hide) {
        alpha(0f).clearAndSetSemantics {}
    } else {
        alpha(1f)
    }
}
