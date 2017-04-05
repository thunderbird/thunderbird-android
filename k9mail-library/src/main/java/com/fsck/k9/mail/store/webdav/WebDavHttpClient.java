package com.fsck.k9.mail.store.webdav;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;
import timber.log.Timber;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * HTTP client for WebDAV communication
 */
public class WebDavHttpClient extends DefaultHttpClient {
    /*
     * Copyright (C) 2007 The Android Open Source Project
     *
     * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
     * compliance with the License. You may obtain a copy of the License at
     *
     * http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software distributed under the License is
     * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
     * the License for the specific language governing permissions and limitations under the License.
     */

    public static class WebDavHttpClientFactory {

        public WebDavHttpClient create() {
            return new WebDavHttpClient();
        }
    }

    public static void modifyRequestToAcceptGzipResponse(HttpRequest request) {
        Timber.i("Requesting gzipped data");
        request.addHeader("Accept-Encoding", "gzip");
    }

    public static InputStream getUngzippedContent(HttpEntity entity)
            throws IOException {
        InputStream responseStream = entity.getContent();
        if (responseStream == null)
            return null;
        Header header = entity.getContentEncoding();
        if (header == null)
            return responseStream;
        String contentEncoding = header.getValue();
        if (contentEncoding == null)
            return responseStream;
        if (contentEncoding.contains("gzip")) {
            Timber.i("Response is gzipped");
            responseStream = new GZIPInputStream(responseStream);
        }
        return responseStream;
    }

    public HttpResponse executeOverride(HttpUriRequest request, HttpContext context)
            throws IOException {
        modifyRequestToAcceptGzipResponse(request);
        return super.execute(request, context);
    }
}
