package com.fsck.k9.activity.setup

import com.fsck.k9.EmailAddressValidator
import com.fsck.k9.activity.setup.AccountSetupCompositionContract.Effect
import com.fsck.k9.activity.setup.AccountSetupCompositionContract.Event
import com.fsck.k9.activity.setup.AccountSetupCompositionContract.State
import com.fsck.k9.ui.R
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.common.resources.StringsResourceManager
import net.thunderbird.core.ui.contract.mvi.BaseViewModel

class AccountSetupCompositionViewModel(
    private val legacyAccountManager: LegacyAccountManager,
    private val resources: StringsResourceManager,
    private val emailAddressValidator: EmailAddressValidator,
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
                    emitEffect(Effect.ToggleSaveButtonEnabled(true))
                } else {
                    emitEffect(Effect.ToggleSaveButtonEnabled(false))
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
                state.copy(signature = account.signature ?: "")
            }

            is Event.SavePressed -> {
                saveAccount()
                emitEffect(Effect.DoneUpdatingAccount)
            }

            is Event.BackPressed -> {
                emitEffect(Effect.Back)
            }
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
            )
        }
    }

    private fun saveAccount() {
        legacyAccountManager.saveAccount(account)
    }
}
