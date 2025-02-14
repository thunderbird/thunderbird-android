package net.discdd.k9.onboarding.ui.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonFilledTonal
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.designsystem.atom.text.TextDisplayMedium
import app.k9mail.core.ui.compose.designsystem.atom.textfield.TextFieldOutlined
import app.k9mail.core.ui.compose.designsystem.molecule.input.PasswordInput
import app.k9mail.core.ui.compose.designsystem.template.LazyColumnWithHeaderFooter
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveContent
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.discdd.k9.onboarding.ui.register.RegisterContract.Event
import net.discdd.k9.onboarding.ui.register.RegisterContract.State

@Composable
internal fun RegisterContent(
    state: State,
    onEvent: (Event) -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
){
    Surface(
        modifier = modifier
    ) {
        ResponsiveContent {
            LazyColumnWithHeaderFooter(
                modifier = Modifier.fillMaxSize(),
                header = {
                    TextDisplayMedium(
                        text = "Register Screen",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 50.dp),
                        textAlign = TextAlign.Center
                    )
                },
                footer = {
                    RegisterFooter(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = MainTheme.spacings.quadruple),
                        onLoginClick = onLoginClick
                    )
                },
                verticalArrangement =  Arrangement.SpaceEvenly
            ) {
                item {
                    RegisterInputs(
                        state = state,
                        onEvent = onEvent
                    )
                }
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ButtonFilledTonal(
                            text = "Register",
                            onClick = {
                                onEvent(
                                    Event.OnClickRegister(
                                        state.prefix1.value,
                                        state.prefix2.value,
                                        state.prefix3.value,
                                        state.suffix1.value,
                                        state.suffix2.value,
                                        state.suffix3.value,
                                        state.password.value
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RegisterFooter(
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        ButtonText(text = "Go login", onClick = onLoginClick)
    }
}

@Composable
private fun RegisterInputs(
    state: State,
    onEvent: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextFieldOutlined(
            value = state.prefix1.value,
            onValueChange = { onEvent(Event.Prefix1Changed(it)) },
            label = "Prefix 1"
        )
        TextFieldOutlined(
            value = state.prefix2.value,
            onValueChange = {onEvent(Event.Prefix2Changed(it))},
            label = "Prefix 2"
        )
        TextFieldOutlined(
            value = state.prefix3.value,
            onValueChange = {onEvent(Event.Prefix3Changed(it))},
            label = "Prefix 3",
        )
        TextFieldOutlined(
            value = state.suffix1.value,
            onValueChange = {onEvent(Event.Suffix1Changed(it))},
            label = "Suffix 1",
        )
        TextFieldOutlined(
            value = state.suffix2.value,
            onValueChange = {onEvent(Event.Suffix2Changed(it))},
            label = "Suffix 2",
        )
        TextFieldOutlined(
            value = state.suffix3.value,
            onValueChange = {onEvent(Event.Suffix3Changed(it))},
            label = "Suffix 3"
        )
        PasswordInput(password = state.password.value, onPasswordChange = {onEvent(Event.PasswordChanged(it))})
    }
}
