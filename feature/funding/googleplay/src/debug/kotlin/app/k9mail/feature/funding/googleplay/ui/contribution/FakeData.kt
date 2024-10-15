package app.k9mail.feature.funding.googleplay.ui.contribution

import app.k9mail.feature.funding.googleplay.domain.entity.OneTimeContribution
import app.k9mail.feature.funding.googleplay.domain.entity.RecurringContribution
import kotlinx.collections.immutable.persistentListOf

internal object FakeData {

    val recurringContribution = RecurringContribution(
        id = "monthly_subscription_50",
        title = "Monthly subscription: $50",
        description = "Monthly subscription for $50",
        price = 5000L,
        priceFormatted = "50 $",
    )

    val recurringContributions = persistentListOf(
        RecurringContribution(
            id = "monthly_subscription_250",
            title = "Monthly subscription: $250",
            description = "Monthly subscription for $250",
            price = 25000L,
            priceFormatted = "250 $",
        ),
        RecurringContribution(
            id = "monthly_subscription_140",
            title = "Monthly subscription: $140",
            description = "Monthly subscription for $140",
            price = 14000L,
            priceFormatted = "140 $",
        ),
        RecurringContribution(
            id = "monthly_subscription_80",
            title = "Monthly subscription: $80",
            description = "Monthly subscription for $80",
            price = 8000L,
            priceFormatted = "80 $",
        ),
        RecurringContribution(
            id = "monthly_subscription_50",
            title = "Monthly subscription: $50",
            description = "Monthly subscription for $50",
            price = 5000L,
            priceFormatted = "50 $",
        ),
        RecurringContribution(
            id = "monthly_subscription_25",
            title = "Monthly subscription: $25",
            description = "Monthly subscription for $25",
            price = 2500L,
            priceFormatted = "25 $",
        ),
        RecurringContribution(
            id = "monthly_subscription_15",
            title = "Monthly subscription: $15",
            description = "Monthly subscription for $15",
            price = 1500L,
            priceFormatted = "15 $",
        ),
    )

    val oneTimeContribution = OneTimeContribution(
        id = "one_time_payment_50",
        title = "One time payment: $50",
        description = "One time payment for $50",
        price = 5000L,
        priceFormatted = "50 $",
    )

    val oneTimeContributions = persistentListOf(
        OneTimeContribution(
            id = "one_time_payment_250",
            title = "One time payment: $250",
            description = "One time payment for $250",
            price = 25000L,
            priceFormatted = "250 $",
        ),
        OneTimeContribution(
            id = "one_time_payment_140",
            title = "One time payment: $140",
            description = "One time payment for $140",
            price = 14000L,
            priceFormatted = "140 $",
        ),
        OneTimeContribution(
            id = "one_time_payment_80",
            title = "One time payment: $80",
            description = "One time payment for $80",
            price = 8000L,
            priceFormatted = "80 $",
        ),
        OneTimeContribution(
            id = "one_time_payment_50",
            title = "One time payment: $50",
            description = "One time payment for $50",
            price = 5000L,
            priceFormatted = "50 $",
        ),
        OneTimeContribution(
            id = "one_time_payment_25",
            title = "One time payment: $25",
            description = "One time payment for $25",
            price = 2500L,
            priceFormatted = "25 $",
        ),
        OneTimeContribution(
            id = "one_time_payment_15",
            title = "One time payment: $15",
            description = "One time payment for $15",
            price = 1500L,
            priceFormatted = "15 $",
        ),
    )
}
