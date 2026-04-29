package com.jian.nemo.core.data.util

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.jian.nemo.core.domain.model.AIExercise
import com.jian.nemo.core.domain.model.AIGradeResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIClient @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateExercise(
        platform: String,
        apiKey: String,
        baseUrl: String?,
        model: String,
        difficulty: String
    ): Result<AIExercise> {
        val systemPrompt = """
            你是一位资深的日语教育专家。请根据指定的日语等级 ($difficulty) 生成一道具有挑战性且实用的翻译练习题。
            
            难度要求：
            - N5: 涵盖基础助词、判断句、动词基本分类，贴近初级生活场景。
            - N4: 涵盖基础敬语、授受关系、可能态/意志态，涉及日常社交。
            - N3: 涵盖中级语法（如被动、使役、假定），涉及社会新闻、个人观点表达。
            - N2: 涵盖复杂书面语、商务场景、抽象概念讨论，要求用词精准、地道。
            - N1: 涵盖专业领域、文学性表达、微妙的语气差别及高度抽象的话题，极具挑战性。
            
            严禁行为：
            1. 严禁生成过于简单的问候语或基础自我介绍（如“我是学生”）。
            2. 严禁生成不符合 $difficulty 等级词汇量要求的题目。
            
            请返回如下 JSON 格式：
            {
              "question": "题目内容",
              "type": "CN_TO_JP" 或 "JP_TO_CN",
              "difficulty": "$difficulty",
              "answer": "地道的标准答案",
              "hints": ["关键语法点提示", "难点词汇提示"]
            }
            直接返回 JSON 字符串，不要包含 Markdown 格式块或其他文字。
        """.trimIndent()
        
        val url = getApiUrl(platform, baseUrl)
        val requestBody = buildChatCompletionRequest(model, systemPrompt, "生成一道练习题", platform)
        
        return try {
            val response = executeRequest(url, apiKey, requestBody)
            val content = extractContentFromChatResponse(response)
            val exercise = json.decodeFromString<AIExercise>(content)
            Result.success(exercise)
        } catch (e: Exception) {
            Log.e("AIClient", "生成题目失败", e)
            Result.failure(e)
        }
    }

    suspend fun gradeAnswer(
        platform: String,
        apiKey: String,
        baseUrl: String?,
        model: String,
        exercise: AIExercise,
        userAnswer: String
    ): Result<AIGradeResult> {
        val systemPrompt = """
            你是一位资深的日语教育专家。请对用户的翻译练习进行专业评分。
            题目：${exercise.question}
            标准参考答案：${exercise.answer}
            用户提交的答案：$userAnswer
            
            评分标准：
            1. 语法准确性 (Grammar)
            2. 用词地道程度 (Lexical Choice)
            3. 语境适配度 (Contextual Accuracy)
            
            请返回如下 JSON 格式：
            {
              "score": 0-100,
              "feedback": "详细的点评，请包含：1. 优点；2. 改进建议（如有）；3. 相关的语法点解析。请使用中文回复。",
              "is_correct": true/false
            }
            直接返回 JSON 字符串。
        """.trimIndent()
        
        val url = getApiUrl(platform, baseUrl)
        val requestBody = buildChatCompletionRequest(model, systemPrompt, "对我提交的答案进行评分", platform)
        
        return try {
            val response = executeRequest(url, apiKey, requestBody)
            val content = extractContentFromChatResponse(response)
            val grade = json.decodeFromString<AIGradeResult>(content)
            Result.success(grade)
        } catch (e: Exception) {
            Log.e("AIClient", "评分失败", e)
            Result.failure(e)
        }
    }

    private fun getApiUrl(platform: String, baseUrl: String?): String {
        if (!baseUrl.isNullOrBlank()) return if (baseUrl.endsWith("/")) "${baseUrl}chat/completions" else "$baseUrl/chat/completions"
        
        return when (platform) {
            "openai" -> "https://api.openai.com/v1/chat/completions"
            "deepseek" -> "https://api.deepseek.com/chat/completions"
            "claude" -> "https://api.anthropic.com/v1/messages" // Claude has a different API structure, but many use OpenAI compatible proxies
            "gemini" -> "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent" // Gemini also different
            else -> "https://api.openai.com/v1/chat/completions"
        }
    }

    private fun buildChatCompletionRequest(model: String, systemPrompt: String, userPrompt: String, platform: String): String {
        val effectiveModel = if (model.isBlank()) {
            when (platform) {
                "deepseek" -> "deepseek-v4-flash"
                "openai" -> "gpt-3.5-turbo"
                "gemini" -> "gemini-pro"
                else -> "gpt-3.5-turbo"
            }
        } else model

        // Default to OpenAI format as most platforms (DeepSeek, Doubao, etc.) support it
        return """
            {
                "model": "$effectiveModel",
                "messages": [
                    {"role": "system", "content": ${Json.encodeToString(systemPrompt)}},
                    {"role": "user", "content": ${Json.encodeToString(userPrompt)}}
                ],
                "temperature": 0.7
            }
        """.trimIndent()
    }

    private suspend fun executeRequest(url: String, apiKey: String, bodyJson: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(bodyJson.toRequestBody(mediaType))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("API 请求失败: ${response.code} ${response.message}")
            response.body?.string() ?: throw Exception("响应体为空")
        }
    }

    private fun extractContentFromChatResponse(responseJson: String): String {
        // Parse OpenAI style response
        val root = json.parseToJsonElement(responseJson)
        val choices = root.asJsonObject()["choices"]?.asJsonArray()
        val content = choices?.get(0)?.asJsonObject()?.get("message")?.asJsonObject()?.get("content")?.asJsonPrimitive()?.content
        
        return content?.trim()?.removePrefix("```json")?.removeSuffix("```")?.trim() ?: throw Exception("无法解析 AI 响应内容")
    }
    
    // Helper extensions for JsonElement to keep it simple without full serialization of the response wrapper
    private fun kotlinx.serialization.json.JsonElement.asJsonObject() = this as? kotlinx.serialization.json.JsonObject ?: throw Exception("Not an object")
    private fun kotlinx.serialization.json.JsonElement.asJsonArray() = this as? kotlinx.serialization.json.JsonArray ?: throw Exception("Not an array")
    private fun kotlinx.serialization.json.JsonElement.asJsonPrimitive() = this as? kotlinx.serialization.json.JsonPrimitive ?: throw Exception("Not a primitive")
}
