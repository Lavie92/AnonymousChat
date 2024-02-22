package com.example.AnonymousChat.exchanges

import android.util.Log
import okhttp3.*
import okhttp3.ConnectionSpec
import okhttp3.TlsVersion
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object HttpProvider {
    private val MODERN_TLS = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
        .tlsVersions(TlsVersion.TLS_1_2)
        .cipherSuites(
            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
            CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
            CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256
        )
        .build()

    fun sendPost(URL: String?, formBody: RequestBody?): JSONObject? {
        var data: JSONObject? = JSONObject()
        try {
            val spec: ConnectionSpec = ConnectionSpec.Builder(MODERN_TLS)
                .tlsVersions(TlsVersion.TLS_1_2)
                .cipherSuites(
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                    CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256
                )
                .build()
            val client: OkHttpClient = OkHttpClient.Builder()
                .connectionSpecs(listOf(spec))
                .callTimeout(5000, TimeUnit.MILLISECONDS)
                .build()
            val request: Request = Request.Builder()
                .url(URL!!)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .post(formBody!!)
                .build()
            val response = client.newCall(request).execute()
            data = if (!response.isSuccessful) {
                Log.println(Log.ERROR, "BAD_REQUEST", response.body!!.string())
                null
            } else {
                JSONObject(response.body!!.string())
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return data
    }
}
