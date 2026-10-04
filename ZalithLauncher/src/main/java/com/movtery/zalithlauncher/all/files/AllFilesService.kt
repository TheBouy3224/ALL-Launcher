package com.movtery.zalithlauncher.all.files

import android.net.Uri
import com.movtery.zalithlauncher.filemanager.logic.entry.FmEntry
import com.movtery.zalithlauncher.filemanager.logic.ops.ConflictResolution
import com.movtery.zalithlauncher.filemanager.viewmodel.FileManagerViewModel

/**
 * ALL adapter over Zalith's established file-manager backend.
 *
 * This class owns no filesystem implementation: path validation, operations, trash, conflict
 * handling, import safety and navigation remain in FileManagerViewModel and its controllers.
 */
class AllFilesService(private val backend: FileManagerViewModel) {
    val state get() = backend.state
    val initState get() = backend.initState
    val errorEvents get() = backend.errorEvents

    fun initialize() = backend.initialize()
    fun refresh() = backend.refresh()
    fun enter(entry: FmEntry) = backend.enterDirectory(entry)
    fun goParent(): Boolean = backend.goParent()
    fun consumeBack(): Boolean = backend.consumeBack()
    fun navigate(path: java.nio.file.Path) = backend.navigateTo(path)
    fun createFolder(name: String, done: (Boolean) -> Unit) = backend.submitCreate(name, true, done)
    fun rename(entry: FmEntry, name: String, done: () -> Unit) = backend.submitRename(entry, name, done)
    fun deleteToTrash(entry: FmEntry) {
        backend.toggleSelection(entry)
        backend.deleteSelected(toTrash = true)
    }
    fun copy(entry: FmEntry) = backend.copyEntry(entry)
    fun move(entry: FmEntry) = backend.cutEntry(entry)
    fun paste() = backend.requestPaste()
    fun resolvePasteConflict(resolution: ConflictResolution) = backend.resolvePasteConflict(resolution)
    fun importFiles(uris: List<Uri>) = backend.onImportFiles(uris)
    fun consumeSnackbar() = backend.consumeSnackbar()
}
