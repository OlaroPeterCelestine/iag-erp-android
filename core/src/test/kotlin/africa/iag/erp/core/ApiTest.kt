package africa.iag.erp.core

import java.net.URL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FakeTransport : ErpTransporting {
    var originPrefix = "https://iag-frontend-five.vercel.app"
    val users = linkedMapOf<String, Triple<String, Map<String, Any?>, String>>()
    val collections = linkedMapOf<String, MutableList<Map<String, Any?>>>()
    var failNetwork = false
    var lastPath = ""
    val paths = mutableListOf<String>()

    override fun data(method: String, url: String, headers: Map<String, String>, body: ByteArray?): ErpHttpResponse {
        if (failNetwork) throw java.net.ConnectException("offline")
        val path = URL(url).path
        lastPath = path
        paths.add(path)
        fun json(status: Int, payload: Map<String, Any?>): ErpHttpResponse {
            return ErpHttpResponse(status, MiniJson.stringify(payload).toByteArray())
        }
        if (path == "/api/auth/login") {
            val parsed = MiniJson.parseObject(String(body ?: ByteArray(0)))
            val username = jsonText(parsed["emailOrUsername"])?.lowercase().orEmpty()
            val password = jsonText(parsed["password"]).orEmpty()
            val row = users[username]
            if (row == null || row.first != password) {
                return json(401, mapOf("ok" to false, "error" to "Invalid email/username or password."))
            }
            return json(
                200,
                mapOf(
                    "ok" to true,
                    "data" to mapOf(
                        "user" to row.second,
                        "token" to row.third,
                        "expiresAt" to "2026-12-31T00:00:00Z",
                        "tokenType" to "Bearer",
                    ),
                ),
            )
        }
        if (path == "/api/auth/me") {
            val auth = headers["Authorization"].orEmpty()
            val match = users.values.firstOrNull { auth.endsWith(it.third) }
                ?: return json(401, mapOf("ok" to false, "error" to "Unauthorized"))
            return json(200, mapOf("data" to match.second))
        }
        if (path == "/api/auth/forgot-password") {
            return json(200, mapOf("ok" to true, "message" to "If an account exists, a reset code has been sent."))
        }
        if (path == "/api/auth/logout") return json(200, mapOf("ok" to true))
        if (path == "/api/auth/profile") {
            val parsed = MiniJson.parseObject(String(body ?: ByteArray(0)))
            val user = (users.values.firstOrNull()?.second ?: emptyMap()).toMutableMap()
            user.putAll(parsed)
            return json(200, mapOf("data" to user))
        }
        if (path == "/api/auth/users") return json(200, mapOf("data" to users.values.map { it.second }))
        if (path == "/api/auth/roles") {
            return json(
                200,
                mapOf(
                    "data" to listOf(
                        mapOf(
                            "id" to "role-admin-db",
                            "name" to "Administrator",
                            "canView" to "Yes",
                            "canCreate" to "Yes",
                            "canEdit" to "Yes",
                            "canDelete" to "Yes",
                            "system" to true,
                        ),
                        mapOf(
                            "id" to "r1",
                            "name" to "Field Clerk",
                            "canView" to "Yes",
                            "canCreate" to "Yes",
                            "canEdit" to "No",
                            "canDelete" to "No",
                            "system" to false,
                            "pagePermissions" to mapOf(
                                "sales" to mapOf(
                                    "canView" to "Yes",
                                    "canCreate" to "Yes",
                                    "canEdit" to "No",
                                    "canDelete" to "No",
                                ),
                            ),
                        ),
                    ),
                ),
            )
        }
        if (path == "/api/approvals/desk") {
            return json(200, mapOf("ok" to true, "data" to mapOf("items" to emptyList<Any>(), "count" to 0)))
        }
        if (path.startsWith("/api/approvals/")) {
            val parts = path.removePrefix("/api/approvals/").split("/")
            val action = parts.lastOrNull() ?: "advance"
            val id = parts.getOrNull(1).orEmpty()
            val status = if (action == "reject") "Rejected" else "PM Approved"
            return json(
                200,
                mapOf(
                    "ok" to true,
                    "data" to mapOf(
                        "id" to id,
                        "status" to status,
                        "record" to mapOf("id" to id, "status" to status, "name" to "PR-1"),
                    ),
                ),
            )
        }
        if (path.startsWith("/api/records/")) {
            val parts = path.removePrefix("/api/records/").split("/")
            if (parts.size >= 2) {
                val key = "${parts[0]}/${parts[1]}"
                if (method == "PUT") return json(200, mapOf("ok" to true))
                if (method == "POST" && parts.size == 2) {
                    val row = MiniJson.parseObject(String(body ?: ByteArray(0))).toMutableMap()
                    if (jsonText(row["id"]) == null) row["id"] = "new-1"
                    collections.getOrPut(key) { mutableListOf() }.add(row)
                    return json(201, mapOf("ok" to true, "data" to row))
                }
                if (parts.size >= 3) {
                    val id = parts[2]
                    if (method == "DELETE") {
                        collections[key] = (collections[key] ?: mutableListOf()).filter { jsonText(it["id"]) != id }.toMutableList()
                        return json(200, mapOf("ok" to true))
                    }
                    if (method == "PATCH") {
                        val patch = MiniJson.parseObject(String(body ?: ByteArray(0)))
                        val list = collections.getOrPut(key) { mutableListOf() }
                        val idx = list.indexOfFirst { jsonText(it["id"]) == id }
                        if (idx < 0) return json(404, mapOf("ok" to false, "error" to "not found"))
                        val row = list[idx].toMutableMap()
                        row.putAll(patch)
                        list[idx] = row
                        return json(200, mapOf("ok" to true, "data" to row))
                    }
                    val row = collections[key]?.firstOrNull { jsonText(it["id"]) == id } ?: emptyMap()
                    return json(200, mapOf("data" to row))
                }
                return json(200, mapOf("data" to (collections[key] ?: emptyList())))
            }
        }
        return json(404, mapOf("ok" to false, "error" to "not found"))
    }
}

class ApiTest {
    @Test
    fun defaultFrontendOriginIsTheLiveApp() {
        assertEquals("https://iag-frontend-five.vercel.app", ErpConfig.liveFrontendOrigin)
        assertEquals("https://iag-frontend-five.vercel.app", ErpConfig.origin())
        assertEquals("https://iag-frontend-five.vercel.app", ErpConfig.sanitizeOrigin("iag-frontend-five.vercel.app/"))
        val store = MemoryKeyValueStore()
        ErpConfig.saveOrigin("http://127.0.0.1:3180", store)
        assertEquals("http://127.0.0.1:3180", ErpConfig.origin(store))
    }

    @Test
    fun entityKeysMatchFrontendSlugs() {
        assertEquals("bank-and-cash-accounts", apiEntityKey("Bank & Cash Accounts"))
        assertEquals("inventory-locations", apiEntityKey("Warehouses & Locations"))
        assertEquals("payment-requests", apiEntityKey("Payment Requests (IPC)"))
        assertEquals("projects", apiEntityKey("New Project"))
        assertEquals("banking", apiStorageTarget("receipts-payments", "Receipts").module)
        assertEquals("attendance", apiStorageTarget("clock-in", "My punches").entity)
        assertEquals("requests", apiStorageTarget("general-requests", "General Requests").module)
        assertTrue(ErpEndpoints.isChainEntity("Payment Requests (IPC)"))
        assertTrue(ErpEndpoints.isChainEntity("Material Requests"))
        assertFalse(ErpEndpoints.isChainEntity("Expense Claims"))
    }

    @Test
    fun remoteLoginUsesFrontendAndFallsBackWhenOffline() {
        val transport = FakeTransport()
        transport.users["admin"] = Triple(
            "Secret123!",
            mapOf("id" to "u1", "username" to "admin", "name" to "Admin", "email" to "admin@iag.africa", "role" to "Administrator"),
            "tok-admin",
        )
        val persistence = MemoryKeyValueStore()
        val api = ErpApi(origin = ErpConfig.liveFrontendOrigin, transport = transport)
        val s = ErpStore(persistence = persistence, api = api)
        s.load()
        assertNull(s.loginAsync("admin", "Secret123!"))
        assertTrue(s.remoteSession)
        assertEquals("admin", s.user?.username)
        assertEquals(ErpConfig.liveFrontendOrigin, s.frontendOrigin)
        assertEquals("tok-admin", api.token)
        assertEquals("role-admin-db", s.roleOf("Administrator")?.id)
        assertEquals("r1", s.roleOf("Field Clerk")?.id)
        assertEquals(true, s.roleOf("Field Clerk")?.crud?.create)

        val reloaded = ErpStore(persistence = persistence, api = ErpApi(origin = ErpConfig.liveFrontendOrigin, transport = transport))
        reloaded.load()
        assertEquals("role-admin-db", reloaded.roleOf("Administrator")?.id)
        assertEquals("r1", reloaded.roleOf("Field Clerk")?.id)

        transport.failNetwork = true
        val offline = ErpStore(persistence = MemoryKeyValueStore(), api = ErpApi(origin = ErpConfig.liveFrontendOrigin, transport = transport))
        offline.load()
        offline.seedTestPasswords("unit-test-login")
        assertNull(offline.loginAsync("admin", "unit-test-login"))
        assertTrue(offline.isSignedIn)
        assertFalse(offline.remoteSession)

        val unreachable = ErpStore(persistence = MemoryKeyValueStore(), api = ErpApi(origin = ErpConfig.liveFrontendOrigin, transport = transport))
        unreachable.load()
        assertEquals(
            "Can't reach the workspace.",
            unreachable.loginAsync("nobody", "nope"),
        )
    }

    @Test
    fun wrongFrontendPasswordDoesNotUseLocalTrial() {
        val transport = FakeTransport()
        transport.users["admin"] = Triple(
            "RemoteOnly!",
            mapOf("username" to "admin", "name" to "Admin", "role" to "Administrator"),
            "tok",
        )
        val s = ErpStore(persistence = MemoryKeyValueStore(), api = ErpApi(origin = ErpConfig.liveFrontendOrigin, transport = transport))
        s.load()
        s.seedTestPasswords("unit-test-login")
        assertEquals("Invalid email/username or password.", s.loginAsync("admin", "unit-test-login"))
        assertFalse(s.isSignedIn)

        assertEquals(
            describeLiveLoginFailure("shortpw", "Invalid email/username or password."),
            s.loginAsync("admin", "shortpw"),
        )
        assertFalse(s.isSignedIn)
    }

    @Test
    fun refreshEntityMapsFrontendRecords() {
        val transport = FakeTransport()
        transport.users["clerk"] = Triple(
            "Secret123!",
            mapOf("username" to "clerk", "name" to "Clerk", "role" to "Clerk"),
            "tok-clerk",
        )
        transport.collections["sales/customers"] = mutableListOf(
            mapOf("id" to "c1", "name" to "Cafe Javas", "status" to "Active", "amount" to "6200000", "description" to "Kampala"),
        )
        val api = ErpApi(origin = ErpConfig.liveFrontendOrigin, transport = transport)
        val s = ErpStore(persistence = MemoryKeyValueStore(), api = api)
        s.load()
        assertNull(s.loginAsync("clerk", "Secret123!"))
        s.refreshEntity("sales", "Customers")
        assertEquals("Cafe Javas", s.recordsFor("sales", "Customers").first().title)
        assertEquals("/api/records/sales/customers", transport.lastPath)
    }

    @Test
    fun itemCrudAndChainApprovalUseRestEndpoints() {
        val transport = FakeTransport()
        transport.users["admin"] = Triple(
            "Secret123!",
            mapOf("username" to "admin", "name" to "Admin", "role" to "Administrator"),
            "tok-admin",
        )
        val api = ErpApi(origin = ErpConfig.liveFrontendOrigin, transport = transport)
        val s = ErpStore(persistence = MemoryKeyValueStore(), api = api)
        s.load()
        assertNull(s.loginAsync("admin", "Secret123!"))
        val draft = ErpRecord(
            id = "pr-9",
            moduleId = "projects",
            entity = "Payment Requests (IPC)",
            title = "PR-9",
            subtitle = "IPC",
            status = "Submitted",
            date = "2026-09-14",
        )
        s.addRecord(draft)
        assertNull(s.approveRecordAsync(draft))
        assertTrue(transport.paths.contains("/api/approvals/payment-requests/pr-9/advance"))
        assertEquals("PM Approved", draft.status)
        assertNull(s.deleteRecordAsync(draft))
        assertTrue(transport.paths.contains("/api/records/projects/payment-requests/pr-9"))
        assertTrue(s.recordsFor("projects", "Payment Requests (IPC)").isEmpty())
    }
}
