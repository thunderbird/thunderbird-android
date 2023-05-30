package app.k9mail.core.common.domain.usecase.validation

import app.k9mail.core.common.domain.usecase.UseCase

interface ValidationUseCase<T> : UseCase<T, ValidationResult>
