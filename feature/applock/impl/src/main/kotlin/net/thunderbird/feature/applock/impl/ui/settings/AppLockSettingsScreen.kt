package net.thunderbird.feature.applock.impl.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.core.ui.compose.designsystem.organism.TopAppBarWithBackButton
import net.thunderbird.feature.applock.api.AppLockAuthenticatorFactory
import net.thunderbird.feature.applock.impl.R
import net.thunderbird.feature.applock.impl.ui.settings.AppLockSettingsContract.Effect
import net.thunderbird.feature.applock.impl.ui.settings.AppLockSettingsContract.Event
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
internal fun AppLockSettingsScreen(
    onBack: () -> Unit,
    viewModel: AppLockSettingsContract.ViewModel = koinViewModel<AppLockSettingsViewModel>(),
    authenticatorFactory: AppLockAuthenticatorFactory = koinInject(),
) {
    val context = LocalContext.current

    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            Effect.NavigateBack -> onBack()
            Effect.RequestAuthentication -> {
                val activity = context as? FragmentActivity ?: return@observe
                val authenticator = authenticatorFactory.create(activity)
                viewModel.event(Event.OnAuthenticatorReady(authenticator))
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                dispatch(Event.OnResume)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler {
        dispatch(Event.OnBackPressed)
    }

    Scaffold(
        topBar = {
            TopAppBarWithBackButton(
                title = stringResource(R.string.applock_settings_screen_title),
                onBackClick = { dispatch(Event.OnBackPressed) },
            )
        },
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
    ) { innerPadding ->
        AppLockSettingsContent(
            state = state.value,
            onEvent = { dispatch(it) },
            contentPadding = innerPadding,
        )
    }
}
