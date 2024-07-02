package com.fsck.k9.autocrypt

interface AutocryptStringProvider {
    fun transferMessageSubject(): String
    fun transferMessageBody(): String
}
