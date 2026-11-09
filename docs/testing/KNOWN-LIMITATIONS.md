# Known Limitations

## Hardware-Dependent (Cannot Be Automated)

| Feature | Why | Future Plan |
|---|---|---|
| Bluetooth HID connection | Requires Bluetooth hardware + host | Manual testing guide |
| USB/ADB reverse | Requires USB device + adb | Manual testing guide |
| Cursor movement | Requires uinput/SendInput | Integration test with mock |
| Touch gestures | Requires Android MotionEvent | Robolectric or instrumented |
| Latency | Requires real network | Benchmark suite |

## Environment Limitations

1. **`android.util.Log` in tests**: Handled via `returnDefaultValues = true`.
   Log messages are silently discarded in unit tests.

2. **Server requires wxWidgets**: The test binary avoids this by using
   `protocol.h` instead of `inputmanager.hpp`.

3. **C++ tests cannot verify actual input injection**: The StubInputManager
   records calls but doesn't produce real input events.

## Known Behavior Issues (Documented, Not Fixed)

1. **Middle click socket divergence**: `MouseClick(MIDDLE)` produces RCLICK
   (0x02) in socket mode but code 4 in HID mode. Documented in tests.

2. **Server message size**: `execute()` reads bytes[1], bytes[2] without
   checking the size parameter. Documented in tests.

3. **Sensitivity=1 produces zero movement**: `(int)(1/10.0 * 1) = 0`. This
   means the lowest sensitivity setting effectively disables movement for
   single-pixel events.

4. **BatteryEvent over socket**: Produces empty report. Battery is only
   reported via Bluetooth HID.

## Pending Physical Validation

The following validations require a real device and PC:

- [ ] Wi-Fi mode: cursor movement accuracy
- [ ] USB/ADB mode: cursor responsiveness
- [ ] Bluetooth mode: HID report delivery
- [ ] All gesture types on real hardware
- [ ] Multi-touch scroll accuracy
- [ ] Keyboard input across all 4 layouts
- [ ] Media key functionality
- [ ] Connection stability under load
- [ ] Battery reporting accuracy
