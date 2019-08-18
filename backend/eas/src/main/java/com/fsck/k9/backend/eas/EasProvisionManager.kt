package com.fsck.k9.backend.eas

import com.fsck.k9.backend.api.BackendStorage

const val EAS_12_POLICY_TYPE = "MS-EAS-Provisioning-WBXML"
const val EXTRA_POLICY_KEY = "EXTRA_POLICY_KEY"

class EasProvisionManager(private val client: EasClient, private val backendStorage: BackendStorage) {
    class ProvisionException : Exception("Couldn't provision user")

    fun <T> ensureProvisioned(block: (() -> T)): T {
        if (client.policyKey == "0") {
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
        ackProvision(canProvision()).let {
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
        if (provisionResponse.status != 1) {
            throw ProvisionException()
        }

        return provisionResponse.policies!!.policy.policyKey!!
    }

    private fun ackProvision(tempPolicyKey: String): String {
        val provisionRequest = Provision(
                ProvisionPolicies(
                        ProvisionPolicy(
                                EAS_12_POLICY_TYPE,
                                tempPolicyKey,
                                1
                        )
                )
        )

        val provisionResponse = client.provision(provisionRequest)
        if (provisionResponse.status != 1) {
            throw ProvisionException()
        }
        return provisionResponse.policies!!.policy.policyKey!!
    }
}
