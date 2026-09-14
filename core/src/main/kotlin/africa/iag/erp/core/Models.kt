package africa.iag.erp.core

const val APP_NAME = "IAG Central"
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

data class WelcomeStat(
    val id: String,
    val label: String,
    val value: String,
)

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

data class SuiteApp(
    val id: String,
    val label: String,
    val description: String,
    val moduleIds: List<String>,
    val icon: String,
    val color: Long,
)

val suiteApps: List<SuiteApp> = listOf(
    SuiteApp("finance", "Finance", "Banking, receipts, claims, accounts, reports, assets, capital, and investments.", listOf("banking", "receipts-payments", "expense-claims", "accounts", "reports", "investments", "assets", "capital"), "account_balance", 0xFF0369A1),
    SuiteApp("sales", "Sales", "Customers, quotes, orders, invoices, and credit notes.", listOf("sales"), "storefront", 0xFF059669),
    SuiteApp("crm", "CRM", "Leads, accounts, opportunities, follow-ups, and complaints.", listOf("crm"), "groups", 0xFFBE123C),
    SuiteApp("pos", "POS", "Tills, tickets, KOTs, cash sessions, and daily closings.", listOf("pos"), "point_of_sale", 0xFF0F766E),
    SuiteApp("procurement", "Procurement", "Suppliers, purchase documents, goods receipts, and inventory.", listOf("purchases", "inventory"), "shopping_cart", 0xFF2563EB),
    SuiteApp("production", "Production", "Plans, machines, batches, roast, packaging, downtime, and yield.", listOf("production"), "precision_manufacturing", 0xFFB45309),
    SuiteApp("fleet", "Fleet", "Vehicles, drivers, fuel, trips, and maintenance.", listOf("fleet"), "directions_car", 0xFFD97706),
    SuiteApp("logistics", "Logistics", "Shipments, dispatch, routes, distribution, and deliveries.", listOf("logistics", "distribution"), "local_shipping", 0xFF0F766E),
    SuiteApp("projects", "Projects", "Projects, Gantt, IPC, materials, and work programs.", listOf("projects"), "work", 0xFF4F46E5),
    SuiteApp("contracts", "Contract Management", "Contracts, contractors, amendments, invoices, and bonds.", listOf("contract-manager"), "handshake", 0xFF047857),
    SuiteApp("hr", "HR & Payroll", "Employees, attendance, leave, payroll runs, and payslips.", listOf("payroll"), "badge", 0xFF7C3AED),
    SuiteApp("security", "Security", "Gate passes, visitor passes, and security incidents.", listOf("security"), "security", 0xFF334155),
    SuiteApp("quality", "Quality", "R&D, lab, QA, and work-system benchmarks.", listOf("rnd", "lab", "qa", "benchmark"), "science", 0xFF6D28D9),
    SuiteApp("requests", "Requests", "General and oral payment requests through the approval desks.", listOf("general-requests", "oral-payment-requests"), "assignment", 0xFF7C3AED),
    SuiteApp("dms", "DMS", "Cabinets, folders, documents, versions, and file shares.", listOf("folders", "documents"), "folder", 0xFFA16207),
)

fun suiteAppById(id: String?): SuiteApp? {
    if (id.isNullOrEmpty()) return null
    val key = when (id) {
        "records" -> "dms"
        "contract-manager", "contract-management" -> "contracts"
        else -> id
    }
    return suiteApps.firstOrNull { it.id == key }
}

fun suiteAppContaining(moduleId: String): SuiteApp? = suiteApps.firstOrNull { moduleId in it.moduleIds }

fun canOpenSuiteApp(role: String?, appId: String, definition: RoleDefinition? = null): Boolean {
    val app = suiteAppById(appId) ?: return false
    return app.moduleIds.any { canAccessModule(role, it, definition) }
}

fun defaultSuiteAppForRole(role: String?): String? {
    if (isAdminRole(role)) return null
    if (isContractorRole(role)) return "projects"
    return when (normalizeRole(role)) {
        "quantity surveyor", "project manager" -> "projects"
        "procurement", "stores manager" -> "procurement"
        "hr", "human resources" -> "hr"
        "accountant", "accounts assistant", "accounts", "finance", "clerk" -> "finance"
        "viewer" -> "finance"
        else -> null
    }
}

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
    WorkspaceTool("clock-in", "Clock In", "People", "GPS clock-in and clock-out against Sites and Blocks."),
    WorkspaceTool("guides", "Guides", "Help", "How desks, approvals, and segregation of duties work."),
    WorkspaceTool("qna", "Q&A", "Help", "Common questions from operators."),
    WorkspaceTool("release-notes", "Release notes", "Help", "What shipped in this app."),
    WorkspaceTool("activity-logs", "Activity", "Admin", "Who changed records.", adminOnly = true),
    WorkspaceTool("system-health", "System health", "Admin", "Local store, roles, and record counts.", adminOnly = true),
    WorkspaceTool("settings", "Settings", "Admin", "Theme and workspace options.", adminOnly = true),
)

enum class QuickActionKind { CREATE, LIST, CLOCK, APPROVALS, ACCESS }

data class QuickAction(
    val id: String,
    val label: String,
    val icon: String,
    val color: Long,
    val kind: QuickActionKind,
    val appId: String? = null,
    val moduleId: String,
    val entity: String? = null,
)

val quickActionCatalog: List<QuickAction> = listOf(
    QuickAction("clock", "Clock in", "clock", 0xFF047857, QuickActionKind.CLOCK, moduleId = "clock-in"),
    QuickAction("approvals", "Approvals", "approvals", 0xFFC47820, QuickActionKind.APPROVALS, moduleId = "general-requests"),
    QuickAction("access", "Users", "access", 0xFF334155, QuickActionKind.ACCESS, moduleId = "payroll"),
    QuickAction("receipt", "Receipt", "receipt", 0xFF0F766E, QuickActionKind.CREATE, "finance", "receipts-payments", "Receipts"),
    QuickAction("payment", "Payment", "payment", 0xFF0369A1, QuickActionKind.CREATE, "finance", "receipts-payments", "Payments"),
    QuickAction("claim", "Claim", "claim", 0xFFB45309, QuickActionKind.CREATE, "finance", "expense-claims", "Expense Claims"),
    QuickAction("journal", "Journal", "journal", 0xFF0F172A, QuickActionKind.CREATE, "finance", "accounts", "Journal Entries"),
    QuickAction("reports", "Reports", "reports", 0xFF0369A1, QuickActionKind.LIST, "finance", "reports", "Balance Sheet"),
    QuickAction("transfer", "Transfer", "transfer", 0xFF0369A1, QuickActionKind.CREATE, "finance", "banking", "Inter Account Transfers"),
    QuickAction("po", "New PO", "po", 0xFF2563EB, QuickActionKind.CREATE, "procurement", "purchases", "Purchase Orders"),
    QuickAction("grn", "GRN", "grn", 0xFF0E7490, QuickActionKind.CREATE, "procurement", "purchases", "Goods Receipts"),
    QuickAction("supplier", "Supplier", "supplier", 0xFF2563EB, QuickActionKind.CREATE, "procurement", "purchases", "Suppliers"),
    QuickAction("item", "Item", "item", 0xFF0E7490, QuickActionKind.CREATE, "procurement", "inventory", "Inventory Items"),
    QuickAction("prod-order", "Order", "prod", 0xFFB45309, QuickActionKind.CREATE, "production", "production", "Production Orders"),
    QuickAction("batch", "Batch", "batch", 0xFFB45309, QuickActionKind.CREATE, "production", "production", "Batch Records"),
    QuickAction("roast", "Roast", "roast", 0xFFC2410C, QuickActionKind.CREATE, "production", "production", "Roast Batches"),
    QuickAction("downtime", "Down", "down", 0xFF57534E, QuickActionKind.CREATE, "production", "production", "Downtime Logs"),
    QuickAction("gate", "Gate", "gate", 0xFF334155, QuickActionKind.CREATE, "security", "security", "Gate Passes"),
    QuickAction("visitor", "Visitor", "visitor", 0xFF334155, QuickActionKind.CREATE, "security", "security", "Visitor Passes"),
    QuickAction("incident", "Incident", "incident", 0xFFB91C1C, QuickActionKind.CREATE, "security", "security", "Security Incidents"),
    QuickAction("leave", "Leave", "leave", 0xFF7C3AED, QuickActionKind.CREATE, "hr", "payroll", "Leave Requests"),
    QuickAction("employee", "Staff", "staff", 0xFF7C3AED, QuickActionKind.CREATE, "hr", "payroll", "Employees"),
    QuickAction("payroll", "Payroll", "payroll", 0xFF7C3AED, QuickActionKind.CREATE, "hr", "payroll", "Payroll Runs"),
    QuickAction("project", "Project", "project", 0xFF4F46E5, QuickActionKind.CREATE, "projects", "projects", "New Project"),
    QuickAction("ipc", "IPC", "ipc", 0xFF4F46E5, QuickActionKind.CREATE, "projects", "projects", "Payment Requests (IPC)"),
    QuickAction("material", "Material", "material", 0xFF4F46E5, QuickActionKind.CREATE, "projects", "projects", "Material Requests"),
    QuickAction("contractor", "Contractor", "contractor", 0xFF047857, QuickActionKind.CREATE, "contracts", "contract-manager", "Contractors"),
    QuickAction("contract", "Contract", "contract", 0xFF047857, QuickActionKind.CREATE, "contracts", "contract-manager", "Contracts"),
    QuickAction("fuel", "Fuel", "fuel", 0xFFD97706, QuickActionKind.CREATE, "fleet", "fleet", "Fuel Requests"),
    QuickAction("trip", "Trip", "trip", 0xFFD97706, QuickActionKind.CREATE, "fleet", "fleet", "Trip Requests"),
    QuickAction("maintenance", "Service", "service", 0xFFD97706, QuickActionKind.CREATE, "fleet", "fleet", "Maintenance Requests"),
    QuickAction("vehicle", "Vehicle", "vehicle", 0xFFD97706, QuickActionKind.CREATE, "fleet", "fleet", "Vehicles"),
    QuickAction("invoice", "Invoice", "invoice", 0xFF059669, QuickActionKind.CREATE, "sales", "sales", "Sales Invoices"),
    QuickAction("customer", "Customer", "customer", 0xFF059669, QuickActionKind.CREATE, "sales", "sales", "Customers"),
    QuickAction("quote", "Quote", "quote", 0xFF059669, QuickActionKind.CREATE, "sales", "sales", "Sales Quotes"),
    QuickAction("lead", "Lead", "lead", 0xFFBE123C, QuickActionKind.CREATE, "crm", "crm", "Leads"),
    QuickAction("opportunity", "Deal", "opportunity", 0xFFBE123C, QuickActionKind.CREATE, "crm", "crm", "Opportunities"),
    QuickAction("ticket", "Ticket", "ticket", 0xFF0F766E, QuickActionKind.CREATE, "pos", "pos", "Open Tickets"),
    QuickAction("pos-sale", "Sale", "pos-sale", 0xFF0F766E, QuickActionKind.CREATE, "pos", "pos", "POS Sales"),
    QuickAction("session", "Shift", "session", 0xFF0F766E, QuickActionKind.CREATE, "pos", "pos", "Cash Sessions"),
    QuickAction("shipment", "Ship", "ship", 0xFF0F766E, QuickActionKind.CREATE, "logistics", "logistics", "Shipments"),
    QuickAction("dispatch", "Dispatch", "dispatch", 0xFF0F766E, QuickActionKind.LIST, "logistics", "logistics", "Dispatch Board"),
    QuickAction("delivery", "Deliver", "deliver", 0xFF0D9488, QuickActionKind.CREATE, "logistics", "distribution", "Delivery Runs"),
    QuickAction("lab", "Lab", "lab", 0xFF6D28D9, QuickActionKind.CREATE, "quality", "lab", "Lab Requests"),
    QuickAction("qa", "QA", "qa", 0xFF0369A1, QuickActionKind.CREATE, "quality", "qa", "Quality Checks"),
    QuickAction("nc", "NC", "nc", 0xFFB91C1C, QuickActionKind.CREATE, "quality", "qa", "Non-conformances"),
    QuickAction("gen-request", "Request", "request", 0xFF7C3AED, QuickActionKind.CREATE, "requests", "general-requests", "General Requests"),
    QuickAction("oral", "Oral pay", "oral", 0xFFC2410C, QuickActionKind.CREATE, "requests", "oral-payment-requests", "Oral Payment Requests"),
    QuickAction("folder", "Folder", "folder", 0xFFA16207, QuickActionKind.CREATE, "dms", "folders", "Folders"),
    QuickAction("attachment", "File", "file", 0xFF57534E, QuickActionKind.LIST, "dms", "documents", "Documents"),
)

val defaultKpis: List<Kpi> = listOf(
    Kpi("Assets", "UGX 2.4B", "Balance sheet"),
    Kpi("Liabilities", "UGX 890M", "Payables + loans", up = false),
    Kpi("Equity", "UGX 1.5B", "Capital + retained"),
    Kpi("Net profit", "UGX 124M", "YTD vs last year"),
)
