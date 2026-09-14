# IAG Central Android

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

The native Android Finance ERP. `:core` is a JVM library with the same SoD rules as the web `access-control` layer. After sign-in you open a full app — Finance, Sales, CRM, POS, Fleet, Contract Management, DMS, and the rest — instead of one mixed desk list. Clock In is on every login. Administrators create **custom roles** with a page matrix (optional `*`), then assign them on Users.

## Who it is for

Anyone who already has a Finance role: admin, accountant, clerk, viewer, HR, HOD, PM, contractor, GM, CEO, QS, stores, procurement, and custom roles you create on device.

## What you can do

- Home: the desks in the app you opened (Finance shows banking and reports, Security shows gate passes, …).
- Apps: switch to another full tool without signing out. Admin lands on the app grid; HR, Procurement, and contractors open their own app.
- Clock: GPS clock-in / clock-out against HR Sites and Blocks. Clerk, viewer, and contractor can punch without opening the HR desk.
- Departments: Banking through Reports, grouped like the web sidebar. Open a desk to see **every feature** (customers, invoices, lots, reports, …).
- Records: open a document, create a draft, submit, approve/reject, void (where the desk allows).
- Approvals: hidden if the role has no desk.
- Workspace: Trace, analytics, accounting documents, templates, comms, guides, Q&A, release notes, activity, settings.
- Access (admin): custom roles with a page matrix and workspace users.

## Custom roles

Built-in demo roles cannot be overwritten. Custom roles get CRUD from the matrix you set. Approve, void, payroll, and geofence still follow the same allow-lists as web ERP. Specialty apps (lab, R&D, POS, fleet, …) need an explicit grant.

Usernames include `admin`, `accountant`, `clerk`, `viewer`, `hr`, `hod`, `pm`, `contractor`, `gm`, `ceo`, `qs`, `stores`, `procurement`. Sign in with the **same password as the live web ERP**. The old short demo password is rejected. To try the app offline, use **Forgot password**, then **Continue on this device**.

```
core/   # RBAC, catalog, store (JVM — CI tests here)
app/    # Jetpack Compose shell
```

## Architecture

```
ERP Android (Compose)
  → IAG Frontend (`https://iag-frontend-five.vercel.app`)
  → shared Go API (same JWT as web ERP)
```

Sign-in and records use the live workspace. Roles are loaded from the database after sign-in.

## ERP API (same paths on Frontend and Go)

The phone talks to Frontend `/api/*` (rewritten to the Go API). Auth is `Authorization: Bearer <token>` from `POST /api/auth/login`.

| Use | Method | Path |
| --- | --- | --- |
| Sign in | `POST` | `/api/auth/login` (`emailOrUsername`, `password`, `keepSignedIn`) |
| Who am I | `GET` | `/api/auth/me` |
| Sign out | `POST` | `/api/auth/logout` |
| Forgot password | `POST` | `/api/auth/forgot-password` |
| Profile | `PATCH` | `/api/auth/profile` |
| Users / roles (admin) | `GET` | `/api/auth/users`, `/api/auth/roles` |
| List / create records | `GET` `POST` | `/api/records/:module/:entity` |
| Update / delete one | `PATCH` `DELETE` | `/api/records/:module/:entity/:id` |
| Approval queue | `GET` | `/api/approvals/desk` |
| Advance / reject chain | `POST` | `/api/approvals/:entity/:id/advance` or `/reject` (`{comment}`) |

Chain entities (IPC, material requests, oral/general requests, fleet requests, leave, payroll runs) must change status through `/api/approvals`, not by PATCHing `Approved`. Draft → Submitted is a record PATCH.

## Identity and data

Sign in with an account from **IAG Admin** when the app is pointed at the shared API.
On-device demo data (SharedPreferences) is only for local/offline trials — it is not the production directory. Passwords are hashed on device; use **Forgot password** to create one. There is no default secret in the app.

## Run locally

```bash
cd erp-android
./gradlew :core:test
./gradlew :app:assembleDebug
```

Open the project in Android Studio, select the **app** run configuration, and install on an emulator or device.

CI matches web ERP: tests, then a Debug build.

## Stack

Kotlin · Jetpack Compose · Material 3 · ErpCore RBAC · IAG Frontend (`https://iag-frontend-five.vercel.app`) · SharedPreferences (offline trial).

## Related IAG systems

| System | GitHub | README |
| --- | --- | --- |
| IAG tools workspace | [iagtools](https://github.com/OlaroPeterCelestine/iagtools) | [README](https://github.com/OlaroPeterCelestine/iagtools#readme) |
| IAG Admin | [iag-admin](https://github.com/OlaroPeterCelestine/iag-admin) | [README](https://github.com/OlaroPeterCelestine/iag-admin#readme) |
| IAG ERP API | [iag-erp-api](https://github.com/OlaroPeterCelestine/iag-erp-api) | [README](https://github.com/OlaroPeterCelestine/iag-erp-api#readme) |
| IAG ERP | [iag-erp](https://github.com/OlaroPeterCelestine/iag-erp) | [README](https://github.com/OlaroPeterCelestine/iag-erp#readme) |
| IAG ERP iOS | [iag-erp-ios](https://github.com/OlaroPeterCelestine/iag-erp-ios) | [README](https://github.com/OlaroPeterCelestine/iag-erp-ios#readme) |
| IAG ERP Android ← this repo | [iag-erp-android](https://github.com/OlaroPeterCelestine/iag-erp-android) | [README](https://github.com/OlaroPeterCelestine/iag-erp-android#readme) |
