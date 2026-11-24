package com.movtery.zalithlauncher.all.touch

import com.movtery.layer_controller.layout.ControlLayout
import com.movtery.layer_controller.layout.loadLayoutFromString
import com.movtery.layer_controller.utils.saveToFile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

/** Zalith 2 <-> ALL, with a REAL Zalith 2 layout file (the app's own default, editorVersion 12) */
class TouchLayoutMapperTest {
    private val screen = LayoutMapper.Screen(2436, 1080, 2.75f)
    private fun real(): ControlLayout = loadLayoutFromString(javaClass.getResource("/zl2/default_layout.json")!!.readText())
    private fun roundTrip(z: ControlLayout): ControlLayout {
        val f = File.createTempFile("zl2", ".json"); runBlocking { z.saveToFile(f) }
        return loadLayoutFromString(f.readText()).also { f.delete() } // saved and read back by LayerController itself
    }

    @Test fun realFileImports() {
        val z = real()
        val m = LayoutMapper.fromZalith(z, screen)
        val buttons = z.layers.sumOf { it.normalButtons.size }; val joys = z.layers.sumOf { it.joystickButtons.size }
        assertEquals(buttons + joys, m.value.items.size)
        assertEquals(joys, m.value.items.count { it.kind == TouchKind.JOYSTICK })
        assertTrue(m.value.items.all { it.x in 0f..1f && it.y in 0f..1f })
        assertTrue(m.warnings.any { it.contains("สไตล์ปุ่ม") }) // the file has 8 button styles: reported, not silently dropped
    }

    @Test fun layersBecomeGroupsAndSwitchEventsFollow() {
        val z = real(); val m = LayoutMapper.fromZalith(z, screen)
        val names = z.layers.map { it.name }.toSet()
        assertEquals(names, m.value.items.mapNotNull { it.group }.toSet())
        val gui = m.value.items.first { it.caption == "GUI" }
        assertEquals(4, gui.events.count { it.startsWith("G:toggle:") })
        assertTrue(gui.events.all { it.removePrefix("G:toggle:") in names })
    }

    @Test fun keysAndMouseAndLauncherEvents() {
        val all = LayoutMapper.fromZalith(real(), screen).value.items.flatMap { it.events }
        assertTrue("K:GLFW_KEY_F3" in all); assertTrue("K:GLFW_MOUSE_BUTTON_LEFT" in all); assertTrue(all.any { it.startsWith("L:") })
    }

    @Test fun joystickKeepsDefaultsAndLock() {
        val j = LayoutMapper.fromZalith(real(), screen).value.items.first { it.kind == TouchKind.JOYSTICK }
        assertEquals(emptyMap<String, List<String>>(), j.dirEvents)
        assertEquals(listOf("K:GLFW_KEY_LEFT_CONTROL"), j.lockEvents)
        assertTrue(j.gameOnly)
    }

    @Test fun exportBackKeepsActionsPositionsSizes() {
        val z = real(); val imported = LayoutMapper.fromZalith(z, screen).value
        val back = roundTrip(LayoutMapper.toZalith(imported, "t", screen).value)
        val before = z.layers.flatMap { l -> l.normalButtons.map { l.name to it } }
        val after = back.layers.flatMap { l -> l.normalButtons.map { l.name to it } }
        assertEquals(before.size, after.size)
        for ((a, b) in before.zip(after)) {
            assertEquals(a.first, b.first)
            assertEquals(a.second.text.translate(), b.second.text.translate())
            assertEquals(a.second.clickEvents.size, b.second.clickEvents.size)
            assertEquals(a.second.isToggleable, b.second.isToggleable)
            assertTrue(abs(a.second.position.x - b.second.position.x) <= 2 && abs(a.second.position.y - b.second.position.y) <= 2)
            val wa = a.second.buttonSize.widthPercentage / 10000f * screen.height / screen.density
            assertTrue(abs(wa - b.second.buttonSize.widthDp) < 1f)
        }
        // the key / launcher / mouse events survive one by one
        val ek = { l: ControlLayout -> l.layers.flatMap { it.normalButtons }.flatMap { b -> b.clickEvents.filter { !it.isAboutLayers() }.map { it.type to it.key } } }
        assertEquals(ek(z), ek(back))
        // layer switches still point at the SAME layers (by name)
        val nameOf = { l: ControlLayout -> l.layers.associate { it.uuid to it.name } }
        val sw = { l: ControlLayout -> l.layers.flatMap { it.normalButtons }.flatMap { b -> b.clickEvents.filter { it.isAboutLayers() }.map { nameOf(l)[it.key] } } }
        assertEquals(sw(z), sw(back))
    }

    /** the file the ALL app itself wrote on the phone (Studio > Profiles > export) must load in LayerController and come back */
    @Test fun fileExportedByTheAppLoadsAndImportsAgain() {
        val z = loadLayoutFromString(javaClass.getResource("/zl2/exported_by_all.json")!!.readText())
        val orig = real()
        assertEquals(orig.layers.map { it.name }, z.layers.map { it.name })
        assertEquals(orig.layers.sumOf { it.normalButtons.size }, z.layers.sumOf { it.normalButtons.size })
        assertEquals(1, z.layers.sumOf { it.joystickButtons.size })
        val again = LayoutMapper.fromZalith(z, screen)
        assertEquals(15, again.value.items.size)
        assertTrue(again.warnings.none { it.contains("Action ที่แปลงไม่ได้") })
    }

    @Test fun exportReportsWhatZalith2CannotHold() {
        val items = listOf(
            TouchItem("a", TouchKind.BUTTON, "touch_controls/control_jump", 0.5f, 0.5f, 50f, listOf("K:GLFW_KEY_SPACE", "T:one", "T:two"), longEvents = listOf("T:x"), tint = 0x123456),
            TouchItem("b", TouchKind.BUTTON, "touch_controls/control_chat", 0.2f, 0.2f, 50f, listOf("K:GLFW_KEY_T"), msgs = listOf("hi")),
            TouchItem("c", TouchKind.DPAD, "touch_controls/control_dpad_cross", 0.2f, 0.8f, 150f),
        )
        val m = LayoutMapper.toZalith(TouchLayout(items), "n", screen)
        val w = m.warnings.joinToString("\n")
        assertTrue("one text", w.contains("ตัดข้อความที่เกิน 1"))
        assertTrue("long press", w.contains("กดค้าง"))
        assertTrue("msgs", w.contains("ปุ่มข้อความแบบรายการ"))
        assertTrue("dpad", w.contains("D-Pad"))
        assertTrue("look", w.contains("สี/รูป"))
        assertEquals(1, m.value.layers.size) // no groups: one layer
    }

    @Test fun hiddenGroupBecomesHiddenLayerAndBack() {
        val items = listOf(
            TouchItem("a", TouchKind.BUTTON, "s", 0.5f, 0.5f, 50f, listOf("G:toggle:Tools")),
            TouchItem("b", TouchKind.BUTTON, "s", 0.3f, 0.5f, 50f, listOf("K:GLFW_KEY_E"), group = "Tools"),
        )
        val z = roundTrip(LayoutMapper.toZalith(TouchLayout(items, hiddenGroups = setOf("Tools")), "n", screen).value)
        assertEquals(setOf(false, true), z.layers.map { it.hide }.toSet())
        val back = LayoutMapper.fromZalith(z, screen)
        assertEquals(setOf("Tools"), back.value.hiddenGroups)
        assertTrue(back.value.items.any { it.events == listOf("G:toggle:Tools") })
    }

    @Test fun joystickWithCustomKeysRoundTrips() {
        val j = TouchItem("j", TouchKind.JOYSTICK, "joystick/joystick_circle", 0.2f, 0.7f, 140f, dirEvents = mapOf("up" to listOf("K:GLFW_KEY_UP"), "down" to listOf("K:GLFW_KEY_S"), "left" to listOf("K:GLFW_KEY_A"), "right" to listOf("K:GLFW_KEY_D")), lockEvents = listOf("K:GLFW_KEY_LEFT_CONTROL"))
        val back = LayoutMapper.fromZalith(roundTrip(LayoutMapper.toZalith(TouchLayout(listOf(j)), "n", screen).value), screen).value.items.single()
        assertEquals(listOf("K:GLFW_KEY_UP"), back.dirList("up")); assertEquals(j.lockEvents, back.lockEvents)
        assertTrue(abs(back.x - j.x) < 0.01f && abs(back.y - j.y) < 0.01f && abs(back.sizeDp - j.sizeDp) < 1f)
    }

    /** the installed Zalith 2 2.3.1 (editorVersion 2) refused ALL files: "Control layout versions are not supported" - the export says which app version is needed */
    @Test fun exportWarnsAboutTheEditorVersionNeeded() {
        val w = LayoutMapper.toZalith(TouchLayout(emptyList()), "t", screen).warnings
        assertTrue(w.any { it.contains("editorVersion") && it.contains("2.4.11") })
    }

    // ---- Legacy editorVersion 2 export (what Zalith 2 2.3.1 reads) ----
    private fun legacyItems() = listOf(
        TouchItem("a", TouchKind.BUTTON, "s", 0.5f, 0.5f, 50f, listOf("K:GLFW_KEY_E", "T:hi", "G:show:Tools"), caption = "btn"),
        TouchItem("b", TouchKind.BUTTON, "s", 0.3f, 0.5f, 50f, listOf("K:GLFW_KEY_F"), group = "Tools", caption = "in"),
        TouchItem("c", TouchKind.DPAD, "touch_controls/control_dpad_cross", 0.2f, 0.8f, 150f),
        TouchItem("d", TouchKind.JOYSTICK, "joystick/joystick_circle", 0.8f, 0.8f, 120f, lockEvents = listOf("K:GLFW_KEY_LEFT_CONTROL")),
    )

    @Test fun legacyExportIsARealV2Structure() {
        val m = LayoutMapper.toZalith(TouchLayout(legacyItems(), hiddenGroups = setOf("Tools")), "t", screen, legacy = true)
        assertEquals(2, m.value.editorVersion)
        assertTrue(m.value.layers.all { it.joystickButtons.isEmpty() })                       // no v12 joystick control
        val main = m.value.layers.first { it.name == "main" }.normalButtons
        // 2 buttons + D-Pad (4) + joystick (4) = main: 1 button + 8 direction buttons ; Tools: 1 button
        assertEquals(1 + 8, main.size); assertEquals(1, m.value.layers.first { it.name == "Tools" }.normalButtons.size)
        val btn = main.first { it.text.default == "btn" }
        assertEquals(listOf("key", "switch_layer"), btn.clickEvents.map { if (it.type.name == "Key") "key" else it.type.name.let { _ -> "switch_layer" } })   // T: dropped, show -> switch
        assertTrue(btn.clickEvents[1].type.name == "SwitchLayer")
        val arrows = main.filter { it.text.default in listOf("▲", "▼", "◀", "▶") }
        assertEquals(8, arrows.size); assertEquals(listOf("W"), arrows.filter { it.text.default == "▲" }.map { it.clickEvents.first().key.removePrefix("GLFW_KEY_") }.distinct())
        val w = m.warnings.joinToString(" | ")
        for (x in listOf("ข้อความ (T:)", "สลับกลุ่ม", "ไม่มีจอยสติ๊ก", "ล็อก 1", "editorVersion 2 (Legacy)")) assertTrue(x, w.contains(x))
        assertTrue(!w.contains("2.4.11"))
    }

    @Test fun legacyJsonHasOnlyKeysOfARealV2File() {
        val f = File.createTempFile("zl2old", ".json"); runBlocking { LayoutMapper.toZalith(TouchLayout(legacyItems()), "t", screen, legacy = true).value.saveToFile(f) }
        val text = f.readText(); f.delete()
        assertTrue(text.contains("\"editorVersion\": 2")); assertTrue(!text.contains("joystickButtons")); assertTrue(!text.contains("commonStyle"))
        val back = loadLayoutFromString(text)                                                 // LayerController (the app's own loader) accepts it
        assertEquals(9 + 1, back.layers.sumOf { it.normalButtons.size })                    // 1 + 8 direction buttons + 1 in Tools, nothing lost on load
    }

    @Test fun currentExportIsStillV12WithJoystick() {
        val m = LayoutMapper.toZalith(TouchLayout(legacyItems()), "t", screen)
        assertEquals(12, m.value.editorVersion); assertEquals(2, m.value.layers.sumOf { it.joystickButtons.size })
        assertTrue(m.warnings.any { it.contains("2.4.11") })
    }
}
