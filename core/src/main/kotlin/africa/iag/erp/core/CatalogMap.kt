package africa.iag.erp.core

/** Maps on-device desk labels to Go record collection keys. */
fun apiEntityKey(label: String): String {
    val lower = label.lowercase()
    if (lower.contains("warehouse") && lower.contains("location")) return "inventory-locations"
    if (lower.contains("pos") && lower.contains("location")) return "pos-locations"
    if (lower == "new project" || lower == "new-project") return "projects"
    if (lower.contains("material request")) return "requisitions"
    if (lower == "machines" || lower == "machine") return "work-centers"
    if (lower.contains("payment request") && !lower.contains("oral")) return "payment-requests"
    if (lower == "clock in" || lower == "my punches") return "attendance"
    return lower
        .replace("&", "and")
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
}

data class ApiStorageTarget(val module: String, val entity: String)

fun apiStorageTarget(moduleId: String, entity: String): ApiStorageTarget {
    val key = apiEntityKey(entity)
    return when (moduleId) {
        "receipts-payments" -> ApiStorageTarget("banking", key)
        "clock-in" -> ApiStorageTarget("payroll", key)
        "folders" -> ApiStorageTarget("documents", key)
        "general-requests", "oral-payment-requests" -> ApiStorageTarget("requests", key)
        "contract-manager" -> ApiStorageTarget("projects", key)
        else -> ApiStorageTarget(moduleId, key)
    }
}

fun recordFromApi(row: Map<String, Any?>, moduleId: String, entity: String): ErpRecord {
    val strings = stringifyApiRow(row)
    val title = firstFilled(strings, listOf("name", "reference", "title", "code", "id")) ?: newId()
    val subtitle = firstFilled(strings, listOf("description", "party", "customer", "subtitle", "employee", "account", "site")) ?: ""
    val status = firstFilled(strings, listOf("status")) ?: "Draft"
    val date = firstFilled(strings, listOf("date", "createdAt", "created")) ?: ""
    val amountRaw = firstFilled(strings, listOf("amount", "balance", "total"))
    val amount = amountRaw?.replace(",", "")?.toDoubleOrNull()
    val fields = strings.filterKeys { it !in setOf("id", "name", "reference", "title", "status", "date", "description", "amount") }
    return ErpRecord(
        id = strings["id"] ?: newId(),
        moduleId = moduleId,
        entity = entity,
        title = title,
        subtitle = subtitle,
        status = status,
        date = date,
        amount = amount,
        fields = fields,
    )
}

fun apiPayload(from: ErpRecord): Map<String, String> {
    val row = from.fields.toMutableMap()
    row["id"] = from.id
    row["name"] = from.title
    row["status"] = from.status
    if (from.date.isNotEmpty()) row["date"] = from.date
    if (from.subtitle.isNotEmpty()) row["description"] = from.subtitle
    from.amount?.let { row["amount"] = it.toString() }
    return row
}

private fun stringifyApiRow(row: Map<String, Any?>): Map<String, String> {
    val out = linkedMapOf<String, String>()
    for ((key, value) in row) {
        if (value == null) continue
        when (value) {
            is Map<*, *> -> {
                val nested = value["name"]?.toString() ?: value["id"]?.toString() ?: "${value.size}"
                if (nested.isNotBlank()) out[key] = nested
            }
            is Iterable<*> -> out[key] = value.joinToString(", ")
            is Number -> out[key] = value.toString()
            is Boolean -> out[key] = value.toString()
            else -> {
                val text = value.toString().trim()
                if (text.isNotEmpty() && text != "null") out[key] = text
            }
        }
    }
    return out
}

private fun firstFilled(row: Map<String, String>, keys: List<String>): String? {
    for (key in keys) {
        val value = row[key]?.trim()
        if (!value.isNullOrEmpty()) return value
    }
    return null
}
