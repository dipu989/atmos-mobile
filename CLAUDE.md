# atmos-mobile

KMP app (Android + iOS). All shared UI in Compose Multiplatform. No ViewModels — all runtime state lives in `AtmosApp` via `remember`. Navigation is a hand-rolled `sealed class Screen`.

## Commands

```bash
./gradlew :shared:compileDebugKotlinAndroid   # verify before every commit
./gradlew :shared:compileKotlinIosArm64       # verify iOS target
./gradlew :androidApp:installDebug            # install on connected device
```

No automated tests. Verify visually against live backend (`https://api.atmosapp.dev`).

## Commits — enforced by commitlint in CI

```
<type>(<scope>): <subject>
```

Allowed: `feat` `fix` `perf` `refactor` `chore` `docs` `test` `ci` `revert`  
**Not allowed (fail CI):** `build` `style`

## Rules

- Never push to `main`. Branch → PR always.
- CO₂ values come from the backend. Never use `TransportModeType.emissionFactor` for statistics — only use it for the real-time estimate inside `LogActivitySheet`.
- Colors inside composables via `LocalAtmosColors.current`. The top-level aliases (`SkyWhite`, `CardSurface`, etc.) are legacy and light-only — they exist only for Canvas drawing code.
- Adding a screen: add `data object MyScreen : Screen()` → add `UiState` data class → wire `when (screen)` branch in `AtmosApp` with its `LaunchedEffect` fetch.
- `PlaceAutocompleteField` search calls must be wrapped in `try/finally` to reset `isSearching`. Coroutine cancellation skips the `finally`-free path.

## Non-obvious gotchas

**iOS framework is dynamic** — `isStatic = false` in `shared/build.gradle.kts`. Do not change to static; iOS can't install a static `ar` archive. The standard `embedAndSignAppleFrameworkForXcode` Gradle task is NOT used (broken in KMP 2.1) — two custom Xcode shell script phases handle it.

**`placeSearchService` singleton** — declared at file level in `PlaceAutocompleteField.kt`. `PlaceSearchService` holds no mutable state, so sharing one instance across composables is safe.

**`isAutoDetected`** — mapped from `ActivityDto.source != "manual"`. Was previously hardcoded `false` for all backend-fetched activities; fixed in feat/dedup-surface.

**Trip detection state** — modify `TripDetectorState` flows (`pendingSession`, `ongoingSession`, etc.) to surface new trip data. Don't touch the `expect`/`actual` impl files unless changing the detection logic itself.
