# Integration Tests

## Scope

Integration tests would validate communication between the Android client and
C++ server without requiring real hardware. Currently, this is limited because:

1. The server requires wxWidgets (GUI library) to compile
2. The client requires an Android runtime
3. Real networking requires socket coordination

## Current State

The protocol consistency tests serve as a form of integration validation:
- Client-side tests verify the bytes that would be sent
- Server-side tests verify those same bytes would be correctly dispatched
- The `RoundtripTest` suite explicitly tests the client→server byte flow

## Future Integration Test Plan

When resources permit, the following integration tests could be added:

### 1. Headless Server Mode
Extract the Server class from wxWidgets dependency to allow running a
headless test server. This would enable:
- Real TCP connection tests
- Real UDP datagram tests
- Handshake validation
- Disconnect handling

### 2. Protocol Fuzzing
Generate random byte sequences and feed them to `execute()` to discover
crashes or undefined behavior.

### 3. Socket Roundtrip
Start a real server on a random port, connect a Kotlin test client, send
events, and verify the server receives and dispatches them correctly.
