package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import org.cloudburstmc.protocol.bedrock.packet.DisconnectPacket

class AutoDisconnectModule : Module("AutoDisconnect", ModuleCategory.Misc) {

    private var hasDisconnected = false

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled || hasDisconnected) return


        disconnectPlayer()
    }

    private fun disconnectPlayer() {
        val disconnectPacket = DisconnectPacket().apply {
            kickMessage = "Disconnected by §cWClient§r AutoDisconnect Module"
        }

        session.clientBound(disconnectPacket)
        session.serverBound(disconnectPacket)

        hasDisconnected = true
        isEnabled = false
        println("AutoDisconnect: Disconnected immediately upon enable.")
    }
}
