package command

import Config
import ImgLocUploader
import ImgLocUploader.HELP
import ImgLocUploader.logger
import ImgLocUploader.sendQuoteReply
import UploadData
import utils.ImglocAPI
import utils.MessageRecorder.quoteMessage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CommandSenderOnMessage
import net.mamoe.mirai.console.command.RawCommand
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl

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
                val (success, result) = ImglocAPI.uploadImageFromUrlImgLoc(quoteImage.queryUrl(), subject)
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

                    val (success, result) = ImglocAPI.uploadImageFromUrlImgLoc(imageUrl, subject)
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