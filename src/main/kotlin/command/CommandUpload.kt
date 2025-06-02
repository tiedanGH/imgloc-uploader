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
        "【禁止内容】请勿上传：儿童色情内容、严重血腥内容、对未成年人的性暴力。允许NSFW图片，但只能私用，禁止添加至bot任何公共图库！\n" +
        "日志会记录所有上传行为，如果您违反了上述任何被禁止的内容，一经发现将会被bot永久拉黑并禁止使用本bot所有功能！"

    override suspend fun CommandSender.onCommand(args: MessageChain) {
        if (Config.API_Key.isEmpty()) {
            sendQuoteReply("请先在Config中填写API Key，可以在imgloc个人账号下找到")
            return
        }
        // 先尝试引用获取图片
        var quoteImage: Image? = null
        if (this is CommandSenderOnMessage<*>) {
            val image = fromEvent.message[QuoteReply.Key]
                ?.let { quoteMessage(event = fromEvent) }
                ?.firstIsInstanceOrNull<Image>()

            if (image == null) {
                sendQuoteReply("[获取失败] 请回复一条近期包含图片的消息，或尝试保存图片后发图上传")
                return
            }
            quoteImage = image
        }
        if (quoteImage != null) {
            uploadLock.withLock {
                val (success, result) = UploadImage.uploadImageFromUrlImgLoc(quoteImage.queryUrl(), subject)
                sendQuoteReply(if (success) "[上传成功] 图片链接为：\n$result" else result)
            }
            return
        }
        // 无引用遍历消息链
        if (args.size == 0) {
            sendQuoteReply(HELP)
            return
        }
        uploadLock.withLock {
            try {
                var message = "【操作成功】上传结果如下："
                var total = 0
                var successCount = 0
                for (arg in args) {
                    val imageUrl = try {
                        (arg as Image).queryUrl()
                    } catch (e: ClassCastException) {
                        if (!arg.content.startsWith("http")) {
                            if (args.size == 1) message = HELP
                            continue
                        }
                        arg.content
                    }
                    val (success, result) = UploadImage.uploadImageFromUrlImgLoc(imageUrl, subject)
                    message += "\n$result"
                    total++
                    if (success) successCount++
                }
                message += "\n·上传总数：$total（成功：$successCount）"
                sendQuoteReply(message)
            } catch (e: Exception) {
                logger.warning(e)
                sendQuoteReply("[发生未知错误] 请查看后台错误信息：${e::class.simpleName}(${e.message})")
            }
        }
    }
}