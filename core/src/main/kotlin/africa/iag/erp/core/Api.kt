package africa.iag.erp.core

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class ErpRemoteUser(
    val id: String = "",
    val username: String,
    val name: String,
    val email: String = "",
    val role: String,
    val roleId: String = "",
    val phone: String = "",
    val title: String = "",
    val crud: Crud? = null,
    val pagePermissions: Map<String, Crud> = emptyMap(),
) {
    val authUser: AuthUser
        get() = AuthUser(
            username = if (username.isEmpty()) email.lowercase() else username.lowercase(),
            name = name.ifEmpty { username },
            role = role.ifEmpty { "Viewer" },
            email = email,
            phone = phone,
            title = title,
        )
}

data class ErpRemoteSession(
    val user: ErpRemoteUser,
    val token: String,
    val expiresAt: String = "",
)

sealed class ErpApiError(message: String) : Exception(message) {
    class Network(text: String) : ErpApiError(text)
    class Unauthorized(text: String) : ErpApiError(text)
    class Http(val code: Int, text: String) : ErpApiError(text)

    val isNetwork: Boolean get() = this is Network
    val isUnauthorized: Boolean get() = this is Unauthorized
    val isNotFound: Boolean get() = this is Http && code == 404
    val isConflict: Boolean get() = this is Http && code == 409
}

data class ErpHttpResponse(val status: Int, val body: ByteArray)

interface ErpTransporting {
    fun data(method: String, url: String, headers: Map<String, String>, body: ByteArray?): ErpHttpResponse
}

class UrlErpTransport : ErpTransporting {
    override fun data(method: String, url: String, headers: Map<String, String>, body: ByteArray?): ErpHttpResponse {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        conn.instanceFollowRedirects = true
        conn.doInput = true
        setRequestMethod(conn, method)
        for ((key, value) in headers) conn.setRequestProperty(key, value)
        if (body != null) {
            conn.doOutput = true
            conn.outputStream.use { out ->
                out.write(body)
                out.flush()
            }
        }
        val status = try {
            conn.responseCode
        } catch (error: Exception) {
            throw error
        }
        val stream = if (status >= 400) conn.errorStream ?: conn.inputStream else conn.inputStream
        val bytes = stream?.readBytes() ?: ByteArray(0)
        conn.disconnect()
        return ErpHttpResponse(status, bytes)
    }

    private fun setRequestMethod(conn: HttpURLConnection, method: String) {
        try {
            conn.requestMethod = method
        } catch (_: Exception) {
            try {
                val field = HttpURLConnection::class.java.getDeclaredField("method")
                field.isAccessible = true
                field.set(conn, method)
            } catch (_: Exception) {
                conn.requestMethod = if (method == "PATCH") "POST" else method
                if (method == "PATCH") conn.setRequestProperty("X-HTTP-Method-Override", "PATCH")
            }
        }
    }
}

interface ErpApiClient {
    var origin: String
    var token: String?
    fun login(username: String, password: String, keepSignedIn: Boolean = true): Result<ErpRemoteSession>
    fun me(): Result<ErpRemoteUser>
    fun logout()
    fun requestPasswordReset(username: String): Result<String>
    fun updateProfile(name: String, email: String, phone: String, title: String): Result<ErpRemoteUser>
    fun getRecords(module: String, entity: String): Result<List<Map<String, Any?>>>
    fun getRecord(module: String, entity: String, id: String): Result<Map<String, Any?>>
    fun createRecord(module: String, entity: String, record: Map<String, String>): Result<Map<String, Any?>>
    fun patchRecord(module: String, entity: String, id: String, record: Map<String, String>): Result<Map<String, Any?>>
    fun deleteRecord(module: String, entity: String, id: String): Result<Unit>
    fun putRecords(module: String, entity: String, records: List<Map<String, String>>, removeIds: List<String> = emptyList()): Result<Unit>
    fun approvalDesk(): Result<Map<String, Any?>>
    fun approvalAction(entity: String, id: String, action: String, comment: String): Result<Map<String, Any?>>
    fun search(query: String): Result<List<Map<String, Any?>>>
    fun summary(): Result<Map<String, Any?>>
    fun listUsers(): Result<List<Map<String, Any?>>>
    fun listRoles(): Result<List<Map<String, Any?>>>
}

class ErpApi(
    origin: String = ErpConfig.liveFrontendOrigin,
    private val transport: ErpTransporting = UrlErpTransport(),
) : ErpApiClient {
    override var origin: String = ErpConfig.sanitizeOrigin(origin)
    override var token: String? = null

    override fun login(username: String, password: String, keepSignedIn: Boolean): Result<ErpRemoteSession> {
        val json = request(
            ErpEndpoints.Auth.login,
            "POST",
            mapOf(
                "emailOrUsername" to username,
                "password" to password,
                "keepSignedIn" to keepSignedIn,
            ),
            authed = false,
        )
        val data = json.getOrElse { return Result.failure(it) }
        val payload = data["data"] as? Map<*, *> ?: return Result.failure(ErpApiError.Http(200, "Login succeeded but no API token was issued"))
        val token = jsonText(payload["token"]).orEmpty()
        val userJson = (payload["user"] as? Map<*, *>)?.asAnyMap()
        val user = userJson?.let { parseUser(it) }
        if (token.isEmpty() || user == null) {
            return Result.failure(ErpApiError.Http(200, jsonText(data["error"]) ?: "Login succeeded but no API token was issued"))
        }
        this.token = token
        return Result.success(ErpRemoteSession(user = user, token = token, expiresAt = jsonText(payload["expiresAt"]).orEmpty()))
    }

    override fun me(): Result<ErpRemoteUser> {
        val json = request(ErpEndpoints.Auth.me, "GET", null, authed = true)
        val data = json.getOrElse { return Result.failure(it) }
        val payload = (data["data"] as? Map<*, *>)?.asAnyMap() ?: data
        val user = parseUser(payload) ?: return Result.failure(ErpApiError.Http(200, "Could not read the signed-in user."))
        return Result.success(user)
    }

    override fun logout() {
        request(ErpEndpoints.Auth.logout, "POST", emptyMap<String, Any?>(), authed = true)
        token = null
    }

    override fun requestPasswordReset(username: String): Result<String> {
        val json = request(ErpEndpoints.Auth.forgotPassword, "POST", mapOf("emailOrUsername" to username), authed = false)
        val data = json.getOrElse { return Result.failure(it) }
        return Result.success(jsonText(data["message"]) ?: "If an account exists, a reset code was sent.")
    }

    override fun updateProfile(name: String, email: String, phone: String, title: String): Result<ErpRemoteUser> {
        val json = request(
            ErpEndpoints.Auth.profile,
            "PATCH",
            mapOf("name" to name, "email" to email, "phone" to phone, "title" to title),
            authed = true,
        )
        val data = json.getOrElse { return Result.failure(it) }
        val payload = (data["data"] as? Map<*, *>)?.asAnyMap() ?: data
        return Result.success(parseUser(payload) ?: ErpRemoteUser(username = "", name = name, email = email, role = "", phone = phone, title = title))
    }

    override fun getRecords(module: String, entity: String): Result<List<Map<String, Any?>>> {
        val json = request(ErpEndpoints.records(module, entity), "GET", null, authed = true)
        val data = json.getOrElse { return Result.failure(it) }
        return Result.success(asObjectList(data["data"]))
    }

    override fun getRecord(module: String, entity: String, id: String): Result<Map<String, Any?>> {
        val json = request(ErpEndpoints.record(module, entity, id), "GET", null, authed = true)
        val data = json.getOrElse { return Result.failure(it) }
        val row = (data["data"] as? Map<*, *>)?.asAnyMap()
            ?: return Result.failure(ErpApiError.Http(200, "Record missing in response."))
        return Result.success(row)
    }

    override fun createRecord(module: String, entity: String, record: Map<String, String>): Result<Map<String, Any?>> {
        val json = request(ErpEndpoints.records(module, entity), "POST", record, authed = true)
        val data = json.getOrElse { return Result.failure(it) }
        return Result.success((data["data"] as? Map<*, *>)?.asAnyMap() ?: record.mapValues { it.value })
    }

    override fun patchRecord(module: String, entity: String, id: String, record: Map<String, String>): Result<Map<String, Any?>> {
        val json = request(ErpEndpoints.record(module, entity, id), "PATCH", record, authed = true)
        val data = json.getOrElse { return Result.failure(it) }
        return Result.success((data["data"] as? Map<*, *>)?.asAnyMap() ?: record.mapValues { it.value })
    }

    override fun deleteRecord(module: String, entity: String, id: String): Result<Unit> {
        val json = request(ErpEndpoints.record(module, entity, id), "DELETE", null, authed = true)
        json.getOrElse { return Result.failure(it) }
        return Result.success(Unit)
    }

    override fun putRecords(
        module: String,
        entity: String,
        records: List<Map<String, String>>,
        removeIds: List<String>,
    ): Result<Unit> {
        val body = linkedMapOf<String, Any?>(
            "records" to records,
            "mode" to "merge",
        )
        if (removeIds.isNotEmpty()) body["removeIds"] = removeIds
        val json = request(ErpEndpoints.records(module, entity), "PUT", body, authed = true)
        json.getOrElse { return Result.failure(it) }
        return Result.success(Unit)
    }

    override fun approvalDesk(): Result<Map<String, Any?>> {
        val json = request(ErpEndpoints.Approvals.desk, "GET", null, authed = true)
        val data = json.getOrElse { return Result.failure(it) }
        return Result.success((data["data"] as? Map<*, *>)?.asAnyMap() ?: data)
    }

    override fun approvalAction(entity: String, id: String, action: String, comment: String): Result<Map<String, Any?>> {
        val path = when (action) {
            "reject" -> ErpEndpoints.Approvals.reject(entity, id)
            "amend" -> ErpEndpoints.Approvals.amend(entity, id)
            "settle" -> ErpEndpoints.Approvals.settle(entity, id)
            else -> ErpEndpoints.Approvals.advance(entity, id)
        }
        val json = request(path, "POST", mapOf("comment" to comment), authed = true)
        val data = json.getOrElse { return Result.failure(it) }
        return Result.success((data["data"] as? Map<*, *>)?.asAnyMap() ?: data)
    }

    override fun search(query: String): Result<List<Map<String, Any?>>> {
        val q = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
        val json = request("${ErpEndpoints.Data.search}?q=$q&limit=30", "GET", null, authed = true)
        val data = json.getOrElse { return Result.failure(it) }
        return Result.success(asObjectList(data["data"]))
    }

    override fun summary(): Result<Map<String, Any?>> {
        val json = request(ErpEndpoints.Data.summary, "GET", null, authed = true)
        val data = json.getOrElse { return Result.failure(it) }
        return Result.success((data["data"] as? Map<*, *>)?.asAnyMap() ?: data)
    }

    override fun listUsers(): Result<List<Map<String, Any?>>> {
        val json = request(ErpEndpoints.Auth.users, "GET", null, authed = true)
        val data = json.getOrElse { return Result.failure(it) }
        return Result.success(asObjectList(data["data"]))
    }

    override fun listRoles(): Result<List<Map<String, Any?>>> {
        val json = request(ErpEndpoints.Auth.roles, "GET", null, authed = true)
        val data = json.getOrElse { return Result.failure(it) }
        return Result.success(asObjectList(data["data"]))
    }

    private fun request(path: String, method: String, body: Map<String, *>?, authed: Boolean): Result<Map<String, Any?>> {
        val root = if (origin.isEmpty()) ErpConfig.liveFrontendOrigin else origin
        val url = root + path
        val headers = linkedMapOf(
            "Accept" to "application/json",
            "User-Agent" to "IAG-Central-Android/$APP_VERSION",
        )
        var payload: ByteArray? = null
        if (body != null) {
            headers["Content-Type"] = "application/json"
            payload = MiniJson.stringify(body).toByteArray(StandardCharsets.UTF_8)
        }
        if (authed && !token.isNullOrEmpty()) {
            headers["Authorization"] = "Bearer $token"
        }
        return try {
            val response = transport.data(method, url, headers, payload)
            val parsed = if (response.body.isEmpty()) emptyMap() else MiniJson.parseObject(String(response.body, StandardCharsets.UTF_8))
            val errorText = jsonText(parsed["error"]) ?: "HTTP ${response.status}"
            when {
                response.status == 401 || response.status == 403 ->
                    Result.failure(ErpApiError.Unauthorized(if (errorText.isEmpty()) "Invalid email/username or password." else errorText))
                response.status == 0 -> Result.failure(ErpApiError.Network("Can't reach the workspace."))
                response.status >= 500 -> Result.failure(ErpApiError.Network(if (errorText.isEmpty()) "The workspace is unavailable." else errorText))
                response.status >= 400 -> Result.failure(ErpApiError.Http(response.status, errorText))
                else -> Result.success(parsed)
            }
        } catch (_: Exception) {
            Result.failure(ErpApiError.Network("Can't reach the workspace."))
        }
    }

    private fun parseUser(json: Map<String, Any?>): ErpRemoteUser? {
        val username = jsonText(json["username"]).orEmpty()
        val email = jsonText(json["email"]).orEmpty()
        if (username.isEmpty() && email.isEmpty()) return null
        val name = jsonText(json["name"]) ?: jsonText(json["fullName"]) ?: username
        val hasCrud = json["canView"] != null || json["canCreate"] != null || json["canEdit"] != null || json["canDelete"] != null
        return ErpRemoteUser(
            id = jsonText(json["id"]) ?: jsonText(json["uid"]).orEmpty(),
            username = username.ifEmpty { email },
            name = name,
            email = email,
            role = jsonText(json["role"]) ?: "Viewer",
            roleId = jsonText(json["roleId"]).orEmpty(),
            phone = jsonText(json["phone"]).orEmpty(),
            title = jsonText(json["title"]).orEmpty(),
            crud = if (hasCrud) Crud.fromFlags(json) else null,
            pagePermissions = parsePagePermissions(json["pagePermissions"]),
        )
    }

    private fun asObjectList(value: Any?): List<Map<String, Any?>> {
        val list = value as? List<*> ?: return emptyList()
        return list.mapNotNull { (it as? Map<*, *>)?.asAnyMap() }
    }
}

internal fun jsonText(value: Any?): String? {
    if (value == null) return null
    if (value is String) {
        val trimmed = value.trim()
        return trimmed.ifEmpty { null }
    }
    if (value is Number || value is Boolean) return value.toString()
    return value.toString()
}

@Suppress("UNCHECKED_CAST")
internal fun Map<*, *>.asAnyMap(): Map<String, Any?> =
    entries.associate { it.key.toString() to it.value }
