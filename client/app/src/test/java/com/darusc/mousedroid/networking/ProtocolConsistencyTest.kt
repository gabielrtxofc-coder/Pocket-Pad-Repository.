package com.darusc.mousedroid.networking

import com.darusc.mousedroid.mkinput.InputEvent
import com.darusc.mousedroid.networking.bluetooth.REPORT_ID_KEYBOARD
import com.darusc.mousedroid.networking.bluetooth.REPORT_ID_MEDIA
import com.darusc.mousedroid.networking.bluetooth.REPORT_ID_MOUSE
import com.darusc.mousedroid.networking.bluetooth.REPORT_ID_BATTERY
import com.darusc.mousedroid.networking.toSocketReport
import com.darusc.mousedroid.networking.toHIDReport
import org.junit.Assert.*
import org.junit.Test

/**
 * Protocol Consistency Tests
 *
 * These tests document and verify the binary protocol contract between
 * the Kotlin client and the C++ server.
 *
 * The protocol uses 1-byte opcodes (0x01–0x0A) followed by arguments.
 * These tests ensure that:
 *   1. Each event type produces the correct opcode.
 *   2. Argument sizes match expectations.
 *   3. The protocol is self-consistent across all event types.
 *
 * Cross-reference: server/src/input/protocol.h
 */
class ProtocolConsistencyTest {

    /**
     * Verifies that every event type that produces a socket report
     * has a unique opcode value.
     */
    @Test
    fun socketProtocol_eachEventTypeHasUniqueOpcode() {
        val opcodeMap = mutableMapOf<Byte, String>()

        fun checkOpcode(opcode: Byte, name: String) {
            val previous = opcodeMap.put(opcode, name)
            if (previous != null) {
                fail("Opcode 0x${opcode.toString(16)} used by both '$previous' and '$name'")
            }
        }

        checkOpcode(InputEvent.MouseMove(0, 0).toSocketReport()[0][0], "MouseMove")
        checkOpcode(InputEvent.MouseScroll(0, 1).toSocketReport()[0][0], "MouseScroll(V)")
        checkOpcode(InputEvent.MouseScroll(1, 0).toSocketReport()[0][0], "MouseScroll(H)")
        checkOpcode(InputEvent.MouseClick(InputEvent.MouseButton.LEFT).toSocketReport()[0][0], "MouseClick")
        checkOpcode(InputEvent.MouseDragState(InputEvent.MouseButton.LEFT, true).toSocketReport()[0][0], "DragDown")
        checkOpcode(InputEvent.MouseDragState(InputEvent.MouseButton.LEFT, false).toSocketReport()[0][0], "DragUp")
        checkOpcode(InputEvent.Zoom(1).toSocketReport()[0][0], "Zoom")
        checkOpcode(InputEvent.KeyPress(listOf(
            com.darusc.mousedroid.layouts.KeyboardLayout.Key(0.toByte(), 0x04.toByte())
        )).toSocketReport()[0][0], "KeyPress")
        checkOpcode(InputEvent.NumpadKeyPress(1.toByte()).toSocketReport()[0][0], "NumpadKeyPress")
        checkOpcode(InputEvent.MediaEvent(InputEvent.MediaAction.FORWARD).toSocketReport()[0][0], "MediaEvent")
    }

    /**
     * Verifies the complete opcode table matches the server's protocol.h.
     *
     * Kotlin side (RawSocketEvents):    C++ side (protocol.h):
     *   LCLICK    = 0x01                   LCLICK  = 0x01
     *   RCLICK    = 0x02                   RCLICK  = 0x02
     *   DOWN      = 0x03                   DOWN    = 0x03
     *   UP        = 0x04                   UP      = 0x04
     *   MOVE      = 0x05                   MOVE    = 0x05
     *   SCROLL    = 0x06                   SCROLL  = 0x06
     *   KEYPRESS  = 0x07                   KEYPRESS= 0x07
     *   SCROLL_H  = 0x08                   SCROLL_H= 0x08
     *   ZOOM      = 0x09                   ZOOM    = 0x09
     *   MEDIA     = 0x0A                   MEDIA   = 0x0A
     */
    @Test
    fun socketProtocol_opcodeTable_isCompleteAndSequential() {
        val expectedOpcodes = listOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A)

        val actualOpcodes = mutableSetOf<Byte>()
        actualOpcodes.add(InputEvent.MouseClick(InputEvent.MouseButton.LEFT).toSocketReport()[0][0])   // 0x01
        actualOpcodes.add(InputEvent.MouseClick(InputEvent.MouseButton.RIGHT).toSocketReport()[0][0])  // 0x02
        actualOpcodes.add(InputEvent.MouseDragState(InputEvent.MouseButton.LEFT, true).toSocketReport()[0][0])   // 0x03
        actualOpcodes.add(InputEvent.MouseDragState(InputEvent.MouseButton.LEFT, false).toSocketReport()[0][0])  // 0x04
        actualOpcodes.add(InputEvent.MouseMove(0, 0).toSocketReport()[0][0])                          // 0x05
        actualOpcodes.add(InputEvent.MouseScroll(0, 1).toSocketReport()[0][0])                        // 0x06
        actualOpcodes.add(InputEvent.NumpadKeyPress(1.toByte()).toSocketReport()[0][0])                        // 0x07
        actualOpcodes.add(InputEvent.MouseScroll(1, 0).toSocketReport()[0][0])                        // 0x08
        actualOpcodes.add(InputEvent.Zoom(1).toSocketReport()[0][0])                                  // 0x09
        actualOpcodes.add(InputEvent.MediaEvent(InputEvent.MediaAction.FORWARD).toSocketReport()[0][0]) // 0x0A

        for (expected in expectedOpcodes) {
            assertTrue(
                "Opcode 0x${expected.toString(16)} missing from protocol",
                actualOpcodes.contains(expected.toByte())
            )
        }
    }

    /**
     * Verifies that each socket message has a consistent byte size.
     *
     * Protocol message sizes:
     *   LCLICK:   1 byte  [opcode]
     *   RCLICK:   1 byte  [opcode]
     *   DOWN:     1 byte  [opcode]
     *   UP:       1 byte  [opcode]
     *   MOVE:     3 bytes [opcode, dx, dy]
     *   SCROLL:   2 bytes [opcode, amount]
     *   SCROLL_H: 2 bytes [opcode, amount]
     *   KEYPRESS: 3 bytes [opcode, key, modifier]
     *   ZOOM:     2 bytes [opcode, scale]
     *   MEDIA:    2 bytes [opcode, bitmask]
     */
    @Test
    fun socketProtocol_messageSizes_areCorrect() {
        // 1-byte messages
        assertEquals(1, InputEvent.MouseClick(InputEvent.MouseButton.LEFT).toSocketReport()[0].size)
        assertEquals(1, InputEvent.MouseClick(InputEvent.MouseButton.RIGHT).toSocketReport()[0].size)
        assertEquals(1, InputEvent.MouseDragState(InputEvent.MouseButton.LEFT, true).toSocketReport()[0].size)
        assertEquals(1, InputEvent.MouseDragState(InputEvent.MouseButton.LEFT, false).toSocketReport()[0].size)

        // 3-byte messages
        assertEquals(3, InputEvent.MouseMove(0, 0).toSocketReport()[0].size)

        // 2-byte messages
        assertEquals(2, InputEvent.MouseScroll(0, 1).toSocketReport()[0].size)
        assertEquals(2, InputEvent.MouseScroll(1, 0).toSocketReport()[0].size)
        assertEquals(2, InputEvent.Zoom(1).toSocketReport()[0].size)
        assertEquals(2, InputEvent.MediaEvent(InputEvent.MediaAction.FORWARD).toSocketReport()[0].size)
        assertEquals(3, InputEvent.NumpadKeyPress(1.toByte()).toSocketReport()[0].size) // KEYPRESS format
    }

    /**
     * Verifies HID report IDs match the descriptor definition.
     *
     * Report IDs are defined in the HID descriptor and must match:
     *   0x01 = Keyboard
     *   0x02 = Mouse
     *   0x03 = Media/Consumer
     *   0x04 = Battery (disabled in descriptor, but class exists)
     */
    @Test
    fun hidProtocol_reportIds_matchDescriptor() {
        assertEquals(1, REPORT_ID_KEYBOARD)
        assertEquals(2, REPORT_ID_MOUSE)
        assertEquals(3, REPORT_ID_MEDIA)
        assertEquals(4, REPORT_ID_BATTERY)
    }

    /**
     * Verifies that the MouseMove event produces the correct
     * direction inversion for both socket and HID paths.
     *
     * Socket path: dx, dy are sent as-is, server negates them
     * HID path: dx, dy are negated before sending (consistent with server)
     */
    @Test
    fun mouseMove_directionConsistency() {
        // Socket: values sent as-is
        val socketEvent = InputEvent.MouseMove(10, 20)
        val socketReport = socketEvent.toSocketReport()[0]
        assertEquals(10.toByte(), socketReport[1])  // dx sent as-is
        assertEquals(20.toByte(), socketReport[2])  // dy sent as-is

        // HID: values are inverted (server doesn't invert HID reports)
        val hidEvent = InputEvent.MouseMove(10, 20)
        val hidReport = hidEvent.toHIDReport()[0]
        assertEquals((-10).toByte(), hidReport.bytes[1])  // dx inverted
        assertEquals((-20).toByte(), hidReport.bytes[2])  // dy inverted
    }

    /**
     * Verifies that MouseClick(MIDDLE) has different behavior between
     * socket and HID paths (known limitation documented in audit).
     */
    @Test
    fun middleClick_socketVsHid_divergence() {
        // Socket: MIDDLE maps to RCLICK (0x02)
        val socketEvent = InputEvent.MouseClick(InputEvent.MouseButton.MIDDLE)
        val socketReport = socketEvent.toSocketReport()[0]
        assertEquals(0x02.toByte(), socketReport[0]) // Same as RIGHT

        // HID: MIDDLE has its own code (4)
        val hidEvent = InputEvent.MouseClick(InputEvent.MouseButton.MIDDLE)
        val hidReport = hidEvent.toHIDReport()[0]
        assertEquals(4.toByte(), hidReport.bytes[0]) // MIDDLE = 4, distinct from RIGHT = 2
    }

    /**
     * Verifies that BatteryEvent produces empty socket report
     * but a valid HID report (battery is only sent via Bluetooth).
     */
    @Test
    fun batteryEvent_onlyOverBluetooth() {
        val event = InputEvent.BatteryEvent(75)

        // Socket: empty (no socket path for battery)
        val socketReport = event.toSocketReport()
        assertEquals(0, socketReport.size)

        // HID: valid battery report
        val hidReport = event.toHIDReport()
        assertEquals(1, hidReport.size)
        assertEquals(REPORT_ID_BATTERY, hidReport[0].id)
    }

    /**
     * Verifies the zoom event uses CTRL + wheel in HID path
     * and a dedicated opcode in socket path.
     */
    @Test
    fun zoom_protocolConsistency() {
        val event = InputEvent.Zoom(5)

        // Socket: dedicated ZOOM opcode
        val socketReport = event.toSocketReport()
        assertEquals(1, socketReport.size)
        assertEquals(0x09.toByte(), socketReport[0][0])

        // HID: CTRL + MouseReport (wheel)
        val hidReport = event.toHIDReport()
        assertEquals(3, hidReport.size)
        assertEquals(REPORT_ID_KEYBOARD, hidReport[0].id) // CTRL press
        assertEquals(REPORT_ID_MOUSE, hidReport[1].id)     // Wheel
        assertEquals(REPORT_ID_KEYBOARD, hidReport[2].id)  // CTRL release
    }
}
