package com.fsck.k9.helper;


import android.app.AlarmManager;
import android.app.PendingIntent;

import com.fsck.k9.power.DozeChecker;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class K9AlarmManagerTest {
    private static final int TIMER_TYPE = AlarmManager.RTC_WAKEUP;
    private static final long TIMEOUT = 15L * 60L * 1000L;
    private static final PendingIntent PENDING_INTENT = createDummyPendingIntent();


    @Mock
    private AlarmManager systemAlarmManager;
    @Mock
    private DozeChecker dozeChecker;

    private K9AlarmManager alarmManager;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        alarmManager = new K9AlarmManager(systemAlarmManager, dozeChecker);
    }

    @Test
    public void set_withoutDozeSupport_shouldCallSetOnAlarmManager() throws Exception {
        configureDozeSupport(false);

        alarmManager.set(TIMER_TYPE, TIMEOUT, PENDING_INTENT);

        verify(systemAlarmManager).set(TIMER_TYPE, TIMEOUT, PENDING_INTENT);
    }

    @Test
    public void set_withDozeSupportAndNotWhiteListed_shouldCallSetOnAlarmManager() throws Exception {
        configureDozeSupport(true);

        alarmManager.set(TIMER_TYPE, TIMEOUT, PENDING_INTENT);

        verify(systemAlarmManager).set(TIMER_TYPE, TIMEOUT, PENDING_INTENT);
    }

    @Test
    public void set_withDozeSupportAndWhiteListed_shouldCallSetAndAllowWhileIdleOnAlarmManager() throws Exception {
        configureDozeSupport(true);
        addAppToBatteryOptimizationWhitelist();

        alarmManager.set(TIMER_TYPE, TIMEOUT, PENDING_INTENT);

        verify(systemAlarmManager).setAndAllowWhileIdle(TIMER_TYPE, TIMEOUT, PENDING_INTENT);
    }

    @Test
    public void cancel_shouldCallCancelOnAlarmManager() throws Exception {
        configureDozeSupport(true);
        addAppToBatteryOptimizationWhitelist();

        alarmManager.cancel(PENDING_INTENT);

        verify(systemAlarmManager).cancel(PENDING_INTENT);
    }


    private void configureDozeSupport(boolean supported) {
        when(dozeChecker.isDeviceIdleModeSupported()).thenReturn(supported);
    }

    private void addAppToBatteryOptimizationWhitelist() {
        when(dozeChecker.isAppWhitelisted()).thenReturn(true);
    }

    private static PendingIntent createDummyPendingIntent() {
        return mock(PendingIntent.class);
    }
}
