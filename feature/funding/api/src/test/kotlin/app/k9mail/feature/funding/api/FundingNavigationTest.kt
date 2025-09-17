package app.k9mail.feature.funding.api

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class FundingNavigationTest {

    @Test
    fun `given Contribution route then basePath and route are correct`() {
        // Arrange
        val expectedBase = "$FUNDING_BASE_DEEP_LINK/contribution"

        // Act
        val basePath = FundingRoute.Contribution.basePath
        val route = FundingRoute.Contribution.route()
        val constBase = FundingRoute.Contribution.BASE_PATH

        // Assert
        assertThat(FUNDING_BASE_DEEP_LINK).isEqualTo("app://feature/funding")
        assertThat(constBase).isEqualTo(expectedBase)
        assertThat(basePath).isEqualTo(expectedBase)
        assertThat(route).isEqualTo(expectedBase)
    }
}
