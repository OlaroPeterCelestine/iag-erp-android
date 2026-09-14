# IAG ERP Android

Native Kotlin / Jetpack Compose Finance ERP: every department desk, records, approvals, and custom roles — same SoD / RBAC as web **IAG ERP**.

**Version:** `1.0.0` — [changelog](./CHANGELOG.md)

**Application ID:** `africa.iag.erp.android`

## Links

- **README:** [https://github.com/OlaroPeterCelestine/iag-erp-android#readme](https://github.com/OlaroPeterCelestine/iag-erp-android#readme)
- **Repository:** [https://github.com/OlaroPeterCelestine/iag-erp-android](https://github.com/OlaroPeterCelestine/iag-erp-android)
- **API:** [https://github.com/OlaroPeterCelestine/iag-erp-api#readme](https://github.com/OlaroPeterCelestine/iag-erp-api#readme)
- **Users & roles (Admin):** [https://github.com/OlaroPeterCelestine/iag-admin#readme](https://github.com/OlaroPeterCelestine/iag-admin#readme)
- **Web ERP:** [https://github.com/OlaroPeterCelestine/iag-erp#readme](https://github.com/OlaroPeterCelestine/iag-erp#readme)
- **Workspace index:** [https://github.com/OlaroPeterCelestine/iagtools#readme](https://github.com/OlaroPeterCelestine/iagtools#readme)

## What this app is

The native Android Finance ERP. `:core` is a JVM library with the same SoD rules as the web `access-control` layer. The Compose shell is Home, Departments, Clock, Approvals, Workspace, Access, and Account. Every web ERP desk and feature is on the phone, plus a Clock In module that any signed-in login can punch. Administrators create **custom roles** with a page matrix (optional `*`), then assign them on Users.

## Who it is for

Anyone who already has a Finance role: admin, accountant, clerk, viewer, HR, HOD, PM, contractor, GM, CEO, QS, stores, procurement, and custom roles you create on device.

## What you can do

- Home: balance-sheet snapshot, search across desks/features/records, then every department you can open.
- Clock: GPS clock-in / clock-out against HR Sites and Blocks. Clerk, viewer, and contractor can punch without opening the HR desk.
- Departments: Banking through Reports, grouped like the web sidebar. Open a desk to see **every feature** (customers, invoices, lots, reports, …).
- Records: open a document, create a draft, submit, approve/reject, void (where the desk allows).
- Approvals: hidden if the role has no desk.
- Workspace: Trace, analytics, accounting documents, templates, comms, guides, Q&A, release notes, activity, settings.
- Access (admin): custom roles with a page matrix and workspace users.

## Custom roles

Built-in demo roles cannot be overwritten. Custom roles get CRUD from the matrix you set. Approve, void, payroll, and geofence still follow the same allow-lists as web ERP. Specialty apps (lab, R&D, POS, fleet, …) need an explicit grant.

Demo password: `iagdemo`. Usernames include `admin`, `accountant`, `clerk`, `viewer`, `hr`, `hod`, `pm`, `contractor`, `gm`, `ceo`, `qs`, `stores`, `procurement`.

```
core/   # RBAC, catalog, store (JVM — CI tests here)
app/    # Jetpack Compose shell
```

## Architecture

```
ERP Android (Compose)
  → :core (roles, departments, records)
  → optional shared Go API (same JWT as web ERP)
```

## Identity and data

Sign in with an account from **IAG Admin** when the app is pointed at the shared API.
On-device demo data (SharedPreferences) is only for local/offline trials — it is not the production directory.

Local demo password is `iagdemo`.

## Run locally

```bash
cd erp-android
./gradlew :core:test
./gradlew :app:assembleDebug
```

Open the project in Android Studio, select the **app** run configuration, and install on an emulator or device.

CI matches web ERP: tests, then a Debug build.

## Stack

Kotlin · Jetpack Compose · Material 3 · ErpCore RBAC · SharedPreferences (local) · optional shared Go API.

## Related IAG systems

| System | GitHub | README |
| --- | --- | --- |
| IAG tools workspace | [iagtools](https://github.com/OlaroPeterCelestine/iagtools) | [README](https://github.com/OlaroPeterCelestine/iagtools#readme) |
| IAG Admin | [iag-admin](https://github.com/OlaroPeterCelestine/iag-admin) | [README](https://github.com/OlaroPeterCelestine/iag-admin#readme) |
| IAG ERP API | [iag-erp-api](https://github.com/OlaroPeterCelestine/iag-erp-api) | [README](https://github.com/OlaroPeterCelestine/iag-erp-api#readme) |
| IAG ERP | [iag-erp](https://github.com/OlaroPeterCelestine/iag-erp) | [README](https://github.com/OlaroPeterCelestine/iag-erp#readme) |
| IAG ERP iOS | [iag-erp-ios](https://github.com/OlaroPeterCelestine/iag-erp-ios) | [README](https://github.com/OlaroPeterCelestine/iag-erp-ios#readme) |
| IAG ERP Android ← this repo | [iag-erp-android](https://github.com/OlaroPeterCelestine/iag-erp-android) | [README](https://github.com/OlaroPeterCelestine/iag-erp-android#readme) |
