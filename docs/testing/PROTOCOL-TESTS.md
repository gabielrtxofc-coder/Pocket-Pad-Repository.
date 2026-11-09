# Protocol Tests

## Purpose

Protocol tests validate the binary contract between the Kotlin client and the
C++ server. This is the most critical test suite because the protocol has no
versioning, no size headers, and no CRC — any divergence silently breaks
communication.

## Protocol Specification

| Opcode | Name | Size | Client (Kotlin) | Server (C++) |
|---|---|---|---|---|
| 0x01 | LCLICK | 1 | MouseClick(LEFT) | click() |
| 0x02 | RCLICK | 1 | MouseClick(RIGHT/MIDDLE*) | right_click() |
| 0x03 | DOWN | 1 | MouseDragState(down) | down() |
| 0x04 | UP | 1 | MouseDragState(up) | up() |
| 0x05 | MOVE | 3 | MouseMove(dx, dy) | move(-dx, -dy) |
| 0x06 | SCROLL | 2 | MouseScroll(0, dy) | scroll(dy) |
| 0x07 | KEYPRESS | 3 | KeyPress/NumpadKeyPress | send_key(key, mod) |
| 0x08 | SCROLL_H | 2 | MouseScroll(dx, 0) | scroll_h(dx) |
| 0x09 | ZOOM | 2 | Zoom(scale) | zoom(scale) |
| 0x0A | MEDIA | 2 | MediaEvent(action) | media(bitmask) |

*Middle click maps to RCLICK in socket mode (known limitation).

## Tests

### Client Side (Kotlin)
- `ProtocolConsistencyTest.kt` validates opcode values and uniqueness
- `TranslatorsSocketTest.kt` validates byte encoding per opcode

### Server Side (C++)
- `ProtocolOpcodes.MatchKotlinClientValues` validates constants match
- `RoundtripTest` validates that client bytes trigger correct server actions

## Known Divergences

1. **Middle click**: Socket mode maps MIDDLE→RCLICK; HID mode has separate code 4
2. **No message validation**: Server reads bytes[1], bytes[2] without checking size
3. **Battery**: Not sent over socket; only over Bluetooth HID
