package com.retrivedmods.wclient.game.module.motion

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.entity.LocalPlayer
import com.retrivedmods.wclient.game.entity.Container
import com.retrivedmods.wclient.game.entity.Entity
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.math.vector.Vector3i
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import org.cloudburstmc.protocol.bedrock.packet.ContainerOpenPacket
import kotlin.math.min
import kotlin.math.sqrt

class TPContainerModule : Module("TP Container", ModuleCategory.Motion) {

    private var maxRange by floatValue("Range", 500f, 10f..500f)
    private var grabSpeed by floatValue("Speed", 8.0f, 1f..50f)
    private var yOffset by floatValue("YOffset", 1.0f, -5f..5f)

    private var lastMoveTime = 0L

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return
        val packet = interceptablePacket.packet
        if (packet !is PlayerAuthInputPacket) return

        val player = session.localPlayer
        val target = findNearestContainer(player) ?: return

        val now = System.currentTimeMillis()
        if (now - lastMoveTime < 50L) return
        lastMoveTime = now

        val playerPos = player.vec3Position
        val targetPos = target.vec3Position.add(0f, yOffset, 0f)

        val dx = targetPos.x - playerPos.x
        val dy = targetPos.y - playerPos.y
        val dz = targetPos.z - playerPos.z
        val distance = sqrt(dx * dx + dy * dy + dz * dz)

        if (distance > maxRange) return

        val maxStep = grabSpeed
        val ratio = min(1.0f, maxStep / distance)
        val newPosition = Vector3f.from(
            playerPos.x + dx * ratio,
            playerPos.y + dy * ratio,
            playerPos.z + dz * ratio
        )

        session.clientBound(MovePlayerPacket().apply {
            runtimeEntityId = player.runtimeEntityId
            position = newPosition
            rotation = player.vec3Rotation
            mode = MovePlayerPacket.Mode.NORMAL
            isOnGround = true
            ridingRuntimeEntityId = 0
            tick = player.tickExists
        })

        if (distance < 1.5f) {
            openContainer(target)
        }
    }

    private fun findNearestContainer(player: LocalPlayer): Entity? {
        return session.level.entityMap.values
            .filterIsInstance<Container>()
            .filter { it.vec3Position.distance(player.vec3Position) <= maxRange }
            .minByOrNull { it.vec3Position.distance(player.vec3Position) }
    }

    private fun openContainer(container: Entity) {
        if (container !is Container) return

        val packet = ContainerOpenPacket().apply {
            id = container.containerId.toByte()
            type = container.containerType
            blockPosition = Vector3i.from(
                container.vec3Position.x.toInt(),
                container.vec3Position.y.toInt(),
                container.vec3Position.z.toInt()
            )
            uniqueEntityId = container.runtimeEntityId
        }
        session.serverBound(packet)
    }
}
