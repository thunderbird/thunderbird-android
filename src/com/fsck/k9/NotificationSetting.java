package com.fsck.k9;

/**
 * Describes how a notification should behave.
 */
public class NotificationSetting
{

    /**
     * Ring notification kill switch. Allow disabling ringtones without losing
     * ringtone selection.
     */
    private boolean mRing;

    private String mRingtoneUri;

    /**
     * LED kill switch.
     */
    private boolean mLed;

    private int mLedColor;

    /**
     * Vibration kill switch.
     */
    private boolean mVibrate;

    private int mVibratePattern;

    private int mVibrateTimes;

    /**
     * Set the ringtone kill switch. Allow to disable ringtone without losing
     * ringtone selection.
     *
     * @param ring
     *            <code>true</code> to allow ringtones, <code>false</code>
     *            otherwise.
     */
    public synchronized void setRing(boolean ring)
    {
        mRing = ring;
    }

    /**
     * @return <code>true</code> if ringtone is allowed to play,
     *         <code>false</code> otherwise.
     */
    public synchronized boolean shouldRing()
    {
        return mRing;
    }

    public synchronized String getRingtone()
    {
        return mRingtoneUri;
    }

    public synchronized void setRingtone(String ringtoneUri)
    {
        mRingtoneUri = ringtoneUri;
    }

    public synchronized boolean isLed()
    {
        return mLed;
    }

    public synchronized void setLed(final boolean led)
    {
        mLed = led;
    }

    public synchronized int getLedColor()
    {
        return mLedColor;
    }

    public synchronized void setLedColor(int color)
    {
        mLedColor = color;
    }

    public synchronized boolean shouldVibrate()
    {
        return mVibrate;
    }

    public synchronized void setVibrate(boolean vibrate)
    {
        mVibrate = vibrate;
    }

    public synchronized int getVibratePattern()
    {
        return mVibratePattern;
    }

    public synchronized int getVibrateTimes()
    {
        return mVibrateTimes;
    }

    public synchronized void setVibratePattern(int pattern)
    {
        mVibratePattern = pattern;
    }

    public synchronized void setVibrateTimes(int times)
    {
        mVibrateTimes = times;
    }

}
