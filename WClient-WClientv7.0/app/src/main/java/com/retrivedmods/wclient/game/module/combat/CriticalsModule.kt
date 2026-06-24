package com.retrivedmods.wclient.game.module.combat

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.entity.Player
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import org.cloudburstmc.math.vector.Vector3f

class CriticalsModule : Module("Criticals", ModuleCategory.Combat) {

    private var autoJump by boolValue("AutoJump", true)


    private var clientTickCounter = 0


    private var lastPlayerY = 0f

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return
        val packet = interceptablePacket.packet
        if (packet !is PlayerAuthInputPacket) return

        val player = session.localPlayer


        lastPlayerY = player.vec3Position.y

        if (autoJump && isOnGround(player) && isTargetNearby()) {

            val jumpHeight = 0.42f
            val currentPos = player.vec3Position
            val newPos = Vector3f.from(currentPos.x, currentPos.y + jumpHeight, currentPos.z)

            clientTickCounter = (clientTickCounter + 1) and 0xFFFF

            session.clientBound(MovePlayerPacket().apply {
                runtimeEntityId = player.runtimeEntityId
                position = newPos
                rotation = player.vec3Rotation
                mode = MovePlayerPacket.Mode.NORMAL
                isOnGround = false
                tick = clientTickCounter.toLong()
            })
        }
    }

    private fun isOnGround(player: Player): Boolean {



        val deltaY = player.vec3Position.y - lastPlayerY
        return deltaY < 0.001f // almost no vertical movement = on ground


    }

    private fun isTargetNearby(): Boolean {
        return session.level.entityMap.values
            .filterIsInstance<Player>()
            .any { it != session.localPlayer && !isBot(it) && it.distance(session.localPlayer) <= 6f }
    }

    private fun isBot(player: Player): Boolean {
        if (player == session.localPlayer) return false
        val info = session.level.playerMap[player.uuid]
        return info?.name.isNullOrBlank()
    }
}
