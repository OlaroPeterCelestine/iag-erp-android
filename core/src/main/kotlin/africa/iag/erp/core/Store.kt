package africa.iag.erp.core

const val STORE_KEY = "iag-erp-android-v1"

class ErpStore(
    val modules: List<ErpModule> = erpModules(),
    private val persistence: KeyValueStore = MemoryKeyValueStore(),
    private val api: ErpApiClient? = null,
    private val onMain: (() -> Unit) -> Unit = { it() },
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
    var activeAppId: String? = null
        private set
    var themeMode: String = "system"
        private set
    var remoteSession: Boolean = false
        private set
    var lastRemoteError: String? = null
        private set
    private val passwords = linkedMapOf<String, String>()
    private val listeners = mutableListOf<() -> Unit>()
    private var apiToken: String = ""
    private val knownRemoteIds = mutableSetOf<String>()
    private val io = java.util.concurrent.Executors.newSingleThreadExecutor()

    val frontendOrigin: String
        get() = api?.origin ?: ErpConfig.origin(persistence)

    val usesFrontend: Boolean get() = api != null

    init {
        if (api != null) {
            api.origin = ErpConfig.origin(persistence)
        }
    }

    val isSignedIn: Boolean get() = user != null
    val roleName: String? get() = user?.role
    val isAdmin: Boolean get() = isAdminRole(roleName)
    val currentRole: RoleDefinition? get() = roleOf(roleName)
    val kpis: List<Kpi> get() = defaultKpis

    val visibleModules: List<ErpModule>
        get() = modules.filter { canAccessModule(roleName, it.id, currentRole) }

    val visibleSuiteApps: List<SuiteApp>
        get() = suiteApps.filter { canOpenSuiteApp(roleName, it.id, currentRole) }

    val activeSuiteApp: SuiteApp?
        get() = suiteAppById(activeAppId)

    val appModules: List<ErpModule>
        get() {
            val app = activeSuiteApp ?: return visibleModules
            return visibleModules.filter { it.id in app.moduleIds }
        }

    val appPendingApprovals: List<ErpRecord>
        get() {
            if (activeSuiteApp == null) return pendingApprovals
            val ids = appModules.map { it.id }.toSet()
            return pendingApprovals.filter { it.moduleId in ids }
        }

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

    val homeQuickActions: List<QuickAction>
        get() = quickActionCatalog.filter { action ->
            val appId = action.appId
            if (appId != null) {
                if (activeAppId != appId) return@filter false
            } else if (activeAppId == null) {
                return@filter false
            }
            allowsQuickAction(action)
        }.take(8)

    val launcherQuickActions: List<QuickAction>
        get() = quickActionCatalog.filter { it.appId == null && allowsQuickAction(it) }

    val welcomeStats: List<WelcomeStat>
        get() {
            val scoped = records.filter { rec ->
                if (!canOpen(rec.moduleId)) return@filter false
                val app = activeSuiteApp ?: return@filter true
                rec.moduleId in app.moduleIds
            }
            val stats = mutableListOf<WelcomeStat>()
            if (activeAppId == null) {
                stats += WelcomeStat("apps", "Apps", "${visibleSuiteApps.size}")
            } else {
                stats += WelcomeStat("desks", "Desks", "${appModules.size}")
            }
            stats += WelcomeStat("records", "Records", "${scoped.size}")
            if (canApprove) {
                val pending = if (activeAppId == null) pendingApprovals.size else appPendingApprovals.size
                stats += WelcomeStat("todo", "To do", "$pending")
            }
            if (canClockIn) {
                stats += WelcomeStat("clock", "Clock", if (openAttendanceToday() == null) "Out" else "In")
            }
            return stats.take(4)
        }

    fun allowsQuickAction(action: QuickAction): Boolean = when (action.kind) {
        QuickActionKind.CLOCK -> canClockIn
        QuickActionKind.APPROVALS -> canApprove
        QuickActionKind.ACCESS -> isAdmin
        QuickActionKind.CREATE -> canOpen(action.moduleId) &&
            (action.entity?.let { canCreate(action.moduleId, it) } ?: canCreate(action.moduleId))
        QuickActionKind.LIST -> canOpen(action.moduleId)
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
        for (module in appModules) {
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
            if (activeSuiteApp != null && appModules.none { it.id == rec.moduleId }) continue
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
        pushRemote(open)
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
        onMain { listeners.toList().forEach { it() } }
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

    internal fun seedTestPasswords(password: String) {
        for (account in demoAccounts) {
            passwords[account.username.lowercase()] = passwordDigest(account.username, password)
        }
    }

    private fun passwordMatches(username: String, password: String): Boolean {
        val u = username.trim().lowercase()
        val stored = passwords[u] ?: return false
        if (stored.isEmpty()) return false
        if (isPasswordHash(stored)) return stored == passwordDigest(u, password)
        if (stored == password) {
            passwords[u] = passwordDigest(u, password)
            persist()
            return true
        }
        return false
    }

    private fun storePassword(username: String, password: String) {
        passwords[username.trim().lowercase()] = passwordDigest(username, password)
    }

    private fun migrateLegacyPasswords() {
        var changed = false
        val updated = passwords.mapValues { (user, value) ->
            if (value.isNotEmpty() && !isPasswordHash(value)) {
                changed = true
                passwordDigest(user, value)
            } else {
                value
            }
        }
        if (changed) {
            passwords.clear()
            passwords.putAll(updated)
            persist()
        }
    }

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
            apiToken = (j["apiToken"] as? String).orEmpty()
            remoteSession = apiToken.isNotEmpty()
            api?.token = apiToken.ifEmpty { null }
            lastRemoteError = null
            val dept = j["activeDepartmentId"] as? String
            activeDepartmentId = if (dept.isNullOrEmpty() || moduleById(dept) == null) null else dept
            val app = j["activeAppId"] as? String
            activeAppId = when {
                !app.isNullOrEmpty() && suiteAppById(app) != null -> app
                activeDepartmentId != null -> suiteAppContaining(activeDepartmentId!!)?.id
                else -> null
            }
            val pw = j["passwords"] as? Map<*, *>
            passwords.clear()
            if (pw != null) {
                for ((k, v) in pw) passwords[k.toString().lowercase()] = v.toString()
            }
            migrateLegacyPasswords()
            readAccessLists(j)
            val loaded = mutableListOf<ErpRecord>()
            val rawRecords = j["records"] as? List<*>
            if (rawRecords != null) {
                for (item in rawRecords) {
                    val map = item as? Map<*, *> ?: continue
                    loaded.add(ErpRecord.fromJson(map.asStringMap()))
                }
            }
            records = if (loaded.isEmpty() && !remoteSession) seed().toMutableList() else loaded
            if (remoteSession) {
                knownRemoteIds.clear()
                knownRemoteIds.addAll(records.map { it.id })
            } else {
                mergeMissingCatalogRecords()
            }
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
                    "activeAppId" to activeAppId,
                    "themeMode" to themeMode,
                    "passwords" to passwords,
                    "roles" to roles.map { it.toJson() },
                    "workspaceUsers" to workspaceUsers.map { it.toJson() },
                    "records" to records.map { it.toJson() },
                    "apiToken" to apiToken,
                    "remoteSession" to remoteSession,
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
        roles = if (remoteSession) adoptApiRoles(stored) else mergeStoredRoles(stored)
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
        if (passwords[u].isNullOrEmpty()) return "No password set. Use Forgot password to create one."
        if (!passwordMatches(u, password)) return "Wrong password."
        return adoptUser(nextUser, departmentId)
    }

    fun loginAsync(username: String, password: String, departmentId: String? = null): String? {
        val client = api ?: return login(username, password, departmentId)
        lastRemoteError = null
        val u = username.trim()
        return client.login(u, password, true).fold(
            onSuccess = { applyRemoteSession(it, departmentId) },
            onFailure = { error ->
                val apiErr = error as? ErpApiError ?: ErpApiError.Network(error.message ?: "Can't reach the workspace.")
                lastRemoteError = apiErr.message
                notifyChange()
                if (apiErr.isNetwork && passwords[u.lowercase()].isNullOrEmpty().not()) {
                    return@fold login(u, password, departmentId)
                }
                apiErr.message
            },
        )
    }

    fun resumeRemoteSession() {
        val client = api ?: return
        if (!remoteSession || apiToken.isEmpty()) return
        client.token = apiToken
        client.me().fold(
            onSuccess = { remoteUser ->
                user = remoteUser.authUser
                persist()
                notifyChange()
                refreshDirectory()
                upsertRoleFromRemoteUser(remoteUser)
                persist()
                notifyChange()
                refreshApprovals()
            },
            onFailure = { error ->
                val apiErr = error as? ErpApiError ?: ErpApiError.Network(error.message ?: "Can't reach the workspace.")
                if (apiErr.isUnauthorized) {
                    clearRemoteSession()
                    user = null
                    activeDepartmentId = null
                    activeAppId = null
                    persist()
                    notifyChange()
                } else {
                    lastRemoteError = apiErr.message
                    notifyChange()
                }
            },
        )
    }

    fun setFrontendOrigin(origin: String) {
        val next = ErpConfig.sanitizeOrigin(origin)
        ErpConfig.saveOrigin(next, persistence)
        api?.origin = next.ifEmpty { ErpConfig.liveFrontendOrigin }
        notifyChange()
    }

    fun refreshEntity(moduleId: String, entity: String) {
        val client = api ?: return
        if (!remoteSession) return
        val target = apiStorageTarget(moduleId, entity)
        client.getRecords(target.module, target.entity).fold(
            onSuccess = { rows ->
                val mapped = rows.map { recordFromApi(it, moduleId, entity) }
                val oldIds = records.filter { it.moduleId == moduleId && it.entity == entity }.map { it.id }
                knownRemoteIds.removeAll(oldIds.toSet())
                records = records.filter { it.moduleId != moduleId || it.entity != entity }.toMutableList()
                records = (mapped + records).toMutableList()
                knownRemoteIds.addAll(mapped.map { it.id })
                persist()
                notifyChange()
            },
            onFailure = { error ->
                lastRemoteError = error.message
                notifyChange()
            },
        )
    }

    fun refreshDirectory() {
        val client = api ?: return
        if (!remoteSession) return
        client.listRoles().onSuccess { rows ->
            roles = adoptApiRoles(rows.map { RoleDefinition.fromJson(it) })
            persist()
            notifyChange()
        }
        if (!isAdmin) return
        client.listUsers().onSuccess { rows ->
            workspaceUsers = rows.mapNotNull { workspaceUserFromApi(it) }
            persist()
            notifyChange()
        }
    }

    private fun upsertRoleFromRemoteUser(remote: ErpRemoteUser) {
        val name = remote.role.trim()
        if (name.isEmpty()) return
        val existing = findRoleDefinition(roles, name)
        var next = existing ?: RoleDefinition(
            id = remote.roleId.ifEmpty { newRoleId() },
            name = name,
            description = "Workspace role",
            crud = remote.crud ?: Crud.none,
            system = isAdminRole(name) || isBuiltInRoleName(name),
            pagePermissions = remote.pagePermissions,
        )
        if (remote.roleId.isNotEmpty()) next = next.copy(id = remote.roleId)
        if (remote.crud != null) next = next.copy(crud = remote.crud)
        if (remote.pagePermissions.isNotEmpty()) next = next.copy(pagePermissions = remote.pagePermissions)
        val idx = roles.indexOfFirst { normalizeRole(it.name) == normalizeRole(name) }
        roles = if (idx >= 0) {
            roles.toMutableList().also { it[idx] = next }
        } else {
            roles + next
        }
    }

    fun refreshApprovals() {
        val client = api ?: return
        if (!remoteSession) return
        client.approvalDesk().fold(
            onSuccess = { data ->
                val items = (data["items"] as? List<*>).orEmpty()
                for (item in items) {
                    val map = (item as? Map<*, *>)?.asAnyMap() ?: continue
                    mergeApprovalItem(map)
                }
                persist()
                notifyChange()
            },
            onFailure = { error ->
                lastRemoteError = error.message
                notifyChange()
            },
        )
    }

    fun requestFrontendPasswordReset(username: String): Result<String> {
        val client = api ?: return Result.failure(ErpApiError.Network("Can't send a reset email right now."))
        return client.requestPasswordReset(username.trim())
    }

    private fun applyRemoteSession(session: ErpRemoteSession, departmentId: String?): String? {
        api?.token = session.token
        apiToken = session.token
        remoteSession = true
        lastRemoteError = null
        records.clear()
        knownRemoteIds.clear()
        val adopted = adoptUser(session.user.authUser, departmentId)
        refreshDirectory()
        upsertRoleFromRemoteUser(session.user)
        persist()
        notifyChange()
        refreshApprovals()
        return adopted
    }

    private fun clearRemoteSession() {
        apiToken = ""
        remoteSession = false
        api?.token = null
        lastRemoteError = null
    }

    private fun adoptUser(nextUser: AuthUser, departmentId: String?): String? {
        val requested = departmentId?.trim()
        val def = findRoleDefinition(roles, nextUser.role)
        var nextApp: String? = null
        var nextDept: String? = null
        if (!requested.isNullOrEmpty()) {
            when {
                suiteAppById(requested) != null -> {
                    if (!canOpenSuiteApp(nextUser.role, requested, def)) return "Your role cannot open that app."
                    nextApp = requested
                }
                moduleById(requested) != null -> {
                    if (!roleCanOpen(nextUser.role, requested)) return "Your role cannot open that app."
                    nextApp = suiteAppContaining(requested)?.id
                    nextDept = requested
                }
                else -> return "Unknown department."
            }
        }
        if (nextApp == null) {
            val homeApp = defaultSuiteAppForRole(nextUser.role)
            nextApp = if (homeApp != null && canOpenSuiteApp(nextUser.role, homeApp, def)) {
                homeApp
            } else {
                val visible = suiteApps.filter { canOpenSuiteApp(nextUser.role, it.id, def) }
                if (visible.size == 1) visible.first().id else null
            }
        }
        if (nextDept == null && nextApp != null) {
            nextDept = suiteAppById(nextApp)?.moduleIds?.firstOrNull { roleCanOpen(nextUser.role, it) }
        }
        if (nextDept == null) {
            val home = defaultDepartmentForRole(nextUser.role)
            if (home != null && moduleById(home) != null && roleCanOpen(nextUser.role, home)) {
                nextDept = home
                if (nextApp == null) nextApp = suiteAppContaining(home)?.id
            }
        }
        user = nextUser
        activeAppId = nextApp
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
        passwords[u] = passwordDigest(u, newPassword)
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

    fun updateProfileAsync(name: String, email: String, phone: String, title: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return "Name is required."
        val client = api
        if (client != null && remoteSession) {
            return client.updateProfile(trimmed, email, phone, title).fold(
                onSuccess = { remote ->
                    val current = user
                    user = AuthUser(
                        username = current?.username ?: remote.username,
                        name = remote.name.ifEmpty { trimmed },
                        role = remote.role.ifEmpty { current?.role ?: "Viewer" },
                        email = remote.email.ifEmpty { email },
                        phone = remote.phone.ifEmpty { phone },
                        title = remote.title.ifEmpty { title },
                    )
                    persist()
                    notifyChange()
                    null
                },
                onFailure = { it.message },
            )
        }
        return updateProfile(name, email, phone, title)
    }

    fun setThemeMode(mode: String) {
        themeMode = mode
        persist()
        notifyChange()
    }

    fun openApp(appId: String) {
        if (!canOpenSuiteApp(roleName, appId, currentRole)) return
        activeAppId = appId
        activeDepartmentId = suiteAppById(appId)?.moduleIds?.firstOrNull { canOpen(it) }
        persist()
        notifyChange()
    }

    fun closeApp() {
        activeAppId = null
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
            suiteAppContaining(dept)?.let { activeAppId = it.id }
        }
        persist()
        notifyChange()
    }

    private fun enforceAccess() {
        val current = user ?: return
        if (activeAppId != null && !canOpenSuiteApp(current.role, activeAppId!!, roleOf(current.role))) {
            activeAppId = defaultSuiteAppForRole(current.role)
        }
        val dept = activeDepartmentId
        if (dept != null && moduleById(dept) != null && roleCanOpen(current.role, dept)) {
            if (activeAppId == null) activeAppId = suiteAppContaining(dept)?.id
            return
        }
        val home = defaultDepartmentForRole(current.role)
        activeDepartmentId =
            if (home != null && moduleById(home) != null && roleCanOpen(current.role, home)) home else null
        if (activeAppId == null) activeAppId = activeDepartmentId?.let { suiteAppContaining(it)?.id }
    }

    fun logout() {
        val client = api
        val hadRemote = remoteSession
        user = null
        activeDepartmentId = null
        activeAppId = null
        clearRemoteSession()
        persist()
        notifyChange()
        if (hadRemote && client != null) {
            io.execute { client.logout() }
        }
    }

    fun setQuery(q: String) {
        search = q
        notifyChange()
    }

    fun setStatus(record: ErpRecord, status: String) {
        record.status = status
        persist()
        notifyChange()
        val chainAdvance = ErpEndpoints.isChainEntity(record.entity) &&
            !isDraftStatus(status) &&
            status.lowercase() != "submitted" &&
            status.lowercase() != "posted"
        if (!chainAdvance) pushRemote(record)
    }

    fun addRecord(record: ErpRecord) {
        if (user != null && !canCreateEntity(user!!.role, record.moduleId, record.entity, currentRole)) return
        insertRecord(record)
    }

    private fun insertRecord(record: ErpRecord) {
        records = (listOf(record) + records).toMutableList()
        persist()
        notifyChange()
        pushRemote(record)
    }

    fun updateRecord(record: ErpRecord) {
        if (user != null && !canEditEntity(user!!.role, record.moduleId, record.entity, currentRole)) return
        persist()
        notifyChange()
        pushRemote(record)
    }

    fun submitRecord(record: ErpRecord): String? {
        submitGate(record)?.let { return it }
        setStatus(record, if (record.entity in approvalEntities) "Submitted" else "Posted")
        return null
    }

    fun submitRecordAsync(record: ErpRecord): String? {
        submitGate(record)?.let { return it }
        val next = if (record.entity in approvalEntities) "Submitted" else "Posted"
        val client = api
        if (client != null && remoteSession) {
            val payload = apiPayload(record).toMutableMap().also { it["status"] = next }
            val target = apiStorageTarget(record.moduleId, record.entity)
            return client.patchRecord(target.module, target.entity, record.id, payload).fold(
                onSuccess = { row ->
                    adoptRemoteRow(row, record)
                    record.status = jsonText(row["status"]) ?: next
                    persist()
                    notifyChange()
                    null
                },
                onFailure = { error ->
                    val apiErr = error as? ErpApiError
                    if (apiErr?.isNotFound == true) submitRecord(record) else error.message
                },
            )
        }
        return submitRecord(record)
    }

    fun approveRecord(record: ErpRecord): String? {
        approvalGate(record)?.let { return it }
        setStatus(record, "Approved")
        return null
    }

    fun approveRecordAsync(record: ErpRecord, comment: String = ""): String? {
        approvalGate(record)?.let { return it }
        val client = api
        if (client != null && remoteSession) {
            if (ErpEndpoints.isChainEntity(record.entity)) {
                return client.approvalAction(apiEntityKey(record.entity), record.id, "advance", comment).fold(
                    onSuccess = { row ->
                        applyApprovalPayload(row, record)
                        persist()
                        notifyChange()
                        null
                    },
                    onFailure = { it.message },
                )
            }
            val payload = apiPayload(record).toMutableMap().also { it["status"] = "Approved" }
            val target = apiStorageTarget(record.moduleId, record.entity)
            return client.patchRecord(target.module, target.entity, record.id, payload).fold(
                onSuccess = { row ->
                    adoptRemoteRow(row, record)
                    null
                },
                onFailure = { it.message },
            )
        }
        return approveRecord(record)
    }

    fun rejectRecord(record: ErpRecord): String? {
        approvalGate(record)?.let { return it }
        setStatus(record, "Rejected")
        return null
    }

    fun rejectRecordAsync(record: ErpRecord, comment: String = "Rejected from IAG Central."): String? {
        approvalGate(record)?.let { return it }
        val reason = comment.trim().ifEmpty { "Rejected from IAG Central." }
        val client = api
        if (client != null && remoteSession) {
            if (ErpEndpoints.isChainEntity(record.entity)) {
                return client.approvalAction(apiEntityKey(record.entity), record.id, "reject", reason).fold(
                    onSuccess = { row ->
                        applyApprovalPayload(row, record)
                        persist()
                        notifyChange()
                        null
                    },
                    onFailure = { it.message },
                )
            }
            val payload = apiPayload(record).toMutableMap().also { it["status"] = "Rejected" }
            val target = apiStorageTarget(record.moduleId, record.entity)
            return client.patchRecord(target.module, target.entity, record.id, payload).fold(
                onSuccess = { row ->
                    adoptRemoteRow(row, record)
                    null
                },
                onFailure = { it.message },
            )
        }
        return rejectRecord(record)
    }

    fun voidRecord(record: ErpRecord): String? {
        voidGate(record)?.let { return it }
        setStatus(record, "Void")
        return null
    }

    fun voidRecordAsync(record: ErpRecord): String? {
        voidGate(record)?.let { return it }
        val client = api
        if (client != null && remoteSession) {
            val payload = apiPayload(record).toMutableMap().also { it["status"] = "Void" }
            val target = apiStorageTarget(record.moduleId, record.entity)
            return client.patchRecord(target.module, target.entity, record.id, payload).fold(
                onSuccess = { row ->
                    adoptRemoteRow(row, record)
                    null
                },
                onFailure = { it.message },
            )
        }
        return voidRecord(record)
    }

    fun deleteRecord(record: ErpRecord): String? {
        if (!canDeleteEntity(roleName, record.moduleId, record.entity, currentRole)) {
            return "Your role cannot delete records here."
        }
        records = records.filter { it.id != record.id }.toMutableList()
        persist()
        notifyChange()
        pushRemote(record, remove = true)
        return null
    }

    fun deleteRecordAsync(record: ErpRecord): String? {
        if (!canDeleteEntity(roleName, record.moduleId, record.entity, currentRole)) {
            return "Your role cannot delete records here."
        }
        val client = api
        if (client != null && remoteSession) {
            val target = apiStorageTarget(record.moduleId, record.entity)
            return client.deleteRecord(target.module, target.entity, record.id).fold(
                onSuccess = {
                    records = records.filter { it.id != record.id }.toMutableList()
                    knownRemoteIds.remove(record.id)
                    persist()
                    notifyChange()
                    null
                },
                onFailure = { it.message },
            )
        }
        return deleteRecord(record)
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
        if (!password.isNullOrEmpty()) storePassword(u, password)
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

    private fun submitGate(record: ErpRecord): String? {
        if (user == null) return "Sign in first."
        if (!canCreateEntity(user!!.role, record.moduleId, record.entity, currentRole) &&
            !canEditEntity(user!!.role, record.moduleId, record.entity, currentRole)
        ) {
            return "Your role cannot submit this record."
        }
        if (!isDraftStatus(record.status)) return "Only drafts can be submitted."
        return null
    }

    private fun approvalGate(record: ErpRecord): String? {
        if (!canApproveIn(roleName, record.moduleId, currentRole)) {
            return "Your role has no approval desk for this app."
        }
        if (record.entity !in approvalEntities || !isOpenStatus(record.status)) {
            return "This record is not waiting for approval."
        }
        return null
    }

    private fun voidGate(record: ErpRecord): String? {
        if (!canVoidIn(roleName, record.moduleId, currentRole)) {
            return "Your role cannot void records here."
        }
        if (isDraftStatus(record.status) || isVoidedStatus(record.status)) {
            return "This record cannot be voided."
        }
        if (!isApprovedStatus(record.status)) {
            return "Only posted, paid, or approved records can be voided."
        }
        return null
    }

    private fun pushRemote(record: ErpRecord, remove: Boolean = false) {
        val client = api ?: return
        if (!remoteSession) return
        val target = apiStorageTarget(record.moduleId, record.entity)
        val payload = apiPayload(record)
        val known = record.id in knownRemoteIds
        io.execute {
            if (remove) {
                client.deleteRecord(target.module, target.entity, record.id).fold(
                    onSuccess = { knownRemoteIds.remove(record.id) },
                    onFailure = { error ->
                        lastRemoteError = error.message
                        notifyChange()
                    },
                )
                return@execute
            }
            val result = if (known) {
                client.patchRecord(target.module, target.entity, record.id, payload).fold(
                    onSuccess = { Result.success(it) },
                    onFailure = { error ->
                        val apiErr = error as? ErpApiError
                        if (apiErr?.isNotFound == true) client.createRecord(target.module, target.entity, payload)
                        else Result.failure(error)
                    },
                )
            } else {
                client.createRecord(target.module, target.entity, payload).fold(
                    onSuccess = { Result.success(it) },
                    onFailure = { error ->
                        val apiErr = error as? ErpApiError
                        if (apiErr?.isConflict == true) client.patchRecord(target.module, target.entity, record.id, payload)
                        else Result.failure(error)
                    },
                )
            }
            result.fold(
                onSuccess = { row -> onMain { adoptRemoteRow(row, record) } },
                onFailure = { error ->
                    lastRemoteError = error.message
                    notifyChange()
                },
            )
        }
    }

    private fun adoptRemoteRow(row: Map<String, Any?>, record: ErpRecord) {
        val mapped = recordFromApi(row, record.moduleId, record.entity)
        record.title = mapped.title
        record.subtitle = mapped.subtitle
        record.status = mapped.status
        record.date = mapped.date
        record.amount = mapped.amount
        record.fields = mapped.fields
        knownRemoteIds.add(record.id)
        if (mapped.id != record.id) {
            val idx = records.indexOfFirst { it.id == record.id }
            if (idx >= 0) records[idx] = mapped
            knownRemoteIds.remove(record.id)
            knownRemoteIds.add(mapped.id)
        }
        persist()
        notifyChange()
    }

    private fun mergeApprovalItem(item: Map<String, Any?>) {
        val id = jsonText(item["id"]).orEmpty()
        if (id.isEmpty()) return
        val moduleId = jsonText(item["module"]).orEmpty()
        val entityKey = jsonText(item["entity"]).orEmpty()
        val payload = (item["record"] as? Map<*, *>)?.asAnyMap() ?: item
        val existing = records.firstOrNull { it.id == id }
        if (existing != null) {
            adoptRemoteRow(payload, existing)
            jsonText(item["status"])?.let { if (it.isNotEmpty()) existing.status = it }
            return
        }
        if (moduleId.isEmpty()) return
        val entity = records.firstOrNull { it.moduleId == moduleId && apiEntityKey(it.entity) == entityKey }?.entity
            ?: moduleById(moduleId)?.entities?.firstOrNull { apiEntityKey(it) == entityKey }
            ?: entityKey
        val mapped = recordFromApi(payload, moduleId, entity)
        records = (listOf(mapped) + records).toMutableList()
        knownRemoteIds.add(mapped.id)
    }

    private fun workspaceUserFromApi(row: Map<String, Any?>): WorkspaceUser? {
        val username = (jsonText(row["username"]) ?: jsonText(row["email"]) ?: "").lowercase()
        if (username.isEmpty() || demoAccountFor(username) != null) return null
        return WorkspaceUser(
            username = username,
            name = jsonText(row["name"]) ?: username,
            role = jsonText(row["role"]) ?: "Viewer",
            title = jsonText(row["title"]).orEmpty(),
            email = jsonText(row["email"]).orEmpty(),
            phone = jsonText(row["phone"]).orEmpty(),
        )
    }

    private fun applyApprovalPayload(json: Map<String, Any?>, record: ErpRecord) {
        val rec = (json["record"] as? Map<*, *>)?.asAnyMap()
        if (rec != null) adoptRemoteRow(rec, record)
        jsonText(json["status"])?.let { if (it.isNotEmpty()) record.status = it }
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
