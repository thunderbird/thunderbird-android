package com.fsck.k9.activity.setup

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fsck.k9.activity.setup.AccountSetupCompositionContract.Effect
import com.fsck.k9.activity.setup.AccountSetupCompositionContract.Event
import com.fsck.k9.activity.setup.signature.SignatureContent
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.BaseActivity
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.button.ButtonIcon
import net.thunderbird.components.ui.bolt.atom.button.ButtonText
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.atom.textfield.TextFieldOutlinedEmailAddress
import net.thunderbird.components.ui.bolt.molecule.input.TextInput
import net.thunderbird.components.ui.bolt.organism.TopAppBar
import net.thunderbird.components.ui.bolt.template.Scaffold
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.core.ui.contract.mvi.observe
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class AccountSetupComposition : BaseActivity() {

    private val themeProvider: FeatureThemeProvider by inject()
    private val viewModel: AccountSetupCompositionViewModel by viewModel {
        val accountId = intent.getStringExtra(EXTRA_ACCOUNT) ?: error("Missing account UUID")
        parametersOf(accountId)
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var saveActionEnabled by rememberSaveable { mutableStateOf(true) }
            val (state, dispatch) = viewModel.observe { effect ->

                when (effect) {
                    is Effect.ToggleSaveButtonEnabled -> saveActionEnabled = effect.isEnabled
                    is Effect.DoneUpdatingAccount, is Effect.Back -> finish()
                }
            }

            themeProvider.WithTheme {
                AccountSetupCompositionScreen(
                    state = state.value,
                    saveActionEnabled = saveActionEnabled,
                    onEvent = { event -> dispatch(event) },
                )
            }
        }
    }

    companion object {
        private const val EXTRA_ACCOUNT = "account"

        fun actionEditCompositionSettings(context: Activity, accountUuid: String?) {
            val intent = Intent(context, AccountSetupComposition::class.java)
            intent.setAction(Intent.ACTION_EDIT)
            intent.putExtra(EXTRA_ACCOUNT, accountUuid)
            context.startActivity(intent)
        }
    }
}

@Composable
fun AccountSetupCompositionScreen(
    state: AccountSetupCompositionContract.State,
    saveActionEnabled: Boolean,
    onEvent: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            AccountSetupCompositionTopBar(onEvent, saveActionEnabled)
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(BoltTheme.spacings.double),
            ) {
                TextInput(
                    text = state.senderName,
                    onTextChange = { onEvent(Event.SenderNameChange(it)) },
                    label = stringResource(id = R.string.account_settings_name_label),
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                )
                TextFieldOutlinedEmailAddress(
                    value = state.senderEmail,
                    onValueChange = { onEvent(Event.SenderEmailChange(it)) },
                    label = stringResource(id = R.string.account_settings_email_label),
                    modifier = Modifier
                        .padding(horizontal = BoltTheme.spacings.double)
                        .fillMaxWidth(),
                )
                TextFieldOutlinedEmailAddress(
                    value = state.bccEmail,
                    onValueChange = { onEvent(Event.BccEmailChange(it)) },
                    label = stringResource(id = R.string.account_settings_always_bcc_label),
                    modifier = Modifier
                        .padding(horizontal = BoltTheme.spacings.double)
                        .fillMaxWidth(),
                )
                SignatureContent(
                    state = state,
                    onEvent = onEvent,
                    modifier = Modifier
                        .padding(bottom = BoltTheme.spacings.quadruple)
                        .imePadding(),
                )
            }
        }
    }
}

@Composable
private fun AccountSetupCompositionTopBar(
    onEvent: (Event) -> Unit,
    saveActionEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = stringResource(R.string.account_settings_composition_label),
        navigationIcon = {
            ButtonIcon(
                onClick = { onEvent(Event.BackPressed) },
                imageVector = Icons.Outlined.ArrowBack,
            )
        },
        actions = {
            ButtonText(
                enabled = saveActionEnabled,
                onClick = { onEvent(Event.SavePressed) },
                text = stringResource(R.string.edit_identity_save),
            )
        },
        modifier = modifier,
    )
}
