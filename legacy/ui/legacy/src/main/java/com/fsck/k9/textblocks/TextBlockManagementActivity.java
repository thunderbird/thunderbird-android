package com.fsck.k9.textblocks;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fsck.k9.ui.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class TextBlockManagementActivity extends AppCompatActivity 
        implements TextBlockAdapter.OnTextBlockClickListener {

    private static final String EXTRA_ENABLE_TEXT_INSERTION = "enable_text_insertion";
    private static final String EXTRA_SELECTED_TEXT = "selected_text";
    
    private TextBlockRepository repository;
    private TextBlockAdapter adapter;
    private RecyclerView recyclerView;
    private TextView emptyView;
    private boolean enableTextInsertion = false;

    public static Intent createIntent(Context context) {
        return new Intent(context, TextBlockManagementActivity.class);
    }
    
    public static Intent createIntentForTextInsertion(Context context) {
        Intent intent = new Intent(context, TextBlockManagementActivity.class);
        intent.putExtra(EXTRA_ENABLE_TEXT_INSERTION, true);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_textblock_management);

        repository = new TextBlockRepository(this);
        enableTextInsertion = getIntent().getBooleanExtra(EXTRA_ENABLE_TEXT_INSERTION, false);
        
        setupToolbar();
        setupRecyclerView();
        setupFab();
        
        loadTextBlocks();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.textblocks_recyclerview);
        emptyView = findViewById(R.id.empty_view);
        
        adapter = new TextBlockAdapter(this, enableTextInsertion);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupFab() {
        FloatingActionButton fab = findViewById(R.id.fab_add_textblock);
        fab.setOnClickListener(v -> showTextBlockDialog(null));
    }

    private void loadTextBlocks() {
        List<TextBlock> textBlocks = repository.getAllTextBlocks();
        
        // Wenn keine Textbausteine vorhanden sind, erstelle Beispiele
        if (textBlocks.isEmpty()) {
            createSampleTextBlocks();
            textBlocks = repository.getAllTextBlocks();
        }
        
        adapter.setTextBlocks(textBlocks);
        
        if (textBlocks.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    private void createSampleTextBlocks() {
        // Beispiel-Textbausteine erstellen
        repository.addTextBlock(new TextBlock(
            "Freundliche Grüße",
            "Mit freundlichen Grüßen\n\n[Ihr Name]"
        ));

        repository.addTextBlock(new TextBlock(
            "Entschuldigung",
            "Entschuldigen Sie bitte die verspätete Antwort."
        ));

        repository.addTextBlock(new TextBlock(
            "Dank für E-Mail",
            "Vielen Dank für Ihre E-Mail."
        ));
    }

    private void showTextBlockDialog(TextBlock textBlock) {
        boolean isEditing = textBlock != null;
        
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_textblock_edit, null);
        
        EditText nameEdit = dialogView.findViewById(R.id.edit_textblock_name);
        EditText contentEdit = dialogView.findViewById(R.id.edit_textblock_content);
        
        if (isEditing) {
            nameEdit.setText(textBlock.getName());
            contentEdit.setText(textBlock.getContent());
        }
        
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(isEditing ? R.string.textblock_dialog_title_edit : R.string.textblock_dialog_title_add)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, (d, which) -> {
                    String name = nameEdit.getText().toString().trim();
                    String content = contentEdit.getText().toString().trim();
                    
                    if (name.isEmpty()) {
                        nameEdit.setError(getString(R.string.textblock_name_empty));
                        return;
                    }
                    
                    if (content.isEmpty()) {
                        contentEdit.setError(getString(R.string.textblock_content_empty));
                        return;
                    }
                    
                    if (isEditing) {
                        textBlock.setName(name);
                        textBlock.setContent(content);
                        repository.updateTextBlock(textBlock);
                    } else {
                        TextBlock newTextBlock = new TextBlock(name, content);
                        repository.addTextBlock(newTextBlock);
                    }
                    
                    loadTextBlocks();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
                
        dialog.show();
    }

    @Override
    public void onEditClick(TextBlock textBlock) {
        showTextBlockDialog(textBlock);
    }

    @Override
    public void onDeleteClick(TextBlock textBlock) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.textblock_delete)
                .setMessage(R.string.textblock_delete_confirm)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    repository.deleteTextBlock(textBlock.getId());
                    loadTextBlocks();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public void onTextBlockClick(TextBlock textBlock) {
        if (enableTextInsertion) {
            // Text zurückgeben und Activity schließen
            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_SELECTED_TEXT, textBlock.getContent());
            setResult(RESULT_OK, resultIntent);
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}