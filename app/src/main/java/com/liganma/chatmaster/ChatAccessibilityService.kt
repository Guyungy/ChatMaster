package com.liganma.chatmaster

import android.content.Context
import android.widget.Toast
import com.ven.assists.AssistsCore
import com.ven.assists.AssistsCore.logNode
import okhttp3.OkHttpClient
import okhttp3.Request

private val client = OkHttpClient()

private val BASE_URL = "https://api.deepseek.com/"

fun analyzingPage(context: Context){
    if(!AssistsCore.isAccessibilityServiceEnabled()){
        Toast.makeText(context,"没有无障碍权限",Toast.LENGTH_SHORT).show();
        return
    }


    var app = context.applicationContext as App
    var accessKey = app.settingsRepository.accessKey

    val request: Request = Request.Builder()
        .url(BASE_URL+"")
        .build()

    var res = client.newCall(request).execute()
    // object


    // 只在微信有效
    if(AssistsCore.getPackageName().startsWith("com.tencent.mm")){
        AssistsCore.getAllNodes().forEach { it.logNode() }

    }else{
        Toast.makeText(context,"请在微信聊天页使用",Toast.LENGTH_SHORT).show()
    }
}

// 构建聊天消息
fun buildChatMsg(){

}

// 发送到大模型端分析
fun sendLLM(){

}

// 解析结果
fun parseResult(){

}

// 通过eventbus推送到界面
fun pushEvent(){

}
