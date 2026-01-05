package me.rerere.rikkahub.service

import android.Manifest
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.rerere.ai.provider.ProviderManager
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.ai.ui.truncate
import me.rerere.rikkahub.AppScope
import me.rerere.rikkahub.CHAT_COMPLETED_NOTIFICATION_CHANNEL_ID
import me.rerere.rikkahub.R
import me.rerere.rikkahub.RouteActivity
import me.rerere.rikkahub.data.ai.GenerationHandler
import me.rerere.rikkahub.data.ai.tools.LocalTools
import me.rerere.rikkahub.data.ai.transformers.TemplateTransformer
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.model.Conversation
import me.rerere.rikkahub.data.repository.ConversationRepository
import me.rerere.rikkahub.data.repository.MemoryRepository
import me.rerere.rikkahub.service.chat.ChatError
import me.rerere.rikkahub.service.chat.ConversationCoordinator
import me.rerere.rikkahub.service.chat.GenerationManager
import me.rerere.rikkahub.service.chat.MessageProcessor
import me.rerere.rikkahub.utils.applyPlaceholders
import java.util.Locale
import kotlin.uuid.Uuid

private const val TAG = "ChatService"

class ChatService(
    private val context: Application,
    private val appScope: AppScope,
    private val settingsStore: SettingsStore,
    private val conversationRepo: ConversationRepository,
    private val memoryRepository: MemoryRepository,
    private val generationHandler: GenerationHandler,
    private val templateTransformer: TemplateTransformer,
    private val providerManager: ProviderManager,
    private val localTools: LocalTools,
) {
    private val conversationCoordinator = ConversationCoordinator(
        context = context,
        appScope = appScope,
        settingsStore = settingsStore,
        conversationRepo = conversationRepo,
        providerManager = providerManager
    )

    private val messageProcessor = MessageProcessor(
        context = context,
        settingsStore = settingsStore,
        templateTransformer = templateTransformer,
        localTools = localTools,
        generationHandler = generationHandler
    )

    private val generationManager = GenerationManager(
        context = context,
        appScope = appScope,
        settingsStore = settingsStore,
        memoryRepository = memoryRepository,
        generationHandler = generationHandler,
        conversationCoordinator = conversationCoordinator,
        messageProcessor = messageProcessor,
        localTools = localTools
    )

    val errors: StateFlow<List<ChatError>> = generationManager.errors

    fun addError(error: Throwable) {
        generationManager.addError(error)
    }

    fun dismissError(id: Uuid) {
        generationManager.dismissError(id)
    }

    fun clearAllErrors() {
        generationManager.clearAllErrors()
    }

    val generationDoneFlow: SharedFlow<Uuid> = generationManager.generationDoneFlow

    private val _isForeground = MutableStateFlow(false)
    val isForeground: StateFlow<Boolean> = _isForeground.asStateFlow()

    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> _isForeground.value = true
            Lifecycle.Event.ON_STOP -> _isForeground.value = false
            else -> {}
        }
    }

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
        generationManager.isForeground = { isForeground.value }
        generationManager.onShowNotification = { sendGenerationDoneNotification(it) }
        conversationCoordinator.hasExternalReference = {
            // Also check if generation manager has a job for this conversation
            generationManager.getGenerationJob(it) != null
        }
    }

    fun cleanup() = runCatching {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
        // Cleanup all jobs
        // We can't iterate over all jobs easily unless we expose them, but for now we rely on app lifecycle
    }

    fun addConversationReference(conversationId: Uuid) {
        conversationCoordinator.addConversationReference(conversationId)
    }

    fun removeConversationReference(conversationId: Uuid) {
        conversationCoordinator.removeConversationReference(conversationId)
    }

    fun checkAllConversationsReferences() {
        conversationCoordinator.checkAllConversationsReferences()
    }

    fun getConversationFlow(conversationId: Uuid): StateFlow<Conversation> {
        return conversationCoordinator.getConversationFlow(conversationId)
    }

    fun getGenerationJobStateFlow(conversationId: Uuid): Flow<Job?> {
        return generationManager.getGenerationJobStateFlow(conversationId)
    }

    suspend fun initializeConversation(conversationId: Uuid) {
        conversationCoordinator.initializeConversation(conversationId)
    }

    fun sendMessage(conversationId: Uuid, content: List<UIMessagePart>, answer: Boolean = true) {
        generationManager.sendMessage(conversationId, content, answer)
    }

    fun regenerateAtMessage(
        conversationId: Uuid,
        message: UIMessage,
        regenerateAssistantMsg: Boolean = true
    ) {
        generationManager.regenerateAtMessage(conversationId, message, regenerateAssistantMsg)
    }

    suspend fun saveConversation(conversationId: Uuid, conversation: Conversation) {
        conversationCoordinator.saveConversation(conversationId, conversation)
    }

    fun cleanupConversation(conversationId: Uuid) {
        generationManager.cancelJob(conversationId)
        generationManager.removeGenerationJob(conversationId)
        conversationCoordinator.cleanupConversation(conversationId)
    }

    fun translateMessage(
        conversationId: Uuid,
        message: UIMessage,
        targetLanguage: Locale
    ) {
        appScope.launch(Dispatchers.IO) {
            try {
                val settings = settingsStore.settingsFlow.first()

                val messageText = message.parts.filterIsInstance<UIMessagePart.Text>()
                    .joinToString("\n\n") { it.text }
                    .trim()

                if (messageText.isBlank()) return@launch

                val loadingText = context.getString(R.string.translating)
                updateTranslationField(conversationId, message.id, loadingText)

                generationHandler.translateText(
                    settings = settings,
                    sourceText = messageText,
                    targetLanguage = targetLanguage
                ) { translatedText ->
                    updateTranslationField(conversationId, message.id, translatedText)
                }.collect { }

                saveConversation(conversationId, getConversationFlow(conversationId).value)
            } catch (e: Exception) {
                clearTranslationField(conversationId, message.id)
                addError(e)
            }
        }
    }

    private fun updateTranslationField(
        conversationId: Uuid,
        messageId: Uuid,
        translationText: String
    ) {
        val currentConversation = getConversationFlow(conversationId).value
        val updatedNodes = currentConversation.messageNodes.map { node ->
            if (node.messages.any { it.id == messageId }) {
                val updatedMessages = node.messages.map { msg ->
                    if (msg.id == messageId) {
                        msg.copy(translation = translationText)
                    } else {
                        msg
                    }
                }
                node.copy(messages = updatedMessages)
            } else {
                node
            }
        }

        conversationCoordinator.updateConversation(conversationId, currentConversation.copy(messageNodes = updatedNodes))
    }

    fun clearTranslationField(conversationId: Uuid, messageId: Uuid) {
        val currentConversation = getConversationFlow(conversationId).value
        val updatedNodes = currentConversation.messageNodes.map { node ->
            if (node.messages.any { it.id == messageId }) {
                val updatedMessages = node.messages.map { msg ->
                    if (msg.id == messageId) {
                        msg.copy(translation = null)
                    } else {
                        msg
                    }
                }
                node.copy(messages = updatedMessages)
            } else {
                node
            }
        }

        conversationCoordinator.updateConversation(conversationId, currentConversation.copy(messageNodes = updatedNodes))
    }

    // Notifications logic moved from private to here or keep it private and pass lambda to GenerationManager
    private fun sendGenerationDoneNotification(conversationId: Uuid) {
        val conversation = getConversationFlow(conversationId).value
        val notification =
            NotificationCompat.Builder(context, CHAT_COMPLETED_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(context.getString(R.string.notification_chat_done_title))
                .setContentText(conversation.currentMessages.lastOrNull()?.toText()?.take(50) ?: "")
                .setSmallIcon(R.drawable.small_icon)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentIntent(getPendingIntent(context, conversationId))

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(1, notification.build())
    }

    private fun getPendingIntent(context: Context, conversationId: Uuid): PendingIntent {
        val intent = Intent(context, RouteActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("conversationId", conversationId.toString())
        }
        return PendingIntent.getActivity(
            context,
            conversationId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
