package app.k9mail.feature.funding.googleplay.data

import app.k9mail.feature.funding.googleplay.domain.entity.Contribution
import app.k9mail.feature.funding.googleplay.domain.entity.OneTimeContribution
import app.k9mail.feature.funding.googleplay.domain.entity.RecurringContribution
import com.android.billingclient.api.ProductDetails

interface DataContract {

    interface Mapper {
        interface Product {
            fun mapToContribution(product: ProductDetails): Contribution

            fun mapToOneTimeContribution(product: ProductDetails): OneTimeContribution
            fun mapToRecurringContribution(product: ProductDetails): RecurringContribution
        }
    }
}
