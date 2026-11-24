package com.movtery.zalithlauncher.all.touch

import com.movtery.layer_controller.data.ButtonPosition
import com.movtery.layer_controller.data.ButtonSize
import com.movtery.layer_controller.data.JoystickData
import com.movtery.layer_controller.data.JoystickDirection
import com.movtery.layer_controller.data.JoystickTriggerMode
import com.movtery.layer_controller.data.NormalData
import com.movtery.layer_controller.data.VisibilityType
import com.movtery.layer_controller.data.lang.TranslatableString
import com.movtery.layer_controller.event.ClickEvent
import com.movtery.layer_controller.layout.ControlLayer
import com.movtery.layer_controller.layout.ControlLayout
import com.movtery.layer_controller.layout.EmptyControlLayout
import java.util.UUID

/**
 * Zalith 2 (LayerController [ControlLayout]) <-> ALL ([TouchLayout]).
 * Pure mapping, no Android: the caller reads the file with LayerController's own loader ([com.movtery.layer_controller.layout.loadLayoutFromString])
 * and saves with its own `saveToFile`. Everything that has no equivalent on the other side is reported in [Mapped.warnings], never dropped silently.
 *
 *  Zalith 2 layer            <-> ALL group (hidden layer = group in [TouchLayout.hiddenGroups]); one layer only = no group
 *  Zalith 2 switch/show/hide <-> "G:toggle|show|hide:<group>" event
 *  NormalData                <-> BUTTON (caption = text, blank frame sprite)
 *  JoystickData              <-> JOYSTICK (direction keys, lock)
 *  position / size           <-> centre fraction / dp (Zalith 2 position = top-left of the free space: (screen - widget) * p)
 */
object LayoutMapper {
    class Screen(val width: Int, val height: Int, val density: Float)
    class Mapped<T>(val value: T, val warnings: List<String>)

    private const val BLANK = "touch_controls/control_blank_m"
    private const val JOY = "joystick/joystick_circle"

    // ---- Zalith 2 -> ALL ----

    /** [aspectOf]: height / width of an ALL sprite (a button's height is derived from it) */
    fun fromZalith(
        layout: ControlLayout, screen: Screen,
        aspectOf: (String) -> Float = { 1f },
    ): Mapped<TouchLayout> {
        val warn = mutableListOf<String>()
        val grouped = layout.layers.size > 1
        val names = HashMap<String, String>() // layer uuid -> unique group name
        layout.layers.forEachIndexed { i, l ->
            val base = l.name.replace(":", "").trim().ifEmpty { "layer ${i + 1}" }.take(16)
            var n = base; var k = 2
            while (n in names.values) n = "$base ${k++}"
            names[l.uuid] = n
        }
        fun group(l: ControlLayer) = if (grouped) names[l.uuid] else null
        var unmappedEvents = 0
        fun ev(e: ClickEvent): String? = when (e.type) {
            ClickEvent.Type.Key -> "K:${e.key}"
            ClickEvent.Type.LauncherEvent -> if (e.key.startsWith("GLFW_MOUSE_")) "K:${e.key}" else "L:${e.key}"
            ClickEvent.Type.SendText -> "T:${e.key}"
            ClickEvent.Type.SwitchLayer -> names[e.key]?.takeIf { grouped }?.let { groupEvent("toggle", it) }
            ClickEvent.Type.ShowLayer -> names[e.key]?.takeIf { grouped }?.let { groupEvent("show", it) }
            ClickEvent.Type.HideLayer -> names[e.key]?.takeIf { grouped }?.let { groupEvent("hide", it) }
        }.also { if (it == null) unmappedEvents++ }

        val items = mutableListOf<TouchItem>()
        var styled = 0; var penetrable = 0; var wrap = 0; var texts = 0; var menuOnly = 0; var clampedHeight = 0
        for (layer in layout.layers) {
            val layerMenuOnly = layer.visibilityType == VisibilityType.IN_MENU
            for (b in layer.normalButtons) {
                if (layerMenuOnly || b.visibilityType == VisibilityType.IN_MENU) { menuOnly++; continue }
                if (b.buttonStyle != null) styled++
                if (b.isPenetrable) penetrable++
                if (b.buttonSize.type == ButtonSize.Type.WrapContent) wrap++
                val w = dp(b.buttonSize.type, b.buttonSize.widthDp, b.buttonSize.widthPercentage, b.buttonSize.widthReference, screen)
                val h = dp(b.buttonSize.type, b.buttonSize.heightDp, b.buttonSize.heightPercentage, b.buttonSize.heightReference, screen)
                val (cx, cy) = centre(b.position, w * screen.density, h * screen.density, screen)
                val scale = h / (w * aspectOf(BLANK))
                if (scale !in 0.2f..4f) clampedHeight++
                val text = b.text.translate().trim()
                items += TouchItem(
                    newTouchId(), TouchKind.BUTTON, BLANK, cx, cy, w.coerceAtLeast(10f),
                    events = b.clickEvents.mapNotNull(::ev), toggle = b.isToggleable,
                    gameOnly = layer.visibilityType == VisibilityType.IN_GAME || b.visibilityType == VisibilityType.IN_GAME,
                    dragCam = b.isPenetrable, slideIn = b.isSwipple, caption = text.ifEmpty { null },
                    heightScale = scale.coerceIn(0.2f, 4f), group = group(layer),
                )
            }
            for (j in layer.joystickButtons) {
                if (layerMenuOnly || j.visibilityType == VisibilityType.IN_MENU) { menuOnly++; continue }
                val size = j.toButtonSize()
                val w = dp(size.type, size.widthDp, size.widthPercentage, size.widthReference, screen)
                val (cx, cy) = centre(j.position, w * screen.density, w * screen.density, screen)
                fun dir(d: JoystickDirection) = j.directionEvents[d].orEmpty().mapNotNull(::ev)
                val dirs = mapOf("up" to dir(JoystickDirection.North), "down" to dir(JoystickDirection.South), "left" to dir(JoystickDirection.West), "right" to dir(JoystickDirection.East))
                val isDefault = dirs == mapOf("up" to listOf("K:GLFW_KEY_W"), "down" to listOf("K:GLFW_KEY_S"), "left" to listOf("K:GLFW_KEY_A"), "right" to listOf("K:GLFW_KEY_D"))
                val diagonals = mapOf(
                    JoystickDirection.NorthEast to listOf("up", "right"), JoystickDirection.NorthWest to listOf("up", "left"),
                    JoystickDirection.SouthEast to listOf("down", "right"), JoystickDirection.SouthWest to listOf("down", "left"),
                )
                if (diagonals.any { (d, parts) -> dir(d).toSet() != parts.flatMap { dirs.getValue(it) }.toSet() }) warn += "จอยสติ๊ก: ทิศเฉียงที่ตั้งเอง ALL ใช้ทิศหลักสองทิศรวมกัน"
                if (j.deadZoneRatio != 0.5f || j.lockThreshold != 0.3f || j.triggerMode != JoystickTriggerMode.DRAG || j.joystickStyleId != null) warn += "จอยสติ๊ก: ค่าตายตัว/ระยะล็อก/โหมดสัมผัส/สไตล์ ไม่ถูกแปลง (ALL ใช้ค่าของตัวเอง)"
                items += TouchItem(
                    newTouchId(), TouchKind.JOYSTICK, JOY, cx, cy, w.coerceAtLeast(20f),
                    gameOnly = layer.visibilityType == VisibilityType.IN_GAME || j.visibilityType == VisibilityType.IN_GAME,
                    dirEvents = if (isDefault) emptyMap() else dirs, lockEvents = if (j.canLock) j.lockEvents.mapNotNull(::ev) else emptyList(), group = group(layer),
                )
            }
            texts += layer.textBoxes.size
        }
        if (menuOnly > 0) warn += "$menuOnly รายการแสดงเฉพาะในเมนู: ข้าม (ALL ไม่มีปุ่มเฉพาะเมนู)"
        if (texts > 0) warn += "กล่องข้อความ $texts รายการ: ข้าม (ALL ไม่มีกล่องข้อความ)"
        if (layout.styles.isNotEmpty() || styled > 0) warn += "สไตล์ปุ่ม (สี/รูปทรง/ฟอนต์): ไม่ถูกแปลง ปุ่มใช้กรอบของ ALL ($styled ปุ่มมีสไตล์)"
        if (layout.joystickStyles.isNotEmpty()) warn += "สไตล์จอยสติ๊ก ${layout.joystickStyles.size} แบบ: ไม่ถูกแปลง"
        if (wrap > 0) warn += "$wrap ปุ่มขนาดตามข้อความ: ใช้ 50 dp แทน"
        if (penetrable > 0) warn += "$penetrable ปุ่มให้สัมผัสทะลุ: แปลงเป็น \"ลากกล้องจากปุ่ม\" (ใกล้เคียง ไม่เหมือนเป๊ะ)"
        if (clampedHeight > 0) warn += "$clampedHeight ปุ่มสัดส่วนสูง/กว้างเกินช่วงของ ALL: ปรับให้อยู่ในช่วง"
        if (unmappedEvents > 0) warn += "$unmappedEvents Action ที่แปลงไม่ได้ (สลับ/ซ่อนชั้นที่ไม่มีอยู่ หรือไฟล์มีชั้นเดียว): ข้าม"
        return Mapped(TouchLayout(items, hiddenGroups = layout.layers.filter { it.hide && grouped }.mapNotNull { names[it.uuid] }.toSet()), warn.distinct())
    }

    // ---- ALL -> Zalith 2 ----

    /**
     * [legacy] = editorVersion 2 file (the format Zalith 2 2.3.1 reads; newer apps upgrade it themselves). It is a real conversion, not a number swap:
     * a v2 file has no joystick controls (v12), no send-text event (v6), no force show/hide layer events (v5). Those become 4 direction buttons,
     * nothing (reported) and the toggle event (reported). Everything else (keys, launcher events, layer toggle, positions, sizes) is the same schema.
     */
    fun toZalith(layout: TouchLayout, name: String, screen: Screen, aspectOf: (String) -> Float = { 1f }, legacy: Boolean = false): Mapped<ControlLayout> {
        val warn = mutableListOf<String>()
        val groups = layout.items.map { it.group }.distinct().sortedBy { it != null } // ungrouped ("main") layer first
        val uuidOf = groups.associateWith { UUID.randomUUID().toString().replace("-", "").take(12) }
        var unmapped = 0; var extraText = 0; var looks = 0; var longDouble = 0; var msgs = 0; var dpads = 0
        var textsDropped = 0; var showHide = 0; var padsAsButtons = 0; var lockDropped = 0
        fun ev(e: String): ClickEvent? = when {
            e.startsWith("L:") -> ClickEvent(ClickEvent.Type.LauncherEvent, e.substring(2))
            e.startsWith("T:") -> if (legacy) { textsDropped++; null } else ClickEvent(ClickEvent.Type.SendText, e.substring(2))
            e.isGroupEvent() -> e.removePrefix("G:").split(":", limit = 2).let { p ->
                val uuid = uuidOf[p.getOrNull(1)]
                val type = when (p[0]) {
                    "show", "hide" -> if (legacy) { showHide++; ClickEvent.Type.SwitchLayer } else if (p[0] == "show") ClickEvent.Type.ShowLayer else ClickEvent.Type.HideLayer
                    else -> ClickEvent.Type.SwitchLayer
                }
                uuid?.let { ClickEvent(type, it) }
            }
            else -> e.removePrefix("K:").let { k -> ClickEvent(if (k.startsWith("GLFW_MOUSE_")) ClickEvent.Type.LauncherEvent else ClickEvent.Type.Key, k) }
        }.also { if (it == null) unmapped++ }
        fun evs(l: List<String>): List<ClickEvent> {
            val m = l.mapNotNull(::ev)
            extraText += (m.count { it.type == ClickEvent.Type.SendText } - 1).coerceAtLeast(0)
            return m
        }
        fun vis(i: TouchItem) = if (i.gameOnly) VisibilityType.IN_GAME else VisibilityType.ALWAYS

        val layers = groups.map { g ->
            val normal = mutableListOf<NormalData>(); val joys = mutableListOf<JoystickData>()
            for (i in layout.items.filter { it.group == g }) {
                if (i.image != null || i.parts.isNotEmpty() || i.tint != 0 || i.bgAlpha != 1f || i.iconAlpha != 1f) looks++
                when (i.kind) {
                    TouchKind.BUTTON -> {
                        if (i.longEvents.isNotEmpty() || i.doubleEvents.isNotEmpty() || i.clickEvents.isNotEmpty()) longDouble++
                        if (i.msgs != null) msgs++
                        val w = i.sizeDp
                        val h = w * aspectOf(i.sprite) * i.heightScale
                        val text = i.caption ?: i.label ?: i.sprite.substringAfterLast("/").removePrefix("control_").removePrefix("key_")
                        normal += NormalData(
                            text = TranslatableString(text, emptyList()), uuid = UUID.randomUUID().toString().replace("-", "").take(18),
                            position = position(i.x, i.y, w * screen.density, h * screen.density, screen),
                            buttonSize = ButtonSize(
                                ButtonSize.Type.Dp, w.coerceAtLeast(5f), h.coerceAtLeast(5f),
                                pct(w, screen), pct(h, screen), ButtonSize.Reference.ScreenHeight, ButtonSize.Reference.ScreenHeight,
                            ),
                            visibilityType = vis(i), _clickEvents = evs(i.events.ifEmpty { i.clickEvents }), isSwipple = i.slideIn, isPenetrable = i.dragCam, isToggleable = i.toggle,
                        )
                    }
                    TouchKind.DPAD, TouchKind.JOYSTICK -> {
                        if (i.kind == TouchKind.DPAD) dpads++
                        val d = { k: String -> evs(i.dirList(k)) }
                        val up = d("up"); val down = d("down"); val left = d("left"); val right = d("right")
                        val px = i.sizeDp * screen.density
                        if (legacy) { // v2 has no joystick control: a cross of 4 direction buttons (thirds of the pad), no diagonals, no lock
                            padsAsButtons++; if (i.lockEvents.isNotEmpty()) lockDropped++
                            val b = i.sizeDp / 3f; val bPx = b * screen.density
                            for ((txt, evsOf, dx, dy) in listOf(Quad("▲", up, 0, -1), Quad("▼", down, 0, 1), Quad("◀", left, -1, 0), Quad("▶", right, 1, 0))) {
                                normal += NormalData(
                                    text = TranslatableString(txt, emptyList()), uuid = UUID.randomUUID().toString().replace("-", "").take(18),
                                    position = position(i.x + dx * bPx / screen.width, i.y + dy * bPx / screen.height, bPx, bPx, screen),
                                    buttonSize = ButtonSize(ButtonSize.Type.Dp, b.coerceAtLeast(5f), b.coerceAtLeast(5f), pct(b, screen), pct(b, screen), ButtonSize.Reference.ScreenHeight, ButtonSize.Reference.ScreenHeight),
                                    visibilityType = vis(i), _clickEvents = evsOf, isSwipple = true, isPenetrable = false, isToggleable = false,
                                )
                            }
                            continue
                        }
                        joys += JoystickData(
                            uuid = UUID.randomUUID().toString().replace("-", "").take(18), position = position(i.x, i.y, px, px, screen),
                            sizeType = ButtonSize.Type.Dp, sizeDp = i.sizeDp.coerceAtLeast(20f), visibilityType = vis(i),
                            canLock = i.kind == TouchKind.JOYSTICK && i.lockEvents.isNotEmpty(),
                            directionEvents = mapOf(
                                JoystickDirection.North to up, JoystickDirection.South to down, JoystickDirection.West to left, JoystickDirection.East to right,
                                JoystickDirection.NorthEast to (up + right).distinct(), JoystickDirection.NorthWest to (up + left).distinct(),
                                JoystickDirection.SouthEast to (down + right).distinct(), JoystickDirection.SouthWest to (down + left).distinct(),
                            ),
                            lockEvents = if (i.kind == TouchKind.JOYSTICK) evs(i.lockEvents) else emptyList(),
                        )
                    }
                }
            }
            ControlLayer(
                name = g ?: "main", uuid = uuidOf.getValue(g), hide = g != null && g in layout.hiddenGroups,
                visibilityType = VisibilityType.ALWAYS, normalButtons = normal, joystickButtons = joys,
            )
        }
        if (unmapped - textsDropped > 0) warn += "${unmapped - textsDropped} Action ชี้ไปที่กลุ่มที่ไม่มีอยู่: ข้าม"
        if (textsDropped > 0) warn += "$textsDropped ข้อความ (T:): ข้าม (editorVersion 2 ไม่มีเหตุการณ์ส่งข้อความ ต้องใช้ v12)"
        if (showHide > 0) warn += "$showHide Action แสดง/ซ่อนกลุ่มโดยตรง: เปลี่ยนเป็นสลับกลุ่ม (editorVersion 2 มีแค่สลับ ผลอาจกลับกัน)"
        if (padsAsButtons > 0) warn += "$padsAsButtons D-Pad/จอยสติ๊ก: editorVersion 2 ไม่มีจอยสติ๊ก แปลงเป็นปุ่มทิศทาง 4 ปุ่ม (ไม่มีแนวทแยง" + (if (lockDropped > 0) ", ล็อก $lockDropped ตัวหาย)" else ")")
        if (extraText > 0) warn += "ปุ่มหนึ่งปุ่มส่งข้อความได้ข้อความเดียวใน Zalith 2: ตัดข้อความที่เกิน $extraText รายการ"
        if (longDouble > 0) warn += "$longDouble ปุ่มมี กดค้าง/แตะสองครั้ง: Zalith 2 ไม่มี (ส่งออกเฉพาะการกดปกติ)"
        if (msgs > 0) warn += "$msgs ปุ่มข้อความแบบรายการ: ส่งออกเป็นปุ่มปกติ (รายการข้อความหาย)"
        if (dpads > 0 && !legacy) warn += "$dpads D-Pad: ส่งออกเป็นจอยสติ๊ก (ปุ่ม Sneak กลาง D-Pad ไม่ถูกส่งออก)"
        if (dpads > 0 && legacy) warn += "ปุ่ม Sneak กลาง D-Pad ไม่ถูกส่งออก"
        if (looks > 0) warn += "$looks รายการมีสี/รูป/ความโปร่งใสของ ALL: ไม่ถูกส่งออก (Zalith 2 ใช้สไตล์ของตัวเอง)"
        warn += if (legacy) "ไฟล์นี้เป็นเลย์เอาต์ Zalith 2 editorVersion 2 (Legacy): Zalith 2 ทุกรุ่นเปิดได้ (รุ่นใหม่อัปเกรดไฟล์ให้เอง)"
            else "ไฟล์นี้เป็นเลย์เอาต์ Zalith 2 editorVersion ${EmptyControlLayout.editorVersion}: ต้องใช้ Zalith 2 รุ่น 2.4.11 ขึ้นไป (รุ่นเก่า เช่น 2.3.1 จะแจ้ง Control layout versions are not supported)"
        val info = ControlLayout.Info(TranslatableString(name, emptyList()), TranslatableString("ALL Launcher", emptyList()), TranslatableString("", emptyList()), 1, "1.0")
        return Mapped(ControlLayout(info, layers, emptyList(), emptyList(), if (legacy) 2 else EmptyControlLayout.editorVersion), warn)
    }

    private data class Quad(val text: String, val events: List<ClickEvent>, val dx: Int, val dy: Int)

    // ---- FCL: see [FclFormat] (independent code, written from FCL's own GPL-3.0 sources) ----

    fun isPojav(text: String) = PojavFormat.isPojav(text)
    fun fromPojav(text: String, screen: Screen, aspectOf: (String) -> Float = { 1f }) = PojavFormat.fromPojav(text, screen, aspectOf)
    fun toPojav(layout: TouchLayout, screen: Screen, aspectOf: (String) -> Float = { 1f }) = PojavFormat.toPojav(layout, screen, aspectOf)
    /** Zalith 1 (Java Pojav fork): same file as Pojav + its info block; import goes through fromPojav (name is read from the block) */
    fun toZalith1(layout: TouchLayout, name: String, screen: Screen, aspectOf: (String) -> Float = { 1f }) = PojavFormat.toPojav(layout, screen, aspectOf, name)
    fun isFcl(text: String) = FclFormat.isFcl(text)
    fun fromFcl(text: String, screen: Screen, aspectOf: (String) -> Float = { 1f }) = FclFormat.fromFcl(text, screen, aspectOf)
    fun toFcl(layout: TouchLayout, name: String, screen: Screen, aspectOf: (String) -> Float = { 1f }) = FclFormat.toFcl(layout, name, screen, aspectOf)

    // ---- geometry ----

    private fun dp(type: ButtonSize.Type, dp: Float, pct: Int, ref: ButtonSize.Reference, s: Screen): Float = when (type) {
        ButtonSize.Type.Dp -> dp
        ButtonSize.Type.Percentage -> pct / 10000f * (if (ref == ButtonSize.Reference.ScreenWidth) s.width else s.height) / s.density
        ButtonSize.Type.WrapContent -> 50f
    }

    /** Zalith 2 top-left = (screen - widget) * p; ALL = centre fraction */
    internal fun centre(p: ButtonPosition, wPx: Float, hPx: Float, s: Screen): Pair<Float, Float> {
        val fw = (wPx / s.width).coerceIn(0f, 1f); val fh = (hPx / s.height).coerceIn(0f, 1f)
        return ((1 - fw) * p.xPercentage() + fw / 2) to ((1 - fh) * p.yPercentage() + fh / 2)
    }

    internal fun position(cx: Float, cy: Float, wPx: Float, hPx: Float, s: Screen): ButtonPosition {
        val fw = (wPx / s.width).coerceIn(0f, 0.99f); val fh = (hPx / s.height).coerceIn(0f, 0.99f)
        fun p(c: Float, f: Float) = (((c - f / 2) / (1 - f)).coerceIn(0f, 1f) * 10000).let { Math.round(it) }.coerceIn(0, 10000)
        return ButtonPosition(p(cx, fw), p(cy, fh))
    }

    private fun pct(dp: Float, s: Screen) = Math.round(dp * s.density / s.height * 10000).coerceIn(100, 10000)
}
