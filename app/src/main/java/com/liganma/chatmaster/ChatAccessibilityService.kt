package com.liganma.chatmaster

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.alibaba.fastjson2.to
import com.blankj.utilcode.util.CollectionUtils.isEmpty
import com.blankj.utilcode.util.CollectionUtils.isNotEmpty
import com.liganma.chatmaster.data.SuggestItem
import com.liganma.chatmaster.data.SuggestMessage
import com.liganma.chatmaster.DEFAULT_OPENAI_BASE_URL
import com.liganma.chatmaster.DEFAULT_OPENAI_MODEL
import com.liganma.chatmaster.utils.ChatCompletionRequest
import com.liganma.chatmaster.utils.OpenAiException
import com.liganma.chatmaster.utils.OpenAiMessage
import com.liganma.chatmaster.utils.OpenAiResponseFormat
import com.liganma.chatmaster.utils.OkClient
import com.liganma.chatmaster.utils.SK_REGEX
import com.ven.assists.AssistsCore
import com.ven.assists.AssistsCore.findByText
import com.ven.assists.AssistsCore.getAllText
import com.ven.assists.AssistsCore.getBoundsInScreen
import com.ven.assists.AssistsCore.getChildren
import com.ven.assists.AssistsCore.getNodes
import com.ven.assists.AssistsCore.isImageView
import com.ven.assists.AssistsCore.isRelativeLayout

val MSG_FORMAT = """
        聊天上下文如下：
        ```
        %s
        ```
        注意当前的上下文包含的时间信息与当前聊天场景是群聊还是私聊。
        注意如果出现三个及以上人数则为群聊，注意【我】表示为当前用户发送的信息。
        注意如果消息中携带的额外消息，比如时间等等
""".trimIndent()
val HEADER_REGEX = Regex("头像")

val EMPTY = SuggestMessage("无","无", listOf(SuggestItem("无","无")),"无")

fun  analyzingPage(context: Context):SuggestMessage{

    val app = context.applicationContext as App
    val accessKey = app.settingsRepository.accessKey

    if (!SK_REGEX.matches(accessKey.value)) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context.applicationContext,"密钥错误",Toast.LENGTH_SHORT).show()
        }
        return EMPTY
    }

    if(!AssistsCore.isAccessibilityServiceEnabled()){
        // 3. 强制切回主线程
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context.applicationContext,"没有无障碍权限",Toast.LENGTH_SHORT).show();
        }

        return EMPTY
    }

    // 只在微信有效
    if(AssistsCore.getPackageName().startsWith("com.tencent.mm")){
        //  确认聊天界面
        val messageScreenFlag = AssistsCore.findByTags("android.widget.ImageView", des = "聊天信息")
        if(isEmpty(messageScreenFlag)){
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context.applicationContext,"请在微信聊天页使用",Toast.LENGTH_SHORT).show()
            }
            return EMPTY;
        }

        var msgContext = "无"

        // RecyclerView >> RelativeLayout每条聊天记录
        val recyclerView = AssistsCore.findByTags("androidx.recyclerview.widget.RecyclerView")
        if(isNotEmpty(recyclerView)){
            // 提取内容
            // AssistsCore.getAppWidthInScreen() 如果头像在中线左边则是当前对方发送的，如果在中线右边则是自己发送的，
            val appWidthInScreen = AssistsCore.getAppWidthInScreen()
            val midLine = appWidthInScreen / 2

            var chatList:List<AccessibilityNodeInfo> = emptyList()
            for (view in recyclerView) {
                val childList = view.getChildren()
                var isChatView = false
                for (chat in childList) {
                    if (chat.isRelativeLayout()){
                        isChatView = true
                        break
                    }
                }

                if (isChatView){
                    chatList = childList
                    break
                }
            }

            if(isNotEmpty(chatList)){
                //
                val msg = StringBuilder()
                for (chat in chatList) {
                    // 是否是头像
                    val head = chat.findByText("头像", filterClass = "android.widget.ImageView")
                    if(!isEmpty(head)){
                        // 判断是自己发的还是对方发的
                        val first = head.first()
                        val boundsInScreen = first.getBoundsInScreen()
                        // 在左边表示是对方发的
                        if (boundsInScreen.left < midLine){
                            var desc = first.contentDescription
                            desc = desc.replace(HEADER_REGEX, "")
                            msg.append("${desc}:")
                        }else{
                            msg.append("【我】:")
                        }
                    }

                    val nodes = chat.getNodes()
                    // 每行数据
                    for (node in nodes) {
                        if(node.isImageView() && node.contentDescription != null && node.contentDescription.contains("头像")){
                            continue
                        }
                        msg.append(node.getAllText()).append(" ")
                    }
                    // 换行处理
                    msg.append(System.lineSeparator())
                }
                // 聊天记录设置
                msgContext = msg.toString();
            }

        }

        Log.d("ChatAccessibilityService","msgcontent:${msgContext}")

        val prompt = app.settingsRepository.prompt

        val baseUrl = app.settingsRepository.baseUrl.value.ifBlank { DEFAULT_OPENAI_BASE_URL }
        val client = OkClient(accessKey.value, baseUrl)

        val chatRequest = ChatCompletionRequest(
            model = DEFAULT_OPENAI_MODEL,
            messages = listOf(
                OpenAiMessage(role = "system", content = prompt.value),
                OpenAiMessage(role = "user", content = MSG_FORMAT.format(msgContext))
            ),
            responseFormat = OpenAiResponseFormat(type = "json_object")
        )

        try{

            val chatResponse = client.createChatCompletion(chatRequest)
            val choice = chatResponse.choices.firstOrNull()
                ?: throw OpenAiException("未获取到模型返回结果")
            val jsonString = choice.message.content
            if (jsonString.isBlank()) {
                throw OpenAiException("模型返回内容为空")
            }
            Log.d("ChatAccessibilityService","result:${jsonString}")
            val suggestMessage = jsonString.to<SuggestMessage>()
            return suggestMessage
        }catch (e:OpenAiException){
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context.applicationContext,"接口异常: ${e.message}",Toast.LENGTH_SHORT).show()
            }

            return EMPTY
        }catch (e:Exception){
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context.applicationContext,"接口异常: ${e.message}",Toast.LENGTH_SHORT).show()
            }

            return EMPTY
        }

    }else{
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context.applicationContext,"请在微信聊天页使用",Toast.LENGTH_SHORT).show()
        }
        return EMPTY;
    }
}

fun testKey(context: Context){
    val app = context.applicationContext as App
    val accessKey = app.settingsRepository.accessKey

    if (!SK_REGEX.matches(accessKey.value)) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context.applicationContext,"密钥错误",Toast.LENGTH_SHORT).show()
        }
        return
    }

    val baseUrl = app.settingsRepository.baseUrl.value.ifBlank { DEFAULT_OPENAI_BASE_URL }
    val client = OkClient(accessKey.value, baseUrl)
    try {
        client.verifyCredentials()

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context.applicationContext,"连接成功",Toast.LENGTH_LONG).show()
        }

    }catch (e:OpenAiException){
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context.applicationContext(),"连接异常: ${e.message}",Toast.LENGTH_SHORT).show()
        }
    }catch (e:Exception){
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context.applicationContext(),"连接异常: ${e.message}",Toast.LENGTH_SHORT).show()
        }
    }
}
