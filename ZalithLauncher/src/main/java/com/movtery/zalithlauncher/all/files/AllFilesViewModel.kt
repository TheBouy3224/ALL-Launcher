package com.movtery.zalithlauncher.all.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movtery.zalithlauncher.filemanager.logic.entry.FmEntry
import com.movtery.zalithlauncher.filemanager.logic.ops.ConflictResolution
import com.movtery.zalithlauncher.filemanager.viewmodel.FileManagerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.nio.file.Path

/** ALL presentation state backed by the existing Zalith file-manager ViewModel. */
class AllFilesViewModel(backend: FileManagerViewModel) : ViewModel() {
    private val service = AllFilesService(backend)
    private val _state = MutableStateFlow(AllFilesState())
    val state: StateFlow<AllFilesState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(service.initState, service.state) { init, backendState ->
                val raw = backendState.rawList
                AllFilesState(
                    init = init,
                    backend = backendState,
                    entries = backendState.visibleEntries,
                    currentPathLabel = raw?.currentDir?.toString().orEmpty(),
                    rootPathLabel = raw?.rootDir?.toString().orEmpty()
                )
            }.collect { _state.value = it }
        }
        viewModelScope.launch {
            service.errorEvents.collect { message ->
                _state.value = _state.value.copy(errorMessage = message)
            }
        }
        service.initialize()
    }

    fun refresh() = service.refresh()
    fun enter(entry: FmEntry) = service.enter(entry)
    fun navigate(path: Path) = service.navigate(path)
    fun goParent(): Boolean = service.goParent()
    fun consumeBack(): Boolean = service.consumeBack()
    fun createFolder(name: String, done: (Boolean) -> Unit) = service.createFolder(name, done)
    fun rename(entry: FmEntry, name: String, done: () -> Unit) = service.rename(entry, name, done)
    fun deleteToTrash(entry: FmEntry) = service.deleteToTrash(entry)
    fun copy(entry: FmEntry) = service.copy(entry)
    fun move(entry: FmEntry) = service.move(entry)
    fun paste() = service.paste()
    fun resolvePasteConflict(resolution: ConflictResolution) = service.resolvePasteConflict(resolution)
    fun importFiles(uris: List<android.net.Uri>) = service.importFiles(uris)
    fun consumeSnackbar() = service.consumeSnackbar()
    fun consumeError() {
        _state.value = _state.value.copy(errorMessage = null)
    }
}
