package org.apache.james.mime4j.message;

import java.io.IOException;
import java.io.InputStream;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.MessageBuilder;
import org.apache.james.mime4j.storage.StorageProvider;
import org.apache.james.mime4j.stream.MimeEntityConfig;
import org.apache.james.mime4j.stream.MutableBodyDescriptorFactory;

/**
 * Default MessageBuilder implementation delegating Message parsing to the "legacy"
 * MessageImpl object.
 */
public class MessageBuilderImpl extends MessageBuilder {

    private StorageProvider storageProvider = null;
    private DecodeMonitor decodeMonitor = null;
    private MimeEntityConfig mimeEntityConfig = null;
    private MutableBodyDescriptorFactory mutableBodyDescriptorFactory = null;
    private boolean flatMode = false;
    private boolean contentDecoding = true;

    public MessageBuilderImpl() {
    }

    @Override
    public Message newMessage() {
        return new MessageImpl();
    }

    @Override
    public Message newMessage(Message source) {
        return new MessageImpl(source);
    }

    @Override
    public Message parse(InputStream source) throws MimeException, IOException {
        return new MessageImpl(source, mimeEntityConfig, storageProvider, mutableBodyDescriptorFactory, decodeMonitor, contentDecoding, flatMode);
    }

    @Override
    public void setDecodeMonitor(DecodeMonitor decodeMonitor) {
        this.decodeMonitor = decodeMonitor;
    }

    public void setStorageProvider(StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
    }

    public void setMimeEntityConfig(MimeEntityConfig mimeEntityConfig) {
        this.mimeEntityConfig = mimeEntityConfig;
    }

    public void setMutableBodyDescriptorFactory(
            MutableBodyDescriptorFactory mutableBodyDescriptorFactory) {
        this.mutableBodyDescriptorFactory  = mutableBodyDescriptorFactory;
    }

    @Override
    public void setContentDecoding(boolean contentDecoding) {
        this.contentDecoding  = contentDecoding;
    }

    @Override
    public void setFlatMode() {
        this.flatMode = true;
    }

}
