package com.tiedan

import com.tiedan.command.*
import com.tiedan.utils.MessageRecorder
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
        name = "imgloc-uploader",
        version = "1.0.0",
    ) {
        author("tiedan")
        info("""image upload plugin""")
    }
) {
    const val CONNECT_TIMEOUT = 10000
    const val READ_TIMEOUT = 10000

    override fun onEnable() {
        Config.reload()
        UploadData.reload()
        GlobalEventChannel.registerListenerHost(MessageRecorder)
        regCommand()
        logger.info { "imgloc-uploader Plugin loaded!" }
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