package net.discdd.k9.onboarding.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonFilledTonal
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.designsystem.atom.text.TextDisplayMedium
import app.k9mail.core.ui.compose.designsystem.molecule.input.EmailAddressInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.PasswordInput
import app.k9mail.core.ui.compose.designsystem.template.LazyColumnWithHeaderFooter
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveContent
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.discdd.k9.onboarding.ui.login.LoginContract.State
import net.discdd.k9.onboarding.ui.login.LoginContract.Event

@Composable
internal fun LoginContent(
    state: State,
    onEvent: (Event) -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier
){
    Surface(
        modifier = modifier
    ){
        ResponsiveContent {
            LazyColumnWithHeaderFooter(
                modifier = Modifier.fillMaxSize(),
                header = {
                    TextDisplayMedium(
                        text = "Login Screen",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 50.dp),
                        textAlign = TextAlign.Center
                    )
                },
                footer = {
                    LoginFooter(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = MainTheme.spacings.quadruple),
                        onRegisterClick = onRegisterClick
                    )
                },
                verticalArrangement =  Arrangement.SpaceEvenly
            ){
                item {
                    LoginInputs(
                        state = state,
                        onEvent = onEvent
                    )
                }
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        ButtonFilledTonal(
                            text = "Login",
                            onClick = {
                                onEvent(Event.OnClickLogin(state.emailAddress.value, state.password.value))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginFooter(
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        ButtonText(text = "Go register", onClick = onRegisterClick)
    }
}

@Composable
private fun LoginInputs(
    state: State,
    onEvent: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    EmailAddressInput(
        emailAddress = state.emailAddress.value,
        onEmailAddressChange = {onEvent(Event.EmailAddressChanged(it))},
    )
    PasswordInput(
        password = state.password.value,
        onPasswordChange = {onEvent(Event.PasswordChanged(it))},
    )
}
