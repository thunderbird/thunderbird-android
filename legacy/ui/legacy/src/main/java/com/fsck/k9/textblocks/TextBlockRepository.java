package com.fsck.k9.textblocks;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository für die lokale Speicherung von Textbausteinen via SharedPreferences
 */
public class TextBlockRepository {
    private static final String PREFS_NAME = "textblocks_prefs";
    private static final String PREFS_KEY_TEXTBLOCKS_COUNT = "textblocks_count";
    private static final String PREFS_KEY_TEXTBLOCK_PREFIX = "textblock_";
    private static final String SEPARATOR = "|||";

    private final SharedPreferences sharedPreferences;

    public TextBlockRepository(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Alle Textbausteine laden
     */
    public List<TextBlock> getAllTextBlocks() {
        List<TextBlock> textBlocks = new ArrayList<>();
        int count = sharedPreferences.getInt(PREFS_KEY_TEXTBLOCKS_COUNT, 0);
        
        for (int i = 0; i < count; i++) {
            String data = sharedPreferences.getString(PREFS_KEY_TEXTBLOCK_PREFIX + i, null);
            if (data != null) {
                TextBlock textBlock = deserializeTextBlock(data);
                if (textBlock != null) {
                    textBlocks.add(textBlock);
                }
            }
        }
        
        return textBlocks;
    }

    /**
     * Textbaustein hinzufügen
     */
    public void addTextBlock(TextBlock textBlock) {
        List<TextBlock> textBlocks = getAllTextBlocks();
        textBlocks.add(textBlock);
        saveTextBlocks(textBlocks);
    }

    /**
     * Textbaustein aktualisieren
     */
    public void updateTextBlock(TextBlock updatedTextBlock) {
        List<TextBlock> textBlocks = getAllTextBlocks();
        for (int i = 0; i < textBlocks.size(); i++) {
            if (textBlocks.get(i).getId().equals(updatedTextBlock.getId())) {
                textBlocks.set(i, updatedTextBlock);
                break;
            }
        }
        saveTextBlocks(textBlocks);
    }

    /**
     * Textbaustein löschen
     */
    public void deleteTextBlock(String textBlockId) {
        List<TextBlock> textBlocks = getAllTextBlocks();
        for (int i = textBlocks.size() - 1; i >= 0; i--) {
            if (textBlocks.get(i).getId().equals(textBlockId)) {
                textBlocks.remove(i);
                break;
            }
        }
        saveTextBlocks(textBlocks);
    }

    /**
     * Textbaustein anhand der ID finden
     */
    public TextBlock getTextBlockById(String id) {
        List<TextBlock> textBlocks = getAllTextBlocks();
        for (TextBlock textBlock : textBlocks) {
            if (textBlock.getId().equals(id)) {
                return textBlock;
            }
        }
        return null;
    }

    /**
     * Alle Textbausteine speichern
     */
    private void saveTextBlocks(List<TextBlock> textBlocks) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        
        // Clear existing textblocks
        int oldCount = sharedPreferences.getInt(PREFS_KEY_TEXTBLOCKS_COUNT, 0);
        for (int i = 0; i < oldCount; i++) {
            editor.remove(PREFS_KEY_TEXTBLOCK_PREFIX + i);
        }
        
        // Save new textblocks
        for (int i = 0; i < textBlocks.size(); i++) {
            String serialized = serializeTextBlock(textBlocks.get(i));
            editor.putString(PREFS_KEY_TEXTBLOCK_PREFIX + i, serialized);
        }
        
        editor.putInt(PREFS_KEY_TEXTBLOCKS_COUNT, textBlocks.size());
        editor.apply();
    }

    /**
     * Alle Textbausteine löschen (für Reset-Funktionalität)
     */
    public void deleteAllTextBlocks() {
        saveTextBlocks(new ArrayList<>());
    }

    /**
     * Textbaustein zu String serialisieren
     */
    private String serializeTextBlock(TextBlock textBlock) {
        return textBlock.getId() + SEPARATOR + 
               escapeText(textBlock.getName()) + SEPARATOR + 
               escapeText(textBlock.getContent());
    }

    /**
     * String zu Textbaustein deserialisieren
     */
    private TextBlock deserializeTextBlock(String data) {
        String[] parts = data.split("\\|\\|\\|", 3);
        if (parts.length == 3) {
            return new TextBlock(
                parts[0], 
                unescapeText(parts[1]), 
                unescapeText(parts[2])
            );
        }
        return null;
    }
    
    private String escapeText(String text) {
        return text.replace(SEPARATOR, "&#SEPARATOR&#");
    }
    
    private String unescapeText(String text) {
        return text.replace("&#SEPARATOR&#", SEPARATOR);
    }
}