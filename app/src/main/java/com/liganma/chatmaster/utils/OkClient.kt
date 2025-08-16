package com.liganma.chatmaster.utils

import cn.lishiyuan.deepseek.Client
import cn.lishiyuan.deepseek.api.BaseRequest
import cn.lishiyuan.deepseek.api.BaseResponse
import cn.lishiyuan.deepseek.api.BaseStreamRequest
import cn.lishiyuan.deepseek.api.BaseStreamResponse
import cn.lishiyuan.deepseek.config.Config
import cn.lishiyuan.deepseek.e.DeepSeekException
import cn.lishiyuan.deepseek.e.DeepseekErrorEnum
import com.alibaba.fastjson2.JSON
import lombok.extern.slf4j.Slf4j
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.http.HttpMethod
import java.io.IOException
import java.util.function.Consumer


val SK_REGEX = Regex("^sk-[a-zA-Z0-9]{10,}$")

@Slf4j
class OkClient(private val config: Config) : Client {

    val JSON_MEDIA_TYPE: MediaType ="application/json".toMediaType()


    constructor(accessKey: String?) : this(Config(accessKey))

    constructor(accessKey: String?, baseUrl: String?) : this(Config(accessKey, baseUrl))

    private var client: OkHttpClient = OkHttpClient.Builder().connectTimeout(config.connectTimeout).readTimeout(config.readTimeout).build()


    init {
        initClient()
    }


    private fun initClient() {

    }

    private fun defaultHeader(): Request.Builder {
        return Request.Builder()
            .header("Authorization", "Bearer " + config.accessKey)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
    }

    override fun <T : BaseResponse?> get(request: BaseRequest<T>): T {
        return http(request, "GET")
    }

    override fun <T : BaseResponse?> post(request: BaseRequest<T>): T {
        return http(request, "POST")
    }

    override fun <T : BaseResponse?> http(request: BaseRequest<T>, method: String): T {
        val jsonString = JSON.toJSONString(request)
        val builder: Request.Builder = defaultHeader()
        val httpRequest: Request = builder
            .method(
                method,
                (if (HttpMethod.requiresRequestBody(method)) {
                    jsonString.toRequestBody(JSON_MEDIA_TYPE)
                }else {
                    null
                })
            )
            .url(config.baseUrl + request.path)
            .build()

        try {

            val response = client.newCall(httpRequest).execute()
            if (response.code == 200) {
                val body: String? = response.body?.string()
                if (body==null){
                    throw DeepSeekException("接口异常")
                }else{
                    return JSON.parseObject(body, request.responseClass)
                }
            }
            val deepseekErrorEnum = response.let { DeepseekErrorEnum.fromCode(it.code) }
            if (deepseekErrorEnum != null) {
                throw DeepSeekException(deepseekErrorEnum.desc)
            }else{
                throw DeepSeekException("接口异常")
            }
        } catch (e: IOException) {
            throw DeepSeekException("接口异常：" + e.message, e)
        } catch (e: InterruptedException) {
            throw DeepSeekException("接口异常：" + e.message, e)
        }
    }

    override fun <T : BaseStreamResponse?> stream(
        request: BaseStreamRequest<T>,
        consumer: Consumer<T>
    ) {
        throw DeepSeekException("接口未实现")
    }
}