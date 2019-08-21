package com.fsck.k9.backend.eas

import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.eas.dto.Provision
import com.fsck.k9.backend.eas.dto.ProvisionPolicies
import com.fsck.k9.backend.eas.dto.ProvisionPolicy
import com.fsck.k9.mail.MessagingException
import com.nhaarman.mockito_kotlin.*
import org.junit.Assert.assertEquals
import org.junit.Test

class EasProvisionManagerTest {

    private val backendStorage = mock<BackendStorage>()
    private val client = mock<EasClient>()

    @Test
    fun ensureProvisioned_alreadyProvisioned() {
        val cut = EasProvisionManager(client, backendStorage)
        client.policyKey = "123"

        val result = cut.ensureProvisioned {
            "OK"
        }

        assertEquals(result, "OK")
    }

    @Test
    fun ensureProvisioned_alreadyProvisioned_shouldLoadFromBackendStorage() {
        val cut = EasProvisionManager(client, backendStorage)
        whenever(client.policyKey).thenReturn("0")
        whenever(backendStorage.getExtraString("EXTRA_POLICY_KEY")).thenReturn("key")

        val result = cut.ensureProvisioned {
            "OK"
        }

        verify(client).policyKey = "key"
        assertEquals(result, "OK")
    }

    @Test
    fun ensureProvisioned_notProvisioned_shouldProvision() {
        val cut = EasProvisionManager(client, backendStorage)
        whenever(client.policyKey).thenReturn("0")
        whenever(backendStorage.getExtraString("EXTRA_POLICY_KEY")).thenReturn(null)

        whenever(client.provision(Provision(
                ProvisionPolicies(
                        ProvisionPolicy(
                                "MS-EAS-Provisioning-WBXML"
                        )
                )
        ))).thenReturn(Provision(
                ProvisionPolicies(
                        ProvisionPolicy(
                                "MS-EAS-Provisioning-WBXML",
                                "keyTemp",
                                1
                        )
                ),
                1
        ))

        whenever(client.provision(Provision(
                ProvisionPolicies(
                        ProvisionPolicy(
                                "MS-EAS-Provisioning-WBXML",
                                "keyTemp",
                                1
                        )
                )
        ))).thenReturn(Provision(
                ProvisionPolicies(
                        ProvisionPolicy(
                                "MS-EAS-Provisioning-WBXML",
                                "key",
                                1
                        )
                ),
                1
        ))


        val result = cut.ensureProvisioned {
            "OK"
        }

        assertEquals(result, "OK")
        verify(client).policyKey = "key"
        verify(backendStorage).setExtraString("EXTRA_POLICY_KEY", "key")
    }

    @Test
    fun ensureProvisioned_notProvisioned_shouldReProvision() {
        val cut = EasProvisionManager(client, backendStorage)
        whenever(client.policyKey).thenReturn("key0")

        whenever(client.provision(Provision(
                ProvisionPolicies(
                        ProvisionPolicy(
                                "MS-EAS-Provisioning-WBXML"
                        )
                )
        ))).thenReturn(Provision(
                ProvisionPolicies(
                        ProvisionPolicy(
                                "MS-EAS-Provisioning-WBXML",
                                "keyTemp",
                                1
                        )
                ),
                1
        ))

        whenever(client.provision(Provision(
                ProvisionPolicies(
                        ProvisionPolicy(
                                "MS-EAS-Provisioning-WBXML",
                                "keyTemp",
                                1
                        )
                )
        ))).thenReturn(Provision(
                ProvisionPolicies(
                        ProvisionPolicy(
                                "MS-EAS-Provisioning-WBXML",
                                "key1",
                                1
                        )
                ),
                1
        ))

        var first = true

        val result = cut.ensureProvisioned {
            if (first) {
                first = false
                throw UnprovisionedException()
            } else {
                "OK"
            }
        }

        assertEquals(result, "OK")
        verify(client).policyKey = "key1"
        verify(backendStorage).setExtraString("EXTRA_POLICY_KEY", "key1")
    }

    @Test(expected = MessagingException::class)
    fun ensureProvisioned_notProvisionedCanProvisionFailed_shouldThrow() {
        val cut = EasProvisionManager(client, backendStorage)
        whenever(client.policyKey).thenReturn("0")

        whenever(client.provision(any())).thenReturn(Provision(status = 2))

        cut.ensureProvisioned {}
    }

    @Test(expected = MessagingException::class)
    fun ensureProvisioned_notProvisionedAckProvisionFailed_shouldThrow() {
        val cut = EasProvisionManager(client, backendStorage)
        whenever(client.policyKey).thenReturn("0")
        whenever(client.provision(Provision(
                ProvisionPolicies(
                        ProvisionPolicy(
                                "MS-EAS-Provisioning-WBXML"
                        )
                )
        ))).thenReturn(Provision(
                ProvisionPolicies(
                        ProvisionPolicy(
                                "MS-EAS-Provisioning-WBXML",
                                "keyTemp",
                                1
                        )
                ),
                1
        ))

        whenever(client.provision(any())).thenReturn(Provision(status = 2))

        cut.ensureProvisioned {}
    }
}
