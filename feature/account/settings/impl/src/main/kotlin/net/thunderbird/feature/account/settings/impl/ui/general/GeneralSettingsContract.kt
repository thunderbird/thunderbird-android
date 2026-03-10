package net.thunderbird.feature.account.settings.impl.ui.general

import androidx.compose.runtime.Stable
import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import com.eygraber.uri.Uri
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.ui.setting.Settings
import net.thunderbird.core.validation.input.IntegerInputField
import net.thunderbird.core.validation.input.StringInputField
import net.thunderbird.feature.account.avatar.Avatar
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.ValidateAccountNameError
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.ValidateMonogramError

internal interface GeneralSettingsContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    @Stable
    data class State(
        val subtitle: String? = null,
        val name: StringInputField = StringInputField(),
        val color: IntegerInputField = IntegerInputField(),
        val avatarType: AvatarType = AvatarType.MONOGRAM,
        val avatar: Avatar? = null,
        val avatarMonogram: StringInputField = StringInputField(),
    )

    enum class AvatarType {
        MONOGRAM,
        IMAGE,
        ICON,
    }

    sealed interface Event {
        data class OnNameChange(val name: String) : Event
        data class OnColorChange(val color: Int) : Event
        data class OnAvatarChange(val avatar: Avatar) : Event
        data object OnSelectAvatarImageClick : Event
        data class OnAvatarImagePicked(val uri: Uri) : Event

        data object OnBackPressed : Event
    }

    sealed interface Effect {
        object NavigateBack : Effect
        object OpenAvatarImagePicker : Effect
    }

    interface Validator {
        fun validateName(name: String): Outcome<Unit, ValidateAccountNameError>
        fun validateMonogram(monogram: String): Outcome<Unit, ValidateMonogramError>
    }

    fun interface SettingsBuilder {
        fun build(
            state: State,
            onEvent: (Event) -> Unit,
        ): Settings
    }
}
