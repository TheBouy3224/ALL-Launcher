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

package com.movtery.zalithlauncher.utils.file

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jackhuang.hmcl.util.DigestUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.util.Random

/** 用程序生成的确定性数据验证 SHA-1：不依赖任何本机私有文件 */
class FileTest {
    private lateinit var dir: File

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("all-file-test").toFile()
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun referenceSha1(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-1").digest(bytes).joinToString("") { "%02x".format(it) }

    @Test
    fun knownAnswerVector() = runBlocking(Dispatchers.IO) {
        //FIPS 180 的标准测试向量："abc"
        val file = File(dir, "abc.bin").apply { writeText("abc") }
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", calculateFileSha1(file))
    }

    @Test
    fun matchesTheReferenceDigestAcrossBufferBoundaries() = runBlocking(Dispatchers.IO) {
        //长度刻意不是缓冲区（8192）的整数倍，并覆盖空文件
        for (size in listOf(0, 1, 8191, 8192, 8193, 300_001)) {
            val bytes = ByteArray(size).also { Random(size.toLong()).nextBytes(it) }
            val file = File(dir, "data-$size.bin").apply { writeBytes(bytes) }

            val expected = referenceSha1(bytes)
            assertEquals("size $size", expected, calculateFileSha1(file))
            assertEquals("size $size (HMCL digest)", expected, DigestUtils.digestToString("SHA-1", file.toPath()))
        }
    }
}
