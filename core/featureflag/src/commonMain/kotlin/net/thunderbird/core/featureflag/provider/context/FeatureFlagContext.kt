package net.thunderbird.core.featureflag.provider.context

/**
 * Evaluation context for feature flag resolution containing attributes that influence flag behaviour.
 *
 * Provides a type-safe container for context attributes used during feature flag evaluation,
 * such as targeting key and custom attributes.
 *
 * @remarks sealed to prevent other modules to implement it.
 */
sealed class FeatureFlagContext(protected val wrapper: Map<String, Value>) :
    Map<String, FeatureFlagContext.Value> by wrapper {
    abstract val targetingKey: String

    sealed interface Value {
        fun asString(): kotlin.String? = if (this is String) value else null

        @JvmInline
        value class String(val value: kotlin.String) : Value

        @JvmInline
        value class Int(val value: kotlin.Int) : Value
    }
}

/**
 * An immutable implementation of FeatureFlagContext with a fixed targeting key and attributes.
 *
 * This implementation creates a snapshot of the context state at construction time,
 * ensuring that the targeting key and attributes cannot be modified after instantiation.
 * The attributes map is defensively copied to prevent external modifications.
 *
 * @param targetingKey The unique identifier used for feature flag targeting and evaluation.
 * @param attributes Optional map of custom attributes that provide additional context for flag evaluation.
 */
class ImmutableFeatureFlagContext(
    override val targetingKey: String,
    attributes: Map<String, Value> = mapOf(),
) : FeatureFlagContext(wrapper = attributes.toMap())
