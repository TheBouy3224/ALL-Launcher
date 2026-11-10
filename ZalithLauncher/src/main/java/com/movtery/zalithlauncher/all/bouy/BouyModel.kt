package com.movtery.zalithlauncher.all.bouy

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import com.movtery.layer_controller.event.ClickEvent
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_SCROLL_DOWN_SINGLE
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_SCROLL_UP_SINGLE
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_SWITCH_IME
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_SWITCH_MENU
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * ALL BOUY controls: one small layout (JSON) of sprite widgets, drawn and touched by [BouyControls], edited by [BouyStudio].
 * Input leaves through the game's existing [ClickEvent] path (GameViewModel.onKeyEvent -> lwjgl events), nothing else is replaced.
 */
enum class BouyKind { BUTTON, DPAD, JOYSTICK }

/** An event is stored as text: "K:GLFW_KEY_SPACE" (key / mouse button) or "L:<launcher event>" */
fun String.toClickEvent(): ClickEvent =
    if (startsWith("L:")) ClickEvent(ClickEvent.Type.LauncherEvent, substring(2)) else ClickEvent(ClickEvent.Type.Key, removePrefix("K:"))

data class BouyItem(
    val id: String,
    val kind: BouyKind,
    /** sprite path under assets/all_bouy, no extension. Joystick: "joystick/joystick_circle" (+_base/_thumb/_pressed) */
    val sprite: String,
    /** centre of the widget as a fraction of the screen (0..1) */
    val x: Float,
    val y: Float,
    /** widget width in dp; height follows the sprite's own aspect (never stretched) */
    val sizeDp: Float,
    val events: List<String> = emptyList(),
    /** tap = on, tap again = off (Sneak) instead of hold */
    val toggle: Boolean = false,
    /** only while the game owns the cursor (not in menus / inventory) */
    val gameOnly: Boolean = true,
    /** look (all optional, default = the supplied art untouched): colour of the button body (0 = original), body and icon opacity 0..1 */
    val tint: Int = 0,
    val bgAlpha: Float = 1f,
    val iconAlpha: Float = 1f,
    /** player picture (file name in [BouyStore.imagesDir], BUTTON only) replacing the sprite; [crop] fills the box, otherwise it is fitted */
    val image: String? = null,
    val crop: Boolean = false,
    /** picture size as a share of the button (0.2..1) and the sprite the button had before a picture turned it into a blank frame */
    val imageScale: Float = 0.6f,
    val origSprite: String? = null,
    /** a touch that STARTS on this button keeps turning the camera (the button stays pressed) */
    val dragCam: Boolean = false,
    /** accepts a finger dragged over from another button (that button is released, this one is pressed) */
    val slideIn: Boolean = false,
    /** D-Pad / Joystick piece pictures: part name (up, northwest, center, base, thumb...) -> file in [BouyStore.imagesDir] */
    val parts: Map<String, String> = emptyMap(),
    /** text drawn on the frame for keys that have no sprite (Print Screen, =, mouse buttons...) */
    val label: String? = null,
    /** the player's own name for the button (shown INSTEAD of the icon; the key it presses never changes); null = icon as usual */
    val caption: String? = null,
    val captionScale: Float = 1f,
    /** caption colour, 0 = the skin's ink colour */
    val captionColor: Int = 0,
    /** message button (null = ordinary button): one tap lists the saved messages, tapping one sends it. The list is edited / re-ordered only in the Studio.
     *  Empty = none saved yet (a tap presses [events] as usual) */
    val msgs: List<String>? = null,
    /** with a caption: true = the icon stays and the name is drawn under it; false = the name replaces the icon */
    val captionIcon: Boolean = false,
    /** BUTTON only: stretches the height against the width (1 = the sprite's own proportions); the owner can drag the right / bottom edge or type width and height */
    val heightScale: Float = 1f,
)

/** [skin]: which sprite set this profile draws with: "a" (light grey), "b" (dark glass); anything else loads as "a" */
data class BouyLayout(val items: List<BouyItem>, val version: Int = 1, val skin: String = "a")

/** The skin of the layout being drawn in THIS process (set whenever the active layout is loaded or changed in the Studio) */
object BouySkin {
    @Volatile var current: String = "a"
    val ALL = listOf("a" to "A เทาอ่อน", "b" to "B กระจกเข้ม")
    fun root(skin: String = current) = when (skin) { "a" -> "all_bouy_a"; "b" -> "all_bouy_b"; else -> "all_bouy_a" }
    fun next(skin: String = current) = ALL[(ALL.indexOfFirst { it.first == skin }.coerceAtLeast(0) + 1) % ALL.size].first
    fun name(skin: String = current) = ALL.firstOrNull { it.first == skin }?.second ?: ALL[0].second
}

/** The app's own font (Noto Sans Thai, SIL OFL; assets/fonts) for button captions and Studio text */
object BouyFont {
    @Volatile private var tf: android.graphics.Typeface? = null
    fun typeface(context: Context): android.graphics.Typeface = tf ?: runCatching {
        android.graphics.Typeface.createFromAsset(context.assets, "fonts/NotoSansThai.ttf")
    }.getOrDefault(android.graphics.Typeface.DEFAULT).also { tf = it }
    fun family(context: Context) = androidx.compose.ui.text.font.FontFamily(typeface(context))
}

fun newBouyId() = UUID.randomUUID().toString().replace("-", "").take(10)

private fun k(name: String) = "K:GLFW_KEY_$name"
private const val MOUSE_L = "K:GLFW_MOUSE_BUTTON_LEFT"
private const val MOUSE_R = "K:GLFW_MOUSE_BUTTON_RIGHT"
private const val MOUSE_M = "K:GLFW_MOUSE_BUTTON_MIDDLE"

/** What each touch sprite does when it is added from the palette. Keyboard sprites map by name (see [keyboardEvent]). */
val TOUCH_DEFAULTS: Map<String, Pair<List<String>, Float>> = mapOf(
    "touch_controls/control_up" to (listOf(k("W")) to 56f),
    "touch_controls/control_down" to (listOf(k("S")) to 56f),
    "touch_controls/control_left" to (listOf(k("A")) to 56f),
    "touch_controls/control_right" to (listOf(k("D")) to 56f),
    "touch_controls/control_jump" to (listOf(k("SPACE")) to 66f),
    "touch_controls/control_sneak" to (listOf(k("LEFT_SHIFT")) to 58f),
    "touch_controls/control_sprint" to (listOf(k("LEFT_CONTROL")) to 58f),
    "touch_controls/control_attack" to (listOf(MOUSE_L) to 72f),
    "touch_controls/control_use" to (listOf(MOUSE_R) to 66f),
    "touch_controls/control_pick_block" to (listOf(MOUSE_M) to 56f),
    "touch_controls/control_inventory" to (listOf(k("E")) to 52f),
    "touch_controls/control_chat" to (listOf(k("T")) to 48f),
    "touch_controls/control_pause" to (listOf(k("ESCAPE")) to 46f),
    "touch_controls/control_settings" to (listOf("L:$LAUNCHER_EVENT_SWITCH_MENU") to 46f),
    "touch_controls/control_eye" to (listOf(k("F5")) to 46f),
    "touch_controls/control_extra" to (listOf("L:$LAUNCHER_EVENT_SWITCH_IME") to 46f),
    "touch_controls/control_f1" to (listOf(k("F1")) to 46f),
    "touch_controls/control_f3" to (listOf(k("F3")) to 46f),
    "touch_controls/control_aim" to (listOf(k("F2")) to 46f),
    "touch_controls/control_blank_s" to (emptyList<String>() to 46f),
    "touch_controls/control_blank_m" to (emptyList<String>() to 52f),
    "touch_controls/control_blank_l" to (emptyList<String>() to 66f),
)

private val KEY_SPECIAL = mapOf(
    "esc" to "ESCAPE", "alt_left" to "LEFT_ALT", "alt_right" to "RIGHT_ALT", "apostrophe" to "APOSTROPHE",
    "arrow_down" to "DOWN", "arrow_left" to "LEFT", "arrow_right" to "RIGHT", "arrow_up" to "UP",
    "backslash" to "BACKSLASH", "backspace" to "BACKSPACE", "backtick" to "GRAVE_ACCENT", "caps_lock" to "CAPS_LOCK",
    "comma" to "COMMA", "ctrl_left" to "LEFT_CONTROL", "ctrl_right" to "RIGHT_CONTROL", "delete" to "DELETE",
    "end" to "END", "enter" to "ENTER", "home" to "HOME", "insert" to "INSERT", "left_bracket" to "LEFT_BRACKET",
    "menu" to "MENU", "meta_left" to "LEFT_SUPER", "minus" to "MINUS", "num_decimal" to "KP_DECIMAL",
    "num_divide" to "KP_DIVIDE", "num_enter" to "KP_ENTER", "num_lock" to "NUM_LOCK", "num_multiply" to "KP_MULTIPLY",
    "num_plus" to "KP_ADD", "num_subtract" to "KP_SUBTRACT", "page_down" to "PAGE_DOWN", "page_up" to "PAGE_UP",
    "period" to "PERIOD", "right_bracket" to "RIGHT_BRACKET", "semicolon" to "SEMICOLON", "shift_left" to "LEFT_SHIFT",
    "shift_right" to "RIGHT_SHIFT", "slash" to "SLASH", "space" to "SPACE", "tab" to "TAB",
)

/** "keyboard/key_digit_3" -> "K:GLFW_KEY_3"; null when the name is unknown */
fun keyboardEvent(sprite: String): String? {
    val n = sprite.substringAfterLast("/").removePrefix("key_")
    val glfw = KEY_SPECIAL[n] ?: when {
        n.startsWith("digit_") -> n.removePrefix("digit_")
        n.startsWith("num_") -> "KP_" + n.removePrefix("num_")
        n.matches(Regex("f\\d{1,2}")) -> n.uppercase()
        n.length == 1 -> n.uppercase()
        else -> return null
    }
    return k(glfw)
}

/** Palette: every sprite the Studio can add, in order (path without extension) */
object BouyCatalog {
    @Volatile private var cache: List<String>? = null
    fun all(context: Context): List<String> = cache ?: run {
        fun list(dir: String) = context.assets.list("all_bouy_a/$dir").orEmpty().filter { it.endsWith(".png") }
            .map { "$dir/${it.removeSuffix(".png")}" }.sorted()
        (list("touch_controls") + list("keyboard")).also { cache = it }
    }
    fun touch(context: Context) = all(context).filter { it.startsWith("touch_controls/") }
    fun keyboard(context: Context) = all(context).filter { it.startsWith("keyboard/") }
}

/** Sprite decoding: PNG alpha kept, no scaling at load; drawn nearest-neighbour by the renderer */
object BouySprites {
    private val cache = HashMap<String, ImageBitmap?>()
    @Synchronized
    fun get(context: Context, path: String): ImageBitmap? = cache.getOrPut(BouySkin.current + "|" + path) {
        runCatching { context.assets.open("${BouySkin.root()}/$path.png").use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull()
    }
}

fun newItemFor(context: Context, sprite: String, x: Float = 0.5f, y: Float = 0.5f): BouyItem {
    val touch = TOUCH_DEFAULTS[sprite]
    val events = touch?.first ?: keyboardEvent(sprite)?.let(::listOf) ?: emptyList()
    val isKey = sprite.startsWith("keyboard/")
    val size = touch?.second ?: if (sprite.endsWith("key_space")) 120f else if (isKey) 42f else 52f
    return BouyItem(newBouyId(), BouyKind.BUTTON, sprite, x, y, size, events, toggle = sprite.endsWith("sneak"), gameOnly = false.takeIf { isKey } ?: true)
}

fun newDpad(x: Float, y: Float) = BouyItem(newBouyId(), BouyKind.DPAD, "touch_controls/control_dpad_cross", x, y, 150f)
fun newJoystick(x: Float, y: Float, square: Boolean = false) =
    BouyItem(newBouyId(), BouyKind.JOYSTICK, if (square) "joystick/joystick_square" else "joystick/joystick_circle", x, y, 140f)

/** The layout a player gets first: D-Pad left, action buttons right, system buttons on top (hotbar stays the launcher's own) */
fun defaultBouyLayout(context: Context): BouyLayout {
    fun b(sprite: String, x: Float, y: Float, sizeDp: Float? = null, gameOnly: Boolean = true) =
        newItemFor(context, sprite, x, y).let { if (sizeDp != null) it.copy(sizeDp = sizeDp) else it }.copy(gameOnly = gameOnly)
    return BouyLayout(listOf(
        newDpad(0.14f, 0.72f),
        b("touch_controls/control_sprint", 0.30f, 0.50f),
        b("touch_controls/control_jump", 0.86f, 0.58f),
        b("touch_controls/control_attack", 0.90f, 0.80f),
        b("touch_controls/control_use", 0.76f, 0.56f),
        b("touch_controls/control_pick_block", 0.66f, 0.40f, 48f),
        b("touch_controls/control_inventory", 0.37f, 0.08f, gameOnly = false),
        b("touch_controls/control_chat", 0.31f, 0.08f, gameOnly = false),
        b("touch_controls/control_pause", 0.25f, 0.08f, gameOnly = false),
        b("touch_controls/control_eye", 0.43f, 0.08f),
        b("touch_controls/control_settings", 0.74f, 0.08f, gameOnly = false),
        b("touch_controls/control_extra", 0.80f, 0.08f, gameOnly = false),
    ))
}

object BouyStore {
    /** same external files dir the game and launcher processes both see (next to control_layouts) */
    private val root get() = PathManager.DIR_CONTROL_LAYOUTS.parentFile
    /** the owner's pre-profile layout: never written or deleted by the profile system (it is only COPIED into the first profile) */
    private fun legacyFile() = File(root, "all_bouy_layout.json")
    private fun profilesDir() = File(root, "all_bouy_profiles").also { it.mkdirs() }
    private fun indexFile() = File(profilesDir(), "index.json")
    private fun file(id: String) = File(profilesDir(), "$id.json")

    /** [template] = what Reset restores: dpad | joy_round | joy_square | legacy (the owner's untouched all_bouy_layout.json, copied again) */
    data class Profile(val id: String, val name: String, val template: String = "dpad")
    private class Index(var active: String, val list: MutableList<Profile>)

    private var cache: Index? = null
    /** the file process may write from the Studio while the game reads: always re-read the small index, never trust a stale copy */
    @Synchronized private fun index(context: Context): Index {
        runCatching {
            val o = JSONObject(indexFile().readText()); val a = o.getJSONArray("profiles")
            val l = (0 until a.length()).map { a.getJSONObject(it).let { j -> Profile(j.getString("id"), j.getString("name"), j.optString("template", "")) } }.toMutableList()
            var fixed = false
            l.forEachIndexed { n, p -> if (p.template.isEmpty()) { l[n] = p.copy(template = guessTemplate(n == 0, runCatching { loadFile(file(p.id), context) }.getOrNull())); fixed = true } }
            if (l.isNotEmpty() && l.any { it.id == o.getString("active") } && l.all { file(it.id).exists() }) return Index(o.getString("active"), l).also { cache = it; if (fixed) writeIndex(it) }
        }
        // first run (or a broken index): profile 1 = the owner's existing layout copied (pictures too), else the D-Pad default
        val id = newBouyId()
        val legacy = runCatching { loadFile(legacyFile(), context).takeIf { legacyFile().exists() } }.getOrNull()
        val first = if (legacy != null) copyLayout(legacy) else defaultBouyLayout(context)
        writeLayout(file(id), first)
        return Index(id, mutableListOf(Profile(id, if (legacy != null) "เลย์เอาต์เดิม" else "D-Pad", if (legacy != null) "legacy" else "dpad"))).also { writeIndex(it); cache = it }
    }

    private fun writeIndex(ix: Index) {
        val a = JSONArray(); ix.list.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("template", it.template)) }
        val f = indexFile(); val tmp = File(f.parentFile, "index.json.tmp")
        tmp.writeText(JSONObject().put("active", ix.active).put("profiles", a).toString())
        tmp.renameTo(f) || (f.delete() && tmp.renameTo(f))
    }

    fun profiles(context: Context): List<Profile> = index(context).list.toList()
    fun activeId(context: Context): String = index(context).active
    fun activeName(context: Context): String = index(context).let { ix -> ix.list.first { it.id == ix.active }.name }

    fun setActive(context: Context, id: String): Boolean = index(context).let { ix ->
        if (ix.list.none { it.id == id }) false else { ix.active = id; writeIndex(ix); true }
    }

    private fun guessTemplate(first: Boolean, l: BouyLayout?): String =
        if (first && legacyFile().exists()) "legacy" else when (l?.items?.firstOrNull { it.kind == BouyKind.JOYSTICK }?.sprite) { null -> "dpad"; "joystick/joystick_square" -> "joy_square"; else -> "joy_round" }

    private fun templateLayout(context: Context, tpl: String): BouyLayout = defaultBouyLayout(context).let { d ->
        if (tpl == "dpad") d else d.copy(items = d.items.map { if (it.kind == BouyKind.DPAD) newJoystick(it.x, it.y, tpl == "joy_square").copy(sizeDp = it.sizeDp) else it })
    }

    /** [from] = profile to copy (pictures duplicated; it keeps the source template), else a new profile of [template] (dpad | joy_round | joy_square) */
    fun create(context: Context, name: String, from: String?, template: String = "dpad"): String {
        val ix = index(context); val id = newBouyId()
        val src = from?.let { f -> ix.list.first { it.id == f } }
        writeLayout(file(id), if (src != null) copyLayout(loadFile(file(src.id), context)) else templateLayout(context, template))
        ix.list.add(Profile(id, name.trim().ifEmpty { "โปรไฟล์ ${ix.list.size + 1}" }.take(30), src?.template ?: template)); writeIndex(ix)
        return id
    }

    fun rename(context: Context, id: String, name: String) {
        val ix = index(context); val i = ix.list.indexOfFirst { it.id == id }
        if (i >= 0 && name.isNotBlank()) { ix.list[i] = ix.list[i].copy(name = name.trim().take(30)); writeIndex(ix) }
    }

    /** the last profile cannot be deleted; deleting the active one activates the first remaining */
    fun delete(context: Context, id: String): Boolean {
        val ix = index(context)
        if (ix.list.size <= 1 || ix.list.none { it.id == id }) return false
        runCatching { imagesOf(loadFile(file(id), context)).forEach { BouyStyle.deletePicture(it) } }
        file(id).delete(); ix.list.removeAll { it.id == id }
        if (ix.active == id) ix.active = ix.list.first().id
        writeIndex(ix); return true
    }

    private fun imagesOf(l: BouyLayout) = l.items.flatMap { listOfNotNull(it.image) + it.parts.values }

    /** duplicate every picture the layout uses under a new name, so deleting / re-skinning in one profile cannot touch another */
    private fun copyLayout(l: BouyLayout): BouyLayout {
        val tag = "cp${System.currentTimeMillis()}"; var n = 0
        fun cp(name: String): String = runCatching {
            val new = "${tag}_${n++}.png"; File(imagesDir(), name).copyTo(File(imagesDir(), new)); new
        }.getOrDefault(name)
        return l.copy(items = l.items.map { it.copy(image = it.image?.let(::cp), parts = it.parts.mapValues { (_, v) -> cp(v) }) })
    }

    fun load(context: Context): BouyLayout = loadFile(file(index(context).active), context).also { BouySkin.current = it.skin }

    private fun loadFile(f: File, context: Context): BouyLayout = runCatching {
        val o = JSONObject(f.readText())
        val a = o.getJSONArray("items")
        BouyLayout((0 until a.length()).map { i ->
            val j = a.getJSONObject(i)
            BouyItem(
                j.getString("id"), BouyKind.valueOf(j.getString("kind")), j.getString("sprite"),
                j.getDouble("x").toFloat(), j.getDouble("y").toFloat(), j.getDouble("size").toFloat(),
                j.getJSONArray("events").let { e -> (0 until e.length()).map { e.getString(it) } },
                j.optBoolean("toggle"), j.optBoolean("gameOnly", true),
                j.optInt("tint", 0), j.optDouble("bgAlpha", 1.0).toFloat(), j.optDouble("iconAlpha", 1.0).toFloat(),
                j.optString("image", "").ifEmpty { null }, j.optBoolean("crop", false),
                j.optDouble("imageScale", 0.6).toFloat(), j.optString("origSprite", "").ifEmpty { null },
                j.optBoolean("dragCam", false), j.optBoolean("slideIn", false),
                j.optJSONObject("parts")?.let { o -> o.keys().asSequence().associateWith { o.getString(it) } }.orEmpty(),
                j.optString("label", "").ifEmpty { null },
                j.optString("caption", "").ifEmpty { null }, j.optDouble("captionScale", 1.0).toFloat(), j.optInt("captionColor", 0),
                msgs = j.optJSONArray("msgs")?.let { m -> (0 until m.length()).map { m.getString(it) } }, captionIcon = j.optBoolean("captionIcon", false), heightScale = j.optDouble("hs", 1.0).toFloat().coerceIn(0.2f, 4f),
            )
        }, skin = o.optString("skin", "a").takeIf { s -> BouySkin.ALL.any { it.first == s } } ?: "a")
    }.getOrNull() ?: defaultBouyLayout(context)

    /** saves into the ACTIVE profile (read from the index, so the Studio and the game can never disagree about which file is current) */
    fun save(layout: BouyLayout): Boolean = runCatching { writeLayout(file(JSONObject(indexFile().readText()).getString("active")), layout) }.getOrDefault(false)

    private fun writeLayout(f: File, layout: BouyLayout): Boolean = runCatching {
        if (com.movtery.zalithlauncher.BuildConfig.DEBUG) android.util.Log.i("ALLBOUY", "store: save items=${layout.items.size} from ${Throwable().stackTrace.getOrNull(2)?.let { it.className.substringAfterLast('.') + "." + it.methodName }}")
        val a = JSONArray()
        layout.items.forEach {
            a.put(JSONObject().put("id", it.id).put("kind", it.kind.name).put("sprite", it.sprite)
                .put("x", it.x.toDouble()).put("y", it.y.toDouble()).put("size", it.sizeDp.toDouble())
                .put("events", JSONArray(it.events)).put("toggle", it.toggle).put("gameOnly", it.gameOnly)
                .put("tint", it.tint).put("bgAlpha", it.bgAlpha.toDouble()).put("iconAlpha", it.iconAlpha.toDouble())
                .put("image", it.image ?: "").put("crop", it.crop)
                .put("imageScale", it.imageScale.toDouble()).put("origSprite", it.origSprite ?: "")
                .put("dragCam", it.dragCam).put("slideIn", it.slideIn).put("parts", JSONObject(it.parts)).put("label", it.label ?: "")
                .put("caption", it.caption ?: "").put("captionScale", it.captionScale.toDouble()).put("captionColor", it.captionColor)
                .apply { it.msgs?.let { m -> put("msgs", JSONArray(m)) }; if (it.captionIcon) put("captionIcon", true); if (it.heightScale != 1f) put("hs", it.heightScale.toDouble()) })
        }
        val tmp = File(f.parentFile, f.name + ".tmp")
        tmp.writeText(JSONObject().put("version", layout.version).put("skin", layout.skin).put("items", a).toString())
        tmp.renameTo(f) || (f.delete() && tmp.renameTo(f))
    }.getOrDefault(false)

    /** player pictures, app-private (same external files dir as the layout; no storage permission involved) */
    fun imagesDir() = File(PathManager.DIR_CONTROL_LAYOUTS.parentFile, "all_bouy_images").also { it.mkdirs() }

    /** Reset = ONLY the active profile goes back to the D-Pad default; other profiles and their pictures are untouched */
    fun resetActive(context: Context) {
        val id = activeId(context)
        runCatching { imagesOf(loadFile(file(id), context)).forEach { BouyStyle.deletePicture(it) } }
        val tpl = index(context).list.first { it.id == id }.template
        val fresh = if (tpl == "legacy" && legacyFile().exists()) copyLayout(loadFile(legacyFile(), context)) else templateLayout(context, tpl.takeIf { it != "legacy" } ?: "dpad")
        writeLayout(file(id), fresh)
    }

    @Suppress("unused") // scroll events are offered by the Studio's action picker
    val SPECIAL_ACTIONS: List<Pair<String, String>> = listOf(
        "คลิกซ้าย (โจมตี)" to MOUSE_L, "คลิกขวา (ใช้ของ)" to MOUSE_R, "คลิกกลาง (เลือกบล็อก)" to MOUSE_M,
        "เปิดคีย์บอร์ดมือถือ" to "L:$LAUNCHER_EVENT_SWITCH_IME", "เมนูเกม ALL" to "L:$LAUNCHER_EVENT_SWITCH_MENU",
        "เลื่อนช่องก่อนหน้า" to "L:$LAUNCHER_EVENT_SCROLL_UP_SINGLE", "เลื่อนช่องถัดไป" to "L:$LAUNCHER_EVENT_SCROLL_DOWN_SINGLE",
    )
}
