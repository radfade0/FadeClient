package com.retrivedmods.wclient.game.module.combat

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.entity.*
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket

class CrystalauraModule : Module("Crystal Aura", ModuleCategory.Combat) {

    private var rangeValue by floatValue("range", 6.0f, 3f..10f)
    private var attackInterval by intValue("delay", 5, 1..20)
    private var cpsValue by intValue("cps", 10, 1..20)
    private var suicideValue by boolValue("Suicide", false)

    private var lastAttackTime = 0L


    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return


        val currentTime = System.currentTimeMillis()
        val minAttackDelay = 1000L / cpsValue

        if ((currentTime - lastAttackTime) >= minAttackDelay) {
            val crystals = searchForCrystals()

            if (crystals.isNotEmpty()) {
                crystals.forEach { crystal ->

                    val damage = calculateCrystalDamage(crystal)


                    if (damage > 0f && (suicideValue || damage < session.localPlayer.health)) {

                        session.localPlayer.attack(crystal)
                        lastAttackTime = currentTime
                    }
                }
            }
        }
    }


    private fun searchForCrystals(): List<EntityUnknown> {
        return session.level.entityMap.values
            .filter { entity ->

                entity is EntityUnknown && entity.identifier == "minecraft:end_crystal" && entity.distance(session.localPlayer) < rangeValue
            }
            .map { it as EntityUnknown }
    }


    private fun calculateCrystalDamage(crystal: EntityUnknown): Float {
        var selfDamage = 0f


        val explosionDamage = 6f

        if (crystal.distance(session.localPlayer) < rangeValue) {
            selfDamage = explosionDamage
        }

        return selfDamage
    }
}
