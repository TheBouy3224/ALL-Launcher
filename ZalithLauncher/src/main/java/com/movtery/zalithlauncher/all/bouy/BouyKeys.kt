package com.movtery.zalithlauncher.all.bouy

import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_SCROLL_DOWN_SINGLE
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_SCROLL_UP_SINGLE


private val NAMES = mapOf(
    "esc" to "Esc", "backtick" to "` ~", "minus" to "- _", "backspace" to "Backspace", "tab" to "Tab",
    "left_bracket" to "[ {", "right_bracket" to "] }", "backslash" to "\\ |", "caps_lock" to "Caps Lock",
    "semicolon" to "; :", "apostrophe" to "' \"", "enter" to "Enter", "shift_left" to "Shift ซ้าย", "shift_right" to "Shift ขวา",
    "comma" to ", <", "period" to ". >", "slash" to "/ ?", "ctrl_left" to "Ctrl ซ้าย", "ctrl_right" to "Ctrl ขวา",
    "alt_left" to "Alt ซ้าย", "alt_right" to "Alt ขวา", "meta_left" to "Win/Meta", "menu" to "Menu", "space" to "Space",
    "insert" to "Insert", "delete" to "Delete", "home" to "Home", "end" to "End", "page_up" to "Page Up", "page_down" to "Page Down",
    "arrow_up" to "ลูกศร ขึ้น", "arrow_down" to "ลูกศร ลง", "arrow_left" to "ลูกศร ซ้าย", "arrow_right" to "ลูกศร ขวา",
    "num_lock" to "Num Lock", "num_divide" to "Numpad /", "num_multiply" to "Numpad *", "num_subtract" to "Numpad -",
    "num_plus" to "Numpad +", "num_enter" to "Numpad Enter", "num_decimal" to "Numpad .",
)

/** "keyboard/key_w" -> "W" */
fun keyName(sprite: String): String {
    val n = sprite.substringAfterLast("/").removePrefix("key_")
    return NAMES[n] ?: when {
        n.startsWith("digit_") -> n.removePrefix("digit_")
        n.startsWith("num_") -> "Numpad " + n.removePrefix("num_")
        else -> n.uppercase()
    }
}

private val HINTS = mapOf(
    "w" to "เดินหน้า", "a" to "เดินซ้าย", "s" to "เดินถอยหลัง", "d" to "เดินขวา", "e" to "เปิด/ปิดช่องเก็บของ",
    "q" to "ทิ้งของที่ถืออยู่", "f" to "สลับของเข้ามืออีกข้าง", "t" to "เปิดแชท", "space" to "กระโดด / บินขึ้น",
    "shift_left" to "ย่อตัว / บินลง", "ctrl_left" to "วิ่ง", "esc" to "หยุดเกม / เมนู", "tab" to "รายชื่อผู้เล่น",
    "f1" to "ซ่อน/แสดง HUD", "f2" to "ถ่ายภาพหน้าจอ", "f3" to "หน้าจอ Debug", "f4" to "สลับโหมดเกม (Java 1.20+)", "f5" to "เปลี่ยนมุมมองกล้อง",
    "f11" to "เต็มหน้าจอ", "l" to "ความก้าวหน้า (Advancements)", "enter" to "ยืนยัน / ส่งแชท", "slash" to "เริ่มพิมพ์คำสั่ง (/)",
    "backspace" to "ลบตัวอักษร", "m" to "แผนที่ (ม็อด)", "r" to "ใช้ได้กับม็อด", "c" to "ซูม (OptiFine) / คีย์ม็อด", "v" to "ใช้ได้กับม็อด",
    "arrow_up" to "เลื่อนขึ้น (ในเมนู/ม็อด)", "arrow_down" to "เลื่อนลง (ในเมนู/ม็อด)", "arrow_left" to "เลื่อนซ้าย", "arrow_right" to "เลื่อนขวา",
)

fun keyHint(sprite: String): String {
    val n = sprite.substringAfterLast("/").removePrefix("key_")
    return HINTS[n] ?: when {
        n.startsWith("digit_") -> "เลือกช่องที่ ${n.removePrefix("digit_")} ในแถบไอเทม (1–9)"
        n.startsWith("num_") -> "ปุ่มบน Numpad (ใช้กับม็อดหรือคีย์ที่ตั้งเอง)"
        else -> "ปุ่มทั่วไป ใช้กับม็อดหรือคีย์ที่ผู้เล่นตั้งเองในเกม"
    }
}

/**
 * One key of the PC keyboard picture on the add-button page. [sprite] is a keyboard/ sprite when the supplied art has one; otherwise
 * [label] is drawn on the standard ALL frame. [w] is the width in key units (1 = a normal key); a negative [w] is an empty gap of that size.
 */
class KCell(val sprite: String?, val label: String, val event: String, val w: Float, val en: String, val th: String, val hint: String, val gameOnly: Boolean = false)

private fun gap(w: Float) = KCell(null, "", "", -w, "", "", "")
private fun k(name: String, w: Float = 1f): KCell {
    val s = "keyboard/key_$name"
    return KCell(s, keyName(s), keyboardEvent(s) ?: "", w, keyName(s), "", keyHint(s))
}
private fun t(label: String, glfw: String, w: Float = 1f, th: String, hint: String) = KCell(null, label, "K:GLFW_KEY_$glfw", w, label, th, hint)

private fun d(n: Int) = k("digit_$n")
private fun ls(s: String) = s.map { k(it.toString()) }

/** The three blocks of a full PC keyboard, row by row: main block (15 units wide), navigation block (3), numpad (4) */
val KB_MAIN: List<List<KCell>> = listOf(
    listOf(k("esc"), gap(1f)) + (1..4).map { k("f$it") } + gap(0.5f) + (5..8).map { k("f$it") } + gap(0.5f) + (9..12).map { k("f$it") },
    listOf(k("backtick"), d(1), d(2), d(3), d(4), d(5), d(6), d(7), d(8), d(9), d(0), k("minus"), t("=", "EQUAL", th = "เครื่องหมายเท่ากับ", hint = "ปุ่ม = / + ใช้กับม็อดหรือคีย์ที่ตั้งเอง"), k("backspace", 2f)),
    listOf(k("tab", 1.5f)) + ls("qwertyuiop") + listOf(k("left_bracket"), k("right_bracket"), k("backslash", 1.5f)),
    listOf(k("caps_lock", 1.75f)) + ls("asdfghjkl") + listOf(k("semicolon"), k("apostrophe"), k("enter", 2.25f)),
    listOf(k("shift_left", 2.25f)) + ls("zxcvbnm") + listOf(k("comma"), k("period"), k("slash"), k("shift_right", 2.75f)),
    listOf(k("ctrl_left", 1.25f), k("meta_left", 1.25f), k("alt_left", 1.25f), k("space", 7.5f), k("alt_right", 1.25f), k("menu", 1.25f), k("ctrl_right", 1.25f)),
)

val KB_NAV: List<List<KCell>> = listOf(
    listOf(t("PrtSc", "PRINT_SCREEN", th = "ถ่ายภาพหน้าจอ", hint = "Print Screen (ระบบ/ม็อด)"), t("ScrLk", "SCROLL_LOCK", th = "Scroll Lock", hint = "Scroll Lock (ใช้กับม็อด)"), t("Pause", "PAUSE", th = "Pause/Break", hint = "Pause/Break (ใช้กับม็อด)")),
    listOf(k("insert"), k("home"), k("page_up")),
    listOf(k("delete"), k("end"), k("page_down")),
    listOf(gap(3f)),
    listOf(gap(1f), k("arrow_up"), gap(1f)),
    listOf(k("arrow_left"), k("arrow_down"), k("arrow_right")),
)

val KB_NUM: List<List<KCell>> = listOf(
    listOf(gap(4f)),
    listOf(k("num_lock"), k("num_divide"), k("num_multiply"), k("num_subtract")),
    listOf(k("num_7"), k("num_8"), k("num_9"), k("num_plus")),
    listOf(k("num_4"), k("num_5"), k("num_6"), gap(1f)),
    listOf(k("num_1"), k("num_2"), k("num_3"), k("num_enter")),
    listOf(k("num_0", 2f), k("num_decimal"), gap(1f)),
)

private fun m(label: String, en: String, ev: String, th: String, hint: String) = KCell(null, label, ev, 2.4f, en, th, hint, gameOnly = true)

val KB_MOUSE: List<KCell> = listOf(
    m("L", "Left Click", "K:GLFW_MOUSE_BUTTON_LEFT", "คลิกซ้าย", "โจมตี / ทุบบล็อก / เลือกในเมนู"),
    m("R", "Right Click", "K:GLFW_MOUSE_BUTTON_RIGHT", "คลิกขวา", "ใช้ของ / วางบล็อก / เปิดประตู"),
    m("M", "Middle Click", "K:GLFW_MOUSE_BUTTON_MIDDLE", "คลิกกลาง", "เลือกบล็อกที่เล็ง (Pick Block)"),
    m("▲", "Scroll Up", "L:$LAUNCHER_EVENT_SCROLL_UP_SINGLE", "เลื่อนขึ้น", "เลื่อนช่องไอเทมไปช่องก่อนหน้า"),
    m("▼", "Scroll Down", "L:$LAUNCHER_EVENT_SCROLL_DOWN_SINGLE", "เลื่อนลง", "เลื่อนช่องไอเทมไปช่องถัดไป"),
)

/** Plain-language info for the touch sprites: English / Thai name and what it does (the real mapping is read from [TOUCH_DEFAULTS]) */
val TOUCH_INFO: Map<String, Pair<String, String>> = mapOf(
    "touch_controls/control_attack" to ("Attack · โจมตี" to "โจมตี / ทุบบล็อก (คลิกซ้าย)"),
    "touch_controls/control_use" to ("Use · ใช้งาน" to "ใช้ของ / วางบล็อก (คลิกขวา)"),
    "touch_controls/control_pick_block" to ("Pick Block · เลือกบล็อก" to "เลือกบล็อกที่เล็ง (คลิกกลาง)"),
    "touch_controls/control_jump" to ("Jump · กระโดด" to "กระโดด / บินขึ้น"),
    "touch_controls/control_sneak" to ("Sneak · ย่อ" to "ย่อตัว / บินลง (แนะนำให้ตั้งล็อกค้าง)"),
    "touch_controls/control_sprint" to ("Sprint · วิ่ง" to "วิ่งเร็ว"),
    "touch_controls/control_inventory" to ("Inventory · ช่องเก็บของ" to "เปิด/ปิดช่องเก็บของ"),
    "touch_controls/control_chat" to ("Chat · แชท" to "เปิดแชท"),
    "touch_controls/control_pause" to ("Pause · พัก" to "เมนูหยุดเกม"),
    "touch_controls/control_settings" to ("Menu · เมนู ALL" to "เปิดเมนูเกมของ ALL Launcher"),
    "touch_controls/control_eye" to ("F5 · มุมมอง" to "เปลี่ยนมุมมองกล้อง"),
    "touch_controls/control_extra" to ("Keyboard · คีย์บอร์ด" to "เปิดคีย์บอร์ดมือถือเพื่อพิมพ์"),
    "touch_controls/control_f1" to ("F1 · ซ่อน HUD" to "ซ่อน/แสดงหน้าจอ HUD"),
    "touch_controls/control_f3" to ("F3 · Debug" to "หน้าจอ Debug"),
    "touch_controls/control_aim" to ("F2 · ภาพหน้าจอ" to "ถ่ายภาพหน้าจอในเกม"),
    "touch_controls/control_up" to ("Up · เดินหน้า" to "เดินหน้า (W)"),
    "touch_controls/control_down" to ("Down · ถอยหลัง" to "ถอยหลัง (S)"),
    "touch_controls/control_left" to ("Left · เดินซ้าย" to "เดินซ้าย (A)"),
    "touch_controls/control_right" to ("Right · เดินขวา" to "เดินขวา (D)"),
    "touch_controls/control_blank_s" to ("Blank S · กรอบเปล่า" to "กรอบเปล่า ไม่ทำอะไรจนกว่าจะเลือกปุ่มที่กด"),
    "touch_controls/control_blank_m" to ("Blank M · กรอบเปล่า" to "กรอบเปล่า ไม่ทำอะไรจนกว่าจะเลือกปุ่มที่กด"),
    "touch_controls/control_blank_l" to ("Blank L · กรอบเปล่า" to "กรอบเปล่า ไม่ทำอะไรจนกว่าจะเลือกปุ่มที่กด"),
)

/** how a new button behaves, for the long-press detail */
const val BEHAVIOUR_HINT = "แตะ/กดค้าง = กดอยู่ตราบที่นิ้วอยู่ ปล่อยแล้วหยุด • เปิด \"ล็อกค้างอัตโนมัติ\" ในแผงตั้งค่า = แตะครั้งเดียวกดค้าง แตะอีกครั้งปล่อย"

