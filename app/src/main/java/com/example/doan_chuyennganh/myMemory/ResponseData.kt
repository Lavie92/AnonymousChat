package com.example.doan_chuyennganh.myMemory

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class ResponseData(val translatedText: String)

object TranslationApiClient {
    private const val BASE_URL = "https://mymemory.translated.net/api/"
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val translationService: TranslationService = retrofit.create(TranslationService::class.java)
}
