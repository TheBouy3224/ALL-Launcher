package com.movtery.zalithlauncher.all.controls

import com.movtery.layer_controller.data.ButtonPosition
import com.movtery.layer_controller.event.ClickEvent
import com.movtery.layer_controller.layout.EmptyControlLayout
import com.movtery.layer_controller.layout.loadLayoutFromFile
import com.movtery.layer_controller.utils.saveToFile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class StudioCustomKeysTest {
    private fun key(c: String) = ClickEvent(ClickEvent.Type.Key, c)

    @Test fun everyOfferedKeyIsOneTheBackendCanSend() {
        CustomKeys.forEach { assertTrue("${it.label} ${it.event.key}", isBackendKey(it.event)) }
        val labels = CustomKeys.map { it.label }
        listOf("A", "Z", "0", "9", "F1", "F12", "Esc", "Tab", "Space", "Enter", "Backspace", "Shift", "Ctrl", "Alt",
            "↑ Up", "Mouse Left", "Mouse Right", "Mouse Middle", "Wheel Up", "Wheel Down").forEach { assertTrue(it, it in labels) }
    }

    @Test fun searchFindsKeysQuickly() {
        assertEquals("Z", searchKeys("z").first().label)
        assertEquals("F3", searchKeys("f3").first().label)
        assertTrue(searchKeys("ล้อ").any { it.label == "Wheel Up" })
        assertTrue(searchKeys("คลิกซ้าย").any { it.label == "Mouse Left" })
        assertTrue(searchKeys("xyz123").isEmpty())
    }

    @Test fun comboPutsModifiersFirstSoTheyAreDownBeforeTheMainKey() {
        assertEquals(listOf(key("GLFW_KEY_LEFT_CONTROL"), key("GLFW_KEY_Z")), orderCombo(listOf(key("GLFW_KEY_Z"), key("GLFW_KEY_LEFT_CONTROL"))))
        assertEquals("Ctrl + Z", comboLabel(orderCombo(listOf(key("GLFW_KEY_Z"), key("GLFW_KEY_LEFT_CONTROL")))))
    }

    @Test fun customButtonSavesInTheNormalFileAndCanBeRebound() = runBlocking {
        val (l, id) = EmptyControlLayout.addCustom("สกิล Z", listOf(key("GLFW_KEY_Z"), key("GLFW_KEY_LEFT_CONTROL")), ButtonPosition(5000, 5000))
        val b = l.findButton(id)!!
        assertEquals("สกิล Z", b.text.default)
        assertEquals(listOf("GLFW_KEY_LEFT_CONTROL", "GLFW_KEY_Z"), b.clickEvents.map { it.key })
        val f = File(Files.createTempDirectory("ck").toFile(), "c.json")
        l.saveToFile(f)
        val back = loadLayoutFromFile(f)
        assertEquals(l, back)
        //rebinding keeps the user's name and the order rule
        val r = back.setButtonEvents(id, listOf(key("GLFW_KEY_R"), key("GLFW_KEY_LEFT_SHIFT")))
        assertEquals(listOf("GLFW_KEY_LEFT_SHIFT", "GLFW_KEY_R"), r.findButton(id)!!.clickEvents.map { it.key })
        assertEquals("สกิล Z", r.findButton(id)!!.text.default)
        //no name -> the combo is the label; copy keeps the binding
        val (l2, id2) = EmptyControlLayout.addCustom("", listOf(key("GLFW_KEY_F3")), null)
        assertEquals("F3", l2.findButton(id2)!!.text.default)
        val (l3, cid) = l2.duplicateButton(id2, ButtonPosition(0, 0))!!
        assertEquals(l3.findButton(id2)!!.clickEvents, l3.findButton(cid)!!.clickEvents)
    }
}
