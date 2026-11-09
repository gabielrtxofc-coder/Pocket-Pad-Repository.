package com.darusc.mousedroid.networking.bluetooth

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the HID Report data classes.
 * These tests validate the byte encoding of each report type
 * used in the Bluetooth HID Device profile.
 */
class ReportsTest {

    // ==================== MouseReport ====================

    @Test
    fun mouseReport_hasCorrectId() {
        val report = MouseReport(0, 0, 0, 0)
        assertEquals(REPORT_ID_MOUSE, report.id)
    }

    @Test
    fun mouseReport_hasCorrectSize() {
        val report = MouseReport(0, 0, 0, 0)
        assertEquals(4, report.bytes.size)
    }

    @Test
    fun mouseReport_byteLayout() {
        val report = MouseReport(1, 10, 20, 5)
        assertEquals(1.toByte(), report.bytes[0])  // button
        assertEquals(10.toByte(), report.bytes[1]) // dx
        assertEquals(20.toByte(), report.bytes[2]) // dy
        assertEquals(5.toByte(), report.bytes[3])  // dw (wheel)
    }

    @Test
    fun mouseReport_negativeValues() {
        val report = MouseReport(0, (-10).toByte(), (-20).toByte(), (-3).toByte())
        assertEquals((-10).toByte(), report.bytes[1])
        assertEquals((-20).toByte(), report.bytes[2])
        assertEquals((-3).toByte(), report.bytes[3])
    }

    @Test
    fun mouseReport_leftButton() {
        val report = MouseReport(1, 0, 0, 0)
        assertEquals(1.toByte(), report.bytes[0])
    }

    @Test
    fun mouseReport_rightButton() {
        val report = MouseReport(2, 0, 0, 0)
        assertEquals(2.toByte(), report.bytes[0])
    }

    @Test
    fun mouseReport_middleButton() {
        val report = MouseReport(4, 0, 0, 0)
        assertEquals(4.toByte(), report.bytes[0])
    }

    @Test
    fun mouseReport_noButtons() {
        val report = MouseReport(0, 0, 0, 0)
        assertEquals(0.toByte(), report.bytes[0])
    }

    @Test
    fun mouseReport_boundaryMax() {
        val report = MouseReport(7, 127, 127, 127)
        assertEquals(127.toByte(), report.bytes[1])
        assertEquals(127.toByte(), report.bytes[2])
        assertEquals(127.toByte(), report.bytes[3])
    }

    @Test
    fun mouseReport_boundaryMin() {
        val report = MouseReport(0, (-128).toByte(), (-128).toByte(), (-128).toByte())
        assertEquals((-128).toByte(), report.bytes[1])
        assertEquals((-128).toByte(), report.bytes[2])
        assertEquals((-128).toByte(), report.bytes[3])
    }

    // ==================== KeyboardReport ====================

    @Test
    fun keyboardReport_hasCorrectId() {
        val report = KeyboardReport(0, 0)
        assertEquals(REPORT_ID_KEYBOARD, report.id)
    }

    @Test
    fun keyboardReport_hasCorrectSize() {
        // Modifier (1) + padding (1) + 6 keys = 8 bytes
        val report = KeyboardReport(0, 0)
        assertEquals(8, report.bytes.size)
    }

    @Test
    fun keyboardReport_byteLayout() {
        val report = KeyboardReport(0x01, 0x04) // Left Ctrl + A
        assertEquals(0x01.toByte(), report.bytes[0]) // modifier
        assertEquals(0x00.toByte(), report.bytes[1]) // padding
        assertEquals(0x04.toByte(), report.bytes[2]) // first key
    }

    @Test
    fun keyboardReport_multipleKeys() {
        val report = KeyboardReport(0, 0x04, 0x05, 0x06) // A, B, C
        assertEquals(0x04.toByte(), report.bytes[2])
        assertEquals(0x05.toByte(), report.bytes[3])
        assertEquals(0x06.toByte(), report.bytes[4])
    }

    @Test
    fun keyboardReport_moreThan6Keys_truncated() {
        // KeyboardReport only holds 6 keys
        val report = KeyboardReport(0, 1, 2, 3, 4, 5, 6, 7)
        assertEquals(8, report.bytes.size) // Still 8 bytes total
        assertEquals(1.toByte(), report.bytes[2])
        assertEquals(6.toByte(), report.bytes[7]) // 7th key truncated
    }

    @Test
    fun keyboardReport_noModifier() {
        val report = KeyboardReport(0, 0x04)
        assertEquals(0.toByte(), report.bytes[0])
    }

    @Test
    fun keyboardReport_allModifiers() {
        val allMods = (0x01 or 0x02 or 0x04 or 0x08 or 0x10 or 0x20 or 0x40 or 0x80).toByte()
        val report = KeyboardReport(allMods, 0)
        assertEquals(allMods, report.bytes[0])
    }

    @Test
    fun keyboardReport_paddingByteIsZero() {
        val report = KeyboardReport(0xFF.toByte(), 0x04)
        assertEquals(0.toByte(), report.bytes[1]) // Padding always 0
    }

    @Test
    fun keyboardReport_emptyReport() {
        val report = KeyboardReport(0, 0)
        for (i in 2..7) {
            assertEquals(0.toByte(), report.bytes[i])
        }
    }

    // ==================== MediaReport ====================

    @Test
    fun mediaReport_hasCorrectId() {
        val report = MediaReport(0)
        assertEquals(REPORT_ID_MEDIA, report.id)
    }

    @Test
    fun mediaReport_hasCorrectSize() {
        val report = MediaReport(0)
        assertEquals(1, report.bytes.size)
    }

    @Test
    fun mediaReport_bitmask() {
        val report = MediaReport(0x05) // FORWARD + NEXT
        assertEquals(0x05.toByte(), report.bytes[0])
    }

    @Test
    fun mediaReport_zeroBitmask() {
        val report = MediaReport(0)
        assertEquals(0.toByte(), report.bytes[0])
    }

    @Test
    fun mediaReport_allBits() {
        // 8 bits for 8 media actions
        val report = MediaReport(0xFF)
        assertEquals((-1).toByte(), report.bytes[0]) // 0xFF as signed byte = -1
    }

    // ==================== BatteryReport ====================

    @Test
    fun batteryReport_hasCorrectId() {
        val report = BatteryReport(50)
        assertEquals(REPORT_ID_BATTERY, report.id)
    }

    @Test
    fun batteryReport_hasCorrectSize() {
        val report = BatteryReport(50)
        assertEquals(1, report.bytes.size)
    }

    @Test
    fun batteryReport_percentage() {
        val report = BatteryReport(75)
        assertEquals(75.toByte(), report.bytes[0])
    }

    @Test
    fun batteryReport_zero() {
        val report = BatteryReport(0)
        assertEquals(0.toByte(), report.bytes[0])
    }

    @Test
    fun batteryReport_max() {
        val report = BatteryReport(100)
        assertEquals(100.toByte(), report.bytes[0])
    }

    // ==================== HIDReport base class ====================

    @Test
    fun hidReport_preservesId() {
        val report = MouseReport(0, 0, 0, 0)
        assertEquals(REPORT_ID_MOUSE, report.id)
    }

    @Test
    fun hidReport_preservesBytes() {
        val report = MouseReport(1, 2, 3, 4)
        assertEquals(4, report.bytes.size)
    }

    // ==================== Report ID constants ====================

    @Test
    fun reportId_keyboard_is1() {
        assertEquals(1, REPORT_ID_KEYBOARD)
    }

    @Test
    fun reportId_mouse_is2() {
        assertEquals(2, REPORT_ID_MOUSE)
    }

    @Test
    fun reportId_media_is3() {
        assertEquals(3, REPORT_ID_MEDIA)
    }

    @Test
    fun reportId_battery_is4() {
        assertEquals(4, REPORT_ID_BATTERY)
    }

    // ==================== HID Descriptor ====================

    @Test
    fun hidDescriptor_isNotEmpty() {
        assertTrue(HID_REPORT_DESC.isNotEmpty())
    }

    @Test
    fun hidDescriptor_containsKeyboardReportId() {
        // The descriptor should contain the keyboard report ID byte
        val reportIdBytes = HID_REPORT_DESC.filter { it == REPORT_ID_KEYBOARD.toByte() }
        assertTrue("Descriptor should contain keyboard report ID", reportIdBytes.isNotEmpty())
    }

    @Test
    fun hidDescriptor_containsMouseReportId() {
        val reportIdBytes = HID_REPORT_DESC.filter { it == REPORT_ID_MOUSE.toByte() }
        assertTrue("Descriptor should contain mouse report ID", reportIdBytes.isNotEmpty())
    }

    @Test
    fun hidDescriptor_containsMediaReportId() {
        val reportIdBytes = HID_REPORT_DESC.filter { it == REPORT_ID_MEDIA.toByte() }
        assertTrue("Descriptor should contain media report ID", reportIdBytes.isNotEmpty())
    }
}
