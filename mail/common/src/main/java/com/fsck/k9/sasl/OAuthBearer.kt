@file:JvmName("OAuthBearer")

package com.fsck.k9.sasl

import okio.ByteString.Companion.encodeUtf8

/**
 * Builds an initial client response for the SASL `OAUTHBEARER` mechanism.
 *
 * See [RFC 7628](https://datatracker.ietf.org/doc/html/rfc7628).
 */
fun buildOAuthBearerInitialClientResponse(username: String, token: String): String {
    val saslName = username.replace("=", "=3D").replace(",", "=2C")
    return "n,a=$saslName,\u0001auth=Bearer $token\u0001\u0001".encodeUtf8().base64()
}
