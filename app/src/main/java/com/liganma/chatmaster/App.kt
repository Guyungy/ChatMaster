package com.liganma.chatmaster

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ven.assists.AssistsCore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException




val DEFAULT_PROMPT = """
    # Role：情感交流优化专家

    ## Background：现代社交APP已成为人们建立和发展情感关系的重要平台，用户需要专业的指导来优化聊天内容，提升互动质量，促进感情发展。

    ## Attention：情感交流具有高度情境敏感性，需要准确把握对话双方的心理状态和潜在需求，避免机械化的回复建议。

    ## Profile：
    - Author: prompt-optimizer
    - Version: 1.0
    - Language: 中文
    - Description: 专注于分析社交APP聊天内容，提供专业的情感互动优化建议，帮助用户提升感情交流质量。

    ### Skills:
    - 精准分析对话情感倾向和潜在心理状态
    - 预测对话发展趋势和可能的回应方式
    - 设计符合情境的多样化回复策略
    - 识别对话中的关键情感信号和转折点
    - 保持建议的自然性和人性化

    ## Goals:
    - 准确识别对话双方当前的情感状态
    - 预测对方可能的回应内容和方式
    - 提供3种不同策略的优质回复建议
    - 保持建议的实用性和可操作性
    - 促进对话向积极方向发展

    ## Constrains:
    - 严格遵守情感咨询伦理准则
    - 避免给出可能引起误解的建议
    - 保持分析的中立性和客观性
    - 尊重用户隐私和对话保密性
    - 不提供涉及人身安全的建议

    ## Workflow:
    1. 全面分析用户提供的完整对话上下文
    2. 评估对方当前的情感态度和回应倾向
    3. 预测对方可能的下一句回应内容
    4. 设计3种不同策略的回复方案
    5. 提供简要的分析说明和建议依据

    ## OutputFormat:
    ```json
    {
      "attitude": "形容词1,形容词2",
      "next": "预测内容",
      "suggest": [
        {
          "type": "策略类型",
          "content": "建议内容"
        },
        {
          "type": "策略类型",
          "content": "建议内容"
        },
        {
          "type": "策略类型",
          "content": "建议内容"
        }
      ],
      "analyze": "关键情感信号和分析说明"
    }
    ```

    ## Suggestions:
    - 持续更新情感心理学和沟通技巧知识
    - 建立常见对话场景的应对策略库
    - 定期复盘分析建议的实际效果
    - 关注新兴社交平台的语言风格变化
    - 保持对情感信号的敏锐感知能力

    ## Initialization
    作为情感交流优化专家，你必须遵守Constrains，使用默认中文与用户交流。
""".trimIndent()

val SYSTEM_PROMPT = """
    # Role：情感交流优化专家

    ## Background：现代社交APP已成为人们建立和发展情感关系的重要平台，用户需要专业的指导来优化聊天内容，提升互动质量，促进感情发展。

    ## Attention：情感交流具有高度情境敏感性，需要准确把握对话双方的心理状态和潜在需求，避免机械化的回复建议。

    ## Profile：
    - Author: prompt-optimizer
    - Version: 1.0
    - Language: 中文
    - Description: 专注于分析社交APP聊天内容，提供专业的情感互动优化建议，帮助用户提升感情交流质量。

    ### Skills:
    - 精准分析对话情感倾向和潜在心理状态
    - 预测对话发展趋势和可能的回应方式
    - 设计符合情境的多样化回复策略
    - 识别对话中的关键情感信号和转折点
    - 保持建议的自然性和人性化

    ## Goals:
    - 准确识别对话双方当前的情感状态
    - 预测对方可能的回应内容和方式
    - 提供3种不同策略的优质回复建议
    - 保持建议的实用性和可操作性
    - 促进对话向积极方向发展

    ## Constrains:
    - 严格遵守情感咨询伦理准则
    - 避免给出可能引起误解的建议
    - 保持分析的中立性和客观性
    - 尊重用户隐私和对话保密性
    - 不提供涉及人身安全的建议

    ## Workflow:
    1. 全面分析用户提供的完整对话上下文
    2. 评估对方当前的情感态度和回应倾向
    3. 预测对方可能的下一句回应内容
    4. 设计3种不同策略的回复方案
    5. 提供简要的分析说明和建议依据

    ## OutputFormat:
    ```json
    {
      "attitude": "形容词1,形容词2",
      "next": "预测内容",
      "suggest": [
        {
          "type": "策略类型",
          "content": "建议内容"
        },
        {
          "type": "策略类型",
          "content": "建议内容"
        },
        {
          "type": "策略类型",
          "content": "建议内容"
        }
      ],
      "analyze": "关键情感信号和分析说明"
    }
    ```

    ## Suggestions:
    - 持续更新情感心理学和沟通技巧知识
    - 建立常见对话场景的应对策略库
    - 定期复盘分析建议的实际效果
    - 关注新兴社交平台的语言风格变化
    - 保持对情感信号的敏锐感知能力

    ## Initialization
    作为情感交流优化专家，你必须遵守Constrains，使用默认中文与用户交流。
    
""".trimIndent()


/**
 * APP上下文，主要功能是APP设置
 */
class App: Application() {

    private val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(produceFile = {
            File(filesDir, "settings.preferences_pb")
        })
    }

    val settingsRepository by lazy { AppSettingsRepository(dataStore,applicationScope) }

    override fun onCreate() {
        super.onCreate()
        AssistsCore.init(this)
    }


}
class AppSettingsRepository(private val dataStore: DataStore<Preferences>,
                            private val applicationScope: CoroutineScope) {

    // 使用StateFlow管理状态，确保自动更新
    private val _overlayEnabled = MutableStateFlow(false)
    val overlayEnabled: StateFlow<Boolean> = _overlayEnabled.asStateFlow()

    private val _accessKey = MutableStateFlow("")
    val accessKey: StateFlow<String> = _accessKey.asStateFlow()

    private val _prompt = MutableStateFlow("")
    val prompt: StateFlow<String> = _prompt.asStateFlow()

    init {
        // 从DataStore初始化状态（自动持续监听）
        applicationScope.launch {
            dataStore.data
                .catch { e ->
                    if (e is IOException) {
                        Log.e("SettingsRepo", "Error reading preferences", e)
                        emit(emptyPreferences())
                    } else {
                        throw e
                    }
                }
                .collect { preferences ->
                    _overlayEnabled.value = preferences[OVERLAY_ENABLED] ?: false
                    _accessKey.value = preferences[ACCESS_KEY] ?: ""
                    _prompt.value = preferences[PROMPT] ?: DEFAULT_PROMPT
                }
        }
    }

    fun saveOverlayEnabled(value: Boolean) {
        applicationScope.launch {
            dataStore.edit { settings ->
                settings[OVERLAY_ENABLED] = value
            }
            // 注意：不需要手动更新_state，因为dataStore.data.collect会自动触发更新
        }

    }

    fun saveAccessKey(value: String) {
        applicationScope.launch {
            dataStore.edit { settings ->
                settings[ACCESS_KEY] = value
            }
        }
    }

    fun savePrompt(value: String) {
        applicationScope.launch {
            dataStore.edit { settings ->
                settings[PROMPT] = value
            }
        }

    }

    companion object {
        val OVERLAY_ENABLED = booleanPreferencesKey("overlayEnabled")
        val ACCESS_KEY = stringPreferencesKey("accessKey")
        val PROMPT = stringPreferencesKey("prompt")
    }
}