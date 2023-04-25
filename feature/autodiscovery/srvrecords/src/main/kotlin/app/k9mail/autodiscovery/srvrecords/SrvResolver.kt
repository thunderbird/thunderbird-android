package app.k9mail.autodiscovery.srvrecords

interface SrvResolver {
    fun lookup(domain: String, type: SrvType): List<MailService>
}
