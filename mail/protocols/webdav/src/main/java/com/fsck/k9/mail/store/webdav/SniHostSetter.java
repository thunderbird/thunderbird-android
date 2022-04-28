package com.fsck.k9.mail.store.webdav;


import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;


public interface SniHostSetter {
    void setSniHost(SSLSocketFactory factory, SSLSocket socket, String hostname);
}
