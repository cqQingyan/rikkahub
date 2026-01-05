package me.rerere.rikkahub.service.initializers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.rerere.rikkahub.AppScope
import me.rerere.rikkahub.data.repository.ConversationRepository

class SearchInitializer(
    private val conversationRepository: ConversationRepository,
    private val appScope: AppScope
) {
    fun init() {
        appScope.launch {
            conversationRepository.populateFtsIfNeeded()
        }
    }
}
