package com.movtery.zalithlauncher.all.touch

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_SCROLL_DOWN_SINGLE
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_SCROLL_UP_SINGLE
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_SWITCH_IME
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_SWITCH_MENU
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_TOGGLE_GUI
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_TOGGLE_MOUSE
import java.util.Locale

/**
 * PojavLauncher control layout <-> ALL ([TouchLayout]), written for ALL directly. PojavLauncher is LGPL-3.0 (https://github.com/PojavLauncherTeam/PojavLauncher, branch v3_openjdk);
 * what is used here are facts about its file format and runtime, read from its own source and its real default layout (assets/default.json):
 *  - file: customcontrols CustomControls.java (version, scaledAt, mControlDataList, mDrawerDataList, mJoystickDataList), ControlData.java, ControlDrawerData.java, ControlJoystickData.java;
 *  - position = a small expression per axis (right, bottom, margin, width, height, screen_width, screen_height as dollar-brace variables, dp() / px()), margin = 2 dp, size in dp;
 *  - key codes = GLFW key codes (LwjglGlfwKeycode.java, the table below), negative codes = special buttons (ControlData SPECIALBTN_*);
 *  - a drawer is a button that opens a row of sub-buttons (ControlDrawer.java); the joystick is fixed to W A S D, forward lock holds Left Control (ControlJoystick.java).
 * Everything Pojav has and ALL has not (and the other way round) is listed in the warnings of the result, never dropped silently.
 */
object PojavFormat {
    private const val BLANK = "touch_controls/control_blank_m"
    private const val JOY = "joystick/joystick_circle"

    private const val SPECIAL_KEYBOARD = -1; private const val SPECIAL_TOGGLE_GUI = -2; private const val SPECIAL_MOUSE_PRIMARY = -3
    private const val SPECIAL_MOUSE_SECONDARY = -4; private const val SPECIAL_VIRTUAL_MOUSE = -5; private const val SPECIAL_MOUSE_MIDDLE = -6
    private const val SPECIAL_SCROLL_UP = -7; private const val SPECIAL_SCROLL_DOWN = -8; private const val SPECIAL_MENU = -9

    /** GLFW key suffix -> code, copied from LwjglGlfwKeycode.java (the same numbers as glfw3.h) */
    private const val GLFW_TABLE = "SPACE=32,APOSTROPHE=39,COMMA=44,MINUS=45,PERIOD=46,SLASH=47,0=48,1=49,2=50,3=51,4=52,5=53,6=54,7=55,8=56,9=57,SEMICOLON=59,EQUAL=61,A=65,B=66,C=67,D=68,E=69,F=70,G=71,H=72,I=73,J=74,K=75,L=76,M=77,N=78,O=79,P=80,Q=81,R=82,S=83,T=84,U=85,V=86,W=87,X=88,Y=89,Z=90,LEFT_BRACKET=91,BACKSLASH=92,RIGHT_BRACKET=93,GRAVE_ACCENT=96,WORLD_1=161,WORLD_2=162,ESCAPE=256,ENTER=257,TAB=258,BACKSPACE=259,INSERT=260,DELETE=261,RIGHT=262,LEFT=263,DOWN=264,UP=265,PAGE_UP=266,PAGE_DOWN=267,HOME=268,END=269,CAPS_LOCK=280,SCROLL_LOCK=281,NUM_LOCK=282,PRINT_SCREEN=283,PAUSE=284,F1=290,F2=291,F3=292,F4=293,F5=294,F6=295,F7=296,F8=297,F9=298,F10=299,F11=300,F12=301,F13=302,F14=303,F15=304,F16=305,F17=306,F18=307,F19=308,F20=309,F21=310,F22=311,F23=312,F24=313,F25=314,KP_0=320,KP_1=321,KP_2=322,KP_3=323,KP_4=324,KP_5=325,KP_6=326,KP_7=327,KP_8=328,KP_9=329,KP_DECIMAL=330,KP_DIVIDE=331,KP_MULTIPLY=332,KP_SUBTRACT=333,KP_ADD=334,KP_ENTER=335,KP_EQUAL=336,LEFT_SHIFT=340,LEFT_CONTROL=341,LEFT_ALT=342,LEFT_SUPER=343,RIGHT_SHIFT=344,RIGHT_CONTROL=345,RIGHT_ALT=346,RIGHT_SUPER=347,MENU=348"
    private val KEY_CODE: Map<String, Int> by lazy { GLFW_TABLE.split(',').associate { it.substringBefore('=') to it.substringAfter('=').toInt() } }
    private val CODE_KEY: Map<Int, String> by lazy { KEY_CODE.entries.associate { it.value to it.key } }

    private fun JsonObject?.f(k: String, d: Float): Float = this?.get(k)?.takeIf { it.isJsonPrimitive }?.runCatching { asFloat }?.getOrNull() ?: d
    private fun JsonObject?.s(k: String): String = this?.get(k)?.takeIf { it.isJsonPrimitive }?.asString ?: ""
    private fun JsonObject?.b(k: String, d: Boolean = false): Boolean = this?.get(k)?.takeIf { it.isJsonPrimitive }?.runCatching { asBoolean }?.getOrNull() ?: d
    private fun JsonObject?.arr(k: String): JsonArray = this?.get(k)?.takeIf { it.isJsonArray }?.asJsonArray ?: JsonArray()
    private fun JsonObject?.obj(k: String): JsonObject? = this?.get(k)?.takeIf { it.isJsonObject }?.asJsonObject
    private fun JsonArray.objs(): List<JsonObject> = mapNotNull { it.takeIf(JsonElement::isJsonObject)?.asJsonObject }
    private class Notes { val m = LinkedHashMap<String, Int>(); fun add(s: String) { m[s] = (m[s] ?: 0) + 1 }; fun list() = m.map { (k, v) -> if (v > 1) "$k (x$v)" else k } }

    private fun parse(text: String): JsonObject = JsonParser.parseString(text.removePrefix("﻿")).asJsonObject
    fun isPojav(text: String) = runCatching { parse(text).has("mControlDataList") }.getOrDefault(false)

    /** the position expressions: numbers, + - * /, parentheses, dp() px(); the dollar-brace variables are replaced first (null = unreadable) */
    internal fun evalExpr(expr: String, vars: Map<String, Double>, density: Float): Double? = runCatching {
        val src = Regex("[$][{]([a-z_]+)[}]").replace(expr) { (vars[it.groupValues[1]] ?: throw IllegalArgumentException(it.value)).toString() }
        var i = 0
        fun ws() { while (i < src.length && src[i] == ' ') i++ }
        lateinit var sum: () -> Double
        fun atom(): Double {
            ws()
            if (i < src.length && src[i] == '(') { i++; val v = sum(); ws(); require(src[i] == ')'); i++; return v }
            if (i < src.length && src[i] == '-') { i++; return -atom() }
            if (i < src.length && src[i] == '+') { i++; return atom() }
            val st = i
            while (i < src.length && (src[i].isLetterOrDigit() || src[i] == '.' || ((src[i] == '-' || src[i] == '+') && i > st && src[i - 1] == 'E' && src[st].isDigit()))) i++
            val tok = src.substring(st, i); require(tok.isNotEmpty())
            if (tok[0].isLetter()) {
                ws(); require(src[i] == '('); i++; val a = sum(); ws(); require(src[i] == ')'); i++
                return when (tok) { "dp" -> a / density; "px" -> a * density; else -> throw IllegalArgumentException(tok) }
            }
            return tok.toDouble()
        }
        fun term(): Double {
            var v = atom()
            while (true) { ws(); if (i < src.length && src[i] == '*') { i++; v *= atom() } else if (i < src.length && src[i] == '/') { i++; v /= atom() } else return v }
        }
        sum = {
            var v = term()
            while (true) { ws(); if (i < src.length && src[i] == '+') { i++; v += term() } else if (i < src.length && src[i] == '-') { i++; v -= term() } else break }
            v
        }
        val r = sum(); ws(); require(i == src.length); r
    }.getOrNull()?.takeIf { it.isFinite() }

    // ---- Pojav -> ALL ----

    fun fromPojav(text: String, screen: LayoutMapper.Screen, aspectOf: (String) -> Float = { 1f }): LayoutMapper.Mapped<Pair<String, TouchLayout>> {
        val root = parse(text); val notes = Notes()
        val version = root.f("version", -1f).toInt()
        require(version >= 3) { "Pojav layout version $version is too old (needs 3 or newer): open and save it in PojavLauncher once" }
        val margin = 2.0 * screen.density
        val items = mutableListOf<TouchItem>()
        val hidden = mutableSetOf<String>(); val used = mutableSetOf<String>()
        var menuOnly = 0; var badExpr = 0; var passThru = 0

        fun eventsOf(d: JsonObject): List<String> = d.arr("keycodes").mapNotNull { e ->
            val c = runCatching { e.asInt }.getOrNull() ?: return@mapNotNull null
            when {
                c == 0 -> null
                c > 0 -> CODE_KEY[c]?.let { "K:GLFW_KEY_$it" } ?: run { notes.add("keycode GLFW $c ไม่มีคีย์เทียบเท่า: ข้าม"); null }
                c == SPECIAL_MOUSE_PRIMARY -> "K:GLFW_MOUSE_BUTTON_LEFT"
                c == SPECIAL_MOUSE_SECONDARY -> "K:GLFW_MOUSE_BUTTON_RIGHT"
                c == SPECIAL_MOUSE_MIDDLE -> "K:GLFW_MOUSE_BUTTON_MIDDLE"
                c == SPECIAL_SCROLL_UP -> "L:$LAUNCHER_EVENT_SCROLL_UP_SINGLE"
                c == SPECIAL_SCROLL_DOWN -> "L:$LAUNCHER_EVENT_SCROLL_DOWN_SINGLE"
                c == SPECIAL_KEYBOARD -> "L:$LAUNCHER_EVENT_SWITCH_IME"
                c == SPECIAL_MENU -> "L:$LAUNCHER_EVENT_SWITCH_MENU"
                c == SPECIAL_TOGGLE_GUI -> "L:$LAUNCHER_EVENT_TOGGLE_GUI"
                c == SPECIAL_VIRTUAL_MOUSE -> "L:$LAUNCHER_EVENT_TOGGLE_MOUSE"
                else -> { notes.add("ปุ่มพิเศษ Pojav รหัส $c ไม่มีใน ALL: คงปุ่มไว้แต่ไม่มีคำสั่ง"); null }
            }
        }
        /** top-left of the button in px: the two expressions, right / bottom use the button's own size */
        fun place(d: JsonObject, wPx: Double, hPx: Double): Pair<Double, Double> {
            val vars = mapOf(
                "top" to 0.0, "left" to 0.0, "right" to screen.width - wPx, "bottom" to screen.height - hPx, "width" to wPx, "height" to hPx,
                "screen_width" to screen.width.toDouble(), "screen_height" to screen.height.toDouble(), "margin" to margin, "preferred_scale" to 1.0,
            )
            fun one(k: String): Double = evalExpr(d.s(k), vars, screen.density) ?: run { badExpr++; 0.0 }
            return one("dynamicX") to one("dynamicY")
        }
        fun centre(xPx: Double, yPx: Double, wPx: Double, hPx: Double) =
            ((xPx + wPx / 2) / screen.width).toFloat().coerceIn(0f, 1f) to ((yPx + hPx / 2) / screen.height).toFloat().coerceIn(0f, 1f)
        fun button(d: JsonObject, group: String?, xy: Pair<Double, Double>?, wDp: Float, hDp: Float, events: List<String>): TouchItem {
            val wPx = wDp * screen.density.toDouble(); val hPx = hDp * screen.density.toDouble()
            val (x, y) = xy ?: place(d, wPx, hPx)
            val (cx, cy) = centre(x, y, wPx, hPx)
            if (d.b("passThruEnabled")) passThru++
            return TouchItem(
                newTouchId(), TouchKind.BUTTON, BLANK, cx, cy, wDp.coerceAtLeast(10f), events = events, toggle = d.b("isToggle") && events.isNotEmpty(),
                gameOnly = !d.b("displayInMenu", true), dragCam = d.b("passThruEnabled"), slideIn = d.b("isSwipeable"),
                caption = d.s("name").trim().ifEmpty { null }, heightScale = (hDp / (wDp * aspectOf(BLANK))).coerceIn(0.2f, 4f), group = group,
            )
        }
        /** shown in game (alone or with the menu) = kept; menu only / hidden = skipped and counted */
        fun shown(d: JsonObject): Boolean = d.b("displayInGame", true).also { if (!it) menuOnly++ }

        for (d in root.arr("mControlDataList").objs()) if (shown(d)) items += button(d, null, null, d.f("width", 50f), d.f("height", 50f), eventsOf(d))
        for (dr in root.arr("mDrawerDataList").objs()) {
            val p = dr.obj("properties") ?: continue
            val base = p.s("name").replace(":", "").trim().ifEmpty { "drawer" }.take(16)
            var name = base; var k = 2
            while (name in used) name = "$base ${k++}"
            used += name; hidden += name // a Pojav drawer starts closed
            val wDp = p.f("width", 50f); val hDp = p.f("height", 50f)
            val wPx = wDp * screen.density.toDouble(); val hPx = hDp * screen.density.toDouble()
            val (dx, dy) = place(p, wPx, hPx)
            items += button(p, null, dx to dy, wDp, hDp, listOf(groupEvent("toggle", name)))
            val orient = dr.s("orientation").ifEmpty { "LEFT" }
            dr.arr("buttonProperties").objs().forEachIndexed { i, sb ->
                val sx = (wPx + margin) * (i + 1); val sy = (hPx + margin) * (i + 1)
                val pos = when (orient) { "RIGHT" -> (dx + sx) to dy; "LEFT" -> (dx - sx) to dy; "UP" -> dx to (dy - sy); "DOWN" -> dx to (dy + sy); else -> null }
                if (shown(sb)) items += if (pos != null) button(sb, name, pos, wDp, hDp, eventsOf(sb)) else button(sb, name, null, sb.f("width", 50f), sb.f("height", 50f), eventsOf(sb))
            }
        }
        for (j in root.arr("mJoystickDataList").objs()) {
            if (!shown(j)) continue
            val side = j.f("width", 100f)
            val wPx = side * screen.density.toDouble(); val hPx = j.f("height", side) * screen.density.toDouble()
            val (x, y) = place(j, wPx, hPx); val (cx, cy) = centre(x, y, wPx, hPx)
            if (j.b("absolute")) notes.add("จอยสติ๊กแบบ absolute (จอยกระโดดตามนิ้ว) ไม่มีใน ALL: ใช้แบบปกติ")
            items += TouchItem(
                newTouchId(), TouchKind.JOYSTICK, JOY, cx, cy, side.coerceAtLeast(20f), gameOnly = !j.b("displayInMenu", true),
                lockEvents = if (j.b("forwardLock")) listOf("K:GLFW_KEY_LEFT_CONTROL") else emptyList(),
            )
        }
        if (menuOnly > 0) notes.add("$menuOnly รายการไม่แสดงในเกม (เฉพาะเมนู/ซ่อน): ข้าม")
        if (badExpr > 0) notes.add("$badExpr ตำแหน่ง (นิพจน์) อ่านไม่ได้: ใช้มุมซ้ายบน")
        if (passThru > 0) notes.add("$passThru ปุ่ม passThru (ให้สัมผัสทะลุไปเมาส์): แปลงเป็น \"ลากกล้องจากปุ่ม\" (ใกล้เคียง)")
        root.f("scaledAt", 100f).takeIf { it != 100f }?.let { notes.add("ไฟล์ปรับขนาดที่ $it%: ใช้ขนาดตามไฟล์ (ไม่คูณ)") }
        notes.add("สี/ความโปร่งใส/ขอบ/มุมโค้งของปุ่ม Pojav ไม่ถูกแปลง (ปุ่มใช้กรอบของ ALL)")
        // Zalith 1 = Pojav v8 schema + mControlInfoDataList {name, version, author, desc}
        val info = root.get("mControlInfoDataList")?.takeIf { it.isJsonObject }?.asJsonObject
        val title = info?.get("name")?.takeIf { it.isJsonPrimitive }?.asString?.trim()?.takeIf { it.isNotEmpty() && it != "null" }
        return LayoutMapper.Mapped((title ?: "Pojav") to TouchLayout(items, hiddenGroups = hidden), notes.list())
    }

    // ---- ALL -> Pojav ----

    /** [zalith1Name] != null writes the Zalith 1 info block (name shown in its control list); null = plain Pojav file */
    fun toPojav(layout: TouchLayout, screen: LayoutMapper.Screen, aspectOf: (String) -> Float = { 1f }, zalith1Name: String? = null): LayoutMapper.Mapped<String> {
        val notes = Notes()
        fun num(v: Double) = String.format(Locale.US, "%.5f", v)
        fun keycodes(events: List<String>): JsonArray {
            val out = mutableListOf<Int>()
            for (e in events) when {
                e.startsWith("L:") -> when (e.substring(2)) {
                    LAUNCHER_EVENT_SWITCH_MENU -> out += SPECIAL_MENU
                    LAUNCHER_EVENT_SWITCH_IME -> out += SPECIAL_KEYBOARD
                    LAUNCHER_EVENT_TOGGLE_GUI -> out += SPECIAL_TOGGLE_GUI
                    LAUNCHER_EVENT_TOGGLE_MOUSE -> out += SPECIAL_VIRTUAL_MOUSE
                    LAUNCHER_EVENT_SCROLL_UP_SINGLE -> out += SPECIAL_SCROLL_UP
                    LAUNCHER_EVENT_SCROLL_DOWN_SINGLE -> out += SPECIAL_SCROLL_DOWN
                    else -> notes.add("คำสั่ง Launcher ${e.substring(2)} ไม่มีใน Pojav: ข้าม")
                }
                e.startsWith("T:") -> notes.add("ข้อความ (T:) ไม่มีใน Pojav: ข้าม")
                e.isGroupEvent() -> notes.add("Action กลุ่มรูปแบบอื่นที่ไม่ใช่ลิ้นชัก (Drawer): ข้าม")
                else -> e.removePrefix("K:").let { k ->
                    val c = when (k) {
                        "GLFW_MOUSE_BUTTON_LEFT" -> SPECIAL_MOUSE_PRIMARY; "GLFW_MOUSE_BUTTON_RIGHT" -> SPECIAL_MOUSE_SECONDARY
                        "GLFW_MOUSE_BUTTON_MIDDLE" -> SPECIAL_MOUSE_MIDDLE; else -> KEY_CODE[k.removePrefix("GLFW_KEY_")]
                    }
                    if (c != null) out += c else notes.add("คีย์ $k ไม่มีใน Pojav: ข้าม")
                }
            }
            if (out.size > 4) notes.add("ปุ่มหนึ่งปุ่มกดได้ไม่เกิน 4 คีย์ใน Pojav: ตัดส่วนที่เกิน")
            return JsonArray().apply { (out.take(4) + List((4 - out.size).coerceAtLeast(0)) { 0 }).forEach { add(it) } }
        }
        fun control(i: TouchItem, name: String, wDp: Float, hDp: Float, events: List<String>): JsonObject {
            val wPx = wDp * screen.density.toDouble(); val hPx = hDp * screen.density.toDouble()
            val fw = (wPx / screen.width).coerceIn(0.0, 0.99); val fh = (hPx / screen.height).coerceIn(0.0, 0.99)
            val px = ((i.x - fw / 2) / (1 - fw)).coerceIn(0.0, 1.0); val py = ((i.y - fh / 2) / (1 - fh)).coerceIn(0.0, 1.0)
            return JsonObject().apply {
                addProperty("bgColor", 0x4D000000); addProperty("cornerRadius", 0)
                addProperty("dynamicX", "$" + "{right} * " + num(px)); addProperty("dynamicY", "$" + "{bottom} * " + num(py))
                addProperty("height", Math.round(hDp * 10) / 10f); addProperty("isDynamicBtn", false); addProperty("isHideable", false); addProperty("isSwipeable", i.slideIn)
                addProperty("isToggle", i.toggle); add("keycodes", keycodes(events)); addProperty("name", name); addProperty("opacity", 1)
                addProperty("passThruEnabled", i.dragCam); addProperty("strokeColor", -1); addProperty("strokeWidth", 0); addProperty("width", Math.round(wDp * 10) / 10f)
                addProperty("displayInGame", true); addProperty("displayInMenu", !i.gameOnly)
            }
        }
        // a group opened by exactly one button whose only action is "toggle that group" becomes a Pojav drawer (FREE orientation keeps the sub-button positions)
        val opener = layout.items
            .mapNotNull { i -> i.takeIf { it.kind == TouchKind.BUTTON && it.events.size == 1 && it.events[0].startsWith("G:toggle:") }?.let { it.events[0].removePrefix("G:toggle:") to it } }
            .groupBy({ it.first }, { it.second }).filterValues { it.size == 1 }.mapValues { it.value[0] }
        val groups = layout.items.mapNotNull { it.group }.distinct().filter { it in opener }
        var looks = 0; var timed = 0; var msgs = 0; var dpads = 0; var lost = 0
        val controls = JsonArray(); val drawers = JsonArray(); val joysticks = JsonArray()
        fun name(i: TouchItem) = i.caption ?: i.label ?: i.sprite.substringAfterLast("/").removePrefix("control_").removePrefix("key_")
        fun size(i: TouchItem) = i.sizeDp to i.sizeDp * aspectOf(i.sprite) * i.heightScale
        fun look(i: TouchItem) { if (i.image != null || i.parts.isNotEmpty() || i.tint != 0 || i.bgAlpha != 1f || i.iconAlpha != 1f) looks++ }
        fun timed(i: TouchItem) { if (i.longEvents.isNotEmpty() || i.doubleEvents.isNotEmpty() || i.clickEvents.isNotEmpty()) timed++ }
        val openerIds = groups.map { opener.getValue(it).id }.toSet()
        for (i in layout.items) {
            look(i)
            if (i.id in openerIds || (i.group != null && i.group in groups)) continue
            when (i.kind) {
                TouchKind.BUTTON -> {
                    timed(i); if (i.msgs != null) msgs++
                    if (i.group != null) lost++
                    val (w, h) = size(i)
                    controls.add(control(i, name(i), w, h, i.events.ifEmpty { i.clickEvents }))
                }
                else -> {
                    if (i.kind == TouchKind.DPAD) dpads++
                    if (i.dirEvents.isNotEmpty()) notes.add("ปุ่มทิศทางที่ตั้งเอง: จอยของ Pojav ใช้ W A S D เสมอ")
                    if (i.lockEvents.any { it != "K:GLFW_KEY_LEFT_CONTROL" }) notes.add("Action ล็อกจอยสติ๊กอื่นที่ไม่ใช่ Ctrl: Pojav ล็อกด้วย Ctrl เท่านั้น")
                    joysticks.add(control(i, "joystick", i.sizeDp, i.sizeDp, emptyList()).apply {
                        addProperty("forwardLock", i.kind == TouchKind.JOYSTICK && i.lockEvents.isNotEmpty()); addProperty("absolute", false)
                    })
                }
            }
        }
        for (g in groups) {
            val o = opener.getValue(g)
            val (w, h) = size(o)
            drawers.add(JsonObject().apply {
                add("buttonProperties", JsonArray().apply {
                    layout.items.filter { it.group == g && it.kind == TouchKind.BUTTON }.forEach { s -> look(s); timed(s); val (sw, sh) = size(s); add(control(s, name(s), sw, sh, s.events.ifEmpty { s.clickEvents })) }
                })
                add("properties", control(o, g, w, h, emptyList())); addProperty("orientation", "FREE")
            })
        }
        if (lost > 0) notes.add("$lost ปุ่มอยู่ในกลุ่มที่ไม่มีปุ่มเปิดแบบเดียว (ปุ่มที่มี Action สลับกลุ่มอย่างเดียว): ส่งออกเป็นปุ่มปกติที่เห็นเสมอ")
        if (timed > 0) notes.add("$timed ปุ่มมี กดค้าง/แตะสองครั้ง/คลิกตอนปล่อย: Pojav ไม่มี (ส่งออกเฉพาะการกดปกติ)")
        if (msgs > 0) notes.add("$msgs ปุ่มข้อความแบบรายการ: ส่งออกเป็นปุ่มปกติ (รายการข้อความหาย)")
        if (dpads > 0) notes.add("$dpads D-Pad: ส่งออกเป็นจอยสติ๊ก (ปุ่ม Sneak กลาง D-Pad ไม่ถูกส่งออก)")
        if (looks > 0) notes.add("$looks รายการมีสี/รูป/ความโปร่งใสของ ALL: ไม่ถูกส่งออก (Pojav ใช้สไตล์พื้นฐาน)")
        val doc = JsonObject().apply {
            add("mControlDataList", controls); add("mDrawerDataList", drawers); add("mJoystickDataList", joysticks)
            addProperty("scaledAt", 100f); addProperty("version", 8)
            if (zalith1Name != null) add("mControlInfoDataList", JsonObject().apply { addProperty("name", zalith1Name); addProperty("version", "1.0"); addProperty("author", "ALL Launcher"); addProperty("desc", "") })
        }
        return LayoutMapper.Mapped(com.google.gson.GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(doc), notes.list())
    }
}
