package net.discdd.k9.onboarding.ui.register


import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.discdd.k9.onboarding.ui.register.RegisterContent
import net.discdd.k9.onboarding.ui.register.RegisterViewModel
import net.discdd.k9.onboarding.ui.register.RegisterContract.Effect

@Composable
fun RegisterScreen(
    onLoginClick: () -> Unit,
    viewModel: RegisterViewModel,
    onPendingState: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel.effectFlow) {
        viewModel.effectFlow.collect { effect ->
            when (effect) {
                Effect.OnPendingState -> onPendingState()
                Effect.OnLoggedInState -> onPendingState()
            }
        }
    }

    RegisterContent(
        state = state.value,
        onEvent = { viewModel.event(it) },
        modifier = modifier,
        onLoginClick = onLoginClick
    )
}
