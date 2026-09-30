package com.fsck.k9.notification

import android.content.Intent
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import net.thunderbird.components.core.logging.Logger
import net.thunderbird.components.core.logging.testing.TestLogger
import net.thunderbird.core.android.testing.RobolectricTest
import net.thunderbird.core.preference.interaction.InteractionSettingsPreferenceManager
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.robolectric.Robolectric

class NotificationActionServiceTest : RobolectricTest() {
    private val preferences = mock<Preferences>()
    private val messagingController = mock<MessagingController>()
    private val interactionPreferences = mock<InteractionSettingsPreferenceManager>()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        startKoin {
            modules(
                module {
                    single { preferences }
                    single { messagingController }
                    single { interactionPreferences }
                    single<Logger> { TestLogger() }
                    single<CoroutineScope>(named("AppCoroutineScope")) { CoroutineScope(UnconfinedTestDispatcher()) }
                },
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `onStartCommand should ignore an invalid account id without accessing accounts`() {
        // Arrange
        val testSubject = Robolectric.buildService(NotificationActionService::class.java).create().get()
        val intent = Intent().putExtra(EXTRA_ACCOUNT_UUID, "not-a-uuid")

        // Act
        val result = testSubject.onStartCommand(intent, 0, 1)

        // Assert
        assertThat(result).isEqualTo(android.app.Service.START_NOT_STICKY)
        verifyNoInteractions(preferences, messagingController)
    }
}
