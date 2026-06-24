package com.retrivedmods.wclient.game.module.visual

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import kotlinx.coroutines.*

class FakeProxyModule : Module("FakeProxy", ModuleCategory.Visual) {

    private val fakeProxyMessage = "§b• §f1.20.0 §bᴛᴏ §f1.21.81 §bᴛʀᴀɴꜱʟᴀᴛᴏʀ ᴘʀᴏxʏ •   \n §dʏᴏᴜᴛᴜʙᴇ §f@ʀᴇᴛʀɪᴠᴇᴅɢᴀᴍᴇʀ §dᴅɪꜱᴄᴏʀᴅ §f@ʀᴇᴛʀɪᴠᴇᴅɢᴀᴍᴇʀ"
    private var messageDelaySeconds by floatValue("Delay", 10f, 1f..30f)

    private var messageJob: Job? = null

    private fun ensureMessageLoopRunning() {
        if (messageJob?.isActive != true) {
            messageJob = CoroutineScope(Dispatchers.Default).launch {
                while (isEnabled) {
                    session.displayClientMessage(fakeProxyMessage)
                    delay((messageDelaySeconds * 1000).toLong())
                }
            }
        }
    }

    private fun stopMessageLoop() {
        messageJob?.cancel()
        messageJob = null
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) {
            stopMessageLoop()
            return
        }

        ensureMessageLoopRunning()

        val packet = interceptablePacket.packet
        if (packet is org.cloudburstmc.protocol.bedrock.packet.TextPacket &&
            packet.type == org.cloudburstmc.protocol.bedrock.packet.TextPacket.Type.CHAT
        ) {

            packet.message = fakeProxyMessage
        }
    }
}
