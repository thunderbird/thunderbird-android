package com.fsck.k9.activity.setup

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.fsck.k9.activity.setup.AccountSetupCompositionContract.Effect
import com.fsck.k9.activity.setup.AccountSetupCompositionContract.Event
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.BaseActivity
import com.fsck.k9.view.MessageWebView
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import net.thunderbird.components.ui.bolt.atom.DividerHorizontal
import net.thunderbird.components.ui.bolt.atom.RadioGroup
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.button.ButtonIcon
import net.thunderbird.components.ui.bolt.atom.button.ButtonText
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.atom.text.TextBodyLarge
import net.thunderbird.components.ui.bolt.atom.textfield.TextFieldOutlined
import net.thunderbird.components.ui.bolt.atom.textfield.TextFieldOutlinedEmailAddress
import net.thunderbird.components.ui.bolt.molecule.input.CheckboxInput
import net.thunderbird.components.ui.bolt.molecule.input.TextInput
import net.thunderbird.components.ui.bolt.organism.TopAppBar
import net.thunderbird.components.ui.bolt.template.Scaffold
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.core.android.webkit.WebViewConfig
import net.thunderbird.core.ui.contract.mvi.observe
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import net.thunderbird.feature.mail.message.composer.signature.configureForSignaturePreview
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

private val PREVIEW_DEBOUNCE = 300.milliseconds

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

@Composable
private fun SignatureContent(
    state: AccountSetupCompositionContract.State,
    onEvent: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(BoltTheme.spacings.double),
        modifier = modifier,
    ) {
        CheckboxInput(
            text = stringResource(R.string.account_settings_signature_use_label),
            checked = state.useSignature,
            onCheckedChange = { onEvent(Event.UseSignatureChange(it)) },
        )

        if (state.useSignature) {
            CheckboxInput(
                text = stringResource(R.string.account_settings_signature_is_html_label),
                checked = state.saveSignatureAsHtml,
                onCheckedChange = { onEvent(Event.OnFormatSignatureAsHtmlCheck(it)) },
            )
            TextFieldOutlined(
                isSingleLine = false,
                label = stringResource(
                    id = if (state.saveSignatureAsHtml) {
                        R.string.account_settings_signature_html_label
                    } else {
                        R.string.account_settings_signature_label
                    },
                ),
                value = state.signature,
                onValueChange = { onEvent(Event.SignatureChange(it)) },
                modifier = Modifier
                    .padding(horizontal = BoltTheme.spacings.double)
                    .fillMaxWidth(),
            )
            AnimatedVisibility(
                visible = state.signature.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .padding(horizontal = BoltTheme.spacings.double)
                    .align(Alignment.CenterHorizontally),
            ) {
                ButtonText(
                    text = stringResource(R.string.account_settings_signature_clear_label),
                    onClick = { onEvent(Event.SignatureChange("")) },
                )
            }

            SignaturePreview(
                signaturePreviewHtmlText = state.signaturePreviewHtmlText,
                webViewConfig = state.webViewConfig,
                isHtmlSignature = state.saveSignatureAsHtml,
                modifier = Modifier.padding(horizontal = BoltTheme.spacings.double),
            )

            SignatureLocation(state, onEvent)
        }
    }
}

@Composable
private fun SignatureLocation(
    state: AccountSetupCompositionContract.State,
    onEvent: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(BoltTheme.spacings.double),
        modifier = modifier,
    ) {
        TextBodyLarge(
            text = stringResource(R.string.account_settings_signature__location_label),
            modifier = Modifier.padding(horizontal = BoltTheme.spacings.double),
        )

        RadioGroup(
            onClick = { onEvent(Event.SignatureLocationChange(it)) },
            options = state.signatureLocations,
            optionTitle = { it.second },
            selectedOption = state.selectedSignatureLocations,
            modifier = Modifier.padding(horizontal = BoltTheme.spacings.default),
        )
    }
}

@Composable
private fun SignaturePreview(
    signaturePreviewHtmlText: String?,
    webViewConfig: WebViewConfig?,
    isHtmlSignature: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isHtmlSignature && signaturePreviewHtmlText?.isNotEmpty() == true && webViewConfig != null,
    ) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(BoltTheme.spacings.double),
        ) {
            TextBodyLarge(text = stringResource(R.string.account_settings_signature_preview))
            SignatureHtmlPreview(
                signaturePreviewHtmlText = requireNotNull(signaturePreviewHtmlText) {
                    "Can't preview signature, wrong state detected. signaturePreviewHtmlText is null"
                },
                webViewConfig = requireNotNull(webViewConfig) {
                    "Can't preview signature, wrong state detected. webViewConfig is null."
                },
                modifier = Modifier.fillMaxWidth(),
            )
            DividerHorizontal()
        }
    }
}

/**
 * Renders the signature the way the composer and the recipient's mail client do: in a WebView, from
 * the same sanitized document the outgoing message is built from.
 */
@Composable
private fun SignatureHtmlPreview(
    signaturePreviewHtmlText: String,
    webViewConfig: WebViewConfig,
    modifier: Modifier = Modifier,
) {
    if (LocalInspectionMode.current) {
        TextBodyLarge(text = signaturePreviewHtmlText, modifier = modifier)
        return
    }

    // Reloading the WebView on every keystroke makes editing a long signature stutter.
    var debouncedSignature by remember { mutableStateOf(signaturePreviewHtmlText) }
    LaunchedEffect(signaturePreviewHtmlText) {
        delay(PREVIEW_DEBOUNCE)
        debouncedSignature = signaturePreviewHtmlText
    }

    // The preview lives in a scrolling column, so it is sized to its content rather than left to
    // scroll on its own. Images that load after the page finishes can leave the height slightly
    // short until the next edit.
    var contentHeight by remember { mutableStateOf(0.dp) }
    val animatedContentHeight by animateDpAsState(contentHeight)

    AndroidView(
        factory = { context ->
            MessageWebView(context).apply {
                configureForSignaturePreview(webViewConfig)
            }
        },
        update = { webView ->
            webView.displayHtmlContentWithInlineAttachments(debouncedSignature, null) {
                contentHeight = webView.contentHeight.dp
            }
        },
        modifier = modifier.height(animatedContentHeight),
    )
}
