package com.movtery.zalithlauncher.all.launchersettings

import android.view.InputDevice
import com.movtery.zalithlauncher.ui.control.gamepad.GamepadMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GamepadMappingDraftTest {
    /** a fake "storage" standing in for GamepadMappingList */
    private val storage = mutableMapOf<Pair<GamepadMap, Boolean>, Set<String>>().apply {
        GamepadMap.entries.forEach { m ->
            put(m to true, m.defaultKeysInGame)
            put(m to false, m.defaultKeysInMenu)
        }
    }
    private fun draft() = GamepadMappingDraft { m, g -> storage[m to g] ?: emptySet() }

    @Test fun editingChangesTheDraftButNeverTheStorage() {
        val d = draft()
        d.set(GamepadMap.ButtonA, true, setOf("KEY_X"))
        assertEquals(setOf("KEY_X"), d.get(GamepadMap.ButtonA, true))
        assertTrue(d.isDirty())
        assertTrue(d.isModified(GamepadMap.ButtonA, true))
        assertEquals(GamepadMap.ButtonA.defaultKeysInGame, storage[GamepadMap.ButtonA to true])
    }

    @Test fun cancelDropsEveryEdit() {
        val d = draft()
        d.set(GamepadMap.ButtonB, false, emptySet())
        d.set(GamepadMap.ButtonY, true, setOf("A", "B"))
        d.discard()
        assertFalse(d.isDirty())
        assertEquals(GamepadMap.ButtonY.defaultKeysInGame, d.get(GamepadMap.ButtonY, true))
    }

    @Test fun settingBackToTheSavedValueIsNotAChange() {
        val d = draft()
        d.set(GamepadMap.ButtonX, true, setOf("Z"))
        d.set(GamepadMap.ButtonX, true, GamepadMap.ButtonX.defaultKeysInGame)
        assertFalse(d.isDirty())
        assertTrue(d.changes().isEmpty())
    }

    @Test fun resetAllStagesFactoryKeysOnlyWhereTheyDiffer() {
        storage[GamepadMap.ButtonA to true] = setOf("CUSTOM")
        val d = draft()
        d.resetAll()
        assertEquals(mapOf((GamepadMap.ButtonA to true) to GamepadMap.ButtonA.defaultKeysInGame), d.changes())
        //the saved copy is still the custom one until Save
        assertEquals(setOf("CUSTOM"), storage[GamepadMap.ButtonA to true])
    }

    @Test fun resetOneAndAnUnboundButton() {
        storage[GamepadMap.ButtonStart to false] = setOf("CUSTOM")
        val d = draft()
        d.resetOne(GamepadMap.ButtonStart, false)
        assertEquals(GamepadMap.ButtonStart.defaultKeysInMenu, d.get(GamepadMap.ButtonStart, false))
        //a button with no factory keys can be unbound and rebound
        d.set(GamepadMap.StickRightClick, true, setOf("K"))
        assertTrue(d.isModified(GamepadMap.StickRightClick, true))
    }

    @Test fun gamepadSources() {
        assertTrue(isGamepadSource(InputDevice.SOURCE_GAMEPAD))
        assertTrue(isGamepadSource(InputDevice.SOURCE_JOYSTICK or InputDevice.SOURCE_KEYBOARD))
        assertFalse(isGamepadSource(InputDevice.SOURCE_KEYBOARD))
        assertFalse(isGamepadSource(InputDevice.SOURCE_TOUCHSCREEN))
    }
}
