package app.k9mail.feature.widget.shortcut

import android.content.Intent
import android.content.res.Resources.Theme
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.fsck.k9.activity.AccountList
import com.fsck.k9.activity.MessageList
import net.thunderbird.feature.mail.account.api.BaseAccount
import net.thunderbird.feature.search.legacy.SearchAccount
import app.k9mail.core.ui.legacy.theme2.common.R as CommonR

class LauncherShortcutActivity : AccountList() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.shortcut_widget_title)

        // finish() immediately if we aren't supposed to be here
        if (intent.action != Intent.ACTION_CREATE_SHORTCUT) {
            finish()
        }
    }

    override fun onAccountSelected(account: BaseAccount) {
        val shortcutIntent = if (account is SearchAccount) {
            MessageList.shortcutIntent(this, account.id)
        } else {
            MessageList.shortcutIntentForAccount(this, account.uuid)
        }

        val displayName = account.name ?: account.email
        val iconResId = theme.resolveDrawableResourceId(CommonR.attr.appLogo)
        val shortcutId = account.uuid

        val resultIntent = createResultIntent(displayName, iconResId, shortcutIntent, shortcutId)

        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun createResultIntent(
        displayName: String,
        iconResId: Int,
        shortcutIntent: Intent,
        shortcutId: String,
    ): Intent {
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S) {
            createResultIntentLegacy(displayName, iconResId, shortcutIntent)
        } else {
            createResultIntentApi32(shortcutId, displayName, iconResId, shortcutIntent)
        }
    }

    @Suppress("DEPRECATION")
    private fun createResultIntentLegacy(displayName: String, iconResId: Int, shortcutIntent: Intent): Intent {
        val iconResource = Intent.ShortcutIconResource.fromContext(this, iconResId)

        return Intent().apply {
            putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
            putExtra(Intent.EXTRA_SHORTCUT_NAME, displayName)
            putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource)
        }
    }

    private fun createResultIntentApi32(
        shortcutId: String,
        displayName: String,
        iconResId: Int,
        shortcutIntent: Intent,
    ): Intent {
        val shortcut = ShortcutInfoCompat.Builder(this, shortcutId)
            .setShortLabel(displayName)
            .setIcon(IconCompat.createWithResource(this, iconResId))
            .setIntent(shortcutIntent)
            .build()

        return ShortcutManagerCompat.createShortcutResultIntent(this, shortcut)
    }

    private fun Theme.resolveDrawableResourceId(@AttrRes attr: Int): Int {
        val typedValue = TypedValue()
        resolveAttribute(attr, typedValue, true)
        return typedValue.resourceId
    }
}
