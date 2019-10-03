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
    private FilterableAdapter<T> mFolders;
    private List<T> mOriginalValues = null;

    public FolderListFilter(final FilterableAdapter<T> folders) {
        this.mFolders = folders;
    }

    @Override
    protected FilterResults performFiltering(CharSequence searchTerm) {
        FilterResults results = new FilterResults();

        if (mOriginalValues == null) {
            int count = mFolders.getCount();
            mOriginalValues = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                mOriginalValues.add(mFolders.getItem(i));
            }
        }

        Locale locale = Locale.getDefault();
        if ((searchTerm == null) || (searchTerm.length() == 0)) {
            List<T> list = new ArrayList<>(mOriginalValues);
            results.values = list;
            results.count = list.size();
        } else {
            final String searchTermString = searchTerm.toString().toLowerCase(locale);
            final String[] words = searchTermString.split(" ");
            final int wordCount = words.length;

            final List<T> values = mOriginalValues;

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
        mFolders.clear();
        if (folders != null) {
            for (T folder : folders) {
                if (folder != null) {
                    mFolders.add(folder);
                }
            }
        } else {
            Timber.w("FolderListFilter.publishResults - null search-result ");
        }

        mFolders.notifyDataSetChanged();
    }

    public interface FilterableAdapter<T> {
        void notifyDataSetChanged();
        void clear();
        void add(T object);
        int getCount();
        T getItem(int i);
    }
}
