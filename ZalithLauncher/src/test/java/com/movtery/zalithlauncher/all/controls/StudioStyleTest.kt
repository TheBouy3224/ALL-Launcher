package com.movtery.zalithlauncher.all.controls

import androidx.compose.ui.graphics.Color
import com.movtery.layer_controller.data.ButtonSize
import com.movtery.layer_controller.event.ClickEvent
import com.movtery.layer_controller.layout.EmptyControlLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioStyleTest {
    @Test fun sendTextIsValidatedAndTrimmed() {
        assertEquals("ฟีฟาย", validSendText("  ฟีฟาย  "))
        assertEquals("/help", validSendText("/help"))
        assertNull(validSendText("   "))
        assertNull(validSendText("line\nbreak"))
        assertNull(validSendText("a".repeat(SEND_TEXT_MAX + 1)))
        assertEquals("a".repeat(SEND_TEXT_MAX), validSendText("a".repeat(SEND_TEXT_MAX)))
    }

    @Test fun sendTextButtonHasOneSendEventAndItsOwnName() {
        val (l, id) = EmptyControlLayout.addSendText("ฟีฟาย", "FF", null)
        val b = l.findButton(id)!!
        assertEquals(listOf(ClickEvent(ClickEvent.Type.SendText, "ฟีฟาย")), b.clickEvents)
        assertEquals("FF", b.text.default)
        assertEquals("ฟีฟาย", sendTextOf(b))
        assertNull(actionOf(b)) //not a simple action: the name stays as typed
    }

    @Test fun sendTextNameDefaultsToTheMessageAndSurvivesEdits() {
        val (l, id) = EmptyControlLayout.addSendText("/help", "", null)
        assertEquals("/help", l.findButton(id)!!.text.default)
        val e = l.setButtonSendText(id, "/time set day", null)
        assertEquals("/time set day", sendTextOf(e.findButton(id)!!))
        assertEquals("/help", e.findButton(id)!!.text.default) //name kept when none is given
        val named = e.setButtonSendText(id, "ฟีฟาย", "FF")
        assertEquals("FF", named.findButton(id)!!.text.default)
    }

    @Test fun invalidSendTextIsRefusedAndLeavesTheButtonAlone() {
        val (l, id) = EmptyControlLayout.addSendText("hi", "", null)
        assertEquals(l, l.setButtonSendText(id, "   ", null))
    }

    @Test fun widthAndHeightAreIndependentAndClamped() {
        val (l, id) = EmptyControlLayout.addAction(StudioAction.ATTACK)
        val s = l.setButtonSize(id, widthPct = 2500, heightPct = 900).findButton(id)!!.buttonSize
        assertEquals(2500, s.widthPercentage)
        assertEquals(900, s.heightPercentage)
        assertEquals(ButtonSize.Reference.ScreenHeight, s.widthReference)
        val c = l.setButtonSize(id, widthPct = 99999, heightPct = 1).findButton(id)!!.buttonSize
        assertEquals(SIZE_PCT_MAX, c.widthPercentage)
        assertEquals(SIZE_PCT_MIN, c.heightPercentage)
    }

    @Test fun lookIsPerStateAndKeepsTheOtherState() {
        val (l, id) = EmptyControlLayout.addAction(StudioAction.JUMP)
        val look = ButtonLook(background = Color.Red, content = Color.Yellow, fontSize = 20, borderColor = Color.Blue, borderWidth = 3, radius = 12)
        val n = l.setButtonLook(id, pressed = false, look = look)
        assertEquals(look, n.lookOf(n.findButton(id)!!, pressed = false))
        //the pressed state is untouched
        assertEquals(l.lookOf(l.findButton(id)!!, pressed = true), n.lookOf(n.findButton(id)!!, pressed = true))
        val p = n.setButtonLook(id, pressed = true, look = look.copy(background = Color.Green, borderWidth = 0))
        assertEquals(Color.Green, p.lookOf(p.findButton(id)!!, pressed = true).background)
        assertEquals(look, p.lookOf(p.findButton(id)!!, pressed = false)) //normal state unchanged by the pressed edit
    }

    @Test fun lookValuesAreClampedToTheEditorRanges() {
        val (l, id) = EmptyControlLayout.addAction(StudioAction.JUMP)
        val n = l.setButtonLook(id, false, ButtonLook(Color.Black, Color.White, fontSize = 500, borderColor = Color.White, borderWidth = -4, radius = 999))
        val r = n.lookOf(n.findButton(id)!!, false)
        assertEquals(FONT_MAX, r.fontSize)
        assertEquals(BORDER_MIN, r.borderWidth)
        assertEquals(RADIUS_MAX, r.radius)
    }

    @Test fun styleIsSharedWhenTheLookIsTheSame() {
        val (l, a) = EmptyControlLayout.addAction(StudioAction.JUMP)
        val (l2, b) = l.addAction(StudioAction.USE)
        val look = ButtonLook(Color.Red, Color.White, 14, Color.White, 2, 6)
        val both = l2.setButtonLook(a, false, look).setButtonLook(b, false, look)
        assertEquals(both.findButton(a)!!.buttonStyle, both.findButton(b)!!.buttonStyle)
    }

    @Test fun redoRestoresWhatUndoRemoved() {
        val (l, id) = EmptyControlLayout.addAction(StudioAction.USE)
        val h = StudioHistory(l)
        h.push(h.current.setButtonSize(id, 2000, 1400))
        assertFalse(h.canRedo())
        h.undo()
        assertTrue(h.canRedo())
        h.redo()
        assertEquals(2000, h.current.findButton(id)!!.buttonSize.widthPercentage)
        assertFalse(h.canRedo())
        //a new edit after an undo drops the redo branch
        h.undo()
        h.push(h.current.setButtonSize(id, 1600, 1400))
        assertFalse(h.canRedo())
    }

    @Test fun oldLayoutWithoutLookFieldsStillReadsTheGameDefaults() {
        val (l, id) = EmptyControlLayout.addAction(StudioAction.ESC)
        val r = l.lookOf(l.findButton(id)!!, pressed = false)
        assertEquals(DEFAULT_TEXT_SIZE, r.fontSize)
        assertEquals(0, r.borderWidth)
        assertEquals(0, r.radius)
    }
}
