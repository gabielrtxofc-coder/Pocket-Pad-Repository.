package com.darusc.mousedroid.layouts

import com.darusc.mousedroid.layouts.languages.KeyboardLayoutUS
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for KeyboardLayout and Keycode.
 *
 * These tests verify the character-to-keycode mapping used by the
 * keyboard input path. The US layout is used as the reference.
 */
class KeyboardLayoutTest {

    private val usLayout = KeyboardLayoutUS()

    // ==================== US Layout basic mappings ====================

    @Test
    fun usLayout_lowercaseLetters() {
        for (c in 'a'..'z') {
            val mapping = usLayout.getMapping(c)
            assertNotNull("US layout should map lowercase '$c'", mapping)
            assertTrue("Lowercase '$c' should produce at least one key", mapping!!.isNotEmpty())
            // Lowercase letters should have no modifier
            assertEquals("Lowercase '$c' should have no modifier",
                Keycode.MOD_NONE, mapping[0].modifier)
        }
    }

    @Test
    fun usLayout_uppercaseLetters() {
        for (c in 'A'..'Z') {
            val mapping = usLayout.getMapping(c)
            assertNotNull("US layout should map uppercase '$c'", mapping)
            assertTrue("Uppercase '$c' should produce at least one key", mapping!!.isNotEmpty())
            // Uppercase letters require SHIFT modifier
            assertEquals("Uppercase '$c' should have SHIFT modifier",
                Keycode.MOD_LEFT_SHIFT, mapping[0].modifier)
        }
    }

    @Test
    fun usLayout_digits() {
        for (c in '0'..'9') {
            val mapping = usLayout.getMapping(c)
            assertNotNull("US layout should map digit '$c'", mapping)
        }
    }

    @Test
    fun usLayout_space() {
        val mapping = usLayout.getMapping(' ')
        assertNotNull("US layout should map space", mapping)
        assertEquals(Keycode.MOD_NONE, mapping!![0].modifier)
        assertEquals(Keycode.KEY_SPACE, mapping[0].code)
    }

    @Test
    fun usLayout_enter() {
        val mapping = usLayout.getMapping('\n')
        assertNotNull("US layout should map newline (enter)", mapping)
        assertEquals(Keycode.KEY_ENTER, mapping!![0].code)
    }

    // ==================== Keycode constant values ====================

    @Test
    fun keycode_modifierValues_arePowersOfTwo() {
        // Each modifier is a single bit
        assertEquals(0x01.toByte(), Keycode.MOD_LEFT_CTRL)
        assertEquals(0x02.toByte(), Keycode.MOD_LEFT_SHIFT)
        assertEquals(0x04.toByte(), Keycode.MOD_LEFT_ALT)
        assertEquals(0x08.toByte(), Keycode.MOD_LEFT_GUI)
        assertEquals(0x10.toByte(), Keycode.MOD_RIGHT_CTRL)
        assertEquals(0x20.toByte(), Keycode.MOD_RIGHT_SHIFT)
        assertEquals(0x40.toByte(), Keycode.MOD_RIGHT_ALT)
        assertEquals(0x80.toByte(), Keycode.MOD_RIGHT_GUI)
    }

    @Test
    fun keycode_letterSequence() {
        // Letters should be sequential starting from KEY_A (0x04)
        assertEquals(0x04.toByte(), Keycode.KEY_A)
        assertEquals(0x05.toByte(), Keycode.KEY_B)
        assertEquals(0x06.toByte(), Keycode.KEY_C)
        assertEquals(0x1D.toByte(), Keycode.KEY_Z) // Z is the 26th letter
    }

    @Test
    fun keycode_digitSequence() {
        // Digits 1-9, 0 should be sequential starting from 0x1E
        assertEquals(0x1E.toByte(), Keycode.KEY_1)
        assertEquals(0x1F.toByte(), Keycode.KEY_2)
        assertEquals(0x26.toByte(), Keycode.KEY_9)
        assertEquals(0x27.toByte(), Keycode.KEY_0)
    }

    @Test
    fun keycode_functionKeys() {
        assertEquals(0x3A.toByte(), Keycode.KEY_F1)
        assertEquals(0x45.toByte(), Keycode.KEY_F12)
    }

    @Test
    fun keycode_navigationKeys() {
        assertEquals(0x49.toByte(), Keycode.KEY_INSERT)
        assertEquals(0x4A.toByte(), Keycode.KEY_HOME)
        assertEquals(0x4B.toByte(), Keycode.KEY_PAGE_UP)
        assertEquals(0x4C.toByte(), Keycode.KEY_DELETE)
        assertEquals(0x4D.toByte(), Keycode.KEY_END)
        assertEquals(0x4E.toByte(), Keycode.KEY_PAGE_DOWN)
    }

    @Test
    fun keycode_arrowKeys() {
        assertEquals(0x4F.toByte(), Keycode.KEY_RIGHT)
        assertEquals(0x50.toByte(), Keycode.KEY_LEFT)
        assertEquals(0x51.toByte(), Keycode.KEY_DOWN)
        assertEquals(0x52.toByte(), Keycode.KEY_UP)
    }

    // ==================== Key data class ====================

    @Test
    fun key_dataClassEquality() {
        val key1 = KeyboardLayout.Key(Keycode.MOD_LEFT_CTRL, Keycode.KEY_A)
        val key2 = KeyboardLayout.Key(Keycode.MOD_LEFT_CTRL, Keycode.KEY_A)
        assertEquals(key1, key2)
    }

    @Test
    fun key_dataClassInequality() {
        val key1 = KeyboardLayout.Key(Keycode.MOD_LEFT_CTRL, Keycode.KEY_A)
        val key2 = KeyboardLayout.Key(Keycode.MOD_LEFT_SHIFT, Keycode.KEY_A)
        assertNotEquals(key1, key2)
    }

    // ==================== Layout name ====================

    @Test
    fun usLayout_hasName() {
        assertTrue("US layout should have a non-empty name", usLayout.name.isNotEmpty())
    }

    // ==================== Special character mappings ====================

    @Test
    fun usLayout_specialCharacters_mapped() {
        // Common special characters should be mapped
        val specialChars = listOf('!', '@', '#', '$', '%', '^', '&', '*', '(', ')',
            '-', '=', '[', ']', '\\', ';', '\'', ',', '.', '/')
        for (c in specialChars) {
            val mapping = usLayout.getMapping(c)
            assertNotNull("US layout should map special character '$c'", mapping)
        }
    }

    @Test
    fun usLayout_shiftedSymbols_requireShift() {
        // Symbols like !, @, #, etc. require SHIFT in US layout
        val shiftRequired = listOf('!', '@', '#', '$', '%', '^', '&', '*', '(', ')',
            '_', '+', '{', '}', '|', ':', '"', '<', '>', '?')
        for (c in shiftRequired) {
            val mapping = usLayout.getMapping(c)
            if (mapping != null && mapping.isNotEmpty()) {
                assertEquals("Symbol '$c' should require SHIFT",
                    Keycode.MOD_LEFT_SHIFT, mapping[0].modifier)
            }
        }
    }

    @Test
    fun usLayout_unmappedCharacters_returnNull() {
        // Characters not in the US layout (e.g., non-ASCII) should return null
        val unmapped = usLayout.getMapping('ñ')
        assertNull("Non-ASCII characters should not be mapped in US layout", unmapped)
    }
}
