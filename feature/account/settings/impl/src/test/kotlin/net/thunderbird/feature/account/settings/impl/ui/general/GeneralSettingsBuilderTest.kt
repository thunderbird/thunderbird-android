package net.thunderbird.feature.account.settings.impl.ui.general

import assertk.all
import assertk.assertThat
import assertk.assertions.any
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.none
import assertk.assertions.prop
import kotlin.test.Test
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.core.common.resources.StringsResourceManager
import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.featureflag.FeatureFlagResult
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.ui.setting.Setting
import net.thunderbird.core.ui.setting.SettingDecoration
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.validation.input.IntegerInputField
import net.thunderbird.core.validation.input.StringInputField
import net.thunderbird.feature.account.avatar.Avatar
import net.thunderbird.feature.account.settings.AccountSettingsFeatureFlags
import net.thunderbird.feature.account.settings.R
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.ValidateAccountNameError
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.ValidateMonogramError
import net.thunderbird.feature.account.settings.impl.domain.usecase.FakeMonogramCreator

internal class GeneralSettingsBuilderTest {

    private val resources = object : StringsResourceManager {
        override fun stringResource(resourceId: Int): String = "String for $resourceId"

        override fun stringResource(resourceId: Int, vararg formatArgs: Any?): String = stringResource(resourceId)
    }
    private val monogramCreator = FakeMonogramCreator()
    private val validator = object : GeneralSettingsContract.Validator {
        override fun validateName(name: String) = Outcome.success(Unit)
        override fun validateMonogram(monogram: String) = Outcome.success(Unit)
    }

    @Test
    fun `should always include profile, name and color settings`() {
        // Arrange
        val builder = GeneralSettingsBuilder(
            resources = resources,
            accountColors = ACCOUNT_COLORS,
            featureFlagProvider = enabled(false),
            monogramCreator = monogramCreator,
            validator = validator,
        )
        val state = GeneralSettingsContract.State(
            name = StringInputField(value = "Thunderbird"),
            color = IntegerInputField(value = 0x123456),
            avatar = null,
        )

        // Act
        val settings = builder.build(state)

        // Assert
        assertThat(settings[0]).all {
            isInstanceOf<SettingDecoration.Custom>()
            prop(Setting::id).isEqualTo(GeneralSettingId.PROFILE)
        }
        assertThat(settings[1]).all {
            isInstanceOf<SettingValue.Text>()
            prop(Setting::id).isEqualTo(GeneralSettingId.NAME)
        }
        assertThat(settings[2]).all {
            isInstanceOf<SettingValue.Color>()
            prop(Setting::id).isEqualTo(GeneralSettingId.COLOR)
        }
    }

    @Test
    fun `should include avatar setting only when feature flag is enabled`() {
        // Arrange enabled
        val builderEnabled = GeneralSettingsBuilder(
            resources = resources,
            accountColors = ACCOUNT_COLORS,
            featureFlagProvider = enabled(true),
            monogramCreator = monogramCreator,
            validator = validator,
        )
        val state = GeneralSettingsContract.State(
            name = StringInputField(value = "TB"),
            color = IntegerInputField(value = 0),
            avatar = null,
        )

        // Act
        val settingsWith = builderEnabled.build(state)

        // Assert
        assertThat(settingsWith).any {
            it.isInstanceOf<SettingValue.SegmentedButton<*>>()
            it.prop(Setting::id).isEqualTo(GeneralSettingId.AVATAR_OPTIONS)
        }

        // Arrange disabled
        val builderDisabled = GeneralSettingsBuilder(
            resources = resources,
            accountColors = ACCOUNT_COLORS,
            featureFlagProvider = enabled(false),
            monogramCreator = monogramCreator,
            validator = validator,
        )

        // Act
        val settingsWithout = builderDisabled.build(state)

        // Assert
        assertThat(settingsWithout).none {
            it.isInstanceOf<SettingValue.SegmentedButton<*>>()
            it.prop(Setting::id).isEqualTo(GeneralSettingId.AVATAR_OPTIONS)
        }
    }

    @Test
    fun `avatar selection should match provided avatar or default to monogram`() {
        // Monogram
        assertSelectedAvatarOption(
            avatar = Avatar.Monogram(value = "AB"),
            expectedOptionId = "avatar_monogram",
        )
        // Image
        assertSelectedAvatarOption(
            avatar = Avatar.Image(uri = "uri"),
            expectedOptionId = "avatar_image",
        )
        // Icon
        assertSelectedAvatarOption(
            avatar = Avatar.Icon(name = "icon"),
            expectedOptionId = "avatar_icon",
        )
        // Null defaults to monogram
        assertSelectedAvatarOption(
            avatar = null,
            expectedOptionId = "avatar_monogram",
        )
    }

    @Test
    fun `should include monogram setting when avatar is monogram and feature flag is enabled and use description`() {
        // Arrange
        val builder = GeneralSettingsBuilder(
            resources = resources,
            accountColors = ACCOUNT_COLORS,
            featureFlagProvider = enabled(true),
            monogramCreator = monogramCreator,
            validator = validator,
        )
        val state = GeneralSettingsContract.State(
            name = StringInputField(value = "Thunderbird"),
            color = IntegerInputField(value = 0),
            avatar = Avatar.Monogram("TB"),
        )

        // Act
        val settings = builder.build(state)

        // Assert
        val monogramSetting = settings.firstOrNull { it.id == GeneralSettingId.AVATAR_MONOGRAM }
        assertThat(monogramSetting).isNotNull()
        val textSetting = monogramSetting as SettingValue.Text
        assertThat(textSetting.value).isEqualTo("TB")
        assertThat(
            textSetting.description(),
        ).isEqualTo(resources.stringResource(R.string.account_settings_general_avatar_monogram_description))
    }

    @Test
    fun `should map validation errors to resource provider strings`() {
        // Arrange: validator that fails for both name and monogram
        val failingValidator = object : GeneralSettingsContract.Validator {
            override fun validateName(name: String): Outcome<Unit, ValidateAccountNameError> =
                Outcome.failure(ValidateAccountNameError.EmptyName)
            override fun validateMonogram(monogram: String): Outcome<Unit, ValidateMonogramError> =
                Outcome.failure(ValidateMonogramError.TooLongMonogram)
        }
        val builder = GeneralSettingsBuilder(
            resources = resources,
            accountColors = ACCOUNT_COLORS,
            featureFlagProvider = enabled(true),
            monogramCreator = monogramCreator,
            validator = failingValidator,
        )
        val state = GeneralSettingsContract.State(
            name = StringInputField(value = ""),
            color = IntegerInputField(value = 0),
            avatar = Avatar.Monogram("TB"),
        )

        // Act
        val settings = builder.build(state)
        val nameSetting = settings.first { it.id == GeneralSettingId.NAME } as SettingValue.Text
        val monogramSetting = settings.first { it.id == GeneralSettingId.AVATAR_MONOGRAM } as SettingValue.Text

        // Assert: name empty error and monogram too long error come from resource provider
        assertThat(nameSetting.validate("")).isEqualTo(
            resources.stringResource(R.string.account_settings_general_name_error_empty)
        )
        assertThat(monogramSetting.validate("TOO_LONG")).isEqualTo(
            resources.stringResource(R.string.account_settings_general_avatar_monogram_error_too_long)
        )
    }

    private fun assertSelectedAvatarOption(
        avatar: Avatar?,
        expectedOptionId: String,
    ) {
        // Assert
        val builder = GeneralSettingsBuilder(
            resources = resources,
            accountColors = ACCOUNT_COLORS,
            featureFlagProvider = enabled(true),
            monogramCreator = monogramCreator,
            validator = validator,
        )
        val state = GeneralSettingsContract.State(
            name = StringInputField(value = "Thunderbird"),
            color = IntegerInputField(value = 0),
            avatar = avatar,
        )

        // Act
        val settings = builder.build(state)
        val avatarSetting = settings.firstOrNull { it.id == GeneralSettingId.AVATAR_OPTIONS }

        // Assert
        assertThat(avatarSetting).isNotNull()
        val compact = avatarSetting as SettingValue.SegmentedButton<*>
        assertThat(compact.options.size).isEqualTo(3)
        assertThat(compact.value.id).isEqualTo(expectedOptionId)
    }

    private fun enabled(isEnabled: Boolean): FeatureFlagProvider = FeatureFlagProvider { key: FeatureFlagKey ->
        when (key) {
            AccountSettingsFeatureFlags.EnableAvatarCustomization -> {
                if (isEnabled) FeatureFlagResult.Enabled else FeatureFlagResult.Disabled
            }
            else -> FeatureFlagResult.Enabled
        }
    }

    private companion object {
        val ACCOUNT_COLORS = persistentListOf(0xFF0000, 0x00FF00, 0x0000FF)
    }
}
