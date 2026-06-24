package com.fsck.k9.textblocks;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fsck.k9.ui.R;

import java.util.ArrayList;
import java.util.List;

public class TextBlockAdapter extends RecyclerView.Adapter<TextBlockAdapter.TextBlockViewHolder> {

    public interface OnTextBlockClickListener {
        void onEditClick(TextBlock textBlock);
        void onDeleteClick(TextBlock textBlock);
    }

    private List<TextBlock> textBlocks = new ArrayList<>();
    private OnTextBlockClickListener listener;
    private boolean enableTextInsertion = false;

    public TextBlockAdapter(OnTextBlockClickListener listener) {
        this.listener = listener;
        this.enableTextInsertion = false;
    }
    
    public TextBlockAdapter(OnTextBlockClickListener listener, boolean enableTextInsertion) {
        this.listener = listener;
        this.enableTextInsertion = enableTextInsertion;
    }

    public void setTextBlocks(List<TextBlock> textBlocks) {
        this.textBlocks = textBlocks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TextBlockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_textblock, parent, false);
        return new TextBlockViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TextBlockViewHolder holder, int position) {
        TextBlock textBlock = textBlocks.get(position);
        holder.bind(textBlock);
    }

    @Override
    public int getItemCount() {
        return textBlocks.size();
    }

    class TextBlockViewHolder extends RecyclerView.ViewHolder {
        private TextView nameTextView;
        private TextView previewTextView;
        private ImageButton editButton;
        private ImageButton deleteButton;
        private View contentArea;

        public TextBlockViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.textblock_name);
            previewTextView = itemView.findViewById(R.id.textblock_preview);
            editButton = itemView.findViewById(R.id.button_edit);
            deleteButton = itemView.findViewById(R.id.button_delete);
            contentArea = itemView.findViewById(R.id.textblock_content_area);
        }

        public void bind(TextBlock textBlock) {
            nameTextView.setText(textBlock.getName());
            previewTextView.setText(textBlock.getContent());

            // Click auf den Content-Bereich (für Texteinfügung)
            contentArea.setOnClickListener(v -> {
                if (listener instanceof TextBlockManagementActivity) {
                    ((TextBlockManagementActivity) listener).onTextBlockClick(textBlock);
                }
            });

            editButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditClick(textBlock);
                }
            });

            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(textBlock);
                }
            });
        }
    }
}