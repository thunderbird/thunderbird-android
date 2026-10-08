package net.thunderbird.core.common.extension

/**
 * Applies the given transform function to this string if it is not null, otherwise returns the default value.
 *
 * @param default the value to return if this string is null, defaults to an empty string
 * @param transform the function to apply to the non-null string value
 * @return the result of applying the transform function if this string is not null, otherwise the default value
 */
fun String?.mapOrDefault(default: String = "", transform: (String) -> String) =
    this?.let(transform) ?: default
