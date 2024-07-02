package com.fsck.k9.mailstore

class FolderNotFoundException(val folderId: Long) : RuntimeException("Folder not found: $folderId")
