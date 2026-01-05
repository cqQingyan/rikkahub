package me.rerere.rikkahub.service.chat

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.rerere.ai.core.MessageRole
import me.rerere.ai.provider.ModelAbility
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.ai.ui.finishReasoning
import me.rerere.rikkahub.AppScope
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.ai.GenerationChunk
import me.rerere.rikkahub.data.ai.GenerationHandler
import me.rerere.rikkahub.data.ai.tools.LocalTools
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.datastore.getCurrentAssistant
import me.rerere.rikkahub.data.datastore.getCurrentChatModel
import me.rerere.rikkahub.data.model.toMessageNode
import me.rerere.rikkahub.data.repository.MemoryRepository
import java.time.Instant
import kotlin.uuid.Uuid

private const val TAG = "GenerationManager"

class GenerationManager(
    private val context: Context,
    private val appScope: AppScope,
    private val settingsStore: SettingsStore,
    private val memoryRepository: MemoryRepository,
    private val generationHandler: GenerationHandler,
    private val conversationCoordinator: ConversationCoordinator,
    private val messageProcessor: MessageProcessor,
    private val localTools: LocalTools
) {
    private val _generationJobs = MutableStateFlow<Map<Uuid, Job?>>(emptyMap())
    private val generationJobs: StateFlow<Map<Uuid, Job?>> = _generationJobs.asStateFlow()

    private val _errors = MutableStateFlow<List<ChatError>>(emptyList())
    val errors: StateFlow<List<ChatError>> = _errors.asStateFlow()

    private val _generationDoneFlow = MutableSharedFlow<Uuid>()
    val generationDoneFlow: SharedFlow<Uuid> = _generationDoneFlow.asSharedFlow()

    // Needs access to check foreground for notifications
    var isForeground: (() -> Boolean) = { true }
    var onShowNotification: ((Uuid) -> Unit)? = null

    fun addError(error: Throwable) {
        if (error is CancellationException) return
        _errors.update { it + ChatError(error = error) }
    }

    fun dismissError(id: Uuid) {
        _errors.update { list -> list.filter { it.id != id } }
    }

    fun clearAllErrors() {
        _errors.value = emptyList()
    }

    fun getGenerationJobStateFlow(conversationId: Uuid): Flow<Job?> {
        return generationJobs.map { jobs -> jobs[conversationId] }
    }

    private fun setGenerationJob(conversationId: Uuid, job: Job?) {
        if (job == null) {
            removeGenerationJob(conversationId)
            return
        }
        _generationJobs.value = _generationJobs.value.toMutableMap().apply {
            this[conversationId] = job
        }.toMap()
    }

    fun getGenerationJob(conversationId: Uuid): Job? {
        return _generationJobs.value[conversationId]
    }

    fun removeGenerationJob(conversationId: Uuid) {
        _generationJobs.value = _generationJobs.value.toMutableMap().apply {
            remove(conversationId)
        }.toMap()
    }

    fun cancelJob(conversationId: Uuid) {
        getGenerationJob(conversationId)?.cancel()
    }

    fun sendMessage(conversationId: Uuid, content: List<UIMessagePart>, answer: Boolean = true) {
        getGenerationJob(conversationId)?.cancel()

        val job = appScope.launch {
            try {
                val currentConversation = conversationCoordinator.getConversationFlow(conversationId).value

                val newConversation = currentConversation.copy(
                    messageNodes = currentConversation.messageNodes + UIMessage(
                        role = MessageRole.USER,
                        parts = content,
                    ).toMessageNode(),
                )
                conversationCoordinator.saveConversation(conversationId, newConversation)

                if (answer) {
                    handleMessageComplete(conversationId)
                }

                _generationDoneFlow.emit(conversationId)
            } catch (e: Exception) {
                e.printStackTrace()
                addError(e)
            }
        }
        setGenerationJob(conversationId, job)
        job.invokeOnCompletion {
            setGenerationJob(conversationId, null)
            appScope.launch {
                delay(500)
                conversationCoordinator.checkAllConversationsReferences()
            }
        }
    }

    fun regenerateAtMessage(
        conversationId: Uuid,
        message: UIMessage,
        regenerateAssistantMsg: Boolean = true
    ) {
        getGenerationJob(conversationId)?.cancel()

        val job = appScope.launch {
            try {
                val conversation = conversationCoordinator.getConversationFlow(conversationId).value

                if (message.role == MessageRole.USER) {
                    val node = conversation.getMessageNodeByMessage(message)
                    val indexAt = conversation.messageNodes.indexOf(node)
                    val newConversation = conversation.copy(
                        messageNodes = conversation.messageNodes.subList(0, indexAt + 1)
                    )
                    conversationCoordinator.saveConversation(conversationId, newConversation)
                    handleMessageComplete(conversationId)
                } else {
                    if (regenerateAssistantMsg) {
                        val node = conversation.getMessageNodeByMessage(message)
                        val nodeIndex = conversation.messageNodes.indexOf(node)
                        handleMessageComplete(conversationId, messageRange = 0..<nodeIndex)
                    } else {
                        conversationCoordinator.saveConversation(conversationId, conversation)
                    }
                }

                _generationDoneFlow.emit(conversationId)
            } catch (e: Exception) {
                addError(e)
            }
        }

        setGenerationJob(conversationId, job)
        job.invokeOnCompletion {
            setGenerationJob(conversationId, null)
            appScope.launch {
                delay(500)
                conversationCoordinator.checkAllConversationsReferences()
            }
        }
    }

    private suspend fun handleMessageComplete(
        conversationId: Uuid,
        messageRange: ClosedRange<Int>? = null
    ) {
        val settings = settingsStore.settingsFlow.first()
        val model = settings.getCurrentChatModel() ?: return

        runCatching {
            var conversation = conversationCoordinator.getConversationFlow(conversationId).value

            if (!model.abilities.contains(ModelAbility.TOOL)) {
                if (settings.enableWebSearch) {
                    addError(IllegalStateException(context.getString(R.string.tools_warning)))
                }
            }

            // Check invalid messages using Processor
            val processedConversation = messageProcessor.checkInvalidMessages(conversation)
            if (processedConversation != conversation) {
                conversationCoordinator.updateConversation(conversationId, processedConversation)
                conversation = processedConversation
            }

            generationHandler.generateText(
                settings = settings,
                model = model,
                messages = conversation.currentMessages.let {
                    if (messageRange != null) {
                        it.subList(messageRange.start, messageRange.endInclusive + 1)
                    } else {
                        it
                    }
                },
                assistant = settings.getCurrentAssistant(),
                memories = memoryRepository.getMemoriesOfAssistant(settings.assistantId.toString()),
                inputTransformers = messageProcessor.inputTransformers,
                outputTransformers = messageProcessor.outputTransformers,
                tools = buildList {
                    if (settings.enableWebSearch) {
                        addAll(messageProcessor.createSearchTool(settings))
                    }
                    addAll(localTools.getTools(settings.getCurrentAssistant().localTools))
                },
                truncateIndex = conversation.truncateIndex,
            ).onCompletion {
                val updatedConversation = conversationCoordinator.getConversationFlow(conversationId).value.copy(
                    messageNodes = conversationCoordinator.getConversationFlow(conversationId).value.messageNodes.map { node ->
                        node.copy(messages = node.messages.map { it.finishReasoning() })
                    },
                    updateAt = Instant.now()
                )
                conversationCoordinator.updateConversation(conversationId, updatedConversation)

                if (!isForeground() && settings.displaySetting.enableNotificationOnMessageGeneration) {
                     onShowNotification?.invoke(conversationId)
                }
            }.collect { chunk ->
                when (chunk) {
                    is GenerationChunk.Messages -> {
                        val updatedConversation = conversationCoordinator.getConversationFlow(conversationId).value
                            .updateCurrentMessages(chunk.messages)
                        conversationCoordinator.updateConversation(conversationId, updatedConversation)
                    }
                }
            }
        }.onFailure {
             it.printStackTrace()
            addError(it)
        }.onSuccess {
            val finalConversation = conversationCoordinator.getConversationFlow(conversationId).value
            conversationCoordinator.saveConversation(conversationId, finalConversation)

            conversationCoordinator.addConversationReference(conversationId)
            appScope.launch {
                coroutineScope {
                    launch { conversationCoordinator.generateTitle(conversationId, finalConversation) }
                }
            }.invokeOnCompletion {
                conversationCoordinator.removeConversationReference(conversationId)
            }
        }
    }
}
