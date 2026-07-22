package com.example.ui.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.CanvasEntity
import com.example.data.repository.CanvasRepository
import com.example.drive.ExportManager
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class HomeViewModel(private val repository: CanvasRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    val canvases: StateFlow<List<CanvasEntity>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            repository.searchCanvases(query)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) _searchQuery.value = ""
    }

    fun createNewCanvas(onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val id = repository.createNewCanvas()
            onCreated(id)
        }
    }

    fun createTemplateCanvas(templateType: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val id = repository.createTemplateCanvas(templateType)
            onCreated(id)
        }
    }

    fun renameCanvas(canvas: CanvasEntity, newTitle: String) {
        viewModelScope.launch {
            repository.updateCanvas(canvas.copy(title = newTitle))
        }
    }

    fun duplicateCanvas(canvasId: String) {
        viewModelScope.launch {
            repository.duplicateCanvas(canvasId)
        }
    }

    fun deleteCanvas(canvasId: String) {
        viewModelScope.launch {
            repository.deleteCanvas(canvasId)
        }
    }

    fun importPdfOrImage(context: Context, uri: Uri, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val imagePath = repository.saveImportedImage(uri)
            val canvasId = repository.createNewCanvas(title = "Імпортований документ")
            val pages = repository.getPagesForCanvasSync(canvasId)
            if (pages.isNotEmpty()) {
                val updatedPage = pages.first().copy(
                    images = listOf(
                        com.example.data.models.ImageElementEntity(
                            sourceUri = imagePath,
                            x = 50f,
                            y = 50f,
                            width = 600f,
                            height = 800f
                        )
                    )
                )
                repository.updatePage(updatedPage)
            }
            onCreated(canvasId)
        }
    }
}
