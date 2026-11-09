#pragma once

/**
 * InputManager header — includes the protocol definition and re-exports it.
 *
 * The protocol constants (opcodes) and Base class are defined in protocol.h
 * to enable unit testing without wxWidgets or OS-specific dependencies.
 * This header includes protocol.h for backward compatibility with existing
 * code that includes "inputmanager.hpp" directly.
 */

#include "input/protocol.h"
