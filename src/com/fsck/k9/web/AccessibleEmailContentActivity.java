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

import java.util.ArrayList;
import android.app.ListActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.widget.ArrayAdapter;

public class AccessibleEmailContentActivity extends ListActivity
{
    /**
     * Immutable empty String array
     */
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        String htmlSource = getIntent().getStringExtra("content");
        Spanned parsedHtml = Html.fromHtml(htmlSource, null, null);
        String[] rawListItems = parsedHtml.toString().split("\n");

        ArrayList<String> cleanedList = new ArrayList<String>();
        for (String rawListItem : rawListItems)
        {
            if (rawListItem.trim().length() > 0)
            {
                addToCleanedList(cleanedList, rawListItem);
            }
        }

        String[] listItems = cleanedList.toArray(EMPTY_STRING_ARRAY);

        setContentView(com.fsck.k9.R.layout.accessible_email_content);
        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listItems));
    }

    private void addToCleanedList(ArrayList<String> cleanedList, String line)
    {
        if (line.length() < 80)
        {
            cleanedList.add(line);
        }
        else
        {
            while (line.length() > 80)
            {
                int cutPoint = line.indexOf(" ", 80);
                if ((cutPoint > 0) && (cutPoint < line.length()))
                {
                    cleanedList.add(line.substring(0, cutPoint));
                    line = line.substring(cutPoint).trim();
                }
                else
                {
                    cleanedList.add(line);
                    line = "";
                }
            }
            if (line.length() > 0)
            {
                cleanedList.add(line);
            }
        }
    }

}
