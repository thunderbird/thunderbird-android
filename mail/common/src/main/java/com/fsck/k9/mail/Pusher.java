package com.fsck.k9.mail;

import java.util.List;


public interface Pusher {
    void start(List<String> folderServerIds);
    void refresh();
    void stop();
    /**
     *
     * @return milliseconds of required refresh interval
     */
    int getRefreshInterval();
    void setLastRefresh(long lastRefresh);
    long getLastRefresh();
}
