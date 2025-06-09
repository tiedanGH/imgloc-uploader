package com.tiedan.command

import com.tiedan.ImgLocUploader
import com.tiedan.ImgLocUploader.logger
import com.tiedan.ImgLocUploader.sendQuoteReply
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.commandPrefix
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.RawCommand
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.content
import utils.DownloadHelper.downloadFile
import java.io.File

object CommandDownload : RawCommand(
    owner = ImgLocUploader,
    primaryName = "download",
    secondaryNames = arrayOf("下载"),
    description = "从图片链接下载图片"
) {
    private val downloadLock = Mutex()
    private val HELP =
        "⬇️ 通过图片链接下载并查看图片：\n" +
        "${commandPrefix}download <链接>"

    override suspend fun CommandSender.onCommand(args: MessageChain) {
        if (args.isEmpty()) {
            sendQuoteReply(HELP)
            return
        }
        downloadLock.withLock {
            val tempFile = File.createTempFile("download_", "")
            try {
                val imageUrl = args[0].content
                val downloadResult = downloadFile(imageUrl, tempFile.path)
                if (!downloadResult.success) {
                    return sendQuoteReply(downloadResult.message)
                }
                subject?.sendImage(tempFile)
            } catch (e: Exception) {
                logger.warning(e)
                sendQuoteReply("发生错误：${e.message}")
            } finally {
                tempFile.delete()
            }
        }
    }
}