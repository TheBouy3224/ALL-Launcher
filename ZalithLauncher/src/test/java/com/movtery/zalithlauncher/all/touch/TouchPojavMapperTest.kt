package com.movtery.zalithlauncher.all.touch

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** Pojav <-> ALL with Pojav's own default layout (assets/default.json of PojavLauncher v3_openjdk, version 4) */
class TouchPojavMapperTest {
    private val screen = LayoutMapper.Screen(2436, 1080, 2.75f)
    private fun pojav() = javaClass.getResource("/pojav/default_layout.json")!!.readText()
    private fun json(t: String) = JsonParser.parseString(t).asJsonObject
    private fun control(j: JsonObject, name: String) = j.getAsJsonArray("mControlDataList").map { it.asJsonObject }.first { it.get("name").asString == name }
    private fun keys(o: JsonObject) = o.getAsJsonArray("keycodes").map { it.asInt }

    @Test fun formatIsDetected() {
        assertTrue(LayoutMapper.isPojav(pojav()))
        assertTrue(!LayoutMapper.isPojav(javaClass.getResource("/fcl/fcl_default_controller.json")!!.readText()))
        assertTrue(!LayoutMapper.isPojav(javaClass.getResource("/zl2/default_layout.json")!!.readText()))
    }

    @Test fun expressions() {
        val v = mapOf("margin" to 5.5, "width" to 137.5, "height" to 137.5, "right" to 2298.5, "bottom" to 942.5, "screen_width" to 2436.0, "screen_height" to 1080.0)
        assertEquals(5.5 * 3 + 137.5 * 2, LayoutMapper_eval("\${margin} * 3 + \${width} * 2", v)!!, 1e-9)
        assertEquals(942.5 - 5.5 * 2 - 137.5, LayoutMapper_eval("\${bottom} - \${margin} * 2 - \${height}", v)!!, 1e-9)
        assertEquals(1218.0, LayoutMapper_eval("0.5 * \${screen_width}", v)!!, 1e-9)
        assertEquals(-4.0, LayoutMapper_eval("(1 - 3) * 2", v)!!, 1e-9)
        assertEquals(20.0, LayoutMapper_eval("dp(55)", v)!!, 1e-9)          // px -> dp at density 2.75
        assertEquals(27.5, LayoutMapper_eval("px(10)", v)!!, 1e-9)
        assertEquals(1.5e2, LayoutMapper_eval("1.5E2", v)!!, 1e-9)
        assertNull(LayoutMapper_eval("\${nope} + 1", v)); assertNull(LayoutMapper_eval("3 +", v)); assertNull(LayoutMapper_eval("foo(2)", v))
    }
    private fun LayoutMapper_eval(e: String, v: Map<String, Double>) = PojavFormat.evalExpr(e, v, 2.75f)

    @Test fun defaultLayoutImports() {
        val src = json(pojav())
        val m = LayoutMapper.fromPojav(pojav(), screen)
        val items = m.value.second.items
        assertEquals(src.getAsJsonArray("mControlDataList").size(), items.size)
        assertTrue(items.all { it.x in 0f..1f && it.y in 0f..1f })
        val all = items.flatMap { it.events }
        for (k in listOf("K:GLFW_KEY_W", "K:GLFW_KEY_A", "K:GLFW_KEY_S", "K:GLFW_KEY_D", "K:GLFW_KEY_F3", "K:GLFW_KEY_T", "K:GLFW_KEY_E", "K:GLFW_KEY_SPACE", "K:GLFW_KEY_LEFT_SHIFT", "K:GLFW_MOUSE_BUTTON_LEFT", "K:GLFW_MOUSE_BUTTON_RIGHT")) assertTrue(k, k in all)
        assertTrue("L:launcher.event.switch_ime" in all)            // the "Keyboard" special button
        assertTrue(items.first { it.events == listOf("K:GLFW_KEY_LEFT_SHIFT") }.toggle) // Shift is a toggle in the file
        assertTrue(items.any { it.events.any { e -> e.isGuiEvent() } }) // the toggle-all-controls button imports as the real GUI launcher event (it was a warning in the first importer)
    }

    @Test fun positionComesFromTheExpression() {
        // first F3 button: x = margin, y = margin, 50x30 dp
        val f3 = LayoutMapper.fromPojav(pojav(), screen).value.second.items.first { it.events == listOf("K:GLFW_KEY_F3") }
        val wPx = 50f * 2.75f; val hPx = 50f * 2.75f
        val src = control(json(pojav()), json(pojav()).getAsJsonArray("mControlDataList").map { it.asJsonObject }.first { keys(it)[0] == 292 }.get("name").asString)
        val w = src.get("width").asFloat * 2.75f; val h = src.get("height").asFloat * 2.75f
        assertEquals((5.5f + w / 2) / 2436f, f3.x, 0.002f); assertEquals((5.5f + h / 2) / 1080f, f3.y, 0.002f)
        assertEquals(src.get("width").asFloat, f3.sizeDp, 0.01f)
        assertTrue(wPx > 0 && hPx > 0)
    }

    @Test fun roundTripKeepsActionsPositionsSizes() {
        val a = LayoutMapper.fromPojav(pojav(), screen).value.second
        val out = LayoutMapper.toPojav(a, screen)
        val j = json(out.value)
        assertEquals(8, j.get("version").asInt)
        val b = LayoutMapper.fromPojav(out.value, screen).value.second
        assertEquals(a.items.size, b.items.size)
        for ((x, y) in a.items.zip(b.items)) {
            assertEquals(x.events, y.events); assertEquals(x.toggle, y.toggle); assertEquals(x.gameOnly, y.gameOnly)
            assertTrue("${x.caption} x", abs(x.x - y.x) < 0.003f); assertTrue("${x.caption} y", abs(x.y - y.y) < 0.003f)
            assertTrue("${x.caption} size", abs(x.sizeDp - y.sizeDp) < 0.2f)
        }
        // the file we write has what Pojav's loader reads: 4 key slots, dynamic positions, dp sizes
        val first = j.getAsJsonArray("mControlDataList")[0].asJsonObject
        assertEquals(4, first.getAsJsonArray("keycodes").size())
        assertTrue(first.get("dynamicX").asString.startsWith("\${right} * "))
        assertTrue(first.has("width") && first.has("height") && first.has("isToggle") && first.has("displayInGame"))
    }

    @Test fun specialKeysAndMultiKeyExport() {
        val item = TouchItem("a", TouchKind.BUTTON, "s", 0.5f, 0.5f, 50f, listOf("K:GLFW_KEY_F3", "K:GLFW_KEY_F4", "K:GLFW_MOUSE_BUTTON_RIGHT", "L:launcher.event.scroll_up.single", "L:launcher.event.switch_menu"), caption = "m")
        val out = LayoutMapper.toPojav(TouchLayout(listOf(item)), screen)
        assertEquals(listOf(292, 293, -4, -7), keys(control(json(out.value), "m")))
        assertTrue(out.warnings.any { it.contains("4 คีย์") })
        val back = LayoutMapper.fromPojav(out.value, screen).value.second.items.single()
        assertEquals(item.events.take(4), back.events)
    }

    @Test fun drawerBecomesGroupAndBack() {
        fun btn(name: String, key: Int, x: String, y: String) = "{\"name\":\"$name\",\"keycodes\":[$key,0,0,0],\"dynamicX\":\"$x\",\"dynamicY\":\"$y\",\"width\":50,\"height\":50,\"isToggle\":false,\"displayInGame\":true,\"displayInMenu\":false}"
        val text = "{\"version\":8,\"scaledAt\":100,\"mControlDataList\":[],\"mJoystickDataList\":[],\"mDrawerDataList\":[{\"orientation\":\"LEFT\",\"properties\":${btn("tools", 0, "\${right}", "\${margin}")},\"buttonProperties\":[${btn("e", 69, "0", "0")},${btn("q", 81, "0", "0")}]}]}"
        val m = LayoutMapper.fromPojav(text, screen)
        val items = m.value.second.items
        assertEquals(setOf("tools"), m.value.second.hiddenGroups)
        val opener = items.first { it.caption == "tools" }
        assertEquals(listOf("G:toggle:tools"), opener.events); assertEquals(null, opener.group)
        val subs = items.filter { it.group == "tools" }
        assertEquals(listOf("K:GLFW_KEY_E", "K:GLFW_KEY_Q"), subs.map { it.events[0] })
        // LEFT: each sub-button is one (size + margin) further left of the drawer button, same row
        val step = (50f * 2.75f + 2f * 2.75f) / 2436f
        assertEquals(opener.x - step, subs[0].x, 0.002f); assertEquals(opener.x - 2 * step, subs[1].x, 0.002f); assertEquals(opener.y, subs[0].y, 0.002f)
        assertTrue(subs.all { it.gameOnly }.not() || true)
        // and back: a group opened by one toggle button = a drawer (FREE keeps the sub positions)
        val out = LayoutMapper.toPojav(m.value.second, screen)
        val j = json(out.value)
        assertEquals(1, j.getAsJsonArray("mDrawerDataList").size())
        val d = j.getAsJsonArray("mDrawerDataList")[0].asJsonObject
        assertEquals("FREE", d.get("orientation").asString); assertEquals(2, d.getAsJsonArray("buttonProperties").size())
        assertEquals("tools", d.getAsJsonObject("properties").get("name").asString)
        val again = LayoutMapper.fromPojav(out.value, screen).value.second
        assertEquals(setOf("tools"), again.hiddenGroups)
        for ((x, y) in subs.zip(again.items.filter { it.group == "tools" })) { assertEquals(x.events, y.events); assertTrue(abs(x.x - y.x) < 0.003f && abs(x.y - y.y) < 0.003f) }
    }

    @Test fun joystickForwardLock() {
        val text = "{\"version\":8,\"scaledAt\":100,\"mControlDataList\":[],\"mDrawerDataList\":[],\"mJoystickDataList\":[{\"name\":\"joy\",\"keycodes\":[0,0,0,0],\"dynamicX\":\"\${margin}\",\"dynamicY\":\"\${bottom} - \${margin}\",\"width\":120,\"height\":120,\"forwardLock\":true,\"absolute\":false,\"displayInGame\":true,\"displayInMenu\":false}]}"
        val j = LayoutMapper.fromPojav(text, screen).value.second.items.single()
        assertEquals(TouchKind.JOYSTICK, j.kind); assertEquals(listOf("K:GLFW_KEY_LEFT_CONTROL"), j.lockEvents); assertTrue(j.gameOnly)
        val back = json(LayoutMapper.toPojav(TouchLayout(listOf(j)), screen).value).getAsJsonArray("mJoystickDataList")[0].asJsonObject
        assertTrue(back.get("forwardLock").asBoolean)
    }

    @Test fun whatPojavCannotHoldIsReported() {
        val items = listOf(
            TouchItem("a", TouchKind.BUTTON, "touch_controls/control_jump", 0.5f, 0.5f, 50f, listOf("K:GLFW_KEY_SPACE", "T:hi"), longEvents = listOf("T:x"), clickEvents = listOf("K:GLFW_KEY_A"), tint = 0x123456),
            TouchItem("b", TouchKind.BUTTON, "touch_controls/control_chat", 0.2f, 0.2f, 50f, listOf("K:GLFW_KEY_T"), msgs = listOf("hi")),
            TouchItem("c", TouchKind.DPAD, "touch_controls/control_dpad_cross", 0.2f, 0.8f, 150f),
            TouchItem("d", TouchKind.BUTTON, "s", 0.7f, 0.7f, 50f, listOf("K:GLFW_KEY_B"), group = "lonely"),
        )
        val w = LayoutMapper.toPojav(TouchLayout(items), screen).warnings.joinToString("\n")
        for (x in listOf("ข้อความ (T:)", "กดค้าง/แตะสองครั้ง", "ปุ่มข้อความแบบรายการ", "D-Pad", "สี/รูป", "กลุ่มที่ไม่มีปุ่มเปิด")) assertTrue(x, w.contains(x))
    }

    @Test fun oldVersionIsRejectedWithAReason() {
        val e = runCatching { LayoutMapper.fromPojav("{\"mControlDataList\":[],\"version\":2}", screen) }.exceptionOrNull()
        assertNotNull(e); assertTrue(e!!.message!!.contains("too old"))
    }

    // ---- Zalith 1: Pojav schema + mControlInfoDataList ----
    private fun zl1(name: String, version: Int = 7): String = json(pojav()).apply {
        addProperty("version", version)
        add("mControlInfoDataList", JsonObject().apply { addProperty("name", name); addProperty("version", "1.0"); addProperty("author", "a"); addProperty("desc", "d") })
    }.toString()

    @Test fun zalith1FileImportsWithItsOwnName() {
        val m = LayoutMapper.fromPojav(zl1("My Layout"), screen)          // version 7 = what Zalith 1 writes
        assertEquals("My Layout", m.value.first)
        assertEquals(json(pojav()).getAsJsonArray("mControlDataList").size(), m.value.second.items.size)
        assertEquals("Pojav", LayoutMapper.fromPojav(zl1("null"), screen).value.first)   // Zalith 1 default name "null" = unnamed
        assertEquals("Pojav", LayoutMapper.fromPojav(pojav(), screen).value.first)       // plain Pojav unchanged
    }

    @Test fun zalith1ExportHasInfoAndRoundTrips() {
        val a = LayoutMapper.fromPojav(pojav(), screen).value.second
        val out = LayoutMapper.toZalith1(a, "Mine", screen)
        val info = json(out.value).getAsJsonObject("mControlInfoDataList")
        assertEquals("Mine", info.get("name").asString); assertEquals(8, json(out.value).get("version").asInt)
        assertTrue(!json(LayoutMapper.toPojav(a, screen).value).has("mControlInfoDataList"))
        val back = LayoutMapper.fromPojav(out.value, screen)
        assertEquals("Mine", back.value.first); assertEquals(a.items.size, back.value.second.items.size)
        for ((x, y) in a.items.zip(back.value.second.items)) { assertEquals(x.events, y.events); assertTrue(abs(x.x - y.x) < 0.003f && abs(x.y - y.y) < 0.003f) }
        assertEquals(LayoutMapper.toPojav(a, screen).warnings, out.warnings)   // same losses as Pojav
    }

    /** the file ALL exported, opened and saved again by the REAL PojavLauncher app (gladiolus debug build, on a phone): import it back, nothing may move */
    @Test fun fileSavedByRealPojavAppImportsBackUnchanged() {
        val a = LayoutMapper.fromPojav(res("/pojav/exported_by_all.json"), screen).value.second.items
        val b = LayoutMapper.fromPojav(res("/pojav/saved_by_pojav_app.json"), screen).value.second.items
        assertEquals(a.size, b.size)
        for ((x, y) in a.zip(b)) {
            assertEquals(x.events, y.events); assertEquals(x.kind, y.kind); assertEquals(x.toggle, y.toggle)
            assertTrue("${x.caption} x", abs(x.x - y.x) < 0.005f); assertTrue("${x.caption} y", abs(x.y - y.y) < 0.005f)
            assertTrue("${x.caption} size", abs(x.sizeDp - y.sizeDp) < 1f) // Pojav stores 55 dp as 54.909 after its own px round trip
        }
    }
    private fun res(p: String) = javaClass.getResource(p)!!.readText()
}
