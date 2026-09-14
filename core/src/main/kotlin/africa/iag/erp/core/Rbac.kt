package africa.iag.erp.core

/** Platform SoD — same rules as the Next.js access-control layer. */
data class Crud(
    val view: Boolean,
    val create: Boolean,
    val edit: Boolean,
    val delete: Boolean,
) {
    val any: Boolean get() = view || create || edit || delete

    fun toFlags(): Map<String, String> = mapOf(
        "canView" to yesNo(view),
        "canCreate" to yesNo(create),
        "canEdit" to yesNo(edit),
        "canDelete" to yesNo(delete),
    )

    companion object {
        val none = Crud(view = false, create = false, edit = false, delete = false)
        val full = Crud(view = true, create = true, edit = true, delete = true)

        fun fromFlags(flags: Map<String, Any?>?): Crud {
            fun yes(value: Any?): Boolean =
                Regex("^yes$", RegexOption.IGNORE_CASE).matches("${value ?: "No"}")
            return Crud(
                view = yes(flags?.get("canView")),
                create = yes(flags?.get("canCreate")),
                edit = yes(flags?.get("canEdit")),
                delete = yes(flags?.get("canDelete")),
            )
        }
    }
}

internal fun yesNo(value: Boolean): String = if (value) "Yes" else "No"

const val PAGE_WILDCARD_KEY = "*"

data class RoleDefinition(
    val id: String,
    val name: String,
    val description: String = "",
    val crud: Crud = Crud.none,
    val system: Boolean = false,
    val pagePermissions: Map<String, Crud> = emptyMap(),
) {
    val restrictToGrantedApps: Boolean get() = pagePermissions[PAGE_WILDCARD_KEY] != null

    fun copyWith(
        name: String? = null,
        description: String? = null,
        crud: Crud? = null,
        pagePermissions: Map<String, Crud>? = null,
    ) = RoleDefinition(
        id = id,
        name = name ?: this.name,
        description = description ?: this.description,
        crud = crud ?: this.crud,
        system = system,
        pagePermissions = pagePermissions ?: this.pagePermissions,
    )

    fun toJson(): Map<String, Any?> = linkedMapOf(
        "id" to id,
        "name" to name,
        "description" to description,
        "system" to system,
        "canView" to crud.toFlags()["canView"],
        "canCreate" to crud.toFlags()["canCreate"],
        "canEdit" to crud.toFlags()["canEdit"],
        "canDelete" to crud.toFlags()["canDelete"],
        "pagePermissions" to pagePermissions.mapValues { it.value.toFlags() },
    )

    companion object {
        fun fromJson(j: Map<String, Any?>): RoleDefinition {
            val pages = linkedMapOf<String, Crud>()
            val raw = j["pagePermissions"]
            if (raw is Map<*, *>) {
                for ((k, v) in raw) {
                    pages[k.toString()] = Crud.fromFlags(
                        if (v is Map<*, *>) v.entries.associate { it.key.toString() to it.value } else null,
                    )
                }
            }
            val id = (j["id"] as? String)?.trim().orEmpty()
            return RoleDefinition(
                id = if (id.isNotEmpty()) id else newRoleId(),
                name = (j["name"] as? String)?.trim().orEmpty(),
                description = (j["description"] as? String)?.trim().orEmpty(),
                crud = Crud.fromFlags(j),
                system = j["system"] == true,
                pagePermissions = pages,
            )
        }
    }
}

data class WorkspaceUser(
    val username: String,
    val name: String,
    val role: String,
    val title: String = "",
    val email: String = "",
    val phone: String = "",
) {
    fun toJson(): Map<String, Any?> = linkedMapOf(
        "username" to username,
        "name" to name,
        "role" to role,
        "title" to title,
        "email" to email,
        "phone" to phone,
    )

    companion object {
        fun fromJson(j: Map<String, Any?>) = WorkspaceUser(
            username = (j["username"] as? String ?: "").trim().lowercase(),
            name = (j["name"] as? String ?: "").trim(),
            role = (j["role"] as? String ?: "Viewer").trim(),
            title = (j["title"] as? String ?: "").trim(),
            email = (j["email"] as? String ?: "").trim(),
            phone = (j["phone"] as? String ?: "").trim(),
        )
    }
}

fun newRoleId(): String = "role-${System.nanoTime()}"

val builtInRoleCatalog: List<Pair<String, String>> = listOf(
    "Administrator" to "Full CRUD across the workspace.",
    "Super Admin" to "Full system access — same as Administrator.",
    "Quantity Surveyor" to "QS desk on the material path.",
    "Project Manager" to "PM desk and assigned projects.",
    "Accounts Assistant" to "Accounts desk on the payment path.",
    "Department Head" to "HOD desk on leave requests.",
    "HR" to "HR desk on leave requests.",
    "General Manager" to "GM desk on the payment path.",
    "CEO" to "CEO desk before Finance pays.",
    "Finance" to "Make payment after CEO.",
    "Stores Manager" to "Stores desk on the material path.",
    "Procurement" to "Procurement follow-up after payment.",
    "Accountant" to "Accounts desk, including Make payment.",
    "Contractor" to "Projects and Contract Manager only.",
    "Reviewer" to "Notify only — no approval desk.",
    "Approver" to "Notify only — no approval desk.",
    "Clerk" to "View and create day-to-day documents.",
    "Viewer" to "Inquiry only.",
)

fun systemRoleDefinitions(): List<RoleDefinition> =
    builtInRoleCatalog.map { (name, description) ->
        RoleDefinition(
            id = "role-${normalizeRole(name).replace(" ", "-")}",
            name = name,
            description = description,
            crud = crudForRole(name),
            system = true,
        )
    }

fun isBuiltInRoleName(name: String?): Boolean {
    val key = normalizeRole(name)
    return builtInRoleCatalog.any { normalizeRole(it.first) == key }
}

fun findRoleDefinition(roles: List<RoleDefinition>, name: String?): RoleDefinition? {
    val key = normalizeRole(name)
    if (key.isEmpty()) return null
    return roles.firstOrNull { normalizeRole(it.name) == key }
}

fun mergeStoredRoles(stored: List<RoleDefinition>): List<RoleDefinition> {
    val custom = stored.filter {
        !it.system &&
            it.name.trim().isNotEmpty() &&
            !isAdminRole(it.name) &&
            !isBuiltInRoleName(it.name)
    }
    return systemRoleDefinitions() + custom
}

data class DemoAccount(
    val username: String,
    val name: String,
    val role: String,
    val title: String,
    val email: String = "",
    val phone: String = "",
)

/** Specialty departments with no CRUD fallback — admin only unless granted. */
val explicitGrantModules: Set<String> = setOf(
    "rnd",
    "lab",
    "qa",
    "production",
    "benchmark",
    "crm",
    "logistics",
    "distribution",
    "fleet",
    "security",
    "investments",
    "assets",
    "capital",
    "contract-manager",
)

val contractorModules: Set<String> = setOf("projects", "contract-manager")

val demoAccounts: List<DemoAccount> = listOf(
    DemoAccount("admin", "Administrator", "Administrator", "Finance administrator", "admin@iag.africa", "+256 700 000 001"),
    DemoAccount("superadmin", "Super Admin", "Super Admin", "Platform owner", "superadmin@iag.africa", "+256 700 000 000"),
    DemoAccount("accountant", "Amina Accountant", "Accountant", "Accounts desk", "accountant@iag.africa"),
    DemoAccount("aa", "Alex Assistant", "Accounts Assistant", "Accounts desk", "aa@iag.africa"),
    DemoAccount("finance", "Fiona Finance", "Finance", "Make payment desk", "finance@iag.africa"),
    DemoAccount("clerk", "Chris Clerk", "Clerk", "Day-to-day documents", "clerk@iag.africa"),
    DemoAccount("viewer", "Vera Viewer", "Viewer", "Inquiry only", "viewer@iag.africa"),
    DemoAccount("hr", "Hannah HR", "HR", "HR desk", "hr@iag.africa"),
    DemoAccount("hod", "Daniel Head", "Department Head", "HOD desk", "hod@iag.africa"),
    DemoAccount("pm", "Patricia Manager", "Project Manager", "PM desk", "pm@iag.africa"),
    DemoAccount("contractor", "Carl Contractor", "Contractor", "Assigned projects", "contractor@iag.africa"),
    DemoAccount("gm", "Grace GM", "General Manager", "GM desk", "gm@iag.africa"),
    DemoAccount("ceo", "Cynthia CEO", "CEO", "CEO desk", "ceo@iag.africa"),
    DemoAccount("qs", "Quentin Surveyor", "Quantity Surveyor", "QS desk", "qs@iag.africa"),
    DemoAccount("stores", "Sam Stores", "Stores Manager", "Stores desk", "stores@iag.africa"),
    DemoAccount("procurement", "Paula Procurement", "Procurement", "Procurement desk", "procurement@iag.africa"),
    DemoAccount("reviewer", "Riley Reviewer", "Reviewer", "Notify only", "reviewer@iag.africa"),
    DemoAccount("approver", "Ava Approver", "Approver", "Notify only", "approver@iag.africa"),
)

fun demoAccountFor(username: String): DemoAccount? {
    val u = username.trim().lowercase()
    return demoAccounts.firstOrNull { it.username == u }
}

fun normalizeRole(role: String?): String =
    (role ?: "").trim().lowercase().replace(Regex("\\s+"), " ")

fun isAdminRole(role: String?): Boolean {
    val r = normalizeRole(role)
    return r == "administrator" || r == "super admin" || r == "superadmin"
}

fun isContractorRole(role: String?): Boolean = normalizeRole(role) == "contractor"

fun crudForRole(role: String?, definition: RoleDefinition? = null): Crud {
    if (isAdminRole(role) || isAdminRole(definition?.name)) return Crud.full
    if (definition != null && !definition.system) return definition.crud
    return when (normalizeRole(role ?: definition?.name)) {
        "project manager",
        "accounts assistant",
        "department head",
        "hr",
        "human resources",
        "general manager",
        "ceo",
        "finance",
        "accountant",
        "quantity surveyor",
        "stores manager",
        "procurement",
        "contractor",
        -> Crud(view = true, create = true, edit = true, delete = false)
        "reviewer",
        "approver",
        "clerk",
        -> Crud(view = true, create = true, edit = false, delete = false)
        else -> Crud(view = true, create = false, edit = false, delete = false)
    }
}

fun canAccessApprovalDesk(role: String?): Boolean {
    if (isAdminRole(role)) return true
    return when (normalizeRole(role)) {
        "quantity surveyor",
        "stores manager",
        "procurement",
        "hr",
        "human resources",
        "department head",
        "ceo",
        "general manager",
        "accounts assistant",
        "accountant",
        "accounts",
        "project manager",
        "finance",
        -> true
        else -> false
    }
}

fun canAccessModule(role: String?, slug: String, definition: RoleDefinition? = null): Boolean {
    if (isAdminRole(role) || isAdminRole(definition?.name)) return true

    val pages = definition?.pagePermissions ?: emptyMap()
    if (pages.containsKey(slug)) return pages[slug]!!.view
    if (pages.containsKey(PAGE_WILDCARD_KEY)) return pages[PAGE_WILDCARD_KEY]!!.view

    if (isContractorRole(role) && slug != "clock-in") return slug in contractorModules

    val crud = crudForRole(role, definition)
    if (slug == "clock-in" || slug == "requests" || slug == "general-requests" || slug == "oral-payment-requests") {
        return crud.view
    }
    if (slug in explicitGrantModules) return false
    if (!crud.view) return false

    return when (slug) {
        "payroll" -> crud.create && crud.edit
        "reports", "accounts", "documents", "folders" -> crud.view
        "banking", "sales", "purchases" -> crud.create || crud.edit
        "receipts-payments", "expense-claims", "inventory", "pos", "projects" -> crud.create
        else -> crud.view
    }
}

fun canViewEntity(role: String?, moduleId: String, definition: RoleDefinition? = null): Boolean =
    canAccessModule(role, moduleId, definition)

fun canCreateIn(role: String?, moduleId: String, definition: RoleDefinition? = null): Boolean {
    if (!canAccessModule(role, moduleId, definition)) return false
    if (moduleId == "clock-in") return true
    val pages = definition?.pagePermissions ?: emptyMap()
    if (pages.containsKey(moduleId)) return pages[moduleId]!!.create
    if (pages.containsKey(PAGE_WILDCARD_KEY)) return pages[PAGE_WILDCARD_KEY]!!.create
    return crudForRole(role, definition).create
}

fun canEditIn(role: String?, moduleId: String, definition: RoleDefinition? = null): Boolean {
    if (!canAccessModule(role, moduleId, definition)) return false
    val pages = definition?.pagePermissions ?: emptyMap()
    if (pages.containsKey(moduleId)) return pages[moduleId]!!.edit
    if (pages.containsKey(PAGE_WILDCARD_KEY)) return pages[PAGE_WILDCARD_KEY]!!.edit
    return crudForRole(role, definition).edit
}

fun canDeleteIn(role: String?, moduleId: String, definition: RoleDefinition? = null): Boolean {
    if (!canAccessModule(role, moduleId, definition)) return false
    val pages = definition?.pagePermissions ?: emptyMap()
    if (pages.containsKey(moduleId)) return pages[moduleId]!!.delete
    if (pages.containsKey(PAGE_WILDCARD_KEY)) return pages[PAGE_WILDCARD_KEY]!!.delete
    return crudForRole(role, definition).delete
}

fun canApproveIn(role: String?, moduleId: String, definition: RoleDefinition? = null): Boolean {
    if (!canAccessModule(role, moduleId, definition)) return false
    return canAccessApprovalDesk(role)
}

fun canVoidIn(role: String?, moduleId: String, definition: RoleDefinition? = null): Boolean {
    if (!canAccessModule(role, moduleId, definition)) return false
    if (isAdminRole(role)) return true
    val r = normalizeRole(role)
    return r == "finance" || r == "accountant" || r == "accounts assistant" || r == "accounts"
}

fun canRunPayroll(role: String?): Boolean {
    if (isAdminRole(role)) return true
    val r = normalizeRole(role)
    return r == "hr" || r == "human resources" || r == "accountant" || r == "finance"
}

val adminOnlySpecialNav: Set<String> = setOf(
    "settings", "users", "request-emails", "activity-logs", "crash-analytics", "system-health", "analytics",
)

fun canAccessSpecialNav(role: String?, key: String, definition: RoleDefinition? = null): Boolean {
    if (isAdminRole(role) || isAdminRole(definition?.name)) return true
    if (key == "profile") return true
    if (key in adminOnlySpecialNav) return false
    val pages = definition?.pagePermissions ?: emptyMap()
    pages[key]?.let { return it.view }
    val crud = crudForRole(role, definition)
    return when (key) {
        "dashboard", "trace", "clock-in", "guides", "qna", "release-notes", "templates", "comms", "accounting-documents", "payment-requests" -> crud.view
        else -> false
    }
}

fun canManageGeofence(role: String?): Boolean {
    if (isAdminRole(role)) return true
    val r = normalizeRole(role)
    return r == "hr" || r == "human resources"
}

fun isReconEntity(entity: String): Boolean = entity.lowercase().contains("reconcil")

fun isPayrollRunEntity(entity: String): Boolean {
    val e = entity.lowercase()
    return e == "payroll runs" ||
        e == "create payroll" ||
        e == "payslip items" ||
        e == "recurring payslips" ||
        e == "statutory remittances"
}

fun isGeofenceEntity(entity: String): Boolean = entity == "Sites" || entity == "Blocks"

fun canCreateEntity(
    role: String?,
    moduleId: String,
    entity: String,
    definition: RoleDefinition? = null,
): Boolean {
    if (!canCreateIn(role, moduleId, definition)) return false
    if (isReconEntity(entity)) return canVoidIn(role, moduleId, definition)
    if (isPayrollRunEntity(entity)) return canRunPayroll(role)
    if (isGeofenceEntity(entity)) return canManageGeofence(role)
    return true
}

fun canEditEntity(
    role: String?,
    moduleId: String,
    entity: String,
    definition: RoleDefinition? = null,
): Boolean {
    if (!canEditIn(role, moduleId, definition)) return false
    if (isReconEntity(entity)) return canVoidIn(role, moduleId, definition)
    if (isPayrollRunEntity(entity)) return canRunPayroll(role)
    if (isGeofenceEntity(entity)) return canManageGeofence(role)
    return true
}

fun canDeleteEntity(
    role: String?,
    moduleId: String,
    entity: String,
    definition: RoleDefinition? = null,
): Boolean {
    if (!canDeleteIn(role, moduleId, definition)) return false
    if (isReconEntity(entity)) return canVoidIn(role, moduleId, definition)
    if (isPayrollRunEntity(entity)) return canRunPayroll(role)
    if (isGeofenceEntity(entity)) return canManageGeofence(role)
    return true
}

fun isDraftStatus(status: String): Boolean = status.trim().lowercase() == "draft"

fun isVoidedStatus(status: String): Boolean {
    val s = status.trim().lowercase()
    return s == "void" || s == "voided"
}

fun isApprovedStatus(status: String): Boolean {
    val s = status.trim().lowercase()
    return s == "approved" || s == "posted" || s == "paid"
}

fun defaultDepartmentForRole(role: String?): String? {
    if (isContractorRole(role)) return "projects"
    return when (normalizeRole(role)) {
        "quantity surveyor", "project manager" -> "projects"
        else -> null
    }
}
