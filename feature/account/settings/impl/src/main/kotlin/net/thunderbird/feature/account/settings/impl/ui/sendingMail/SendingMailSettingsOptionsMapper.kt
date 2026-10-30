package net.thunderbird.feature.account.settings.impl.ui.sendingMail

import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.core.common.resources.StringsResourceManager
import net.thunderbird.core.ui.setting.SettingValue.Select.SelectOption
import net.thunderbird.feature.account.settings.R

internal class SendingMailSettingsOptionsMapper(
    private val resources: StringsResourceManager,
) {

    private val messageFormatOptions = persistentListOf(
        SelectOption("TEXT") {
            resources.stringResource(
                R.string.account_settings_message_format_text,
            )
        },
        SelectOption("HTML") {
            resources.stringResource(
                R.string.account_settings_message_format_html,
            )
        },
        SelectOption("AUTO") {
            resources.stringResource(
                R.string.account_settings_message_format_auto,
            )
        },
    )

    private val messageFormatOptionById = messageFormatOptions.associateBy { it.id }

    private val defaultMessageFormatOption =
        messageFormatOptionById.getValue(DEFAULT_MESSAGE_FORMAT_OPTION_ID)

    fun messageFormatOptions() = messageFormatOptions

    fun defaultMessageFormatOption(): SelectOption =
        defaultMessageFormatOption

    fun messageFormatOption(value: String): SelectOption =
        messageFormatOptionById[value] ?: defaultMessageFormatOption

    private companion object {
        const val DEFAULT_MESSAGE_FORMAT_OPTION_ID = "AUTO"
        const val DEFAULT_QUOTE_STYLE_OPTION_ID = "PREFIX"
    }

    private val quoteStyleOptions = persistentListOf(
        SelectOption("PREFIX") {
            resources.stringResource(
                R.string.account_settings_quote_style_prefix,
            )
        },
        SelectOption("HEADER") {
            resources.stringResource(
                R.string.account_settings_quote_style_header,
            )
        },
    )

    private val quoteStyleOptionsById = quoteStyleOptions.associateBy { it.id }

    private val defaultQuoteStyleOption =
        quoteStyleOptionsById.getValue(DEFAULT_QUOTE_STYLE_OPTION_ID)

    fun quoteStyleOptions() = quoteStyleOptions

    fun defaultQuoteStyleOption(): SelectOption =
        defaultQuoteStyleOption

    fun quoteStyleOption(value: String): SelectOption =
        quoteStyleOptionsById[value] ?: defaultQuoteStyleOption
}
