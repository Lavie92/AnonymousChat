package com.example.doan_chuyennganh.myMemory

interface TranslationCallback {
    fun onTranslationResult(translatedText: String)
    fun onTranslationError(errorMessage: String)
}