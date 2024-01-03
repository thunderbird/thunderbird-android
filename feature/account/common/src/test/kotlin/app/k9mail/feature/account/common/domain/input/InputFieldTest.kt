package app.k9mail.feature.account.common.domain.input

import app.k9mail.core.common.domain.usecase.validation.ValidationError
import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotSameInstanceAs
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import assertk.assertions.prop
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

data class InputFieldTestData<T>(
    val name: String,
    val initialState: InputField<T>,
    val initialValue: T,
    val initialValueEmpty: T,
    val initialError: ValidationError?,
    val initialIsValid: Boolean,
    val createInitialInput: (value: T, error: ValidationError?, isValid: Boolean) -> InputField<T>,
    val updatedValue: T,
)

@RunWith(Parameterized::class)
class InputFieldTest(
    private val data: InputFieldTestData<Any>,
) {

    @Test
    fun `should set default values`() {
        assertThat(data.initialState).all {
            hasValue(data.initialValueEmpty)
            hasNoError()
            isNotValid()
        }
    }

    @Test
    fun `should reset error and isValid when value changed`() {
        val initialInput = data.createInitialInput(
            data.initialValue,
            TestValidationError,
            true,
        )

        val result = initialInput.updateValue(data.updatedValue)

        assertThat(result).all {
            isNotSameInstanceAs(initialInput)
            hasValue(data.updatedValue)
            hasNoError()
            isNotValid()
        }
    }

    @Test
    fun `should reset isValid when error set`() {
        val initialInput = data.createInitialInput(
            data.initialValue,
            null,
            true,
        )

        val result = initialInput.updateError(TestValidationError)

        assertThat(result).all {
            isNotSameInstanceAs(initialInput)
            hasValue(data.initialValue)
            hasError(TestValidationError)
            isNotValid()
        }
    }

    @Test
    fun `should reset error when valid`() {
        val initialInput = data.createInitialInput(
            data.initialValue,
            TestValidationError,
            false,
        )

        val result = initialInput.updateValidity(isValid = true)

        assertThat(result).all {
            isNotSameInstanceAs(initialInput)
            hasValue(data.initialValue)
            hasNoError()
            isValid()
        }
    }

    @Test
    fun `should not reset error when invalid`() {
        val initialInput = data.createInitialInput(
            data.initialValue,
            TestValidationError,
            false,
        )

        val result = initialInput.updateValidity(isValid = false)

        assertThat(result).all {
            isSameInstanceAs(initialInput)
            hasValue(data.initialValue)
            hasError(TestValidationError)
            isNotValid()
        }
    }

    @Test
    fun `should change error when error changed`() {
        val initialInput = data.createInitialInput(
            data.initialValue,
            TestValidationError,
            false,
        )

        val result = initialInput.updateError(TestValidationError2)

        assertThat(result).all {
            isNotSameInstanceAs(initialInput)
            hasValue(data.initialValue)
            hasError(TestValidationError2)
            isNotValid()
        }
    }

    @Test
    fun `should map from success ValidationResult`() {
        val initialInput = data.createInitialInput(
            data.initialValue,
            TestValidationError,
            false,
        )

        val result = initialInput.updateFromValidationResult(ValidationResult.Success)

        assertThat(result).all {
            isNotSameInstanceAs(initialInput)
            hasValue(data.initialValue)
            hasNoError()
            isValid()
        }
    }

    @Test
    fun `should map from failure ValidationResult`() {
        val initialInput = data.createInitialInput(
            data.initialValue,
            null,
            true,
        )

        val result = initialInput.updateFromValidationResult(ValidationResult.Failure(TestValidationError))

        assertThat(result).all {
            isNotSameInstanceAs(initialInput)
            hasValue(data.initialValue)
            hasError(TestValidationError)
            isNotValid()
        }
    }

    @Test
    fun `should decide equality on properties`() {
        val input1 = data.createInitialInput(
            data.initialValue,
            data.initialError,
            data.initialIsValid,
        )
        val input2 = data.createInitialInput(
            data.initialValue,
            data.initialError,
            data.initialIsValid,
        )

        assertThat(input1.equals(input2)).isTrue()
    }

    @Test
    fun `should have same hashCode`() {
        val input1 = data.createInitialInput(
            data.initialValue,
            data.initialError,
            data.initialIsValid,
        )
        val input2 = data.createInitialInput(
            data.initialValue,
            data.initialError,
            data.initialIsValid,
        )

        assertThat(input1.hashCode()).isEqualTo(input2.hashCode())
    }

    private fun Assert<InputField<Any>>.hasValue(value: Any) {
        prop("value") { InputField<*>::value.call(it) }.isEqualTo(value)
    }

    private fun Assert<InputField<Any>>.hasError(error: ValidationError) {
        prop("error") { InputField<*>::error.call(it) }.isEqualTo(error)
    }

    private fun Assert<InputField<Any>>.hasNoError() {
        prop("error") { InputField<*>::error.call(it) }.isNull()
    }

    private fun Assert<InputField<Any>>.isValid() {
        prop("isValid") { InputField<*>::isValid.call(it) }.isTrue()
    }

    private fun Assert<InputField<Any>>.isNotValid() {
        prop("isValid") { InputField<*>::isValid.call(it) }.isFalse()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): List<InputFieldTestData<*>> = listOf(
            InputFieldTestData(
                name = "StringInputField",
                createInitialInput = { value, error, isValid -> StringInputField(value, error, isValid) },
                initialState = StringInputField(),
                initialValue = "input",
                initialValueEmpty = "",
                initialError = null,
                initialIsValid = false,
                updatedValue = "new value",
            ),
            InputFieldTestData(
                name = "NumberInputField",
                createInitialInput = { value, error, isValid -> NumberInputField(value, error, isValid) },
                initialState = NumberInputField(),
                initialValue = 123L,
                initialValueEmpty = null,
                initialError = null,
                initialIsValid = false,
                updatedValue = 456L,
            ),
            InputFieldTestData(
                name = "BooleanInputField",
                createInitialInput = { value, error, isValid -> BooleanInputField(value, error, isValid) },
                initialState = BooleanInputField(),
                initialValue = true,
                initialValueEmpty = null,
                initialError = null,
                initialIsValid = false,
                updatedValue = false,
            ),
        )
    }

    private object TestValidationError : ValidationError
    private object TestValidationError2 : ValidationError
}
