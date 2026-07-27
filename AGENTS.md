# HonorarCraftAndroid Agent Guide

## Architecture Overview
This is an Android invoice management app for educational services using Jetpack Compose and Room database. The app manages invoices with entries for lesson units, calculates revenue, and generates PDFs.

Key components:
- **MainActivity**: Entry point with splash screen and horizontal pager navigation (4 tabs: Dashboard, Create, Preview, Data)
- **MainViewModel**: Central state management using Kotlin Flows for reactive UI updates
- **AppDatabase**: Room database with InvoiceData, InvoiceEntry, CompanyData entities
- **Screens**: DashboardScreen (revenue/year), CreateInvoiceScreen (add entries), EntryWindowScreen (view/edit/generate PDF), DataWindowScreen (company settings)

Data flow: User inputs → MainViewModel → Room DAO → UI updates via Flows.

## Key Patterns
- **Invoice Number Formatting**: Configurable as NUMBER, YEAR_NUMBER, YEAR_MONTH_NUMBER (e.g., "2026-03-1"). Set in Dashboard dropdown menu.
- **Lesson Unit Conversion**: Entries store lessonUnits; calculations use `lessonUnits * 60 / 45` to convert to teaching units (UE). See `InvoiceWithEntries.totalSum`.
- **Revenue Calculation**: Sums `(lessonUnits * 60 / 45) * rate` for entries where date ends with selected year. See `MainViewModel.yearlyRevenue`.
- **State Persistence**: Invoice year/month/number stored in SharedPreferences; company data in Room.
- **PDF Generation**: Uses Android PdfDocument API with company data and signature image. See `CreatePdf.kt`.
- **Navigation Guard**: Prevents tab changes with unsaved changes in DataWindow via AlertDialog.

## Conventions
- **Locale**: German (GERMANY) for dates ("dd.MM.yyyy") and number formatting.
- **BigDecimal**: Used for all monetary values and calculations with RoundingMode.HALF_UP.
- **Database**: Version 6, exportSchema=true, fallbackToDestructiveMigration. Schemas in `app/schemas/`.
- **Compose BOM**: Uses very recent 2025.02.00 for alpha features.
- **Dependencies**: Managed via version catalogs in `gradle/libs.versions.toml`.

## Workflows
- **Build**: `./gradlew build` (uses AGP 9.1.0, Kotlin 2.2.10)
- **Run/Debug**: `./gradlew installDebug` or use Android Studio
- **Tests**: `./gradlew test` (JUnit 4 for unit, Espresso for UI)
- **Database Migration**: Increment `AppDatabase.version` and update schema files
- **PDF Output**: Saved to external storage (requires WRITE_EXTERNAL_STORAGE on API <29)

## Key Files
- `MainViewModel.kt`: Core logic, state flows, calculations
- `AppDatabase.kt`: DAOs and entities
- `CreatePdf.kt`: PDF generation logic
- `app/build.gradle.kts`: Dependencies, KSP for Room
- `gradle/libs.versions.toml`: Version catalog definitions
