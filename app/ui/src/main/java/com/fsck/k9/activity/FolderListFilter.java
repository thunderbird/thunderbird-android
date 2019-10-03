package com.fsck.k9.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;
import android.widget.Filter;

/**
 * Filter to search for occurrences of the search-expression in any place of the folder name.
 */
public class FolderListFilter<T> extends Filter {
    private FilterableAdapter<T> adapter;
    private List<T> unfilteredFolderList = null;

    public FolderListFilter(final FilterableAdapter<T> folders) {
        this.adapter = folders;
    }

    @Override
    protected FilterResults performFiltering(CharSequence searchTerm) {
        FilterResults results = new FilterResults();

        if (unfilteredFolderList == null) {
            int count = adapter.getCount();
            unfilteredFolderList = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                unfilteredFolderList.add(adapter.getItem(i));
            }
        }

        Locale locale = Locale.getDefault();
        if ((searchTerm == null) || (searchTerm.length() == 0)) {
            List<T> list = new ArrayList<>(unfilteredFolderList);
            results.values = list;
            results.count = list.size();
        } else {
            final String searchTermString = searchTerm.toString().toLowerCase(locale);
            final String[] words = searchTermString.split(" ");
            final int wordCount = words.length;

            final List<T> values = unfilteredFolderList;

            final List<T> newValues = new ArrayList<>();

            for (final T value : values) {
                final String valueText = value.toString().toLowerCase(locale);

                for (int k = 0; k < wordCount; k++) {
                    if (valueText.contains(words[k])) {
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

    @SuppressWarnings("unchecked")
    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
        final List<T> folders = (List<T>) results.values;
        adapter.clear();
        if (folders != null) {
            for (T folder : folders) {
                if (folder != null) {
                    adapter.add(folder);
                }
            }
        } else {
            Timber.w("FolderListFilter.publishResults - null search-result ");
        }

        adapter.notifyDataSetChanged();
    }

    public interface FilterableAdapter<T> {
        void notifyDataSetChanged();
        void clear();
        void add(T object);
        int getCount();
        T getItem(int i);
    }
}
