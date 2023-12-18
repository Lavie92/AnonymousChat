package com.example.doan_chuyennganh.myMemory

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface TranslationService {
    @GET("get")
    fun translate(
        @Query("q") text: String,
        @Query("langpair") langPair: String,
        @Query("key") apiKey: String
    ): Call<TranslationResponse>
}
