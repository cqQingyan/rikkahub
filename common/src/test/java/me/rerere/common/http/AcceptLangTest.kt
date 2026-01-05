package me.rerere.common.http

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class AcceptLangTest {

    @Test
    fun testFromJvmSystem() {
        val builder = AcceptLanguageBuilder.fromJvmSystem()
        val result = builder.build()
        // Assuming default JVM locale is something like en_US or just en
        // result should not be empty
        assert(result.isNotEmpty())
    }

    @Test
    fun testWithCustomLocales() {
        val locales = listOf(
            Locale.SIMPLIFIED_CHINESE, // zh-CN
            Locale.US, // en-US
            Locale.JAPAN // ja-JP
        )
        val builder = AcceptLanguageBuilder.withLocales(locales, AcceptLanguageBuilder.Options(maxLanguages = 10))
        val result = builder.build()

        // Expected: zh-CN, zh;q=0.9, en-US;q=0.8, en;q=0.7, ja-JP;q=0.6, ja;q=0.5
        // Note: The builder includes generic languages by default

        val parts = result.split(", ")
        assertEquals("zh-CN", parts[0])
        assertEquals("zh;q=0.9", parts[1])
        assertEquals("en-US;q=0.8", parts[2])
        assertEquals("en;q=0.7", parts[3])
        assertEquals("ja-JP;q=0.6", parts[4])
        assertEquals("ja;q=0.5", parts[5])
    }

    @Test
    fun testDeduplication() {
        val locales = listOf(
            Locale.SIMPLIFIED_CHINESE, // zh-CN -> [zh-CN, zh]
            Locale.CHINESE // zh -> [zh]
        )
        // Without deduplication it would be: zh-CN, zh, zh
        // With deduplication it should be: zh-CN, zh

        val builder = AcceptLanguageBuilder.withLocales(locales)
        val result = builder.build()

        assertEquals("zh-CN, zh;q=0.9", result)
    }

    @Test
    fun testMaxLanguages() {
        val locales = listOf(
            Locale.SIMPLIFIED_CHINESE,
            Locale.US
        )
        // zh-CN, zh, en-US, en

        val builder = AcceptLanguageBuilder.withLocales(
            locales,
            AcceptLanguageBuilder.Options(maxLanguages = 3)
        )
        val result = builder.build()

        val parts = result.split(", ")
        assertEquals(3, parts.size)
        assertEquals("zh-CN", parts[0])
        assertEquals("zh;q=0.9", parts[1])
        assertEquals("en-US;q=0.8", parts[2])
    }

    @Test
    fun testQValueSteps() {
        val locales = listOf(
            Locale("en"), // a
            Locale("fr"), // b
            Locale("de")  // c
        )
        val builder = AcceptLanguageBuilder.withLocales(
            locales,
            AcceptLanguageBuilder.Options(
                includeGenericLanguage = false,
                qStep = 0.2,
                minQ = 0.5
            )
        )
        val result = builder.build()

        val parts = result.split(", ")
        assertEquals("en", parts[0]) // q=1.0
        assertEquals("fr;q=0.8", parts[1]) // q=1.0 - 0.2 = 0.8
        assertEquals("de;q=0.6", parts[2]) // q=1.0 - 0.4 = 0.6
    }

    @Test
    fun testMinQ() {
        val locales = listOf(
            Locale("en"), Locale("fr"), Locale("de"), Locale("it"), Locale("es"), Locale("ja")
        )
        val builder = AcceptLanguageBuilder.withLocales(
            locales,
            AcceptLanguageBuilder.Options(
                includeGenericLanguage = false,
                qStep = 0.5,
                minQ = 0.1
            )
        )
        val result = builder.build()

        val parts = result.split(", ")
        assertEquals("en", parts[0])
        assertEquals("fr;q=0.5", parts[1])
        assertEquals("de;q=0.1", parts[2]) // minQ reached
        assertEquals("it;q=0.1", parts[3])
    }
}
