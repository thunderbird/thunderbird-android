package net.thunderbird.core.ui.compose.designsystem.molecule.swipe

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.R
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.setContentWithTheme
import app.k9mail.core.ui.compose.theme2.MainTheme
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
    fun `when enableDismissFromStartToEnd is false, should not call onSwipeEnd when swiping from start to end`() =
        runComposeTest {
            // Arrange
            val swipeEndDirections = mutableListOf<SwipeDirection>()

            setContentWithTheme {
                TestSwipeableRow(
                    enableDismissFromStartToEnd = false,
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
    fun `when enableDismissFromEndToStart is false, should not call onSwipeEnd when swiping from end to start`() =
        runComposeTest {
            // Arrange
            val swipeEndDirections = mutableListOf<SwipeDirection>()

            setContentWithTheme {
                TestSwipeableRow(
                    enableDismissFromEndToStart = false,
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
    fun `when enableDismissFromStartToEnd is false, should not have accessibility label when swiping from start to end`() =
        runComposeTest {
            // Arrange
            setContentWithTheme {
                TestSwipeableRow(
                    enableDismissFromStartToEnd = false,
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
    fun `when enableDismissFromEndToStart is false, should not have accessibility label when swiping from end to start`() =
        runComposeTest {
            // Arrange
            setContentWithTheme {
                TestSwipeableRow(
                    enableDismissFromEndToStart = false,
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

    private companion object {
        const val TEST_TAG = "swipeableRow"
    }
}

@Composable
private fun TestSwipeableRow(
    enableDismissFromStartToEnd: Boolean = true,
    enableDismissFromEndToStart: Boolean = true,
    gesturesEnabled: Boolean = true,
    accessibilityActions: ImmutableList<SwipeDirectionAccessibilityAction> = persistentListOf(),
    onSwipeEnd: (SwipeDirection) -> Unit = {},
    onSwipeChange: (SwipeDirection) -> Unit = {},
) {
    val state = rememberSwipeableRowState(accessibilityActions = accessibilityActions)
    SwipeableRow(
        state = state,
        backgroundContent = { direction ->
            Surface(
                color = MainTheme.colors.primaryContainer,
                contentColor = MainTheme.colors.onPrimaryContainer,
                modifier = Modifier.fillMaxSize(),
            ) {
                Row {
                    TextBodyLarge(
                        text = "Background Element (${direction.name.lowercase()})",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = MainTheme.spacings.quadruple, horizontal = MainTheme.spacings.double),
                        textAlign = when (direction) {
                            SwipeDirection.StartToEnd -> TextAlign.Start
                            SwipeDirection.EndToStart -> TextAlign.End
                            SwipeDirection.Settled -> TextAlign.Unspecified
                        },
                    )
                }
            }
        },
        modifier = Modifier
            .testTag("swipeableRow")
            .fillMaxWidth()
            .height(80.dp),
        enableDismissFromStartToEnd = enableDismissFromStartToEnd,
        enableDismissFromEndToStart = enableDismissFromEndToStart,
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
