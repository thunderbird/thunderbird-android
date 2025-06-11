package net.thunderbird.core.common.io

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class KmpParcelize

expect interface KmpParcelable

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
expect annotation class KmpIgnoredOnParcel()
