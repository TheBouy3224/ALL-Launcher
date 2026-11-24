package com.movtery.zalithlauncher.all.touch

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** FCL <-> ALL (FclFormat, independent code) with FCL's own default controller file (FCL/src/main/assets/controllers/00000000.json) */
class TouchFclMapperTest {
    private val screen = LayoutMapper.Screen(2436, 1080, 2.75f)
    private fun fclText() = javaClass.getResource("/fcl/fcl_default_controller.json")!!.readText().removePrefix("\uFEFF")
    private fun json(t: String) = JsonParser.parseString(t.removePrefix("\uFEFF")).asJsonObject
    private fun buttons(j: JsonObject): List<JsonObject> = j.getAsJsonArray("viewGroups").flatMap { it.asJsonObject.getAsJsonObject("viewData").getAsJsonArray("buttonList").map { b -> b.asJsonObject } }
    private fun button(j: JsonObject, text: String) = buttons(j).first { it.get("text").asString == text }
    private fun ev(b: JsonObject, name: String) = b.getAsJsonObject("event").getAsJsonObject(name)
    private fun codes(e: JsonObject) = e.getAsJsonArray("outputKeycodes").map { it.asInt }

    @Test fun formatIsDetected() {
        assertTrue(LayoutMapper.isFcl(fclText()))
        assertTrue(!LayoutMapper.isFcl(javaClass.getResource("/zl2/default_layout.json")!!.readText()))
    }

    @Test fun fclDefaultImportsAsPlayableProfile() {
        val src = json(fclText())
        val m = LayoutMapper.fromFcl(fclText(), screen)
        val items = m.value.second.items
        assertEquals("Default", m.value.first)
        assertEquals(buttons(src).size + 1, items.size) // 12 buttons + the direction pad
        val pad = items.single { it.kind != TouchKind.BUTTON }
        assertEquals(TouchKind.DPAD, pad.kind)         // FCL default direction style is BUTTON = D-Pad
        assertEquals(emptyMap<String, List<String>>(), pad.dirEvents) // W S A D = ALL default
        assertTrue(items.all { it.x in 0f..1f && it.y in 0f..1f })
        assertEquals(listOf("K:GLFW_KEY_ESCAPE"), items.first { it.caption == "Esc" }.events)
        assertEquals(listOf("K:GLFW_KEY_E"), items.first { it.caption == "E" }.events)
    }

    /** FCL's "▼" button latches Left Shift on a click (clickEvent autoKeep): that is ALL's toggle, not a second trigger */
    @Test fun clickThatLatchesBecomesToggle() {
        val down = LayoutMapper.fromFcl(fclText(), screen).value.second.items.first { it.caption == "▼" }
        assertEquals(listOf("K:GLFW_KEY_LEFT_SHIFT"), down.events); assertTrue(down.toggle); assertEquals(emptyList<String>(), down.clickEvents)
    }

    /** the 4 FCL actions on one button: press E, click A, long press Esc, double click F3 + text "hello" */
    private fun withFour(): String {
        val j = json(fclText())
        val e = button(j, "E").getAsJsonObject("event")
        e.getAsJsonObject("clickEvent").add("outputKeycodes", JsonArray().apply { add(30) })
        e.getAsJsonObject("longPressEvent").add("outputKeycodes", JsonArray().apply { add(1) })
        e.getAsJsonObject("doubleClickEvent").apply { add("outputKeycodes", JsonArray().apply { add(61) }); addProperty("outputText", "hello") }
        return j.toString()
    }

    @Test fun allFourActionsImportToTheirOwnLists() {
        val e = LayoutMapper.fromFcl(withFour(), screen).value.second.items.first { it.caption == "E" }
        assertEquals(listOf("K:GLFW_KEY_E"), e.events)
        assertEquals(listOf("K:GLFW_KEY_A"), e.clickEvents)       // on release, NOT in the plain press
        assertEquals(listOf("K:GLFW_KEY_ESCAPE"), e.longEvents)
        assertEquals(listOf("K:GLFW_KEY_F3", "T:hello"), e.doubleEvents)
    }

    @Test fun allFourActionsExportBackAndRoundTrip() {
        val items = LayoutMapper.fromFcl(withFour(), screen).value.second
        val out = LayoutMapper.toFcl(items, "t", screen)
        assertTrue(LayoutMapper.isFcl(out.value))
        val j = json(out.value)
        assertEquals(21, j.get("controllerVersion").asInt)
        val b = button(j, "E")
        assertEquals(listOf(18), codes(ev(b, "pressEvent")))
        assertEquals(listOf(30), codes(ev(b, "clickEvent")))
        assertEquals(listOf(1), codes(ev(b, "longPressEvent")))
        assertEquals(listOf(61), codes(ev(b, "doubleClickEvent"))); assertEquals("hello", ev(b, "doubleClickEvent").get("outputText").asString)
        val again = LayoutMapper.fromFcl(out.value, screen).value.second.items
        val e0 = items.items.first { it.caption == "E" }; val e1 = again.first { it.caption == "E" }
        assertEquals(e0.events, e1.events); assertEquals(e0.clickEvents, e1.clickEvents); assertEquals(e0.longEvents, e1.longEvents); assertEquals(e0.doubleEvents, e1.doubleEvents)
        assertEquals(items.items.flatMap { it.events }.sorted(), again.flatMap { it.events }.sorted())
        assertEquals(items.items.count { it.toggle }, again.count { it.toggle })
    }

    @Test fun positionsAndSizesSurviveTheRoundTrip() {
        val a = LayoutMapper.fromFcl(fclText(), screen).value.second.items
        val b = LayoutMapper.fromFcl(LayoutMapper.toFcl(TouchLayout(a), "t", screen).value, screen).value.second.items
        assertEquals(a.size, b.size)
        for ((x, y) in a.sortedBy { it.caption ?: "~" }.zip(b.sortedBy { it.caption ?: "~" })) {
            assertTrue("${x.caption} x", kotlin.math.abs(x.x - y.x) < 0.005f); assertTrue("${x.caption} y", kotlin.math.abs(x.y - y.y) < 0.005f)
            assertTrue("${x.caption} size", kotlin.math.abs(x.sizeDp - y.sizeDp) < 1.5f)
        }
    }

    @Test fun mouseAndScrollCodes() {
        val j = json(fclText())
        button(j, "Esc").getAsJsonObject("event").getAsJsonObject("pressEvent").add("outputKeycodes", JsonArray().apply { add(1000); add(1002); add(1003); add(1004); add(1001) })
        val e = LayoutMapper.fromFcl(j.toString(), screen).value.second.items.first { it.caption == "Esc" }.events
        assertEquals(listOf("K:GLFW_MOUSE_BUTTON_LEFT", "K:GLFW_MOUSE_BUTTON_RIGHT", "L:launcher.event.scroll_up.single", "L:launcher.event.scroll_down.single", "K:GLFW_MOUSE_BUTTON_MIDDLE"), e)
        val back = json(LayoutMapper.toFcl(TouchLayout(listOf(TouchItem("a", TouchKind.BUTTON, "s", 0.5f, 0.5f, 50f, e, caption = "m"))), "t", screen).value)
        assertEquals(listOf(1000, 1002, 1003, 1004, 1001), codes(ev(button(back, "m"), "pressEvent")))
    }

    @Test fun groupsAndHiddenGroupsFollow() {
        val items = listOf(
            TouchItem("a", TouchKind.BUTTON, "s", 0.5f, 0.5f, 50f, listOf("G:toggle:Tools"), caption = "open"),
            TouchItem("b", TouchKind.BUTTON, "s", 0.3f, 0.5f, 50f, listOf("K:GLFW_KEY_E"), group = "Tools", caption = "inv"),
        )
        val out = LayoutMapper.toFcl(TouchLayout(items, hiddenGroups = setOf("Tools")), "t", screen)
        val j = json(out.value)
        assertEquals(setOf("VISIBLE", "INVISIBLE"), j.getAsJsonArray("viewGroups").map { it.asJsonObject.get("visibility").asString }.toSet())
        val back = LayoutMapper.fromFcl(out.value, screen).value.second
        assertEquals(setOf("Tools"), back.hiddenGroups)
        assertEquals(listOf("G:toggle:Tools"), back.items.first { it.caption == "open" }.events)
        assertEquals("Tools", back.items.first { it.caption == "inv" }.group)
    }

    @Test fun joystickAndCustomDirectionKeys() {
        val j = TouchItem("j", TouchKind.JOYSTICK, "joystick/joystick_circle", 0.2f, 0.7f, 140f, dirEvents = mapOf("up" to listOf("K:GLFW_KEY_UP"), "down" to listOf("K:GLFW_KEY_S"), "left" to listOf("K:GLFW_KEY_A"), "right" to listOf("K:GLFW_KEY_D")), lockEvents = listOf("K:GLFW_KEY_LEFT_CONTROL"))
        val out = LayoutMapper.toFcl(TouchLayout(listOf(j)), "t", screen)
        assertTrue(out.warnings.any { it.contains("ล็อกจอยสติ๊ก") })
        val back = LayoutMapper.fromFcl(out.value, screen).value.second.items.single()
        assertEquals(TouchKind.JOYSTICK, back.kind); assertEquals(listOf("K:GLFW_KEY_UP"), back.dirList("up"))
    }

    @Test fun whatFclCannotHoldIsReported() {
        val items = listOf(TouchItem("a", TouchKind.BUTTON, "touch_controls/control_jump", 0.5f, 0.5f, 50f, listOf("K:GLFW_KEY_SPACE", "T:one", "T:two", "L:launcher.event.nope"), tint = 0x123456),
            TouchItem("b", TouchKind.BUTTON, "touch_controls/control_chat", 0.2f, 0.2f, 50f, listOf("K:GLFW_KEY_T"), msgs = listOf("hi")))
        val w = LayoutMapper.toFcl(TouchLayout(items), "n", screen).warnings.joinToString("\n")
        assertTrue(w.contains("ข้อความเดียว")); assertTrue(w.contains("สี/รูป")); assertTrue(w.contains("ปุ่มข้อความแบบรายการ")); assertTrue(w.contains("launcher.event.nope"))
    }

    /** a big real FCL layout (2 MB, 100 groups): local check only, the file is another person's work and is NOT in the repository */
    @Test fun bigRealUserFileIfPresent() {
        val f = File("E:/gh-dl/fclconv/fcl_real_user.json"); assumeTrue(f.exists())
        val m = LayoutMapper.fromFcl(f.readText(), screen)
        val items = m.value.second.items
        assertTrue(items.size > 800)
        assertTrue(items.count { it.longEvents.isNotEmpty() } >= 11)  // the 11 long presses stay long presses
        assertTrue(items.count { it.clickEvents.isNotEmpty() } > 10)  // click events stay clicks, not folded into the plain press
        val out = LayoutMapper.toFcl(m.value.second, "big", screen)
        assertTrue(LayoutMapper.isFcl(out.value))
        assertEquals(items.size, LayoutMapper.fromFcl(out.value, screen).value.second.items.size)
        println("BIG: items=${items.size} long=${items.count { it.longEvents.isNotEmpty() }} click=${items.count { it.clickEvents.isNotEmpty() }} hidden=${m.value.second.hiddenGroups.size}")
        m.warnings.forEach { println("BIGW: $it") }
    }
}
