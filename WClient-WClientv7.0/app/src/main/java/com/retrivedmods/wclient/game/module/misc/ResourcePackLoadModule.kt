package com.retrivedmods.wclient.game.module.misc

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import com.retrivedmods.wclient.application.AppContext
import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import org.cloudburstmc.protocol.bedrock.packet.ResourcePackChunkDataPacket
import org.cloudburstmc.protocol.bedrock.packet.ResourcePackDataInfoPacket
import org.cloudburstmc.protocol.bedrock.packet.ResourcePacksInfoPacket
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID

class ResourcePackLoadModule : Module("resourcepackload", ModuleCategory.Misc) {

    private val saveDirectory = File("/storage/emulated/0/download/WClient")
    private val incomingPacks = HashMap<PackKey, PackState>()

    override fun onEnabled() {
        super.onEnabled()
        ensureStorageAccess()
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) {
            return
        }

        when (val packet = interceptablePacket.packet) {
            is ResourcePacksInfoPacket -> preparePackList(packet)
            is ResourcePackDataInfoPacket -> preparePack(packet)
            is ResourcePackChunkDataPacket -> saveChunk(packet)
        }
    }

    override fun onDisconnect(reason: String) {
        incomingPacks.clear()
    }

    private fun preparePackList(packet: ResourcePacksInfoPacket) {
        if (!hasStorageAccess()) {
            return
        }

        packet.resourcePackInfos.forEach { entry ->
            incomingPacks.putIfAbsent(PackKey(entry.packId, entry.packVersion), PackState())
        }
        packet.behaviorPackInfos.forEach { entry ->
            incomingPacks.putIfAbsent(PackKey(entry.packId, entry.packVersion), PackState())
        }
    }

    private fun preparePack(packet: ResourcePackDataInfoPacket) {
        if (!hasStorageAccess()) {
            return
        }

        val key = PackKey(packet.packId, packet.packVersion)
        val state = incomingPacks.getOrPut(key) { PackState() }
        state.chunkCount = packet.chunkCount.toInt()
        state.packSize = packet.compressedPackSize
        state.file = File(saveDirectory, buildFileName(packet.packId, packet.packVersion))
        state.receivedChunks.clear()

        saveDirectory.mkdirs()
        state.file?.let { file ->
            if (file.exists()) {
                file.delete()
            }
        }
    }

    private fun saveChunk(packet: ResourcePackChunkDataPacket) {
        if (!hasStorageAccess()) {
            return
        }

        val key = PackKey(packet.packId, packet.packVersion)
        val state = incomingPacks.getOrPut(key) { PackState() }
        val file = state.file ?: File(saveDirectory, buildFileName(packet.packId, packet.packVersion)).also {
            state.file = it
        }

        saveDirectory.mkdirs()

        runCatching {
            RandomAccessFile(file, "rw").use { output ->
                output.seek(packet.progress)
                output.write(packet.data.toByteArray())
                state.receivedChunks.add(packet.chunkIndex)

                if (state.isComplete) {
                    if (state.packSize > 0L) {
                        output.setLength(state.packSize)
                    }
                    incomingPacks.remove(key)
                    session.displayClientMessage("§l§c[WClient] §r§aSaved resource pack: ${file.name}")
                }
            }
        }.onFailure {
            session.displayClientMessage("§l§c[WClient] §r§cResource pack save failed: ${it.message}")
        }
    }

    private fun ensureStorageAccess() {
        if (hasStorageAccess()) {
            saveDirectory.mkdirs()
            return
        }

        val context = AppContext.instance
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }

        runCatching { context.startActivity(intent) }
        session.displayClientMessage("§l§c[WClient] §r§eGrant storage access, then enable ResourcePackLoad again.")
    }

    private fun hasStorageAccess(): Boolean {
        val context = AppContext.instance
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun buildFileName(packId: UUID, packVersion: String): String {
        val cleanVersion = packVersion.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "unknown" }
        return "${packId}_$cleanVersion.mcpack"
    }

    private fun io.netty.buffer.ByteBuf.toByteArray(): ByteArray {
        val bytes = ByteArray(readableBytes())
        getBytes(readerIndex(), bytes)
        return bytes
    }

    private data class PackKey(val packId: UUID, val packVersion: String)

    private class PackState {
        var chunkCount = 0
        var packSize = 0L
        var file: File? = null
        val receivedChunks = HashSet<Int>()

        val isComplete: Boolean
            get() = chunkCount > 0 && receivedChunks.size >= chunkCount
    }
}
