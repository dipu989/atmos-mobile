<h1 align="center">Atmos Mobile</h1>
<img width="1774" height="887" alt="atmos-mobile" src="https://github.com/user-attachments/assets/d9ad6ea8-dca4-43d5-9ef1-bf55195eb34e" />


Personal carbon footprint tracker for Android and iOS, built with Kotlin Multiplatform and Compose Multiplatform.

---

## Overview

Atmos helps you understand and reduce your daily carbon footprint by tracking how you move through the world. The app logs transport activity, calculates CO₂ emissions in real time, and surfaces weekly trends and actionable insights — all with minimal manual input.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.1 |
| UI | Compose Multiplatform 1.8 |
| Networking | Ktor 3.1 |
| DI | Koin 4.0 |
| Async | Kotlinx Coroutines 1.10 |
| Serialization | Kotlinx Serialization 1.8 |
| Date/Time | Kotlinx DateTime 0.6 |
| Local storage | Multiplatform Settings 1.2 |
| Android build | AGP 8.10 |

---

## Project Structure

```
atmos-mobile/
├── shared/                         # KMP module — all shared code
│   └── src/
│       ├── commonMain/             # UI, state, networking, business logic
│       │   └── kotlin/dev/atmos/shared/
│       │       ├── ui/
│       │       │   ├── home/       # Home screen + state
│       │       │   ├── common/     # Reusable components
│       │       │   └── theme/      # Colors, typography, shapes
│       ├── androidMain/            # Android-specific implementations
│       └── iosMain/                # iOS entry point (MainViewController)
├── androidApp/                     # Android host app
└── iosApp/                         # Xcode project (iOS host)
```

---

## Requirements

- Android Studio Meerkat or newer (includes KMP plugin)
- Xcode 15+
- JDK 11 (bundled with Android Studio)
- iOS 15+ simulator or device
- Android API 26+ device or emulator

---

## Running the App

### Android

Open the project in Android Studio and run the `androidApp` configuration on an emulator or device.

### iOS

The Xcode project is pre-configured with two custom build phases that handle the Kotlin framework automatically.

Run the `iosApp` configuration from Android Studio, or open `iosApp/iosApp.xcodeproj` directly in Xcode and hit Run.

> **Note:** The first iOS build will take a few minutes while Gradle compiles the shared framework. Subsequent builds are fast.

---

## iOS Build Notes

The iOS setup avoids the standard `embedAndSignAppleFrameworkForXcode` Gradle task due to a KMP 2.1 incompatibility. Instead, two shell script build phases handle it:

- **KMP Framework Build** — invokes Gradle to compile `shared.framework`
- **KMP Framework Embed** — copies the built framework into the app bundle

The framework is built as a **dynamic library** (`isStatic = false`). Static linking produces an `ar archive` that iOS cannot install.

---

## Architecture

Shared UI is written entirely in Compose Multiplatform inside `commonMain`. Platform entry points are minimal:

- **Android** — `MainActivity` sets `AtmosTheme { HomeScreen() }`
- **iOS** — `MainViewController.kt` wraps the same composable in `ComposeUIViewController`, exposed to Swift via `ContentView.swift`

State is modelled as immutable `UiState` data classes. The `HomeUiState` drives the entire home screen from a single source of truth.

---

## License

MIT License — Copyright (c) 2025 Shantnu Kumar

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
