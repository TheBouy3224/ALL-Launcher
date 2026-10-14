package com.movtery.zalithlauncher.game.version.mod

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 崩溃特征文件的解析回归：一份写坏的数据文件只能少一条特征，不能让崩溃页面失败。
 */
class CrashSignaturesTest {
    @Test
    fun shippedSignatureFileIsUsable() {
        val asset = File("src/main/assets/crash_signatures.json")
        assertTrue("the shipped signature file is missing: ${asset.absolutePath}", asset.isFile)

        val signatures = parseCrashSignatures(asset.readText())

        assertEquals(
            listOf("platform_limit_native", "mixin_apply_failed", "missing_required_dependency_fabric", "loader_fatal_unattributed"),
            signatures.map { it.id },
        )
        assertEquals(
            listOf("platform_limit", "mixin_failure", "loader_missing_dependency", "loader_fatal"),
            signatures.map { it.className },
        )
        assertEquals(DiagnosisConfidence.HIGH, signatures[0].confidence)
        assertEquals(DiagnosisAction.DISABLE, signatures[0].action)
        assertEquals("crash_diagnosis_platform_limit", signatures[0].messageKey)
        assertEquals(DiagnosisConfidence.MEDIUM, signatures[1].confidence)
        assertEquals(DiagnosisAction.NONE, signatures[1].action)
        assertEquals(DiagnosisConfidence.HIGH, signatures[2].confidence)
        assertEquals(DiagnosisAction.DISABLE, signatures[2].action)
        assertEquals(DiagnosisConfidence.LOW, signatures[3].confidence)
        assertEquals(DiagnosisAction.NONE, signatures[3].action)
    }

    @Test
    fun malformedFileYieldsNothing() {
        assertEquals(emptyList<CrashSignature>(), parseCrashSignatures("not json at all"))
        assertEquals(emptyList<CrashSignature>(), parseCrashSignatures("""{"signatures":"nope"}"""))
        assertEquals(emptyList<CrashSignature>(), parseCrashSignatures("""[1,2,3]"""))
    }

    @Test
    fun emptyFileYieldsNothing() {
        assertEquals(emptyList<CrashSignature>(), parseCrashSignatures(""))
        assertEquals(emptyList<CrashSignature>(), parseCrashSignatures("{}"))
        assertEquals(emptyList<CrashSignature>(), parseCrashSignatures("""{"signatures_version":1}"""))
        assertEquals(emptyList<CrashSignature>(), parseCrashSignatures("""{"signatures":[]}"""))
    }

    @Test
    fun incompleteEntriesAreDroppedAndNeverThrow() {
        val json = """
            {"signatures":[
              {"id":"no_confidence","class":"loader_fatal","match_any":["Mod resolution failed"],"action":"none","message_key":"k"},
              {"id":"no_action","class":"loader_fatal","match_any":["Mod resolution failed"],"confidence":"LOW","message_key":"k"},
              {"id":"no_message","class":"loader_fatal","match_any":["Mod resolution failed"],"confidence":"LOW","action":"none"},
              {"id":"no_class","match_any":["Mod resolution failed"],"confidence":"LOW","action":"none","message_key":"k"},
              {"class":"loader_fatal","match_any":["Mod resolution failed"],"confidence":"LOW","action":"none","message_key":"k"},
              {"id":"no_match","class":"loader_fatal","confidence":"LOW","action":"none","message_key":"k"},
              {"id":"unknown_confidence","class":"loader_fatal","match_any":["x"],"confidence":"CERTAIN","action":"none","message_key":"k"},
              {"id":"unknown_action","class":"loader_fatal","match_any":["x"],"confidence":"LOW","action":"delete_everything","message_key":"k"},
              {"id":"good","class":"loader_fatal","match_any":["Mod resolution failed"],"confidence":"LOW","action":"none","message_key":"k"}
            ]}
        """.trimIndent()

        val signatures = parseCrashSignatures(json)

        assertEquals(listOf("good"), signatures.map { it.id })
    }

    @Test
    fun missingAssetYieldsNothingAndDoesNotThrow() {
        //本地单测里没有 AssetManager，读随包资源这条路必然失败，它只能返回空列表
        assertEquals(emptyList<CrashSignature>(), CrashSignatures.load())
    }

    @Test
    fun anUnknownMessageKeyFallsBackToTheUnclassifiedText() {
        //文案留在 Kotlin 里：数据文件里的键认不出来时，退回「说不清原因」，而不是把键显示给用户
        assertEquals(
            com.movtery.zalithlauncher.R.string.crash_diagnosis_unclassified,
            diagnosisMessageRes("no_such_key"),
        )
        assertEquals(
            com.movtery.zalithlauncher.R.string.crash_diagnosis_platform_limit,
            diagnosisMessageRes("crash_diagnosis_platform_limit"),
        )
    }
}
