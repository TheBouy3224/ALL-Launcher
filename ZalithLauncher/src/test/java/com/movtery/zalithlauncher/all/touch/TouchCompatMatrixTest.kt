package com.movtery.zalithlauncher.all.touch

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.movtery.layer_controller.layout.loadLayoutFromString
import com.movtery.layer_controller.utils.saveToFile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

/** Every REAL layout file we have (Zalith 2, FCL, Pojav, Zalith 1 style) -> ALL -> every format -> ALL again: keys and positions must survive */
class TouchCompatMatrixTest {
    private val screen = LayoutMapper.Screen(2436, 1080, 2.75f)
    private fun res(p: String) = javaClass.getResource(p)!!.readText().removePrefix("﻿")

    private fun sources(): Map<String, TouchLayout> {
        val z2 = LayoutMapper.fromZalith(loadLayoutFromString(res("/zl2/default_layout.json")), screen).value
        val z2all = LayoutMapper.fromZalith(loadLayoutFromString(res("/zl2/exported_by_all.json")), screen).value
        val fcl = LayoutMapper.fromFcl(res("/fcl/fcl_default_controller.json"), screen).value.second
        val pj = LayoutMapper.fromPojav(res("/pojav/default_layout.json"), screen).value.second
        val zl1 = JsonParser.parseString(res("/pojav/default_layout.json")).asJsonObject.apply {
            addProperty("version", 7); add("mControlInfoDataList", JsonObject().apply { addProperty("name", "Z1") })
        }.toString().let { LayoutMapper.fromPojav(it, screen).value.second }
        return mapOf("zalith2" to z2, "zalith2-by-ALL" to z2all, "fcl" to fcl, "pojav" to pj, "zalith1" to zl1)
    }

    private fun viaZalith2(l: TouchLayout): TouchLayout {
        val f = File.createTempFile("zl2", ".json"); runBlocking { LayoutMapper.toZalith(l, "t", screen).value.saveToFile(f) }
        return LayoutMapper.fromZalith(loadLayoutFromString(f.readText()).also { f.delete() }, screen).value
    }
    private fun targets(): Map<String, (TouchLayout) -> TouchLayout> = mapOf(
        "zalith2" to ::viaZalith2,
        "fcl" to { l -> LayoutMapper.fromFcl(LayoutMapper.toFcl(l, "t", screen).value, screen).value.second },
        "pojav" to { l -> LayoutMapper.fromPojav(LayoutMapper.toPojav(l, screen).value, screen).value.second },
        "zalith1" to { l -> LayoutMapper.fromPojav(LayoutMapper.toZalith1(l, "t", screen).value, screen).value.second },
    )
    private fun keys(l: TouchLayout) = l.items.flatMap { it.events + it.longEvents + it.doubleEvents + it.clickEvents }.filter { it.startsWith("K:") }.toSet()

    @Test fun keysSurviveEveryRoute() {
        val bad = ArrayList<String>()
        for ((sn, a) in sources()) for ((tn, t) in targets()) {
            val b = runCatching { t(a) }.getOrElse { bad.add("$sn -> $tn: CRASH ${it.message}"); continue }
            val lost = keys(a) - keys(b)
            if (lost.isNotEmpty()) bad.add("$sn -> $tn: lost keys $lost")
            if (b.items.size < a.items.size * 0.8) bad.add("$sn -> $tn: items ${a.items.size} -> ${b.items.size}")
            val far = a.items.count { x -> b.items.none { y -> x.events == y.events && abs(x.x - y.x) < 0.01f && abs(x.y - y.y) < 0.01f } }
            if (far > a.items.size / 4) bad.add("$sn -> $tn: $far/${a.items.size} items moved or changed")
        }
        assertTrue(bad.joinToString("\n"), bad.isEmpty())
    }
}
