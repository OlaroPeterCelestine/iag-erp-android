# Agent notes

Native Android ERP. Keep RBAC in `core/` aligned with `erp/src/lib/access-control.ts`.

- UI: `app/` (Jetpack Compose)
- Core: `core/` (roles, catalog, store — JVM, no Android SDK)
- Tests: `./gradlew :core:test` (must stay green in CI before `assembleDebug`)
