package africa.iag.erp.core

const val DEMO_PASSWORD = "iagdemo"
const val APP_VERSION = "1.0.0"

data class AuthUser(
    val username: String,
    val name: String,
    val role: String,
    val email: String = "",
    val phone: String = "",
    val title: String = "",
) {
    val initials: String
        get() {
            val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (parts.isEmpty()) return if (username.isEmpty()) "?" else username.first().uppercase()
            if (parts.size == 1) return parts.first().first().uppercase()
            return "${parts.first().first()}${parts.last().first()}".uppercase()
        }

    fun copyWith(
        name: String? = null,
        role: String? = null,
        email: String? = null,
        phone: String? = null,
        title: String? = null,
    ) = AuthUser(
        username = username,
        name = name ?: this.name,
        role = role ?: this.role,
        email = email ?: this.email,
        phone = phone ?: this.phone,
        title = title ?: this.title,
    )

    fun toJson(): Map<String, Any?> = linkedMapOf(
        "username" to username,
        "name" to name,
        "role" to role,
        "email" to email,
        "phone" to phone,
        "title" to title,
    )

    companion object {
        fun fromJson(j: Map<String, Any?>): AuthUser {
            val username = j["username"] as String
            val defaults = demo(username)
            val email = (j["email"] as? String)?.trim().orEmpty()
            val phone = (j["phone"] as? String)?.trim().orEmpty()
            val title = (j["title"] as? String)?.trim().orEmpty()
            return AuthUser(
                username = username,
                name = j["name"] as? String ?: defaults.name,
                role = j["role"] as? String ?: defaults.role,
                email = email.ifEmpty { defaults.email },
                phone = phone.ifEmpty { defaults.phone },
                title = title.ifEmpty { defaults.title },
            )
        }

        fun demo(username: String): AuthUser {
            val account = demoAccountFor(username)
            if (account == null) {
                val u = username.trim().lowercase()
                return AuthUser(username = u, name = u, role = "Viewer")
            }
            return AuthUser(
                username = account.username,
                name = account.name,
                role = account.role,
                title = account.title,
                email = account.email,
                phone = account.phone,
            )
        }
    }
}

data class ErpRecord(
    val id: String,
    val moduleId: String,
    val entity: String,
    var title: String,
    var subtitle: String,
    var status: String,
    var date: String,
    var amount: Double? = null,
    var fields: Map<String, String> = emptyMap(),
) {
    fun toJson(): Map<String, Any?> = linkedMapOf(
        "id" to id,
        "moduleId" to moduleId,
        "entity" to entity,
        "title" to title,
        "subtitle" to subtitle,
        "status" to status,
        "date" to date,
        "amount" to amount,
        "fields" to fields,
    )

    companion object {
        fun fromJson(j: Map<String, Any?>): ErpRecord {
            val fieldsRaw = j["fields"]
            val fields = if (fieldsRaw is Map<*, *>) {
                fieldsRaw.entries.associate { it.key.toString() to it.value.toString() }
            } else emptyMap()
            return ErpRecord(
                id = j["id"] as String,
                moduleId = j["moduleId"] as String,
                entity = j["entity"] as String,
                title = j["title"] as String,
                subtitle = j["subtitle"] as? String ?: "",
                status = j["status"] as? String ?: "Draft",
                date = j["date"] as? String ?: "",
                amount = (j["amount"] as? Number)?.toDouble(),
                fields = fields,
            )
        }
    }
}

data class Kpi(
    val label: String,
    val value: String,
    val hint: String,
    val up: Boolean = true,
)

data class ErpModule(
    val id: String,
    val label: String,
    val group: String,
    val description: String = "",
    val icon: String,
    val color: Long,
    val entities: List<String>,
    val approvalEntities: Set<String> = emptySet(),
    val seed: List<ErpRecord> = emptyList(),
)

interface KeyValueStore {
    fun get(key: String): String?
    fun put(key: String, value: String)
}

class MemoryKeyValueStore : KeyValueStore {
    private val data = linkedMapOf<String, String>()
    override fun get(key: String): String? = data[key]
    override fun put(key: String, value: String) {
        data[key] = value
    }
}

fun formatMoney(amount: Double): String {
    val abs = kotlin.math.abs(amount).toLong()
    val body = "%,d".format(abs)
    return "${if (amount < 0) "-" else ""}UGX $body"
}

fun todayIsoDate(): String {
    val cal = java.util.Calendar.getInstance()
    val y = cal.get(java.util.Calendar.YEAR)
    val m = cal.get(java.util.Calendar.MONTH) + 1
    val d = cal.get(java.util.Calendar.DAY_OF_MONTH)
    return "%04d-%02d-%02d".format(y, m, d)
}

fun newId(): String {
    val now = System.nanoTime()
    return "$now-${System.currentTimeMillis() % 997}"
}

fun seedRecord(
    module: String,
    entity: String,
    title: String,
    subtitle: String,
    status: String,
    date: String? = null,
    amount: Double? = null,
    fields: Map<String, String> = emptyMap(),
): ErpRecord = ErpRecord(
    id = newId(),
    moduleId = module,
    entity = entity,
    title = title,
    subtitle = subtitle,
    status = status,
    date = date ?: "2026-08-20",
    amount = amount,
    fields = fields,
)

val departmentGroups: List<String> = listOf(
    "Treasury",
    "Requests",
    "Commercial",
    "Inventory & production",
    "Projects",
    "Operations",
    "Quality",
    "People",
    "Accounting",
    "Records",
)

val workspaceGroups: List<String> = listOf("Command", "Records", "Requests", "People", "Help", "Admin")

enum class SearchKind { MODULE, ENTITY, RECORD, TOOL }

data class SearchHit(
    val kind: SearchKind,
    val title: String,
    val subtitle: String,
    val moduleId: String,
    val entity: String? = null,
    val recordId: String? = null,
) {
    val id: String = when (kind) {
        SearchKind.MODULE -> "module:$moduleId"
        SearchKind.ENTITY -> "entity:$moduleId:${entity.orEmpty()}"
        SearchKind.RECORD -> "record:${recordId ?: title}"
        SearchKind.TOOL -> "tool:$moduleId"
    }
}

data class WorkspaceTool(
    val id: String,
    val label: String,
    val group: String,
    val description: String,
    val adminOnly: Boolean = false,
)

val workspaceTools: List<WorkspaceTool> = listOf(
    WorkspaceTool("trace", "Trace", "Command", "Search an invoice, plate, lot, employee, or document across every desk."),
    WorkspaceTool("analytics", "Analytics", "Command", "Record counts and activity by department.", adminOnly = true),
    WorkspaceTool("accounting-documents", "Accounting documents", "Records", "Sales, purchase, payroll, and payment documents in one pack."),
    WorkspaceTool("templates", "Templates", "Records", "Reusable request and document templates."),
    WorkspaceTool("payment-requests", "Approval desks", "Requests", "Open requests waiting on a desk, plus your own returned items."),
    WorkspaceTool("comms", "Comms", "People", "Email and SMS sent from the ERP."),
    WorkspaceTool("guides", "Guides", "Help", "How desks, approvals, and segregation of duties work."),
    WorkspaceTool("qna", "Q&A", "Help", "Common questions from operators."),
    WorkspaceTool("release-notes", "Release notes", "Help", "What shipped in this app."),
    WorkspaceTool("activity-logs", "Activity", "Admin", "Who changed records.", adminOnly = true),
    WorkspaceTool("system-health", "System health", "Admin", "Local store, roles, and record counts.", adminOnly = true),
    WorkspaceTool("settings", "Settings", "Admin", "Theme and workspace options.", adminOnly = true),
)

val defaultKpis: List<Kpi> = listOf(
    Kpi("Assets", "UGX 2.4B", "Balance sheet"),
    Kpi("Liabilities", "UGX 890M", "Payables + loans", up = false),
    Kpi("Equity", "UGX 1.5B", "Capital + retained"),
    Kpi("Net profit", "UGX 124M", "YTD vs last year"),
)
