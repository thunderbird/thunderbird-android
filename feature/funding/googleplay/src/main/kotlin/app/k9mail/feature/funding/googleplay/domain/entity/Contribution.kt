package app.k9mail.feature.funding.googleplay.domain.entity

interface Contribution {
    val id: String
    val title: String
    val description: String
    val price: Long
    val priceFormatted: String
}
