package site.tiedan.utils

import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.events.MessagePostSendEvent
import net.mamoe.mirai.event.events.MessageRecallEvent
import net.mamoe.mirai.event.events.source
import net.mamoe.mirai.message.data.*
import site.tiedan.Config

internal object MessageRecorder : SimpleListenerHost() {

    private val records: MutableMap<Long, MutableList<MessageSource>> = HashMap()

    @EventHandler(priority = EventPriority.HIGHEST)
    fun MessageEvent.mark() {
        val record = records.getOrPut(subject.id, ::mutableListOf)
        if (record.size >= Config.recordLimit) {
            record.removeFirst()
        }
        record.add(source)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun MessagePostSendEvent<*>.mark() {
        val record = records.getOrPut(target.id, ::mutableListOf)
        if (record.size >= Config.recordLimit) {
            record.removeFirst()
        }
        record.add(source ?: return)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun MessageRecallEvent.mark() {
        when (this) {
            is MessageRecallEvent.FriendRecall -> records[author.id]?.removeIf {
                it.ids.contentEquals(messageIds) && it.internalIds.contentEquals(messageInternalIds)
            }
            is MessageRecallEvent.GroupRecall -> records[group.id]?.removeIf {
                it.ids.contentEquals(messageIds) && it.internalIds.contentEquals(messageInternalIds)
            }
        }
    }

    fun quoteMessage(event: MessageEvent): MessageChain? {
        val quote = event.message.findIsInstance<QuoteReply>() ?: return null
        if (quote.source.originalMessage.contains(Image)) return quote.source.originalMessage
        val recordList = records[event.subject.id] ?: return null
        val sourceIds = quote.source.ids
        return recordList.asReversed().firstOrNull { rec -> rec.ids.any { it in sourceIds } }?.originalMessage
    }
}
