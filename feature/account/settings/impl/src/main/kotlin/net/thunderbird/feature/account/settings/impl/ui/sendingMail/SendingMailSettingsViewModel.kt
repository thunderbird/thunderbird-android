package net.thunderbird.feature.account.settings.impl.ui.sendingMail

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.thunderbird.components.core.outcome.handle
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.ui.contract.mvi.BaseViewModel
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.AccountSettingError
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase
import net.thunderbird.feature.account.settings.impl.ui.sendingMail.SendingMailSettingContract.Effect
import net.thunderbird.feature.account.settings.impl.ui.sendingMail.SendingMailSettingContract.Event
import net.thunderbird.feature.account.settings.impl.ui.sendingMail.SendingMailSettingContract.State

private const val TAG = "SendingMailSettingsViewModel"
internal class SendingMailSettingsViewModel(
    private val accountId: AccountId,
    private val logger: Logger,
    private val getAccountName: UseCase.GetAccountName,
    private val getLegacyAccount: UseCase.GetLegacyAccount,
    private val updateSendingMailSettings: UseCase.UpdateSendingMailSettings,
    private val optionsMapper: SendingMailSettingsOptionsMapper,
    initialState: State = State(
        messageFormat = optionsMapper.defaultMessageFormatOption(),
        alwaysShowCcBcc = false,
        readReceipt = false,
        replyQuotingStyle = optionsMapper.defaultQuoteStyleOption(),
        quoteMessageWhenReplying = false,
        replyAfterQuotedText = false,
        stripSignatureOnReply = false,
        quotedTextPrefix = ">",
        uploadSentMessages = false,
    ),
) : BaseViewModel<State, Event, Effect>(initialState = initialState) {

    init {
        observeAccountName()
        observerSendingMailSettings()
    }

    override fun event(event: Event) {
        when (event) {
            is Event.onBackPressed -> {
                emitEffect(Effect.NavigateBack)
            }

            is Event.OnMessageFormatChange -> {
                viewModelScope.launch {
                    updateSendingMailSettings(
                        accountId = accountId,
                        command = AccountSettingsDomainContract
                            .UpdateSendingMailSettingsCommand.UpdateMessageFormat(
                                event.messageFormat.id,
                            ),
                    )
                }
            }

            is Event.OnAlwaysShowCcBccToggle -> {
                viewModelScope.launch {
                    updateSendingMailSettings(
                        accountId = accountId,
                        command = AccountSettingsDomainContract
                            .UpdateSendingMailSettingsCommand.UpdateAlwaysShowCcBcc(
                                event.alwaysShowCcBcc,
                            ),
                    )
                }
            }

            is Event.OnReadReceiptToggle -> {
                viewModelScope.launch {
                    updateSendingMailSettings(
                        accountId = accountId,
                        command = AccountSettingsDomainContract
                            .UpdateSendingMailSettingsCommand.UpdateReadReceipt(
                                event.readReceipt,
                            ),
                    )
                }
            }

            is Event.OnReplyQuotingStyleChange -> {
                viewModelScope.launch {
                    updateSendingMailSettings(
                        accountId = accountId,
                        command = AccountSettingsDomainContract
                            .UpdateSendingMailSettingsCommand.UpdateReplyQuotingStyle(
                                event.replyQuotingStyle.id,
                            ),
                    )
                }
            }

            is Event.OnQuoteMessageWhenReplyingToggle -> {
                viewModelScope.launch {
                    updateSendingMailSettings(
                        accountId = accountId,
                        command = AccountSettingsDomainContract
                            .UpdateSendingMailSettingsCommand.UpdateQuoteMessageWhenReplying(
                                event.quoteMessageWhenReplying,
                            ),
                    )
                }
            }
            is Event.OnReplyAfterQuotedTextToggle -> {
                viewModelScope.launch {
                    updateSendingMailSettings(
                        accountId = accountId,
                        command = AccountSettingsDomainContract
                            .UpdateSendingMailSettingsCommand.UpdateReplyAfterQuotedText(
                                event.replyAfterQuotedText,
                            ),
                    )
                }
            }

            is Event.OnStripSignatureOnReplyToggle-> {
                viewModelScope.launch {
                    updateSendingMailSettings(
                        accountId = accountId,
                        command = AccountSettingsDomainContract
                            .UpdateSendingMailSettingsCommand.UpdateStripSignatureOnReply(
                                event.stripSignatureOnReply,
                            ),
                    )
                }
            }
            is Event.OnQuotedTextPrefixChange -> {
                viewModelScope.launch {
                    updateSendingMailSettings(
                        accountId = accountId,
                        command = AccountSettingsDomainContract
                            .UpdateSendingMailSettingsCommand.UpdateQuotedTextPrefix(
                                event.quotedTextPrefix,
                            ),
                    )

                }
            }

            is Event.OnUploadSentMessagesToggle -> {
                viewModelScope.launch {
                    updateSendingMailSettings(
                        accountId = accountId,
                        command = AccountSettingsDomainContract
                            .UpdateSendingMailSettingsCommand.UpdateUploadSentMessages(
                                event.uploadSentMessages,
                            ),
                    )
                }
            }
            is Event.OnCompositionDefaultsClick -> {
                emitEffect(Effect.NavigateToCompositionDefaults)
            }
            is Event.OnManageIdentitiesClick -> {
                emitEffect(Effect.NavigateToManageIdentities)
            }
            is Event.OnOutgoingServerClick -> {
                emitEffect(Effect.NavigateToOutGoingServerSettings)
            }
        }
    }

    private fun observeAccountName() {
        getAccountName(accountId)
            .onEach { outcome ->
                outcome.handle(
                    onSuccess = { updateState { state -> state.copy(subtitle = it) } },
                    onFailure = { handleError(it) },
                )
            }.launchIn(viewModelScope)
    }

    private fun handleError(error: AccountSettingError) {
        when (error) {
            is AccountSettingError.NotFound -> logger.error(tag = TAG, message = { error.message })
            is AccountSettingError.StorageError -> logger.error(tag = TAG, message = { error.message })
            is AccountSettingError.UnsupportedFormat -> logger.error(tag = TAG, message = { error.message })
        }
    }

    private fun observerSendingMailSettings() {
        viewModelScope.launch {
            getLegacyAccount(accountId).handle(
                onSuccess = {
                    val messageFormat = optionsMapper.messageFormatOption(it.messageFormat.name)
                    val alwaysShowCcBcc = it.isAlwaysShowCcBcc
                    val readReceipt = it.isMessageReadReceipt
                    val replyQuotingStyle = optionsMapper.quoteStyleOption(it.quoteStyle.name)
                    val quoteMessageWhenReplying = it.isDefaultQuotedTextShown
                    val replyAfterQuotedText = it.isReplyAfterQuote
                    val stripSignatureOnReply = it.isStripSignature
                    val quotedTextPrefix = it.quotePrefix ?: ""
                    val uploadSentMessages = it.isUploadSentMessages

                    updateState {
                        state -> state.copy(
                            messageFormat = messageFormat,
                            alwaysShowCcBcc = alwaysShowCcBcc,
                            readReceipt = readReceipt,
                            replyQuotingStyle = replyQuotingStyle,
                            quoteMessageWhenReplying = quoteMessageWhenReplying,
                            replyAfterQuotedText = replyAfterQuotedText,
                            stripSignatureOnReply = stripSignatureOnReply,
                            quotedTextPrefix = quotedTextPrefix,
                            uploadSentMessages = uploadSentMessages,
                        )
                    }

                },
                onFailure = {
                    handleError(it)
                },
            )
        }
    }
}
