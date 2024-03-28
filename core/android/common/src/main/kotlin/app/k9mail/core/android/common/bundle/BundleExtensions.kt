package app.k9mail.core.android.common.bundle

import android.os.Bundle

fun <T : Enum<T>> Bundle.putEnum(key: String, value: T) {
    putString(key, value.name)
}

inline fun <reified T : Enum<T>> Bundle.getEnum(key: String, defaultValue: T): T {
    val value = getString(key) ?: return defaultValue
    return enumValueOf(value)
}

inline fun <reified T : Enum<T>> Bundle.getEnum(key: String): T {
    val value = getString(key) ?: error("Missing enum value for key '$key'")
    return enumValueOf(value)
}
