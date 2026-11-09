# CI Test Operations

## Workflow

The CI workflow (`.github/workflows/foundation-build.yml`) runs tests in
multiple jobs:

### Android Job
1. Runs `bash ./gradlew testDebugUnitTest assembleDebug`
2. Summarizes test results via XML parsing
3. Uploads test reports as artifacts

### Android Instrumented Job
1. Starts API 34 emulator
2. Runs `bash ./gradlew connectedDebugAndroidTest`
3. Summarizes and uploads instrumented test results

### Linux Server Job
1. Installs build dependencies
2. Builds and installs wxWidgets 3.3.4
3. Configures with CMake (includes test target)
4. Compiles server AND test binary
5. Runs `ctest --output-on-failure`
6. Uploads Linux executable

### Windows Server Job
1. Checks out into Windows-safe path
2. Installs vcpkg dependencies
3. Configures with CMake
4. Compiles server AND test binary
5. Runs `ctest -C Release --output-on-failure`
6. Uploads Windows executable

## Test Failure Behavior

- Any test failure causes the job to fail
- Reports are uploaded even on failure (`if: always()`)
- No tests are silently ignored

## Artifacts

| Artifact | Contains |
|---|---|
| `android-test-reports` | HTML + XML test results |
| `android-instrumented-test-reports` | Instrumented test results |
| `windows-compile-log` | Full compilation log |
