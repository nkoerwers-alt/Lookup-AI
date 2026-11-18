
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

    private val _conversations =
        MutableStateFlow<List<SavedConversation>>(emptyList())
    val conversations: StateFlow<List<SavedConversation>> =
        _conversations.asStateFlow()

    private val _currentConversationId =
        MutableStateFlow(UUID.randomUUID().toString())
    val currentConversationId: StateFlow<String> =
        _currentConversationId.asStateFlow()

    init {
        loadConversations()
    }

    fun askAI(question: String) {
        if (question.isBlank() || _loading.value) return

        _error.value = null
        _messages.value += ChatMessage(question, true)
        _loading.value = true

        val currentId = _currentConversationId.value

        viewModelScope.launch {
            try {
                val response = AIClient.ask(question, currentId)

                _currentConversationId.value = response.conversationId

                _messages.value += ChatMessage(
                    text = response.answer,
                    isUser = false
                )

                loadConversations()
            } catch (e: Exception) {
                _error.value = e.message ?: "Something went wrong."
            } finally {
                _loading.value = false
            }
        }
    }

    fun loadConversations() {
        viewModelScope.launch {
            try {
                _conversations.value = AIClient.getConversations()
            } catch (e: Exception) {
                _error.value = e.message ?: "Could not load chat history."
            }
        }
    }

    fun openConversation(id: String) {
        if (_loading.value) return

        viewModelScope.launch {
            try {
                _error.value = null
                val conversation = AIClient.getConversation(id)

                _currentConversationId.value = conversation.id
                _messages.value = conversation.messages
            } catch (e: Exception) {
                _error.value = e.message ?: "Could not open conversation."
            }
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            try {
                AIClient.deleteConversation(id)

                if (_currentConversationId.value == id) {
                    clearChat()
                }

                loadConversations()
            } catch (e: Exception) {
                _error.value = e.message ?: "Could not delete conversation."
            }
        }
    }

    fun clearChat() {
        if (_loading.value) return

        _messages.value = emptyList()
        _error.value = null
        _currentConversationId.value = UUID.randomUUID().toString()
    }

    fun loadMemories(onResult: (List<String>) -> Unit) {
        viewModelScope.launch {
            try {
                onResult(AIClient.getMemories())
            } catch (e: Exception) {
                _error.value = e.message ?: "Could not load memories."
            }
        }
    }

    fun deleteMemories() {
        viewModelScope.launch {
            try {
                AIClient.deleteMemories()
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Could not delete memories."
            }
        }
    }
}
