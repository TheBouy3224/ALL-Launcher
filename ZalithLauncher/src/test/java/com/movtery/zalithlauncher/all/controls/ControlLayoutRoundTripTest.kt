package com.movtery.zalithlauncher.all.controls

import com.movtery.layer_controller.data.ButtonPosition
import com.movtery.layer_controller.data.DefaultSize
import com.movtery.layer_controller.data.NormalData
import com.movtery.layer_controller.data.VisibilityType
import com.movtery.layer_controller.data.lang.createTranslatable
import com.movtery.layer_controller.event.ClickEvent
import com.movtery.layer_controller.layout.ControlLayer
import com.movtery.layer_controller.layout.EmptyControlLayout
import com.movtery.layer_controller.layout.EmptyLayoutInfo
import com.movtery.layer_controller.layout.loadLayoutFromFile
import com.movtery.layer_controller.observable.ObservableControlLayout
import com.movtery.layer_controller.utils.saveToFile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * The ALL control editor keeps Zalith's engine and file format: a layout that is saved must load back identical
 * (button position, size, key binding), and saving must not leave a temp file or touch other layouts.
 */
class ControlLayoutRoundTripTest {
    private fun layoutWithButton(x: Int, y: Int, key: String) = EmptyControlLayout.copy(
        info = EmptyLayoutInfo.copy(name = createTranslatable(default = "QA layout")),
        layers = listOf(
            ControlLayer(
                name = "main", uuid = "layer-1", hide = false, visibilityType = VisibilityType.ALWAYS,
                normalButtons = listOf(
                    NormalData(
                        text = createTranslatable(default = "W"), uuid = "btn-1",
                        position = ButtonPosition(x, y), buttonSize = DefaultSize,
                        visibilityType = VisibilityType.ALWAYS,
                        _clickEvents = listOf(ClickEvent(ClickEvent.Type.Key, key)),
                        isSwipple = false, isPenetrable = false, isToggleable = false
                    )
                )
            )
        )
    )

    @Test
    fun savedLayoutLoadsBackIdentical() = runBlocking {
        val dir = Files.createTempDirectory("ctl").toFile()
        val file = File(dir, "qa.json")
        val layout = layoutWithButton(2500, 7000, "w")
        layout.saveToFile(file)
        val loaded = loadLayoutFromFile(file)
        assertEquals(layout, loaded)
        val b = loaded.layers.single().normalButtons.single()
        assertEquals(2500, b.position.x)
        assertEquals(7000, b.position.y)
        assertEquals("w", b.clickEvents.single().key)
        assertFalse("no temp file may stay behind", File(dir, "qa.json.tmp").exists())
    }

    @Test
    fun savingOneLayoutDoesNotTouchAnother() = runBlocking {
        val dir = Files.createTempDirectory("ctl2").toFile()
        val a = File(dir, "a.json"); val b = File(dir, "b.json")
        layoutWithButton(100, 100, "a").saveToFile(a)
        val before = a.readText()
        layoutWithButton(900, 900, "b").saveToFile(b)
        assertEquals(before, a.readText())
        assertTrue(b.isFile)
    }

    @Test
    fun movingAButtonChangesOnlyItsPosition() = runBlocking {
        val dir = Files.createTempDirectory("ctl3").toFile()
        val f = File(dir, "m.json")
        layoutWithButton(1000, 1000, "e").saveToFile(f)
        val moved = loadLayoutFromFile(f).let { l ->
            val layer = l.layers.single()
            val btn = layer.normalButtons.single().copy(position = ButtonPosition(3000, 4000))
            l.copy(layers = listOf(layer.copy(normalButtons = listOf(btn))))
        }
        moved.saveToFile(f)
        val back = loadLayoutFromFile(f).layers.single().normalButtons.single()
        assertEquals(3000, back.position.x); assertEquals(4000, back.position.y)
        assertEquals("e", back.clickEvents.single().key)
        assertEquals(DefaultSize, back.buttonSize)
    }

    @Test
    fun restoreReplacesTheContentButKeepsTheSavedBaseline() {
        val saved = layoutWithButton(1000, 1000, "w")
        val obs = ObservableControlLayout(saved)
        assertEquals("packing twice must give equal data (undo compares snapshots)", obs.pack(), obs.pack())
        assertFalse(obs.isModified())
        obs.restore(layoutWithButton(4000, 5000, "a"))
        val b = obs.pack().layers.single().normalButtons.single()
        assertEquals(4000, b.position.x)
        assertEquals("a", b.clickEvents.single().key)
        assertTrue("baseline is still the saved layout, so the editor still sees unsaved changes", obs.isModified())
        obs.restore(saved)
        assertFalse("restoring the saved state is no change", obs.isModified())
    }
}
