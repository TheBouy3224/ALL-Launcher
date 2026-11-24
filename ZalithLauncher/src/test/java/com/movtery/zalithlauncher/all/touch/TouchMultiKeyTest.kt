package com.movtery.zalithlauncher.all.touch

import com.movtery.layer_controller.event.ClickEvent
import org.junit.Assert.assertEquals
import org.junit.Test

/** Multi-Key: one button, several events pressed together (the layer already presses every entry of [TouchItem.events]) */
class TouchMultiKeyTest {
    private val f3 = "K:GLFW_KEY_F3"
    private val f4 = "K:GLFW_KEY_F4"

    @Test fun addKeepsOrder() = assertEquals(listOf(f3, f4), listOf(f3).withEvent(f4))
    @Test fun addTwiceIsOnce() = assertEquals(listOf(f3, f4), listOf(f3, f4).withEvent(f4))
    @Test fun addToEmptyButton() = assertEquals(listOf(f3), emptyList<String>().withEvent(f3))
    @Test fun removeOne() = assertEquals(listOf(f3), listOf(f3, f4).withoutEvent(f4))
    @Test fun lastKeyStays() = assertEquals(listOf(f3), listOf(f3).withoutEvent(f3))
    @Test fun removeUnknownChangesNothing() = assertEquals(listOf(f3, f4), listOf(f3, f4).withoutEvent("K:GLFW_KEY_A"))
    @Test fun mixedKeyAndLauncherEvent() = assertEquals(listOf(f3, "L:x"), listOf(f3).withEvent("L:x"))

    @Test fun f3PlusF4BecomeTwoKeyEvents() {
        val events = emptyList<String>().withEvent(f3).withEvent(f4).map { it.toClickEvent() }
        assertEquals(listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_F3"), ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_F4")), events)
    }
}
