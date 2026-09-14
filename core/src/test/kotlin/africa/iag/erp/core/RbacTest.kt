package africa.iag.erp.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RbacTest {
    private fun store(): ErpStore = ErpStore(persistence = MemoryKeyValueStore()).also { it.load() }

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
        assertEquals("Unknown user.", s.login("nobody", "iagdemo"))
        assertEquals("Your role cannot open that app.", s.login("viewer", "iagdemo", "banking"))
        assertFalse(s.isSignedIn)

        assertNull(s.login("viewer", "iagdemo"))
        assertEquals("Viewer", s.user?.role)
        assertFalse(s.canOpen("banking"))
        assertTrue(s.canOpen("reports"))
        assertFalse(s.canApprove)
        assertFalse(s.visibleModules.any { it.id == "banking" })
        assertTrue(s.visibleModules.any { it.id == "reports" })
        s.logout()

        assertEquals("Your role cannot open that app.", s.login("contractor", "iagdemo", "banking"))
        assertNull(s.login("contractor", "iagdemo"))
        assertEquals("projects", s.activeDepartmentId)
        assertTrue(s.canOpen("contract-manager"))
        assertFalse(s.canOpen("sales"))
        s.setActiveDepartment("banking")
        assertEquals("projects", s.activeDepartmentId)
        s.logout()

        assertNull(s.login("clerk", "iagdemo"))
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

        assertNull(s.login("admin", "iagdemo"))
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

        assertNull(s.login("admin", "iagdemo"))
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
        assertNull(s.saveWorkspaceUser(username = "labtech", name = "Lina Lab", role = "Lab Tech", password = "iagdemo"))
        s.logout()

        assertNull(s.login("labtech", "iagdemo"))
        assertEquals("Lab Tech", s.user?.role)
        assertTrue(s.canOpen("lab"))
        assertFalse(s.canOpen("banking"))
        assertTrue(s.canCreate("lab"))
        assertFalse(s.canApprove)
        assertEquals("lab", s.activeDepartmentId)
    }

    @Test
    fun catalogSeedsEveryWebErpFeature() {
        val modules = erpModules()
        val seed = completeCatalogSeed(modules)
        assertTrue(modules.size >= 28)
        for (module in modules) {
            for (entity in module.entities) {
                assertTrue(
                    seed.any { it.moduleId == module.id && it.entity == entity },
                    "${module.id} missing $entity",
                )
            }
        }
        val s = store()
        assertNull(s.login("admin", "iagdemo"))
        assertEquals(modules.size, s.visibleModules.size)
        assertTrue(canAccessSpecialNav("Administrator", "analytics"))
        assertTrue(s.visibleWorkspaceTools.any { it.id == "trace" })
        assertTrue(s.visibleWorkspaceTools.any { it.id == "analytics" })
        s.logout()
        assertNull(s.login("viewer", "iagdemo"))
        assertTrue(canAccessSpecialNav("Viewer", "guides"))
        assertFalse(canAccessSpecialNav("Viewer", "analytics"))
        assertTrue(s.visibleWorkspaceTools.any { it.id == "trace" })
        assertFalse(s.visibleWorkspaceTools.any { it.id == "analytics" })
    }
}
