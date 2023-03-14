package app.k9mail.core.android.common.contact

class TestContactPermissionResolver(hasPermission: Boolean) : ContactPermissionResolver {
    var hasContactPermission = hasPermission

    override fun hasContactPermission(): Boolean {
        return hasContactPermission
    }
}
