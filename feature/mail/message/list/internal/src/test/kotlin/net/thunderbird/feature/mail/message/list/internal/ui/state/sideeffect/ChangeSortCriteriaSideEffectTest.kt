package net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect

import androidx.compose.ui.graphics.Color
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import dev.mokkery.mock
import kotlin.test.Test
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.common.state.sideeffect.StateSideEffectHandler
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.message.list.domain.UpdateSortCriteriaOutcome
import net.thunderbird.feature.mail.message.list.domain.model.SortCriteria
import net.thunderbird.feature.mail.message.list.domain.model.SortType
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.Account
import net.thunderbird.feature.mail.message.list.ui.state.Folder
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState

class ChangeSortCriteriaSideEffectTest : BaseSideEffectHandlerTest() {
    @Test
    fun `handle() should return Consumed if event is ChangeSortCriteria`() = runTest {
        // Arrange
        val testSubject = ChangeSortCriteriaSideEffect(
            dispatch = {},
            logger = TestLogger(),
            updateSortCriteria = { _, _ -> Outcome.success(UpdateSortCriteriaOutcome.Success) },
        )
        val newState = MessageListState.WarmingUp(
            metadata = createReadyMetadata().copy(
                folder = null,
                sortCriteriaPerAccount = persistentMapOf(null to SortCriteria(primary = SortType.DateDesc)),
            ),
            preferences = createMessageListPreferences(),
        )

        // Act
        val actual = testSubject.handle(
            event = MessageListEvent.ChangeSortCriteria(
                accountId = null,
                sortCriteria = SortCriteria(primary = SortType.DateDesc),
            ),
            oldState = MessageListState.WarmingUp(),
            newState = newState,
        )

        // Assert
        assertThat(actual).isEqualTo(StateSideEffectHandler.ConsumeResult.Consumed)
    }

    @Test
    fun `handle() should return Ignored if event is not ChangeSortCriteria`() = runTest {
        // Arrange
        val testSubject = ChangeSortCriteriaSideEffect(
            dispatch = {},
            logger = TestLogger(),
            updateSortCriteria = mock(),
        )

        // Act
        val actual = testSubject.handle(
            event = MessageListEvent.LoadMore,
            oldState = MessageListState.WarmingUp(),
            newState = createReadyWarmingUpState(),
        )

        // Assert
        assertThat(actual).isEqualTo(StateSideEffectHandler.ConsumeResult.Ignored)
    }

    @Test
    fun `handle() should throw IllegalArgumentException if newSortCriteria is null`() = runTest {
        // Arrange
        val accountIdMissingSortCriteria = AccountIdFactory.create()
        val accountMissingSortCriteria = Account(id = accountIdMissingSortCriteria, color = Color.Unspecified)
        val sortCriteriaPerAccount = persistentMapOf<AccountId?, SortCriteria>(
            AccountIdFactory.create() to SortCriteria(primary = SortType.DateDesc),
            AccountIdFactory.create() to SortCriteria(primary = SortType.ArrivalAsc),
        )

        val testSubject = ChangeSortCriteriaSideEffect(
            dispatch = {},
            logger = TestLogger(),
            updateSortCriteria = { _, _ -> error("should not be called") },
        )

        val oldState = MessageListState.WarmingUp()
        val newState = oldState.withMetadata {
            copy(
                folder = Folder(
                    id = "mock",
                    account = accountMissingSortCriteria,
                    name = "mock",
                    type = FolderType.INBOX,
                ),
                sortCriteriaPerAccount = sortCriteriaPerAccount,
            )
        }

        val event = MessageListEvent.ChangeSortCriteria(
            accountId = accountIdMissingSortCriteria,
            sortCriteria = SortCriteria(primary = SortType.DateDesc),
        )

        // Act
        val actual = assertFailure {
            testSubject.handle(event, oldState, newState)
        }

        // Assert
        actual
            .isInstanceOf(IllegalArgumentException::class)
            .hasMessage("The new sort criteria for account $accountIdMissingSortCriteria must not be null")
    }
}
