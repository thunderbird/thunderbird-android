package com.fsck.k9.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;
import android.widget.ArrayAdapter;
import android.widget.Filter;


/**
 * Filter to search for occurrences of the search-expression in any place of the
 * folder-name instead of doing just a prefix-search.
 *
 * @author Marcus@Wolschon.biz
 */
public class FolderListFilter<T> extends Filter {
    /**
     * ArrayAdapter that contains the list of folders displayed in the
     * ListView.
     * This object is modified by {@link #publishResults} to reflect the
     * changes due to the filtering performed by {@link #performFiltering}.
     * This in turn will change the folders displayed in the ListView.
     */
    private ArrayAdapter<T> folders;

    /**
     * All folders.
     */
    private List<T> originalValues = null;

    /**
     * Create a filter for a list of folders.
     *
     * @param folderNames
     */
    public FolderListFilter(final ArrayAdapter<T> folderNames) {
        this.folders = folderNames;
    }

    /**
     * Do the actual search.
     * {@inheritDoc}
     *
     * @see #publishResults(CharSequence, FilterResults)
     */
    @Override
    protected FilterResults performFiltering(CharSequence searchTerm) {
        FilterResults results = new FilterResults();

        // Copy the values from folders to originalValues if this is the
        // first time this method is called.
        if (originalValues == null) {
            int count = folders.getCount();
            originalValues = new ArrayList<T>(count);
            for (int i = 0; i < count; i++) {
                originalValues.add(folders.getItem(i));
            }
        }

        Locale locale = Locale.getDefault();
        if ((searchTerm == null) || (searchTerm.length() == 0)) {
            List<T> list = new ArrayList<T>(originalValues);
            results.values = list;
            results.count = list.size();
        } else {
            final String searchTermString = searchTerm.toString().toLowerCase(locale);
            final String[] words = searchTermString.split(" ");

            final List<T> values = originalValues;

            final List<T> newValues = new ArrayList<T>();

            for (final T value : values) {
                final String valueText = value.toString().toLowerCase(locale);

                for (String word : words) {
                    if (valueText.contains(word)) {
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
    protected void publishResults(CharSequence constraint, FilterResults results) {
        // Don't notify for every change
        folders.setNotifyOnChange(false);
        try {

            //noinspection unchecked
            final List<T> folders = (List<T>) results.values;
            this.folders.clear();
            if (folders != null) {
                for (T folder : folders) {
                    if (folder != null) {
                        this.folders.add(folder);
                    }
                }
            } else {
                Timber.w("FolderListFilter.publishResults - null search-result ");
            }

            // Send notification that the data set changed now
            this.folders.notifyDataSetChanged();
        } finally {
            // restore notification status
            folders.setNotifyOnChange(true);
        }
    }

    public void invalidate() {
        originalValues = null;
    }
}
