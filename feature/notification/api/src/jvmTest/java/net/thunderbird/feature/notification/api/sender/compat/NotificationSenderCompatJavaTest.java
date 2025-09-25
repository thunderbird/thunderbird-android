package net.thunderbird.feature.notification.api.sender.compat;


import java.util.ArrayList;
import java.util.List;

import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.test.TestCoroutineDispatchersKt;
import kotlinx.coroutines.test.TestDispatcher;
import kotlinx.coroutines.test.TestDispatchers;
import net.thunderbird.core.outcome.Outcome;
import net.thunderbird.feature.notification.api.command.NotificationCommandException;
import net.thunderbird.feature.notification.api.command.outcome.CommandExecutionFailed;
import net.thunderbird.feature.notification.api.command.outcome.Failure;
import net.thunderbird.feature.notification.api.command.outcome.NotificationCommandOutcome;
import net.thunderbird.feature.notification.api.command.outcome.Success;
import net.thunderbird.feature.notification.api.content.Notification;
import net.thunderbird.feature.notification.testing.fake.FakeNotification;
import net.thunderbird.feature.notification.testing.fake.command.FakeInAppNotificationCommand;
import net.thunderbird.feature.notification.testing.fake.command.FakeSystemNotificationCommand;
import net.thunderbird.feature.notification.testing.fake.sender.FakeNotificationSender;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


public class NotificationSenderCompatJavaTest {
    private TestDispatcher testDispatcher;

    @Before
    public void setUp() {
        testDispatcher = TestCoroutineDispatchersKt.UnconfinedTestDispatcher(null, null);
        TestDispatchers.setMain(Dispatchers.INSTANCE, testDispatcher);
    }

    @After
    public void tearDown() {
        // restore original Main
        TestDispatchers.resetMain(Dispatchers.INSTANCE);
    }

    @Test
    public void send_shouldCallListenerCallback_wheneverAResultIsReceived() {
        // Arrange
        @SuppressWarnings("unchecked") final List<
            Outcome<
                ? extends @NotNull Success<? extends @NotNull Notification>,
                ? extends @NotNull Failure<? extends @NotNull Notification>
                >
            > expectedResults = List.of(
            Outcome.Companion.success(NotificationCommandOutcome.Success(1, new FakeInAppNotificationCommand())),
            Outcome.Companion.success(NotificationCommandOutcome.Success(2, new FakeSystemNotificationCommand())),
            Outcome.Companion.failure(
                new CommandExecutionFailed<>(
                    new FakeSystemNotificationCommand(),
                    "What an issue?",
                    new NotificationCommandException("What an issue?")
                )
            )
        );


        final FakeNotificationSender sender = new FakeNotificationSender(expectedResults);

        final ResultListener listener = new ResultListener();
        final NotificationSenderCompat.OnResultListener spyListener = spy(listener);

        final NotificationSenderCompat testSubject = new NotificationSenderCompat(sender, testDispatcher);

        // Act
        testSubject.send(new FakeNotification(), spyListener);

        // Assert
        verify(spyListener, times(expectedResults.size())).onResult(any());
        assertEquals(expectedResults, listener.actualResults);
    }

    private static class ResultListener implements NotificationSenderCompat.OnResultListener {
        final ArrayList<
            Outcome<
                ? extends @NotNull Success<? extends @NotNull Notification>,
                ? extends @NotNull Failure<? extends @NotNull Notification>
                >
            > actualResults = new ArrayList<>();

        @Override
        public void onResult(
            @NotNull Outcome<? extends @NotNull Success<? extends @NotNull Notification>, ? extends @NotNull Failure<? extends @NotNull Notification>> outcome) {
            actualResults.add(outcome);
        }
    }
}
