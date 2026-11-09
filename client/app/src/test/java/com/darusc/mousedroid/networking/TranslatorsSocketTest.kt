package com.darusc.mousedroid.networking

import com.darusc.mousedroid.layouts.Keycode
import com.darusc.mousedroid.layouts.KeyboardLayout
import com.darusc.mousedroid.mkinput.InputEvent
import com.darusc.mousedroid.networking.toSocketReport
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Translators.kt — toSocketReport() extension function.
 *
 * These tests validate the binary encoding of InputEvent objects into the
 * socket protocol bytes sent over TCP/UDP to the C++ server.
 *
 * The protocol uses 1-byte opcodes followed by arguments:
 *   0x01 = LCLICK, 0x02 = RCLICK, 0x03 = DOWN, 0x04 = UP,
 *   0x05 = MOVE, 0x06 = SCROLL, 0x07 = KEYPRESS, 0x08 = SCROLL_H,
 *   0x09 = ZOOM, 0x0A = MEDIA
 *
 * Expected results are calculated independently of the implementation,
 * using the protocol specification from docs/audit/PROTOCOLS.md.
 */
class TranslatorsSocketTest {

    // ==================== MouseMove ====================

    @Test
    fun mouseMove_producesOpcode05WithDxDy() {
        val event = InputEvent.MouseMove(10, 20)
        val reports = event.toSocketReport()
        assertEquals(1, reports.size)
        assertEquals(0x05.toByte(), reports[0][0])
        assertEquals(10.toByte(), reports[0][1])
        assertEquals(20.toByte(), reports[0][2])
    }

    @Test
    fun mouseMove_zeroValues() {
        val event = InputEvent.MouseMove(0, 0)
        val reports = event.toSocketReport()
        assertEquals(1, reports.size)
        assertEquals(0x05.toByte(), reports[0][0])
        assertEquals(0.toByte(), reports[0][1])
        assertEquals(0.toByte(), reports[0][2])
    }

    @Test
    fun mouseMove_negativeValues() {
        val event = InputEvent.MouseMove(-5, -10)
        val reports = event.toSocketReport()
        assertEquals(0x05.toByte(), reports[0][0])
        assertEquals((-5).toByte(), reports[0][1])
        assertEquals((-10).toByte(), reports[0][2])
    }

    @Test
    fun mouseMove_boundaryValue127() {
        val event = InputEvent.MouseMove(127, -128)
        val reports = event.toSocketReport()
        assertEquals(0x05.toByte(), reports[0][0])
        assertEquals(127.toByte(), reports[0][1])
        assertEquals((-128).toByte(), reports[0][2])
    }

    @Test
    fun mouseMove_truncationOnOverflow() {
        // Values > 127 will be truncated when cast to Byte
        // This documents existing behavior, not desired behavior
        val event = InputEvent.MouseMove(200, 300)
        val reports = event.toSocketReport()
        assertEquals(0x05.toByte(), reports[0][0])
        // 200 as Byte = 200 - 256 = -56
        assertEquals((200).toByte(), reports[0][1])
        assertEquals((300).toByte(), reports[0][2])
    }

    @Test
    fun mouseMove_ignoresButtonField() {
        // Socket protocol MOVE does not carry button state
        val event = InputEvent.MouseMove(5, 10, InputEvent.MouseButton.LEFT)
        val reports = event.toSocketReport()
        assertEquals(3, reports[0].size)
        assertEquals(0x05.toByte(), reports[0][0])
    }

    @Test
    fun mouseMove_totalSizeIs3Bytes() {
        val event = InputEvent.MouseMove(1, 1)
        val reports = event.toSocketReport()
        assertEquals(1, reports.size)
        assertEquals(3, reports[0].size)
    }

    // ==================== MouseScroll ====================

    @Test
    fun mouseScroll_vertical_producesOpcode06() {
        val event = InputEvent.MouseScroll(0, 50)
        val reports = event.toSocketReport()
        assertEquals(1, reports.size)
        assertEquals(0x06.toByte(), reports[0][0])
        assertEquals(50.toByte(), reports[0][1])
    }

    @Test
    fun mouseScroll_horizontal_producesOpcode08() {
        val event = InputEvent.MouseScroll(30, 0)
        val reports = event.toSocketReport()
        assertEquals(1, reports.size)
        assertEquals(0x08.toByte(), reports[0][0])
        assertEquals(30.toByte(), reports[0][1])
    }

    @Test
    fun mouseScroll_verticalTakesPrecedenceOverHorizontal() {
        // When both dy and dx are non-zero, vertical scroll is sent
        val event = InputEvent.MouseScroll(30, 50)
        val reports = event.toSocketReport()
        assertEquals(1, reports.size)
        assertEquals(0x06.toByte(), reports[0][0]) // SCROLL, not SCROLL_H
        assertEquals(50.toByte(), reports[0][1])
    }

    @Test
    fun mouseScroll_negativeVertical() {
        val event = InputEvent.MouseScroll(0, -25)
        val reports = event.toSocketReport()
        assertEquals(0x06.toByte(), reports[0][0])
        assertEquals((-25).toByte(), reports[0][1])
    }

    @Test
    fun mouseScroll_negativeHorizontal() {
        val event = InputEvent.MouseScroll(-15, 0)
        val reports = event.toSocketReport()
        assertEquals(0x08.toByte(), reports[0][0])
        assertEquals((-15).toByte(), reports[0][1])
    }

    @Test
    fun mouseScroll_totalSizeIs2Bytes() {
        val event = InputEvent.MouseScroll(0, 10)
        val reports = event.toSocketReport()
        assertEquals(1, reports.size)
        assertEquals(2, reports[0].size)
    }

    // ==================== MouseClick ====================

    @Test
    fun mouseClick_left_producesOpcode01() {
        val event = InputEvent.MouseClick(InputEvent.MouseButton.LEFT)
        val reports = event.toSocketReport()
        assertEquals(1, reports.size)
        assertEquals(1, reports[0].size)
        assertEquals(0x01.toByte(), reports[0][0])
    }

    @Test
    fun mouseClick_right_producesOpcode02() {
        val event = InputEvent.MouseClick(InputEvent.MouseButton.RIGHT)
        val reports = event.toSocketReport()
        assertEquals(1, reports.size)
        assertEquals(0x02.toByte(), reports[0][0])
    }

    @Test
    fun mouseClick_middle_producesOpcode02() {
        // MIDDLE is mapped to RCLICK in socket mode (known limitation)
        val event = InputEvent.MouseClick(InputEvent.MouseButton.MIDDLE)
        val reports = event.toSocketReport()
        assertEquals(1, reports.size)
        assertEquals(0x02.toByte(), reports[0][0]) // RCLICK, not a separate opcode
    }

    @Test
    fun mouseClick_none_producesOpcode02() {
        // NONE button also maps to RCLICK since it's not LEFT
        val event = InputEvent.MouseClick(InputEvent.MouseButton.NONE)
        val reports = event.toSocketReport()
        assertEquals(1, reports.size)
        assertEquals(0x02.toByte(), reports[0][0])
    }

    @Test
    fun mouseClick_totalSizeIs1Byte() {
        val event = InputEvent.MouseClick(InputEvent.MouseButton.LEFT)
        val reports = event.toSocketReport()
        assertEquals(1, reports[0].size)
    }

    // ==================== MouseDragState ====================

    @Test
    fun mouseDragState_down_producesOpcode03() {
        val event = InputEvent.MouseDragState(InputEvent.MouseButton.LEFT, true)
        val reports = event.toSocketReport()
        assertEquals(1, reports.size)
        assertEquals(0x03.toByte(), reports[0][0])
    }

    @Test
    fun mouseDragState_up_producesOpcode04() {
        val event = InputEvent.MouseDragState(InputEvent.MouseButton.LEFT, false)
        val reports = event.toSocketReport()
        assertEquals(1, reports.size)
        assertEquals(0x04.toByte(), reports[0][0])
    }

    @Test
    fun mouseDragState_rightButtonDown_producesOpcode03() {
        // Opcode is DOWN regardless of button
        val event = InputEvent.MouseDragState(InputEvent.MouseButton.RIGHT, true)
        val reports = event.toSocketReport()
        assertEquals(0x03.toByte(), reports[0][0])
    }

    @Test
    fun mouseDragState_totalSizeIs1Byte() {
        val event = InputEvent.MouseDragState(InputEvent.MouseButton.LEFT, true)
        val reports = event.toSocketReport()
        assertEquals(1, reports[0].size)
    }

    // ==================== Zoom ====================

    @Test
    fun zoom_positiveScale() {
        val event = InputEvent.Zoom(30)
        val reports = event.toSocketReport()
        assertEquals(1, reports.size)
        assertEquals(0x09.toByte(), reports[0][0])
        assertEquals(30.toByte(), reports[0][1])
    }

    @Test
    fun zoom_negativeScale() {
        val event = InputEvent.Zoom(-20)
        val reports = event.toSocketReport()
        assertEquals(0x09.toByte(), reports[0][0])
        assertEquals((-20).toByte(), reports[0][1])
    }

    @Test
    fun zoom_zeroScale() {
        val event = InputEvent.Zoom(0)
        val reports = event.toSocketReport()
        assertEquals(0x09.toByte(), reports[0][0])
        assertEquals(0.toByte(), reports[0][1])
    }

    @Test
    fun zoom_totalSizeIs2Bytes() {
        val event = InputEvent.Zoom(10)
        val reports = event.toSocketReport()
        assertEquals(2, reports[0].size)
    }

    // ==================== KeyPress ====================

    @Test
    fun keyPress_singleKey_producesCorrectBytes() {
        val keys = listOf(KeyboardLayout.Key(Keycode.MOD_NONE, Keycode.KEY_A))
        val event = InputEvent.KeyPress(keys)
        val reports = event.toSocketReport()
        assertEquals(1, reports.size)
        assertEquals(0x07.toByte(), reports[0][0])
        assertEquals(Keycode.KEY_A, reports[0][1])
        assertEquals(Keycode.MOD_NONE, reports[0][2])
    }

    @Test
    fun keyPress_withModifier_producesCorrectBytes() {
        val keys = listOf(KeyboardLayout.Key(Keycode.MOD_LEFT_CTRL, Keycode.KEY_A))
        val event = InputEvent.KeyPress(keys)
        val reports = event.toSocketReport()
        assertEquals(1, reports.size)
        assertEquals(0x07.toByte(), reports[0][0])
        assertEquals(Keycode.KEY_A, reports[0][1])
        assertEquals(Keycode.MOD_LEFT_CTRL, reports[0][2])
    }

    @Test
    fun keyPress_multipleKeys_producesMultipleReports() {
        val keys = listOf(
            KeyboardLayout.Key(Keycode.MOD_LEFT_SHIFT, Keycode.KEY_A),
            KeyboardLayout.Key(Keycode.MOD_LEFT_SHIFT, Keycode.KEY_B)
        )
        val event = InputEvent.KeyPress(keys)
        val reports = event.toSocketReport()
        assertEquals(2, reports.size)
        // First key
        assertEquals(0x07.toByte(), reports[0][0])
        assertEquals(Keycode.KEY_A, reports[0][1])
        assertEquals(Keycode.MOD_LEFT_SHIFT, reports[0][2])
        // Second key
        assertEquals(0x07.toByte(), reports[1][0])
        assertEquals(Keycode.KEY_B, reports[1][1])
        assertEquals(Keycode.MOD_LEFT_SHIFT, reports[1][2])
    }

    @Test
    fun keyPress_emptyList_producesEmptyArray() {
        val event = InputEvent.KeyPress(emptyList())
        val reports = event.toSocketReport()
        assertEquals(0, reports.size)
    }

    @Test
    fun keyPress_eachReportIs3Bytes() {
        val keys = listOf(
            KeyboardLayout.Key(Keycode.MOD_NONE, Keycode.KEY_A),
            KeyboardLayout.Key(Keycode.MOD_NONE, Keycode.KEY_B)
        )
        val event = InputEvent.KeyPress(keys)
        val reports = event.toSocketReport()
        for (report in reports) {
            assertEquals(3, report.size)
        }
    }

    // ==================== NumpadKeyPress ====================

    @Test
    fun numpadKeyPress_producesCorrectBytes() {
        val event = InputEvent.NumpadKeyPress(0x59.toByte()) // KEYPAD_1
        val reports = event.toSocketReport()
        assertEquals(1, reports.size)
        assertEquals(0x07.toByte(), reports[0][0]) // KEYPRESS opcode
        assertEquals(0x59.toByte(), reports[0][1])
        assertEquals(0x00.toByte(), reports[0][2]) // No modifier
    }

    @Test
    fun numpadKeyPress_zeroKey() {
        val event = InputEvent.NumpadKeyPress(0.toByte())
        val reports = event.toSocketReport()
        assertEquals(0x07.toByte(), reports[0][0])
        assertEquals(0.toByte(), reports[0][1])
        assertEquals(0x00.toByte(), reports[0][2])
    }

    @Test
    fun numpadKeyPress_totalSizeIs3Bytes() {
        val event = InputEvent.NumpadKeyPress(0x62.toByte())
        val reports = event.toSocketReport()
        assertEquals(3, reports[0].size)
    }

    // ==================== MediaEvent ====================

    @Test
    fun mediaEvent_forward() {
        val event = InputEvent.MediaEvent(InputEvent.MediaAction.FORWARD)
        val reports = event.toSocketReport()
        assertEquals(1, reports.size)
        assertEquals(0x0A.toByte(), reports[0][0])
        assertEquals(0x01.toByte(), reports[0][1]) // First bit
    }

    @Test
    fun mediaEvent_replay() {
        val event = InputEvent.MediaEvent(InputEvent.MediaAction.REPLAY)
        val reports = event.toSocketReport()
        assertEquals(0x0A.toByte(), reports[0][0])
        assertEquals(0x02.toByte(), reports[0][1]) // Second bit
    }

    @Test
    fun mediaEvent_next() {
        val event = InputEvent.MediaEvent(InputEvent.MediaAction.NEXT)
        val reports = event.toSocketReport()
        assertEquals(0x0A.toByte(), reports[0][0])
        assertEquals(0x04.toByte(), reports[0][1]) // Third bit
    }

    @Test
    fun mediaEvent_previous() {
        val event = InputEvent.MediaEvent(InputEvent.MediaAction.PREVIOUS)
        val reports = event.toSocketReport()
        assertEquals(0x0A.toByte(), reports[0][0])
        assertEquals(0x08.toByte(), reports[0][1]) // Fourth bit
    }

    @Test
    fun mediaEvent_playPause() {
        val event = InputEvent.MediaEvent(InputEvent.MediaAction.PLAY_PAUSE)
        val reports = event.toSocketReport()
        assertEquals(0x0A.toByte(), reports[0][0])
        assertEquals(0x10.toByte(), reports[0][1]) // Fifth bit
    }

    @Test
    fun mediaEvent_volumeMute() {
        val event = InputEvent.MediaEvent(InputEvent.MediaAction.VOLUME_MUTE)
        val reports = event.toSocketReport()
        assertEquals(0x0A.toByte(), reports[0][0])
        assertEquals(0x20.toByte(), reports[0][1]) // Sixth bit
    }

    @Test
    fun mediaEvent_volumeUp() {
        val event = InputEvent.MediaEvent(InputEvent.MediaAction.VOLUME_UP)
        val reports = event.toSocketReport()
        assertEquals(0x0A.toByte(), reports[0][0])
        assertEquals(0x40.toByte(), reports[0][1]) // Seventh bit
    }

    @Test
    fun mediaEvent_volumeDown() {
        val event = InputEvent.MediaEvent(InputEvent.MediaAction.VOLUME_DOWN)
        val reports = event.toSocketReport()
        assertEquals(0x0A.toByte(), reports[0][0])
        assertEquals(0x80.toByte(), reports[0][1]) // Eighth bit (negative as signed byte)
    }

    @Test
    fun mediaEvent_totalSizeIs2Bytes() {
        val event = InputEvent.MediaEvent(InputEvent.MediaAction.PLAY_PAUSE)
        val reports = event.toSocketReport()
        assertEquals(2, reports[0].size)
    }

    // ==================== BatteryEvent ====================

    @Test
    fun batteryEvent_producesEmptyReport() {
        // BatteryEvent produces an empty socketReport() - no data sent over socket
        val event = InputEvent.BatteryEvent(75)
        val reports = event.toSocketReport()
        assertEquals(0, reports.size)
    }

    // ==================== Opcode consistency ====================

    @Test
    fun allOpcodes_areUnique() {
        // Verify that each event type produces a distinct opcode
        val opcodes = mutableSetOf<Byte>()

        opcodes.add(InputEvent.MouseMove(0, 0).toSocketReport()[0][0])
        opcodes.add(InputEvent.MouseScroll(0, 1).toSocketReport()[0][0])
        opcodes.add(InputEvent.MouseClick(InputEvent.MouseButton.LEFT).toSocketReport()[0][0])
        opcodes.add(InputEvent.MouseDragState(InputEvent.MouseButton.LEFT, true).toSocketReport()[0][0])
        opcodes.add(InputEvent.Zoom(1).toSocketReport()[0][0])
        opcodes.add(InputEvent.NumpadKeyPress(1.toByte()).toSocketReport()[0][0])
        opcodes.add(InputEvent.MediaEvent(InputEvent.MediaAction.FORWARD).toSocketReport()[0][0])

        // All opcodes should be unique (7 distinct opcodes for 7 event types that produce reports)
        assertEquals("All opcodes must be unique", 7, opcodes.size)
    }

    @Test
    fun opcodes_matchProtocolSpecification() {
        // Verify opcode values match the documented protocol
        assertEquals(0x01.toByte(), InputEvent.MouseClick(InputEvent.MouseButton.LEFT).toSocketReport()[0][0])
        assertEquals(0x02.toByte(), InputEvent.MouseClick(InputEvent.MouseButton.RIGHT).toSocketReport()[0][0])
        assertEquals(0x03.toByte(), InputEvent.MouseDragState(InputEvent.MouseButton.LEFT, true).toSocketReport()[0][0])
        assertEquals(0x04.toByte(), InputEvent.MouseDragState(InputEvent.MouseButton.LEFT, false).toSocketReport()[0][0])
        assertEquals(0x05.toByte(), InputEvent.MouseMove(1, 1).toSocketReport()[0][0])
        assertEquals(0x06.toByte(), InputEvent.MouseScroll(0, 1).toSocketReport()[0][0])
        assertEquals(0x07.toByte(), InputEvent.NumpadKeyPress(1.toByte()).toSocketReport()[0][0])
        assertEquals(0x08.toByte(), InputEvent.MouseScroll(1, 0).toSocketReport()[0][0])
        assertEquals(0x09.toByte(), InputEvent.Zoom(1).toSocketReport()[0][0])
        assertEquals(0x0A.toByte(), InputEvent.MediaEvent(InputEvent.MediaAction.FORWARD).toSocketReport()[0][0])
    }
}
