package net.thunderbird.feature.account.settings.impl.ui.search

import assertk.all
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import kotlin.test.Test
import net.thunderbird.core.common.resources.StringsResourceManager
import net.thunderbird.core.ui.setting.SettingValue.Select
import net.thunderbird.core.ui.setting.SettingValue.Select.SelectOption
import net.thunderbird.feature.account.settings.R

internal class SearchSettingBuilderTest {

    private val resources = object : StringsResourceManager {
        override fun stringResource(resourceId: Int): String =
            "string_$resourceId"

        override fun stringResource(resourceId: Int, vararg formatArgs: Any?): String =
            "string_$resourceId"
    }

    private val builder = SearchSettingBuilder(resources)

    private fun state(selected: SelectOption = SelectOption("25") { "" }) =
        SearchSettingsContract.State(
            serverSearchLimit = selected,
        )

    @Test
    fun `should build server search limit setting with correct options`() {
        val settings = builder.build(state()) {}

        assertThat(settings).hasSize(1)

        val setting = settings.first()

        assertThat(setting)
            .isInstanceOf<Select>()
            .all {
                prop(Select::id)
                    .isEqualTo(SearchSettingId.SERVER_SEARCH_LIMIT)

                prop(Select::options)
                    .hasSize(ServerSearchLimit.values().size)

                prop(Select::options)
                    .transform { it.map { option -> option.id } }
                    .isEqualTo(
                        listOf("10", "25", "50", "100", "250", "500", "1000", "0"),
                    )
            }
    }

    @Test
    fun `should preserve selected value`() {
        val selected = SelectOption(ServerSearchLimit.HUNDRED.count.toString()) { "" }

        val settings = builder.build(state(selected)) {}

        val select = settings.first() as Select

        assertThat(select.value.id)
            .isEqualTo(ServerSearchLimit.HUNDRED.count.toString())
    }

    @Test
    fun `should contain ALL option`() {
        val settings = builder.build(state()) {}
        val select = settings.first() as Select

        val allOption = select.options.first { it.id == "0" }

        assertThat(allOption.id).isEqualTo("0")
    }

    @Test
    fun `should set correct title from resource`() {
        val settings = builder.build(state()) {}
        val select = settings.first() as Select

        val title = select.title()

        assertThat(title).isEqualTo(
            "string_${R.string.account_settings_remote_search_num_label}",
        )
    }
}
