package com.example.AnonymousChat.myMemory

interface TranslationCallback {
    fun onTranslationResult(translatedText: String)
    fun onTranslationError(errorMessage: String)
}