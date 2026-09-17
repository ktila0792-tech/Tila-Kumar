package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class Message(
    val text: String,
    val isUser: Boolean
)

data class UiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val conversationHistory = mutableListOf<Content>()

    init {
        // Initial greeting
        _uiState.update { 
            it.copy(messages = listOf(Message("Hello, I am Jarvis. How can I assist you today?", false)))
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMessage = Message(text, true)
        _uiState.update { 
            it.copy(
                messages = it.messages + userMessage,
                isLoading = true,
                error = null
            )
        }

        conversationHistory.add(Content(listOf(Part(text))))

        viewModelScope.launch {
            try {
                val response = RetrofitClient.service.generateContent(
                    apiKey = BuildConfig.GEMINI_API_KEY,
                    request = GenerateContentRequest(
                        contents = conversationHistory,
                        systemInstruction = Content(listOf(Part("You are Jarvis, a highly intelligent and helpful AI assistant. Your tone is professional, sophisticated, and loyal, like the AI from Iron Man. Keep responses concise but helpful. Speak in English unless addressed in another language.")))
                    )
                )

                val aiResponseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "I'm sorry, I couldn't process that."
                
                conversationHistory.add(Content(listOf(Part(aiResponseText))))

                _uiState.update { 
                    it.copy(
                        messages = it.messages + Message(aiResponseText, false),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to communicate with Jarvis: ${e.message}"
                    )
                }
            }
        }
    }
}
