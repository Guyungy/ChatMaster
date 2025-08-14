package com.liganma.chatmaster

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import com.liganma.chatmaster.data.SuggestMessage
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.assist.FxGravity
import com.petterp.floatingx.assist.FxScopeType
import com.petterp.floatingx.compose.enableComposeSupport
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration

var FLOAT_WINDOW = "float_window"

fun showFloat(context: Context){
    if(!FloatingX.isInstalled(FLOAT_WINDOW)){
        // 展示
        val composeView = ComposeView(context).apply {
            setContent {
                MaterialTheme {
                    FloatingChatAssistant()
                }
            }
        }

        FloatingX.install {
            setContext(context)
            setEdgeOffset(24.0f)
            setGravity(FxGravity.RIGHT_OR_TOP)
            setTag(FLOAT_WINDOW)
            setLayoutView(composeView)
            setScopeType(FxScopeType.SYSTEM_AUTO)
            enableComposeSupport()
        }
    }
    FloatingX.controlOrNull(FLOAT_WINDOW)?.show()
}

fun closeFloat(){
    FloatingX.controlOrNull(FLOAT_WINDOW)?.cancel()
}
@Composable
fun FloatingChatAssistant(context: Context = LocalContext.current) {
    // 添加最小化状态管理
    var isMinimized by remember { mutableStateOf(false) }

    // 保持原有的分析状态管理
    var analysisState by remember { mutableStateOf(AnalysisState.Idle) }
    var analysisResult by remember { mutableStateOf(SuggestMessage("无","无", listOf("无"),"无")) }
    val coroutineScope = rememberCoroutineScope()

    // 根布局：根据状态动态调整尺寸
    Box(
        modifier = Modifier
            // 动态尺寸：最小化50x50，展开300x400
            .width(if (isMinimized) 50.dp else 300.dp)
            .defaultMinSize(50.dp,50.dp)
//            .height(if (isMinimized) 50.dp else 400.dp)
            .verticalScroll(rememberScrollState()) // 核心：添加垂直滚动
            .animateContentSize(animationSpec = tween(300)) // 平滑过渡动画
            .then(
                if (isMinimized) {
                    // 最小化状态样式
                    Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { isMinimized = false } // 点击展开
                } else {
                    // 展开状态样式
                    Modifier
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(12.dp)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isMinimized) {
            // 最小化状态：纯圆形按钮
            Icon(
                imageVector = Icons.Default.OpenInFull,
                contentDescription = "展开悬浮窗",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        } else {
            // 展开状态：完整内容
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 顶部标题栏（包含最小化按钮）
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "聊天大师",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                            .clickable {
                                isMinimized = true
                            }
                        .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                        ){
                        Icon(
                            imageVector = Icons.Default.Minimize,
                            contentDescription = "展开悬浮窗",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // 分析按钮（保持原有设计）
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable {
                                    analysisState = AnalysisState.Loading
                                    analyzingPage(context)
                                    coroutineScope.launch {
                                        delay(1000)
                                        analysisState = AnalysisState.Success
                                    }
                                }
                                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            when (analysisState) {
                                AnalysisState.Idle -> Icon(Icons.Default.Search, "开始分析", tint = Color.White, modifier =  Modifier.size(24.dp))
                                AnalysisState.Loading -> CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier =  Modifier.size(24.dp))
                                AnalysisState.Success -> Icon(Icons.Default.Refresh, "重新分析", tint = Color.White, modifier =  Modifier.size(24.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 分析结果区域
                        AnimatedContent(
                            targetState = analysisState,
                            transitionSpec = {
                                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) togetherWith
                                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
                            }
                        ) { state ->
                            when (state) {
                                AnalysisState.Idle -> PlaceholderMessage("点击按钮分析当前聊天")
                                AnalysisState.Loading -> LoadingIndicator()
                                AnalysisState.Success -> AnalysisResultView(analysisResult)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 分析结果视图
@Composable
fun AnalysisResultView(analysis: SuggestMessage) {
    Column {
        // 情绪分析
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "情绪",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "对方态度与情绪: ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = analysis.attitude,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // 下一句预测
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                text = "对方下一句预测:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = analysis.next,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F7FA), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            )
        }

        // 下一句预测
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                text = "回复分析:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = analysis.analyze,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F7FA), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            )
        }

        // 回复建议
        Text(
            text = "推荐回复:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        analysis.suggest.forEachIndexed { index, suggestion ->
            SuggestionItem(index + 1, suggestion)
            if (index < analysis.suggest.size - 1) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// 单条建议组件
@Composable
fun SuggestionItem(index: Int, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF5F7FA))
            .clickable {
                /* 点击后填充到输入框 */

            }
            .padding(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = "$index",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = "复制",
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
    }
}

// 其他辅助组件
@Composable
fun PlaceholderMessage(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "分析",
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}

@Composable
fun LoadingIndicator() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "正在分析聊天内容...",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

enum class AnalysisState {
    Idle, Loading, Success
}



