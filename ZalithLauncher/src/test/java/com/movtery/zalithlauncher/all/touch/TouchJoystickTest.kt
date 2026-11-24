package com.movtery.zalithlauncher.all.touch

import org.junit.Assert.assertEquals
import org.junit.Test

/** Advanced Joystick: keys per direction (default W S A D) and the walk-lock events */
class TouchJoystickTest {
    private val j = TouchItem("j", TouchKind.JOYSTICK, "joystick/joystick_circle", 0.2f, 0.7f, 140f)

    @Test fun defaultsAreWasd() {
        assertEquals(listOf("K:GLFW_KEY_W"), j.dirList("up")); assertEquals(listOf("K:GLFW_KEY_S"), j.dirList("down"))
        assertEquals(listOf("K:GLFW_KEY_A"), j.dirList("left")); assertEquals(listOf("K:GLFW_KEY_D"), j.dirList("right"))
    }
    @Test fun noLockByDefault() = assertEquals(emptyList<String>(), j.lockEvents)
    @Test fun rebindOneDirection() {
        val r = j.withEvents(3, listOf("K:GLFW_KEY_UP"))
        assertEquals(listOf("K:GLFW_KEY_UP"), r.eventsOf(3)); assertEquals(listOf("K:GLFW_KEY_S"), r.eventsOf(4))
    }
    @Test fun directionMayBeEmpty() = assertEquals(emptyList<String>(), j.withEvents(5, emptyList()).dirList("left"))
    @Test fun lockList() = assertEquals(listOf("K:GLFW_KEY_LEFT_CONTROL"), j.withEvents(7, listOf("K:GLFW_KEY_LEFT_CONTROL")).eventsOf(7))
    @Test fun pressListUntouched() = assertEquals(j.events, j.withEvents(3, listOf("T:x")).events)
}
