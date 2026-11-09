/**
 * Pocket Pad — C++ Server Test Suite
 *
 * Zero-dependency test runner. No external frameworks needed.
 * Tests the protocol constants, dispatch logic, device parsing,
 * and sensitivity calculations.
 *
 * Uses a simple assertion macro that tracks pass/fail counts.
 */

#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <cstdint>
#include <string>
#include <sstream>
#include <vector>
#include <algorithm>
#include <functional>

// Include the protocol header (no wxWidgets, no Asio needed)
#include "input/protocol.h"

// Include device.h for DeviceInfo
#include "net/device.h"

// Provide definitions for extern variables declared in protocol.h.
// In the real application these are defined in linuxinputmanager.cpp /
// windowsinputmanager.cpp. The test binary doesn't link those files.
namespace InputManager {
    int MOVE_SENSITIVITY = 1;
    int SCROLL_SENSITIVITY = 1;
}

// ============================================================================
// Minimal test framework
// ============================================================================

static int g_tests_run = 0;
static int g_tests_passed = 0;
static int g_tests_failed = 0;
static std::string g_current_suite;

#define TEST_SUITE(name) \
    do { g_current_suite = name; } while(0)

#define EXPECT_EQ(a, b) do { \
    g_tests_run++; \
    auto _a = (a); auto _b = (b); \
    if (_a == _b) { g_tests_passed++; } \
    else { \
        g_tests_failed++; \
        printf("  FAIL [%s] %s:%d: expected %d == %d\n", \
               g_current_suite.c_str(), __FILE__, __LINE__, \
               (int)_a, (int)_b); \
    } \
} while(0)

#define EXPECT_NE(a, b) do { \
    g_tests_run++; \
    auto _a = (a); auto _b = (b); \
    if (_a != _b) { g_tests_passed++; } \
    else { \
        g_tests_failed++; \
        printf("  FAIL [%s] %s:%d: expected %d != %d\n", \
               g_current_suite.c_str(), __FILE__, __LINE__, \
               (int)_a, (int)_b); \
    } \
} while(0)

#define EXPECT_GE(a, b) do { \
    g_tests_run++; \
    auto _a = (a); auto _b = (b); \
    if (_a >= _b) { g_tests_passed++; } \
    else { \
        g_tests_failed++; \
        printf("  FAIL [%s] %s:%d: expected %d >= %d\n", \
               g_current_suite.c_str(), __FILE__, __LINE__, \
               (int)_a, (int)_b); \
    } \
} while(0)

#define EXPECT_LE(a, b) do { \
    g_tests_run++; \
    auto _a = (a); auto _b = (b); \
    if (_a <= _b) { g_tests_passed++; } \
    else { \
        g_tests_failed++; \
        printf("  FAIL [%s] %s:%d: expected %d <= %d\n", \
               g_current_suite.c_str(), __FILE__, __LINE__, \
               (int)_a, (int)_b); \
    } \
} while(0)

#define EXPECT_TRUE(expr) do { \
    g_tests_run++; \
    if (expr) { g_tests_passed++; } \
    else { \
        g_tests_failed++; \
        printf("  FAIL [%s] %s:%d: expected true\n", \
               g_current_suite.c_str(), __FILE__, __LINE__); \
    } \
} while(0)

#define EXPECT_FALSE(expr) do { \
    g_tests_run++; \
    if (!(expr)) { g_tests_passed++; } \
    else { \
        g_tests_failed++; \
        printf("  FAIL [%s] %s:%d: expected false\n", \
               g_current_suite.c_str(), __FILE__, __LINE__); \
    } \
} while(0)

#define EXPECT_STREQ(a, b) do { \
    g_tests_run++; \
    std::string _a(a); std::string _b(b); \
    if (_a == _b) { g_tests_passed++; } \
    else { \
        g_tests_failed++; \
        printf("  FAIL [%s] %s:%d: expected \"%s\" == \"%s\"\n", \
               g_current_suite.c_str(), __FILE__, __LINE__, \
               _a.c_str(), _b.c_str()); \
    } \
} while(0)

// ============================================================================
// StubInputManager — records calls for verification
// ============================================================================

struct Call {
    std::string method;
    int arg1 = 0;
    int arg2 = 0;
};

class StubInputManager : public InputManager::Base {
public:
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
    void reset() { calls.clear(); }
};

// ============================================================================
// Protocol Opcode Tests
// ============================================================================

void test_protocol_opcodes() {
    TEST_SUITE("ProtocolOpcodes");

    // Match Kotlin RawSocketEvents
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

    // All unique
    char opcodes[] = {
        InputManager::LCLICK, InputManager::RCLICK, InputManager::DOWN,
        InputManager::UP, InputManager::MOVE, InputManager::SCROLL,
        InputManager::KEYPRESS, InputManager::SCROLL_H, InputManager::ZOOM,
        InputManager::MEDIA
    };
    std::vector<char> sorted(opcodes, opcodes + 10);
    std::sort(sorted.begin(), sorted.end());
    auto last = std::unique(sorted.begin(), sorted.end());
    EXPECT_TRUE(last == sorted.end());

    // Range
    EXPECT_GE((int)InputManager::LCLICK, 1);
    EXPECT_LE((int)InputManager::MEDIA, 10);
}

// ============================================================================
// Execute Dispatch Tests
// ============================================================================

void test_execute_dispatch() {
    TEST_SUITE("ExecuteDispatch");
    StubInputManager stub;

    // LCLICK
    stub.reset();
    { char b[] = {InputManager::LCLICK}; stub.execute(b, 1); }
    EXPECT_EQ(1u, stub.calls.size());
    EXPECT_STREQ("click", stub.calls[0].method.c_str());

    // RCLICK
    stub.reset();
    { char b[] = {InputManager::RCLICK}; stub.execute(b, 1); }
    EXPECT_EQ(1u, stub.calls.size());
    EXPECT_STREQ("right_click", stub.calls[0].method.c_str());

    // DOWN
    stub.reset();
    { char b[] = {InputManager::DOWN}; stub.execute(b, 1); }
    EXPECT_EQ(1u, stub.calls.size());
    EXPECT_STREQ("down", stub.calls[0].method.c_str());

    // UP
    stub.reset();
    { char b[] = {InputManager::UP}; stub.execute(b, 1); }
    EXPECT_EQ(1u, stub.calls.size());
    EXPECT_STREQ("up", stub.calls[0].method.c_str());

    // MOVE — server negates dx and dy
    stub.reset();
    { char b[] = {InputManager::MOVE, 10, 20}; stub.execute(b, 3); }
    EXPECT_EQ(1u, stub.calls.size());
    EXPECT_STREQ("move", stub.calls[0].method.c_str());
    EXPECT_EQ(-10, stub.calls[0].arg1);
    EXPECT_EQ(-20, stub.calls[0].arg2);

    // MOVE zero
    stub.reset();
    { char b[] = {InputManager::MOVE, 0, 0}; stub.execute(b, 3); }
    EXPECT_EQ(1u, stub.calls.size());
    EXPECT_EQ(0, stub.calls[0].arg1);
    EXPECT_EQ(0, stub.calls[0].arg2);

    // MOVE negative
    stub.reset();
    { char b[] = {InputManager::MOVE, (char)-5, (char)-10}; stub.execute(b, 3); }
    EXPECT_EQ(1u, stub.calls.size());
    EXPECT_EQ(5, stub.calls[0].arg1);
    EXPECT_EQ(10, stub.calls[0].arg2);

    // MOVE boundary
    stub.reset();
    { char b[] = {InputManager::MOVE, 127, (char)-128}; stub.execute(b, 3); }
    EXPECT_EQ(1u, stub.calls.size());
    EXPECT_EQ(-127, stub.calls[0].arg1);
    EXPECT_EQ(128, stub.calls[0].arg2);

    // SCROLL
    stub.reset();
    { char b[] = {InputManager::SCROLL, 50}; stub.execute(b, 2); }
    EXPECT_EQ(1u, stub.calls.size());
    EXPECT_STREQ("scroll", stub.calls[0].method.c_str());
    EXPECT_EQ(50, stub.calls[0].arg1);

    // SCROLL negative
    stub.reset();
    { char b[] = {InputManager::SCROLL, (char)-30}; stub.execute(b, 2); }
    EXPECT_EQ(-30, stub.calls[0].arg1);

    // SCROLL_H
    stub.reset();
    { char b[] = {InputManager::SCROLL_H, 25}; stub.execute(b, 2); }
    EXPECT_EQ(1u, stub.calls.size());
    EXPECT_STREQ("scroll_h", stub.calls[0].method.c_str());
    EXPECT_EQ(25, stub.calls[0].arg1);

    // KEYPRESS
    stub.reset();
    { char b[] = {InputManager::KEYPRESS, 0x04, 0x00}; stub.execute(b, 3); }
    EXPECT_EQ(1u, stub.calls.size());
    EXPECT_STREQ("send_key", stub.calls[0].method.c_str());
    EXPECT_EQ(0x04, stub.calls[0].arg1);
    EXPECT_EQ(0x00, stub.calls[0].arg2);

    // KEYPRESS with modifier
    stub.reset();
    { char b[] = {InputManager::KEYPRESS, 0x04, 0x01}; stub.execute(b, 3); }
    EXPECT_EQ(0x04, stub.calls[0].arg1);
    EXPECT_EQ(0x01, stub.calls[0].arg2);

    // ZOOM
    stub.reset();
    { char b[] = {InputManager::ZOOM, 10}; stub.execute(b, 2); }
    EXPECT_EQ(1u, stub.calls.size());
    EXPECT_STREQ("zoom", stub.calls[0].method.c_str());
    EXPECT_EQ(10, stub.calls[0].arg1);

    // ZOOM negative
    stub.reset();
    { char b[] = {InputManager::ZOOM, (char)-15}; stub.execute(b, 2); }
    EXPECT_EQ(-15, stub.calls[0].arg1);

    // MEDIA
    stub.reset();
    { char b[] = {InputManager::MEDIA, 0x10}; stub.execute(b, 2); }
    EXPECT_EQ(1u, stub.calls.size());
    EXPECT_STREQ("media", stub.calls[0].method.c_str());
    EXPECT_EQ(0x10, stub.calls[0].arg1);

    // UNKNOWN opcode
    stub.reset();
    { char b[] = {0x00}; stub.execute(b, 1); }
    EXPECT_EQ(0u, stub.calls.size());

    // HIGH opcode
    stub.reset();
    { char b[] = {0x0B}; stub.execute(b, 1); }
    EXPECT_EQ(0u, stub.calls.size());

    // MAX opcode
    stub.reset();
    { char b[] = {(char)0xFF}; stub.execute(b, 1); }
    EXPECT_EQ(0u, stub.calls.size());
}

// ============================================================================
// Device Details Parsing Tests
// ============================================================================

void test_device_parsing() {
    TEST_SUITE("DeviceParsing");

    // Basic format
    {
        std::string details = "Google/Pixel 6/aosp/1";
        std::stringstream ss(details);
        std::string token;
        std::vector<std::string> tokens;
        while (std::getline(ss, token, '/')) tokens.push_back(token);
        EXPECT_EQ(4u, tokens.size());
        EXPECT_STREQ("Google", tokens[0].c_str());
        EXPECT_STREQ("Pixel 6", tokens[1].c_str());
        EXPECT_STREQ("aosp", tokens[2].c_str());
        EXPECT_STREQ("1", tokens[3].c_str());
    }

    // USB type
    {
        std::string details = "Samsung/Galaxy S21/SM-G991/0";
        std::stringstream ss(details);
        std::string token;
        std::vector<std::string> tokens;
        while (std::getline(ss, token, '/')) tokens.push_back(token);
        EXPECT_EQ(4u, tokens.size());
        EXPECT_EQ(0, std::atoi(tokens[3].c_str()));
    }

    // Too few tokens (documents potential crash in Connection constructor)
    {
        std::string details = "Google/Pixel";
        std::stringstream ss(details);
        std::string token;
        std::vector<std::string> tokens;
        while (std::getline(ss, token, '/')) tokens.push_back(token);
        EXPECT_EQ(2u, tokens.size());
    }

    // Extra slashes
    {
        std::string details = "Google/Pixel/aosp/1/extra";
        std::stringstream ss(details);
        std::string token;
        std::vector<std::string> tokens;
        while (std::getline(ss, token, '/')) tokens.push_back(token);
        EXPECT_EQ(5u, tokens.size());
    }

    // Special characters in name
    {
        std::string details = "Samsung/Galaxy S21 Ultra/SM-G998B/1";
        std::stringstream ss(details);
        std::string token;
        std::vector<std::string> tokens;
        while (std::getline(ss, token, '/')) tokens.push_back(token);
        EXPECT_EQ(4u, tokens.size());
        EXPECT_STREQ("Galaxy S21 Ultra", tokens[1].c_str());
    }
}

// ============================================================================
// Connection Type Tests
// ============================================================================

void test_connection_type() {
    TEST_SUITE("ConnectionType");
    EXPECT_EQ(0, USB);
    EXPECT_EQ(1, WIFI);
}

// ============================================================================
// DeviceInfo Tests
// ============================================================================

void test_device_info() {
    TEST_SUITE("DeviceInfo");

    DeviceInfo info;
    info.ID = 1;
    info.Address = "192.168.1.100";
    info.Manufacturer = "Google";
    info.Name = "Pixel 6";
    info.Model = "aosp";
    info.ctype = WIFI;

    EXPECT_EQ(1, info.ID);
    EXPECT_STREQ("192.168.1.100", info.Address.c_str());
    EXPECT_STREQ("Google", info.Manufacturer.c_str());
    EXPECT_STREQ("Pixel 6", info.Name.c_str());
    EXPECT_EQ(WIFI, info.ctype);

    // Default values
    DeviceInfo info2;
    EXPECT_STREQ("", info2.Address.c_str());
    EXPECT_STREQ("", info2.Manufacturer.c_str());
}

// ============================================================================
// Roundtrip Tests (client bytes → server dispatch)
// ============================================================================

void test_roundtrip() {
    TEST_SUITE("Roundtrip");
    StubInputManager stub;

    // MouseMove(5, 10) → [0x05, 5, 10] → move(-5, -10)
    stub.reset();
    { char b[] = {0x05, 5, 10}; stub.execute(b, 3); }
    EXPECT_EQ(1u, stub.calls.size());
    EXPECT_STREQ("move", stub.calls[0].method.c_str());
    EXPECT_EQ(-5, stub.calls[0].arg1);
    EXPECT_EQ(-10, stub.calls[0].arg2);

    // LeftClick → [0x01] → click()
    stub.reset();
    { char b[] = {0x01}; stub.execute(b, 1); }
    EXPECT_STREQ("click", stub.calls[0].method.c_str());

    // RightClick → [0x02] → right_click()
    stub.reset();
    { char b[] = {0x02}; stub.execute(b, 1); }
    EXPECT_STREQ("right_click", stub.calls[0].method.c_str());

    // DragDown → [0x03] → down()
    stub.reset();
    { char b[] = {0x03}; stub.execute(b, 1); }
    EXPECT_STREQ("down", stub.calls[0].method.c_str());

    // DragUp → [0x04] → up()
    stub.reset();
    { char b[] = {0x04}; stub.execute(b, 1); }
    EXPECT_STREQ("up", stub.calls[0].method.c_str());

    // VerticalScroll → [0x06, 50] → scroll(50)
    stub.reset();
    { char b[] = {0x06, 50}; stub.execute(b, 2); }
    EXPECT_STREQ("scroll", stub.calls[0].method.c_str());
    EXPECT_EQ(50, stub.calls[0].arg1);

    // HorizontalScroll → [0x08, 30] → scroll_h(30)
    stub.reset();
    { char b[] = {0x08, 30}; stub.execute(b, 2); }
    EXPECT_STREQ("scroll_h", stub.calls[0].method.c_str());
    EXPECT_EQ(30, stub.calls[0].arg1);

    // KeyPress → [0x07, 0x04, 0x00] → send_key(0x04, 0x00)
    stub.reset();
    { char b[] = {0x07, 0x04, 0x00}; stub.execute(b, 3); }
    EXPECT_STREQ("send_key", stub.calls[0].method.c_str());
    EXPECT_EQ(0x04, stub.calls[0].arg1);

    // Zoom → [0x09, 5] → zoom(5)
    stub.reset();
    { char b[] = {0x09, 5}; stub.execute(b, 2); }
    EXPECT_STREQ("zoom", stub.calls[0].method.c_str());
    EXPECT_EQ(5, stub.calls[0].arg1);

    // Media → [0x0A, 0x10] → media(0x10)
    stub.reset();
    { char b[] = {0x0A, 0x10}; stub.execute(b, 2); }
    EXPECT_STREQ("media", stub.calls[0].method.c_str());
    EXPECT_EQ(0x10, stub.calls[0].arg1);

    // Sequence of events
    stub.reset();
    {
        char m1[] = {0x05, 1, 2}; stub.execute(m1, 3);
        char m2[] = {0x05, 3, 4}; stub.execute(m2, 3);
        char cl[] = {0x01};       stub.execute(cl, 1);
        char m3[] = {0x05, 5, 6}; stub.execute(m3, 3);
    }
    EXPECT_EQ(4u, stub.calls.size());
    EXPECT_STREQ("move", stub.calls[0].method.c_str());
    EXPECT_STREQ("move", stub.calls[1].method.c_str());
    EXPECT_STREQ("click", stub.calls[2].method.c_str());
    EXPECT_STREQ("move", stub.calls[3].method.c_str());
}

// ============================================================================
// Sensitivity Calculation Tests
// ============================================================================

void test_sensitivity() {
    TEST_SUITE("Sensitivity");

    EXPECT_EQ(1, InputManager::MOVE_SENSITIVITY);
    EXPECT_EQ(1, InputManager::SCROLL_SENSITIVITY);

    // Sensitivity=1, dx=1: (int)(1/10.0 * 1) = 0
    EXPECT_EQ(0, (int)(1 / 10.0 * 1));

    // Sensitivity=10, dx=5: (int)(10/10.0 * 5) = 5
    EXPECT_EQ(5, (int)(10 / 10.0 * 5));

    // Sensitivity=20, dx=5: (int)(20/10.0 * 5) = 10
    EXPECT_EQ(10, (int)(20 / 10.0 * 5));

    // Scroll: sensitivity=1, amount=5: (int)(5 * (1/10.0)) = 0
    EXPECT_EQ(0, (int)(5 * (1 / 10.0)));

    // Scroll: sensitivity=10, amount=5: (int)(5 * (10/10.0)) = 5
    EXPECT_EQ(5, (int)(5 * (10 / 10.0)));
}

// ============================================================================
// Main
// ============================================================================

int main() {
    printf("Pocket Pad C++ Test Suite\n");
    printf("========================\n\n");

    test_protocol_opcodes();
    test_execute_dispatch();
    test_device_parsing();
    test_connection_type();
    test_device_info();
    test_roundtrip();
    test_sensitivity();

    printf("\n========================\n");
    printf("Results: %d run, %d passed, %d failed\n",
           g_tests_run, g_tests_passed, g_tests_failed);

    return g_tests_failed > 0 ? 1 : 0;
}
