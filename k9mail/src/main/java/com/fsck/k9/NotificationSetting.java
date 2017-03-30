package com.fsck.k9;

/**
 * Describes how a notification should behave.
 */
public class NotificationSetting {
    private boolean ringEnabled;
    private String ringtoneUri;

    private boolean ledEnabled;
    private int ledColor;

    private boolean vibrateEnabled;

    private int vibratePattern;
    private int vibrateTimes;

    /**
     * Set the ringtone kill switch. Allow to disable ringtone without losing
     * ringtone selection.
     *
     * @param ringEnabled
     *            <code>true</code> to allow ringtones, <code>false</code>
     *            otherwise.
     */
    public synchronized void setRingEnabled(boolean ringEnabled) {
        this.ringEnabled = ringEnabled;
    }

    /**
     * @return <code>true</code> if ringtone is allowed to play,
     *         <code>false</code> otherwise.
     */
    public synchronized boolean isRingEnabled() {
        return ringEnabled;
    }

    public synchronized String getRingtone() {
        return ringtoneUri;
    }

    public synchronized void setRingtone(String ringtoneUri) {
        this.ringtoneUri = ringtoneUri;
    }

    public synchronized boolean isLedEnabled() {
        return ledEnabled;
    }

    public synchronized void setLed(final boolean led) {
        ledEnabled = led;
    }

    public synchronized int getLedColor() {
        return ledColor;
    }

    public synchronized void setLedColor(int color) {
        ledColor = color;
    }

    public synchronized boolean isVibrateEnabled() {
        return vibrateEnabled;
    }

    public synchronized void setVibrate(boolean vibrate) {
        vibrateEnabled = vibrate;
    }

    public synchronized int getVibratePattern() {
        return vibratePattern;
    }

    public synchronized int getVibrateTimes() {
        return vibrateTimes;
    }

    public synchronized void setVibratePattern(int pattern) {
        vibratePattern = pattern;
    }

    public synchronized void setVibrateTimes(int times) {
        vibrateTimes = times;
    }



    /*
     * Fetch a vibration pattern.
     *
     * @param vibratePattern Vibration pattern index to use.
     * @param vibrateTimes Number of times to do the vibration pattern.
     * @return Pattern multiplied by the number of times requested.
     */

    public long[] getVibration() {
        return getVibration(vibratePattern, vibrateTimes);
    }

    public static long[] getVibration(int pattern, int times) {
        // These are "off, on" patterns, specified in milliseconds
        long[] pattern0 = new long[] {300, 200}; // like the default pattern
        long[] pattern1 = new long[] {100, 200};
        long[] pattern2 = new long[] {100, 500};
        long[] pattern3 = new long[] {200, 200};
        long[] pattern4 = new long[] {200, 500};
        long[] pattern5 = new long[] {500, 500};

        long[] selectedPattern = pattern0; //default pattern

        switch (pattern) {
        case 1:
            selectedPattern = pattern1;
            break;
        case 2:
            selectedPattern = pattern2;
            break;
        case 3:
            selectedPattern = pattern3;
            break;
        case 4:
            selectedPattern = pattern4;
            break;
        case 5:
            selectedPattern = pattern5;
            break;
        }

        long[] repeatedPattern = new long[selectedPattern.length * times];
        for (int n = 0; n < times; n++) {
            System.arraycopy(selectedPattern, 0, repeatedPattern, n * selectedPattern.length, selectedPattern.length);
        }
        // Do not wait before starting the vibration pattern.
        repeatedPattern[0] = 0;
        return repeatedPattern;
    }

}
