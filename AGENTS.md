# Agent notes

Native Android ERP. Keep RBAC in `core/` aligned with `erp/src/lib/access-control.ts`.

- UI: `app/` (Jetpack Compose)
- Core: `core/` (roles, catalog, store, IAG Frontend API — JVM, no Android SDK)
- Default frontend origin: `https://iag-frontend-five.vercel.app`
- Records: item REST + approval chain (`ErpEndpoints` in `Endpoints.kt`)
- Tests: `./gradlew :core:test` (must stay green in CI before `assembleDebug`)
