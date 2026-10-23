package com.movtery.zalithlauncher.all.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 安装目标选择框：不能自动预选当前实例、不能记住上一次的选择，避免文件被装进用户自己的实例 */
class DownloadTargetSelectionTest {
    private val installed = listOf("Remarkably Optimized", "QA-IQOO-1", "1.20.1 Forge 47.4.23")

    @Test
    fun noExplicitTargetMeansNothingIsPreselected() {
        assertTrue(initialTargetSelection(null, installed).isEmpty())
    }

    @Test
    fun theActiveOrPreviouslyChosenInstanceIsNeverPreselected() {
        //旧行为：打开选择框时自动勾上“当前启用的实例”。现在 explicitTarget 只来自用户的明确操作
        val selection = initialTargetSelection(explicitTarget = null, installedNames = installed)
        assertTrue("Remarkably Optimized" !in selection)
        assertEquals(emptySet<String>(), selection)
    }

    @Test
    fun anExplicitTargetFromTheInstanceWindowIsPreselectedAlone() {
        assertEquals(setOf("QA-IQOO-1"), initialTargetSelection("QA-IQOO-1", installed))
    }

    @Test
    fun anExplicitTargetThatNoLongerExistsIsDropped() {
        assertTrue(initialTargetSelection("Deleted Instance", installed).isEmpty())
    }

    @Test
    fun reopeningTheChooserStartsFromTheSameCleanStateEveryTime() {
        //两次打开的结果只取决于参数，不依赖任何残留状态
        assertEquals(initialTargetSelection(null, installed), initialTargetSelection(null, installed))
    }
}
