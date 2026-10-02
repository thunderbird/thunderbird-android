package net.thunderbird.feature.account.settings.impl.domain.usecase

import kotlinx.coroutines.flow.firstOrNull
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.core.android.account.LegacyAccountRepository
import net.thunderbird.core.android.account.MessageFormat
import net.thunderbird.core.android.account.QuoteStyle
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.AccountSettingError
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UpdateSendingMailSettingsCommand
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase

@Suppress("CyclomaticComplexMethod", "LongMethod")
internal class UpdateSendingMailSettings(
    private val repository: LegacyAccountRepository,
) : UseCase.UpdateSendingMailSettings {
    override suspend fun invoke(
        accountId: AccountId,
        command: UpdateSendingMailSettingsCommand,
    ): Outcome<Unit, AccountSettingError> {
        val account = repository.getById(accountId)
            .firstOrNull()
            ?: return Outcome.failure(
                AccountSettingError.NotFound(
                    "Account not found",
                ),
            )

        val updatedAccount = when (command) {
            is UpdateSendingMailSettingsCommand.UpdateMessageFormat -> {
                account.copy(
                    messageFormat = when (command.value) {
                        "TEXT" -> MessageFormat.TEXT
                        "HTML" -> MessageFormat.HTML
                        "AUTO" -> MessageFormat.AUTO
                        else -> error("Invalid Message format value: ${command.value}")
                    },
                )
            }

            is UpdateSendingMailSettingsCommand.UpdateAlwaysShowCcBcc -> {
                account.copy(
                    isAlwaysShowCcBcc = command.value,
                )
            }

            is UpdateSendingMailSettingsCommand.UpdateReadReceipt -> {
                account.copy(
                    isMessageReadReceipt = command.value,
                )
            }

            is UpdateSendingMailSettingsCommand.UpdateReplyQuotingStyle -> {
                account.copy(
                    quoteStyle = when (command.value) {
                        "PREFIX" -> QuoteStyle.PREFIX
                        "HEADER" -> QuoteStyle.HEADER
                        else -> error("Invalid quote style value: ${command.value}")
                    },
                )
            }

            is UpdateSendingMailSettingsCommand.UpdateQuoteMessageWhenReplying -> {
                account.copy(
                    isDefaultQuotedTextShown = command.value,
                )
            }

            is UpdateSendingMailSettingsCommand.UpdateReplyAfterQuotedText -> {
                account.copy(
                    isReplyAfterQuote = command.value,
                )
            }

            is UpdateSendingMailSettingsCommand.UpdateStripSignatureOnReply -> {
                account.copy(
                    isStripSignature = command.value,
                )
            }

            is UpdateSendingMailSettingsCommand.UpdateQuotedTextPrefix -> {
                account.copy(
                    quotePrefix = command.value,
                )
            }

            is UpdateSendingMailSettingsCommand.UpdateUploadSentMessages -> {
                account.copy(
                    isUploadSentMessages = command.value,
                )
            }
        }

        repository.update(updatedAccount)

        return Outcome.success(Unit)
    }
}
