# Repository Guidelines

## Project Structure & Module Organization

This is a single-module Android application. The `app/` module contains all
production code and resources:

- `app/src/main/java/com/viniciuscoscia/kotlincoroutinesflow/` holds Kotlin and
  Jetpack Compose UI code; keep theme code in `ui/theme/`.
- `app/src/main/res/` contains Android resources, including strings, themes,
  launcher assets, and XML configuration.
- `app/src/test/` contains local JVM unit tests; `app/src/androidTest/` contains
  device/emulator instrumentation tests.
- `gradle/libs.versions.toml` is the version catalog. Add or update dependency
  versions there rather than hard-coding them in module build files.

## Build, Test, and Development Commands

Run commands from the repository root:

```bash
./gradlew assembleDebug              # Build the debug APK
./gradlew test                       # Run local unit tests
./gradlew connectedDebugAndroidTest  # Run device/emulator tests
./gradlew lint                       # Run Android lint checks
```

Use Android Studio for interactive runs and previews. Do not commit generated
outputs such as `app/build/` or IDE-local configuration.

## Coding Style & Naming Conventions

Use idiomatic Kotlin with four-space indentation and the existing Kotlin DSL
style in `*.gradle.kts` files. Prefer Jetpack Compose for UI and keep
composables focused and state-hoisted where appropriate. Use `PascalCase` for
classes and composables, `camelCase` for functions, properties, and test names,
and lowercase `snake_case` for resource names (for example,
`screen_profile.xml`). Keep packages under
`com.viniciuscoscia.kotlincoroutinesflow`.

## Testing Guidelines

Write local tests with JUnit 4 in `app/src/test` and Android tests with
AndroidX JUnit/Espresso or Compose UI testing in `app/src/androidTest`. Name
tests after the behavior they verify, such as `loadProfile_showsErrorState`.
Run `./gradlew test` before every change handoff; run instrumentation tests for
changes that depend on Android framework behavior or Compose UI interaction.

## Commit & Pull Request Guidelines

The repository has no commit history yet. Use concise imperative Conventional
Commit-style messages, for example `feat: add flow collection example` or
`fix: handle empty state`. Keep commits focused. Pull requests should explain
the user-visible change, list validation performed, link the relevant issue
when applicable, and include screenshots or recordings for UI changes.
