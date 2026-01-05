package me.rerere.ai.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyRouletteTest {

    @Test
    fun testDefaultKeyRoulette() {
        val roulette = KeyRoulette.default()

        // Single key
        val singleKey = "sk-123456"
        assertEquals(singleKey, roulette.next(singleKey))

        // Multiple keys separated by newline
        val keysNewline = "sk-1\nsk-2\nsk-3"
        val resultNewline = roulette.next(keysNewline)
        assertTrue(resultNewline in listOf("sk-1", "sk-2", "sk-3"))

        // Multiple keys separated by comma
        val keysComma = "sk-1,sk-2,sk-3"
        val resultComma = roulette.next(keysComma)
        assertTrue(resultComma in listOf("sk-1", "sk-2", "sk-3"))

        // Multiple keys separated by mixed delimiters
        val keysMixed = "sk-1, sk-2\nsk-3"
        val resultMixed = roulette.next(keysMixed)
        assertTrue(resultMixed in listOf("sk-1", "sk-2", "sk-3"))

        // Empty keys
        val emptyKeys = ""
        assertEquals("", roulette.next(emptyKeys))

        // Whitespace only
        val whitespaceKeys = "   \n  "
        // Based on implementation: splitKey filters blank, so keyList will be empty, returns original keys
        assertEquals(whitespaceKeys, roulette.next(whitespaceKeys))
    }

    @Test
    fun testKeySplitting() {
        val roulette = KeyRoulette.default()

        // Ensure that trimming works
        val keys = "  sk-1  ,  sk-2  "
        val result = roulette.next(keys)
        assertTrue(result in listOf("sk-1", "sk-2"))

        // Ensure deduplication logic (indirectly tested by randomness, but we can trust splitKey implementation for now or reflectively test it if needed)
        // Since splitKey is private, we test the public behavior.

        // Test robustness
        val messyKeys = ",,sk-1,,,\n\nsk-2,,"
        val messyResult = roulette.next(messyKeys)
        assertTrue(messyResult in listOf("sk-1", "sk-2"))
    }
}
