package app.k9mail.legacy.ui.account

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.util.TypedValue
import androidx.core.content.ContextCompat
import app.k9mail.core.ui.legacy.designsystem.atom.icon.Icons

private const val PADDING_DP = 8f

/**
 * Provides a [Drawable] for the account using the account's color as background color.
 */
class AccountFallbackImageProvider(private val context: Context) {
    fun getDrawable(color: Int): Drawable {
        val drawable = ContextCompat.getDrawable(context, Icons.Outlined.Person)
            ?: error("Error loading drawable")

        val inset = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            PADDING_DP,
            context.resources.displayMetrics,
        ).toInt()

        return LayerDrawable(
            arrayOf(
                ColorDrawable(color),
                InsetDrawable(drawable, inset),
            ),
        )
    }
}
