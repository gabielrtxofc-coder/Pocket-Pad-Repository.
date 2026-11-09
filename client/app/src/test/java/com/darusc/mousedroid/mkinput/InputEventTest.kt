package com.darusc.mousedroid.mkinput

import com.darusc.mousedroid.layouts.Keycode
import com.darusc.mousedroid.layouts.KeyboardLayout
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for InputEvent sealed class hierarchy.
 * These tests validate the data class construction, equality, and property access
 * for all input event types used in the Pocket Pad protocol.
 */
class InputEventTest {

    // ---- MouseMove tests ----

    @Test
    fun mouseMove_defaultButton_isNone() {
        val event = InputEvent.MouseMove(10, 20)
        assertEquals(InputEvent.MouseButton.NONE, event.button)
    }

    @Test
    fun mouseMove_explicitButton_preserved() {
        val event = InputEvent.MouseMove(5, -3, InputEvent.MouseButton.LEFT)
        assertEquals(5, event.dx)
        assertEquals(-3, event.dy)
        assertEquals(InputEvent.MouseButton.LEFT, event.button)
    }

    @Test
    fun mouseMove_zeroValues() {
        val event = InputEvent.MouseMove(0, 0)
        assertEquals(0, event.dx)
        assertEquals(0, event.dy)
    }

    @Test
    fun mouseMove_negativeValues() {
        val event = InputEvent.MouseMove(-128, -127)
        assertEquals(-128, event.dx)
        assertEquals(-127, event.dy)
    }

    @Test
    fun mouseMove_equality() {
        val a = InputEvent.MouseMove(10, 20, InputEvent.MouseButton.LEFT)
        val b = InputEvent.MouseMove(10, 20, InputEvent.MouseButton.LEFT)
        assertEquals(a, b)
    }

    @Test
    fun mouseMove_inequalityOnDx() {
        val a = InputEvent.MouseMove(10, 20)
        val b = InputEvent.MouseMove(11, 20)
        assertNotEquals(a, b)
    }

    @Test
    fun mouseMove_largeValues() {
        val event = InputEvent.MouseMove(Int.MAX_VALUE, Int.MIN_VALUE)
        assertEquals(Int.MAX_VALUE, event.dx)
        assertEquals(Int.MIN_VALUE, event.dy)
    }

    // ---- MouseScroll tests ----

    @Test
    fun mouseScroll_positiveValues() {
        val event = InputEvent.MouseScroll(5, 10)
        assertEquals(5, event.dx)
        assertEquals(10, event.dy)
    }

    @Test
    fun mouseScroll_zeroValues() {
        val event = InputEvent.MouseScroll(0, 0)
        assertEquals(0, event.dx)
        assertEquals(0, event.dy)
    }

    @Test
    fun mouseScroll_negativeValues() {
        val event = InputEvent.MouseScroll(-50, -100)
        assertEquals(-50, event.dx)
        assertEquals(-100, event.dy)
    }

    @Test
    fun mouseScroll_equality() {
        val a = InputEvent.MouseScroll(3, 7)
        val b = InputEvent.MouseScroll(3, 7)
        assertEquals(a, b)
    }

    // ---- MouseClick tests ----

    @Test
    fun mouseClick_leftButton() {
        val event = InputEvent.MouseClick(InputEvent.MouseButton.LEFT)
        assertEquals(InputEvent.MouseButton.LEFT, event.button)
    }

    @Test
    fun mouseClick_rightButton() {
        val event = InputEvent.MouseClick(InputEvent.MouseButton.RIGHT)
        assertEquals(InputEvent.MouseButton.RIGHT, event.button)
    }

    @Test
    fun mouseClick_middleButton() {
        val event = InputEvent.MouseClick(InputEvent.MouseButton.MIDDLE)
        assertEquals(InputEvent.MouseButton.MIDDLE, event.button)
    }

    @Test
    fun mouseClick_noneButton() {
        val event = InputEvent.MouseClick(InputEvent.MouseButton.NONE)
        assertEquals(InputEvent.MouseButton.NONE, event.button)
    }

    // ---- MouseDragState tests ----

    @Test
    fun mouseDragState_down() {
        val event = InputEvent.MouseDragState(InputEvent.MouseButton.LEFT, true)
        assertEquals(InputEvent.MouseButton.LEFT, event.button)
        assertTrue(event.isDown)
    }

    @Test
    fun mouseDragState_up() {
        val event = InputEvent.MouseDragState(InputEvent.MouseButton.LEFT, false)
        assertFalse(event.isDown)
    }

    // ---- Zoom tests ----

    @Test
    fun zoom_positiveScale() {
        val event = InputEvent.Zoom(50)
        assertEquals(50, event.scale)
    }

    @Test
    fun zoom_negativeScale() {
        val event = InputEvent.Zoom(-30)
        assertEquals(-30, event.scale)
    }

    @Test
    fun zoom_zeroScale() {
        val event = InputEvent.Zoom(0)
        assertEquals(0, event.scale)
    }

    // ---- KeyPress tests ----

    @Test
    fun keyPress_singleKey() {
        val keys = listOf(KeyboardLayout.Key(Keycode.MOD_NONE, Keycode.KEY_A))
        val event = InputEvent.KeyPress(keys)
        assertEquals(1, event.keyList.size)
        assertEquals(Keycode.KEY_A, event.keyList[0].code)
    }

    @Test
    fun keyPress_multipleKeys() {
        val keys = listOf(
            KeyboardLayout.Key(Keycode.MOD_LEFT_CTRL, Keycode.KEY_A),
            KeyboardLayout.Key(Keycode.MOD_LEFT_CTRL, Keycode.KEY_C)
        )
        val event = InputEvent.KeyPress(keys)
        assertEquals(2, event.keyList.size)
    }

    @Test
    fun keyPress_emptyList() {
        val event = InputEvent.KeyPress(emptyList())
        assertTrue(event.keyList.isEmpty())
    }

    // ---- NumpadKeyPress tests ----

    @Test
    fun numpadKeyPress_value() {
        val event = InputEvent.NumpadKeyPress(0x59.toByte()) // KEYPAD_1
        assertEquals(0x59.toByte(), event.key)
    }

    // ---- MediaEvent tests ----

    @Test
    fun mediaEvent_allActions() {
        for (action in InputEvent.MediaAction.values()) {
            val event = InputEvent.MediaEvent(action)
            assertEquals(action, event.action)
        }
    }

    // ---- BatteryEvent tests ----

    @Test
    fun batteryEvent_percentage() {
        val event = InputEvent.BatteryEvent(75)
        assertEquals(75, event.percentage)
    }

    @Test
    fun batteryEvent_zeroPercentage() {
        val event = InputEvent.BatteryEvent(0)
        assertEquals(0, event.percentage)
    }

    @Test
    fun batteryEvent_fullPercentage() {
        val event = InputEvent.BatteryEvent(100)
        assertEquals(100, event.percentage)
    }

    // ---- MouseButton enum completeness ----

    @Test
    fun mouseButton_hasAllExpectedValues() {
        val values = InputEvent.MouseButton.values()
        assertEquals(4, values.size)
        assertTrue(values.contains(InputEvent.MouseButton.LEFT))
        assertTrue(values.contains(InputEvent.MouseButton.RIGHT))
        assertTrue(values.contains(InputEvent.MouseButton.MIDDLE))
        assertTrue(values.contains(InputEvent.MouseButton.NONE))
    }

    // ---- MediaAction enum completeness ----

    @Test
    fun mediaAction_hasAllExpectedValues() {
        val values = InputEvent.MediaAction.values()
        assertEquals(8, values.size)
        assertTrue(values.contains(InputEvent.MediaAction.FORWARD))
        assertTrue(values.contains(InputEvent.MediaAction.REPLAY))
        assertTrue(values.contains(InputEvent.MediaAction.NEXT))
        assertTrue(values.contains(InputEvent.MediaAction.PREVIOUS))
        assertTrue(values.contains(InputEvent.MediaAction.PLAY_PAUSE))
        assertTrue(values.contains(InputEvent.MediaAction.VOLUME_MUTE))
        assertTrue(values.contains(InputEvent.MediaAction.VOLUME_UP))
        assertTrue(values.contains(InputEvent.MediaAction.VOLUME_DOWN))
    }

    // ---- Sealed class type checks ----

    @Test
    fun sealedClass_isInstanceOfInputEvent() {
        val events: List<InputEvent> = listOf(
            InputEvent.MouseMove(1, 2),
            InputEvent.MouseScroll(3, 4),
            InputEvent.MouseClick(InputEvent.MouseButton.LEFT),
            InputEvent.MouseDragState(InputEvent.MouseButton.LEFT, true),
            InputEvent.Zoom(5),
            InputEvent.KeyPress(emptyList()),
            InputEvent.NumpadKeyPress(0),
            InputEvent.MediaEvent(InputEvent.MediaAction.PLAY_PAUSE),
            InputEvent.BatteryEvent(50)
        )
        for (event in events) {
            assertTrue("Expected instance of InputEvent", event is InputEvent)
        }
    }
}
