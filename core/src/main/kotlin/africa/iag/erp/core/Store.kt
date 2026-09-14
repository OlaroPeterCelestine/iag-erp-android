package africa.iag.erp.core

const val STORE_KEY = "iag-erp-android-v1"

class ErpStore(
    val modules: List<ErpModule> = erpModules(),
    private val persistence: KeyValueStore = MemoryKeyValueStore(),
) {
    var user: AuthUser? = null
        private set
    var records: MutableList<ErpRecord> = mutableListOf()
    var roles: List<RoleDefinition> = systemRoleDefinitions()
    var workspaceUsers: List<WorkspaceUser> = emptyList()
    var search: String = ""
    var booted: Boolean = false
        private set
    var activeDepartmentId: String? = null
        private set
    var themeMode: String = "system"
        private set
    private val passwords = linkedMapOf<String, String>()
    private val listeners = mutableListOf<() -> Unit>()

    val isSignedIn: Boolean get() = user != null
    val roleName: String? get() = user?.role
    val isAdmin: Boolean get() = isAdminRole(roleName)
    val currentRole: RoleDefinition? get() = roleOf(roleName)
    val kpis: List<Kpi> get() = defaultKpis

    val visibleModules: List<ErpModule>
        get() = modules.filter { canAccessModule(roleName, it.id, currentRole) }

    val visibleWorkspaceTools: List<WorkspaceTool>
        get() = workspaceTools.filter { canAccessSpecialNav(roleName, it.id, currentRole) }

    val approvalEntities: Set<String>
        get() = modules.flatMap { it.approvalEntities }.toSet()

    val canApprove: Boolean get() = canAccessApprovalDesk(roleName)
    val canPayroll: Boolean get() = canRunPayroll(roleName)
    val canFence: Boolean get() = canManageGeofence(roleName)

    val customRoles: List<RoleDefinition> get() = roles.filter { !it.system }

    val pendingApprovals: List<ErpRecord> get() = pendingApprovalsFor(null)

    val recent: List<ErpRecord> get() = records.take(8)

    val accountingDocuments: List<ErpRecord>
        get() {
            val needles = listOf("invoice", "quote", "order", "note", "receipt", "payment", "payslip", "journal", "transfer", "claim", "request")
            return records.filter { rec ->
                canOpen(rec.moduleId) && needles.any { rec.entity.lowercase().contains(it) }
            }
        }

    fun count(moduleId: String, entity: String): Int = recordsFor(moduleId, entity).size

    fun recordsFor(moduleId: String, entity: String): List<ErpRecord> {
        if (entity == "My punches") return myPunches()
        if (entity == "Punch Log" && moduleId == "clock-in") return forEntity("payroll", "Punch Log")
        return forEntity(moduleId, entity)
    }

    fun searchHits(query: String): List<SearchHit> {
        val q = query.trim().lowercase()
        if (q.length < 2) return emptyList()
        val hits = mutableListOf<SearchHit>()
        for (module in visibleModules) {
            if (module.label.lowercase().contains(q) || module.description.lowercase().contains(q)) {
                hits += SearchHit(SearchKind.MODULE, module.label, module.group, module.id)
            }
            for (entity in module.entities) {
                if (entity.lowercase().contains(q)) {
                    hits += SearchHit(SearchKind.ENTITY, entity, module.label, module.id, entity = entity)
                }
            }
        }
        for (tool in visibleWorkspaceTools) {
            if (tool.label.lowercase().contains(q) || tool.description.lowercase().contains(q)) {
                hits += SearchHit(SearchKind.TOOL, tool.label, tool.group, tool.id)
            }
        }
        for (rec in records) {
            if (!canOpen(rec.moduleId)) continue
            if (rec.title.lowercase().contains(q) || rec.subtitle.lowercase().contains(q) || rec.entity.lowercase().contains(q)) {
                hits += SearchHit(SearchKind.RECORD, rec.title, "${rec.entity} · ${rec.status}", rec.moduleId, entity = rec.entity, recordId = rec.id)
            }
            if (hits.size >= 40) break
        }
        return hits.take(30)
    }

    val canClockIn: Boolean
        get() = user != null && crudForRole(roleName, currentRole).view

    fun geofenceZones(): List<GeofenceZone> {
        val sites = forEntity("payroll", "Sites").mapNotNull { zoneFromRecord(it, "site") }
        val blocks = forEntity("payroll", "Blocks").mapNotNull { zoneFromRecord(it, "block") }
        return blocks + sites
    }

    fun myPunches(): List<ErpRecord> {
        val name = user?.name?.trim().orEmpty()
        return forEntity("payroll", "Attendance").filter {
            recordField(it, "employee", "Employee") == name || it.subtitle.contains(name) || it.title.contains(name)
        }
    }

    fun openAttendanceToday(): ErpRecord? {
        val today = todayIsoDate()
        return myPunches().firstOrNull {
            it.date == today &&
                recordField(it, "clockIn", "Clock in").isNotEmpty() &&
                recordField(it, "clockOut", "Clock out").isEmpty()
        }
    }

    fun punch(kind: String, latitude: Double, longitude: Double, accuracy: Double): String? {
        if (!canClockIn) return "Sign in to clock in."
        val check = verifyAgainstZones(GeoPoint(latitude, longitude), geofenceZones(), accuracy)
        val name = user?.name ?: "Staff"
        val clock = nowClock()
        val today = todayIsoDate()
        if (check.status == "Outside") {
            addPunchLog(kind, name, check, latitude, longitude, accuracy)
            return check.note
        }
        if (kind == "in") {
            if (openAttendanceToday() != null) return "You already have an open check-in today. Clock out first."
            val site = if (check.zone?.kind == "site") check.zone?.name.orEmpty() else check.zone?.siteName.orEmpty()
            val block = if (check.zone?.kind == "block") check.zone?.name.orEmpty() else ""
            insertRecord(
                ErpRecord(
                    id = newId(),
                    moduleId = "payroll",
                    entity = "Attendance",
                    title = "ATT-${today.replace("-", "")}-${clock.replace(":", "")}",
                    subtitle = "$name · ${check.zone?.name ?: "On site"}",
                    status = "Present",
                    date = today,
                    fields = mapOf(
                        "employee" to name,
                        "site" to site,
                        "block" to block,
                        "clockIn" to clock,
                        "clockOut" to "",
                        "hours" to "",
                        "latitude" to "%.6f".format(latitude),
                        "longitude" to "%.6f".format(longitude),
                        "accuracyMeters" to "${accuracy.toInt()}",
                        "verification" to check.status,
                        "verificationNote" to check.note,
                    ),
                ),
            )
            return "Checked in at $clock · ${check.status} · ${check.note}"
        }
        val open = openAttendanceToday() ?: return "No open check-in found for today."
        val clockIn = recordField(open, "clockIn", "Clock in")
        open.subtitle = "$name · out $clock"
        open.fields = open.fields + mapOf(
            "clockOut" to clock,
            "hours" to hoursBetween(clockIn, clock),
            "latitude" to "%.6f".format(latitude),
            "longitude" to "%.6f".format(longitude),
            "accuracyMeters" to "${accuracy.toInt()}",
            "verification" to check.status,
        )
        persist()
        notifyChange()
        return "Checked out at $clock · ${check.status}"
    }

    private fun addPunchLog(kind: String, name: String, check: GeofenceCheck, latitude: Double, longitude: Double, accuracy: Double) {
        insertRecord(
            ErpRecord(
                id = newId(),
                moduleId = "payroll",
                entity = "Punch Log",
                title = "Rejected $kind · ${nowClock()}",
                subtitle = name,
                status = "Rejected",
                date = todayIsoDate(),
                fields = mapOf(
                    "employee" to name,
                    "kind" to kind,
                    "latitude" to "%.6f".format(latitude),
                    "longitude" to "%.6f".format(longitude),
                    "accuracyMeters" to "${accuracy.toInt()}",
                    "verification" to check.status,
                    "verificationNote" to check.note,
                ),
            ),
        )
    }

    fun addListener(listener: () -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyChange() {
        listeners.toList().forEach { it() }
    }

    fun roleOf(name: String?): RoleDefinition? = findRoleDefinition(roles, name)

    fun canOpen(moduleId: String): Boolean = canAccessModule(roleName, moduleId, currentRole)

    fun canCreate(moduleId: String, entity: String? = null): Boolean =
        if (entity == null) canCreateIn(roleName, moduleId, currentRole)
        else canCreateEntity(roleName, moduleId, entity, currentRole)

    fun canEdit(moduleId: String, entity: String? = null): Boolean =
        if (entity == null) canEditIn(roleName, moduleId, currentRole)
        else canEditEntity(roleName, moduleId, entity, currentRole)

    fun canDelete(moduleId: String, entity: String? = null): Boolean =
        if (entity == null) canDeleteIn(roleName, moduleId, currentRole)
        else canDeleteEntity(roleName, moduleId, entity, currentRole)

    fun canApproveModule(moduleId: String): Boolean = canApproveIn(roleName, moduleId, currentRole)

    fun canVoid(moduleId: String): Boolean = canVoidIn(roleName, moduleId, currentRole)

    fun passwordFor(username: String): String =
        passwords[username.trim().lowercase()] ?: DEMO_PASSWORD

    fun moduleById(id: String): ErpModule? = modules.firstOrNull { it.id == id }

    fun forEntity(moduleId: String, entity: String): List<ErpRecord> =
        records.filter { it.moduleId == moduleId && it.entity == entity }

    fun forModule(moduleId: String): List<ErpRecord> = records.filter { it.moduleId == moduleId }

    fun isOpenStatus(status: String): Boolean {
        val s = status.lowercase()
        return s.contains("pending") || s.contains("open") || s.contains("submitted")
    }

    fun pendingApprovalsFor(moduleId: String?): List<ErpRecord> {
        val rows = if (moduleId == null) records else forModule(moduleId)
        val def = currentRole
        return rows.filter {
            it.entity in approvalEntities &&
                isOpenStatus(it.status) &&
                canAccessModule(roleName, it.moduleId, def)
        }
    }

    fun filtered(moduleId: String, entity: String): List<ErpRecord> {
        val q = search.trim().lowercase()
        val list = forEntity(moduleId, entity)
        if (q.isEmpty()) return list
        return list.filter {
            it.title.lowercase().contains(q) ||
                it.subtitle.lowercase().contains(q) ||
                it.status.lowercase().contains(q)
        }
    }

    fun load() {
        val raw = persistence.get(STORE_KEY)
        if (raw == null) {
            records = seed().toMutableList()
            roles = systemRoleDefinitions()
            workspaceUsers = emptyList()
            persist()
            booted = true
            notifyChange()
            return
        }
        try {
            val j = MiniJson.parseObject(raw)
            user = (j["user"] as? Map<*, *>)?.let { AuthUser.fromJson(it.asStringMap()) }
            val dept = j["activeDepartmentId"] as? String
            activeDepartmentId = if (dept.isNullOrEmpty() || moduleById(dept) == null) null else dept
            val pw = j["passwords"] as? Map<*, *>
            passwords.clear()
            if (pw != null) {
                for ((k, v) in pw) passwords[k.toString().lowercase()] = v.toString()
            }
            readAccessLists(j)
            val loaded = mutableListOf<ErpRecord>()
            val rawRecords = j["records"] as? List<*>
            if (rawRecords != null) {
                for (item in rawRecords) {
                    val map = item as? Map<*, *> ?: continue
                    loaded.add(ErpRecord.fromJson(map.asStringMap()))
                }
            }
            records = if (loaded.isEmpty()) seed().toMutableList() else loaded
            mergeMissingCatalogRecords()
            enforceAccess()
            themeMode = when (j["themeMode"] as? String) {
                "light", "dark" -> j["themeMode"] as String
                else -> "system"
            }
        } catch (_: Exception) {
            records = seed().toMutableList()
            roles = mergeStoredRoles(roles)
        }
        booted = true
        notifyChange()
    }

    fun persist() {
        persistence.put(
            STORE_KEY,
            MiniJson.stringify(
                linkedMapOf(
                    "user" to user?.toJson(),
                    "activeDepartmentId" to activeDepartmentId,
                    "themeMode" to themeMode,
                    "passwords" to passwords,
                    "roles" to roles.map { it.toJson() },
                    "workspaceUsers" to workspaceUsers.map { it.toJson() },
                    "records" to records.map { it.toJson() },
                ),
            ),
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun readAccessLists(j: Map<String, Any?>) {
        val stored = mutableListOf<RoleDefinition>()
        val rawRoles = j["roles"] as? List<*>
        if (rawRoles != null) {
            for (item in rawRoles) {
                val map = item as? Map<*, *> ?: continue
                stored.add(RoleDefinition.fromJson(map.asStringMap()))
            }
        }
        roles = mergeStoredRoles(stored)
        val users = mutableListOf<WorkspaceUser>()
        val rawUsers = j["workspaceUsers"] as? List<*>
        if (rawUsers != null) {
            for (item in rawUsers) {
                val map = item as? Map<*, *> ?: continue
                val user = WorkspaceUser.fromJson(map.asStringMap())
                if (user.username.isNotEmpty() && demoAccountFor(user.username) == null) users.add(user)
            }
        }
        workspaceUsers = users
    }

    fun workspaceUserFor(username: String): WorkspaceUser? {
        val u = username.trim().lowercase()
        return workspaceUsers.firstOrNull { it.username == u }
    }

    fun knownUsername(username: String): Boolean {
        val u = username.trim().lowercase()
        return demoAccountFor(u) != null || workspaceUserFor(u) != null
    }

    fun accountFor(username: String): AuthUser? {
        val demo = demoAccountFor(username)
        if (demo != null) return AuthUser.demo(demo.username)
        val custom = workspaceUserFor(username) ?: return null
        return AuthUser(
            username = custom.username,
            name = custom.name.ifEmpty { custom.username },
            role = custom.role,
            title = custom.title,
            email = custom.email,
            phone = custom.phone,
        )
    }

    private fun roleCanOpen(role: String?, slug: String): Boolean =
        canAccessModule(role, slug, roleOf(role))

    fun login(username: String, password: String, departmentId: String? = null): String? {
        val u = username.trim().lowercase()
        val nextUser = accountFor(u) ?: return "Unknown user."
        if (password != passwordFor(u)) return "Wrong password."
        val dept = departmentId?.trim()
        if (!dept.isNullOrEmpty() && moduleById(dept) == null) return "Unknown department."
        var nextDept = if (dept.isNullOrEmpty()) null else dept
        if (nextDept != null && !roleCanOpen(nextUser.role, nextDept)) {
            return "Your role cannot open that app."
        }
        if (nextDept == null) {
            val home = defaultDepartmentForRole(nextUser.role)
            nextDept = if (home != null && moduleById(home) != null && roleCanOpen(nextUser.role, home)) {
                home
            } else {
                val visible = modules.filter { roleCanOpen(nextUser.role, it.id) }.map { it.id }
                if (visible.size == 1) visible.first() else null
            }
        }
        user = nextUser
        activeDepartmentId = nextDept
        persist()
        notifyChange()
        return null
    }

    fun resetPassword(username: String, newPassword: String, confirm: String): String? {
        val u = username.trim().lowercase()
        if (!knownUsername(u)) return "Unknown user."
        if (newPassword.length < 6) return "Use at least 6 characters."
        if (newPassword != confirm) return "Passwords do not match."
        passwords[u] = newPassword
        persist()
        notifyChange()
        return null
    }

    fun updateProfile(name: String, email: String, phone: String, title: String): String? {
        val current = user ?: return "Sign in to edit your profile."
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return "Name is required."
        user = current.copyWith(name = trimmed, email = email.trim(), phone = phone.trim(), title = title.trim())
        val idx = workspaceUsers.indexOfFirst { it.username == current.username }
        if (idx >= 0) {
            val row = workspaceUsers[idx]
            workspaceUsers = workspaceUsers.toMutableList().also {
                it[idx] = WorkspaceUser(
                    username = row.username,
                    name = trimmed,
                    role = row.role,
                    title = title.trim(),
                    email = email.trim(),
                    phone = phone.trim(),
                )
            }
        }
        persist()
        notifyChange()
        return null
    }

    fun setThemeMode(mode: String) {
        themeMode = mode
        persist()
        notifyChange()
    }

    fun setActiveDepartment(departmentId: String?) {
        val dept = departmentId?.trim()
        if (dept.isNullOrEmpty() || moduleById(dept) == null) {
            activeDepartmentId = null
        } else if (user != null && !roleCanOpen(user!!.role, dept)) {
            return
        } else {
            activeDepartmentId = dept
        }
        persist()
        notifyChange()
    }

    private fun enforceAccess() {
        val current = user ?: return
        val dept = activeDepartmentId ?: return
        if (moduleById(dept) != null && roleCanOpen(current.role, dept)) return
        val home = defaultDepartmentForRole(current.role)
        activeDepartmentId =
            if (home != null && moduleById(home) != null && roleCanOpen(current.role, home)) home else null
    }

    fun logout() {
        user = null
        activeDepartmentId = null
        persist()
        notifyChange()
    }

    fun setQuery(q: String) {
        search = q
        notifyChange()
    }

    fun setStatus(record: ErpRecord, status: String) {
        record.status = status
        persist()
        notifyChange()
    }

    fun addRecord(record: ErpRecord) {
        if (user != null && !canCreateEntity(user!!.role, record.moduleId, record.entity, currentRole)) return
        insertRecord(record)
    }

    private fun insertRecord(record: ErpRecord) {
        records = (listOf(record) + records).toMutableList()
        persist()
        notifyChange()
    }

    fun updateRecord(record: ErpRecord) {
        if (user != null && !canEditEntity(user!!.role, record.moduleId, record.entity, currentRole)) return
        persist()
        notifyChange()
    }

    fun submitRecord(record: ErpRecord): String? {
        if (user == null) return "Sign in first."
        if (!canCreateEntity(user!!.role, record.moduleId, record.entity, currentRole) &&
            !canEditEntity(user!!.role, record.moduleId, record.entity, currentRole)
        ) {
            return "Your role cannot submit this record."
        }
        if (!isDraftStatus(record.status)) return "Only drafts can be submitted."
        setStatus(record, if (record.entity in approvalEntities) "Submitted" else "Posted")
        return null
    }

    fun approveRecord(record: ErpRecord): String? {
        if (!canApproveIn(roleName, record.moduleId, currentRole)) {
            return "Your role has no approval desk for this app."
        }
        if (record.entity !in approvalEntities || !isOpenStatus(record.status)) {
            return "This record is not waiting for approval."
        }
        setStatus(record, "Approved")
        return null
    }

    fun rejectRecord(record: ErpRecord): String? {
        if (!canApproveIn(roleName, record.moduleId, currentRole)) {
            return "Your role has no approval desk for this app."
        }
        if (record.entity !in approvalEntities || !isOpenStatus(record.status)) {
            return "This record is not waiting for approval."
        }
        setStatus(record, "Rejected")
        return null
    }

    fun voidRecord(record: ErpRecord): String? {
        if (!canVoidIn(roleName, record.moduleId, currentRole)) {
            return "Your role cannot void records here."
        }
        if (isDraftStatus(record.status) || isVoidedStatus(record.status)) {
            return "This record cannot be voided."
        }
        if (!isApprovedStatus(record.status)) {
            return "Only posted, paid, or approved records can be voided."
        }
        setStatus(record, "Void")
        return null
    }

    fun deleteRecord(record: ErpRecord): String? {
        if (!canDeleteEntity(roleName, record.moduleId, record.entity, currentRole)) {
            return "Your role cannot delete records here."
        }
        records = records.filter { it.id != record.id }.toMutableList()
        persist()
        notifyChange()
        return null
    }

    fun saveRole(role: RoleDefinition): String? {
        if (!isAdmin) return "Only administrators can manage roles."
        val name = role.name.trim()
        if (name.isEmpty()) return "Role name is required."
        if (isAdminRole(name) || isBuiltInRoleName(name)) return "That name is a built-in role."
        if (role.system) return "Built-in roles cannot be changed."
        val key = normalizeRole(name)
        if (roles.any { it.id != role.id && normalizeRole(it.name) == key }) {
            return "A role with that name already exists."
        }
        val next = RoleDefinition(
            id = if (role.id.trim().isEmpty()) newRoleId() else role.id,
            name = name,
            description = role.description.trim(),
            crud = role.crud,
            system = false,
            pagePermissions = role.pagePermissions,
        )
        val idx = roles.indexOfFirst { it.id == next.id }
        roles = if (idx >= 0) roles.toMutableList().also { it[idx] = next } else roles + next
        persist()
        notifyChange()
        return null
    }

    fun deleteRole(id: String): String? {
        if (!isAdmin) return "Only administrators can manage roles."
        val role = roles.firstOrNull { it.id == id } ?: return "Role not found."
        if (role.system) return "Built-in roles cannot be deleted."
        val used = workspaceUsers.any { normalizeRole(it.role) == normalizeRole(role.name) }
        if (used) return "Reassign users on this role first."
        roles = roles.filter { it.id != id }
        persist()
        notifyChange()
        return null
    }

    fun saveWorkspaceUser(
        username: String,
        name: String,
        role: String,
        title: String = "",
        email: String = "",
        phone: String = "",
        password: String? = null,
    ): String? {
        if (!isAdmin) return "Only administrators can manage users."
        val u = username.trim().lowercase()
        if (u.isEmpty() || !Regex("^[a-z0-9._-]{3,32}$").matches(u)) {
            return "Use 3–32 letters, numbers, dots, or dashes."
        }
        if (demoAccountFor(u) != null) return "That username is reserved."
        if (roleOf(role) == null) return "Unknown role."
        if (isAdminRole(role)) return "Use the built-in admin accounts."
        if (!password.isNullOrEmpty() && password.length < 6) return "Use at least 6 characters."
        val row = WorkspaceUser(
            username = u,
            name = if (name.trim().isEmpty()) u else name.trim(),
            role = role.trim(),
            title = title.trim(),
            email = email.trim(),
            phone = phone.trim(),
        )
        val idx = workspaceUsers.indexOfFirst { it.username == u }
        workspaceUsers = if (idx >= 0) workspaceUsers.toMutableList().also { it[idx] = row } else workspaceUsers + row
        if (!password.isNullOrEmpty()) passwords[u] = password
        if (user?.username == u) {
            user = AuthUser(
                username = row.username,
                name = row.name,
                role = row.role,
                title = row.title,
                email = row.email,
                phone = row.phone,
            )
        }
        persist()
        notifyChange()
        return null
    }

    fun deleteWorkspaceUser(username: String): String? {
        if (!isAdmin) return "Only administrators can manage users."
        val u = username.trim().lowercase()
        if (user?.username == u) return "You cannot delete the signed-in user."
        if (demoAccountFor(u) != null) return "Built-in users cannot be deleted."
        if (workspaceUserFor(u) == null) return "User not found."
        workspaceUsers = workspaceUsers.filter { it.username != u }
        passwords.remove(u)
        persist()
        notifyChange()
        return null
    }

    private fun seed(): List<ErpRecord> = completeCatalogSeed(modules)

    private fun mergeMissingCatalogRecords() {
        val have = records.map { "${it.moduleId}|${it.entity}" }.toSet()
        val extras = mutableListOf<ErpRecord>()
        for (module in modules) {
            for (entity in module.entities) {
                val key = "${module.id}|$entity"
                if (key in have) continue
                extras += sampleRecord(module, entity)
            }
        }
        if (extras.isEmpty()) return
        records = (extras + records).toMutableList()
        persist()
    }
}

@Suppress("UNCHECKED_CAST")
private fun Map<*, *>.asStringMap(): Map<String, Any?> =
    entries.associate { it.key.toString() to it.value }
