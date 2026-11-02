package com.movtery.zalithlauncher.path

import com.movtery.zalithlauncher.utils.logging.Logger
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.jar.JarFile

/**
 * Helper jars ALL does not ship in the APK (no licence that allows ALL to redistribute them).
 * They are fetched on first use from the party that publishes them and kept outside the components folder,
 * so a component update does not delete them.
 */
object OptionalJars {
    private const val TAG = "OptionalJars"
    private const val ZL2 = "https://raw.githubusercontent.com/ZalithLauncher/ZalithLauncher2/cbf877a979b9375569010b1021fe885d3a05895a/ZalithLauncher/src/main/assets/components"

    /** a fetched jar: [sha256] pins the exact file when the source is a fixed revision; null = vendor file that changes, checked by [premainClass] */
    class Jar(val fileName: String, val url: String, val sha256: String?, val premainClass: String?) {
        val file: File get() = File(File(PathManager.DIR_COMPONENTS.parentFile, "components_dl"), fileName)
    }

    /** Mio's mod-compatibility agent, as distributed by Zalith Launcher 2 (upstream repository, fixed commit) */
    val MIO_LIB_PATCHER = Jar("MioLibPatcher.jar", "$ZL2/launcher/MioLibPatcher.jar", "80445f93b45d6f271205964528fd631f53d1c9b94971909eca1b4ac63ab63539", "com.mio.libpatcher.MainAgent")
    /** bangbang93's forge-install-bootstrapper, as distributed by Zalith Launcher 2 (no launch path uses it, same as upstream) */
    val FORGE_INSTALLER = Jar("forge_installer.jar", "$ZL2/launcher/forge_installer.jar", "7e37ac73caea277d22ba16032bc91a9f2fa1974b0739824a5f8e71b8393a6745", null)
    /** Nide8 login agent from Nide8's own download address (the vendor updates it, so it is checked by its manifest, not a hash) */
    val NIDE_8_AUTH = Jar("nide8auth.jar", "https://login.mc-user.com:233/index/jar", null, "com.nide8.login2.Nide8Auth")

    /** Returns the jar, downloading it if it is missing or damaged; null when it cannot be fetched (blocking: call off the main thread). */
    fun ensure(jar: Jar): File? {
        if (isValid(jar)) return jar.file
        return runCatching {
            jar.file.parentFile?.mkdirs()
            val tmp = File(jar.file.path + ".part")
            var url = URL(jar.url)
            var conn: HttpURLConnection
            var hops = 0
            while (true) { //follow redirects, also across hosts (Nide8 redirects to its static host)
                conn = (url.openConnection() as HttpURLConnection).apply { connectTimeout = 15000; readTimeout = 30000; instanceFollowRedirects = false }
                if (conn.responseCode !in 300..399 || hops++ > 5) break
                url = URL(url, conn.getHeaderField("Location")); conn.disconnect()
            }
            check(conn.responseCode == 200) { "HTTP ${conn.responseCode} for ${jar.url}" }
            conn.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it) } }
            if (jar.file.exists()) jar.file.delete()
            check(tmp.renameTo(jar.file)) { "cannot move ${tmp.name}" }
            check(isValid(jar)) { "downloaded ${jar.fileName} failed verification" }
            Logger.info(TAG, "Fetched ${jar.fileName} from ${jar.url}")
            jar.file
        }.onFailure { e ->
            Logger.warning(TAG, "Could not fetch ${jar.fileName}", e)
            jar.file.delete()
        }.getOrNull()
    }

    private fun isValid(jar: Jar): Boolean {
        val f = jar.file
        if (!f.isFile || f.length() == 0L) return false
        jar.sha256?.let { expected ->
            val md = MessageDigest.getInstance("SHA-256")
            f.inputStream().use { s -> val b = ByteArray(1 shl 16); while (true) { val n = s.read(b); if (n < 0) break; md.update(b, 0, n) } }
            if (md.digest().joinToString("") { "%02x".format(it) } != expected) return false
        }
        jar.premainClass?.let { cls ->
            val premain = runCatching { JarFile(f).use { it.manifest?.mainAttributes?.getValue("Premain-Class") } }.getOrNull()
            if (premain != cls) return false
        }
        return true
    }
}
