package app.k9mail.feature.navigation.drawer

import android.view.Surface
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.theme.api.FeatureThemeProvider
import app.k9mail.legacy.account.Account
import com.mikepenz.materialdrawer.widget.MaterialDrawerSliderView
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FolderDrawer(
    override val parent: AppCompatActivity,
) : NavigationDrawer, KoinComponent {

    private val themeProvider: FeatureThemeProvider by inject()

    private val drawer: DrawerLayout = parent.findViewById(R.id.navigation_drawer_layout)
    private val drawerView: ComposeView = parent.findViewById(R.id.material_drawer_compose_view)
    private val sliderView: MaterialDrawerSliderView = parent.findViewById(R.id.material_drawer_slider)

    init {
        sliderView.visibility = View.GONE
        drawerView.visibility = View.VISIBLE

        drawerView.setContent {
            themeProvider.WithTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Red,
                ) {
                    TextBodyLarge("Folder Drawer")
                }
            }
        }
    }

    override val isOpen: Boolean
        get() = drawer.isOpen

    override fun updateUserAccountsAndFolders(account: Account?) {
        // TODO("Not yet implemented")
    }

    override fun selectAccount(accountUuid: String) {
        // TODO("Not yet implemented")
    }

    override fun selectFolder(folderId: Long) {
        // TODO("Not yet implemented")
    }

    override fun selectUnifiedInbox() {
        // TODO("Not yet implemented")
    }

    override fun deselect() {
        // TODO("Not yet implemented")
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
