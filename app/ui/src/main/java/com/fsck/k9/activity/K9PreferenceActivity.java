package com.fsck.k9.activity;


import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Lifecycle.State;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

public abstract class K9PreferenceActivity extends AppCompatPreferenceActivity implements LifecycleOwner {
    private final K9ActivityCommon base = new K9ActivityCommon(this, ThemeType.ACTION_BAR);

    private LifecycleRegistry lifecycleRegistry;

    @Override
    public void onCreate(Bundle icicle) {
        base.preOnCreate();
        super.onCreate(icicle);
        lifecycleRegistry = new LifecycleRegistry(this);
        lifecycleRegistry.markState(State.CREATED);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        lifecycleRegistry.markState(State.STARTED);
    }

    @Override
    protected void onResume() {
        base.preOnResume();
        super.onResume();
        lifecycleRegistry.markState(State.RESUMED);
    }

    @Override
    protected void onPause() {
        lifecycleRegistry.markState(State.STARTED);
        super.onPause();
    }

    @Override
    protected void onStop() {
        lifecycleRegistry.markState(State.CREATED);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        lifecycleRegistry.markState(State.DESTROYED);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // see https://developer.android.com/topic/libraries/architecture/lifecycle.html#onStop-and-savedState
        lifecycleRegistry.markState(State.CREATED);
        super.onSaveInstanceState(outState);
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return lifecycleRegistry;
    }

    /**
     * Set up the {@link ListPreference} instance identified by {@code key}.
     *
     * @param key
     *         The key of the {@link ListPreference} object.
     * @param value
     *         Initial value for the {@link ListPreference} object.
     *
     * @return The {@link ListPreference} instance identified by {@code key}.
     */
    protected ListPreference setupListPreference(final String key, final String value) {
        final ListPreference prefView = (ListPreference) findPreference(key);
        prefView.setValue(value);
        prefView.setSummary(prefView.getEntry());
        prefView.setOnPreferenceChangeListener(new PreferenceChangeListener(prefView));
        return prefView;
    }

    /**
     * Initialize a given {@link ListPreference} instance.
     *
     * @param prefView
     *         The {@link ListPreference} instance to initialize.
     * @param value
     *         Initial value for the {@link ListPreference} object.
     * @param entries
     *         Sets the human-readable entries to be shown in the list.
     * @param entryValues
     *         The array to find the value to save for a preference when an
     *         entry from entries is selected.
     */
    protected void initListPreference(final ListPreference prefView, final String value,
                                      final CharSequence[] entries, final CharSequence[] entryValues) {
        prefView.setEntries(entries);
        prefView.setEntryValues(entryValues);
        prefView.setValue(value);
        prefView.setSummary(prefView.getEntry());
        prefView.setOnPreferenceChangeListener(new PreferenceChangeListener(prefView));
    }

    /**
     * This class handles value changes of the {@link ListPreference} objects.
     */
    private static class PreferenceChangeListener implements Preference.OnPreferenceChangeListener {
        private ListPreference mPrefView;

        private PreferenceChangeListener(final ListPreference prefView) {
            this.mPrefView = prefView;
        }

        /**
         * Show the preference value in the preference summary field.
         */
        @Override
        public boolean onPreferenceChange(final Preference preference, final Object newValue) {
            final String summary = newValue.toString();
            final int index = mPrefView.findIndexOfValue(summary);
            mPrefView.setSummary(mPrefView.getEntries()[index]);
            mPrefView.setValue(summary);
            return false;
        }
    }
}
