package app.k9mail.legacy.notification

import android.app.Notification

enum class NotificationLight {
    Disabled,
    AccountColor,
    SystemDefaultColor,
    White,
    Red,
    Green,
    Blue,
    Yellow,
    Cyan,
    Magenta,
    ;

    fun toColor(accountColor: Int): Int? {
        return when (this) {
            Disabled -> null
            AccountColor -> accountColor.toArgb()
            SystemDefaultColor -> Notification.COLOR_DEFAULT
            White -> 0xFFFFFF.toArgb()
            Red -> 0xFF0000.toArgb()
            Green -> 0x00FF00.toArgb()
            Blue -> 0x0000FF.toArgb()
            Yellow -> 0xFFFF00.toArgb()
            Cyan -> 0x00FFFF.toArgb()
            Magenta -> 0xFF00FF.toArgb()
        }
    }

    private fun Int.toArgb() = this or 0xFF000000L.toInt()
}
