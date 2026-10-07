package net.thunderbird.feature.navigation.drawer.dropdown

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.UnifiedAccountId
import net.thunderbird.feature.navigation.drawer.api.NavigationDrawer
import net.thunderbird.feature.navigation.drawer.api.R
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayFolderType
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.createMailDisplayAccountFolderId
import net.thunderbird.feature.navigation.drawer.dropdown.ui.DrawerView
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Stable
internal data class FolderDrawerState(
    val selectedAccountId: AccountId? = null,
    val selectedFolderId: String? = null,
)

@Suppress("LongParameterList")
class DropDownDrawer(
    override val parent: AppCompatActivity,
    private val openAccount: (accountId: AccountId) -> Unit,
    private val openFolder: (accountId: AccountId, folderId: Long) -> Unit,
    private val openUnifiedFolder: () -> Unit,
    private val openManageFolders: () -> Unit,
    private val openSettings: () -> Unit,
    private val openAddAccount: () -> Unit,
    createDrawerListener: () -> DrawerLayout.DrawerListener,
) : NavigationDrawer, KoinComponent {

    private val themeProvider: FeatureThemeProvider by inject()
    private val drawer: DrawerLayout = parent.findViewById(R.id.navigation_drawer_layout)
    private val drawerContent: ComposeView = parent.findViewById(R.id.navigation_drawer_content)

    private val drawerState = MutableStateFlow(FolderDrawerState())

    init {
        drawer.addDrawerListener(createDrawerListener())

        // Make insets available to the drawer's Compose content
        ViewCompat.setOnApplyWindowInsetsListener(drawer) { _, insets ->
            drawerContent.dispatchApplyWindowInsets(insets.toWindowInsets())
            insets
        }

        drawerContent.setContent {
            themeProvider.WithTheme {
                val state = drawerState.collectAsStateWithLifecycle()

                DrawerView(
                    drawerState = state.value,
                    openAccount = openAccount,
                    openFolder = openFolder,
                    openUnifiedFolder = openUnifiedFolder,
                    openManageFolders = openManageFolders,
                    openSettings = openSettings,
                    openAddAccount = openAddAccount,
                    closeDrawer = { close() },
                )
            }
        }
    }

    override val isOpen: Boolean
        get() = drawer.isDrawerOpen(GravityCompat.START)

    override fun selectAccount(accountId: AccountId) {
        drawerState.update {
            it.copy(selectedAccountId = accountId)
        }
    }

    override fun selectFolder(accountId: AccountId, folderId: Long) {
        drawerState.update {
            it.copy(
                selectedAccountId = accountId,
                selectedFolderId = createMailDisplayAccountFolderId(accountId, folderId),
            )
        }
    }

    override fun selectUnifiedInbox() {
        drawerState.update {
            it.copy(
                selectedAccountId = UnifiedAccountId,
                selectedFolderId = UnifiedDisplayFolderType.INBOX.id,
            )
        }
    }

    override fun deselect() {
        drawerState.update {
            it.copy(
                selectedFolderId = null,
            )
        }
    }

    override fun open() {
        drawer.openDrawer(GravityCompat.START)
    }

    override fun close() {
        drawer.closeDrawer(GravityCompat.START)
    }

    override fun lock() {
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    override fun unlock() {
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    }
}
