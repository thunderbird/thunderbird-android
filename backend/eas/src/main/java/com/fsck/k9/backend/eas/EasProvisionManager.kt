package com.fsck.k9.backend.eas

import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.eas.dto.Provision
import com.fsck.k9.backend.eas.dto.ProvisionPolicies
import com.fsck.k9.backend.eas.dto.ProvisionPolicy
import com.fsck.k9.mail.MessagingException

const val EAS_12_POLICY_TYPE = "MS-EAS-Provisioning-WBXML"
const val EXTRA_POLICY_KEY = "EXTRA_POLICY_KEY"
const val INITIAL_POLICY_KEY = "0"

open class EasProvisionManager(private val client: EasClient, private val backendStorage: BackendStorage) {
    open fun <T> ensureProvisioned(block: (() -> T)): T {
        if (client.policyKey == INITIAL_POLICY_KEY) {
            val policyKey = backendStorage.getExtraString(EXTRA_POLICY_KEY)

            if (policyKey == null) {
                provisionClient()
            } else {
                client.policyKey = policyKey
            }
        }
        return try {
            block()
        } catch (e: UnprovisionedException) {
            provisionClient()
            block()
        }
    }

    private fun provisionClient() {
        val tempPolicyKey = canProvision()

        ackProvision(tempPolicyKey).let {
            client.policyKey = it
            backendStorage.setExtraString(EXTRA_POLICY_KEY, it)
        }
    }

    private fun canProvision(): String {
        val provisionRequest = Provision(
                ProvisionPolicies(
                        ProvisionPolicy(
                                EAS_12_POLICY_TYPE
                        )
                )
        )

        val provisionResponse = client.provision(provisionRequest)
        if (provisionResponse.status != STATUS_OK) {
            throw MessagingException("Couldn't request user provision")
        }

        return provisionResponse.policies!!.policy.policyKey!!
    }

    private fun ackProvision(tempPolicyKey: String): String {
        val provisionRequest = Provision(
                ProvisionPolicies(
                        ProvisionPolicy(
                                EAS_12_POLICY_TYPE,
                                tempPolicyKey,
                                STATUS_OK
                        )
                )
        )

        val provisionResponse = client.provision(provisionRequest)
        if (provisionResponse.status != STATUS_OK) {
            throw MessagingException("Couldn't acknowledge user provision")
        }
        return provisionResponse.policies!!.policy.policyKey!!
    }
}
