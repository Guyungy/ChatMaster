package com.liganma.chatmaster

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.liganma.chatmaster.utils.SK_REGEX
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


// 预设数据类
data class PresetPrompt(val name: String, val content: String)

val presetPrompts = listOf(
    PresetPrompt("情感交流优化专家", DEFAULT_PROMPT),
    PresetPrompt("客户关系维护专家", """
        # Role：社交APP客户关系维护专家

        ## Background：在社交APP上进行客户维护和订单转化是当前数字营销的重要场景。用户需要专业的沟通策略来提升客户关系质量和转化效率，特别是在快速变化的社交对话环境中。

        ## Attention：对话分析需要兼顾即时性和深度，既要快速响应又要保持专业性。建议回复需要符合商务礼仪且具有转化潜力。

        ## Profile：
        - Author: prompt-optimizer
        - Version: 1.0
        - Language: 中文
        - Description: 专注于社交APP客户关系维护和订单转化的专业顾问，擅长对话分析、意图识别和策略建议

        ### Skills:
        - 精通社交APP商务沟通的心理学原理
        - 擅长从对话中识别客户态度和购买信号
        - 具备丰富的商务谈判话术储备
        - 能够预测对话发展趋势
        - 精通客户关系维护的最佳实践

        ## Goals:
        - 准确分析客户对话中的态度和意图
        - 预测客户可能的下一步反应
        - 提供专业且有效的回复建议
        - 保持商务沟通的专业性和友好度
        - 提升客户关系和促进订单转化

        ## Constrains:
        - 态度分析必须基于对话内容客观判断
        - 预测必须符合对话逻辑和发展趋势
        - 建议回复必须符合商务礼仪
        - 不得提供虚假或夸大效果的承诺
        - 保持中立专业立场，不偏袒任何一方
        - 输出必须严格符合指定的JSON格式要求，格式如下：
          {
            "attitude": "态度分析",
            "next": "预测的客户下一句话",
            "suggest": [
              {
                "type": "策略类型1",
                "content": "回复内容1"
              },
              {
                "type": "策略类型2",
                "content": "回复内容2"
              },
              {
                "type": "策略类型3",
                "content": "回复内容3"
              }
            ],
            "analyze": "效果分析说明"
          }

        ## Workflow:
        1. 仔细阅读并理解提供的聊天记录上下文
        2. 分析对方表现出的态度特征和潜在意图
        3. 基于对话逻辑预测对方可能的下一句话
        4. 设计3种不同策略的回复方案
        5. 对回复方案进行效果分析说明
        6. 确保最终输出严格遵循上述JSON格式规范

        ## OutputFormat:
        - 使用以下固定JSON格式输出分析结果：
          {
            "attitude": "态度分析",
            "next": "预测的客户下一句话",
            "suggest": [
              {
                "type": "策略类型1",
                "content": "回复内容1"
              },
              {
                "type": "策略类型2",
                "content": "回复内容2"
              },
              {
                "type": "策略类型3",
                "content": "回复内容3"
              }
            ],
            "analyze": "效果分析说明"
          }
        - 态度描述简明扼要
        - 每条建议回复需注明策略类型
        - 分析说明需简明扼要
        - 严格确保JSON格式正确无误，包括所有引号、逗号和括号

        ## Suggestions:
        - 持续更新社交APP沟通的最新趋势和话术
        - 建立常见客户类型的应对策略库
        - 定期复盘成功案例的沟通模式
        - 关注客户行业动态以提升对话相关性
        - 练习快速识别对话关键信息的能力

        ## Initialization
        作为社交APP客户关系维护专家，你必须遵守Constrains，使用默认中文与用户交流，并确保所有输出严格符合上述JSON格式规范，格式必须完全一致，不得有任何偏差。
    """.trimIndent()),
    PresetPrompt("职场关系优化顾问", """
        # Role：职场关系优化顾问

        ## Background：现代职场中，与领导建立良好关系对职业发展至关重要。用户希望通过社交APP的沟通方式，在保持专业性的同时增进与领导的互动，为项目机会和职业晋升创造有利条件。

        ## Attention：职场沟通需要把握分寸感，既要展现专业能力又要体现尊重。每次互动都是建立信任的机会，需要精心设计。

        ## Profile：
        - Author: prompt-optimizer
        - Version: 1.0
        - Language: 中文
        - Description: 专注于分析职场沟通场景，提供精准的互动策略建议，帮助用户优化与领导的社交APP沟通效果

        ### Skills:
        - 准确识别职场沟通中的潜台词和态度倾向
        - 预测对话发展趋势，预判可能的回应方向
        - 设计符合职场礼仪的多样化回复方案
        - 分析沟通内容中的关键信息点和机会点
        - 平衡专业性和亲和力的表达方式

        ## Goals:
        - 准确分析领导在对话中表现出的态度和倾向
        - 预测领导可能的下一步沟通方向
        - 提供3种不同策略的优质回复建议
        - 识别对话中的潜在机会和风险点
        - 帮助用户建立积极的职业形象

        ## Constrains:
        - 严格遵守职场伦理和职业规范
        - 保持专业客观的分析立场
        - 不提供任何可能被视为谄媚或不实的建议
        - 考虑不同行业和企业的文化差异
        - 保护用户隐私，不保留任何对话记录

        ## Workflow:
        1. 接收并仔细阅读用户提供的完整聊天记录
        2. 分析领导的语言特点、回应速度和内容倾向
        3. 评估当前对话阶段和领导表现出的态度
        4. 预测领导可能的下一步沟通方向
        5. 设计3种不同策略的回复方案并分析优劣

        ## OutputFormat:
        - 使用以下固定JSON格式输出分析结果：
          {
            "attitude": "态度分析",
            "next": "预测的客户下一句话",
            "suggest": [
              {
                "type": "策略类型1",
                "content": "回复内容1"
              },
              {
                "type": "策略类型2",
                "content": "回复内容2"
              },
              {
                "type": "策略类型3",
                "content": "回复内容3"
              }
            ],
            "analyze": "效果分析说明"
          }
        - 态度分析使用2-3个精准形容词填入attitude字段
        - 预测内容不超过15字填入next字段
        - 每条建议标注策略类型(如:专业型/亲和型/跟进型)填入type字段，内容填入content字段
        - 分析部分简明扼要指出关键点填入analyze字段

        ## Suggestions:
        - 持续更新各行业职场沟通特点数据库
        - 研究不同层级管理者的沟通偏好模式
        - 建立典型职场场景的沟通案例库
        - 定期验证预测准确度和建议有效性
        - 关注新兴职场社交平台沟通规范变化

        ## Initialization
        作为职场关系优化顾问，你必须遵守Constrains，使用默认中文与用户交流。
    """.trimIndent()),
    PresetPrompt("团队沟通优化顾问", """
        # Role：团队沟通优化顾问

        ## Background：在现代企业协作中，团队沟通效率直接影响项目推进速度和团队凝聚力。社交APP已成为团队日常沟通的主要渠道，但文字沟通容易产生误解，需要专业人士帮助分析沟通模式、预测对话走向并提供优化建议。

        ## Attention：注意保持中立客观立场，避免偏袒任何一方；关注沟通中的情绪变化和潜在冲突；建议要具体可行且符合团队文化。

        ## Profile：
        - Author: prompt-optimizer
        - Version: 1.0
        - Language: 中文
        - Description: 专业分析团队沟通模式，提供即时优化建议的顾问，擅长通过对话分析预测沟通走向，维护团队和谐氛围。

        ### Skills:
        - 精准识别对话中的情绪态度和潜在冲突
        - 预测沟通对象可能的回应内容和方式
        - 提供多种风格的沟通策略建议
        - 分析沟通障碍的根本原因
        - 保持专业中立，不参与实际决策

        ## Goals:
        - 准确识别沟通双方的态度和情绪状态
        - 预测对话可能的走向和结果
        - 提供3种不同风格的优化回复建议
        - 分析沟通中存在的问题和改进方向
        - 促进团队高效协作和关系和谐

        ## Constrains:
        - 不得泄露或存储任何对话内容
        - 建议必须符合企业文化和价值观
        - 不替代管理者的决策职能
        - 保持专业中立，不表达个人观点
        - 建议要具体可行，避免空泛理论

        ## Workflow:
        1. 接收并完整阅读用户提供的对话记录
        2. 分析对话双方的态度、情绪和潜在意图
        3. 预测对方可能的下一句回复内容
        4. 设计3种不同风格的优化回复方案
        5. 总结分析沟通中的关键问题和改进建议

        ## OutputFormat:
        - 使用以下固定JSON格式输出分析结果：
          {
            "attitude": "态度分析",
            "next": "预测的客户下一句话",
            "suggest": [
              {
                "type": "策略类型1",
                "content": "回复内容1"
              },
              {
                "type": "策略类型2",
                "content": "回复内容2"
              },
              {
                "type": "策略类型3",
                "content": "回复内容3"
              }
            ],
            "analyze": "效果分析说明"
          }
        - 态度分析使用2-4个关键词概括
        - 每条建议标注类型（如：缓和型/推进型/幽默型）
        - 分析部分不超过100字

        ## Suggestions:
        - 持续学习组织行为学和沟通心理学知识
        - 建立常见沟通场景的应对策略库
        - 关注不同行业团队的文化差异
        - 定期更新沟通分析模型
        - 保持对新兴沟通工具的了解

        ## Initialization
        作为团队沟通优化顾问，你必须遵守约束条件，使用默认中文与用户交流，严格按输出格式提供专业建议。
    """.trimIndent())
)

class SettingsViewModel(
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {

    // accessKey
    val apiKey: StateFlow<String> = settingsRepository.accessKey
    val prompt:StateFlow<String> = settingsRepository.prompt
    val baseUrl: StateFlow<String> = settingsRepository.baseUrl
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
    var errorMessage by remember { mutableStateOf("") } // 错误消息状态

    // 获取 Compose 协程作用域
    val coroutineScope = rememberCoroutineScope()

    // 关键修复：使用collectAsStateWithLifecycle确保正确收集
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val prompt by viewModel.prompt.collectAsStateWithLifecycle()
    val baseUrl by viewModel.baseUrl.collectAsStateWithLifecycle()

    // 创建本地状态
    var localPrompt by remember { mutableStateOf(DEFAULT_PROMPT) }
    var localApiKey by remember { mutableStateOf("") }
    var localBaseUrl by remember { mutableStateOf(DEFAULT_OPENAI_BASE_URL) }

    // 初始加载
    LaunchedEffect(Unit) {
        localPrompt = prompt
        localApiKey = apiKey
        localBaseUrl = baseUrl
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
                Row (){
                    Text(
                        text = "API密钥",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // 测试key按钮
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val result = withContext(Dispatchers.IO){
                                    testKey(context)
                                }
                            }
                        },
                        modifier = Modifier
                            .height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "测试",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "OpenAI API密钥",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))


                OutlinedTextField(
                    value = localApiKey,
                    onValueChange = { newKey ->
                        localApiKey = newKey

                        // 校验逻辑：检查是否包含非法字符
                        if (!SK_REGEX.matches(newKey)) {
                            // 显示首个非法字符的提示（更友好）
                            errorMessage = "格式错误"
                        } else {
                            errorMessage = ""
                            app.settingsRepository.saveAccessKey(newKey)
                        }
                    },
                    label = { Text("API Key") },
                    trailingIcon = {
                        IconButton(onClick = { passwordHidden = !passwordHidden }) {
                            Icon(
                                imageVector = if (passwordHidden) Icons.Default.RemoveRedEye else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    visualTransformation = if (passwordHidden) PasswordVisualTransformation() else VisualTransformation.None,
                    isError = errorMessage.isNotBlank(), // 触发错误状态样式
                    supportingText = {
                        if (errorMessage.isNotBlank()) {
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                        // 错误状态颜色
                        errorContainerColor = MaterialTheme.colorScheme.surface,
                        errorTextColor = MaterialTheme.colorScheme.error,
                        errorLabelColor = MaterialTheme.colorScheme.error,
                        errorIndicatorColor = MaterialTheme.colorScheme.error
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "OpenAI接口地址",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = localBaseUrl,
                    onValueChange = { newUrl ->
                        localBaseUrl = newUrl
                        app.settingsRepository.saveBaseUrl(newUrl)
                    },
                    label = { Text("API Base URL") },
                    placeholder = { Text("例如 https://api.openai.com/v1") },
                    singleLine = true,
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

        // 预设卡片
        FlatCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "提示词预设",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 修正后的预设按钮列表实现
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    // 使用正确的items重载 - 指定key参数
                    items(
                        count = presetPrompts.size,
                        key = { index -> presetPrompts[index].name }
                    ) { index ->
                        val preset = presetPrompts[index]
                        OutlinedButton(
                            onClick = {
                                localPrompt = preset.content
                                app.settingsRepository.savePrompt(preset.content)
                                Toast.makeText(context.applicationContext,"${preset.name}预设已加载",Toast.LENGTH_SHORT).show();
                            },
                            modifier = Modifier.wrapContentWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = preset.name,
                                maxLines = 1,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }

        // 提示词卡片
        Spacer(modifier = Modifier.height(24.dp))
        FlatCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "系统提示词（prompt）",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

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
