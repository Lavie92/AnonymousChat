package com.example.anonymousChat.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.anonymousChat.model.Message
import com.example.anonymousChat.repository.ChatRepository
import com.example.anonymousChat.repository.IChatRepository
import com.example.anonymousChat.service.ChatService
import kotlinx.coroutines.launch

class ChatViewModel(private val chatRepository: IChatRepository = ChatRepository(ChatService()), chatType: String) : ViewModel() {
    private val _messageLiveData = MutableLiveData<List<Message>>()
    private val messageLiveData: LiveData<List<Message>> get() = _messageLiveData
    fun observeMessageLiveData() : LiveData<List<Message>> {
        return messageLiveData
    }
    fun sendMessage(chatRoomId: String, chatType: String, senderId: String, receiverId: String, content: String) {
        viewModelScope.launch {
            chatRepository.sendMessage(chatRoomId, chatType, senderId, receiverId, content)
        }
    }
    fun loadMessageAsync(chatRoomId: String, chatType: String) {
        viewModelScope.launch {
            chatRepository.loadMessageSync(chatRoomId, chatType = chatType) { messages ->
                _messageLiveData.postValue(messages)
            }
        }
    }

}
