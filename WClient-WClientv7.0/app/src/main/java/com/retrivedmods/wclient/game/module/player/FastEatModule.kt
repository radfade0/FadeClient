package com.retrivedmods.wclient.game.module.player

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import org.cloudburstmc.protocol.bedrock.packet.PlayerStartItemCooldownPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket

class FastEatModule : Module("FastEat", ModuleCategory.Player) {

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return
        val packet = interceptablePacket.packet


        if (packet is PlayerStartItemCooldownPacket) {

            interceptablePacket.isIntercepted
        }


        if (packet is PlayerAuthInputPacket) {

        }
    }
}
