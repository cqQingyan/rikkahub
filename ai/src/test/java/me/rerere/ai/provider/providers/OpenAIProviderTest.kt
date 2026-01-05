package me.rerere.ai.provider.providers

import kotlinx.coroutines.test.runTest
import me.rerere.ai.provider.Model
import me.rerere.ai.provider.ProviderError
import me.rerere.ai.provider.ProviderSetting
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.uuid.Uuid

class OpenAIProviderTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var client: OkHttpClient
    private lateinit var openAIProvider: OpenAIProvider

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        client = OkHttpClient.Builder().build()
        openAIProvider = OpenAIProvider(client)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `listModels returns models when response is successful`() = runTest {
        val modelsJson = """
            {
              "data": [
                {
                  "id": "gpt-3.5-turbo",
                  "object": "model",
                  "created": 1677610602,
                  "owned_by": "openai"
                },
                {
                  "id": "gpt-4",
                  "object": "model",
                  "created": 1687882411,
                  "owned_by": "openai"
                }
              ]
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(modelsJson)
        )

        val setting = ProviderSetting.OpenAI(
            id = Uuid.random(),
            name = "Test",
            baseUrl = mockWebServer.url("/").toString(),
            apiKey = "test-key"
        )

        val result = openAIProvider.listModels(setting)

        assertTrue(result.isSuccess)
        val models = result.getOrNull()
        assertEquals(2, models?.size)
        assertEquals("gpt-3.5-turbo", models?.get(0)?.modelId)
        assertEquals("gpt-4", models?.get(1)?.modelId)
    }

    @Test
    fun `listModels returns ApiError when response is error`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("Unauthorized")
        )

        val setting = ProviderSetting.OpenAI(
            id = Uuid.random(),
            name = "Test",
            baseUrl = mockWebServer.url("/").toString(),
            apiKey = "test-key"
        )

        val result = openAIProvider.listModels(setting)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is ProviderError.ApiError)
        assertEquals(401, (exception as ProviderError.ApiError).code)
    }

    @Test
    fun `getBalance returns balance when response is successful`() = runTest {
         val balanceJson = """
            {
              "balance": 12.3456
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(balanceJson)
        )

        val setting = ProviderSetting.OpenAI(
            id = Uuid.random(),
            name = "Test",
            baseUrl = mockWebServer.url("/").toString(),
            apiKey = "test-key"
        )

        val result = openAIProvider.getBalance(setting)

        assertTrue(result.isSuccess)
        assertEquals("12.35", result.getOrNull())
    }
}
