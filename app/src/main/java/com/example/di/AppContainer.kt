package com.example.di

import android.app.Application
import com.example.ai.AICollabManager
import com.example.ai.GeminiApiService
import com.example.db.AppDatabase
import com.example.db.DatabaseProvider
import com.example.vfs.VirtualFileSystem
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class AppContainer(application: Application) {
    val database: AppDatabase by lazy {
        DatabaseProvider.getDatabase(application)
    }

    val vfs: VirtualFileSystem by lazy {
        VirtualFileSystem()
    }

    val geminiApiService: GeminiApiService by lazy {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
            
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    val aiCollabManager: AICollabManager by lazy {
        AICollabManager(geminiApiService)
    }
}
