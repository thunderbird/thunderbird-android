package com.fsck.k9.mail;


public class ProxySettings {
    public final boolean enabled;
    public final String host;
    public final int port;
    public ProxySettings(boolean enabled, String host, int port) {
        this.enabled = enabled;
        this.host = host;
        this.port = port;
    }
}
