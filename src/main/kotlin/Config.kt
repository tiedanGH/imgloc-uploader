package com.tiedan

import net.mamoe.mirai.console.data.ReadOnlyPluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object Config : ReadOnlyPluginConfig("Config") {

    @ValueDescription("填写imgloc账号的API key")
    var API_Key: String by value()

    @ValueDescription("下载和上传超时时间")
    var timeout: Long by value(30L)

    @ValueDescription("启用引用回复")
    var quote_enable: Boolean by value(true)

    @ValueDescription("记录消息上限")
    var recordLimit: Int by value(500)

}