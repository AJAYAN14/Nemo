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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import com.jian.nemo.core.domain.model.AIExercise
import com.jian.nemo.core.domain.model.AIGradeResult
import com.jian.nemo.core.domain.model.AIReadingArticle
import com.jian.nemo.core.domain.model.AIWordTranslation
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIClient @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * 语法专项模式下传递给 AI 的语法信息
     */
    data class GrammarInfo(
        val name: String,          // 语法名称，如 "～あがる"
        val connection: String,    // 接续方式，如 "动词「ます形」＋あがる"
        val explanation: String,   // 含义说明
        val subtype: String?,      // 用法子类型，如 "完成"
        val notes: String?         // 注意事项
    )

    suspend fun generateExercise(
        platform: String,
        apiKey: String,
        baseUrl: String?,
        model: String,
        difficulty: String,
        grammarInfo: GrammarInfo? = null,
        exerciseType: String = if (kotlin.random.Random.nextBoolean()) "CN_TO_JP" else "JP_TO_CN"
    ): Result<AIExercise> {
        val systemPrompt = if (grammarInfo != null) {
            buildGrammarExercisePrompt(difficulty, grammarInfo, exerciseType)
        } else {
            buildFreeExercisePrompt(difficulty, exerciseType)
        }
        
        val url = getApiUrl(platform, baseUrl, apiKey, model)
        val requestBody = buildChatRequest(platform, model, systemPrompt, "生成一道练习题")
        
        return try {
            val response = executeRequest(platform, url, apiKey, requestBody)
            val content = extractContentFromChatResponse(platform, response)
            val cleanJson = extractJsonFromString(content)
            val exercise = json.decodeFromString<AIExercise>(cleanJson)
            Result.success(exercise)
        } catch (e: Exception) {
            Log.e("AIClient", "生成题目失败", e)
            Result.failure(e)
        }
    }

    /**
     * 自由模式出题 Prompt（保持原有逻辑）
     */
    private fun buildFreeExercisePrompt(difficulty: String, exerciseType: String): String {
        val typeDesc = if (exerciseType == "CN_TO_JP") {
            "CN_TO_JP（中译日）。你【必须】出一句纯中文题目，让用户翻译成日语。生成的标准参考答案中应包含地道的日文表述。"
        } else {
            "JP_TO_CN（日译中）。你【必须】出一句纯日语题目（不得带中括号发音注音），让用户翻译成中文。生成的标准参考答案为对应的地道中文。"
        }

        return """
            你是一位资深的日语教育专家。请根据指定的日语等级 ($difficulty) 生成一道具有挑战性且实用的翻译练习题。
            
            难度要求：
            - N5: 涵盖基础助词、判断句、动词基本分类，贴近初级生活场景。
            - N4: 涵盖基础敬语、授受关系、可能态/意志态，涉及日常社交。
            - N3: 涵盖中级语法（如被动、使役、假定），涉及社会新闻、个人观点表达。
            - N2: 涵盖复杂书面语、商务场景、抽象概念讨论，要求用词精准、地道。
            - N1: 涵盖专业领域、文学性表达、微妙的语气差别及高度抽象的话题，极具挑战性。
            
            严禁行为：
            1. 严禁生成过于简单的问候语或基础自我介绍（如"我是学生"）。
            2. 严禁生成不符合 $difficulty 等级词汇量要求的题目。
            
            【模式考察硬性命令】
            本题你【必须且只能】生成该类型：$typeDesc
            
            请返回如下 JSON 格式：
            {
              "question": "题目内容",
              "type": "$exerciseType",
              "difficulty": "$difficulty",
              "answer": "地道的标准答案",
              "hints": ["关键语法点提示", "难点词汇提示"]
            }
            直接返回 JSON 字符串，不要包含 Markdown 格式块或其他文字。
        """.trimIndent()
    }

    /**
     * 语法专项模式出题 Prompt
     *
     * 核心策略：
     * 1. 将数据库中的语法详细信息（名称、接续、含义、注意事项）注入 Prompt
     * 2. 强制要求标准答案必须使用指定语法点
     * 3. 不发送数据库中的例句，要求 AI 完全原创
     */
    private fun buildGrammarExercisePrompt(difficulty: String, info: GrammarInfo, exerciseType: String): String {
        val subtypeInfo = if (!info.subtype.isNullOrBlank()) "用法分类：${info.subtype}" else ""
        val notesInfo = if (!info.notes.isNullOrBlank()) "注意事项：${info.notes}" else ""
        val typeDesc = if (exerciseType == "CN_TO_JP") {
            "CN_TO_JP（中译日）。你【必须】出一句纯中文题目，让用户翻译成日语。标准参考答案中【必须】正确使用且包含语法点「${info.name}」。"
        } else {
            "JP_TO_CN（日译中）。你【必须】出一句纯日语题目（该日语句子中必须包含「${info.name}」语法点），让用户翻译成中文。"
        }

        return """
            你是一位资深的日语教育专家。现在进入【语法专项训练模式】。
            
            你需要基于以下语法规则，为 $difficulty 等级的学习者生成一道翻译练习题。
            
            ═══════════ 语法详细信息 ═══════════
            语法名称：${info.name}
            接续方式：${info.connection}
            含义说明：${info.explanation}
            $subtypeInfo
            $notesInfo
            ═══════════════════════════════════
            
            【出题要求】
            1. 生成的题目句子必须自然、实用，符合 $difficulty 等级的词汇和表达水平。
            2. 标准答案中【必须】正确使用上述语法点「${info.name}」。
            3. 如果该语法有特定的接续规则，标准答案必须严格遵守.
            4. 严禁使用同义语法或其他表达方式替代「${info.name}」。
            5. 严禁直接照搬教科书中的常见例句，必须原创。
            
            【模式考察硬性命令】
            本题你【必须且只能】生成该类型：$typeDesc
            
            【提示内容】
            hints 中请给出与该语法点相关的关键接续提示和语义提示，帮助用户回忆该语法的用法。
            
            请返回如下 JSON 格式：
            {
              "question": "题目内容",
              "type": "$exerciseType",
              "difficulty": "$difficulty",
              "answer": "地道的标准答案（必须包含「${info.name}」）",
              "hints": ["接续提示", "语义提示"]
            }
            直接返回 JSON 字符串，不要包含 Markdown 格式块或其他文字。
        """.trimIndent()
    }

    suspend fun gradeAnswer(
        platform: String,
        apiKey: String,
        baseUrl: String?,
        model: String,
        exercise: AIExercise,
        userAnswer: String,
        grammarInfo: GrammarInfo? = null
    ): Result<AIGradeResult> {
        val grammarSection = if (grammarInfo != null) {
            if (exercise.type == "CN_TO_JP") {
                """
                
                【语法专项评分附加要求】
                本题为「中翻日」专项训练，指定日语语法点为「${grammarInfo.name}」。
                含义：${grammarInfo.explanation}
                接续：${grammarInfo.connection}
                
                评分时必须额外检查：
                - 用户是否在日语回答中正确使用了「${grammarInfo.name}」
                - 日语的接续方式是否正确
                - 如果用户在日语作答中完全没有使用该语法点，即使意思正确，也应大幅扣分（最高不超过 40 分）
                - 在 feedback 中必须专门点评用户对「${grammarInfo.name}」的使用情况
                """.trimIndent()
            } else {
                """
                
                【语法专项评分附加要求】
                本题为「日翻中」专项训练，题目本身已包含了日语语法点「${grammarInfo.name}」（含义：${grammarInfo.explanation}）。
                用户的任务是把这句带有该语法点的日语准确、地道地翻译成【中文】。
                
                评分时请特别注意：
                - 用户的作答是中文，绝对【不需要】也不应该在中文回答中包含该日语语法点「${grammarInfo.name}」！
                - 评分的核心标准是：用户是否准确地翻译出了「${grammarInfo.name}」在原句中所表达的语气、语气强度和语义。
                - 如果用户翻译的意思完全正确，千万不要以“未使用该日语语法点”为由扣分。
                - 在 feedback 中请点评用户对该语法点在中文翻译中的语义还原准确度。
                """.trimIndent()
            }
        } else ""

        val systemPrompt = """
            你是一位资深的日语教育专家。请对用户的翻译练习进行专业评分。
            题目：${exercise.question}
            标准参考答案：${exercise.answer}
            用户提交的答案：$userAnswer
            
            评分标准：
            1. 语法准确性 (Grammar)
            2. 用词地道程度 (Lexical Choice)
            3. 语境适配度 (Contextual Accuracy)
            $grammarSection
            
            请返回如下 JSON 格式：
            {
              "score": 0-100,
              "feedback": "详细的点评，请包含：1. 优点；2. 改进建议（如有）；3. 相关的语法点解析。请使用中文回复。",
              "is_correct": true/false,
              "standard_answer": "地道的标准参考答案"
            }
            直接返回 JSON 字符串。
        """.trimIndent()
        
        val url = getApiUrl(platform, baseUrl, apiKey, model)
        val requestBody = buildChatRequest(platform, model, systemPrompt, "对我提交的答案进行评分")
        
        return try {
            val response = executeRequest(platform, url, apiKey, requestBody)
            val content = extractContentFromChatResponse(platform, response)
            val cleanJson = extractJsonFromString(content)
            val grade = json.decodeFromString<AIGradeResult>(cleanJson)
            Result.success(grade)
        } catch (e: Exception) {
            Log.e("AIClient", "评分失败", e)
            Result.failure(e)
        }
    }

    private fun getApiUrl(platform: String, baseUrl: String?, apiKey: String, model: String): String {
        val cleanModel = model.ifBlank {
            if (platform == "gemini") "gemini-3.1-pro-preview" else "gpt-5.5"
        }.removePrefix("models/")

        if (platform == "gemini") {
            val base = baseUrl?.takeIf { it.isNotBlank() }?.removeSuffix("/") 
                ?: "https://generativelanguage.googleapis.com/v1beta"
            return "$base/models/$cleanModel:generateContent?key=$apiKey"
        }
        
        val effectiveBaseUrl = baseUrl?.takeIf { it.isNotBlank() }
        
        if (effectiveBaseUrl != null) {
            val base = if (effectiveBaseUrl.endsWith("/")) effectiveBaseUrl else "$effectiveBaseUrl/"
            return "${base}chat/completions"
        }
        
        return when (platform) {
            "openai" -> "https://api.openai.com/v1/chat/completions"
            "deepseek" -> "https://api.deepseek.com/chat/completions"
            "doubao" -> "https://ark.cn-beijing.volces.com/api/v3/chat/completions"
            "mimo" -> "https://api.xiaomimimo.com/v1/chat/completions"
            else -> "https://api.openai.com/v1/chat/completions"
        }
    }

    private fun buildChatRequest(
        platform: String,
        model: String,
        systemPrompt: String,
        userPrompt: String
    ): String {
        val effectiveModel = model.ifBlank {
            when (platform) {
                "deepseek" -> "deepseek-v4-pro"
                "gemini" -> "gemini-3.1-pro-preview"
                else -> "gpt-5.5"
            }
        }

        return if (platform == "gemini") {
            // 原生 Gemini 格式
            val geminiModel = if (model.isBlank()) "gemini-3.1-pro-preview" else model
            val useThinking = geminiModel.contains("gemini-3")
            
            """
            {
                "contents": [
                    {
                        "role": "user",
                        "parts": [
                            {"text": ${Json.encodeToString(systemPrompt + "\n\n" + userPrompt)}}
                        ]
                    }
                ],
                "generationConfig": {
                    "temperature": 0.7,
                    ${if (useThinking) "\"thinkingConfig\": { \"thinkingLevel\": \"high\" }," else ""}
                    "topP": 0.95
                }
            }
            """.trimIndent()
        } else {
            // 标准 OpenAI 格式 (兼容 DeepSeek 等)
            """
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
    }

    private suspend fun executeRequest(platform: String, url: String, apiKey: String, bodyJson: String): String = withContext(Dispatchers.IO) {
        val builder = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
        
        if (platform != "gemini") {
            builder.header("Authorization", "Bearer $apiKey")
        }

        val request = builder.post(bodyJson.toRequestBody(mediaType)).build()

        okHttpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                response.body?.string() ?: throw Exception("响应体为空")
            } else {
                val errorBody = response.body?.string()
                val errorMsg = if (response.code == 404) {
                    "模型不存在 (404): 请检查模型名称是否正确，建议尝试在末尾带上 -preview 后缀（如 gemini-3-flash-preview）。"
                } else {
                    "API 请求失败: ${response.code}\n$errorBody"
                }
                throw Exception(errorMsg)
            }
        }
    }

    private fun extractContentFromChatResponse(platform: String, responseJson: String): String {
        val root = json.parseToJsonElement(responseJson)
        
        return if (platform == "gemini") {
            // 解析原生 Gemini 响应
            val candidates = root.asJsonObject()["candidates"]?.asJsonArray()
            val text = candidates?.get(0)?.asJsonObject()?.get("content")
                ?.asJsonObject()?.get("parts")?.asJsonArray()?.get(0)
                ?.asJsonObject()?.get("text")?.asJsonPrimitive()?.content
            text?.trim() ?: throw Exception("无法解析 Gemini 响应内容")
        } else {
            // 解析 OpenAI 风格响应
            val choices = root.asJsonObject()["choices"]?.asJsonArray()
            val content = choices?.get(0)?.asJsonObject()?.get("message")?.asJsonObject()?.get("content")?.asJsonPrimitive()?.content
            content?.trim() ?: throw Exception("无法解析 OpenAI 风格响应内容")
        }
        .removePrefix("```json")
        .removeSuffix("```")
        .trim()
    }
    
    @Serializable
    data class AIVerbConjugationQuestion(
        val word: String = "",
        val furigana: String = "",
        val meaning: String = "",
        val qText: String,
        val translation: String,
        val options: List<String>,
        val correctIndex: Int,
        val explanation: String
    )

    data class VerbWordInfo(val spelling: String, val hiragana: String, val chinese: String)

    suspend fun generateVerbConjugationQuestions(
        platform: String,
        apiKey: String,
        baseUrl: String?,
        model: String,
        difficulty: String,
        words: List<VerbWordInfo>,
        targetCount: Int = 5,
        oversampleCount: Int = 8 // 已废弃，因为我们改用并发生成
    ): Result<List<AIVerbConjugationQuestion>> = coroutineScope {
        val forms = listOf("辞书形", "ます形", "ない形", "た形", "て形", "意志形", "命令形", "禁止形", "ば形", "可能形", "被动形", "使役形", "使役被动形").shuffled()
        
        val deferreds = words.take(targetCount).mapIndexed { index, word ->
            val targetForm = forms[index % forms.size]
            async(Dispatchers.IO) {
                generateSingleVerbQuestionWithRetry(platform, apiKey, baseUrl, model, difficulty, word, targetForm)
            }
        }
        
        try {
            val questions = deferreds.awaitAll().filterNotNull()
            if (questions.isNotEmpty()) {
                Result.success(questions)
            } else {
                Result.failure(Exception("AI 生成的题目全部失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun generateSingleVerbQuestionWithRetry(
        platform: String,
        apiKey: String,
        baseUrl: String?,
        model: String,
        difficulty: String,
        word: VerbWordInfo,
        targetForm: String
    ): AIVerbConjugationQuestion? {
        var retryCount = 0
        val maxRetries = 2
        
        while (retryCount <= maxRetries) {
            try {
                val systemPrompt = buildSingleVerbConjugationPrompt(difficulty, word, targetForm)
                val url = getApiUrl(platform, baseUrl, apiKey, model)
                val requestBody = buildChatRequest(platform, model, systemPrompt, "请开始出题。")
                
                val response = executeRequest(platform, url, apiKey, requestBody)
                val content = extractContentFromChatResponse(platform, response)
                
                val cleanJson = extractJsonFromString(content)
                var q = json.decodeFromString<AIVerbConjugationQuestion>(cleanJson)
                
                // 物理覆写！彻底抛弃 AI 吐出来的字词/注音/释义，直接强制装填本地数据库的黄金原装数据！
                // 同时利用正则自动净化器把题干例句中任何被大模型错误生成的中括号注音物理剔除，确保界面绝对清爽
                val cleanQText = q.qText.replace(Regex("\\[[^\\]]+\\]"), "")
                q = q.copy(
                    word = word.spelling,
                    furigana = word.hiragana,
                    meaning = word.chinese,
                    qText = cleanQText
                )
                
                // 本地质量验证器 (QuestionQualityValidator)
                if (q.options.size != 4) throw Exception("选项数量不为4")
                if (q.correctIndex !in 0..3) throw Exception("正确答案索引越界")
                if (q.options.distinct().size != 4) throw Exception("选项存在重复")
                if (q.word != word.spelling) throw Exception("没有使用指定动词")
                if (!q.qText.contains("____")) throw Exception("题干未包含挖空")
                if (q.qText.length < 8) throw Exception("题干过短缺乏语境")
                
                return q
            } catch (e: Exception) {
                Log.w("AIClient", "单题生成失败，准备重试 (${word.spelling}): ${e.message}")
                retryCount++
                if (retryCount <= maxRetries) delay(1000L) // 避免触发并发限制过快
            }
        }
        return null
    }

    private fun buildSingleVerbConjugationPrompt(difficulty: String, word: VerbWordInfo, targetForm: String): String {
        return """
            你是一位资深的日语教育专家。请为日语等级为 $difficulty 的学习者生成 1 道“动词活用”的四选一单选题。
            【词汇硬约束】必须使用动词【${word.spelling}】（假名：${word.hiragana}，意思：${word.chinese}）出题。
            【语法等级约束】题干语法必须绝对符合 $difficulty 等级，严禁超纲。
            【变形考察约束】必须且只能考察该动词的【$targetForm】！请构建需要填入【$targetForm】的语境。
            【语境与唯一性约束】（极其重要！）
            1. 题干(qText)必须提供足够明确的上下文（如时间状语“今”、“毎日”，或人物对话“先生：「...」”），确保在日语语法和该语境下，正确答案具有【绝对唯一性】。
            2. 严禁仅靠“礼貌体（ます）”和“普通体（辞书形）”的差异来设置干扰项（例如，如果不提供明确的敬语语境，不要同时给出「掛かる」和「掛かります」让用户二选一，这会导致题目不可判定）。
            3. 中文翻译(translation)必须精确对应正确选项的时态与语态（如过去式就要翻译出“了”，进行时要翻译出“正在”），不能与日语语感冲突。
            【干扰项约束】正确答案唯一。另外3个错误选项必须且仅能是该动词的其他不同日文活用变形（禁止出现其他动词）。
            【挖空与格式要求】
            1. 设问处必须用 `____` 表示。
            2. 挖空必须“连根拔起”！`____` 必须替代该动词的完整活用形态（包含后缀 ます、ません、ない、た 等）。例如原句为「毎日日記を書きます。」，必须挖空为「毎日日記を____。」，选项提供「書きます」，**绝对禁止**保留后缀写成「毎日日記を____ます。」。
            3. 🚨【绝对禁止对句子中的汉字加注音】🚨为了保持题干句子的极致清爽与高级感，题目句子(qText)中包含汉字时，必须使用最纯正、最原始的日语汉字和假名，【绝对严禁】在汉字后加中括号标注发音（例如，只能写「毎日日記を____。」，绝对严禁写成「毎日[まいにち]日記[にっき]を____。」）！
            4. 🚨【数据结构极简化要求】🚨为了节省通信开销，你返回的 JSON 中【绝对禁止包含】"word"、"furigana"、"meaning" 这三个属性！你只需要返回包含 qText, translation, options, correctIndex, explanation 的纯 JSON 对象即可。绝对不要包含 ```json 标签 and 数组框：
            {
              "qText": "先生：「この電話番号を____ください。」",
              "translation": "老师：“请记住这个电话号码。”",
              "options": ["覚えた", "覚えない", "覚える", "覚えて"],
              "correctIndex": 3,
              "explanation": "「ください」前接て形...「覚える」的て形是「覚えて」。"
            }
            
            （注：上述例子考察的是“て形”，仅供 JSON 格式和注音格式参考。本题你必须严格按照要求，出考察【$targetForm】的题目！）
        """.trimIndent()
    }

    suspend fun generateReadingArticle(
        platform: String,
        apiKey: String,
        baseUrl: String?,
        model: String,
        difficulty: String,
        theme: String
    ): Result<AIReadingArticle> {
        val systemPrompt = buildReadingArticlePrompt(difficulty, theme)
        val url = getApiUrl(platform, baseUrl, apiKey, model)
        val requestBody = buildChatRequest(platform, model, systemPrompt, "请为我生成一篇日语小短文")
        
        return try {
            val response = executeRequest(platform, url, apiKey, requestBody)
            val content = extractContentFromChatResponse(platform, response)
            val cleanJson = extractJsonFromString(content)
            val article = json.decodeFromString<AIReadingArticle>(cleanJson)
            Result.success(article)
        } catch (e: Exception) {
            Log.e("AIClient", "生成短文失败", e)
            Result.failure(e)
        }
    }

    private fun buildReadingArticlePrompt(difficulty: String, theme: String): String {
        val wordCountGuide = when (difficulty) {
            "N5" -> "约 150～200 字（平假名为主，汉字极少）"
            "N4" -> "约 200～280 字（简单汉字，句型多样）"
            "N3" -> "约 300～380 字（中级词汇，段落清晰）"
            "N2" -> "约 400～480 字（书面语为主，逻辑严密）"
            "N1" -> "约 500～600 字（专业词汇，结构复杂）"
            else -> "约 200～300 字"
        }
        return """
            你是一位资深的日语教育专家。请为日语等级为 $difficulty 的学习者，围绕主题【$theme】生成一篇日语短文，用来提升他们的阅读理解能力。
            
            难度要求：
            - N5: 涵盖基础助词、判断句、动词基本分类，贴近极简单的日常生活场景，句子简短，汉字极少。
            - N4: 涵盖基础敬语、授受关系、可能态/意志态，涉及日常社交与简单叙事。
            - N3: 涵盖中级语法（如被动、使役、假定），涉及社会新闻、个人观点表达或文化故事。
            - N2: 涵盖复杂书面语、商务场景、抽象概念讨论，用词精准地道，具有一定文学性。
            - N1: 涵盖专业领域、文学性表达、微妙的语气差别及高度抽象的话题，句子结构复杂，词汇极其丰富。
            
            短文篇幅要求：$wordCountGuide。请保证内容充实、段落自然流畅，不可过短。
            
            短文生成具体要求：
            1. title：一个精致的短文标题。
            2. contentRaw：符合以上字数要求、没有任何 HTML 标签或注音的纯日语原文。使用换行符（\n）分隔段落。
            3. translation：小短文的高质量中文翻译，与原文段落结构对应。
            4. vocabulary：精选本篇短文中的 5～8 个核心重点词汇（必须在短文中出现过）。每个词汇包含 word (单词汉字或假名), kana (读音假名), pos (词性), meaning (中文释义)。
            5. questions：针对文章内容设计 3 道高水准的日语单选题，考察用户的阅读理解。每道题包含：
               - question: 日语提问的问题题干（必须符合该等级）
               - options: 4个备选答案（A, B, C, D，用日语或中文）
               - answer: 正确选项的索引值（0, 1, 2, 3，0代表第一个，1代表第二个，以此类推）
               - explanation: 对正确和错误选项的详细中文解析。
               
            请严格返回以下 JSON 格式，直接返回 JSON 字符串，不要包含 Markdown 格式块或其他文字：
            {
              "title": "标题",
              "level": "$difficulty",
              "contentRaw": "纯原文内容，用\\n分隔段落...",
              "contentHtml": "",
              "translation": "中文对照，用\\n分隔段落...",
              "vocabulary": [
                {"word": "単語", "kana": "たんご", "pos": "名词", "meaning": "单词"}
              ],
              "questions": [
                {
                  "question": "問題...",
                  "options": ["选项1", "选项2", "选项3", "选项4"],
                  "answer": 0,
                  "explanation": "解析..."
                }
              ]
            }
        """.trimIndent()
    }

    suspend fun translateWordOrPhrase(
        platform: String,
        apiKey: String,
        baseUrl: String?,
        model: String,
        text: String,
        contextSentence: String
    ): Result<AIWordTranslation> {
        val systemPrompt = buildWordTranslationPrompt(text, contextSentence)
        val url = getApiUrl(platform, baseUrl, apiKey, model)
        val requestBody = buildChatRequest(platform, model, systemPrompt, "请翻译这个词")

        return try {
            val response = executeRequest(platform, url, apiKey, requestBody)
            val content = extractContentFromChatResponse(platform, response)
            val cleanJson = extractJsonFromString(content)
            var translation = json.decodeFromString<AIWordTranslation>(cleanJson)
            // Fallback：如果 AI 返回的字段不完整，用用户选中的原文兜底
            if (translation.word.isBlank()) {
                translation = translation.copy(word = text)
            }
            if (translation.meaning.isBlank()) {
                translation = translation.copy(meaning = "AI 未能解析释义，请重试")
            }
            Result.success(translation)
        } catch (e: Exception) {
            Log.e("AIClient", "划词翻译失败", e)
            Result.failure(e)
        }
    }

    private fun buildWordTranslationPrompt(text: String, contextSentence: String): String {
        return """
            你是一位专业的日语词典与翻译工具。用户在阅读日语短文时，选中了一段文字，请你为其提供精准的翻译释义。

            用户选中的文字：「$text」
            该文字所在的上下文句子：「$contextSentence」

            请根据上下文语境，分析该词/短语的含义，并返回以下 JSON 格式：
            {
              "word": "该词的标准书写形式（汉字优先）",
              "kana": "完整假名读音",
              "pos": "词性（如：名词、动词、形容词、副词、助词、接续词、短语 等）",
              "meaning": "结合上下文的精准中文释义",
              "note": "简要的语法说明或用法提示（如有必要，否则留空字符串）"
            }

            要求：
            1. 如果用户选中的是一个完整单词，请给出该单词的标准辞书形式。
            2. 如果用户选中的是活用形态（如て形、た形等），word 字段仍给出辞书形，但 meaning 需结合上下文解释当前活用含义。
            3. 如果用户选中的是一个短语或句子片段，pos 填"短语"，meaning 给出整体翻译。
            4. 直接返回 JSON 字符串，不要包含 Markdown 格式块或其他文字。
        """.trimIndent()
    }

    // Helper extensions for JsonElement to keep it simple without full serialization of the response wrapper
    private fun kotlinx.serialization.json.JsonElement.asJsonObject() = this as? kotlinx.serialization.json.JsonObject ?: throw Exception("Not an object")
    private fun kotlinx.serialization.json.JsonElement.asJsonArray() = this as? kotlinx.serialization.json.JsonArray ?: throw Exception("Not an array")
    private fun kotlinx.serialization.json.JsonElement.asJsonPrimitive() = this as? kotlinx.serialization.json.JsonPrimitive ?: throw Exception("Not a primitive")

    private fun extractJsonFromString(input: String): String {
        val start = input.indexOf('{')
        val end = input.lastIndexOf('}')
        return if (start != -1 && end != -1 && end > start) {
            input.substring(start, end + 1)
        } else {
            input
        }
    }
}
