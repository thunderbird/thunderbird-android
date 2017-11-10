package com.fsck.k9.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.TypedValue;

import com.fsck.k9.activity.ColorPickerDialog;
import com.fsck.k9.fragment.KeywordAddDialogFragment;
import com.fsck.k9.fragment.KeywordEditNameDialogFragment;
import com.fsck.k9.helper.KeywordColorUtils;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.mail.FlagManager;
import com.fsck.k9.mail.Keyword;


public class KeywordEditor extends K9ListActivity implements
    KeywordEditNameDialogFragment.KeywordEditNameDialogListener,
    KeywordAddDialogFragment.KeywordAddDialogFragmentDialogListener
{
    private static final KeywordColorUtils keywordColorUtils =
        new KeywordColorUtils();

    private KeywordEditorListAdapter adapter;
    private int colorKeywordNotVisible;
    final private FlagManager flagManager = FlagManager.getFlagManager();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.keyword_editor_list);
        ListView listView = getListView();
        listView.setLongClickable(true);
        registerForContextMenu(listView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.keyword_editor_option, menu);
        return true;
    }

    private void initializeActivityView() {

        final ContextThemeWrapper themeContext = new ContextThemeWrapper(
            this, K9.getK9ThemeResourceId(K9.getK9ComposerTheme()));
        TypedValue outValue = new TypedValue();
        themeContext.getTheme().resolveAttribute(
            R.attr.colorKeywordNotVisible, outValue, true);
        colorKeywordNotVisible = outValue.data;

        ArrayList<Keyword> keywords = flagManager.getKeywords();
        adapter = new KeywordEditorListAdapter(this, keywords);
        setListAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();

        Preferences.getPreferences(this).loadKeywords();
        Preferences.getPreferences(this).saveKeywords();

        if (adapter == null) {
            initializeActivityView();
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        super.onBackPressed();
    }

    private void saveChangedKeyword(Keyword k) {
        Preferences.getPreferences(this).saveKeyword(k);
    }

    private void saveChangedKeywordOrder() {
        Preferences.getPreferences(this).saveKeywordOrder();
    }

    private void saveDeletedKeyword() {
        Preferences.getPreferences(this).saveKeywords();
    }

    public class KeywordEditorListAdapter extends ArrayAdapter<Keyword> {

        KeywordEditorListAdapter(Context context, List<Keyword> keywords) {
            super(context, 0 /* resource not used */, keywords);
        }

        @Override
        public View getView(
            final int position, View convertView, ViewGroup parent)
        {
            if (convertView == null) {
                LayoutInflater inflater = getLayoutInflater();
                convertView = inflater.inflate(
                    R.layout.keyword_editor_list_item, null);
            }
            final Keyword keyword = getItem(position);
            if (keyword != null) {
                ItemViewHolder holder = getHolder(convertView);
                assignKeyword(holder, position, keyword);
            }
            return convertView;
        }

        private ItemViewHolder getHolder(View view) {
            ItemViewHolder holder = (ItemViewHolder) view.getTag();
            if (holder == null) {
                holder = new ItemViewHolder();

                holder.cbVisibility = (CheckBox) view.findViewById(
                    R.id.checkbox_keyword_visibility);
                holder.cbVisibility.setOnCheckedChangeListener(
                    new OnVisibilityChangeListener());
                holder.cbVisibility.setLongClickable(true);

                holder.llTexts = (LinearLayout) view.findViewById(
                    R.id.keyword_name_and_external_code);
                holder.llTexts.setLongClickable(true);

                holder.tvName = (TextView) view.findViewById(R.id.keyword_name);

                holder.tvKeyword = (TextView) view.findViewById(
                    R.id.keyword_external_code);

                holder.btnColorDialog = (ImageButton) view.findViewById(
                    R.id.button_keyword_color_dialog);
                holder.btnColorDialog.setLongClickable(true);

                holder.btnUp = (ImageButton) view.findViewById(
                    R.id.button_keyword_up);
                holder.btnUp.setLongClickable(true);

                holder.btnDown = (ImageButton) view.findViewById(
                    R.id.button_keyword_down);
                holder.btnDown.setLongClickable(true);

                view.setTag(holder);
            }
            return holder;
        }

        private void assignKeyword(
            ItemViewHolder holder, final int position, final Keyword keyword)
        {
            holder.cbVisibility.setTag(position);
            holder.cbVisibility.setChecked(keyword.isVisible());

            holder.llTexts.setTag(position);
            if (keyword.isVisible()) {
                holder.llTexts.setOnClickListener(new OnEditNameListener());
            } else {
                holder.llTexts.setOnClickListener(null);
            }

            holder.tvName.setText(keyword.getName());
            if (keyword.isVisible()) {
                holder.tvName.setTextColor(
                    keyword.getTextColor(keywordColorUtils));
            } else {
                holder.tvName.setTextColor(colorKeywordNotVisible);
            }

            holder.tvKeyword.setText(keyword.getExternalCode());

            holder.btnColorDialog.setTag(position);
            if (keyword.isVisible()) {
                holder.btnColorDialog.setColorFilter(
                    keyword.getTextColor(keywordColorUtils));
                holder.btnColorDialog.setOnClickListener(
                    new OnColorDialogListener());
            } else {
                holder.btnColorDialog.setColorFilter(colorKeywordNotVisible);
                holder.btnColorDialog.setOnClickListener(null);
            }

            holder.btnUp.setTag(position);
            if (position >= 1) {
                holder.btnUp.setVisibility(View.VISIBLE);
                holder.btnUp.setOnClickListener(new OnMoveUpListener());
            } else {
                holder.btnUp.setVisibility(View.INVISIBLE);
                holder.btnUp.setOnClickListener(null);
            }

            holder.btnDown.setTag(position);
            if (position < getCount() - 1) {
                holder.btnDown.setVisibility(View.VISIBLE);
                holder.btnDown.setOnClickListener(new OnMoveDownListener());
            } else {
                holder.btnDown.setVisibility(View.INVISIBLE);
                holder.btnDown.setOnClickListener(null);
            }
        }

        private class OnVisibilityChangeListener
            implements CompoundButton.OnCheckedChangeListener
        {
            @Override
            public void onCheckedChanged(CompoundButton view, boolean isChecked)
            {
                final int pos = (int) view.getTag();
                Keyword keyword = getItem(pos);
                if (keyword.isVisible() != isChecked) {
                    remove(keyword);
                    keyword.setVisible(isChecked);
                    insert(keyword, pos);
                    saveChangedKeyword(keyword);
                }
            }
        }

        private class OnEditNameListener implements View.OnClickListener {
            @Override
            public void onClick(final View view) {
                final int pos = (int) view.getTag();
                final Keyword keyword = getItem(pos);
                KeywordEditNameDialogFragment fragment =
                    new KeywordEditNameDialogFragment();
                fragment.setKeyword(keyword, pos);
                fragment.show(getFragmentManager(), "keyword_edit_name");
            }
        }

        private class OnColorDialogListener implements View.OnClickListener {
            @Override
            public void onClick(final View view) {
                final int pos = (int) view.getTag();
                final Keyword keyword = getItem(pos);
                Dialog dialog = new ColorPickerDialog(
                    view.getContext(),
                    new ColorPickerDialog.OnColorChangedListener()
                    {
                        public void colorChanged(int color) {
                            final int pos = (int) view.getTag();
                            Keyword keyword = getItem(pos);
                            remove(keyword);
                            keyword.setColor(color);
                            insert(keyword, pos);
                            saveChangedKeyword(keyword);
                        }
                    },
                    keyword.getColor());
                dialog.show();
            }
        }

        private class OnMoveUpListener implements View.OnClickListener {
            @Override
            public void onClick(final View view) {
                final int pos = (int) view.getTag();
                if (pos >= 1) {
                    final int newPos = pos - 1;
                    Keyword keyword = getItem(pos);
                    remove(keyword);
                    flagManager.moveKeyword(keyword, newPos);
                    insert(keyword, newPos);
                    saveChangedKeywordOrder();
                }
            }
        }

        private class OnMoveDownListener implements View.OnClickListener {
            @Override
            public void onClick(final View view) {
                final int pos = (int) view.getTag();
                if (pos < getCount() - 1) {
                    final int newPos = pos + 1;
                    Keyword keyword = getItem(pos);
                    remove(keyword);
                    flagManager.moveKeyword(keyword, newPos);
                    insert(keyword, newPos);
                    saveChangedKeywordOrder();
                }
            }
        }

        private class ItemViewHolder {
            public CheckBox cbVisibility;
            public LinearLayout llTexts;
            public TextView tvName;
            public TextView tvKeyword;
            public ImageButton btnColorDialog;
            public ImageButton btnUp;
            public ImageButton btnDown;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.add_keyword:
            onAddKeyword();
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void onAddKeyword() {
        KeywordAddDialogFragment fragment = new KeywordAddDialogFragment();
        fragment.show(getFragmentManager(), "add_keyword");
    }

    @Override
    public void onCreateContextMenu(
        ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        getMenuInflater().inflate(R.menu.keyword_context, menu);
        Keyword keyword = adapter.getItem(info.position);
        menu.setHeaderTitle(
            keyword.getName() + " (" + keyword.getExternalCode() + ")");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info =
            (AdapterContextMenuInfo) item.getMenuInfo();
        Keyword keyword = adapter.getItem(info.position);

        switch (item.getItemId()) {
        case R.id.delete_keyword:
            onDeleteKeyword(keyword);
            break;
        }

        return super.onContextItemSelected(item);
    }

    private void onDeleteKeyword(final Keyword keyword) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(getString(R.string.delete_keyword_confirmation_title,
                keyword.getName()))
            .setMessage(getString(R.string.delete_keyword_confirmation_message,
                keyword.getName(), keyword.getExternalCode()))
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(
                android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(
                        DialogInterface dialog, int whichButton) {
                            adapter.remove(keyword);
                            flagManager.deleteKeyword(keyword);
                            saveDeletedKeyword();
                        }
                })
            .setNegativeButton(android.R.string.no, null)
            .show();
    }


    // KeywordEditNameDialogFragment.KeywordEditNameDialogListener interface
    @Override
    public void onKeywordNameChanged(String name, int pos) {
        if (!name.trim().isEmpty()) {
            Keyword keyword = adapter.getItem(pos);
            adapter.remove(keyword);
            keyword.setName(name);
            adapter.insert(keyword, pos);
            saveChangedKeyword(keyword);
        }
    }


    // KeywordAddDialogFragment.KeywordAddDialogFragmentDialogListener
    @Override
    public void onKeywordAdded(String externalCode) {
        try {
            Keyword keyword =
                flagManager.getKeywordByExternalCode(externalCode);
            keyword.setVisible(true);
            adapter.add(keyword);
            saveChangedKeyword(keyword);
        } catch (IllegalArgumentException e) {
            Toast.makeText(
                this, "Adding keyword failed: " + e.toString(),
                Toast.LENGTH_LONG).show();
        }
    }

}
