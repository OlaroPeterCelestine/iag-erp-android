package africa.iag.erp.core

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** ERP HTTP paths. Same contract on IAG Frontend (/api rewrite) and the Go API. */
object ErpEndpoints {
    const val liveFrontendOrigin = "https://iag-frontend-five.vercel.app"
    const val liveApiOrigin = "https://api-production-b0c8d.up.railway.app"
    const val localFrontendOrigin = "http://127.0.0.1:3180"
    const val localApiOrigin = "http://127.0.0.1:8080"

    object Auth {
        const val login = "/api/auth/login"
        const val logout = "/api/auth/logout"
        const val me = "/api/auth/me"
        const val forgotPassword = "/api/auth/forgot-password"
        const val verifyResetOTP = "/api/auth/verify-reset-otp"
        const val resetPassword = "/api/auth/reset-password"
        const val changePassword = "/api/auth/change-password"
        const val profile = "/api/auth/profile"
        const val users = "/api/auth/users"
        const val roles = "/api/auth/roles"
        const val sessions = "/api/auth/sessions"
    }

    object Sync {
        const val ready = "/api/sync/ready"
        const val status = "/api/sync/status"
        const val bootstrap = "/api/sync/bootstrap"
    }

    object Data {
        const val index = "/api/data"
        const val catalog = "/api/data/catalog"
        const val modules = "/api/data/modules"
        const val records = "/api/data/records"
        const val search = "/api/data/search"
        const val summary = "/api/data/summary"
    }

    object Ledger {
        const val accounts = "/api/ledger/accounts"
        const val balances = "/api/ledger/balances"
        const val lines = "/api/ledger/lines"
        const val trialBalance = "/api/ledger/reports/trial-balance"
        const val balanceSheet = "/api/ledger/reports/balance-sheet"
        const val profitAndLoss = "/api/ledger/reports/profit-and-loss"
    }

    object Banking {
        const val balances = "/api/banking/bank-balances"
    }

    object Approvals {
        const val chain = "/api/approvals/chain"
        const val desk = "/api/approvals/desk"
        fun progress(entity: String, id: String) = "/api/approvals/${enc(entity)}/${enc(id)}"
        fun advance(entity: String, id: String) = "/api/approvals/${enc(entity)}/${enc(id)}/advance"
        fun settle(entity: String, id: String) = "/api/approvals/${enc(entity)}/${enc(id)}/settle"
        fun reject(entity: String, id: String) = "/api/approvals/${enc(entity)}/${enc(id)}/reject"
        fun amend(entity: String, id: String) = "/api/approvals/${enc(entity)}/${enc(id)}/amend"
    }

    const val health = "/api/health"
    const val recordsIndex = "/api/records"

    fun records(module: String, entity: String) = "/api/records/${enc(module)}/${enc(entity)}"

    fun record(module: String, entity: String, id: String) =
        "/api/records/${enc(module)}/${enc(entity)}/${enc(id)}"

    val chainEntities: Set<String> = setOf(
        "payment-requests",
        "oral-payment-requests",
        "requisitions",
        "general-requests",
        "fuel-requests",
        "trip-requests",
        "maintenance-requests",
        "equipment-and-vehicle-requests",
        "document-requests",
        "leave-requests",
        "payroll-runs",
    )

    fun isChainEntity(entity: String): Boolean = entity.lowercase() in chainEntities ||
        apiEntityKey(entity) in chainEntities

    fun enc(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
}
