package com.movtery.zalithlauncher.all.launchersettings

import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Cursor packs (pure logic, unit tested): reading a ZIP or several picked files into a small library folder, guessing which file
 * plays which cursor role, and the pack manifest. Nothing here touches the live cursors; installing a pack is done by the Cursor tab
 * through [CursorStore] (staged, with backups).
 *
 * Safety: entry paths are never used as paths - only the file name is kept, and names with ".." or odd characters are skipped, so
 * a ZIP cannot write outside the pack folder. Every entry and the whole archive are read through hard byte limits (zip bomb), and
 * the number of entries is capped. A .inf file is only read as text to find role names; it is never executed or applied.
 */
object CursorPackLimits {
    const val MAX_ENTRIES = 200
    const val MAX_FILES = 40
    const val MAX_FILE_BYTES = CURSOR_MAX_FILE_BYTES
    const val MAX_INF_BYTES = 64 * 1024
    const val MAX_TOTAL_BYTES = 24L * 1024 * 1024
}

/** The roles ALL's virtual pointer can show (CursorShape names) */
val CURSOR_ROLES = listOf("Arrow", "IBeam", "Hand", "CrossHair", "ResizeNS", "ResizeEW", "ResizeAll", "NotAllowed")

private val CURSOR_EXT = setOf("ani", "cur", "png", "gif", "webp", "jpg", "jpeg")

enum class PackSkip { NOT_A_CURSOR, UNSAFE_NAME, DUPLICATE, TOO_LARGE, TOO_MANY, INVALID }

class PackImport(
    val name: String,
    /** accepted cursor files, file name -> bytes (not yet image-checked; the caller decodes them) */
    val files: LinkedHashMap<String, ByteArray>,
    val infText: String?,
    val skipped: MutableList<Pair<String, PackSkip>>
)

class PackTooLargeException : RuntimeException("The archive unpacks to more than the allowed size")

/** Android 14+ refuses to even list a ZIP entry like "../x" (platform zip path check); the whole archive is then refused */
class PackUnsafeException : RuntimeException("The archive contains an unsafe path")

/** File name only; null when the entry must not be used (directory, "..", hidden, control characters, too long) */
fun safeEntryName(path: String): String? {
    val p = path.replace('\\', '/')
    if (p.endsWith("/")) return null
    if (p.split('/').any { it == ".." }) return null
    val name = p.substringAfterLast('/')
    if (name.isEmpty() || name.startsWith(".") || name.length > 100) return null
    if (name.any { it < ' ' || it in "<>:\"|?*" }) return null
    return name
}

private fun ext(name: String) = name.substringAfterLast('.', "").lowercase()

/** Reads at most [limit] bytes; null when the stream is longer (the rest is not read) */
private fun readCapped(input: InputStream, limit: Int, total: LongArray, totalLimit: Long): ByteArray? {
    val out = java.io.ByteArrayOutputStream()
    val buf = ByteArray(16 * 1024)
    while (true) {
        val n = input.read(buf)
        if (n < 0) break
        total[0] += n
        if (total[0] > totalLimit) throw PackTooLargeException()
        if (out.size() + n > limit) return null
        out.write(buf, 0, n)
    }
    return out.toByteArray()
}

private class Collector(val fallbackName: String) {
    val files = LinkedHashMap<String, ByteArray>()
    var inf: String? = null
    val skipped = mutableListOf<Pair<String, PackSkip>>()
    val total = LongArray(1)

    fun offer(path: String, read: (limit: Int) -> ByteArray?) {
        val name = safeEntryName(path)
        if (name == null) {
            if (!path.endsWith("/")) skipped += path to PackSkip.UNSAFE_NAME
            return
        }
        val e = ext(name)
        if (e == "inf") {
            if (inf == null) read(CursorPackLimits.MAX_INF_BYTES)?.let { inf = String(it, Charsets.UTF_8) } ?: run { skipped += name to PackSkip.TOO_LARGE }
            return
        }
        if (e !in CURSOR_EXT) { skipped += name to PackSkip.NOT_A_CURSOR; return }
        if (files.keys.any { it.equals(name, ignoreCase = true) }) { skipped += name to PackSkip.DUPLICATE; return }
        if (files.size >= CursorPackLimits.MAX_FILES) { skipped += name to PackSkip.TOO_MANY; return }
        val bytes = read(CursorPackLimits.MAX_FILE_BYTES)
        if (bytes == null) skipped += name to PackSkip.TOO_LARGE else files[name] = bytes
    }

    fun result() = PackImport(packNameFromInf(inf) ?: fallbackName, files, inf, skipped)
}

/** Reads a cursor ZIP (any folder depth, e.g. WindowsMita/Mita/Normal.ani). Throws [PackTooLargeException] for a zip bomb. */
fun readCursorZip(input: InputStream, fallbackName: String): PackImport {
    val c = Collector(fallbackName)
    var entries = 0
    ZipInputStream(input).use { zip ->
        while (true) {
            val entry = try {
                zip.nextEntry
            } catch (e: java.util.zip.ZipException) {
                if (e.message?.contains("path", ignoreCase = true) == true) throw PackUnsafeException() else throw e
            } ?: break
            if (++entries > CursorPackLimits.MAX_ENTRIES) { c.skipped += entry.name to PackSkip.TOO_MANY; break }
            if (entry.isDirectory) continue
            c.offer(entry.name) { limit -> readCapped(zip, limit, c.total, CursorPackLimits.MAX_TOTAL_BYTES) }
        }
    }
    return c.result()
}

/** Several picked files; each one is a (display name, stream opener) */
fun readCursorFiles(files: List<Pair<String, () -> InputStream?>>, fallbackName: String): PackImport {
    val c = Collector(fallbackName)
    files.forEach { (name, open) ->
        c.offer(name) { limit -> open()?.use { readCapped(it, limit, c.total, CursorPackLimits.MAX_TOTAL_BYTES) } }
    }
    return c.result()
}

/** `key = "value"` pairs of the [Strings] section of an .inf, keys lower-cased */
fun infStrings(inf: String): Map<String, String> {
    val out = mutableMapOf<String, String>()
    var inStrings = false
    inf.lineSequence().forEach { raw ->
        val line = raw.substringBefore(';').trim()
        if (line.startsWith("[")) { inStrings = line.equals("[Strings]", ignoreCase = true); return@forEach }
        if (!inStrings || '=' !in line) return@forEach
        val key = line.substringBefore('=').trim().lowercase()
        val value = line.substringAfter('=').trim().trim('"')
        if (key.isNotEmpty()) out[key] = value
    }
    return out
}

fun packNameFromInf(inf: String?): String? = inf?.let { infStrings(it)["scheme_name"]?.takeIf { n -> n.isNotBlank() }?.take(40) }

/** Windows registry cursor names -> ALL roles */
private val WINDOWS_ROLE = mapOf(
    "arrow" to "Arrow", "ibeam" to "IBeam", "hand" to "Hand", "crosshair" to "CrossHair",
    "sizens" to "ResizeNS", "sizewe" to "ResizeEW", "sizeall" to "ResizeAll", "no" to "NotAllowed"
)

/** Common [Strings] variable names -> ALL roles (packs without registry lines) */
private val INF_VAR_ROLE = mapOf(
    "pointer" to "Arrow", "arrow" to "Arrow", "normal" to "Arrow", "text" to "IBeam", "ibeam" to "IBeam", "link" to "Hand",
    "precision" to "CrossHair", "cross" to "CrossHair", "vert" to "ResizeNS", "horz" to "ResizeEW", "move" to "ResizeAll",
    "unavailable" to "NotAllowed", "unavailiable" to "NotAllowed", "no" to "NotAllowed"
)

/** File name words -> ALL roles (last resort). "hand" alone is not used: Windows packs call the pen cursor Handwriting. */
private val NAME_ROLE = listOf(
    "Arrow" to listOf("normal", "arrow", "pointer", "default"),
    "IBeam" to listOf("text", "ibeam", "beam"),
    "Hand" to listOf("link"),
    "CrossHair" to listOf("precision", "cross"),
    "ResizeNS" to listOf("vertical", "sizens", "ns_resize"),
    "ResizeEW" to listOf("horizontal", "sizewe", "ew_resize"),
    "ResizeAll" to listOf("move", "sizeall"),
    "NotAllowed" to listOf("unavailable", "notallowed", "not_allowed", "forbidden")
)

/**
 * Role -> file name. Order: the .inf registry lines (HKCU "Control Panel\Cursors", Arrow, ... "%pointer%"), then its [Strings]
 * variable names, then words in the file names. Only files that exist in [files] are used.
 */
fun guessCursorRoles(files: Collection<String>, inf: String?): Map<String, String> {
    val byLower = files.associateBy { it.lowercase() }
    fun file(n: String?) = n?.substringAfterLast('\\')?.substringAfterLast('/')?.let { byLower[it.lowercase()] }
    val roles = linkedMapOf<String, String>()
    if (inf != null) {
        val strings = infStrings(inf)
        Regex("""(?i)"Control Panel\\Cursors"\s*,\s*([A-Za-z]+)\s*,[^"]*"([^"]*)"""").findAll(inf).forEach { m ->
            val role = WINDOWS_ROLE[m.groupValues[1].lowercase()] ?: return@forEach
            val variable = Regex("%([^%]+)%").findAll(m.groupValues[2]).map { it.groupValues[1].lowercase() }.lastOrNull { it != "10" && it != "cur_dir" }
            file(strings[variable])?.let { roles.putIfAbsent(role, it) }
        }
        strings.forEach { (k, v) -> INF_VAR_ROLE[k]?.let { role -> file(v)?.let { roles.putIfAbsent(role, it) } } }
    }
    NAME_ROLE.forEach { (role, words) ->
        if (role in roles) return@forEach
        files.firstOrNull { f -> val base = f.substringBeforeLast('.').lowercase(); words.any { base == it || base.startsWith(it) } }
            ?.let { roles[role] = it }
    }
    return CURSOR_ROLES.mapNotNull { r -> roles[r]?.let { r to it } }.toMap()
}

/** A pack in the library: DIR_MOUSE_POINTER/packs/<id>/ with its files and pack.txt (name + role lines) */
class CursorPack(val dir: File, val name: String, val roles: Map<String, String>) {
    val files: List<String> get() = dir.listFiles()?.filter { it.isFile && it.name != MANIFEST }?.map { it.name }?.sorted() ?: emptyList()
    fun file(name: String) = File(dir, name)

    fun withRoles(newRoles: Map<String, String>) = CursorPack(dir, name, newRoles).also { it.save() }

    fun save() {
        val text = buildString {
            append("name=").append(name.replace('\n', ' ')).append('\n')
            roles.forEach { (r, f) -> append("role.").append(r).append('=').append(f).append('\n') }
        }
        File(dir, MANIFEST).writeText(text)
    }

    companion object {
        const val MANIFEST = "pack.txt"

        fun load(dir: File): CursorPack? {
            val m = File(dir, MANIFEST).takeIf { it.isFile } ?: return null
            var name = dir.name
            val roles = linkedMapOf<String, String>()
            m.readLines().forEach { l ->
                when {
                    l.startsWith("name=") -> name = l.removePrefix("name=")
                    l.startsWith("role.") && '=' in l -> {
                        val r = l.removePrefix("role.").substringBefore('=')
                        val f = l.substringAfter('=')
                        if (r in CURSOR_ROLES && safeEntryName(f) == f && File(dir, f).isFile) roles[r] = f
                    }
                }
            }
            return CursorPack(dir, name, roles)
        }
    }
}

class CursorPackLibrary(private val root: File) {
    fun list(): List<CursorPack> = root.listFiles()?.filter { it.isDirectory }?.mapNotNull { CursorPack.load(it) }?.sortedBy { it.name.lowercase() } ?: emptyList()

    /** Writes an already-checked import as a new pack folder (never overwrites another pack) */
    fun add(name: String, files: Map<String, ByteArray>, roles: Map<String, String>): CursorPack {
        root.mkdirs()
        val base = name.filter { it.isLetterOrDigit() || it == '_' || it == '-' }.take(30).ifEmpty { "pack" }
        var dir = File(root, base)
        var i = 2
        while (dir.exists()) dir = File(root, "${base}_${i++}")
        val tmp = File(root, ".importing_" + dir.name)
        tmp.deleteRecursively()
        tmp.mkdirs()
        files.forEach { (n, b) -> File(tmp, n).writeBytes(b) }
        if (!tmp.renameTo(dir)) { tmp.deleteRecursively(); error("Could not save the cursor pack") }
        return CursorPack(dir, name, roles.filterValues { it in files }).also { it.save() }
    }

    fun delete(pack: CursorPack) { pack.dir.deleteRecursively() }
}
