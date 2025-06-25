package site.tiedan.command

import site.tiedan.ImgLocUploader.HELP
import site.tiedan.ImgLocUploader.logger
import site.tiedan.ImgLocUploader.sendQuoteReply
import site.tiedan.utils.MessageRecorder.quoteMessage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CommandSenderOnMessage
import net.mamoe.mirai.console.command.RawCommand
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import site.tiedan.Config
import site.tiedan.ImgLocUploader
import site.tiedan.utils.ImglocAPI
import site.tiedan.UploadData

object CommandUpload : RawCommand(
    owner = ImgLocUploader,
    primaryName = "upload",
    secondaryNames = arrayOf("上传"),
    description = "上传图片至图床，获取图片链接"
) {
    private val uploadLock = Mutex()

    override suspend fun CommandSender.onCommand(args: MessageChain) {
        if (Config.API_Key.isEmpty()) {
            sendQuoteReply("请先在Config中填写API Key，可以在imgloc个人账号下找到")
            return
        }
        if (args.getOrNull(0)?.content in arrayOf("history", "历史")) {
            sendQuoteReply(" · 近期上传历史：\n" + UploadData.history.joinToString("\n"))
            return
        }
        var messages: MessageChain = args
        // 如果存在引用消息：改为获取其中的图片
        if (this is CommandSenderOnMessage<*> && fromEvent.message[QuoteReply.Key] != null) {
            messages = quoteMessage(event = fromEvent) ?: messageChainOf()

            if (messages.contains(Image).not()) {
                sendQuoteReply("[获取失败] 请回复一条近期包含图片的消息，或尝试保存图片后发图上传")
                return
            }
        }
        // 遍历消息链上传
        uploadLock.withLock {
            try {
                var totalCount = 0
                var successCount = 0
                var lastSuccess = false
                var lastResult = ""

                val multiResult = buildString {
                    append("【操作成功】上传结果如下：\n")
                    for (msg in messages) {
                        val imageUrl = when (msg) {
                            is Image -> msg.queryUrl()
                            else -> {
                                val url = msg.content
                                if (!url.startsWith("http")) continue
                                url
                            }
                        }

                        val (success, result) = ImglocAPI.uploadImageFromUrlImgLoc(imageUrl, subject)
                        appendLine(result)

                        totalCount++
                        if (success) successCount++
                        lastSuccess = success
                        lastResult = result
                    }
                    appendLine("·上传总数：$totalCount（成功：$successCount）")
                }

                when (totalCount) {
                    0 -> sendQuoteReply(HELP)
                    1 -> sendQuoteReply(if (lastSuccess) "【上传成功】图片链接为：\n$lastResult" else lastResult)
                    else -> sendQuoteReply(multiResult)
                }
            } catch (e: Exception) {
                logger.warning(e)
                sendQuoteReply("[发生未知错误] 请查看后台错误信息：${e::class.simpleName}(${e.message})")
            }
        }
    }
}