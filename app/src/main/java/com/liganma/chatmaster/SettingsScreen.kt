package com.liganma.chatmaster

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

import com.liganma.chatmaster.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch



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

class SettingsViewModel(
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {

    // accessKey
    val apiKey: StateFlow<String> = settingsRepository.accessKey
    val prompt:StateFlow<String> = settingsRepository.prompt
}

// ViewModel工厂
class SettingsViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            val app = context.applicationContext as App
            val repository = app.settingsRepository

            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(
                repository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun SettingsScreen(
    navController: NavController,
    context: Context = LocalContext.current,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(context))
    ) {
    var app = LocalContext.current.applicationContext as App
    var passwordHidden by remember { mutableStateOf(true) }

    // 获取 Compose 协程作用域
    val coroutineScope = rememberCoroutineScope()

    // 关键修复：使用collectAsStateWithLifecycle确保正确收集
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val prompt by viewModel.prompt.collectAsStateWithLifecycle()

    // 创建本地状态
    var localPrompt by remember { mutableStateOf(DEFAULT_PROMPT) }
    var localApiKey by remember { mutableStateOf("") }

    // 初始加载
    LaunchedEffect(Unit) {
        localPrompt = prompt
        localApiKey = apiKey
    }


    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // 标题栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "设置",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )

            // 占位符保持对称
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // API密钥卡片
        FlatCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "API密钥",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "DeepSeek API密钥",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = localApiKey,
                    onValueChange = { newKey ->
                        localApiKey = newKey
                        app.settingsRepository.saveAccessKey(newKey)
                    },
                    label = { Text("sk-xxxxxxxxxxxxxxxxxxxxxxxx") },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                passwordHidden = !passwordHidden
                            }
                        ){
                            Icon(Icons.Default.RemoveRedEye, null)
                        }
                    },
                    visualTransformation = if(passwordHidden) PasswordVisualTransformation() else VisualTransformation.None,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // API密钥卡片
        FlatCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row (){
                    Text(
                        text = "系统提示词（prompt）",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // 保存按钮
                    Button(
                        onClick = {
                            localPrompt = DEFAULT_PROMPT
                            app.settingsRepository.savePrompt(DEFAULT_PROMPT)
                            Toast.makeText(context,"已重置",Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "重置",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }


                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "填写系统提示词",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = localPrompt,
                    onValueChange = { newPrompt ->
                        localPrompt = newPrompt
                        app.settingsRepository.savePrompt(newPrompt)
                    },
                    label = { Text("提示词") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    maxLines = 10,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                    )
                )

            }
        }

    }
}