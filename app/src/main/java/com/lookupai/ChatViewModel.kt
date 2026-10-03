package com.lookupai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun askAI(question: String) {
        if (question.isBlank() || _loading.value) return

        _error.value = null
        _messages.value =
            _messages.value + ChatMessage(text = question, isUser = true)

        _loading.value = true

        viewModelScope.launch {
            try {
                val response = AIClient.ask(question)

                _messages.value =
                    _messages.value + ChatMessage(
                        text = response,
                        isUser = false
                    )
            } catch (e: Exception) {
                _error.value = e.message ?: "Something went wrong."
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
        _error.value = null
    }
}