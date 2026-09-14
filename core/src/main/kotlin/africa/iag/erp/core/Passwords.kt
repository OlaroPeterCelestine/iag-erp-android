package africa.iag.erp.core

import java.security.MessageDigest

internal fun passwordDigest(username: String, password: String): String {
    val user = username.trim().lowercase()
    val material = "iag-central|$user|$password"
    val digest = MessageDigest.getInstance("SHA-256").digest(material.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}

internal fun isPasswordHash(value: String): Boolean =
    value.length == 64 && value.all { it in "0123456789abcdefABCDEF" }
