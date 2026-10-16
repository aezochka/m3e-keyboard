# M3E Keyboard

Custom Android keyboard (IME) with Material 3 Expressive design, built with Jetpack Compose.

## Features
- English (QWERTY) + Russian (ЙЦУКЕН) layouts, symbols layer
- Shift / Caps lock, auto-repeat backspace
- Material You dynamic colors (Android 12+), dark mode
- Expressive key shapes, springy press animation, haptic feedback
- Settings: key height, default language, dynamic colors
- Enter key adapts to the action of the app (GO / SEARCH / SEND / DONE / NEXT)

## Setup
1. Install the APK, open the app
2. Enable **M3E Keyboard** in system input-method settings
3. Select it as the active keyboard

## Build
```
./gradlew :app:assembleDebug
```

Requires JDK 17. CI builds both debug and release APKs on every push to `main`.
