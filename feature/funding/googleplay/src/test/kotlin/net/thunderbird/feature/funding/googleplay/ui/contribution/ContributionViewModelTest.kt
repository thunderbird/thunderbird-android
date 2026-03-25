package net.thunderbird.feature.funding.googleplay.ui.contribution

import app.k9mail.core.ui.compose.testing.mvi.MviContext
import app.k9mail.core.ui.compose.testing.mvi.MviTurbines
import app.k9mail.core.ui.compose.testing.mvi.runMviTest
import app.k9mail.core.ui.compose.testing.mvi.turbinesWithInitialStateCheck
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.testing.coroutines.MainDispatcherHelper
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract
import net.thunderbird.feature.funding.googleplay.domain.entity.AvailableContributions
import net.thunderbird.feature.funding.googleplay.domain.entity.Contribution
import net.thunderbird.feature.funding.googleplay.domain.entity.PurchasedContribution
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.ContributionListState
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.ContributionType
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.Effect
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.Event
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.State

class ContributionViewModelTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val mainDispatcher = MainDispatcherHelper(UnconfinedTestDispatcher())

    @BeforeTest
    fun setUp() {
        mainDispatcher.setUp()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcher.tearDown()
    }

    @Test
    fun `should change selected contribution and selected type when one time contribution selected`() = runMviTest {
        // Arrange
        val initialState = State(
            listState = ContributionListState(
                oneTimeContributions = FakeData.oneTimeContributions.toImmutableList(),
                recurringContributions = FakeData.recurringContributions.toImmutableList(),
                preselection = FakeData.preselection,
                selectedContributionId = FakeData.preselection.recurringId,
                isRecurringContributionSelected = true,
                isLoading = false,
            ),
            purchasedContribution = null,
            showContributionList = true,
        )

        contributionRobot(initialState) {
            // Act
            selectOneTimeContribution()

            // Assert
            verifyOneTimeContributionSelected()
        }
    }

    @Test
    fun `should change selected contribution and selected type when recurring contribution selected`() = runMviTest {
        // Arrange
        val initialState = State(
            listState = ContributionListState(
                oneTimeContributions = FakeData.oneTimeContributions.toImmutableList(),
                recurringContributions = FakeData.recurringContributions.toImmutableList(),
                preselection = FakeData.preselection,
                selectedContributionId = FakeData.preselection.oneTimeId,
                isRecurringContributionSelected = false,
                isLoading = false,
            ),
            purchasedContribution = null,
            showContributionList = true,
        )

        contributionRobot(initialState) {
            // Act
            selectRecurringContribution()

            // Assert
            verifyRecurringContributionSelected()
        }
    }

    @Test
    fun `should change selected contribution when contribution item clicked`() = runMviTest {
        // Arrange
        val selectedContribution = FakeData.recurringContributions[0]
        val initialState = State(
            listState = ContributionListState(
                oneTimeContributions = FakeData.oneTimeContributions.toImmutableList(),
                recurringContributions = FakeData.recurringContributions.toImmutableList(),
                preselection = FakeData.preselection,
                selectedContributionId = FakeData.preselection.recurringId,
                isRecurringContributionSelected = true,
                isLoading = false,
            ),
            purchasedContribution = null,
            showContributionList = true,
        )

        contributionRobot(initialState) {
            // Act
            selectContributionItem(selectedContribution)

            // Assert
            verifyContributionItemSelected(selectedContribution)
        }
    }

    @Test
    fun `should hide list when purchase successful`() = runMviTest {
        // Arrange
        val repository = FakeContributionRepository()
        val purchasedContribution = FakeData.purchasedOneTimeContribution
        val initialState = State(
            listState = ContributionListState(
                oneTimeContributions = FakeData.oneTimeContributions.toImmutableList(),
                recurringContributions = FakeData.recurringContributions.toImmutableList(),
                preselection = FakeData.preselection,
                selectedContributionId = FakeData.oneTimeContributions[0].id,
                isRecurringContributionSelected = false,
                isLoading = false,
            ),
            purchasedContribution = null,
            showContributionList = true,
            isPurchasing = false,
        )

        contributionRobot(initialState = initialState, repository = repository) {
            // Act
            repository.purchasedContribution.value = Outcome.success(purchasedContribution)

            // Assert
            verifyListHidden(purchasedContribution)
        }
    }

    @Test
    fun `should show loading and disable button when purchase clicked`() = runMviTest {
        // Arrange
        val repository = FakeContributionRepository()
        val initialState = State(
            listState = ContributionListState(
                oneTimeContributions = FakeData.oneTimeContributions.toImmutableList(),
                recurringContributions = FakeData.recurringContributions.toImmutableList(),
                preselection = FakeData.preselection,
                selectedContributionId = FakeData.oneTimeContributions[0].id,
                isRecurringContributionSelected = false,
                isLoading = false,
            ),
            purchasedContribution = null,
            showContributionList = true,
        )

        contributionRobot(initialState = initialState, repository = repository) {
            // Act
            clickPurchase()

            // Assert
            verifyPurchasingState()
        }
    }

    @Test
    fun `should dismiss loading and not show error when purchase cancelled by user`() = runMviTest {
        val repository = FakeContributionRepository()
        val oneTimeContributions = FakeData.oneTimeContributions
        val recurringContributions = FakeData.recurringContributions
        val preselection = FakeData.preselection
        val selectedContributionId = preselection.recurringId!!
        val initialState = State(
            listState = ContributionListState(
                oneTimeContributions = oneTimeContributions.toImmutableList(),
                recurringContributions = recurringContributions.toImmutableList(),
                preselection = preselection,
                selectedContributionId = selectedContributionId,
                isRecurringContributionSelected = true,
                isLoading = false,
            ),
            purchasedContribution = null,
            showContributionList = true,
        )

        contributionRobot(
            initialState = initialState,
            repository = repository,
        ) {
            // Act
            clickPurchase()
            verifyPurchasingState()

            repository.purchasedContribution.emit(
                Outcome.failure(FundingDomainContract.ContributionError.UserCancelled("Cancelled")),
            )

            // Assert
            verifyPurchasingStateDismissed(purchaseError = null)
        }
    }

    @Test
    fun `should keep previously purchased contribution when subsequent purchase cancelled`() = runMviTest {
        val repository = FakeContributionRepository()
        val purchasedContribution = FakeData.purchasedOneTimeContribution
        repository.purchasedContribution.value = Outcome.success(purchasedContribution)

        val listState = ContributionListState(
            oneTimeContributions = FakeData.oneTimeContributions.toImmutableList(),
            recurringContributions = FakeData.recurringContributions.toImmutableList(),
            preselection = FakeData.preselection,
            selectedContributionId = FakeData.oneTimeContributions[1].id,
            isRecurringContributionSelected = false,
            isLoading = false,
        )
        val initialState = State(
            listState = listState,
            purchasedContribution = purchasedContribution,
            showContributionList = false,
        )

        contributionRobot(
            initialState = initialState,
            repository = repository,
        ) {
            // Act
            clickPurchase()
            verifyPurchasingState()

            repository.purchasedContribution.emit(
                Outcome.failure(FundingDomainContract.ContributionError.UserCancelled("Cancelled")),
            )

            // Assert
            verifyPurchasingStateDismissed(purchaseError = null)
        }
    }

    @Test
    fun `should keep previously purchased contribution when subsequent purchase failed`() = runMviTest {
        val repository = FakeContributionRepository()
        val purchasedContribution = FakeData.purchasedOneTimeContribution
        repository.purchasedContribution.value = Outcome.success(purchasedContribution)

        val listState = ContributionListState(
            oneTimeContributions = FakeData.oneTimeContributions.toImmutableList(),
            recurringContributions = FakeData.recurringContributions.toImmutableList(),
            preselection = FakeData.preselection,
            selectedContributionId = FakeData.oneTimeContributions[1].id,
            isRecurringContributionSelected = false,
            isLoading = false,
        )
        val initialState = State(
            listState = listState,
            purchasedContribution = purchasedContribution,
            showContributionList = false,
        )
        val error = FundingDomainContract.ContributionError.PurchaseFailed("Failed")

        contributionRobot(
            initialState = initialState,
            repository = repository,
        ) {
            // Act
            clickPurchase()
            verifyPurchasingState()

            repository.purchasedContribution.emit(
                Outcome.failure(error),
            )

            // Assert
            verifyPurchasingStateDismissed(purchaseError = error)
        }
    }

    @Test
    fun `should dismiss loading and show error when purchase failed`() = runMviTest {
        val repository = FakeContributionRepository()
        val oneTimeContributions = FakeData.oneTimeContributions
        val recurringContributions = FakeData.recurringContributions
        val preselection = FakeData.preselection
        val selectedContributionId = preselection.recurringId!!
        val initialState = State(
            listState = ContributionListState(
                oneTimeContributions = oneTimeContributions.toImmutableList(),
                recurringContributions = recurringContributions.toImmutableList(),
                preselection = preselection,
                selectedContributionId = selectedContributionId,
                isRecurringContributionSelected = true,
                isLoading = false,
            ),
            purchasedContribution = null,
            showContributionList = true,
        )
        val error = FundingDomainContract.ContributionError.PurchaseFailed("Failed")

        contributionRobot(
            initialState = initialState,
            repository = repository,
        ) {
            // Act
            clickPurchase()
            verifyPurchasingState()

            repository.purchasedContribution.emit(
                Outcome.failure(error),
            )

            // Assert
            verifyPurchasingStateDismissed(purchaseError = error)
        }
    }

    @Test
    fun `should dismiss loading and cancel job when purchase cancelled`() = runMviTest {
        val repository = FakeContributionRepository()
        val initialState = State(
            listState = ContributionListState(
                oneTimeContributions = FakeData.oneTimeContributions.toImmutableList(),
                recurringContributions = FakeData.recurringContributions.toImmutableList(),
                preselection = FakeData.preselection,
                selectedContributionId = FakeData.oneTimeContributions[0].id,
                isRecurringContributionSelected = false,
                isLoading = false,
            ),
            purchasedContribution = null,
            showContributionList = true,
        )

        contributionRobot(
            initialState = initialState,
            repository = repository,
        ) {
            // Act
            clickPurchase()
            verifyPurchasingState()

            clickCancelPurchase()

            // Assert
            verifyPurchasingStateDismissed(purchaseError = null)
        }
    }

    @Test
    fun `should dismiss loading when purchase cancelled multiple times`() = runMviTest {
        val repository = FakeContributionRepository()
        val oneTimeContributions = FakeData.oneTimeContributions
        val recurringContributions = FakeData.recurringContributions
        val preselection = FakeData.preselection
        val selectedContributionId = preselection.recurringId!!
        val initialState = State(
            listState = ContributionListState(
                oneTimeContributions = oneTimeContributions.toImmutableList(),
                recurringContributions = recurringContributions.toImmutableList(),
                preselection = preselection,
                selectedContributionId = selectedContributionId,
                isRecurringContributionSelected = true,
                isLoading = false,
            ),
            purchasedContribution = null,
            showContributionList = true,
        )

        contributionRobot(
            initialState = initialState,
            repository = repository,
        ) {
            // First cancellation
            clickPurchase()
            verifyPurchasingState()

            repository.purchasedContribution.emit(
                Outcome.failure(
                    FundingDomainContract.ContributionError.UserCancelled(
                        message = "Cancelled",
                    ),
                ),
            )
            verifyPurchasingStateDismissed(purchaseError = null)

            // Second cancellation
            clickPurchase()
            verifyPurchasingState()

            repository.purchasedContribution.emit(
                Outcome.failure(
                    FundingDomainContract.ContributionError.UserCancelled(
                        message = "Cancelled",
                    ),
                ),
            )
            verifyPurchasingStateDismissed(purchaseError = null)
        }
    }
}

private suspend fun MviContext.contributionRobot(
    initialState: State = State(),
    repository: FakeContributionRepository = FakeContributionRepository(),
    interaction: suspend ContributionRobot.() -> Unit,
) = ContributionRobot(this, initialState, repository).apply {
    initialize()
    interaction()
}

private class ContributionRobot(
    private val mviContext: MviContext,
    private val initialState: State = State(),
    private val repository: FakeContributionRepository = FakeContributionRepository(),
) {
    private val viewModel: ContributionContract.ViewModel by lazy {
        ContributionViewModel(
            getAvailableContributions = {
                flowOf(
                    Outcome.success(
                        AvailableContributions(
                            oneTimeContributions = FakeData.oneTimeContributions,
                            recurringContributions = FakeData.recurringContributions,
                            preselection = FakeData.preselection,
                        ),
                    ),
                )
            },
            repository = repository,
            initialState = initialState,
            logger = TestLogger(),
        )
    }
    private lateinit var turbines: MviTurbines<State, Effect>

    suspend fun initialize() {
        turbines = mviContext.turbinesWithInitialStateCheck(viewModel, initialState)
    }

    fun selectOneTimeContribution() {
        viewModel.event(Event.OnContributionTypeSelected(ContributionType.OneTime))
    }

    suspend fun verifyOneTimeContributionSelected() {
        assertThat(turbines.awaitStateItem()).isEqualTo(

            initialState.copy(
                listState = initialState.listState.copy(
                    isRecurringContributionSelected = false,
                    selectedContributionId = FakeData.preselection.oneTimeId,
                ),
            ),
        )
    }

    fun selectRecurringContribution() {
        viewModel.event(Event.OnContributionTypeSelected(ContributionType.Recurring))
    }

    suspend fun verifyRecurringContributionSelected() {
        assertThat(turbines.awaitStateItem()).isEqualTo(
            initialState.copy(
                listState = initialState.listState.copy(
                    isRecurringContributionSelected = true,
                    selectedContributionId = FakeData.preselection.recurringId,
                ),
            ),
        )
    }

    fun selectContributionItem(item: Contribution) {
        viewModel.event(Event.OnContributionItemClicked(item.id))
    }

    suspend fun verifyContributionItemSelected(item: Contribution) {
        assertThat(turbines.awaitStateItem()).isEqualTo(
            initialState.copy(
                listState = initialState.listState.copy(
                    selectedContributionId = item.id,
                ),
            ),
        )
    }

    suspend fun verifyListHidden(purchasedContribution: PurchasedContribution) {
        assertThat(turbines.awaitStateItem()).isEqualTo(
            initialState.copy(
                isPurchasing = false,
                listState = initialState.listState.copy(
                    isLoading = false,
                ),
                purchasedContribution = purchasedContribution,
                showContributionList = false,
                purchaseError = null,
            ),
        )
    }

    fun clickPurchase() {
        viewModel.event(Event.OnPurchaseClicked)
    }

    fun clickCancelPurchase() {
        viewModel.event(Event.OnCancelPurchaseClicked)
    }

    suspend fun verifyPurchasingState() {
        assertThat(turbines.awaitStateItem()).isEqualTo(
            initialState.copy(
                isPurchasing = true,
            ),
        )
    }

    suspend fun verifyPurchasingStateDismissed(purchaseError: FundingDomainContract.ContributionError?) {
        var nextState = turbines.awaitStateItem()
        while (nextState.isPurchasing && nextState.purchaseError == null) {
            nextState = turbines.awaitStateItem()
        }
        assertThat(nextState).isEqualTo(
            initialState.copy(
                isPurchasing = false,
                listState = initialState.listState.copy(
                    isLoading = false,
                ),
                purchasedContribution = initialState.purchasedContribution,
                showContributionList = initialState.showContributionList,
                purchaseError = purchaseError,
            ),
        )
    }

    suspend fun verifyPurchasedContribution(purchasedContribution: PurchasedContribution) {
        assertThat(turbines.awaitStateItem()).isEqualTo(
            initialState.copy(
                isPurchasing = false,
                purchasedContribution = purchasedContribution,
                showContributionList = false,
            ),
        )
    }
}
