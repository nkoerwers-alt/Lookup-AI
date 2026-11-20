
package com.lookupai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class FennAuthState {
    SIGNED_OUT,
    GUEST,
    GOOGLE
}

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

    private val _authState = MutableStateFlow(
        when {
            AIClient.isSignedIn() -> FennAuthState.GOOGLE
            AIClient.isGuest() -> FennAuthState.GUEST
            else -> FennAuthState.SIGNED_OUT
        }
    )
    val authState: StateFlow<FennAuthState> =
        _authState.asStateFlow()

    private val _account = MutableStateFlow(AIClient.getAccount())
    val account: StateFlow<FennAccount?> = _account.asStateFlow()

    init {
        if (_authState.value == FennAuthState.GOOGLE) {
            loadConversations()
        }
    }

    fun onGoogleSignedIn() {
        _authState.value = FennAuthState.GOOGLE
        _account.value = AIClient.getAccount()
        _messages.value = emptyList()
        _conversations.value = emptyList()
        _currentConversationId.value = UUID.randomUUID().toString()
        _error.value = null
        loadConversations()
    }

    fun continueAsGuest() {
        if (_loading.value) return

        AIClient.startGuestMode()
        _authState.value = FennAuthState.GUEST
        _account.value = null
        _messages.value = emptyList()
        _conversations.value = emptyList()
        _currentConversationId.value = UUID.randomUUID().toString()
        _error.value = null
    }

    fun signOut() {
        if (_loading.value) return

        AIClient.signOut()
        _authState.value = FennAuthState.SIGNED_OUT
        _account.value = null
        _messages.value = emptyList()
        _conversations.value = emptyList()
        _currentConversationId.value = UUID.randomUUID().toString()
        _error.value = null
    }

    fun askAI(question: String) {
        if (question.isBlank() || _loading.value) return
        if (_authState.value == FennAuthState.SIGNED_OUT) return

        _error.value = null

        val previousMessages = _messages.value
        _messages.value = previousMessages + ChatMessage(question, true)
        _loading.value = true

        val currentId = _currentConversationId.value

        viewModelScope.launch {
            try {
                val response = AIClient.ask(
                    question = question,
                    conversationId = currentId,
                    previousMessages = previousMessages
                )

                _currentConversationId.value = response.conversationId

                _messages.value += ChatMessage(
                    text = response.answer,
                    isUser = false
                )

                if (_authState.value == FennAuthState.GOOGLE) {
                    loadConversations()
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Something went wrong."
            } finally {
                _loading.value = false
            }
        }
    }

    fun loadConversations() {
        if (_authState.value != FennAuthState.GOOGLE) {
            _conversations.value = emptyList()
            return
        }

        viewModelScope.launch {
            try {
                _conversations.value = AIClient.getConversations()
            } catch (e: Exception) {
                _error.value = e.message ?: "Could not load chat history."
            }
        }
    }

    fun openConversation(id: String) {
        if (_loading.value ||
            _authState.value != FennAuthState.GOOGLE
        ) return

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
        if (_authState.value != FennAuthState.GOOGLE) return

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
        if (_authState.value != FennAuthState.GOOGLE) {
            onResult(emptyList())
            return
        }

        viewModelScope.launch {
            try {
                onResult(AIClient.getMemories())
            } catch (e: Exception) {
                _error.value = e.message ?: "Could not load memories."
            }
        }
    }

    fun deleteMemories() {
        if (_authState.value != FennAuthState.GOOGLE) return

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
