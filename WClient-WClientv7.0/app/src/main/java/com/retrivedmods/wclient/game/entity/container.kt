package com.retrivedmods.wclient.game.entity

import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType

open class Container(
    val containerId: Int,
    val containerType: ContainerType,
    runtimeEntityId: Long
) : Entity(runtimeEntityId) {
    // Add extra logic if needed
}
