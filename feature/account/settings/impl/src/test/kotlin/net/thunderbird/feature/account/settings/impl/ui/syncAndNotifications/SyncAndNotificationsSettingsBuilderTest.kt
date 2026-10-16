package net.thunderbird.feature.account.settings.impl.ui.syncAndNotifications

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import net.thunderbird.core.common.resources.StringsResourceManager
import net.thunderbird.core.ui.setting.Setting
import net.thunderbird.core.ui.setting.SettingValue
import org.junit.Test

internal class SyncAndNotificationsSettingsBuilderTest {

    private val resources = object : StringsResourceManager {
        override fun stringResource(resourceId: Int): String =
            "string_$resourceId"

        override fun stringResource(resourceId: Int, vararg formatArgs: Any?): String =
            "string_$resourceId"
    }

    private val builder = SyncAndNotificationSettingsBuilder(
        resources = resources,
    )

    @Test
    fun `should include notification, folder, and fetching sections`() {
        val state = SyncAndNotificationSettingsContract.State(
            enablePushNotifications = true,
        )
        val settings = builder.build(state = state) {}

        assertThat(settings[0]).all {
            isInstanceOf<SettingValue.ActionText>()
            prop(Setting::id).isEqualTo(SyncAndNotificationsSettingsId.NOTIFICATION_STYLE)
        }
        assertThat(settings[1]).all {
            isInstanceOf<SettingValue.ActionText>()
            prop(Setting::id).isEqualTo(SyncAndNotificationsSettingsId.FOLDER_SYNC_AND_PUSH)
        }
        assertThat(settings[2]).all {
            isInstanceOf<SettingValue.ActionText>()
            prop(Setting::id).isEqualTo(SyncAndNotificationsSettingsId.FETCHING_AND_DATA_LIMITS)
        }
    }
}
