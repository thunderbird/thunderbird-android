package com.fsck.k9.activity.compose

import android.app.Activity
import app.k9mail.feature.launcher.FeatureLauncherActivity
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.fsck.k9.K9RobolectricTest
import com.fsck.k9.activity.MessageCompose
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.usecase.GetDefaultAccountId
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.dsl.module
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

class MessageActionsTest : K9RobolectricTest() {
    private val getDefaultAccountId: GetDefaultAccountId = mock()
    private lateinit var activity: Activity

    private val module = module {
        single<GetDefaultAccountId> { getDefaultAccountId }
    }

    @Before
    fun setUp() {
        loadKoinModules(module)
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    }

    @After
    fun tearDown() {
        unloadKoinModules(module)
    }

    @Test
    fun `actionCompose with explicit LegacyAccount launches MessageCompose with account uuid`() {
        val accountId = AccountIdFactory.create()

        MessageActions.actionCompose(activity, accountId)

        val startedIntent = shadowOf(activity).nextStartedActivity
        assertThat(startedIntent).isNotNull()
        assertThat(startedIntent.component?.className).isEqualTo(MessageCompose::class.java.name)
        assertThat(startedIntent.action).isEqualTo(MessageCompose.ACTION_COMPOSE)
        assertThat(startedIntent.getStringExtra(MessageCompose.EXTRA_ACCOUNT)).isEqualTo(accountId.toString())
    }

    @Test
    fun `actionCompose with null account and default account launches MessageCompose with default uuid`() {
        val defaultAccountId = AccountIdFactory.create()
        whenever(getDefaultAccountId.invoke()).thenReturn(defaultAccountId)

        MessageActions.actionCompose(activity, null)

        val startedIntent = shadowOf(activity).nextStartedActivity
        assertThat(startedIntent).isNotNull()
        assertThat(startedIntent.component?.className).isEqualTo(MessageCompose::class.java.name)
        assertThat(startedIntent.action).isEqualTo(MessageCompose.ACTION_COMPOSE)
        assertThat(startedIntent.getStringExtra(MessageCompose.EXTRA_ACCOUNT)).isEqualTo(defaultAccountId.toString())
    }

    @Test
    fun `actionCompose with null account and no default account launches AccountSetup`() {
        whenever(getDefaultAccountId.invoke()).thenReturn(null)

        MessageActions.actionCompose(activity, null)

        val startedIntent = shadowOf(activity).nextStartedActivity
        assertThat(startedIntent).isNotNull()
        assertThat(startedIntent.component?.className).isEqualTo(FeatureLauncherActivity::class.java.name)
    }
}
