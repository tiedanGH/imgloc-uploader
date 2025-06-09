package com.tiedan

import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

@PublishedApi
internal object UploadData : AutoSavePluginData("UploadData") {

    @ValueDescription("近期上传历史")
    var history: MutableList<String> by value()

}