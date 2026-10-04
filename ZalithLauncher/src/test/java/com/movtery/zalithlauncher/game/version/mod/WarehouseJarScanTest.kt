package com.movtery.zalithlauncher.game.version.mod

import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.security.MessageDigest

/**
 * Warehouse tool, not a normal regression test.
 *
 * It exists so the overnight warehouse can read real JARs through **the launcher's own readers** -
 * `AllModReader` and the Mod Doctor's metadata mapper - instead of a second parser written for the
 * occasion. Running it under the module's test task is what puts those classes on the classpath.
 *
 * It does nothing unless both environment variables are set, so an ordinary test run is unaffected:
 *
 *   WAREHOUSE_JAR_DIR  a directory of .jar files
 *   WAREHOUSE_JAR_OUT  the JSONL file to write
 *
 * One JSON object per mod found in one file. A file that no reader accepts is reported with
 * `readable = false` rather than dropped, because "ALL cannot read this" is itself warehouse data.
 */
class WarehouseJarScanTest {
    @Test
    fun scanJarDirectory() {
        val dirPath = System.getenv("WAREHOUSE_JAR_DIR")
        val outPath = System.getenv("WAREHOUSE_JAR_OUT")
        if (dirPath.isNullOrBlank() || outPath.isNullOrBlank()) {
            println("WarehouseJarScanTest: WAREHOUSE_JAR_DIR/WAREHOUSE_JAR_OUT not set, nothing to do")
            return
        }

        val modsDir = File(dirPath)
        require(modsDir.isDirectory) { "not a directory: $dirPath" }
        val jarCount = modsDir.listFiles()?.count { !it.isDirectory } ?: 0
        println("WarehouseJarScanTest: reading $jarCount file(s) from $modsDir")

        val gson = Gson()
        val doctor = ModDoctor()
        var written = 0
        var unreadable = 0

        BufferedWriter(FileWriter(outPath)).use { out ->
            val locals = runBlocking { AllModReader(modsDir).readAllLocals() }
            for (local in locals) {
                val metadata = runBlocking { doctor.toDoctorMetadata(local) }
                if (!metadata.readable) unreadable++
                val row = linkedMapOf<String, Any?>(
                    "sha1" to sha1(local.file),
                    "fileName" to metadata.fileName,
                    "readable" to metadata.readable,
                    "modId" to metadata.modId,
                    "name" to metadata.name,
                    "version" to metadata.version,
                    "loader" to metadata.loader.name,
                    "requiredDependencies" to metadata.requiredDependencies,
                    "requiredDependencyPredicates" to metadata.requiredDependencyPredicates,
                    "breakPredicates" to metadata.breakPredicates,
                    "minecraftVersionRanges" to metadata.minecraftVersionRanges,
                    "providedModIds" to metadata.providedModIds,
                    "providedModVersions" to metadata.providedModVersions,
                )
                out.write(gson.toJson(row))
                out.newLine()
                written++
                if (written % 500 == 0) println("WarehouseJarScanTest: $written row(s)")
            }
        }
        println("WarehouseJarScanTest: wrote $written row(s), $unreadable unreadable, to $outPath")
    }

    private fun sha1(file: File): String {
        val digest = MessageDigest.getInstance("SHA-1")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
