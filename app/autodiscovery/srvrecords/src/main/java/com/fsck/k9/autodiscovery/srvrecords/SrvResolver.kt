package com.fsck.k9.autodiscovery.srvrecords

interface SrvResolver {
    fun lookup(domain: String, type: SrvType): List<MailService>
}
