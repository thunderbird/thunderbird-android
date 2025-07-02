package net.thunderbird.core.common.io

actual interface KmpParcelable

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
actual annotation class KmpIgnoredOnParcel()

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
actual annotation class KmpRawValue
