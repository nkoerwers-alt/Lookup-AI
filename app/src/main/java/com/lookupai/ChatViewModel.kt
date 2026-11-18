
package com.lookupai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel : ViewModel() {

    private val _messages =
        MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> =
        _messages.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var conversationId = UUID.randomUUID().toString()

    fun askAI(question: String) {
        if (question.isBlank() || _loading.value) return

        _error.value = null

        _messages.value += ChatMessage(
            text = question,
            isUser = true
        )

        _loading.value = true

        val currentConversation = conversationId

        viewModelScope.launch {
            try {
                val response = AIClient.ask(
                    question,
                    currentConversation
                )

                _messages.value += ChatMessage(
                    text = response.answer,
                    isUser = false
                )

            } catch (e: Exception) {
                _error.value =
                    e.message ?: "Something went wrong."

            } finally {
                _loading.value = false
            }
        }
    }

    fun clearChat() {
        if (_loading.value) return

        _messages.value = emptyList()
        _error.value = null
        conversationId = UUID.randomUUID().toString()
    }

    fun loadMemories(
        onResult: (List<String>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                onResult(AIClient.getMemories())
            } catch (e: Exception) {
                _error.value =
                    e.message ?: "Could not load memories."
            }
        }
    }

    fun deleteMemories() {
        viewModelScope.launch {
            try {
                AIClient.deleteMemories()
                _error.value = null
            } catch (e: Exception) {
                _error.value =
                    e.message ?: "Could not delete memories."
            }
        }
    }
}
