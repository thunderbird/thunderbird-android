package net.thunderbird.feature.funding.googleplay.data.remote.bilingclient

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import assertk.assertThat
import assertk.assertions.isNotNull
import assertk.assertions.isNotSameInstanceAs
import assertk.assertions.isSameInstanceAs
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import kotlin.test.BeforeTest
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BillingClientProviderTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val listener: PurchasesUpdatedListener = TestPurchasesUpdatedListener()
    private lateinit var testSubject: BillingClientProvider

    @BeforeTest
    fun setUp() {
        testSubject = BillingClientProvider(context)
    }

    @Test
    fun `current returns billing client even if listener not set`() {
        val client = testSubject.current

        assertThat(client).isNotNull()
    }

    @Test
    fun `current returns billing client when listener is set`() {
        testSubject.setPurchasesUpdatedListener(listener)

        val client = testSubject.current

        assertThat(client).isNotNull()
    }

    @Test
    fun `current returns same instance when called multiple times`() {
        testSubject.setPurchasesUpdatedListener(listener)

        val client1 = testSubject.current
        val client2 = testSubject.current

        assertThat(client1).isSameInstanceAs(client2)
    }

    @Test
    fun `clear resets instance`() {
        testSubject.setPurchasesUpdatedListener(listener)
        val client1 = testSubject.current

        testSubject.clear()

        val client2 = testSubject.current
        assertThat(client1).isNotNull()
        assertThat(client2).isNotNull()
        assertThat(client1).isNotSameInstanceAs(client2)
    }

    private class TestPurchasesUpdatedListener : PurchasesUpdatedListener {
        override fun onPurchasesUpdated(
            billingResult: BillingResult,
            purchases: MutableList<Purchase>?,
        ) {
            // Nothing to do
        }
    }
}
