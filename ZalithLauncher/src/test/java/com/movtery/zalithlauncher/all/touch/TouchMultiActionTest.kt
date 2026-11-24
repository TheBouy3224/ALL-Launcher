package com.movtery.zalithlauncher.all.touch

import com.movtery.layer_controller.event.ClickEvent
import org.junit.Assert.assertEquals
import org.junit.Test

/** Multi-Action: key + launcher event + text in one button's events */
class TouchMultiActionTest {
    @Test fun textEventIsSendText() = assertEquals(ClickEvent(ClickEvent.Type.SendText, "/home"), "T:/home".toClickEvent())
    @Test fun textKeepsColonsAndPrefixLikeChars() = assertEquals("K:x T:y", "T:K:x T:y".toClickEvent().key)
    @Test fun keyLauncherAndTextTogether() {
        val ev = listOf("K:GLFW_KEY_F3").withEvent("L:x").withEvent("T:hi").map { it.toClickEvent().type }
        assertEquals(listOf(ClickEvent.Type.Key, ClickEvent.Type.LauncherEvent, ClickEvent.Type.SendText), ev)
    }
    @Test fun sameTextTwiceIsOnce() = assertEquals(listOf("T:hi"), listOf("T:hi").withEvent("T:hi"))
}
