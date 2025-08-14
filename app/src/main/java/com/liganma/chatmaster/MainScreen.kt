package com.liganma.chatmaster

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

import com.liganma.chatmaster.theme.*
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


class PermissionViewModel(
    private val context: Context,
    private val settingsRepository: AppSettingsRepository
    ) : ViewModel() {
    // 悬浮窗权限状态（响应式）
    private val _overlayPermissionEnabled = MutableStateFlow(false)
    val overlayPermissionEnabled: StateFlow<Boolean> = _overlayPermissionEnabled.asStateFlow()

    // 无障碍服务状态（响应式）
    private val _accessibilityEnabled = MutableStateFlow(false)
    val accessibilityEnabled: StateFlow<Boolean> = _accessibilityEnabled.asStateFlow()

    // 悬浮窗启用状态（直接绑定到Repository）
    val overlayEnabled: StateFlow<Boolean> = settingsRepository.overlayEnabled

    init {
        // 初始化权限状态
        updatePermissionStates()
    }

    private fun updatePermissionStates() {
        _overlayPermissionEnabled.value = Settings.canDrawOverlays(context.applicationContext)
        _accessibilityEnabled.value = isAccessibilityServiceEnabled()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {

        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager

        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )

        return if (enabledServices != null) {
            enabledServices.contains(context.packageName)
        } else {
            am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.DEFAULT)
                ?.any { it.id.contains(context.packageName) } == true
        }
    }

    fun refreshPermission() {
        // 监听悬浮窗权限变化（Android 11+）
        val currentPermission = Settings.canDrawOverlays(context)
        if (currentPermission != _overlayPermissionEnabled.value) {
            _overlayPermissionEnabled.value = currentPermission
        }

        val accessibilityServiceEnabled = isAccessibilityServiceEnabled()
        if (accessibilityServiceEnabled != _accessibilityEnabled.value){
            _accessibilityEnabled.value = accessibilityServiceEnabled
        }
    }
}

// ViewModel工厂
class PermissionViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PermissionViewModel::class.java)) {
            val app = context.applicationContext as App
            val repository = app.settingsRepository

            @Suppress("UNCHECKED_CAST")
            return PermissionViewModel(
                context,
                repository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun MainScreen(
    navController: NavController,
    context: Context = LocalContext.current,
    viewModel: PermissionViewModel = viewModel(factory = PermissionViewModelFactory(context))
    ) {
    val app = context.applicationContext as App
    val repository = app.settingsRepository

    // 关键修复：使用collectAsStateWithLifecycle确保正确收集
    val overlayPermissionEnabled by viewModel.overlayPermissionEnabled.collectAsStateWithLifecycle()
    val accessibilityEnabled by viewModel.accessibilityEnabled.collectAsStateWithLifecycle()
    val overlayEnabled by viewModel.overlayEnabled.collectAsStateWithLifecycle()

    // 创建本地状态
    var localOverlayEnabled by remember { mutableStateOf(false) }

    // 初始加载
    LaunchedEffect(Unit) {
        localOverlayEnabled = overlayEnabled
        if (localOverlayEnabled){
            showFloat(context)
        }else{
            closeFloat()
        }
    }

    // 初始化配置
    val scrollState = rememberScrollState()

    // 获取 Compose 协程作用域
    val coroutineScope = rememberCoroutineScope()

    Log.i("MainScreen", "overlayPermissionEnabled:${viewModel.overlayPermissionEnabled},accessibilityEnabled:${viewModel.accessibilityEnabled},overlayEnabled:${viewModel.overlayEnabled}")

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // 不管结果如何，都重新检查
        viewModel.refreshPermission()
    }


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
            Text(
                text = "聊天大师",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = { navController.navigate("settings") }
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "设置",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 权限卡片
        FlatCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "权限状态",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 悬浮窗权限
                PermissionItem(
                    title = "悬浮窗权限",
                    description = "用于显示聊天分析结果",
                    enabled = overlayPermissionEnabled,
                    onActionClick = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            "package:${context.packageName}".toUri()
                        )
                        overlayPermissionLauncher.launch(intent)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 无障碍权限
                PermissionItem(
                    title = "无障碍服务",
                    description = "用于读取微信聊天内容",
                    enabled = accessibilityEnabled,
                    onActionClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        overlayPermissionLauncher.launch(intent)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 悬浮窗控制卡片
        FlatCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "悬浮窗控制",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )

                    Switch(
                        checked = localOverlayEnabled,
                        enabled = overlayPermissionEnabled,
                        onCheckedChange = { enabled ->
                            localOverlayEnabled = enabled
                            if(enabled){
                                showFloat(context)
                            }else{
                                closeFloat()
                            }
                            repository.saveOverlayEnabled(enabled)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "悬浮窗将在聊天界面显示情感分析和回复建议",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}