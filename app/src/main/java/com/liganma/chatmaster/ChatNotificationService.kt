package com.liganma.chatmaster

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.LaunchedEffect
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

var NOTIFY_TAG = "float_control"
// 在类顶部定义常量
const val TOGGLE_FLOATING_ACTION = "TOGGLE_FLOATING_ACTION"
const val NOTIFICATION_ID = 1


/**
 * 已弃用
 */
// 创建广播接收器处理按钮点击
class NotificationReceiver : BroadcastReceiver() {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            TOGGLE_FLOATING_ACTION -> {
                val app = context.applicationContext as App
                val overlayEnabled = app.settingsRepository.overlayEnabled.value

                app.settingsRepository.saveOverlayEnabled(!overlayEnabled)

                // 更新通知
                showNotification(context)
            }
        }
    }
}


// 展示通知
fun showNotification(context:Context){


    if(!NotificationsUtils.isNotificationEnabled(context)){
        // 先申请权限
        NotificationsUtils.openPush(context)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = context.packageName
        val descriptionText = context.packageName
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(context.packageName, name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system.
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    // 创建切换浮窗的 PendingIntent
    val toggleIntent = Intent(context, NotificationReceiver::class.java).apply {
        action = TOGGLE_FLOATING_ACTION
    }
    val pendingToggleIntent = PendingIntent.getBroadcast(
        context,
        0,
        toggleIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, context.packageName)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle("聊天大师正在服务")
        .setContentText("浮窗已启动")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(false)  // 保持常驻通知
        .setOngoing(true)
        .addAction(
            R.mipmap.ic_launcher, // 自定义切换图标
            "关闭浮窗",
            pendingToggleIntent
        )
        .build()

    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }
    sendNotify(context,notification)
}

@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
fun sendNotify(context: Context,notification: Notification){
    NotificationManagerCompat.from(context).notify(NOTIFY_TAG,NOTIFICATION_ID,notification)
}

fun stopNotification(context: Context){
    NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID);
}

/**
 * 通知工具
 */
object NotificationsUtils {
    /**
     * 是否打开通知按钮
     * @param context
     * @return
     */
    fun isNotificationEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context.applicationContext).areNotificationsEnabled()
    }

    fun openPush(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //这种方案适用于 API 26, 即8.0（含8.0）以上可以用
            val intent = Intent()
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            intent.putExtra(Settings.EXTRA_CHANNEL_ID, context.applicationInfo.uid)
            context.startActivity(intent)
        } else {
            toPermissionSetting(context)
        }
    }


    /**
     * 跳转到权限设置
     *
     * @param activity
     */
    fun toPermissionSetting(context: Context) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            toSystemConfig(context)
        } else {
            try {
                toApplicationInfo(context)
            } catch (e: Exception) {
                e.printStackTrace()
                toSystemConfig(context)
            }
        }
    }

    /**
     * 应用信息界面
     *
     * @param activity
     */
    fun toApplicationInfo(context: Context) {
        val localIntent = Intent()
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        localIntent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        localIntent.setData(Uri.fromParts("package", context.packageName, null))
        context.startActivity(localIntent)
    }

    /**
     * 系统设置界面
     *
     * @param activity
     */
    fun toSystemConfig(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_SETTINGS)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}