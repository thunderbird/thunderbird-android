package net.thunderbird.feature.account.settings.impl.ui.sendingMail

import androidx.compose.runtime.Stable
import net.thunderbird.core.ui.contract.mvi.UnidirectionalViewModel
import net.thunderbird.core.ui.setting.SettingValue.Select.SelectOption
import net.thunderbird.core.ui.setting.Settings

interface SendingMailSettingContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    @Stable
    data class State(
        val subtitle: String? = null,
        val messageFormat: SelectOption,
        val alwaysShowCcBcc: Boolean = false,
        val readReceipt: Boolean = false,
        val replyQuotingStyle: SelectOption,
        val quoteMessageWhenReplying: Boolean = false,
        val replyAfterQuotedText: Boolean = false,
        val stripSignatureOnReply: Boolean = false,
        val quotedTextPrefix: String,
        val uploadSentMessages: Boolean = false,

    )

    sealed interface Event {
        data object OnBackPressed : Event
        data class OnMessageFormatChange(val messageFormat: SelectOption) : Event
        data class OnAlwaysShowCcBccToggle(val alwaysShowCcBcc: Boolean) : Event
        data class OnReadReceiptToggle(val readReceipt: Boolean) : Event
        data class OnReplyQuotingStyleChange(val replyQuotingStyle: SelectOption) : Event
        data class OnQuoteMessageWhenReplyingToggle(val quoteMessageWhenReplying: Boolean) : Event
        data class OnReplyAfterQuotedTextToggle(val replyAfterQuotedText: Boolean) : Event
        data class OnStripSignatureOnReplyToggle(val stripSignatureOnReply: Boolean) : Event
        data class OnQuotedTextPrefixChange(val quotedTextPrefix: String) : Event
        data class OnUploadSentMessagesToggle(val uploadSentMessages: Boolean) : Event
        data object OnCompositionDefaultsClick : Event
        data object OnManageIdentitiesClick : Event
        data object OnOutgoingServerClick : Event
    }

    sealed interface Effect {
        object NavigateBack : Effect
        object NavigateToCompositionDefaults : Effect
        object NavigateToManageIdentities : Effect
        object NavigateToOutGoingServerSettings : Effect
    }

    interface SettingsBuilder {
        fun buildSettings(
            state: State,
            onEvent: (Event) -> Unit,
        ): Settings
    }
}
