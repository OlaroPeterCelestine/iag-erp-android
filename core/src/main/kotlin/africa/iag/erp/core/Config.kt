package africa.iag.erp.core

/** Live IAG Frontend. Next.js proxies /api to the shared Go API. */
object ErpConfig {
    const val liveFrontendOrigin = ErpEndpoints.liveFrontendOrigin
    const val liveApiOrigin = ErpEndpoints.liveApiOrigin
    const val localFrontendOrigin = ErpEndpoints.localFrontendOrigin
    const val localApiOrigin = ErpEndpoints.localApiOrigin
    const val originOverrideKey = "iag-erp-android-frontend-url"

    fun origin(
        persistence: KeyValueStore? = null,
        environment: Map<String, String> = System.getenv(),
    ): String {
        persistence?.get(originOverrideKey)?.let {
            val normalized = sanitizeOrigin(it)
            if (normalized.isNotEmpty()) return normalized
        }
        environment["IAG_FRONTEND_URL"]?.let {
            val normalized = sanitizeOrigin(it)
            if (normalized.isNotEmpty()) return normalized
        }
        return liveFrontendOrigin
    }

    fun sanitizeOrigin(raw: String): String {
        var value = raw.trim()
        while (value.endsWith("/")) value = value.dropLast(1)
        if (value.isEmpty()) return ""
        if (value.startsWith("http://") || value.startsWith("https://")) return value
        return "https://$value"
    }

    fun saveOrigin(origin: String, persistence: KeyValueStore) {
        val normalized = sanitizeOrigin(origin)
        persistence.put(originOverrideKey, if (normalized.isEmpty()) liveFrontendOrigin else normalized)
    }
}
