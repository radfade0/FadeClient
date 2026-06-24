package com.retrivedmods.wclient.game.module.motion

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.math.vector.Vector3f

class SpiderModule : Module("Spider", ModuleCategory.Motion) {

    private var speedMultiplier by floatValue("SpeedMultiplier", 0.3f, 0.1f..1.5f)


    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet
        if (packet !is MovePlayerPacket) return

        val player = session.localPlayer
        val pos = player.vec3Position

        if (isAgainstWall()) {

            val newY = pos.y + speedMultiplier
            packet.position = Vector3f.from(pos.x, newY, pos.z)
            packet.isOnGround = false
        }
    }


    private fun isAgainstWall(): Boolean {
        // TODO: Implement actual collision or raycast check here.

        return true
    }
}
