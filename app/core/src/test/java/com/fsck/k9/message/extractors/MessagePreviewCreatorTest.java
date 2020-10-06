package com.fsck.k9.message.extractors;


import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.message.extractors.PreviewResult.PreviewType;
import org.junit.Before;
import org.junit.Test;

import static com.fsck.k9.message.MessageCreationHelper.createEmptyPart;
import static com.fsck.k9.message.MessageCreationHelper.createTextPart;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


public class MessagePreviewCreatorTest {
    private TextPartFinder textPartFinder;
    private PreviewTextExtractor previewTextExtractor;
    private MessagePreviewCreator previewCreator;

    @Before
    public void setUp() throws Exception {
        textPartFinder = mock(TextPartFinder.class);
        previewTextExtractor = mock(PreviewTextExtractor.class);

        previewCreator = new MessagePreviewCreator(textPartFinder, previewTextExtractor);
    }

    @Test
    public void createPreview_withoutTextPart() {
        Message message = createDummyMessage();
        when(textPartFinder.findFirstTextPart(message)).thenReturn(null);

        PreviewResult result = previewCreator.createPreview(message);

        assertFalse(result.isPreviewTextAvailable());
        assertEquals(PreviewType.NONE, result.getPreviewType());
        verifyNoMoreInteractions(previewTextExtractor);
    }

    @Test
    public void createPreview_withEmptyTextPart() throws Exception {
        Message message = createDummyMessage();
        Part textPart = createEmptyPart("text/plain");
        when(textPartFinder.findFirstTextPart(message)).thenReturn(textPart);

        PreviewResult result = previewCreator.createPreview(message);

        assertFalse(result.isPreviewTextAvailable());
        assertEquals(PreviewType.NONE, result.getPreviewType());
        verifyNoMoreInteractions(previewTextExtractor);
    }

    @Test
    public void createPreview_withTextPart() throws Exception {
        Message message = createDummyMessage();
        Part textPart = createTextPart("text/plain");
        when(textPartFinder.findFirstTextPart(message)).thenReturn(textPart);
        when(previewTextExtractor.extractPreview(textPart)).thenReturn("expected");

        PreviewResult result = previewCreator.createPreview(message);

        assertTrue(result.isPreviewTextAvailable());
        assertEquals(PreviewType.TEXT, result.getPreviewType());
        assertEquals("expected", result.getPreviewText());
    }

    @Test
    public void createPreview_withPreviewTextExtractorThrowing() throws Exception {
        Message message = createDummyMessage();
        Part textPart = createTextPart("text/plain");
        when(textPartFinder.findFirstTextPart(message)).thenReturn(textPart);
        when(previewTextExtractor.extractPreview(textPart)).thenThrow(new PreviewExtractionException(""));

        PreviewResult result = previewCreator.createPreview(message);

        assertFalse(result.isPreviewTextAvailable());
        assertEquals(PreviewType.ERROR, result.getPreviewType());
    }

    @Test
    public void createPreview_withPreviewTextExtractorThrowingUnexpectedException() throws Exception {
        Message message = createDummyMessage();
        Part textPart = createTextPart("text/plain");
        when(textPartFinder.findFirstTextPart(message)).thenReturn(textPart);
        when(previewTextExtractor.extractPreview(textPart)).thenThrow(new IllegalStateException(""));

        PreviewResult result = previewCreator.createPreview(message);

        assertFalse(result.isPreviewTextAvailable());
        assertEquals(PreviewType.ERROR, result.getPreviewType());
    }

    private Message createDummyMessage() {
        return new MimeMessage();
    }
}
