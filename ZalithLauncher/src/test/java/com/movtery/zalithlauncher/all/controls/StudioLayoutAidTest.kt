package com.movtery.zalithlauncher.all.controls

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.movtery.layer_controller.data.ButtonPosition
import com.movtery.layer_controller.layout.EmptyControlLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioLayoutAidTest {
    private val sw = 2436f
    private val sh = 1080f
    private val d = 2.6f

    @Test fun slotButtonsPressTheNumberKeysNotScroll() {
        val slots = StudioAction.entries.filter { it.isSlot }
        assertEquals(9, slots.size)
        slots.forEachIndexed { i, a -> assertEquals("GLFW_KEY_${i + 1}", a.events.single().key) }
        assertEquals("GLFW_KEY_TAB", StudioAction.PLAYER_LIST.events.single().key)
        assertEquals("GLFW_KEY_F3", StudioAction.DEBUG_INFO.events.single().key)
        assertEquals("GLFW_KEY_F11", StudioAction.FULLSCREEN.events.single().key)
        assertEquals("ปุ่ม 7", StudioAction.SLOT_7.inputHint(true))
    }

    @Test fun slotLabelsStayOnOneLineAndKeepTheirKey() {
        val b = newButton(StudioAction.SLOT_1)
        assertFalse("no breakable space between word and number", b.text.default.contains(' '))
        assertEquals("ช่อง 1", b.text.default)
        assertTrue("wider than tall so the label fits", b.buttonSize.widthPercentage > b.buttonSize.heightPercentage)
        assertEquals("GLFW_KEY_1", b.clickEvents.single().key)
        assertEquals(StudioAction.SLOT_1, actionOf(b))
        //other buttons stay square
        val j = newButton(StudioAction.JUMP)
        assertEquals(j.buttonSize.widthPercentage, j.buttonSize.heightPercentage)
    }

    @Test fun hotbarZoneMatchesTheGameOnTecno() {
        //measured in Minecraft 1.20.1 on the TECNO (2436x1080, GUI scale auto = 4): hotbar x 854..1580, bottom of screen
        val z = hotbarZone(sw, sh)
        assertEquals(854f, z.left, 6f); assertEquals(1580f, z.right, 6f); assertEquals(sh, z.bottom, 0.1f)
    }

    @Test fun newButtonsLandOnFreeSpaceAwayFromHudAndEachOther() {
        var l = EmptyControlLayout
        val px = sh * StudioAction.JUMP.sizePct / 10000f
        val ids = (1..6).map {
            val spot = l.spotForNew(px, px, sw, sh, d)!!
            val (n, id) = l.addAction(StudioAction.JUMP, at = percentPosition(spot.x, spot.y, px, px, sw, sh)); l = n; id
        }
        val rects = l.widgetRects(sw, sh, d)
        assertTrue("no overlaps", overlapping(rects, listOf(hotbarZone(sw, sh), crosshairZone(sw, sh), toolbarZone(sw, sh))).isEmpty())
        assertEquals(6, ids.toSet().size)
    }

    @Test fun noFreeSpaceGivesNull() {
        assertNull(findFreeSpot(100f, 100f, 1000f, 500f, listOf(Rect(0f, 0f, 1000f, 500f)), 0f))
        assertNull(findFreeSpot(2000f, 100f, 1000f, 500f, emptyList(), 0f))
    }

    @Test fun copyKeepsEverythingAndMovesAside() {
        val (l0, id) = EmptyControlLayout.addAction(StudioAction.SNEAK, at = ButtonPosition(5000, 5000))
        val l1 = l0.setButtonBehavior(id, hold = true, passThrough = true, draggable = false)
            .setButtonColor(id, androidx.compose.ui.graphics.Color(0xCC2E7D32)).setButtonOpacity(id, 0.6f)
            .setButtonEvents(id, StudioAction.SNEAK.events + StudioAction.SLOT_2.events).setButtonName(id, "My sneak")
        val src = l1.findButton(id)!!
        val r = l1.widgetRects(sw, sh, d)[id]!!
        val spot = l1.spotForNew(r.width, r.height, sw, sh, d, Offset(r.right + 20f, r.top))!!
        val (l2, nid) = l1.duplicateButton(id, percentPosition(spot.x, spot.y, r.width, r.height, sw, sh))!!
        val c = l2.findButton(nid)!!
        assertNotEquals(id, nid)
        assertEquals(src.copy(uuid = nid, position = c.position), c) //same name, keys, size, style, switches
        assertFalse(l2.widgetRects(sw, sh, d)[nid]!!.overlaps(r))
        assertEquals(src, l2.findButton(id)) //original untouched
    }

    @Test fun snapAlignsEdgesAndCentres() {
        val other = Rect(100f, 100f, 200f, 200f)
        val s = snapRect(Rect(Offset(105f, 400f), Size(100f, 100f)), listOf(other), sw, sh, 8f)
        assertEquals(Offset(-5f, 0f), s.nudge); assertEquals(100f, s.xLine); assertNull(s.yLine)
        val far = snapRect(Rect(Offset(500f, 400f), Size(50f, 50f)), listOf(other), sw, sh, 8f)
        assertEquals(Offset.Zero, far.nudge)
        //screen centre counts too
        assertNotNull(snapRect(Rect(Offset(sw / 2 - 52f, 10f), Size(100f, 50f)), emptyList(), sw, sh, 8f).xLine)
    }

    @Test fun overlapWarnings() {
        val rects = mapOf("a" to Rect(0f, 0f, 100f, 100f), "b" to Rect(50f, 50f, 150f, 150f), "c" to Rect(500f, 0f, 600f, 100f), "d" to Rect(0f, 900f, 2436f, 1080f))
        assertEquals(setOf("a", "b", "d"), overlapping(rects, listOf(hotbarZone(sw, sh))))
    }
}
