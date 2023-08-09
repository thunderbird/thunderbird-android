package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.feature.account.setup.domain.DomainContract
import com.fsck.k9.mail.ssl.LocalKeyStore
import java.security.cert.X509Certificate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class AddServerCertificateException(
    private val localKeyStore: LocalKeyStore,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DomainContract.UseCase.AddServerCertificateException {
    override suspend fun addCertificate(hostname: String, port: Int, certificate: X509Certificate?) {
        withContext(coroutineDispatcher) {
            localKeyStore.addCertificate(hostname, port, certificate)
        }
    }
}
