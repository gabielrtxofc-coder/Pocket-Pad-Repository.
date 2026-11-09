# Test Coverage

## What IS Covered

### Protocol Layer (fully covered)
- All 10 opcode constants match between Kotlin and C++
- Byte encoding for all event types in both socket and HID paths
- Direction inversion consistency (socket vs HID)
- Message sizes for all event types
- Opcode uniqueness and completeness

### Data Layer (fully covered)
- InputEvent sealed class construction and equality
- All MouseButton enum values
- All MediaAction enum values
- HID report byte layout
- Report ID constants

### Keyboard Layer (partially covered)
- US layout letter mappings
- US layout digit mappings
- US layout special character mappings
- Modifier key behavior
- Keycode constant values

### Server Dispatch (fully covered)
- All 10 opcodes dispatch to correct methods
- MOVE negation behavior
- Unknown opcode handling
- Boundary value handling
- Sequence of events

### Sensitivity Math (covered)
- Default values
- Calculation formula at sensitivity 1, 10, 20

## What is NOT Covered

### Requires Hardware
- Bluetooth HID connection establishment
- USB/ADB reverse port forwarding
- Actual cursor movement via uinput/SendInput
- Real device touch events
- Latency measurement
- Physical gesture recognition

### Requires Integration Environment
- TCP connection handshake
- UDP datagram delivery
- Socket reconnection after disconnect
- Network timeout behavior
- Concurrent connections

### Not Yet Specified
- Behavior with malformed handshake strings
- Behavior with truncated messages
- Behavior with out-of-range sensitivity values
- Thread safety of server-side dispatch
- Memory leaks in connection lifecycle

## Coverage Summary

| Area | Tests | Status |
|---|---|---|
| Protocol opcodes | 10+ | ✅ Covered |
| Socket byte encoding | 45+ | ✅ Covered |
| HID byte encoding | 55+ | ✅ Covered |
| Report classes | 35+ | ✅ Covered |
| Event data classes | 30+ | ✅ Covered |
| Server dispatch | 16+ | ✅ Covered |
| Device parsing | 7+ | ✅ Covered |
| Keyboard layout | 20+ | ✅ Covered |
| Bluetooth connection | 0 | ❌ Hardware |
| TCP/UDP transport | 0 | ❌ Hardware |
| Gesture recognition | 0 | ❌ Hardware |
