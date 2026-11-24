package com.movtery.zalithlauncher.all.touch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** V2 group panel: declared (empty) groups, rename / delete reach every member and every group event of every trigger */
class TouchGroupV2Test {
    private fun btn(id: String, group: String? = null, events: List<String> = emptyList(), long: List<String> = emptyList()) =
        TouchItem(id, TouchKind.BUTTON, "touch_controls/control_blank_m", 0.5f, 0.5f, 56f, events, group = group, longEvents = long)

    @Test fun allGroupsIncludesEmptyAndHidden() {
        val l = TouchLayout(listOf(btn("a", "x")), hiddenGroups = setOf("h"), groups = listOf("e", "x"))
        assertEquals(listOf("e", "x", "h"), l.allGroups())
    }

    @Test fun renameMovesMembersEventsAndHiddenState() {
        val l = TouchLayout(listOf(btn("a", "old"), btn("b", null, listOf(groupEvent("toggle", "old"), "K:GLFW_KEY_A"), long = listOf(groupEvent("hide", "old")))),
            hiddenGroups = setOf("old"), groups = listOf("old")).renameGroup("old", "new")
        assertEquals("new", l.items[0].group)
        assertEquals(listOf("G:toggle:new", "K:GLFW_KEY_A"), l.items[1].events)
        assertEquals(listOf("G:hide:new"), l.items[1].longEvents)
        assertEquals(setOf("new"), l.hiddenGroups); assertEquals(listOf("new"), l.groups)
    }

    @Test fun renameLeavesOtherGroupsAlone() {
        val l = TouchLayout(listOf(btn("a", "keep", listOf("G:show:keep")))).renameGroup("old", "new")
        assertEquals(listOf("G:show:keep"), l.items[0].events); assertEquals("keep", l.items[0].group)
    }

    @Test fun deleteUngroupsMembersAndDropsItsEventsOnly() {
        val l = TouchLayout(listOf(btn("a", "g"), btn("b", null, listOf("G:toggle:g", "G:toggle:other", "K:GLFW_KEY_A"), long = listOf("G:show:g"))),
            hiddenGroups = setOf("g"), groups = listOf("g")).deleteGroup("g")
        assertEquals(null, l.items[0].group)
        assertEquals(listOf("G:toggle:other", "K:GLFW_KEY_A"), l.items[1].events)
        assertTrue(l.items[1].longEvents.isEmpty()); assertTrue(l.hiddenGroups.isEmpty()); assertTrue(l.groups.isEmpty())
    }

    @Test fun multiGroupActionAddsEveryEventOnce() {
        val evs = listOf("a", "b", "a").map { groupEvent("toggle", it) }
        assertEquals(listOf("G:toggle:a", "G:toggle:b"), evs.fold(emptyList<String>()) { l, e -> l.withEvent(e) })
    }

    @Test fun previewGroupEvents() {
        val none = emptySet<String>()
        assertEquals(setOf("a"), none.afterGroupEvent("G:toggle:a"))
        assertEquals(none, setOf("a").afterGroupEvent("G:toggle:a"))
        assertEquals(none, setOf("a").afterGroupEvent("G:show:a"))
        assertEquals(setOf("a"), none.afterGroupEvent("G:hide:a"))
        assertEquals(setOf("a"), setOf("a").afterGroupEvent("K:GLFW_KEY_A"))
    }
}
