package com.fsck.k9.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.widget.Filter;

/**
 * Filter to search for occurrences of the search-expression in any place of the folder name.
 */
public class FolderListFilter extends Filter {
    private final FolderAdapter adapter;
    private final List<FolderInfoHolder> folders;


    public FolderListFilter(FolderAdapter adapter, List<FolderInfoHolder> folders) {
        this.adapter = adapter;
        this.folders = folders;
    }

    @Override
    protected FilterResults performFiltering(CharSequence searchTerm) {
        FilterResults results = new FilterResults();

        Locale locale = Locale.getDefault();
        if (searchTerm == null || searchTerm.length() == 0) {
            List<FolderInfoHolder> list = new ArrayList<>(folders);
            results.values = list;
            results.count = list.size();
        } else {
            String searchTermString = searchTerm.toString().toLowerCase(locale);
            String[] words = searchTermString.split(" ");

            List<FolderInfoHolder> newValues = new ArrayList<>();
            for (FolderInfoHolder folderInfoHolder : folders) {
                String valueText = folderInfoHolder.displayName.toLowerCase(locale);

                for (String word : words) {
                    if (valueText.contains(word)) {
                        newValues.add(folderInfoHolder);
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
        List<FolderInfoHolder> folders = (List<FolderInfoHolder>) results.values;
        adapter.setFilteredFolders(constraint, folders);
    }


    public interface FolderAdapter {
        void setFilteredFolders(CharSequence filterText, List<FolderInfoHolder> folders);
    }
}
