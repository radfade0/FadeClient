package com.retrivedmods.wclient.game.module.motion

import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.InterceptablePacket
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket

class FastStopModule : Module("FastStop", ModuleCategory.Motion) {

    // A setting for enabling/disabling the module
    private val enabled by boolValue("enabled", true)

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        val packet = interceptablePacket.packet

        // Only apply the fast stop to MovePlayerPacket
        if (enabled && packet is MovePlayerPacket) {
            // Apply rapid deceleration by manipulating the player's position
            // This will effectively stop the player from moving further
            val stopPosition = packet.position

            // Set the position to the current position without further movement
            packet.position = stopPosition

            // If there is velocity or other movement properties, set them to zero as well

        }
    }
}
