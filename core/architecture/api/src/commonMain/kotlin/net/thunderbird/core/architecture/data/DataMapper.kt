package net.thunderbird.core.architecture.data

/**
 * Mapper definition for converting between domain models and data transfer objects (DTOs).
 *
 * @param TDomain The domain model type.
 * @param TDto The data transfer object type.
 */
interface DataMapper<TDomain, TDto> {
    fun toDomain(dto: TDto): TDomain
    fun toDto(domain: TDomain): TDto
}
