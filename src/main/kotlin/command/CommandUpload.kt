package com.tiedan.command

import com.tiedan.Config
import com.tiedan.ImgLocUploader
import com.tiedan.ImgLocUploader.logger
import com.tiedan.ImgLocUploader.sendQuoteReply
import com.tiedan.utils.MessageRecorder.quoteMessage
import com.tiedan.utils.UploadImage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.commandPrefix
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CommandSenderOnMessage
import net.mamoe.mirai.console.command.RawCommand
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl

object CommandUpload : RawCommand(
    owner = ImgLocUploader,
    primaryName = "upload",
    secondaryNames = arrayOf("上传", "图床"),
    description = "上传图片至图床，获取图片链接"
) {
    private val uploadLock = Mutex()
    private val HELP =
        "\uD83D\uDDBC\uFE0F 本功能用于将自定义图片上传至图床。上传成功时，您将获得图片链接。\n" +
        "使用 https://imgloc.com/ 提供的上传接口，使用方法如下：\n" +
        "${commandPrefix}upload <引用一张图片>\n" +
        "${commandPrefix}upload <图片> [图片] [图片]...\n" +
        "${commandPrefix}upload <链接> [链接] [链接]...\n" +
        "\n" +
        "【禁止内容】请勿上传：儿童色情内容、严重血腥内容、对未成年人的性暴力。"

    override suspend fun CommandSender.onCommand(args: MessageChain) {
        if (Config.API_Key.isEmpty()) {
            sendQuoteReply("请先在Config中填写API Key，可以在imgloc个人账号下找到")
            return
        }
        // 尝试引用消息获取图片
        if (this is CommandSenderOnMessage<*> && fromEvent.message[QuoteReply.Key] != null) {
            val messages = quoteMessage(event = fromEvent)
            // TODO 支持获取引用消息中的所有图片
            val quoteImage = messages?.firstIsInstanceOrNull<Image>()

            if (quoteImage == null) {
                sendQuoteReply("[获取失败] 请回复一条近期包含图片的消息，或尝试保存图片后发图上传")
                return
            }

            uploadLock.withLock {
                val (success, result) = UploadImage.uploadImageFromUrlImgLoc(quoteImage.queryUrl(), subject)
                sendQuoteReply(if (success) "[上传成功] 图片链接为：\n$result" else result)
            }
            return
        }
        // 无引用遍历消息链上传
        uploadLock.withLock {
            try {
                val resultMessage = StringBuilder("【操作成功】上传结果如下：\n")
                var totalCount = 0
                var successCount = 0

                for (arg in args) {
                    val imageUrl = when (arg) {
                        is Image -> arg.queryUrl()
                        else -> {
                            val url = arg.content
                            if (!url.startsWith("http")) continue
                            url
                        }
                    }

                    val (success, result) = UploadImage.uploadImageFromUrlImgLoc(imageUrl, subject)
                    resultMessage.appendLine(result)
                    totalCount++
                    if (success) successCount++
                }

                if (totalCount > 0) {
                    resultMessage.append("·上传总数：$totalCount（成功：$successCount）")
                    sendQuoteReply(resultMessage.toString())
                } else {
                    sendQuoteReply(HELP)
                }
            } catch (e: Exception) {
                logger.warning(e)
                sendQuoteReply("[发生未知错误] 请查看后台错误信息：${e::class.simpleName}(${e.message})")
            }
        }
    }
}