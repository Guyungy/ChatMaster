package com.liganma.chatmaster.data

import com.alibaba.fastjson2.annotation.JSONField

/**
 * 建议消息
 */
data class SuggestMessage (
    @JSONField(name = "attitude")
    val attitude: String,

    @JSONField(name = "next")
    val next: String,

    @JSONField(name = "suggest")
    val suggest: List<String>,

    @JSONField(name = "analyze")
    var analyze: String
)