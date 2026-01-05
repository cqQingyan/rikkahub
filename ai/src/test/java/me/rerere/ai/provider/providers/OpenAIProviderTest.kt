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
    private lateinit var provider: OpenAIProvider
    private lateinit var client: OkHttpClient

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        client = OkHttpClient.Builder().build()
        provider = OpenAIProvider(client)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `listModels returns success when API call succeeds`() = runTest {
        val jsonResponse = """
            {
              "object": "list",
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

        mockWebServer.enqueue(MockResponse().setBody(jsonResponse).setResponseCode(200))

        val settings = ProviderSetting.OpenAI(
            id = Uuid.random(),
            baseUrl = mockWebServer.url("/").toString(),
            apiKey = "test-key"
        )

        val result = provider.listModels(settings)

        assertTrue(result.isSuccess)
        val models = result.getOrThrow()
        assertEquals(2, models.size)
        assertEquals("gpt-3.5-turbo", models[0].modelId)
        assertEquals("gpt-4", models[1].modelId)
    }

    @Test
    fun `listModels returns failure when API call fails`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(401).setBody("Unauthorized"))

        val settings = ProviderSetting.OpenAI(
            id = Uuid.random(),
            baseUrl = mockWebServer.url("/").toString(),
            apiKey = "test-key"
        )

        val result = provider.listModels(settings)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is ProviderError.ApiError)
        assertEquals(401, (exception as ProviderError.ApiError).code)
    }

    @Test
    fun `listModels returns failure when parsing fails`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody("{invalid json").setResponseCode(200))

        val settings = ProviderSetting.OpenAI(
            id = Uuid.random(),
            baseUrl = mockWebServer.url("/").toString(),
            apiKey = "test-key"
        )

        val result = provider.listModels(settings)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ProviderError.ParsingError)
    }
}
