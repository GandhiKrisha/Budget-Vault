package com.teamvault.budgetvault

import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

// Data classes for Gemini API
data class GeminiRequest(
    val contents: List<Content>
)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String
)

data class GeminiResponse(
    val candidates: List<Candidate>?
)

data class Candidate(
    val content: Content?
)

// Retrofit API interface with CORRECT model name
interface GeminiApiService {
    @POST("v1beta/models/gemini-1.5-flash:generateContent")  // Model name
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

// Gemini service with correct model
class GeminiService {
    companion object {
        private const val API_KEY = "AIzaSyDEJH2ZBbomssWaM2D9C1bX4ep4TOl6QBk"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/"
        private const val MODEL_NAME = "gemini-1.5-flash"  // FIXED: Updated model name
        private const val TAG = "GeminiService"
    }

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d("HTTP_LOG", message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(GeminiApiService::class.java)

    suspend fun getChatResponse(userMessage: String): String {
        Log.d(TAG, "=== STARTING API CALL ===")
        Log.d(TAG, "User message: $userMessage")
        Log.d(TAG, "Using model: $MODEL_NAME")
        Log.d(TAG, "API Key (first 10 chars): ${API_KEY.take(10)}...")

        return try {
            // Validate API key format
            if (!isValidApiKey()) {
                Log.e(TAG, "❌ Invalid API key format!")
                return "❌ **API Configuration Issue**\n\nThe API key format is invalid."
            }

            Log.d(TAG, "✅ API key format is valid")

            // Create enhanced system prompt for better financial advice
            val systemPrompt = """
                You are BudgetVault AI, a helpful and knowledgeable financial advisor assistant for the BudgetVault mobile app.
                
                Your role is to provide practical, actionable financial advice including:
                • Personal budgeting strategies (50/30/20 rule, zero-based budgeting)
                • Savings and emergency fund guidance
                • Debt management (snowball vs avalanche methods)
                • Basic investment advice for beginners
                • Expense tracking and analysis tips
                • Financial goal setting and planning
                
                Guidelines for responses:
                • Keep responses practical and actionable
                • Use bullet points with • symbol for lists
                • Include relevant emojis (💰 📊 💡 🎯 📈 🚨)
                • Be encouraging and supportive
                • Ask the user the currency they want to use
                • Provide specific examples with realistic currency based on user's currency selection
                • Keep responses between 150-400 words
                • Ask follow-up questions when helpful
                • Focus on helping users build wealth and financial security
                
                If asked about non-financial topics, politely redirect to financial advice.
            """.trimIndent()

            val request = GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part("$systemPrompt\n\nUser question: $userMessage\n\nProvide helpful financial advice:")
                        )
                    )
                )
            )

            Log.d(TAG, "📤 Sending request to Gemini API...")
            Log.d(TAG, "Request URL: ${BASE_URL}v1beta/models/$MODEL_NAME:generateContent")

            val response = apiService.generateContent(API_KEY, request)

            Log.d(TAG, "📥 Received response from API")
            Log.d(TAG, "Response candidates count: ${response.candidates?.size ?: 0}")

            if (response.candidates.isNullOrEmpty()) {
                Log.e(TAG, "❌ No candidates in response")
                return "🤖 I received an empty response from the AI. Let me try to help with some general advice instead!\n\nWhat specific financial topic would you like guidance on?"
            }

            val firstCandidate = response.candidates.first()
            if (firstCandidate.content == null) {
                Log.e(TAG, "❌ No content in first candidate")
                return "🤖 The AI response was filtered. Could you try rephrasing your question?"
            }

            val parts = firstCandidate.content.parts
            if (parts.isNullOrEmpty()) {
                Log.e(TAG, "❌ No parts in content")
                return "🤖 I'm having trouble generating a response. What financial topic can I help you with?"
            }

            val aiResponse = parts.firstOrNull()?.text
            if (aiResponse.isNullOrBlank()) {
                Log.e(TAG, "❌ Empty text in first part")
                return "🤖 I got an empty response. Let me help you with some financial advice anyway! What would you like to know about budgeting, saving, or investing?"
            }

            Log.d(TAG, "✅ Successfully got response")
            Log.d(TAG, "Response length: ${aiResponse.length} characters")
            Log.d(TAG, "Response preview: ${aiResponse.take(100)}...")
            Log.d(TAG, "=== API CALL COMPLETED SUCCESSFULLY ===")

            return aiResponse.trim()

        } catch (e: retrofit2.HttpException) {
            val errorCode = e.code()
            val errorBody = e.response()?.errorBody()?.string()

            Log.e(TAG, "🚨 HTTP Exception occurred")
            Log.e(TAG, "Error code: $errorCode")
            Log.e(TAG, "Error body: $errorBody")

            return when (errorCode) {
                400 -> "❌ **Request Error**\n\nThere was an issue with the request format. Please try rephrasing your question."
                401 -> "🔑 **Authentication Error**\n\nAPI key is invalid. Please check the configuration."
                403 -> "🚫 **Access Denied**\n\nAPI quota exceeded or access denied. Please try again later."
                404 -> "🔍 **Model Not Found**\n\nThe AI model is not available. This is a configuration issue."
                429 -> "⏰ **Rate Limited**\n\nToo many requests. Please wait a moment and try again."
                500, 502, 503 -> "🛠️ **Server Error**\n\nGoogle's servers are having issues. Please try again in a few minutes."
                else -> "🌐 **Connection Error**\n\nThere was a network issue. Please check your internet connection and try again."
            }

        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "🌐 Network error: ${e.message}")
            return "📱 **No Internet Connection**\n\nCannot reach the AI servers. Please check your internet connection."

        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "⏱️ Timeout error: ${e.message}")
            return "⏱️ **Request Timeout**\n\nThe request took too long. Please try again."

        } catch (e: Exception) {
            Log.e(TAG, "💥 Unexpected error occurred")
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Exception message: ${e.message}")
            e.printStackTrace()

            return "🤖 I'm having technical difficulties right now, but I can still help! What financial topic would you like advice on? I can help with budgeting, saving, debt management, or investing basics."
        }
    }

    fun isValidApiKey(): Boolean {
        val isValid = API_KEY.isNotBlank() &&
                API_KEY.startsWith("AIzaSy") &&
                API_KEY.length >= 35

        Log.d(TAG, "API key validation: $isValid (length: ${API_KEY.length})")
        return isValid
    }

    suspend fun testConnection(): String {
        Log.d(TAG, "🧪 Testing API connection with model: $MODEL_NAME...")
        return try {
            val response = getChatResponse("Hello, this is a test. Please respond with a brief financial tip.")
            if (response.contains("❌") || response.contains("💥") || response.contains("🔍")) {
                "❌ Connection test failed:\n$response"
            } else {
                "✅ **Connection Test Successful!**\n\nThe Gemini AI is working properly. Model: $MODEL_NAME"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Connection test exception: ${e.message}")
            "❌ Connection test failed: ${e.message}"
        }
    }
}