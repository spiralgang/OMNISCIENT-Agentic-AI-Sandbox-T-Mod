package com.example.ai

import android.util.Base64
import com.example.BuildConfig

class VisionAgentModule(private val geminiApiService: GeminiApiService) {
    suspend fun analyzeScreenshot(base64Image: String, prompt: String): String {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey.contains("MY_GEMINI_API_KEY")) {
                return "Error: Missing API Key"
            }

            val request = GenerateContentRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = prompt),
                            Part(
                                inlineData = InlineData(
                                    mimeType = "image/png",
                                    data = base64Image
                                )
                            )
                        )
                    )
                )
            )

            val response = geminiApiService.generateContent("gemini-3.5-flash", apiKey, request)
            return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No valid response."
        } catch (e: Exception) {
            e.printStackTrace()
            return "Vision Analysis Error: ${e.message}"
        }
    }
}
