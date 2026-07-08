package com.meetnote.android.security

interface ProviderKeyStore {
    suspend fun save(provider: String, apiKey: String)
    suspend fun read(provider: String): String?
}
