package com.fsck.k9.activity.setup

data class OAuthConfiguration(
    val clientId: String,
    val scopes: List<String>,
    val authorizationEndpoint: String,
    val tokenEndpoint: String
)
