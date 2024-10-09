package app.k9mail.feature.funding.googleplay.ui.contribution

import app.k9mail.feature.funding.googleplay.domain.entity.OneTimeContribution
import app.k9mail.feature.funding.googleplay.domain.entity.RecurringContribution
import kotlinx.collections.immutable.persistentListOf

internal object FakeData {

    val recurringContributions = persistentListOf(
        RecurringContribution(
            id = "monthly_subscription_250",
            title = "Monthly subscription: $250",
            price = "$250",
        ),
        RecurringContribution(
            id = "monthly_subscription_140",
            title = "Monthly subscription: $140",
            price = "$140",
        ),
        RecurringContribution(
            id = "monthly_subscription_80",
            title = "Monthly subscription: $80",
            price = "$80",
        ),
        RecurringContribution(
            id = "monthly_subscription_50",
            title = "Monthly subscription: $50",
            price = "$50",
        ),
        RecurringContribution(
            id = "monthly_subscription_25",
            title = "Monthly subscription: $25",
            price = "$25",
        ),
        RecurringContribution(
            id = "monthly_subscription_15",
            title = "Monthly subscription: $15",
            price = "$15",
        ),
    )

    val oneTimeContributions = persistentListOf(
        OneTimeContribution(
            id = "one_time_payment_250",
            title = "One time payment: $250",
            price = "$250",
        ),
        OneTimeContribution(
            id = "one_time_payment_140",
            title = "One time payment: $140",
            price = "$140",
        ),
        OneTimeContribution(
            id = "one_time_payment_80",
            title = "One time payment: $80",
            price = "$80",
        ),
        OneTimeContribution(
            id = "one_time_payment_50",
            title = "One time payment: $50",
            price = "$50",
        ),
        OneTimeContribution(
            id = "one_time_payment_25",
            title = "One time payment: $25",
            price = "$25",
        ),
        OneTimeContribution(
            id = "one_time_payment_15",
            title = "One time payment: $15",
            price = "$15",
        ),
    )
}
