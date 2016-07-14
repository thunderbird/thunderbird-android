package com.fsck.k9.service;


import java.io.File;
import java.io.IOException;


public interface FileFactory {
    File createFile() throws IOException;
}
