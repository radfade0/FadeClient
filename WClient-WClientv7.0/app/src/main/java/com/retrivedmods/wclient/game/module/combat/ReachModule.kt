package com.retrivedmods.wclient.game.module.combat

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.entity.Entity
import com.retrivedmods.wclient.game.entity.LocalPlayer
import com.retrivedmods.wclient.game.entity.Player
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket


class ReachModule : Module("Reach", ModuleCategory.Combat) {

    private var combatReachEnabled by boolValue("Combat", true)
    private var combatReach by floatValue("Reach", 3.0f, 1.0f..10.0f)


    private var lastAttackTime = 0L
    private val attackDelayMs = 100L

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled || !combatReachEnabled) return

        val packet = interceptablePacket.packet
        if (packet !is PlayerAuthInputPacket) return

        val player = session.localPlayer
        val now = System.currentTimeMillis()

        if (now - lastAttackTime < attackDelayMs) return

        val target = findBestTarget(player)
        if (target != null) {
            // Simulate reach hit
            player.swing()
            player.attack(target)
            lastAttackTime = now
        }
    }

    private fun findBestTarget(player: LocalPlayer): Player? {
        return session.level.entityMap.values
            .asSequence()
            .filterIsInstance<Player>()
            .filter { it != player && !isBot(it) && isInReach(player, it) }
            .sortedBy { it.vec3Position.distanceSquared(player.vec3Position) }
            .firstOrNull()
    }

    private fun isInReach(player: LocalPlayer, target: Player): Boolean {
        val reachSqr = combatReach * combatReach
        val distanceSqr = target.vec3Position.distanceSquared(player.vec3Position)
        return distanceSqr <= reachSqr && distanceSqr > 0f
    }

    private fun isBot(entity: Entity): Boolean {
        if (entity !is Player || entity is LocalPlayer) return true
        val data = session.level.playerMap[entity.uuid]
        return data?.name.isNullOrBlank()
    }
}
