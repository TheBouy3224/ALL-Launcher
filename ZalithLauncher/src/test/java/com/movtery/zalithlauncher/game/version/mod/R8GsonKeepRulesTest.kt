package com.movtery.zalithlauncher.game.version.mod

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * R8 renamed the Gson model fields of this package in RC2 ("signatures" -> "a"): the bundled crash rule pack loaded with count=0 and
 * repair-snapshot manifests were written with keys a/b/c/d. The real guard is the `verifyR8GsonModels` Gradle task that inspects the
 * release mapping.txt; this test makes sure the keep rule it depends on cannot be dropped from proguard-rules.pro unnoticed.
 */
class R8GsonKeepRulesTest {
    private fun rules(): String {
        val f = listOf("proguard-rules.pro", "ZalithLauncher/proguard-rules.pro").map(::File).first { it.isFile }
        return f.readText()
    }

    @Test
    fun gsonModelFieldsOfTheModPackageAreKept() {
        val text = rules().replace(Regex("\\s+"), " ")
        assertTrue(
            "keep rule for game.version.mod.** fields is missing",
            text.contains("-keepclassmembers class com.movtery.zalithlauncher.game.version.mod.** {") && text.contains("<fields>;")
        )
        assertTrue("no-arg constructor must be kept so Kotlin defaults apply under Gson", text.contains("<init>();"))
    }

    @Test
    fun theRuleDoesNotDisableMinification() {
        // R8 stays on: the rule keeps members only, it must not be a blanket "-dontobfuscate"/"-dontshrink"
        val t = rules()
        assertTrue(!Regex("^\\s*-dont(obfuscate|shrink|optimize)", RegexOption.MULTILINE).containsMatchIn(t))
    }
}
