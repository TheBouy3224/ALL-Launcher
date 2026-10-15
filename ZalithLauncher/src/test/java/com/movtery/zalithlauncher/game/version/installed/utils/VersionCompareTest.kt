/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.game.version.installed.utils

import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

/**
 * 版本比较使用随 APK 发布的真实版本表（assets/game/versions.txt，测试里由构建脚本原样复制到类路径）。
 * 旧的测试用一个只有两行的桩文件遮住了真实版本表，所以快照顺序全部算错；这里改用真实版本号与真实表。
 */
class VersionCompareTest {

    /** 插件门槛的判定方式：minMCVer <= 版本 <= maxMCVer（与 LauncherElements 使用同一组比较函数） */
    private fun inGate(version: String, min: String?, max: String?): Boolean =
        (min == null || version.isBiggerOrEqualVer(min)) && (max == null || version.isLowerOrEqualVer(max))

    @Test
    fun testCompareLegacyReleases() {
        assertTrue("1.8".isBiggerVer("1.7"))
        assertTrue("1.0".isBiggerOrEqualVer("1.0"))
        assertTrue("1.16".isLowerVer("1.17"))
        assertTrue("1.21.11".isBiggerVer("1.21.5"))
    }

    @Test
    fun testCompareLegacySnapshots() {
        //旧式快照 YYwWWx 与发布版：按真实发布顺序
        assertTrue("20w30a".isLowerVer("1.21.5"))
        assertTrue("20w30a".isLowerVer("1.16.2"))
        assertTrue("20w30a".isBiggerVer("1.16.1"))
        assertTrue("20w14a".isBiggerVer("1.15.2"))
        assertTrue("20w14a".isLowerVer("1.16"))
        assertTrue("1.15".isBiggerVer("19w46b"))
        assertTrue("1.14.4".isLowerVer("19w46b"))
        assertTrue("20w14a".isLowerVer("20w14b"))
    }

    @Test
    fun testSnapshotsAroundRecentReleases() {
        assertTrue("23w40a".isBiggerVer("1.20.2"))
        assertTrue("24w10a".isLowerVer("1.20.5"))
        assertTrue("25w10a".isBiggerVer("1.21.4"))
        assertTrue("25w10a".isLowerVer("1.21.5"))
    }

    @Test
    fun testPreReleasesAndReleaseCandidates() {
        assertTrue("1.21.5-pre1".isLowerVer("1.21.5-rc1"))
        assertTrue("1.21.5-rc1".isLowerVer("1.21.5"))
        assertTrue("1.21.5-pre1".isLowerVer("1.21.5"))
        assertTrue("1.21.5-pre1".isBiggerVer("1.21.4"))
        assertFalse("1.21.5".isLowerVer("1.21.5-rc1"))
    }

    @Test
    fun testCompareNewReleases() {
        assertTrue("26.3".isBiggerOrEqualVer("26.2"))
        assertTrue("26.1".isLowerOrEqualVer("26.1"))
        assertFalse("26.1".isBiggerVer("26.2"))
    }

    @Test
    fun testCompareNewSchemeSnapshots() {
        //新命名规则（26.x）：快照低于它之后的正式版，高于之前的正式版
        assertTrue("26.2-snapshot-1".isLowerVer("26.2"))
        assertTrue("26.1-snapshot-1".isLowerVer("26.1"))
        assertTrue("26.2-snapshot-1".isBiggerVer("26.1"))
        assertTrue("26.1-snapshot-1".isBiggerVer("1.21.11"))
    }

    @Test
    fun testNewVsLegacy() {
        assertTrue("26.1".isBiggerVer("1.21.11"))
        assertTrue("26.2-snapshot-1".isBiggerVer("1.21.11"))
        assertTrue("26.1-snapshot-1".isBiggerVer("23w40a"))
        assertFalse("20w14a".isBiggerVer("1.21.11"))
    }

    @Test
    fun testVersionEquality() {
        assertTrue("1.21.5".isBiggerOrEqualVer("1.21.5"))
        assertTrue("20w30a".isLowerOrEqualVer("1.21.5"))
        assertTrue("1.21.5".isLowerOrEqualVer("1.21.5"))
    }

    // ---- 插件门槛（minMCVer / maxMCVer）----

    @Test
    fun pluginGateAcceptsTheSupportedRangeAndRejectsOutsideIt() {
        assertTrue(inGate("1.21.11", min = "1.21.5", max = null))
        assertTrue(inGate("1.21.5", min = "1.21.5", max = "1.21.11"))
        assertFalse(inGate("1.21.4", min = "1.21.5", max = null))
        assertFalse(inGate("26.1", min = null, max = "1.21.11"))
    }

    @Test
    fun pluginGateTreatsSnapshotsByTheirRealPosition() {
        //1.21.5 的快照 25w10a 还没到 1.21.5：要求 >=1.21.5 的插件不能在它上面启用
        assertFalse(inGate("25w10a", min = "1.21.5", max = null))
        assertTrue(inGate("25w10a", min = "1.21.4", max = "1.21.5"))
        //旧快照不会被误当成比最新正式版更新
        assertFalse(inGate("20w30a", min = "1.21.5", max = null))
        assertTrue(inGate("20w30a", min = null, max = "1.21.5"))
        assertFalse(inGate("1.21.5-pre1", min = "1.21.5", max = null))
        assertTrue(inGate("1.21.5-rc1", min = "1.21.5-pre1", max = "1.21.5"))
    }
}
