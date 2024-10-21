package app.k9mail.feature.funding.googleplay.ui.contribution

import app.k9mail.feature.funding.googleplay.domain.entity.OneTimeContribution
import app.k9mail.feature.funding.googleplay.domain.entity.RecurringContribution
import kotlinx.collections.immutable.persistentListOf

internal object FakeData {

    val recurringContribution = RecurringContribution(
        id = "contribution_tfa_monthly_m",
        title = "Monthly subscription: $50",
        description = "Monthly subscription for $50",
        price = 5000L,
        priceFormatted = "50 $",
    )

    val recurringContributions = persistentListOf(
        RecurringContribution(
            id = "contribution_tfa_monthly_xxl",
            title = "Monthly subscription: $250",
            description = "Monthly subscription for $250",
            price = 25000L,
            priceFormatted = "250 $",
        ),
        RecurringContribution(
            id = "contribution_tfa_monthly_xl",
            title = "Monthly subscription: $140",
            description = "Monthly subscription for $140",
            price = 14000L,
            priceFormatted = "140 $",
        ),
        RecurringContribution(
            id = "contribution_tfa_monthly_l",
            title = "Monthly subscription: $80",
            description = "Monthly subscription for $80",
            price = 8000L,
            priceFormatted = "80 $",
        ),
        RecurringContribution(
            id = "contribution_tfa_monthly_m",
            title = "Monthly subscription: $50",
            description = "Monthly subscription for $50",
            price = 5000L,
            priceFormatted = "50 $",
        ),
        RecurringContribution(
            id = "contribution_tfa_monthly_s",
            title = "Monthly subscription: $25",
            description = "Monthly subscription for $25",
            price = 2500L,
            priceFormatted = "25 $",
        ),
        RecurringContribution(
            id = "contribution_tfa_monthly_xs",
            title = "Monthly subscription: $15",
            description = "Monthly subscription for $15",
            price = 1500L,
            priceFormatted = "15 $",
        ),
    )

    val oneTimeContribution = OneTimeContribution(
        id = "contribution_tfa_onetime_m",
        title = "One time payment: $50",
        description = "One time payment for $50",
        price = 5000L,
        priceFormatted = "50 $",
    )

    val oneTimeContributions = persistentListOf(
        OneTimeContribution(
            id = "contribution_tfa_onetime_xxl",
            title = "One time payment: $250",
            description = "One time payment for $250",
            price = 25000L,
            priceFormatted = "250 $",
        ),
        OneTimeContribution(
            id = "contribution_tfa_onetime_xl",
            title = "One time payment: $140",
            description = "One time payment for $140",
            price = 14000L,
            priceFormatted = "140 $",
        ),
        OneTimeContribution(
            id = "contribution_tfa_onetime_l",
            title = "One time payment: $80",
            description = "One time payment for $80",
            price = 8000L,
            priceFormatted = "80 $",
        ),
        OneTimeContribution(
            id = "contribution_tfa_onetime_m",
            title = "One time payment: $50",
            description = "One time payment for $50",
            price = 5000L,
            priceFormatted = "50 $",
        ),
        OneTimeContribution(
            id = "contribution_tfa_onetime_s",
            title = "One time payment: $25",
            description = "One time payment for $25",
            price = 2500L,
            priceFormatted = "25 $",
        ),
        OneTimeContribution(
            id = "contribution_tfa_onetime_xs",
            title = "One time payment: $15",
            description = "One time payment for $15",
            price = 1500L,
            priceFormatted = "15 $",
        ),
    )
}
