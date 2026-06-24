package com.retrivedmods.wclient.game.module.visual

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import org.cloudburstmc.protocol.bedrock.packet.AddPlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.RemoveEntityPacket

class PlayerJoinNotifierModule : Module("Player Logs", ModuleCategory.Visual) {

    private val trackedPlayers = mutableMapOf<Long, String>()

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet

        when (packet) {
            is AddPlayerPacket -> {
                val username = packet.username
                if (
                    packet.uniqueEntityId != session.localPlayer.uniqueEntityId &&
                    username.isNotBlank() &&
                    trackedPlayers.put(packet.uniqueEntityId, username) == null
                ) {
                    session.displayClientMessage("§a[+] §f$username §ajoined")
                }
            }

            is RemoveEntityPacket -> {
                val username = trackedPlayers.remove(packet.uniqueEntityId)
                if (!username.isNullOrBlank()) {
                    session.displayClientMessage("§c[-] §f$username §cleft")
                }
            }
        }
    }

   fun onDisable() {
        trackedPlayers.clear()
    }
}
