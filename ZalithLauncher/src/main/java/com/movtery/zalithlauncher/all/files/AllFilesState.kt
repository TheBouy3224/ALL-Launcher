package com.movtery.zalithlauncher.all.files

import com.movtery.zalithlauncher.filemanager.logic.entry.FmEntry
import com.movtery.zalithlauncher.filemanager.viewmodel.FmInitState
import com.movtery.zalithlauncher.filemanager.viewmodel.FileManagerUiState

/** UI state owned by ALL; entries and navigation facts come from the existing file backend. */
data class AllFilesState(
    val init: FmInitState = FmInitState.Pending,
    val backend: FileManagerUiState = FileManagerUiState(),
    val entries: List<FmEntry> = emptyList(),
    val currentPathLabel: String = "",
    val rootPathLabel: String = "",
    val errorMessage: String? = null
)
