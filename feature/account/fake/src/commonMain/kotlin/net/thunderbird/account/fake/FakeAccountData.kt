package net.thunderbird.account.fake

import net.thunderbird.feature.account.AccountIdFactory

object FakeAccountData {

    const val ACCOUNT_ID_RAW = "bc722927-9197-417d-919e-6fd702038de1"
    val ACCOUNT_ID = AccountIdFactory.create(ACCOUNT_ID_RAW)
    const val ACCOUNT_ID_OTHER_RAW = "c2890a43-0f54-4a69-a0af-bdfce8d831ad"
    val ACCOUNT_ID_OTHER = AccountIdFactory.create(ACCOUNT_ID_OTHER_RAW)
}
