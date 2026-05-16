# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## What this app is

Atmos is a personal carbon footprint tracker for Android and iOS. The guiding principle is **effortless awareness**: the app detects trips automatically in the background, shows you your carbon impact without manual input, and only interrupts when it needs a human decision (e.g. confirming an auto-detected trip). It is calibrated for the **Indian market** first — transport modes include auto-rickshaw, two-wheeler, Namma Yatri, etc., and emission factors use India-region DEFRA 2023 data.

The backend (`atmos-core`, a separate Go/GoFiber repo) is fully built. The mobile app is currently **UI-only** — all screen data is driven by in-file hardcoded preview objects.

---

## Commands

### Build

```bash
# Compile shared KMP module (runs all targets)
./gradlew :shared:build

# Android debug APK
./gradlew :androidApp:assembleDebug

# Install on a connected device/emulator
./gradlew :androidApp:installDebug

# Build the iOS framework (invoked by Xcode build phases — rarely needed manually)
./gradlew :shared:assembleSharedReleaseXCFramework
```

### Run

- **Android**: Run the `androidApp` configuration from Android Studio, or use `installDebug` above.
- **iOS**: Open `iosApp/iosApp.xcodeproj` in Xcode and hit Run, or use the `iosApp` configuration in Android Studio. The first build takes a few minutes to compile the shared framework; subsequent builds are fast.

### Lint / type checks

```bash
# Check Kotlin compilation for all targets
./gradlew :shared:compileKotlinAndroid
./gradlew :shared:compileKotlinIosArm64
./gradlew :shared:compileKotlinIosSimulatorArm64
```

There are no automated tests yet. All correctness verification is visual/manual against the preview data.

---

## Repo map

```
atmos-mobile/
├── androidApp/
│   └── src/androidMain/
│       ├── kotlin/dev/atmos/android/
│       │   └── MainActivity.kt               # Calls AtmosApp(), enables edge-to-edge
│       └── res/values/strings.xml
│
├── iosApp/
│   └── iosApp/
│       ├── iOSApp.swift                      # @main entry point
│       └── ContentView.swift                 # Wraps MainViewController() in UIViewControllerRepresentable
│
├── shared/
│   └── src/
│       ├── commonMain/kotlin/dev/atmos/shared/
│       │   ├── AtmosApp.kt                   # Root composable: Screen sealed class + all navigation state
│       │   ├── Platform.kt                   # expect fun platformName(): String
│       │   │
│       │   ├── ui/
│       │   │   ├── onboarding/
│       │   │   │   └── OnboardingScreen.kt   # 3-page animated flow (Welcome, How it Works, Permissions)
│       │   │   │
│       │   │   ├── auth/
│       │   │   │   ├── AuthComponents.kt     # Shared input fields, buttons used across auth screens
│       │   │   │   ├── LoginScreen.kt
│       │   │   │   ├── SignUpScreen.kt
│       │   │   │   └── ForgotPasswordScreen.kt
│       │   │   │
│       │   │   ├── home/
│       │   │   │   ├── HomeScreen.kt         # LazyColumn with 4 states: loading/error/empty/populated
│       │   │   │   ├── HomeUiState.kt        # HomeUiState + all data models (TransportModeType, InsightEntry, etc.) + preview data
│       │   │   │   └── components/
│       │   │   │       ├── AtmosBottomBar.kt # Custom bottom nav with center-notch FAB; AtmosTab enum
│       │   │   │       ├── AtmosHeader.kt    # Greeting + date + avatar
│       │   │   │       ├── TodayImpactCard.kt
│       │   │   │       ├── WeeklyTrendCard.kt
│       │   │   │       ├── TransportBreakdownCard.kt
│       │   │   │       ├── RecentActivityCard.kt
│       │   │   │       ├── InsightsSection.kt
│       │   │   │       └── PendingTripCard.kt  # Auto-detected trip awaiting confirmation
│       │   │   │
│       │   │   ├── activities/
│       │   │   │   └── ActivitiesScreen.kt   # Paginated trip history grouped by date
│       │   │   │
│       │   │   ├── logactivity/
│       │   │   │   └── LogActivitySheet.kt   # Global ModalBottomSheet; emissionFactor + displayLabel extensions on TransportModeType
│       │   │   │
│       │   │   ├── tripdetail/
│       │   │   │   └── TripDetailScreen.kt   # Single trip view with edit/delete actions
│       │   │   │
│       │   │   ├── insights/
│       │   │   │   └── InsightsScreen.kt     # Full insights feed
│       │   │   │
│       │   │   ├── insightdetail/
│       │   │   │   └── InsightDetailScreen.kt
│       │   │   │
│       │   │   ├── profile/
│       │   │   │   ├── ProfileScreen.kt
│       │   │   │   ├── ProfileUiState.kt     # ProfileUiState + CommuteLocation + AppearanceMode + preview data
│       │   │   │   └── components/
│       │   │   │       ├── ProfileHeaderCard.kt
│       │   │   │       ├── MyImpactCard.kt
│       │   │   │       ├── DailyGoalCard.kt
│       │   │   │       ├── CommuteCard.kt
│       │   │   │       ├── PreferencesCard.kt
│       │   │   │       └── AccountCard.kt
│       │   │   │
│       │   │   ├── common/
│       │   │   │   ├── AtmosCard.kt          # Zero-elevation themed card wrapper — use for all card containers
│       │   │   │   ├── CircularProgressRing.kt
│       │   │   │   └── Shimmer.kt            # Loading skeleton modifier + named skeleton composables
│       │   │   │
│       │   │   └── theme/
│       │   │       ├── AtmosTheme.kt         # Wraps Material3; provides LocalAtmosColors
│       │   │       ├── Color.kt              # AtmosColors data class, Light/Dark instances, brand palette
│       │   │       ├── Type.kt               # AtmosTypography (DM Sans)
│       │   │       └── Shape.kt              # AtmosShapes, CardShape
│       │   │
│       │   └── util/
│       │       └── DateTimeUtils.kt          # currentGreeting(), currentDateLabel(), currentTimeLabel() via kotlinx-datetime
│       │
│       ├── androidMain/kotlin/dev/atmos/shared/
│       │   └── Platform.android.kt           # actual fun platformName() = "Android"
│       │
│       └── iosMain/kotlin/dev/atmos/shared/
│           ├── MainViewController.kt         # fun MainViewController() = ComposeUIViewController { AtmosApp() }
│           └── Platform.ios.kt               # actual fun platformName() = "iOS"
│
├── gradle/
│   └── libs.versions.toml                   # Version catalog — all dependency versions live here
├── build.gradle.kts                          # Root: plugin declarations only (apply false)
├── settings.gradle.kts                       # Includes :shared, :androidApp
└── gradle.properties                         # Xmx4g, configuration-cache=true, parallel=true
```

---

## Architecture

### Navigation

Navigation is a hand-rolled `sealed class Screen` inside `AtmosApp.kt` — no Compose Navigation or other library. A single `var screen by remember { mutableStateOf<Screen>(Screen.Onboarding) }` drives the entire app. Each screen composable receives typed lambdas (`onNavigateTo*`, `onBack`) for transitions.

**Screen flow:**
```
Onboarding (3-page animated) → SignUp / Login / ForgotPassword → Home
Home → Profile | Activities | TripDetail | InsightDetail | Insights
```
`LogActivitySheet` (a `ModalBottomSheet`) is a global overlay rendered above any screen; it is controlled by `showLogActivity: Boolean` in `AtmosApp`.

### State model

Each screen has an immutable `UiState` data class (e.g. `HomeUiState`, `ProfileUiState`). Data models are co-located in the same file as their `UiState` (e.g. `TransportModeType`, `InsightEntry`, `RecentActivityEntry` all live in `HomeUiState.kt`).

There are **no ViewModels**. `AtmosApp` holds all runtime state in `remember` variables and passes hardcoded preview objects (`previewHomeUiState`, `previewProfileUiState`) to every screen. The 2-second simulated load in `AtmosApp` is a `LaunchedEffect + delay`. Koin is declared as a dependency but `startKoin` is never called — no modules exist.

When adding a new screen:
1. Add a `data object MyScreen : Screen()` entry to the sealed class in `AtmosApp.kt`.
2. Add the screen's `UiState` and data models to a new `MyScreenUiState.kt` alongside the screen file.
3. Wire the `when (screen)` branch in `AtmosApp`.

### Theme system

`AtmosTheme` wraps Material3 and injects a custom `AtmosColors` via `CompositionLocal`.

**Always read colors inside composables via `LocalAtmosColors.current`**, not the legacy top-level aliases (`SkyWhite`, `CardSurface`, etc.) — those reference light values only and exist only for backward compatibility in Canvas drawing code.

Brand palette (static, same in light and dark):

| Token | Hex | Semantic use |
|---|---|---|
| `HorizonBlue` | `#4A90C4` | Primary, CTAs, nav active, focus rings |
| `Sage` | `#3DAB82` | Low-emission, success, streaks |
| `Peach` | `#E89066` | Medium-emission (two-wheeler, auto, bus) |
| `AlertRed` | `#E86B5F` | High-emission (car, cab, flight), errors |

Transport mode color coding used across charts and labels:
- **Green (Sage)**: walking, cycling, metro, train
- **Peach**: bus, two-wheeler, auto-rickshaw
- **Red (AlertRed)**: car/driving, cab, flight

### Emission factors

CO₂e factors (kg per km) are defined as `TransportModeType.emissionFactor` extension property in `LogActivitySheet.kt`. Distance calculation and CO₂ estimation are stubbed at `0f` — the `Co2EstimateCard` only renders when `distanceKm > 0`.

### Reusable components (`shared/src/commonMain/kotlin/dev/atmos/shared/ui/common/`)

- **`AtmosCard`** — zero-elevation card with themed surface color and 20dp content padding. Use this for all card-shaped containers.
- **`CircularProgressRing`** — custom Canvas ring for daily goal progress.
- **`Shimmer`** — loading skeleton modifier. The home screen has named skeleton composables (`TodayImpactSkeleton`, `WeeklyTrendSkeleton`, etc.) for each card.

### Home screen layout

`HomeScreen` is a `LazyColumn` inside a `Scaffold`. It renders one of four states based on `HomeUiState`:
- **Loading** — shimmer skeletons for each card
- **Error** — `HomeErrorState` with retry CTA
- **Empty** (no activities yet) — `HomeEmptyState` with first-trip CTA
- **Populated** — `TodayImpactCard`, `WeeklyTrendCard`, `TransportBreakdownCard`, `RecentActivityCard`, `InsightsSection`

`AtmosBottomBar` is a custom bottom nav with a center-notch FAB (+) floating above it. It has two tabs: **Home** and **Activities**. The FAB opens `LogActivitySheet`.

### iOS build notes

The standard `embedAndSignAppleFrameworkForXcode` Gradle task is **not used** — it is broken in KMP 2.1. Two custom Xcode shell script build phases handle the framework compile and embed instead. The shared framework is built as **dynamic** (`isStatic = false` in `shared/build.gradle.kts`). Do not change this to static — iOS cannot install a static `ar archive`.

