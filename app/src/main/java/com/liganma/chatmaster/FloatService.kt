package com.liganma.chatmaster

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liganma.chatmaster.data.SuggestItem
import com.liganma.chatmaster.data.SuggestMessage
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.assist.FxDisplayMode
import com.petterp.floatingx.assist.FxGravity
import com.petterp.floatingx.assist.FxScopeType
import com.petterp.floatingx.compose.enableComposeSupport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

var FLOAT_WINDOW = "float_window"


// 复制功能的扩展实现
fun copyToClipboard(context: Context, text: String) {

    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clipData = ClipData.newPlainText("chat_reply", text)
    clipboardManager.setPrimaryClip(clipData)

    Toast.makeText(context.applicationContext, "内容已复制到剪贴板", Toast.LENGTH_SHORT).show()
}

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
            setDisplayMode(FxDisplayMode.Normal)
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

    // 根布局：根据状态动态调整尺寸
    Box(
        modifier = Modifier
            // 动态尺寸：最小化50x50，展开300x400
            .width(if (isMinimized) 50.dp else 300.dp)
            .defaultMinSize(50.dp,50.dp)
            .verticalScroll(rememberScrollState()) // 核心：添加垂直滚动
            .animateContentSize(animationSpec = tween(100)) // 平滑过渡动画
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
        if (isMinimized) MinimizedView() else ExpandedView(onMinimize = {isMinimized = true},context)
    }
}

// 最小化视图
@Composable
private fun MinimizedView() {
    Icon(
        imageVector = Icons.Default.OpenInFull,
        contentDescription = "展开悬浮窗",
        tint = Color.White,
        modifier = Modifier.size(24.dp)
    )
}

// 展开视图
@Composable
private fun ExpandedView(onMinimize: () -> Unit,context: Context) {
    // 保持原有的分析状态管理
    var analysisState by remember { mutableStateOf(AnalysisState.Idle) }
    var analysisResult by remember { mutableStateOf(EMPTY) }
    val coroutineScope = rememberCoroutineScope()
    // 展开状态：完整内容
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        HeaderBar(onMinimize)

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
                            coroutineScope.launch {  // launch 默认从主线程开始
                                try {
                                    // 1. 在后台线程执行网络请求
                                    val result = withContext(Dispatchers.IO) {
                                        analyzingPage(context)
                                    }

                                    // 2. 自动回到主线程（launch的初始上下文）
                                    analysisResult = result
                                    analysisState = AnalysisState.Success
                                } catch (e: Exception) {
                                    Log.e("FloatService","e message : ${e.message}",e)
                                    analysisState = AnalysisState.Idle
                                }
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

@Composable
private fun HeaderBar(onMinimize: () -> Unit){
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
                .clickable(onClick = onMinimize)
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
}


// 分析结果视图
@Composable
fun AnalysisResultView(analysis: SuggestMessage) {
    Column(modifier = Modifier
        .fillMaxWidth()
    ) {
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F7FA), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            )
        }

        //
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                text = "回复分析:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))

            ExpandableText(
                text = analysis.analyze,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F7FA), RoundedCornerShape(8.dp))
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
            SuggestionItem(suggestion)
            if (index < analysis.suggest.size - 1) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ExpandableText(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 3
) {
    var isExpanded by remember { mutableStateOf(false) }

    // 使用onTextLayout精确检测是否需要展开
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = if (isExpanded) Int.MAX_VALUE else maxLines,
        overflow = if (isExpanded) TextOverflow.Clip else TextOverflow.Ellipsis,
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F7FA), RoundedCornerShape(8.dp))
            .padding(12.dp)
    )

    // 仅在需要时显示展开/收起按钮
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = if (isExpanded) "收起▲" else "展开▼",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            modifier = Modifier
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

// 单条建议组件
@Composable
fun SuggestionItem(item: SuggestItem,context: Context = LocalContext.current) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF5F7FA))
            .clickable {
                /**
                 * 复制到列列表
                 */
                copyToClipboard(context,item.content)
            }
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    text = item.type,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                Text(
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    text = item.content,
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



