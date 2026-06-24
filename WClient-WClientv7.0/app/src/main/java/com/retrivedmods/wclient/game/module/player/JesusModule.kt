package com.retrivedmods.wclient.game.module.player

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.math.vector.Vector3f

class JesusModule : Module("Jesus", ModuleCategory.Player) {

    // 0 = Solid, 1 = Bounce
    private var modeIndex by intValue("Mode", 0, 0..1)
    private var heightOffset by floatValue("HeightOffset", 0.02f, 0f..0.2f)

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return
        val packet = interceptablePacket.packet
        if (packet !is MovePlayerPacket) return

        val player = session.localPlayer
        val pos = player.vec3Position

        if (isOverLiquid(pos)) {
            when (modeIndex) {
                0 -> {

                    packet.position = Vector3f.from(pos.x, pos.y + heightOffset, pos.z)
                    packet.isOnGround = true
                }
                1 -> {

                    val bounceY = if ((player.tickExists % 2L) == 0L) heightOffset else -heightOffset
                    packet.position = Vector3f.from(pos.x, pos.y + bounceY, pos.z)
                    packet.isOnGround = true
                }
            }
        }
    }


    private fun isOverLiquid(pos: Vector3f): Boolean {

        return true
    }
}
