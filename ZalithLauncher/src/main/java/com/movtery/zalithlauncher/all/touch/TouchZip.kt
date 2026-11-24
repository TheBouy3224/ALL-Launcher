package com.movtery.zalithlauncher.all.touch

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * ALL profile as one ZIP (share a profile with its pictures / GIFs): `manifest.json`, `profile.json` (the profile file exactly as TouchStore keeps it),
 * `images/<name>` for every picture the profile uses. Pure bytes in / out (no Android), TouchStore does the profile and picture handling.
 * Reading never trusts the archive: entry names are never used as paths, only `images/<safe name>` is accepted, counts and sizes are capped.
 */
object TouchZip {
    const val FORMAT = "all-touch-profile"
    private const val MANIFEST = "manifest.json"
    private const val PROFILE = "profile.json"
    private const val IMAGES = "images/"
    private const val MAX_ENTRIES = 400
    private const val MAX_PROFILE = 8 * 1024 * 1024
    private const val MAX_IMAGE = 10 * 1024 * 1024
    private const val MAX_TOTAL = 64L * 1024 * 1024
    private val SAFE_NAME = Regex("[A-Za-z0-9_-]{1,64}[.](png|jpg|jpeg|webp|gif)", RegexOption.IGNORE_CASE)

    /** [skipped] = archive entries that were ignored (unsafe name, too big, not part of the format) */
    class Pack(val name: String, val profile: String, val images: Map<String, ByteArray>, val skipped: Int)

    fun isZip(b: ByteArray) = b.size > 4 && b[0] == 'P'.code.toByte() && b[1] == 'K'.code.toByte() && b[2].toInt() == 3 && b[3].toInt() == 4

    fun write(out: OutputStream, name: String, profileJson: String, images: Map<String, ByteArray>) {
        ZipOutputStream(out).use { z ->
            fun put(n: String, b: ByteArray) { z.putNextEntry(ZipEntry(n)); z.write(b); z.closeEntry() }
            put(MANIFEST, JsonObject().apply { addProperty("format", FORMAT); addProperty("formatVersion", 1); addProperty("name", name); addProperty("images", images.size) }.toString().toByteArray())
            put(PROFILE, profileJson.toByteArray())
            images.forEach { (n, b) -> if (SAFE_NAME.matches(n)) put(IMAGES + n, b) }
        }
    }

    /** throws IllegalArgumentException with a reason the owner can read */
    fun read(bytes: ByteArray): Pack {
        var name = ""; var profile: String? = null; var skipped = 0; var total = 0L; var entries = 0
        val images = LinkedHashMap<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { z ->
            while (true) {
                val e = z.nextEntry ?: break
                if (e.isDirectory) continue
                require(++entries <= MAX_ENTRIES) { "ไฟล์ ZIP มีรายการมากเกินไป" }
                val en = e.name.replace('\\', '/') // Windows tools (PowerShell Compress-Archive) write backslashes
                val isProfile = en == PROFILE; val isManifest = en == MANIFEST
                val imageName = en.removePrefix(IMAGES).takeIf { en.startsWith(IMAGES) && SAFE_NAME.matches(it) }
                if (!isProfile && !isManifest && imageName == null) { skipped++; continue }
                val limit = if (isImage(imageName)) MAX_IMAGE else MAX_PROFILE
                val buf = ByteArrayOutputStream(); val chunk = ByteArray(16 * 1024); var size = 0; var tooBig = false
                while (true) {
                    val n = z.read(chunk); if (n < 0) break
                    size += n; total += n
                    require(total <= MAX_TOTAL) { "ไฟล์ ZIP ใหญ่เกินไป" }
                    if (size > limit) { tooBig = true; continue } // keep draining so the total cap still applies
                    buf.write(chunk, 0, n)
                }
                if (tooBig) { skipped++; continue }
                when {
                    isProfile -> profile = buf.toString(Charsets.UTF_8.name())
                    isManifest -> runCatching { name = JsonParser.parseString(buf.toString(Charsets.UTF_8.name())).asJsonObject.get("name").asString }
                    else -> images[imageName!!] = buf.toByteArray()
                }
            }
        }
        requireNotNull(profile) { "ZIP นี้ไม่ใช่โปรไฟล์ ALL (ไม่มี profile.json)" }
        val items = runCatching { JsonParser.parseString(profile).asJsonObject.getAsJsonArray("items") }.getOrNull()
        require(items != null) { "profile.json อ่านไม่ได้" }
        require(items.size() <= 5000) { "โปรไฟล์มีรายการมากเกินไป" }
        return Pack(name, profile, images, skipped)
    }

    private fun isImage(n: String?) = n != null
}
