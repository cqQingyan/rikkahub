package me.rerere.rikkahub.ui.pages.chat

import android.app.Application
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.model.Conversation
import me.rerere.rikkahub.data.repository.ConversationRepository
import me.rerere.rikkahub.domain.usecase.chat.ChatUseCase
import me.rerere.rikkahub.service.ChatService
import me.rerere.rikkahub.utils.UpdateChecker
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.util.UUID
import kotlin.uuid.Uuid

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ChatVMTest {

    @Mock
    private lateinit var context: Application

    @Mock
    private lateinit var settingsStore: SettingsStore

    @Mock
    private lateinit var conversationRepo: ConversationRepository

    @Mock
    private lateinit var chatService: ChatService

    @Mock
    private lateinit var updateChecker: UpdateChecker

    @Mock
    private lateinit var analytics: FirebaseAnalytics

    @Mock
    private lateinit var chatUseCase: ChatUseCase

    private lateinit var chatVM: ChatVM
    private val conversationId = Uuid.random()

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        `when`(settingsStore.settingsFlow).thenReturn(MutableStateFlow(Settings.dummy()))
        `when`(chatService.getConversationFlow(conversationId)).thenReturn(
            MutableStateFlow(
                Conversation(
                    id = conversationId,
                    assistantId = Uuid.random(),
                    messageNodes = emptyList()
                )
            )
        )
        `when`(chatService.getGenerationJobStateFlow(conversationId)).thenReturn(MutableStateFlow(null))
        `when`(chatService.getConversationJobs()).thenReturn(MutableStateFlow(emptyMap()))

        chatVM = ChatVM(
            id = conversationId.toString(),
            context = context,
            settingsStore = settingsStore,
            conversationRepo = conversationRepo,
            chatService = chatService,
            updateChecker = updateChecker,
            analytics = analytics,
            chatUseCase = chatUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `deleteMessage delegates to ChatUseCase`() = runTest {
        val message = UIMessage(parts = listOf(UIMessagePart.Text("test")))

        chatVM.deleteMessage(message)

        verify(chatUseCase).deleteMessage(any(), org.mockito.kotlin.eq(message), any())
    }

    @Test
    fun `forkMessage delegates to ChatUseCase`() = runTest {
        val message = UIMessage(parts = listOf(UIMessagePart.Text("test")))
        val forkedConversation = Conversation(id = Uuid.random(), assistantId = Uuid.random(), messageNodes = emptyList())

        `when`(chatUseCase.forkMessage(any(), org.mockito.kotlin.eq(message), any())).thenReturn(forkedConversation)

        val result = chatVM.forkMessage(message)

        assertEquals(forkedConversation, result)
        verify(chatUseCase).forkMessage(any(), org.mockito.kotlin.eq(message), any())
    }
}
