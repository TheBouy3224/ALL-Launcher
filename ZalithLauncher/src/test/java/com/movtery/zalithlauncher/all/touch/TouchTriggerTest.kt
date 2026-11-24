package com.movtery.zalithlauncher.all.touch

import org.junit.Assert.assertEquals
import org.junit.Test

/** Long press / double tap: extra event lists of one button, same strings as [TouchItem.events] */
class TouchTriggerTest {
    private val b = TouchItem("a", TouchKind.BUTTON, "s", 0.5f, 0.5f, 46f, listOf("K:GLFW_KEY_F3"))

    @Test fun listsStartOff() { assertEquals(emptyList<String>(), b.longEvents); assertEquals(emptyList<String>(), b.doubleEvents) }
    @Test fun setLong() = assertEquals(listOf("K:GLFW_KEY_F5"), b.withEvents(1, listOf("K:GLFW_KEY_F5")).longEvents)
    @Test fun setDouble() = assertEquals(listOf("T:hi"), b.withEvents(2, listOf("T:hi")).doubleEvents)
    @Test fun pressUntouched() = assertEquals(b.events, b.withEvents(1, listOf("T:x")).withEvents(2, listOf("T:y")).eventsOf(0))
    @Test fun eventsOfPicksList() = assertEquals(listOf("L:q"), b.withEvents(1, listOf("L:q")).eventsOf(1))
    @Test fun thresholds() { assert(LONG_PRESS_MS > DOUBLE_TAP_MS) }
}
