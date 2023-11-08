package app.k9mail.core.android.permissions

enum class PermissionState {
    /**
     * The permission is not a runtime permission in the Android version we're running on.
     */
    GrantedImplicitly,
    Granted,
    Denied,
}
