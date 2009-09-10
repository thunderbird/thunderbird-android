package com.android.email.mail;


public interface Pusher
{
    public void start();
    public void refresh();
    public void stop();
    /**
     * 
     * @return milliseconds of required refresh interval
     */
    public int getRefreshInterval();
}
