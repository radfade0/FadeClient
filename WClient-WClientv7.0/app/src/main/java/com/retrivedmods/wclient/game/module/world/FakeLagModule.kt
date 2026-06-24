package com.retrivedmods.wclient.game.module.world

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.random.Random

class FakeLagModule : Module("FakeLag", ModuleCategory.World) {

    private var lagChance by floatValue("LagChance", 0.5f, 0f..1f)

    private val packetQueue = ConcurrentLinkedQueue<InterceptablePacket>()

    @OptIn(DelicateCoroutinesApi::class)
    override fun onEnabled() {
        super.onEnabled()
        GlobalScope.launch {
            while (isEnabled) {
                while (packetQueue.isNotEmpty()) {
                    val pkt = packetQueue.poll()
                    if (pkt != null) {

                        session.clientBound(pkt.packet)
                    }
                    delay(Random.nextLong(100, 300))
                }
                delay(50)
            }
        }
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet
        if (packet is MovePlayerPacket) {

            if (Random.nextFloat() < lagChance) {

                packetQueue.add(interceptablePacket)
                interceptablePacket.intercept()
            }
        }
    }
}
