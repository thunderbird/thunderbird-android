package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.feature.account.common.domain.entity.SpecialFolderOption
import app.k9mail.feature.account.common.domain.entity.SpecialFolderOptions
import app.k9mail.feature.account.setup.domain.DomainContract
import app.k9mail.feature.account.setup.domain.DomainContract.UseCase.ValidateSpecialFolderOptions.Failure
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import dev.forkhandles.fabrikate.Fabrikate
import net.thunderbird.core.common.domain.usecase.validation.ValidationResult
import org.junit.Test

class ValidateSpecialFolderOptionsTest {

    private val testSubject = createTestSubject()

    @Test
    fun `validate special folder options should succeed when all default options are present`() {
        val result = testSubject(SPECIAL_FOLDER_OPTIONS)

        assertThat(result).isInstanceOf<ValidationResult.Success>()
    }

    @Test
    fun `validate special folder options should fail when archive default option is missing`() {
        val specialFolderOptions = SPECIAL_FOLDER_OPTIONS.copy(
            archiveSpecialFolderOptions = listOf(
                SpecialFolderOption.None(
                    isAutomatic = true,
                ),
            ),
        )

        val result = testSubject(specialFolderOptions)

        assertThat(result).isInstanceOf<ValidationResult.Failure>()
            .prop(ValidationResult.Failure::error)
            .isInstanceOf<Failure.MissingDefaultSpecialFolderOption>()
    }

    @Test
    fun `validate special folder options should fail when drafts default option is missing`() {
        val specialFolderOptions = SPECIAL_FOLDER_OPTIONS.copy(
            draftsSpecialFolderOptions = listOf(
                SpecialFolderOption.None(
                    isAutomatic = true,
                ),
            ),
        )

        val result = testSubject(specialFolderOptions)

        assertThat(result).isInstanceOf<ValidationResult.Failure>()
            .prop(ValidationResult.Failure::error)
            .isInstanceOf<Failure.MissingDefaultSpecialFolderOption>()
    }

    @Test
    fun `validate special folder options should fail when sent default option is missing`() {
        val specialFolderOptions = SPECIAL_FOLDER_OPTIONS.copy(
            sentSpecialFolderOptions = listOf(
                SpecialFolderOption.None(
                    isAutomatic = true,
                ),
            ),
        )

        val result = testSubject(specialFolderOptions)

        assertThat(result).isInstanceOf<ValidationResult.Failure>()
            .prop(ValidationResult.Failure::error)
            .isInstanceOf<Failure.MissingDefaultSpecialFolderOption>()
    }

    @Test
    fun `validate special folder options should fail when spam default option is missing`() {
        val specialFolderOptions = SPECIAL_FOLDER_OPTIONS.copy(
            spamSpecialFolderOptions = listOf(
                SpecialFolderOption.None(
                    isAutomatic = true,
                ),
            ),
        )

        val result = testSubject(specialFolderOptions)

        assertThat(result).isInstanceOf<ValidationResult.Failure>()
            .prop(ValidationResult.Failure::error)
            .isInstanceOf<Failure.MissingDefaultSpecialFolderOption>()
    }

    @Test
    fun `validate special folder options should fail when trash default option is missing`() {
        val specialFolderOptions = SPECIAL_FOLDER_OPTIONS.copy(
            trashSpecialFolderOptions = listOf(
                SpecialFolderOption.None(
                    isAutomatic = true,
                ),
            ),
        )

        val result = testSubject(specialFolderOptions)

        assertThat(result).isInstanceOf<ValidationResult.Failure>()
            .prop(ValidationResult.Failure::error)
            .isInstanceOf<Failure.MissingDefaultSpecialFolderOption>()
    }

    private companion object {
        fun createTestSubject(): DomainContract.UseCase.ValidateSpecialFolderOptions = ValidateSpecialFolderOptions()

        val SPECIAL_FOLDER_OPTIONS = SpecialFolderOptions(
            archiveSpecialFolderOptions = listOf(
                SpecialFolderOption.Special(
                    isAutomatic = true,
                    remoteFolder = Fabrikate().random(),
                ),
            ),
            draftsSpecialFolderOptions = listOf(
                SpecialFolderOption.Special(
                    isAutomatic = true,
                    remoteFolder = Fabrikate().random(),
                ),
            ),
            sentSpecialFolderOptions = listOf(
                SpecialFolderOption.Special(
                    isAutomatic = true,
                    remoteFolder = Fabrikate().random(),
                ),
            ),
            spamSpecialFolderOptions = listOf(
                SpecialFolderOption.Special(
                    isAutomatic = true,
                    remoteFolder = Fabrikate().random(),
                ),
            ),
            trashSpecialFolderOptions = listOf(
                SpecialFolderOption.Special(
                    isAutomatic = true,
                    remoteFolder = Fabrikate().random(),
                ),
            ),
        )
    }
}
