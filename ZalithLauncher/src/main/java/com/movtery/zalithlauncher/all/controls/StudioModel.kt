package com.movtery.zalithlauncher.all.controls

import com.movtery.inputmap.keycodes.ControlEventKeycode
import com.movtery.layer_controller.data.ButtonPosition
import com.movtery.layer_controller.data.ButtonSize
import com.movtery.layer_controller.data.ButtonStyle
import com.movtery.layer_controller.data.DefaultButtonStyle
import com.movtery.layer_controller.data.JoystickData
import com.movtery.layer_controller.data.NormalData
import com.movtery.layer_controller.data.VisibilityType
import com.movtery.layer_controller.data.lang.createTranslatable
import com.movtery.layer_controller.event.ClickEvent
import com.movtery.layer_controller.layout.ControlLayer
import com.movtery.layer_controller.layout.ControlLayout
import com.movtery.layer_controller.layout.EmptyControlLayout
import com.movtery.layer_controller.layout.EmptyLayoutInfo
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_SCROLL_DOWN_SINGLE
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_SCROLL_UP_SINGLE
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_SWITCH_IME
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_SWITCH_MENU
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.util.UUID
import kotlin.math.roundToInt

/**
 * ALL Controls Studio - pure editing logic over Zalith's own ControlLayout (same JSON file, same geometry, same events).
 * Every action maps to events the game already handles (Key -> lwjglEvent, LauncherEvent -> launcherEvent), so a button made here
 * always does something in Minecraft. No new input engine.
 */
/** [widthPct] lets a short-label button be wider than tall (slot buttons: the game wraps "ช่อง 1" in a square) */
enum class StudioAction(val labelTh: String, val labelEn: String, val events: List<ClickEvent>, val sizePct: Int = 1400, val widthPct: Int = sizePct) {
    JUMP("กระโดด", "Jump", key(ControlEventKeycode.GLFW_KEY_SPACE), 1700),
    ATTACK("โจมตี", "Attack", key(ControlEventKeycode.GLFW_MOUSE_BUTTON_LEFT), 1700),
    USE("ใช้ของ", "Use", key(ControlEventKeycode.GLFW_MOUSE_BUTTON_RIGHT), 1500),
    SNEAK("ย่อง", "Sneak", key(ControlEventKeycode.GLFW_KEY_LEFT_SHIFT)),
    SPRINT("วิ่ง", "Sprint", key(ControlEventKeycode.GLFW_KEY_LEFT_CONTROL)),
    INVENTORY("กระเป๋า", "Inventory", key(ControlEventKeycode.GLFW_KEY_E)),
    ESC("ESC", "ESC", key(ControlEventKeycode.GLFW_KEY_ESCAPE), 1100),
    DROP("ทิ้งของ", "Drop", key(ControlEventKeycode.GLFW_KEY_Q), 1100),
    CHAT("แชท", "Chat", key(ControlEventKeycode.GLFW_KEY_T), 1100),
    PERSPECTIVE("มุมกล้อง", "Camera", key(ControlEventKeycode.GLFW_KEY_F5), 1100),
    HOTBAR_PREV("◀ ช่อง", "Prev slot", launcher(LAUNCHER_EVENT_SCROLL_UP_SINGLE), 1100),
    HOTBAR_NEXT("ช่อง ▶", "Next slot", launcher(LAUNCHER_EVENT_SCROLL_DOWN_SINGLE), 1100),
    KEYBOARD("คีย์บอร์ด", "Keyboard", launcher(LAUNCHER_EVENT_SWITCH_IME), 1100),
    MENU("เมนูเกม", "Game menu", launcher(LAUNCHER_EVENT_SWITCH_MENU), 1100),
    PLAYER_LIST("รายชื่อผู้เล่น", "Player list", key(ControlEventKeycode.GLFW_KEY_TAB), 1100, 2200),
    DEBUG_INFO("ข้อมูลเกม", "Debug info", key(ControlEventKeycode.GLFW_KEY_F3), 1100, 1700),
    FULLSCREEN("เต็มจอ", "Fullscreen", key(ControlEventKeycode.GLFW_KEY_F11), 1100),
    //hotbar slots: the number keys, so the slot is picked directly (not a scroll); a no-break space keeps "ช่อง 1" on one line
    SLOT_1("ช่อง\u00A01", "Slot\u00A01", key(ControlEventKeycode.GLFW_KEY_1), 1000, 1500),
    SLOT_2("ช่อง\u00A02", "Slot\u00A02", key(ControlEventKeycode.GLFW_KEY_2), 1000, 1500),
    SLOT_3("ช่อง\u00A03", "Slot\u00A03", key(ControlEventKeycode.GLFW_KEY_3), 1000, 1500),
    SLOT_4("ช่อง\u00A04", "Slot\u00A04", key(ControlEventKeycode.GLFW_KEY_4), 1000, 1500),
    SLOT_5("ช่อง\u00A05", "Slot\u00A05", key(ControlEventKeycode.GLFW_KEY_5), 1000, 1500),
    SLOT_6("ช่อง\u00A06", "Slot\u00A06", key(ControlEventKeycode.GLFW_KEY_6), 1000, 1500),
    SLOT_7("ช่อง\u00A07", "Slot\u00A07", key(ControlEventKeycode.GLFW_KEY_7), 1000, 1500),
    SLOT_8("ช่อง\u00A08", "Slot\u00A08", key(ControlEventKeycode.GLFW_KEY_8), 1000, 1500),
    SLOT_9("ช่อง\u00A09", "Slot\u00A09", key(ControlEventKeycode.GLFW_KEY_9), 1000, 1500),
    /** the movement stick (JoystickData, W/A/S/D by the existing joystick widget) */
    MOVE("ปุ่มเคลื่อนที่", "Movement", emptyList(), 3800);

    fun label(thai: Boolean) = if (thai) labelTh else labelEn

    val isSlot get() = name.startsWith("SLOT_")

    /** What the player would press on a PC, in plain words (display only; the real binding is [events]) */
    fun inputHint(thai: Boolean): String = when (this) {
        JUMP -> if (thai) "Spacebar" else "Spacebar"
        ATTACK -> if (thai) "คลิกซ้าย" else "Left click"
        USE -> if (thai) "คลิกขวา" else "Right click"
        SNEAK -> "Shift"
        SPRINT -> "Ctrl"
        INVENTORY -> if (thai) "ปุ่ม E" else "E key"
        ESC -> if (thai) "ปุ่ม Esc" else "Esc key"
        DROP -> if (thai) "ปุ่ม Q" else "Q key"
        CHAT -> if (thai) "ปุ่ม T" else "T key"
        PERSPECTIVE -> if (thai) "ปุ่ม F5" else "F5 key"
        HOTBAR_PREV -> if (thai) "เลื่อนเมาส์ขึ้น" else "Scroll up"
        HOTBAR_NEXT -> if (thai) "เลื่อนเมาส์ลง" else "Scroll down"
        KEYBOARD -> if (thai) "เปิดคีย์บอร์ดมือถือ" else "Phone keyboard"
        MENU -> if (thai) "เมนูของ ALL" else "ALL menu"
        PLAYER_LIST -> if (thai) "ปุ่ม Tab" else "Tab key"
        DEBUG_INFO -> if (thai) "ปุ่ม F3" else "F3 key"
        FULLSCREEN -> if (thai) "ปุ่ม F11" else "F11 key"
        SLOT_1, SLOT_2, SLOT_3, SLOT_4, SLOT_5, SLOT_6, SLOT_7, SLOT_8, SLOT_9 -> (if (thai) "ปุ่ม " else "Key ") + name.last()
        MOVE -> "W A S D"
    }
}

private fun key(code: String) = listOf(ClickEvent(ClickEvent.Type.Key, code))
private fun launcher(code: String) = listOf(ClickEvent(ClickEvent.Type.LauncherEvent, code))

/** Which simple action a button performs, or null when it was made elsewhere with other events (left as it is) */
fun actionOf(button: NormalData): StudioAction? =
    StudioAction.entries.firstOrNull { it != StudioAction.MOVE && it.events == button.clickEvents }

fun newId(): String = UUID.randomUUID().toString().replace("-", "").take(12)

/** One layer that the Studio edits: the first layer, created when the layout has none */
private fun ControlLayout.ensureLayer(): ControlLayout =
    if (layers.isNotEmpty()) this
    else copy(layers = listOf(ControlLayer(name = "ALL", uuid = newId(), hide = false, visibilityType = VisibilityType.ALWAYS)))

private fun ControlLayout.mapLayers(f: (ControlLayer) -> ControlLayer) = copy(layers = layers.map(f))

fun newButton(action: StudioAction, x: Int = 5000, y: Int = 5000, sizePct: Int = action.sizePct, name: String? = null, thai: Boolean = true, widthPct: Int = sizePct * action.widthPct / action.sizePct) = NormalData(
    text = createTranslatable(name ?: action.label(thai)),
    uuid = newId(),
    position = ButtonPosition(x.coerceIn(0, 10000), y.coerceIn(0, 10000)),
    buttonSize = ButtonSize(
        type = ButtonSize.Type.Percentage, widthDp = 50f, heightDp = 50f,
        widthPercentage = widthPct, heightPercentage = sizePct,
        widthReference = ButtonSize.Reference.ScreenHeight, heightReference = ButtonSize.Reference.ScreenHeight
    ),
    visibilityType = VisibilityType.ALWAYS,
    _clickEvents = action.events,
    isSwipple = false, isPenetrable = false, isToggleable = false
)

fun newJoystick(x: Int = 1000, y: Int = 8500, sizePct: Int = StudioAction.MOVE.sizePct) =
    JoystickData(uuid = newId(), position = ButtonPosition(x, y), sizePercentage = sizePct)

/** Adds [action] to the first layer, in the middle of the screen (or the joystick bottom left); returns the layout and the new id */
fun ControlLayout.addAction(action: StudioAction, thai: Boolean = true, at: ButtonPosition? = null): Pair<ControlLayout, String> {
    val base = ensureLayer()
    return if (action == StudioAction.MOVE) {
        val j = newJoystick().let { if (at != null) it.copy(position = at) else it }
        base.copy(layers = listOf(base.layers.first().let { it.copy(joystickButtons = it.joystickButtons + j) }) + base.layers.drop(1)) to j.uuid
    } else {
        val b = newButton(action, x = at?.x ?: 5000, y = at?.y ?: 5000, thai = thai)
        base.copy(layers = listOf(base.layers.first().let { it.copy(normalButtons = it.normalButtons + b) }) + base.layers.drop(1)) to b.uuid
    }
}

fun ControlLayout.findButton(id: String): NormalData? = layers.firstNotNullOfOrNull { l -> l.normalButtons.firstOrNull { it.uuid == id } }
fun ControlLayout.findJoystick(id: String): JoystickData? = layers.firstNotNullOfOrNull { l -> l.joystickButtons.firstOrNull { it.uuid == id } }

fun ControlLayout.updateButton(id: String, f: (NormalData) -> NormalData) =
    mapLayers { l -> l.copy(normalButtons = l.normalButtons.map { if (it.uuid == id) f(it) else it }) }

fun ControlLayout.updateJoystick(id: String, f: (JoystickData) -> JoystickData) =
    mapLayers { l -> l.copy(joystickButtons = l.joystickButtons.map { if (it.uuid == id) f(it) else it }) }

fun ControlLayout.removeWidget(id: String) =
    mapLayers { l -> l.copy(normalButtons = l.normalButtons.filterNot { it.uuid == id }, joystickButtons = l.joystickButtons.filterNot { it.uuid == id }) }

fun ControlLayout.moveWidget(id: String, pos: ButtonPosition) =
    updateButton(id) { it.copy(position = pos) }.updateJoystick(id) { it.copy(position = pos) }

/** Same rule as the game: position = top-left / (screen - widget) in 1/10000 */
fun percentPosition(leftPx: Float, topPx: Float, widgetW: Float, widgetH: Float, screenW: Float, screenH: Float): ButtonPosition {
    fun pct(v: Float, free: Float) = if (free > 0) (v / free * 10000).roundToInt().coerceIn(0, 10000) else 0
    return ButtonPosition(pct(leftPx, screenW - widgetW), pct(topPx, screenH - widgetH))
}

/** Pixel size of a button exactly as Buttons.buttonSize computes it in game (WrapContent shown as the default 14 % square) */
fun buttonPixelSize(size: ButtonSize, screenW: Float, screenH: Float, density: Float): Pair<Float, Float> = when (size.type) {
    ButtonSize.Type.Dp -> size.widthDp * density to size.heightDp * density
    ButtonSize.Type.Percentage -> {
        fun ref(r: ButtonSize.Reference) = if (r == ButtonSize.Reference.ScreenWidth) screenW else screenH
        ref(size.widthReference) * size.widthPercentage / 10000f to ref(size.heightReference) * size.heightPercentage / 10000f
    }
    ButtonSize.Type.WrapContent -> screenH * 0.14f to screenH * 0.14f
}

fun joystickPixelSize(j: JoystickData, screenH: Float, density: Float): Float {
    val s = j.toButtonSize()
    return if (s.type == ButtonSize.Type.Dp) s.widthDp * density else screenH * s.widthPercentage / 10000f
}

/** Sets a button's size in pixels (stored as % of screen height, like new ALL buttons; clamped to the format's range) */
fun ControlLayout.resizeButton(id: String, widthPx: Float, heightPx: Float, screenH: Float) = updateButton(id) {
    fun pct(px: Float) = (px / screenH * 10000).roundToInt().coerceIn(300, 10000)
    it.copy(buttonSize = it.buttonSize.copy(
        type = ButtonSize.Type.Percentage,
        widthPercentage = pct(widthPx), heightPercentage = pct(heightPx),
        widthReference = ButtonSize.Reference.ScreenHeight, heightReference = ButtonSize.Reference.ScreenHeight
    ))
}

fun ControlLayout.resizeJoystick(id: String, sidePx: Float, screenH: Float) = updateJoystick(id) {
    it.copy(sizeType = ButtonSize.Type.Percentage, sizePercentage = (sidePx / screenH * 10000).roundToInt().coerceIn(2000, 10000))
}

fun ControlLayout.setButtonName(id: String, name: String) = updateButton(id) { it.copy(text = createTranslatable(name)) }

fun ControlLayout.setButtonAction(id: String, action: StudioAction, thai: Boolean = true) = updateButton(id) { b ->
    //follow the old default name so a renamed button keeps its own name
    val oldDefault = actionOf(b)?.label(thai)
    val name = if (oldDefault == null || b.text.default == oldDefault) action.label(thai) else b.text.default
    b.copy(text = createTranslatable(name), _clickEvents = action.events)
}

/** Current opacity of a button (its style's alpha; buttons without a style use the default 1.0) */
fun ControlLayout.opacityOf(button: NormalData): Float =
    styles.firstOrNull { it.uuid == button.buttonStyle }?.lightStyle?.alpha ?: 1f

/**
 * Opacity is stored the way the format already does it: in a ButtonStyle. The button's current style (or the default one) is
 * copied with the new alpha; an identical existing style is reused so styles do not pile up.
 */
fun ControlLayout.setButtonOpacity(id: String, alpha: Float): ControlLayout {
    val a = (alpha * 100).roundToInt().coerceIn(10, 100) / 100f
    return restyle(id, "ALL ${(a * 100).roundToInt()}%") { it.copy(alpha = a) }
}

/** Current background colour of a button (its style's light colour; default style otherwise) */
fun ControlLayout.colorOf(button: NormalData): Color =
    (styles.firstOrNull { it.uuid == button.buttonStyle } ?: DefaultButtonStyle).lightStyle.backgroundColor

/** Background colour, stored in the button's ButtonStyle like the old editor's "Background Color" (light + dark alike) */
fun ControlLayout.setButtonColor(id: String, color: Color): ControlLayout =
    restyle(id, "ALL #%08X".format(color.toArgb())) { it.copy(backgroundColor = color) }

/** Copies the button's style (or the default) with [change] applied to light and dark; reuses an identical style */
internal fun ControlLayout.restyle(id: String, name: String, change: (ButtonStyle.StyleConfig) -> ButtonStyle.StyleConfig): ControlLayout {
    val button = findButton(id) ?: return this
    val base = styles.firstOrNull { it.uuid == button.buttonStyle } ?: DefaultButtonStyle
    val wanted = base.copy(lightStyle = change(base.lightStyle), darkStyle = change(base.darkStyle))
    val existing = styles.firstOrNull { it.lightStyle == wanted.lightStyle && it.darkStyle == wanted.darkStyle && it.animateSwap == wanted.animateSwap }
    val (layout, styleId) = if (existing != null) this to existing.uuid else {
        val s = ButtonStyle(name = name, uuid = newId(), animateSwap = wanted.animateSwap, commonStyle = true, lightStyle = wanted.lightStyle, darkStyle = wanted.darkStyle)
        copy(styles = styles + s) to s.uuid
    }
    return layout.updateButton(id) { it.copy(buttonStyle = styleId) }
}

/**
 * The old editor's three switches, same NormalData fields and same backend behaviour (InteractionBehavior / ObservableNormalData):
 * hold = isToggleable (tap = stays pressed, tap again = release), passThrough = isPenetrable (the touch also reaches the camera /
 * mouse under the button), draggable = isSwipple (a finger sliding onto the button presses it).
 * The backend filters out buttons that are both penetrable and swipeable (ObservableNormalData.canProcess), so those two never go on together.
 */
fun ControlLayout.setButtonBehavior(id: String, hold: Boolean, passThrough: Boolean, draggable: Boolean) = updateButton(id) {
    it.copy(isToggleable = hold, isPenetrable = passThrough, isSwipple = draggable && !passThrough)
}

/** Several keys on one button (the format's click-event list). Never empty: a button must always do something. */
fun ControlLayout.setButtonEvents(id: String, events: List<ClickEvent>, thai: Boolean = true) = updateButton(id) { b ->
    if (events.isEmpty()) return@updateButton b
    val oldDefault = actionOf(b)?.label(thai)
    val newDefault = StudioAction.entries.firstOrNull { it != StudioAction.MOVE && it.events == events }?.label(thai)
    val name = if (oldDefault != null && b.text.default == oldDefault && newDefault != null) newDefault else b.text.default
    b.copy(text = createTranslatable(name), _clickEvents = orderCombo(events))
}

/** Plain-word name of one event (for the extra-keys list); old/unknown events keep their raw key so nothing is hidden */
fun eventName(e: ClickEvent, thai: Boolean): String =
    StudioAction.entries.firstOrNull { it != StudioAction.MOVE && it.events == listOf(e) }?.let { it.label(thai) + " (" + it.inputHint(thai) + ")" } ?: keyLabel(e)

enum class StudioPreset { STANDARD, PVP, BLANK }

/** A brand-new layout for a preset (always a new file - existing profiles are never overwritten) */
fun presetLayout(preset: StudioPreset, name: String, thai: Boolean = true): ControlLayout {
    fun b(a: StudioAction, x: Int, y: Int, size: Int = a.sizePct) = newButton(a, x, y, size, thai = thai)
    val buttons: List<NormalData>
    val sticks: List<JoystickData>
    when (preset) {
        StudioPreset.BLANK -> { buttons = emptyList(); sticks = emptyList() }
        StudioPreset.STANDARD -> {
            sticks = listOf(newJoystick(400, 9200))
            buttons = listOf(
                b(StudioAction.ESC, 0, 0), b(StudioAction.PERSPECTIVE, 0, 2500), b(StudioAction.CHAT, 0, 5000), //left column: keeps the top centre free (toolbar / Minecraft hotbar-free sky)
                b(StudioAction.INVENTORY, 10000, 0, 1100), b(StudioAction.KEYBOARD, 10000, 2500),
                b(StudioAction.JUMP, 10000, 9400), b(StudioAction.ATTACK, 9800, 6600), b(StudioAction.USE, 8500, 8300),
                b(StudioAction.SNEAK, 8600, 10000, 1200), b(StudioAction.DROP, 7400, 10000, 1000)
            )
        }
        StudioPreset.PVP -> {
            sticks = listOf(newJoystick(400, 9200, 4200))
            buttons = listOf(
                b(StudioAction.ESC, 0, 0), b(StudioAction.INVENTORY, 10000, 0, 1100),
                b(StudioAction.ATTACK, 10000, 6200, 2200), b(StudioAction.JUMP, 10000, 9800, 1800),
                b(StudioAction.USE, 8300, 7600, 1800), b(StudioAction.SPRINT, 8200, 10000, 1300),
                b(StudioAction.SNEAK, 7000, 10000, 1300), b(StudioAction.HOTBAR_PREV, 0, 2500), b(StudioAction.HOTBAR_NEXT, 0, 4500) //left column, never under the Studio toolbar
            )
        }
    }
    val layer = ControlLayer(name = "ALL", uuid = newId(), hide = false, visibilityType = VisibilityType.ALWAYS, normalButtons = buttons, joystickButtons = sticks)
    return EmptyControlLayout.copy(info = EmptyLayoutInfo.copy(name = createTranslatable(name)), layers = listOf(layer))
}

/** Draft + undo: every finished edit is one step; Save writes the draft, Cancel drops it (the file is untouched until Save) */
class StudioHistory(initial: ControlLayout) {
    var saved: ControlLayout = initial
        private set
    private val past = ArrayDeque<ControlLayout>()
    var current: ControlLayout = initial
        private set

    private val future = ArrayDeque<ControlLayout>()

    fun push(next: ControlLayout) {
        if (next == current) return
        past.addLast(current)
        if (past.size > 100) past.removeFirst()
        future.clear()
        current = next
    }

    fun canUndo() = past.isNotEmpty()
    fun undo() { past.removeLastOrNull()?.let { future.addLast(current); current = it } }
    fun canRedo() = future.isNotEmpty()
    fun redo() { future.removeLastOrNull()?.let { past.addLast(current); current = it } }
    fun isDirty() = current != saved
    fun markSaved() { saved = current }
}
