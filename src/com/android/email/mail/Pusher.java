package com.android.email.mail;

import java.util.List;


public interface Pusher
{
    public void start(List<String> folderNames);
    public void refresh();
    public void stop();
    /**
     *
     * @return milliseconds of required refresh interval
     */
    public int getRefreshInterval();
}
