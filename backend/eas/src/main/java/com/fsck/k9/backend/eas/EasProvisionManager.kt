package com.fsck.k9.backend.eas

import com.fsck.k9.backend.api.BackendStorage

const val EAS_12_POLICY_TYPE = "MS-EAS-Provisioning-WBXML"

const val EXTRA_POLICY_KEY = "EXTRA_POLICY_KEY"

class EasProvisionManager(val client: EasClient, val backendStorage: BackendStorage) {
    fun <T> ensureProvisioned(block: (() -> T)): T {
        if (client.policyKey == "0") {
            val policyKey = backendStorage.getExtraString(EXTRA_POLICY_KEY)
            println("POL->" + policyKey )

            if (policyKey == null) {
                provisionClient()
            } else {
                client.policyKey = policyKey
            }
        }
        try {
            return block()
        } catch (e: UnprovisionedException) {
            provisionClient()
            return block()
        }
    }

    private fun provisionClient() {
        client.policyKey = ackProvision(canProvision())
        backendStorage.setExtraString(EXTRA_POLICY_KEY, client.policyKey)
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
        return provisionResponse.policies!!.policy.policyKey!!
    }
}
