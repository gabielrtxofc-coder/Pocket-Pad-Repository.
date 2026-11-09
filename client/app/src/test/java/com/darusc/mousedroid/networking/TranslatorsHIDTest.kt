package com.darusc.mousedroid.networking

import com.darusc.mousedroid.layouts.Keycode
import com.darusc.mousedroid.layouts.KeyboardLayout
import com.darusc.mousedroid.mkinput.InputEvent
import com.darusc.mousedroid.networking.bluetooth.REPORT_ID_KEYBOARD
import com.darusc.mousedroid.networking.bluetooth.REPORT_ID_MEDIA
import com.darusc.mousedroid.networking.bluetooth.REPORT_ID_MOUSE
import com.darusc.mousedroid.networking.bluetooth.REPORT_ID_BATTERY
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Translators.kt — toHIDReport() extension function.
 *
 * These tests validate the Bluetooth HID report encoding for each InputEvent type.
 * The HID reports are sent to the host PC via Bluetooth HID Device profile.
 *
 * Report IDs:
 *   0x01 = Keyboard, 0x02 = Mouse, 0x03 = Media, 0x04 = Battery
 */
class TranslatorsHIDTest {

    // ==================== MouseMove ====================

    @Test
    fun mouseMove_producesSingleMouseReport() {
        val event = InputEvent.MouseMove(10, 20)
        val reports = event.toHIDReport()
        assertEquals(1, reports.size)
        assertEquals(REPORT_ID_MOUSE, reports[0].id)
    }

    @Test
    fun mouseMove_invertsDxDy() {
        // HID report inverts dx and dy (consistent with server-side negation)
        val event = InputEvent.MouseMove(10, 20)
        val reports = event.toHIDReport()
        val bytes = reports[0].bytes
        assertEquals((-10).toByte(), bytes[1]) // dx inverted
        assertEquals((-20).toByte(), bytes[2]) // dy inverted
    }

    @Test
    fun mouseMove_noButton_defaultIsZero() {
        val event = InputEvent.MouseMove(5, 5)
        val reports = event.toHIDReport()
        assertEquals(0.toByte(), reports[0].bytes[0]) // No button
    }

    @Test
    fun mouseMove_withLeftButton() {
        val event = InputEvent.MouseMove(5, 5, InputEvent.MouseButton.LEFT)
        val reports = event.toHIDReport()
        assertEquals(1.toByte(), reports[0].bytes[0]) // LEFT = 1
    }

    @Test
    fun mouseMove_withRightButton() {
        val event = InputEvent.MouseMove(5, 5, InputEvent.MouseButton.RIGHT)
        val reports = event.toHIDReport()
        assertEquals(2.toByte(), reports[0].bytes[0]) // RIGHT = 2
    }

    @Test
    fun mouseMove_withMiddleButton() {
        val event = InputEvent.MouseMove(5, 5, InputEvent.MouseButton.MIDDLE)
        val reports = event.toHIDReport()
        assertEquals(4.toByte(), reports[0].bytes[0]) // MIDDLE = 4
    }

    @Test
    fun mouseMove_wheelIsZero() {
        val event = InputEvent.MouseMove(10, 20)
        val reports = event.toHIDReport()
        assertEquals(0.toByte(), reports[0].bytes[3]) // dw = 0
    }

    @Test
    fun mouseMove_reportIs4Bytes() {
        val event = InputEvent.MouseMove(0, 0)
        val reports = event.toHIDReport()
        assertEquals(4, reports[0].bytes.size)
    }

    // ==================== MouseClick ====================

    @Test
    fun mouseClick_producesTwoReports() {
        // Click generates press + release
        val event = InputEvent.MouseClick(InputEvent.MouseButton.LEFT)
        val reports = event.toHIDReport()
        assertEquals(2, reports.size)
    }

    @Test
    fun mouseClick_firstReportHasButton() {
        val event = InputEvent.MouseClick(InputEvent.MouseButton.LEFT)
        val reports = event.toHIDReport()
        assertEquals(1.toByte(), reports[0].bytes[0]) // LEFT pressed
    }

    @Test
    fun mouseClick_secondReportReleasesButton() {
        val event = InputEvent.MouseClick(InputEvent.MouseButton.LEFT)
        val reports = event.toHIDReport()
        assertEquals(0.toByte(), reports[1].bytes[0]) // All released
    }

    @Test
    fun mouseClick_leftButton_code1() {
        val event = InputEvent.MouseClick(InputEvent.MouseButton.LEFT)
        val reports = event.toHIDReport()
        assertEquals(1.toByte(), reports[0].bytes[0])
    }

    @Test
    fun mouseClick_rightButton_code2() {
        val event = InputEvent.MouseClick(InputEvent.MouseButton.RIGHT)
        val reports = event.toHIDReport()
        assertEquals(2.toByte(), reports[0].bytes[0])
    }

    @Test
    fun mouseClick_middleButton_code4() {
        val event = InputEvent.MouseClick(InputEvent.MouseButton.MIDDLE)
        val reports = event.toHIDReport()
        assertEquals(4.toByte(), reports[0].bytes[0])
    }

    @Test
    fun mouseClick_bothReportsAreMouseReports() {
        val event = InputEvent.MouseClick(InputEvent.MouseButton.LEFT)
        val reports = event.toHIDReport()
        assertEquals(REPORT_ID_MOUSE, reports[0].id)
        assertEquals(REPORT_ID_MOUSE, reports[1].id)
    }

    @Test
    fun mouseClick_zeroMovement() {
        val event = InputEvent.MouseClick(InputEvent.MouseButton.LEFT)
        val reports = event.toHIDReport()
        assertEquals(0.toByte(), reports[0].bytes[1]) // dx = 0
        assertEquals(0.toByte(), reports[0].bytes[2]) // dy = 0
        assertEquals(0.toByte(), reports[0].bytes[3]) // dw = 0
    }

    // ==================== MouseDragState ====================

    @Test
    fun mouseDragState_down_hasButtonCode() {
        val event = InputEvent.MouseDragState(InputEvent.MouseButton.LEFT, true)
        val reports = event.toHIDReport()
        assertEquals(1, reports.size)
        assertEquals(1.toByte(), reports[0].bytes[0]) // LEFT = 1
    }

    @Test
    fun mouseDragState_up_noButton() {
        val event = InputEvent.MouseDragState(InputEvent.MouseButton.LEFT, false)
        val reports = event.toHIDReport()
        assertEquals(0.toByte(), reports[0].bytes[0]) // Released
    }

    @Test
    fun mouseDragState_rightDown() {
        val event = InputEvent.MouseDragState(InputEvent.MouseButton.RIGHT, true)
        val reports = event.toHIDReport()
        assertEquals(2.toByte(), reports[0].bytes[0]) // RIGHT = 2
    }

    @Test
    fun mouseDragState_middleDown() {
        val event = InputEvent.MouseDragState(InputEvent.MouseButton.MIDDLE, true)
        val reports = event.toHIDReport()
        assertEquals(4.toByte(), reports[0].bytes[0]) // MIDDLE = 4
    }

    // ==================== MouseScroll ====================

    @Test
    fun mouseScroll_vertical_producesMouseReport() {
        val event = InputEvent.MouseScroll(0, 100)
        val reports = event.toHIDReport()
        assertTrue(reports.isNotEmpty())
        assertEquals(REPORT_ID_MOUSE, reports[0].id)
    }

    @Test
    fun mouseScroll_vertical_dividedBy10() {
        // Scroll value is dy/10
        val event = InputEvent.MouseScroll(0, 100)
        val reports = event.toHIDReport()
        // Wheel is in bytes[3]
        assertEquals(10.toByte(), reports[0].bytes[3])
    }

    @Test
    fun mouseScroll_smallValue_belowThreshold() {
        // dy < 10 produces 0 after integer division, so no report
        val event = InputEvent.MouseScroll(0, 5)
        val reports = event.toHIDReport()
        assertTrue(reports.isEmpty()) // 5/10 = 0, so no scroll report
    }

    @Test
    fun mouseScroll_horizontal_usesShiftTrick() {
        // Horizontal scroll uses Shift + wheel
        val event = InputEvent.MouseScroll(100, 0)
        val reports = event.toHIDReport()
        // Should produce 3 reports: Shift press, MouseReport, Shift release
        assertEquals(3, reports.size)
        assertEquals(REPORT_ID_KEYBOARD, reports[0].id) // Shift press
        assertEquals(REPORT_ID_MOUSE, reports[1].id)    // Scroll
        assertEquals(REPORT_ID_KEYBOARD, reports[2].id) // Shift release
    }

    @Test
    fun mouseScroll_horizontal_shiftModifier() {
        val event = InputEvent.MouseScroll(100, 0)
        val reports = event.toHIDReport()
        val shiftReport = reports[0].bytes
        assertEquals(Keycode.MOD_LEFT_SHIFT, shiftReport[0]) // Shift modifier
    }

    @Test
    fun mouseScroll_horizontal_shiftRelease() {
        val event = InputEvent.MouseScroll(100, 0)
        val reports = event.toHIDReport()
        val releaseReport = reports[2].bytes
        assertEquals(0.toByte(), releaseReport[0]) // No modifier = release
    }

    @Test
    fun mouseScroll_negativeVertical() {
        val event = InputEvent.MouseScroll(0, -50)
        val reports = event.toHIDReport()
        assertTrue(reports.isNotEmpty())
        assertEquals((-5).toByte(), reports[0].bytes[3]) // -50/10 = -5
    }

    @Test
    fun mouseScroll_clampedTo127() {
        // Values beyond byte range should be clamped
        val event = InputEvent.MouseScroll(0, 2000)
        val reports = event.toHIDReport()
        assertTrue(reports.isNotEmpty())
        // 2000/10 = 200, clamped to 127
        assertEquals(127.toByte(), reports[0].bytes[3])
    }

    @Test
    fun mouseScroll_clampedToNegative127() {
        val event = InputEvent.MouseScroll(0, -2000)
        val reports = event.toHIDReport()
        assertTrue(reports.isNotEmpty())
        // -2000/10 = -200, clamped to -127
        assertEquals((-127).toByte(), reports[0].bytes[3])
    }

    // ==================== KeyPress ====================

    @Test
    fun keyPress_singleKey_producesPressAndRelease() {
        val keys = listOf(KeyboardLayout.Key(Keycode.MOD_NONE, Keycode.KEY_A))
        val event = InputEvent.KeyPress(keys)
        val reports = event.toHIDReport()
        assertEquals(2, reports.size) // Press + release
    }

    @Test
    fun keyPress_firstReportHasKey() {
        val keys = listOf(KeyboardLayout.Key(Keycode.MOD_NONE, Keycode.KEY_A))
        val event = InputEvent.KeyPress(keys)
        val reports = event.toHIDReport()
        assertEquals(REPORT_ID_KEYBOARD, reports[0].id)
        assertEquals(Keycode.KEY_A, reports[0].bytes[2]) // Key at position 2
    }

    @Test
    fun keyPress_secondReportIsRelease() {
        val keys = listOf(KeyboardLayout.Key(Keycode.MOD_NONE, Keycode.KEY_A))
        val event = InputEvent.KeyPress(keys)
        val reports = event.toHIDReport()
        assertEquals(REPORT_ID_KEYBOARD, reports[1].id)
        assertEquals(0.toByte(), reports[1].bytes[0]) // No modifier
        assertEquals(0.toByte(), reports[1].bytes[2]) // No key
    }

    @Test
    fun keyPress_withModifier() {
        val keys = listOf(KeyboardLayout.Key(Keycode.MOD_LEFT_CTRL, Keycode.KEY_A))
        val event = InputEvent.KeyPress(keys)
        val reports = event.toHIDReport()
        assertEquals(Keycode.MOD_LEFT_CTRL, reports[0].bytes[0]) // Ctrl modifier
    }

    @Test
    fun keyPress_multipleKeys_producesPairs() {
        val keys = listOf(
            KeyboardLayout.Key(Keycode.MOD_NONE, Keycode.KEY_A),
            KeyboardLayout.Key(Keycode.MOD_NONE, Keycode.KEY_B),
            KeyboardLayout.Key(Keycode.MOD_NONE, Keycode.KEY_C)
        )
        val event = InputEvent.KeyPress(keys)
        val reports = event.toHIDReport()
        assertEquals(6, reports.size) // 3 keys * 2 (press + release)
    }

    @Test
    fun keyPress_emptyList_producesEmpty() {
        val event = InputEvent.KeyPress(emptyList())
        val reports = event.toHIDReport()
        assertEquals(0, reports.size)
    }

    // ==================== NumpadKeyPress ====================

    @Test
    fun numpadKeyPress_producesPressAndRelease() {
        val event = InputEvent.NumpadKeyPress(0x59.toByte())
        val reports = event.toHIDReport()
        assertEquals(2, reports.size)
    }

    @Test
    fun numpadKeyPress_firstReportHasKey() {
        val event = InputEvent.NumpadKeyPress(0x59.toByte())
        val reports = event.toHIDReport()
        assertEquals(REPORT_ID_KEYBOARD, reports[0].id)
        assertEquals(0x59.toByte(), reports[0].bytes[2]) // Key at position 2
    }

    @Test
    fun numpadKeyPress_noModifier() {
        val event = InputEvent.NumpadKeyPress(0x59.toByte())
        val reports = event.toHIDReport()
        assertEquals(0x00.toByte(), reports[0].bytes[0]) // No modifier
    }

    @Test
    fun numpadKeyPress_secondReportIsRelease() {
        val event = InputEvent.NumpadKeyPress(0x59.toByte())
        val reports = event.toHIDReport()
        assertEquals(0.toByte(), reports[1].bytes[0])
        assertEquals(0.toByte(), reports[1].bytes[2])
    }

    // ==================== Zoom ====================

    @Test
    fun zoom_positiveScale_producesCtrlPlusWheel() {
        val event = InputEvent.Zoom(5)
        val reports = event.toHIDReport()
        assertEquals(3, reports.size)
        assertEquals(REPORT_ID_KEYBOARD, reports[0].id) // Ctrl press
        assertEquals(REPORT_ID_MOUSE, reports[1].id)    // Wheel
        assertEquals(REPORT_ID_KEYBOARD, reports[2].id) // Ctrl release
    }

    @Test
    fun zoom_ctrlModifier() {
        val event = InputEvent.Zoom(5)
        val reports = event.toHIDReport()
        assertEquals(Keycode.MOD_LEFT_CTRL, reports[0].bytes[0]) // Ctrl modifier
    }

    @Test
    fun zoom_wheelValue() {
        val event = InputEvent.Zoom(5)
        val reports = event.toHIDReport()
        assertEquals(5.toByte(), reports[1].bytes[3]) // Wheel value
    }

    @Test
    fun zoom_ctrlRelease() {
        val event = InputEvent.Zoom(5)
        val reports = event.toHIDReport()
        assertEquals(0.toByte(), reports[2].bytes[0]) // No modifier = release
    }

    @Test
    fun zoom_zeroScale_producesEmpty() {
        val event = InputEvent.Zoom(0)
        val reports = event.toHIDReport()
        assertEquals(0, reports.size)
    }

    @Test
    fun zoom_clampedTo127() {
        val event = InputEvent.Zoom(200)
        val reports = event.toHIDReport()
        assertTrue(reports.isNotEmpty())
        assertEquals(127.toByte(), reports[1].bytes[3])
    }

    @Test
    fun zoom_clampedToNegative127() {
        val event = InputEvent.Zoom(-200)
        val reports = event.toHIDReport()
        assertTrue(reports.isNotEmpty())
        assertEquals((-127).toByte(), reports[1].bytes[3])
    }

    // ==================== MediaEvent ====================

    @Test
    fun mediaEvent_forward() {
        val event = InputEvent.MediaEvent(InputEvent.MediaAction.FORWARD)
        val reports = event.toHIDReport()
        assertEquals(2, reports.size) // Press + release
        assertEquals(REPORT_ID_MEDIA, reports[0].id)
        assertEquals(0x01.toByte(), reports[0].bytes[0]) // Bit 0
    }

    @Test
    fun mediaEvent_playPause() {
        val event = InputEvent.MediaEvent(InputEvent.MediaAction.PLAY_PAUSE)
        val reports = event.toHIDReport()
        assertEquals(0x10.toByte(), reports[0].bytes[0]) // Bit 4
    }

    @Test
    fun mediaEvent_volumeMute() {
        val event = InputEvent.MediaEvent(InputEvent.MediaAction.VOLUME_MUTE)
        val reports = event.toHIDReport()
        assertEquals(0x20.toByte(), reports[0].bytes[0]) // Bit 5
    }

    @Test
    fun mediaEvent_secondReportIsRelease() {
        val event = InputEvent.MediaEvent(InputEvent.MediaAction.PLAY_PAUSE)
        val reports = event.toHIDReport()
        assertEquals(0.toByte(), reports[1].bytes[0]) // Release
    }

    @Test
    fun mediaEvent_allActions_produceTwoReports() {
        for (action in InputEvent.MediaAction.values()) {
            val event = InputEvent.MediaEvent(action)
            val reports = event.toHIDReport()
            assertEquals("Action $action should produce 2 reports", 2, reports.size)
            assertEquals(REPORT_ID_MEDIA, reports[0].id)
            assertEquals(REPORT_ID_MEDIA, reports[1].id)
        }
    }

    @Test
    fun mediaEvent_allActions_haveUniqueBits() {
        // Each media action should activate a different bit
        val bits = mutableSetOf<Byte>()
        for (action in InputEvent.MediaAction.values()) {
            val event = InputEvent.MediaEvent(action)
            val reports = event.toHIDReport()
            bits.add(reports[0].bytes[0])
        }
        assertEquals("All media actions should have unique bitmasks", 8, bits.size)
    }

    // ==================== BatteryEvent ====================

    @Test
    fun batteryEvent_producesBatteryReport() {
        val event = InputEvent.BatteryEvent(75)
        val reports = event.toHIDReport()
        assertEquals(1, reports.size)
        assertEquals(REPORT_ID_BATTERY, reports[0].id)
    }

    @Test
    fun batteryEvent_percentageInBytes() {
        val event = InputEvent.BatteryEvent(50)
        val reports = event.toHIDReport()
        assertEquals(50.toByte(), reports[0].bytes[0])
    }

    @Test
    fun batteryEvent_zeroPercentage() {
        val event = InputEvent.BatteryEvent(0)
        val reports = event.toHIDReport()
        assertEquals(0.toByte(), reports[0].bytes[0])
    }

    @Test
    fun batteryEvent_fullPercentage() {
        val event = InputEvent.BatteryEvent(100)
        val reports = event.toHIDReport()
        assertEquals(100.toByte(), reports[0].bytes[0])
    }

    // ==================== Report ID consistency ====================

    @Test
    fun reportIds_areCorrect() {
        assertEquals(1, REPORT_ID_KEYBOARD)
        assertEquals(2, REPORT_ID_MOUSE)
        assertEquals(3, REPORT_ID_MEDIA)
        assertEquals(4, REPORT_ID_BATTERY)
    }

    @Test
    fun reportIds_areUnique() {
        val ids = setOf(REPORT_ID_KEYBOARD, REPORT_ID_MOUSE, REPORT_ID_MEDIA, REPORT_ID_BATTERY)
        assertEquals(4, ids.size)
    }
}
