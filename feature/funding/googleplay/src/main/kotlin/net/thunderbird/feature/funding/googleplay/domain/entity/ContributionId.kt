package net.thunderbird.feature.funding.googleplay.domain.entity

@JvmInline
internal value class ContributionId(val value: String) {
    override fun toString(): String = value
}
