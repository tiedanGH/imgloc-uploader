package site.tiedan.command

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.RawCommand
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.content
import site.tiedan.ImgLocUploader
import site.tiedan.ImgLocUploader.HELP
import site.tiedan.ImgLocUploader.logger
import site.tiedan.ImgLocUploader.save
import site.tiedan.ImgLocUploader.sendQuoteReply
import site.tiedan.UploadData
import site.tiedan.utils.ImglocAPI

object CommandDelete : RawCommand(
    owner = ImgLocUploader,
    primaryName = "delete",
    secondaryNames = arrayOf("删除"),
    description = "删除已上传的图片"
) {
    override suspend fun CommandSender.onCommand(args: MessageChain) {
        if (args.isEmpty()) {
            sendQuoteReply(HELP)
            return
        }
        try {
            val id = args[0].content
            val item = UploadData.history.firstOrNull { it.first.contains(id) }

            if (item != null) {
                val deleteUrl = item.second
                if (deleteUrl != null) {
                    val (success, result) = ImglocAPI.deleteImageFromUrlImgLoc(deleteUrl)
                    if (!success) {
                        sendQuoteReply(result)
                        return
                    }
                    UploadData.history.remove(item)
                    UploadData.save()
                    sendQuoteReply("[删除成功] 在账号下删除图片：${item.first}")
                } else {
                    sendQuoteReply("[删除失败] 此图片ID记录下的删除链接为空")
                }
            } else {
                sendQuoteReply("[删除失败] 未能在上传记录中找到此图片ID")
            }
        } catch (e: Exception) {
            logger.warning(e)
            sendQuoteReply("[删除失败] 请求发生错误：${e.message}")
        }
    }
}