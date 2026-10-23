package com.movtery.zalithlauncher.all.controls

import com.movtery.layer_controller.data.ButtonPosition
import com.movtery.layer_controller.layout.EmptyControlLayout
import com.movtery.layer_controller.utils.widgetPosition
import com.movtery.layer_controller.utils.saveToFile
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioModelTest {
    @Test fun addingCreatesALayerAndAButtonWithTheRightEvents() {
        val (l, id) = EmptyControlLayout.addAction(StudioAction.JUMP)
        assertEquals(1, l.layers.size)
        val b = l.findButton(id)!!
        assertEquals(StudioAction.JUMP, actionOf(b))
        assertEquals("กระโดด", b.text.default)
        val (l2, jid) = l.addAction(StudioAction.MOVE)
        assertNotNull(l2.findJoystick(jid))
    }

    @Test fun positionRoundTripsWithTheGamesFormula() {
        //drag a 100x100 px button to (300, 200) on a 1000x500 screen, then let the game place it
        val pos = percentPosition(300f, 200f, 100f, 100f, 1000f, 500f)
        val back = widgetPosition(pos.xPercentage(), pos.yPercentage(), IntSize(100, 100), IntSize(1000, 500))
        assertEquals(300f, back.x, 1f)
        assertEquals(200f, back.y, 1f)
        assertEquals(ButtonPosition(10000, 0), percentPosition(5000f, -40f, 100f, 100f, 1000f, 500f))
    }

    @Test fun resizeStoresPercentOfScreenHeightLikeTheGameReadsIt() {
        val (l, id) = EmptyControlLayout.addAction(StudioAction.ATTACK)
        val r = l.resizeButton(id, 150f, 100f, 500f)
        val (w, h) = buttonPixelSize(r.findButton(id)!!.buttonSize, 1000f, 500f, 2f)
        assertEquals(150f, w, 1f)
        assertEquals(100f, h, 1f)
    }

    @Test fun renamingActionAndOpacity() {
        val (l, id) = EmptyControlLayout.addAction(StudioAction.SNEAK)
        val a = l.setButtonAction(id, StudioAction.SPRINT)
        assertEquals("วิ่ง", a.findButton(id)!!.text.default) //default name follows the action
        val named = a.setButtonName(id, "My run").setButtonAction(id, StudioAction.JUMP)
        assertEquals("My run", named.findButton(id)!!.text.default) //a user name is kept
        val o = named.setButtonOpacity(id, 0.7f)
        assertEquals(0.7f, o.opacityOf(o.findButton(id)!!), 0.001f)
        val o2 = o.setButtonOpacity(id, 0.7f)
        assertEquals(o.styles.size, o2.styles.size) //same opacity reuses the style
    }

    @Test fun removeAndUndoAndDirty() {
        val (l, id) = EmptyControlLayout.addAction(StudioAction.USE)
        val h = StudioHistory(l)
        assertFalse(h.isDirty())
        h.push(h.current.moveWidget(id, ButtonPosition(100, 200)))
        h.push(h.current.removeWidget(id))
        assertNull(h.current.findButton(id))
        assertTrue(h.isDirty())
        h.undo()
        assertEquals(ButtonPosition(100, 200), h.current.findButton(id)!!.position)
        h.undo()
        assertFalse(h.isDirty())
        h.push(h.current.removeWidget(id)); h.markSaved()
        assertFalse(h.isDirty())
    }

    @Test fun presetsAreValidAndUseOnlySupportedActions() {
        StudioPreset.entries.forEach { p ->
            val l = presetLayout(p, "x")
            l.layers.flatMap { it.normalButtons }.forEach { assertNotNull("${p}: ${it.text.default}", actionOf(it)) }
        }
        assertTrue(presetLayout(StudioPreset.STANDARD, "x").layers.single().joystickButtons.isNotEmpty())
        assertTrue(presetLayout(StudioPreset.BLANK, "x").layers.single().normalButtons.isEmpty())
    }

    @Test fun presetButtonsStayOutOfTheToolbarStrip() {
        //TECNO landscape; the Studio toolbar takes the top-centre strip (about 19%..81% wide, 15% high at UI scale 75-100%)
        val sw = 2436f; val sh = 1080f
        StudioPreset.entries.forEach { p ->
            presetLayout(p, "x").layers.single().normalButtons.forEach { b ->
                val (w, h) = buttonPixelSize(b.buttonSize, sw, sh, 2.6f)
                val pos = widgetPosition(b.position.xPercentage(), b.position.yPercentage(), IntSize(w.toInt(), h.toInt()), IntSize(sw.toInt(), sh.toInt()))
                val overlaps = pos.x < sw * 0.85f && pos.x + w > sw * 0.15f && pos.y < sh * 0.18f
                assertFalse("$p: ${b.text.default} under the toolbar", overlaps)
            }
        }
    }

    @Test fun legacyOptionsSaveToTheOldFieldsAndKeepEverythingElse() = kotlinx.coroutines.runBlocking {
        val (l0, id) = EmptyControlLayout.addAction(StudioAction.SNEAK)
        val l = l0.setButtonBehavior(id, hold = true, passThrough = true, draggable = false)
            .setButtonColor(id, androidx.compose.ui.graphics.Color(0xCC2E7D32))
            .setButtonEvents(id, StudioAction.SNEAK.events + StudioAction.JUMP.events)
            .setButtonOpacity(id, 0.6f)
        val f = java.io.File(java.nio.file.Files.createTempDirectory("st").toFile(), "s.json")
        l.saveToFile(f)
        val back = com.movtery.layer_controller.layout.loadLayoutFromFile(f)
        assertEquals(l, back)
        val b = back.findButton(id)!!
        assertTrue(b.isToggleable); assertTrue(b.isPenetrable); assertFalse(b.isSwipple)
        assertEquals(listOf("GLFW_KEY_LEFT_SHIFT", "GLFW_KEY_SPACE"), b.clickEvents.map { it.key })
        assertEquals(androidx.compose.ui.graphics.Color(0xCC2E7D32), back.colorOf(b))
        assertEquals(0.6f, back.opacityOf(b), 0.001f) //colour change kept the opacity and vice versa
        //backend drops buttons that are both pass-through and swipeable: never written together
        assertFalse(l.setButtonBehavior(id, false, true, true).findButton(id)!!.isSwipple)
        //a button can never lose its last key
        assertEquals(2, l.setButtonEvents(id, emptyList()).findButton(id)!!.clickEvents.size)
    }

    @Test fun tryModeShowsPlainWordsNotKeycodes() {
        StudioAction.entries.forEach { assertFalse(it.inputHint(true).contains("GLFW")) }
        assertEquals("คลิกซ้าย", StudioAction.ATTACK.inputHint(true))
    }
}
