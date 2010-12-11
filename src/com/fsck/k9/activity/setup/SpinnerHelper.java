package com.fsck.k9.activity.setup;
/*
 * SpinnerHelper donated to K-9 Mail by Boutique Software
 */

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class SpinnerHelper
{
    public static void initSpinner(Context context, Spinner spinner, int entryRes, int valueRes, String curVal)
    {
        String[] entryArray = context.getResources().getStringArray(entryRes);
        String[] valueArray = context.getResources().getStringArray(valueRes);
        initSpinner(context, spinner, entryArray, valueArray, curVal);
    }
    public static void initSpinner(Context context, Spinner spinner, String[] entryArray, String[] valueArray, String curVal)
    {

        if (entryArray.length != valueArray.length)
        {
            throw new RuntimeException("Entry and value arrays are of unequal lenght");
        }

        EntryValue[] entryValues = new EntryValue[entryArray.length];
        int curSelection = 0;
        for (int i = 0; i < entryArray.length; i++)
        {
            entryValues[i] = new EntryValue(entryArray[i], valueArray[i]);
            if (valueArray[i].equals(curVal))
            {
                curSelection = i;
            }
        }

        ArrayAdapter<EntryValue> entryValuesAdapter = new ArrayAdapter<EntryValue>(context, android.R.layout.simple_spinner_item, entryValues);
        entryValuesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(entryValuesAdapter);
        spinner.setSelection(curSelection);
    }

    public static String getSpinnerValue(Spinner spinner)
    {
        EntryValue entryValue = (EntryValue)spinner.getSelectedItem();
        if (entryValue != null)
        {
            return entryValue.getValue();
        }
        else
        {
            return null;
        }
    }
    public static String getSpinnerEntry(Spinner spinner)
    {
        EntryValue entryValue = (EntryValue)spinner.getSelectedItem();
        if (entryValue != null)
        {
            return entryValue.getEntry();
        }
        else
        {
            return null;
        }
    }
    private static class EntryValue
    {
        final String entry;
        final String value;
        EntryValue(String entry, String value)
        {
            this.entry = entry;
            this.value = value;
        }
        @Override
        public String toString()
        {
            return entry;
        }
        public String getEntry()
        {
            return entry;
        }
        public String getValue()
        {
            return value;
        }
    }
}
