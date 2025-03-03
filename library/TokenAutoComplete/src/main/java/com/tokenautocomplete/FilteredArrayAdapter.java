package com.tokenautocomplete;

import android.content.Context;
import androidx.annotation.NonNull;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Simplified custom filtered ArrayAdapter
 * override keepObject with your test for filtering
 * <p>
 * Based on gist <a href="https://gist.github.com/tobiasschuerg/3554252/raw/30634bf9341311ac6ad6739ef094222fc5f07fa8/FilteredArrayAdapter.java">
 * FilteredArrayAdapter</a> by Tobias Schürg
 * <p>
 * Created on 9/17/13.
 * @author mgod
 */

abstract public class FilteredArrayAdapter<T> extends ArrayAdapter<T> {

    private List<T> originalObjects;
    private Filter filter;

    /**
     * Constructor
     *
     * @param context The current context.
     * @param resource The resource ID for a layout file containing a TextView to use when
     *                 instantiating views.
     * @param objects The objects to represent in the ListView.
     */
    public FilteredArrayAdapter(Context context, int resource, T[] objects) {
        this(context, resource, 0, objects);
    }

    /**
     * Constructor
     *
     * @param context The current context.
     * @param resource The resource ID for a layout file containing a layout to use when
     *                 instantiating views.
     * @param textViewResourceId The id of the TextView within the layout resource to be populated
     * @param objects The objects to represent in the ListView.
     */
    @SuppressWarnings("WeakerAccess")
    public FilteredArrayAdapter(Context context, int resource, int textViewResourceId, T[] objects) {
        this(context, resource, textViewResourceId, new ArrayList<>(Arrays.asList(objects)));
    }

    /**
     * Constructor
     *
     * @param context The current context.
     * @param resource The resource ID for a layout file containing a TextView to use when
     *                 instantiating views.
     * @param objects The objects to represent in the ListView.
     */
    @SuppressWarnings("unused")
    public FilteredArrayAdapter(Context context, int resource, List<T> objects) {
        this(context, resource, 0, objects);
    }

    /**
     * Constructor
     *
     * @param context The current context.
     * @param resource The resource ID for a layout file containing a layout to use when
     *                 instantiating views.
     * @param textViewResourceId The id of the TextView within the layout resource to be populated
     * @param objects The objects to represent in the ListView.
     */
    @SuppressWarnings("WeakerAccess")
    public FilteredArrayAdapter(Context context, int resource, int textViewResourceId, List<T> objects) {
        super(context, resource, textViewResourceId, new ArrayList<>(objects));
        this.originalObjects = objects;
    }

    @NonNull 
    @Override
    public Filter getFilter() {
        if (filter == null)
            filter = new AppFilter();
        return filter;
    }

    /**
     * Filter method used by the adapter. Return true if the object should remain in the list
     *
     * @param obj object we are checking for inclusion in the adapter
     * @param mask current text in the edit text we are completing against
     * @return true if we should keep the item in the adapter
     */
    abstract protected boolean keepObject(T obj, String mask);

    /**
     * Class for filtering Adapter, relies on keepObject in FilteredArrayAdapter
     *
     * based on gist by Tobias Schürg
     * in turn inspired by inspired by Alxandr
     *         (http://stackoverflow.com/a/2726348/570168)
     */
    private class AppFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence chars) {
            ArrayList<T> sourceObjects = new ArrayList<>(originalObjects);

            FilterResults result = new FilterResults();
            if (chars != null && chars.length() > 0) {
                String mask = chars.toString();
                ArrayList<T> keptObjects = new ArrayList<>();

                for (T object : sourceObjects) {
                    if (keepObject(object, mask))
                        keptObjects.add(object);
                }
                result.count = keptObjects.size();
                result.values = keptObjects;
            } else {
                // add all objects
                result.values = sourceObjects;
                result.count = sourceObjects.size();
            }
            return result;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            clear();
            if (results.count > 0) {
                FilteredArrayAdapter.this.addAll((Collection)results.values);
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }
}
