package com.fsck.k9.activity.setup

import com.fsck.k9.EmailAddressValidator
import com.fsck.k9.activity.account.identity.LegacyIdentitySignatureWebViewConfigurator
import com.fsck.k9.activity.setup.AccountSetupCompositionContract.Effect
import com.fsck.k9.activity.setup.AccountSetupCompositionContract.Effect.Back
import com.fsck.k9.activity.setup.AccountSetupCompositionContract.Effect.DoneUpdatingAccount
import com.fsck.k9.activity.setup.AccountSetupCompositionContract.Effect.ToggleSaveButtonEnabled
import com.fsck.k9.activity.setup.AccountSetupCompositionContract.Event
import com.fsck.k9.activity.setup.AccountSetupCompositionContract.State
import com.fsck.k9.ui.R
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.common.resources.StringsResourceManager
import net.thunderbird.core.ui.contract.mvi.BaseViewModel

internal class AccountSetupCompositionViewModel(
    private val legacyAccountManager: LegacyAccountManager,
    private val resources: StringsResourceManager,
    private val emailAddressValidator: EmailAddressValidator,
    private val legacyIdentitySignatureWebViewConfigurator: LegacyIdentitySignatureWebViewConfigurator,
    accountUuid: String,
) : BaseViewModel<State, Event, Effect>(initialState = State.EMPTY) {
    private val signatureLocations = persistentListOf(
        Pair(1, resources.stringResource(R.string.account_settings_signature__location_before_quoted_text)),
        Pair(2, resources.stringResource(R.string.account_settings_signature__location_after_quoted_text)),
    )

    private var account: LegacyAccount = legacyAccountManager.getAccount(accountUuid) ?: error("Couldn't find account")

    init {
        loadState()
    }

    override fun event(event: Event) {
        when (event) {
            is Event.SenderNameChange -> updateState { state ->
                account = account.copy(senderName = event.name)
                state.copy(senderName = account.senderName ?: "")
            }

            is Event.SenderEmailChange -> updateState { state ->
                account = account.copy(email = event.email)
                if (emailAddressValidator.isValidAddressOnly(event.email)) {
                    emitEffect(ToggleSaveButtonEnabled(true))
                } else {
                    emitEffect(ToggleSaveButtonEnabled(false))
                }
                state.copy(senderEmail = account.email)
            }

            is Event.BccEmailChange -> updateState { state ->
                account = account.copy(alwaysBcc = event.bccEmail.takeUnless { it.isBlank() })
                state.copy(bccEmail = account.alwaysBcc ?: "")
            }

            is Event.UseSignatureChange -> updateState { state ->
                account = account.copy(signatureUse = event.useSignature)
                state.copy(useSignature = account.signatureUse)
            }

            is Event.SignatureLocationChange -> updateState { state ->
                account = account.copy(isSignatureBeforeQuotedText = event.signatureLocation.first == 1)
                state.copy(selectedSignatureLocations = event.signatureLocation)
            }

            is Event.SignatureChange -> updateState { state ->
                account = account.copy(signature = event.signature)
                state.copy(
                    signature = account.signature ?: "",
                    signaturePreviewHtmlText = account.signature?.buildSignatureHtmlPreviewText(),
                )
            }

            is Event.SavePressed -> {
                saveAccount()
                emitEffect(DoneUpdatingAccount)
            }

            is Event.BackPressed -> {
                emitEffect(Back)
            }

            is Event.OnFormatSignatureAsHtmlCheck -> handleOnFormatSignatureAsHtmlCheck(event)
        }
    }

    private fun loadState() {
        updateState { state ->
            state.copy(
                senderName = account.senderName ?: "",
                senderEmail = account.email,
                bccEmail = account.alwaysBcc ?: "",
                useSignature = account.signatureUse,
                signature = account.signature ?: "",
                signatureLocations = signatureLocations,
                selectedSignatureLocations = if (account.isSignatureBeforeQuotedText) {
                    Pair(1, resources.stringResource(R.string.account_settings_signature__location_before_quoted_text))
                } else {
                    Pair(2, resources.stringResource(R.string.account_settings_signature__location_after_quoted_text))
                },
                saveSignatureAsHtml = account.signatureIsHtml,
                signaturePreviewHtmlText = account.signature?.buildSignatureHtmlPreviewText(),
                webViewConfig = legacyIdentitySignatureWebViewConfigurator.buildWebConfig(),
            )
        }
    }

    private fun saveAccount() {
        legacyAccountManager.saveAccount(account)
    }

    private fun handleOnFormatSignatureAsHtmlCheck(event: Event.OnFormatSignatureAsHtmlCheck) {
        account = account.copy(signatureIsHtml = event.checked)
        updateState { it.copy(saveSignatureAsHtml = event.checked) }
    }

    private fun String?.buildSignatureHtmlPreviewText(): String? =
        legacyIdentitySignatureWebViewConfigurator.buildSignatureHtmlPreviewText(signature = this)
}
