package net.thunderbird.core.common.io

actual interface KmpParcelable

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
actual annotation class KmpIgnoredOnParcel()
