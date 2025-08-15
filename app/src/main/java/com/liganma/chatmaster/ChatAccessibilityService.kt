package com.liganma.chatmaster

import android.content.Context
import android.widget.Toast
import cn.lishiyuan.deepseek.Client
import cn.lishiyuan.deepseek.DefualtClient
import cn.lishiyuan.deepseek.api.EmptyRequest
import cn.lishiyuan.deepseek.api.chat.ChatRequest
import cn.lishiyuan.deepseek.api.chat.ChatRequest.Message
import cn.lishiyuan.deepseek.api.chat.ChatRequest.ResponseFormat
import cn.lishiyuan.deepseek.config.enums.ModelEnums
import cn.lishiyuan.deepseek.config.enums.ResponseFormatEnums
import cn.lishiyuan.deepseek.config.enums.RoleEnums
import cn.lishiyuan.deepseek.e.DeepSeekException
import com.alibaba.fastjson2.to
import com.liganma.chatmaster.data.SuggestMessage
import com.ven.assists.AssistsCore
import com.ven.assists.AssistsCore.logNode



private val BASE_URL = "https://api.deepseek.com/"

val MSG_FORMAT = """
    聊天上下文如下：
    ```
    %s
    ```
    注意当前的上下文包含的时间信息与当前聊天场景是群聊还是私聊
""".trimIndent()


val EMPTY = SuggestMessage("无","无", listOf("无"),"无")

fun analyzingPage(context: Context):SuggestMessage{
    if(!AssistsCore.isAccessibilityServiceEnabled()){
        Toast.makeText(context,"没有无障碍权限",Toast.LENGTH_SHORT).show();
        return EMPTY
    }
    // 只在微信有效
    if(AssistsCore.getPackageName().startsWith("com.tencent.mm")){
        AssistsCore.getAllNodes().forEach { it.logNode() }
        // 读取和解析聊天上下文

        val msgContext = "无"

        val app = context.applicationContext as App
        val accessKey = app.settingsRepository.accessKey
        val prompt = app.settingsRepository.prompt

        val client:Client = DefualtClient(accessKey.value)

        val systemMessage= Message()
        systemMessage.role = RoleEnums.SYSTEM.code
        systemMessage.content = prompt.value

        val userMessage = Message()
        userMessage.role = RoleEnums.USER.code
        userMessage.content = MSG_FORMAT.format(msgContext)

        val chatRequest = ChatRequest.create(listOf(systemMessage,userMessage),ModelEnums.DEEPSEEK_REASONER.code)
        chatRequest.responseFormat = ResponseFormat()
        chatRequest.responseFormat.type = ResponseFormatEnums.JSON_OBJECT.code
        val chatResponse = client.post(chatRequest)

        val choice = chatResponse.choices[0]
        val jsonString = choice.message.content

        val suggestMessage = jsonString.to<SuggestMessage>()

        return suggestMessage
    }else{
        Toast.makeText(context,"请在微信聊天页使用",Toast.LENGTH_SHORT).show()
        return EMPTY;
    }
}

fun testKey(context: Context){
    val app = context.applicationContext as App
    val accessKey = app.settingsRepository.accessKey

    val client:Client = DefualtClient(accessKey.value)
    var createBalanceRequest = EmptyRequest.createBalanceRequest()
    try {
        var balanceInfoResponse = client.get(createBalanceRequest)
        if(balanceInfoResponse.isAvailable){

            balanceInfoResponse.balanceInfos

            Toast.makeText(context,"连接成功",Toast.LENGTH_LONG).show()
        }else{
            Toast.makeText(context,"key不可使用",Toast.LENGTH_SHORT).show()
        }

    }catch (e:DeepSeekException){
        Toast.makeText(context,"连接异常: ${e.message}",Toast.LENGTH_SHORT).show()
    }
}
