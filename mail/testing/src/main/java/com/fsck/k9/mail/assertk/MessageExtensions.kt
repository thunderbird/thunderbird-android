@file:Suppress("TooManyFunctions")

package com.fsck.k9.mail.assertk

import assertk.Assert
import assertk.assertions.prop
import com.fsck.k9.mail.Body
import com.fsck.k9.mail.BodyPart
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.internet.MimeMultipart
import com.fsck.k9.mail.internet.MimeParameterDecoder
import com.fsck.k9.mail.internet.MimeUtility
import com.fsck.k9.mail.internet.MimeValue
import com.fsck.k9.mail.internet.RawDataBody
import com.fsck.k9.mail.internet.TextBody

fun Assert<Part>.body() = prop(Part::getBody)

@JvmName("textBodyEncoding")
fun Assert<TextBody>.contentTransferEncoding() = prop(TextBody::getEncoding)

@JvmName("rawDataBodyEncoding")
fun Assert<RawDataBody>.contentTransferEncoding() = prop(RawDataBody::getEncoding)

fun Assert<Body>.asBytes() = transform { it.inputStream.readBytes() }

fun Assert<Body>.asText() = transform {
    String(MimeUtility.decodeBody(it).readBytes())
}

fun Assert<MimeMultipart>.bodyParts() = transform { it.bodyParts }

fun Assert<MimeMultipart>.bodyPart(index: Int): Assert<BodyPart> = transform { it.getBodyPart(index) }

fun Assert<Part>.mimeType() = transform { it.mimeType }

fun Assert<Part>.contentType() = transform { MimeParameterDecoder.decode(it.contentType) }

fun Assert<MimeValue>.value() = transform { it.value }

fun Assert<MimeValue>.parameter(name: String): Assert<String?> = transform { it.parameters[name] }
