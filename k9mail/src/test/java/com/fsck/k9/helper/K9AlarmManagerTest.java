package com.fsck.k9.helper;


import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build.VERSION_CODES;
import android.os.PowerManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class K9AlarmManagerTest {
    private static final String PACKAGE_NAME = "org.example.package";
    private static final int TIMER_TYPE = AlarmManager.RTC_WAKEUP;
    private static final long TIMEOUT = 15L * 60L * 1000L;
    private static final PendingIntent PENDING_INTENT = createDummyPendingIntent();


    @Mock
    private AlarmManager systemAlarmManager;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void set_withoutDozeSupport_shouldCallSetOnAlarmManager() throws Exception {
        K9AlarmManager alarmManager = createK9AlarmManagerWithoutDozeSupport();

        alarmManager.set(TIMER_TYPE, TIMEOUT, PENDING_INTENT);

        verify(systemAlarmManager).set(TIMER_TYPE, TIMEOUT, PENDING_INTENT);
    }

    @Test
    public void set_withDozeSupportAndNotWhiteListed_shouldCallSetOnAlarmManager() throws Exception {
        K9AlarmManager alarmManager = createK9AlarmManagerWithDozeSupport(false);

        alarmManager.set(TIMER_TYPE, TIMEOUT, PENDING_INTENT);

        verify(systemAlarmManager).set(TIMER_TYPE, TIMEOUT, PENDING_INTENT);
    }

    @TargetApi(VERSION_CODES.M)
    @Test
    public void set_withDozeSupportAndWhiteListed_shouldCallSetAndAllowWhileIdleOnAlarmManager() throws Exception {
        K9AlarmManager alarmManager = createK9AlarmManagerWithDozeSupport(true);

        alarmManager.set(TIMER_TYPE, TIMEOUT, PENDING_INTENT);

        verify(systemAlarmManager).setAndAllowWhileIdle(TIMER_TYPE, TIMEOUT, PENDING_INTENT);
    }

    @TargetApi(VERSION_CODES.M)
    @Test
    public void cancel_shouldCallCancelOnAlarmManager() throws Exception {
        K9AlarmManager alarmManager = createK9AlarmManagerWithDozeSupport(true);

        alarmManager.cancel(PENDING_INTENT);

        verify(systemAlarmManager).cancel(PENDING_INTENT);
    }


    private K9AlarmManager createK9AlarmManagerWithDozeSupport(boolean whiteListed) {
        PowerManager powerManager = createPowerManager(whiteListed);
        Context context = createContext(powerManager);

        return new TestK9AlarmManager(context, true);
    }

    private K9AlarmManager createK9AlarmManagerWithoutDozeSupport() {
        PowerManager powerManager = mock(PowerManager.class);
        Context context = createContext(powerManager);

        return new TestK9AlarmManager(context, false);
    }

    @TargetApi(VERSION_CODES.M)
    private PowerManager createPowerManager(boolean whiteListed) {
        PowerManager powerManager = mock(PowerManager.class);
        when(powerManager.isIgnoringBatteryOptimizations(PACKAGE_NAME)).thenReturn(whiteListed);

        return powerManager;
    }

    private Context createContext(PowerManager powerManager) {
        Context context = mock(Context.class);
        when(context.getPackageName()).thenReturn(PACKAGE_NAME);
        when(context.getSystemService(Context.ALARM_SERVICE)).thenReturn(systemAlarmManager);
        when(context.getSystemService(Context.POWER_SERVICE)).thenReturn(powerManager);

        return context;
    }

    private static PendingIntent createDummyPendingIntent() {
        return mock(PendingIntent.class);
    }


    class TestK9AlarmManager extends K9AlarmManager {
        private final boolean dozeSupported;


        TestK9AlarmManager(Context context, boolean dozeSupported) {
            super(context);
            this.dozeSupported = dozeSupported;
        }

        @Override
        protected boolean isDozeSupported() {
            return dozeSupported;
        }
    }
}
