package com.movtery.zalithlauncher.all.touch

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

/** D-Pad / joystick direction bits (UP=1, DOWN=2, LEFT=4, RIGHT=8): dead zone in the middle, diagonals when both axes pass */
class TouchDirectionTest {
    private val c = Offset(100f, 100f)
    private fun bits(dx: Float, dy: Float, dead: Float = 1f / 3f) = dirBits(Offset(c.x + dx, c.y + dy), c, 50f, 50f, dead)

    @Test fun centreIsDeadZone() = assertEquals(0, bits(0f, 0f))
    @Test fun insideDeadZoneIsStill() = assertEquals(0, bits(10f, -10f))
    @Test fun up() = assertEquals(1, bits(0f, -45f))
    @Test fun down() = assertEquals(2, bits(0f, 45f))
    @Test fun left() = assertEquals(4, bits(-45f, 0f))
    @Test fun right() = assertEquals(8, bits(45f, 0f))
    @Test fun diagonalNeedsBothAxes() = assertEquals(1 or 8, bits(45f, -45f))
}
