package me.rerere.common.http

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonExpressionTest {

    @Test
    fun testParseExpression() {
        // Valid expressions
        assertTrue(parseExpression("1 + 2").success)
        assertTrue(parseExpression("\"hello\" ++ \" world\"").success)
        assertTrue(parseExpression("data.value").success)
        assertTrue(parseExpression("items[0]").success)
        assertTrue(parseExpression("-1").success)
        assertTrue(parseExpression("1 * 2 + 3").success)

        // Invalid expressions
        assertFalse(parseExpression("1 +").success)
        assertFalse(parseExpression("++").success)
        assertFalse(parseExpression("unclosed \"string").success)
        // 1 / 0 is syntactically valid (Binary op), but evaluation might default to Infinity/NaN or something else.
        // The parser only checks syntax. So this assertion fails because it IS valid syntax.
        // We should check that it is valid syntax.
        assertTrue(parseExpression("1 / 0").success)
    }

    @Test
    fun testEvaluateJsonExpr() {
        val root = buildJsonObject {
            put("name", "Rikka")
            put("age", 17)
            put("score", 98.5)
            putJsonObject("stats") {
                put("hp", 100)
                put("mp", 50)
            }
            putJsonArray("items") {
                add(buildJsonObject { put("id", 1); put("name", "Potion") })
                add(buildJsonObject { put("id", 2); put("name", "Sword") })
            }
        }

        // Basic literals
        assertEquals("3", evaluateJsonExpr("1 + 2", root))
        assertEquals("hello", evaluateJsonExpr("\"hello\"", root))

        // Field access
        assertEquals("Rikka", evaluateJsonExpr("name", root))
        assertEquals("17", evaluateJsonExpr("age", root))
        assertEquals("98.5", evaluateJsonExpr("score", root))

        // Nested access
        assertEquals("100", evaluateJsonExpr("stats.hp", root))

        // Array access
        assertEquals("Potion", evaluateJsonExpr("items[0].name", root))
        assertEquals("Sword", evaluateJsonExpr("items[1].name", root))

        // Arithmetic
        assertEquals("117", evaluateJsonExpr("age + stats.hp", root))
        assertEquals("49.25", evaluateJsonExpr("score / 2", root))
        assertEquals("200", evaluateJsonExpr("stats.hp * 2", root))
        assertEquals("50", evaluateJsonExpr("stats.hp - stats.mp", root))

        // String concatenation
        assertEquals("Rikka is 17", evaluateJsonExpr("name ++ \" is \" ++ age", root))

        // Missing fields (should resolve to empty string or 0 for calculations depending on implementation details)
        // Based on implementation: missing -> null -> empty string -> 0.0 for number conversion
        assertEquals("", evaluateJsonExpr("missing", root))
        assertEquals("0", evaluateJsonExpr("missing + 0", root)) // "" -> 0.0 + 0 = 0
    }

    @Test
    fun testEvaluateMath() {
        val root = buildJsonObject {}
        assertEquals("4", evaluateJsonExpr("2 + 2", root))
        assertEquals("0", evaluateJsonExpr("2 - 2", root))
        assertEquals("6", evaluateJsonExpr("2 * 3", root))
        assertEquals("2.5", evaluateJsonExpr("5 / 2", root))
        assertEquals("-2", evaluateJsonExpr("-2", root))
        assertEquals("2", evaluateJsonExpr("+2", root))
        assertEquals("7", evaluateJsonExpr("1 + 2 * 3", root)) // Precedence
        assertEquals("9", evaluateJsonExpr("(1 + 2) * 3", root)) // Parentheses
    }

    @Test
    fun testEvaluateString() {
        val root = buildJsonObject {
            put("a", "Hello")
            put("b", "World")
        }
        assertEquals("HelloWorld", evaluateJsonExpr("a ++ b", root))
        assertEquals("Hello World", evaluateJsonExpr("a ++ \" \" ++ b", root))
        assertEquals("escaped \" quote", evaluateJsonExpr("\"escaped \\\" quote\"", root))
    }
}
