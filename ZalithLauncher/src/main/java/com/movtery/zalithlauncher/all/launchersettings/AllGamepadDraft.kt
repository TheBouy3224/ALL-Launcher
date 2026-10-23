package com.movtery.zalithlauncher.all.launchersettings

import android.view.InputDevice
import com.movtery.zalithlauncher.ui.control.gamepad.GamepadMap

/**
 * Unsaved edits of the gamepad -> keyboard bindings shown by the ALL Gamepad tab.
 *
 * Nothing here touches storage: the tab stages every change in this object and only on Save writes the changed entries through the
 * existing [com.movtery.zalithlauncher.ui.control.gamepad.GamepadMappingList.saveMapping] (Zalith's MMKV format, unchanged).
 * Cancel just drops the edits, so the saved data can never be damaged by editing or by a reset that is not confirmed with Save.
 *
 * @param saved what is currently saved for a button in the game (true) / in menus (false)
 */
class GamepadMappingDraft(private val saved: (GamepadMap, Boolean) -> Set<String>) {
    private val edits = mutableMapOf<Pair<GamepadMap, Boolean>, Set<String>>()

    fun get(map: GamepadMap, inGame: Boolean): Set<String> = edits[map to inGame] ?: saved(map, inGame)

    fun set(map: GamepadMap, inGame: Boolean, keys: Set<String>) {
        if (keys == saved(map, inGame)) edits.remove(map to inGame) else edits[map to inGame] = keys
    }

    fun defaultsOf(map: GamepadMap, inGame: Boolean): Set<String> = if (inGame) map.defaultKeysInGame else map.defaultKeysInMenu

    /** Stage the factory keys of one button */
    fun resetOne(map: GamepadMap, inGame: Boolean) = set(map, inGame, defaultsOf(map, inGame))

    /** Stage the factory keys of every button, in game and in menus (still needs Save) */
    fun resetAll() {
        GamepadMap.entries.forEach { map -> listOf(true, false).forEach { inGame -> resetOne(map, inGame) } }
    }

    fun isModified(map: GamepadMap, inGame: Boolean): Boolean = edits.containsKey(map to inGame)

    fun isDirty(): Boolean = edits.isNotEmpty()

    /** The entries that differ from what is saved, ready to be written */
    fun changes(): Map<Pair<GamepadMap, Boolean>, Set<String>> = edits.toMap()

    /** Cancel / after a successful save */
    fun discard() = edits.clear()
}

/** A device counts as a gamepad when it reports the gamepad or the joystick source (same idea as Android's own checks) */
fun isGamepadSource(sources: Int): Boolean =
    (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
        (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK

/** Names of the connected gamepads; empty when none is connected */
fun connectedGamepadNames(): List<String> =
    InputDevice.getDeviceIds().toList().mapNotNull { id ->
        InputDevice.getDevice(id)?.takeIf { !it.isVirtual && isGamepadSource(it.sources) }?.name
    }
