package net.thunderbird.core.ui.compose.preference.api

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * A preference that can be displayed in a preference screen.
 */
sealed interface Preference : Parcelable {
    val id: String
}

/**
 * A preference that holds a value of type [T].
 */
sealed interface PreferenceSetting<T> : Preference {
    val value: T
    val requiresEditView: Boolean

    @Parcelize
    data class Text(
        override val id: String,
        val title: () -> String,
        val description: () -> String? = { null },
        val icon: () -> ImageVector? = { null },
        override val value: String,
    ) : PreferenceSetting<String> {
        @IgnoredOnParcel
        override val requiresEditView: Boolean = true
    }

    @Parcelize
    data class Color(
        override val id: String,
        val title: () -> String,
        val description: () -> String? = { null },
        val icon: () -> ImageVector? = { null },
        override val value: Int,
        val colors: List<Int>,
    ) : PreferenceSetting<Int> {
        @IgnoredOnParcel
        override val requiresEditView: Boolean = true
    }
}

/**
 * A preference that does not hold a value. It is used to display a section, a divider or custom UI.
 */
sealed interface PreferenceDisplay : Preference {

    @Parcelize
    data class Custom(
        override val id: String,
        val customUi: @Composable (Modifier) -> Unit,
    ) : PreferenceDisplay
}
