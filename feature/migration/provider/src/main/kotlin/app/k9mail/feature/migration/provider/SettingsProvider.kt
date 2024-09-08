package app.k9mail.feature.migration.provider

import android.annotation.TargetApi
import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import app.k9mail.legacy.account.AccountManager
import com.fsck.k9.helper.MimeTypeUtil
import com.fsck.k9.helper.mapToSet
import com.fsck.k9.preferences.SettingsExporter
import kotlin.concurrent.thread
import okio.ByteString.Companion.toByteString
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import timber.log.Timber

/**
 * A `ContentProvider` that makes settings available to another app.
 *
 * This can be used when migrating from one app to another, e.g. from K-9 Mail to Thunderbird for Android.
 * Only apps on the allowlist (see [isTrustedCaller()][SettingsProvider.isTrustedCaller]) are allowed access to the
 * settings (including passwords).
 */
class SettingsProvider : ContentProvider(), KoinComponent {
    private val accountManager: AccountManager by inject()
    private val settingsExporter: SettingsExporter by inject()

    override fun onCreate(): Boolean {
        return true
    }

    override fun getType(uri: Uri): String {
        return MimeTypeUtil.K9_SETTINGS_MIME_TYPE
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        if (!isTrustedCaller()) {
            Timber.d("Caller must be in the allowlist")
            return null
        }

        val (readFileDescriptor, writeFileDescriptor) = ParcelFileDescriptor.createPipe()

        thread {
            val accountUuids = accountManager.getAccounts().mapToSet { it.uuid }
            ParcelFileDescriptor.AutoCloseOutputStream(writeFileDescriptor).use { outputStream ->
                settingsExporter.exportPreferences(
                    outputStream,
                    includeGlobals = true,
                    accountUuids,
                    includePasswords = true,
                )
            }
        }

        return readFileDescriptor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw UnsupportedOperationException("not implemented")
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        throw UnsupportedOperationException("not implemented")
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        throw UnsupportedOperationException("not implemented")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        throw UnsupportedOperationException("not implemented")
    }

    /**
     * A trusted caller must appear exactly in our allowlist map - its package map must map to a known signature.
     * In case of any deviation (multiple signers, certificate rotation), assume that the caller isn't trusted.
     */
    // Based on https://searchfox.org/mozilla-esr68/source/mobile/android/services/src/main/java/org/mozilla/gecko/fxa/AuthStateProvider.java
    @Suppress("ReturnCount")
    private fun isTrustedCaller(): Boolean {
        val packageManager = context!!.packageManager

        // Signatures can be easily obtained locally. For an APK in question, unzip it and run:
        // keytool -printcert -file META-INF/SIGNATURE.RSA
        // SHA256 certificate fingerprint is what's listed below.

        // We will only service query requests from callers that exactly match our allowlist.
        // Allowlist is local to this function to avoid exposing it to the world more than necessary.
        val packageAllowlist = listOf(
            // K-9 Mail (our signing key)
            "com.fsck.k9" to "55c8a523b97335f5bf60dfe8a9f3e1dde744516d9357e80a925b7b22e4f55524",
            // K-9 Mail (F-Droid)
            "com.fsck.k9" to "c430665e3662253b2078dcda350c2c6ce44d915a3d8a147b63ced619bb9e8576",
            // Thunderbird for Android (release)
            "net.thunderbird.android" to "b6524779b3dbbc5ac17a5ac271ddb29dcfbf723578c238e03c3c217811356dd1",
            // Thunderbird for Android (beta)
            "net.thunderbird.android.beta" to "056bfafb450249502fd9226228704c2529e1b822da06760d47a85c9557741fbd",
            // Thunderbird for Android (daily)
            "net.thunderbird.android.daily" to "c48d74a75c45cd362b0ff2c1e9756f541dee816163e3684a9fd59f6c3ae949b2",
        )

        val callerPackage = callingPackage ?: return false
        val expectedHashes = packageAllowlist
            .asSequence()
            .filter { it.first == callerPackage }
            .map { it.second }
            .toList()
            .takeIf { it.isNotEmpty() }
            ?: return false

        val callerSignature = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getSignaturePostApi28(packageManager, callerPackage)
        } else {
            getSignaturePreApi28(packageManager, callerPackage)
        }

        if (callerSignature == null) {
            Timber.v("Couldn't retrieve caller signature")
            return false
        }

        val callerSignatureHash = callerSignature.toByteArray().toByteString().sha256().hex()
        val result = callerSignatureHash in expectedHashes
        if (result) {
            Timber.d("Caller %s signature fingerprint matches %s", callerPackage, callerSignatureHash)
        } else {
            Timber.d("Failed! Signature mismatch for calling package %s (%s)", callerPackage, callerSignatureHash)
        }

        return result
    }

    @Suppress("DEPRECATION")
    private fun getSignaturePreApi28(packageManager: PackageManager, callerPackage: String): Signature? {
        // For older APIs, we use the deprecated `signatures` field, which isn't aware of certificate rotation.
        val packageInfo = packageManager.getPackageInfo(callerPackage, PackageManager.GET_SIGNATURES)

        // We don't expect our callers to have multiple signers, so we don't service such requests.
        if (packageInfo.signatures.size != 1) {
            return null
        }

        // In case of signature rotation, this will report the oldest used certificate, pretending that the signature
        // rotation never took place. We can only rely on our allowlist being up-to-date in this case.
        return packageInfo.signatures[0]
    }

    @TargetApi(Build.VERSION_CODES.P)
    @Suppress("ReturnCount")
    private fun getSignaturePostApi28(packageManager: PackageManager, callerPackage: String): Signature? {
        // For API28+, we can perform some extra checks.
        val packageInfo = packageManager.getPackageInfo(callerPackage, PackageManager.GET_SIGNING_CERTIFICATES)

        // We don't expect our callers to have multiple signers, so we don't service such requests.
        if (packageInfo.signingInfo.hasMultipleSigners()) {
            return null
        }

        // We currently don't support servicing requests from callers that performed certificate rotation.
        if (packageInfo.signingInfo.hasPastSigningCertificates()) {
            return null
        }

        return packageInfo.signingInfo.signingCertificateHistory[0]
    }
}
