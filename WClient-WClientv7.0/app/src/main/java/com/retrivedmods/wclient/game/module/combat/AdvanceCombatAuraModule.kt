package com.retrivedmods.wclient.game.module.combat

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.entity.Entity
import com.retrivedmods.wclient.game.entity.EntityUnknown
import com.retrivedmods.wclient.game.entity.LocalPlayer
import com.retrivedmods.wclient.game.entity.MobList
import com.retrivedmods.wclient.game.entity.Player
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import kotlin.math.cos
import kotlin.math.sin

class AdvanceCombatAuraModule : Module("AdvanceCombatAura", ModuleCategory.Combat) {

    private var playersOnly by boolValue("players_only", true)
    private var mobsOnly by boolValue("mobs_only", true)
    private var tpAuraEnabled by boolValue("tp_aura", true)
    private var strafe by boolValue("strafe", true)
    private var criticalHits by boolValue("Critical Hit", true)

    private var rangeValue by floatValue("range", 7.0f, 2f..10f)
    private var cpsValue by intValue("cps", 12, 5..20)
    private var apsEnabled by boolValue("Use APS", true)
    private var apsValue by intValue("APS", 18, 5..30)
    private var apsBoostEnabled by boolValue("APS Boost", true)
    private var boostDuration by intValue("Boost Duration", 1000, 100..5000) // in ms
    private var boostInterval by intValue("Boost Interval", 5000, 2000..10000) // in ms

    private var tpSpeed by intValue("tp_speed", 150, 50..2000)
    private var tpYLevel by intValue("yOffset", 0, -10..10)

    private var distanceToKeep by floatValue("keep_distance", 2.0f, 1f..5f)
    private var strafeAngle = 0.0f
    private val strafeSpeed by floatValue("strafe_speed", 2.0f, 1f..3f)
    private val strafeRadius by floatValue("strafe_radius", 3.0f, 2f..6f)
    private var lastAttackTime = 0L
    private var tpCooldown = 0L
    private var boostStartTime = 0L

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet
        if (packet is PlayerAuthInputPacket) {
            val currentTime = System.currentTimeMillis()

            val inBoost = apsBoostEnabled && (currentTime - boostStartTime <= boostDuration)
            if (apsBoostEnabled && currentTime - boostStartTime >= boostInterval + boostDuration) {
                boostStartTime = currentTime
            }

            val attackDelay = if (apsEnabled) {
                val aps = if (inBoost) apsValue + 5 else apsValue
                1000L / aps.coerceAtLeast(1)
            } else {
                1000L / cpsValue.coerceAtLeast(1)
            }

            if ((currentTime - lastAttackTime) >= attackDelay) {
                val closestEntities = searchForClosestEntities()
                if (closestEntities.isEmpty()) return

                closestEntities.forEach { entity ->
                    if (tpAuraEnabled && (currentTime - tpCooldown) >= tpSpeed) {
                        teleportTo(entity, distanceToKeep, tpYLevel)
                        tpCooldown = currentTime
                    }

                    if (criticalHits) triggerCriticalHit()
                    session.localPlayer.attack(entity)
                    if (strafe) strafeAroundTarget(entity)
                    lastAttackTime = currentTime
                }
            }
        }
    }

    private fun triggerCriticalHit() {
        val player = session.localPlayer
        val pos = player.vec3Position

        val jump = MovePlayerPacket().apply {
            runtimeEntityId = player.runtimeEntityId
            position = Vector3f.from(pos.x, pos.y + 0.06f, pos.z)
            rotation = player.vec3Rotation
            mode = MovePlayerPacket.Mode.NORMAL
            isOnGround = false
            tick = player.tickExists
        }

        val land = MovePlayerPacket().apply {
            runtimeEntityId = player.runtimeEntityId
            position = Vector3f.from(pos.x, pos.y - 0.01f, pos.z)
            rotation = player.vec3Rotation
            mode = MovePlayerPacket.Mode.NORMAL
            isOnGround = true
            tick = player.tickExists
        }

        session.clientBound(jump)
        session.clientBound(land)
    }

    private fun strafeAroundTarget(entity: Entity) {
        val targetPos = entity.vec3Position
        strafeAngle += strafeSpeed
        if (strafeAngle >= 360.0f) strafeAngle -= 360.0f

        val offsetX = strafeRadius * cos(Math.toRadians(strafeAngle.toDouble())).toFloat()
        val offsetZ = strafeRadius * sin(Math.toRadians(strafeAngle.toDouble())).toFloat()

        val newPosition = targetPos.add(offsetX, 0f, offsetZ)

        val movePlayerPacket = MovePlayerPacket().apply {
            runtimeEntityId = session.localPlayer.runtimeEntityId
            position = newPosition
            rotation = Vector3f.from(0f, 0f, 0f)
            mode = MovePlayerPacket.Mode.NORMAL
            isOnGround = true
            tick = session.localPlayer.tickExists
        }

        session.clientBound(movePlayerPacket)
    }

    private fun teleportTo(entity: Entity, distance: Float, yOffset: Int) {
        val targetPosition = entity.vec3Position
        val playerPosition = session.localPlayer.vec3Position

        val targetYaw = Math.toRadians(entity.vec3Rotation.y.toDouble()).toFloat()
        val direction = Vector3f.from(sin(targetYaw), 0f, -cos(targetYaw))
        val length = direction.length()
        val normalizedDirection = if (length != 0f) Vector3f.from(direction.x / length, 0f, direction.z / length) else direction

        val newPosition = Vector3f.from(
            targetPosition.x + normalizedDirection.x * distance,
            targetPosition.y + yOffset,
            targetPosition.z + normalizedDirection.z * distance
        )

        val movePlayerPacket = MovePlayerPacket().apply {
            runtimeEntityId = session.localPlayer.runtimeEntityId
            position = newPosition
            rotation = entity.vec3Rotation
            mode = MovePlayerPacket.Mode.NORMAL
            isOnGround = false
            tick = session.localPlayer.tickExists
        }

        session.clientBound(movePlayerPacket)
    }

    private fun Entity.isTarget(): Boolean {
        return when (this) {
            is LocalPlayer -> false
            is Player -> playersOnly && !this.isBot()
            is EntityUnknown -> mobsOnly && isMob()
            else -> false
        }
    }

    private fun EntityUnknown.isMob(): Boolean {
        return this.identifier in MobList.mobTypes
    }

    private fun Player.isBot(): Boolean {
        if (this is LocalPlayer) return false
        val playerList = session.level.playerMap[this.uuid] ?: return true
        return playerList.name.isBlank()
    }

    private fun searchForClosestEntities(): List<Entity> {
        return session.level.entityMap.values
            .filter { it.distance(session.localPlayer) < rangeValue && it.isTarget() }
            .sortedBy { it.distance(session.localPlayer) }
    }
}