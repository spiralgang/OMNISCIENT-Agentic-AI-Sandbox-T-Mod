package com.example.ai

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class NvidiaChatRequest(
    val model: String,
    val messages: List<NvidiaMessage>,
    val max_tokens: Int = 1024
)

@JsonClass(generateAdapter = true)
data class NvidiaMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class NvidiaChatResponse(
    val choices: List<NvidiaChoice>?
)

@JsonClass(generateAdapter = true)
data class NvidiaChoice(
    val message: NvidiaMessage?
)

interface NvidiaApiService {
    // Conceptual endpoint for build.nvidia.com LLMs (e.g., Llama 3 or Nemotron provided via NVIDIA)
    @POST("v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authHeader: String,
        @Body request: NvidiaChatRequest
    ): NvidiaChatResponse
}

object NvidiaRetrofitClient {
    private const val BASE_URL = "https://integrate.api.nvidia.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: NvidiaApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(NvidiaApiService::class.java)
    }
}
