# Contributing to Complete Linux Installer

Thank you for your interest in contributing to Complete Linux Installer (LinuxOnAndroid)!

## Getting Started

1. Fork the repository and create a feature or bugfix branch from `main` (or the active release branch if specified).
2. Make sure you can build and run unit tests locally:
   ```bash
   ./gradlew testDebugUnitTest
   ./gradlew assembleDebug
   ```
3. If your change touches distribution installers or Software Hub recipes, test on a real Android device or emulator and check the relevant validation scripts under `scripts/`.

## Pull Request Guidelines

- Keep PRs focused on a single feature, fix, or improvement.
- Fill out all sections of the Pull Request template, including how you tested your changes.
- Ensure `./gradlew testDebugUnitTest` passes with no new warnings or regressions.
- Preserve existing comments, docstrings, and code formatting conventions.
- Do not modify `app/src/main/jniLibs/` binaries unless your PR specifically updates native dependencies and rebuilds them via `scripts/build_native_libs.sh`.

## AI Usage Policy

AI coding assistants are welcome as development aids, provided you follow our [AI Usage Policy](AI.md): you must review every line, test your changes, and take full human ownership of your PR. Automated bot PRs are rejected.

## Reporting Bugs

Please use the [GitHub Bug Report form](https://github.com/devwithzachary/LinuxOnAndroid/issues/new/choose) and include the **Debug Report** from **Settings -> Diagnostics & Debug Report -> Generate Debug Report** in the app.

