package com.liganma.chatmaster.utils

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.annotation.JSONField
import com.liganma.chatmaster.DEFAULT_OPENAI_BASE_URL
import com.liganma.chatmaster.DEFAULT_OPENAI_MODEL
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

val SK_REGEX = Regex("^sk-[a-zA-Z0-9]{10,}$")

private val JSON_MEDIA_TYPE = "application/json".toMediaType()

class OpenAiException(message: String, cause: Throwable? = null) : Exception(message, cause)

class OkClient(
    private val accessKey: String,
    baseUrl: String = DEFAULT_OPENAI_BASE_URL
) {

    private val baseUrl: String = baseUrl.takeIf { it.isNotBlank() }?.trimEnd('/') ?: DEFAULT_OPENAI_BASE_URL

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun createChatCompletion(request: ChatCompletionRequest): ChatCompletionResponse {
        val jsonString = JSON.toJSONString(request)

        val httpRequest = Request.Builder()
            .header("Authorization", "Bearer $accessKey")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .url("$baseUrl/chat/completions")
            .post(jsonString.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(httpRequest).execute().use { response ->
            val body = response.body?.string()
            if (response.isSuccessful && body != null) {
                return JSON.parseObject(body, ChatCompletionResponse::class.java)
            }
            val errorMessage = body?.let { extractErrorMessage(it) } ?: "请求失败，HTTP ${response.code}"
            throw OpenAiException(errorMessage)
        }
    }

    fun verifyCredentials() {
        val request = Request.Builder()
            .header("Authorization", "Bearer $accessKey")
            .header("Accept", "application/json")
            .url("$baseUrl/models")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                return
            }
            val body = response.body?.string()
            val errorMessage = body?.let { extractErrorMessage(it) } ?: "验证失败，HTTP ${response.code}"
            throw OpenAiException(errorMessage)
        }
    }

    private fun extractErrorMessage(body: String): String {
        return try {
            val json = JSON.parseObject(body)
            json.getJSONObject("error")?.getString("message") ?: body
        } catch (e: Exception) {
            body
        }
    }
}

data class ChatCompletionRequest(
    val model: String = DEFAULT_OPENAI_MODEL,
    val messages: List<OpenAiMessage>,
    @JSONField(name = "response_format")
    val responseFormat: OpenAiResponseFormat? = null,
    val temperature: Double? = null
)

data class OpenAiResponseFormat(
    val type: String
)

data class OpenAiMessage(
    val role: String,
    val content: String
)

data class ChatCompletionResponse(
    val id: String? = null,
    val choices: List<ChatCompletionChoice> = emptyList(),
    val usage: Usage? = null
)

data class ChatCompletionChoice(
    val index: Int? = null,
    val message: OpenAiMessage = OpenAiMessage(role = "", content = ""),
    @JSONField(name = "finish_reason")
    val finishReason: String? = null
)

data class Usage(
    @JSONField(name = "prompt_tokens")
    val promptTokens: Int? = null,
    @JSONField(name = "completion_tokens")
    val completionTokens: Int? = null,
    @JSONField(name = "total_tokens")
    val totalTokens: Int? = null
)
