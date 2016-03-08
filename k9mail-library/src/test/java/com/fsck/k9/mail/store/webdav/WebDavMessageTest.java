package com.fsck.k9.mail.store.webdav;

import com.fsck.k9.mail.MessagingException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 21)
public class WebDavMessageTest {

    private WebDavMessage message;
    @Mock
    private WebDavFolder mockFolder;
    @Mock
    private WebDavStore mockStore;
    @Mock
    private WebDavFolder mockTrashFolder;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        when(mockFolder.getName()).thenReturn("Inbox");
        message = new WebDavMessage("message1", mockFolder);
    }

    @Test
    public void delete_asks_folder_to_delete_message() throws MessagingException {
        when(mockFolder.getStore()).thenReturn(mockStore);
        when(mockStore.getFolder("Trash")).thenReturn(mockTrashFolder);
        message.delete("Trash");
        verify(mockFolder).moveMessages(Collections.singletonList(message), mockTrashFolder);
    }
}
