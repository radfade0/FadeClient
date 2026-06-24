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
import okhttp3.OkHttpClient
import okhttp3.Request
import org.cloudburstmc.protocol.bedrock.packet.ResourcePackChunkDataPacket
import org.cloudburstmc.protocol.bedrock.packet.ResourcePackDataInfoPacket
import org.cloudburstmc.protocol.bedrock.packet.ResourcePacksInfoPacket
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.concurrent.thread

class ResourcePackLoadModule : Module("resourcepackload", ModuleCategory.Misc) {

    private val saveDirectory: File
        get() = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "WClient"
        )

    private val httpClient by lazy { OkHttpClient() }
    private val incomingPacks = HashMap<PackKey, PackState>()

    override fun onEnabled() {
        super.onEnabled()
        ensureStorageAccess()
        if (hasStorageAccess()) {
            ensureServerDirectory()
            displayMessage("§l§c[WClient] §r§aResourcePackLoad ready: ${serverZipFile().absolutePath}")
        }
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
        rebuildServerZip()
        incomingPacks.clear()
    }

    private fun preparePackList(packet: ResourcePacksInfoPacket) {
        if (!ensureWritable()) {
            return
        }

        val entries = packet.resourcePackInfos + packet.behaviorPackInfos
        if (entries.isEmpty()) {
            displayMessage("§l§c[WClient] §r§7ResourcePackLoad: server did not announce resource packs.")
            return
        }

        displayMessage("§l§c[WClient] §r§eResourcePackLoad: found ${entries.size} server packs.")

        entries.forEach { entry ->
            val key = PackKey(entry.packId, entry.packVersion)
            val state = incomingPacks.getOrPut(key) { PackState() }
            state.file = packFile(entry.packId, entry.packVersion, entry.contentId, entry.subPackName)
            state.packSize = entry.packSize
            state.cdnUrl = entry.cdnUrl.orEmpty()

            if (state.cdnUrl.isNotBlank()) {
                downloadCdnPack(key, state)
            }
        }
    }

    private fun preparePack(packet: ResourcePackDataInfoPacket) {
        if (!ensureWritable()) {
            return
        }

        val key = PackKey(packet.packId, packet.packVersion)
        val state = incomingPacks.getOrPut(key) { PackState() }
        state.chunkCount = packet.chunkCount.toInt()
        state.packSize = packet.compressedPackSize
        state.file = state.file ?: packFile(packet.packId, packet.packVersion)
        state.receivedChunks.clear()
        state.complete = false

        state.file?.let { file ->
            if (file.exists()) {
                file.delete()
            }
        }

        displayMessage("§l§c[WClient] §r§eDownloading pack ${state.safeName}: ${state.chunkCount} chunks.")
    }

    private fun saveChunk(packet: ResourcePackChunkDataPacket) {
        if (!ensureWritable()) {
            return
        }

        val key = PackKey(packet.packId, packet.packVersion)
        val state = incomingPacks.getOrPut(key) { PackState() }
        val file = state.file ?: packFile(packet.packId, packet.packVersion).also {
            state.file = it
        }

        runCatching {
            RandomAccessFile(file, "rw").use { output ->
                output.seek(packet.progress)
                output.write(packet.data.toByteArray())
                state.receivedChunks.add(packet.chunkIndex)

                if (state.isComplete(file)) {
                    completePack(key, state, file)
                }
            }
        }.onFailure {
            displayMessage("§l§c[WClient] §r§cResourcePackLoad write failed: ${it.message}")
        }
    }

    private fun downloadCdnPack(key: PackKey, state: PackState) {
        if (state.cdnDownloadStarted || state.complete) {
            return
        }
        state.cdnDownloadStarted = true

        thread(name = "ResourcePackLoadCdn") {
            val file = state.file ?: packFile(key.packId, key.packVersion).also {
                state.file = it
            }

            runCatching {
                val request = Request.Builder()
                    .url(state.cdnUrl)
                    .build()
                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    error("HTTP ${response.code}")
                }

                val body = response.body ?: error("empty response")
                file.outputStream().use { output ->
                    body.byteStream().use { input ->
                        input.copyTo(output)
                    }
                }

                completePack(key, state, file)
            }.onFailure {
                displayMessage("§l§c[WClient] §r§cCDN pack download failed: ${it.message}")
            }
        }
    }

    private fun completePack(key: PackKey, state: PackState, file: File) {
        if (state.complete) {
            return
        }

        state.complete = true
        incomingPacks.remove(key)
        rebuildServerZip()
        displayMessage("§l§c[WClient] §r§aSaved ${file.name} into ${serverZipFile().name}")
    }

    private fun rebuildServerZip() {
        if (!hasStorageAccess()) {
            return
        }

        val directory = ensureServerDirectory()
        val packFiles = directory.listFiles { file ->
            file.isFile && (file.extension.equals("mcpack", true) || file.extension.equals("zip", true))
        }?.sortedBy { it.name }.orEmpty()

        if (packFiles.isEmpty()) {
            return
        }

        val zipFile = serverZipFile()
        val tempZipFile = File(zipFile.parentFile, "${zipFile.name}.tmp")

        runCatching {
            ZipOutputStream(tempZipFile.outputStream()).use { zip ->
                packFiles.forEach { pack ->
                    zip.putNextEntry(ZipEntry(pack.name))
                    pack.inputStream().use { input ->
                        input.copyTo(zip)
                    }
                    zip.closeEntry()
                }
            }

            if (zipFile.exists()) {
                zipFile.delete()
            }
            tempZipFile.renameTo(zipFile)
        }.onFailure {
            tempZipFile.delete()
            displayMessage("§l§c[WClient] §r§cServer zip rebuild failed: ${it.message}")
        }
    }

    private fun ensureWritable(): Boolean {
        if (!hasStorageAccess()) {
            ensureStorageAccess()
            return false
        }

        return runCatching {
            ensureServerDirectory()
            true
        }.getOrElse {
            displayMessage("§l§c[WClient] §r§cCannot create WClient folder: ${it.message}")
            false
        }
    }

    private fun ensureServerDirectory(): File {
        val root = saveDirectory
        val directory = File(root, safeServerName())
        directory.mkdirs()
        return directory
    }

    private fun packFile(packId: UUID, packVersion: String, contentId: String = "", subPackName: String = ""): File {
        val baseName = listOf(contentId, subPackName, packId.toString(), packVersion)
            .filter { it.isNotBlank() }
            .joinToString("_")
        return File(ensureServerDirectory(), "${sanitizeFileName(baseName)}.mcpack")
    }

    private fun serverZipFile(): File {
        val root = saveDirectory
        root.mkdirs()
        return File(root, "${safeServerName()}.zip")
    }

    private fun safeServerName(): String {
        val name = if (isSessionCreated) session.serverAddress else "unknown_server"
        return sanitizeFileName(name).ifBlank { "unknown_server" }
    }

    private fun ensureStorageAccess() {
        if (hasStorageAccess()) {
            saveDirectory.mkdirs()
            return
        }

        val context = AppContext.instance
        val openedSettings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            openSettings(
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                },
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            )
        } else {
            openSettings(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            )
        }

        val message = if (openedSettings) {
            "§l§c[WClient] §r§eGrant storage access, then enable ResourcePackLoad again."
        } else {
            "§l§c[WClient] §r§eOpen Android app settings and grant storage access."
        }
        displayMessage(message)
    }

    private fun openSettings(vararg intents: Intent): Boolean {
        val context = AppContext.instance
        for (intent in intents) {
            val opened = runCatching {
                context.startActivity(intent.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
            }.isSuccess

            if (opened) {
                return true
            }
        }
        return false
    }

    private fun hasStorageAccess(): Boolean {
        val context = AppContext.instance
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun displayMessage(message: String) {
        if (isSessionCreated) {
            session.displayClientMessage(message)
        }
    }

    private fun sanitizeFileName(value: String): String {
        return value.replace(Regex("[^A-Za-z0-9._-]"), "_").trim('_')
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
        var cdnUrl = ""
        var cdnDownloadStarted = false
        var complete = false
        val receivedChunks = HashSet<Int>()

        val safeName: String
            get() = file?.name ?: "unknown pack"

        fun isComplete(file: File): Boolean {
            val chunkComplete = chunkCount > 0 && receivedChunks.size >= chunkCount
            val sizeComplete = packSize <= 0L || file.length() >= packSize
            return chunkComplete && sizeComplete
        }
    }
}
