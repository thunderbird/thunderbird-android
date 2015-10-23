package com.fsck.k9.mail;

public class ProxySettings {
    public boolean enabled;
    public String host;
    public int port;

    public ProxySettings(boolean enabled, String host, int port) {
        this.enabled = enabled;
        this.host = host;
        this.port = port;
    }
}
