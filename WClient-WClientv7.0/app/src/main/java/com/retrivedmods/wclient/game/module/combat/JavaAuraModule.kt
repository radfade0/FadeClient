package com.retrivedmods.wclient.game.module.combat

import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.entity.Entity
import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.entity.LocalPlayer


class JavaAuraModule : Module("JavaAura", ModuleCategory.Combat) {

    private var attackRange by floatValue("Range", 5.0f, 1.0f..10.0f)
    private var cps by intValue("CPS", 12, 1..20)

    private var lastAttackTime = 0L
    private val attackDelay: Long
        get() = (1000L / cps).coerceAtLeast(50)

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val target = getClosestTarget() ?: return
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastAttackTime >= attackDelay) {


            session.localPlayer.attack(target)
            lastAttackTime = currentTime
        }
    }

    private fun getClosestTarget(): Entity? {
        return session.level.entityMap.values
            .filter { it != session.localPlayer && it.isTarget() && it.distance(session.localPlayer) <= attackRange }
            .minByOrNull { it.distance(session.localPlayer) }
    }

    private fun Entity.isTarget(): Boolean {
        return this !is LocalPlayer
    }



}

