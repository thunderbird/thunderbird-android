package net.thunderbird.feature.account.settings.impl.ui.fetchingMail

import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import assertk.assertions.prop
import kotlin.test.Test
import net.thunderbird.core.android.account.DeletePolicy
import net.thunderbird.core.android.account.Expunge
import net.thunderbird.core.common.resources.StringsResourceManager
import net.thunderbird.core.ui.setting.Setting
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.SettingValue.Select.SelectOption
import net.thunderbird.feature.account.settings.R

internal class FetchingMailSettingsBuilderTest {

    private val resources = object : StringsResourceManager {
        override fun stringResource(resourceId: Int): String =
            "String for $resourceId"

        override fun stringResource(resourceId: Int, vararg formatArgs: Any?): String =
            stringResource(resourceId)
    }

    private val builder = FetchingMailSettingsBuilder(resources)

    private fun createState(
        localFolderSize: String = "10",
        syncMessageFrom: String = "-1",
        fetchMessageUpTo: String = "1024",
        folderPollFrequency: String = "15",
        syncServerDeletions: Boolean = false,
        markAsReadWhenDeleted: Boolean = false,
        deletePolicy: DeletePolicy = DeletePolicy.NEVER,
        expunge: Expunge = Expunge.EXPUNGE_IMMEDIATELY,
        maxFolderPush: String = "5",
        refreshIdle: String = "2",
    ): FetchingMailSettingsContract.State {
        return FetchingMailSettingsContract.State(
            localFolderSize = SelectOption(localFolderSize) { "" },
            syncMessageFrom = SelectOption(syncMessageFrom) { "" },
            fetchMessageUpTo = SelectOption(fetchMessageUpTo) { "" },
            folderPollFrequency = SelectOption(folderPollFrequency) { "" },
            syncServerDeletions = syncServerDeletions,
            markAsReadWhenDeleted = markAsReadWhenDeleted,
            whenIDeleteAMessage = SelectOption(deletePolicy.name) { "" },
            eraseDeletedMessageOnServer = SelectOption(expunge.name) { "" },
            maxFolderToCheckWithPush = SelectOption(maxFolderPush) { "" },
            refreshIdleConnection = SelectOption(refreshIdle) { "" },
        )
    }

    @Test
    fun `buildCoreFetchingMailSettings should build settings in correct order`() {
        val settings = builder.buildCoreFetchingMailSettings(createState()) {}

        assertThat(settings.map { it.id }).containsExactly(
            FetchingMailSettingsId.LOCAL_FOLDER_SIZE,
            FetchingMailSettingsId.SYNC_MESSAGE_FROM,
            FetchingMailSettingsId.FETCH_MESSAGE_UP_TO,
            FetchingMailSettingsId.FOLDER_POLL_FREQUENCY,
            FetchingMailSettingsId.SYNC_SERVER_DELETIONS,
            FetchingMailSettingsId.MARK_AS_READ_WHEN_DELETED,
            FetchingMailSettingsId.WHEN_I_DELETE_A_MESSAGE,
            FetchingMailSettingsId.ERASE_DELETED_MESSAGE_ON_SERVER,
            FetchingMailSettingsId.IN_COMING_SERVER,
            FetchingMailSettingsId.ADVANCE,
        )
    }

    @Test
    fun `buildAdvancedFetchingMailSettings should build settings in correct order`() {
        val settings = builder.buildAdvancedFetchingMailSettings(createState()) {}

        assertThat(settings.map { it.id }).containsExactly(
            FetchingMailSettingsId.MAX_FOLDER_TO_CHECK_WITH_PUSH,
            FetchingMailSettingsId.REFRESH_IDLE_CONNECTION,
        )
    }

    @Test
    fun `core settings should have correct types`() {
        val settings = builder.buildCoreFetchingMailSettings(createState()) {}

        assertThat(settings[0]).all {
            isInstanceOf<SettingValue.Select>()
            prop(Setting::id).isEqualTo(FetchingMailSettingsId.LOCAL_FOLDER_SIZE)
        }

        assertThat(settings[4]).all {
            isInstanceOf<SettingValue.Switch>()
            prop(Setting::id).isEqualTo(FetchingMailSettingsId.SYNC_SERVER_DELETIONS)
        }

        assertThat(settings[8]).all {
            isInstanceOf<SettingValue.ActionText>()
            prop(Setting::id).isEqualTo(FetchingMailSettingsId.IN_COMING_SERVER)
        }
    }

    @Test
    fun `advanced settings should have correct types`() {
        val settings = builder.buildAdvancedFetchingMailSettings(createState()) {}

        settings.forEach {
            assertThat(it).isInstanceOf<SettingValue.Select>()
        }
    }

    @Test
    fun `select settings should preserve selected values`() {
        val state = createState(
            localFolderSize = "500",
            syncMessageFrom = "365",
            fetchMessageUpTo = "5242880",
            folderPollFrequency = "720",
        )

        val settings = builder.buildCoreFetchingMailSettings(state) {}

        assertThat((settings[0] as SettingValue.Select).value.id)
            .isEqualTo("500")

        assertThat((settings[1] as SettingValue.Select).value.id)
            .isEqualTo("365")

        assertThat((settings[2] as SettingValue.Select).value.id)
            .isEqualTo("5242880")

        assertThat((settings[3] as SettingValue.Select).value.id)
            .isEqualTo("720")
    }

    @Test
    fun `advanced select settings should preserve selected values`() {
        val state = createState(
            maxFolderPush = "250",
            refreshIdle = "48",
        )

        val settings = builder.buildAdvancedFetchingMailSettings(state) {}

        assertThat((settings[0] as SettingValue.Select).value.id)
            .isEqualTo("250")

        assertThat((settings[1] as SettingValue.Select).value.id)
            .isEqualTo("48")
    }

    @Test
    fun `switch settings should preserve values`() {
        val state = createState(
            syncServerDeletions = true,
            markAsReadWhenDeleted = true,
        )

        val settings = builder.buildCoreFetchingMailSettings(state) {}

        assertThat((settings[4] as SettingValue.Switch).value).isTrue()

        assertThat((settings[5] as SettingValue.Switch).value).isTrue()
    }

    @Test
    fun `incoming server action should dispatch event`() {
        var event: FetchingMailSettingsContract.Event? = null

        val settings = builder.buildCoreFetchingMailSettings(createState()) {
            event = it
        }

        val action = settings[8] as SettingValue.ActionText

        action.onClick()

        assertThat(event)
            .isEqualTo(FetchingMailSettingsContract.Event.OnInComingServerClick)
    }

    @Test
    fun `advanced action should dispatch event`() {
        var event: FetchingMailSettingsContract.Event? = null

        val settings = builder.buildCoreFetchingMailSettings(createState()) {
            event = it
        }

        val action = settings[9] as SettingValue.ActionText

        action.onClick()

        assertThat(event)
            .isEqualTo(FetchingMailSettingsContract.Event.OnAdvanceClick)
    }

    @Test
    fun `delete policy options should contain all enum values`() {
        val settings = builder.buildCoreFetchingMailSettings(createState()) {}

        val select = settings[6] as SettingValue.Select

        assertThat(select.options.map { it.id }).containsExactly(
            DeletePolicy.NEVER.name,
            DeletePolicy.ON_DELETE.name,
            DeletePolicy.MARK_AS_READ.name,
        )
    }

    @Test
    fun `expunge policy options should contain all enum values`() {
        val settings = builder.buildCoreFetchingMailSettings(createState()) {}

        val select = settings[7] as SettingValue.Select

        assertThat(select.options.map { it.id }).containsExactly(
            Expunge.EXPUNGE_IMMEDIATELY.name,
            Expunge.EXPUNGE_ON_POLL.name,
            Expunge.EXPUNGE_MANUALLY.name,
        )
    }

    @Test
    fun `local folder size should use correct title`() {
        val settings = builder.buildCoreFetchingMailSettings(createState()) {}

        val select = settings[0] as SettingValue.Select

        assertThat(select.title()).isEqualTo(
            resources.stringResource(
                R.string.account_settings_mail_display_count_label,
            ),
        )
    }

    @Test
    fun `select option titles should come from resource manager`() {
        val settings = builder.buildCoreFetchingMailSettings(createState()) {}

        val select = settings[0] as SettingValue.Select

        val title = select.options.first { it.id == "10" }.title()

        assertThat(title).isEqualTo(
            resources.stringResource(
                R.string.account_settings_options_mail_display_count_10,
            ),
        )
    }

    @Test
    fun `all select settings should display value as secondary text`() {
        val coreSettings = builder.buildCoreFetchingMailSettings(createState()) {}
        val advancedSettings = builder.buildAdvancedFetchingMailSettings(createState()) {}

        (coreSettings + advancedSettings)
            .filterIsInstance<SettingValue.Select>()
            .forEach {
                assertThat(it.displayValueAsSecondaryText).isTrue()
            }
    }
}
