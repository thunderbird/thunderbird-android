package net.thunderbird.core.architecture.data

/**
 * Mapper definition for converting between domain models and data transfer objects (DTOs).
 *
 * @param Domain The domain model type.
 * @param Dto The data transfer object type.
 */
interface DataMapper<Domain, Dto> {
    fun toDomain(dto: Dto): Domain
    fun toDto(domain: Domain): Dto
}
