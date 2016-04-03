/*
 * Copyright (C) 2016 cketti
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.cketti.safecontentresolver;


import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;

import android.content.ContentResolver;


class SafeContentResolverApi14 extends SafeContentResolver {
    SafeContentResolverApi14(ContentResolver contentResolver) {
        super(contentResolver);
    }

    @Override
    protected int getFileUidOrThrow(FileDescriptor fileDescriptor) throws FileNotFoundException {
        try {
            int systemFileDescriptor = extractSystemFileDescriptor(fileDescriptor);

            return Os.fstat(systemFileDescriptor);
        } catch (ErrnoException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    private int extractSystemFileDescriptor(FileDescriptor fileDescriptor) throws FileNotFoundException {
        Field descriptor;
        try {
            descriptor = fileDescriptor.getClass().getDeclaredField("descriptor");
        } catch (NoSuchFieldException e) {
            throw new FileNotFoundException("Couldn't find field that holds system file descriptor");
        }

        descriptor.setAccessible(true);

        try {
            return descriptor.getInt(fileDescriptor);
        } catch (IllegalAccessException e) {
            throw new FileNotFoundException("Couldn't read system file descriptor");
        }
    }
}
