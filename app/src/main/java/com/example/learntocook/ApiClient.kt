package com.example.learntocook

import android.content.Context
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

object ApiClient {
    private const val BASE_URL = "https://a847f1ec5e2d.ngrok-free.app/api"
    private val client = OkHttpClient()

    fun makeRequest(
        context: Context,
        endpoint: String,
        method: String = "GET",
        body: String? = null,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = UserManager.getCurrentUserId(context)
        if (userId == null) {
            onError("User not authenticated")
            return
        }

        val url = "$BASE_URL$endpoint"
        val requestBuilder = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $userId")

        when (method.uppercase()) {
            "POST" -> {
                val requestBody = body?.toRequestBody("application/json".toMediaType())
                requestBuilder.post(requestBody ?: "".toRequestBody("application/json".toMediaType()))
            }
            "PUT" -> {
                val requestBody = body?.toRequestBody("application/json".toMediaType())
                requestBuilder.put(requestBody ?: "".toRequestBody("application/json".toMediaType()))
            }
            "DELETE" -> {
                requestBuilder.delete()
            }
            else -> {
                requestBuilder.get()
            }
        }

        val request = requestBuilder.build()
        Log.d("ApiClient", "Making $method request to: $url")
        Log.d("ApiClient", "Request headers: ${request.headers}")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ApiClient", "Request failed", e)
                onError("Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    Log.d("ApiClient", "Response code: ${response.code}")
                    Log.d("ApiClient", "Response headers: ${response.headers}")
                    
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        Log.d("ApiClient", "Response body: $responseBody")
                        if (responseBody != null) {
                            onSuccess(responseBody)
                        } else {
                            onError("Empty response")
                        }
                    } else {
                        val errorBody = response.body?.string() ?: "Unknown error"
                        Log.e("ApiClient", "API error: ${response.code} - $errorBody")
                        onError("API error: ${response.code} - $errorBody")
                    }
                }
            }
        })
    }


    fun makePublicRequest(
        endpoint: String,
        method: String = "GET",
        body: String? = null,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val url = "$BASE_URL$endpoint"
        val requestBuilder = Request.Builder()
            .url(url)

        when (method.uppercase()) {
            "POST" -> {
                val requestBody = body?.toRequestBody("application/json".toMediaType())
                requestBuilder.post(requestBody ?: "".toRequestBody("application/json".toMediaType()))
            }
            "PUT" -> {
                val requestBody = body?.toRequestBody("application/json".toMediaType())
                requestBuilder.put(requestBody ?: "".toRequestBody("application/json".toMediaType()))
            }
            "DELETE" -> {
                requestBuilder.delete()
            }
            else -> {
                requestBuilder.get()
            }
        }

        val request = requestBuilder.build()
        Log.d("ApiClient", "Making public $method request to: $url")
        Log.d("ApiClient", "Public request headers: ${request.headers}")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ApiClient", "Public request failed", e)
                onError("Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    Log.d("ApiClient", "Public response code: ${response.code}")
                    Log.d("ApiClient", "Public response headers: ${response.headers}")
                    
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        Log.d("ApiClient", "Public response body: $responseBody")
                        if (responseBody != null) {
                            onSuccess(responseBody)
                        } else {
                            onError("Empty response")
                        }
                    } else {
                        val errorBody = response.body?.string() ?: "Unknown error"
                        Log.e("ApiClient", "Public API error: ${response.code} - $errorBody")
                        onError("API error: ${response.code} - $errorBody")
                    }
                }
            }
        })
    }
} 