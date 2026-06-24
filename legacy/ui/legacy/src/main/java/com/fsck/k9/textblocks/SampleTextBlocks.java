package com.fsck.k9.textblocks;

import android.content.Context;

/**
 * Hilfklasse zum Erstellen von Beispiel-Textbausteinen
 */
public class SampleTextBlocks {

    public static void createSampleTextBlocks(Context context) {
        TextBlockRepository repository = new TextBlockRepository(context);
        
        // Prüfen, ob bereits Textbausteine existieren
        if (!repository.getAllTextBlocks().isEmpty()) {
            return;
        }

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

        repository.addTextBlock(new TextBlock(
            "Termin vorschlagen",
            "Gerne können wir einen Termin vereinbaren. Wären Sie verfügbar am:"
        ));

        repository.addTextBlock(new TextBlock(
            "Weitere Informationen",
            "Falls Sie weitere Informationen benötigen, stehe ich Ihnen gerne zur Verfügung."
        ));
    }
}