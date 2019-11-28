package com.fsck.k9.crypto.openpgp

import android.content.res.Resources
import com.fsck.k9.DI
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType.*
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing.IteratorWithIOThrow
import java.io.InputStream

class SecretKeyImporter {
    fun findSecretKeyRings(inputStream: InputStream): List<SecretKeyInfo>  {
        val operationLog = OperationLog()

        return inputStream.use { stream ->
            val keyRingIterator = UncachedKeyRing.fromStream(stream)
            keyRingIterator.iterator().asSequence()
                    .mapNotNull { it.canonicalize(operationLog, 0) }
                    .filterIsInstance<CanonicalizedSecretKeyRing>()
                    .mapNotNull { it.toSecretKeyInfoOrNull() }
                    .toList()
        }
    }

    private fun CanonicalizedSecretKeyRing.toSecretKeyInfoOrNull(): SecretKeyInfo? {
        val needsPassword = when (secretKey.secretKeyTypeSuperExpensive) {
            UNAVAILABLE -> return null
            GNU_DUMMY -> return null
            DIVERT_TO_CARD -> return null
            PASSPHRASE -> true
            PASSPHRASE_EMPTY -> false
        }

        return SecretKeyInfo(secretKeyRing = this, needsPassword = needsPassword)
    }

    private fun <T> IteratorWithIOThrow<T>.iterator(): Iterator<T> {
        return ProperIterator(this)
    }

    private class ProperIterator<T>(private val iterator: IteratorWithIOThrow<T>) : Iterator<T> {
        override fun hasNext(): Boolean = iterator.hasNext()
        override fun next(): T = iterator.next()
    }
}

data class SecretKeyInfo(val secretKeyRing: CanonicalizedSecretKeyRing, val needsPassword: Boolean)
