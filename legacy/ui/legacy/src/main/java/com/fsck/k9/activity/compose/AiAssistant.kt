package com.fsck.k9.activity.compose

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.widget.Toast
import com.fsck.k9.ui.R
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class AiAssistant(private val context: Context) {
    
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    
    interface AiAssistantCallback {
        fun onAiResponseReceived(response: String)
        fun onAiError(error: String)
    }
    
    fun requestAiAssistance(
        emailHistory: String,
        userMessage: String,
        userName: String,
        signature: String,
        callback: AiAssistantCallback
    ) {
        val webhookUrl = com.fsck.k9.K9.aiN8nWebhookUrl
        
        // Debug-Ausgabe
        android.util.Log.d("AiAssistant", "Webhook URL from K9: '$webhookUrl'")
        
        if (webhookUrl.isNullOrEmpty()) {
            mainHandler.post {
                callback.onAiError(context.getString(R.string.ai_error_no_url) + " (Debug: '$webhookUrl')")
            }
            return
        }
        
        executor.execute {
            try {
                val response = makeHttpRequest(webhookUrl, emailHistory, userMessage, userName, signature)
                mainHandler.post {
                    callback.onAiResponseReceived(response)
                }
            } catch (e: Exception) {
                mainHandler.post {
                    callback.onAiError(context.getString(R.string.ai_error_network) + ": " + e.message)
                }
            }
        }
    }
    
    private fun makeHttpRequest(webhookUrl: String, emailHistory: String, userMessage: String, userName: String, signature: String): String {
        val url = URL(webhookUrl)
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 120000
            connection.readTimeout = 120000
            
            // JSON-Payload erstellen
            val jsonPayload = JSONObject().apply {
                put("emailHistory", emailHistory)
                put("userMessage", userMessage)
                put("userName", userName)
                put("signature", signature)
                put("timestamp", getFormattedTimestamp())
            }
            
            // Request senden
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonPayload.toString())
                writer.flush()
            }
            
            // Response lesen
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    val response = reader.readText()
                    
                    return parseAiResponse(response)
                }
            } else {
                throw Exception("HTTP $responseCode: ${connection.responseMessage}")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseAiResponse(response: String): String {
        android.util.Log.d("AiAssistant", "Raw response: $response")
        
        return try {
            // Versuche zuerst als JSON-Array zu parsen (wie in deinem Beispiel)
            val jsonArray = org.json.JSONArray(response)
            if (jsonArray.length() > 0) {
                val firstItem = jsonArray.getJSONObject(0)
                val output = firstItem.optString("output", "")
                android.util.Log.d("AiAssistant", "Parsed output from array: $output")
                // Konvertiere alle JSON-Escape-Sequenzen
                val cleanedOutput = unescapeJsonString(output)
                android.util.Log.d("AiAssistant", "Cleaned output: $cleanedOutput")
                return cleanedOutput
            }
            
            // Fallback: Als JSON-Objekt versuchen
            val jsonObject = JSONObject(response)
            val aiResponse = jsonObject.optString("aiResponse", "")
            android.util.Log.d("AiAssistant", "Parsed aiResponse from object: $aiResponse")
            return unescapeJsonString(aiResponse)
            
        } catch (e: Exception) {
            // Fallback: Direkt den Response zurückgeben, falls JSON-Parsing fehlschlägt
            android.util.Log.w("AiAssistant", "Could not parse JSON response: $response", e)
            
            // Versuche manuell das output-Feld zu extrahieren, falls es im Raw-Text steht
            // Für mehrzeilige JSON-Strings mit Escape-Sequenzen
            val outputPattern = "\"output\"\\s*:\\s*\"([^\"]*(?:\\\\.[^\"]*)*)\""
            val regex = Regex(outputPattern, RegexOption.DOT_MATCHES_ALL)
            val matchResult = regex.find(response)
            
            if (matchResult != null) {
                val extractedOutput = matchResult.groupValues[1]
                android.util.Log.d("AiAssistant", "Extracted via regex: $extractedOutput")
                return unescapeJsonString(extractedOutput)
            }
            
            // Alternative: Versuche den kompletten JSON zu säubern
            if (response.contains("\"output\"")) {
                // Extrahiere alles zwischen den ersten Anführungszeichen nach "output":
                val startIndex = response.indexOf("\"output\":")
                if (startIndex != -1) {
                    val valueStart = response.indexOf("\"", startIndex + 9) + 1
                    val valueEnd = response.lastIndexOf("\"")
                    if (valueStart > 0 && valueEnd > valueStart) {
                        val extracted = response.substring(valueStart, valueEnd)
                        android.util.Log.d("AiAssistant", "Extracted via string manipulation: $extracted")
                        return unescapeJsonString(extracted)
                    }
                }
            }
            
            // Letzter Fallback: Raw response mit vollständiger Escape-Behandlung
            return unescapeJsonString(response)
        }
    }

    private fun unescapeJsonString(input: String): String {
        return input
            .replace("\\n", "\n")           // Zeilenumbrüche
            .replace("\\r", "\r")           // Carriage Returns
            .replace("\\t", "\t")           // Tabs
            .replace("\\\"", "\"")          // Anführungszeichen
            .replace("\\'", "'")            // Einfache Anführungszeichen
            .replace("\\\\", "\\")          // Backslashes (zuletzt!)
    }

    private fun getFormattedTimestamp(): String {
        val dateFormat = java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss", java.util.Locale.GERMANY)
        return dateFormat.format(java.util.Date())
    }
}
