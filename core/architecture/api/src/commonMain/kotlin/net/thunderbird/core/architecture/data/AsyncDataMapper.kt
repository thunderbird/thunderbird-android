package net.thunderbird.core.architecture.data

/**
 * Mapper definition for converting between domain models and data transfer objects (DTOs).
 *
 * @param TDomain The domain model type.
 * @param TDto The data transfer object type.
 */
interface AsyncDataMapper<TDomain, TDto> {
    suspend fun toDomain(dto: TDto): TDomain
    suspend fun toDto(domain: TDomain): TDto
}
