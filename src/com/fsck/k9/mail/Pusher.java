package com.fsck.k9.mail;

import java.util.List;


public interface Pusher {
    public void start(List<String> folderNames);
    public void refresh();
    public void stop();
    /**
     *
     * @return milliseconds of required refresh interval
     */
    public int getRefreshInterval();
    public void setLastRefresh(long lastRefresh);
    public long getLastRefresh();
}
