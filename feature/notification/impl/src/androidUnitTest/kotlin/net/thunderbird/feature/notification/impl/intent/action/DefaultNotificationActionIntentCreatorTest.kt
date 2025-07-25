package net.thunderbird.feature.notification.impl.intent.action

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.PendingIntentCompat
import androidx.test.core.app.ApplicationProvider
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import assertk.assertions.prop
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.feature.notification.api.ui.action.NotificationAction
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultNotificationActionIntentCreatorTest {
    private val application: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun `accept should return true for any type of notification action`() {
        // Arrange
        val multipleActions = listOf(
            NotificationAction.Tap,
            NotificationAction.Reply,
            NotificationAction.MarkAsRead,
            NotificationAction.Delete,
            NotificationAction.MarkAsSpam,
            NotificationAction.Archive,
            NotificationAction.UpdateServerSettings,
            NotificationAction.Retry,
            NotificationAction.CustomAction(title = "Custom Action 1"),
            NotificationAction.CustomAction(title = "Custom Action 2"),
            NotificationAction.CustomAction(title = "Custom Action 3"),
        )
        val testSubject = createTestSubject()

        // Act
        val accepted = multipleActions.fold(initial = true) { accepted, action ->
            accepted and testSubject.accept(action)
        }

        // Assert
        assertThat(accepted).isTrue()
    }

    @Test
    fun `create should return PendingIntent for any type of notification action`() {
        // Arrange
        mockStatic(PendingIntentCompat::class.java).use { pendingIntentCompat ->
            // Arrange (cont.)
            val expectedIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(application.packageName)
            }
            val mockedPackageManager = mock<PackageManager> {
                on {
                    getLaunchIntentForPackage(eq(application.packageName))
                } doReturn expectedIntent
            }
            val context = spy(application) {
                on { packageManager } doReturn mockedPackageManager
            }

            pendingIntentCompat
                .`when`<PendingIntent> {
                    PendingIntentCompat.getActivity(
                        /* context = */
                        any(),
                        /* requestCode = */
                        any(),
                        /* intent = */
                        any(),
                        /* flags = */
                        any(),
                        /* isMutable = */
                        eq(false),
                    )
                }
                .thenReturn(mock<PendingIntent>())

            val intentCaptor = argumentCaptor<Intent>()
            val testSubject = createTestSubject(context)

            // Act
            testSubject.create(NotificationAction.Tap)

            // Assert
            pendingIntentCompat.verify {
                PendingIntentCompat.getActivity(
                    /* context = */
                    eq(context),
                    /* requestCode = */
                    eq(1),
                    /* intent = */
                    intentCaptor.capture(),
                    /* flags = */
                    eq(0),
                    /* isMutable = */
                    eq(false),
                )
            }
            assertThat(intentCaptor.firstValue).all {
                prop(Intent::getAction).isEqualTo(Intent.ACTION_MAIN)
                prop(Intent::getPackage).isEqualTo(application.packageName)
                transform { intent -> intent.hasCategory(Intent.CATEGORY_LAUNCHER) }
                    .isTrue()
            }
        }
    }

    private fun createTestSubject(
        context: Context = application,
    ): DefaultNotificationActionIntentCreator {
        return DefaultNotificationActionIntentCreator(
            logger = TestLogger(),
            applicationContext = context,
        )
    }
}
