package net.thunderbird.core.architecture.model

import kotlin.uuid.Uuid
import org.jetbrains.annotations.VisibleForTesting

/**
 * Abstract factory providing bidirectional conversion between legacy [Long] identifiers and
 * UUID-based entity identifiers.
 *
 * Legacy IDs must be non-negative because they originate from database primary keys.
 *
 * Legacy IDs are encoded as UUIDv8 using the byte layout `D3ADC0D3-HHHH-8T00-8000-LLLLLLLLLLLL`,
 * where:
 *
 * - `D3ADC0D3` marks the legacy range.
 * - `HHHH` holds the upper 16 bits of the legacy [Long].
 * - `8T00` holds the UUID version (`8`), the entity type `T` as defined by [ByteRepresentation],
 * and a fixed byte.
 * - `8000` is a fixed marker.
 * - `LLLLLLLLLLLL` holds the lower 48 bits of the legacy [Long].
 *
 * @param TEntityId The type of entity identifier produced by this factory.
 * @param entityByteRepresentation The byte representation identifying the entity type in the encoding.
 * @see ByteRepresentation
 * @see LegacyRange
 * @see FixedPayload
 * @see upper16bits
 * @see lower48bits
 */
abstract class LegacyEntityIdFactory<TEntityId : BaseUuidIdentifier>(
    private val entityByteRepresentation: ByteRepresentation,
    private val fromUuid: (Uuid) -> TEntityId,
) {
    /**
     * The most significant 16 bits of a legacy numeric identifier, obtained by shifting the value
     * right by 48 positions and masking the result to 16 bits.
     *
     * A legacy identifier does not fit contiguously into the UUID layout, as the middle bits are
     * reserved for the UUID version, the entity type, and the fixed marker. It is therefore split
     * in two parts, this one being written to bytes 4-5 of the UUID.
     */
    private val Long.upper16bits: Long
        get() = (this ushr 48) and 0xFFFFL

    /**
     * The least significant 48 bits of a legacy numeric identifier, obtained by masking out the
     * upper 16 bits.
     *
     * This is the counterpart of [upper16bits] and holds the payload of virtually every legacy
     * identifier, as values beyond 48 bits are not expected in practice. It is written to bytes
     * 10-15 of the UUID.
     */
    private val Long.lower48bits: Long
        get() = this and 0x0000_FFFF_FFFF_FFFFL

    /**
     * Creates an entity identifier encoding the given legacy numeric ID.
     *
     * @param legacyId The non-negative legacy numeric identifier to encode.
     * @return An entity identifier containing the encoded legacy ID.
     * @throws IllegalArgumentException If [legacyId] is negative.
     */
    fun of(legacyId: Long): TEntityId {
        require(legacyId >= 0) { "Legacy IDs must be non-negative" }
        return fromUuid(generateUuid(legacyId))
    }

    /**
     * Decodes the legacy numeric ID previously encoded by [of], reading it from the upper 16 bits
     * and the lower 48 bits of the UUID.
     *
     * @param id The entity identifier to decode.
     * @return The legacy numeric identifier extracted from the UUID.
     */
    fun toLegacyId(id: TEntityId): Long =
        id.value.toLongs { mostSignificantBits, leastSignificantBits ->
            val upper = (mostSignificantBits ushr 16) and 0xFFFFL
            val lower = (leastSignificantBits and 0x0000_FFFF_FFFF_FFFFL)
            Uuid.from(upper16 = upper, lower48 = lower)
        }

    /**
     * Checks whether an entity identifier encodes a legacy numeric ID, by verifying the legacy
     * range marker and the fixed payload.
     *
     * @param id The entity identifier to check.
     * @return `true` if the identifier encodes a legacy ID, `false` otherwise.
     */
    fun isLegacy(id: TEntityId): Boolean {
        return id.value.toLongs { mostSignificantBits, leastSignificantBits ->
            val currentRange = (mostSignificantBits ushr 32)
            val currentFixedPayload = (leastSignificantBits ushr 48)
            currentRange == LegacyRange.bytes && currentFixedPayload == FixedPayload.bytes
        }
    }

    /**
     * Encodes a legacy ID into a UUID with the following byte layout:
     *
     * - Bytes 0-3: legacy range marker.
     * - Bytes 4-5: upper 16 bits of the legacy ID.
     * - Bytes 6-7: [entityByteRepresentation].
     * - Bytes 8-9: fixed payload.
     * - Bytes 10-15: lower 48 bits of the legacy ID.
     *
     * @param legacyId The legacy numeric identifier to encode.
     * @return A UUID encoding the legacy ID and the entity type.
     */
    private fun generateUuid(legacyId: Long): Uuid {
        val upper = legacyId.upper16bits
        val lower = legacyId.lower48bits
        val bytes = ByteArray(size = 16)
        val legacyRange = LegacyRange.bytes
        val fixedPayload = FixedPayload.bytes
        bytes.apply {
            this[0] = (legacyRange ushr 24).toByte()
            this[1] = (legacyRange ushr 16).toByte()
            this[2] = (legacyRange ushr 8).toByte()
            this[3] = legacyRange.toByte()
            this[4] = (upper ushr 8).toByte()
            this[5] = upper.toByte()
            this[6] = (entityByteRepresentation.bytes ushr 8).toByte()
            this[7] = entityByteRepresentation.bytes.toByte()
            this[8] = (fixedPayload ushr 8).toByte()
            this[9] = fixedPayload.toByte()
            this[10] = (lower ushr 40).toByte()
            this[11] = (lower ushr 32).toByte()
            this[12] = (lower ushr 24).toByte()
            this[13] = (lower ushr 16).toByte()
            this[14] = (lower ushr 8).toByte()
            this[15] = lower.toByte()
        }
        return Uuid.fromByteArray(bytes)
    }

    /**
     * Recombines the two halves of a legacy numeric ID into a single [Long].
     *
     * @param upper16 The most significant 16 bits of the legacy ID.
     * @param lower48 The least significant 48 bits of the legacy ID.
     * @return The recombined legacy numeric identifier.
     */
    private fun Uuid.Companion.from(upper16: Long, lower48: Long): Long =
        ((upper16 and 0xFFFFL) shl 48) or (lower48 and 0x0000_FFFF_FFFF_FFFFL)

    /**
     * Unique byte markers distinguishing the entity types encoded in legacy UUID-based identifiers.
     *
     * @property bytes The marker written to bytes 6-7 of the UUID.
     */
    enum class ByteRepresentation(val bytes: Int) {
        MESSAGE(bytes = 0x8000),
        MESSAGE_PARTS(bytes = 0x8100),
        THREADS(bytes = 0x8200),
        FOLDERS(bytes = 0x8300),
        FOLDERS_EXTRA_VALUES(bytes = 0x8400),
        OUTBOX_STATE(bytes = 0x8500),
        PENDING_COMMANDS(bytes = 0x8600),
        NOTIFICATIONS(bytes = 0x8700),
        ;

        @VisibleForTesting
        internal fun toHexString(): String = bytes.toHexString(format = DefaultHexFormat)
    }

    @JvmInline
    internal value class LegacyRange(val bytes: Long) {
        override fun toString(): String = bytes.toHexString(format = DefaultHexFormat)
    }

    @JvmInline
    internal value class FixedBitMarker(val bytes: Long) {
        override fun toString(): String = bytes.toHexString(format = DefaultHexFormat)
    }

    companion object {
        /**
         * Magic number used to identify UUIDs that encode legacy numeric identifiers.
         *
         * This constant is embedded in the most significant bits of UUIDs created from
         * legacy IDs, allowing the factory to distinguish between newly generated UUIDs
         * and those converted from legacy numeric identifiers.
         */
        @VisibleForTesting
        internal val LegacyRange = LegacyRange(bytes = 0xD3ADC0D3)

        /**
         * A fixed bit pattern used as a marker in the UUID byte layout to identify
         * legacy entity identifiers.
         *
         * This value is stored in bits 80-95 of the UUID and is used to verify that
         * an identifier was created from a legacy numeric ID during the isLegacy check.
         */
        @VisibleForTesting
        internal val FixedPayload = FixedBitMarker(bytes = 0x8000)
        private val DefaultHexFormat = HexFormat {
            number { removeLeadingZeros = true }
        }
    }
}
