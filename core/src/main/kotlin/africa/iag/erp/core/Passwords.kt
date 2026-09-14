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

data class UserNotice(val title: String, val message: String)

/** Maps store/API errors to short copy for alerts. Never pass the raw string through. */
fun userNotice(raw: String): UserNotice {
    val t = raw.trim().lowercase()
    return when {
        t.contains("enter your username") || (t.contains("username") && t.contains("password") && t.contains("enter")) ->
            UserNotice("Sign in", "Enter your username and password.")
        t.contains("at least 6") ->
            UserNotice("Password too short", "Use at least 6 characters.")
        t.contains("do not match") || t.contains("don't match") ->
            UserNotice("Passwords don't match", "Type the same password in both fields.")
        t.contains("no password set") ->
            UserNotice("Set a password", "Save a password on this phone, then try again.")
        t.contains("can't reach") || t.contains("cant reach") || t.contains("timed out") || t.contains("network") ->
            UserNotice("No connection", "We couldn't reach IAG right now. Check your internet, or continue on this device.")
        t.contains("wrong password") || t.contains("unknown user") || t.contains("invalid")
            || t.contains("live sign-in") || t.contains("rejected this password")
            || t.contains("unauthorized") || t.contains("10+") || t.contains("10 character") ->
            UserNotice(
                "Couldn't sign in",
                "That username or password isn't right. Try again, or tap Continue on this device.",
            )
        else -> UserNotice("Couldn't sign in", "Something went wrong. Please try again.")
    }
}
