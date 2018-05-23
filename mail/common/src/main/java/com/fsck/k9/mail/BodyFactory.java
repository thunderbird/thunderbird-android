package com.fsck.k9.mail;


import java.io.IOException;
import java.io.InputStream;


public interface BodyFactory {
    Body createBody(String contentTransferEncoding, String contentType, InputStream inputStream) throws IOException;
}
