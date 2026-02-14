package net.thunderbird.feature.account.settings.impl.ui.readingMail

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import assertk.assertions.prop
import kotlin.test.Test
import net.thunderbird.core.android.account.ShowPictures
import net.thunderbird.core.common.resources.StringsResourceManager
import net.thunderbird.core.ui.setting.Setting
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.SettingValue.Select.SelectOption
import net.thunderbird.feature.account.settings.R

internal class ReadingMailSettingsBuilderTest {

    private val resources = object : StringsResourceManager {
        override fun stringResource(resourceId: Int): String =
            "String for $resourceId"

        override fun stringResource(resourceId: Int, vararg formatArgs: Any?): String =
            stringResource(resourceId)
    }

    private val builder = ReadingMailSettingsBuilder(resources)

    @Test
    fun `should build show pictures and mark as read settings in order`() {
        val state = ReadingMailSettingsContract.State(
            showPictures = SelectOption(ShowPictures.ALWAYS.name) { "" },
            isMarkMessageAsReadOnView = true,
        )

        val settings = builder.build(state) {}

        assertThat(settings.size).isEqualTo(2)

        assertThat(settings[0]).all {
            isInstanceOf<SettingValue.Select>()
            prop(Setting::id).isEqualTo(ReadingMailSettingId.SHOW_PICTURES)
        }

        assertThat(settings[1]).all {
            isInstanceOf<SettingValue.Switch>()
            prop(Setting::id).isEqualTo(ReadingMailSettingId.MARKED_AS_READ_ON_VIEW)
        }
    }

    @Test
    fun `show pictures select should contain all enum options`() {
        val state = ReadingMailSettingsContract.State(
            showPictures = SelectOption(ShowPictures.NEVER.name) { "" },
            isMarkMessageAsReadOnView = false,
        )

        val settings = builder.build(state) {}
        val select = settings.first() as SettingValue.Select

        val optionIds = select.options.map { it.id }

        assertThat(optionIds).isEqualTo(
            listOf(
                ShowPictures.NEVER.name,
                ShowPictures.ALWAYS.name,
                ShowPictures.ONLY_FROM_CONTACTS.name,
            ),
        )
    }

    @Test
    fun `show pictures select should preserve selected value`() {
        val selected = SelectOption(ShowPictures.ONLY_FROM_CONTACTS.name) { "" }

        val state = ReadingMailSettingsContract.State(
            showPictures = selected,
            isMarkMessageAsReadOnView = false,
        )

        val settings = builder.build(state) {}
        val select = settings.first() as SettingValue.Select

        assertThat(select.value.id).isEqualTo(ShowPictures.ONLY_FROM_CONTACTS.name)
    }

    @Test
    fun `mark as read switch should preserve value`() {
        val state = ReadingMailSettingsContract.State(
            showPictures = SelectOption(ShowPictures.ALWAYS.name) { "" },
            isMarkMessageAsReadOnView = true,
        )

        val settings = builder.build(state) {}
        val switch = settings.last() as SettingValue.Switch

        assertThat(switch.value).isTrue()
    }

    @Test
    fun `select option titles should come from resource manager`() {
        val state = ReadingMailSettingsContract.State(
            showPictures = SelectOption(ShowPictures.ALWAYS.name) { "" },
            isMarkMessageAsReadOnView = false,
        )

        val settings = builder.build(state) {}
        val select = settings.first() as SettingValue.Select

        val neverTitle = select.options.first { it.id == ShowPictures.NEVER.name }.title()

        assertThat(neverTitle).isEqualTo(
            resources.stringResource(R.string.account_settings_show_pictures_never),
        )
    }
}
