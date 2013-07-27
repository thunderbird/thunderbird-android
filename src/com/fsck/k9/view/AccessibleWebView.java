/*
 * Copyright (C) 2010 The IDEAL Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fsck.k9.view;

import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import com.fsck.k9.activity.AccessibleEmailContentActivity;
import com.fsck.k9.controller.MessagingListener;

import java.util.Set;

public class AccessibleWebView extends TextView {
    private Context mContext;
    private String mHtmlSource;
    private WebView mDummyWebView;
    private Set<MessagingListener> mListeners = null;

    public AccessibleWebView(Context context) {
        super(context);
        init(context);
    }

    public AccessibleWebView(Context context, AttributeSet attributes) {
        super(context, attributes);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mDummyWebView = new WebView(context);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                diveIn();
            }
        });
    }

    public void loadData(String data, String mimeType, String encoding) {
        mHtmlSource = data;
        this.setText(Html.fromHtml(mHtmlSource, null, null));
    }

    public WebSettings getSettings() {
        return mDummyWebView.getSettings();
    }

    public void setText(String text) {
        this.setText(Html.fromHtml(text, null, null));

        // Let everyone know that loading has finished.
        if (mListeners != null) {
            for (MessagingListener l : mListeners) {
                l.messageViewFinished();
            }
        }
    }

    public boolean zoomIn() {
        if (getTextSize() < 100) {
            setTextSize(getTextSize() + 5);
            return true;
        }
        return false;
    }

    public boolean zoomOut() {
        if (getTextSize() > 5) {
            setTextSize(getTextSize() - 5);
            return true;
        }
        return false;
    }

    private void diveIn() {
        Intent i = new Intent();
        i.setClass(mContext, AccessibleEmailContentActivity.class);
        i.putExtra("content", mHtmlSource);
        mContext.startActivity(i);
    }

    public void setListeners(final Set<MessagingListener> listeners) {
        this.mListeners = listeners;
    }
}
