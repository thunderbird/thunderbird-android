package net.thunderbird.feature.funding.googleplay.ui.contribution.purchase

import app.k9mail.core.ui.compose.testing.mvi.MviContext
import app.k9mail.core.ui.compose.testing.mvi.MviTurbines
import app.k9mail.core.ui.compose.testing.mvi.advanceUntilIdle
import app.k9mail.core.ui.compose.testing.mvi.runMviTest
import app.k9mail.core.ui.compose.testing.mvi.turbines
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.flow.flowOf
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.UseCase
import net.thunderbird.feature.funding.googleplay.ui.contribution.FakeContributionRepository
import net.thunderbird.feature.funding.googleplay.ui.contribution.FakeData
import net.thunderbird.feature.funding.googleplay.ui.contribution.purchase.PurchaseSliceContract.Effect
import net.thunderbird.feature.funding.googleplay.ui.contribution.purchase.PurchaseSliceContract.Event
import net.thunderbird.feature.funding.googleplay.ui.contribution.purchase.PurchaseSliceContract.PurchaseFlow
import net.thunderbird.feature.funding.googleplay.ui.contribution.purchase.PurchaseSliceContract.State
import org.junit.Test

class PurchaseSliceTest {

    private val repository = FakeContributionRepository()

    @Test
    fun `should load latest purchase on init`() = runMviTest {
        // Arrange
        val purchasedContribution = FakeData.purchasedOneTimeContribution
        val getLatestPurchasedContribution = UseCase.GetLatestPurchasedContribution {
            flowOf(Outcome.success(purchasedContribution))
        }

        purchaseRobot(getLatestPurchasedContribution = getLatestPurchasedContribution) {
            // Assert
            verifyState { assertThat(it.purchasedContribution).isEqualTo(purchasedContribution) }
            verifyEffect(Effect.Purchased(purchasedContribution.id))
        }
    }

    @Test
    fun `should emmit empty purchase id initially when repository returns null`() = runMviTest {
        purchaseRobot {
            // Act
            advanceUntilIdle()

            // Assert
            verifyEffect(Effect.Purchased(null))
        }
    }

    @Test
    fun `should purchase contribution when purchase clicked`() = runMviTest {
        // Arrange
        val contributionId = FakeData.oneTimeContribution.id

        purchaseRobot {
            // Consume emissions from init
            advanceUntilIdle()
            verifyEffect(Effect.Purchased(null))

            // Act
            event(Event.PurchaseClicked(contributionId))

            // Assert
            verifyState {
                assertThat(it.purchaseFlow).isEqualTo(PurchaseFlow.Launching(contributionId))
            }

            verifyState {
                assertThat(it.purchaseFlow).isEqualTo(PurchaseFlow.Waiting(contributionId))
            }
        }
    }

    @Test
    fun `should handle purchase error and clear flow when purchase clicked`() = runMviTest {
        // Arrange
        val contributionId = FakeData.oneTimeContribution.id
        val repository = FakeContributionRepository().apply {
            purchaseResult = Outcome.failure(ContributionError.PurchaseFailed("Error"))
        }

        purchaseRobot(repository = repository) {
            // Consume emissions from init
            advanceUntilIdle()
            verifyEffect(Effect.Purchased(null))

            // Act
            event(Event.PurchaseClicked(contributionId))

            // Assert
            verifyState {
                assertThat(it.purchaseFlow).isEqualTo(PurchaseFlow.Launching(contributionId))
            }

            verifyState {
                assertThat(
                    it.purchaseFlow,
                ).isEqualTo(PurchaseFlow.Failed(contributionId, ContributionError.PurchaseFailed("Error")))
            }
        }
    }

    private suspend fun MviContext.purchaseRobot(
        getLatestPurchasedContribution: UseCase.GetLatestPurchasedContribution = {
            flowOf(Outcome.success(null))
        },
        repository: FakeContributionRepository = this@PurchaseSliceTest.repository,
        initialState: State = State(),
        block: suspend PurchaseRobot.() -> Unit,
    ) {
        val testSubject = PurchaseSlice(
            getLastestPurchase = getLatestPurchasedContribution,
            repository = repository,
            logger = TestLogger(),
            scope = testScope.backgroundScope,
            initialState = initialState,
        )
        val turbines = turbines(testSubject)
        assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(initialState)

        PurchaseRobot(testSubject, turbines).block()
    }

    private class PurchaseRobot(
        private val testSubject: PurchaseSlice,
        val turbines: MviTurbines<State, Effect>,
    ) {

        fun event(event: Event) {
            testSubject.event(event)
        }

        suspend fun verifyState(block: (State) -> Unit) {
            block(turbines.stateTurbine.awaitItem())
        }

        suspend fun verifyEffect(effect: Effect) {
            assertThat(turbines.effectTurbine.awaitItem()).isEqualTo(effect)
        }
    }
}
