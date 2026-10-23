package com.movtery.zalithlauncher.all.controls

import com.movtery.inputmap.keycodes.ControlEventKeyName
import com.movtery.inputmap.keycodes.ControlEventKeycode
import com.movtery.layer_controller.data.ButtonPosition
import com.movtery.layer_controller.data.lang.createTranslatable
import com.movtery.layer_controller.event.ClickEvent
import com.movtery.layer_controller.layout.ControlLayout
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_SCROLL_DOWN_SINGLE
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_SCROLL_UP_SINGLE

/**
 * Controls Studio V1.2: any key the existing input backend can send (ControlEventKeycode -> lwjglEvent, plus the launcher's
 * scroll events), as plain ClickEvents in the normal profile file. Nothing new is sent: the game's EventHandler presses every
 * event of a button in list order on touch and releases them in the same order on lift, in one call.
 */
enum class KeyGroup { LETTER, DIGIT, FUNCTION, CONTROL, MODIFIER, ARROW, SYMBOL, NUMPAD, MOUSE }

/** [key] is the event string stored in the profile; [label] is what the user sees */
data class CustomKey(val event: ClickEvent, val label: String, val group: KeyGroup) {
    val isModifier get() = group == KeyGroup.MODIFIER
}

private fun k(code: String, group: KeyGroup, label: String? = null) =
    CustomKey(ClickEvent(ClickEvent.Type.Key, code), label ?: ControlEventKeyName.getNameByKey(code) ?: code.removePrefix("GLFW_KEY_"), group)

private fun scroll(code: String, label: String) = CustomKey(ClickEvent(ClickEvent.Type.LauncherEvent, code), label, KeyGroup.MOUSE)

/** Every key offered in the "custom" picker; each one exists in ControlEventKeycode (checked by a unit test) */
val CustomKeys: List<CustomKey> = buildList {
    ('A'..'Z').forEach { add(k("GLFW_KEY_$it", KeyGroup.LETTER)) }
    ('0'..'9').forEach { add(k("GLFW_KEY_$it", KeyGroup.DIGIT)) }
    (1..12).forEach { add(k("GLFW_KEY_F$it", KeyGroup.FUNCTION, "F$it")) }
    listOf("ESCAPE" to "Esc", "TAB" to "Tab", "SPACE" to "Space", "ENTER" to "Enter", "BACKSPACE" to "Backspace",
        "DELETE" to "Delete", "INSERT" to "Insert", "HOME" to "Home", "END" to "End", "PAGE_UP" to "Page Up",
        "PAGE_DOWN" to "Page Down", "CAPS_LOCK" to "Caps Lock").forEach { (c, l) -> add(k("GLFW_KEY_$c", KeyGroup.CONTROL, l)) }
    listOf("LEFT_SHIFT" to "Shift", "LEFT_CONTROL" to "Ctrl", "LEFT_ALT" to "Alt",
        "RIGHT_SHIFT" to "Right Shift", "RIGHT_CONTROL" to "Right Ctrl", "RIGHT_ALT" to "Right Alt").forEach { (c, l) -> add(k("GLFW_KEY_$c", KeyGroup.MODIFIER, l)) }
    listOf("UP" to "↑ Up", "DOWN" to "↓ Down", "LEFT" to "← Left", "RIGHT" to "→ Right").forEach { (c, l) -> add(k("GLFW_KEY_$c", KeyGroup.ARROW, l)) }
    listOf("MINUS" to "-", "EQUAL" to "=", "LEFT_BRACKET" to "[", "RIGHT_BRACKET" to "]", "SEMICOLON" to ";", "APOSTROPHE" to "'",
        "COMMA" to ",", "PERIOD" to ".", "SLASH" to "/", "BACKSLASH" to "\\", "GRAVE_ACCENT" to "`").forEach { (c, l) -> add(k("GLFW_KEY_$c", KeyGroup.SYMBOL, l)) }
    (0..9).forEach { add(k("GLFW_KEY_KP_$it", KeyGroup.NUMPAD, "Num $it")) }
    add(k("GLFW_MOUSE_BUTTON_LEFT", KeyGroup.MOUSE, "Mouse Left"))
    add(k("GLFW_MOUSE_BUTTON_RIGHT", KeyGroup.MOUSE, "Mouse Right"))
    add(k("GLFW_MOUSE_BUTTON_MIDDLE", KeyGroup.MOUSE, "Mouse Middle"))
    add(k("GLFW_MOUSE_BUTTON_4", KeyGroup.MOUSE, "Mouse 4"))
    add(k("GLFW_MOUSE_BUTTON_5", KeyGroup.MOUSE, "Mouse 5"))
    //the wheel is not a key: the launcher's own single-step scroll events (one notch per tap)
    add(scroll(LAUNCHER_EVENT_SCROLL_UP_SINGLE, "Wheel Up"))
    add(scroll(LAUNCHER_EVENT_SCROLL_DOWN_SINGLE, "Wheel Down"))
}

/** Thai search aliases so "ซ้าย", "ล้อ" or "ลูกศร" also find keys */
private val ThaiAliases = mapOf(
    KeyGroup.MOUSE to "เมาส์ คลิก ล้อ", KeyGroup.ARROW to "ลูกศร", KeyGroup.FUNCTION to "ฟังก์ชัน",
    KeyGroup.MODIFIER to "ปุ่มควบคุม", KeyGroup.NUMPAD to "แป้นตัวเลข"
)

fun searchKeys(query: String): List<CustomKey> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return CustomKeys
    return CustomKeys.filter { key ->
        key.label.lowercase().contains(q) || key.event.key.lowercase().removePrefix("glfw_key_").contains(q) ||
            (ThaiAliases[key.group]?.contains(q) == true) ||
            (q.contains("ซ้าย") && key.label.contains("Left")) || (q.contains("ขวา") && key.label.contains("Right")) ||
            (q.contains("กลาง") && key.label.contains("Middle"))
    }.sortedBy { if (it.label.lowercase() == q) 0 else if (it.label.lowercase().startsWith(q)) 1 else 2 }
}

/** Label of any stored event (custom or not), e.g. "Ctrl", "Z", "Wheel Up" */
fun keyLabel(e: ClickEvent): String =
    CustomKeys.firstOrNull { it.event == e }?.label ?: (if (e.type == ClickEvent.Type.Key) ControlEventKeyName.getNameByKey(e.key) else null) ?: e.key

/** "Ctrl + Z" */
fun comboLabel(events: List<ClickEvent>): String = events.joinToString(" + ") { keyLabel(it) }

/**
 * Modifiers first: the backend presses the list in order, so Ctrl/Shift/Alt must already be down (and in GLFW's modifier state,
 * which lwjglEvent updates) when the main key goes down. Duplicates are dropped.
 */
fun orderCombo(events: List<ClickEvent>): List<ClickEvent> {
    fun isMod(e: ClickEvent) = CustomKeys.any { it.event == e && it.isModifier }
    return events.distinct().sortedBy { if (isMod(it)) 0 else 1 }
}

/** A new custom button with the user's name; falls back to the key combo as its name */
fun ControlLayout.addCustom(name: String, events: List<ClickEvent>, at: ButtonPosition?): Pair<ControlLayout, String> {
    val ordered = orderCombo(events)
    require(ordered.isNotEmpty()) { "a button needs at least one key" }
    //base on a normal action button (same size rules) and swap name + events; widen for long labels
    val label = name.trim().ifEmpty { comboLabel(ordered) }
    val (l0, id) = addAction(StudioAction.CHAT, at = at)
    val wide = (1100 * (1 + (label.length - 4).coerceAtLeast(0) * 0.12f)).toInt().coerceAtMost(3000)
    return l0.updateButton(id) {
        it.copy(text = createTranslatable(label), _clickEvents = ordered, buttonSize = it.buttonSize.copy(widthPercentage = wide))
    } to id
}

/** Sanity: every catalog key must be one the backend can map (Key) or one of its launcher events */
fun isBackendKey(e: ClickEvent): Boolean = when (e.type) {
    ClickEvent.Type.Key -> ControlEventKeycode.getKeycodeFromEvent(e.key) != null
    ClickEvent.Type.LauncherEvent -> e.key == LAUNCHER_EVENT_SCROLL_UP_SINGLE || e.key == LAUNCHER_EVENT_SCROLL_DOWN_SINGLE
    else -> false
}
