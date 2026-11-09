# C++ Server Tests

## Running Locally

### Linux
```bash
cmake -S server -B build/server-tests
cmake --build build/server-tests --parallel 2
ctest --test-dir build/server-tests --output-on-failure
```

### Windows
```powershell
cmake -S server -B build/server-tests -A x64 `
  -DCMAKE_TOOLCHAIN_FILE="$env:VCPKG_INSTALLATION_ROOT/scripts/buildsystems/vcpkg.cmake"
cmake --build build/server-tests --config Release --parallel 2
ctest --test-dir build/server-tests -C Release --output-on-failure
```

## Test File

`server/tests/test_protocol_and_dispatch.cpp` contains all server-side tests.

### Test Categories

| Category | Tests | Description |
|---|---|---|
| ProtocolOpcodes | 3 | Constants match Kotlin, uniqueness, range |
| ExecuteDispatch | 16 | Opcode dispatch to correct methods |
| DeviceDetailsParsing | 7 | Handshake string parsing |
| ConnectionType | 2 | Enum values |
| DeviceInfo | 2 | Structure construction |
| Roundtrip | 10 | Client bytes → server dispatch |
| SensitivityCalculation | 5 | Math behavior of sensitivity |
| Edge Cases | 2 | Extra bytes, sequences |

## Architecture

The test binary (`pocket_pad_tests`) links against:
- GoogleTest (fetched via CMake FetchContent)
- `protocol.h` header only (no wxWidgets, no Asio, no OS-specific code)

A `StubInputManager` subclass of `InputManager::Base` records method calls
without requiring uinput or SendInput.

## Protocol Header

`server/src/input/protocol.h` was extracted from `inputmanager.hpp` to enable
testing without GUI dependencies. It contains:
- Opcode constants (LCLICK through MEDIA)
- `InputManager::Base` class with `execute()` dispatch
- `Command` struct
- `MOVE_SENSITIVITY` / `SCROLL_SENSITIVITY` extern declarations

This is a non-functional change — no bytes, opcodes, or behavior were modified.
