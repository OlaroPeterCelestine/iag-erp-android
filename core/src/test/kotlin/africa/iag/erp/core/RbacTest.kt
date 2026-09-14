package africa.iag.erp.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RbacTest {
    private val testPassword = "unit-test-login"

    private fun store(): ErpStore = ErpStore(persistence = MemoryKeyValueStore()).also {
        it.load()
        it.seedTestPasswords(testPassword)
    }

    @Test
    fun adminSeesEveryDepartmentIncludingExplicitGrantApps() {
        assertTrue(canAccessModule("Administrator", "banking"))
        assertTrue(canAccessModule("Administrator", "fleet"))
        assertTrue(canAccessModule("Administrator", "crm"))
        assertTrue(canAccessModule("Super Admin", "pos"))
        assertTrue(canDeleteIn("Administrator", "sales"))
        assertTrue(canAccessApprovalDesk("Administrator"))
    }

    @Test
    fun viewerIsInquiryOnlyOnReportsAndRequests() {
        assertTrue(canAccessModule("Viewer", "reports"))
        assertTrue(canAccessModule("Viewer", "accounts"))
        assertTrue(canAccessModule("Viewer", "general-requests"))
        assertFalse(canAccessModule("Viewer", "banking"))
        assertFalse(canAccessModule("Viewer", "sales"))
        assertFalse(canAccessModule("Viewer", "payroll"))
        assertFalse(canAccessModule("Viewer", "fleet"))
        assertFalse(canAccessModule("Viewer", "pos"))
        assertFalse(canCreateIn("Viewer", "reports"))
        assertFalse(canAccessApprovalDesk("Viewer"))
    }

    @Test
    fun clerkCanCreateDayToDayButCannotEditDeleteOrApprove() {
        assertTrue(canAccessModule("Clerk", "banking"))
        assertTrue(canAccessModule("Clerk", "sales"))
        assertTrue(canAccessModule("Clerk", "pos"))
        assertFalse(canAccessModule("Clerk", "payroll"))
        assertFalse(canAccessModule("Clerk", "fleet"))
        assertTrue(canCreateIn("Clerk", "sales"))
        assertFalse(canEditIn("Clerk", "sales"))
        assertFalse(canDeleteIn("Clerk", "sales"))
        assertFalse(canAccessApprovalDesk("Clerk"))
        assertFalse(canCreateEntity("Clerk", "banking", "Reconciliations"))
    }

    @Test
    fun contractorIsLockedToProjectsAndContractManager() {
        assertTrue(canAccessModule("Contractor", "projects"))
        assertTrue(canAccessModule("Contractor", "contract-manager"))
        assertFalse(canAccessModule("Contractor", "banking"))
        assertFalse(canAccessModule("Contractor", "payroll"))
        assertFalse(canAccessModule("Contractor", "reports"))
        assertFalse(canAccessApprovalDesk("Contractor"))
    }

    @Test
    fun hrCanRunPayrollAndFencesAccountantCanVoidButNotFences() {
        assertTrue(canAccessModule("HR", "payroll"))
        assertFalse(canAccessModule("HR", "fleet"))
        assertTrue(canRunPayroll("HR"))
        assertTrue(canManageGeofence("HR"))
        assertTrue(canVoidIn("Accountant", "banking"))
        assertFalse(canManageGeofence("Accountant"))
        assertFalse(canCreateEntity("Accountant", "payroll", "Sites"))
        assertTrue(canCreateEntity("HR", "payroll", "Sites"))
        assertTrue(canRunPayroll("Accountant"))
        assertTrue(canAccessApprovalDesk("Accountant"))
        assertFalse(canAccessApprovalDesk("Reviewer"))
    }

    @Test
    fun loginEnforcesDepartmentGrantsAndWorkflowSod() {
        val s = store()
        assertEquals("Unknown user.", s.login("nobody", testPassword))
        assertEquals("Your role cannot open that app.", s.login("viewer", testPassword, "banking"))
        assertFalse(s.isSignedIn)

        assertNull(s.login("viewer", testPassword))
        assertEquals("Viewer", s.user?.role)
        assertFalse(s.canOpen("banking"))
        assertTrue(s.canOpen("reports"))
        assertFalse(s.canApprove)
        assertFalse(s.visibleModules.any { it.id == "banking" })
        assertTrue(s.visibleModules.any { it.id == "reports" })
        s.logout()

        assertEquals("Your role cannot open that app.", s.login("contractor", testPassword, "banking"))
        assertNull(s.login("contractor", testPassword))
        assertEquals("projects", s.activeDepartmentId)
        assertEquals("projects", s.activeAppId)
        assertTrue(s.canOpen("contract-manager"))
        assertFalse(s.canOpen("sales"))
        s.setActiveDepartment("banking")
        assertEquals("projects", s.activeDepartmentId)
        s.logout()

        assertNull(s.login("clerk", testPassword))
        assertTrue(s.canCreate("sales", "Sales Invoices"))
        assertFalse(s.canEdit("sales", "Sales Invoices"))
        assertFalse(s.canDelete("sales", "Sales Invoices"))
        assertFalse(s.canApprove)

        val draft = ErpRecord(
            id = "rbac-draft",
            moduleId = "sales",
            entity = "Sales Invoices",
            title = "SI-TEST",
            subtitle = "RBAC",
            status = "Draft",
            date = "2026-08-25",
        )
        s.addRecord(draft)
        assertTrue(s.records.any { it.id == "rbac-draft" })
        assertNull(s.submitRecord(draft))
        assertEquals("Posted", draft.status)
        assertNotNull(s.approveRecord(draft))
        assertNotNull(s.deleteRecord(draft))
        s.logout()

        assertNull(s.login("admin", testPassword))
        assertTrue(s.canOpen("fleet"))
        assertTrue(s.canDelete("sales", "Sales Invoices"))
        assertTrue(s.canApprove)
        val pending = s.records.first { it.title == "EXP-2026-019" }
        assertNull(s.approveRecord(pending))
        assertEquals("Approved", pending.status)
        assertNull(s.voidRecord(pending))
        assertEquals("Void", pending.status)
    }

    @Test
    fun customRolesUseCrudPlusOptionalAppGrants() {
        val lab = RoleDefinition(
            id = "role-lab-tech",
            name = "Lab Tech",
            crud = Crud(view = true, create = true, edit = true, delete = false),
            pagePermissions = mapOf(
                PAGE_WILDCARD_KEY to Crud.none,
                "lab" to Crud(view = true, create = true, edit = true, delete = false),
            ),
        )
        assertTrue(canAccessModule("Lab Tech", "lab", lab))
        assertTrue(canCreateIn("Lab Tech", "lab", lab))
        assertFalse(canAccessModule("Lab Tech", "banking", lab))
        assertFalse(canAccessModule("Lab Tech", "fleet", lab))
        assertFalse(canAccessApprovalDesk("Lab Tech"))
        assertFalse(canDeleteIn("Lab Tech", "lab", lab))
    }

    @Test
    fun adminCanCreateCustomRoleAndUserThenThatUserSignsIn() {
        val s = store()
        assertNotNull(s.saveRole(RoleDefinition(id = "x", name = "Lab Tech")))

        assertNull(s.login("admin", testPassword))
        assertNull(
            s.saveRole(
                RoleDefinition(
                    id = "role-lab-tech",
                    name = "Lab Tech",
                    description = "Lab bench",
                    crud = Crud(view = true, create = true, edit = true, delete = false),
                    pagePermissions = mapOf(
                        PAGE_WILDCARD_KEY to Crud.none,
                        "lab" to Crud(view = true, create = true, edit = true, delete = false),
                    ),
                ),
            ),
        )
        assertNull(s.saveWorkspaceUser(username = "labtech", name = "Lina Lab", role = "Lab Tech", password = testPassword))
        s.logout()

        assertNull(s.login("labtech", testPassword))
        assertEquals("Lab Tech", s.user?.role)
        assertTrue(s.canOpen("lab"))
        assertFalse(s.canOpen("banking"))
        assertTrue(s.canCreate("lab"))
        assertFalse(s.canApprove)
        assertEquals("lab", s.activeDepartmentId)
        assertEquals("quality", s.activeAppId)
    }

    @Test
    fun catalogSeedsEveryWebErpFeature() {
        val modules = erpModules()
        val seed = completeCatalogSeed(modules)
        assertTrue(modules.size >= 28)
        for (module in modules) {
            if (module.id == "clock-in") continue
            for (entity in module.entities) {
                assertTrue(
                    seed.any { it.moduleId == module.id && it.entity == entity },
                    "${module.id} missing $entity",
                )
            }
        }
        val s = store()
        assertNull(s.login("admin", testPassword))
        assertEquals(modules.size, s.visibleModules.size)
        assertTrue(canAccessSpecialNav("Administrator", "analytics"))
        assertTrue(s.visibleWorkspaceTools.any { it.id == "trace" })
        assertTrue(s.visibleWorkspaceTools.any { it.id == "analytics" })
        s.logout()
        assertNull(s.login("viewer", testPassword))
        assertTrue(canAccessSpecialNav("Viewer", "guides"))
        assertFalse(canAccessSpecialNav("Viewer", "analytics"))
        assertTrue(s.visibleWorkspaceTools.any { it.id == "trace" })
        assertFalse(s.visibleWorkspaceTools.any { it.id == "analytics" })
    }

    @Test
    fun everyWebDepartmentAndClockInAreOnThePhone() {
        val modules = erpModules()
        val ids = modules.map { it.id }.toSet()
        for (id in webErpDepartmentIds) {
            assertTrue(id in ids, "missing web department $id")
        }
        assertTrue("clock-in" in ids)
        assertTrue(canAccessModule("Viewer", "clock-in"))
        assertTrue(canAccessModule("Clerk", "clock-in"))
        assertTrue(canAccessModule("Contractor", "clock-in"))
        assertTrue(canCreateIn("Viewer", "clock-in"))

        val s = store()
        assertNull(s.login("clerk", testPassword))
        assertTrue(s.canOpen("clock-in"))
        assertFalse(s.canOpen("payroll"))
        assertTrue(s.canClockIn)
        assertTrue(s.geofenceZones().isNotEmpty())

        val inside = s.punch("in", HQ_LATITUDE, HQ_LONGITUDE, 8.0)
        assertTrue(inside?.contains("Checked in") == true, inside ?: "nil")
        assertNotNull(s.openAttendanceToday())
        assertEquals("Present", s.openAttendanceToday()?.status)
        assertEquals("Verified", s.openAttendanceToday()?.fields?.get("verification"))

        val outside = s.punch("out", 0.0, 0.0, 8.0)
        assertTrue(outside?.contains("Outside", ignoreCase = true) == true, outside ?: "nil")
        assertNotNull(s.openAttendanceToday())
        assertTrue(s.forEntity("payroll", "Punch Log").any { it.status == "Rejected" })

        val out = s.punch("out", HQ_LATITUDE, HQ_LONGITUDE, 8.0)
        assertTrue(out?.contains("Checked out") == true, out ?: "nil")
        assertNull(s.openAttendanceToday())
        s.logout()

        assertNull(s.login("viewer", testPassword))
        assertTrue(s.canOpen("clock-in"))
        assertTrue(s.visibleModules.any { it.id == "clock-in" })
        assertTrue(s.punch("in", HQ_LATITUDE, HQ_LONGITUDE, 8.0)?.contains("Checked in") == true)
        s.logout()

        assertNull(s.login("contractor", testPassword))
        assertTrue(s.canOpen("clock-in"))
        assertTrue(s.canOpen("projects"))
        assertFalse(s.canOpen("payroll"))
    }

    @Test
    fun suiteAppsAreSeparateFullTools() {
        assertEquals(
            listOf(
                "contracts", "crm", "dms", "finance", "fleet", "hr", "logistics", "pos",
                "procurement", "production", "projects", "quality", "requests", "sales", "security",
            ).sorted(),
            suiteApps.map { it.id }.sorted(),
        )
        assertTrue(canOpenSuiteApp("Administrator", "crm"))
        assertTrue(canOpenSuiteApp("Administrator", "pos"))
        assertTrue(canOpenSuiteApp("Administrator", "contracts"))
        assertTrue(canOpenSuiteApp("Administrator", "dms"))
        assertTrue(canOpenSuiteApp("Administrator", "fleet"))
        assertEquals("dms", suiteAppById("records")?.id)
        assertEquals("contracts", suiteAppById("contract-manager")?.id)
        assertTrue(canOpenSuiteApp("Procurement", "procurement"))
        assertFalse(canOpenSuiteApp("Procurement", "security"))
        assertTrue(canOpenSuiteApp("HR", "hr"))
        assertFalse(canOpenSuiteApp("Viewer", "security"))

        val s = store()
        assertNull(s.login("admin", testPassword))
        assertNull(s.activeAppId)
        assertEquals(suiteApps.size, s.visibleSuiteApps.size)
        s.openApp("finance")
        assertEquals("finance", s.activeAppId)
        assertTrue(s.appModules.any { it.id == "banking" })
        assertFalse(s.appModules.any { it.id == "security" })
        s.openApp("security")
        assertEquals("security", s.activeAppId)
        assertEquals(listOf("security"), s.appModules.map { it.id })
        s.closeApp()
        assertNull(s.activeAppId)
        s.logout()

        assertNull(s.login("procurement", testPassword))
        assertEquals("procurement", s.activeAppId)
        assertTrue(s.appModules.any { it.id == "purchases" })
        assertFalse(s.appModules.any { it.id == "banking" })
        s.logout()

        assertNull(s.login("hr", testPassword))
        assertEquals("hr", s.activeAppId)
        s.logout()

        assertEquals("Your role cannot open that app.", s.login("clerk", testPassword, "security"))
        assertNull(s.login("clerk", testPassword))
        assertEquals("finance", s.activeAppId)
    }

    @Test
    fun quickActionsFollowAppAndRbac() {
        val s = store()
        assertNull(s.login("admin", testPassword))
        assertTrue(s.launcherQuickActions.any { it.id == "clock" })
        assertTrue(s.launcherQuickActions.any { it.id == "approvals" })
        s.openApp("finance")
        val financeIds = s.homeQuickActions.map { it.id }
        assertTrue(financeIds.contains("clock"))
        assertTrue(financeIds.contains("receipt"))
        assertTrue(financeIds.contains("payment"))
        assertFalse(financeIds.contains("po"))
        assertTrue(s.homeQuickActions.size <= 8)
        s.openApp("sales")
        assertTrue(s.homeQuickActions.any { it.id == "invoice" })
        assertTrue(s.homeQuickActions.any { it.id == "customer" })
        assertFalse(s.homeQuickActions.any { it.id == "lead" })
        s.openApp("crm")
        assertTrue(s.homeQuickActions.any { it.id == "lead" })
        s.openApp("pos")
        assertTrue(s.homeQuickActions.any { it.id == "ticket" })
        s.openApp("contracts")
        assertTrue(s.homeQuickActions.any { it.id == "contract" })
        s.openApp("fleet")
        assertTrue(s.homeQuickActions.any { it.id == "vehicle" })
        s.openApp("dms")
        assertTrue(s.homeQuickActions.any { it.id == "folder" })
        s.logout()

        assertNull(s.login("viewer", testPassword))
        assertEquals("finance", s.activeAppId)
        assertTrue(s.homeQuickActions.any { it.id == "clock" })
        assertTrue(s.homeQuickActions.any { it.id == "reports" })
        assertFalse(s.homeQuickActions.any { it.id == "receipt" })
        assertFalse(s.homeQuickActions.any { it.id == "approvals" })
        s.logout()

        assertNull(s.login("procurement", testPassword))
        assertTrue(s.homeQuickActions.any { it.id == "po" })
        assertFalse(s.homeQuickActions.any { it.id == "invoice" })
    }

    @Test
    fun welcomeStatsShowLiveCounts() {
        val s = store()
        assertNull(s.login("admin", testPassword))
        assertNull(s.activeAppId)
        assertEquals(listOf("apps", "records", "todo", "clock"), s.welcomeStats.map { it.id })
        assertEquals("${s.visibleSuiteApps.size}", s.welcomeStats.first { it.id == "apps" }.value)
        assertEquals("Out", s.welcomeStats.first { it.id == "clock" }.value)
        s.openApp("finance")
        assertEquals(listOf("desks", "records", "todo", "clock"), s.welcomeStats.map { it.id })
        assertEquals("${s.appModules.size}", s.welcomeStats.first { it.id == "desks" }.value)
        s.logout()

        assertNull(s.login("viewer", testPassword))
        assertFalse(s.welcomeStats.any { it.id == "todo" })
        assertTrue(s.welcomeStats.any { it.id == "desks" })
        assertTrue(s.welcomeStats.any { it.id == "clock" })
    }

    @Test
    fun passwordsAreHashedAndHaveNoSharedDefault() {
        val persistence = MemoryKeyValueStore()
        val s = ErpStore(persistence = persistence)
        s.load()
        assertEquals("No password set. Use Forgot password to create one.", s.login("admin", "anything-at-all"))
        assertNull(s.resetPassword("admin", testPassword, testPassword))
        assertNull(s.login("admin", testPassword))
        val raw = persistence.get(STORE_KEY) ?: ""
        assertFalse(raw.contains(testPassword))
        assertTrue(raw.contains(passwordDigest("admin", testPassword)))
    }

    @Test
    fun legacyPlaintextPasswordsAreMigratedOnLoad() {
        val persistence = MemoryKeyValueStore()
        val legacy = "legacy-secret"
        persistence.put(STORE_KEY, """{"passwords":{"admin":"$legacy"}}""")
        val s = ErpStore(persistence = persistence)
        s.load()
        assertNull(s.login("admin", legacy))
        val raw = persistence.get(STORE_KEY) ?: ""
        assertFalse(raw.contains(legacy))
        assertTrue(raw.contains(passwordDigest("admin", legacy)))
    }

    @Test
    fun adoptApiRolesKeepsDatabaseSystemRoles() {
        val api = listOf(
            RoleDefinition(id = "role-admin-db", name = "Administrator", crud = Crud.full, system = true),
            RoleDefinition(
                id = "r1",
                name = "Field Clerk",
                crud = Crud(view = true, create = true, edit = false, delete = false),
                system = false,
                pagePermissions = mapOf("sales" to Crud(view = true, create = true, edit = false, delete = false)),
            ),
        )
        val adopted = adoptApiRoles(api)
        assertEquals("role-admin-db", findRoleDefinition(adopted, "Administrator")?.id)
        assertEquals(true, findRoleDefinition(adopted, "Field Clerk")?.crud?.create)
        assertEquals(true, findRoleDefinition(adopted, "Field Clerk")?.pagePermissions?.get("sales")?.view)
        assertNotNull(findRoleDefinition(adopted, "Clerk"))
        val merged = mergeStoredRoles(api)
        assertTrue(findRoleDefinition(merged, "Administrator")?.id != "role-admin-db")
        assertEquals("r1", findRoleDefinition(merged, "Field Clerk")?.id)
    }
}
