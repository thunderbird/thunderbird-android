package com.fsck.k9.backend.jmap

import java.net.UnknownHostException
import net.thunderbird.core.logging.legacy.Log
import rs.ltt.jmap.client.JmapClient
import rs.ltt.jmap.client.api.EndpointNotFoundException
import rs.ltt.jmap.client.api.UnauthorizedException
import rs.ltt.jmap.common.entity.capability.MailAccountCapability

class JmapAccountDiscovery {
    fun discover(emailAddress: String, password: String): JmapDiscoveryResult {
        val jmapClient = JmapClient(emailAddress, password)
        val session = try {
            jmapClient.session.futureGetOrThrow()
        } catch (e: EndpointNotFoundException) {
            return JmapDiscoveryResult.EndpointNotFoundFailure
        } catch (e: UnknownHostException) {
            return JmapDiscoveryResult.EndpointNotFoundFailure
        } catch (e: UnauthorizedException) {
            return JmapDiscoveryResult.AuthenticationFailure
        } catch (e: Exception) {
            Log.e(e, "Unable to get JMAP session")
            return JmapDiscoveryResult.GenericFailure(e)
        }

        val accounts = session.getAccounts(MailAccountCapability::class.java)
        val accountId = when {
            accounts.isEmpty() -> return JmapDiscoveryResult.NoEmailAccountFoundFailure
            accounts.size == 1 -> accounts.keys.first()
            else -> session.getPrimaryAccount(MailAccountCapability::class.java)
        }

        val account = accounts[accountId]!!
        val accountName = account.name ?: emailAddress
        return JmapDiscoveryResult.JmapAccount(accountId, accountName)
    }
}

sealed class JmapDiscoveryResult {
    class GenericFailure(val cause: Throwable) : JmapDiscoveryResult()
    object EndpointNotFoundFailure : JmapDiscoveryResult()
    object AuthenticationFailure : JmapDiscoveryResult()
    object NoEmailAccountFoundFailure : JmapDiscoveryResult()

    data class JmapAccount(val accountId: String, val name: String) : JmapDiscoveryResult()
}
