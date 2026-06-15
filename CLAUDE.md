# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## What this app is

Atmos is a personal carbon footprint tracker for Android and iOS. The guiding principle is **effortless awareness**: the app detects trips automatically in the background, shows you your carbon impact without manual input, and only interrupts when it needs a human decision (e.g. confirming an auto-detected trip). It is calibrated for the **Indian market** first — transport modes include auto-rickshaw, two-wheeler, Namma Yatri, etc., and emission factors use India-region DEFRA 2023 data.

The backend (`atmos-core`, a separate Go/GoFiber repo) is fully built and fully integrated. All screens fetch real data from `https://atmos-core.fly.dev`. CO₂ values are always computed by the backend (DEFRA IN 2023, region-aware) — never derive them locally from `TransportModeType.emissionFactor`.

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
./gradlew :shared:compileDebugKotlinAndroid
./gradlew :shared:compileKotlinIosArm64
./gradlew :shared:compileKotlinIosSimulatorArm64
```

There are no automated tests. All correctness verification is visual/manual against a real device or emulator connected to the live backend.

### Git workflow

Always work on a feature branch — never commit directly to `main`:
```bash
git checkout -b feat/<short-description>   # or fix/, chore/
# ... make changes, commit ...
git push -u origin feat/<short-description>
gh pr create
```

### Commit message format

This repo follows Conventional Commits convention. CI does not currently enforce it automatically, but all commits should comply for consistency with the other Atmos repos.

```
<type>(<scope>): <subject>
```

**Allowed types:**

| Type | When to use |
|---|---|
| `feat` | New feature or capability |
| `fix` | Bug fix |
| `perf` | Performance improvement |
| `refactor` | Code change that neither fixes a bug nor adds a feature |
| `chore` | Maintenance, Gradle/dependency bumps, config changes |
| `docs` | Documentation only |
| `test` | Adding or updating tests |
| `ci` | CI/CD workflow changes |
| `revert` | Reverting a previous commit |

**Avoid**: `build`, `style` (not used in other Atmos repos)

**Examples:**
```
feat(home): add PendingTripCard for auto-detected trips
fix(auth): handle Google Sign-In 401 on iOS
chore: bump play-services-location to 21.3.0
refactor(location): consolidate trip state machine into TripDetectorState
test(tripmatcher): add GPS late-start edge case
```

---

## Repo map

```
atmos-mobile/
├── androidApp/
│   └── src/androidMain/
│       ├── AndroidManifest.xml
│       └── kotlin/dev/atmos/android/
│           ├── MainActivity.kt                   # Entry point; wires AndroidPermissionRequester + CompositionLocal
│           ├── TripDetectionService.kt           # Foreground service (location type); shows trip-in-progress notification
│           ├── TripActionReceiver.kt             # BroadcastReceiver for notification action intents (confirm/dismiss)
│           ├── AtmosFirebaseMessagingService.kt  # FCM push token + payload routing → NotificationState
│           ├── AndroidShareLauncher.kt           # actual ShareLauncher for Android
│           └── AvatarUploadHelper.kt             # Photo picker + multipart upload helper
│
├── iosApp/
│   └── iosApp/
│       ├── iOSApp.swift                          # @main entry point
│       └── ContentView.swift                     # Wraps MainViewController() in UIViewControllerRepresentable
│
├── shared/
│   └── src/
│       ├── commonMain/kotlin/dev/atmos/shared/
│       │   ├── AtmosApp.kt                       # Root composable: Screen sealed class, all navigation + fetching state
│       │   ├── Platform.kt                       # expect fun platformName(): String
│       │   │
│       │   ├── auth/
│       │   │   ├── AuthState.kt                  # Global AuthUser + sign-in/sign-out state
│       │   │   ├── GoogleSignIn.kt               # expect interface for Google Sign-In
│       │   │   ├── TokenRefresher.kt             # Auto-refresh access tokens before expiry
│       │   │   └── TokenStore.kt                 # expect interface; stores access/refresh tokens securely
│       │   │
│       │   ├── db/
│       │   │   ├── DatabaseDriverFactory.kt      # expect factory for SQLDelight driver
│       │   │   ├── DatabaseProvider.kt           # Singleton database instance
│       │   │   ├── SessionMapper.kt              # Maps raw DB rows → domain objects
│       │   │   ├── TripRepository.kt             # Interface for local trip persistence
│       │   │   └── TripRepositoryImpl.kt         # SQLDelight implementation
│       │   │
│       │   ├── location/
│       │   │   ├── TripDetector.kt               # expect interface + TripDetectorState singleton (StateFlows)
│       │   │   ├── TripState.kt                  # TripPhase, RawTrip, LatLng, LocationPermissionState
│       │   │   └── PermissionRequester.kt        # interface + NoOpPermissionRequester + LocalPermissionRequester
│       │   │
│       │   ├── network/
│       │   │   ├── AtmosHttpClient.kt            # Ktor HttpClient singleton; JWT inject + auto-refresh
│       │   │   ├── AuthService.kt                # login, signup, refresh, forgot-password
│       │   │   ├── ActivityService.kt            # list/create/update/delete trip activities
│       │   │   ├── DeviceService.kt              # register device for push notifications
│       │   │   ├── FcmTokenStore.kt              # Stores FCM push token across sessions
│       │   │   ├── InsightsService.kt            # GET /api/v1/insights (period param: week/month/year)
│       │   │   ├── TimelineService.kt            # GET daily + weekly CO₂ timeline
│       │   │   └── UserService.kt                # getMe, updateMe, getPreferences, updatePreferences
│       │   │
│       │   ├── ui/
│       │   │   ├── NotificationState.kt          # Global MutableStateFlow for push notification routing
│       │   │   │
│       │   │   ├── onboarding/
│       │   │   │   └── OnboardingScreen.kt       # 3-page animated flow (Welcome, How it Works, Permissions)
│       │   │   │
│       │   │   ├── auth/
│       │   │   │   ├── AuthComponents.kt         # Shared input fields + buttons
│       │   │   │   ├── LoginScreen.kt
│       │   │   │   ├── SignUpScreen.kt
│       │   │   │   ├── ForgotPasswordScreen.kt
│       │   │   │   └── EmailVerificationScreen.kt
│       │   │   │
│       │   │   ├── home/
│       │   │   │   ├── HomeScreen.kt             # LazyColumn: loading/error/empty/populated states
│       │   │   │   ├── HomeUiState.kt            # HomeUiState + data models (TransportModeType, InsightEntry, etc.)
│       │   │   │   └── components/
│       │   │   │       ├── AtmosBottomBar.kt     # Custom bottom nav with center-notch FAB; AtmosTab enum
│       │   │   │       ├── AtmosHeader.kt        # Greeting + date + avatar
│       │   │   │       ├── TodayImpactCard.kt
│       │   │   │       ├── WeeklyTrendCard.kt
│       │   │   │       ├── TransportBreakdownCard.kt
│       │   │   │       ├── RecentActivityCard.kt
│       │   │   │       ├── InsightsSection.kt
│       │   │   │       ├── PendingTripCard.kt    # Auto-detected trip awaiting user confirmation
│       │   │   │       └── OngoingTripCard.kt    # Live tracking banner for in-progress trips
│       │   │   │
│       │   │   ├── activities/
│       │   │   │   └── ActivitiesScreen.kt       # Paginated trip history grouped by date
│       │   │   │
│       │   │   ├── logactivity/
│       │   │   │   └── LogActivitySheet.kt       # Global ModalBottomSheet; emissionFactor + displayLabel on TransportModeType
│       │   │   │
│       │   │   ├── tripdetail/
│       │   │   │   └── TripDetailScreen.kt       # Single trip view with edit/delete
│       │   │   │
│       │   │   ├── insights/
│       │   │   │   └── InsightsScreen.kt         # Full insights feed with Week/Month/Year filter
│       │   │   │
│       │   │   ├── insightdetail/
│       │   │   │   └── InsightDetailScreen.kt
│       │   │   │
│       │   │   ├── stats/
│       │   │   │   ├── StatsScreen.kt            # Day/Week/Month CO₂ breakdown charts
│       │   │   │   └── StatsUiState.kt
│       │   │   │
│       │   │   ├── profile/
│       │   │   │   ├── ProfileScreen.kt
│       │   │   │   ├── ProfileUiState.kt
│       │   │   │   └── components/
│       │   │   │       ├── ProfileHeaderCard.kt
│       │   │   │       ├── MyImpactCard.kt
│       │   │   │       ├── DailyGoalCard.kt
│       │   │   │       ├── CommuteCard.kt
│       │   │   │       ├── PreferencesCard.kt
│       │   │   │       ├── AccountCard.kt
│       │   │   │       └── ExportDataSheet.kt    # CSV export bottom sheet
│       │   │   │
│       │   │   └── common/
│       │   │       ├── AtmosCard.kt              # Zero-elevation themed card wrapper
│       │   │       ├── AvatarUploader.kt         # expect composable for camera/gallery avatar upload
│       │   │       ├── CircularProgressRing.kt
│       │   │       ├── ShareLauncher.kt          # expect composable for OS share sheet
│       │   │       └── Shimmer.kt                # Loading skeleton modifier + named skeleton composables
│       │   │
│       │   └── util/
│       │       ├── DateTimeUtils.kt              # currentGreeting(), currentDateLabel(), currentTimeLabel()
│       │       └── FormatUtils.kt                # Float.toDisplayString() — shared "5 km" / "5.0 km" formatter
│       │
│       ├── androidMain/kotlin/dev/atmos/shared/
│       │   ├── Platform.android.kt
│       │   ├── auth/
│       │   │   ├── GoogleSignIn.android.kt
│       │   │   └── TokenStore.android.kt         # EncryptedSharedPreferences
│       │   ├── db/
│       │   │   └── DatabaseDriverFactory.android.kt
│       │   └── location/
│       │       ├── TripDetector.android.kt       # Activity Recognition Transitions + FusedLocation state machine
│       │       └── TripTransitionReceiver.kt     # BroadcastReceiver for ActivityTransitionResult
│       │
│       └── iosMain/kotlin/dev/atmos/shared/
│           ├── MainViewController.kt             # fun MainViewController() = ComposeUIViewController { AtmosApp() }
│           ├── Platform.ios.kt
│           ├── IosShareLauncher.kt
│           ├── auth/
│           │   ├── GoogleSignIn.ios.kt
│           │   └── TokenStore.ios.kt             # Keychain-backed
│           ├── db/
│           │   └── DatabaseDriverFactory.ios.kt
│           └── location/
│               ├── IosPermissionRequester.kt     # CLLocationManager + UNUserNotificationCenter delegate
│               └── TripDetector.ios.kt           # CLLocationManager + CMMotionActivityManager state machine
│
├── gradle/
│   └── libs.versions.toml                        # Version catalog
├── build.gradle.kts                              # Root: plugin declarations only (apply false)
├── settings.gradle.kts                           # Includes :shared, :androidApp
└── gradle.properties                             # Xmx4g, configuration-cache=true, parallel=true
```

---

## Architecture

### Navigation

Navigation is a hand-rolled `sealed class Screen` inside `AtmosApp.kt` — no Compose Navigation or other library. A single `var screen by remember { mutableStateOf<Screen>(Screen.Onboarding) }` drives the entire app. Each screen composable receives typed lambdas (`onNavigateTo*`, `onBack`) for transitions.

**Screen flow:**
```
Onboarding (3-page animated) → SignUp / Login / ForgotPassword → Home
Home → Profile | Activities | TripDetail | InsightDetail | Insights | Stats
```
`LogActivitySheet` (a `ModalBottomSheet`) is a global overlay rendered above any screen; it is controlled by `showLogActivity: Boolean` in `AtmosApp`.

### State model

Each screen has an immutable `UiState` data class (e.g. `HomeUiState`, `ProfileUiState`). Data models are co-located in the same file as their `UiState` (e.g. `TransportModeType`, `InsightEntry`, `RecentActivityEntry` all live in `HomeUiState.kt`).

There are **no ViewModels**. `AtmosApp` holds all runtime state in `remember` variables and drives fetches via `LaunchedEffect` blocks keyed on screen/trigger state. Each screen receives only the data it needs via parameters.

When adding a new screen:
1. Add a `data object MyScreen : Screen()` entry to the sealed class in `AtmosApp.kt`.
2. Add the screen's `UiState` and data models to a new `MyScreenUiState.kt` alongside the screen file.
3. Wire the `when (screen)` branch in `AtmosApp`, including any `LaunchedEffect` fetch blocks.

### Network layer

All API calls go through `AtmosHttpClient` (Ktor). Each domain has its own service class in `network/`. Services return `Result<T>` — callers use `.onSuccess { }` / `.onFailure { }`. The HTTP client automatically injects the JWT access token and triggers `TokenRefresher` on 401.

**Important**: CO₂ values always come from the backend. Never compute them locally using `TransportModeType.emissionFactor` for displaying statistics — that property exists only for the real-time estimate in `LogActivitySheet`.

### Trip detection

`TripDetectorState` is a singleton `object` with `StateFlow`s collected in `AtmosApp`:
- `ongoingSession` — active trip being tracked (drives `OngoingTripCard`)
- `pendingSession` — completed trip awaiting confirmation (drives `PendingTripCard`)
- `recentlySaved` — just-saved trip (triggers home timeline re-fetch)
- `permissionState` — `LocationPermissionState` (drives onboarding permission pills)
- `notificationsGranted` — drives notification permission pill

Android uses Activity Recognition Transitions + FusedLocation inside a foreground `TripDetectionService`. iOS uses `CLLocationManager` + `CMMotionActivityManager`. Both share identical session lifecycle logic via `TripDetectorState`.

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

### Reusable components (`shared/src/commonMain/kotlin/dev/atmos/shared/ui/common/`)

- **`AtmosCard`** — zero-elevation card with themed surface color and 20dp content padding. Use this for all card-shaped containers.
- **`CircularProgressRing`** — custom Canvas ring for daily goal progress.
- **`Shimmer`** — loading skeleton modifier. Each major screen section has a named skeleton composable (`TodayImpactSkeleton`, `WeeklyTrendSkeleton`, `InsightsSkeleton`, etc.).

### Home screen layout

`HomeScreen` is a `LazyColumn` inside a `Scaffold`. It renders one of four states based on `HomeUiState`:
- **Loading** — shimmer skeletons for each card
- **Error** — `HomeErrorState` with retry CTA
- **Empty** (no activities yet) — `HomeEmptyState` with first-trip CTA
- **Populated** — `TodayImpactCard`, `WeeklyTrendCard`, `TransportBreakdownCard`, `RecentActivityCard`, `InsightsSection`

`AtmosBottomBar` is a custom bottom nav with a center-notch FAB (+) floating above it. It has two tabs: **Home** and **Activities**. The FAB opens `LogActivitySheet`.

### iOS build notes

The standard `embedAndSignAppleFrameworkForXcode` Gradle task is **not used** — it is broken in KMP 2.1. Two custom Xcode shell script build phases handle the framework compile and embed instead. The shared framework is built as **dynamic** (`isStatic = false` in `shared/build.gradle.kts`). Do not change this to static — iOS cannot install a static `ar archive`.
