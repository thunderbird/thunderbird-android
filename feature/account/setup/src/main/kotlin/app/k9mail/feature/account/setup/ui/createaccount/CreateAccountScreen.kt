package app.k9mail.feature.account.setup.ui.createaccount

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import app.k9mail.core.common.provider.BrandNameProvider
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.feature.account.common.ui.AppTitleTopHeader
import app.k9mail.feature.account.common.ui.WizardNavigationBar
import app.k9mail.feature.account.common.ui.WizardNavigationBarState
import app.k9mail.feature.account.setup.domain.entity.AccountUuid
import app.k9mail.feature.account.setup.ui.createaccount.CreateAccountContract.Effect
import app.k9mail.feature.account.setup.ui.createaccount.CreateAccountContract.Event
import app.k9mail.feature.account.setup.ui.createaccount.CreateAccountContract.ViewModel

@Composable
internal fun CreateAccountScreen(
    onNext: (AccountUuid) -> Unit,
    onBack: () -> Unit,
    viewModel: ViewModel,
    brandNameProvider: BrandNameProvider,
    modifier: Modifier = Modifier,
) {
    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            Effect.NavigateBack -> onBack()
            is Effect.NavigateNext -> onNext(effect.accountUuid)
        }
    }

    LaunchedEffect(key1 = Unit) {
        dispatch(Event.CreateAccount)
    }

    BackHandler {
        dispatch(Event.OnBackClicked)
    }

    Scaffold(
        topBar = {
            AppTitleTopHeader(
                title = brandNameProvider.brandName,
            )
        },
        bottomBar = {
            WizardNavigationBar(
                onNextClick = {},
                onBackClick = {
                    dispatch(Event.OnBackClicked)
                },
                state = WizardNavigationBarState(
                    showNext = false,
                    isBackEnabled = state.value.error != null,
                ),
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        CreateAccountContent(
            state = state.value,
            contentPadding = innerPadding,
        )
    }
}
