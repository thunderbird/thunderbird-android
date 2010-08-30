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

package com.fsck.k9.web;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

public class AccessibleWebView extends TextView
{
    private Activity parent;

    private String htmlSource;

    private WebView dummyWebView;

    public AccessibleWebView(Context context)
    {
        super(context);
        parent = (Activity) context;
        dummyWebView = new WebView(context);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View arg0)
            {
                diveIn();
            }
        });
    }

    public AccessibleWebView(Context context, AttributeSet attributes)
    {
        super(context, attributes);
        parent = (Activity) context;
        dummyWebView = new WebView(context);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View arg0)
            {
                diveIn();
            }
        });

    }

    public void loadData(String data, String mimeType, String encoding)
    {
        htmlSource = data;
        this.setText(Html.fromHtml(htmlSource, null, null));
    }

    public WebSettings getSettings()
    {
        return dummyWebView.getSettings();
    }

    public void setVerticalScrollbarOverlay(boolean booleanValue)
    {
        // Do nothing here; dummy stub method to maintain compatibility with
        // standard WebView.
    }

    public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding,
                                    String historyUrl)
    {
        htmlSource = data;
        this.setText(Html.fromHtml(htmlSource, null, null));
    }

    public void loadUrl(String url)
    {
        // Do nothing here; dummy stub method to maintain compatibility with
        // standard WebView.
    }

    public boolean zoomIn()
    {
        if (getTextSize() < 100)
        {
            setTextSize(getTextSize() + 5);
            return true;
        }
        return false;
    }

    public boolean zoomOut()
    {
        if (getTextSize() > 5)
        {
            setTextSize(getTextSize() - 5);
            return true;
        }
        return false;
    }

    private void diveIn()
    {
        Intent i = new Intent();
        i.setClass(parent, AccessibleEmailContentActivity.class);
        i.putExtra("content", htmlSource);
        parent.startActivity(i);
    }
}
