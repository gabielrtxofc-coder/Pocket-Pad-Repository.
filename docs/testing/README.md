# Pocket Pad — Test Infrastructure

> Missão 05 — Test Engineering, Automated Testing & Validation Infrastructure

This directory contains all documentation related to the test infrastructure
of the Pocket Pad project.

---

## Purpose

The test infrastructure was built to detect real regressions in the Pocket Pad
codebase. It covers:

1. **Android Unit Tests** — pure logic (translators, protocol, HID reports)
2. **C++ Server Tests** — opcode dispatch, protocol constants, device parsing
3. **Protocol Consistency Tests** — client ↔ server contract validation
4. **CI Integration** — all tests run automatically on every push/PR

## Documents

| Document | Description |
|---|---|
| [TEST-ARCHITECTURE.md](TEST-ARCHITECTURE.md) | Overall test architecture and strategy |
| [ANDROID-TESTS.md](ANDROID-TESTS.md) | Android test suite documentation |
| [CPP-TESTS.md](CPP-TESTS.md) | C++ server test suite documentation |
| [PROTOCOL-TESTS.md](PROTOCOL-TESTS.md) | Protocol consistency and validation |
| [CI-TEST-OPERATIONS.md](CI-TEST-OPERATIONS.md) | CI pipeline test operations |
| [TEST-COVERAGE.md](TEST-COVERAGE.md) | What is and isn't covered |
| [KNOWN-LIMITATIONS.md](KNOWN-LIMITATIONS.md) | Known limitations and gaps |
| [MISSION-05-REPORT.md](MISSION-05-REPORT.md) | Mission 05 final report |

## Quick Start

### Running Android Tests Locally

```bash
cd client
bash ./gradlew testDebugUnitTest
```

### Running C++ Tests Locally (Linux)

```bash
cmake -S server -B build/server-tests
cmake --build build/server-tests --parallel 2
ctest --test-dir build/server-tests --output-on-failure
```

### Running C++ Tests Locally (Windows)

```powershell
cmake -S server -B build/server-tests -A x64 `
  -DCMAKE_TOOLCHAIN_FILE="$env:VCPKG_INSTALLATION_ROOT/scripts/buildsystems/vcpkg.cmake"
cmake --build build/server-tests --config Release --parallel 2
ctest --test-dir build/server-tests -C Release --output-on-failure
```

## Test Categories

| Category | Location | Framework | Runs On |
|---|---|---|---|
| Android Unit | `client/app/src/test/` | JUnit 4 | JVM |
| Android Instrumented | `client/app/src/androidTest/` | AndroidX Test | Emulator |
| C++ Server | `server/tests/` | GoogleTest 1.14 | Linux/Windows |
| Protocol Consistency | Both sides | JUnit + GTest | CI |

## What We Test

- ✅ Translators (toSocketReport, toHIDReport)
- ✅ Protocol opcodes (client ↔ server consistency)
- ✅ HID report encoding
- ✅ Input event data classes
- ✅ Keyboard layout mappings
- ✅ Opcode dispatch logic (server)
- ✅ Device details parsing
- ✅ Sensitivity calculations
- ✅ Report byte layout
- ✅ Edge cases and boundary values

## What Requires Hardware

- ❌ Bluetooth HID connection
- ❌ USB/ADB port forwarding
- ❌ Actual cursor movement
- ❌ Real device latency measurement
- ❌ Multi-touch gesture recognition
- ❌ Physical keyboard input

See [KNOWN-LIMITATIONS.md](KNOWN-LIMITATIONS.md) for details.
