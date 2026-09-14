package africa.iag.erp.core

fun erpModules(): List<ErpModule> = listOf(
    ErpModule(
        id = "banking",
        label = "Banking",
        group = "Treasury",
        description = "Bank and cash accounts linked to the chart of accounts, then statements, transfers, and reconciliations.",
        icon = "account_balance",
        color = 0xFF0369A1,
        entities = listOf("Bank & Cash Accounts", "Inter Account Transfers", "Bank Statements", "Reconciliations"),
        seed = listOf(
            seedRecord("banking", "Bank & Cash Accounts", "Stanbic Current — UGX", "**** 4412 · Main Shop", "Active", amount = 186400000.0, fields = mapOf("Institution" to "Stanbic", "Currency" to "UGX", "GL" to "1000 Cash-UGX")),
            seedRecord("banking", "Bank & Cash Accounts", "Centenary — Operations", "**** 8821 · HQ", "Active", amount = 54200000.0, fields = mapOf("Institution" to "Centenary", "Currency" to "UGX")),
            seedRecord("banking", "Bank & Cash Accounts", "Cash till — Front", "POS float", "Active", amount = 850000.0),
        ),
    ),
    ErpModule(
        id = "receipts-payments",
        label = "Receipts & Payments",
        group = "Treasury",
        description = "Money in and money out — receipts, payments, and the rules that categorise them.",
        icon = "swap_horiz",
        color = 0xFF0F766E,
        entities = listOf("Receipts", "Payments", "Receipt Rules", "Payment Rules"),
        seed = listOf(
            seedRecord("receipts-payments", "Receipts", "RCPT-2026-0412", "Nakumatt Kisementi", "Cleared", amount = 12800000.0),
        ),
    ),
    ErpModule(
        id = "expense-claims",
        label = "Expense Claims",
        group = "Requests",
        description = "Payers and claims paid with personal funds or allowance rates.",
        icon = "receipt_long",
        color = 0xFFB45309,
        entities = listOf("Expense Claim Payers", "Expense Claims"),
        approvalEntities = setOf("Expense Claims"),
        seed = listOf(
            seedRecord("expense-claims", "Expense Claims", "EXP-2026-019", "Field travel — Masaka", "Pending", amount = 420000.0, fields = mapOf("Claimant" to "Daniel Okello", "Account" to "Travel")),
            seedRecord("expense-claims", "Expense Claims", "EXP-2026-018", "Client lunch", "Approved", amount = 180000.0, fields = mapOf("Claimant" to "Sarah Nambi")),
        ),
    ),
    ErpModule(
        id = "general-requests",
        label = "General Requests",
        group = "Requests",
        description = "General (non-project) requests through accounts, GM, CEO, and finance.",
        icon = "assignment",
        color = 0xFF7C3AED,
        entities = listOf("General Requests"),
        approvalEntities = setOf("General Requests"),
        seed = listOf(
            seedRecord("general-requests", "General Requests", "REQ-2026-077", "New laptop for QA", "Pending", fields = mapOf("Requested by" to "Lab Manager", "Priority" to "High")),
        ),
    ),
    ErpModule(
        id = "oral-payment-requests",
        label = "Oral Payment Requests",
        group = "Requests",
        description = "Verbal payment requests captured and sent through the approval desk.",
        icon = "mic",
        color = 0xFFC2410C,
        entities = listOf("Oral Payment Requests"),
        approvalEntities = setOf("Oral Payment Requests"),
        seed = listOf(
            seedRecord("oral-payment-requests", "Oral Payment Requests", "OPR-2026-011", "Casual wages — loading", "Pending", amount = 350000.0, fields = mapOf("Payee" to "Site casuals", "Purpose" to "Loading bags")),
        ),
    ),
    ErpModule(
        id = "sales",
        label = "Sales",
        group = "Commercial",
        description = "Customers, quotes, orders, invoices, credit notes, and related sales documents.",
        icon = "storefront",
        color = 0xFF059669,
        entities = listOf("Customers", "Customer Ledgers", "Sales Quotes", "Sales Orders", "Sales Invoices", "Credit Notes", "Late Payment Fees", "Delivery Notes", "Billable Time", "Billable Expenses", "Withholding Tax Receipts", "Customer Portals", "Recurring Sales Invoices", "Revenue Contracts"),
        seed = listOf(
            seedRecord("sales", "Customers", "Cafe Javas Kampala", "CUST-0142", "Active", amount = 6200000.0, fields = mapOf("Email" to "orders@cafejavas.ug", "Currency" to "UGX")),
            seedRecord("sales", "Sales Invoices", "SI-2026-188", "Cafe Javas Kampala", "Posted", amount = 4200000.0),
        ),
    ),
    ErpModule(
        id = "purchases",
        label = "Purchases",
        group = "Commercial",
        description = "Suppliers, quotes, orders, invoices, goods receipts, and withholding tax.",
        icon = "shopping_cart",
        color = 0xFF2563EB,
        entities = listOf("Suppliers", "Supplier Ledgers", "Purchase Quotes", "Purchase Orders", "Purchase Invoices", "Debit Notes", "Goods Receipts", "Recurring Purchase Invoices", "Withholding Tax"),
        seed = listOf(
            seedRecord("purchases", "Suppliers", "Kyagalanyi Coffee Ltd", "SUP-0031", "Active"),
            seedRecord("purchases", "Purchase Orders", "PO-2026-044", "Green bean — AA", "Open", amount = 98000000.0),
        ),
    ),
    ErpModule(
        id = "inventory",
        label = "Inventory",
        group = "Inventory & production",
        description = "Items, warehouses, stock movements, green bean, roast, packaging, and stocktakes.",
        icon = "inventory_2",
        color = 0xFF0E7490,
        entities = listOf("Inventory Items", "Non-inventory Items", "Inventory Kits", "Stock In", "Inventory Transfers", "Warehouses & Locations", "Inventory Write-offs", "Inventory Sales", "Green Bean Intakes", "Production Orders", "Roast Batches", "Quality Checks", "Packaging Runs", "Stocktakes", "Landed Costs"),
        seed = listOf(
            seedRecord("inventory", "Inventory Items", "Arabica AA — green", "INV-BEAN-AA", "Active", fields = mapOf("Location" to "Main warehouse", "On hand" to "4,200 kg")),
            seedRecord("inventory", "Warehouses & Locations", "Main warehouse", "Namanve", "Active"),
        ),
    ),
    ErpModule(
        id = "projects",
        label = "Project Manager",
        group = "Projects",
        description = "Projects, Gantt, material and IPC requests, equipment, documents, and work programs.",
        icon = "work",
        color = 0xFF4F46E5,
        entities = listOf("New Project", "Project Updates", "Gantt Chart", "Project Managers", "Material Requests", "Payment Requests (IPC)", "Equipment & Vehicle Requests", "Document Requests", "Work Programs", "Variations of Work"),
        approvalEntities = setOf("Payment Requests (IPC)", "Material Requests", "Equipment & Vehicle Requests", "Document Requests"),
        seed = listOf(
            seedRecord("projects", "New Project", "Roastery expansion", "PRJ-004", "Active", amount = 420000000.0, fields = mapOf("Manager" to "Peter Olaro", "Site" to "Namanve")),
            seedRecord("projects", "Payment Requests (IPC)", "IPC-2026-006", "Civil works — certificate 2", "Pending", amount = 48000000.0, fields = mapOf("Project" to "Roastery expansion", "Payee" to "Mukwano Builders")),
            seedRecord("projects", "Material Requests", "MR-2026-021", "Cement and steel", "Approved"),
        ),
    ),
    ErpModule(
        id = "contract-manager",
        label = "Contract Manager",
        group = "Projects",
        description = "Contractors, contractor invoices, and contractor ledgers under a project.",
        icon = "handshake",
        color = 0xFF047857,
        entities = listOf("Contractors", "Contractor Invoices", "Contractor Ledgers"),
        seed = listOf(
            seedRecord("contract-manager", "Contractors", "Mukwano Builders", "CTR-012", "Active"),
        ),
    ),
    ErpModule(
        id = "fleet",
        label = "Fleet",
        group = "Operations",
        description = "Vehicles, drivers, fuel, trips, maintenance, map analytics, and fleet cost.",
        icon = "local_shipping",
        color = 0xFFD97706,
        entities = listOf("Vehicles", "Drivers", "Fuel Requests", "Fuel Logs", "Trip Requests", "Maintenance Requests", "Map Analytics", "Service Reminders", "Fleet Cost Report"),
        approvalEntities = setOf("Fuel Requests", "Trip Requests", "Maintenance Requests"),
        seed = listOf(
            seedRecord("fleet", "Vehicles", "UAX 221K", "Isuzu NPR", "Active"),
            seedRecord("fleet", "Fuel Requests", "FUEL-2026-044", "UAX 221K · Namanve", "Pending", amount = 420000.0),
        ),
    ),
    ErpModule(
        id = "security",
        label = "Security",
        group = "Operations",
        description = "Gate passes, visitor passes, and security incidents.",
        icon = "security",
        color = 0xFF334155,
        entities = listOf("Gate Passes", "Visitor Passes", "Security Incidents"),
        seed = listOf(
            seedRecord("security", "Visitor Passes", "VP-2026-331", "URA audit team", "Open", date = todayIsoDate()),
        ),
    ),
    ErpModule(
        id = "crm",
        label = "CRM",
        group = "Commercial",
        description = "Leads, opportunities, contacts, follow-ups, and complaints.",
        icon = "groups",
        color = 0xFFBE123C,
        entities = listOf("Leads", "Opportunities", "Contacts", "Follow-ups", "Complaints"),
        seed = listOf(
            seedRecord("crm", "Leads", "Shoprite Lugogo", "Retail listing", "Open", amount = 0.0, fields = mapOf("Owner" to "Sarah Nambi", "Source" to "Walk-in")),
        ),
    ),
    ErpModule(
        id = "logistics",
        label = "Logistics",
        group = "Operations",
        description = "Shipments, dispatch board, routes, proof of delivery, and carriers.",
        icon = "map",
        color = 0xFF0F766E,
        entities = listOf("Shipments", "Dispatch Board", "Routes", "Proof of Delivery", "Carriers"),
        seed = listOf(
            seedRecord("logistics", "Shipments", "SHP-2026-077", "Namanve → Javas Kololo", "In transit"),
        ),
    ),
    ErpModule(
        id = "distribution",
        label = "Distribution",
        group = "Operations",
        description = "Distribution orders, picking, packing, delivery runs, allocations, and returns.",
        icon = "share",
        color = 0xFF0D9488,
        entities = listOf("Distribution Orders", "Picking Lists", "Packing Lists", "Delivery Runs", "Stock Allocations", "Distribution Returns"),
        seed = listOf(
            seedRecord("distribution", "Delivery Runs", "RUN-2026-14", "Kampala city loop", "Scheduled", date = todayIsoDate()),
        ),
    ),
    ErpModule(
        id = "rnd",
        label = "R&D",
        group = "Quality",
        description = "Experiments, formulations, sensory panels, spec sheets, pilots, and cost models.",
        icon = "science",
        color = 0xFF7C3AED,
        entities = listOf("Experiments", "Formulations", "Sensory Panels", "Spec Sheets", "Pilot Batches", "Cost Models", "AI Insights"),
    ),
    ErpModule(
        id = "lab",
        label = "Lab",
        group = "Quality",
        description = "Lab requests, samples, trials, methods, calibrations, results, and stability.",
        icon = "biotech",
        color = 0xFF6D28D9,
        entities = listOf("Product Simulations", "Lab Requests", "Lab Samples", "Lab Trials", "Lab Methods", "Instrument Calibrations", "Lab Results", "Stability Studies"),
        seed = listOf(
            seedRecord("lab", "Lab Results", "LAB-2026-033", "Cupping — AA lot 12", "Verified"),
        ),
    ),
    ErpModule(
        id = "qa",
        label = "Quality Assurance",
        group = "Quality",
        description = "Incoming and in-process checks, release decisions, NCs, CAPA, and hold logs.",
        icon = "verified",
        color = 0xFF0369A1,
        entities = listOf("Quality Checks", "Incoming Inspections", "In-process Checks", "Release Decisions", "Non-conformances", "CAPA Actions", "Hold & Release Log"),
        seed = listOf(
            seedRecord("qa", "Quality Checks", "QC-2026-090", "Roast batch RB-441", "Released"),
        ),
    ),
    ErpModule(
        id = "production",
        label = "Production",
        group = "Inventory & production",
        description = "Plans, orders, machines, BOMs, batches, roast, packaging, downtime, and yield.",
        icon = "precision_manufacturing",
        color = 0xFFB45309,
        entities = listOf("Production Plans", "Production Orders", "Machines", "Bill of Materials", "Batch Records", "Roast Batches", "Packaging Runs", "Downtime Logs", "Yield Reports"),
        seed = listOf(
            seedRecord("production", "Production Orders", "MO-2026-118", "House blend 250g", "In progress"),
        ),
    ),
    ErpModule(
        id = "benchmark",
        label = "Work Systems",
        group = "Quality",
        description = "Work systems, benchmarks, KPIs, cycle time, productivity, gaps, and improvements.",
        icon = "analytics",
        color = 0xFF57534E,
        entities = listOf("Work Systems", "Benchmark Studies", "KPI Definitions", "Cycle Time Studies", "Productivity Scores", "Gap Analyses", "Improvement Actions"),
    ),
    ErpModule(
        id = "pos",
        label = "POS",
        group = "Commercial",
        description = "Front-of-house restaurant POS: dine-in floor, KOTs, receipts, kitchen display, and shift close.",
        icon = "point_of_sale",
        color = 0xFF059669,
        entities = listOf("POS Terminal", "POS Locations", "POS Products", "POS Services", "POS Stock In", "Registers", "Cash Sessions", "Dining Tables", "Open Tickets", "POS Sales", "POS Returns", "Daily Closings"),
        seed = listOf(
            seedRecord("pos", "POS Sales", "POS-20260824-0012", "Front Till · Cash", "Paid", amount = 28500.0),
        ),
    ),
    ErpModule(
        id = "payroll",
        label = "HR & Payroll",
        group = "People",
        description = "Employees, attendance, leave, payroll runs, payslips, and statutory remittances.",
        icon = "badge",
        color = 0xFF7C3AED,
        entities = listOf("Employees", "Departments", "Sites", "Blocks", "Attendance", "Leave Requests", "Holidays", "Job Positions", "Onboarding", "Create Payroll", "Payroll Runs", "Payslip Items", "Payslips", "Recurring Payslips", "Statutory Remittances"),
        approvalEntities = setOf("Leave Requests"),
        seed = listOf(
            seedRecord("payroll", "Employees", "Sarah Nambi", "Sales · EMP-014", "Active", fields = mapOf("Department" to "Sales", "Basic pay" to "UGX 2,400,000")),
            seedRecord("payroll", "Employees", "Daniel Okello", "Operations · EMP-022", "Active", fields = mapOf("Department" to "Operations", "Basic pay" to "UGX 1,800,000")),
            seedRecord("payroll", "Sites", "IAG Head Office", "Kampala · 150 m fence", "Active", fields = mapOf("Code" to "SITE-HQ", "Address" to "Kampala", "Latitude" to "0.347596", "Longitude" to "32.582520", "Radius (m)" to "150")),
            seedRecord("payroll", "Sites", "Africa Coffee Park", "Masaka · 250 m fence", "Active", fields = mapOf("Code" to "SITE-ACP", "Address" to "Masaka")),
            seedRecord("payroll", "Blocks", "ACP Wet mill", "Africa Coffee Park", "Active"),
            seedRecord("payroll", "Leave Requests", "LV-2026-009", "Sarah Nambi · annual", "Pending"),
        ),
    ),
    ErpModule(
        id = "investments",
        label = "Investments",
        group = "Accounting",
        description = "Investment holdings and related accounting.",
        icon = "pie_chart",
        color = 0xFF0F766E,
        entities = listOf("Investments"),
    ),
    ErpModule(
        id = "assets",
        label = "Fixed Assets",
        group = "Accounting",
        description = "Fixed and intangible assets, depreciation, amortization, and leases.",
        icon = "apartment",
        color = 0xFF1D4ED8,
        entities = listOf("Fixed Assets", "Depreciation Entries", "Intangible Assets", "Amortization Entries", "Leases"),
        seed = listOf(
            seedRecord("assets", "Fixed Assets", "Probat roaster P12", "FA-ROAST-01", "Active", amount = 310000000.0),
        ),
    ),
    ErpModule(
        id = "capital",
        label = "Capital Accounts",
        group = "Accounting",
        description = "Capital accounts, subaccounts, and share-based payments.",
        icon = "account_balance_wallet",
        color = 0xFF4338CA,
        entities = listOf("Capital Accounts", "Capital Subaccounts", "Share-based Payments"),
    ),
    ErpModule(
        id = "accounts",
        label = "Accounts",
        group = "Accounting",
        description = "Chart of accounts, journals, matching, provisions, ledgers, and trial balance.",
        icon = "menu_book",
        color = 0xFF0F172A,
        entities = listOf("Chart of Accounts", "Control Accounts", "Special Accounts", "Journal Entries", "Recurring Journal Entries", "Matching Entries", "Provisions", "Ledgers", "Trial Balance"),
        seed = listOf(
            seedRecord("accounts", "Journal Entries", "JE-2026-240", "Roast depreciation", "Posted", amount = 2100000.0),
        ),
    ),
    ErpModule(
        id = "folders",
        label = "Folders",
        group = "Records",
        description = "Folder structure for documents and attachments.",
        icon = "folder_open",
        color = 0xFFA16207,
        entities = listOf("Folders"),
        seed = listOf(
            seedRecord("folders", "Folders", "Finance — 2026", "Statutory packs", "Active"),
        ),
    ),
    ErpModule(
        id = "documents",
        label = "Documents",
        group = "Records",
        description = "Attachments, history, and deleted records.",
        icon = "folder",
        color = 0xFF57534E,
        entities = listOf("Attachments", "History", "Deleted Records"),
    ),
    ErpModule(
        id = "reports",
        label = "Reports",
        group = "Accounting",
        description = "Balance sheet, P&L, cash flow, aged ledgers, tax, and inventory value.",
        icon = "bar_chart",
        color = 0xFF0369A1,
        entities = listOf("Balance Sheet", "Profit & Loss", "Accounting Operations", "Management Analysis", "Profit & Loss by Class", "Division Exception Report", "Cash Flow", "Cash Flow Indirect", "Trial Balance", "Ledgers", "Statement of Changes in Equity", "Other Comprehensive Income", "Budget vs Actual", "Forecast P&L", "Notes to Financial Statements", "Control Account Reconciliation", "Bank Reconciliation", "Integrity Tests", "Field Audit Log", "Aged Receivables", "Aged Payables", "Customer Statements", "Supplier Statements", "Tax Summary", "Inventory Value Summary"),
        seed = listOf(
            seedRecord("reports", "Profit & Loss", "P&L — August 2026", "Net profit UGX 18.4M", "Draft"),
        ),
    ),
)

fun entityCode(entity: String): String {
    val parts = entity.split(Regex("[^A-Za-z0-9]+")).filter { it.isNotEmpty() }
    if (parts.isEmpty()) return "REC"
    if (parts.size == 1) return parts.first().take(3).uppercase()
    return parts.take(4).joinToString("") { it.take(1).uppercase() }
}

fun sampleStatus(entity: String): String {
    val e = entity.lowercase()
    if (e.contains("request")) return "Pending"
    if (e.contains("report") || e.contains("balance") || e.contains("profit") || e.contains("cash flow") || e.contains("forecast")) {
        return "Draft"
    }
    if (e.contains("invoice") || e.contains("journal")) return "Posted"
    return "Active"
}

fun sampleAmount(entity: String): Double? {
    val e = entity.lowercase()
    return if (listOf("invoice", "order", "payment", "receipt", "claim", "payroll", "transfer", "fee").any { e.contains(it) }) {
        1_250_000.0
    } else {
        null
    }
}

fun sampleRecord(module: ErpModule, entity: String): ErpRecord = seedRecord(
    module.id,
    entity,
    "${entityCode(entity)}-2026-001",
    "${module.label} · demo",
    sampleStatus(entity),
    amount = sampleAmount(entity),
    fields = mapOf("Source" to "Demo catalog"),
)

fun completeCatalogSeed(modules: List<ErpModule> = erpModules()): List<ErpRecord> =
    modules.flatMap { module ->
        val grouped = module.seed.groupBy { it.entity }
        module.entities.flatMap { entity ->
            val rows = grouped[entity]
            if (!rows.isNullOrEmpty()) rows else listOf(sampleRecord(module, entity))
        }
    }

fun reportLines(entity: String): List<Pair<String, String>> = when (entity) {
    "Balance Sheet" -> listOf("Assets" to "UGX 2.4B", "Liabilities" to "UGX 890M", "Equity" to "UGX 1.5B")
    "Profit & Loss", "Profit & Loss by Class", "Forecast P&L" ->
        listOf("Revenue" to "UGX 410M", "Cost of sales" to "UGX 186M", "Net profit" to "UGX 124M")
    "Cash Flow", "Cash Flow Indirect" ->
        listOf("Operating" to "UGX 92M", "Investing" to "UGX -18M", "Financing" to "UGX -12M")
    "Trial Balance" -> listOf("Debits" to "UGX 2.4B", "Credits" to "UGX 2.4B")
    "Aged Receivables" -> listOf("Current" to "UGX 48M", "30 days" to "UGX 12M", "60+ days" to "UGX 6.2M")
    "Aged Payables" -> listOf("Current" to "UGX 31M", "30 days" to "UGX 9M", "60+ days" to "UGX 4.1M")
    "Tax Summary" -> listOf("VAT output" to "UGX 18.4M", "VAT input" to "UGX 11.2M", "WHT" to "UGX 2.1M")
    "Inventory Value Summary" ->
        listOf("Green bean" to "UGX 210M", "Roast" to "UGX 64M", "Packaging" to "UGX 8.4M")
    else -> listOf("Period" to "August 2026", "Prepared by" to "Finance", "Status" to "Draft")
}

fun featureSummary(module: ErpModule): String {
    val names = module.entities.take(3).joinToString(", ")
    val extra = if (module.entities.size > 3) " +${module.entities.size - 3}" else ""
    return "${module.entities.size} features · $names$extra"
}
