package net.thunderbird.feature.mail.message.composer.internal.smime

import android.content.Context
import android.security.KeyChain
import android.security.KeyChainException
import java.security.PrivateKey
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateNotYetValidException
import java.security.cert.X509Certificate
import javax.security.auth.x500.X500Principal
import net.thunderbird.components.core.logging.Logger
import net.thunderbird.feature.mail.message.composer.smime.SmimeCertificateInfo
import net.thunderbird.feature.mail.message.composer.smime.SmimeKeyChainProvider
import net.thunderbird.feature.mail.message.composer.smime.SmimeSigningCredentials
import net.thunderbird.feature.mail.message.composer.smime.SmimeSigningException

internal class AndroidKeyChainSmimeProvider(
    private val context: Context,
    private val logger: Logger,
) : SmimeKeyChainProvider {

    @Throws(SmimeSigningException::class)
    override fun getSigningCredentials(alias: String): SmimeSigningCredentials {
        if (alias.isBlank()) {
            throw SmimeSigningException("No S/MIME signing certificate selected")
        }

        val chain = loadCertificateChain(alias)
        validateCertificate(chain.first())
        val privateKey = loadPrivateKey(alias)

        return SmimeSigningCredentials(
            alias = alias,
            privateKey = privateKey,
            certificateChain = chain,
        )
    }

    override fun getCertificateInfo(alias: String): SmimeCertificateInfo? {
        val leaf = alias.takeIf { it.isNotBlank() }
            ?.let(::loadCertificateChainOrNull)
            ?.firstOrNull()
            ?: return null

        val isExpired = try {
            leaf.checkValidity()
            false
        } catch (_: CertificateExpiredException) {
            true
        } catch (_: CertificateNotYetValidException) {
            true
        }

        return SmimeCertificateInfo(
            alias = alias,
            subjectDisplayName = extractDisplayName(leaf.subjectX500Principal) ?: alias,
            issuerDisplayName = extractDisplayName(leaf.issuerX500Principal),
            isExpired = isExpired,
        )
    }

    @Throws(SmimeSigningException::class)
    private fun loadCertificateChain(alias: String): List<X509Certificate> {
        val chain = runKeyChainCall("certificate chain") {
            KeyChain.getCertificateChain(context, alias)
        }
        if (chain == null || chain.isEmpty()) {
            throw SmimeSigningException("Selected S/MIME certificate was not found in Android KeyChain")
        }
        return chain.toList()
    }

    @Throws(SmimeSigningException::class)
    private fun loadPrivateKey(alias: String): PrivateKey {
        return runKeyChainCall("private key") {
            KeyChain.getPrivateKey(context, alias)
        } ?: throw SmimeSigningException("No private key found for the selected S/MIME certificate")
    }

    @Throws(SmimeSigningException::class)
    private fun <T> runKeyChainCall(resourceName: String, block: () -> T): T {
        val failure = try {
            return block()
        } catch (e: KeyChainException) {
            logger.error(tag = TAG, throwable = e) { "Failed to retrieve $resourceName from Android KeyChain" }
            SmimeSigningException("Could not load $resourceName from Android KeyChain", e)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            logger.error(tag = TAG, throwable = e) { "Interrupted while loading $resourceName" }
            SmimeSigningException("Interrupted while loading signing $resourceName", e)
        } catch (e: SecurityException) {
            logger.error(tag = TAG, throwable = e) { "Permission denied while loading $resourceName" }
            SmimeSigningException("Permission denied while accessing Android KeyChain $resourceName", e)
        }
        throw failure
    }

    private fun loadCertificateChainOrNull(alias: String): Array<X509Certificate>? {
        return try {
            KeyChain.getCertificateChain(context, alias)
        } catch (e: KeyChainException) {
            logger.warn(tag = TAG, throwable = e) { "Unable to inspect S/MIME certificate info" }
            null
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        } catch (e: SecurityException) {
            logger.warn(tag = TAG, throwable = e) { "Permission denied while inspecting S/MIME certificate" }
            null
        }
    }

    private fun validateCertificate(certificate: X509Certificate) {
        try {
            certificate.checkValidity()
        } catch (e: CertificateExpiredException) {
            throw SmimeSigningException("Selected S/MIME certificate has expired", e)
        } catch (e: CertificateNotYetValidException) {
            throw SmimeSigningException("Selected S/MIME certificate is not yet valid", e)
        }
    }

    private fun extractDisplayName(principal: X500Principal): String? {
        val dn = principal.getName(X500Principal.RFC2253)
        return extractRdnValue(dn, "CN") ?: extractRdnValue(dn, "O")
    }

    private fun extractRdnValue(dn: String, attribute: String): String? {
        val prefix = "$attribute="
        return dn.split(',')
            .asSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith(prefix, ignoreCase = true) }
            ?.substring(prefix.length)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    private companion object {
        const val TAG = "AndroidKeyChainSmimeProvider"
    }
}
