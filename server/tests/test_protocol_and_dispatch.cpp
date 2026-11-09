/**
 * Pocket Pad — C++ Server Test Suite
 *
 * This file contains unit tests for the server-side components that can be
 * tested without hardware or GUI dependencies:
 *
 * 1. InputManager::Base::execute() — opcode dispatch logic
 * 2. Protocol opcode constants — consistency with Kotlin client
 * 3. DeviceInfo parsing — handshake string format
 *
 * The test approach uses a recording mock (StubInputManager) that captures
 * calls to virtual methods without requiring uinput or SendInput.
 */

#include <gtest/gtest.h>
#include <cstdint>
#include <cstring>
#include <string>
#include <sstream>
#include <vector>
#include <functional>

// We need to include the inputmanager header but it depends on logger.h
// and has virtual methods tied to the OS. We'll extract the testable logic
// by re-defining the protocol constants and the execute() dispatch logic
// in a test-friendly way, verified against the actual header.

// Include the protocol header (extracted for testing without wxWidgets)
#include "input/protocol.h"

// Include device.h for DeviceInfo parsing test
#include "net/device.h"

/**
 * StubInputManager — records all method calls for verification.
 * Inherits from InputManager::Base and overrides all pure virtual methods
 * to record invocations instead of interacting with the OS.
 */
class StubInputManager : public InputManager::Base {
public:
    struct Call {
        std::string method;
        int arg1 = 0;
        int arg2 = 0;
    };

    std::vector<Call> calls;

    void click() const override {
        const_cast<StubInputManager*>(this)->calls.push_back({"click", 0, 0});
    }

    void right_click() const override {
        const_cast<StubInputManager*>(this)->calls.push_back({"right_click", 0, 0});
    }

    void move(int dx, int dy) const override {
        const_cast<StubInputManager*>(this)->calls.push_back({"move", dx, dy});
    }

    void scroll(int dy) const override {
        const_cast<StubInputManager*>(this)->calls.push_back({"scroll", dy, 0});
    }

    void scroll_h(int dx) const override {
        const_cast<StubInputManager*>(this)->calls.push_back({"scroll_h", dx, 0});
    }

    void down() const override {
        const_cast<StubInputManager*>(this)->calls.push_back({"down", 0, 0});
    }

    void up() const override {
        const_cast<StubInputManager*>(this)->calls.push_back({"up", 0, 0});
    }

    void send_key(uint8_t keycode, uint8_t modifier) const override {
        const_cast<StubInputManager*>(this)->calls.push_back({"send_key", keycode, modifier});
    }

    void zoom(int scale) const override {
        const_cast<StubInputManager*>(this)->calls.push_back({"zoom", scale, 0});
    }

    void media(uint8_t action) const override {
        const_cast<StubInputManager*>(this)->calls.push_back({"media", action, 0});
    }

    void reset() {
        calls.clear();
    }
};


// ============================================================================
// Protocol Opcode Tests
// ============================================================================

/**
 * Verify that C++ opcode constants match the Kotlin client values.
 * These are the contract between client and server — any divergence breaks communication.
 */
TEST(ProtocolOpcodes, MatchKotlinClientValues) {
    // These values must match RawSocketEvents in Translators.kt
    EXPECT_EQ(0x01, InputManager::LCLICK);
    EXPECT_EQ(0x02, InputManager::RCLICK);
    EXPECT_EQ(0x03, InputManager::DOWN);
    EXPECT_EQ(0x04, InputManager::UP);
    EXPECT_EQ(0x05, InputManager::MOVE);
    EXPECT_EQ(0x06, InputManager::SCROLL);
    EXPECT_EQ(0x07, InputManager::KEYPRESS);
    EXPECT_EQ(0x08, InputManager::SCROLL_H);
    EXPECT_EQ(0x09, InputManager::ZOOM);
    EXPECT_EQ(0x0A, InputManager::MEDIA);
}

TEST(ProtocolOpcodes, AllOpcodesAreUnique) {
    std::vector<char> opcodes = {
        InputManager::LCLICK,
        InputManager::RCLICK,
        InputManager::DOWN,
        InputManager::UP,
        InputManager::MOVE,
        InputManager::SCROLL,
        InputManager::KEYPRESS,
        InputManager::SCROLL_H,
        InputManager::ZOOM,
        InputManager::MEDIA
    };

    std::vector<char> sorted_opcodes = opcodes;
    std::sort(sorted_opcodes.begin(), sorted_opcodes.end());
    auto last = std::unique(sorted_opcodes.begin(), sorted_opcodes.end());
    EXPECT_EQ(sorted_opcodes.begin(), last)
        << "All opcodes must be unique";
}

TEST(ProtocolOpcodes, OpcodesInRange1To10) {
    EXPECT_GE(InputManager::LCLICK, 1);
    EXPECT_LE(InputManager::MEDIA, 10);
}


// ============================================================================
// InputManager::Base::execute() Dispatch Tests
// ============================================================================

class ExecuteDispatchTest : public ::testing::Test {
protected:
    StubInputManager stub;

    void SetUp() override {
        stub.reset();
    }
};

TEST_F(ExecuteDispatchTest, LCLICK_callsClick) {
    char bytes[] = {InputManager::LCLICK};
    stub.execute(bytes, sizeof(bytes));
    ASSERT_EQ(1u, stub.calls.size());
    EXPECT_EQ("click", stub.calls[0].method);
}

TEST_F(ExecuteDispatchTest, RCLICK_callsRightClick) {
    char bytes[] = {InputManager::RCLICK};
    stub.execute(bytes, sizeof(bytes));
    ASSERT_EQ(1u, stub.calls.size());
    EXPECT_EQ("right_click", stub.calls[0].method);
}

TEST_F(ExecuteDispatchTest, DOWN_callsDown) {
    char bytes[] = {InputManager::DOWN};
    stub.execute(bytes, sizeof(bytes));
    ASSERT_EQ(1u, stub.calls.size());
    EXPECT_EQ("down", stub.calls[0].method);
}

TEST_F(ExecuteDispatchTest, UP_callsUp) {
    char bytes[] = {InputManager::UP};
    stub.execute(bytes, sizeof(bytes));
    ASSERT_EQ(1u, stub.calls.size());
    EXPECT_EQ("up", stub.calls[0].method);
}

TEST_F(ExecuteDispatchTest, MOVE_callsMoveWithNegatedArgs) {
    // Server negates dx and dy: move(-(int)bytes[1], -(int)bytes[2])
    char bytes[] = {InputManager::MOVE, 10, 20};
    stub.execute(bytes, sizeof(bytes));
    ASSERT_EQ(1u, stub.calls.size());
    EXPECT_EQ("move", stub.calls[0].method);
    EXPECT_EQ(-10, stub.calls[0].arg1); // Negated
    EXPECT_EQ(-20, stub.calls[0].arg2); // Negated
}

TEST_F(ExecuteDispatchTest, MOVE_zeroValues) {
    char bytes[] = {InputManager::MOVE, 0, 0};
    stub.execute(bytes, sizeof(bytes));
    ASSERT_EQ(1u, stub.calls.size());
    EXPECT_EQ("move", stub.calls[0].method);
    EXPECT_EQ(0, stub.calls[0].arg1);
    EXPECT_EQ(0, stub.calls[0].arg2);
}

TEST_F(ExecuteDispatchTest, MOVE_negativeValues) {
    // Negative bytes: -5 as char = 0xFB, -10 as char = 0xF6
    char bytes[] = {InputManager::MOVE, (char)-5, (char)-10};
    stub.execute(bytes, sizeof(bytes));
    ASSERT_EQ(1u, stub.calls.size());
    EXPECT_EQ("move", stub.calls[0].method);
    EXPECT_EQ(5, stub.calls[0].arg1);  // -(-5) = 5
    EXPECT_EQ(10, stub.calls[0].arg2); // -(-10) = 10
}

TEST_F(ExecuteDispatchTest, MOVE_boundaryValues) {
    char bytes[] = {InputManager::MOVE, 127, (char)-128};
    stub.execute(bytes, sizeof(bytes));
    ASSERT_EQ(1u, stub.calls.size());
    EXPECT_EQ(-127, stub.calls[0].arg1); // -(127) = -127
    EXPECT_EQ(128, stub.calls[0].arg2);  // -(-128) = 128
}

TEST_F(ExecuteDispatchTest, SCROLL_callsScroll) {
    char bytes[] = {InputManager::SCROLL, 50};
    stub.execute(bytes, sizeof(bytes));
    ASSERT_EQ(1u, stub.calls.size());
    EXPECT_EQ("scroll", stub.calls[0].method);
    EXPECT_EQ(50, stub.calls[0].arg1);
}

TEST_F(ExecuteDispatchTest, SCROLL_negativeValue) {
    char bytes[] = {InputManager::SCROLL, (char)-30};
    stub.execute(bytes, sizeof(bytes));
    ASSERT_EQ(1u, stub.calls.size());
    EXPECT_EQ("scroll", stub.calls[0].method);
    EXPECT_EQ(-30, stub.calls[0].arg1);
}

TEST_F(ExecuteDispatchTest, SCROLL_H_callsScrollH) {
    char bytes[] = {InputManager::SCROLL_H, 25};
    stub.execute(bytes, sizeof(bytes));
    ASSERT_EQ(1u, stub.calls.size());
    EXPECT_EQ("scroll_h", stub.calls[0].method);
    EXPECT_EQ(25, stub.calls[0].arg1);
}

TEST_F(ExecuteDispatchTest, KEYPRESS_callsSendKey) {
    char bytes[] = {InputManager::KEYPRESS, 0x04, 0x00}; // KEY_A, no modifier
    stub.execute(bytes, sizeof(bytes));
    ASSERT_EQ(1u, stub.calls.size());
    EXPECT_EQ("send_key", stub.calls[0].method);
    EXPECT_EQ(0x04, stub.calls[0].arg1); // keycode
    EXPECT_EQ(0x00, stub.calls[0].arg2); // modifier
}

TEST_F(ExecuteDispatchTest, KEYPRESS_withModifier) {
    char bytes[] = {InputManager::KEYPRESS, 0x04, 0x01}; // KEY_A + Left Ctrl
    stub.execute(bytes, sizeof(bytes));
    ASSERT_EQ(1u, stub.calls.size());
    EXPECT_EQ("send_key", stub.calls[0].method);
    EXPECT_EQ(0x04, stub.calls[0].arg1); // keycode
    EXPECT_EQ(0x01, stub.calls[0].arg2); // modifier
}

TEST_F(ExecuteDispatchTest, ZOOM_callsZoom) {
    char bytes[] = {InputManager::ZOOM, 10};
    stub.execute(bytes, sizeof(bytes));
    ASSERT_EQ(1u, stub.calls.size());
    EXPECT_EQ("zoom", stub.calls[0].method);
    EXPECT_EQ(10, stub.calls[0].arg1);
}

TEST_F(ExecuteDispatchTest, ZOOM_negativeValue) {
    char bytes[] = {InputManager::ZOOM, (char)-15};
    stub.execute(bytes, sizeof(bytes));
    ASSERT_EQ(1u, stub.calls.size());
    EXPECT_EQ("zoom", stub.calls[0].method);
    EXPECT_EQ(-15, stub.calls[0].arg1);
}

TEST_F(ExecuteDispatchTest, MEDIA_callsMedia) {
    char bytes[] = {InputManager::MEDIA, 0x10}; // PLAY_PAUSE
    stub.execute(bytes, sizeof(bytes));
    ASSERT_EQ(1u, stub.calls.size());
    EXPECT_EQ("media", stub.calls[0].method);
    EXPECT_EQ(0x10, stub.calls[0].arg1);
}

TEST_F(ExecuteDispatchTest, UNKNOWN_OPCODE_noAction) {
    // Unknown opcode (0x00 is not defined) should not call any method
    char bytes[] = {0x00};
    stub.execute(bytes, sizeof(bytes));
    EXPECT_EQ(0u, stub.calls.size());
}

TEST_F(ExecuteDispatchTest, HIGH_OPCODE_noAction) {
    // Opcode beyond defined range
    char bytes[] = {0x0B};
    stub.execute(bytes, sizeof(bytes));
    EXPECT_EQ(0u, stub.calls.size());
}

TEST_F(ExecuteDispatchTest, MAX_OPCODE_noAction) {
    // 0xFF is not a valid opcode
    char bytes[] = {(char)0xFF};
    stub.execute(bytes, sizeof(bytes));
    EXPECT_EQ(0u, stub.calls.size());
}


// ============================================================================
// Device Details Parsing Tests
// ============================================================================

/**
 * Tests for the device details string format used in the TCP handshake.
 * Format: "Manufacturer/DeviceName/Model/ConnectionType"
 * Example: "Google/Pixel 6/aosp/1"
 *
 * These tests verify the parsing logic that exists in Connection::Connection()
 * by replicating the split-by-'/' algorithm.
 */
TEST(DeviceDetailsParsing, BasicFormat) {
    std::string details = "Google/Pixel 6/aosp/1";
    std::stringstream ss(details);
    std::string token;
    std::vector<std::string> tokens;
    while (std::getline(ss, token, '/')) {
        tokens.push_back(token);
    }

    ASSERT_EQ(4u, tokens.size());
    EXPECT_EQ("Google", tokens[0]);    // Manufacturer
    EXPECT_EQ("Pixel 6", tokens[1]);   // Name
    EXPECT_EQ("aosp", tokens[2]);      // Model
    EXPECT_EQ("1", tokens[3]);         // Connection type
}

TEST(DeviceDetailsParsing, ConnectionTypeUSB) {
    std::string details = "Samsung/Galaxy S21/SM-G991/0";
    std::stringstream ss(details);
    std::string token;
    std::vector<std::string> tokens;
    while (std::getline(ss, token, '/')) {
        tokens.push_back(token);
    }

    ASSERT_EQ(4u, tokens.size());
    EXPECT_EQ("0", tokens[3]);
    EXPECT_EQ(0, std::atoi(tokens[3].c_str())); // USB
}

TEST(DeviceDetailsParsing, ConnectionTypeWIFI) {
    std::string details = "Samsung/Galaxy S21/SM-G991/1";
    std::stringstream ss(details);
    std::string token;
    std::vector<std::string> tokens;
    while (std::getline(ss, token, '/')) {
        tokens.push_back(token);
    }

    ASSERT_EQ(4u, tokens.size());
    EXPECT_EQ("1", tokens[3]);
    EXPECT_EQ(1, std::atoi(tokens[3].c_str())); // WIFI
}

TEST(DeviceDetailsParsing, EmptyFields) {
    std::string details = "//";
    std::stringstream ss(details);
    std::string token;
    std::vector<std::string> tokens;
    while (std::getline(ss, token, '/')) {
        tokens.push_back(token);
    }

    // All tokens are empty strings
    EXPECT_GE(tokens.size(), 3u);
}

TEST(DeviceDetailsParsing, TooFewTokens) {
    // Only 2 separators instead of 3 — should produce fewer tokens
    std::string details = "Google/Pixel";
    std::stringstream ss(details);
    std::string token;
    std::vector<std::string> tokens;
    while (std::getline(ss, token, '/')) {
        tokens.push_back(token);
    }

    EXPECT_EQ(2u, tokens.size());
    // NOTE: Accessing tokens[2] or tokens[3] here would be out of bounds
    // This documents a potential crash in the real Connection constructor
}

TEST(DeviceDetailsParsing, ExtraSlashes) {
    // Extra slashes produce extra empty tokens
    std::string details = "Google/Pixel/6/aosp/1/extra";
    std::stringstream ss(details);
    std::string token;
    std::vector<std::string> tokens;
    while (std::getline(ss, token, '/')) {
        tokens.push_back(token);
    }

    EXPECT_EQ(6u, tokens.size());
}

TEST(DeviceDetailsParsing, SpecialCharactersInName) {
    std::string details = "Samsung/Galaxy S21 Ultra/SM-G998B/1";
    std::stringstream ss(details);
    std::string token;
    std::vector<std::string> tokens;
    while (std::getline(ss, token, '/')) {
        tokens.push_back(token);
    }

    ASSERT_EQ(4u, tokens.size());
    EXPECT_EQ("Galaxy S21 Ultra", tokens[1]);
}

TEST(DeviceDetailsParsing, InvalidConnectionType) {
    std::string details = "Google/Pixel/aosp/99";
    std::stringstream ss(details);
    std::string token;
    std::vector<std::string> tokens;
    while (std::getline(ss, token, '/')) {
        tokens.push_back(token);
    }

    ASSERT_EQ(4u, tokens.size());
    EXPECT_EQ(99, std::atoi(tokens[3].c_str())); // atoi returns the number, no validation
}


// ============================================================================
// Connection Type Enum Tests
// ============================================================================

TEST(ConnectionType, USB_isZero) {
    EXPECT_EQ(0, USB);
}

TEST(ConnectionType, WIFI_isOne) {
    EXPECT_EQ(1, WIFI);
}


// ============================================================================
// DeviceInfo Structure Tests
// ============================================================================

TEST(DeviceInfo, CanBeConstructed) {
    DeviceInfo info;
    info.ID = 1;
    info.Address = "192.168.1.100";
    info.Manufacturer = "Google";
    info.Name = "Pixel 6";
    info.Model = "aosp";
    info.ctype = WIFI;

    EXPECT_EQ(1, info.ID);
    EXPECT_EQ("192.168.1.100", info.Address);
    EXPECT_EQ("Google", info.Manufacturer);
    EXPECT_EQ("Pixel 6", info.Name);
    EXPECT_EQ("aosp", info.Model);
    EXPECT_EQ(WIFI, info.ctype);
}

TEST(DeviceInfo, DefaultValues) {
    DeviceInfo info;
    EXPECT_EQ("", info.Address);
    EXPECT_EQ("", info.Manufacturer);
    EXPECT_EQ("", info.Name);
    EXPECT_EQ("", info.Model);
}


// ============================================================================
// Roundtrip Protocol Tests
// ============================================================================

/**
 * These tests verify that a message generated by the Kotlin client's
 * toSocketReport() would be correctly dispatched by the server's execute().
 *
 * The byte values here match what the Kotlin client produces for each event.
 */
class RoundtripTest : public ::testing::Test {
protected:
    StubInputManager stub;

    void SetUp() override {
        stub.reset();
    }
};

TEST_F(RoundtripTest, MouseMoveLeft5Down10) {
    // Kotlin: MouseMove(5, 10) → [0x05, 5, 10]
    // Server: execute() → move(-5, -10)
    char bytes[] = {0x05, 5, 10};
    stub.execute(bytes, sizeof(bytes));
    ASSERT_EQ(1u, stub.calls.size());
    EXPECT_EQ("move", stub.calls[0].method);
    EXPECT_EQ(-5, stub.calls[0].arg1);
    EXPECT_EQ(-10, stub.calls[0].arg2);
}

TEST_F(RoundtripTest, LeftClick) {
    // Kotlin: MouseClick(LEFT) → [0x01]
    char bytes[] = {0x01};
    stub.execute(bytes, sizeof(bytes));
    ASSERT_EQ(1u, stub.calls.size());
    EXPECT_EQ("click", stub.calls[0].method);
}

TEST_F(RoundtripTest, RightClick) {
    // Kotlin: MouseClick(RIGHT) → [0x02]
    char bytes[] = {0x02};
    stub.execute(bytes, sizeof(bytes));
    ASSERT_EQ(1u, stub.calls.size());
    EXPECT_EQ("right_click", stub.calls[0].method);
}

TEST_F(RoundtripTest, DragDown) {
    // Kotlin: MouseDragState(LEFT, true) → [0x03]
    char bytes[] = {0x03};
    stub.execute(bytes, sizeof(bytes));
    ASSERT_EQ(1u, stub.calls.size());
    EXPECT_EQ("down", stub.calls[0].method);
}

TEST_F(RoundtripTest, DragUp) {
    // Kotlin: MouseDragState(LEFT, false) → [0x04]
    char bytes[] = {0x04};
    stub.execute(bytes, sizeof(bytes));
    ASSERT_EQ(1u, stub.calls.size());
    EXPECT_EQ("up", stub.calls[0].method);
}

TEST_F(RoundtripTest, VerticalScroll) {
    // Kotlin: MouseScroll(0, 50) → [0x06, 50]
    char bytes[] = {0x06, 50};
    stub.execute(bytes, sizeof(bytes));
    ASSERT_EQ(1u, stub.calls.size());
    EXPECT_EQ("scroll", stub.calls[0].method);
    EXPECT_EQ(50, stub.calls[0].arg1);
}

TEST_F(RoundtripTest, HorizontalScroll) {
    // Kotlin: MouseScroll(30, 0) → [0x08, 30]
    char bytes[] = {0x08, 30};
    stub.execute(bytes, sizeof(bytes));
    ASSERT_EQ(1u, stub.calls.size());
    EXPECT_EQ("scroll_h", stub.calls[0].method);
    EXPECT_EQ(30, stub.calls[0].arg1);
}

TEST_F(RoundtripTest, KeyPress) {
    // Kotlin: KeyPress([Key(MOD_NONE, KEY_A)]) → [0x07, 0x04, 0x00]
    char bytes[] = {0x07, 0x04, 0x00};
    stub.execute(bytes, sizeof(bytes));
    ASSERT_EQ(1u, stub.calls.size());
    EXPECT_EQ("send_key", stub.calls[0].method);
    EXPECT_EQ(0x04, stub.calls[0].arg1);
    EXPECT_EQ(0x00, stub.calls[0].arg2);
}

TEST_F(RoundtripTest, ZoomIn) {
    // Kotlin: Zoom(5) → [0x09, 5]
    char bytes[] = {0x09, 5};
    stub.execute(bytes, sizeof(bytes));
    ASSERT_EQ(1u, stub.calls.size());
    EXPECT_EQ("zoom", stub.calls[0].method);
    EXPECT_EQ(5, stub.calls[0].arg1);
}

TEST_F(RoundtripTest, MediaPlayPause) {
    // Kotlin: MediaEvent(PLAY_PAUSE) → [0x0A, 0x10]
    char bytes[] = {0x0A, 0x10};
    stub.execute(bytes, sizeof(bytes));
    ASSERT_EQ(1u, stub.calls.size());
    EXPECT_EQ("media", stub.calls[0].method);
    EXPECT_EQ(0x10, stub.calls[0].arg1);
}


// ============================================================================
// Sensitivity Calculation Tests
// ============================================================================

/**
 * Tests for the sensitivity calculation used in Linux input manager:
 *   move: (int)(MOVE_SENSITIVITY / 10.0 * dx)
 *   scroll: (int)((scroll_amount * (SCROLL_SENSITIVITY / 10.0)))
 *
 * These verify the mathematical behavior that affects cursor movement speed.
 */
TEST(SensitivityCalculation, DefaultSensitivityIs1) {
    // Default sensitivity values from linuxinputmanager.cpp
    EXPECT_EQ(1, InputManager::MOVE_SENSITIVITY);
    EXPECT_EQ(1, InputManager::SCROLL_SENSITIVITY);
}

TEST(SensitivityCalculation, Sensitivity1_producesZeroForSmallValues) {
    // With sensitivity=1: (int)(1 / 10.0 * dx) for small dx
    // 1/10.0 = 0.1, so 0.1 * 1 = 0.1, (int)0.1 = 0
    int sensitivity = 1;
    int dx = 1;
    int result = (int)(sensitivity / 10.0 * dx);
    EXPECT_EQ(0, result);
}

TEST(SensitivityCalculation, Sensitivity10_producesExactValue) {
    // With sensitivity=10: (int)(10 / 10.0 * dx) = (int)(1.0 * dx) = dx
    int sensitivity = 10;
    int dx = 5;
    int result = (int)(sensitivity / 10.0 * dx);
    EXPECT_EQ(5, result);
}

TEST(SensitivityCalculation, Sensitivity20_producesDoubledValue) {
    // With sensitivity=20: (int)(20 / 10.0 * dx) = (int)(2.0 * dx)
    int sensitivity = 20;
    int dx = 5;
    int result = (int)(sensitivity / 10.0 * dx);
    EXPECT_EQ(10, result);
}

TEST(SensitivityCalculation, ScrollSensitivity_defaultIs1) {
    // scroll_amount * (1 / 10.0) = scroll_amount * 0.1
    int sensitivity = 1;
    int scroll_amount = 5;
    int result = (int)(scroll_amount * (sensitivity / 10.0));
    EXPECT_EQ(0, result); // 5 * 0.1 = 0.5, truncated to 0
}

TEST(SensitivityCalculation, ScrollSensitivity_10) {
    int sensitivity = 10;
    int scroll_amount = 5;
    int result = (int)(scroll_amount * (sensitivity / 10.0));
    EXPECT_EQ(5, result); // 5 * 1.0 = 5
}


// ============================================================================
// Edge Case Tests
// ============================================================================

TEST_F(ExecuteDispatchTest, MOVE_withExtraBytes_ignoresThem) {
    // Extra bytes beyond the expected 3 should be ignored
    char bytes[] = {InputManager::MOVE, 10, 20, 99, 88};
    stub.execute(bytes, 3); // Only pass 3 bytes as size
    ASSERT_EQ(1u, stub.calls.size());
    EXPECT_EQ("move", stub.calls[0].method);
}

TEST_F(RoundtripTest, SequenceOfEvents) {
    // Simulate a sequence of events as would happen in real usage
    char move1[] = {0x05, 1, 2};
    char move2[] = {0x05, 3, 4};
    char click[] = {0x01};
    char move3[] = {0x05, 5, 6};

    stub.execute(move1, sizeof(move1));
    stub.execute(move2, sizeof(move2));
    stub.execute(click, sizeof(click));
    stub.execute(move3, sizeof(move3));

    ASSERT_EQ(4u, stub.calls.size());
    EXPECT_EQ("move", stub.calls[0].method);
    EXPECT_EQ("move", stub.calls[1].method);
    EXPECT_EQ("click", stub.calls[2].method);
    EXPECT_EQ("move", stub.calls[3].method);
}


int main(int argc, char **argv) {
    ::testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}
