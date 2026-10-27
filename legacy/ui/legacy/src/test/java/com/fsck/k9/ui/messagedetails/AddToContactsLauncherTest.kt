package com.fsck.k9.ui.messagedetails

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import assertk.assertThat
import assertk.assertions.isFalse
import kotlin.test.Test
import net.thunderbird.core.android.testing.RobolectricTest

class AddToContactsLauncherTest : RobolectricTest() {
    private val context = NoContactsAppContext(ApplicationProvider.getApplicationContext())
    private val testSubject = AddToContactsLauncher()

    @Test
    fun `launch returns false when no contacts app is available`() {
        val result = testSubject.launch(context, name = "John Doe", email = "john@example.com")

        assertThat(result).isFalse()
    }
}

private class NoContactsAppContext(base: Context) : ContextWrapper(base) {
    override fun startActivity(intent: Intent) {
        throw ActivityNotFoundException()
    }
}
