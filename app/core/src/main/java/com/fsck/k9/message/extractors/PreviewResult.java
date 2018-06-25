package com.fsck.k9.message.extractors;


import android.support.annotation.NonNull;


public class PreviewResult {
    private final PreviewType previewType;
    private final String previewText;


    private PreviewResult(PreviewType previewType, String previewText) {
        this.previewType = previewType;
        this.previewText = previewText;
    }

    public static PreviewResult text(@NonNull String previewText) {
        return new PreviewResult(PreviewType.TEXT, previewText);
    }

    public static PreviewResult encrypted() {
        return new PreviewResult(PreviewType.ENCRYPTED, null);
    }

    public static PreviewResult none() {
        return new PreviewResult(PreviewType.NONE, null);
    }

    public static PreviewResult error() {
        return new PreviewResult(PreviewType.ERROR, null);
    }

    public PreviewType getPreviewType() {
        return previewType;
    }

    public boolean isPreviewTextAvailable() {
        return previewType == PreviewType.TEXT;
    }

    public String getPreviewText() {
        if (!isPreviewTextAvailable()) {
            throw new IllegalStateException("Preview is not available");
        }

        return previewText;
    }


    public enum PreviewType {
        NONE,
        TEXT,
        ENCRYPTED,
        ERROR
    }
}
