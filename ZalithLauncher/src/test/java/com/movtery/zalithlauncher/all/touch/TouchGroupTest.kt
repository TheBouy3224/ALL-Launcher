package com.movtery.zalithlauncher.all.touch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Button groups (layers / drawer): a group action is an event string, the group is a name on the item */
class TouchGroupTest {
    @Test fun eventString() = assertEquals("G:toggle:Tools", groupEvent("toggle", "Tools"))
    @Test fun isGroup() { assertTrue("G:hide:x".isGroupEvent()); assertFalse("K:GLFW_KEY_G".isGroupEvent()); assertFalse("T:G:x".isGroupEvent()) }
    @Test fun groupNameMayHaveSpaces() = assertEquals(listOf("show", "My tools"), "G:show:My tools".removePrefix("G:").split(":", limit = 2))
    @Test fun itemDefaultsToNoGroup() = assertEquals(null, TouchItem("a", TouchKind.BUTTON, "s", 0.5f, 0.5f, 46f).group)
    @Test fun layoutDefaultsToNoHidden() = assertEquals(emptySet<String>(), TouchLayout(emptyList()).hiddenGroups)
}
