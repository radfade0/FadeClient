package com.retrivedmods.wclient.game.module.player

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import kotlin.math.cos
import kotlin.math.sin

class BlinkModule : Module("Blink", ModuleCategory.Player) {

    private var blinkDistance by floatValue("Blink Distance", 10.0f, 1.0f..50.0f)
    private var blinkCooldown by intValue("Cooldown", 500, 100..2000)
    private var lastBlinkTime = 0L

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val currentTime = System.currentTimeMillis()
        if ((currentTime - lastBlinkTime) >= blinkCooldown) {
            performBlink()
            lastBlinkTime = currentTime
        }
    }

    private fun performBlink() {
        val player = session.localPlayer
        val playerPosition = player.vec3Position
        val playerRotation = player.vec3Rotation


        val yawRadians = Math.toRadians(playerRotation.y.toDouble()).toFloat()
        val direction = Vector3f.from(sin(yawRadians), 0f, -cos(yawRadians))


        val targetPosition = playerPosition.add(direction.x * blinkDistance, 0f, direction.z * blinkDistance)


        val movePlayerPacket = MovePlayerPacket().apply {
            runtimeEntityId = player.runtimeEntityId
            position = targetPosition
            rotation = playerRotation
            mode = MovePlayerPacket.Mode.NORMAL
            isOnGround = true
            tick = player.tickExists
        }

        session.clientBound(movePlayerPacket)
    }
}