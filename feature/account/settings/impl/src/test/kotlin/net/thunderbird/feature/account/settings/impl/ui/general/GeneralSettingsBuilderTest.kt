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
import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.featureflag.FeatureFlagResult
import net.thunderbird.core.ui.setting.Setting
import net.thunderbird.core.ui.setting.SettingDecoration
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.validation.input.IntegerInputField
import net.thunderbird.core.validation.input.StringInputField
import net.thunderbird.feature.account.avatar.Avatar
import net.thunderbird.feature.account.settings.AccountSettingsFeatureFlags
import net.thunderbird.feature.account.settings.impl.domain.usecase.FakeGeneralResourceProvider
import net.thunderbird.feature.account.settings.impl.domain.usecase.FakeMonogramCreator

internal class GeneralSettingsBuilderTest {

    private val resources = FakeGeneralResourceProvider()
    private val monogramCreator = FakeMonogramCreator()

    @Test
    fun `should always include profile, name and color settings`() {
        // Arrange
        val builder = GeneralSettingsBuilder(
            resources = resources,
            provider = enabled(false),
            monogramCreator = monogramCreator,
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
            provider = enabled(true),
            monogramCreator = monogramCreator,
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
            it.isInstanceOf<SettingValue.CompactSelectSingleOption<*>>()
            it.prop(Setting::id).isEqualTo(GeneralSettingId.AVATAR)
        }

        // Arrange disabled
        val builderDisabled = GeneralSettingsBuilder(
            resources = resources,
            provider = enabled(false),
            monogramCreator = monogramCreator,
        )

        // Act
        val settingsWithout = builderDisabled.build(state)

        // Assert
        assertThat(settingsWithout).none {
            it.isInstanceOf<SettingValue.CompactSelectSingleOption<*>>()
            it.prop(Setting::id).isEqualTo(GeneralSettingId.AVATAR)
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

    private fun assertSelectedAvatarOption(
        avatar: Avatar?,
        expectedOptionId: String,
    ) {
        // Assert
        val builder = GeneralSettingsBuilder(
            resources = resources,
            provider = enabled(true),
            monogramCreator = monogramCreator,
        )
        val state = GeneralSettingsContract.State(
            name = StringInputField(value = "Thunderbird"),
            color = IntegerInputField(value = 0),
            avatar = avatar,
        )

        // Act
        val settings = builder.build(state)
        val avatarSetting = settings.firstOrNull { it.id == GeneralSettingId.AVATAR }

        // Assert
        assertThat(avatarSetting).isNotNull()
        val compact = avatarSetting as SettingValue.CompactSelectSingleOption<*>
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
}
