package com.movtery.zalithlauncher.all.touch

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.movtery.layer_controller.data.ButtonPosition
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_SCROLL_DOWN_SINGLE
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_SCROLL_UP_SINGLE
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_SWITCH_IME
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_SWITCH_MENU
import java.util.UUID

/**
 * FoldCraftLauncher controller file <-> ALL ([TouchLayout]), written for ALL directly (no Zalith 2 step, no third-party converter).
 *
 * Sources of every fact used here (all read from FCL's own GPL-3.0 repository, https://github.com/FCL-Team/FoldCraftLauncher):
 *  - file layout: the Java data classes in `control/data` (ControlViewGroup, ControlButtonData, ControlDirectionData, BaseInfoData, ButtonEventData, DirectionEventData) and real controller files;
 *  - position = (screen - size) * p / 1000 and size = per-mille of the screen width / height or dp: `control/view/ControlButton.java`, `ControlDirection.java`;
 *  - the four triggers: `ControlButton.java` onTouch (press = finger down..up, click = a quick tap on lift, long press = held 400 ms, double click = two clicks within 400 ms);
 *  - key codes are Linux input-event codes (`fclauncher/keycodes/FCLKeycodes.java`), mouse / scroll codes 1000..1004 (`control/FCLInput.java`).
 * Everything FCL has and ALL has not is listed in the warnings of the result, never dropped silently.
 */
object FclFormat {
    private const val CONTROLLER_VERSION = 21
    private const val BLANK = "touch_controls/control_blank_m"
    private const val DPAD = "touch_controls/control_dpad_cross"
    private const val JOY = "joystick/joystick_circle"

    private const val MOUSE_LEFT = 1000; private const val MOUSE_MIDDLE = 1001; private const val MOUSE_RIGHT = 1002
    private const val SCROLL_UP = 1003; private const val SCROLL_DOWN = 1004
    private const val SHIFT_CODE = 42

    /** Linux input-event code -> GLFW key name (ALL's own `K:` names) */
    private val KEYS: Map<Int, String> = buildMap {
        put(1, "ESCAPE"); "1234567890".forEachIndexed { i, c -> put(2 + i, c.toString()) }
        put(12, "MINUS"); put(13, "EQUAL"); put(14, "BACKSPACE"); put(15, "TAB")
        "QWERTYUIOP".forEachIndexed { i, c -> put(16 + i, c.toString()) }
        put(26, "LEFT_BRACKET"); put(27, "RIGHT_BRACKET"); put(28, "ENTER"); put(29, "LEFT_CONTROL")
        "ASDFGHJKL".forEachIndexed { i, c -> put(30 + i, c.toString()) }
        put(39, "SEMICOLON"); put(40, "APOSTROPHE"); put(41, "GRAVE_ACCENT"); put(42, "LEFT_SHIFT"); put(43, "BACKSLASH")
        "ZXCVBNM".forEachIndexed { i, c -> put(44 + i, c.toString()) }
        put(51, "COMMA"); put(52, "PERIOD"); put(53, "SLASH"); put(54, "RIGHT_SHIFT"); put(55, "KP_MULTIPLY"); put(56, "LEFT_ALT")
        put(57, "SPACE"); put(58, "CAPS_LOCK"); for (i in 1..10) put(58 + i, "F$i")
        put(69, "NUM_LOCK"); put(70, "SCROLL_LOCK")
        put(71, "KP_7"); put(72, "KP_8"); put(73, "KP_9"); put(74, "KP_SUBTRACT"); put(75, "KP_4"); put(76, "KP_5"); put(77, "KP_6"); put(78, "KP_ADD")
        put(79, "KP_1"); put(80, "KP_2"); put(81, "KP_3"); put(82, "KP_0"); put(83, "KP_DECIMAL"); put(87, "F11"); put(88, "F12")
        put(96, "KP_ENTER"); put(97, "RIGHT_CONTROL"); put(98, "KP_DIVIDE"); put(99, "PRINT_SCREEN"); put(100, "RIGHT_ALT")
        put(102, "HOME"); put(103, "UP"); put(104, "PAGE_UP"); put(105, "LEFT"); put(106, "RIGHT"); put(107, "END"); put(108, "DOWN"); put(109, "PAGE_DOWN")
        put(110, "INSERT"); put(111, "DELETE"); put(117, "KP_EQUAL"); put(119, "PAUSE"); put(125, "LEFT_SUPER"); put(126, "RIGHT_SUPER")
        for (i in 13..24) put(170 + i, "F$i")
    }
    private val KEY_CODES: Map<String, Int> by lazy { KEYS.entries.associate { "GLFW_KEY_" + it.value to it.key } }

    private fun parse(text: String): JsonObject = JsonParser.parseString(text.removePrefix("﻿")).asJsonObject
    fun isFcl(text: String) = runCatching { parse(text).let { it.has("viewGroups") && it.has("controllerVersion") } }.getOrDefault(false)

    // small null-safe readers: FCL leaves defaults out of its files
    private fun JsonObject?.obj(k: String): JsonObject? = this?.get(k)?.takeIf { it.isJsonObject }?.asJsonObject
    private fun JsonObject?.arr(k: String): JsonArray = this?.get(k)?.takeIf { it.isJsonArray }?.asJsonArray ?: JsonArray()
    private fun JsonObject?.str(k: String, d: String = ""): String = this?.get(k)?.takeIf { it.isJsonPrimitive }?.asString ?: d
    private fun JsonObject?.int(k: String, d: Int = 0): Int = this?.get(k)?.takeIf { it.isJsonPrimitive }?.runCatching { asInt }?.getOrNull() ?: d
    private fun JsonObject?.bool(k: String): Boolean = this?.get(k)?.takeIf { it.isJsonPrimitive }?.runCatching { asBoolean }?.getOrNull() ?: false
    private fun JsonArray.objs(): List<JsonObject> = mapNotNull { it.takeIf(JsonElement::isJsonObject)?.asJsonObject }

    private class Notes { val m = LinkedHashMap<String, Int>(); fun add(s: String) { m[s] = (m[s] ?: 0) + 1 }; fun list() = m.map { (k, v) -> if (v > 1) "$k (x$v)" else k } }

    // ---- FCL -> ALL ----

    fun fromFcl(text: String, screen: LayoutMapper.Screen, aspectOf: (String) -> Float = { 1f }): LayoutMapper.Mapped<Pair<String, TouchLayout>> {
        val root = parse(text); val notes = Notes()
        val groups = root.arr("viewGroups").objs()
        val grouped = groups.size > 1
        val names = HashMap<String, String>() // FCL group id -> unique ALL group name
        groups.forEachIndexed { i, g ->
            val base = g.str("name").replace(":", "").trim().ifEmpty { "group ${i + 1}" }.take(16)
            var n = base; var k = 2
            while (n in names.values) n = "$base ${k++}"
            names[g.str("id")] = n
        }
        val dirKind = root.arr("directionStyles").objs().associate { it.str("name") to it.str("styleType") }
        val items = mutableListOf<TouchItem>()
        var menuOnly = 0; var styled = 0; var wrap = 0

        fun keysOf(a: JsonArray): List<String> = a.mapNotNull { e ->
            val code = runCatching { e.asInt }.getOrNull() ?: return@mapNotNull null
            when (code) {
                MOUSE_LEFT -> "K:GLFW_MOUSE_BUTTON_LEFT"; MOUSE_MIDDLE -> "K:GLFW_MOUSE_BUTTON_MIDDLE"; MOUSE_RIGHT -> "K:GLFW_MOUSE_BUTTON_RIGHT"
                SCROLL_UP -> "L:$LAUNCHER_EVENT_SCROLL_UP_SINGLE"; SCROLL_DOWN -> "L:$LAUNCHER_EVENT_SCROLL_DOWN_SINGLE"
                else -> KEYS[code]?.let { "K:GLFW_KEY_$it" } ?: run { notes.add("keycode FCL $code ไม่มีคีย์เทียบเท่า: ข้าม"); null }
            }
        }
        fun event(e: JsonObject?): List<String> {
            if (e == null) return emptyList()
            val out = keysOf(e.arr("outputKeycodes")).toMutableList()
            if (e.bool("input")) out += "L:$LAUNCHER_EVENT_SWITCH_IME"
            if (e.bool("openMenu")) out += "L:$LAUNCHER_EVENT_SWITCH_MENU"
            e.str("outputText").takeIf { it.isNotEmpty() }?.let { out += "T:$it" }
            e.arr("bindViewGroup").forEach { id -> names[id.asString]?.takeIf { grouped }?.let { out += groupEvent("toggle", it) } ?: notes.add("Action สลับกลุ่มที่ไม่มีอยู่: ข้าม") }
            if (e.bool("quickInput")) notes.add("quickInput (ข้อความด่วน) ไม่มีใน ALL: ข้าม")
            if (e.bool("switchTouchMode")) notes.add("switchTouchMode ไม่มีใน ALL: ข้าม")
            if (e.bool("autoClick")) notes.add("autoClick (กดย้ำ) ไม่มีใน ALL: ทำงานเป็นกดปกติ")
            return out
        }
        fun sizeDp(base: JsonObject?, axis: String, abs: String): Float {
            if (base.str("sizeType") == "ABSOLUTE") return base.int(abs, 50).toFloat()
            val p = base.obj(axis)
            val per = p.int("size", 140) / 1000f
            return per * (if (p.str("reference") == "SCREEN_WIDTH") screen.width else screen.height) / screen.density
        }
        fun centreOf(base: JsonObject?, w: Float, h: Float) =
            LayoutMapper.centre(ButtonPosition(base.int("xPosition").coerceIn(0, 1000) * 10, base.int("yPosition").coerceIn(0, 1000) * 10), w * screen.density, h * screen.density, screen)

        for (g in groups) {
            val groupName = if (grouped) names[g.str("id")] else null
            val view = g.obj("viewData")
            for (b in view.arr("buttonList").objs()) {
                val base = b.obj("baseInfo")
                if (base.str("visibilityType") == "MENU") { menuOnly++; continue }
                if (base.str("sizeType") == "WRAP_CONTENT") wrap++
                val w = sizeDp(base, "percentageWidth", "absoluteWidth"); val h = sizeDp(base, "percentageHeight", "absoluteHeight")
                val (cx, cy) = centreOf(base, w, h)
                val ev = b.obj("event")
                val press = ev.obj("pressEvent"); val click = ev.obj("clickEvent"); val long = ev.obj("longPressEvent"); val dbl = ev.obj("doubleClickEvent")
                var plain = event(press); var clickEv = event(click); var toggle = press.bool("autoKeep") && plain.isNotEmpty()
                if (click.bool("autoKeep") && clickEv.isNotEmpty()) { plain = (plain + clickEv).distinct(); clickEv = emptyList(); toggle = true } // a click that latches = ALL's tap-on / tap-off
                if (long.bool("autoKeep") || dbl.bool("autoKeep")) notes.add("autoKeep ของ กดค้าง/ดับเบิลคลิก ไม่มีใน ALL: ทำงานเป็นกดปกติ")
                if (b.str("style").isNotEmpty() && b.str("style") != "Default") styled++
                val scale = (h / (w * aspectOf(BLANK))).coerceIn(0.2f, 4f)
                items += TouchItem(
                    newTouchId(), TouchKind.BUTTON, BLANK, cx, cy, w.coerceAtLeast(10f),
                    events = plain, toggle = toggle, gameOnly = base.str("visibilityType") == "IN_GAME",
                    dragCam = ev.bool("pointerFollow"), slideIn = ev.bool("swipable"),
                    caption = b.str("text").trim().ifEmpty { null }, heightScale = scale, group = groupName,
                    longEvents = event(long).distinct(), doubleEvents = event(dbl).distinct(), clickEvents = clickEv.distinct(),
                )
                if (ev.bool("Movable")) notes.add("Movable (ปุ่มลากย้ายได้ในเกม) ไม่มีใน ALL: ข้าม")
            }
            for (d in view.arr("directionList").objs()) {
                val base = d.obj("baseInfo")
                if (base.str("visibilityType") == "MENU") { menuOnly++; continue }
                val w = sizeDp(base, "percentageWidth", "absoluteWidth")
                val (cx, cy) = centreOf(base, w, w)
                val ev = d.obj("event")
                val rocker = dirKind[d.str("style")] != "BUTTON"
                val dirs = mapOf("up" to keysOf(ev.arr("upKeycode")), "down" to keysOf(ev.arr("downKeycode")), "left" to keysOf(ev.arr("leftKeycode")), "right" to keysOf(ev.arr("rightKeycode")))
                val isDefault = dirs == mapOf("up" to listOf("K:GLFW_KEY_W"), "down" to listOf("K:GLFW_KEY_S"), "left" to listOf("K:GLFW_KEY_A"), "right" to listOf("K:GLFW_KEY_D"))
                if (rocker && ev.bool("sneak")) notes.add("Sneak ของจอย (Rocker) ไม่มีใน ALL: ข้าม")
                if (!rocker && (!ev.bool("sneak") || ev.int("sneakKeycode", SHIFT_CODE) != SHIFT_CODE)) notes.add("Sneak กลาง D-Pad ต่างจากของ ALL (ALL ใช้ Shift ซ้ายเสมอ)")
                if (ev.str("followOption") == "FOLLOW" || ev.str("followOption") == "CENTER_FOLLOW") notes.add("followOption ของจอยไม่มีใน ALL")
                items += TouchItem(
                    newTouchId(), if (rocker) TouchKind.JOYSTICK else TouchKind.DPAD, if (rocker) JOY else DPAD, cx, cy, w.coerceAtLeast(20f),
                    gameOnly = base.str("visibilityType") == "IN_GAME", dirEvents = if (isDefault) emptyMap() else dirs, group = groupName,
                )
            }
        }
        if (menuOnly > 0) notes.add("$menuOnly รายการแสดงเฉพาะในเมนู: ข้าม")
        if (wrap > 0) notes.add("$wrap ปุ่มขนาดตามข้อความ: ใช้ขนาดในไฟล์")
        if (styled > 0 || root.arr("buttonStyles").size() > 1 || root.arr("directionStyles").size() > 1) notes.add("สไตล์ปุ่ม/จอย (สี ขอบ ฟอนต์): ไม่ถูกแปลง ปุ่มใช้กรอบของ ALL")
        val hidden = if (grouped) groups.filter { it.str("visibility") == "INVISIBLE" }.mapNotNull { names[it.str("id")] }.toSet() else emptySet()
        return LayoutMapper.Mapped(root.str("name") to TouchLayout(items, hiddenGroups = hidden), notes.list())
    }

    // ---- ALL -> FCL ----

    fun toFcl(layout: TouchLayout, name: String, screen: LayoutMapper.Screen, aspectOf: (String) -> Float = { 1f }): LayoutMapper.Mapped<String> {
        val notes = Notes()
        val groupNames = layout.items.map { it.group }.distinct().sortedBy { it != null }
        val idOf = groupNames.associateWith { UUID.randomUUID().toString() }
        val byName = groupNames.filterNotNull().associateWith { idOf.getValue(it) }

        fun fclEvent(list: List<String>, autoKeep: Boolean = false): JsonObject {
            val keys = JsonArray(); var text = ""; var menu = false; var ime = false; val bind = JsonArray()
            for (e in list) when {
                e.startsWith("L:") -> when (e.substring(2)) {
                    LAUNCHER_EVENT_SWITCH_MENU -> menu = true; LAUNCHER_EVENT_SWITCH_IME -> ime = true
                    LAUNCHER_EVENT_SCROLL_UP_SINGLE -> keys.add(SCROLL_UP); LAUNCHER_EVENT_SCROLL_DOWN_SINGLE -> keys.add(SCROLL_DOWN)
                    else -> notes.add("คำสั่ง Launcher ${e.substring(2)} ไม่มีใน FCL: ข้าม")
                }
                e.startsWith("T:") -> if (text.isEmpty()) text = e.substring(2) else notes.add("ปุ่มหนึ่งปุ่มส่งข้อความได้ข้อความเดียวต่อ trigger ใน FCL: ตัดข้อความที่เกิน")
                e.isGroupEvent() -> e.removePrefix("G:").split(":", limit = 2).let { p ->
                    byName[p.getOrNull(1)]?.let { bind.add(it) } ?: notes.add("Action กลุ่มที่ไม่มีอยู่: ข้าม")
                    if (p[0] != "toggle") notes.add("เปิด/ปิดกลุ่มแยก: FCL มีแต่สลับ (ส่งออกเป็นสลับ)")
                }
                else -> e.removePrefix("K:").let { k ->
                    val code = when (k) { "GLFW_MOUSE_BUTTON_LEFT" -> MOUSE_LEFT; "GLFW_MOUSE_BUTTON_MIDDLE" -> MOUSE_MIDDLE; "GLFW_MOUSE_BUTTON_RIGHT" -> MOUSE_RIGHT; else -> KEY_CODES[k] }
                    if (code != null) keys.add(code) else notes.add("คีย์ $k ไม่มีใน FCL: ข้าม")
                }
            }
            return JsonObject().apply {
                addProperty("autoKeep", autoKeep); addProperty("autoClick", false); addProperty("openMenu", menu); addProperty("switchTouchMode", false)
                addProperty("input", ime); addProperty("quickInput", false); addProperty("outputText", text); add("outputKeycodes", keys); add("bindViewGroup", bind)
            }
        }
        fun baseInfo(i: TouchItem, wDp: Float, hDp: Float): JsonObject {
            val p = LayoutMapper.position(i.x, i.y, wDp * screen.density, hDp * screen.density, screen)
            fun milli(dp: Float) = Math.round(dp * screen.density / screen.height * 1000).coerceIn(10, 1000)
            fun size(dp: Float) = JsonObject().apply { addProperty("reference", "SCREEN_HEIGHT"); addProperty("size", milli(dp)) }
            return JsonObject().apply {
                addProperty("visibilityType", if (i.gameOnly) "IN_GAME" else "ALWAYS")
                addProperty("xPosition", Math.round(p.x / 10f)); addProperty("yPosition", Math.round(p.y / 10f))
                addProperty("sizeType", "PERCENTAGE"); addProperty("absoluteWidth", Math.round(wDp)); addProperty("absoluteHeight", Math.round(hDp))
                add("percentageWidth", size(wDp)); add("percentageHeight", size(hDp))
            }
        }
        var looks = 0; var msgs = 0
        val viewGroups = JsonArray()
        for (g in groupNames) {
            val buttons = JsonArray(); val directions = JsonArray()
            for (i in layout.items.filter { it.group == g }) {
                if (i.image != null || i.parts.isNotEmpty() || i.tint != 0 || i.bgAlpha != 1f || i.iconAlpha != 1f) looks++
                if (i.kind == TouchKind.BUTTON) {
                    if (i.msgs != null) msgs++
                    val w = i.sizeDp; val h = w * aspectOf(i.sprite) * i.heightScale
                    val text = i.caption ?: i.label ?: i.sprite.substringAfterLast("/").removePrefix("control_").removePrefix("key_")
                    buttons.add(JsonObject().apply {
                        addProperty("id", UUID.randomUUID().toString()); addProperty("text", text); addProperty("style", "Default"); add("baseInfo", baseInfo(i, w, h))
                        add("event", JsonObject().apply {
                            addProperty("pointerFollow", i.dragCam); addProperty("Movable", false); if (i.slideIn) addProperty("swipable", true)
                            add("pressEvent", fclEvent(i.events, autoKeep = i.toggle)); add("longPressEvent", fclEvent(i.longEvents))
                            add("clickEvent", fclEvent(i.clickEvents)); add("doubleClickEvent", fclEvent(i.doubleEvents))
                        })
                    })
                } else {
                    val w = i.sizeDp; val dpad = i.kind == TouchKind.DPAD
                    if (i.lockEvents.isNotEmpty()) notes.add("ล็อกจอยสติ๊ก ไม่มีใน FCL: ข้าม")
                    fun codes(k: String) = fclEvent(i.dirList(k)).getAsJsonArray("outputKeycodes")
                    directions.add(JsonObject().apply {
                        addProperty("id", UUID.randomUUID().toString()); addProperty("style", if (dpad) "ALL Pad" else "ALL Rocker"); add("baseInfo", baseInfo(i, w, w))
                        add("event", JsonObject().apply {
                            add("upKeycode", codes("up")); add("downKeycode", codes("down")); add("leftKeycode", codes("left")); add("rightKeycode", codes("right"))
                            addProperty("followOption", "FIXED"); addProperty("sneak", dpad); addProperty("sneakKeycode", SHIFT_CODE)
                        })
                    })
                }
            }
            viewGroups.add(JsonObject().apply {
                addProperty("id", idOf.getValue(g)); addProperty("name", g ?: "main")
                addProperty("visibility", if (g != null && g in layout.hiddenGroups) "INVISIBLE" else "VISIBLE")
                add("viewData", JsonObject().apply { add("buttonList", buttons); add("directionList", directions) })
            })
        }
        if (msgs > 0) notes.add("$msgs ปุ่มข้อความแบบรายการ: ส่งออกเป็นปุ่มปกติ (รายการข้อความหาย)")
        if (looks > 0) notes.add("$looks รายการมีสี/รูป/ความโปร่งใสของ ALL: ไม่ถูกส่งออก (FCL ใช้สไตล์ของตัวเอง)")
        val doc = JsonObject().apply {
            addProperty("id", UUID.randomUUID().toString().take(8)); addProperty("name", name); addProperty("version", "1.0"); addProperty("versionCode", 1)
            addProperty("author", "ALL Launcher"); addProperty("description", ""); addProperty("controllerVersion", CONTROLLER_VERSION)
            add("buttonStyles", JsonArray().apply { add(buttonStyle("Default")) })
            add("directionStyles", JsonArray().apply { add(directionStyle("ALL Rocker", "ROCKER")); add(directionStyle("ALL Pad", "BUTTON")) })
            add("viewGroups", viewGroups)
        }
        return LayoutMapper.Mapped(com.google.gson.GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(doc), notes.list())
    }

    /** the default look FCL itself ships (same numbers as FCL's default controller): colours are ARGB ints, sizes in tenths of dp */
    private fun buttonStyle(name: String?) = JsonObject().apply {
        name?.let { addProperty("name", it) }; addProperty("textColor", -1); addProperty("textSize", 12); addProperty("strokeColor", -12303292); addProperty("strokeWidth", 10)
        addProperty("cornerRadius", 100); addProperty("fillColor", 0); addProperty("textColorPressed", -1); addProperty("textSizePressed", 12)
        addProperty("strokeColorPressed", -12303292); addProperty("strokeWidthPressed", 10); addProperty("cornerRadiusPressed", 100); addProperty("fillColorPressed", -3355444)
    }

    private fun directionStyle(name: String, type: String) = JsonObject().apply {
        addProperty("name", name); addProperty("styleType", type)
        add("buttonStyle", buttonStyle(null).apply { addProperty("interval", 50) })
        add("rockerStyle", JsonObject().apply {
            addProperty("rockerSize", 400); addProperty("bgCornerRadius", 500); addProperty("bgStrokeWidth", 20); addProperty("bgStrokeColor", -12303292)
            addProperty("bgFillColor", 0); addProperty("rockerCornerRadius", 500); addProperty("rockerStrokeWidth", 10); addProperty("rockerStrokeColor", -12303292); addProperty("rockerFillColor", -7829368)
        })
    }
}
