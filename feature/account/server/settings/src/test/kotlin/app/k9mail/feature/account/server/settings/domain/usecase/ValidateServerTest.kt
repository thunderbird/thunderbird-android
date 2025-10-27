package app.k9mail.feature.account.server.settings.domain.usecase

import app.k9mail.feature.account.server.settings.domain.usecase.ValidateServer.ValidateServerError
import assertk.Assert
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.validation.ValidationError
import net.thunderbird.core.validation.ValidationOutcome
import org.junit.Test

/**
 * Test data copied from Thunderbird Desktop `mailnews/base/test/unit/test_hostnameUtils.js`
 */
class ValidateServerTest {

    private val testSubject = ValidateServer()

    @Test
    fun `should fail when server is empty or blank`() {
        assertThat(validate("")).isFailureEmptyServer()
        assertThat(validate(" ")).isFailureEmptyServer()
    }

    @Test
    fun `should succeed when server is valid IPv4 address`() {
        assertThat(validate("1.2.3.4")).isSuccess()
        assertThat(validate("123.245.111.222")).isSuccess()
        assertThat(validate("255.255.255.255")).isSuccess()
        assertThat(validate("1.2.0.4")).isSuccess()
        assertThat(validate("1.2.3.4")).isSuccess()
        assertThat(validate("127.1.2.3")).isSuccess()
        assertThat(validate("10.1.2.3")).isSuccess()
        assertThat(validate("192.168.2.3")).isSuccess()
    }

    @Test
    fun `should succeed when server is valid IPv6 address`() {
        assertThat(validate("2001:0db8:85a3:0000:0000:8a2e:0370:7334")).isSuccess()
        assertThat(validate("2001:db8:85a3:0:0:8a2e:370:7334")).isSuccess()
        assertThat(validate("2001:db8:85a3::8a2e:370:7334")).isSuccess()
        assertThat(validate("2001:0db8:85a3:0000:0000:8a2e:0370:")).isSuccess()
        assertThat(validate("::ffff:c000:0280")).isSuccess()
        assertThat(validate("::ffff:192.0.2.128")).isSuccess()
        assertThat(validate("2001:db8::1")).isSuccess()
        assertThat(validate("2001:DB8::1")).isSuccess()
        assertThat(validate("1:2:3:4:5:6:7:8")).isSuccess()
        assertThat(validate("::1")).isSuccess()
        assertThat(validate("::0000:0000:1")).isSuccess()
    }

    @Test
    fun `should succeed when server is valid domain`() {
        assertThat(validate("localhost")).isSuccess()
        assertThat(validate("some-server")).isSuccess()
        assertThat(validate("server.company.other")).isSuccess()
        assertThat(validate("server.comp-any.other")).isSuccess()
        assertThat(validate("server.123.other")).isSuccess()
        assertThat(validate("1server.123.other")).isSuccess()
        assertThat(validate("1.2.3.4.5")).isSuccess()
        assertThat(validate("very.log.sub.domain.name.other")).isSuccess()
        assertThat(validate("1234567890")).isSuccess()
        assertThat(validate("1234567890.")).isSuccess()
        assertThat(validate("server.company.other.")).isSuccess()
    }

    @Test
    fun `should fail when server is invalid IPv4 address`() {
        assertThat(validate(".1.2.3")).isFailureInvalidDomainOrIpAddress()
        assertThat(validate("1.2..123")).isFailureInvalidDomainOrIpAddress()
    }

    @Test
    fun `should fail when server is invalid IPv6 address`() {
        assertThat(validate("::")).isFailureInvalidDomainOrIpAddress()
        assertThat(validate("2001:0db8:85a3:0000:0000:8a2e:0370:73346")).isFailureInvalidDomainOrIpAddress()
        assertThat(validate("2001:0db8:85a3:0000:0000:8a2e:0370:7334:1")).isFailureInvalidDomainOrIpAddress()
        assertThat(validate("2001:0db8:85a3:0000:0000:8a2e:0370:7334x")).isFailureInvalidDomainOrIpAddress()
        assertThat(validate("2001:0db8:85a3:0000:0000:8a2e:03707334")).isFailureInvalidDomainOrIpAddress()
        assertThat(validate("2001:0db8:85a3:0000:0000x8a2e:0370:7334")).isFailureInvalidDomainOrIpAddress()
        assertThat(validate("2001:0db8:85a3:0000:0000:::1")).isFailureInvalidDomainOrIpAddress()
        assertThat(validate("2001:0db8:85a3:0000:0000:0000:some:junk")).isFailureInvalidDomainOrIpAddress()
        assertThat(validate("2001:0db8:85a3:0000:0000:0000::192.0.2.359")).isFailureInvalidDomainOrIpAddress()
        assertThat(validate("some::junk")).isFailureInvalidDomainOrIpAddress()
        assertThat(validate("some_junk")).isFailureInvalidDomainOrIpAddress()
    }

    @Test
    fun `should fail when server is invalid domain`() {
        assertThat(validate("server.badcompany!.invalid")).isFailureInvalidDomainOrIpAddress()
        assertThat(validate("server._badcompany.invalid")).isFailureInvalidDomainOrIpAddress()
        assertThat(validate("server.bad_company.invalid")).isFailureInvalidDomainOrIpAddress()
        assertThat(validate("server.badcompany-.invalid")).isFailureInvalidDomainOrIpAddress()
        assertThat(validate("server.bad company.invalid")).isFailureInvalidDomainOrIpAddress()
        assertThat(validate("server.b…dcompany.invalid")).isFailureInvalidDomainOrIpAddress()
        assertThat(validate(".server.badcompany.invalid")).isFailureInvalidDomainOrIpAddress()
        assertThat(validate("make-this-a-long-host-name-component-that-is-over-63-characters-long.invalid"))
            .isFailureInvalidDomainOrIpAddress()

        val domain =
            "append-strings-to-make-this-a-too-long-host-name.that-is-really-over-255-characters-long.invalid." +
                "append-strings-to-make-this-a-too-long-host-name.that-is-really-over-255-characters-long.invalid." +
                "append-strings-to-make-this-a-too-long-host-name.that-is-really-over-255-characters-long.invalid." +
                "append-strings-to-make-this-a-too-long-host-name.that-is-really-over-255-characters-long.invalid"

        assertThat(validate(domain)).isFailureInvalidDomainOrIpAddress()
    }

    private fun validate(input: String): ValidationOutcome {
        return testSubject.execute(input)
    }

    private fun Assert<ValidationOutcome>.isSuccess() = isInstanceOf<Outcome.Success<Unit>>()

    private fun Assert<ValidationOutcome>.isFailureEmptyServer() =
        isInstanceOf<Outcome.Failure<ValidationError>>()
            .prop(Outcome.Failure<ValidationError>::error)
            .isInstanceOf(ValidateServerError.EmptyServer::class)

    private fun Assert<ValidationOutcome>.isFailureInvalidDomainOrIpAddress() =
        isInstanceOf<Outcome.Failure<ValidationError>>()
            .prop(Outcome.Failure<ValidationError>::error)
            .isInstanceOf(ValidateServerError.InvalidHostnameOrIpAddress::class)
}
