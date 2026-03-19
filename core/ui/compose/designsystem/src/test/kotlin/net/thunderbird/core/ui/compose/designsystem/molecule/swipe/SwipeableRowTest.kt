package net.thunderbird.core.ui.compose.designsystem.molecule.swipe

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.R
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.setContentWithTheme
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.core.ui.compose.designsystem.molecule.swipe.SwipeDirectionAccessibilityAction.EndToStartAccessibilityAction
import net.thunderbird.core.ui.compose.designsystem.molecule.swipe.SwipeDirectionAccessibilityAction.StartToEndAccessibilityAction
import net.thunderbird.core.ui.compose.theme2.MainTheme
import org.junit.Test

@Suppress("MaxLineLength")
class SwipeableRowTest : ComposeTest() {

    // region [ onSwipeEnd verifications ]
    @Test
    fun `should call onSwipeEnd when swipe gesture ends`() = runComposeTest {
        // Arrange
        val swipeEndDirections = mutableListOf<SwipeDirection>()

        setContentWithTheme {
            TestSwipeableRow(
                onSwipeEnd = { swipeEndDirections.add(it) },
            )
        }

        // Act
        onNodeWithTag(TEST_TAG).performTouchInput { swipeLeft() }
        waitForIdle()

        // Assert
        assertThat(swipeEndDirections).isEqualTo(listOf(SwipeDirection.EndToStart))
    }

    @Test
    fun `when start-to-end behaviour is Disabled, should not call onSwipeEnd when swiping from start to end`() =
        runComposeTest {
            // Arrange
            val swipeEndDirections = mutableListOf<SwipeDirection>()

            setContentWithTheme {
                TestSwipeableRow(
                    startToEndBehaviour = SwipeBehaviour.Disabled,
                    onSwipeEnd = { swipeEndDirections.add(it) },
                )
            }

            // Act
            onNodeWithTag(TEST_TAG).performTouchInput { swipeRight() }
            waitForIdle()

            // Assert
            assertThat(swipeEndDirections).isEmpty()
        }

    @Test
    fun `when end-to-start behaviour is Disabled, should not call onSwipeEnd when swiping from end to start`() =
        runComposeTest {
            // Arrange
            val swipeEndDirections = mutableListOf<SwipeDirection>()

            setContentWithTheme {
                TestSwipeableRow(
                    endToStartBehaviour = SwipeBehaviour.Disabled,
                    onSwipeEnd = { swipeEndDirections.add(it) },
                )
            }

            // Act
            onNodeWithTag(TEST_TAG).performTouchInput { swipeLeft() }
            waitForIdle()

            // Assert
            assertThat(swipeEndDirections).isEmpty()
        }

    @Test
    fun `when gesturesEnabled is false, should not call onSwipeEnd when swiping`() = runComposeTest {
        // Arrange
        val swipeEndDirections = mutableListOf<SwipeDirection>()

        setContentWithTheme {
            TestSwipeableRow(
                gesturesEnabled = false,
                onSwipeEnd = { swipeEndDirections.add(it) },
            )
        }

        // Act
        onNodeWithTag(TEST_TAG).performTouchInput { swipeLeft() }
        waitForIdle()

        // Assert
        assertThat(swipeEndDirections).isEmpty()
    }
    // endregion [ onSwipeEnd verifications ]

    // region [ onSwipeChange verifications ]
    @Test
    fun `should call onSwipeChange when swipe gesture changes`() = runComposeTest {
        // Arrange
        val swipeChangeDirections = mutableListOf<SwipeDirection>()

        setContentWithTheme {
            TestSwipeableRow(
                onSwipeChange = { swipeChangeDirections.add(it) },
            )
        }

        // Act
        onNodeWithTag(TEST_TAG).performTouchInput { swipeRight() }
        waitForIdle()

        // Assert
        assertThat(swipeChangeDirections).contains(SwipeDirection.StartToEnd)
    }

    @Test
    fun `when gesturesEnabled is false, should not call onSwipeChange when swiping`() = runComposeTest {
        // Arrange
        val swipeChangeDirections = mutableListOf<SwipeDirection>()

        setContentWithTheme {
            TestSwipeableRow(
                gesturesEnabled = false,
                onSwipeChange = { swipeChangeDirections.add(it) },
            )
        }

        // Act
        onNodeWithTag(TEST_TAG).performTouchInput { swipeLeft() }
        waitForIdle()

        // Assert
        assertThat(swipeChangeDirections).isEmpty()
    }

    // endregion [ onSwipeChange verifications ]

    // region [ Accessibility verifications ]
    @Test
    fun `should have correct accessibility label`() = runComposeTest {
        // Arrange
        setContentWithTheme {
            TestSwipeableRow(
                accessibilityActions = persistentListOf(
                    StartToEndAccessibilityAction(
                        actionStringRes = R.string.designsystem_molecule_error_view_button_retry,
                    ),
                    EndToStartAccessibilityAction(
                        actionStringRes = R.string.designsystem_molecule_error_view_button_retry,
                    ),
                ),
            )
        }
        val expectedStartToEnd = getString(
            R.string.designsystem_molecule_swipeable_row_start_to_end_accessibility_description,
        ).format(getString(R.string.designsystem_molecule_error_view_button_retry))
        val expectedEndToStart = getString(
            R.string.designsystem_molecule_swipeable_row_end_to_start_accessibility_description,
        ).format(getString(R.string.designsystem_molecule_error_view_button_retry))

        // Act
        val customActions = onNodeWithTag(TEST_TAG)
            .fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]

        // Assert
        assertThat(customActions.map { it.label }).containsExactlyInAnyOrder(
            expectedStartToEnd,
            expectedEndToStart,
        )
    }

    @Test
    fun `when gesturesEnabled is false, should not have accessibility label`() = runComposeTest {
        // Arrange
        setContentWithTheme {
            TestSwipeableRow(
                gesturesEnabled = false,
                accessibilityActions = persistentListOf(
                    StartToEndAccessibilityAction(
                        actionStringRes = R.string.designsystem_molecule_error_view_button_retry,
                    ),
                    EndToStartAccessibilityAction(
                        actionStringRes = R.string.designsystem_molecule_error_view_button_retry,
                    ),
                ),
            )
        }

        // Act
        val customActions = onNodeWithTag(TEST_TAG)
            .fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]

        // Assert
        assertThat(customActions).isEmpty()
    }

    @Test
    fun `when start-to-end behaviour is Disabled, should not have accessibility label when swiping from start to end`() =
        runComposeTest {
            // Arrange
            setContentWithTheme {
                TestSwipeableRow(
                    startToEndBehaviour = SwipeBehaviour.Disabled,
                    accessibilityActions = persistentListOf(
                        StartToEndAccessibilityAction(
                            actionStringRes = R.string.designsystem_molecule_error_view_button_retry,
                        ),
                        EndToStartAccessibilityAction(
                            actionStringRes = R.string.designsystem_molecule_error_view_button_retry,
                        ),
                    ),
                )
            }
            val expectedEndToStart = getString(
                R.string.designsystem_molecule_swipeable_row_end_to_start_accessibility_description,
            ).format(getString(R.string.designsystem_molecule_error_view_button_retry))

            // Act
            val customActions = onNodeWithTag(TEST_TAG)
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]

            // Assert
            assertThat(customActions.map { it.label }).isEqualTo(listOf(expectedEndToStart))
        }

    @Test
    fun `when end-to-start behaviour is Disabled, should not have accessibility label when swiping from end to start`() =
        runComposeTest {
            // Arrange
            setContentWithTheme {
                TestSwipeableRow(
                    endToStartBehaviour = SwipeBehaviour.Disabled,
                    accessibilityActions = persistentListOf(
                        StartToEndAccessibilityAction(
                            actionStringRes = R.string.designsystem_molecule_error_view_button_retry,
                        ),
                        EndToStartAccessibilityAction(
                            actionStringRes = R.string.designsystem_molecule_error_view_button_retry,
                        ),
                    ),
                )
            }
            val expectedStartToEnd = getString(
                R.string.designsystem_molecule_swipeable_row_start_to_end_accessibility_description,
            ).format(getString(R.string.designsystem_molecule_error_view_button_retry))

            // Act
            val customActions = onNodeWithTag(TEST_TAG)
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]

            // Assert
            assertThat(customActions.map { it.label }).isEqualTo(listOf(expectedStartToEnd))
        }

    @Test
    fun `should trigger onSwipeEnd(StartToEnd) when StartToEndAccessibilityAction is triggered`() = runComposeTest {
        // Arrange
        val swipeEndDirections = mutableListOf<SwipeDirection>()

        setContentWithTheme {
            TestSwipeableRow(
                accessibilityActions = persistentListOf(
                    StartToEndAccessibilityAction(
                        actionStringRes = R.string.designsystem_molecule_error_view_button_retry,
                    ),
                ),
                onSwipeEnd = { swipeEndDirections.add(it) },
            )
        }

        // Act
        val customActions = onNodeWithTag(TEST_TAG)
            .fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]

        // Assert
        val result = customActions.first().action()
        assertThat(result).isTrue()
        assertThat(swipeEndDirections).isEqualTo(listOf(SwipeDirection.StartToEnd))
    }

    @Test
    fun `should trigger onSwipeEnd(EndToStart) when EndToStartAccessibilityAction is triggered`() = runComposeTest {
        // Arrange
        val swipeEndDirections = mutableListOf<SwipeDirection>()

        setContentWithTheme {
            TestSwipeableRow(
                accessibilityActions = persistentListOf(
                    EndToStartAccessibilityAction(
                        actionStringRes = R.string.designsystem_molecule_error_view_button_retry,
                    ),
                ),
                onSwipeEnd = { swipeEndDirections.add(it) },
            )
        }

        // Act
        val customActions = onNodeWithTag(TEST_TAG)
            .fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]

        // Assert
        val result = customActions.first().action()
        assertThat(result).isTrue()
        assertThat(swipeEndDirections).isEqualTo(listOf(SwipeDirection.EndToStart))
    }

    // endregion [ Accessibility verifications ]

    // region [ Reveal behaviour verifications ]
    @Test
    fun `when using Reveal behaviour, should call onSwipeEnd when swiping from start to end`() = runComposeTest {
        // Arrange
        val swipeEndDirections = mutableListOf<SwipeDirection>()

        setContentWithTheme {
            TestSwipeableRow(
                startToEndBehaviour = SwipeBehaviour.Reveal(),
                endToStartBehaviour = SwipeBehaviour.Disabled,
                onSwipeEnd = { swipeEndDirections.add(it) },
            )
        }

        // Act
        onNodeWithTag(TEST_TAG).performTouchInput { swipeRight() }
        waitForIdle()

        // Assert
        assertThat(swipeEndDirections).isEqualTo(listOf(SwipeDirection.StartToEnd))
    }

    @Test
    fun `when using Reveal behaviour, should call onSwipeEnd when swiping from end to start`() = runComposeTest {
        // Arrange
        val swipeEndDirections = mutableListOf<SwipeDirection>()

        setContentWithTheme {
            TestSwipeableRow(
                startToEndBehaviour = SwipeBehaviour.Disabled,
                endToStartBehaviour = SwipeBehaviour.Reveal(),
                onSwipeEnd = { swipeEndDirections.add(it) },
            )
        }

        // Act
        onNodeWithTag(TEST_TAG).performTouchInput { swipeLeft() }
        waitForIdle()

        // Assert
        assertThat(swipeEndDirections).isEqualTo(listOf(SwipeDirection.EndToStart))
    }

    @Test
    fun `when using Reveal behaviour, should call onSwipeChange when swipe direction changes`() = runComposeTest {
        // Arrange
        val swipeChangeDirections = mutableListOf<SwipeDirection>()

        setContentWithTheme {
            TestSwipeableRow(
                startToEndBehaviour = SwipeBehaviour.Reveal(),
                endToStartBehaviour = SwipeBehaviour.Reveal(),
                onSwipeChange = { swipeChangeDirections.add(it) },
            )
        }

        // Act
        onNodeWithTag(TEST_TAG).performTouchInput { swipeRight() }
        waitForIdle()

        // Assert
        assertThat(swipeChangeDirections).contains(SwipeDirection.StartToEnd)
    }
    // endregion [ Reveal behaviour verifications ]

    // region [ Mixed behaviour verifications ]
    @Test
    fun `when using Reveal for start-to-end and Dismiss for end-to-start, should call onSwipeEnd with StartToEnd on swipe right`() =
        runComposeTest {
            // Arrange
            val swipeEndDirections = mutableListOf<SwipeDirection>()

            setContentWithTheme {
                TestSwipeableRow(
                    startToEndBehaviour = SwipeBehaviour.Reveal(),
                    endToStartBehaviour = SwipeBehaviour.Dismiss(),
                    onSwipeEnd = { swipeEndDirections.add(it) },
                )
            }

            // Act
            onNodeWithTag(TEST_TAG).performTouchInput { swipeRight() }
            waitForIdle()

            // Assert
            assertThat(swipeEndDirections).isEqualTo(listOf(SwipeDirection.StartToEnd))
        }

    @Test
    fun `when using Reveal for start-to-end and Dismiss for end-to-start, should call onSwipeEnd with EndToStart on swipe left`() =
        runComposeTest {
            // Arrange
            val swipeEndDirections = mutableListOf<SwipeDirection>()

            setContentWithTheme {
                TestSwipeableRow(
                    startToEndBehaviour = SwipeBehaviour.Reveal(),
                    endToStartBehaviour = SwipeBehaviour.Dismiss(),
                    onSwipeEnd = { swipeEndDirections.add(it) },
                )
            }

            // Act
            onNodeWithTag(TEST_TAG).performTouchInput { swipeLeft() }
            waitForIdle()

            // Assert
            assertThat(swipeEndDirections).isEqualTo(listOf(SwipeDirection.EndToStart))
        }
    // endregion [ Mixed behaviour verifications ]

    // region [ Background content verifications ]
    @Test
    fun `should display background content when swiping`() = runComposeTest {
        // Arrange
        setContentWithTheme {
            TestSwipeableRow(
                startToEndBehaviour = SwipeBehaviour.Reveal(threshold = 0.25f),
                endToStartBehaviour = SwipeBehaviour.Reveal(threshold = 0.25f),
            )
        }

        // Act
        onNodeWithTag(TEST_TAG).performTouchInput { swipeLeft() }
        waitForIdle()

        // Assert
        onNodeWithTag(BACKGROUND_TEST_TAG, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `should not display background content when settled`() = runComposeTest {
        // Arrange
        setContentWithTheme {
            TestSwipeableRow()
        }

        // Act & Assert
        onNodeWithTag(BACKGROUND_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `when gesturesEnabled is false, should not display background content`() = runComposeTest {
        // Arrange
        setContentWithTheme {
            TestSwipeableRow(gesturesEnabled = false)
        }

        // Act
        onNodeWithTag(TEST_TAG).performTouchInput { swipeLeft() }
        waitForIdle()

        // Assert
        onNodeWithTag(BACKGROUND_TEST_TAG).assertDoesNotExist()
    }
    // endregion [ Background content verifications ]

    // region [ Fling verifications ]
    @Test
    fun `should call onSwipeEnd when flinging from start to end with Dismiss behaviour`() = runComposeTest {
        // Arrange
        val swipeEndDirections = mutableListOf<SwipeDirection>()

        setContentWithTheme {
            TestSwipeableRow(
                onSwipeEnd = { swipeEndDirections.add(it) },
            )
        }

        // Act
        onNodeWithTag(TEST_TAG).performTouchInput {
            swipeWithVelocity(
                start = center,
                end = Offset(center.x + FLING_SHORT_DISTANCE, center.y),
                endVelocity = FLING_HIGH_VELOCITY,
            )
        }
        waitForIdle()

        // Assert
        assertThat(swipeEndDirections).isEqualTo(listOf(SwipeDirection.StartToEnd))
    }

    @Test
    fun `should call onSwipeEnd when flinging from end to start with Dismiss behaviour`() = runComposeTest {
        // Arrange
        val swipeEndDirections = mutableListOf<SwipeDirection>()

        setContentWithTheme {
            TestSwipeableRow(
                onSwipeEnd = { swipeEndDirections.add(it) },
            )
        }

        // Act
        onNodeWithTag(TEST_TAG).performTouchInput {
            swipeWithVelocity(
                start = center,
                end = Offset(center.x - FLING_SHORT_DISTANCE, center.y),
                endVelocity = FLING_HIGH_VELOCITY,
            )
        }
        waitForIdle()

        // Assert
        assertThat(swipeEndDirections).isEqualTo(listOf(SwipeDirection.EndToStart))
    }

    @Test
    fun `should call onSwipeEnd when flinging from start to end with Reveal behaviour`() = runComposeTest {
        // Arrange
        val swipeEndDirections = mutableListOf<SwipeDirection>()

        setContentWithTheme {
            TestSwipeableRow(
                startToEndBehaviour = SwipeBehaviour.Reveal(),
                endToStartBehaviour = SwipeBehaviour.Disabled,
                onSwipeEnd = { swipeEndDirections.add(it) },
            )
        }

        // Act
        onNodeWithTag(TEST_TAG).performTouchInput {
            swipeWithVelocity(
                start = center,
                end = Offset(center.x + FLING_SHORT_DISTANCE, center.y),
                endVelocity = FLING_HIGH_VELOCITY,
            )
        }
        waitForIdle()

        // Assert
        assertThat(swipeEndDirections).isEqualTo(listOf(SwipeDirection.StartToEnd))
    }

    @Test
    fun `should call onSwipeEnd when flinging from end to start with Reveal behaviour`() = runComposeTest {
        // Arrange
        val swipeEndDirections = mutableListOf<SwipeDirection>()

        setContentWithTheme {
            TestSwipeableRow(
                startToEndBehaviour = SwipeBehaviour.Disabled,
                endToStartBehaviour = SwipeBehaviour.Reveal(),
                onSwipeEnd = { swipeEndDirections.add(it) },
            )
        }

        // Act
        onNodeWithTag(TEST_TAG).performTouchInput {
            swipeWithVelocity(
                start = center,
                end = Offset(center.x - FLING_SHORT_DISTANCE, center.y),
                endVelocity = FLING_HIGH_VELOCITY,
            )
        }
        waitForIdle()

        // Assert
        assertThat(swipeEndDirections).isEqualTo(listOf(SwipeDirection.EndToStart))
    }

    @Test
    fun `should not call onSwipeEnd when flinging in a disabled direction`() = runComposeTest {
        // Arrange
        val swipeEndDirections = mutableListOf<SwipeDirection>()

        setContentWithTheme {
            TestSwipeableRow(
                startToEndBehaviour = SwipeBehaviour.Disabled,
                onSwipeEnd = { swipeEndDirections.add(it) },
            )
        }

        // Act
        onNodeWithTag(TEST_TAG).performTouchInput {
            swipeWithVelocity(
                start = center,
                end = Offset(center.x + FLING_SHORT_DISTANCE, center.y),
                endVelocity = FLING_HIGH_VELOCITY,
            )
        }
        waitForIdle()

        // Assert
        assertThat(swipeEndDirections).isEmpty()
    }

    @Test
    fun `when gesturesEnabled is false, should not call onSwipeEnd when flinging`() = runComposeTest {
        // Arrange
        val swipeEndDirections = mutableListOf<SwipeDirection>()

        setContentWithTheme {
            TestSwipeableRow(
                gesturesEnabled = false,
                onSwipeEnd = { swipeEndDirections.add(it) },
            )
        }

        // Act
        onNodeWithTag(TEST_TAG).performTouchInput {
            swipeWithVelocity(
                start = center,
                end = Offset(center.x + FLING_SHORT_DISTANCE, center.y),
                endVelocity = FLING_HIGH_VELOCITY,
            )
        }
        waitForIdle()

        // Assert
        assertThat(swipeEndDirections).isEmpty()
    }

    @Test
    fun `should not call onSwipeEnd when flinging with low velocity and high Dismiss threshold`() = runComposeTest {
        // Arrange
        val swipeEndDirections = mutableListOf<SwipeDirection>()

        setContentWithTheme {
            TestSwipeableRow(
                startToEndBehaviour = SwipeBehaviour.Dismiss(threshold = FLING_HIGH_THRESHOLD),
                endToStartBehaviour = SwipeBehaviour.Disabled,
                onSwipeEnd = { swipeEndDirections.add(it) },
            )
        }

        // Act
        onNodeWithTag(TEST_TAG).performTouchInput {
            swipeWithVelocity(
                start = center,
                end = Offset(center.x + FLING_SHORT_DISTANCE, center.y),
                endVelocity = FLING_LOW_VELOCITY,
            )
        }
        waitForIdle()

        // Assert
        assertThat(swipeEndDirections).isEmpty()
    }

    @Test
    fun `should not call onSwipeEnd when flinging with low velocity and high Reveal threshold`() = runComposeTest {
        // Arrange
        val swipeEndDirections = mutableListOf<SwipeDirection>()

        setContentWithTheme {
            TestSwipeableRow(
                startToEndBehaviour = SwipeBehaviour.Reveal(threshold = FLING_HIGH_THRESHOLD),
                endToStartBehaviour = SwipeBehaviour.Disabled,
                onSwipeEnd = { swipeEndDirections.add(it) },
            )
        }

        // Act
        onNodeWithTag(TEST_TAG).performTouchInput {
            swipeWithVelocity(
                start = center,
                end = Offset(center.x + FLING_SHORT_DISTANCE, center.y),
                endVelocity = FLING_LOW_VELOCITY,
            )
        }
        waitForIdle()

        // Assert
        assertThat(swipeEndDirections).isEmpty()
    }
    // endregion [ Fling verifications ]

    private companion object {
        const val TEST_TAG = SwipeableRowDefaults.SWIPEABLE_ROW_CORE_ELEMENT_TEST_TAG
        const val BACKGROUND_TEST_TAG = "backgroundContent"
        const val FLING_HIGH_VELOCITY = 5_000f
        const val FLING_LOW_VELOCITY = 200f
        const val FLING_SHORT_DISTANCE = 100f
        const val FLING_HIGH_THRESHOLD = 0.9f
    }
}

@Composable
private fun TestSwipeableRow(
    startToEndBehaviour: SwipeBehaviour = SwipeBehaviour.Dismiss(),
    endToStartBehaviour: SwipeBehaviour = SwipeBehaviour.Dismiss(),
    gesturesEnabled: Boolean = true,
    accessibilityActions: ImmutableList<SwipeDirectionAccessibilityAction> = persistentListOf(),
    onSwipeEnd: (SwipeDirection) -> Unit = {},
    onSwipeChange: (SwipeDirection) -> Unit = {},
) {
    val state = rememberSwipeableRowState(
        startToEndBehaviour = startToEndBehaviour,
        endToStartBehaviour = endToStartBehaviour,
        accessibilityActions = accessibilityActions,
    )
    SwipeableRow(
        state = state,
        backgroundContent = {
            Surface(
                color = MainTheme.colors.primaryContainer,
                contentColor = MainTheme.colors.onPrimaryContainer,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("backgroundContent"),
            ) {
                Row {
                    TextBodyLarge(
                        text = "Background Element",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = MainTheme.spacings.quadruple, horizontal = MainTheme.spacings.double),
                    )
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        gesturesEnabled = gesturesEnabled,
        onSwipeEnd = onSwipeEnd,
        onSwipeChange = onSwipeChange,
    ) {
        Surface(
            color = MainTheme.colors.surfaceContainer,
        ) {
            TextBodyLarge(
                text = "Foreground Element",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MainTheme.spacings.quadruple),
            )
        }
    }
}
