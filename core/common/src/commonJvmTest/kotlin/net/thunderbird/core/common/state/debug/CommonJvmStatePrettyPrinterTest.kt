package net.thunderbird.core.common.state.debug

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import kotlin.test.AfterTest
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import net.thunderbird.core.common.state.StateMachine
import net.thunderbird.core.common.state.debug.StatePrettyPrinterVocabulary.STATE_HISTORY_DUMP_BEGIN
import net.thunderbird.core.common.state.debug.StatePrettyPrinterVocabulary.STATE_HISTORY_DUMP_END
import net.thunderbird.core.common.state.debug.StatePrettyPrinterVocabulary.STATE_HISTORY_STATE_SEPARATOR
import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.testing.TestLogger
import org.junit.Test

@OptIn(ExperimentalTime::class)
class CommonJvmStatePrettyPrinterTest {
    private val logger = TestLogger()

    @AfterTest
    fun tearDown() {
        logger.dump()
        logger.events.clear()
    }

    // region prettyPrint() - empty stack

    @Test
    fun `prettyPrint - should log empty message when history stack is empty`() {
        // Arrange
        val testSubject = createTestSubject(logger = logger)
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert
        assertThat(logger.events).hasSize(1)
        assertThat(logger.events[0].level).isEqualTo(LogLevel.VERBOSE)
        assertThat(logger.events[0].message).contains(StatePrettyPrinterVocabulary.STATE_HISTORY_EMPTY)
    }

    // endregion

    // region prettyPrint() - latest transition logging

    @Test
    fun `prettyPrint - should log only latest transition for single record without full dump`() {
        // Arrange
        val clock = TestClock(Instant.fromEpochMilliseconds(0))
        val testSubject = createTestSubject(logger = logger, clock = clock)
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.Idle,
                newState = TestState.Loading(progress = 0.5f),
                event = TestEvent.Start,
                timestamp = Instant.fromEpochMilliseconds(100),
            ),
        )

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert - single record never triggers full dump
        assertThat(logger.events).hasSize(1)
        assertThat(logger.events[0].level).isEqualTo(LogLevel.VERBOSE)
        assertThat(logger.events[0].message).contains("Latest transition")
        assertThat(logger.events[0].message).contains("Idle")
        assertThat(logger.events[0].message).contains("Loading")
    }

    @Test
    fun `prettyPrint - should include event class name in latest transition log`() {
        // Arrange
        val testSubject = createTestSubject(logger = logger)
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.Idle,
                newState = TestState.Loading(progress = 0f),
                event = TestEvent.Start,
            ),
        )

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert
        assertThat(logger.events).hasSize(1)
        assertThat(logger.events[0].message).contains(TestEvent.Start::class.java.simpleName)
    }

    @Test
    fun `prettyPrint - should include logTag in log tag`() {
        // Arrange
        val logTag = "MyStateMachine"
        val testSubject = createTestSubject(logger = logger, logTag = logTag)
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.Idle,
                newState = TestState.Loading(progress = 0f),
                event = TestEvent.Start,
            ),
        )

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert
        assertThat(logger.events).hasSize(1)
        assertThat(logger.events[0].tag).isEqualTo($$"$$logTag$StatePrettyPrinter")
    }

    @Test
    fun `prettyPrint - should handle null logTag in log tag`() {
        // Arrange
        val testSubject = createTestSubject(logger = logger, logTag = null)
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.Idle,
                newState = TestState.Loading(progress = 0f),
                event = TestEvent.Start,
            ),
        )

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert
        assertThat(logger.events).hasSize(1)
        assertThat(logger.events[0].tag).isEqualTo("StatePrettyPrinter")
    }

    // endregion

    // region prettyPrint() - elapsed time

    @Test
    fun `prettyPrint - should include elapsed time between transitions`() {
        // Arrange
        val testSubject = createTestSubject(logger = logger)
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.Idle,
                newState = TestState.Loading(progress = 0f),
                event = TestEvent.Start,
                timestamp = Instant.fromEpochMilliseconds(100),
            ),
        )
        historyStack.addLast(
            createRecord(
                previousState = TestState.Loading(progress = 0f),
                newState = TestState.Loading(progress = 1f),
                event = TestEvent.UpdateProgress(1f),
                timestamp = Instant.fromEpochMilliseconds(350),
            ),
        )

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert
        assertThat(logger.events).hasSize(2)
        assertThat(logger.events[0].message).contains("+250ms")
    }

    @Test
    fun `prettyPrint - should not include elapsed time for first record`() {
        // Arrange
        val testSubject = createTestSubject(logger = logger)
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.Idle,
                newState = TestState.Loading(progress = 0f),
                event = TestEvent.Start,
                timestamp = Instant.fromEpochMilliseconds(100),
            ),
        )

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert
        assertThat(logger.events).hasSize(1)
        val latestTransitionLog = logger.events[0].message
        val hasElapsed = latestTransitionLog.lines().any { it.contains(Regex("\\+\\d+ms")) }
        assertThat(hasElapsed).isFalse()
    }

    // endregion

    // region prettyPrint() - transition markers

    @Test
    fun `prettyPrint - should use arrow marker for state class change`() {
        // Arrange
        val testSubject = createTestSubject(logger = logger)
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.Idle,
                newState = TestState.Loading(progress = 0f),
                event = TestEvent.Start,
            ),
        )

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert
        assertThat(logger.events).hasSize(1)
        assertThat(logger.events[0].message).contains(
            "Idle ${StatePrettyPrinterVocabulary.STATE_CHANGE_MARKER} Loading",
        )
    }

    @Test
    fun `prettyPrint - should use loop marker for same state class`() {
        // Arrange
        val testSubject = createTestSubject(logger = logger)
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.Loading(progress = 0f),
                newState = TestState.Loading(progress = 0.5f),
                event = TestEvent.UpdateProgress(0.5f),
            ),
        )

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert
        assertThat(logger.events).hasSize(1)
        assertThat(logger.events[0].message).contains(
            "Loading ${StatePrettyPrinterVocabulary.STATE_NO_CHANGE_MARKER} Loading",
        )
    }

    // endregion

    // region prettyPrint() - full dump and cooldown

    @Test
    fun `prettyPrint - should log full dump when multiple records and cooldown elapsed`() {
        // Arrange
        val clock = TestClock(Instant.fromEpochMilliseconds(0))
        val testSubject = createTestSubject(logger = logger, clock = clock, fullDumpCooldown = 2.seconds)
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.Idle,
                newState = TestState.Loading(progress = 0f),
                event = TestEvent.Start,
            ),
        )
        historyStack.addLast(
            createRecord(
                previousState = TestState.Loading(progress = 0f),
                newState = TestState.Loading(progress = 0.5f),
                event = TestEvent.UpdateProgress(0.5f),
            ),
        )

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert - 2 logs: latest transition + full dump (cooldown elapsed from DISTANT_PAST)
        assertThat(logger.events).hasSize(2)
        assertThat(logger.events[1].message).contains(StatePrettyPrinterVocabulary.STATE_HISTORY_DUMP_TITLE)
        assertThat(logger.events[1].message).contains(STATE_HISTORY_DUMP_BEGIN)
        assertThat(logger.events[1].message).contains(STATE_HISTORY_DUMP_END)
    }

    @Test
    fun `prettyPrint - should skip full dump when multiple records and within cooldown`() {
        // Arrange
        val clock = TestClock(Instant.fromEpochMilliseconds(0))
        val testSubject = createTestSubject(logger = logger, clock = clock, fullDumpCooldown = 2.seconds)
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.Idle,
                newState = TestState.Loading(progress = 0f),
                event = TestEvent.Start,
            ),
        )
        historyStack.addLast(
            createRecord(
                previousState = TestState.Loading(progress = 0f),
                newState = TestState.Loading(progress = 0.5f),
                event = TestEvent.UpdateProgress(0.5f),
            ),
        )

        // The first call with multiple records triggers a full dump (cooldown elapsed from DISTANT_PAST)
        testSubject.prettyPrint(historyStack)
        val eventsAfterFirstCall = logger.events.size

        // Add a third record within cooldown
        historyStack.addLast(
            createRecord(
                previousState = TestState.Loading(progress = 0.5f),
                newState = TestState.Loading(progress = 1f),
                event = TestEvent.UpdateProgress(1f),
            ),
        )
        clock.plusAssign(1.seconds)

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert - only the latest transition added, no additional full dump
        assertThat(logger.events).hasSize(eventsAfterFirstCall + 1)
        assertThat(logger.events.last().message).contains("Latest transition")
    }

    @Test
    fun `prettyPrint - should log full dump again after cooldown elapses with multiple records`() {
        // Arrange
        val clock = TestClock(Instant.fromEpochMilliseconds(0))
        val testSubject = createTestSubject(logger = logger, clock = clock, fullDumpCooldown = 2.seconds)
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.Idle,
                newState = TestState.Loading(progress = 0f),
                event = TestEvent.Start,
            ),
        )
        historyStack.addLast(
            createRecord(
                previousState = TestState.Loading(progress = 0f),
                newState = TestState.Loading(progress = 0.5f),
                event = TestEvent.UpdateProgress(0.5f),
            ),
        )

        // The first call with multiple records triggers a full dump (cooldown from DISTANT_PAST)
        testSubject.prettyPrint(historyStack)
        val eventsAfterFirstCall = logger.events.size

        // Add a third record and advance past cooldown
        historyStack.addLast(
            createRecord(
                previousState = TestState.Loading(progress = 0.5f),
                newState = TestState.Loading(progress = 1f),
                event = TestEvent.UpdateProgress(1f),
            ),
        )
        clock.plusAssign(3.seconds)

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert - latest transition + full dump added
        assertThat(logger.events).hasSize(eventsAfterFirstCall + 2)
        assertThat(logger.events.last().message).contains(StatePrettyPrinterVocabulary.STATE_HISTORY_DUMP_TITLE)
    }

    @Test
    fun `prettyPrint - full dump should contain all records in order`() {
        // Arrange
        val clock = TestClock(Instant.fromEpochMilliseconds(0))
        val testSubject = createTestSubject(logger = logger, clock = clock)
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.Idle,
                newState = TestState.Loading(progress = 0f),
                event = TestEvent.Start,
                timestamp = Instant.fromEpochMilliseconds(100),
            ),
        )
        historyStack.addLast(
            createRecord(
                previousState = TestState.Loading(progress = 0f),
                newState = TestState.Loading(progress = 1f),
                event = TestEvent.UpdateProgress(1f),
                timestamp = Instant.fromEpochMilliseconds(200),
            ),
        )
        historyStack.addLast(
            createRecord(
                previousState = TestState.Loading(progress = 1f),
                newState = TestState.Done(result = "ok"),
                event = TestEvent.Finish,
                timestamp = Instant.fromEpochMilliseconds(300),
            ),
        )

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert - full dump should contain all states in chronological order
        val fullDump = logger.events[1].message
        val fullDumpLines = fullDump.split(STATE_HISTORY_STATE_SEPARATOR)
        val stateTransitions = fullDumpLines.drop(1).dropLast(1)
        // Beginning Section + State transitions (3) + End Section = 5
        assertThat(fullDumpLines).hasSize(5)
        assertThat(fullDumpLines.first()).contains(STATE_HISTORY_DUMP_BEGIN)
        assertThat(fullDumpLines.last()).contains(STATE_HISTORY_DUMP_END)
        assertThat(stateTransitions).all {
            hasSize(3)
            transform { transitions -> transitions.map { it.trim().lines().first() } }
                .containsExactly(
                    "Idle ${StatePrettyPrinterVocabulary.STATE_CHANGE_MARKER} Loading  event=Start",
                    "Loading ${StatePrettyPrinterVocabulary.STATE_NO_CHANGE_MARKER} Loading  event=UpdateProgress  +100ms",
                    "Loading ${StatePrettyPrinterVocabulary.STATE_CHANGE_MARKER} Done  event=Finish  +100ms",
                )
        }
    }

    // endregion

    // region prettyPrint() - scalar property diffs

    @Test
    fun `prettyPrint - should show property diff for changed scalar properties`() {
        // Arrange
        val testSubject = createTestSubject(logger = logger)
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.Loading(progress = 0f),
                newState = TestState.Loading(progress = 0.75f),
                event = TestEvent.UpdateProgress(0.75f),
            ),
        )

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert
        assertThat(logger.events).hasSize(1)
        val output = logger.events[0].message
        assertThat(output).contains("progress: 0.75")
        assertThat(output).contains("progress: 0.0 -> 0.75")
    }

    @Test
    fun `prettyPrint - should show string property changes`() {
        // Arrange
        val testSubject = createTestSubject(logger = logger)
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.Done(result = "initial"),
                newState = TestState.Done(result = "updated"),
                event = TestEvent.Finish,
            ),
        )

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert
        assertThat(logger.events).hasSize(1)
        val output = logger.events[0].message
        assertThat(output).contains("result: initial -> updated")
    }

    // endregion

    // region prettyPrint() - collection diffs

    @Test
    fun `prettyPrint - should show individual item diff for small collection size change`() {
        // Arrange
        val testSubject = createTestSubject(logger = logger)
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.WithCollection(items = listOf("a", "b")),
                newState = TestState.WithCollection(items = listOf("a", "b", "c", "d")),
                event = TestEvent.UpdateItems(listOf("a", "b", "c", "d")),
            ),
        )

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert - collections <= 5 items show individual items
        assertThat(logger.events).hasSize(1)
        val output = logger.events[0].message
        assertThat(output).contains("[0]: a (No changes)")
        assertThat(output).contains("[1]: b (No changes)")
        assertThat(output).contains("[2]: null -> c")
        assertThat(output).contains("[3]: null -> d")
    }

    @Test
    fun `prettyPrint - should show summary diff for large collection size change`() {
        // Arrange
        val testSubject = createTestSubject(logger = logger)
        val largeOldList = (1..6).map { "item$it" }
        val largeNewList = (1..8).map { "item$it" }
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.WithCollection(items = largeOldList),
                newState = TestState.WithCollection(items = largeNewList),
                event = TestEvent.UpdateItems(largeNewList),
            ),
        )

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert - collections > 5 items show summary count
        assertThat(logger.events).hasSize(1)
        val output = logger.events[0].message
        assertThat(output).contains("items: [6 items] -> [8 items]")
    }

    @Test
    fun `prettyPrint - should not show diff for unchanged list`() {
        // Arrange
        val testSubject = createTestSubject(logger = logger)
        val items = listOf("a", "b")
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.WithCollection(items = items),
                newState = TestState.WithCollection(items = items),
                event = TestEvent.UpdateItems(items),
            ),
        )

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert - same state class with identical items should not show items in the diff
        assertThat(logger.events).hasSize(1)
        val updatedData = logger.events[0].message.substringAfter("updated data:")
        assertThat(updatedData.trim()).isEqualTo("")
    }

    // endregion

    // region prettyPrint() - map diffs

    @Test
    fun `prettyPrint - should show map entry changes`() {
        // Arrange
        val testSubject = createTestSubject(logger = logger)
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.WithMap(config = mapOf("theme" to "light", "lang" to "en")),
                newState = TestState.WithMap(config = mapOf("theme" to "dark", "lang" to "en")),
                event = TestEvent.UpdateConfig(mapOf("theme" to "dark", "lang" to "en")),
            ),
        )

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert
        assertThat(logger.events).hasSize(1)
        val output = logger.events[0].message
        assertThat(output).contains("[theme]: light -> dark")
        assertThat(output).contains("[lang]: en (No changes)")
    }

    @Test
    fun `prettyPrint - should not show diff for unchanged small map`() {
        // Arrange
        val testSubject = createTestSubject(logger = logger)
        val config = mapOf("key" to "value")
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.WithMap(config = config),
                newState = TestState.WithMap(config = config),
                event = TestEvent.UpdateConfig(config),
            ),
        )

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert - same state class with identical small map should not show config in the diff
        assertThat(logger.events).hasSize(1)
        val updatedData = logger.events[0].message.substringAfter("updated data:")
        assertThat(updatedData.trim()).isEqualTo("")
    }

    @Test
    fun `prettyPrint - should not show diff for unchanged large map`() {
        // Arrange
        val testSubject = createTestSubject(logger = logger)
        val config = (1..6).associate { "key$it" to "value$it" }
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.WithMap(config = config),
                newState = TestState.WithMap(config = config),
                event = TestEvent.UpdateConfig(config),
            ),
        )

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert - same class, no changes: nothing shown (same as collection behavior)
        assertThat(logger.events).hasSize(1)
        val updatedData = logger.events[0].message.substringAfter("updated data:")
        assertThat(updatedData.trim()).isEqualTo("")
    }

    @Test
    fun `prettyPrint - should show summary diff for large map entry changes`() {
        // Arrange
        val testSubject = createTestSubject(logger = logger)
        val oldConfig = (1..6).associate { "key$it" to "value$it" }
        val newConfig = oldConfig.toMutableMap().apply { this["key3"] = "changed" }
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.WithMap(config = oldConfig),
                newState = TestState.WithMap(config = newConfig),
                event = TestEvent.UpdateConfig(newConfig),
            ),
        )

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert - large map (>5) with changes shows summary count
        assertThat(logger.events).hasSize(1)
        val output = logger.events[0].message
        assertThat(output).contains("config: {6 entries} -> {6 entries}")
    }

    @Test
    fun `prettyPrint - should show individual entry diff for small map changes`() {
        // Arrange
        val testSubject = createTestSubject(logger = logger)
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.WithMap(config = mapOf("theme" to "light", "lang" to "en")),
                newState = TestState.WithMap(config = mapOf("theme" to "dark", "lang" to "en", "size" to "large")),
                event = TestEvent.UpdateConfig(mapOf("theme" to "dark", "lang" to "en", "size" to "large")),
            ),
        )

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert - small map shows individual entry diffs
        assertThat(logger.events).hasSize(1)
        val output = logger.events[0].message
        assertThat(output).contains("[theme]: light -> dark")
        assertThat(output).contains("[lang]: en (No changes)")
        assertThat(output).contains("[size]: null -> large")
    }

    // endregion

    // region prettyPrint() - nested object diffs

    @Test
    fun `prettyPrint - should show nested object property changes`() {
        // Arrange
        val testSubject = createTestSubject(logger = logger)
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.WithNested(nested = NestedData(name = "old", count = 1)),
                newState = TestState.WithNested(nested = NestedData(name = "new", count = 1)),
                event = TestEvent.UpdateNested(NestedData(name = "new", count = 1)),
            ),
        )

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert
        assertThat(logger.events).hasSize(1)
        val output = logger.events[0].message
        assertThat(output).contains("nested")
        assertThat(output).contains("name")
        assertThat(output).contains("old")
        assertThat(output).contains("new")
    }

    @Test
    fun `prettyPrint - should not show diff for unchanged nested object`() {
        // Arrange
        val testSubject = createTestSubject(logger = logger)
        val nested = NestedData(name = "same", count = 5)
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.WithNested(nested = nested),
                newState = TestState.WithNested(nested = nested),
                event = TestEvent.UpdateNested(nested),
            ),
        )

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert - same state class with identical nested object should not show nested in the diff
        assertThat(logger.events).hasSize(1)
        val updatedData = logger.events[0].message.substringAfter("updated data:")
        assertThat(updatedData.trim()).isEqualTo("")
    }

    // endregion

    // region prettyPrint() - state class change with property display

    @Test
    fun `prettyPrint - should show all new state properties when state class changes`() {
        // Arrange
        val testSubject = createTestSubject(logger = logger)
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.Idle,
                newState = TestState.Loading(progress = 0.5f),
                event = TestEvent.Start,
            ),
        )

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert - state class changed, so new state's properties should be shown
        assertThat(logger.events).hasSize(1)
        val output = logger.events[0].message
        assertThat(output).contains("progress")
        assertThat(output).contains("0.5")
    }

    @Test
    fun `prettyPrint - should show unchanged nested object with no changes marker when state class changes`() {
        // Arrange
        val testSubject = createTestSubject(logger = logger)
        val nested = NestedData(name = "same", count = 5)
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.Idle,
                newState = TestState.WithNested(nested = nested),
                event = TestEvent.UpdateNested(nested),
            ),
        )

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert - state class changed so properties of new state appear even if "unchanged"
        assertThat(logger.events).hasSize(1)
        val output = logger.events[0].message
        assertThat(output).contains("nested")
    }

    @Test
    fun `prettyPrint - should show unchanged collection with summary when state class changes`() {
        // Arrange
        val testSubject = createTestSubject(logger = logger)
        val items = listOf("a", "b")
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.Idle,
                newState = TestState.WithCollection(items = items),
                event = TestEvent.UpdateItems(items),
            ),
        )

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert - state class changed so collection should appear with summary
        assertThat(logger.events).hasSize(1)
        val output = logger.events[0].message
        assertThat(output).contains("items")
    }

    @Test
    fun `prettyPrint - should show new map property when state class changes`() {
        // Arrange
        val testSubject = createTestSubject(logger = logger)
        val config = mapOf("key" to "value")
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.Idle,
                newState = TestState.WithMap(config = config),
                event = TestEvent.UpdateConfig(config),
            ),
        )

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert - state class changed, new property appears as null -> value
        assertThat(logger.events).hasSize(1)
        val output = logger.events[0].message
        assertThat(output).contains("config: null ->")
    }

    // endregion

    // region prettyPrint() - event payload

    @Test
    fun `prettyPrint - should show event payload properties for data class events`() {
        // Arrange
        val testSubject = createTestSubject(logger = logger)
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.Loading(progress = 0f),
                newState = TestState.Loading(progress = 0.5f),
                event = TestEvent.UpdateProgress(0.5f),
            ),
        )

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert
        assertThat(logger.events).hasSize(1)
        val output = logger.events[0].message
        assertThat(output).contains("UpdateProgress payload:")
        assertThat(output).contains("progress: 0.0 -> 0.5")
    }

    @Test
    fun `prettyPrint - should not show payload section for object events`() {
        // Arrange
        val testSubject = createTestSubject(logger = logger)
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.Idle,
                newState = TestState.Loading(progress = 0f),
                event = TestEvent.Start,
            ),
        )

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert - Start is a data object, no payload
        assertThat(logger.events).hasSize(1)
        val output = logger.events[0].message
        assertThat(output.contains("Start payload:")).isFalse()
    }

    // endregion

    // region prettyPrint() - multiple sequential transitions

    @Test
    fun `prettyPrint - should log only latest transition for first single record`() {
        // Arrange
        val clock = TestClock(Instant.fromEpochMilliseconds(0))
        val testSubject = createTestSubject(logger = logger, clock = clock, fullDumpCooldown = 10.seconds)
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.Idle,
                newState = TestState.Loading(progress = 0f),
                event = TestEvent.Start,
                timestamp = Instant.fromEpochMilliseconds(100),
            ),
        )

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert - single record produces only latest transition log
        assertThat(logger.events).hasSize(1)
        assertThat(logger.events[0].message).contains("Latest transition")
    }

    @Test
    fun `prettyPrint - should trigger full dump on second record when cooldown elapsed`() {
        // Arrange
        val clock = TestClock(Instant.fromEpochMilliseconds(0))
        val testSubject = createTestSubject(logger = logger, clock = clock, fullDumpCooldown = 10.seconds)
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.Idle,
                newState = TestState.Loading(progress = 0f),
                event = TestEvent.Start,
                timestamp = Instant.fromEpochMilliseconds(100),
            ),
        )
        testSubject.prettyPrint(historyStack)

        // Add second record (cooldown elapsed from DISTANT_PAST)
        clock.plusAssign(1.seconds)
        historyStack.addLast(
            createRecord(
                previousState = TestState.Loading(progress = 0f),
                newState = TestState.Loading(progress = 0.5f),
                event = TestEvent.UpdateProgress(0.5f),
                timestamp = Instant.fromEpochMilliseconds(1100),
            ),
        )

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert - first call: 1 event, second call: latest transition + full dump = 2
        assertThat(logger.events).hasSize(3)
        assertThat(logger.events[1].message).contains("Latest transition")
        assertThat(logger.events[2].message).contains(StatePrettyPrinterVocabulary.STATE_HISTORY_DUMP_TITLE)
    }

    @Test
    fun `prettyPrint - should skip full dump on third record within cooldown`() {
        // Arrange
        val clock = TestClock(Instant.fromEpochMilliseconds(0))
        val testSubject = createTestSubject(logger = logger, clock = clock, fullDumpCooldown = 10.seconds)
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.Idle,
                newState = TestState.Loading(progress = 0f),
                event = TestEvent.Start,
                timestamp = Instant.fromEpochMilliseconds(100),
            ),
        )
        testSubject.prettyPrint(historyStack)

        clock.plusAssign(1.seconds)
        historyStack.addLast(
            createRecord(
                previousState = TestState.Loading(progress = 0f),
                newState = TestState.Loading(progress = 0.5f),
                event = TestEvent.UpdateProgress(0.5f),
                timestamp = Instant.fromEpochMilliseconds(1100),
            ),
        )
        testSubject.prettyPrint(historyStack)
        val eventsAfterSecondCall = logger.events.size

        // Add third record within cooldown
        clock.plusAssign(1.seconds)
        historyStack.addLast(
            createRecord(
                previousState = TestState.Loading(progress = 0.5f),
                newState = TestState.Done(result = "finished"),
                event = TestEvent.Finish,
                timestamp = Instant.fromEpochMilliseconds(2100),
            ),
        )

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert - only latest transition added, no full dump
        assertThat(logger.events).hasSize(eventsAfterSecondCall + 1)
        val lastTransition = logger.events.last().message
        assertThat(lastTransition).contains("Loading ${StatePrettyPrinterVocabulary.STATE_CHANGE_MARKER} Done")
        assertThat(lastTransition).contains("+1000ms")
    }

    // endregion

    // region prettyPrint() - custom value formatter

    @Test
    fun `prettyPrint - should use custom value formatter for property values`() {
        // Arrange
        val testSubject = createTestSubject(
            logger = logger,
            valueFormatter = { value, _ ->
                if (value is Float) "progress=${(value * 100).toInt()}%" else value.toString()
            },
        )
        val historyStack = ArrayDeque<StateMachine.StateTransitionRecord<TestState, TestEvent>>()
        historyStack.addLast(
            createRecord(
                previousState = TestState.Loading(progress = 0f),
                newState = TestState.Loading(progress = 0.75f),
                event = TestEvent.UpdateProgress(0.75f),
            ),
        )

        // Act
        testSubject.prettyPrint(historyStack)

        // Assert
        assertThat(logger.events).hasSize(1)
        val output = logger.events[0].message
        assertThat(output).contains("progress=0%")
        assertThat(output).contains("progress=75%")
    }

    // endregion

    // region helpers

    private fun createTestSubject(
        logger: TestLogger = TestLogger(),
        logTag: String? = "Test",
        clock: TestClock = TestClock(Instant.fromEpochMilliseconds(0)),
        valueFormatter: (Any, formatter: (Any) -> String) -> String = { value, _ -> value.toString() },
        fullDumpCooldown: kotlin.time.Duration = 2.seconds,
    ) = CommonJvmStatePrettyPrinter<TestState, TestEvent>(
        logger = logger,
        logTag = logTag,
        clock = clock,
        customValueFormatter = valueFormatter,
        fullDumpCooldown = fullDumpCooldown,
    )

    private fun createRecord(
        previousState: TestState,
        newState: TestState,
        event: TestEvent,
        timestamp: Instant = Instant.fromEpochMilliseconds(0),
    ) = StateMachine.StateTransitionRecord(
        // Transition's guard and createNewState are not used by prettyPrint;
        // dummy values are provided because StateTransitionRecord requires them.
        transition = StateMachine.Transition(
            guard = { _, _ -> true },
            createNewState = { _, _ -> newState },
        ),
        event = event,
        previousState = previousState,
        newState = newState,
        timestamp = timestamp,
    )

    // endregion
}

private sealed interface TestState {
    data object Idle : TestState
    data class Loading(val progress: Float) : TestState
    data class Done(val result: String) : TestState
    data class WithCollection(val items: List<String>) : TestState
    data class WithMap(val config: Map<String, String>) : TestState
    data class WithNested(val nested: NestedData) : TestState
}

private data class NestedData(
    val name: String,
    val count: Int,
)

private sealed interface TestEvent {
    data object Start : TestEvent
    data class UpdateProgress(val progress: Float) : TestEvent
    data object Finish : TestEvent
    data class UpdateItems(val items: List<String>) : TestEvent
    data class UpdateConfig(val config: Map<String, String>) : TestEvent
    data class UpdateNested(val nested: NestedData) : TestEvent
}

@ExperimentalTime
private class TestClock(private var now: Instant) : Clock {
    override fun now(): Instant = now

    operator fun plusAssign(duration: kotlin.time.Duration) {
        now += duration
    }
}
