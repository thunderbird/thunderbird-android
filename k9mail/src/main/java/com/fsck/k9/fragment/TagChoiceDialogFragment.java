package com.fsck.k9.fragment;

import java.lang.ClassCastException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.fsck.k9.mail.FlagManager;
import com.fsck.k9.mail.Keyword;
import com.fsck.k9.R;

public class TagChoiceDialogFragment extends DialogFragment {

    public interface TagChoiceDialogListener {
        public void onStoreTags(Set<Keyword> addedTags, Set<Keyword> deletedTags);
        public void onStartKeywordEditor();
    }

    ArrayList<Keyword> tags;
    Set<Keyword> preSelectedTags;
    Set<Keyword> selectedTags;
    TagChoiceDialogListener listener;

    public void setPreSelectedTags(Collection<Keyword> preSelectedTags) {
        this.preSelectedTags = new HashSet<Keyword>(preSelectedTags);
        this.selectedTags = new HashSet<Keyword>(preSelectedTags);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArray(
            "preSelectedTags", preSelectedTags.toArray(new Keyword[0]));
        outState.putParcelableArray(
            "selectedTags", selectedTags.toArray(new Keyword[0]));
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstance) {

        if (savedInstance != null) {
            preSelectedTags = new HashSet<Keyword>(Arrays.asList(
                (Keyword[]) savedInstance.getParcelableArray("preSelectedTags")));
            selectedTags = new HashSet<Keyword>(Arrays.asList(
                (Keyword[]) savedInstance.getParcelableArray("selectedTags")));
        }

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View tagChoiceListView =
            inflater.inflate(R.layout.tag_choice_list, null);

        ImageButton buttonTagEditor = (ImageButton)
            tagChoiceListView.findViewById(R.id.button_keyword_editor);
        buttonTagEditor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onStartKeywordEditor();
                dismiss();
            }
        });

        tags = FlagManager.getFlagManager().getVisibleKeywords();
        TagListAdapter adapter = new TagListAdapter(getActivity(), tags);
        ListView listView = (ListView) tagChoiceListView.findViewById(
            R.id.tag_choice_list_view);

        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(
                AdapterView parent, View view, int position, long id)
            {
                ListView parentListView = (ListView) parent;
                Keyword clickedTag = tags.get(position);
                if (parentListView.isItemChecked(position)) {
                    selectedTags.add(clickedTag);
                } else {
                    selectedTags.remove(clickedTag);
                }
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.tag_choice_dialog_title)
            .setView(tagChoiceListView)
            .setPositiveButton(
                android.R.string.ok, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    Set<Keyword> addedTags = new HashSet<Keyword>(selectedTags);
                    addedTags.removeAll(preSelectedTags);
                    Set<Keyword> deletedTags = new HashSet<Keyword>(preSelectedTags);
                    deletedTags.removeAll(selectedTags);
                    listener.onStoreTags(addedTags, deletedTags);
                }
            })
            .setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (TagChoiceDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() +
                "does not implement TagChoiceDialogListener");
        }
    }


    public class TagListAdapter extends ArrayAdapter<Keyword> {

        TagListAdapter(Context context, List<Keyword> tags) {
            super(context, 0 /* resource not used */, tags);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = getActivity().getLayoutInflater();
                convertView = inflater.inflate(
                    android.R.layout.simple_list_item_multiple_choice, null);
            }

            Keyword tag = getItem(position);
            TextView textViewName = (TextView) convertView.findViewById(
                android.R.id.text1);
            if (textViewName != null && tag != null) {
                textViewName.setText(tag.getName());
                textViewName.setTextColor(tag.getTextColor());
            }

            ListView parentListView = (ListView) parent;
            if (parentListView != null && selectedTags.contains(tag)) {
                parentListView.setItemChecked(position, true);
            }

            return convertView;
        }

    }

}
