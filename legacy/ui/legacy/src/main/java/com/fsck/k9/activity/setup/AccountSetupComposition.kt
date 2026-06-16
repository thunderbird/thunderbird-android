package com.fsck.k9.activity.setup

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.thunderbird.components.ui.bolt.atom.Checkbox
import net.thunderbird.components.ui.bolt.atom.RadioGroup
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.button.ButtonIcon
import net.thunderbird.components.ui.bolt.atom.button.ButtonText
import net.thunderbird.components.ui.bolt.atom.text.TextBodyLarge
import net.thunderbird.components.ui.bolt.atom.text.TextBodySmall
import net.thunderbird.components.ui.bolt.atom.textfield.TextFieldOutlined
import net.thunderbird.components.ui.bolt.atom.textfield.TextFieldOutlinedEmailAddress
import net.thunderbird.components.ui.bolt.molecule.input.TextInput
import net.thunderbird.components.ui.bolt.organism.TopAppBar
import net.thunderbird.components.ui.bolt.template.Scaffold
import com.fsck.k9.activity.setup.AccountSetupCompositionContract.Effect
import com.fsck.k9.activity.setup.AccountSetupCompositionContract.Event
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.BaseActivity
import kotlinx.collections.immutable.PersistentList
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.theme.MainTheme
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
                    senderName = state.value.senderName,
                    senderEmail = state.value.senderEmail,
                    onEvent = { event -> dispatch(event) },
                    bccEmail = state.value.bccEmail,
                    useSignature = state.value.useSignature,
                    saveActionEnabled = saveActionEnabled,
                    signature = state.value.signature,
                    signatureLocations = state.value.signatureLocations,
                    selectedSignatureLocations = state.value.selectedSignatureLocations,
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

@Suppress("LongMethod", "LongParameterList")
@Composable
fun AccountSetupCompositionScreen(
    senderName: String,
    senderEmail: String,
    bccEmail: String,
    useSignature: Boolean,
    signature: String,
    saveActionEnabled: Boolean,
    signatureLocations: PersistentList<Pair<Int, String>>,
    selectedSignatureLocations: Pair<Int, String>,
    onEvent: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
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
            )
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
            ) {
                TextInput(
                    text = senderName,
                    onTextChange = { onEvent(Event.SenderNameChange(it)) },
                    label = stringResource(id = R.string.account_settings_name_label),
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                )
                TextFieldOutlinedEmailAddress(
                    value = senderEmail,
                    onValueChange = { onEvent(Event.SenderEmailChange(it)) },
                    label = stringResource(id = R.string.account_settings_email_label),
                    modifier = Modifier
                        .padding(horizontal = MainTheme.spacings.double)
                        .fillMaxWidth(),
                )
                TextFieldOutlinedEmailAddress(
                    value = bccEmail,
                    onValueChange = { onEvent(Event.BccEmailChange(it)) },
                    label = stringResource(id = R.string.account_settings_always_bcc_label),
                    modifier = Modifier
                        .padding(horizontal = MainTheme.spacings.double)
                        .fillMaxWidth(),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            enabled = true,
                            onClick = {
                                onEvent(Event.UseSignatureChange(!useSignature))
                            },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = useSignature, onCheckedChange = { onEvent(Event.UseSignatureChange(it)) })
                    TextBodySmall(text = stringResource(R.string.account_settings_signature_use_label))
                }
                if (useSignature) {
                    TextFieldOutlined(
                        label = stringResource(id = R.string.account_settings_signature_label),
                        value = signature,
                        onValueChange = { onEvent(Event.SignatureChange(it)) },
                        modifier = Modifier
                            .padding(horizontal = MainTheme.spacings.double)
                            .fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(MainTheme.spacings.half))
                    TextBodyLarge(
                        text = stringResource(R.string.account_settings_signature__location_label),
                        modifier = Modifier.padding(horizontal = MainTheme.spacings.double),
                    )

                    RadioGroup(
                        onClick = { onEvent(Event.SignatureLocationChange(it)) },
                        options = signatureLocations,
                        optionTitle = { it.second },
                        selectedOption = selectedSignatureLocations,
                        modifier = Modifier.padding(horizontal = MainTheme.spacings.default),
                    )
                }
            }
        }
    }
}
