package com.example.anonymousChat.myMemory

interface TranslationCallback {
    fun onTranslationResult(translatedText: String)
    fun onTranslationError(errorMessage: String)
}