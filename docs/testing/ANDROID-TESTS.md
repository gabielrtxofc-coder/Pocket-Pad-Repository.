# Android Tests

## Running Locally

```bash
cd client
bash ./gradlew testDebugUnitTest
```

Reports: `client/app/build/reports/tests/testDebugUnitTest/`
Results: `client/app/build/test-results/testDebugUnitTest/`

## Test Files

### InputEventTest.kt
- Location: `client/app/src/test/java/com/darusc/mousedroid/mkinput/`
- Validates: `InputEvent` sealed class data construction, equality, enum completeness
- Count: 30+ tests

### TranslatorsSocketTest.kt
- Location: `client/app/src/test/java/com/darusc/mousedroid/networking/`
- Validates: `toSocketReport()` byte encoding for all event types
- Tests: opcodes, byte sizes, boundary values, edge cases
- Count: 45+ tests

### TranslatorsHIDTest.kt
- Location: `client/app/src/test/java/com/darusc/mousedroid/networking/`
- Validates: `toHIDReport()` HID report encoding
- Tests: button codes, direction inversion, zoom, media, battery
- Count: 55+ tests

### ReportsTest.kt
- Location: `client/app/src/test/java/com/darusc/mousedroid/networking/bluetooth/`
- Validates: HID report byte layout and constants
- Tests: MouseReport, KeyboardReport, MediaReport, BatteryReport
- Count: 35+ tests

### ProtocolConsistencyTest.kt
- Location: `client/app/src/test/java/com/darusc/mousedroid/networking/`
- Validates: Client-server protocol contract
- Tests: opcode uniqueness, message sizes, direction consistency
- Count: 10+ tests

### KeyboardLayoutTest.kt
- Location: `client/app/src/test/java/com/darusc/mousedroid/layouts/`
- Validates: Character-to-keycode mapping
- Tests: US layout letters, digits, symbols, Keycode constants
- Count: 20+ tests

## Notes

- `android.util.Log` is handled via `testOptions.unitTests.returnDefaultValues = true`
- No Robolectric or Mockito required for current test suite
- Tests run on JVM, not on device
- Instrumented tests use the existing `androidTest` configuration (currently empty)
