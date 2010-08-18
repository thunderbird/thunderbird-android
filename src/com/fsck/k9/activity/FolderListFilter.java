package com.fsck.k9.activity;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import com.fsck.k9.K9;

/**
 * Filter to search for occurences of the search-expression in any place of the
 * folder-name instead of doing jsut a prefix-search.
 *
 * @author Marcus@Wolschon.biz
 */
public class FolderListFilter<T> extends Filter
{
    /**
     * ArrayAdapter that contains the list of folders displayed in the
     * ListView.
     * This object is modified by {@link #publishResults} to reflect the
     * changes due to the filtering performed by {@link #performFiltering}.
     * This in turn will change the folders displayed in the ListView.
     */
    private ArrayAdapter<T> mFolders;

    /**
     * All folders.
     */
    private ArrayList<T> mOriginalValues = null;

    /**
     * Create a filter for a list of folders.
     *
     * @param folderNames
     */
    public FolderListFilter(final ArrayAdapter<T> folderNames)
    {
        this.mFolders = folderNames;
    }

    /**
     * Do the actual search.
     * {@inheritDoc}
     *
     * @see #publishResults(CharSequence, FilterResults)
     */
    @Override
    protected FilterResults performFiltering(CharSequence searchTerm)
    {
        FilterResults results = new FilterResults();

        // Copy the values from mFolders to mOriginalValues if this is the
        // first time this method is called.
        if (mOriginalValues == null)
        {
            int count = mFolders.getCount();
            mOriginalValues = new ArrayList<T>(count);
            for (int i = 0; i < count; i++)
            {
                mOriginalValues.add(mFolders.getItem(i));
            }
        }

        if ((searchTerm == null) || (searchTerm.length() == 0))
        {
            ArrayList<T> list = new ArrayList<T>(mOriginalValues);
            results.values = list;
            results.count = list.size();
        }
        else
        {
            final String searchTermString = searchTerm.toString().toLowerCase();
            final String[] words = searchTermString.split(" ");
            final int wordCount = words.length;

            final ArrayList<T> values = mOriginalValues;
            final int count = values.size();

            final ArrayList<T> newValues = new ArrayList<T>();

            for (int i = 0; i < count; i++)
            {
                final T value = values.get(i);
                final String valueText = value.toString().toLowerCase();

                for (int k = 0; k < wordCount; k++)
                {
                    if (valueText.contains(words[k]))
                    {
                        newValues.add(value);
                        break;
                    }
                }
            }

            results.values = newValues;
            results.count = newValues.size();
        }

        return results;
    }

    /**
     * Publish the results to the user-interface.
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void publishResults(CharSequence constraint, FilterResults results)
    {
        // Don't notify for every change
        mFolders.setNotifyOnChange(false);

        //noinspection unchecked
        final List<T> folders = (List<T>) results.values;
        mFolders.clear();
        if (folders != null)
        {
            for (T folder : folders)
            {
                if (folder != null)
                {
                    mFolders.add(folder);
                }
            }
        }
        else
        {
            Log.w(K9.LOG_TAG, "FolderListFilter.publishResults - null search-result ");
        }

        // Send notification that the data set changed now
        mFolders.notifyDataSetChanged();
    }

    public void invalidate() {
        mOriginalValues = null;
    }
}
