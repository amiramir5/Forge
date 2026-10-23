package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ApiKeyPreferences
import com.example.engine.HtmlMotionRenderer
import com.example.model.AspectRatioPreset
import com.example.model.BitratePreset
import com.example.model.CaptureMode
import com.example.model.ExportedVideoItem
import com.example.model.FpsOption
import com.example.model.MotionTemplate
import com.example.model.MotionTemplatesCatalog
import com.example.model.RenderPhase
import com.example.service.GeminiMotionAiService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.webkit.WebView
import java.io.File
import java.util.regex.Pattern

enum class StudioTab(val label: String) {
    PREVIEW("Preview"),
    CODE("HTML Code"),
    SETTINGS("Settings"),
    EXPORTS("Library")
}

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val renderer = HtmlMotionRenderer(application)
    val preferences = ApiKeyPreferences(application)

    private val aiService = GeminiMotionAiService(
        apiKeyProvider = { preferences.geminiApiKey },
        modelProvider = { preferences.selectedModel }
    )

    private val _selectedTab = MutableStateFlow(StudioTab.PREVIEW)
    val selectedTab: StateFlow<StudioTab> = _selectedTab.asStateFlow()

    private val _projectTitle = MutableStateFlow("Custom Motion")
    val projectTitle: StateFlow<String> = _projectTitle.asStateFlow()

    private val _htmlCode = MutableStateFlow("")
    val htmlCode: StateFlow<String> = _htmlCode.asStateFlow()

    private var originalCode: String = ""
    private val codeUndoHistory = mutableListOf<String>()

    private val _hasChangesToReset = MutableStateFlow(false)
    val hasChangesToReset: StateFlow<Boolean> = _hasChangesToReset.asStateFlow()

    private val _selectedPreset = MutableStateFlow(AspectRatioPreset.PORTRAIT_9_16)
    val selectedPreset: StateFlow<AspectRatioPreset> = _selectedPreset.asStateFlow()

    private val _selectedFps = MutableStateFlow(FpsOption.FPS_60)
    val selectedFps: StateFlow<FpsOption> = _selectedFps.asStateFlow()

    private val _durationSec = MutableStateFlow(10.0f) // Set to 10s default as requested by user
    val durationSec: StateFlow<Float> = _durationSec.asStateFlow()

    private val _selectedBitrate = MutableStateFlow(BitratePreset.HIGH)
    val selectedBitrate: StateFlow<BitratePreset> = _selectedBitrate.asStateFlow()

    private val _captureMode = MutableStateFlow(CaptureMode.DETERMINISTIC_SCRUB)
    val captureMode: StateFlow<CaptureMode> = _captureMode.asStateFlow()

    private val _autoCleanControls = MutableStateFlow(preferences.autoCleanControls)
    val autoCleanControls: StateFlow<Boolean> = _autoCleanControls.asStateFlow()

    private val _renderPhase = MutableStateFlow<RenderPhase>(RenderPhase.Idle)
    val renderPhase: StateFlow<RenderPhase> = _renderPhase.asStateFlow()

    private val _exportedVideos = MutableStateFlow<List<ExportedVideoItem>>(emptyList())
    val exportedVideos: StateFlow<List<ExportedVideoItem>> = _exportedVideos.asStateFlow()

    private val _previewKey = MutableStateFlow(0)
    val previewKey: StateFlow<Int> = _previewKey.asStateFlow()

    private val _showTemplatePicker = MutableStateFlow(false)
    val showTemplatePicker: StateFlow<Boolean> = _showTemplatePicker.asStateFlow()

    private val _activeVideoPreview = MutableStateFlow<ExportedVideoItem?>(null)
    val activeVideoPreview: StateFlow<ExportedVideoItem?> = _activeVideoPreview.asStateFlow()

    // Interactive timeline scrubbing state
    private val _scrubTimeSec = MutableStateFlow(0f)
    val scrubTimeSec: StateFlow<Float> = _scrubTimeSec.asStateFlow()

    // AI Assistant State
    private val _showAiDialog = MutableStateFlow(false)
    val showAiDialog: StateFlow<Boolean> = _showAiDialog.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    private var renderJob: Job? = null

    init {
        loadExistingVideos()
    }

    private fun loadExistingVideos() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val app = getApplication<Application>()
            val primaryDir = File(app.filesDir, "videos")
            val legacyDir = File(app.cacheDir, "videos")

            val files = mutableListOf<File>()
            if (primaryDir.exists()) {
                primaryDir.listFiles { file -> file.extension.equals("mp4", ignoreCase = true) }?.let { files.addAll(it) }
            }
            if (legacyDir.exists()) {
                legacyDir.listFiles { file -> file.extension.equals("mp4", ignoreCase = true) }?.let { files.addAll(it) }
            }

            val sortedFiles = files.distinctBy { it.name }.sortedByDescending { it.lastModified() }

            val items = sortedFiles.map { file ->
                val thumb = generateVideoThumbnail(file)
                var duration = 0f
                var resolution = "MP4 Video"
                try {
                    val retriever = android.media.MediaMetadataRetriever()
                    retriever.setDataSource(file.absolutePath)
                    val durStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                    if (durStr != null) {
                        duration = (durStr.toLongOrNull() ?: 0L) / 1000f
                    }
                    val width = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    val height = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    if (width != null && height != null) {
                        resolution = "${width}×${height}"
                    }
                    retriever.release()
                } catch (e: Exception) {
                    // Ignore
                }

                ExportedVideoItem(
                    id = file.name,
                    title = file.nameWithoutExtension,
                    file = file,
                    durationSec = duration,
                    resolution = resolution,
                    timestamp = file.lastModified(),
                    thumbnail = thumb
                )
            }
            _exportedVideos.value = items
        }
    }

    private fun generateVideoThumbnail(file: File): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ThumbnailUtils.createVideoThumbnail(file, Size(240, 320), null)
            } else {
                @Suppress("DEPRECATION")
                ThumbnailUtils.createVideoThumbnail(file.absolutePath, MediaStore.Images.Thumbnails.MINI_KIND)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun selectTab(tab: StudioTab) {
        _selectedTab.value = tab
    }

    fun updateTitle(title: String) {
        _projectTitle.value = title
    }

    fun updateHtml(code: String) {
        if (_htmlCode.value != code) {
            codeUndoHistory.add(_htmlCode.value)
            _hasChangesToReset.value = true
        }
        _htmlCode.value = code
    }

    fun refreshPreview() {
        _previewKey.value += 1
    }

    fun selectPreset(preset: AspectRatioPreset) {
        _selectedPreset.value = preset
    }

    fun selectFps(fps: FpsOption) {
        _selectedFps.value = fps
    }

    fun setDuration(duration: Float) {
        _durationSec.value = duration
    }

    fun selectBitrate(bitrate: BitratePreset) {
        _selectedBitrate.value = bitrate
    }

    fun selectCaptureMode(mode: CaptureMode) {
        _captureMode.value = mode
    }

    fun toggleAutoCleanControls(clean: Boolean) {
        _autoCleanControls.value = clean
        preferences.autoCleanControls = clean
        refreshPreview()
    }

    fun setScrubTime(timeSec: Float) {
        _scrubTimeSec.value = timeSec.coerceIn(0f, _durationSec.value)
    }

    fun openTemplatePicker() {
        _showTemplatePicker.value = true
    }

    fun closeTemplatePicker() {
        _showTemplatePicker.value = false
    }

    fun applyTemplate(template: MotionTemplate) {
        _projectTitle.value = template.name
        originalCode = template.htmlCode
        codeUndoHistory.clear()
        _hasChangesToReset.value = false
        _htmlCode.value = template.htmlCode
        _durationSec.value = template.recommendedDurationSec
        _selectedPreset.value = template.defaultPreset
        _showTemplatePicker.value = false
        refreshPreview()
    }

    fun resetAllChanges() {
        _htmlCode.value = originalCode
        codeUndoHistory.clear()
        _hasChangesToReset.value = false
        refreshPreview()
    }

    // AI Studio Dialog Controls
    fun openAiDialog() {
        _aiError.value = null
        _showAiDialog.value = true
    }

    fun closeAiDialog() {
        _showAiDialog.value = false
    }

    fun executeAiMotionTask(taskType: String, prompt: String) {
        if (prompt.isBlank() && taskType != "FIX") return
        viewModelScope.launch {
            _isAiThinking.value = true
            _aiError.value = null
            val result = aiService.executeAiMotionTask(taskType, _htmlCode.value, prompt)
            _isAiThinking.value = false

            result.onSuccess { newHtml ->
                if (originalCode.isEmpty()) {
                    originalCode = _htmlCode.value
                }
                codeUndoHistory.add(_htmlCode.value)
                _htmlCode.value = newHtml
                _hasChangesToReset.value = true
                _showAiDialog.value = false
                refreshPreview()
            }.onFailure { err ->
                _aiError.value = err.message ?: "Failed to execute AI task"
            }
        }
    }

    fun executeAiMotionEdit(instruction: String) {
        executeAiMotionTask("EDIT", instruction)
    }

    fun startRender(activity: android.app.Activity? = null, activeWebView: WebView? = null) {
        if (renderJob?.isActive == true) return

        renderJob = viewModelScope.launch {
            try {
                val file = renderer.renderToVideo(
                    htmlContent = _htmlCode.value,
                    preset = _selectedPreset.value,
                    fpsOption = _selectedFps.value,
                    durationSec = _durationSec.value,
                    bitratePreset = _selectedBitrate.value,
                    captureMode = _captureMode.value,
                    cleanControls = _autoCleanControls.value,
                    activity = activity,
                    activeWebView = activeWebView,
                    onProgress = { phase -> _renderPhase.value = phase }
                )

                val thumb = generateVideoThumbnail(file)
                val newItem = ExportedVideoItem(
                    id = file.name,
                    title = _projectTitle.value,
                    file = file,
                    durationSec = _durationSec.value,
                    resolution = "${_selectedPreset.value.width}×${_selectedPreset.value.height}",
                    timestamp = System.currentTimeMillis(),
                    thumbnail = thumb
                )

                _exportedVideos.value = listOf(newItem) + _exportedVideos.value.filter { it.id != newItem.id }
                _activeVideoPreview.value = newItem

            } catch (e: Exception) {
                // Handled in renderer onProgress
            }
        }
    }

    fun cancelRender() {
        renderJob?.cancel()
        renderJob = null
        _renderPhase.value = RenderPhase.Idle
    }

    fun dismissRenderModal() {
        _renderPhase.value = RenderPhase.Idle
    }

    fun playVideo(item: ExportedVideoItem) {
        _activeVideoPreview.value = item
    }

    fun closeVideoPlayer() {
        _activeVideoPreview.value = null
    }

    fun deleteExportedVideo(item: ExportedVideoItem) {
        item.file.delete()
        _exportedVideos.value = _exportedVideos.value.filter { it.id != item.id }
        if (_activeVideoPreview.value?.id == item.id) {
            _activeVideoPreview.value = null
        }
    }
}
