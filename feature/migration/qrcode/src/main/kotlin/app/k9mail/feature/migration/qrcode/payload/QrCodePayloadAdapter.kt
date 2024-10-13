package app.k9mail.feature.migration.qrcode.payload

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import timber.log.Timber

internal class QrCodePayloadAdapter : JsonAdapter<QrCodeData>() {
    override fun fromJson(jsonReader: JsonReader): QrCodeData? {
        jsonReader.beginArray()

        val version = jsonReader.nextInt()
        if (version != 1) {
            // We don't even attempt to read something that is newer than version 1.
            Timber.d("Unsupported version: %s", version)
            return null
        }

        val misc = readMiscellaneousData(jsonReader)

        val accounts = buildList {
            do {
                add(readAccount(jsonReader))
            } while (jsonReader.hasNext())
        }

        jsonReader.endArray()

        return QrCodeData(version, misc, accounts)
    }

    private fun readMiscellaneousData(jsonReader: JsonReader): QrCodeData.Misc {
        jsonReader.beginArray()

        val sequenceNumber = jsonReader.nextInt()
        val sequenceEnd = jsonReader.nextInt()

        skipAdditionalArrayEntries(jsonReader)
        jsonReader.endArray()

        return QrCodeData.Misc(
            sequenceNumber,
            sequenceEnd,
        )
    }

    private fun readAccount(jsonReader: JsonReader): QrCodeData.Account {
        val incomingServer = readIncomingServer(jsonReader)
        val outgoingServers = readOutgoingServers(jsonReader)

        return QrCodeData.Account(incomingServer, outgoingServers)
    }

    private fun readIncomingServer(jsonReader: JsonReader): QrCodeData.IncomingServer {
        jsonReader.beginArray()

        val protocol = jsonReader.nextInt()
        val hostname = jsonReader.nextString()
        val port = jsonReader.nextInt()
        val connectionSecurity = jsonReader.nextInt()
        val authenticationType = jsonReader.nextInt()
        val username = jsonReader.nextString()
        val accountName = if (jsonReader.hasNext()) jsonReader.nextString() else null
        val password = if (jsonReader.hasNext()) jsonReader.nextString() else null

        skipAdditionalArrayEntries(jsonReader)
        jsonReader.endArray()

        return QrCodeData.IncomingServer(
            protocol,
            hostname,
            port,
            connectionSecurity,
            authenticationType,
            username,
            accountName,
            password,
        )
    }

    private fun readOutgoingServers(jsonReader: JsonReader): List<QrCodeData.OutgoingServer> {
        jsonReader.beginArray()

        val outgoingServers = buildList {
            do {
                add(readOutgoingServer(jsonReader))
            } while (jsonReader.hasNext())
        }

        jsonReader.endArray()

        return outgoingServers
    }

    private fun readOutgoingServer(jsonReader: JsonReader): QrCodeData.OutgoingServer {
        jsonReader.beginArray()

        jsonReader.beginArray()

        val protocol = jsonReader.nextInt()
        val hostname = jsonReader.nextString()
        val port = jsonReader.nextInt()
        val connectionSecurity = jsonReader.nextInt()
        val authenticationType = jsonReader.nextInt()
        val username = jsonReader.nextString()
        val password = if (jsonReader.hasNext()) jsonReader.nextString() else null

        skipAdditionalArrayEntries(jsonReader)
        jsonReader.endArray()

        val identities = buildList {
            do {
                add(readIdentity(jsonReader))
            } while (jsonReader.hasNext())
        }

        jsonReader.endArray()

        return QrCodeData.OutgoingServer(
            protocol,
            hostname,
            port,
            connectionSecurity,
            authenticationType,
            username,
            password,
            identities,
        )
    }

    private fun readIdentity(jsonReader: JsonReader): QrCodeData.Identity {
        jsonReader.beginArray()

        val emailAddress = jsonReader.nextString()
        val displayName = jsonReader.nextString()

        skipAdditionalArrayEntries(jsonReader)
        jsonReader.endArray()

        return QrCodeData.Identity(emailAddress, displayName)
    }

    private fun skipAdditionalArrayEntries(jsonReader: JsonReader) {
        // For forward compatibility allow additional array elements.
        while (jsonReader.hasNext()) {
            jsonReader.readJsonValue()
        }
    }

    override fun toJson(jsonWriter: JsonWriter, value: QrCodeData?) {
        throw UnsupportedOperationException("not implemented")
    }
}
