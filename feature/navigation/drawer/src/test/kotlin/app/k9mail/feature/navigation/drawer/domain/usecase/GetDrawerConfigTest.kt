package app.k9mail.feature.navigation.drawer.domain.usecase

import app.k9mail.feature.navigation.drawer.domain.entity.DrawerConfig
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class GetDrawerConfigTest {

    @Test
    fun `should get drawer config`() = runTest {
        val drawerConfig = DrawerConfig(
            showUnifiedInbox = true,
            showStarredCount = true,
        )

        val testSubject = GetDrawerConfig(
            configProver = { drawerConfig },
        )

        val result = testSubject().first()

        assertThat(result).isEqualTo(
            DrawerConfig(
                showUnifiedInbox = true,
                showStarredCount = true,
            ),
        )
    }
}
