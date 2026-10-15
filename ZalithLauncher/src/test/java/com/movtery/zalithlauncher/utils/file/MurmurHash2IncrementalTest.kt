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

import org.apache.commons.codec.digest.MurmurHash2
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.Random

/** 增量实现必须与“整个读进内存再哈希”的参考实现逐位一致；数据由程序生成，不依赖本机私有文件 */
class MurmurHash2IncrementalTest {
    private lateinit var dir: File
    private val skipped = listOf(0x9, 0xa, 0xd, 0x20)

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("all-murmur-test").toFile()
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun incrementalEqualsTheReferenceForEverySize() {
        //数据里刻意混入大量需要跳过的字节（制表符/换行/回车/空格），并覆盖空文件与各种缓冲区边界
        for (size in listOf(0, 1, 3, 4, 5, 1023, 1024, 1025, 65_536, 300_001)) {
            val random = Random(size.toLong())
            val bytes = ByteArray(size) { if (random.nextInt(4) == 0) skipped[random.nextInt(skipped.size)].toByte() else random.nextInt(256).toByte() }
            val file = File(dir, "data-$size.bin").apply { writeBytes(bytes) }

            assertEquals("size $size", reference(file), MurmurHash2Incremental.computeHash(file, byteToSkip = skipped))
        }
    }

    @Test
    fun aFileMadeOnlyOfSkippedBytesHashesLikeAnEmptyInput() {
        val file = File(dir, "blank.bin").apply { writeBytes(ByteArray(5000) { 0x20 }) }

        assertEquals(reference(File(dir, "empty.bin").apply { writeBytes(ByteArray(0)) }), MurmurHash2Incremental.computeHash(file, byteToSkip = skipped))
    }

    /** 参考实现：整个文件读进内存、去掉要跳过的字节、再一次性哈希（旧写法） */
    private fun reference(file: File): Long {
        val baos = ByteArrayOutputStream()
        Files.newInputStream(file.toPath()).use { stream ->
            val buf = ByteArray(1024)
            var bytesRead: Int
            while (stream.read(buf).also { bytesRead = it } != -1) {
                for (i in 0 until bytesRead) {
                    val b = buf[i]
                    if (b.toInt() !in skipped) baos.write(b.toInt())
                }
            }
        }
        return Integer.toUnsignedLong(MurmurHash2.hash32(baos.toByteArray(), baos.size(), 1))
    }
}
