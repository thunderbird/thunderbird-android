package net.thunderbird.feature.notification.api.ui.host

import app.cash.turbine.test
import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.hasMessage
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.test.runTest
import net.thunderbird.feature.notification.api.NotificationSeverity
import net.thunderbird.feature.notification.api.ui.action.NotificationAction
import net.thunderbird.feature.notification.api.ui.host.visual.BannerGlobalVisual
import net.thunderbird.feature.notification.api.ui.host.visual.BannerInlineVisual
import net.thunderbird.feature.notification.api.ui.host.visual.BannerInlineVisual.Companion.MAX_SUPPORTING_TEXT_LENGTH
import net.thunderbird.feature.notification.api.ui.host.visual.BannerInlineVisual.Companion.MAX_TITLE_LENGTH
import net.thunderbird.feature.notification.api.ui.host.visual.InAppNotificationHostState
import net.thunderbird.feature.notification.api.ui.host.visual.SnackbarVisual
import net.thunderbird.feature.notification.api.ui.style.inAppNotificationStyles
import net.thunderbird.feature.notification.testing.fake.FakeInAppOnlyNotification
import net.thunderbird.feature.notification.testing.fake.ui.action.createFakeNotificationAction

@Suppress("MaxLineLength", "LargeClass")
class InAppNotificationHostStateHolderTest {
    @Test
    fun `showInAppNotification should show bannerGlobal when InAppNotification has BannerGlobalNotification style and BannerGlobalNotifications is enabled`() =
        runTest {
            // Arrange
            val expectedContentText = "expected text"
            val expectedAction = createFakeNotificationAction()
            val expectedSeverity = NotificationSeverity.Warning
            val notification = FakeInAppOnlyNotification(
                contentText = expectedContentText,
                inAppNotificationStyles = inAppNotificationStyles { bannerGlobal() },
                actions = setOf(expectedAction),
                severity = expectedSeverity,
            )
            val flags = persistentSetOf(DisplayInAppNotificationFlag.BannerGlobalNotifications)
            val testSubject = InAppNotificationHostStateHolder(enabled = flags)

            // Act
            testSubject.showInAppNotification(notification)

            // Assert
            testSubject.currentInAppNotificationHostState.test {
                val state = awaitItem()
                assertThat(state)
                    .prop(InAppNotificationHostState::bannerGlobalVisual)
                    .isNotNull()
                    .all {
                        prop(BannerGlobalVisual::message)
                            .isEqualTo(expectedContentText)
                        prop(BannerGlobalVisual::action)
                            .isEqualTo(expectedAction)
                        prop(BannerGlobalVisual::severity)
                            .isEqualTo(expectedSeverity)
                    }
            }
        }

    @Test
    fun `showInAppNotification should show bannerGlobal when InAppNotification has BannerGlobalNotification style and AllNotifications is enabled`() =
        runTest {
            // Arrange
            val expectedContentText = "expected text"
            val expectedAction = createFakeNotificationAction()
            val expectedSeverity = NotificationSeverity.Warning
            val notification = FakeInAppOnlyNotification(
                contentText = expectedContentText,
                inAppNotificationStyles = inAppNotificationStyles { bannerGlobal() },
                actions = setOf(expectedAction),
                severity = expectedSeverity,
            )
            val flags = DisplayInAppNotificationFlag.AllNotifications
            val testSubject = InAppNotificationHostStateHolder(enabled = flags)

            // Act
            testSubject.showInAppNotification(notification)

            // Assert
            testSubject.currentInAppNotificationHostState.test {
                val state = awaitItem()
                assertThat(state)
                    .prop(InAppNotificationHostState::bannerGlobalVisual)
                    .isNotNull()
                    .all {
                        prop(BannerGlobalVisual::message)
                            .isEqualTo(expectedContentText)
                        prop(BannerGlobalVisual::action)
                            .isEqualTo(expectedAction)
                        prop(BannerGlobalVisual::severity)
                            .isEqualTo(expectedSeverity)
                    }
            }
        }

    @Test
    fun `showInAppNotification should not show bannerGlobal when InAppNotification has BannerGlobalNotification style but BannerGlobalNotifications is disabled`() =
        runTest {
            // Arrange
            val notification = FakeInAppOnlyNotification(
                inAppNotificationStyles = inAppNotificationStyles { bannerGlobal() },
                contentText = "not important in this test case",
                actions = setOf(createFakeNotificationAction()),
            )
            val flags = persistentSetOf<DisplayInAppNotificationFlag>()
            val testSubject = InAppNotificationHostStateHolder(enabled = flags)

            // Act
            testSubject.showInAppNotification(notification)

            // Assert
            testSubject.currentInAppNotificationHostState.test {
                val state = awaitItem()
                assertThat(state)
                    .prop(InAppNotificationHostState::bannerGlobalVisual)
                    .isNull()
            }
        }

    @Test
    fun `showInAppNotification throws IllegalStateException when InAppNotification has BannerGlobalNotification style but has multiple actions`() =
        runTest {
            // Arrange
            val expectedMessage = "A notification with a BannerGlobalNotification style must have at zero or one action"
            val notification = FakeInAppOnlyNotification(
                inAppNotificationStyles = inAppNotificationStyles { bannerGlobal() },
                actions = setOf(
                    createFakeNotificationAction(title = "fake action 1"),
                    createFakeNotificationAction(title = "fake action 2"),
                ),
            )
            val flags = persistentSetOf<DisplayInAppNotificationFlag>()
            val testSubject = InAppNotificationHostStateHolder(enabled = flags)

            // Act
            val exception = assertFails {
                testSubject.showInAppNotification(notification)
            }

            // Assert
            assertThat(exception)
                .isInstanceOf<IllegalStateException>()
                .hasMessage(expectedMessage)
        }

    @Test
    fun `showInAppNotification throws IllegalStateException when InAppNotification has BannerGlobalNotification style but no contentText is configured`() =
        runTest {
            // Arrange
            val expectedMessage =
                "A notification with a BannerGlobalNotification style must have a contentText not null"
            val notification = FakeInAppOnlyNotification(
                inAppNotificationStyles = inAppNotificationStyles { bannerGlobal() },
                contentText = null,
                actions = setOf(createFakeNotificationAction()),
            )
            val flags = persistentSetOf<DisplayInAppNotificationFlag>()
            val testSubject = InAppNotificationHostStateHolder(enabled = flags)

            // Act
            val exception = assertFails {
                testSubject.showInAppNotification(notification)
            }

            // Assert
            assertThat(exception)
                .isInstanceOf<IllegalStateException>()
                .hasMessage(expectedMessage)
        }

    @Test
    fun `showInAppNotification should show bannerInline when InAppNotification has BannerInlineNotification style and BannerInlineNotifications is enabled`() =
        runTest {
            // Arrange
            val expectedContentText = "expected text"
            val expectedAction = createFakeNotificationAction()
            val expectedSeverity = NotificationSeverity.Warning
            val notification = FakeInAppOnlyNotification(
                contentText = expectedContentText,
                inAppNotificationStyles = inAppNotificationStyles { bannerInline() },
                actions = setOf(expectedAction),
                severity = expectedSeverity,
            )
            val flags = persistentSetOf(DisplayInAppNotificationFlag.BannerInlineNotifications)
            val testSubject = InAppNotificationHostStateHolder(enabled = flags)

            // Act
            testSubject.showInAppNotification(notification)

            // Assert
            testSubject.currentInAppNotificationHostState.test {
                val state = awaitItem()
                assertThat(state)
                    .prop(InAppNotificationHostState::bannerInlineVisuals)
                    .all {
                        isNotEmpty()
                        hasSize(1)
                        transform { it.single() }
                            .all {
                                prop(BannerInlineVisual::supportingText)
                                    .isEqualTo(expectedContentText)
                                prop(BannerInlineVisual::actions).all {
                                    hasSize(1)
                                    contains(expectedAction)
                                }
                                prop(BannerInlineVisual::severity)
                                    .isEqualTo(expectedSeverity)
                            }
                    }
            }
        }

    @Test
    fun `showInAppNotification should show bannerInline when InAppNotification has BannerInlineNotification style and AllNotifications is enabled`() =
        runTest {
            // Arrange
            val expectedContentText = "expected text"
            val expectedAction = createFakeNotificationAction()
            val expectedSeverity = NotificationSeverity.Warning
            val notification = FakeInAppOnlyNotification(
                contentText = expectedContentText,
                inAppNotificationStyles = inAppNotificationStyles { bannerInline() },
                actions = setOf(expectedAction),
                severity = expectedSeverity,
            )
            val flags = DisplayInAppNotificationFlag.AllNotifications
            val testSubject = InAppNotificationHostStateHolder(enabled = flags)

            // Act
            testSubject.showInAppNotification(notification)

            // Assert
            testSubject.currentInAppNotificationHostState.test {
                val state = awaitItem()
                assertThat(state)
                    .prop(InAppNotificationHostState::bannerInlineVisuals)
                    .all {
                        isNotEmpty()
                        hasSize(1)
                        transform { it.single() }
                            .all {
                                prop(BannerInlineVisual::supportingText)
                                    .isEqualTo(expectedContentText)
                                prop(BannerInlineVisual::actions).all {
                                    hasSize(1)
                                    contains(expectedAction)
                                }
                                prop(BannerInlineVisual::severity)
                                    .isEqualTo(expectedSeverity)
                            }
                    }
            }
        }

    @Test
    fun `showInAppNotification should not show bannerInline when InAppNotification has BannerInlineNotification style but BannerInlineNotifications is disabled`() =
        runTest {
            // Arrange
            val notification = FakeInAppOnlyNotification(
                inAppNotificationStyles = inAppNotificationStyles { bannerInline() },
                contentText = "not important in this test case",
                actions = setOf(createFakeNotificationAction()),
            )
            val flags = persistentSetOf<DisplayInAppNotificationFlag>()
            val testSubject = InAppNotificationHostStateHolder(enabled = flags)

            // Act
            testSubject.showInAppNotification(notification)

            // Assert
            testSubject.currentInAppNotificationHostState.test {
                val state = awaitItem()
                assertThat(state)
                    .prop(InAppNotificationHostState::bannerInlineVisuals)
                    .isEmpty()
            }
        }

    @Test
    fun `showInAppNotification should not show duplicated banner inline notifications`() = runTest {
        // Arrange
        val expectedContentText = "expected text"
        val expectedAction = createFakeNotificationAction()
        val expectedSeverity = NotificationSeverity.Warning
        val notification = FakeInAppOnlyNotification(
            contentText = expectedContentText,
            inAppNotificationStyles = inAppNotificationStyles { bannerInline() },
            actions = setOf(expectedAction),
            severity = expectedSeverity,
        )
        val flags = DisplayInAppNotificationFlag.AllNotifications
        val testSubject = InAppNotificationHostStateHolder(enabled = flags)

        // Act
        repeat(times = 100) {
            testSubject.showInAppNotification(notification)
        }

        // Assert
        testSubject.currentInAppNotificationHostState.test {
            val state = awaitItem()
            assertThat(state)
                .prop(InAppNotificationHostState::bannerInlineVisuals)
                .all {
                    isNotEmpty()
                    hasSize(1)
                    transform { it.single() }
                        .all {
                            prop(BannerInlineVisual::supportingText)
                                .isEqualTo(expectedContentText)
                            prop(BannerInlineVisual::actions).all {
                                hasSize(1)
                                contains(expectedAction)
                            }
                            prop(BannerInlineVisual::severity)
                                .isEqualTo(expectedSeverity)
                        }
                }
        }
    }

    @Test
    fun `showInAppNotification should show multiple bannerInlines when different BannerInlineNotification are triggered`() =
        runTest {
            // Arrange
            fun getSeverity(index: Int): NotificationSeverity = when (index) {
                in 0..<25 -> NotificationSeverity.Critical
                in 25..<50 -> NotificationSeverity.Information
                in 50..<75 -> NotificationSeverity.Temporary
                else -> NotificationSeverity.Fatal
            }

            fun getAction(index: Int): NotificationAction = when (index) {
                in 0..<25 -> createFakeNotificationAction(title = "fake action 1")
                in 25..<50 -> createFakeNotificationAction(title = "fake action 2")
                in 50..<75 -> createFakeNotificationAction(title = "fake action 3")
                else -> createFakeNotificationAction(title = "fake action 4")
            }

            val expectedSize = 100
            val notifications = List(size = expectedSize) { index ->
                FakeInAppOnlyNotification(
                    title = "fake title $index",
                    contentText = "fake notification $index",
                    inAppNotificationStyles = inAppNotificationStyles { bannerInline() },
                    actions = setOf(getAction(index)),
                    severity = getSeverity(index),
                )
            }
            val flags = DisplayInAppNotificationFlag.AllNotifications
            val testSubject = InAppNotificationHostStateHolder(enabled = flags)

            // Act
            notifications.forEach { notification ->
                testSubject.showInAppNotification(notification)
            }

            // Assert
            testSubject.currentInAppNotificationHostState.test {
                val state = awaitItem()
                assertThat(state)
                    .prop(InAppNotificationHostState::bannerInlineVisuals)
                    .all {
                        isNotEmpty()
                        hasSize(expectedSize)
                        given { visuals ->
                            visuals.forEachIndexed { index, visual ->
                                assertThat(visual).all {
                                    prop(BannerInlineVisual::title)
                                        .isEqualTo("fake title $index")
                                    prop(BannerInlineVisual::supportingText)
                                        .isEqualTo("fake notification $index")
                                    prop(BannerInlineVisual::severity)
                                        .isEqualTo(getSeverity(index))
                                    prop(BannerInlineVisual::actions).all {
                                        hasSize(1)
                                        containsExactly(getAction(index))
                                    }
                                }
                            }
                        }
                    }
            }
        }

    @Test
    fun `showInAppNotification throws IllegalStateException when InAppNotification has BannerInlineNotification style but no action is configured`() =
        runTest {
            // Arrange
            val expectedMessage = "A notification with a BannerInlineNotification style must have at one or two actions"
            val notification = FakeInAppOnlyNotification(
                inAppNotificationStyles = inAppNotificationStyles { bannerInline() },
                contentText = "not important in this test case",
            )
            val flags = persistentSetOf<DisplayInAppNotificationFlag>()
            val testSubject = InAppNotificationHostStateHolder(enabled = flags)

            // Act
            val exception = assertFails {
                testSubject.showInAppNotification(notification)
            }

            // Assert
            assertThat(exception)
                .isInstanceOf<IllegalStateException>()
                .hasMessage(expectedMessage)
        }

    @Test
    fun `showInAppNotification throws IllegalStateException when InAppNotification has BannerInlineNotification style with an empty title`() =
        runTest {
            // Arrange
            val expectedMessage =
                "A notification with a BannerInlineNotification style must have a title length of 1 to " +
                    "$MAX_TITLE_LENGTH characters."
            val notification = FakeInAppOnlyNotification(
                inAppNotificationStyles = inAppNotificationStyles { bannerInline() },
                title = "", // empty
            )
            val flags = persistentSetOf<DisplayInAppNotificationFlag>()
            val testSubject = InAppNotificationHostStateHolder(enabled = flags)

            // Act
            val exception = assertFails {
                testSubject.showInAppNotification(notification)
            }

            // Assert
            assertThat(exception)
                .isInstanceOf<IllegalStateException>()
                .hasMessage(expectedMessage)
        }

    @Test
    fun `showInAppNotification throws IllegalStateException when InAppNotification has BannerInlineNotification style with a title longer than 100 chars`() =
        runTest {
            // Arrange
            val expectedMessage =
                "A notification with a BannerInlineNotification style must have a title length of 1 to " +
                    "$MAX_TITLE_LENGTH characters."
            val notification = FakeInAppOnlyNotification(
                inAppNotificationStyles = inAppNotificationStyles { bannerInline() },
                title = "*".repeat(101),
            )
            val flags = persistentSetOf<DisplayInAppNotificationFlag>()
            val testSubject = InAppNotificationHostStateHolder(enabled = flags)

            // Act
            val exception = assertFails {
                testSubject.showInAppNotification(notification)
            }

            // Assert
            assertThat(exception)
                .isInstanceOf<IllegalStateException>()
                .hasMessage(expectedMessage)
        }

    @Test
    fun `showInAppNotification throws IllegalStateException when InAppNotification has BannerInlineNotification style with a null contentText`() =
        runTest {
            // Arrange
            val expectedMessage =
                "A notification with a BannerInlineNotification style must have a contentText not null"
            val notification = FakeInAppOnlyNotification(
                inAppNotificationStyles = inAppNotificationStyles { bannerInline() },
                title = "fake title",
                contentText = null,
            )
            val flags = persistentSetOf<DisplayInAppNotificationFlag>()
            val testSubject = InAppNotificationHostStateHolder(enabled = flags)

            // Act
            val exception = assertFails {
                testSubject.showInAppNotification(notification)
            }

            // Assert
            assertThat(exception)
                .isInstanceOf<IllegalStateException>()
                .hasMessage(expectedMessage)
        }

    @Test
    fun `showInAppNotification throws IllegalStateException when InAppNotification has BannerInlineNotification style with an empty contentText`() =
        runTest {
            // Arrange
            val expectedMessage =
                "A notification with a BannerInlineNotification style must have a contentText length " +
                    "of 1 to $MAX_SUPPORTING_TEXT_LENGTH characters."
            val notification = FakeInAppOnlyNotification(
                inAppNotificationStyles = inAppNotificationStyles { bannerInline() },
                title = "fake title",
                contentText = "", // empty
            )
            val flags = persistentSetOf<DisplayInAppNotificationFlag>()
            val testSubject = InAppNotificationHostStateHolder(enabled = flags)

            // Act
            val exception = assertFails {
                testSubject.showInAppNotification(notification)
            }

            // Assert
            assertThat(exception)
                .isInstanceOf<IllegalStateException>()
                .hasMessage(expectedMessage)
        }

    @Test
    fun `showInAppNotification throws IllegalStateException when InAppNotification has BannerInlineNotification style with a contentText longer than 200 chars`() =
        runTest {
            // Arrange
            val expectedMessage =
                "A notification with a BannerInlineNotification style must have a contentText length of " +
                    "1 to $MAX_SUPPORTING_TEXT_LENGTH characters."
            val notification = FakeInAppOnlyNotification(
                inAppNotificationStyles = inAppNotificationStyles { bannerInline() },
                title = "fake title",
                contentText = "*".repeat(201),
            )
            val flags = persistentSetOf<DisplayInAppNotificationFlag>()
            val testSubject = InAppNotificationHostStateHolder(enabled = flags)

            // Act
            val exception = assertFails {
                testSubject.showInAppNotification(notification)
            }

            // Assert
            assertThat(exception)
                .isInstanceOf<IllegalStateException>()
                .hasMessage(expectedMessage)
        }

    @Test
    fun `showInAppNotification throws IllegalStateException when InAppNotification has BannerInlineNotification style with more than 2 actions`() =
        runTest {
            // Arrange
            val expectedMessage = "A notification with a BannerInlineNotification style must have at one or two actions"
            val notification = FakeInAppOnlyNotification(
                inAppNotificationStyles = inAppNotificationStyles { bannerInline() },
                contentText = "not important in this test case",
                actions = setOf(
                    createFakeNotificationAction(title = "fake action 1"),
                    createFakeNotificationAction(title = "fake action 2"),
                    createFakeNotificationAction(title = "fake action 3"),
                ),
            )
            val flags = persistentSetOf<DisplayInAppNotificationFlag>()
            val testSubject = InAppNotificationHostStateHolder(enabled = flags)

            // Act
            val exception = assertFails {
                testSubject.showInAppNotification(notification)
            }

            // Assert
            assertThat(exception)
                .isInstanceOf<IllegalStateException>()
                .hasMessage(expectedMessage)
        }

    @Test
    fun `showInAppNotification should show snackbar when InAppNotification has SnackbarNotification style and SnackbarNotifications is enabled`() =
        runTest {
            // Arrange
            val expectedContentText = "expected text"
            val expectedAction = createFakeNotificationAction()
            val expectedSeverity = NotificationSeverity.Warning
            val expectedDuration = 1.hours
            val notification = FakeInAppOnlyNotification(
                contentText = expectedContentText,
                inAppNotificationStyles = inAppNotificationStyles { snackbar(expectedDuration) },
                actions = setOf(expectedAction),
                severity = expectedSeverity,
            )
            val flags = persistentSetOf(DisplayInAppNotificationFlag.SnackbarNotifications)
            val testSubject = InAppNotificationHostStateHolder(enabled = flags)

            // Act
            testSubject.showInAppNotification(notification)

            // Assert
            testSubject.currentInAppNotificationHostState.test {
                val state = awaitItem()
                assertThat(state)
                    .prop(InAppNotificationHostState::snackbarVisual)
                    .isNotNull()
                    .all {
                        prop(SnackbarVisual::message)
                            .isEqualTo(expectedContentText)
                        prop(SnackbarVisual::action)
                            .isEqualTo(expectedAction)
                        prop(SnackbarVisual::duration)
                            .isEqualTo(expectedDuration)
                    }
            }
        }

    @Test
    fun `showInAppNotification should show snackbar when InAppNotification has SnackbarNotification style and AllNotifications is enabled`() =
        runTest {
            // Arrange
            val expectedContentText = "expected text"
            val expectedAction = createFakeNotificationAction()
            val expectedSeverity = NotificationSeverity.Warning
            val expectedDuration = 1.hours
            val notification = FakeInAppOnlyNotification(
                contentText = expectedContentText,
                inAppNotificationStyles = inAppNotificationStyles { snackbar(expectedDuration) },
                actions = setOf(expectedAction),
                severity = expectedSeverity,
            )
            val flags = DisplayInAppNotificationFlag.AllNotifications
            val testSubject = InAppNotificationHostStateHolder(enabled = flags)

            // Act
            testSubject.showInAppNotification(notification)

            // Assert
            testSubject.currentInAppNotificationHostState.test {
                val state = awaitItem()
                assertThat(state)
                    .prop(InAppNotificationHostState::snackbarVisual)
                    .isNotNull()
                    .all {
                        prop(SnackbarVisual::message)
                            .isEqualTo(expectedContentText)
                        prop(SnackbarVisual::action)
                            .isEqualTo(expectedAction)
                        prop(SnackbarVisual::duration)
                            .isEqualTo(expectedDuration)
                    }
            }
        }

    @Test
    fun `showInAppNotification should not show snackbar when InAppNotification has SnackbarNotification style but SnackbarNotifications is disabled`() =
        runTest {
            // Arrange
            val notification = FakeInAppOnlyNotification(
                inAppNotificationStyles = inAppNotificationStyles { snackbar() },
                contentText = "not important in this test case",
                actions = setOf(createFakeNotificationAction()),
            )
            val flags = persistentSetOf<DisplayInAppNotificationFlag>()
            val testSubject = InAppNotificationHostStateHolder(enabled = flags)

            // Act
            testSubject.showInAppNotification(notification)

            // Assert
            testSubject.currentInAppNotificationHostState.test {
                val state = awaitItem()
                assertThat(state)
                    .prop(InAppNotificationHostState::snackbarVisual)
                    .isNull()
            }
        }

    @Test
    fun `showInAppNotification throws IllegalStateException when InAppNotification has SnackbarNotification style but no action is configured`() =
        runTest {
            // Arrange
            val expectedMessage = "A notification with a SnackbarNotification style must have exactly one action"
            val notification = FakeInAppOnlyNotification(
                inAppNotificationStyles = inAppNotificationStyles { snackbar() },
                contentText = "not important in this test case",
            )
            val flags = persistentSetOf<DisplayInAppNotificationFlag>()
            val testSubject = InAppNotificationHostStateHolder(enabled = flags)

            // Act
            val exception = assertFails {
                testSubject.showInAppNotification(notification)
            }

            // Assert
            assertThat(exception)
                .isInstanceOf<IllegalStateException>()
                .hasMessage(expectedMessage)
        }

    @Test
    fun `showInAppNotification throws IllegalStateException when InAppNotification has SnackbarNotification style but has multiple actions`() =
        runTest {
            // Arrange
            val expectedMessage = "A notification with a SnackbarNotification style must have exactly one action"
            val notification = FakeInAppOnlyNotification(
                inAppNotificationStyles = inAppNotificationStyles { snackbar() },
                actions = setOf(
                    createFakeNotificationAction(title = "fake action 1"),
                    createFakeNotificationAction(title = "fake action 2"),
                ),
            )
            val flags = persistentSetOf<DisplayInAppNotificationFlag>()
            val testSubject = InAppNotificationHostStateHolder(enabled = flags)

            // Act
            val exception = assertFails {
                testSubject.showInAppNotification(notification)
            }

            // Assert
            assertThat(exception)
                .isInstanceOf<IllegalStateException>()
                .hasMessage(expectedMessage)
        }

    @Test
    fun `showInAppNotification throws IllegalStateException when InAppNotification has SnackbarNotification style but no contentText is configured`() =
        runTest {
            // Arrange
            val expectedMessage = "A notification with a SnackbarNotification style must have a contentText not null"
            val notification = FakeInAppOnlyNotification(
                inAppNotificationStyles = inAppNotificationStyles { snackbar() },
                contentText = null,
                actions = setOf(createFakeNotificationAction()),
            )
            val flags = persistentSetOf<DisplayInAppNotificationFlag>()
            val testSubject = InAppNotificationHostStateHolder(enabled = flags)

            // Act
            val exception = assertFails {
                testSubject.showInAppNotification(notification)
            }

            // Assert
            assertThat(exception)
                .isInstanceOf<IllegalStateException>()
                .hasMessage(expectedMessage)
        }

    @Test
    fun `dismiss should remove bannerGlobal notification given a BannerGlobalVisual`() = runTest {
        // Arrange
        val notification = FakeInAppOnlyNotification(
            inAppNotificationStyles = inAppNotificationStyles { bannerGlobal() },
            actions = setOf(createFakeNotificationAction()),
        )
        val visual = requireNotNull(BannerGlobalVisual.from(notification))
        val flags = persistentSetOf(DisplayInAppNotificationFlag.BannerGlobalNotifications)
        val testSubject = InAppNotificationHostStateHolder(enabled = flags)
        testSubject.showInAppNotification(notification)

        // Act
        testSubject.dismiss(visual)

        // Assert
        testSubject.currentInAppNotificationHostState.test {
            val state = awaitItem()
            assertThat(state)
                .prop(InAppNotificationHostState::bannerGlobalVisual)
                .isNull()
        }
    }

    @Test
    fun `dismiss should remove bannerInline notification given a BannerInlineVisual`() = runTest {
        // Arrange
        val expectedContentText = "expected text"
        val expectedAction = createFakeNotificationAction()
        val expectedSeverity = NotificationSeverity.Warning
        val notification = FakeInAppOnlyNotification(
            contentText = expectedContentText,
            inAppNotificationStyles = inAppNotificationStyles { bannerInline() },
            actions = setOf(expectedAction),
            severity = expectedSeverity,
        )
        val visual = BannerInlineVisual.from(notification).first()
        val flags = DisplayInAppNotificationFlag.AllNotifications
        val testSubject = InAppNotificationHostStateHolder(enabled = flags)
        testSubject.showInAppNotification(notification)

        // Act
        testSubject.dismiss(visual)

        // Assert
        testSubject.currentInAppNotificationHostState.test {
            val state = awaitItem()
            assertThat(state)
                .prop(InAppNotificationHostState::bannerInlineVisuals)
                .isEmpty()
        }
    }

    @Test
    fun `dismiss should remove only one bannerInline notification when multiple bannerInline notifications are visible`() =
        runTest {
            // Arrange
            fun getSeverity(index: Int): NotificationSeverity = when (index) {
                in 0..<25 -> NotificationSeverity.Critical
                in 25..<50 -> NotificationSeverity.Information
                in 50..<75 -> NotificationSeverity.Temporary
                else -> NotificationSeverity.Fatal
            }

            fun getAction(index: Int): NotificationAction = when (index) {
                in 0..<25 -> createFakeNotificationAction(title = "fake action 1")
                in 25..<50 -> createFakeNotificationAction(title = "fake action 2")
                in 50..<75 -> createFakeNotificationAction(title = "fake action 3")
                else -> createFakeNotificationAction(title = "fake action 4")
            }

            val expectedSize = 99
            val notificationToDismiss = FakeInAppOnlyNotification(
                inAppNotificationStyles = inAppNotificationStyles { bannerInline() },
                actions = setOf(createFakeNotificationAction()),
            )
            val visualToDismiss = BannerInlineVisual.from(notificationToDismiss).first()
            val notifications = List(size = expectedSize) { index ->
                FakeInAppOnlyNotification(
                    title = "fake title $index",
                    contentText = "fake notification $index",
                    inAppNotificationStyles = inAppNotificationStyles { bannerInline() },
                    actions = setOf(getAction(index)),
                    severity = getSeverity(index),
                )
            } + notificationToDismiss
            val flags = DisplayInAppNotificationFlag.AllNotifications
            val testSubject = InAppNotificationHostStateHolder(enabled = flags)
            notifications.forEach { notification ->
                testSubject.showInAppNotification(notification)
            }

            // Act
            testSubject.dismiss(visualToDismiss)

            // Assert
            testSubject.currentInAppNotificationHostState.test {
                val state = awaitItem()
                assertThat(state)
                    .prop(InAppNotificationHostState::bannerInlineVisuals)
                    .all {
                        isNotEmpty()
                        hasSize(expectedSize)
                        given { visuals ->
                            assertThat(visuals.none { it == visualToDismiss }).isTrue()
                            visuals.forEachIndexed { index, visual ->
                                assertThat(visual).all {
                                    prop(BannerInlineVisual::title)
                                        .isEqualTo("fake title $index")
                                    prop(BannerInlineVisual::supportingText)
                                        .isEqualTo("fake notification $index")
                                    prop(BannerInlineVisual::severity)
                                        .isEqualTo(getSeverity(index))
                                    prop(BannerInlineVisual::actions).all {
                                        hasSize(1)
                                        containsExactly(getAction(index))
                                    }
                                }
                            }
                        }
                    }
            }
        }

    @Test
    fun `dismiss should remove snackbar notification given a SnackbarVisual`() = runTest {
        // Arrange
        val notification = FakeInAppOnlyNotification(
            inAppNotificationStyles = inAppNotificationStyles { snackbar(10.seconds) },
            actions = setOf(createFakeNotificationAction()),
        )
        val visual = requireNotNull(SnackbarVisual.from(notification))
        val flags = persistentSetOf(DisplayInAppNotificationFlag.SnackbarNotifications)
        val testSubject = InAppNotificationHostStateHolder(enabled = flags)
        testSubject.showInAppNotification(notification)

        // Act
        testSubject.dismiss(visual)

        // Assert
        testSubject.currentInAppNotificationHostState.test {
            val state = awaitItem()
            assertThat(state)
                .prop(InAppNotificationHostState::snackbarVisual)
                .isNull()
        }
    }
}
