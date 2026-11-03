package net.thunderbird.core.android.common.keypad

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.view.KeyEvent
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 31, 36])
class PhoneIdleProviderTest {
    private val application = RuntimeEnvironment.getApplication()
    private val telecom = application.getSystemService(TelecomManager::class.java)
    private val telephony = application.getSystemService(TelephonyManager::class.java)
    private val audio = application.getSystemService(AudioManager::class.java)
    private val testSubject = PhoneIdleProvider(application)

    @Before
    fun setUp() {
        shadowOf(application).grantPermissions(Manifest.permission.READ_PHONE_STATE)
        shadowOf(telecom).setIsInCall(false)
        shadowOf(telephony).setCallState(TelephonyManager.CALL_STATE_IDLE)
        audio.mode = AudioManager.MODE_NORMAL
    }

    @Test
    fun `phone should be idle only with permission and normal communication audio`() {
        // Act
        val idle = testSubject.isIdle()

        // Assert
        assertThat(idle).isTrue()
    }

    @Test
    fun `permission denial or revocation must keep shortcuts inactive`() {
        // Arrange
        shadowOf(application).denyPermissions(Manifest.permission.READ_PHONE_STATE)

        // Act
        val idleWithoutPermission = testSubject.isIdle()
        shadowOf(application).grantPermissions(Manifest.permission.READ_PHONE_STATE)
        val idleAfterGrant = testSubject.isIdle()
        shadowOf(application).denyPermissions(Manifest.permission.READ_PHONE_STATE)
        val idleAfterRevocation = testSubject.isIdle()

        // Assert
        assertThat(idleWithoutPermission).isFalse()
        assertThat(idleAfterGrant).isTrue()
        assertThat(idleAfterRevocation).isFalse()
    }

    @Test
    fun `an active or ringing telecom call must not allow shortcuts`() {
        // Arrange
        shadowOf(telecom).setIsInCall(true)

        // Act
        val idle = testSubject.isIdle()

        // Assert
        assertThat(idle).isFalse()
    }

    @Test
    fun `cellular ringing or off-hook state must block shortcuts before Telecom reports a call`() {
        // Arrange
        shadowOf(telecom).setIsInCall(false)

        // Act and assert
        for (state in listOf(TelephonyManager.CALL_STATE_RINGING, TelephonyManager.CALL_STATE_OFFHOOK)) {
            shadowOf(telephony).setCallState(state)
            assertThat(testSubject.isIdle()).isFalse()
        }
    }

    @Test
    fun `ringtone cellular call and VoIP audio modes must keep shortcuts inactive`() {
        // Arrange
        val busyModes =
            listOf(AudioManager.MODE_RINGTONE, AudioManager.MODE_IN_CALL, AudioManager.MODE_IN_COMMUNICATION)

        // Act and assert
        for (mode in busyModes) {
            audio.mode = mode
            assertThat(testSubject.isIdle()).isFalse()
        }
    }

    @Test
    fun `an unavailable system service must not be considered idle`() {
        // Arrange
        val context = object : ContextWrapper(application) {
            override fun getSystemService(name: String): Any? =
                if (name == Context.TELECOM_SERVICE) null else super.getSystemService(name)
        }
        val testSubject = PhoneIdleProvider(context)

        // Act
        val idle = testSubject.isIdle()

        // Assert
        assertThat(idle).isFalse()
    }

    @Test
    fun `a failing system service must not be considered idle`() {
        // Arrange
        val context = object : ContextWrapper(application) {
            override fun getSystemService(name: String): Any? {
                if (name == Context.TELECOM_SERVICE) throw SecurityException("Phone state unavailable")
                return super.getSystemService(name)
            }
        }
        val testSubject = PhoneIdleProvider(context)

        // Act
        val idle = testSubject.isIdle()

        // Assert
        assertThat(idle).isFalse()
    }

    @Test
    fun `a denied permission should leave call and end-call events entirely to Android`() {
        // Arrange
        shadowOf(application).denyPermissions(Manifest.permission.READ_PHONE_STATE)
        val actions = mutableListOf<String>()
        val handler = PhoneKeyShortcutHandler(
            isPhoneIdle = testSubject::isIdle,
            getShortcut = { _: Int -> "DELETE" },
            getTarget = { "focused-message" },
            performAction = { action, _ -> actions.add(action) },
        )

        // Act and assert
        for (keyCode in listOf(KeyEvent.KEYCODE_CALL, KeyEvent.KEYCODE_ENDCALL)) {
            assertThat(handler.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))).isFalse()
            assertThat(handler.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))).isFalse()
        }
        assertThat(actions).isEmpty()
    }
}
