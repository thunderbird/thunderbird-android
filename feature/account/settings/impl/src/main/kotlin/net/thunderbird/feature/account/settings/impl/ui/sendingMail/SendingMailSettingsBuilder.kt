package net.thunderbird.feature.account.settings.impl.ui.sendingMail

import kotlinx.collections.immutable.toImmutableList
import net.thunderbird.core.common.resources.StringsResourceManager
import net.thunderbird.core.ui.setting.Setting
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.SettingValue.Select.SelectOption
import net.thunderbird.core.ui.setting.Settings
import net.thunderbird.feature.account.settings.R

@Suppress("TooManyFunctions")
internal class SendingMailSettingsBuilder(
    private val resources: StringsResourceManager,
    private val optionMapper: SendingMailSettingsOptionsMapper,
) : SendingMailSettingContract.SettingsBuilder {
    override fun buildSettings(
        state: SendingMailSettingContract.State,
        onEvent: (SendingMailSettingContract.Event) -> Unit,
    ): Settings {
        val settings = mutableListOf<Setting>()
        settings += compositionDefaults(onEvent)
        settings += manageIdentities(onEvent)
        settings += messageFormat(state.messageFormat)
        settings += alwaysShowCcBcc(state.alwaysShowCcBcc)
        settings += readReceipt(state.readReceipt)
        settings += replyQuotingStyle(state.replyQuotingStyle)
        settings += quoteMessageWhenReplying(state.quoteMessageWhenReplying)
        settings += replyAfterQuotedText(state.replyAfterQuotedText)
        settings += stripSignatureOnReply(state.stripSignatureOnReply)
        settings += quotePrefix(state.quotedTextPrefix)
        settings += uploadSentMessages(state.uploadSentMessages)
        settings += outgoingServer(onEvent)
        return settings.toImmutableList()
    }

    fun compositionDefaults(onEvent: (SendingMailSettingContract.Event) -> Unit): Setting {
        return SettingValue.ActionText(
            id = SendingMailSettingsId.COMPOSITION_DEFAULT,
            title = { resources.stringResource(R.string.account_settings_composition_label) },
            description = { resources.stringResource(R.string.account_settings_composition_summary) },
            icon = { null },
            value = "",
            onClick = {
                onEvent(SendingMailSettingContract.Event.OnCompositionDefaultsClick)
            },
        )
    }

    fun manageIdentities(onEvent: (SendingMailSettingContract.Event) -> Unit): Setting {
        return SettingValue.ActionText(
            id = SendingMailSettingsId.MANAGE_IDENTITIES,
            title = { resources.stringResource(R.string.account_settings_identities_label) },
            description = { resources.stringResource(R.string.account_settings_identities_summary) },
            icon = { null },
            value = "",
            onClick = {
                onEvent(SendingMailSettingContract.Event.OnManageIdentitiesClick)
            },
        )
    }

    fun messageFormat(value: SelectOption): Setting {
        return SettingValue.Select(
            id = SendingMailSettingsId.MESSAGE_FORMAT,
            title = { resources.stringResource(R.string.account_settings_message_format_label) },
            description = { null },
            icon = { null },
            displayValueAsSecondaryText = true,
            value = value,
            options = optionMapper.messageFormatOptions(),
        )
    }

    fun alwaysShowCcBcc(value: Boolean): Setting = SettingValue.Switch(
        id = SendingMailSettingsId.ALWAYS_SHOW_CC_BCC,
        title = { resources.stringResource(R.string.account_settings_always_show_cc_bcc_label) },
        description = { null },
        value = value,
    )

    fun readReceipt(value: Boolean): Setting = SettingValue.Switch(
        id = SendingMailSettingsId.READ_RECEIPT,
        title = { resources.stringResource(R.string.account_settings_message_read_receipt_label) },
        description = { resources.stringResource(R.string.account_settings_message_read_receipt_summary) },
        value = value,
    )

    fun replyQuotingStyle(value: SelectOption): Setting {
        return SettingValue.Select(
            id = SendingMailSettingsId.REPLY_QUOTING_STYLE,
            title = { resources.stringResource(R.string.account_settings_quote_style_label) },
            description = { null },
            icon = { null },
            displayValueAsSecondaryText = true,
            value = value,
            options = optionMapper.quoteStyleOptions(),
        )
    }

    fun quoteMessageWhenReplying(value: Boolean): Setting = SettingValue.Switch(
        id = SendingMailSettingsId.QUOTE_MESSAGE_WHEN_REPLYING,
        title = { resources.stringResource(R.string.account_settings_default_quoted_text_shown_label) },
        description = { resources.stringResource(R.string.account_settings_default_quoted_text_shown_summary) },
        value = value,
    )

    fun replyAfterQuotedText(value: Boolean): Setting = SettingValue.Switch(
        id = SendingMailSettingsId.REPLY_AFTER_QUOTED_TEXT,
        title = { resources.stringResource(R.string.account_settings_reply_after_quote_label) },
        description = { resources.stringResource(R.string.account_settings_default_quoted_text_shown_summary) },
        value = value,
    )

    fun stripSignatureOnReply(value: Boolean): Setting = SettingValue.Switch(
        id = SendingMailSettingsId.STRIP_SIGNATURE_ON_REPLY,
        title = { resources.stringResource(R.string.account_settings_strip_signature_label) },
        description = { resources.stringResource(R.string.account_settings_strip_signature_summary) },
        value = value,
    )

    fun quotePrefix(value: String): Setting = SettingValue.Text(
        id = SendingMailSettingsId.QUOTED_TEXT_PREFIX,
        title = { resources.stringResource(R.string.account_settings_quote_prefix_label) },
        description = { null },
        icon = { null },
        value = value,
    )

    fun uploadSentMessages(value: Boolean): Setting = SettingValue.Switch(
        id = SendingMailSettingsId.UPLOAD_SENT_MESSAGES,
        title = { resources.stringResource(R.string.account_settings_upload_sent_messages_label) },
        description = { resources.stringResource(R.string.account_settings_upload_sent_messages_summary) },
        value = value,
    )

    fun outgoingServer(onEvent: (SendingMailSettingContract.Event) -> Unit): Setting {
        return SettingValue.ActionText(
            id = SendingMailSettingsId.OUTGOING_SERVER,
            title = { resources.stringResource(R.string.account_settings_outgoing_label) },
            description = { resources.stringResource(R.string.account_settings_outgoing_summary) },
            icon = { null },
            value = "",
            onClick = {
                onEvent(SendingMailSettingContract.Event.OnOutgoingServerClick)
            },
        )
    }
}
