package com.retrivedmods.wclient.game.module.combat

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.entity.Entity
import com.retrivedmods.wclient.game.entity.LocalPlayer
import com.retrivedmods.wclient.game.entity.Player
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket

class SmartAuraModule : Module("SmartAura", ModuleCategory.Combat) {

    private var attackRange by floatValue("Range", 6f, 1f..10f)
    private var delayMs by intValue("Delay", 100, 0..500)

    private var lastAttackTime = 0L

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return
        if (interceptablePacket.packet !is PlayerAuthInputPacket) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAttackTime < delayMs) return

        val player = session.localPlayer
        val target = findNearestEnemy(player) ?: return

        player.swing()
        player.attack(target)

        lastAttackTime = currentTime
    }

    private fun findNearestEnemy(player: LocalPlayer): Entity? {
        return session.level.entityMap.values
            .filterIsInstance<Player>()
            .filter { it != player && !isBot(it) }
            .filter { it.vec3Position.distance(player.vec3Position) <= attackRange }
            .minByOrNull { it.vec3Position.distance(player.vec3Position) }
    }

    private fun isBot(player: Player): Boolean {
        if (player is LocalPlayer) return false
        val info = session.level.playerMap[player.uuid]
        return info?.name.isNullOrBlank()
    }
}
