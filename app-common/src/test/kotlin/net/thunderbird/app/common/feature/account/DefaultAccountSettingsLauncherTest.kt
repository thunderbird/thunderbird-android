package net.thunderbird.app.common.feature.account

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.fsck.k9.ui.managefolders.ManageFoldersActivity
import com.fsck.k9.ui.settings.account.AccountSettingsActivity
import com.fsck.k9.ui.settings.account.AccountSettingsFragment
import kotlin.test.Test
import net.thunderbird.core.android.testing.RobolectricTest
import net.thunderbird.feature.account.AccountIdFactory
import org.robolectric.RuntimeEnvironment

class DefaultAccountSettingsLauncherTest : RobolectricTest() {

    private val accountId = AccountIdFactory.create()
    private val testSubject = DefaultAccountSettingsLauncher()

    @Test
    fun `launches legacy folder settings`() {
        // Arrange
        val context = RecordingContext(RuntimeEnvironment.getApplication())

        // Act
        testSubject.launchFolderSettings(context, accountId)

        // Assert
        assertThat(context.startedIntents).hasSize(1)
        assertSettingsIntent(context.startedIntents.single(), AccountSettingsFragment.PREFERENCE_FOLDERS)
    }

    @Test
    fun `launches legacy notification style`() {
        // Arrange
        val context = RecordingContext(RuntimeEnvironment.getApplication())

        // Act
        testSubject.launchNotificationStyleSettings(context, accountId)

        // Assert
        assertThat(context.startedIntents).hasSize(1)
        assertSettingsIntent(context.startedIntents.single(), AccountSettingsFragment.PREFERENCE_NOTIFICATION_STYLE)
    }

    @Test
    fun `launches legacy folder sync and push management`() {
        // Arrange
        val context = RecordingContext(RuntimeEnvironment.getApplication())

        // Act
        testSubject.launchFolderSyncAndPushSettings(context, accountId)

        // Assert
        assertThat(context.startedIntents).hasSize(1)
        val intent = context.startedIntents.single()
        assertThat(intent.component?.className).isEqualTo(ManageFoldersActivity::class.java.name)
        assertThat(intent.getStringExtra("account")).isEqualTo(accountId.toString())
        assertThat(intent.flags).isEqualTo(0)
    }

    private fun assertSettingsIntent(intent: Intent, startScreen: String) {
        assertThat(intent.component?.className).isEqualTo(AccountSettingsActivity::class.java.name)
        assertThat(intent.getStringExtra("accountUuid")).isEqualTo(accountId.value.toString())
        assertThat(intent.getStringExtra("startScreen")).isEqualTo(startScreen)
        assertThat(intent.flags).isEqualTo(0)
    }

    private class RecordingContext(base: Context) : ContextWrapper(base) {
        val startedIntents = mutableListOf<Intent>()

        override fun startActivity(intent: Intent) {
            startedIntents += intent
        }
    }
}
