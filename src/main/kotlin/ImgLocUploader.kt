import command.*
import utils.MessageRecorder
import command.CommandUpload
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.commandPrefix
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CommandSenderOnMessage
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.message.data.buildMessageChain
import net.mamoe.mirai.utils.info

object ImgLocUploader : KotlinPlugin(
    JvmPluginDescription(
        id = "com.tiedan.ImgLocUploader",
        name = "ImgLoc Uploader",
        version = "1.0.0",
    ) {
        author("tiedan")
        info("""image upload plugin""")
    }
) {
    const val CONNECT_TIMEOUT = 10000
    const val READ_TIMEOUT = 10000
    val HELP =
        "🖼️ 本功能用于将自定义图片上传至图床。上传成功时，您将获得图片链接。\n" +
        "使用 https://imgloc.com/ 提供的上传接口，使用方法如下：\n" +
        "${commandPrefix}upload <引用图片>\n" +
        "${commandPrefix}upload <图片> [图片] [图片]...\n" +
        "${commandPrefix}upload <链接> [链接] [链接]...\n" +
        "${commandPrefix}upload history\n" +
        "⬇️ 通过图片链接下载并查看图片\n" +
        "${commandPrefix}download <链接>\n" +
        "\n" +
        "【禁止内容】请勿上传：R18G图片或严重血腥内容"

    override fun onEnable() {
        Config.reload()
        UploadData.reload()
        GlobalEventChannel.registerListenerHost(MessageRecorder)
        regCommand()
        logger.info { "ImgLoc Uploader Plugin loaded!" }
    }

    override fun onDisable() {
        CommandUpload.unregister()
        CommandDownload.unregister()
    }

    private fun regCommand() {
        CommandUpload.register()
        CommandDownload.register()
    }

    suspend fun CommandSender.sendQuoteReply(msgToSend: String) {
        if (this is CommandSenderOnMessage<*> && Config.quote_enable) {
            sendMessage(buildMessageChain {
                +QuoteReply(fromEvent.message)
                +PlainText(msgToSend)
            })
        } else {
            sendMessage(msgToSend)
        }
    }
}