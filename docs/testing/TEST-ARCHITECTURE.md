# Test Architecture

## Overview

The Pocket Pad test infrastructure validates the binary protocol contract between
the Kotlin Android client and the C++ server. Tests are organized by layer:

```
┌─────────────────────────────────────────────────┐
│              Integration Tests                   │
│   (Protocol roundtrip: client bytes → server)   │
├─────────────────────────────────────────────────┤
│              Component Tests                     │
│   (Translators, HID Reports, Dispatch Logic)    │
├─────────────────────────────────────────────────┤
│              Unit Tests                          │
│   (InputEvent, Keycode, Report classes)         │
└─────────────────────────────────────────────────┘
```

## Test Locations

```
client/
  app/src/test/                          # Android JVM unit tests
    java/com/darusc/mousedroid/
      mkinput/InputEventTest.kt          # Data class validation
      networking/
        TranslatorsSocketTest.kt         # toSocketReport() tests
        TranslatorsHIDTest.kt            # toHIDReport() tests
        ProtocolConsistencyTest.kt       # Cross-layer consistency
      networking/bluetooth/
        ReportsTest.kt                   # HID report byte encoding
      layouts/
        KeyboardLayoutTest.kt            # Character → keycode mapping

server/
  tests/
    test_protocol_and_dispatch.cpp       # Server-side protocol tests
  src/input/
    protocol.h                           # Extracted for testability
```

## Classification of Components

| Component | Classification | Rationale |
|---|---|---|
| `InputEvent` sealed class | TESTABLE | Pure Kotlin data classes |
| `Translators.toSocketReport()` | TESTABLE | Pure Kotlin, no Android deps |
| `Translators.toHIDReport()` | TESTABLE_WITH_SEAM | Uses `android.util.Log`, needs `returnDefaultValues` |
| `Reports.kt` (HID reports) | TESTABLE | Pure Kotlin byte encoding |
| `Keycode` constants | TESTABLE | Object with constants |
| `KeyboardLayout.getMapping()` | TESTABLE | Pure Kotlin map lookup |
| `ConnectionManager` | INTEGRATION_ONLY | Singleton, requires coroutines + Context |
| `TCPConnection` / `UDPConnection` | HARDWARE_REQUIRED | Requires real socket + server |
| `BluetoothConnection` | HARDWARE_REQUIRED | Requires Bluetooth hardware |
| `GestureHandler` | HARDWARE_REQUIRED | Requires Android MotionEvent from real touch |
| `InputManager::Base::execute()` | TESTABLE | Pure dispatch logic |
| `Server::StartReceive()` | HARDWARE_REQUIRED | Requires Asio + network |
| `Connection` (C++) | TESTABLE_WITH_SEAM | Parsing logic testable, socket requires mock |

## Dependencies

### Android (build.gradle)
```groovy
testImplementation 'junit:junit:4.13.2'
testOptions {
    unitTests.returnDefaultValues = true
}
```

### C++ (CMakeLists.txt)
```cmake
FetchContent_Declare(
    googletest
    GIT_REPOSITORY https://github.com/google/googletest.git
    GIT_TAG        v1.14.0
)
```

## Key Design Decisions

1. **JUnit 4 over JUnit 5**: JUnit 4 is the standard for Android unit tests
   and is already declared in the project's dependencies.

2. **GoogleTest for C++**: Widely supported on Linux and Windows, integrates
   with CTest, and can be fetched without manual installation.

3. **`returnDefaultValues = true`**: Allows `android.util.Log` calls to return
   default values instead of throwing in JVM tests. This avoids needing
   Robolectric for the translator tests.

4. **`protocol.h` extraction**: The opcode constants and `execute()` dispatch
   logic were extracted into a separate header to enable testing without
   pulling in wxWidgets or Asio. This is a non-functional change.

5. **No mocking framework for C++**: The `StubInputManager` subclass approach
   is simpler than introducing a mock framework and sufficient for testing
   the dispatch logic.

6. **Independent expected values**: All test assertions use expected values
   calculated independently of the implementation, not by reproducing the
   implementation's logic.
