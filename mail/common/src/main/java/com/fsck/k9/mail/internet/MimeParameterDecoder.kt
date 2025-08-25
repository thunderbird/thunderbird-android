package com.fsck.k9.mail.internet

import java.nio.charset.Charset
import java.nio.charset.IllegalCharsetNameException
import okio.Buffer

private typealias Parameters = Map<String, String>
private typealias BasicParameters = Map<String, ParameterValue>
private typealias SimpleParameter = Pair<String, String>
private typealias IgnoredParameters = List<SimpleParameter>

/**
 * Decode MIME parameter values as specified in RFC 2045 and RFC 2231.
 *
 * Parsing MIME header parameters is quite challenging because a lot of things have to be considered:
 * - RFC 822 allows comments and (folding) whitespace between all tokens in structured header fields
 * - parameter names are case insensitive
 * - the ordering of parameters is not significant
 * - parameters could be present in RFC 2045 style and RFC 2231 style
 * - it's not specified what happens when an extended parameter value (RFC 2231) doesn't specify a charset value
 * - some clients don't use the method described in RFC 2231 to encode parameter values with non-ASCII characters; but
 *   encoded words as described in RFC 2047
 *
 * This class takes a very lenient approach in order to be able to decode as many real world messages as possible.
 * First it does a pass to extract RFC 2045 style parameter names and values while remembering how the values were
 * encoded (token vs. quoted string). When a parsing error is encountered parsing is stopped, but anything successfully
 * parsed before that is kept. A second pass then checks if successfully parsed parameters are RFC 2231 encoded and
 * combines/decodes them.
 * If a parameter is present encoded according to RFC 2045 and also also as described in RFC 2231 the latter version
 * is preferred. In case only a RFC 2045 style parameter is present this class attempts to RFC 2047 (encoded word)
 * decode it. This is not a valid encoding for the structured header fields `Content-Type` and `Content-Disposition`,
 * but it is seen in the wild.
 */
object MimeParameterDecoder {

    @JvmStatic
    fun decode(headerBody: String): MimeValue {
        val parser = MimeHeaderParser(headerBody)

        val value = parser.readHeaderValue()
        parser.skipCFWS()
        if (parser.endReached()) {
            return MimeValue(value)
        }

        val (basicParameters, duplicateParameters, parserErrorIndex) = readBasicParameters(parser)
        val (parameters, ignoredParameters) = reconstructParameters(basicParameters)

        return MimeValue(
            value = value,
            parameters = parameters,
            ignoredParameters = duplicateParameters + ignoredParameters,
            parserErrorIndex = parserErrorIndex,
        )
    }

    fun decodeBasic(headerBody: String): MimeValue {
        val parser = MimeHeaderParser(headerBody)

        val value = parser.readHeaderValue()
        parser.skipCFWS()
        if (parser.endReached()) {
            return MimeValue(value)
        }

        val (basicParameters, duplicateParameters, parserErrorIndex) = readBasicParameters(parser)
        val parameters = basicParameters.mapValues { (_, parameterValue) -> parameterValue.value }

        return MimeValue(
            value = value,
            parameters = parameters,
            ignoredParameters = duplicateParameters,
            parserErrorIndex = parserErrorIndex,
        )
    }

    @JvmStatic
    fun extractHeaderValue(headerBody: String): String {
        val parser = MimeHeaderParser(headerBody)
        return parser.readHeaderValue()
    }

    private fun readBasicParameters(parser: MimeHeaderParser): BasicParameterResults {
        val parameters = mutableMapOf<String, ParameterValue>()
        val duplicateParameterNames = mutableSetOf<String>()
        val ignoredParameters = mutableListOf<SimpleParameter>()
        val parserErrorIndex = try {
            do {
                parser.expect(SEMICOLON)

                val parameterName = parser.readToken().lowercase()

                parser.skipCFWS()
                parser.expect(EQUALS_SIGN)
                parser.skipCFWS()

                val parameterValue = if (parser.peek() == DQUOTE) {
                    ParameterValue(parser.readQuotedString(), wasToken = false)
                } else {
                    ParameterValue(parser.readToken(), wasToken = true)
                }

                val existingParameterValue = parameters.remove(parameterName)
                when {
                    existingParameterValue != null -> {
                        duplicateParameterNames.add(parameterName)
                        ignoredParameters.add(parameterName to existingParameterValue.value)
                        ignoredParameters.add(parameterName to parameterValue.value)
                    }
                    parameterName !in duplicateParameterNames -> parameters[parameterName] = parameterValue
                    else -> ignoredParameters.add(parameterName to parameterValue.value)
                }

                parser.skipCFWS()
            } while (!parser.endReached())

            null
        } catch (e: MimeHeaderParserException) {
            e.errorIndex
        }

        return BasicParameterResults(parameters, ignoredParameters, parserErrorIndex)
    }

    private fun reconstructParameters(basicParameters: BasicParameters): Pair<Parameters, IgnoredParameters> {
        val parameterSectionMap = mutableMapOf<String, MutableList<ParameterSection>>()
        val singleParameters = mutableMapOf<String, String>()

        for ((parameterName, parameterValue) in basicParameters) {
            val parameterSection = convertToParameterSection(parameterName, parameterValue)

            if (parameterSection == null) {
                singleParameters[parameterName] = parameterValue.value
            } else {
                parameterSectionMap.getOrPut(parameterSection.name) { mutableListOf() }
                    .add(parameterSection)
            }
        }

        val parameters = mutableMapOf<String, String>()
        for ((parameterName, parameterSections) in parameterSectionMap) {
            parameterSections.sortBy { it.section }

            if (areParameterSectionsValid(parameterSections)) {
                parameters[parameterName] = combineParameterSections(parameterSections)
            } else {
                for (parameterSection in parameterSections) {
                    val originalParameterName = parameterSection.originalName
                    parameters[originalParameterName] = basicParameters[originalParameterName]!!.value
                }
            }
        }

        val ignoredParameters = mutableListOf<Pair<String, String>>()
        for ((parameterName, parameterValue) in singleParameters) {
            if (parameterName !in parameters) {
                parameters[parameterName] = DecoderUtil.decodeEncodedWords(parameterValue, null)
            } else {
                ignoredParameters.add(parameterName to parameterValue)
            }
        }

        return Pair(parameters, ignoredParameters)
    }

    private fun convertToParameterSection(parameterName: String, parameterValue: ParameterValue): ParameterSection? {
        val extendedValue = parameterName.endsWith(ASTERISK)
        if (extendedValue && !parameterValue.wasToken) {
            return null
        }

        val parts = parameterName.split(ASTERISK)
        if (parts.size !in 2..3 || parts.size == 3 && parts[2].isNotEmpty()) {
            return null
        }

        val newParameterName = parts[0]
        val sectionText = parts[1]
        val section = when {
            parts.size == 2 && extendedValue -> null
            sectionText == "0" -> 0
            sectionText.startsWith('0') -> return null
            sectionText.isNotAsciiNumber() -> return null
            else -> parts[1].toIntOrNull() ?: return null
        }

        val parameterText = parameterValue.value
        return if (extendedValue) {
            val parser = MimeHeaderParser(parameterText)
            if (section == null || section == 0) {
                readExtendedParameterValue(parser, parameterName, newParameterName, section, parameterText)
            } else {
                val data = Buffer()
                parser.readExtendedParameterValueInto(data)

                ExtendedValueParameterSection(newParameterName, parameterName, section, data)
            }
        } else {
            RegularValueParameterSection(newParameterName, parameterName, section, parameterText)
        }
    }

    private fun readExtendedParameterValue(
        parser: MimeHeaderParser,
        parameterName: String,
        newParameterName: String,
        section: Int?,
        parameterText: String,
    ): ParameterSection? {
        return try {
            val charsetName = parser.readUntil(SINGLE_QUOTE)
            parser.expect(SINGLE_QUOTE)
            val language = parser.readUntil(SINGLE_QUOTE)
            parser.expect(SINGLE_QUOTE)

            if (charsetName.isSupportedCharset()) {
                val data = Buffer()
                parser.readExtendedParameterValueInto(data)

                InitialExtendedValueParameterSection(
                    newParameterName,
                    parameterName,
                    section,
                    charsetName,
                    language,
                    data,
                )
            } else {
                val encodedParameterText = parameterText.substring(parser.position())
                RegularValueParameterSection(newParameterName, parameterName, section, encodedParameterText)
            }
        } catch (e: MimeHeaderParserException) {
            null
        }
    }

    private fun areParameterSectionsValid(parameterSections: MutableList<ParameterSection>): Boolean {
        if (parameterSections.size == 1) {
            val section = parameterSections.first().section
            return section == null || section == 0
        }

        val isExtendedValue = parameterSections.first() is InitialExtendedValueParameterSection

        parameterSections.forEachIndexed { index, parameterSection ->
            if (parameterSection.section != index ||
                !isExtendedValue &&
                parameterSection is ExtendedValueParameterSection
            ) {
                return false
            }
        }

        return true
    }

    private fun combineParameterSections(parameterSections: MutableList<ParameterSection>): String {
        val initialParameterSection = parameterSections.first()
        return if (initialParameterSection is InitialExtendedValueParameterSection) {
            val charset = Charset.forName(initialParameterSection.charsetName)
            combineExtendedParameterSections(parameterSections, charset)
        } else {
            combineRegularParameterSections(parameterSections)
        }
    }

    private fun combineExtendedParameterSections(parameterSections: List<ParameterSection>, charset: Charset): String {
        val buffer = Buffer()
        return buildString {
            for (parameterSection in parameterSections) {
                when (parameterSection) {
                    is ExtendedValueParameterSection -> buffer.writeAll(parameterSection.data)
                    is RegularValueParameterSection -> {
                        append(buffer.readString(charset))
                        append(parameterSection.text)
                    }
                }
            }
            append(buffer.readString(charset))
        }
    }

    private fun combineRegularParameterSections(parameterSections: MutableList<ParameterSection>): String {
        return buildString {
            for (parameterSection in parameterSections) {
                if (parameterSection !is RegularValueParameterSection) throw AssertionError()
                append(parameterSection.text)
            }
        }
    }

    private fun String.isSupportedCharset(): Boolean {
        if (isEmpty()) return false

        return try {
            Charset.isSupported(this)
        } catch (e: IllegalCharsetNameException) {
            false
        }
    }

    private fun String.isNotAsciiNumber(): Boolean = any { character -> character !in '0'..'9' }
}

private data class ParameterValue(val value: String, val wasToken: Boolean)

private data class BasicParameterResults(
    val parameters: BasicParameters,
    val ignoredParameters: IgnoredParameters,
    val parserErrorIndex: Int?,
)
